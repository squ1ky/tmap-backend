package ru.tbank.tmap.generator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.tbank.tmap.generator.config.GeneratorProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(GeneratorProperties.class)
public class DataGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DataGeneratorApplication.class, args);
    }

}
