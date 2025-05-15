package com.github.streamshub.systemtests.enums;

public enum ResourceStatus {
    READY("Ready"),
    NOT_READY("NotReady"),
    WARNING("Warning"),
    RECONCILIATION_PAUSED("ReconciliationPaused");

    private final String status;

    ResourceStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return status;
    }
}