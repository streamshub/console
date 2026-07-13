package com.github.streamshub.systemtests.enums;

public enum MessagesWhereFilter {
    ANYWHERE("Anywhere"),
    KEY("Key"),
    HEADERS("Headers"),
    VALUE("Value");

    private final String label;

    MessagesWhereFilter(String label) {
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
