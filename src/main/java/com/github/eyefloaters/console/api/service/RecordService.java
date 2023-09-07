package com.github.eyefloaters.console.api.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.errors.UnknownTopicIdException;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.KafkaRecord;

@ApplicationScoped
public class RecordService {

    public static final String BINARY_DATA_MESSAGE = "Binary or non-UTF-8 encoded data cannot be displayed";
    static final int REPLACEMENT_CHARACTER = '\uFFFD';

    @Inject
    Logger logger;

    @Inject
    Supplier<Admin> clientSupplier;

    @Inject
    Supplier<Consumer<byte[], byte[]>> consumerSupplier;

    @Inject
    ThreadContext threadContext;

    public List<KafkaRecord> consumeRecords(String topicId,
            Integer partition,
            Long offset,
            Instant timestamp,
            Integer limit,
            List<String> include,
            Integer maxValueLength) {

        Uuid kafkaTopicId = Uuid.fromString(topicId);

        List<PartitionInfo> partitions = clientSupplier.get()
            .listTopics()
            .listings()
            .toCompletionStage()
            .thenApply(Collection::stream)
            .thenApply(listings -> listings
                    .filter(topic -> kafkaTopicId.equals(topic.topicId()))
                    .findFirst()
                    .map(TopicListing::name)
                    .orElseThrow(() -> noSuchTopic(topicId)))
            .thenApplyAsync(
                    topicName -> consumerSupplier.get().partitionsFor(topicName),
                    threadContext.currentContextExecutor())
            .toCompletableFuture()
            .join();

        List<TopicPartition> assignments = partitions.stream()
            .filter(p -> partition == null || partition.equals(p.partition()))
            .map(p -> new TopicPartition(p.topic(), p.partition()))
            .toList();

        if (assignments.isEmpty()) {
            return Collections.emptyList();
        }

        Consumer<byte[], byte[]> consumer = consumerSupplier.get();
        consumer.assign(assignments);

        if (timestamp != null) {
            seekToTimestamp(consumer, assignments, timestamp);
        } else {
            seekToOffset(consumer, assignments, offset, limit);
        }

        Instant timeout = Instant.now().plusSeconds(2);
        int maxRecords = assignments.size() * limit;
        List<KafkaRecord> results = new ArrayList<>();
        AtomicInteger recordsConsumed = new AtomicInteger(0);

        Iterable<ConsumerRecords<byte[], byte[]>> poll = () -> new Iterator<>() {
            boolean emptyPoll = false;

            @Override
            public boolean hasNext() {
                return !emptyPoll && recordsConsumed.get() < maxRecords && Instant.now().isBefore(timeout);
            }

            @Override
            public ConsumerRecords<byte[], byte[]> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                var records = consumer.poll(Duration.ofMillis(100));
                int pollSize = records.count();
                emptyPoll = pollSize == 0;
                recordsConsumed.addAndGet(pollSize);
                if (logger.isTraceEnabled()) {
                    logger.tracef("next() consumed records: %d; total %s", pollSize, recordsConsumed.get());
                }
                return records;
            }
        };

        NavigableSet<ConsumerRecord<byte[], byte[]>> limitSet = new TreeSet<>(buildComparator(timestamp, offset)) {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean add(ConsumerRecord<byte[], byte[]> rec) {
                boolean added = super.add(rec);
                if (size() > limit) {
                    pollLast();
                }
                return added;
            }
        };

        StreamSupport.stream(poll.spliterator(), false)
                .flatMap(records -> StreamSupport.stream(records.spliterator(), false))
                .collect(Collectors.toCollection(() -> limitSet))
                .stream()
                .map(rec -> getItems(rec, topicId, include, maxValueLength))
                .forEach(results::add);

        if (logger.isDebugEnabled()) {
            logger.debugf("Total consumed records: %d", recordsConsumed.get());
        }

