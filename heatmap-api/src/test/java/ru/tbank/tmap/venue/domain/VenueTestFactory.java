package ru.tbank.tmap.venue.domain;

import ru.tbank.tmap.shared.geo.GeoPoint;
import ru.tbank.tmap.venue.api.VenueCategory;

import java.util.UUID;

public class VenueTestFactory {

    public static final UUID VENUE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID OWNER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final long H3_RES_9 = 617422037122678783L;

    public static VenueContent defaultContent() {
        return new VenueContent(
                "Bar One",
                "Kazan Center, 2",
                GeoPoint.of(55.7905, 49.1140),
                H3_RES_9,
                VenueCategory.ENTERTAINMENT,
                "Default description",
                null,
                null
        );
    }

    public static VenueContent defaultUpdatedContent() {
        return new VenueContent(
                "Bar Two",
                "Kazan Center, 5",
                GeoPoint.of(55.8000, 49.1300),
                617422037122678784L,
                VenueCategory.FOOD,
                "Updated description",
                "Soup",
                "Jazz"
        );
    }

    public static VenueContent content(String name, String address, VenueCategory category) {
        VenueContent def = defaultContent();

        return new VenueContent(
                name,
                address,
                def.location(),
                def.h3Res9(),
                category,
                def.description(),
                def.dishOfDay(),
                def.music()
        );
    }

    public static VenueContent content(
            String name, String address, GeoPoint location, long h3Res9,
            VenueCategory category, String description, String dishOfDay, String music
    ) {
        return new VenueContent(
                name,
                address,
                location,
                h3Res9,
                category,
                description,
                dishOfDay,
                music
        );
    }

    public static Venue createVenue(VenueStatus status, String rejectReason) {
        return createVenue(status, rejectReason, defaultContent());
    }

    public static Venue createVenue(VenueStatus status, String rejectReason, VenueContent content) {
        Venue venue = Venue.builder()
                .id(VENUE_ID)
                .ownerId(OWNER_ID)
                .content(content)
                .build();

        venue.setStatus(status);
        venue.setRejectReason(rejectReason);
        return venue;
    }

    public static VenuePendingUpdate createPendingUpdate(Venue venue, VenueStatus status) {
        return createPendingUpdate(venue, status, null, defaultUpdatedContent());
    }

    public static VenuePendingUpdate createPendingUpdate(
            Venue venue,
            VenueStatus status,
            String rejectReason,
            VenueContent updatedContent
    ) {
        VenuePendingUpdate pendingUpdate = VenuePendingUpdate.create(venue, updatedContent);
        pendingUpdate.setStatus(status);
        pendingUpdate.setRejectReason(rejectReason);
        return pendingUpdate;
    }
}