package com.github.streamshub.console.api.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.NotFoundException;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.QuorumInfo;
import org.apache.kafka.common.config.ConfigResource;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.model.ConfigEntry;
import com.github.streamshub.console.api.model.Metrics;
import com.github.streamshub.console.api.model.Metrics.ValueMetric;
import com.github.streamshub.console.api.model.Node;
import com.github.streamshub.console.api.model.Node.BrokerStatus;
import com.github.streamshub.console.api.model.Node.ControllerStatus;
import com.github.streamshub.console.api.model.Node.MetadataStatus;
import com.github.streamshub.console.api.model.NodeSummary;
import com.github.streamshub.console.api.security.PermissionService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.MetadataQuorumSupport;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.ResourceTypes;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaClusterSpec;
import io.strimzi.api.kafka.model.kafka.KafkaSpec;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;

import static com.github.streamshub.console.api.BlockingSupplier.get;

@ApplicationScoped
public class NodeService {

    @Inject
    Logger logger;

    @Inject
    KubernetesClient k8s;

    @Inject
    ConfigService configService;

    @Inject
    MetricsService metricsService;

    @Inject
    ConsoleConfig consoleConfig;

    /**
     * Kafka context for a single-Kafka request.
     * E.g. {@linkplain #describeCluster(List) describeCluster}.
     */
    @Inject
    KafkaContext kafkaContext;

    @Inject
    PermissionService permissionService;

    @Inject
    ThreadContext threadContext;

    @Inject
    @Named("KafkaNodePools")
    // Keys: namespace -> cluster name -> pool name
    Map<String, Map<String, Map<String, KafkaNodePool>>> nodePools;

    /**
     * Used to model the roles assigned to each pool in
     * {@linkplain KafkaClusterService#nodePools nodePools}.
     */
    private record PoolRoles(String poolName, Set<Node.Role> roles) {
        static final PoolRoles BROKER = new PoolRoles(null, Set.of(Node.Role.BROKER));
        static final PoolRoles CONTROLLER = new PoolRoles(null, Set.of(Node.Role.CONTROLLER));
    }

    public CompletionStage<List<Node>> listNodes(ListRequestContext<Node> listSupport) {
        var summary = new NodeSummary();
        listSupport.meta().put("summary", summary);

        return listNodes()
            .thenApply(nodes -> nodes.stream()
                    .filter(permissionService.permitted(ResourceTypes.Kafka.NODES, Privilege.LIST, Node::getId))
                    .map(node -> tallySummary(node, summary))
                    .filter(listSupport.filter(Node.class))
                    .map(listSupport::tally)
                    .filter(listSupport::betweenCursors)
                    .sorted(listSupport.getSortComparator())
                    .dropWhile(listSupport::beforePageBegin)
                    .takeWhile(listSupport::pageCapacityAvailable)
                    .map(permissionService.addPrivileges(ResourceTypes.Kafka.NODES, Node::getId))
                    .toList());
    }

    CompletionStage<List<Node>> listNodes() {
        Admin adminClient = kafkaContext.admin();
        var clusterResult = adminClient.describeCluster();
        var quorumResult = MetadataQuorumSupport.quorumInfo(adminClient.describeMetadataQuorum());

        return CompletableFuture.allOf(clusterResult.nodes().toCompletionStage().toCompletableFuture(), quorumResult)
            .thenComposeAsync(nothing -> getNodes(clusterResult, quorumResult), threadContext.currentContextExecutor());
    }

    public CompletionStage<Map<String, ConfigEntry>> describeConfigs(String nodeId) {
        return kafkaContext.admin().describeCluster().nodes()
            .thenApply(nodes -> {
                if (nodes.stream().map(n -> String.valueOf(n.id())).noneMatch(nodeId::equals)) {
                    throw new NotFoundException("No such node: " + nodeId);
                }
                return null;
            })
            .toCompletionStage()
            .thenComposeAsync(
                    nothing -> configService.describeConfigs(ConfigResource.Type.BROKER, nodeId),
                    threadContext.currentContextExecutor());
    }

