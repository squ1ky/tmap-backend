package ru.tbank.tmap.infrastructure.minio;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.errors.ErrorResponseException;
import io.minio.errors.MinioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@RequiredArgsConstructor
@Component
public class MinioObjectStorage {

    private static final String NO_SUCH_KEY = "NoSuchKey";

    private static final String MESSAGE_FAILED_TO_PUT_OBJECT = "Failed to put object";
    private static final String MESSAGE_FAILED_TO_DELETE_OBJECT = "Failed to delete object";

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    public void putObject(
            final String objectKey,
            final InputStream inputStream,
            final long size,
            final String contentType
    ) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectKey)
                            .stream(inputStream, size, -1L)
                            .contentType(contentType)
                            .build()
            );
        } catch (MinioException e) {
            throw new ObjectStorageException(MESSAGE_FAILED_TO_PUT_OBJECT + ": " + objectKey, e);
        }
    }

    public void deleteObject(final String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioProperties.bucket())
                            .object(objectKey)
                            .build()
            );
        } catch (ErrorResponseException e) {
            if (NO_SUCH_KEY.equals(e.errorResponse().code())) {
                log.debug("Object already absent in storage: {}", objectKey);
                return;
            }
            throw new ObjectStorageException(MESSAGE_FAILED_TO_DELETE_OBJECT + ": " + objectKey, e);
        } catch (Exception e) {
            throw new ObjectStorageException(MESSAGE_FAILED_TO_DELETE_OBJECT + ": " + objectKey, e);
        }
    }
}
