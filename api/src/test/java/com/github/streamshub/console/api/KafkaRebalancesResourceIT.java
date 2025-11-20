package com.github.streamshub.console.api;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.test.TestHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalance;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceBuilder;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceMode;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceState;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static java.util.Comparator.nullsLast;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyArray;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
@TestHTTPEndpoint(KafkaRebalancesResource.class)
@TestProfile(TestPlainProfile.class)
class KafkaRebalancesResourceIT {

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @Inject
    ConsoleConfig consoleConfig;

    TestHelper utils;

    String clusterId1;
    String clusterId2;
    URI bootstrapServers;
    URI randomBootstrapServers;

    static KafkaRebalance buildRebalance(int sequence, String clusterName, KafkaRebalanceMode mode, KafkaRebalanceState state, boolean template) {
        var builder = new KafkaRebalanceBuilder()
            .withNewMetadata()
                .withName("rebalance-" + sequence)
                .withNamespace("default")
            .endMetadata()
            .withNewSpec()
                .withMode(mode)
            .endSpec();

        if (clusterName != null) {
            builder.editMetadata()
                .addToLabels(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName)
                .endMetadata();
        }

        if (template) {
            builder.editMetadata()
                .addToAnnotations(ResourceAnnotations.ANNO_STRIMZI_IO_REBALANCE_TEMPLATE, "true")
                .endMetadata();
        }

        if (state != null) {
            builder = builder
                .withNewStatus()
                    .addNewCondition()
                        .withType(state.name())
                        .withStatus("True")
                        .withLastTransitionTime(Instant.now().toString())
                    .endCondition()
                    .addToOptimizationResult("intraBrokerDataToMoveMB", "0")
                .endStatus();
        }

        return builder.build();
    }

    @BeforeEach
    void setup() {
        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        randomBootstrapServers = URI.create(consoleConfig.getKafka()
                .getCluster("default/test-kafka2")
                .map(k -> k.getProperties().get("bootstrap.servers"))
                .orElseThrow());

        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);

        client.resources(Kafka.class).inAnyNamespace().delete();
        client.resources(KafkaRebalance.class).inAnyNamespace().delete();

        utils.apply(client, new KafkaBuilder(utils.buildKafkaResource("test-kafka1", utils.getClusterId(), bootstrapServers))
                .editSpec()
                    .withNewCruiseControl()
                        // empty
                    .endCruiseControl()
                .endSpec()
                .build());

        // Second cluster is offline/non-existent
        utils.apply(client, new KafkaBuilder(utils.buildKafkaResource("test-kafka2", UUID.randomUUID().toString(), randomBootstrapServers))
            .editOrNewStatus()
                .addNewCondition()
                    .withType("NotReady")
                    .withStatus("True")
                .endCondition()
            .endStatus()
            .build());

        int r = 0;

        // No cluster name - MUST BE FIRST for "Not found" test
        utils.apply(client, buildRebalance(r++, null, KafkaRebalanceMode.FULL, null, false));

        for (String clusterName : Arrays.asList("test-kafka1", "test-kafka2", "test-kafka3")) {
            for (KafkaRebalanceMode mode : KafkaRebalanceMode.values()) {
                // No status
                utils.apply(client, buildRebalance(r++, clusterName, mode, null, false));
                // No status + template annotation
                utils.apply(client, buildRebalance(r++, clusterName, mode, null, true));

                for (KafkaRebalanceState state : KafkaRebalanceState.values()) {
                    utils.apply(client, buildRebalance(r++, clusterName, mode, state, false));
                }
            }
        }

