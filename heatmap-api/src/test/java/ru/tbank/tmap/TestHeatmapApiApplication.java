package ru.tbank.tmap;

import org.springframework.boot.SpringApplication;

public class TestHeatmapApiApplication {

	public static void main(String[] args) {
		SpringApplication.from(HeatmapApiApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
