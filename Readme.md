# Overview
Excel Exporter solves the problem were the data retrieval from db takes a long time and/or the data is large. 
It handles data retrieval in a separate thread so as not to block writing to the excel sheet.  
As excel has a limit on data rows per sheet it also handles creating new sheets on the fly.
It simply manages data retrieval via pageable function call to extract data from the database in pages and then export these pages on to excel.  The class allows mapping the data to a dto and automatically reads predefined properties from the dto to build the excel columns.


