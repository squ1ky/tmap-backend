package ru.tbank.tmap.venue.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

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
