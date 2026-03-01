package com.github.streamshub.console.kafka.systemtest.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.GroupListing;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.GroupProtocol;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.KafkaShareConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.ShareConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.GroupType;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.BlockingSupplier;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

public class ConsumerUtils {

    static final Logger log = Logger.getLogger(ConsumerUtils.class);
    final Config config;
    final Properties adminConfig;

    public ConsumerUtils(Config config) {
        this(null, config);
    }

    public ConsumerUtils(URI bootstrapServers, Config config) {
        this.config = config;
        this.adminConfig = ClientsConfig.getAdminConfig(config);

        if (bootstrapServers != null) {
            adminConfig.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers.toString());
        }
    }

    public GroupState consumerGroupState(String groupId) {
        try (Admin admin = Admin.create(adminConfig)) {
            return admin.describeConsumerGroups(List.of(groupId))
                    .describedGroups()
                    .get(groupId)
                    .toCompletionStage()
                    .toCompletableFuture()
                    .join()
                    .groupState();
        } catch (Exception e) {
            fail(e);
        }

        return null;
    }

    public Map<TopicPartition, OffsetAndMetadata> consumerGroupOffsets(String groupId) {
        try (Admin admin = Admin.create(adminConfig)) {
            return admin.listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata()
                    .toCompletionStage()
                    .toCompletableFuture()
                    .join();
        } catch (Exception e) {
            fail(e);
        }

        return null;
    }

    public void alterConsumerGroupOffsets(String groupId, Map<TopicPartition, OffsetAndMetadata> offsets) {
        try (Admin admin = Admin.create(adminConfig)) {
            admin.alterConsumerGroupOffsets(groupId, offsets)
                    .all()
                    .toCompletionStage()
                    .toCompletableFuture()
                    .join();
        } catch (Exception e) {
            fail(e);
        }
    }

    private Map<GroupType, List<String>> allGroupIds(Admin admin) {
        return admin.listGroups()
            .all()
            .toCompletionStage()
            .toCompletableFuture()
            .join()
            .stream()
            .collect(groupingBy(
                    listing -> listing.type().orElse(GroupType.UNKNOWN),
                    mapping(GroupListing::groupId, toList())));
    }

    public void deleteGroups() {
        try (Admin admin = Admin.create(adminConfig)) {
            var allGroupIds = allGroupIds(admin);
            log.infof("Deleting consumer groups: %s", allGroupIds);

            while (!allGroupIds.isEmpty()) {
                allGroupIds.entrySet()
                    .stream()
                    .map(entry -> CompletableFuture.supplyAsync(() -> {
                        return switch (entry.getKey()) {
                            case CLASSIC, CONSUMER -> admin.deleteConsumerGroups(entry.getValue()).deletedGroups();
                            case SHARE -> admin.deleteShareGroups(entry.getValue()).deletedGroups();
                            case STREAMS -> admin.deleteStreamsGroups(entry.getValue()).deletedGroups();
                            default -> throw new IllegalStateException("Unexpected group type: " + entry.getKey());
                        };
                    }))
                    .map(CompletableFuture::join)
                    .map(Map::entrySet)
                    .flatMap(Collection::stream)
                    .map(e -> {
                        return e.getValue().toCompletionStage().handle((nothing, error) -> {
                            if (error == null || error instanceof GroupIdNotFoundException) {
                                return (Void) null;
                            }

                            log.warnf("Failed to delete consumer group %s: %s", e.getKey(), error.getMessage());
                            throw new CompletionException(error);
                        }).toCompletableFuture();
                    })
                    .reduce(CompletableFuture::allOf)
                    .orElseGet(() -> CompletableFuture.completedFuture(null))
                    .get(10, TimeUnit.SECONDS);

                allGroupIds = allGroupIds(admin);
            }
        } catch (Exception e) {
            fail(e);
        }
    }

    public enum ConsumerType {
        CLASSIC(GroupType.CLASSIC, GroupProtocol.CLASSIC),
        CONSUMER(GroupType.CONSUMER, GroupProtocol.CONSUMER),
        SHARE(GroupType.SHARE, null),
        STREAMS(GroupType.STREAMS, null);

        private final GroupType type;
        private final GroupProtocol protocol;

        private ConsumerType(GroupType type, GroupProtocol protocol) {
            this.type = type;
            this.protocol = protocol;
        }

        public GroupType groupType() {
            return type;
        }

        public GroupProtocol protocol() {
            return protocol;
        }
    }

    public ConsumerRequest request(ConsumerType type) {
        return new ConsumerRequest(type);
    }

    public class ConsumerRequest {
        final ConsumerType type;
        String groupId;
        String clientId;
        List<NewTopic> topics = new ArrayList<>();
        boolean createTopic = true;
        int messagesPerTopic = 0;
        Function<Integer, Object> messageSupplier = i -> "message-" + i;
        String keySerializer = StringSerializer.class.getName();
        String valueSerializer = StringSerializer.class.getName();
        int consumeMessages = 0;
        boolean autoClose = false;
        Map<String, String> configs = new HashMap<>();

        private ConsumerRequest(ConsumerType type) {
            this.type = type;
        }

        public ConsumerRequest topic(String topicName, int numPartitions) {
            this.topics.add(new NewTopic(topicName, numPartitions, (short) 1));
            return this;
        }

        public ConsumerRequest topic(String topicName) {
            this.topics.add(new NewTopic(topicName, 1, (short) 1));
            return this;
        }

        public ConsumerRequest createTopic(boolean createTopic) {
            this.createTopic = createTopic;
            return this;
        }

        public ConsumerRequest groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public ConsumerRequest clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public ConsumerRequest messagesPerTopic(int messagesPerTopic) {
            this.messagesPerTopic = messagesPerTopic;
            return this;
        }

        public ConsumerRequest messageSupplier(Function<Integer, Object> messageSupplier) {
            this.messageSupplier = messageSupplier;
            return this;
        }

        public ConsumerRequest valueSerializer(String valueSerializer) {
            this.valueSerializer = valueSerializer;
            return this;
        }

        public ConsumerRequest consumeMessages(int consumeMessages) {
            this.consumeMessages = consumeMessages;
            return this;
        }

        public ConsumerRequest autoClose(boolean autoClose) {
            this.autoClose = autoClose;
            return this;
        }

        public ConsumerRequest config(String key, String value) {
            this.configs.put(key, value);
            return this;
        }

        public ConsumerRequest configs(Map<String, String> configs) {
            this.configs.clear();
            this.configs.putAll(configs);
            return this;
        }

        public <C extends AutoCloseable> ConsumerResponse<C> consume() {
            switch (type) {
                case CLASSIC, CONSUMER:
                    return ConsumerUtils.this.consume(this, autoClose);
                case SHARE:
                    return ConsumerUtils.this.consume(this, autoClose);
                case STREAMS:
                default:
                    throw new IllegalArgumentException("Unsupported consumer type: " + type);
            }
        }

        int fetchLimit() {
            return consumeMessages > 0 ? consumeMessages : (messagesPerTopic * topics.size());
        }
    }

    public static class ConsumerResponse<C extends AutoCloseable> implements AutoCloseable {
        private final ConsumerRequest request;
        private final int messageLimit;
        private final Map<String, Integer> messagesPerPartition;
        private C consumer;
        private AtomicBoolean consumerRunning = new AtomicBoolean(false);
        private Map<String, Map<Integer, List<ConsumerRecord<String, String>>>> records = new HashMap<>();

        ConsumerResponse(ConsumerRequest request) {
            this.request = request;
            messageLimit = request.messagesPerTopic * request.topics.size();
            messagesPerPartition = request.topics
                    .stream()
                    .collect(Collectors.toMap(NewTopic::name, topic -> {
                        int perPartition = request.messagesPerTopic / topic.numPartitions();
                        return Math.max(perPartition, 1);
                    }));
        }

        @Override
        public void close() {
            if (consumer != null) {
                try {
                    consumer.close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public C consumer() {
            return consumer;
        }

        private static final short FIRST_DELIVERY = 1;

        boolean add(ConsumerRecord<String, String> rec) {
            final boolean added;

            if (rec.deliveryCount().orElse(FIRST_DELIVERY) == FIRST_DELIVERY) {
                var partitionRecords = records
                        .computeIfAbsent(rec.topic(), t -> new HashMap<>())
                        .computeIfAbsent(rec.partition(), p -> new ArrayList<>());
                var partitionLimit = messagesPerPartition.get(rec.topic());

                if (partitionRecords.size() < partitionLimit && recordCount() < messageLimit) {
                    partitionRecords.add(rec);
                    log.debugf("Accepted record [%s-%d @ %d]: %s", rec.topic(), rec.partition(), rec.offset(), rec.value());
                    added = true;
                } else {
                    log.debugf("Dropped overflow record [%s-%d @ %d]: %s", rec.topic(), rec.partition(), rec.offset(), rec.value());
                    added = false;
                }
            } else {
                log.debugf("Dropped re-delivered record [%s-%d @ %d]: %s", rec.topic(), rec.partition(), rec.offset(), rec.value());
                added = false;
            }

            return added;
        }

        long recordCount() {
            return records.values()
                .stream()
                .flatMap(e -> e.values().stream())
                .mapToInt(Collection::size)
                .sum();
        }

        boolean continuePolling() {
            return recordCount() < request.fetchLimit();
        }
    }

    @SuppressWarnings("unchecked")
    public <C extends AutoCloseable> C consume(
            ConsumerType type,
            String groupId,
            String topicName,
            String clientId,
            int numPartitions,
            boolean autoClose) {

        return (C) request(type)
                .groupId(groupId)
                .topic(topicName, numPartitions)
                .clientId(clientId)
                .messagesPerTopic(1)
                .autoClose(autoClose)
                .consume()
                .consumer();
    }

    @SuppressWarnings("unchecked")
    public Consumer<String, String> consume(String groupId, String topicName, String clientId, int numPartitions, boolean autoClose) {
        return (Consumer<String, String>) request(ConsumerType.CLASSIC)
                .groupId(groupId)
                .topic(topicName, numPartitions)
                .clientId(clientId)
                .messagesPerTopic(1)
                .autoClose(autoClose)
                .consume()
                .consumer();
    }

    <C extends AutoCloseable> ConsumerResponse<C> consume(ConsumerRequest request, boolean autoClose) {
        ConsumerResponse<C> response = new ConsumerResponse<>(request);

        try (Admin admin = Admin.create(adminConfig)) {
            if (request.createTopic) {
                BlockingSupplier.get(() -> admin.createTopics(request.topics).all());
            }

            var executor = Executors.newFixedThreadPool(2);

            var consumePromise = CompletableFuture
                    .runAsync(() -> {
                        if (request.type == ConsumerType.SHARE) {
                            shareConsumeMessages(admin, request, response);
                        } else {
                            consumeMessages(request, response);
                        }
                    }, executor);

            var producePromise = CompletableFuture
                    .runAsync(() -> {
                        await().atMost(15, TimeUnit.SECONDS).until(response.consumerRunning::get);
                    }, executor)
                    .thenComposeAsync(nothing -> produceMessages(request), executor);

            CompletableFuture.allOf(consumePromise, producePromise).join();
        } catch (Exception e) {
            response.close();
            throw new RuntimeException(e);
        }

        if (autoClose) {
            response.close();
        }

        return response;
    }

    CompletionStage<Void> produceMessages(ConsumerRequest consumerRequest) {
        if (consumerRequest.messagesPerTopic < 1) {
            return CompletableFuture.completedStage(null);
        }

        Properties cfg = ClientsConfig.getProducerConfig(config, consumerRequest.keySerializer, consumerRequest.valueSerializer);

        List<CompletableFuture<Void>> pending = new ArrayList<>();

        try (var producer = new KafkaProducer<String, Object>(cfg)) {
            long timestamp = System.currentTimeMillis();

            for (NewTopic topic : consumerRequest.topics) {
                String topicName = topic.name();
                int msgCount = consumerRequest.messagesPerTopic;

                // distribute the message over the partitions in the topic
                while (msgCount > -1) {
                    for (int p = 0, m = topic.numPartitions(); p < m && --msgCount > -1; p++) {
                        int partition = p;
                        Object value = consumerRequest.messageSupplier.apply(msgCount);
                        CompletableFuture<Void> promise = new CompletableFuture<>();
                        pending.add(promise);

                        long now = System.currentTimeMillis();

                        if (timestamp < now) {
                            timestamp = now;
                        } else {
                            timestamp++;
                        }

                        producer.send(new ProducerRecord<>(
                                topicName,
                                partition,
                                timestamp,
                                null /* key */,
                                value),
                            (metadata, exception) -> {
                                if (exception != null) {
                                    promise.completeExceptionally(exception);
                                } else {
                                    log.debugf("Message sent to topic/partition %s-%d: %s @ %d", topicName, partition, value, metadata.offset());
                                    promise.complete(null);
                                }
                            });
                    }
                }
            }
        }

        return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new));
    }

    @SuppressWarnings("unchecked")
    <C extends AutoCloseable> void consumeMessages(ConsumerRequest request, ConsumerResponse<C> response) {
        Properties cfg = ClientsConfig.getConsumerConfig(config, request.groupId);

        cfg.put(ConsumerConfig.GROUP_PROTOCOL_CONFIG, request.type.protocol().name.toLowerCase(Locale.ROOT));
        cfg.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        cfg.put(CommonClientConfigs.CLIENT_ID_CONFIG, request.clientId);

        if (request.type == ConsumerType.CLASSIC) {
            cfg.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 50_000);
        }

        if (request.consumeMessages > 0) {
            cfg.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(request.consumeMessages));
        }

        @SuppressWarnings("resource")
        Consumer<String, String> consumer = new KafkaConsumer<String, String>(cfg);
        response.consumer = (C) consumer;

        try {
            List<String> topics = request.topics
                    .stream()
                    .map(NewTopic::name)
                    .toList();

            if (request.groupId.isEmpty()) {
                List<TopicPartition> assignments = topics.stream()
                        .map(consumer::partitionsFor)
                        .flatMap(Collection::stream)
                        .map(p -> new TopicPartition(p.topic(), p.partition()))
                        .distinct()
                        .toList();

                // Must use assign instead of subscribe to support empty group.id
                consumer.assign(assignments);
            } else {
                consumer.subscribe(topics);
            }

            consumer.poll(Duration.ofMillis(100));
            consumer.commitSync();
            consume(request, response, consumer::poll, x -> { /* No-op */ }, consumer::assignment);
            consumer.commitSync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    <C extends AutoCloseable> void shareConsumeMessages(Admin admin, ConsumerRequest request, ConsumerResponse<C> response) {
        Properties cfg = ClientsConfig.getConsumerConfig(config, request.groupId);
        cfg.put(CommonClientConfigs.CLIENT_ID_CONFIG, request.clientId);
        cfg.put(ConsumerConfig.SHARE_ACKNOWLEDGEMENT_MODE_CONFIG, "explicit");

        if (request.consumeMessages > 0) {
            cfg.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(request.consumeMessages));
        }

        ShareConsumer<String, String> consumer = new KafkaShareConsumer<>(cfg);
        response.consumer = (C) consumer;

        try {
            List<String> topics = request.topics
                    .stream()
                    .map(NewTopic::name)
                    .toList();

            consumer.subscribe(topics);
            AtomicInteger status = new AtomicInteger(0);
            await().atMost(10, TimeUnit.SECONDS).ignoreExceptions()
                .until(() -> assignmentReceived(admin, consumer, request, status));

            consumer.commitSync();
            consume(request, response, consumer::poll, consumer::acknowledge, consumer::subscription);
            consumer.commitSync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void consume(ConsumerRequest request,
            ConsumerResponse<?> response,
            java.util.function.Function<Duration, ConsumerRecords<String, String>> poll,
            java.util.function.Consumer<ConsumerRecord<String, String>> acknowlege,
            java.util.function.Supplier<Object> assignment) {

        response.consumerRunning.set(true);

        if (request.consumeMessages < 1 && request.messagesPerTopic < 1) {
            var records = poll.apply(Duration.ofSeconds(5));
            records.forEach(rec -> {
                acknowlege.accept(rec);
                response.add(rec);
            });
        } else {
            Instant timeout = Instant.now().plusSeconds(20);

            while (response.continuePolling() && Instant.now().isBefore(timeout)) {
                var records = poll.apply(Duration.ofMillis(200));
                records.forEach(rec -> {
                    acknowlege.accept(rec);
                    response.add(rec);
                });
                log.debugf("Polled: %s ; total messages received: %d", assignment.get(), response.recordCount());
            }
        }
    }

    private boolean assignmentReceived(Admin admin, ShareConsumer<?, ?> consumer, ConsumerRequest request, AtomicInteger status) {
        consumer.poll(Duration.ofMillis(200));

        var group = admin.describeShareGroups(List.of(request.groupId))
                .all()
                .toCompletionStage()
                .toCompletableFuture()
                .join()
                .get(request.groupId);

        if (group.groupState() != GroupState.STABLE) {
            log.debugf("Group %s is not yet stable: %s", request.groupId, group.groupState());
            return false;
        }

        if (status.compareAndSet(0, 1)) {
            log.debugf("Group %s is now stable", request.groupId);
        }

        return group.members()
                .stream()
                .filter(m -> {
                    if (m.clientId().equals(request.clientId)) {
                        if (status.compareAndSet(1, 2)) {
                            log.debugf("Client %s became a member of group %s",
                                    request.clientId, request.groupId);
                        }
                        return true;
                    }
                    log.debugf("Client %s is not yet a member of group %s",
                            request.clientId, request.groupId);
                    return false;
                })
                .findFirst()
                .map(member -> {
                    if (member.assignment().topicPartitions().isEmpty()) {
                        log.debugf("Client %s does not yet have an assignments in group %s",
                                request.clientId, request.groupId);
                        return false;
                    }
                    if (status.compareAndSet(2, 3)) {
                        log.debugf("Client %s received assignment in group %s: %s", request.clientId,
                                request.groupId, member.assignment().topicPartitions());
                    }
                    return true;
                })
                .orElse(false);
    }
}
