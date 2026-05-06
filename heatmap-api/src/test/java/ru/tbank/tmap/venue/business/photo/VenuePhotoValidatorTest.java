package ru.tbank.tmap.venue.business.photo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import ru.tbank.tmap.venue.domain.exception.InvalidVenuePhotoException;

class VenuePhotoValidatorTest {

    private static final long MAX_SIZE_BYTES = 10L * 1024L * 1024L;

    private final VenuePhotoValidator validator = new VenuePhotoValidator();

    @Test
    void validateAndGetExtension_whenJpegFile_thenReturnsJpgExtension() {
        final MultipartFile file = file("image/jpeg", new byte[]{1, 2, 3});

        final String extension = validator.validateAndGetExtension(file);

        assertThat(extension).isEqualTo("jpg");
    }

    @Test
    void validateAndGetExtension_whenPngFile_thenReturnsPngExtension() {
        final MultipartFile file = file("image/png", new byte[]{1, 2, 3});

        final String extension = validator.validateAndGetExtension(file);

        assertThat(extension).isEqualTo("png");
    }

    @Test
    void validateAndGetExtension_whenWebpFile_thenReturnsWebpExtension() {
        final MultipartFile file = file("image/webp", new byte[]{1, 2, 3});

        final String extension = validator.validateAndGetExtension(file);

        assertThat(extension).isEqualTo("webp");
    }

    @Test
    void validateAndGetExtension_whenFileIsNull_thenThrows() {
        assertThatThrownBy(() -> validator.validateAndGetExtension(null))
                .isInstanceOf(InvalidVenuePhotoException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void validateAndGetExtension_whenFileIsEmpty_thenThrows() {
        final MultipartFile file = file("image/jpeg", new byte[0]);

        assertThatThrownBy(() -> validator.validateAndGetExtension(file))
                .isInstanceOf(InvalidVenuePhotoException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void validateAndGetExtension_whenFileExceedsMaxSize_thenThrows() {
        final MultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[(int) MAX_SIZE_BYTES + 1]
        );

        assertThatThrownBy(() -> validator.validateAndGetExtension(file))
                .isInstanceOf(InvalidVenuePhotoException.class)
                .hasMessageContaining("too large");
    }

    @Test
    void validateAndGetExtension_whenFileSizeIsExactlyAtLimit_thenAccepts() {
        final MultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                new byte[(int) MAX_SIZE_BYTES]
        );

        final String extension = validator.validateAndGetExtension(file);

        assertThat(extension).isEqualTo("jpg");
    }

    @Test
    void validateAndGetExtension_whenContentTypeIsNull_thenThrows() {
        final MultipartFile file = file(null, new byte[]{1, 2, 3});

        assertThatThrownBy(() -> validator.validateAndGetExtension(file))
                .isInstanceOf(InvalidVenuePhotoException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    void validateAndGetExtension_whenContentTypeIsNotImage_thenThrows() {
        final MultipartFile file = file("application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> validator.validateAndGetExtension(file))
                .isInstanceOf(InvalidVenuePhotoException.class)
                .hasMessageContaining("Unsupported");
    }

    @Test
    void validateAndGetExtension_whenContentTypeIsImageGif_thenThrows() {
        final MultipartFile file = file("image/gif", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> validator.validateAndGetExtension(file))
                .isInstanceOf(InvalidVenuePhotoException.class)
                .hasMessageContaining("Unsupported");
    }

    private MultipartFile file(final String contentType, final byte[] content) {
        return new MockMultipartFile("file", "photo", contentType, content);
    }
}