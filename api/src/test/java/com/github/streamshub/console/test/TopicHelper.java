package com.github.streamshub.console.test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.DescribeLogDirsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.LogDirDescription;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.core.ConditionTimeoutException;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.BlockingSupplier;
import com.github.streamshub.console.kafka.systemtest.utils.ClientsConfig;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.fail;

public class TopicHelper {

    private static final Duration READINESS_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READINESS_POLL_INTERVAL = Duration.ofMillis(100);
    private static final int ADMIN_TIMEOUT_MS = 5_000;
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
     * Wait for every partition of the given topics to have an assigned leader
     * and for each leader replica to appear in the leader broker's log
     * directory information. Topic visibility and leader assignment alone do
     * not guarantee that replicas are present in {@code describeLogDirs()}.
     *
     * @param topicNames names of the topics to wait for
     */
    public void awaitTopicLeadersAndStorage(List<String> topicNames) {
        long deadlineNanos = System.nanoTime() + READINESS_TIMEOUT.toNanos();
        Admin admin = null;

        try {
            admin = Admin.create(adminConfig);
            Admin readinessAdmin = admin;

            long remainingNanos = deadlineRemainingNanos(deadlineNanos);
            if (remainingNanos <= 0) {
                fail("Topics did not become ready: readiness deadline exceeded during Admin client creation");
            }

            Duration awaitTimeout = Duration.ofMillis(Math.max(1, TimeUnit.NANOSECONDS.toMillis(remainingNanos)));
            AtomicReference<String> readinessProblem = new AtomicReference<>("readiness check did not run");

            try {
                await().atMost(awaitTimeout)
                    .pollInterval(READINESS_POLL_INTERVAL)
                    .until(() -> {
                        try {
                            String problem = topicReadinessProblem(readinessAdmin, topicNames, deadlineNanos);
                            readinessProblem.set(problem);
                            return problem == null;
                        } catch (Exception e) {
                            if (e instanceof InterruptedException) {
                                Thread.currentThread().interrupt();
                            }
                            readinessProblem.set("readiness check failed: " + e);
                            return false;
                        }
                    });
            } catch (ConditionTimeoutException e) {
                fail("Topics did not become ready: " + readinessProblem.get(), e);
            }
        } catch (Exception e) {
            fail(e);
        } finally {
            if (admin != null) {
                closeAdmin(admin, deadlineNanos);
            }
        }
    }

    private void closeAdmin(Admin admin, long deadlineNanos) {
        long closeTimeoutMillis = Math.max(1, TimeUnit.NANOSECONDS.toMillis(deadlineRemainingNanos(deadlineNanos)));

        try {
            admin.close(Duration.ofMillis(closeTimeoutMillis));
        } catch (Exception e) {
            log.warn("Failed to close readiness Admin client", e);
        }
    }

    private String topicReadinessProblem(Admin admin, List<String> topicNames, long deadlineNanos) throws Exception {
        long remainingNanos = deadlineRemainingNanos(deadlineNanos);
        if (remainingNanos <= 0) {
            return "readiness deadline exceeded";
        }

        Set<String> topicProblems = new HashSet<>();
        Map<String, TopicDescription> descriptions = describeTopics(
                admin, topicNames, deadlineNanos, callTimeoutMillis(remainingNanos), topicProblems);

        if (!topicProblems.isEmpty()) {
            return "topics not ready: " + topicProblems;
        }

        Map<Integer, Set<TopicPartition>> leaderPartitions = new HashMap<>();
        Set<TopicPartition> missingLeaders = new HashSet<>();
        collectLeaderPartitions(topicNames, descriptions, leaderPartitions, missingLeaders);

        if (!missingLeaders.isEmpty()) {
            return "partitions without leaders: " + missingLeaders;
        }

        long logDirsRemainingNanos = deadlineRemainingNanos(deadlineNanos);
        if (logDirsRemainingNanos <= 0) {
            return "readiness deadline exceeded before checking leader log directories";
        }

        Map<Integer, CompletableFuture<Map<String, LogDirDescription>>> pendingLogDirs = describeLogDirs(
                admin, leaderPartitions.keySet(), callTimeoutMillis(logDirsRemainingNanos));
        waitForLogDirs(pendingLogDirs, deadlineRemainingNanos(deadlineNanos));

        Set<String> missingReplicas = missingReplicas(leaderPartitions, pendingLogDirs);
        return missingReplicas.isEmpty() ? null : "leader replica metadata missing: " + missingReplicas;
    }

    private Map<String, TopicDescription> describeTopics(Admin admin, List<String> topicNames, long deadlineNanos,
            int timeoutMs, Set<String> topicProblems) throws InterruptedException {
        Map<String, KafkaFuture<TopicDescription>> topicFutures = admin
            .describeTopics(topicNames, new DescribeTopicsOptions().timeoutMs(timeoutMs))
            .topicNameValues();

        Map<String, TopicDescription> descriptions = new HashMap<>();

        for (String topicName : topicNames) {
            KafkaFuture<TopicDescription> pending = topicFutures.get(topicName);

            if (pending == null) {
                topicProblems.add(topicName + " (no response)");
                continue;
            }

            long topicRemainingNanos = deadlineRemainingNanos(deadlineNanos);
            if (topicRemainingNanos <= 0) {
                topicProblems.add(topicName + " (readiness deadline exceeded)");
                continue;
            }

            try {
                descriptions.put(topicName, pending.get(topicRemainingNanos, TimeUnit.NANOSECONDS));
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                topicProblems.add(topicName + " (" + (cause != null ? cause : e) + ")");
            } catch (TimeoutException e) {
                topicProblems.add(topicName + " (timed out)");
            }
        }

        return descriptions;
    }

