package com.github.streamshub.systemtests.constants;

import io.skodjob.testframe.TestFrameConstants;

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
    public static final long ROLLING_UPDATE_POLL_INTERVAL = secondsInMilis(5);
    // Timeout
    public static final long COMPONENT_LOAD_TIMEOUT = secondsInMilis(40);
    public static final long GLOBAL_STATUS_TIMEOUT = minutesInMilis(3);
    public static final int GLOBAL_STABILITY_OFFSET_TIME = 20;
    // HTML elements
    public static final long ELEMENT_VISIBILITY_TIMEOUT = minutesInMilis(1);
    public static final long UI_COMPONENT_REACTION_INTERVAL_SHORT = secondsInMilis(5);

    // Time values depending on variable
    public static long timeoutForClientFinishJob(int messagesCount) {
        return messagesCount * (TestFrameConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS + TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM);
    }
}
