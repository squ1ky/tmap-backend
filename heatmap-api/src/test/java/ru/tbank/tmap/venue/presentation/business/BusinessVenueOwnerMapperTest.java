package ru.tbank.tmap.venue.presentation.business;

import static org.assertj.core.api.Assertions.assertThat;
import static ru.tbank.tmap.venue.domain.VenueTestFactory.OWNER_ID;
import static ru.tbank.tmap.venue.domain.VenueTestFactory.VENUE_ID;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.openapitools.model.LoyaltyRuleResponse;
import org.openapitools.model.VenueModerationStatus;
import org.openapitools.model.VenueOwnerResponse;
import ru.tbank.tmap.infrastructure.minio.MinioProperties;
import ru.tbank.tmap.infrastructure.minio.MinioUrlBuilder;
import ru.tbank.tmap.venue.domain.VenueTestFactory;
import ru.tbank.tmap.venue.presentation.VenueMapper;
import ru.tbank.tmap.venue.application.query.VenueDetails;
import ru.tbank.tmap.venue.domain.Venue;
import ru.tbank.tmap.venue.domain.VenuePendingUpdate;
import ru.tbank.tmap.venue.domain.VenueStatus;

class BusinessVenueOwnerMapperTest {

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

    private final BusinessVenueOwnerMapper venueOwnerMapper =
            new BusinessVenueOwnerMapper(new VenueMapper(minioUrlBuilder));

    @Test
    void toResponse_whenVenueIsRejected_thenIncludesModerationStatusAndRejectReason() {
        final String rejectReason = "Address does not match coordinates";
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.REJECTED, rejectReason);

        final VenueOwnerResponse response = venueOwnerMapper.toResponse(venue);

        assertThat(response.getId()).isEqualTo(VENUE_ID);
        assertThat(response.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(response.getName()).isEqualTo("Bar One");
        assertThat(response.getModerationStatus()).isEqualTo(VenueModerationStatus.REJECTED);
        assertThat(response.getRejectReason()).isEqualTo(rejectReason);
    }

    @Test
    void toResponse_whenVenueHasPhoto_thenBuildsPublicUrlFromObjectKey() {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        venue.setPhotoObjectKey("venues/" + VENUE_ID + "/photo.jpg");

        final VenueOwnerResponse response = venueOwnerMapper.toResponse(venue);

        assertThat(response.getPhotoUrl())
                .hasToString("http://localhost:9000/tmap-test/venues/" + VENUE_ID + "/photo.jpg");
    }

    @Test
    void toResponse_whenVenueHasNoPhoto_thenPhotoUrlIsNull() {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);

        final VenueOwnerResponse response = venueOwnerMapper.toResponse(venue);

        assertThat(response.getPhotoUrl()).isNull();
    }

    @Test
    void toResponse_whenVenueHasPendingUpdate_thenTakesUpdatedAtFromDraft() {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        venue.setUpdatedAt(OffsetDateTime.parse("2026-05-06T15:00:00+03:00"));

        final VenuePendingUpdate pendingUpdate =
                VenueTestFactory.createPendingUpdate(venue, VenueStatus.PENDING_UPDATE);
        pendingUpdate.setUpdatedAt(OffsetDateTime.parse("2026-05-06T16:30:00+03:00"));

        final VenueDetails details = new VenueDetails(venue, pendingUpdate);

        final VenueOwnerResponse response = venueOwnerMapper.toResponse(details);

        assertThat(response.getUpdatedAt()).isEqualTo(OffsetDateTime.parse("2026-05-06T16:30:00+03:00"));
    }

    @Test
    void toResponse_whenPromotionsProvided_thenIncludesLoyaltyRules() {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);
        final LoyaltyRuleResponse promotion = new LoyaltyRuleResponse()
                .id(java.util.UUID.fromString("44444444-4444-4444-4444-444444444444"))
                .venueId(VENUE_ID)
                .description("Discount 15%")
                .discountPercent(15)
                .maxUsages(100)
                .remainingUsages(96L)
                .active(true);

        final VenueOwnerResponse response = venueOwnerMapper.toResponse(venue, List.of(promotion));

        assertThat(response.getPromotions()).hasSize(1);
        assertThat(response.getPromotions().getFirst().getDiscountPercent()).isEqualTo(15);
    }
}
