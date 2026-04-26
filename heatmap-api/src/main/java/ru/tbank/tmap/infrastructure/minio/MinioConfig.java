package ru.tbank.tmap.infrastructure.minio;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.errors.MinioException;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();
    }

    @Bean
    public HealthIndicator minioHealthIndicator(
            MinioClient minioClient,
            MinioProperties properties
    ) {
        return () -> {
            try {
                boolean bucketExists = minioClient.bucketExists(
                        BucketExistsArgs.builder()
                                .bucket(properties.bucket())
                                .build()
                );

                if (!bucketExists) {
                    return Health.down()
                            .withDetail("bucket", properties.bucket())
                            .withDetail("reason", "Configured bucket does not exist")
                            .build();
                }

                return Health.up()
                        .withDetail("bucket", properties.bucket())
                        .withDetail("endpoint", properties.endpoint())
                        .build();
            } catch (MinioException exception) {
                return Health.down(exception)
                        .withDetail("bucket", properties.bucket())
                        .withDetail("endpoint", properties.endpoint())
                        .build();
            }
        };
    }
}
