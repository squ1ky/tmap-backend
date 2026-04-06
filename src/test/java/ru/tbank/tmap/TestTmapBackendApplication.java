package ru.tbank.tmap;

import org.springframework.boot.SpringApplication;

public class TestTmapBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(TmapBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
