package com.github.streamshub.console.api;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.github.streamshub.console.api.model.KafkaCluster;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.config.security.GlobalSecurityConfigBuilder;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.kafka.systemtest.utils.TokenUtils;
import com.github.streamshub.console.test.AdminClientSpy;
import com.github.streamshub.console.test.TestHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestHTTPEndpoint(KafkaClustersResource.class)
@TestProfile(TestPlainProfile.class)
class KafkaClustersResourceOidcIT {

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @Inject
    Map<String, KafkaContext> configuredContexts;

    @Inject
    ConsoleConfig consoleConfig;

    TestHelper utils;
    TokenUtils tokens;

    String clusterId1;
    URI bootstrapServers;

    @BeforeEach
    void setup() {
        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));

        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, true);
        tokens = new TokenUtils(config);

        client.resources(Kafka.class).inAnyNamespace().delete();

        Kafka kafka1 = new KafkaBuilder(utils.buildKafkaResource("test-kafka1", utils.getClusterId(), bootstrapServers))
            .editOrNewStatus()
                .addNewCondition()
                    .withType("Ready")
                    .withStatus("True")
                .endCondition()
                .addNewKafkaNodePool()
                    .withName("my-node-pool")
                .endKafkaNodePool()
            .endStatus()
            .build();

        utils.apply(client, kafka1);

        // Second cluster is offline/non-existent
        URI randomBootstrapServers = URI.create(consoleConfig.getKafka()
                .getCluster("default/test-kafka2")
                .map(k -> k.getProperties().get("bootstrap.servers"))
                .orElseThrow());

        utils.apply(client, new KafkaBuilder(utils.buildKafkaResource("test-kafka2", UUID.randomUUID().toString(), randomBootstrapServers))
            .editOrNewStatus()
                .addNewCondition()
                    .withType("NotReady")
                    .withStatus("True")
                .endCondition()
            .endStatus()
            .build());

        // Wait for the added cluster to be configured in the context map
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> configuredContexts.values()
                    .stream()
                    .map(KafkaContext::clusterConfig)
                    .map(KafkaClusterConfig::clusterKey)
                    .anyMatch(Cache.metaNamespaceKeyFunc(kafka1)::equals));

        clusterId1 = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
    }

    @Test
    void testListClustersWithNoRolesDefined() {
        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken("alice"))
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name")
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(KafkaClustersResourceIT.STATIC_KAFKAS.size()))
            .body("data.attributes.name", containsInAnyOrder(KafkaClustersResourceIT.STATIC_KAFKAS.toArray(String[]::new)));
    }

    @Test
    void testListClustersWithFullAccess() {
        // alice is a developer and developers may list all kafkas
        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewRole()
                    .withName("developer")
                    .addNewRule()
                        .withResources("kafkas")
                        .withPrivileges(Privilege.LIST)
                    .endRule()
                .endRole()
                .addNewSubject()
                    .withInclude("alice")
                    .withRoleNames("developer")
                .endSubject()
            .build());

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken("alice"))
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name")
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(KafkaClustersResourceIT.STATIC_KAFKAS.size()))
            .body("data.attributes.name", containsInAnyOrder(KafkaClustersResourceIT.STATIC_KAFKAS.toArray(String[]::new)))
            .body("data.meta.privileges", everyItem(is(List.of(Privilege.LIST.name()))));
    }

    @Test
    void testListClustersUnauthenticated() {
        // alice is a developer and developers may list all kafkas
        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewRole()
                    .withName("developer")
                    .addNewRule()
                        .withResources("kafkas")
                        .withPrivileges(Privilege.LIST)
                    .endRule()
                .endRole()
            .build());

        whenRequesting(req -> req
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name")
                .get())
            .assertThat()
            .statusCode(is(Status.UNAUTHORIZED.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("401"))
            .body("errors.code", contains("4011"));
    }

    @Test
    void testListClustersWithReducedAccess() {
        List<String> visibleClusters = Arrays.asList("test-kafka1", "test-kafkaY");

        // alice is a developer and developers may only list two of three clusters
        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewRole()
                    .withName("developer")
                    .addNewRule()
                        .withResources("kafkas")
                        .withPrivileges(Privilege.LIST)
                        .withResourceNames(visibleClusters)
                    .endRule()
                .endRole()
                .addNewSubject()
                    .withInclude("alice")
                    .withRoleNames("developer")
                .endSubject()
            .build());

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken("alice"))
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name")
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(visibleClusters.size()))
            .body("data.attributes.name", containsInAnyOrder(visibleClusters.toArray(String[]::new)))
            .body("data.meta.privileges", everyItem(is(List.of(Privilege.LIST.name()))));
    }

    @Test
    void testDescribeClusterWithAdminAccess() {
        // make alice an admin
        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewRole()
                    .withName("admin")
                    .addNewRule()
                        .withResources("kafkas")
                        .withPrivileges(Privilege.ALL)
                    .endRule()
                .endRole()
                .addNewSubject()
                    .withInclude("alice")
                    .withRoleNames("admin")
                .endSubject()
            .build());

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken("alice"))
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name")
                .get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.meta.privileges", is(Privilege.ALL.expand().stream().map(Enum::name).toList()));
    }

    @Test
    void testDescribeClusterWithoutAdminAccess() {
        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewRole()
                    .withName("admin")
                    .addNewRule()
                        .withResources("kafkas")
                        .withPrivileges(Privilege.ALL)
                    .endRule()
                .endRole()
                .addNewSubject()
                    .withInclude("alice")
                    .withRoleNames("developer")
                .endSubject()
            .build());

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken("alice"))
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name")
                .get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.FORBIDDEN.getStatusCode()));
    }

    @ParameterizedTest
    // alice and bob are on team-a; susan is only on team-b
    @CsvSource({
        "alice, groups, OK",
        "bob, memberOf, OK",
        "susan, groups, FORBIDDEN",
    })
    void testDescribeClusterWithGroupAccess(String username, String claimName, Status expectedStatus) {
        // team-a group is given admin access Kafkas
        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewRole()
                    .withName("admin-a")
                    .addNewRule()
                        .withResources("kafkas")
                        .withPrivileges(Privilege.ALL)
                    .endRule()
                .endRole()
                .addNewSubject()
                    // here we define the subject in terms of group membership
                    .withClaim(claimName)
                    .withInclude("team-a")
                    .withRoleNames("admin-a")
                .endSubject()
            .build());

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken(username))
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name")
                .get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(expectedStatus.getStatusCode()));
    }

    // Helper methods

    static Map<String, Object> mockAdminClient() {
        return mockAdminClient(Map.of(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.PLAINTEXT.name));
    }

    static Map<String, Object> mockAdminClient(Map<String, Object> overrides) {
        Map<String, Object> clientConfig = new HashMap<>();

        AdminClientSpy.install(config -> {
            clientConfig.putAll(config);

            Map<String, Object> newConfig = new HashMap<>(config);
            newConfig.putAll(overrides);
            return newConfig;
        }, client -> { /* No-op */ });

        return clientConfig;
    }
}