    NodeSummary summarize(List<Node> nodes) {
        var summary = new NodeSummary();
        nodes.forEach(node -> tallySummary(node, summary));
        return summary;
    }

    private Node tallySummary(Node node, NodeSummary summary) {
        boolean healthy = true;
        BiFunction<String, Integer, Integer> accumulator = (k, v) -> v == null ? 1 : v + 1;

        if (node.isBroker()) {
            var status = node.broker().status();
            if (status != BrokerStatus.RUNNING) {
                healthy = false;
            }
            summary.getStatuses().brokers().compute(status.toString(), accumulator);
        }

        if (node.isController()) {
            var status = node.controller().status();
            if (status == ControllerStatus.FOLLOWER_LAGGED) {
                healthy = false;
            }
            summary.getStatuses().controllers().compute(status.toString(), accumulator);

            if (status == ControllerStatus.LEADER) {
                summary.setLeaderId(node.getId());
            }
        }

        String combinedStatusKey = healthy ? "Healthy" : "Unhealthy";
        summary.getStatuses().combined().compute(combinedStatusKey, accumulator);

        String poolName = node.nodePool();

        if (poolName != null) {
            var pool = summary.nodePool(poolName);
            pool.roles().addAll(node.roles());
            pool.count().getAndIncrement();
        }

        return node;
    }

    CompletionStage<List<Node>> getNodes(DescribeClusterResult clusterResult, CompletableFuture<QuorumInfo> quorumResult) {
        Map<Integer, Node.MetadataState> metadataStates = getMetadataStates(quorumResult.join());
        var valueMetrics = getClusterValueMetrics();
        var podPromise = getClusterPods();
        Map<Integer, PoolRoles> nodePoolRoles = nodePoolRoles();
        Map<Integer, Node> nodes = new TreeMap<>();

        for (var node : get(clusterResult::nodes)) {
            /*
             * The nodes array from the describe cluster operation gives a list of broker nodes. If
             * no Strimzi KafkaNodePool is present for this node, we can at least determine that
             * this node has the broker role.
             */
            int nodeId = node.id();
            PoolRoles poolRoles = nodePoolRoles.getOrDefault(node.id(), PoolRoles.BROKER);

            nodes.put(nodeId, Node.fromKafkaModel(
                    node,
                    poolRoles.poolName(),
                    poolRoles.roles(),
                    metadataStates.get(nodeId)));
        }

        metadataStates.forEach((nodeId, state) -> {
            PoolRoles poolRoles = nodePoolRoles.get(nodeId);

            if (poolRoles == null) {
                poolRoles = state.status() == MetadataStatus.OBSERVER ? PoolRoles.BROKER : PoolRoles.CONTROLLER;
            }

            if (nodes.containsKey(nodeId)) {
                // This node is both a controller and a broker
                nodes.get(nodeId).roles().addAll(poolRoles.roles());
            } else {
                /*
                 * This node is probably a controller only. An exception is when the node is offline and
                 * the describe cluster operation did not return an entry for this node, but we determine
                 * from the Strimzi KafkaNodePool that it is a dual role node.
                 */
                nodes.put(nodeId, Node.fromMetadataState(
                        String.valueOf(nodeId),
                        poolRoles.poolName(),
                        poolRoles.roles(),
                        state));
            }
        });

        /*
         * Initialize all broker nodes' state to unknown status. We will attempt obtain the correct status
         * from metrics, the pod status, or assumed from the nodes list from describeCluster.
         */
        var initialBrokerState = new Node.Attributes.Broker(BrokerStatus.UNKNOWN, 0, 0);
        nodes.values().stream().filter(Node::isBroker).forEach(n -> n.broker(initialBrokerState));

        return valueMetrics
                .thenCombine(podPromise, (metrics, pods) -> includeMetricsAndPods(nodes, metrics, pods))
                .thenApplyAsync(this::finalizeStatuses, threadContext.currentContextExecutor())
                .thenApply(ArrayList::new);
    }

