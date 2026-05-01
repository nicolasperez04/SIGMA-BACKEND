package com.SIGMA.USCO.config;

import java.time.LocalDateTime;
import java.time.ZoneId;

public class DateUtilsConfig {

    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE);
    }

}
