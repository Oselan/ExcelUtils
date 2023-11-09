package com.oselan.excelexporter;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.util.StringUtils;

import com.oselan.commons.exceptions.ConflictException;
import com.oselan.commons.exceptions.NotFoundException;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

/***
 * A performance oriented non-blocking excel exporter, records are added to a
 * queue and exporting reads from the queue to build the workbook report.
 * Finally the workbook is written to a stream.
 * 
 * @author Ahmad Hamid
 *
 * @param <T>
 */
@Slf4j
public class ExcelExporter<T> implements AutoCloseable {

	private Workbook workbook = null;
	/***
	 * Sheet to write to
	 */
	private Sheet activeSheet;

	private OutputStream stream;

	private ConcurrentLinkedQueue<T> dataRecordsQueue = new ConcurrentLinkedQueue<T>();

	private List<ColumnDefinition> columns;

	/***
	 * Report name
	 */
	private String sheetName = "Report";
	// used by POI excel to keep window of records in memory
	private static final int DEFAULT_BATCH_SIZE = 100;

	// used when calling the data provider to fetch data
	private static final int DEFAULT_DATA_FETCH_SIZE = 2000;
	// Excel has a limit of 1,048,576 rows
	private static final int DEFAULT_MAX_ROWS_PER_SHEET = 1048576;
	// Max size of queue so not to consume too much memory
	private static final int DEFAULT_MAX_QUEUE_SIZE = 10000;
	// wait 5mins for data before timeout.
	private static final long DEFAULT_DATA_WAIT_TIMEOUT = 5 * 60 * 1000;

	private int maxRowsPerSheet = DEFAULT_MAX_ROWS_PER_SHEET;
	private long dataWaitTimeout = DEFAULT_DATA_WAIT_TIMEOUT;
	private int maxQueueSize = DEFAULT_MAX_QUEUE_SIZE;
	private int dataFetchSize = DEFAULT_DATA_FETCH_SIZE;

	/***
	 * 
	 * @return The number of rows per sheet
	 */
	public int getMaxRowsPerSheet() {
		return maxRowsPerSheet;
	}

	/***
	 * Excel has a max rows per sheet of 1,048,576 (Default) This allows controlling
	 * number of rows per sheet. New sheets are created on the fly.
	 * 
	 * @param maxRowsPerSheet
	 */
	public void setMaxRowsPerSheet(int maxRowsPerSheet) {
		this.maxRowsPerSheet = maxRowsPerSheet;
	}

	/***
	 * @return The time to wait for slow queries in millseconds
	 */
	public long getDataWaitTimeout() {
		return dataWaitTimeout;
	}

	/**
	 * The time to wait for slow queries in millseconds Default is 5 x 60 x 1000 = 5
	 * mins
	 * 
	 * @param dataWaitTimeout
	 */
	public void setDataWaitTimeout(long dataWaitTimeout) {
		this.dataWaitTimeout = dataWaitTimeout;
	}

	/***
	 * @return The number of records to keep in queue before fetching new data
	 */
	public int getMaxQueueSize() {
		return maxQueueSize;
	}

	/***
	 * Control the number of records to keep in queue before fetching new data
	 * Default is 10000
	 * 
	 * @param maxQueueSize
	 */
	public void setMaxQueueSize(int maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
	}

	/***
	 * 
	 * @return Page size of date fetched from db.
	 */
	public int getDataFetchSize() {
		return dataFetchSize;
	}

	/***
	 * The size of the page to fetch from the database. Default is 2000 
	 * Larger pages will take more time and memory 
	 * @param dataFetchSize
	 */
	public void setDataFetchSize(int dataFetchSize) {
		this.dataFetchSize = dataFetchSize;
	}

	/***
	 * Flag to indicate the no more records are read from the data provider.
	 */
	private AtomicBoolean isEndOfData = new AtomicBoolean();

	/***
	 * Flag to indicate exporting to the stream completed
	 */
	private AtomicBoolean isWritingCompleted = new AtomicBoolean();

