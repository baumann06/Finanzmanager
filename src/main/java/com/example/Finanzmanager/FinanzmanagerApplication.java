package com.example.Finanzmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FinanzmanagerApplication {
	public static void main(String[] args) {
		SpringApplication.run(FinanzmanagerApplication.class, args);
	}
}