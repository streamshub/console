package com.github.streamshub.systemtests.enums;

public enum FilterType {
    NAME("Name", 1),
    TOPIC_ID("Topic ID", 2),
    STATUS("Status", 3);

    private final String name;
    private final int position;

    FilterType(String name, int position) {
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
