package com.github.streamshub.console.config.security;

import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum Decision {

    ALLOWED {
        @Override
        public boolean logResult(boolean allowed) {
            return allowed;
        }
    },

    DENIED {
        @Override
        public boolean logResult(boolean allowed) {
            return !allowed;
        }
    },

    ALL {
        @Override
        public boolean logResult(boolean allowed) {
            return true;
        }
    };

    public abstract boolean logResult(boolean allowed);

    @JsonCreator
    public static Decision forValue(String value) {
        if ("*".equals(value)) {
            return ALL;
        }
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
