package ru.tbank.tmap;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.tbank.tmap.config.security.cookie.CookieSecurityProperties;
import ru.tbank.tmap.config.security.jwt.JwtProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({JwtProperties.class, CookieSecurityProperties.class})
public class HeatmapApiApplication {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(HeatmapApiApplication.class, args);
    }

}
