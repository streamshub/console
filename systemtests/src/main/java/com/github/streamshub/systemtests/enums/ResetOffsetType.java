package com.github.streamshub.systemtests.enums;

public enum ResetOffsetType {

    EARLIEST("Earliest", "earliest"),
    LATEST("Latest", "latest"),
    DATE_TIME("DateTime", "datetime"),
    // Is displayed only if resetting with specific partition
    CUSTOM_OFFSET("CustomOffset", "offset");

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
