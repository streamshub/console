package com.github.streamshub.console.api;

import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jakarta.inject.Inject;
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
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalance;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserBuilder;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static io.strimzi.api.kafka.model.user.KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512;
import static io.strimzi.api.kafka.model.user.KafkaUserTlsClientAuthentication.TYPE_TLS;
import static io.strimzi.api.kafka.model.user.KafkaUserTlsExternalClientAuthentication.TYPE_TLS_EXTERNAL;
import static java.util.Comparator.nullsLast;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestHTTPEndpoint(KafkaUsersResource.class)
@TestProfile(TestPlainProfile.class)
class KafkaUsersResourceIT {

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

    static KafkaUser buildUser(int sequence, String clusterName, String authN, boolean ready) {
        var builder = new KafkaUserBuilder()
            .withNewMetadata()
                .withName("user-" + sequence)
                .withNamespace("default")
            .endMetadata()
            .withNewSpec()
                .withNewKafkaUserScramSha512ClientAuthentication()
                .endKafkaUserScramSha512ClientAuthentication()
                .withNewKafkaUserAuthorizationSimple()
                .endKafkaUserAuthorizationSimple()
            .endSpec();

        if (clusterName != null) {
            builder.editMetadata()
                .addToLabels(ResourceLabels.STRIMZI_CLUSTER_LABEL, clusterName)
                .endMetadata();
        }

        switch (authN) {
            case TYPE_TLS:
                builder = builder.editSpec()
                    .withNewKafkaUserTlsClientAuthentication()
                    .endKafkaUserTlsClientAuthentication()
                    .endSpec();
                break;
            case TYPE_TLS_EXTERNAL:
                builder = builder.editSpec()
                    .withNewKafkaUserTlsExternalClientAuthentication()
                    .endKafkaUserTlsExternalClientAuthentication()
                    .endSpec();
                break;
            case TYPE_SCRAM_SHA_512:
                builder = builder.editSpec()
                    .withNewKafkaUserScramSha512ClientAuthentication()
                    .endKafkaUserScramSha512ClientAuthentication()
                    .endSpec();
                break;
            default:
                break;
        }

        builder = builder
            .withNewStatus()
                .addNewCondition()
                    .withType("Ready")
                    .withStatus(ready ? "True" : "False")
                    .withLastTransitionTime(Instant.now().toString())
                .endCondition()
            .endStatus();

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
                    .withNewEntityOperator()
                        .withNewUserOperator()
                        .endUserOperator()
                    .endEntityOperator()
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
        utils.apply(client, buildUser(r++, null, "", false));

        for (String clusterName : Arrays.asList("test-kafka1", "test-kafka2", "test-kafka3")) {
            for (String authN : List.of(TYPE_TLS, TYPE_TLS_EXTERNAL, TYPE_SCRAM_SHA_512, "")) {
                // Not ready
                utils.apply(client, buildUser(r++, clusterName, authN, false));
                // Ready
                utils.apply(client, buildUser(r++, clusterName, authN, true));
            }
        }

        clusterId1 = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
        clusterId2 = consoleConfig.getKafka().getCluster("default/test-kafka2").get().getId();
    }

    @Test
    void testListUsersSimple() {
        whenRequesting(req -> req.get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(8)); // ready + not ready for each of 4 authN types
    }

    @ParameterizedTest
    @ValueSource(strings = {
        com.github.streamshub.console.api.model.KafkaUser.Fields.NAME,
        com.github.streamshub.console.api.model.KafkaUser.Fields.NAMESPACE,
        com.github.streamshub.console.api.model.KafkaUser.Fields.CREATION_TIMESTAMP,
    })
    void testListUsersFullySorted(String sortField) {
        var values = whenRequesting(req -> req
                .param("fields[" + com.github.streamshub.console.api.model.KafkaUser.API_TYPE + "]", sortField)
                .param("sort", sortField)
                .param("page[size]", 2)
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(2))
            .extract()
            .jsonPath()
            .getList("data.findAll { it }.collect { it.attributes. " + sortField + " }");

        var sortedValues = values.stream()
                .map(String.class::cast)
                .sorted(nullsLast(String::compareTo))
                .toList();

        assertEquals(sortedValues, values);
    }
}
