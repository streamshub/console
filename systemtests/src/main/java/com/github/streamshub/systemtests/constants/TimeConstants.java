package com.github.streamshub.systemtests.constants;

import java.time.Duration;

public class TimeConstants {
    private TimeConstants() {}

    private static long minutesInMilis(int minutes) {
        return Duration.ofMinutes(minutes).toMillis();
    }

    private static long secondsInMilis(int seconds) {
        return Duration.ofSeconds(seconds).toMillis();
    }

    // Poll
    public static final long GLOBAL_POLL_INTERVAL_SHORT = secondsInMilis(3);
    public static final long POLL_INTERVAL_FOR_RESOURCE_READINESS = secondsInMilis(5);
    public static final long ROLLING_UPDATE_POLL_INTERVAL = Duration.ofSeconds(5).toMillis();

    // Timeout
    public static final long COMPONENT_LOAD_TIMEOUT = secondsInMilis(40);
    public static final long GLOBAL_STATUS_TIMEOUT = minutesInMilis(3);
    // HTML elements
    public static final long ELEMENT_VISIBILITY_TIMEOUT = minutesInMilis(1);
}
