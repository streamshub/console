package com.github.streamshub.systemtests.constants;

import java.time.Duration;

public class TimeConstants {

    private static long minutes(int minutes) {
        return Duration.ofMinutes(minutes).toMillis();
    }

    private static long seconds(int seconds) {
        return Duration.ofSeconds(seconds).toMillis();
    }

    // Poll
    public static final long GLOBAL_POLL_INTERVAL_SHORT = seconds(3);
    public static final long POLL_INTERVAL_FOR_RESOURCE_READINESS = seconds(5);

    // Timeout
    public static final long COMPONENT_LOAD_TIMEOUT = seconds(40);
    public static final long GLOBAL_STATUS_TIMEOUT = minutes(3);
}
