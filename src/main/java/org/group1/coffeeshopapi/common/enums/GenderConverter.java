package org.group1.coffeeshopapi.common.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

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
