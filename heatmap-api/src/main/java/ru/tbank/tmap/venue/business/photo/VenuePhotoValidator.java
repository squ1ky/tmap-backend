package ru.tbank.tmap.venue.business.photo;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import ru.tbank.tmap.venue.exception.InvalidVenuePhotoException;

import java.util.Map;

@Component
public class VenuePhotoValidator {

    private static final long MAX_SIZE_BYTES = 10L * 1024L * 1024L; // 10 MB

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    public String validateAndGetExtension(final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidVenuePhotoException("Photo file must not be empty");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new InvalidVenuePhotoException("Photo file is too large. Maximum size is 10MB.");
        }

        final String contentType = file.getContentType();
        final String extension = contentType == null ? null : CONTENT_TYPE_TO_EXTENSION.get(contentType);
        if (extension == null) {
            throw new InvalidVenuePhotoException("Unsupported photo format. Allowed formats: jpeg, png, webp.");
        }
        return extension;
    }
}