        clusterId1 = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
        clusterId2 = consoleConfig.getKafka().getCluster("default/test-kafka2").get().getId();
    }

    @Test
    void testListRebalancesIncludesAllowedActions() {
        whenRequesting(req -> req.get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data", not(emptyArray()))
            .body("data.findAll { it }.collect { it.meta }", everyItem(allOf(
                    hasKey("allowedActions"),
                    hasKey("autoApproval"))));
    }

    @Test
    void testListRebalancesFilteredByMode() {
        whenRequesting(req -> req
                .param("filter[mode]", KafkaRebalanceMode.FULL.toValue())
                .param("fields[" + com.github.streamshub.console.api.model.KafkaRebalance.API_TYPE + "]", "name,mode")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(KafkaRebalanceState.values().length + 1))
            .body("data.findAll { it }.collect { it.attributes }",
                    everyItem(hasEntry("mode", KafkaRebalanceMode.FULL.toValue())));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        com.github.streamshub.console.api.model.KafkaRebalance.Fields.NAME,
        com.github.streamshub.console.api.model.KafkaRebalance.Fields.NAMESPACE,
        com.github.streamshub.console.api.model.KafkaRebalance.Fields.CREATION_TIMESTAMP,
        com.github.streamshub.console.api.model.KafkaRebalance.Fields.MODE,
        com.github.streamshub.console.api.model.KafkaRebalance.Fields.STATUS,
    })
    void testListRebalancesFullySorted(String sortField) {
        int total = KafkaRebalanceMode.values().length * (KafkaRebalanceState.values().length + 1);

        var values = whenRequesting(req -> req
                .param("fields[" + com.github.streamshub.console.api.model.KafkaRebalance.API_TYPE + "]", sortField)
                .param("sort", sortField)
                .param("page[size]", total)
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(total))
            .extract().jsonPath().getList("data.findAll { it }.collect { it.attributes. " + sortField + " }");

        var sortedValues = values.stream()
                .map(String.class::cast)
                .sorted(nullsLast(String::compareTo))
                .toList();

        assertEquals(sortedValues, values);
    }

    @Test
    void testPatchRebalanceWithStatusProposalReady() {
        String rebalanceId = whenRequesting(req -> req
                .param("filter[mode]", KafkaRebalanceMode.FULL.toValue())
                .param("filter[status]", KafkaRebalanceState.ProposalReady.name())
                .param("filter[name]", "like,rebalance-*")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(1))
            .extract().jsonPath().getString("data[0].id");

        var rebalance = whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("id", rebalanceId)
                                .add("type", com.github.streamshub.console.api.model.KafkaRebalance.API_TYPE)
                                .add("meta", Json.createObjectBuilder()
                                        .add("action", "refresh"))
                                .add("attributes", Json.createObjectBuilder()))
                        .build()
                        .toString())
                .patch("{rebalanceId}", clusterId1, rebalanceId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .extract().jsonPath();

        String namespace = rebalance.getString("data.attributes.namespace");
        String name = rebalance.getString("data.attributes.name");

        var rebalanceCR = client.resources(KafkaRebalance.class)
            .inNamespace(namespace)
            .withName(name)
            .get();

        assertEquals("refresh", rebalanceCR.getMetadata().getAnnotations().get(ResourceAnnotations.ANNO_STRIMZI_IO_REBALANCE));

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("id", rebalanceId)
                                .add("type", com.github.streamshub.console.api.model.KafkaRebalance.API_TYPE)
                                .add("meta", Json.createObjectBuilder()
                                        .addNull("action"))
                                .add("attributes", Json.createObjectBuilder()))
                        .build()
                        .toString())
                .patch("{rebalanceId}", clusterId1, rebalanceId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .extract().jsonPath();

        rebalanceCR = client.resources(KafkaRebalance.class)
                .inNamespace(namespace)
                .withName(name)
                .get();

        assertNull(rebalanceCR.getMetadata().getAnnotations().get(ResourceAnnotations.ANNO_STRIMZI_IO_REBALANCE));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "x/y/z",
        "x/y",
        "default/rebalance-0"
    })
    void testPatchRebalanceNotFound(String rebalanceId) {
        String encodedId = Base64.getUrlEncoder().encodeToString(rebalanceId.getBytes(StandardCharsets.UTF_8));

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("id", encodedId)
                                .add("type", com.github.streamshub.console.api.model.KafkaRebalance.API_TYPE)
                                .add("meta", Json.createObjectBuilder()
                                        .add("action", "refresh"))
                                .add("attributes", Json.createObjectBuilder()))
                        .build()
                        .toString())
                .patch("{rebalanceId}", clusterId1, encodedId))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()));
    }

}
