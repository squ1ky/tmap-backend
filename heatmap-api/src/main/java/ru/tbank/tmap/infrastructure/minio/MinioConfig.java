package ru.tbank.tmap.infrastructure.minio;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import io.minio.errors.MinioException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MinioConfig {

    private static final String DETAIL_BUCKET = "bucket";
    private static final String DETAIL_ENDPOINT = "endpoint";
    private static final String DETAIL_REASON = "reason";

    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        MinioClient client = MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .build();

        initBucketPolicy(client, properties.bucket());

        return client;
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
                            .withDetail(DETAIL_BUCKET, properties.bucket())
                            .withDetail(DETAIL_REASON, "Configured bucket does not exist")
                            .build();
                }

                return Health.up()
                        .withDetail(DETAIL_BUCKET, properties.bucket())
                        .withDetail(DETAIL_ENDPOINT, properties.endpoint())
                        .build();
            } catch (MinioException exception) {
                return Health.down(exception)
                        .withDetail(DETAIL_BUCKET, properties.bucket())
                        .withDetail(DETAIL_ENDPOINT, properties.endpoint())
                        .withDetail(DETAIL_REASON, "MinIO API exception")
                        .build();
            } catch (Exception exception) {
                return Health.down(exception)
                        .withDetail(DETAIL_BUCKET, properties.bucket())
                        .withDetail(DETAIL_ENDPOINT, properties.endpoint())
                        .withDetail(DETAIL_REASON, "Unexpected error during MinIO health check")
                        .build();
            }
        };
    }

    private void initBucketPolicy(MinioClient client, String bucket) {
        String policy = """
            {
                "Version": "2012-10-17",
                "Statement": [
                    {
                        "Effect": "Allow",
                        "Principal": {"AWS": "*"},
                        "Action": ["s3:GetObject"],
                        "Resource": ["arn:aws:s3:::%s/*"]
                    }
                ]
            }
            """.formatted(bucket);

        try {
            client.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(bucket)
                            .config(policy)
                            .build()
            );
        } catch (Exception e) {
            log.warn("Failed to set bucket policy for '{}': {}", bucket, e.getMessage());
        }
    }
}
