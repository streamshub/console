package com.github.streamshub.systemtests.constants;

import io.skodjob.kubetest4j.KubeTestConstants;

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

    // Element visibility
    public static final long ELEMENT_VISIBILITY_TIMEOUT = secondsInMilis(30);
    public static final long ELEMENT_VISIBILITY_TIMEOUT_MEDIUM = minutesInMilis(1);

    // Page
    public static final long PAGE_LOAD_TIMEOUT = minutesInMilis(1);

    // Action
    public static final long ACTION_WAIT_LONG = minutesInMilis(1);
    public static final long ACTION_WAIT_MEDIUM = secondsInMilis(30);
    public static final long ACTION_WAIT_SHORT = secondsInMilis(10);

    // Reaction time
    public static final long COMPONENT_REACTION_TIME_SHORT = secondsInMilis(5);

    // Time values depending on variable
    public static long timeoutForClientFinishJob(int messagesCount) {
        return messagesCount * (KubeTestConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS + KubeTestConstants.GLOBAL_TIMEOUT_MEDIUM);
    }
}
