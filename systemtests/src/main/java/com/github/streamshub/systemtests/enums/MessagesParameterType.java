package com.github.streamshub.systemtests.enums;

public enum MessagesParameterType {
    FROM_OFFSET("From offset"),
    FROM_TIMESTAMP("From timestamp"),
    FROM_UNIX_TIMESTAMP("From Unix timestamp"),
    LATEST("Latest messages");

    private final String label;

    MessagesParameterType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
