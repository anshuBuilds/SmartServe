package com.smartserve;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SmartServeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SmartServeApplication.class, args);
	}

}
