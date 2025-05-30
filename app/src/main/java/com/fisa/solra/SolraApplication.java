package com.fisa.solra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(scanBasePackages = "com.fisa.solra")
@EnableCaching
public class SolraApplication {

	public static void main(String[] args) {
		SpringApplication.run(SolraApplication.class, args);
	}

}
