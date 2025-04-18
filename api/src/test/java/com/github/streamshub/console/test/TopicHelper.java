package com.github.streamshub.console.test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.BlockingSupplier;
import com.github.streamshub.console.kafka.systemtest.utils.ClientsConfig;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

public class TopicHelper {

    static final Logger log = Logger.getLogger(TopicHelper.class);
    final URI bootstrapServers;
    final Config config;
    final Properties adminConfig;

    public TopicHelper(URI bootstrapServers, Config config) {
        this.bootstrapServers = bootstrapServers;
        this.config = config;
        this.adminConfig = ClientsConfig.getAdminConfig(config);

        adminConfig.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers.toString());
    }

    private List<String> allTopics(Admin admin) {
        return admin.listTopics()
            .listings()
            .toCompletionStage()
            .thenApply(topics -> topics.stream().map(TopicListing::name).toList())
            .toCompletableFuture()
            .join();
    }

    public void deleteTopics(String... topicNames) {
        try (Admin admin = Admin.create(adminConfig)) {
            List<String> topics = Arrays.asList(topicNames);
            log.infof("Deleting topics: %s", topics);

            while (!topics.isEmpty()) {
                admin.deleteTopics(topics)
                    .topicNameValues()
                    .entrySet()
                    .stream()
                    .map(e -> {
                        return e.getValue().toCompletionStage().handle((nothing, error) -> {
                            if (error == null || error instanceof UnknownTopicOrPartitionException) {
                                return (Void) null;
                            }

                            log.warnf("Failed to delete topic %s: %s", e.getKey(), error.getMessage());
                            throw new CompletionException(error);
                        }).toCompletableFuture();
                    })
                    .reduce(CompletableFuture::allOf)
                    .orElseGet(() -> CompletableFuture.completedFuture(null))
                    .get(10, TimeUnit.SECONDS);

                topics = allTopics(admin).stream().filter(topics::contains).toList();
            }
        } catch (InterruptedException e) {
            log.warn("Process interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail(e);
        }
    }

    public void deleteAllTopics() {
        // Tests assume a clean slate - remove any existing topics
        try (Admin admin = Admin.create(adminConfig)) {
            List<String> allTopics = allTopics(admin);
            log.infof("Deleting topics: %s", allTopics);

            while (!allTopics.isEmpty()) {
                admin.deleteTopics(allTopics)
                    .topicNameValues()
                    .entrySet()
                    .stream()
                    .map(e -> {
                        return e.getValue().toCompletionStage().handle((nothing, error) -> {
                            if (error == null || error instanceof UnknownTopicOrPartitionException) {
                                return (Void) null;
                            }

                            log.warnf("Failed to delete topic %s: %s", e.getKey(), error.getMessage());
                            throw new CompletionException(error);
                        }).toCompletableFuture();
                    })
                    .reduce(CompletableFuture::allOf)
                    .orElseGet(() -> CompletableFuture.completedFuture(null))
                    .get(10, TimeUnit.SECONDS);

                allTopics = allTopics(admin);
            }
        } catch (InterruptedException e) {
            log.warn("Process interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail(e);
        }
    }

    public Map<String, String> createTopics(List<String> names, int numPartitions) {
        return createTopics(names, numPartitions, null);
    }

    public Map<String, String> createTopics(List<String> names, int numPartitions, Map<String, String> configs) {
        Map<String, String> topicIds = null;

        try (Admin admin = Admin.create(adminConfig)) {
            var result = admin.createTopics(names.stream()
                    .map(name ->  new NewTopic(name, Optional.of(numPartitions), Optional.empty())
                            .configs(configs))
                    .toList());

            result.all()
                .toCompletionStage()
                .thenRun(() -> log.infof("Topics created: %s", names))
                .toCompletableFuture()
                .get(20, TimeUnit.SECONDS);

            topicIds = names.stream().collect(Collectors.toMap(Function.identity(), name -> {
                return BlockingSupplier.get(() -> result.topicId(name)).toString();
            }));

            await().atMost(Duration.ofSeconds(5))
                .until(() -> allTopics(admin).containsAll(names));
        } catch (InterruptedException e) {
            log.warn("Process interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail(e);
        }

        return topicIds;
    }

    /**
     * Delete records in the given topic/partition with an offset before the given
     * offset.
     *
     * @param topicName name of the topic
     * @param partition partition in topic
     * @param offset    offset before which all records will be deleted
     */
    public void deleteRecords(String topicName, int partition, long offset) {
        try (Admin admin = Admin.create(adminConfig)) {
            admin.deleteRecords(Map.of(new TopicPartition(topicName, partition), RecordsToDelete.beforeOffset(offset)))
                .all()
                .get(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Process interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail(e);
        }
    }

    public long getTopicSize(String topicName) {
        return getPartitionSizes(topicName).values().stream().reduce(0L, Long::sum);
    }

    public Map<TopicPartition, Long> getPartitionSizes(String topicName) {
        Map<TopicPartition, Long> result = new HashMap<>();

        try (Admin admin = Admin.create(adminConfig)) {
            admin.describeTopics(List.of(topicName))
                .allTopicNames()
                .toCompletionStage()
                .thenApply(descriptions -> descriptions.get(topicName))
                .thenApply(description -> description.partitions()
                        .stream()
                        .map(TopicPartitionInfo::partition)
                        .map(p -> new TopicPartition(topicName, p))
                        .toList())
                .thenCompose(partitions -> {
                    Map<TopicPartition, OffsetSpec> earliestReq = partitions.stream()
                            .collect(Collectors.toMap(Function.identity(), p -> OffsetSpec.earliest()));
                    Map<TopicPartition, OffsetSpec> latestReq = partitions.stream()
                            .collect(Collectors.toMap(Function.identity(), p -> OffsetSpec.latest()));

                    var earliestPromise = admin.listOffsets(earliestReq)
                        .all()
                        .thenApply(offsets -> offsets.entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().offset())))
                        .toCompletionStage()
                        .toCompletableFuture();

                    var latestPromise = admin.listOffsets(latestReq)
                        .all()
                        .thenApply(offsets -> offsets.entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().offset())))
                        .toCompletionStage()
                        .toCompletableFuture();

                    return CompletableFuture.allOf(earliestPromise, latestPromise)
                        .thenAccept(nothing -> {
                            earliestPromise.join().forEach((partition, earliestOffset) -> {
                                result.put(partition, latestPromise.join().get(partition) - earliestOffset);
                            });
                        });
                })
                .toCompletableFuture()
                .join();
        }


        return result;
    }

    public void produceRecord(String topicName, Integer partition, Instant instant, Map<String, Object> headers, String key, String value) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers.toString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        Long timestamp = Optional.ofNullable(instant).map(Instant::toEpochMilli).orElse(null);

        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            var rec = new ProducerRecord<String, String>(topicName, partition, timestamp, key, value);
            headers.forEach((k, v) -> rec.headers().add(k, v.toString().getBytes(StandardCharsets.UTF_8)));
            producer.send(rec);
        } catch (Exception e) {
            fail(e);
        }
    }
}
