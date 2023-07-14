package org.bf2.admin.kafka.admin;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.bf2.admin.kafka.admin.handlers.AdminClientFactory;
import org.bf2.admin.kafka.admin.model.Types;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@RequestScoped
public class RecordOperations {

    private static final Logger log = Logger.getLogger(RecordOperations.class);
    public static final String BINARY_DATA_MESSAGE = "Binary or non-UTF-8 encoded data cannot be displayed";
    static final int REPLACEMENT_CHARACTER = '\uFFFD';

    @Inject
    AdminClientFactory clientFactory;

    public Types.PagedResponse<Types.Record> consumeRecords(String topicName,
                                              Integer partition,
                                              Integer offset,
                                              String timestamp,
                                              Integer limit,
                                              List<String> include,
                                              Integer maxValueLength) {

        try (Consumer<byte[], byte[]> consumer = clientFactory.createConsumer(null)) {
            List<PartitionInfo> partitions = consumer.partitionsFor(topicName);

            if (partitions.isEmpty()) {
                throw noSuchTopic(topicName);
            }

            List<TopicPartition> assignments = partitions.stream()
                .filter(p -> partition == null || partition.equals(p.partition()))
                .map(p -> new TopicPartition(p.topic(), p.partition()))
                .collect(Collectors.toList());

            if (assignments.isEmpty()) {
                throw noSuchTopicPartition(topicName, partition);
            }

            consumer.assign(assignments);

            if (timestamp != null) {
                Long tsMillis = stringToTimestamp(timestamp);
                Map<TopicPartition, Long> timestampsToSearch =
                        assignments.stream().collect(Collectors.toMap(Function.identity(), p -> tsMillis));
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
            } else {
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
            Instant timeout = Instant.now().plusSeconds(2);
            int maxRecords = assignments.size() * limit;
            List<Types.Record> results = new ArrayList<>();
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
                    if (log.isTraceEnabled()) {
                        log.tracef("next() consumed records: %d; total %s", pollSize, recordsConsumed.get());
                    }
                    return records;
                }
            };

            Comparator<ConsumerRecord<byte[], byte[]>> comparator = Comparator.comparingLong(ConsumerRecord::timestamp);
            if (timestamp == null && offset == null) {
                comparator = comparator.reversed();
            }
            comparator = comparator
                    .thenComparingInt(ConsumerRecord::partition)
                    .thenComparingLong(ConsumerRecord::offset);

            NavigableSet<ConsumerRecord<byte[], byte[]>> limitSet = new TreeSet<>(comparator) {
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
                    .map(rec -> getItems(rec, topicName, include, maxValueLength))
                    .forEach(results::add);

            if (log.isDebugEnabled()) {
                log.debugf("Total consumed records: %d", recordsConsumed.get());
            }

            return Types.PagedResponse.forItems(Types.Record.class, results);

        }
    }

    public Types.Record getItems(ConsumerRecord<byte[], byte[]> rec, String topicName, List<String> include, Integer maxValueLength) {
        Types.Record item = new Types.Record(topicName);

        setProperty(Types.Record.PROP_PARTITION, include, rec::partition, item::setPartition);
        setProperty(Types.Record.PROP_OFFSET, include, rec::offset, item::setOffset);
        setProperty(Types.Record.PROP_TIMESTAMP, include, () -> timestampToString(rec.timestamp()), item::setTimestamp);
        setProperty(Types.Record.PROP_TIMESTAMP_TYPE, include, rec.timestampType()::name, item::setTimestampType);
        setProperty(Types.Record.PROP_KEY, include, rec::key, k -> item.setKey(bytesToString(k, maxValueLength)));
        setProperty(Types.Record.PROP_VALUE, include, rec::value, v -> item.setValue(bytesToString(v, maxValueLength)));
        setProperty(Types.Record.PROP_HEADERS, include, () -> headersToMap(rec.headers(), maxValueLength), item::setHeaders);
        item.updateHref();

        return item;
    }

    public CompletionStage<Types.Record> produceRecord(String topicName, Types.Record input) {
        CompletableFuture<Types.Record> promise = new CompletableFuture<>();
        Producer<String, String> producer = clientFactory.createProducer();

        try {
            List<PartitionInfo> partitions = producer.partitionsFor(topicName);

            if (partitions.isEmpty()) {
                promise.completeExceptionally(noSuchTopic(topicName));
            } else if (input.getPartition() != null && partitions.stream().noneMatch(p -> input.getPartition().equals(p.partition()))) {
                promise.completeExceptionally(noSuchTopicPartition(topicName, input.getPartition()));
            } else {
                send(topicName, input, producer, promise);
            }
        } catch (TimeoutException e) {
            promise.completeExceptionally(noSuchTopic(topicName));
        } catch (Exception e) {
            promise.completeExceptionally(e);
        }

        return promise.whenComplete((result, exception) -> {
            try {
                producer.close(Duration.ZERO);
            } catch (Exception e) {
                log.warnf("Exception closing Kafka Producer", e);
            }
        });
    }

    void send(String topicName, Types.Record input, Producer<String, String> producer, CompletableFuture<Types.Record> promise) {
        String key = input.getKey();
        List<Header> headers = input.getHeaders() != null ? input.getHeaders()
            .entrySet()
            .stream()
            .map(h -> new Header() {
                @Override
                public String key() {
                    return h.getKey();
                }

                @Override
                public byte[] value() {
                    return h.getValue() != null ? h.getValue().getBytes() : null;
                }
            })
            .collect(Collectors.toList()) : Collections.emptyList();

        ProducerRecord<String, String> request = new ProducerRecord<>(topicName, input.getPartition(), stringToTimestamp(input.getTimestamp()), key, input.getValue(), headers);

        producer.send(request, (meta, exception) -> {
            if (exception != null) {
                promise.completeExceptionally(exception);
            } else {
                Types.Record result = new Types.Record(topicName);
                result.setPartition(meta.partition());
                if (meta.hasOffset()) {
                    result.setOffset(meta.offset());
                }
                if (meta.hasTimestamp()) {
                    result.setTimestamp(timestampToString(meta.timestamp()));
                }
                result.setKey(input.getKey());
                result.setValue(input.getValue());
                result.setHeaders(input.getHeaders());
                promise.complete(result);
            }
        });
    }

    <T> void setProperty(String fieldName, List<String> include, Supplier<T> source, java.util.function.Consumer<T> target) {
        if (include.isEmpty() || include.contains(fieldName)) {
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

    String timestampToString(long timestamp) {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneOffset.UTC).toString();
    }

    Long stringToTimestamp(String value) {
        if (value == null) {
            return null;
        }

        return ZonedDateTime.parse(value).toInstant().toEpochMilli();
    }

    static UnknownTopicOrPartitionException noSuchTopic(String topicName) {
        return new UnknownTopicOrPartitionException("No such topic: " + topicName);
    }

    static InvalidPartitionsException noSuchTopicPartition(String topicName, int partition) {
        return new InvalidPartitionsException(String.format("No such partition for topic %s: %d", topicName, partition));
    }
}