        return results;
    }

    void seekToTimestamp(Consumer<byte[], byte[]> consumer, List<TopicPartition> assignments, Instant timestamp) {
        Long tsMillis = timestamp.toEpochMilli();
        Map<TopicPartition, Long> timestampsToSearch = assignments.stream()
                .collect(Collectors.toMap(Function.identity(), p -> tsMillis));

        consumer.offsetsForTimes(timestampsToSearch)
            .forEach((p, tsOffset) -> {
                if (tsOffset != null) {
                    consumer.seek(p, tsOffset.offset());
                } else {
                    /*
                     * No offset for the time-stamp (future date?), seek to
                     * end and return nothing for this partition.
                     */
                    consumer.seekToEnd(List.of(p));
                }
            });
    }

    void seekToOffset(Consumer<byte[], byte[]> consumer, List<TopicPartition> assignments, Long offset, int limit) {
        Map<TopicPartition, Long> endOffsets = consumer.endOffsets(assignments);

        assignments.forEach(p -> {
            long partitionEnd = endOffsets.get(p);

            if (offset == null) {
                // Fetch the latest records
                consumer.seek(p, Math.max(partitionEnd - limit, 0));
            } else if (offset <= partitionEnd) {
                consumer.seek(p, offset);
            } else {
                /*
                 * Requested offset is beyond the end of the partition,
                 * seek to end and return nothing for this partition.
                 */
                consumer.seek(p, endOffsets.get(p));
            }
        });
    }

    Comparator<ConsumerRecord<byte[], byte[]>> buildComparator(Instant timestamp, Long offset) {
        Comparator<ConsumerRecord<byte[], byte[]>> comparator = Comparator.comparingLong(ConsumerRecord::timestamp);

        if (timestamp == null && offset == null) {
            comparator = comparator.reversed();
        }

        return comparator
                .thenComparingInt(ConsumerRecord::partition)
                .thenComparingLong(ConsumerRecord::offset);
    }

    KafkaRecord getItems(ConsumerRecord<byte[], byte[]> rec, String topicId, List<String> include, Integer maxValueLength) {
        KafkaRecord item = new KafkaRecord(topicId);

        setProperty(KafkaRecord.Fields.PARTITION, include, rec::partition, item::setPartition);
        setProperty(KafkaRecord.Fields.OFFSET, include, rec::offset, item::setOffset);
        setProperty(KafkaRecord.Fields.TIMESTAMP, include, () -> Instant.ofEpochMilli(rec.timestamp()), item::setTimestamp);
        setProperty(KafkaRecord.Fields.TIMESTAMP_TYPE, include, rec.timestampType()::name, item::setTimestampType);
        setProperty(KafkaRecord.Fields.KEY, include, rec::key, k -> item.setKey(bytesToString(k, maxValueLength)));
        setProperty(KafkaRecord.Fields.VALUE, include, rec::value, v -> item.setValue(bytesToString(v, maxValueLength)));
        setProperty(KafkaRecord.Fields.HEADERS, include, () -> headersToMap(rec.headers(), maxValueLength), item::setHeaders);

        return item;
    }

    <T> void setProperty(String fieldName, List<String> include, Supplier<T> source, java.util.function.Consumer<T> target) {
        if (include.contains(fieldName)) {
            target.accept(source.get());
        }
    }

    String bytesToString(byte[] bytes, Integer maxValueLength) {
        if (bytes == null) {
            return null;
        }

        if (bytes.length == 0) {
            return "";
        }

        int bufferSize = maxValueLength != null ? Math.min(maxValueLength, bytes.length) : bytes.length;
        StringBuilder buffer = new StringBuilder(bufferSize);

        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            int input;

            while ((input = reader.read()) > -1) {
                if (input == REPLACEMENT_CHARACTER || !Character.isDefined(input)) {
                    return BINARY_DATA_MESSAGE;
                }

                buffer.append((char) input);

                if (maxValueLength != null && buffer.length() == maxValueLength) {
                    break;
                }
            }

            return buffer.toString();
        } catch (IOException e) {
            return BINARY_DATA_MESSAGE;
        }
    }

    Map<String, String> headersToMap(Headers headers, Integer maxValueLength) {
        Map<String, String> headerMap = new LinkedHashMap<>();
        headers.iterator().forEachRemaining(h -> headerMap.put(h.key(), bytesToString(h.value(), maxValueLength)));
        return headerMap;
    }

    static UnknownTopicIdException noSuchTopic(String topicId) {
        return new UnknownTopicIdException("No such topic: " + topicId);
    }

}
