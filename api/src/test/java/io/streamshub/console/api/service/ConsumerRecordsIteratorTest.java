package io.streamshub.console.api.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.streamshub.console.api.service.RecordService.ConsumerRecordsIterator;
import io.streamshub.console.api.service.RecordService.ConsumerRecordsIterator.EndReason;
import io.streamshub.console.api.support.serdes.RecordData;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsumerRecordsIteratorTest {

    static final TopicPartition PARTITION = new TopicPartition("test-topic", 0);
    static final int RECORD_COUNT = 5;

    MockConsumer<RecordData, RecordData> consumer;

    @BeforeEach
    void setup() {
        consumer = new MockConsumer<>("earliest");
        consumer.assign(List.of(PARTITION));
        consumer.updateBeginningOffsets(Map.of(PARTITION, 0L));
        consumer.updateEndOffsets(Map.of(PARTITION, (long) RECORD_COUNT));
        consumer.seek(PARTITION, 0);

        for (int i = 0; i < RECORD_COUNT; i++) {
            consumer.addRecord(new ConsumerRecord<>(
                    PARTITION.topic(),
                    PARTITION.partition(),
                    i,
                    new RecordData("key-" + i),
                    new RecordData("value-" + i)));
        }
    }

    private ConsumerRecordsIterator iterator(
            Instant timeout,
            Predicate<ConsumerRecord<RecordData, RecordData>> predicate,
            AtomicBoolean cancelled) {

        return new ConsumerRecordsIterator(
                consumer,
                Map.of(PARTITION, (long) RECORD_COUNT),
                RECORD_COUNT,
                timeout,
                Integer.MAX_VALUE,
                List.of(predicate),
                cancelled::get);
    }

    private ConsumerRecordsIterator iterator(int maxScanned, Predicate<ConsumerRecord<RecordData, RecordData>> predicate) {
        return new ConsumerRecordsIterator(
                consumer,
                Map.of(PARTITION, (long) RECORD_COUNT),
                RECORD_COUNT,
                Instant.now().plusSeconds(60),
                maxScanned,
                List.of(predicate),
                () -> false);
    }

    @Test
    void testScanLimitStopsScanWithinABatch() {
        AtomicInteger tested = new AtomicInteger();

        var iterator = iterator(2, rec -> {
            tested.incrementAndGet();
            return false;
        });

        var matched = iterator.next();

        // The remaining records of the batch were neither tested nor counted
        assertEquals(2, tested.get());
        assertTrue(matched.isEmpty());
        assertEquals(2, iterator.totalConsumed());
        assertFalse(iterator.hasNext());
        assertEquals(EndReason.SCAN_LIMIT, iterator.endReason());
        assertEquals(2 * 100 / RECORD_COUNT, iterator.scanProgress());
    }

    @Test
    void testMatchesWithinScanLimitAreReturned() {
        var iterator = iterator(3, rec -> rec.offset() != 1);

        var matched = iterator.next();

        assertEquals(List.of(0L, 2L), matched.stream().map(ConsumerRecord::offset).toList());
        assertEquals(EndReason.SCAN_LIMIT, iterator.endReason());
    }

    @Test
    void testScanLimitEqualToRecordCountIsComplete() {
        var iterator = iterator(RECORD_COUNT, rec -> false);

        iterator.next();

        // All of the records were read, the limit was not what ended the scan
        assertEquals(RECORD_COUNT, iterator.totalConsumed());
        assertFalse(iterator.hasNext());
        assertEquals(EndReason.COMPLETE, iterator.endReason());
        assertEquals(100, iterator.scanProgress());
    }

    @Test
    void testScanLimitAboveRecordCountIsComplete() {
        var iterator = iterator(RECORD_COUNT + 1, rec -> false);

        iterator.next();

        assertEquals(EndReason.COMPLETE, iterator.endReason());
    }

    @Test
    void testMatchLimitMetBeforeScanLimitIsLimit() {
        var iterator = new ConsumerRecordsIterator(
                consumer,
                Map.of(PARTITION, (long) RECORD_COUNT),
                2,
                Instant.now().plusSeconds(60),
                3,
                List.of(rec -> true),
                () -> false);

        iterator.next();

        assertFalse(iterator.hasNext());
        assertEquals(EndReason.LIMIT, iterator.endReason());
    }

    @Test
    void testTimeoutIsCheckedBetweenRecordsOfABatch() {
        AtomicInteger tested = new AtomicInteger();
        Instant timeout = Instant.now().plusMillis(200);

        // Matching the first record takes until the time allowed for the iteration has run out
        var iterator = iterator(timeout, rec -> {
            tested.incrementAndGet();
            await().pollInterval(Duration.ofMillis(10)).until(() -> !Instant.now().isBefore(timeout));
            return true;
        }, new AtomicBoolean());

        var matched = iterator.next();

        // The remaining records of the batch were not tested
        assertEquals(1, tested.get());
        assertEquals(1, matched.size());
        assertEquals(1, iterator.totalConsumed());
        assertFalse(iterator.hasNext());
        assertEquals(EndReason.TIMEOUT, iterator.endReason());
        // Progress only covers the records that were scanned
        assertEquals(100 / RECORD_COUNT, iterator.scanProgress());
    }

    @Test
    void testCancellationIsCheckedBetweenRecordsOfABatch() {
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicInteger tested = new AtomicInteger();

        var iterator = iterator(Instant.now().plusSeconds(60), rec -> {
            tested.incrementAndGet();
            // The client goes away while the first record is being matched
            cancelled.set(true);
            return true;
        }, cancelled);

        var matched = iterator.next();

        assertEquals(1, tested.get());
        assertEquals(1, matched.size());
        assertFalse(iterator.hasNext());
        assertEquals(EndReason.CANCELLED, iterator.endReason());
    }

    @Test
    void testAllRecordsScannedWhenTimeNotExceeded() {
        var iterator = iterator(Instant.now().plusSeconds(60), rec -> false, new AtomicBoolean());

        var matched = iterator.next();

        assertTrue(matched.isEmpty());
        assertEquals(RECORD_COUNT, iterator.totalConsumed());
        assertFalse(iterator.hasNext());
        assertEquals(EndReason.COMPLETE, iterator.endReason());
        assertEquals(100, iterator.scanProgress());
    }

    @Test
    void testMatchLimitMetIsNotReportedAsTimeout() {
        var iterator = new ConsumerRecordsIterator(
                consumer,
                Map.of(PARTITION, (long) RECORD_COUNT),
                2,
                Instant.now().plusSeconds(60),
                Integer.MAX_VALUE,
                List.of(rec -> true),
                () -> false);

        iterator.next();

        assertFalse(iterator.hasNext());
        assertEquals(EndReason.LIMIT, iterator.endReason());
    }
}
