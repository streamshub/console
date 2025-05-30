package com.github.streamshub.systemtests.enums;

public enum TopicStatus {
    FULLY_REPLICATED("Fully replicated", 0),
    UNDER_REPLICATED("Under replicated", 1),
    PARTIALLY_OFFLINE("Partially offline", 2),
    UNKNOWN("Unknown", 3),
    OFFLINE("Offline", 4);

    private final String name;
    private final int position;

    TopicStatus(String name, int position) {
        this.name = name;
        this.position = position;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }
}
