package com.github.streamshub.systemtests.enums;

public enum ResetOffsetDateTimeType {
    UNIX_EPOCH("Unix Epoch Milliseconds"),
    ISO_8601("ISO 8601 Date-Time Format");

    private final String type;

    ResetOffsetDateTimeType(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return type;
    }
}
