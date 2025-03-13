package com.github.streamshub.systemtests.constants;

import java.time.Duration;

public interface TimeConstants {
    static long minutes(int minutes) {
        return Duration.ofMinutes(minutes).toMillis();
    }

    static long seconds(int seconds) {
        return Duration.ofSeconds(seconds).toMillis();
    }

    long GLOBAL_TIMEOUT = minutes(5);
    long GLOBAL_POLL_INTERVAL = seconds(1);
    long GLOBAL_POLL_INTERVAL_SHORT = seconds(3);
    long GLOBAL_POLL_INTERVAL_MEDIUM_SHORT = seconds(5);
    long GLOBAL_POLL_INTERVAL_MEDIUM = seconds(10);
    long POLL_INTERVAL_FOR_RESOURCE_READINESS = seconds(5);
    long POLL_INTERVAL_FOR_RESOURCE_DELETION = seconds(1);
    long READINESS_TIMEOUT = minutes(6);
    long DELETION_TIMEOUT = minutes(5);
    long GLOBAL_RELOAD_TIMEOUT = minutes(3);
    long GLOBAL_CLIENTS_TIMEOUT = minutes(2);
    long GLOBAL_STATUS_TIMEOUT = minutes(3);
    long THROTTLING_EXCEPTION_TIMEOUT = minutes(10);
    long CO_OPERATION_TIMEOUT_MEDIUM = minutes(2);
    long TIMEOUT_MEDIUM = minutes(3);
    long GLOBAL_TIMEOUT_LONG = minutes(10);

    // Playwright timeouts
    long PAGE_LOAD_TIMEOUT_LONG = minutes(2);
    long PAGE_LOGIN_TIME = seconds(10);
    long COMPONENT_RELOAD_COMPONENT_TIME_SMALL = seconds(5);
    long COMPONENT_LOAD_TIMEOUT = seconds(40);
    long COMPONENT_LOAD_TIMEOUT_MEDIUM = minutes(2);
    long COMPONENT_LOAD_TIMEOUT_EXTRA_LONG = minutes(5);
    long CONSOLE_LOAD_COMPONENT_TIMEOUT_LONG = minutes(3);
    long CONSOLE_RELOAD_COMPONENT_TIME = seconds(40);
}
