package ru.tbank.tmap.infrastructure.minio;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MinioUrlBuilder {

    private final String baseUrl;

    public MinioUrlBuilder(final MinioProperties properties) {
        final String endpoint = StringUtils.hasText(properties.publicEndpoint())
                ? properties.publicEndpoint()
                : properties.endpoint();
        this.baseUrl = stripTrailingSlash(endpoint) + "/" + properties.bucket();
    }

    public String buildPublicUrl(final String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }

        final String normalized;
        if (objectKey.startsWith("/")) {
            normalized = objectKey.substring(1);
        } else {
            normalized = objectKey;
        }

        return baseUrl + "/" + normalized;
    }

    private String stripTrailingSlash(final String url) {
        if (url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }
}
