package com.github.streamshub.systemtests.enums;

public enum ResetOffsetType {

    EARLIEST("Earliest offset", "earliest"),
    LATEST("Latest offset", "latest"),
    DATE_TIME("Specific Date time", "datetime"),
    // Is displayed only if resetting with specific partition
    CUSTOM_OFFSET("Custom offset", "offset");

    private final String description;
    private final String command;

    ResetOffsetType(String description, String command) {
        this.description = description;
        this.command = command;
    }

    @Override
    public String toString() {
        return description;
    }

    public String getCommand() {
        return command;
    }
}
