package com.github.streamshub.console.api;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.DescribeMetadataQuorumOptions;
import org.apache.kafka.clients.admin.DescribeMetadataQuorumResult;
import org.apache.kafka.clients.admin.QuorumInfo;
import org.apache.kafka.common.KafkaFuture;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.kafka.systemtest.deployment.DeploymentManager;
import com.github.streamshub.console.test.AdminClientSpy;
import com.github.streamshub.console.test.MockHelper;
import com.github.streamshub.console.test.TestHelper;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
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
import io.strimzi.test.container.StrimziKafkaContainer;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestHTTPEndpoint(NodesResource.class)
@TestProfile(TestPlainProfile.class)
class NodesResourceIT {

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @Inject
    ConsoleConfig consoleConfig;

    @DeploymentManager.InjectDeploymentManager
    DeploymentManager deployments;

    TestHelper utils;

    final String clusterName1 = "test-kafka1";
    final String clusterNamespace1 = "default";
    StrimziKafkaContainer kafkaContainer;
    String clusterId;
    URI bootstrapServers;

    @BeforeEach
    void setup() {
        kafkaContainer = deployments.getKafkaContainer();
        bootstrapServers = URI.create(kafkaContainer.getBootstrapServers());
        utils = new TestHelper(bootstrapServers, config, null);

        client.resources(Kafka.class).inAnyNamespace().delete();
        client.resources(KafkaNodePool.class).inAnyNamespace().delete();
        client.resources(Pod.class).inAnyNamespace().delete();
        consoleConfig.clearSecurity();

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

        clusterId = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
    }

