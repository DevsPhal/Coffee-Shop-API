package org.group1.coffeeshopapi.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

// Plain @Enumerated(EnumType.STRING) throws and crashes the whole read if the column ever holds
// "" or any other value that isn't a current enum constant (this has happened twice already from
// stale data) — this converter treats anything unrecognized as null instead of blowing up reads.
@Slf4j
@Converter
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return Gender.valueOf(dbData.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("Unrecognized Gender value '{}' in database — treating as null", dbData);
            return null;
        }
    }
}
