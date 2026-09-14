package com.github.streamshub.console.api.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
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
import java.util.concurrent.CompletionException;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.UnknownTopicIdException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.streamshub.console.api.model.KafkaRecord;
import com.github.streamshub.console.api.model.jsonapi.Identifier;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRelationshipToOne;
import com.github.streamshub.console.api.support.ContextualExecutorProvider;
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
    ContextualExecutorProvider threadContext;

    @Inject
    TopicDescribeService topicService;

    public List<KafkaRecord> consumeRecords(String topicId,
            Integer partition,
            Long offset,
            Instant timestamp,
            Integer limit,
            List<String> include,
            Integer maxValueLength) {

        String topicName = topicNameForId(topicId);
        List<PartitionInfo> partitions = consumer.partitionsFor(topicName);
        List<TopicPartition> assignments = partitions.stream()
                .filter(p -> partition == null || partition.equals(p.partition()))
                .map(p -> new TopicPartition(p.topic(), p.partition()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (assignments.isEmpty()) {
            return Collections.emptyList();
        }

        var beginningOffsets = consumer.beginningOffsets(assignments);
        var endOffsets = consumer.endOffsets(assignments);
        // End offset of zero means the partition has not been written to - don't bother reading them
        assignments.removeIf(assignment -> {
            long endOffset = endOffsets.get(assignment);

            if (endOffset == 0) {
                return true;
            }

            long beginningOffset = beginningOffsets.get(assignment);
            return endOffset - beginningOffset == 0;
        });

        if (assignments.isEmpty()) {
            return Collections.emptyList();
        }

        consumer.assign(assignments);

        if (timestamp != null) {
            seekToTimestamp(consumer, assignments, timestamp);
        } else {
            seekToOffset(consumer, assignments, beginningOffsets, endOffsets, offset, limit);
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
    }

    public KafkaRecord produceRecord(String topicId, KafkaRecord input) {
        String topicName = topicNameForId(topicId);

        List<PartitionInfo> partitions = producer.partitionsFor(topicName);
        Integer partition = input.partition();

        if (partition != null && partitions.stream().noneMatch(p -> partition.equals(p.partition()))) {
            throw invalidPartition(topicId, partition);
        }

        return send(topicName, input, producer);
    }

    KafkaRecord send(String topicName, KafkaRecord input, Producer<RecordData, RecordData> producer) {
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

        RecordMetadata meta;

        try {
            meta = producer.send(request).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CompletionException("Error occurred while sending record to Kafka cluster", e);
        } catch (Exception e) {
            throw new CompletionException("Error occurred while sending record to Kafka cluster", e);
        }

        KafkaRecord result = new KafkaRecord();
        result.partition(meta.partition());

        if (meta.hasOffset()) {
            result.offset(meta.offset());
        }

        if (meta.hasTimestamp()) {
            result.timestamp(Instant.ofEpochMilli(meta.timestamp()));
        }

        result.key(input.key());
        result.value(input.value());
        result.headers(input.headers());
        result.size(sizeOf(meta, request.headers()));

        schemaRelationship(key).ifPresent(result::keySchema);
        schemaRelationship(value).ifPresent(result::valueSchema);

        return result;
    }

    void setSchemaMeta(JsonApiRelationshipToOne schemaRelationship, RecordData data) {
        schemaMeta(schemaRelationship, "coordinates").ifPresent(gav -> data.meta.put("schema-gav", gav));
        schemaMeta(schemaRelationship, "messageType").ifPresent(type -> data.meta.put("message-type", type));
    }

    Optional<String> schemaMeta(JsonApiRelationshipToOne schemaRelationship, String key) {
        return Optional.ofNullable(schemaRelationship)
                .map(JsonApiRelationshipToOne::meta)
                .map(meta -> {
                    Object value = meta.get(key);
                    return (value instanceof String stringValue) ? stringValue : null;
                });
    }

    String topicNameForId(String topicId) {
        return topicService.topicNameForId(topicId)
            .thenApply(topic -> topic.orElseThrow(() -> noSuchTopic(topicId)))
            .toCompletableFuture()
            .join();
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

    void seekToOffset(Consumer<RecordData, RecordData> consumer, List<TopicPartition> assignments,
            Map<TopicPartition, Long> beginningOffsets,
            Map<TopicPartition, Long> endOffsets,
            Long offset, int limit) {

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
        Map<String, Object> contentMeta = new LinkedHashMap<>();

        setProperty(KafkaRecord.Fields.PARTITION, include, rec::partition, item::partition);
        setProperty(KafkaRecord.Fields.OFFSET, include, rec::offset, item::offset);
        setProperty(KafkaRecord.Fields.TIMESTAMP, include, () -> Instant.ofEpochMilli(rec.timestamp()), item::timestamp);
        setProperty(KafkaRecord.Fields.TIMESTAMP_TYPE, include, rec.timestampType()::name, item::timestampType);
        if (include.contains(KafkaRecord.Fields.KEY)) {
            FieldResult keyResult = encodeField(rec.key() != null ? rec.key().bytes() : null, maxValueLength);
            item.key(keyResult.value());
            if (keyResult.meta() != null) {
                contentMeta.put(KafkaRecord.Fields.KEY, keyResult.meta());
            }
        }

        if (include.contains(KafkaRecord.Fields.VALUE)) {
            FieldResult valueResult = encodeField(rec.value() != null ? rec.value().bytes() : null, maxValueLength);
            item.value(valueResult.value());
            if (valueResult.meta() != null) {
                contentMeta.put(KafkaRecord.Fields.VALUE, valueResult.meta());
            }
        }

        if (include.contains(KafkaRecord.Fields.HEADERS)) {
            HeadersResult headersResult = headersToResult(rec.headers(), maxValueLength);
            item.headers(headersResult.values());
            if (!headersResult.contentMeta().isEmpty()) {
                contentMeta.put(KafkaRecord.Fields.HEADERS, headersResult.contentMeta());
            }
        }

        setProperty(KafkaRecord.Fields.SIZE, include, () -> sizeOf(rec), item::size);

        if (!contentMeta.isEmpty()) {
            item.addMeta("content", contentMeta);
        }

        schemaRelationship(rec.key()).ifPresent(item::keySchema);
        schemaRelationship(rec.value()).ifPresent(item::valueSchema);

        return item;
    }

    Optional<JsonApiRelationshipToOne> schemaRelationship(RecordData data) {
        return Optional.ofNullable(data)
                .map(d -> d.meta)
                .filter(recordMeta -> recordMeta.containsKey("schema-id"))
                .map(recordMeta -> {
                    String artifactType = recordMeta.get("schema-type");
                    // schema-id is present, it is null-safe to retrieve the name from configuration
                    String registryId = kafkaContext.schemaRegistryContext().getConfig().getName();
                    String schemaId = recordMeta.get("schema-id");
                    String name = recordMeta.get("schema-name");

                    var relationship = new JsonApiRelationshipToOne(new Identifier("schemas", schemaId));
                    relationship.addMeta("artifactType", artifactType);
                    relationship.addMeta("name", name);
                    relationship.addLink("content", "/api/registries/%s/schemas/%s".formatted(registryId, schemaId));

                    schemaError(data).ifPresent(error -> relationship.addMeta("errors", List.of(error)));

                    return relationship;
                })
                .or(() -> schemaError(data).map(error -> {
                    var relationship = new JsonApiRelationshipToOne(null);
                    relationship.addMeta("errors", List.of(error));
                    return relationship;
                }));
    }

    Optional<com.github.streamshub.console.api.model.jsonapi.JsonApiError> schemaError(RecordData data) {
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

    private record HeadersResult(Map<String, String> values, Map<String, ContentMeta> contentMeta) { }

    HeadersResult headersToResult(Headers headers, Integer maxValueLength) {
        Map<String, String> valueMap = new LinkedHashMap<>();
        Map<String, ContentMeta> metaMap = new LinkedHashMap<>();
        headers.iterator().forEachRemaining(h -> {
            FieldResult result = encodeField(h.value(), maxValueLength);
            valueMap.put(h.key(), result.value());
            if (result.meta() != null) {
                metaMap.put(h.key(), result.meta());
            }
        });
        return new HeadersResult(valueMap, metaMap);
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

    /**
     * Inspects {@code bytes} for binary content and returns a {@link FieldResult}
     * pairing the appropriate string value with an optional {@link ContentMeta}.
     */
    private FieldResult encodeField(byte[] bytes, Integer maxValueLength) {
        if (bytes == null) {
            return new FieldResult(null, null);
        }
        if (bytes.length == 0) {
            return new FieldResult("", null);
        }

        StringBuilder sb = new StringBuilder();
        boolean binary = false;

        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8
                .newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT))) {
            int cp;

            while ((cp = reader.read()) != -1) {
                if (isExcludedUnicodeC0(cp) || isUnicodeC1(cp)) {
                    /*
                     * Consider NUL, C0 controls (except common text whitespace), DEL,
                     * and C1 controls to be indicative of "binary data" that will not 
                     * be displayed as text in the UI.
                     */
                    binary = true;
                    break;
                }

                sb.appendCodePoint(cp);
            }
        } catch (IOException e) {
            binary = true;
        }

        if (binary) {
            if (maxValueLength != null && bytes.length > maxValueLength) {
                return new FieldResult(null, ContentMeta.forBinaryOmitted());
            }
            return new FieldResult(Base64.getEncoder().encodeToString(bytes), ContentMeta.forBinaryEncoded());
        }

        // text path — sb already contains the full decoded string
        String text = sb.toString();
        if (maxValueLength != null && text.length() > maxValueLength) {
            return new FieldResult(text.substring(0, maxValueLength), ContentMeta.forTextTruncated());
        }
        return new FieldResult(text, null);
    }

    /** Returns true for C0 control codes except common text whitespace (tab, LF, CR). */
    private static boolean isExcludedUnicodeC0(int cp) {
        return cp <= 0x1F && cp != '\t' && cp != '\n' && cp != '\r';
    }

    /** Returns true for DEL and C1 control codes (0x7F–0x9F). */
    private static boolean isUnicodeC1(int cp) {
        return cp >= 0x7F && cp <= 0x9F;
    }

    /** Carries the processed string value alongside optional content metadata. */
    private record FieldResult(String value, ContentMeta meta) { }

    /** Describes the content type and encoding of a single record field. */
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    public static final class ContentMeta {

        private static final ContentMeta BINARY_ENCODED = new ContentMeta("application/octet-stream", "base64", false, false);
        private static final ContentMeta BINARY_OMITTED = new ContentMeta("application/octet-stream", null, true, false);
        private static final ContentMeta TEXT_TRUNCATED = new ContentMeta("text/plain", null, false, true);

        private final String type;
        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final String encoding;
        private final boolean omitted;
        private final boolean truncated;

        private ContentMeta(String type, String encoding, boolean omitted, boolean truncated) {
            this.type = type;
            this.encoding = encoding;
            this.omitted = omitted;
            this.truncated = truncated;
        }

        /** Binary field within size limit — base64-encoded. */
        public static ContentMeta forBinaryEncoded() {
            return BINARY_ENCODED;
        }

        /** Binary field exceeding size limit — omitted from response. */
        public static ContentMeta forBinaryOmitted() {
            return BINARY_OMITTED;
        }

        /** Text field truncated by maxValueLength. */
        public static ContentMeta forTextTruncated() {
            return TEXT_TRUNCATED;
        }

        public String getType() {
            return type;
        }

        public String getEncoding() {
            return encoding;
        }

        public boolean isOmitted() {
            return omitted;
        }

        public boolean isTruncated() {
            return truncated;
        }
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
                    LOGGER.tracef("Consumed %d records (more than limit %d) from partition %s", total, limit, partition);
                    assignments.remove(partition);
                } else if (consumed > 0) {
                    long maxOffset = partitionRecords.stream().mapToLong(ConsumerRecord::offset).max().getAsLong() + 1;

                    if (maxOffset >= endOffsets.get(partition)) {
                        // Reached the end of the partition
                        LOGGER.tracef("Reached end of partition %s at offset %s", partition, maxOffset);
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