    @Test
    void testListNodesWithDualRoles() {
        var clusterResult = Mockito.mock(DescribeClusterResult.class);
        var nodes = List.<org.apache.kafka.common.Node>of(
                new org.apache.kafka.common.Node(0, "node-0.example.com", 9092, "az1"),
                new org.apache.kafka.common.Node(1, "node-1.example.com", 9092, "az2"),
                new org.apache.kafka.common.Node(2, "node-2.example.com", 9092, "az3")
        );
        when(clusterResult.nodes()).thenReturn(KafkaFuture.completedFuture(nodes));

        var quorumResult = Mockito.mock(DescribeMetadataQuorumResult.class);
        var quorumInfo = MockHelper.mockAll(QuorumInfo.class, Map.of(
                QuorumInfo::leaderId, 0,
                QuorumInfo::voters, List.of(
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 0,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        )),
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 1,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        )),
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 2,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        ))
                ),
                QuorumInfo::observers, List.of()
        ));
        when(quorumResult.quorumInfo()).thenReturn(KafkaFuture.completedFuture(quorumInfo));

        AdminClientSpy.install(client -> {
            doReturn(clusterResult)
                .when(client)
                .describeCluster(any(DescribeClusterOptions.class));
            doReturn(quorumResult)
                .when(client)
                .describeMetadataQuorum(any(DescribeMetadataQuorumOptions.class));
        });

        whenRequesting(req -> req.get("", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(3))
            .body("data.attributes.findAll { it }.collect { it.roles }", everyItem(contains("controller", "broker")));
    }

    @Test
    void testListNodesWithSplitRoles() {
        var clusterResult = Mockito.mock(DescribeClusterResult.class);
        var nodes = List.<org.apache.kafka.common.Node>of(
                // Controllers are not returned in the nodes listing
                new org.apache.kafka.common.Node(3, "node-3.example.com", 9092, "az2"),
                new org.apache.kafka.common.Node(4, "node-4.example.com", 9092, "az3"),
                new org.apache.kafka.common.Node(5, "node-5.example.com", 9092, "az3")
        );
        when(clusterResult.nodes()).thenReturn(KafkaFuture.completedFuture(nodes));

        var quorumResult = Mockito.mock(DescribeMetadataQuorumResult.class);
        var quorumInfo = MockHelper.mockAll(QuorumInfo.class, Map.of(
                QuorumInfo::leaderId, 0,
                QuorumInfo::voters, List.of(
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 0,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        )),
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 1,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        )),
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 2,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        ))
                ),
                QuorumInfo::observers, List.of(
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 3,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        )),
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 4,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        )),
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 5,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        ))
                )
        ));
        when(quorumResult.quorumInfo()).thenReturn(KafkaFuture.completedFuture(quorumInfo));

        AdminClientSpy.install(client -> {
            doReturn(clusterResult)
                .when(client)
                .describeCluster(any(DescribeClusterOptions.class));
            doReturn(quorumResult)
                .when(client)
                .describeMetadataQuorum(any(DescribeMetadataQuorumOptions.class));
        });

        whenRequesting(req -> req.get("", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(6))
            .body("data.findAll { it.id <= '2' }.attributes.collect { it.roles }", everyItem(contains("controller")))
            .body("data.findAll { it.id >= '3' }.attributes.collect { it.roles }", everyItem(contains("broker")));
    }

    @Test
    void testListNodesWithNodePoolsAndPods() {
        var clusterResult = Mockito.mock(DescribeClusterResult.class);
        var nodes = List.<org.apache.kafka.common.Node>of(
                // Controllers are not returned in the nodes listing
                new org.apache.kafka.common.Node(3, "node-3.example.com", 9092, "az2"),
                new org.apache.kafka.common.Node(4, "node-4.example.com", 9092, "az3"),
                new org.apache.kafka.common.Node(5, "node-5.example.com", 9092, "az3")
        );
        when(clusterResult.nodes()).thenReturn(KafkaFuture.completedFuture(nodes));

        var quorumResult = Mockito.mock(DescribeMetadataQuorumResult.class);
        var quorumInfo = MockHelper.mockAll(QuorumInfo.class, Map.of(
                QuorumInfo::leaderId, 0,
                QuorumInfo::voters, List.of(
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 0,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        )),
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 1,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        )),
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 2,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        ))
                ),
                QuorumInfo::observers, List.of(
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 3,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        )),
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 4,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        )),
                        MockHelper.mockAll(QuorumInfo.ReplicaState.class, Map.of(
                                QuorumInfo.ReplicaState::replicaId, 5,
                                QuorumInfo.ReplicaState::logEndOffset, 1000L
                        ))
                )
        ));
        when(quorumResult.quorumInfo()).thenReturn(KafkaFuture.completedFuture(quorumInfo));

        AdminClientSpy.install(client -> {
            doReturn(clusterResult)
                .when(client)
                .describeCluster(any(DescribeClusterOptions.class));
            doReturn(quorumResult)
                .when(client)
                .describeMetadataQuorum(any(DescribeMetadataQuorumOptions.class));
        });

        utils.apply(client, new ServiceAccountBuilder()
                .withNewMetadata()
                    .withNamespace(clusterNamespace1)
                    .withName("default")
                .endMetadata()
                .build());

        IntStream.rangeClosed(0, 5).forEach(nodeId -> {
            utils.apply(client, new PodBuilder()
                    .withNewMetadata()
                        .withNamespace(clusterNamespace1)
                        .withName(clusterId + "-" + nodeId)
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
                        .withPhase("Running")
                        .addNewCondition()
                            .withType("Ready")
                            .withStatus("True")
                        .endCondition()
                    .endStatus()
                    .build());
        });

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
                    .withNodeIds(0, 1, 2)
                .endStatus()
                .build());
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
                    .withNodeIds(3, 4, 5)
                .endStatus()
                .build());

        whenRequesting(req -> req.get("", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(6))
            .body("data.findAll { it.id <= '2' }.attributes.collect { it.nodePool }", everyItem(is(clusterId + "-controllers")))
            .body("data.findAll { it.id >= '3' }.attributes.collect { it.nodePool }", everyItem(is(clusterId + "-brokers")));
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
}
