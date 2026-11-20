package io.streamshub.console.api.service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.Test;

import io.streamshub.console.api.errors.client.TooManyRequestsException;
import io.streamshub.console.api.service.RecordService.StreamSlot;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecordServiceStreamLimitTest {

    final Map<String, Integer> streams = new ConcurrentHashMap<>();

    @Test
    void testNoSlotTakenWhenUnlimited() {
        for (int i = 0; i < 100; i++) {
            RecordService.acquireStream(streams, "user1", Optional.empty(), "this user");
        }

        assertTrue(streams.isEmpty());
    }

    @Test
    void testSlotsLimitedForEachKey() {
        var first = RecordService.acquireStream(streams, "user1", Optional.of(2), "this user");
        var second = RecordService.acquireStream(streams, "user1", Optional.of(2), "this user");

        var thrown = assertThrows(TooManyRequestsException.class,
                () -> RecordService.acquireStream(streams, "user1", Optional.of(2), "this user"));
        assertEquals("Too many record streams are running for this user, retry later (limit: 2)", thrown.getMessage());

        // The limit is for each key, other keys are not affected
        var other = assertDoesNotThrow(
                () -> RecordService.acquireStream(streams, "user2", Optional.of(2), "this user"));

        // The rejected attempt did not hold on to a slot
        assertEquals(2, streams.get("user1"));

        first.close();
        second.close();
        other.close();
    }

    @Test
    void testClosingSlotAllowsAnotherAndRemovesIdleKey() {
        var slot = RecordService.acquireStream(streams, "cluster1", Optional.of(1), "this Kafka cluster");

        assertThrows(TooManyRequestsException.class,
                () -> RecordService.acquireStream(streams, "cluster1", Optional.of(1), "this Kafka cluster"));

        slot.close();
        assertTrue(streams.isEmpty());

        StreamSlot again = RecordService.acquireStream(streams, "cluster1", Optional.of(1), "this Kafka cluster");
        assertEquals(1, streams.get("cluster1"));
        again.close();
        assertTrue(streams.isEmpty());
    }

    @Test
    void testZeroLimitRejectsEveryStream() {
        assertThrows(TooManyRequestsException.class,
                () -> RecordService.acquireStream(streams, "user1", Optional.of(0), "this user"));
        assertTrue(streams.isEmpty());
    }
}
