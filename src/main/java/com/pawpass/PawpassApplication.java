package com.pawpass;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling // facility 배치 동기화(@Scheduled)를 위해 필요
@SpringBootApplication
public class PawpassApplication {

	public static void main(String[] args) {
		SpringApplication.run(PawpassApplication.class, args);
	}

}
