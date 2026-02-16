package com.github.streamshub.systemtests.enums;

public enum ResetOffsetType {

    EARLIEST("Earliest", "--to-earliest"),
    LATEST("Latest", "--to-latest"),
    DATE_TIME("DateTime", "--to-datetime"),
    // Is displayed only if resetting with specific partition
    CUSTOM_OFFSET("CustomOffset", "--to-offset"),
    DELETE_COMMITED_OFFSETS("DeleteCommitedOffsets", "--delete-offsets");

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
