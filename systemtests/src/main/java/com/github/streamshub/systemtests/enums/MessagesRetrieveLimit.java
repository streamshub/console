package com.github.streamshub.systemtests.enums;

public enum MessagesRetrieveLimit {
    FIVE(5),
    TEN(10),
    TWENTY_FIVE(25),
    FIFTY(50),
    SEVENTY_FIVE(75),
    ONE_HUNDRED(100);

    private final int value;

    MessagesRetrieveLimit(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public String getLabel() {
        return String.valueOf(value);
    }

    @Override
    public String toString() {
        return getLabel();
    }
}
