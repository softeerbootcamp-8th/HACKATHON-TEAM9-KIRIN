package com.kirin.superservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SuperserviceApplication {

	public static void main(String[] args) {
		SpringApplication.run(SuperserviceApplication.class, args);
	}

}
