package com.github.streamshub.console.api;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.DescribeMetadataQuorumOptions;
import org.apache.kafka.clients.admin.DescribeMetadataQuorumResult;
import org.apache.kafka.clients.admin.QuorumInfo;
import org.apache.kafka.common.KafkaFuture;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.github.streamshub.console.api.service.MetricsService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.PrometheusConfig;
import com.github.streamshub.console.config.PrometheusConfig.Type;
import com.github.streamshub.console.config.ValueBuilder;
import com.github.streamshub.console.config.authentication.AuthenticationConfigBuilder;
import com.github.streamshub.console.config.authentication.Basic;
import com.github.streamshub.console.config.security.GlobalSecurityConfigBuilder;
import com.github.streamshub.console.config.security.KafkaSecurityConfigBuilder;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.kafka.systemtest.utils.TokenUtils;
import com.github.streamshub.console.test.AdminClientSpy;
import com.github.streamshub.console.test.MockHelper;
import com.github.streamshub.console.test.TestHelper;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePoolBuilder;
import io.strimzi.api.kafka.model.nodepool.ProcessRoles;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(NodesResource.class)
@TestProfile(TestPlainProfile.class)
class NodesResourceIT implements ClientRequestFilter {

    static final JsonObject EMPTY_METRICS = Json.createObjectBuilder()
        .add("data", Json.createObjectBuilder()
                .add("result", Json.createArrayBuilder()))
        .build();

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    MetricsService metricsService;

    @Inject
    Map<String, KafkaContext> configuredContexts;

    TestHelper utils;

    final String clusterName1 = "test-kafka1";
    final String clusterNamespace1 = "default";
    String clusterId;
    URI bootstrapServers;

    Consumer<ClientRequestContext> filterQuery;
    final Consumer<ClientRequestContext> filterQueryRange = ctx -> { /* No-op */ };

    @Override
    public void filter(ClientRequestContext requestContext) throws IOException {
        var requestUri = requestContext.getUri();

        if (requestUri.getPath().endsWith("query")) {
            filterQuery.accept(requestContext);
        } else if (requestUri.getPath().endsWith("query_range")) {
            filterQueryRange.accept(requestContext);
        }
    }

    @BeforeEach
    void setup() {
        metricsService.setAdditionalFilter(Optional.of(this));

        /*
         * Create a mock Prometheus configuration and point test-kafka1 to use it. A client
         * will be created when the Kafka CR is discovered. The request filter mock created
         * above is our way to intercept outbound requests and abort them with the desired
         * response for each test.
         */
        var prometheusConfig = new PrometheusConfig();
        prometheusConfig.setName("test");
        prometheusConfig.setType(Type.fromValue("standalone"));
        prometheusConfig.setUrl("http://prometheus.example.com");

        var authN = new Basic();
        authN.setUsername("pr0m3th3u5");
        authN.setPassword(new ValueBuilder().withValue("password42").build());
        prometheusConfig.setAuthentication(new AuthenticationConfigBuilder()
            .withBasic(authN)
            .build());

        consoleConfig.setMetricsSources(List.of(prometheusConfig));
        consoleConfig.getKafka().getCluster(clusterNamespace1 + '/' + clusterName1).get().setMetricsSource("test");

        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);

        client.resources(Kafka.class).inAnyNamespace().delete();
        client.resources(KafkaNodePool.class).inAnyNamespace().delete();
        client.resources(Pod.class).inAnyNamespace().delete();

        utils.apply(client, new KafkaBuilder()
                .withNewMetadata()
                    .withName(clusterName1)
                    .withNamespace(clusterNamespace1)
                .endMetadata()
                .withNewSpec()
                    .withNewKafka()
                        .addNewListener()
                            .withName("listener0")
                            .withType(KafkaListenerType.NODEPORT)
                        .endListener()
                    .endKafka()
                .endSpec()
                .withNewStatus()
                    .withClusterId(utils.getClusterId())
                    .addNewListener()
                        .withName("listener0")
                        .addNewAddress()
                            .withHost(bootstrapServers.getHost())
                            .withPort(bootstrapServers.getPort())
                        .endAddress()
                    .endListener()
                .endStatus()
                .build());

        clusterId = consoleConfig.getKafka().getCluster(clusterNamespace1 + '/' + clusterName1).get().getId();

