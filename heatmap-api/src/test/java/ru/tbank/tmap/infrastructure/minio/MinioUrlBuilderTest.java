package ru.tbank.tmap.infrastructure.minio;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class MinioUrlBuilderTest {

    private static final String ENDPOINT = "http://minio:9000";
    private static final String PUBLIC_ENDPOINT = "http://localhost:9000";
    private static final String BUCKET = "tmap";

    @Nested
    class BuildPublicUrl {

        @Test
        void buildPublicUrl_whenObjectKeyIsValid_thenReturnsAbsoluteUrl() {
            final MinioUrlBuilder builder = newBuilder(ENDPOINT, PUBLIC_ENDPOINT, BUCKET);

            final String url = builder.buildPublicUrl("districts/kazan/aviastroitelny.jpg");

            assertThat(url).isEqualTo(PUBLIC_ENDPOINT + "/tmap/districts/kazan/aviastroitelny.jpg");
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {" ", "   "})
        void buildPublicUrl_whenObjectKeyIsBlank_thenReturnsNull(final String objectKey) {
            final MinioUrlBuilder builder = newBuilder(ENDPOINT, PUBLIC_ENDPOINT, BUCKET);

            assertThat(builder.buildPublicUrl(objectKey)).isNull();
        }

        @Test
        void buildPublicUrl_whenObjectKeyHasLeadingSlash_thenStripsSlashFromKey() {
            final MinioUrlBuilder builder = newBuilder(ENDPOINT, PUBLIC_ENDPOINT, BUCKET);

            final String url = builder.buildPublicUrl("/districts/kazan/aviastroitelny.jpg");

            assertThat(url).isEqualTo(PUBLIC_ENDPOINT + "/tmap/districts/kazan/aviastroitelny.jpg");
        }

        @Test
        void buildPublicUrl_whenPublicEndpointHasTrailingSlash_thenStripsSlashFromEndpoint() {
            final MinioUrlBuilder builder = newBuilder(ENDPOINT, PUBLIC_ENDPOINT + "/", BUCKET);

            final String url = builder.buildPublicUrl("districts/kazan/aviastroitelny.jpg");

            assertThat(url).isEqualTo(PUBLIC_ENDPOINT + "/tmap/districts/kazan/aviastroitelny.jpg");
        }
    }

    @Nested
    class EndpointSelection {

        @Test
        void buildPublicUrl_whenPublicEndpointIsProvided_thenUsesPublicEndpoint() {
            final MinioUrlBuilder builder = newBuilder(ENDPOINT, PUBLIC_ENDPOINT, BUCKET);

            assertThat(builder.buildPublicUrl("a.jpg"))
                    .startsWith(PUBLIC_ENDPOINT + "/");
        }

        @Test
        void buildPublicUrl_whenPublicEndpointIsNull_thenFallsBackToEndpoint() {
            final MinioUrlBuilder builder = newBuilder(ENDPOINT, null, BUCKET);

            assertThat(builder.buildPublicUrl("a.jpg"))
                    .isEqualTo(ENDPOINT + "/tmap/a.jpg");
        }

        @Test
        void buildPublicUrl_whenPublicEndpointIsBlank_thenFallsBackToEndpoint() {
            final MinioUrlBuilder builder = newBuilder(ENDPOINT, "  ", BUCKET);

            assertThat(builder.buildPublicUrl("a.jpg"))
                    .isEqualTo(ENDPOINT + "/tmap/a.jpg");
        }
    }

    private static MinioUrlBuilder newBuilder(
            final String endpoint,
            final String publicEndpoint,
            final String bucket
    ) {
        return new MinioUrlBuilder(new MinioProperties(
                endpoint,
                publicEndpoint,
                "access",
                "secret",
                bucket,
                "us-east-1"
        ));
    }
}
