package ru.tbank.tmap.venue.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VenuePendingUpdateTest {

    @Test
    void create_whenPendingUpdateIsCreated_thenItIsMarkedAsNewForSpringData() {
        final Venue venue = VenueTestFactory.createVenue(VenueStatus.ACTIVE, null);

        final VenuePendingUpdate pendingUpdate = VenuePendingUpdate.create(
                venue,
                VenueTestFactory.defaultUpdatedContent()
        );

        assertThat(pendingUpdate.isNew()).isTrue();
        assertThat(pendingUpdate.getId()).isEqualTo(venue.getId());
    }
}