        /*
         * Below sets up several mocked nodes to support node listing tests
         */
        record NodeProperties(
                int id,
                String rack,
                boolean address,
                boolean nodePool,
                String podPhase,
                String podReadyStatus,
                String brokerStatus,
                String controllerStatus
        ) {
            public boolean isSingleRole() {
                return brokerStatus == null || controllerStatus == null;
            }

            public boolean isDualRole() {
                return !isSingleRole();
            }
        }

        var nodeProps = List.of(
            // Brokers
            new NodeProperties(0, "az1", true, true, "Running", "True", "Running", null),
            new NodeProperties(1, "az2", false, true, "Running", "False", "Starting", null),
            new NodeProperties(2, "az3", true, true, "Pending", null, "NotRunning", null),
            // Controllers
            new NodeProperties(3, "az1", false, true, "Running", "True", null, "QuorumLeader"),
            new NodeProperties(4, "az2", false, true, "Running", "False", null, "QuorumFollower"),
            new NodeProperties(5, "az3", false, true, "Unknown", null, null, "QuorumFollower"),
            // Dual role
            new NodeProperties(6, "az1", false, true, "Running", "True", "Running", "QuorumFollowerLagged"),
            new NodeProperties(7, "az2", false, true, "Unknown", null, "NotRunning", "QuorumFollower"),
            new NodeProperties(8, "az3", true, true, "Unknown", null, "Running", "QuorumFollower"),
            // Not a member of a node pool
            new NodeProperties(9, "az1", false, false, "Running", "True", null, "QuorumFollower"),
            new NodeProperties(10, "az2", true, false, "Running", "True", "Running", null)
        );

        var clusterResult = Mockito.mock(DescribeClusterResult.class);
        var nodes = nodeProps.stream().filter(n -> n.brokerStatus() != null).map(n -> {
            return new org.apache.kafka.common.Node(
                n.id(),
                n.address() ? "node-%d.example.com".formatted(n.id()) : null,
                n.address() ? 9092 : -1,
                n.rack()
            );
        }).toList();
        when(clusterResult.nodes()).thenReturn(KafkaFuture.completedFuture(nodes));

        var now = OptionalLong.of(System.currentTimeMillis());
        var beforeTimeout = OptionalLong.of(System.currentTimeMillis() - 2000);
        var quorumResult = Mockito.mock(DescribeMetadataQuorumResult.class);
        var quorumInfo = MockHelper.mockAll(QuorumInfo.class, Map.of(
                QuorumInfo::leaderId, nodeProps.stream()
                    .filter(n -> "QuorumLeader".equals(n.controllerStatus()))
                    .findFirst()
                    .orElseThrow()
                    .id(),

                QuorumInfo::voters, nodeProps.stream()
                    .filter(n -> n.controllerStatus() != null)
                    .map(n -> MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                        QuorumInfo.ReplicaState::replicaId, n.id(),
                        QuorumInfo.ReplicaState::logEndOffset, 1000L - n.id(), // for some variety
                        QuorumInfo.ReplicaState::lastFetchTimestamp, n.controllerStatus().contains("Lagged") ? beforeTimeout : now,
                        QuorumInfo.ReplicaState::lastCaughtUpTimestamp, n.controllerStatus().contains("Lagged") ? beforeTimeout : now
                    )))
                    .toList(),