    private Map<Integer, Node.MetadataState> getMetadataStates(QuorumInfo quorum) {
        Map<Integer, Node.MetadataState> metadataStates;

        if (quorum != null) {
            metadataStates = new IdentityHashMap<>();
            /*
             * Cluster is running in KRaft mode and we can obtain the metadate quorum information
             * that tells us which nodes are controllers (voters) and which nodes are exclusively
             * brokers (observers). A controller may be either the leader or a follower. Note, that
             * we cannot know from the metadata quorum information which nodes have a dual role of
             * both controller and broker.
             */
            int leaderId = quorum.leaderId();
            var leader = quorum.voters().stream().filter(r -> r.replicaId() == leaderId).findFirst().orElseThrow();

            for (var r : quorum.voters()) {
                int id = r.replicaId();
                long logEndOffset = r.logEndOffset();
                var status = (id == leaderId) ? Node.MetadataStatus.LEADER : Node.MetadataStatus.FOLLOWER;

                metadataStates.put(id, new Node.MetadataState(
                        status,
                        logEndOffset,
                        instantOf(r.lastFetchTimestamp()),
                        instantOf(r.lastCaughtUpTimestamp()),
                        // lag
                        leader.logEndOffset() - logEndOffset,
                        // timeLag
                        leader.lastCaughtUpTimestamp().orElse(-1) - r.lastCaughtUpTimestamp().orElse(-1)));
            }

            for (var r : quorum.observers()) {
                int id = r.replicaId();
                long logEndOffset = r.logEndOffset();

                metadataStates.put(id, new Node.MetadataState(
                        Node.MetadataStatus.OBSERVER,
                        logEndOffset,
                        instantOf(r.lastFetchTimestamp()),
                        instantOf(r.lastCaughtUpTimestamp()),
                        // lag
                        leader.logEndOffset() - logEndOffset,
                        // timeLag
                        leader.lastCaughtUpTimestamp().orElse(-1) - r.lastCaughtUpTimestamp().orElse(-1)));
            }
        } else {
            metadataStates = Collections.emptyMap();
        }

        return metadataStates;
    }

    private static Instant instantOf(OptionalLong millis) {
        return millis.stream().mapToObj(Instant::ofEpochMilli).findFirst().orElse(null);
    }

