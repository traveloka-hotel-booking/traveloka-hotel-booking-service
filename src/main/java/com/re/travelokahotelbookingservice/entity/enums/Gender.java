package com.re.travelokahotelbookingservice.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Gender {
    MALE,
    FEMALE,
    OTHER;

    @JsonCreator
    public static Gender fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String normalized = value.trim();
        if ("NAM".equalsIgnoreCase(normalized) || "MALE".equalsIgnoreCase(normalized)) {
            return MALE;
        }
        if ("NỮ".equalsIgnoreCase(normalized) || "NU".equalsIgnoreCase(normalized) || "FEMALE".equalsIgnoreCase(normalized)) {
            return FEMALE;
        }
        if ("KHÁC".equalsIgnoreCase(normalized) || "KHAC".equalsIgnoreCase(normalized) || "OTHER".equalsIgnoreCase(normalized)) {
            return OTHER;
        }
        for (Gender g : Gender.values()) {
            if (g.name().equalsIgnoreCase(normalized)) {
                return g;
            }
        }
        return null;
    }

    @JsonValue
    public String toValue() {
        return name();
    }
}
