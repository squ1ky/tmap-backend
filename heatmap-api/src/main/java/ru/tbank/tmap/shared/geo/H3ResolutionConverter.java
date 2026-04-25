package ru.tbank.tmap.shared.geo;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class H3ResolutionConverter implements AttributeConverter<H3Resolution, Short> {

    @Override
    public Short convertToDatabaseColumn(H3Resolution attribute) {
        return attribute == null ? null : (short) attribute.getValue();
    }

    @Override
    public H3Resolution convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : H3Resolution.of(dbData);
    }
}
