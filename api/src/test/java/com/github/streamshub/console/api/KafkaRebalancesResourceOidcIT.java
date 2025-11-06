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
import org.junit.jupiter.params.provider.CsvSource;

import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.security.GlobalSecurityConfigBuilder;
import com.github.streamshub.console.config.security.KafkaSecurityConfigBuilder;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.kafka.systemtest.utils.TokenUtils;
import com.github.streamshub.console.test.TestHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalance;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceBuilder;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceMode;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceState;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestHTTPEndpoint(KafkaRebalancesResource.class)
@TestProfile(TestPlainProfile.class)
class KafkaRebalancesResourceOidcIT {

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @Inject
    ConsoleConfig consoleConfig;

    TestHelper utils;
    TokenUtils tokens;

    String clusterId1;
    String clusterId2;
    URI bootstrapServers;
    URI randomBootstrapServers;

    static KafkaRebalance buildRebalance(int sequence, String clusterName, KafkaRebalanceMode mode, KafkaRebalanceState state) {
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
        utils.resetSecurity(consoleConfig, true);
        tokens = new TokenUtils(config);

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
        utils.apply(client, buildRebalance(r++, null, KafkaRebalanceMode.FULL, null));

        for (String clusterName : Arrays.asList("test-kafka1", "test-kafka2", "test-kafka3")) {
            for (KafkaRebalanceMode mode : KafkaRebalanceMode.values()) {
                // No status
                utils.apply(client, buildRebalance(r++, clusterName, mode, null));

                for (KafkaRebalanceState state : KafkaRebalanceState.values()) {
                    utils.apply(client, buildRebalance(r++, clusterName, mode, state));
                }
            }
        }

        clusterId1 = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
        clusterId2 = consoleConfig.getKafka().getCluster("default/test-kafka2").get().getId();
    }

    @ParameterizedTest
    @CsvSource({
        "alice, a",
        // bob is on both teams, not used for this test
        "susan, b",
    })
    void testListRebalancesWithPerTeamKafkaClusterAccess(String username, String team) {
        int total = KafkaRebalanceMode.values().length * (KafkaRebalanceState.values().length + 1);

        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewSubject()
                    .withClaim("groups")
                    .withInclude("team-a")
                    .withRoleNames("dev-a")
                .endSubject()
                .addNewSubject()
                    .withClaim("groups")
                    .withInclude("team-b")
                    .withRoleNames("dev-b")
                .endSubject()
            .build());

        consoleConfig.getKafka().getClusterById(clusterId1).ifPresent(cfg -> {
            cfg.setSecurity(new KafkaSecurityConfigBuilder()
                    .addNewRole()
                        .withName("dev-a")
                        .addNewRule()
                            .withResources("rebalances")
                            .withPrivileges(Privilege.LIST)
                        .endRule()
                    .endRole()
                    .build());
        });

        consoleConfig.getKafka().getClusterById(clusterId2).ifPresent(cfg -> {
            cfg.setSecurity(new KafkaSecurityConfigBuilder()
                    .addNewRole()
                        .withName("dev-b")
                        .addNewRule()
                            .withResources("rebalances")
                            .withPrivileges(Privilege.LIST)
                        .endRule()
                    .endRole()
                    .build());
        });

        String allowedId = "a".equals(team) ? clusterId1 : clusterId2;
        String forbiddenId = "a".equals(team) ? clusterId2 : clusterId1;

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken(username))
                .param("page[size]", total)
                .get("", allowedId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(total))
            .body("data.meta.privileges", everyItem(is(List.of(Privilege.LIST.name()))));

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken(username))
                .get("", forbiddenId))
            .assertThat()
            .statusCode(is(Status.FORBIDDEN.getStatusCode()));
    }

    @Test
    void testListRebalancesWithUnrelatedRoleAccess() {
        // alice is granted access to topics, but not rebalances
        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewSubject()
                    .withInclude("alice")
                    .withRoleNames("developer")
                .endSubject()
            .build());

        consoleConfig.getKafka().getClusterById(clusterId1).ifPresent(cfg -> {
            cfg.setSecurity(new KafkaSecurityConfigBuilder()
                    .addNewRole()
                        .withName("developer")
                        .addNewRule()
                            .withResources("topics")
                            .withPrivileges(Privilege.ALL)
                        .endRule()
                    .endRole()
                    .build());
        });

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken("alice"))
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.FORBIDDEN.getStatusCode()));
    }

    @Test
    void testListRebalancesWithMissingPrivilege() {
        // alice can get and update rebalances, but she may not list them
        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewSubject()
                    .withInclude("alice")
                    .withRoleNames("developer")
                .endSubject()
            .build());

        consoleConfig.getKafka().getClusterById(clusterId1).ifPresent(cfg -> {
            cfg.setSecurity(new KafkaSecurityConfigBuilder()
                    .addNewRole()
                        .withName("developer")
                        .addNewRule()
                            .withResources("rebalances")
                            .withPrivileges(Privilege.GET, Privilege.UPDATE)
                        .endRule()
                    .endRole()
                    .build());
        });

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken("alice"))
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.FORBIDDEN.getStatusCode()));
    }

}