	/***
	 * 
	 * @param stream  output stream to write to
	 * @param headers optional ordered list of headers to add to the sheet.
	 * @param ordered list of column definitions
	 */
	public ExcelExporter(OutputStream stream, List<ColumnDefinition> columns) {
		this.stream = stream;
		this.columns = columns;
		this.columns.sort(new Comparator<ColumnDefinition>() {
			@Override
			public int compare(ColumnDefinition o1, ColumnDefinition o2) {
				return o1.getIndex().compareTo(o2.getIndex());
			}
		});
	}

	/***
	 * 
	 * @param stream
	 * @param columns
	 * @param sheetName
	 */
	public ExcelExporter(OutputStream stream, List<ColumnDefinition> columns, String sheetName) {
		this(stream, columns);
		this.sheetName = sheetName;
	}

	/***
	 * Adds a list of data records to the queue to be exported to the excel sheet.
	 * 
	 * @param dataRecords
	 * @throws IOException
	 */
	public void addRecords(List<T> dataRecords) throws IOException {
		addRecords(dataRecords, false);
	}

	/**
	 * Adds a list of data records to the queue and signals that there are more data
	 * 
	 * @param dataRecords
	 * @param isEndOfData true if no more records available false otherwise.
	 * @throws IOException
	 */
	@SneakyThrows(InterruptedException.class)
	public void addRecords(List<T> dataRecords, boolean isEndOfData) throws IOException {
		if (!isOpen())
			throw new IOException("Exporter not open - call open() before attempting to send data ");
		if (this.isEndOfData.get() || this.isWritingCompleted.get())
			throw new IOException("Attempting to add data after exporter was closed");
		// mem-safte wait until queue size goes belo max-queue-size
		while (dataRecordsQueue.size() + dataRecords.size() > maxQueueSize) {
			log.info("Waiting for queue to be written to excel.");
			TimeUnit.MILLISECONDS.sleep(100);
		}
		log.info("Adding data records, size {}", dataRecordsQueue.size());
		dataRecordsQueue.addAll(dataRecords);
		if (isEndOfData)
			closeData();
	}

	/**
	 * Indicate that no more data are available
	 */
	public void closeData() {
		this.isEndOfData.compareAndSet(false, true);
	}

	/***
	 * Create a cell
	 * 
	 * @param row
	 * @param columnCount
	 * @param value
	 * @param style
	 */
	private void createCell(Row row, int columnCount, Object value, CellStyle style) {
		Cell cell = row.createCell(columnCount);
		if (value instanceof Integer) {
			cell.setCellValue((Integer) value);
		} else if (value instanceof Boolean) {
			cell.setCellValue((Boolean) value);
		} else if (value instanceof Double) {
			cell.setCellValue((Double) value);
		} else {
			cell.setCellValue((String) value);
		}
		cell.setCellStyle(style);
	}

	/***
	 * Create a cell in a specific row , column with a value
	 * 
	 * @param row
	 * @param columnCount
	 * @param value
	 */
	private void createCell(Row row, int columnCount, Object value) {
		Cell cell = row.createCell(columnCount);
		if (value instanceof Integer) {
			cell.setCellValue((Integer) value);
		} else if (value instanceof Boolean) {
			cell.setCellValue((Boolean) value);
		} else if (value instanceof Double) {
			cell.setCellValue((Double) value);
		} else if (value instanceof Long) {
			cell.setCellValue((Long) value);
		} else if (value instanceof String) {
			cell.setCellValue((String) value);
		} else if (value != null) {
			cell.setCellValue(value.toString());
		}
	}

	/***
	 * Creates a header if headers list is found and has values
	 * 
	 * @param sheet
	 */
	private void writeHeaderLine(Sheet sheet) {

		Row row = sheet.createRow(0);
		int c = 0;
		for (ColumnDefinition colDef : columns) {
			createCell(row, c++, colDef.getHeader());
		}
	}

