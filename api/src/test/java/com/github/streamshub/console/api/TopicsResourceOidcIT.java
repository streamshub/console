package com.github.streamshub.console.api;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.json.Json;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.provider.CsvSource;

import com.github.streamshub.console.api.security.ConsoleAuthenticationMechanism;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.config.security.Decision;
import com.github.streamshub.console.config.security.GlobalSecurityConfigBuilder;
import com.github.streamshub.console.config.security.KafkaSecurityConfigBuilder;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.ResourceTypes;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.kafka.systemtest.utils.ConsumerUtils;
import com.github.streamshub.console.kafka.systemtest.utils.TokenUtils;
import com.github.streamshub.console.support.Identifiers;
import com.github.streamshub.console.test.LogCapture;
import com.github.streamshub.console.test.TestHelper;
import com.github.streamshub.console.test.TopicHelper;
import com.github.streamshub.console.test.VarargsAggregator;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.topic.KafkaTopic;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@TestHTTPEndpoint(TopicsResource.class)
@TestProfile(TestPlainProfile.class)
class TopicsResourceOidcIT {

    static LogCapture auditLogCapture = LogCapture.with(logRecord -> logRecord
            .getLoggerName()
            .equals(ConsoleAuthenticationMechanism.class.getName()),
            Level.INFO);

    @Inject
    Config config;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    KubernetesClient client;

    @Inject
    Map<String, KafkaContext> configuredContexts;

    @Inject
    @Named("KafkaTopics")
    Map<String, Map<String, Map<String, KafkaTopic>>> managedTopics;

    TestHelper utils;
    TopicHelper topicUtils;
    ConsumerUtils groupUtils;
    TokenUtils tokens;

    final String clusterName1 = "test-kafka1";
    String clusterId1;
    URI bootstrapServers1;
    String clusterId2;

    @BeforeAll
    static void initialize() {
        auditLogCapture.register();
    }

    @AfterAll
    static void cleanup() {
        auditLogCapture.deregister();
    }

    @BeforeEach
    void setup() {
        bootstrapServers1 = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        URI randomBootstrapServers = URI.create(consoleConfig.getKafka()
                .getCluster("default/test-kafka2")
                .map(k -> k.getProperties().get("bootstrap.servers"))
                .orElseThrow());

        topicUtils = new TopicHelper(bootstrapServers1, config);
        topicUtils.deleteAllTopics();

        groupUtils = new ConsumerUtils(config);

        utils = new TestHelper(bootstrapServers1, config);
        utils.resetSecurity(consoleConfig, true);
        tokens = new TokenUtils(config);

        client.resources(Kafka.class).inAnyNamespace().delete();
        client.resources(KafkaTopic.class).inAnyNamespace().delete();

        var kafka1 = utils.apply(client, utils.buildKafkaResource(clusterName1, utils.getClusterId(), bootstrapServers1));
        // Second cluster is offline/non-existent
        utils.apply(client, utils.buildKafkaResource("test-kafka2", UUID.randomUUID().toString(), randomBootstrapServers));

        // Wait for the added cluster to be configured in the context map
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> configuredContexts.values()
                    .stream()
                    .map(KafkaContext::clusterConfig)
                    .map(KafkaClusterConfig::clusterKey)
                    .anyMatch(Cache.metaNamespaceKeyFunc(kafka1)::equals));

