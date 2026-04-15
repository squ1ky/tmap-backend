package ru.tbank.tmap;

import org.springframework.boot.SpringApplication;

public class TestDataGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.from(DataGeneratorApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
