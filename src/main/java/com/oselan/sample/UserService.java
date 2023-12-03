package com.oselan.sample;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.oselan.commons.exceptions.ConflictException;
import com.oselan.excelexporter.ColumnDefinition;
import com.oselan.excelexporter.ExcelExporter;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class UserService  {
 
	@Autowired
	private UserRepository userRepository;
	 
	@Async 
//	@SneakyThrows(InterruptedException.class)
	public void generateReport(OutputStream stream) throws ConflictException, IOException {

		log.info("Generating report ... "); 
		List<ColumnDefinition> columnsDef = ColumnDefinition.listBuilder()
				.withColumn("Id", "id")
				.withColumn("First Name", "firstName")
				.withColumn("Last Name", "lastName") .build();
		ExcelExporter<UserDTO> exporter = new ExcelExporter<UserDTO>(stream, columnsDef, "User Sheet");
		try (exporter) { 
			log.info("generating users report" );
			exporter.open();
			exporter.setDataFetchSize(5000);
//     		exporter.setMaxRowsPerSheet(5989);
			exporter.generateReportFromDataProvider(
					//function that retrieves data page and takes a parameter a pageable 
					(pageable) ->{
						pageable = pageable.withSort(Sort.by("id","firstName"));
						return userRepository.findAll( pageable);
					},
					//function that maps the data to a dto containing the properties defined in the columnsDef. 
					   u ->  UserDTO.builder().id(u.getId())
									.firstName(u.getFirstName())
									.lastName(u.getLastName())
									.build() 
					   		 );
			exporter.close();
		} catch (Exception e) {
			log.error("Exception occured generating report", e);
			throw e;
		}
	}
}
