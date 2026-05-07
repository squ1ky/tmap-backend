package ru.tbank.tmap.venue.business;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.openapitools.model.VenueModerationStatus;
import org.openapitools.model.VenueOwnerResponse;
import ru.tbank.tmap.infrastructure.minio.MinioProperties;
import ru.tbank.tmap.infrastructure.minio.MinioUrlBuilder;
import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.venue.presentation.VenueMapper;
import ru.tbank.tmap.venue.application.query.VenueDetails;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenueCategory;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
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

    @Test
    void toResponse_whenActiveVenueHasRejectedPendingUpdate_thenKeepsPublishedFieldsAndReturnsPendingStatus() {
        final Venue venue = venue(VenueStatus.ACTIVE, null);
        final VenuePendingUpdate pendingUpdate = new VenuePendingUpdate(venue);
        pendingUpdate.setName("Bar Two");
        pendingUpdate.setAddress("Kazan Center, 4");
        pendingUpdate.setLocation(GeoPoint.of(55.8000, 49.1300));
        pendingUpdate.setH3Res9(617422037122678784L);
        pendingUpdate.setCategory(VenueCategory.FOOD);
        pendingUpdate.setStatus(VenueStatus.REJECTED);
        pendingUpdate.setRejectReason("Name does not match");

        final VenueOwnerResponse response = venueOwnerMapper.toResponse(new VenueDetails(venue, pendingUpdate));

        assertThat(response.getName()).isEqualTo("Bar One");
        assertThat(response.getModerationStatus()).isEqualTo(VenueModerationStatus.REJECTED);
        assertThat(response.getRejectReason()).isEqualTo("Name does not match");
    }

    @Test
    void toResponse_whenActiveVenueHasPendingUpdate_thenKeepsPublishedFieldsAndUsesDraftStatusAndUpdatedAt() {
        final Venue venue = venue(VenueStatus.ACTIVE, null);
        venue.setUpdatedAt(OffsetDateTime.parse("2026-05-06T15:00:00+03:00"));

        final VenuePendingUpdate pendingUpdate = new VenuePendingUpdate(venue);
        pendingUpdate.setName("Bar Two");
        pendingUpdate.setAddress("Kazan Center, 4");
        pendingUpdate.setLocation(GeoPoint.of(55.8000, 49.1300));
        pendingUpdate.setH3Res9(617422037122678784L);
        pendingUpdate.setCategory(VenueCategory.FOOD);
        pendingUpdate.setStatus(VenueStatus.PENDING_UPDATE);
        pendingUpdate.setUpdatedAt(OffsetDateTime.parse("2026-05-06T16:30:00+03:00"));

        final VenueOwnerResponse response = venueOwnerMapper.toResponse(new VenueDetails(venue, pendingUpdate));

        assertThat(response.getName()).isEqualTo("Bar One");
        assertThat(response.getAddress()).isEqualTo("Kazan Center, 2");
        assertThat(response.getLat()).isEqualTo(55.7905);
        assertThat(response.getLng()).isEqualTo(49.1140);
        assertThat(response.getModerationStatus()).isEqualTo(VenueModerationStatus.PENDING_UPDATE);
        assertThat(response.getUpdatedAt()).isEqualTo(OffsetDateTime.parse("2026-05-06T16:30:00+03:00"));
    }

    private Venue venue(final VenueStatus status, final String rejectReason) {
        final Venue venue = Venue.builder()
                .id(VENUE_ID)
                .ownerId(OWNER_ID)
                .name("Bar One")
                .address("Kazan Center, 2")
                .location(GeoPoint.of(55.7905, 49.1140))
                .h3Res9(617422037122678783L)
                .category(VenueCategory.ENTERTAINMENT)
                .build();

        venue.setStatus(status);
        venue.setRejectReason(rejectReason);
        return venue;
    }
}
