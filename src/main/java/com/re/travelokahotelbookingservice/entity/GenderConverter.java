package com.re.travelokahotelbookingservice.entity;

import com.re.travelokahotelbookingservice.entity.enums.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name();
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        return Gender.fromValue(dbData);
    }
}