    /**
     * For the current Kafka cluster in request context, build a map keyed by node
     * ID where the value is a {@link PoolRoles} record which holds the name of the
     * node pool for that node and the roles assigned for the pool.
     */
    private Map<Integer, PoolRoles> nodePoolRoles() {
        return Optional.ofNullable(kafkaContext.resource())
                .map(Kafka::getMetadata)
                .flatMap(kafkaMeta -> Optional.ofNullable(nodePools.get(kafkaMeta.getNamespace()))
                        .map(clustersInNamespace -> clustersInNamespace.get(kafkaMeta.getName()))
                        .map(Map::values))
                .orElseGet(Collections::emptyList)
                .stream()
                .flatMap(nodePool -> nodePool.getStatus().getNodeIds().stream().map(nodeId -> {
                    Map.Entry<Integer, PoolRoles> entry;
                    entry = Map.entry(
                            nodeId,
                            new PoolRoles(
                                    nodePool.getMetadata().getName(),
                                    nodePool.getSpec().getRoles().stream().map(r -> Node.Role.valueOf(r.name())).collect(Collectors.toSet())));
                    return entry;
                }))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    CompletionStage<Map<String, Pod>> getClusterPods() {
        return Optional.ofNullable(kafkaContext.resource()).map(Kafka::getMetadata).map(kafkaMeta -> {
            String namespace = kafkaMeta.getNamespace();
            String name = kafkaMeta.getName();

            return CompletableFuture.supplyAsync(() -> k8s.resources(Pod.class)
                    .inNamespace(namespace)
                    .withLabels(Map.of(
                            ResourceLabels.STRIMZI_CLUSTER_LABEL, name,
                            ResourceLabels.STRIMZI_COMPONENT_TYPE_LABEL, "kafka"
                    ))
                    .list()
                    .getItems())
                .thenApply(pods -> pods.stream()
                        .collect(Collectors.toMap(
                                pod -> {
                                    String podName = pod.getMetadata().getName();
                                    int index = podName.lastIndexOf("-");
                                    return podName.substring(index + 1);
                                },
                                Function.identity())));
        }).orElseGet(() -> CompletableFuture.completedFuture(Collections.<String, Pod>emptyMap()));
    }

    CompletionStage<Map<String, List<ValueMetric>>> getClusterValueMetrics() {
        if (kafkaContext.prometheus() == null) {
            logger.debugf("Kafka cluster metrics are not available due to missing metrics source configuration");
            return CompletableFuture.completedStage(Collections.<String, List<ValueMetric>>emptyMap());
        }

        var clusterConfig = kafkaContext.clusterConfig();
        String namespace = clusterConfig.getNamespace();
        String name = clusterConfig.getName();
        String valueQuery;

        try (var valuesStream = getClass().getResourceAsStream("/metrics/queries/kafkaCluster_values.promql")) {
            valueQuery = new String(valuesStream.readAllBytes(), StandardCharsets.UTF_8)
                    .formatted(namespace, name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return metricsService.queryValues(valueQuery);
    }

    Collection<Node> includeMetricsAndPods(
            Map<Integer, Node> nodes,
            Map<String, List<ValueMetric>> metrics,
            Map<String, Pod> pods) {

        for (var node : nodes.values()) {
            String nodeId = node.getId();

            if (nodeHasMetrics(nodeId, metrics)) {
                includeMetrics(node, metrics);
            }

            if (pods.containsKey(nodeId)) {
                includePodStatus(node, pods.get(nodeId));
            }
        }

        return nodes.values();
    }

    private void includeMetrics(Node node, Map<String, List<ValueMetric>> metrics) {
        String nodeId = node.getId();

        if (node.isBroker()) {
            var brokerState = getMetric(metrics, nodeId, "broker_state", BrokerStatus::fromState, BrokerStatus.UNKNOWN);
            var replicaCount = getMetric(metrics, nodeId, "replica_count", Integer::parseInt, 0);
            var leaderCount = getMetric(metrics, nodeId, "leader_count", Integer::parseInt, 0);
            node.broker(new Node.Attributes.Broker(brokerState, replicaCount, leaderCount));
        }

        node.storage(
            getMetric(metrics, nodeId, "volume_stats_used_bytes", Long::valueOf, null),
            getMetric(metrics, nodeId, "volume_stats_capacity_bytes", Long::valueOf, null)
        );
    }

    private void includePodStatus(Node node, Pod nodePod) {
        Optional.ofNullable(nodePod.getMetadata())
            .map(ObjectMeta::getAnnotations)
            .map(annotations -> annotations.get(ResourceAnnotations.STRIMZI_DOMAIN + "kafka-version"))
            .ifPresent(node::kafkaVersion);

        if (node.isBroker() && node.broker().status() == BrokerStatus.UNKNOWN) {
            var podStatus = nodePod.getStatus();
            String podPhase = podStatus.getPhase();

            if ("Running".equals(podPhase)) {
                boolean podReady = Readiness.isPodReady(nodePod);
                node.broker(node.broker().status(podReady ? BrokerStatus.RUNNING : BrokerStatus.STARTING));
            } else if (!"Unknown".equals(podPhase)) {
                node.broker(node.broker().status(BrokerStatus.NOT_RUNNING));
            }
        }
    }

    private Collection<Node> finalizeStatuses(Collection<Node> nodes) {
        for (var node : nodes) {
            if (node.isBroker() && node.broker().status() == BrokerStatus.UNKNOWN) {
                if (node.isAddressable()) {
                    node.broker(node.broker().status(BrokerStatus.RUNNING));
                } else {
                    node.broker(node.broker().status(BrokerStatus.NOT_RUNNING));
                }
            }

            if (node.isController()) {
                ControllerStatus controllerStatus;
                if (node.isQuorumLeader()) {
                    controllerStatus = ControllerStatus.LEADER;
                } else if (node.hasLag(getQuorumFetchTimeout())) {
                    controllerStatus = ControllerStatus.FOLLOWER_LAGGED;
                } else {
                    controllerStatus = ControllerStatus.FOLLOWER;
                }
                node.controller(new Node.Attributes.Controller(controllerStatus));
            }
        }

        return nodes;
    }

    /**
     * Obtain the value of the `controller.quorum.fetch.timeout.ms` configuration from
     * the Kafka CR, or default to 2s if not available.
     */
    private long getQuorumFetchTimeout() {
        return Optional.ofNullable(kafkaContext.resource())
            .map(Kafka::getSpec)
            .map(KafkaSpec::getKafka)
            .map(KafkaClusterSpec::getConfig)
            .map(config -> config.get("controller.quorum.fetch.timeout.ms"))
            .map(String::valueOf)
            .map(value -> {
                try {
                    return Long.parseLong(value);
                } catch (Exception e) {
                    logger.debugf("Value of controller.quorum.fetch.timeout.ms could not be parsed as a long: %s", value);
                    // Ignore the un-parseable value and use the default.
                    return null;
                }
            })
            .orElse(2000L);
    }

    private static boolean nodeHasMetrics(String nodeId, Map<String, List<ValueMetric>> metrics) {
        for (var metricList : metrics.values()) {
            for (var metric : metricList) {
                if (nodeId.equals(metric.attributes().get("nodeId"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static <T> T getMetric(
            Map<String, List<ValueMetric>> metrics,
            String nodeId,
            String metricName,
            Function<String, T> map,
            T defaultValue) {
        String metricValue = Optional.ofNullable(metrics.get(metricName))
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(m -> nodeId.equals(m.attributes().get("nodeId")))
                .findFirst()
                .map(ValueMetric::value)
                .orElse(null);
        return metricValue != null ? map.apply(metricValue) : defaultValue;
    }

    private <M extends Metrics.Metric> void extractNodeMetrics(
        String nodeId,
        Map<String, List<M>> allMetrics,
        Map<String, List<M>> nodeMetrics) {

        allMetrics.forEach((metricName, dataList) -> {
            var filtered = dataList.stream()
                .filter(m -> nodeId.equals(m.attributes().get("nodeId")))
                .toList();

            if (!filtered.isEmpty()) {
                nodeMetrics.put(metricName, filtered);
            }
        });
    }
    
    public CompletionStage<Metrics> getNodeMetrics(String nodeId, int durationMinutes) {
        if (kafkaContext.prometheus() == null) {
            logger.warnf("Metrics requested for node %s, but Prometheus is not configured", nodeId);
            return CompletableFuture.completedStage(new Metrics());
        }

        var clusterConfig = kafkaContext.clusterConfig();
        String namespace = clusterConfig.getNamespace();
        String name = clusterConfig.getName();

        String rawRangeQuery;
        String valueQuery;

    
        try (
            var rangesStream = getClass().getResourceAsStream("/metrics/queries/kafkaCluster_ranges.promql");
            var valuesStream = getClass().getResourceAsStream("/metrics/queries/kafkaCluster_values.promql")
        ) {
            rawRangeQuery = new String(rangesStream.readAllBytes(), StandardCharsets.UTF_8)
                    .formatted(namespace, name);
            valueQuery = new String(valuesStream.readAllBytes(), StandardCharsets.UTF_8)
                    .formatted(namespace, name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    
        String promInterval = "5m";
        if (durationMinutes >= 1440) promInterval = "30m"; 
        if (durationMinutes >= 10080) promInterval = "2h"; 

        final String finalizedQuery = rawRangeQuery.replace("[5m]", "[" + promInterval + "]");

        logger.debugf("Executing PromQL: %s", finalizedQuery);

        Metrics nodeMetrics = new Metrics();
        var rangeFuture = metricsService.queryRanges(finalizedQuery, durationMinutes).toCompletableFuture();
        var valueFuture = metricsService.queryValues(valueQuery).toCompletableFuture();

        return CompletableFuture.allOf(rangeFuture, valueFuture)
                .thenApply(nothing -> {
                    extractNodeMetrics(nodeId, rangeFuture.join(), nodeMetrics.ranges());
                    extractNodeMetrics(nodeId, valueFuture.join(), nodeMetrics.values());
                    return nodeMetrics;
                });
    }
}
