package com.github.streamshub.console.config.security;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Privilege {

    CREATE,
    DELETE,
    GET,
    LIST,
    UPDATE,
    ALL;

    @JsonCreator
    public static Privilege forValue(String value) {
        if ("*".equals(value)) {
            return ALL;
        }
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

}
