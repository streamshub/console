package com.github.streamshub.systemtests.enums;

public enum ResetOffsetDateTimeType {
    UNIX_EPOCH("Unix_Epoch_Milliseconds"),
    ISO_8601("ISO_8601_DateTime");

    private final String description;

    ResetOffsetDateTimeType(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }
}
