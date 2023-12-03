package com.oselan;

import java.io.File;
import java.io.FileOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.oselan.sample.UserService;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@Slf4j
public class ExcelExporterApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(ExcelExporterApplication.class, args);
	}
	
	@Autowired
	private UserService userService; 
	
	@Override
	public void run(String... args) throws Exception {
		log.info("Generating and exporting data");
		File file = new File("FileStore\\temp.xlsx");
		
		if (!file.exists())
			file.createNewFile();
		    
        log.info("Generating report to : "+  file.getAbsolutePath());
		FileOutputStream stream = new FileOutputStream(file);
		 
		userService.generateReport(stream); 
		
	}

}
