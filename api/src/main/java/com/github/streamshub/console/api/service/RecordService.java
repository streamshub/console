package com.github.streamshub.console.api.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
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
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.UnknownTopicIdException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.model.Identifier;
import com.github.streamshub.console.api.model.JsonApiRelationship;
import com.github.streamshub.console.api.model.KafkaRecord;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.SizeLimitedSortedSet;
import com.github.streamshub.console.api.support.serdes.RecordData;

import static java.util.Objects.requireNonNullElse;

@ApplicationScoped
public class RecordService {

    @Inject
    Logger logger;

    @Inject
    @ConfigProperty(name = "console.topics.records.poll-timeout", defaultValue = "PT5S")
    Duration pollTimeout;

    @Inject
    KafkaContext kafkaContext;

    @Inject
    Consumer<RecordData, RecordData> consumer;

    @Inject
    Producer<RecordData, RecordData> producer;

    @Inject
    ThreadContext threadContext;

    public CompletionStage<List<KafkaRecord>> consumeRecords(String topicId,
            Integer partition,
            Long offset,
            Instant timestamp,
            Integer limit,
            List<String> include,
            Integer maxValueLength) {

        return topicNameForId(topicId).thenApplyAsync(topicName -> {
            List<PartitionInfo> partitions = consumer.partitionsFor(topicName);
            List<TopicPartition> assignments = partitions.stream()
                    .filter(p -> partition == null || partition.equals(p.partition()))
                    .map(p -> new TopicPartition(p.topic(), p.partition()))
                    .collect(Collectors.toCollection(ArrayList::new));

            if (assignments.isEmpty()) {
                return Collections.emptyList();
            }

            var endOffsets = consumer.endOffsets(assignments);
            // End offset of zero means the partition has not been written to - don't bother reading them
            assignments.removeIf(assignment -> endOffsets.get(assignment) == 0);

            if (assignments.isEmpty()) {
                return Collections.emptyList();
            }

            consumer.assign(assignments);

            if (timestamp != null) {
                seekToTimestamp(consumer, assignments, timestamp);
            } else {
                seekToOffset(consumer, assignments, endOffsets, offset, limit);
            }

            if (assignments.isEmpty()) {
                return Collections.emptyList();
            }

            /*
             * Re-assign, seek operations may have removed assignments for requests beyond
             * the end of the partition.
             */
            consumer.assign(assignments);

            Iterable<ConsumerRecords<RecordData, RecordData>> poll =
                    () -> new ConsumerRecordsIterator<>(consumer, endOffsets, limit, Instant.now().plus(pollTimeout));
            var limitSet = new SizeLimitedSortedSet<ConsumerRecord<RecordData, RecordData>>(buildComparator(timestamp, offset), limit);

            return StreamSupport.stream(poll.spliterator(), false)
                    .flatMap(records -> StreamSupport.stream(records.spliterator(), false))
                    .collect(Collectors.toCollection(() -> limitSet))
                    .stream()
                    .map(rec -> getItems(rec, topicId, include, maxValueLength))
                    .toList();
        }, threadContext.currentContextExecutor());
    }

    public CompletionStage<KafkaRecord> produceRecord(String topicId, KafkaRecord input) {
        CompletableFuture<KafkaRecord> promise = new CompletableFuture<>();

        topicNameForId(topicId).thenAcceptAsync(topicName -> {
            List<PartitionInfo> partitions = producer.partitionsFor(topicName);
            Integer partition = input.partition();

            if (partition != null && partitions.stream().noneMatch(p -> partition.equals(p.partition()))) {
                promise.completeExceptionally(invalidPartition(topicId, partition));
            } else {
                send(topicName, input, producer, promise);
            }

            promise.whenComplete((kafkaRecord, error) -> producer.close());
        }, threadContext.currentContextExecutor()).exceptionally(e -> {
            promise.completeExceptionally(e);
            return null;
        });

        return promise;
    }

    void send(String topicName, KafkaRecord input, Producer<RecordData, RecordData> producer, CompletableFuture<KafkaRecord> promise) {
        List<Header> headers = Optional.ofNullable(input.headers())
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
            .collect(Collectors.toCollection(ArrayList::new));

        Long timestamp = Optional.ofNullable(input.timestamp()).map(Instant::toEpochMilli).orElse(null);
        var key = new RecordData(input.key());
        setSchemaMeta(input.keySchema(), key);

        var value = new RecordData(input.value());
        setSchemaMeta(input.valueSchema(), value);

        ProducerRecord<RecordData, RecordData> request = new ProducerRecord<>(topicName,
                input.partition(),
                timestamp,
                key,
                value,
                headers);

        CompletableFuture.completedFuture(null)
            .thenApplyAsync(nothing -> {
                try {
                    return producer.send(request).get();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new CompletionException("Error occurred while sending record to Kafka cluster", e);
                } catch (Exception e) {
                    throw new CompletionException("Error occurred while sending record to Kafka cluster", e);
                }
            }, threadContext.currentContextExecutor())
            .thenApply(meta -> {
                KafkaRecord result = new KafkaRecord();
                result.partition(meta.partition());

                if (meta.hasOffset()) {
                    result.offset(meta.offset());
                }

                if (meta.hasTimestamp()) {
                    result.timestamp(Instant.ofEpochMilli(meta.timestamp()));
                }

                result.key(key.dataString(null));
                result.value(value.dataString(null));
                result.headers(Arrays.stream(request.headers().toArray())
                        .collect(
                                // Duplicate headers will be overwritten
                                LinkedHashMap::new,
                                (map, hdr) -> map.put(hdr.key(), headerValue(hdr, null)),
                                HashMap::putAll));
                result.size(sizeOf(meta, request.headers()));

                schemaRelationship(key).ifPresent(result::keySchema);
                schemaRelationship(value).ifPresent(result::valueSchema);

                return result;
            })
            .thenAccept(promise::complete)
            .exceptionally(exception -> {
                promise.completeExceptionally(exception);
                return null;
            });
    }