    private void collectLeaderPartitions(List<String> topicNames, Map<String, TopicDescription> descriptions,
            Map<Integer, Set<TopicPartition>> leaderPartitions, Set<TopicPartition> missingLeaders) {
        for (String topicName : topicNames) {
            TopicDescription description = descriptions.get(topicName);

            if (description.partitions().isEmpty()) {
                missingLeaders.add(new TopicPartition(topicName, -1));
            }

            for (TopicPartitionInfo partition : description.partitions()) {
                if (partition.leader() == null) {
                    missingLeaders.add(new TopicPartition(topicName, partition.partition()));
                } else {
                    leaderPartitions.computeIfAbsent(partition.leader().id(), id -> new HashSet<>())
                        .add(new TopicPartition(topicName, partition.partition()));
                }
            }
        }
    }

    private Map<Integer, CompletableFuture<Map<String, LogDirDescription>>> describeLogDirs(
            Admin admin, Set<Integer> leaderIds, int timeoutMs) {
        return admin.describeLogDirs(leaderIds, new DescribeLogDirsOptions().timeoutMs(timeoutMs))
            .descriptions()
            .entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getKey,
                    entry -> entry.getValue().toCompletionStage().toCompletableFuture()));
    }

    private void waitForLogDirs(Map<Integer, CompletableFuture<Map<String, LogDirDescription>>> pendingLogDirs,
            long remainingNanos) throws InterruptedException {
        if (pendingLogDirs.isEmpty() || remainingNanos <= 0) {
            return;
        }

        try {
            CompletableFuture.allOf(pendingLogDirs.values().toArray(CompletableFuture[]::new))
                .get(remainingNanos, TimeUnit.NANOSECONDS);
        } catch (ExecutionException | TimeoutException e) {
            // Individual broker failures and timeouts are reported by missingReplicas.
        }
    }

    private Set<String> missingReplicas(Map<Integer, Set<TopicPartition>> leaderPartitions,
            Map<Integer, CompletableFuture<Map<String, LogDirDescription>>> pendingLogDirs) {
        Set<String> missingReplicas = new HashSet<>();

        for (Map.Entry<Integer, Set<TopicPartition>> entry : leaderPartitions.entrySet()) {
            String problem = brokerReplicaProblem(entry.getKey(), entry.getValue(), pendingLogDirs.get(entry.getKey()));

            if (problem != null) {
                missingReplicas.add(problem);
            }
        }

        return missingReplicas;
    }

    private String brokerReplicaProblem(int leaderId, Set<TopicPartition> expectedPartitions,
            CompletableFuture<Map<String, LogDirDescription>> pending) {
        if (pending == null) {
            return "describeLogDirs returned no result for broker " + leaderId + "; expected " + expectedPartitions;
        }

        Map<String, LogDirDescription> nodeLogDirs;

        try {
            nodeLogDirs = pending.getNow(null);
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            return "describeLogDirs failed for broker " + leaderId + "; expected " + expectedPartitions
                    + ": " + (cause != null ? cause : e);
        }

        if (nodeLogDirs == null) {
            return "describeLogDirs did not complete for broker " + leaderId + "; expected " + expectedPartitions;
        }

        Set<TopicPartition> replicas = nodeLogDirs.values()
            .stream()
            .flatMap(description -> description.replicaInfos().keySet().stream())
            .collect(Collectors.toSet());

        Set<TopicPartition> missing = expectedPartitions.stream()
            .filter(partition -> !replicas.contains(partition))
            .collect(Collectors.toSet());

        return missing.isEmpty() ? null : missing + " on broker " + leaderId;
    }

    private static long deadlineRemainingNanos(long deadlineNanos) {
        return deadlineNanos - System.nanoTime();
    }

    private static int callTimeoutMillis(long remainingNanos) {
        return (int) Math.min(ADMIN_TIMEOUT_MS, Math.max(1, TimeUnit.NANOSECONDS.toMillis(remainingNanos)));
    }

    public void createPartitions(String name, int totalPartitions) {
        try (Admin admin = Admin.create(adminConfig)) {
            var result = admin.createPartitions(Map.of(name, NewPartitions.increaseTo(totalPartitions)));
            result.all()
                .toCompletionStage()
                .thenRun(() -> log.infof("Partitions increased to %d for topic %s", totalPartitions, name))
                .toCompletableFuture()
                .get(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Process interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail(e);
        }
    }

    public void alterTopicConfigs(String name, Collection<AlterConfigOp> alteredConfigs) {
        try (Admin admin = Admin.create(adminConfig)) {
            var result = admin.incrementalAlterConfigs(Map.of(new ConfigResource(ConfigResource.Type.TOPIC, name), alteredConfigs));
            result.all()
                .toCompletionStage()
                .thenRun(() -> log.infof("Altered configs for topic %s", name))
                .toCompletableFuture()
                .get(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Process interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail(e);
        }
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
