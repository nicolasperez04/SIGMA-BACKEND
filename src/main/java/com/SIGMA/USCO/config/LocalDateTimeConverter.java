package com.SIGMA.USCO.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, OffsetDateTime> {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    @Override
    public OffsetDateTime convertToDatabaseColumn(LocalDateTime localDateTime) {
        if (localDateTime == null) return null;

        return localDateTime.atZone(ZONE).toOffsetDateTime();
    }

    @Override
    public LocalDateTime convertToEntityAttribute(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) return null;


        return offsetDateTime
                .atZoneSameInstant(ZONE)
                .toLocalDateTime();
    }
}