        clusterId1 = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
        clusterId2 = consoleConfig.getKafka().getCluster("default/test-kafka2").get().getId();
    }

    @ParameterizedTest
    @CsvSource({
        "alice, a",
        // bob is on both teams, not used for this test
        "susan, b",
    })
    void testListTopicsWithForbiddenFieldsNull(String username, String team) {
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

        /*
         * Both teams may list all topics, but may only describe their own team's topics
         * and may only list/get their own groups.
         */
        consoleConfig.getKafka().getClusterById(clusterId1).ifPresent(cfg -> {
            cfg.setSecurity(new KafkaSecurityConfigBuilder()
                    .addNewRole()
                        .withName("dev-a")
                        .addNewRule()
                            .withResources("topics")
                            .withPrivileges(Privilege.LIST)
                        .endRule()
                        .addNewRule()
                            .withResources("topics")
                            .withResourceNames("a-*")
                            .withPrivileges(Privilege.GET)
                        .endRule()
                        .addNewRule()
                            .withResources("groups")
                            .withResourceNames("ga-*")
                            .withPrivileges(Privilege.LIST, Privilege.GET)
                        .endRule()
                    .endRole()
                    .addNewRole()
                        .withName("dev-b")
                        .addNewRule()
                            .withResources("topics")
                            .withPrivileges(Privilege.LIST)
                        .endRule()
                        .addNewRule()
                            .withResources("topics")
                            .withResourceNames("b-*")
                            .withPrivileges(Privilege.GET)
                        .endRule()
                        .addNewRule()
                            .withResources("consumerGroups") // deprecated name
                            .withResourceNames("gb-*")
                            .withPrivileges(Privilege.LIST, Privilege.GET)
                        .endRule()
                    .endRole()
                .build());
        });

        String topicA = "a-" + UUID.randomUUID().toString();
        String topicB = "b-" + UUID.randomUUID().toString();

        String groupA = "ga-" + UUID.randomUUID().toString();
        String groupB = "gb-" + UUID.randomUUID().toString();

        String clientA = "ca-" + UUID.randomUUID().toString();
        String clientB = "cb-" + UUID.randomUUID().toString();

        String allowedTopic;
        String allowedGroup;
        String forbiddenTopic;

        if ("a".equals(team)) {
            allowedTopic = topicA;
            allowedGroup = Identifiers.encode(groupA);
            forbiddenTopic = topicB;
        } else {
            allowedTopic = topicB;
            allowedGroup = Identifiers.encode(groupB);
            forbiddenTopic = topicA;
        }

        try (var consumerA = groupUtils.consume(groupA, topicA, clientA, 2, false);
             var consumerB = groupUtils.consume(groupB, topicB, clientB, 2, false)) {
            whenRequesting(req -> req
                    .auth()
                        .oauth2(tokens.getToken(username))
                    .queryParam("fields[topics]", "name,partitions,configs,groups")
                    .get("", clusterId1))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.size()", is(2))
                .body("data.attributes.name", containsInAnyOrder(topicA, topicB))
                .body("data.find { it.attributes.name == '" + allowedTopic + "'}.attributes", allOf(
                    hasEntry(is("partitions"), not(nullValue())),
                    hasEntry(is("configs"), not(nullValue()))
                ))
                .body("data.find { it.attributes.name == '" + allowedTopic + "'}.relationships", allOf(
                    hasEntry(
                        is("groups"),
                        hasEntry(
                            is("data"),
                            contains(allOf(
                                hasEntry(equalTo("type"), equalTo("groups")),
                                hasEntry(equalTo("id"), equalTo(allowedGroup))
                            ))
                        )
                    )
                ))
                .body("data.find { it.attributes.name == '" + forbiddenTopic + "'}.attributes", allOf(
                    hasEntry(is("partitions"), nullValue()),
                    hasEntry(is("configs"), nullValue())
                ))
                .body("data.find { it.attributes.name == '" + forbiddenTopic + "'}.relationships", allOf(
                    hasEntry(is("groups"), nullValue())
                ));
        }
    }


    @ParameterizedTest
    @CsvSource({
        "name, LIST",
        // numPartitions requires an additional describe
        "'name,numPartitions', LIST, GET"
    })
    void testListTopicsWithAuditLogging(String fields, @AggregateWith(VarargsAggregator.class) Privilege... privilegesAudited) {
        String allowedTopic = "a-" + UUID.randomUUID().toString();
        String deniedTopic = "b-" + UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(allowedTopic, deniedTopic), 1);

        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewSubject()
                    .withClaim("groups")
                    .withInclude("team-a")
                    .withRoleNames("dev-a")
                .endSubject()
            .build());

        consoleConfig.getKafka().getClusterById(clusterId1).ifPresent(clusterConfig -> {
            clusterConfig.setSecurity(new KafkaSecurityConfigBuilder()
                    .addNewAudit()
                        .withDecision(Decision.ALL)
                        .withResources(ResourceTypes.Kafka.TOPICS.value())
                        .withPrivileges(privilegesAudited)
                    .endAudit()
                    .addNewRole()
                        .withName("dev-a")
                        .addNewRule()
                            .withResources("topics")
                            .withResourceNames("a-*")
                            .withPrivileges(Privilege.LIST, Privilege.GET)
                        .endRule()
                    .endRole()
                    .build());
        });

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken("alice"))
                .queryParam("fields[topics]", fields)
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(1));

        var auditLogs = auditLogCapture.records();
        final String auditTmpl = "alice %s console:kafkas/[default/test-kafka1]/topics:[%s]:[%s]";

        assertThat(auditLogs, hasItem(both(hasProperty("message", containsString(
                    auditTmpl.formatted("allowed", "", Privilege.LIST))))
                .and(hasProperty("level", equalTo(Level.INFO)))));
        assertThat(auditLogs, hasItem(both(hasProperty("message", containsString(
                    auditTmpl.formatted("denied", deniedTopic, Privilege.LIST))))
                .and(hasProperty("level", equalTo(Level.INFO)))));

        for (var p : privilegesAudited) {
            assertThat(auditLogs, hasItem(both(hasProperty("message", containsString(auditTmpl.formatted("allowed", allowedTopic, p))))
                    .and(hasProperty("level", equalTo(Level.INFO)))));
        }
    }

    @ParameterizedTest
    @CsvSource({
        "alice, a",
        // bob is on both teams, not used for this test
        "susan, b",
    })
    void testDescribeTopicWithForbiddenFieldsNull(String username, String team) {
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

        /*
         * Both teams may only describe their own topics.
         */
        consoleConfig.getKafka().getClusterById(clusterId1).ifPresent(cfg -> {
            cfg.setSecurity(new KafkaSecurityConfigBuilder()
                    .addNewRole()
                        .withName("dev-a")
                        .addNewRule()
                            .withResources("topics")
                            .withResourceNames("a-*")
                            .withPrivileges(Privilege.GET)
                        .endRule()
                    .endRole()
                    .addNewRole()
                        .withName("dev-b")
                        .addNewRule()
                            .withResources("topics")
                            .withResourceNames("b-*")
                            .withPrivileges(Privilege.GET)
                        .endRule()
                    .endRole()
                .build());
        });

        String topicA = "a-" + UUID.randomUUID().toString();
        String topicB = "b-" + UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicA, topicB), 1);

        String allowedTopic;
        String forbiddenTopic;

        if ("a".equals(team)) {
            allowedTopic = topicA;
            forbiddenTopic = topicB;
        } else {
            allowedTopic = topicB;
            forbiddenTopic = topicA;
        }

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken(username))
                .queryParam("fields[topics]", "name,partitions,configs")
                .get("{topicId}", clusterId1, topicIds.get(allowedTopic)))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is(allowedTopic))
            .body("data.attributes", allOf(
                hasEntry(is("partitions"), not(nullValue())),
                hasEntry(is("configs"), not(nullValue()))
            ));

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken(username))
                .queryParam("fields[topics]", "name,partitions,configs")
                .get("{topicId}", clusterId1, topicIds.get(forbiddenTopic)))
            .assertThat()
            .statusCode(is(Status.FORBIDDEN.getStatusCode()));
    }

    @ParameterizedTest
    @CsvSource({
        "a-, CREATED",
        "b-, FORBIDDEN",
    })
    void testCreateTopicWithAuthorization(String topicPrefix, Status expectedStatus) {
        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewSubject()
                    .withClaim("groups")
                    .withInclude("team-a")
                    .withRoleNames("dev-a")
                .endSubject()
            .build());

        // alice's team may only create topics starting with `a-`
        consoleConfig.getKafka().getClusterById(clusterId1).ifPresent(cfg -> {
            cfg.setSecurity(new KafkaSecurityConfigBuilder()
                    .addNewRole()
                        .withName("dev-a")
                        .addNewRule()
                            .withResources("topics")
                            .withResourceNames("a-*")
                            .withPrivileges(Privilege.CREATE)
                        .endRule()
                    .endRole()
                .build());
        });

        String topicName = topicPrefix + UUID.randomUUID().toString();

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken("alice"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", topicName)
                                        .add("numPartitions", 3)
                                        .add("replicationFactor", 1)))
                        .build()
                        .toString())
                .post("", clusterId1))
            .assertThat()
            .statusCode(is(expectedStatus.getStatusCode()));
    }

    @ParameterizedTest
    @CsvSource({
        "a-, NO_CONTENT",
        "b-, FORBIDDEN",
    })
    void testDeleteTopicWithAuthorization(String topicPrefix, Status expectedStatus) {
        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewSubject()
                    .withClaim("groups")
                    .withInclude("team-a")
                    .withRoleNames("dev-a")
                .endSubject()
            .build());

        // alice's team may only delete topics starting with `a-`
        consoleConfig.getKafka().getClusterById(clusterId1).ifPresent(cfg -> {
            cfg.setSecurity(new KafkaSecurityConfigBuilder()
                    .addNewRole()
                        .withName("dev-a")
                        .addNewRule()
                            .withResources("topics")
                            .withResourceNames("a-*")
                            .withPrivileges(Privilege.DELETE)
                        .endRule()
                    .endRole()
                .build());
        });

        String topicName = topicPrefix + UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 2);

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken("alice"))
                .delete("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(expectedStatus.getStatusCode()));
    }

    @ParameterizedTest
    @CsvSource({
        "a-, NO_CONTENT",
        "b-, FORBIDDEN",
    })
    void testPatchTopicWithAuthorization(String topicPrefix, Status expectedStatus) {
        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewSubject()
                    .withClaim("groups")
                    .withInclude("team-a")
                    .withRoleNames("dev-a")
                .endSubject()
            .build());

        // alice's team may only update topics starting with `a-`
        // UPDATE requires GET: old version of topic required for validations
        consoleConfig.getKafka().getClusterById(clusterId1).ifPresent(cfg -> {
            cfg.setSecurity(new KafkaSecurityConfigBuilder()
                    .addNewRole()
                        .withName("dev-a")
                        .addNewRule()
                            .withResources("topics")
                            .withResourceNames("a-*")
                            .withPrivileges(Privilege.GET, Privilege.UPDATE)
                        .endRule()
                    .endRole()
                .build());
        });

        String topicName = topicPrefix + UUID.randomUUID().toString();
        Map<String, String> topicIds = topicUtils.createTopics(List.of(topicName), 1);

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken("alice"))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("id", topicIds.get(topicName))
                                .add("type", "topics")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("numPartitions", 2) // adding partition
                                )
                        )
                        .build()
                        .toString())
                .patch("{topicId}", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(expectedStatus.getStatusCode()));
    }

    @Test
    void testListTopicWithNullResourceNames() {
        utils.updateSecurity(consoleConfig.getSecurity(), new GlobalSecurityConfigBuilder()
                .addNewSubject()
                    .withClaim("groups")
                    .withInclude("team-a")
                    .withRoleNames("dev-a")
                .endSubject()
            .build());

        // alice's team may list all topics, but the names are omitted from the configuration
        consoleConfig.getKafka().getClusterById(clusterId1).ifPresent(cfg -> {
            cfg.setSecurity(new KafkaSecurityConfigBuilder()
                    .addNewRole()
                        .withName("dev-a")
                        .addNewRule()
                            .withResources("topics")
                            // Setting to null results in an empty collection in the config model
                            .withResourceNames((List<String>) null)
                            .withPrivileges(Privilege.LIST)
                        .endRule()
                    .endRole()
                .build());
        });

        String topicName = UUID.randomUUID().toString();
        topicUtils.createTopics(List.of(topicName), 1);

        whenRequesting(req -> req
                .auth()
                    .oauth2(tokens.getToken("alice"))
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(1))
            .body("data.meta.privileges", everyItem(is(List.of(Privilege.LIST.name()))));
    }
}