                QuorumInfo::observers, nodeProps.stream()
                    .filter(n -> n.brokerStatus() != null)
                    .filter(n -> n.controllerStatus() == null)
                    .map(n -> MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                        QuorumInfo.ReplicaState::replicaId, n.id(),
                        QuorumInfo.ReplicaState::logEndOffset, 1000L
                    )))
                    .toList()
        ));
        when(quorumResult.quorumInfo()).thenReturn(KafkaFuture.completedFuture(quorumInfo));

        AdminClientSpy.install(adminClient -> {
            doReturn(clusterResult)
                .when(adminClient)
                .describeCluster(any(DescribeClusterOptions.class));
            doReturn(quorumResult)
                .when(adminClient)
                .describeMetadataQuorum(any(DescribeMetadataQuorumOptions.class));
        });

        utils.apply(client, new ServiceAccountBuilder()
                .withNewMetadata()
                    .withNamespace(clusterNamespace1)
                    .withName("default")
                .endMetadata()
                .build());

        nodeProps.stream().forEach(n -> {
            utils.apply(client, new PodBuilder()
                    .withNewMetadata()
                        .withNamespace(clusterNamespace1)
                        .withName(clusterId + "-" + n.id())
                        .addToLabels(Map.of(
                            ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName1,
                            ResourceLabels.STRIMZI_COMPONENT_TYPE_LABEL, "kafka"
                        ))
                        .addToAnnotations(ResourceAnnotations.STRIMZI_DOMAIN + "kafka-version", "3.9.0")
                    .endMetadata()
                    .withNewSpec()
                        .addNewContainer()
                            .withName("kafka")
                            .withImage("dummy")
                        .endContainer()
                    .endSpec()
                    .withNewStatus()
                        .withPhase(n.podPhase())
                        .addNewCondition()
                            .withType("Ready")
                            .withStatus(n.podReadyStatus())
                        .endCondition()
                    .endStatus()
                    .build());
        });

        utils.apply(client, new KafkaNodePoolBuilder()
                .withNewMetadata()
                    .withNamespace(clusterNamespace1)
                    .withName(clusterId + "-brokers")
                    .addToLabels(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName1)
                .endMetadata()
                .withNewSpec()
                    .withRoles(ProcessRoles.BROKER)
                .endSpec()
                .withNewStatus()
                    .withNodeIds(nodeProps.stream()
                            .filter(NodeProperties::nodePool)
                            .filter(NodeProperties::isSingleRole)
                            .filter(n -> n.brokerStatus() != null)
                            .map(NodeProperties::id)
                            .toList())
                .endStatus()
                .build());

        utils.apply(client, new KafkaNodePoolBuilder()
                .withNewMetadata()
                    .withNamespace(clusterNamespace1)
                    .withName(clusterId + "-controllers")
                    .addToLabels(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName1)
                .endMetadata()
                .withNewSpec()
                    .withRoles(ProcessRoles.CONTROLLER)
                .endSpec()
                .withNewStatus()
                    .withNodeIds(nodeProps.stream()
                            .filter(NodeProperties::nodePool)
                            .filter(NodeProperties::isSingleRole)
                            .filter(n -> n.controllerStatus() != null)
                            .map(NodeProperties::id)
                            .toList())
                .endStatus()
                .build());

        utils.apply(client, new KafkaNodePoolBuilder()
                .withNewMetadata()
                    .withNamespace(clusterNamespace1)
                    .withName(clusterId + "-dual")
                    .addToLabels(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName1)
                .endMetadata()
                .withNewSpec()
                    .withRoles(ProcessRoles.CONTROLLER, ProcessRoles.BROKER)
                .endSpec()
                .withNewStatus()
                    .withNodeIds(nodeProps.stream()
                            .filter(NodeProperties::nodePool)
                            .filter(NodeProperties::isDualRole)
                            .map(NodeProperties::id)
                            .toList())
                .endStatus()
                .build());

        // Generate metrics only for node 10
        filterQuery = ctx -> {
            ctx.abortWith(Response.ok(Json.createObjectBuilder()
                    .add("data", Json.createObjectBuilder()
                        .add("result", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                .add("metric", Json.createObjectBuilder()
                                    .add(MetricsService.METRIC_NAME, "broker_state")
                                    .add("nodeId", "10"))
                                .add("value", Json.createArrayBuilder()
                                    .add(Instant.now().toEpochMilli() / 1000f)
                                    .add("3"))))) // Running
                    .build())
                .build());
        };
    }

    @Test
    void testListNodesWithDualRoles() {
        whenRequesting(req -> req
                .param("filter[nodePool]", "in," + clusterId + "-dual")
                .get("", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.summary.leaderId", is("3"))
            .body("data.size()", is(3))
            .body("data.attributes.findAll { it }.collect { it.roles }", everyItem(contains("controller", "broker")));
    }

    @Test
    void testListNodesWithSplitRoles() {
        whenRequesting(req -> req
                .param("filter[nodePool]", "in," + clusterId + "-controllers," + clusterId + "-brokers")
                .get("", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.summary.leaderId", is("3"))
            .body("data.size()", is(6))
            .body("data.findAll { it.id <= '2' }.attributes.collect { it.roles }", everyItem(contains("broker")))
            .body("data.findAll { it.id >= '3' && it.id <= '5' }.attributes.collect { it.roles }", everyItem(contains("controller")));
    }

    @Test
    void testListNodesAllPools() {
        whenRequesting(req -> req
                .param("page[size]", "20")
                .get("", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.summary.leaderId", is("3"))
            .body("data.size()", is(11))
            .body("data.findAll { it.id as Integer <= 2 }.attributes.collect { it.nodePool }",
                    everyItem(is(clusterId + "-brokers")))
            .body("data.findAll { it.id as Integer >= 3 && it.id as Integer <= 5 }.attributes.collect { it.nodePool }",
                    everyItem(is(clusterId + "-controllers")))
            .body("data.findAll { it.id as Integer >= 6 && it.id as Integer <= 8 }.attributes.collect { it.nodePool }",
                    everyItem(is(clusterId + "-dual")))
            .body("data.findAll { it.id as Integer >= 9 }.attributes.collect { it.nodePool }",
                    everyItem(is(nullValue())));
    }

    @ParameterizedTest
    @ValueSource(strings = { "broker", "controller" })
    void testListNodesByRole(String role) {
        whenRequesting(req -> req
                .param("filter[roles]", "in," + role)
                .get("", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.summary.leaderId", is("3"))
            .body("data.size()", is(7))
            .body("data.attributes.collect { it.roles }", everyItem(hasItem(role)));
    }

    static Stream<Arguments> testListNodesByStatusSource() {
        return Stream.of(
            Arguments.of(List.of("Running"), Collections.emptyList(), List.of(0, 6, 8, 10)),
            Arguments.of(List.of("NotRunning"), Collections.emptyList(), List.of(2, 7)),
            Arguments.of(List.of("Running"), List.of("QuorumLeader"), List.of(0, 3, 6, 8, 10)),
            Arguments.of(Collections.emptyList(), List.of("QuorumLeader", "QuorumFollowerLagged"), List.of(3, 6))
        );
    }

    @ParameterizedTest
    @MethodSource("testListNodesByStatusSource")
    void testListNodesByStatus(List<String> brokerStatuses, List<String> controllerStatuses, List<Integer> nodeIds) {
        whenRequesting(req -> {
            if (!brokerStatuses.isEmpty()) {
                req = req.param("filter[broker.status]", "in," + String.join(",", brokerStatuses));
            }

            if (!controllerStatuses.isEmpty()) {
                req = req.param("filter[controller.status]", "in," + String.join(",", controllerStatuses));
            }

            return req.get("", clusterId);
        }).assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.summary.leaderId", is("3"))
            .body("data.id", contains(nodeIds.stream().map(String::valueOf).toArray(String[]::new)));
    }

    static Stream<Arguments> testListNodesWithSortSource() {
        return Stream.of(
            Arguments.of("roles,-id,nodePool,rack", List.of(9, 5, 4, 3, 8, 7, 6, 10, 2, 1, 0))
        );
    }

    @ParameterizedTest
    @MethodSource("testListNodesWithSortSource")
    void testListNodesWithSort(String sort, List<Integer> nodeIds) {
        final int pageSize = 6;

        var fullResponse = whenRequesting(req -> req
                    .param("sort", sort)
                    .param("page[size]", pageSize)
                    .get("", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.summary.leaderId", is("3"))
            .body("meta.page.total", is(nodeIds.size()))
            .body("data.size()", is(pageSize))
            .body("data.meta.page", everyItem(hasKey(equalTo("cursor"))))
            .body("data.id", contains(nodeIds.stream()
                    .limit(pageSize)
                    .map(String::valueOf)
                    .toArray(String[]::new)))
            .extract()
            .asInputStream();

        JsonObject responseJson;

        try (var reader = Json.createReader(fullResponse)) {
            responseJson = reader.readObject();
        }

        Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put("sort", sort);
        parametersMap.put("page[size]", pageSize);
        utils.getCursor(responseJson, pageSize - 1)
            .ifPresent(cursor -> parametersMap.put("page[after]", cursor));

        whenRequesting(req -> req
                .queryParams(parametersMap)
                .get("", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.summary.leaderId", is("3"))
            .body("meta.page.total", is(nodeIds.size())) // total is the count for the full/unpaged result set
            .body("data.size()", is(nodeIds.size() - pageSize))
            .body("data.meta.page", everyItem(hasKey(equalTo("cursor"))))
            .body("data.id", contains(nodeIds.stream()
                    .skip(pageSize)
                    .map(String::valueOf)
                    .toArray(String[]::new)));
    }

    @ParameterizedTest
    @CsvSource({
        "2000, QuorumFollowerLagged",
        "3000, QuorumFollower",
        "NaN,  QuorumFollowerLagged "
    })
    void testListNodesWithVaryingQuorumFetchTimeout(String quorumFetchTime, String nodeStatus6) {
        final String quorumFetchConfigKey = "controller.quorum.fetch.timeout.ms";

        var kafka = client.resources(Kafka.class)
            .inNamespace(clusterNamespace1)
            .withName(clusterName1)
            .edit(k -> new KafkaBuilder(k)
                    .editSpec()
                        .editKafka()
                            .addToConfig(quorumFetchConfigKey, quorumFetchTime)
                        .endKafka()
                    .endSpec()
                    .build());

        // Wait for updated config to reach the context map
        await().atMost(10, TimeUnit.SECONDS).ignoreException(NullPointerException.class)
            .until(() -> configuredContexts.values()
                    .stream()
                    .map(KafkaContext::resource)
                    .filter(Objects::nonNull)
                    .filter(k -> Cache.metaNamespaceKeyFunc(kafka).equals(Cache.metaNamespaceKeyFunc(k)))
                    .allMatch(k -> quorumFetchTime.equals(k.getSpec().getKafka().getConfig().get(quorumFetchConfigKey))));

        whenRequesting(req -> req
                .param("filter[controller.status]", "in," + nodeStatus6)
                .get("", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.summary.leaderId", is("3"))
            .body("data.collect { it.id }", hasItem("6"));
    }

    @Test
    void testListNodesWithLimitedAuthorization() {
        utils.resetSecurity(consoleConfig, true);
        TokenUtils tokens = new TokenUtils(config);

        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewSubject()
                    .withInclude("alice")
                    .withRoleNames("limited-nodes-role")
                .endSubject()
            .build());

        consoleConfig.getKafka().getClusterById(clusterId).ifPresent(cfg -> {
            cfg.setSecurity(new KafkaSecurityConfigBuilder()
                    .addNewRole()
                        .withName("limited-nodes-role")
                        .addNewRule()
                            .withResources("nodes")
                            .withPrivileges(Privilege.LIST)
                            // allowed to list nodes 0, 3, 6, & 9; expressed as RegExp
                            .withResourceNames("/[0369]/")
                        .endRule()
                    .endRole()
                    .build());
        });

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken("alice"))
                .param("sort", "-id")
                .get("", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(4))
            .body("data.id", contains("9", "6", "3", "0"))
            .body("data.meta.privileges", everyItem(is(List.of(Privilege.LIST.name()))));
    }

    @Test
    void testDescribeConfigs() {
        whenRequesting(req -> req.get("{nodeId}/configs", clusterId, "0"))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", not(anEmptyMap()))
            .body("data.attributes.findAll { it }.collect { it.value }",
                    everyItem(allOf(
                            hasKey("source"),
                            hasKey("sensitive"),
                            hasKey("readOnly"),
                            hasKey("type"))));
    }

    @Test
    void testDescribeConfigsNodeNotFound() {
        whenRequesting(req -> req.get("{nodeId}/configs", clusterId, "99"))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", equalTo(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"))
            .body("errors.detail", contains("No such node: 99"));
    }

    @Test
    void testGetNodeMetrics() {
        whenRequesting(req -> req.get("{nodeId}/metrics", clusterId, "10"))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", equalTo("10"))
            .body("data.type", equalTo("nodeMetrics"))
            .body("data.attributes.metrics.values", not(anEmptyMap()))
            .body("data.attributes.metrics.values.broker_state[0].value", equalTo("3"));
    }

    @Test
    void testGetNodeMetrics_PrometheusReturnsNoData() {
        filterQuery = ctx ->
            ctx.abortWith(Response.ok(EMPTY_METRICS).build());

        whenRequesting(req -> req.get("{nodeId}/metrics", clusterId, "10"))
            .assertThat()
            .statusCode(200)
            .body("data.id", equalTo("10"))
            .body("data.attributes.metrics.values", anEmptyMap())
            .body("data.attributes.metrics.ranges", anEmptyMap());
    }

    @Test
    void testGetNodeMetrics_PrometheusNotConfigured() {
        filterQuery = ctx -> ctx.abortWith(Response.ok(EMPTY_METRICS).build());

        consoleConfig.getKafka()
            .getCluster(clusterNamespace1 + '/' + clusterName1)
            .get()
            .setMetricsSource(null);

        whenRequesting(req -> req.get("{nodeId}/metrics", clusterId, "10"))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.metrics.values", anEmptyMap())
            .body("data.attributes.metrics.ranges", anEmptyMap());
    }   
}
