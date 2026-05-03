package ru.tbank.tmap;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.tbank.tmap.infrastructure.minio.MinioProperties;
import ru.tbank.tmap.infrastructure.security.cors.CorsProperties;
import ru.tbank.tmap.auth.infrastructure.security.cookie.RefreshTokenCookieProperties;
import ru.tbank.tmap.auth.infrastructure.security.JwtProperties;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({
        JwtProperties.class,
        RefreshTokenCookieProperties.class,
        CorsProperties.class,
        MinioProperties.class
})
public class HeatmapApiApplication {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    public static void main(String[] args) {
        SpringApplication.run(HeatmapApiApplication.class, args);
    }

}
