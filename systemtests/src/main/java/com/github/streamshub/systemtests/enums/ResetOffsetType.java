package com.github.streamshub.systemtests.enums;

public enum ResetOffsetType {

    EARLIEST("Earliest", "--to-earliest"),
    LATEST("Latest", "--to-latest"),
    DATE_TIME_ISO("DateTime ISO", "--to-datetime"),
    DATE_TIME_UNIX("DateTime UNIX", "--to-datetime"),
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
