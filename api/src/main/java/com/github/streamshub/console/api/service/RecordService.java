package com.github.streamshub.console.api.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.UnknownTopicIdException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.streamshub.console.api.errors.client.TooManyRequestsException;
import com.github.streamshub.console.api.model.KafkaRecord;
import com.github.streamshub.console.api.model.RecordFilterParams;
import com.github.streamshub.console.api.model.jsonapi.Identifier;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRelationshipToOne;
import com.github.streamshub.console.api.support.ContextualExecutorProvider;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.serdes.RecordData;

import static java.util.Objects.requireNonNullElse;

@ApplicationScoped
public class RecordService {

    static final String SSE_EVENT_HEARTBEAT = "heartbeat";
    static final String SSE_EVENT_DONE = "done";

    @Inject
    Logger logger;

    @Inject
    @ConfigProperty(name = "console.topics.records.poll-timeout", defaultValue = "PT5S")
    Duration pollTimeout;

    /**
     * Config name of the maximum time a single SSE record stream may run before it is
     * ended with the {@code timeout} reason. Read for each request.
     */
    public static final String STREAM_TIMEOUT_CONFIG = "console.topics.records.stream-timeout";
    static final Duration STREAM_TIMEOUT_DEFAULT = Duration.ofMinutes(5);

    /**
     * Config name of the maximum number of SSE record streams that may run at once.
     * Each stream holds a worker thread and a Kafka consumer for its entire duration.
     * Read for each request.
     */
    public static final String MAX_CONCURRENT_STREAMS_CONFIG = "console.topics.records.max-concurrent-streams";
    static final int MAX_CONCURRENT_STREAMS_DEFAULT = 10;

    private final AtomicInteger activeStreams = new AtomicInteger();

    @Inject
    Config config;

    @Inject
    KafkaContext kafkaContext;

    @Inject
    @Named("singleConsumer")
    Consumer<RecordData, RecordData> singleConsumer;

    @Inject
    @Named("streamConsumer")
    Consumer<RecordData, RecordData> streamConsumer;

    @Inject
    Producer<RecordData, RecordData> producer;

    @Inject
    ContextualExecutorProvider threadContext;

    @Inject
    TopicDescribeService topicService;

    // public for testing
    public int activeStreams() {
        return activeStreams.get();
    }

    /**
     * Fetch the single record at the given partition and offset of a topic.
     *
     * @throws NotFoundException when the partition does not exist or it has no record
     *         at the offset, e.g. it was deleted, compacted away, or is beyond the end
     *         of the partition.
     */
    public KafkaRecord getRecord(String topicId, int partition, long offset, List<String> include) {
        ConsumerRecordsIterator iterator = prepareConsumer(
                topicId,
                partition,
                offset
        );

        if (iterator != null) {
            while (iterator.hasNext()) {
                for (var rec : iterator.next()) {
                    /*
                     * Records are read in offset order starting at the requested offset, the
                     * first record is the requested one unless it no longer exists.
                     */
                    if (rec.partition() == partition && rec.offset() == offset) {
                        return getItems(rec, topicId, include, null);
                    }
                }
            }

            if (iterator.endReason() == ConsumerRecordsIterator.EndReason.TIMEOUT) {
                throw new TimeoutException("Timed out reading record at partition %d, offset %d"
                        .formatted(partition, offset));
            }
        }

        throw new NotFoundException("No such record");
    }

    public void consumeRecords(String topicId, List<String> include, RecordFilterParams params, SseEventSink sink, Sse sse) {
        int maxStreams = config.getOptionalValue(MAX_CONCURRENT_STREAMS_CONFIG, Integer.class)
                .orElse(MAX_CONCURRENT_STREAMS_DEFAULT);
        Duration streamTimeout = config.getOptionalValue(STREAM_TIMEOUT_CONFIG, Duration.class)
                .orElse(STREAM_TIMEOUT_DEFAULT);

        if (activeStreams.incrementAndGet() > maxStreams) {
            activeStreams.decrementAndGet();
            throw new TooManyRequestsException(
                    "Too many record streams are running, retry later (limit: %d)".formatted(maxStreams));
        }

        try {
            ConsumerRecordsIterator iterator = prepareConsumer(
                    topicId,
                    params,
                    streamTimeout,
                    sink::isClosed
            );

            if (iterator == null) {
                sendSseDone(sink, sse, ConsumerRecordsIterator.EndReason.COMPLETE);
                return;
            }

            consumeRecordsAsSse(iterator, topicId, include, params, sink, sse);
        } finally {
            activeStreams.decrementAndGet();
        }
    }

