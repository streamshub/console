package com.github.eyefloaters.console.api.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.UnknownTopicIdException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.KafkaRecord;
import com.github.eyefloaters.console.api.support.SizeLimitedSortedSet;

import static java.util.Objects.requireNonNullElse;

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
    Supplier<Producer<String, String>> producerSupplier;

    @Inject
    ThreadContext threadContext;

    public List<KafkaRecord> consumeRecords(String topicId,
            Integer partition,
            Long offset,
            Instant timestamp,
            Integer limit,
            List<String> include,
            Integer maxValueLength) {

        List<PartitionInfo> partitions = topicNameForId(topicId)
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
        var endOffsets = consumer.endOffsets(assignments);

        if (timestamp != null) {
            seekToTimestamp(consumer, assignments, timestamp);
        } else {
            seekToOffset(consumer, assignments, endOffsets, offset, limit);
        }

        Iterable<ConsumerRecords<byte[], byte[]>> poll = () -> new ConsumerRecordsIterator<>(consumer, endOffsets, limit);
        var limitSet = new SizeLimitedSortedSet<ConsumerRecord<byte[], byte[]>>(buildComparator(timestamp, offset), limit);

        return StreamSupport.stream(poll.spliterator(), false)
                .flatMap(records -> StreamSupport.stream(records.spliterator(), false))
                .collect(Collectors.toCollection(() -> limitSet))
                .stream()
                .map(rec -> getItems(rec, topicId, include, maxValueLength))
                .toList();
    }

    public CompletionStage<KafkaRecord> produceRecord(String topicId, KafkaRecord input) {
        CompletableFuture<KafkaRecord> promise = new CompletableFuture<>();
        Executor asyncExec = threadContext.currentContextExecutor();

        topicNameForId(topicId)
            .thenApplyAsync(
                    topicName -> producerSupplier.get().partitionsFor(topicName),
                    asyncExec)
            .thenAcceptAsync(
                    partitions -> {
                        Producer<String, String> producer = producerSupplier.get();
                        String topicName = partitions.iterator().next().topic();
                        Integer partition = input.getPartition();

                        if (partition != null && partitions.stream().noneMatch(p -> partition.equals(p.partition()))) {
                            promise.completeExceptionally(invalidPartition(topicId, partition));
                        } else {
                            send(topicName, input, producer, promise);
                        }
                    },
                    asyncExec);

        return promise;
    }

    void send(String topicName, KafkaRecord input, Producer<String, String> producer, CompletableFuture<KafkaRecord> promise) {
        String key = input.getKey();

        List<Header> headers = Optional.ofNullable(input.getHeaders())
            .orElseGet(Collections::emptyMap)
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
            .map(Header.class::cast)
            .toList();

        Long timestamp = Optional.ofNullable(input.getTimestamp()).map(Instant::toEpochMilli).orElse(null);

        ProducerRecord<String, String> request = new ProducerRecord<>(topicName,
                input.getPartition(),
                timestamp,
                key,
                input.getValue(),
                headers);

        producer.send(request, (meta, exception) -> {
            if (exception != null) {
                promise.completeExceptionally(exception);
            } else {
                KafkaRecord result = new KafkaRecord();
                result.setPartition(meta.partition());
                if (meta.hasOffset()) {
                    result.setOffset(meta.offset());
                }
                if (meta.hasTimestamp()) {
                    result.setTimestamp(Instant.ofEpochMilli(meta.timestamp()));
                }
                result.setKey(input.getKey());
                result.setValue(input.getValue());
                result.setHeaders(input.getHeaders());
                promise.complete(result);
            }
        });
    }

    CompletionStage<String> topicNameForId(String topicId) {
        Uuid kafkaTopicId = Uuid.fromString(topicId);

        return clientSupplier.get()
            .listTopics(new ListTopicsOptions().listInternal(true))
            .listings()
            .toCompletionStage()
            .thenApply(Collection::stream)
            .thenApply(listings -> listings
                    .filter(topic -> kafkaTopicId.equals(topic.topicId()))
                    .findFirst()
                    .map(TopicListing::name)
                    .orElseThrow(() -> noSuchTopic(topicId)));
    }

    void seekToTimestamp(Consumer<byte[], byte[]> consumer, List<TopicPartition> assignments, Instant timestamp) {
        Long tsMillis = timestamp.toEpochMilli();
        Map<TopicPartition, Long> timestampsToSearch = assignments.stream()
                .collect(Collectors.toMap(Function.identity(), p -> tsMillis));

        consumer.offsetsForTimes(timestampsToSearch)
            .forEach((p, tsOffset) -> {
                if (tsOffset != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debugf("Seeking to { offset=%d, timestamp=%d } in topic %s/partition %d for search timestamp %d",
                                tsOffset.offset(), tsOffset.timestamp(), p.topic(), p.partition(), tsMillis);
                    }
                    consumer.seek(p, tsOffset.offset());
                } else {
                    /*
                     * No offset for the time-stamp (future date?), seek to
                     * end and return nothing for this partition.
                     */
                    if (logger.isDebugEnabled()) {
                        logger.debugf("No offset found for search timestamp %d, seeking to end of topic %s/partition %d",
                                (Object) tsMillis, p.topic(), p.partition());
                    }
                    consumer.seekToEnd(List.of(p));
                }
            });
    }

    void seekToOffset(Consumer<byte[], byte[]> consumer, List<TopicPartition> assignments, Map<TopicPartition, Long> endOffsets, Long offset, int limit) {
        var beginningOffsets = consumer.beginningOffsets(assignments);

        assignments.forEach(p -> {
            long partitionBegin = beginningOffsets.get(p);
            long partitionEnd = endOffsets.get(p);
            long seekTarget;

            if (offset == null) {
                // Fetch the latest records, no earlier than the beginning of the partition
                seekTarget = Math.max(partitionBegin, partitionEnd - limit);
            } else if (offset <= partitionEnd) {
                // Seek to the requested offset, no earlier than the beginning of the partition
                seekTarget = Math.max(partitionBegin, offset);
            } else {
                /*
                 * Requested offset is beyond the end of the partition,
                 * seek to end and return nothing for this partition.
                 */
                seekTarget = partitionEnd;
            }

            consumer.seek(p, seekTarget);
        });
    }

    Comparator<ConsumerRecord<byte[], byte[]>> buildComparator(Instant timestamp, Long offset) {
        Comparator<ConsumerRecord<byte[], byte[]>> comparator = Comparator
                .<ConsumerRecord<byte[], byte[]>>comparingLong(ConsumerRecord::timestamp)
                .thenComparingInt(ConsumerRecord::partition)
                .thenComparingLong(ConsumerRecord::offset);

        if (timestamp == null && offset == null) {
            // Returning "latest" records, newest to oldest within the result set size limit
            comparator = comparator.reversed();
        }

        return comparator;
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
        setProperty(KafkaRecord.Fields.SIZE, include, () -> sizeOf(rec), item::setSize);

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

    long sizeOf(ConsumerRecord<?, ?> rec) {
        return rec.serializedKeySize() +
            rec.serializedValueSize() +
            Arrays.stream(rec.headers().toArray())
                .mapToLong(h -> h.key().length() + h.value().length)
                .sum();
    }

    static UnknownTopicIdException noSuchTopic(String topicId) {
        return new UnknownTopicIdException("No such topic: " + topicId);
    }

    static InvalidPartitionsException invalidPartition(String topicId, int partition) {
        return new InvalidPartitionsException("Partition " + partition + " is not valid for topic " + topicId);
    }

    static class ConsumerRecordsIterator<K, V> implements Iterator<ConsumerRecords<K, V>> {
        private static final Logger LOGGER = Logger.getLogger(ConsumerRecordsIterator.class);

        private Instant timeout = Instant.now().plusSeconds(2);
        private int recordsConsumed = 0;
        private Map<TopicPartition, Integer> partitionConsumed = new HashMap<>();
        private final Consumer<K, V> consumer;
        private final Set<TopicPartition> assignments;
        private final Map<TopicPartition, Long> endOffsets;
        private final int limit;

        public ConsumerRecordsIterator(Consumer<K, V> consumer, Map<TopicPartition, Long> endOffsets, int limit) {
            this.consumer = consumer;
            this.assignments = new HashSet<>(consumer.assignment());
            this.endOffsets = endOffsets;
            this.limit = limit;
        }

        @Override
        public boolean hasNext() {
            boolean moreRecords = !assignments.isEmpty() && Instant.now().isBefore(timeout);

            if (!moreRecords && LOGGER.isDebugEnabled()) {
                LOGGER.debugf("Total consumed records: %d", recordsConsumed);
            }

            return moreRecords;
        }

        @Override
        public ConsumerRecords<K, V> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            var pollTimeout = Duration.between(Instant.now(), timeout);
            var records = consumer.poll(pollTimeout.isNegative() ? Duration.ZERO : pollTimeout);
            int pollSize = 0;

            for (var partition : records.partitions()) {
                var partitionRecords = records.records(partition);
                int consumed = partitionRecords.size();
                pollSize += consumed;
                int total = partitionConsumed.compute(partition, (k, v) -> requireNonNullElse(v, 0) + consumed);
                long maxOffset = partitionRecords.stream().mapToLong(ConsumerRecord::offset).max().orElse(-1);

                if (total >= limit || maxOffset >= endOffsets.get(partition)) {
                    // Consumed `limit` records for this partition or reached the end of the partition
                    assignments.remove(partition);
                }
            }

            if (pollSize == 0) {
                // End of stream, unsubscribe everything
                assignments.clear();
            }

            consumer.assign(assignments);
            recordsConsumed += pollSize;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.tracef("next() consumed records: %d; total %s", pollSize, recordsConsumed);
            }

            return records;
        }
    }
}
