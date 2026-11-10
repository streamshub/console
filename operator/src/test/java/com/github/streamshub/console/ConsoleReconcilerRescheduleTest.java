package com.github.streamshub.console;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.status.Condition;
import com.github.streamshub.console.api.v1alpha1.status.ConditionBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleReconcilerRescheduleTest {

    @Test
    void testFastPollingWhenRecentlyNotReady() {
        assertEquals(Duration.ofMillis(500), ConsoleReconciler.rescheduleDelay(consoleNotReadySince(Instant.now())));
    }

    @Test
    void testFastPollingJustInsideInitialPeriod() {
        var since = Instant.now().minus(Duration.ofSeconds(50));
        assertEquals(Duration.ofMillis(500), ConsoleReconciler.rescheduleDelay(consoleNotReadySince(since)));
    }

    @Test
    void testBackoffWhenNotReadyForLongerThanInitialPeriod() {
        var since = Instant.now().minus(Duration.ofMinutes(2));
        assertEquals(Duration.ofSeconds(10), ConsoleReconciler.rescheduleDelay(consoleNotReadySince(since)));
    }

    @Test
    void testFastPollingWhenTransitionTimeUnparseable() {
        assertEquals(Duration.ofMillis(500), ConsoleReconciler.rescheduleDelay(consoleWithTransitionTime("not-a-time")));
    }

    private static Console consoleNotReadySince(Instant since) {
        return consoleWithTransitionTime(since.toString());
    }

    private static Console consoleWithTransitionTime(String transitionTime) {
        var console = new Console();
        console.getOrCreateStatus().updateCondition(new ConditionBuilder()
                .withType(Condition.Types.READY)
                .withStatus("False")
                .withLastTransitionTime(transitionTime)
                .build());
        return console;
    }
}