    /**
     * Resolves the topic, seeks to the requested position, and returns a
     * ready-to-poll {@link ConsumerRecordsIterator}. Returns {@code null} when
     * there are no assignments to read (empty partitions, no matching offset,
     * etc.).
     */
    private ConsumerRecordsIterator prepareConsumer(
            String topicId,
            int partition,
            long offset) {

        String topicName = topicNameForId(topicId);
        var knownPartitions = singleConsumer.partitionsFor(topicName)
                .stream()
                .map(PartitionInfo::partition)
                .collect(Collectors.toSet());

        if (!knownPartitions.contains(partition)) {
            return null;
        }

        TopicPartition assignment = new TopicPartition(topicName, partition);
        List<TopicPartition> assignments = List.of(assignment);
        var beginningOffsets = singleConsumer.beginningOffsets(assignments);
        var endOffsets = singleConsumer.endOffsets(assignments);

        long partitionBegin = beginningOffsets.get(assignment);
        long partitionEnd = endOffsets.get(assignment);

        if (offset < partitionBegin || offset >= partitionEnd) {
            return null;
        }

        singleConsumer.assign(assignments);

        // Seek to the requested offset, no earlier than the beginning of the partition
        var seekOffset = Math.max(partitionBegin, offset);
        singleConsumer.seek(assignment, seekOffset);

        return new ConsumerRecordsIterator(
                singleConsumer,
                endOffsets,
                1,
                Instant.now().plus(pollTimeout),
                Collections.emptyList(),
                () -> false
        );
    }

    /**
     * Resolves the topic, computes assignments, seeks to the requested position, and returns
     * a ready-to-poll {@link ConsumerRecordsIterator}. Returns {@code null} when there are no
     * assignments to read (empty partitions, no matching offset/timestamp, etc.).
     */
    private ConsumerRecordsIterator prepareConsumer(
            String topicId,
            RecordFilterParams params,
            Duration timeout,
            BooleanSupplier cancelled) {

        String topicName = topicNameForId(topicId);
        Integer partition = params.getPartition();
        Long offset = params.getOffset();
        Instant timestamp = params.getTimestamp();
        Integer limit = params.getLimit();
        List<Predicate<ConsumerRecord<RecordData, RecordData>>> predicates = searchPredicates(params);
        List<PartitionInfo> partitions = streamConsumer.partitionsFor(topicName);
        List<TopicPartition> assignments = partitions.stream()
                .filter(p -> partition == null || partition.equals(p.partition()))
                .map(p -> new TopicPartition(p.topic(), p.partition()))
                .collect(Collectors.toCollection(ArrayList::new));

        if (assignments.isEmpty()) {
            return null;
        }

        var beginningOffsets = streamConsumer.beginningOffsets(assignments);
        var endOffsets = streamConsumer.endOffsets(assignments);
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
            return null;
        }

        streamConsumer.assign(assignments);

        if (timestamp != null) {
            seekToTimestamp(streamConsumer, assignments, timestamp);
        } else {
            seekToOffset(streamConsumer, assignments, beginningOffsets, endOffsets, offset, limit);
        }

        if (assignments.isEmpty()) {
            return null;
        }

        /*
         * Re-assign, seek operations may have removed assignments for requests beyond
         * the end of the partition.
         */
        streamConsumer.assign(assignments);