	/***
	 * Blocks until excel data is available or timeout Starts writing to the excel
	 * sheet When done writes the workbook to the output stream and signals
	 * completion.
	 * 
	 * @throws IOException
	 * @throws ConflictException
	 * @throws InterruptedException
	 * @throws NotFoundException
	 */
	@SneakyThrows(InterruptedException.class)
	public void export() throws ConflictException {
		try {
			if (!isOpen())
				throw new ConflictException("Exporter not open - call open() before attempting to send data ");
			long startTime = System.currentTimeMillis();
			// wait until we have data then create the workbook
			log.info("Waiting for initial records to write ... {} sec remaining.", (dataWaitTimeout) / 1000);
			while (dataRecordsQueue.isEmpty() && !isEndOfData.get()
					&& (System.currentTimeMillis() - startTime) <= dataWaitTimeout) {
				TimeUnit.MILLISECONDS.sleep(100);
			}

			if (dataRecordsQueue.isEmpty() && !isEndOfData.get()) {
				throw new ConflictException("Timed out after " + dataWaitTimeout + " ms and no data provided!");
			}
			// write while user is not done or more records are available
			while (!isEndOfData.get() || !dataRecordsQueue.isEmpty()) {
				int rowCount = writeDataLines(activeSheet);
				if (rowCount >= maxRowsPerSheet)
					openSheet();
				if (!isEndOfData.get() && dataRecordsQueue.isEmpty()) {
					log.info("Waiting for more records to write...");
					TimeUnit.MILLISECONDS.sleep(50);
				}
			}
			log.info("Writing workbook to stream {} rows ", activeSheet.getPhysicalNumberOfRows());
			workbook.write(stream);
		} catch (IOException ex) {
			throw new ConflictException("Failure during export", ex);
		} finally {
			// stream writing completed
			isWritingCompleted.set(true);
		}
	}

	/***
	 * Writes data records
	 * 
	 * @param sheet
	 * @throws IOException
	 * @returns rowCount on sheet
	 */
	private int writeDataLines(Sheet sheet) throws IOException {

		int rowCount = sheet.getLastRowNum() + 1;
		T dto;
		while (rowCount <= maxRowsPerSheet && (dto = dataRecordsQueue.poll()) != null) {
			Row row = sheet.createRow(rowCount++);
			int currentColumnCount = 0;
			for (ColumnDefinition colDef : columns) {
				if (StringUtils.hasText(colDef.getProperty())) {
					createCell(row, currentColumnCount++, getProperty(dto, colDef.getProperty()));
				} else
					createCell(row, currentColumnCount++, "");
			}
			if (rowCount % 100 == 0)
				log.info("Writing data records {} , remaining in queue {}", rowCount, dataRecordsQueue.size());
		}
		return rowCount;
	}

	/**
	 * Create the workbook and sheet and writes header.
	 */
	public void open() {
		if (workbook != null)
			return;
		isEndOfData.set(false);
		isWritingCompleted.set(false);
		// create workbook
		workbook = new SXSSFWorkbook(DEFAULT_BATCH_SIZE);
		openSheet();
	}

	/***
	 * Creates a new sheet with sheet name and sets active sheet. Every time this
	 * function is called it creates a new sheet with same name suffixed by a number
	 */
	private void openSheet() {
		int sheetsCount = workbook.getNumberOfSheets();
		String genSheetName = sheetsCount == 0 ? sheetName : sheetName + "_" + sheetsCount;
		activeSheet = workbook.createSheet(genSheetName);
		writeHeaderLine(activeSheet);
	}

	private boolean isOpen() {
		return (workbook != null && !isWritingCompleted.get());
	}

	/***
	 * Waits until excel is generated and written to the stream and closes the
	 * workbook
	 * 
	 * @throws ConflictException
	 * @throws IOException
	 */
	@Override
	@SneakyThrows(InterruptedException.class)
	public void close() {
		// consumer closed so no more records will be added.
		log.info("Closing exporter after all pending records are written.");
		if (!isOpen()) {
			log.warn("Exporter already closed! nothing to do.");
			return;
		}
		int sleeping = 0;
		while (!isEndOfData.get() || !isWritingCompleted.get()) {
			// sleep while more records are available or not all records are written out.
			if ((sleeping--) % 100 == 0)
				log.info("Waiting for records to be written and data stream to be closed to close ...");
			TimeUnit.MILLISECONDS.sleep(50);
		}
		try {
			workbook.close();
		} catch (IOException e) {
			// throw new ConflictException(e);
			log.error("Unexpected exception ", e);
		}
		// free file resources
		if (workbook instanceof SXSSFWorkbook)
			((SXSSFWorkbook) workbook).dispose();

	}