    void setSchemaMeta(JsonApiRelationship schemaRelationship, RecordData data) {
        schemaMeta(schemaRelationship, "coordinates").ifPresent(gav -> data.meta.put("schema-gav", gav));
        schemaMeta(schemaRelationship, "messageType").ifPresent(type -> data.meta.put("message-type", type));
    }

    Optional<String> schemaMeta(JsonApiRelationship schemaRelationship, String key) {
        return Optional.ofNullable(schemaRelationship)
                .map(JsonApiRelationship::meta)
                .map(meta -> {
                    Object value = meta.get(key);
                    return (value instanceof String stringValue) ? stringValue : null;
                });
    }

    CompletionStage<String> topicNameForId(String topicId) {
        Uuid kafkaTopicId = Uuid.fromString(topicId);

        return kafkaContext.admin()
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

    void seekToTimestamp(Consumer<RecordData, RecordData> consumer, List<TopicPartition> assignments, Instant timestamp) {
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
                     * No offset for the time-stamp (future date?), remove the assignment
                     * and return nothing for this partition.
                     */
                    if (logger.isDebugEnabled()) {
                        logger.debugf("No offset found for search timestamp %d, removing assignment for topic %s/partition %d",
                                (Object) tsMillis, p.topic(), p.partition());
                    }

                    assignments.remove(p);
                }
            });
    }

    void seekToOffset(Consumer<RecordData, RecordData> consumer, List<TopicPartition> assignments, Map<TopicPartition, Long> endOffsets, Long offset, int limit) {
        var beginningOffsets = consumer.beginningOffsets(assignments);
        Iterator<TopicPartition> cursor = assignments.iterator();

        while (cursor.hasNext()) {
            TopicPartition p = cursor.next();
            long partitionBegin = beginningOffsets.get(p);
            long partitionEnd = endOffsets.get(p);
            long seekTarget;

            if (offset == null) {
                // Fetch the latest records, no earlier than the beginning of the partition
                seekTarget = Math.max(partitionBegin, partitionEnd - limit);
                consumer.seek(p, seekTarget);
            } else if (offset < partitionEnd) {
                // Seek to the requested offset, no earlier than the beginning of the partition
                seekTarget = Math.max(partitionBegin, offset);
                consumer.seek(p, seekTarget);
            } else {
                /*
                 * Requested offset is beyond the end of the partition,
                 * remove the assignment and return nothing for this partition.
                 */
                cursor.remove();
            }
        }
    }

    Comparator<ConsumerRecord<RecordData, RecordData>> buildComparator(Instant timestamp, Long offset) {
        Comparator<ConsumerRecord<RecordData, RecordData>> comparator = Comparator
                .<ConsumerRecord<RecordData, RecordData>>comparingLong(ConsumerRecord::timestamp)
                .thenComparingInt(ConsumerRecord::partition)
                .thenComparingLong(ConsumerRecord::offset);

        if (timestamp == null && offset == null) {
            // Returning "latest" records, newest to oldest within the result set size limit
            comparator = comparator.reversed();
        }

        return comparator;
    }

    KafkaRecord getItems(ConsumerRecord<RecordData, RecordData> rec, String topicId, List<String> include, Integer maxValueLength) {
        KafkaRecord item = new KafkaRecord(topicId);

        setProperty(KafkaRecord.Fields.PARTITION, include, rec::partition, item::partition);
        setProperty(KafkaRecord.Fields.OFFSET, include, rec::offset, item::offset);
        setProperty(KafkaRecord.Fields.TIMESTAMP, include, () -> Instant.ofEpochMilli(rec.timestamp()), item::timestamp);
        setProperty(KafkaRecord.Fields.TIMESTAMP_TYPE, include, rec.timestampType()::name, item::timestampType);
        setProperty(KafkaRecord.Fields.KEY, include, rec::key, k -> item.key(k.dataString(maxValueLength)));
        setProperty(KafkaRecord.Fields.VALUE, include, rec::value, v -> item.value(v.dataString(maxValueLength)));
        setProperty(KafkaRecord.Fields.HEADERS, include, () -> headersToMap(rec.headers(), maxValueLength), item::headers);
        setProperty(KafkaRecord.Fields.SIZE, include, () -> sizeOf(rec), item::size);

        schemaRelationship(rec.key()).ifPresent(item::keySchema);
        schemaRelationship(rec.value()).ifPresent(item::valueSchema);

        return item;
    }

    Optional<JsonApiRelationship> schemaRelationship(RecordData data) {
        return Optional.ofNullable(data)
                .map(d -> d.meta)
                .filter(recordMeta -> recordMeta.containsKey("schema-id"))
                .map(recordMeta -> {
                    String artifactType = recordMeta.get("schema-type");
                    // schema-id is present, it is null-safe to retrieve the name from configuration
                    String registryId = kafkaContext.schemaRegistryContext().getConfig().getName();
                    String schemaId = recordMeta.get("schema-id");
                    String name = recordMeta.get("schema-name");

                    var relationship = new JsonApiRelationship();
                    relationship.addMeta("artifactType", artifactType);
                    relationship.addMeta("name", name);
                    relationship.data(new Identifier("schemas", schemaId));
                    relationship.addLink("content", "/api/registries/%s/schemas/%s".formatted(registryId, schemaId));

                    schemaError(data).ifPresent(error -> relationship.addMeta("errors", List.of(error)));

                    return relationship;
                })
                .or(() -> schemaError(data).map(error -> {
                    var relationship = new JsonApiRelationship();
                    relationship.addMeta("errors", List.of(error));
                    return relationship;
                }));
    }

    Optional<com.github.streamshub.console.api.model.Error> schemaError(RecordData data) {
        return Optional.ofNullable(data).map(RecordData::error);
    }

    <T> void setProperty(String fieldName, List<String> include, Supplier<T> source, java.util.function.Consumer<T> target) {
        if (include.contains(fieldName)) {
            T value = source.get();
            if (value != null) {
                target.accept(value);
            }
        }
    }

    Map<String, String> headersToMap(Headers headers, Integer maxValueLength) {
        Map<String, String> headerMap = new LinkedHashMap<>();
        headers.iterator().forEachRemaining(h -> headerMap.put(h.key(), headerValue(h, maxValueLength)));
        return headerMap;
    }

    static String headerValue(Header header, Integer maxValueLength) {
        byte[] value = header.value();

        if (value != null) {
            int length;

            if (maxValueLength == null) {
                length = value.length;
            } else {
                length = Integer.min(maxValueLength, value.length);
            }

            return new String(value, 0, length);
        }

        return null;
    }

    static long sizeOf(RecordMetadata meta, Headers headers) {
        return sizeOf(meta.serializedKeySize(), meta.serializedValueSize(), headers);
    }

    static long sizeOf(ConsumerRecord<?, ?> rec) {
        return sizeOf(rec.serializedKeySize(), rec.serializedValueSize(), rec.headers());
    }

    static long sizeOf(int keySize, int valueSize, Headers headers) {
        return keySize + valueSize + Arrays.stream(headers.toArray())
                .mapToLong(h -> h.key().length() + (h.value() != null ? h.value().length : 0))
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
        private static final Duration MAX_POLL_TIME = Duration.ofMillis(100);

        private final Instant timeout;
        private int recordsConsumed = 0;
        private Map<TopicPartition, Integer> partitionConsumed = new HashMap<>();
        private final Consumer<K, V> consumer;
        private final Set<TopicPartition> assignments;
        private final Map<TopicPartition, Long> endOffsets;
        private final int limit;

        public ConsumerRecordsIterator(Consumer<K, V> consumer, Map<TopicPartition, Long> endOffsets, int limit, Instant timeout) {
            this.consumer = consumer;
            this.assignments = new HashSet<>(consumer.assignment());
            this.endOffsets = endOffsets;
            this.limit = limit;
            this.timeout = timeout;
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

            ConsumerRecords<K, V> records = ConsumerRecords.empty();

            while (records.isEmpty() && Instant.now().isBefore(timeout)) {
                records = poll();
            }

            int pollSize = 0;

            for (var partition : records.partitions()) {
                var partitionRecords = records.records(partition);
                int consumed = partitionRecords.size();
                pollSize += consumed;
                int total = partitionConsumed.compute(partition, (k, v) -> requireNonNullElse(v, 0) + consumed);

                if (total >= limit) {
                    // Consumed `limit` records for this partition
                    assignments.remove(partition);
                } else if (consumed > 0) {
                    long maxOffset = partitionRecords.stream().mapToLong(ConsumerRecord::offset).max().getAsLong() + 1;

                    if (maxOffset >= endOffsets.get(partition)) {
                        // Reached the end of the partition
                        assignments.remove(partition);
                    }
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

        ConsumerRecords<K, V> poll() {
            var timeRemaining = Duration.between(Instant.now(), timeout);
            Duration pollTimeout;

            if (timeRemaining.isNegative()) {
                pollTimeout = Duration.ZERO;
            } else {
                pollTimeout = MAX_POLL_TIME.compareTo(timeRemaining) < 0 ? MAX_POLL_TIME : timeRemaining;
            }

            return consumer.poll(pollTimeout);
        }
    }
}
