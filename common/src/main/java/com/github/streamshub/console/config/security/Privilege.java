package com.github.streamshub.console.config.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Privilege {

    CREATE,
    DELETE,
    GET,
    LIST,
    UPDATE,
    ALL {
        @Override
        public Set<Privilege> expand() {
            return ALL_EXPANDED;
        }
    };

    private static final Set<Privilege> ALL_EXPANDED = Arrays.stream(Privilege.values())
            .filter(Predicate.not(ALL::equals))
            .collect(Collectors.toSet());

    @JsonCreator
    public static Privilege forValue(String value) {
        if ("*".equals(value)) {
            return ALL;
        }
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    public Set<Privilege> expand() {
        return Collections.singleton(this);
    }
}
