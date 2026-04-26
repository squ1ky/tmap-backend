package ru.tbank.tmap.infrastructure.minio;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.minio")
public record MinioProperties(
        String endpoint,
        String publicEndpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String region,
        Duration presignedUrlExpiry,
        DataSize maxFileSize
) {
}
