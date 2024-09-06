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
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import jakarta.annotation.PostConstruct;
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
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.console.api.model.KafkaRecord;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.SizeLimitedSortedSet;
import com.github.streamshub.console.api.support.serdes.MultiformatDeserializer;
import com.github.streamshub.console.api.support.serdes.MultiformatSerializer;
import com.github.streamshub.console.api.support.serdes.RecordData;

import io.apicurio.registry.rest.client.RegistryClient;
import io.apicurio.registry.rest.client.RegistryClientFactory;

import static java.util.Objects.requireNonNullElse;

@ApplicationScoped
public class RecordService {

    @Inject
    Logger logger;

    @Inject
    KafkaContext kafkaContext;

    @Inject
    BiFunction<
        Deserializer<RecordData>,
        Deserializer<RecordData>,
        Consumer<RecordData, RecordData>
        > consumerFactory;

    @Inject
    BiFunction<
        Serializer<RecordData>,
        Serializer<RecordData>,
        Producer<RecordData, RecordData>
        > producerFactory;

    @Inject
    ThreadContext threadContext;

    @Inject
    ObjectMapper objectMapper;

    RegistryClient registryClient;

    MultiformatDeserializer keyDeserializer;
    MultiformatDeserializer valueDeserializer;

    MultiformatSerializer keySerializer;
    MultiformatSerializer valueSerializer;

    @PostConstruct
    public void initialize() {
        String registryUrl = ConfigProvider.getConfig().getOptionalValue("console.registry.endpoint", String.class)
                // TODO: remove default
                .orElse("http://localhost:9080");

        registryClient = RegistryClientFactory.create(registryUrl);

        keyDeserializer = new MultiformatDeserializer(registryClient);
        keyDeserializer.configure(kafkaContext.configs(Consumer.class), true);

        valueDeserializer = new MultiformatDeserializer(registryClient);
        valueDeserializer.configure(kafkaContext.configs(Consumer.class), false);

        keySerializer = new MultiformatSerializer(registryClient, objectMapper);
        keySerializer.configure(kafkaContext.configs(Producer.class), true);

        valueSerializer = new MultiformatSerializer(registryClient, objectMapper);
        valueSerializer.configure(kafkaContext.configs(Producer.class), false);
    }

    public CompletionStage<List<KafkaRecord>> consumeRecords(String topicId,
            Integer partition,
            Long offset,
            Instant timestamp,
            Integer limit,
            List<String> include,
            Integer maxValueLength) {

        return topicNameForId(topicId).thenApplyAsync(topicName -> {
            Consumer<RecordData, RecordData> consumer = consumerFactory.apply(keyDeserializer, valueDeserializer);
            List<PartitionInfo> partitions = consumer.partitionsFor(topicName);
            List<TopicPartition> assignments = partitions.stream()
                    .filter(p -> partition == null || partition.equals(p.partition()))
                    .map(p -> new TopicPartition(p.topic(), p.partition()))
                    .toList();

            if (assignments.isEmpty()) {
                return Collections.emptyList();
            }

            consumer.assign(assignments);
            var endOffsets = consumer.endOffsets(assignments);

            if (timestamp != null) {
                seekToTimestamp(consumer, assignments, timestamp);
            } else {
                seekToOffset(consumer, assignments, endOffsets, offset, limit);
            }

            Iterable<ConsumerRecords<RecordData, RecordData>> poll = () -> new ConsumerRecordsIterator<>(consumer, endOffsets, limit);
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
        Executor asyncExec = threadContext.currentContextExecutor();

        topicNameForId(topicId).thenAcceptAsync(topicName -> {
            Producer<RecordData, RecordData> producer = producerFactory.apply(keySerializer, valueSerializer);
            List<PartitionInfo> partitions = producer.partitionsFor(topicName);
            Integer partition = input.partition();

            if (partition != null && partitions.stream().noneMatch(p -> partition.equals(p.partition()))) {
                promise.completeExceptionally(invalidPartition(topicId, partition));
            } else {
                send(topicName, (String) input.getMeta("format-value"), input, producer, promise);
            }

            promise.whenComplete((kafkaRecord, error) -> producer.close());
        }, asyncExec).exceptionally(e -> {
            promise.completeExceptionally(e);
            return null;
        });

        return promise;
    }

    void send(String topicName, String format, KafkaRecord input, Producer<RecordData, RecordData> producer, CompletableFuture<KafkaRecord> promise) {
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

        if (format != null) {
            headers.add(new Header() {
                @Override
                public String key() {
                    return "com.github.streamshub.console.message-format-value";
                }

                @Override
                public byte[] value() {
                    return format != null ? format.getBytes() : null;
                }
            });
        }

        Long timestamp = Optional.ofNullable(input.timestamp()).map(Instant::toEpochMilli).orElse(null);
        var key = new RecordData(input.key());
        var value = new RecordData(input.value());

        ProducerRecord<RecordData, RecordData> request = new ProducerRecord<>(topicName,
                input.partition(),
                timestamp,
                key,
                value,
                headers);

        producer.send(request, (meta, exception) -> {
            if (exception != null) {
                promise.completeExceptionally(exception);
            } else {
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
                result.headers(input.headers());
                result.size(sizeOf(meta, request.headers()));
                promise.complete(result);
            }
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

    void seekToOffset(Consumer<RecordData, RecordData> consumer, List<TopicPartition> assignments, Map<TopicPartition, Long> endOffsets, Long offset, int limit) {
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

        Optional.ofNullable(rec.key()).map(RecordData::type)
                .ifPresent(fmt -> item.addMeta("key-format", fmt));
        Optional.ofNullable(rec.value()).map(RecordData::type)
                .ifPresent(fmt -> item.addMeta("value-format", fmt));

        setProperty(KafkaRecord.Fields.PARTITION, include, rec::partition, item::partition);
        setProperty(KafkaRecord.Fields.OFFSET, include, rec::offset, item::offset);
        setProperty(KafkaRecord.Fields.TIMESTAMP, include, () -> Instant.ofEpochMilli(rec.timestamp()), item::timestamp);
        setProperty(KafkaRecord.Fields.TIMESTAMP_TYPE, include, rec.timestampType()::name, item::timestampType);
        setProperty(KafkaRecord.Fields.KEY, include, rec::key, k -> item.key(k.dataString(maxValueLength)));
        setProperty(KafkaRecord.Fields.VALUE, include, rec::value, v -> item.value(v.dataString(maxValueLength)));
        setProperty(KafkaRecord.Fields.HEADERS, include, () -> headersToMap(rec.headers(), maxValueLength), item::headers);
        setProperty(KafkaRecord.Fields.SIZE, include, () -> sizeOf(rec), item::size);

        return item;
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
        headers.iterator().forEachRemaining(h -> headerMap.put(h.key(), RecordData.bytesToString(h.value(), maxValueLength)));
        return headerMap;
    }

    long sizeOf(RecordMetadata meta, Headers headers) {
        return meta.serializedKeySize() +
                meta.serializedValueSize() +
                Arrays.stream(headers.toArray())
                    .mapToLong(h -> h.key().length() + h.value().length)
                    .sum();
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
