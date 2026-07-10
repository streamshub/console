package com.github.streamshub.systemtests.enums;

public enum MessagesRetrieveType {
    NUMBER_OF_MESSAGES("Number of messages"),
    CONTINUOUSLY("Continuously");

    private final String label;

    MessagesRetrieveType(String label) {
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
