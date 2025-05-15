package com.github.streamshub.systemtests.enums;

public enum ConditionStatus {
    TRUE("True"),
    FALSE("False");

    private final String status;

    ConditionStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }
}