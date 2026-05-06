package ru.tbank.tmap.venue.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openapitools.model.VenueModerationStatus;
import org.openapitools.model.VenueOwnerResponse;
import ru.tbank.tmap.infrastructure.minio.MinioProperties;
import ru.tbank.tmap.infrastructure.minio.MinioUrlBuilder;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.user.domain.User;
import ru.tbank.tmap.user.domain.UserRole;
import ru.tbank.tmap.venue.presentation.VenueMapper;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenueStatus;
import ru.tbank.tmap.venue.presentation.business.BusinessVenueOwnerMapper;

class VenueOwnerMapperTest {

    private static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private final MinioUrlBuilder minioUrlBuilder = new MinioUrlBuilder(
            new MinioProperties(
                    "http://localhost:9000",
                    null,
                    "test-access-key",
                    "test-secret-key",
                    "tmap-test",
                    "us-east-1"
            )
    );

    private final BusinessVenueOwnerMapper venueOwnerMapper = new BusinessVenueOwnerMapper(new VenueMapper(minioUrlBuilder));

    @Test
    void toResponse_whenVenueIsRejected_thenIncludesModerationStatusAndRejectReason() {
        final Venue venue = venue(VenueStatus.REJECTED, "Address does not match coordinates");

        final VenueOwnerResponse response = venueOwnerMapper.toResponse(venue);

        assertThat(response.getId()).isEqualTo(VENUE_ID);
        assertThat(response.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(response.getModerationStatus()).isEqualTo(VenueModerationStatus.REJECTED);
        assertThat(response.getRejectReason()).isEqualTo("Address does not match coordinates");
    }

    @Test
    void toResponse_whenVenueIsPendingUpdate_thenIncludesModerationStatus() {
        final Venue venue = venue(VenueStatus.PENDING_UPDATE, null);

        final VenueOwnerResponse response = venueOwnerMapper.toResponse(venue);

        assertThat(response.getModerationStatus()).isEqualTo(VenueModerationStatus.PENDING_UPDATE);
        assertThat(response.getRejectReason()).isNull();
    }

    @Test
    void toResponse_whenVenueHasPhoto_thenBuildsPublicUrlFromObjectKey() {
        final Venue venue = venue(VenueStatus.ACTIVE, null);
        venue.setPhotoObjectKey("venues/" + VENUE_ID + "/photo.jpg");

        final VenueOwnerResponse response = venueOwnerMapper.toResponse(venue);

        assertThat(response.getPhotoUrl())
                .hasToString("http://localhost:9000/tmap-test/venues/" + VENUE_ID + "/photo.jpg");
    }

    @Test
    void toResponse_whenVenueHasNoPhoto_thenPhotoUrlIsNull() {
        final Venue venue = venue(VenueStatus.ACTIVE, null);

        final VenueOwnerResponse response = venueOwnerMapper.toResponse(venue);

        assertThat(response.getPhotoUrl()).isNull();
    }

    private Venue venue(final VenueStatus status, final String rejectReason) {
        final User owner = new User(
                OWNER_ID,
                "owner@example.com",
                "password-hash",
                "Owner",
                UserRole.BUSINESS_OWNER
        );
        final Venue venue = new Venue(
                VENUE_ID,
                owner,
                "Bar One",
                "Kazan Center, 2",
                GeoPoint.of(55.7905, 49.1140),
                617422037122678783L,
                VenueCategory.ENTERTAINMENT
        );
        venue.setStatus(status);
        venue.setRejectReason(rejectReason);
        return venue;
    }
}
