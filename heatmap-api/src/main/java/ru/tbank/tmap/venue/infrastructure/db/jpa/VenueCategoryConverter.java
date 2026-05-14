package ru.tbank.tmap.venue.infrastructure.db.jpa;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.tbank.tmap.venue.api.VenueCategory;

@Converter
public class VenueCategoryConverter implements AttributeConverter<VenueCategory, String> {

    @Override
    public String convertToDatabaseColumn(VenueCategory venueCategory) {
        return venueCategory.name();
    }

    @Override
    public VenueCategory convertToEntityAttribute(String value) {
        return VenueCategory.fromString(value);
    }
}