        return new ConsumerRecordsIterator(
                streamConsumer,
                endOffsets,
                limit,
                Instant.now().plus(timeout),
                predicates,
                cancelled
        );
    }

    /**
     * Search predicates (key, value, headers) requested by the client. An empty list
     * means that all records are selected.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static List<Predicate<ConsumerRecord<RecordData, RecordData>>> searchPredicates(RecordFilterParams params) {
        return (List) params.getPredicates(ConsumerRecord.class);
    }

    private void consumeRecordsAsSse(
            ConsumerRecordsIterator iterator,
            String topicId,
            List<String> include,
            RecordFilterParams params,
            SseEventSink sink,
            Sse sse) {

        try {
            while (iterator.hasNext()) {
                if (sink.isClosed()) {
                    logger.debugf("SSE event sink for topic %s is closed - client has gone.", topicId);
                    break;
                }

                List<KafkaRecord> batchItems = iterator.next().stream()
                        .map(rec -> getItems(rec, topicId, include, params.getMaxValueLength()))
                        .toList();

                if (!batchItems.isEmpty()) {
                    OutboundSseEvent event = sse.newEventBuilder()
                            .mediaType(MediaType.APPLICATION_JSON_TYPE)
                            .data(List.class, batchItems)
                            .build();
                    sink.send(event);
                }

                /*
                 * Report progress after every batch, including those without any
                 * matching records, so clients can show how much has been scanned.
                 */
                sink.send(sse.newEventBuilder()
                        .name(SSE_EVENT_HEARTBEAT)
                        .data("{\"scanned\":" + iterator.totalConsumed()
                                + ",\"progress\":" + iterator.scanProgress()
                                + ",\"elapsed\":" + iterator.elapsed().toMillis()
                                + ",\"remaining\":" + iterator.remaining().toMillis() + "}")
                        .build());
            }
        } finally {
            sendSseDone(sink, sse, iterator.endReason());
        }
    }

    private static void sendSseDone(SseEventSink sink, Sse sse, ConsumerRecordsIterator.EndReason reason) {
        try {
            if (!sink.isClosed()) {
                sink.send(sse.newEventBuilder()
                        .name(SSE_EVENT_DONE)
                        .data("{\"reason\":\"" + reason.value() + "\"}")
                        .build());
            }
        } finally {
            sink.close();
        }
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

        Optional<String> text = RecordFilterParams.decodeText(bytes);

        if (text.isEmpty()) {
            if (maxValueLength != null && bytes.length > maxValueLength) {
                return new FieldResult(null, ContentMeta.forBinaryOmitted());
            }
            return new FieldResult(Base64.getEncoder().encodeToString(bytes), ContentMeta.forBinaryEncoded());
        }

        String decoded = text.get();
        if (maxValueLength != null && decoded.length() > maxValueLength) {
            return new FieldResult(decoded.substring(0, maxValueLength), ContentMeta.forTextTruncated());
        }
        return new FieldResult(decoded, null);
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

    /**
     * Iterates over batches of records polled from the consumer. Each batch contains
     * only the records that match the search predicates, if any were given.
     *
     * The {@code limit} is applied per partition to the records that <em>match</em> the
     * predicates, not to the records scanned. The total number of records scanned
     * (matching or not) is available from {@link #totalConsumed()}, and the estimated
     * fraction of the requested offset range that has been scanned from
     * {@link #scanProgress()}.
     */
    static class ConsumerRecordsIterator implements Iterator<List<ConsumerRecord<RecordData, RecordData>>> {
        /**
         * Why the iteration ended, reported to SSE clients in the final event.
         */
        enum EndReason {
            /** All requested partitions were read to their end offsets. */
            COMPLETE,
            /** The requested number of matching records was found. */
            LIMIT,
            /** The time allowed for the request ran out before all records were read. */
            TIMEOUT,
            /** The iteration was stopped before completing, e.g. the client went away. */
            CANCELLED;

            String value() {
                return name().toLowerCase(Locale.ROOT);
            }
        }

        private static final Logger LOGGER = Logger.getLogger(ConsumerRecordsIterator.class);
        private static final Duration MAX_POLL_TIME = Duration.ofMillis(100);

        private final Instant started = Instant.now();
        private final Instant timeout;
        /** Reports whether the consumer of the iteration has gone away, e.g. a closed SSE client. */
        private final BooleanSupplier cancelled;
        private int recordsConsumed = 0;
        private final Map<TopicPartition, Integer> partitionMatched = new HashMap<>();
        private final Consumer<RecordData, RecordData> consumer;
        private final Set<TopicPartition> assignments;
        private final Map<TopicPartition, Long> endOffsets;
        private final int limit;
        private final List<Predicate<ConsumerRecord<RecordData, RecordData>>> predicates;

        /** Offset at which reading started for each partition. */
        private final Map<TopicPartition, Long> startOffsets = new HashMap<>();
        /** Offset that will be read next for each partition. */
        private final Map<TopicPartition, Long> nextOffsets = new HashMap<>();
        /** Partitions that have nothing further to read, either at the end or the match limit was met. */
        private final Set<TopicPartition> finished = new HashSet<>();
        private boolean limitMet = false;
        private boolean timedOut = false;

        public ConsumerRecordsIterator(
                Consumer<RecordData, RecordData> consumer,
                Map<TopicPartition, Long> endOffsets,
                int limit,
                Instant timeout,
                List<Predicate<ConsumerRecord<RecordData, RecordData>>> predicates,
                BooleanSupplier cancelled) {

            this.consumer = consumer;
            this.cancelled = cancelled;
            this.assignments = new HashSet<>(consumer.assignment());
            this.endOffsets = endOffsets;
            this.limit = limit;
            this.timeout = timeout;
            this.predicates = predicates;

            for (var assignment : assignments) {
                long position = consumer.position(assignment);
                startOffsets.put(assignment, position);
                nextOffsets.put(assignment, position);
            }
        }

        /**
         * Estimate of the fraction (0 to 100, as a whole number) of the offset range to
         * be read that has been scanned. Measured in offsets rather than record counts
         * because compaction and transaction markers leave gaps in the offsets.
         */
        public int scanProgress() {
            long totalRange = 0;
            long scannedRange = 0;

            for (var entry : startOffsets.entrySet()) {
                var partition = entry.getKey();
                long range = Math.clamp(endOffsets.get(partition) - entry.getValue(), 0L, Long.MAX_VALUE);
                totalRange += range;

                if (finished.contains(partition)) {
                    scannedRange += range;
                } else {
                    scannedRange += Math.clamp(nextOffsets.get(partition) - entry.getValue(), 0L, range);
                }
            }

            return totalRange == 0 ? 100 : (int) (scannedRange * 100 / totalRange);
        }

        /**
         * The reason iteration ended. Only meaningful once {@link #hasNext()} is false.
         */
        public EndReason endReason() {
            if (finished.size() == startOffsets.size()) {
                return limitMet ? EndReason.LIMIT : EndReason.COMPLETE;
            }

            if (timedOut || !Instant.now().isBefore(timeout)) {
                return EndReason.TIMEOUT;
            }

            return EndReason.CANCELLED;
        }

        /**
         * The time since the iterator was created.
         */
        public Duration elapsed() {
            return Duration.between(started, Instant.now());
        }

        /**
         * The time left before the iteration is ended with the timeout reason, never negative.
         */
        public Duration remaining() {
            Duration remaining = Duration.between(Instant.now(), timeout);
            return remaining.isNegative() ? Duration.ZERO : remaining;
        }

        /**
         * The total number of records polled so far, whether or not they matched
         * the search predicates.
         */
        public int totalConsumed() {
            return recordsConsumed;
        }

        @Override
        public boolean hasNext() {
            boolean moreRecords = !assignments.isEmpty()
                    && Instant.now().isBefore(timeout)
                    && !cancelled.getAsBoolean();

            if (!moreRecords && LOGGER.isDebugEnabled()) {
                LOGGER.debugf("Total consumed records: %d", recordsConsumed);
            }

            return moreRecords;
        }

        /**
         * Poll the next batch of records.
         *
         * @return the records of the batch that match the search predicates. May be
         *         empty when none of the polled records matched.
         */
        @Override
        public List<ConsumerRecord<RecordData, RecordData>> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            ConsumerRecords<RecordData, RecordData> records = ConsumerRecords.empty();

            while (records.isEmpty()
                    && !assignments.isEmpty()
                    && Instant.now().isBefore(timeout)
                    && !cancelled.getAsBoolean()) {

                records = poll();

                if (records.isEmpty()) {
                    /*
                     * Nothing was returned, but the position may still have advanced past
                     * records that are never returned to the application (e.g. transaction
                     * markers), so partitions may have reached their end offsets.
                     */
                    finishPartitionsAtEnd();
                }
            }

            List<ConsumerRecord<RecordData, RecordData>> matched = new ArrayList<>();
            int pollSize = 0;

            for (var partition : records.partitions()) {
                var partitionRecords = records.records(partition);
                int consumed = partitionRecords.size();
                pollSize += consumed;
                int matchCount = selectMatches(partitionRecords, matched);

                final int partitionMatches = matchCount;
                int total = partitionMatched.compute(partition, (k, v) -> requireNonNullElse(v, 0) + partitionMatches);

                if (consumed > 0) {
                    long maxOffset = partitionRecords.stream().mapToLong(ConsumerRecord::offset).max().getAsLong() + 1;
                    nextOffsets.merge(partition, maxOffset, Math::max);
                }

                if (total >= limit) {
                    // Found `limit` matching records for this partition
                    LOGGER.tracef("Matched %d records (more than limit %d) from partition %s", total, limit, partition);
                    assignments.remove(partition);
                    finished.add(partition);
                    limitMet = true;
                }
            }

            finishPartitionsAtEnd();

            if (pollSize == 0) {
                /*
                 * No records before the timeout or the iteration was cancelled,
                 * unsubscribe everything. Partitions that finished normally were
                 * already removed and are not a timeout.
                 */
                timedOut = !assignments.isEmpty() && !cancelled.getAsBoolean();
                assignments.clear();
            } else if (filterLimitMet()) {
                // There are filters and the match limit has been met, unsubscribe everything
                limitMet = true;
                finished.addAll(startOffsets.keySet());
                assignments.clear();
            }

            consumer.assign(assignments);
            recordsConsumed += pollSize;

            if (LOGGER.isTraceEnabled()) {
                LOGGER.tracef("next() consumed records: %d, matched: %d; total consumed %s",
                        pollSize, matched.size(), recordsConsumed);
            }

            return matched;
        }

        /**
         * Marks partitions as finished when the consumer position has reached the end
         * offset recorded at the start. The position is used rather than the offset of
         * the last record returned because the last offset of a partition may belong to
         * a transaction marker, which is never returned by the consumer.
         */
        private void finishPartitionsAtEnd() {
            for (var partition : new ArrayList<>(assignments)) {
                long position = consumer.position(partition);
                nextOffsets.merge(partition, position, Math::max);

                if (position >= endOffsets.get(partition)) {
                    LOGGER.tracef("Reached end of partition %s at offset %s", partition, position);
                    assignments.remove(partition);
                    finished.add(partition);
                }
            }
        }

        private int selectMatches(List<ConsumerRecord<RecordData, RecordData>> partitionRecords,
                List<ConsumerRecord<RecordData, RecordData>> matched) {

            int matchCount = 0;

            for (var rec : partitionRecords) {
                if (matches(rec)) {
                    matched.add(rec);
                    matchCount++;
                }
            }

            return matchCount;
        }

        /**
         * A record is selected when no search predicates were requested or when at least
         * one of the key/value/headers predicates matches (OR semantics).
         */
        private boolean matches(ConsumerRecord<RecordData, RecordData> rec) {
            return predicates.isEmpty() || predicates.stream().anyMatch(p -> p.test(rec));
        }

        private boolean filterLimitMet() {
            if (predicates.isEmpty()) {
                return false;
            }

            var totalMatched = partitionMatched.values().stream().mapToLong(Integer::longValue).sum();
            return totalMatched >= limit;
        }

        ConsumerRecords<RecordData, RecordData> poll() {
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
