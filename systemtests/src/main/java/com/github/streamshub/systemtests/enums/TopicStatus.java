package com.github.streamshub.systemtests.enums;

public enum TopicStatus {
    FULLY_REPLICATED("Fully replicated", 1),
    UNDER_REPLICATED("Under replicated", 2),
    PARTIALLY_OFFLINE("Partially offline", 3),
    UNKNOWN("Unknown", 4),
    OFFLINE("Offline", 5);

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
