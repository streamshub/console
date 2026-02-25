package com.github.streamshub.console.kafka.systemtest.utils;

import java.io.Closeable;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.GroupListing;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.KafkaShareConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.consumer.ShareConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.GroupType;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.ConfigResource;
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

    public ConsumerRequest request() {
        return new ConsumerRequest();
    }

    public class ConsumerRequest {
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

        public ConsumerResponse consume() {
            return ConsumerUtils.this.consume(this, autoClose);
        }

        public ShareConsumerResponse share() {
            return ConsumerUtils.this.share(this, autoClose);
        }
    }

    public class ConsumerResponse implements Closeable {
        Consumer<String, String> consumer;
        List<ConsumerRecord<String, String>> records = new ArrayList<>();

        @Override
        public void close() {
            if (consumer != null) {
                consumer.close();
            }
        }

        public Consumer<String, String> consumer() {
            return consumer;
        }

        public List<ConsumerRecord<String, String>> records() {
            return records;
        }
    }

    public class ShareConsumerResponse implements Closeable {
        ShareConsumer<String, String> consumer;
        List<ConsumerRecord<String, String>> records = new ArrayList<>();

        @Override
        public void close() {
            if (consumer != null) {
                consumer.close();
            }
        }

        public ShareConsumer<String, String> consumer() {
            return consumer;
        }

        public List<ConsumerRecord<String, String>> records() {
            return records;
        }
    }

    @SuppressWarnings("unchecked")
    public <C extends AutoCloseable> C consume(
            Class<C> type,
            String groupId,
            String topicName,
            String clientId,
            int numPartitions,
            boolean autoClose) {

        if (Consumer.class.equals(type)) {
            return (C) consume(groupId, topicName, clientId, numPartitions, autoClose);
        }

        if (ShareConsumer.class.equals(type)) {
            return (C) share(groupId, topicName, clientId, numPartitions, autoClose);
        }

        throw new IllegalArgumentException("Unknown consumer type: " + type);
    }

    public Consumer<String, String> consume(String groupId, String topicName, String clientId, int numPartitions, boolean autoClose) {
        return request()
                .groupId(groupId)
                .topic(topicName, numPartitions)
                .clientId(clientId)
                .messagesPerTopic(1)
                .autoClose(autoClose)
                .consume()
                .consumer;
    }

    public ShareConsumer<String, String> share(String groupId, String topicName, String clientId, int numPartitions, boolean autoClose) {
        return request()
                .groupId(groupId)
                .topic(topicName, numPartitions)
                .clientId(clientId)
                .messagesPerTopic(1)
                .autoClose(autoClose)
                .share()
                .consumer;
    }

    ConsumerResponse consume(ConsumerRequest consumerRequest, boolean autoClose) {
        ConsumerResponse response = new ConsumerResponse();

        try (Admin admin = Admin.create(adminConfig)) {
            if (consumerRequest.createTopic) {
                BlockingSupplier.get(() -> admin.createTopics(consumerRequest.topics)
                    .all());
            }

            produceMessages(consumerRequest)
                .thenRun(() -> consumeMessages(consumerRequest, response))
                .toCompletableFuture()
                .get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            response.close();
            throw new RuntimeException(e);
        }

        if (autoClose) {
            response.close();
        }

        return response;
    }

    ShareConsumerResponse share(ConsumerRequest consumerRequest, boolean autoClose) {
        ShareConsumerResponse response = new ShareConsumerResponse();

        try (Admin admin = Admin.create(adminConfig)) {
            if (consumerRequest.createTopic) {
                BlockingSupplier.get(() -> admin.createTopics(consumerRequest.topics)
                        .all());
            }

            produceMessages(consumerRequest)
                .thenRun(() -> consumeMessages(admin, consumerRequest, response))
                .toCompletableFuture()
                .get(15, TimeUnit.SECONDS);
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

        Properties producerConfig =
                ClientsConfig.getProducerConfig(config, consumerRequest.keySerializer, consumerRequest.valueSerializer);

        List<CompletableFuture<Void>> pending = new ArrayList<>();

        try (var producer = new KafkaProducer<String, Object>(producerConfig)) {
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

                        producer.send(new ProducerRecord<>(
                                topicName,
                                partition,
                                System.currentTimeMillis(),
                                null /* key */,
                                value),
                            (metadata, exception) -> {
                                if (exception != null) {
                                    promise.completeExceptionally(exception);
                                } else {
                                    log.debugf("Message sent to topic/partition %s-%d: %s", topicName, partition, value);
                                    promise.complete(null);
                                }
                            });
                        // Wait to ensure each record receives a unique time stamp
                        await().atLeast(1, TimeUnit.MILLISECONDS).until(() -> true);
                    }
                }
            }
        }

        return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new));
    }

    void consumeMessages(ConsumerRequest consumerRequest, ConsumerResponse response) {
        Properties consumerConfig =
                ClientsConfig.getConsumerConfig(config, consumerRequest.groupId);

        consumerConfig.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 50_000);
        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerConfig.put(CommonClientConfigs.CLIENT_ID_CONFIG, consumerRequest.clientId);

        if (consumerRequest.consumeMessages > 0) {
            consumerConfig.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(consumerRequest.consumeMessages));
        }

        response.consumer = new KafkaConsumer<>(consumerConfig);

        try {
            List<String> topics = consumerRequest.topics
                    .stream()
                    .map(NewTopic::name)
                    .toList();

            if (consumerRequest.groupId.isEmpty()) {
                List<TopicPartition> assignments = topics.stream()
                        .map(response.consumer::partitionsFor)
                        .flatMap(partitions -> partitions.stream().map(p -> new TopicPartition(p.topic(), p.partition())))
                        .distinct()
                        .toList();

                // Must use assign instead of subscribe to support empty group.id
                response.consumer.assign(assignments);
            } else {
                response.consumer.subscribe(topics);
            }

            if (consumerRequest.consumeMessages < 1 && consumerRequest.messagesPerTopic < 1) {
                var records = response.consumer.poll(Duration.ofSeconds(5));
                records.forEach(response.records::add);
            } else {
                int pollCount = 0;
                int fetchCount = consumerRequest.consumeMessages > 0 ?
                    consumerRequest.consumeMessages :
                    (consumerRequest.messagesPerTopic * consumerRequest.topics.size());

                while (response.records.size() < fetchCount && pollCount++ < 10) {
                    var records = response.consumer.poll(Duration.ofSeconds(1));
                    records.forEach(response.records::add);
                    log.debugf("Assignments polled: %s ; total messages received: %d", response.consumer.assignment(), response.records.size());
                }
            }

            response.consumer.commitSync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void consumeMessages(Admin admin, ConsumerRequest consumerRequest, ShareConsumerResponse response) {
        Properties consumerConfig =
                ClientsConfig.getConsumerConfig(config, consumerRequest.groupId);

        consumerConfig.put(CommonClientConfigs.CLIENT_ID_CONFIG, consumerRequest.clientId);

        if (consumerRequest.consumeMessages > 0) {
            consumerConfig.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(consumerRequest.consumeMessages));
        }

        response.consumer = new KafkaShareConsumer<>(consumerConfig);

        await().atMost(10, TimeUnit.SECONDS).ignoreExceptions().until(() -> {
            // Set share.auto.offset.reset=earliest for the group
            admin.incrementalAlterConfigs(Map.of(
                    new ConfigResource(ConfigResource.Type.GROUP, consumerRequest.groupId),
                    Arrays.asList(new AlterConfigOp(new ConfigEntry(
                            "share.auto.offset.reset", "earliest"),
                            AlterConfigOp.OpType.SET))))
                    .all()
                    .toCompletionStage()
                    .toCompletableFuture()
                    .join();
            return true;
        });

        try {
            List<String> topics = consumerRequest.topics
                    .stream()
                    .map(NewTopic::name)
                    .toList();

            if (consumerRequest.groupId.isEmpty()) {
                throw new IllegalStateException("groupId is required for share consumers");
            } else {
                response.consumer.subscribe(topics);
            }

            if (consumerRequest.consumeMessages < 1 && consumerRequest.messagesPerTopic < 1) {
                var records = response.consumer.poll(Duration.ofSeconds(5));
                records.forEach(response.records::add);
            } else {
                int pollCount = 0;
                int fetchCount = consumerRequest.consumeMessages > 0 ?
                    consumerRequest.consumeMessages :
                    (consumerRequest.messagesPerTopic * consumerRequest.topics.size());

                while (response.records.size() < fetchCount && pollCount++ < 10) {
                    var records = response.consumer.poll(Duration.ofSeconds(1));
                    records.forEach(response.records::add);
                    log.debugf("Subscription polled: %s ; total messages received: %d", response.consumer.subscription(), response.records.size());
                }
            }

            response.consumer.commitSync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