	/**
	 * Fetch a property from an object. For example of you wanted to get the foo
	 * property on a bar object you would normally call {@code bar.getFoo()}. This
	 * method lets you call it like {@code BeanUtil.getProperty(bar, "foo")}
	 * 
	 * @param obj      The object who's property you want to fetch
	 * @param property The property name
	 * @return The value of the property or null if it does not exist.
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public Object getProperty(T obj, String property) throws IOException {
		Object returnValue = null;

		try {
			String methodName = "get" + property.substring(0, 1).toUpperCase()
					+ property.substring(1, property.length());
			Class<T> clazz = (Class<T>) obj.getClass();
			Method method = clazz.getMethod(methodName);
			returnValue = method.invoke(obj);
		} catch (Exception e) {
			throw new IOException("Error reading property " + property, e);
		}

		return returnValue;
	}

	/***
	 * Asyncronously generate a report
	 * 
	 * @param A function that takes as parameter a Pageable and provides a slice.
	 *          Typically a repository method of the format: Slice<T>
	 *          methodName(....params, Pageable pageable)
	 * @throws ConflictException
	 */
	public void generateReportFromDataProvider(Function<PageRequest, Slice<T>> pagedDataProvider)
			throws ConflictException {
		generateReportFromDataProvider(pagedDataProvider, null);
	}

	/***
	 * Asyncronously generate a report
	 * 
	 * @param A      function that takes as parameter a Pageable and provides a
	 *               slice. Typically a repository method of the format: Slice<T>
	 *               methodName(....params, Pageable pageable)
	 * @param mapper A function that maps the data from provider to record data type
	 *               or null if both are the same.
	 * @throws ConflictException
	 */
	public <D> void generateReportFromDataProvider(Function<PageRequest, Slice<D>> pagedDataProvider,
			Function<D, T> mapper) throws ConflictException {
		CompletableFuture<Integer> runner = CompletableFuture.supplyAsync(() -> {
			try {
				return generateReport(pagedDataProvider, mapper);
			} catch (ConflictException e) {
				throw new CompletionException(e);
			}
		});
		this.export();

		try {
			Integer numberOfRecords = runner.join();
			log.info("Report generated for {} records ", numberOfRecords);
		} catch (CompletionException ex) {
			// convert the inner exception to a conflict exception
			log.error("Report generation failed", ex.getCause());
			if (ex.getCause() instanceof ConflictException)
				throw (ConflictException) ex.getCause();
			else
				throw new ConflictException("Unknown exception occured while generating the report", ex.getCause());
		}

	}

	/***
	 * Scrolls through the data provider and builds the excel report. NOTE: This
	 * method is running asyncrhonously
	 * 
	 * @param pagedDataProvider
	 * @return
	 * @throws ConflictException
	 */
	@SuppressWarnings("unchecked")
	private <D> Integer generateReport(Function<PageRequest, Slice<D>> pagedDataProvider, Function<D, T> mapper)
			throws ConflictException {

		int totalCount = 0;
		boolean hasMore = true;
		PageRequest pageable = PageRequest.of(0, dataFetchSize);
//    		  Pageable.ofSize(dataFetchSize).withPage(0);
		try {
			while (hasMore) {

				log.info("Retrieving next batch of {} records", dataFetchSize);
				Slice<D> pageOfRecords = pagedDataProvider.apply(pageable);

				hasMore = pageOfRecords.hasNext();

				totalCount += pageOfRecords.getNumberOfElements();
				log.info("Retrieved {} records , sending to exporter, total {} ", pageOfRecords.getNumberOfElements(),
						totalCount);
				if (pageOfRecords.hasContent()) {
					if (mapper != null) {
						List<T> recordList = pageOfRecords.getContent().stream().map(mapper)
								.collect(Collectors.toList());
						this.addRecords(recordList);
					} else // Assume Record type and mapper type are the same.
						this.addRecords((List<T>) pageOfRecords.getContent());

					if (hasMore)
						pageable = pageable.withPage(pageable.getPageNumber() + 1);
				} else if (pageable.getPageNumber() == 0) { // first page and has no content
					throw new ConflictException("No data found to generate report.");
				}

			}
		} catch (IOException e) {
			throw new ConflictException("Failed to add data to report ", e);
		} catch (ConflictException e) {
			throw e;
		} catch (Exception e) {
			throw new ConflictException("Failed to provide data ", e);
		} finally {
			closeData();
		}
		return totalCount;
	}
}