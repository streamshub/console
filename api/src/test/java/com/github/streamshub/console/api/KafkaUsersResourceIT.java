package com.github.streamshub.console.api;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.support.Identifiers;
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
import io.strimzi.api.kafka.model.user.acl.AclOperation;
import io.strimzi.api.kafka.model.user.acl.AclResourcePatternType;
import io.strimzi.api.kafka.model.user.acl.AclRule;
import io.strimzi.api.kafka.model.user.acl.AclRuleBuilder;
import io.strimzi.api.kafka.model.user.acl.AclRuleType;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static io.strimzi.api.kafka.model.user.KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512;
import static io.strimzi.api.kafka.model.user.KafkaUserTlsClientAuthentication.TYPE_TLS;
import static io.strimzi.api.kafka.model.user.KafkaUserTlsExternalClientAuthentication.TYPE_TLS_EXTERNAL;
import static java.util.Comparator.nullsLast;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestHTTPEndpoint(KafkaUsersResource.class)
@TestProfile(TestPlainProfile.class)
class KafkaUsersResourceIT {

    static final String NAMESPACE = "default";
    static final List<String> KNOWN_AUTHN_TYPES = Arrays.asList(TYPE_TLS, TYPE_TLS_EXTERNAL, TYPE_SCRAM_SHA_512, "");

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

    Map<String, List<String>> clusterUserNames;

    static List<AclRule> commonRules = List.of(
            clusterRule(AclRuleType.ALLOW, AclOperation.DESCRIBE, AclOperation.DESCRIBECONFIGS),
            topicRule(AclRuleType.ALLOW, "topic-a", null, AclOperation.ALL),
            topicRule(AclRuleType.DENY, "topic-x-*", AclResourcePatternType.PREFIX, AclOperation.ALL),
            groupRule(AclRuleType.ALLOW, "group-a", null, AclOperation.READ),
            groupRule(AclRuleType.DENY, "group-x-*", AclResourcePatternType.PREFIX, AclOperation.ALL),
            txRule(AclRuleType.ALLOW, "tx-a", null, AclOperation.IDEMPOTENTWRITE),
            txRule(AclRuleType.DENY, "tx-x-*", AclResourcePatternType.PREFIX, AclOperation.ALL)
    );

    static AclRuleBuilder initRuleBuilder(AclRuleType type, AclOperation... operations) {
        var builder = new AclRuleBuilder()
                .withType(type);

        if (operations.length == 1) {
            builder = builder.withOperation(operations[0]);
        } else {
            builder = builder.withOperations(operations);
        }

        return builder;
    }

    static AclRule clusterRule(AclRuleType type, AclOperation... operations) {
        var builder = initRuleBuilder(type, operations);

        return builder
                .withNewAclRuleClusterResource()
                .endAclRuleClusterResource()
                .build();
    }

    static AclRule topicRule(AclRuleType type, String name, AclResourcePatternType patternType, AclOperation... operations) {
        var builder = initRuleBuilder(type, operations);
        var topicBuilder = builder.withNewAclRuleTopicResource()
                .withName(name);

        if (patternType != null) {
            topicBuilder = topicBuilder.withPatternType(patternType);
        }

        return topicBuilder.endAclRuleTopicResource().build();
    }

    static AclRule groupRule(AclRuleType type, String name, AclResourcePatternType patternType, AclOperation... operations) {
        var builder = initRuleBuilder(type, operations);
        var groupBuilder = builder.withNewAclRuleGroupResource()
                .withName(name);

        if (patternType != null) {
            groupBuilder = groupBuilder.withPatternType(patternType);
        }

        return groupBuilder.endAclRuleGroupResource().build();
    }

    static AclRule txRule(AclRuleType type, String name, AclResourcePatternType patternType, AclOperation... operations) {
        var builder = initRuleBuilder(type, operations);
        var groupBuilder = builder.withNewAclRuleTransactionalIdResource()
                .withName(name);

        if (patternType != null) {
            groupBuilder = groupBuilder.withPatternType(patternType);
        }

        return groupBuilder.endAclRuleTransactionalIdResource().build();
    }

    static KafkaUser buildUser(int sequence, String clusterName, String authN, boolean ready) {
        var username = (clusterName != null ? clusterName : "nocluster") + "-user-" + sequence;

        var builder = new KafkaUserBuilder()
            .withNewMetadata()
                .withName(username)
                .withNamespace(NAMESPACE)
            .endMetadata()
            .withNewSpec()
                .withNewKafkaUserAuthorizationSimple()
                    .withAcls(commonRules)
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
                .getCluster(NAMESPACE + "/test-kafka2")
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

        clusterUserNames = new HashMap<>();

        for (String clusterName : Arrays.asList("test-kafka1", "test-kafka2")) {
            String clusterKey = NAMESPACE + "/" + clusterName;
            clusterUserNames.put(clusterKey, new ArrayList<>());

            for (String authN : KNOWN_AUTHN_TYPES) {
                KafkaUser ku;
                // Not ready
                ku = utils.apply(client, buildUser(r++, clusterName, authN, false));
                clusterUserNames.get(clusterKey).add(ku.getMetadata().getName());
                // Ready
                ku = utils.apply(client, buildUser(r++, clusterName, authN, true));
                clusterUserNames.get(clusterKey).add(ku.getMetadata().getName());
            }
        }

        clusterId1 = consoleConfig.getKafka().getCluster(NAMESPACE + "/test-kafka1").get().getId();
        clusterId2 = consoleConfig.getKafka().getCluster(NAMESPACE + "/test-kafka2").get().getId();
    }

    @ParameterizedTest
    @CsvSource({
        NAMESPACE + "/test-kafka1",
        NAMESPACE + "/test-kafka2"
    })
    void testListUsersSimple(String clusterKey) {
        var clusterId = consoleConfig.getKafka().getCluster(clusterKey).get().getId();
        var userNames = clusterUserNames.get(clusterKey);
        var userIds = userNames.stream().map(n -> Identifiers.encode("", NAMESPACE, n)).toList();

        whenRequesting(req -> req.get("", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(userNames.size())) // ready + not ready for each of 4 authN types
            .body("data.id", containsInAnyOrder(userIds.toArray(String[]::new)))
            .body("data.attributes.name", containsInAnyOrder(userNames.toArray(String[]::new)))
            .body("data.attributes.namespace", everyItem(is(NAMESPACE)))
            .body("data.attributes.authenticationType", everyItem(anyOf(is(in(KNOWN_AUTHN_TYPES)), nullValue(String.class))))
            .body("data.attributes.authorization.accessControls", everyItem(hasSize(commonRules.size())));
    }

    @ParameterizedTest
    @CsvSource({
        NAMESPACE + ", test-kafka1",
        NAMESPACE + ", test-kafka2"
    })
    void testListUsersFiltered(String namespace, String clusterName) {
        String clusterKey = namespace + '/' + clusterName;
        var clusterId = consoleConfig.getKafka().getCluster(clusterKey).get().getId();
        var userNames = clusterUserNames.get(clusterKey);
        var userIds = userNames.stream().map(n -> Identifiers.encode("", NAMESPACE, n)).toList();

        whenRequesting(req -> req
                .param("filter[username]", "like," + clusterName + "-*")
                .get("", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(userNames.size())) // ready + not ready for each of 4 authN types
            .body("data.id", containsInAnyOrder(userIds.toArray(String[]::new)))
            .body("data.attributes.name", containsInAnyOrder(userNames.toArray(String[]::new)));
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

    @Test
    void testDescribeUser() {
        var userNames = clusterUserNames.get(NAMESPACE + "/test-kafka1");
        var userId = Identifiers.encode("", NAMESPACE, userNames.get(0));

        whenRequesting(req -> req.get("{userId}", clusterId1, userId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", is(userId))
            .body("data.attributes.name", is(userNames.get(0)))
            .body("data.attributes.namespace", is(NAMESPACE))
            .body("data.attributes.authorization.accessControls.size()", is(commonRules.size()));
    }

    @Test
    void testDescribeUserNotFoundUnencodedUserId() {
        whenRequesting(req -> req.get("{userId}", clusterId1, UUID.randomUUID().toString()))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    void testDescribeUserNotFoundMissingEncodedNamespace() {
        var userId = Identifiers.encode("", UUID.randomUUID().toString());

        whenRequesting(req -> req.get("{userId}", clusterId1, userId))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    void testDescribeUserNotFoundWrongNamespaceEncoded() {
        var userNames = clusterUserNames.get(NAMESPACE + "/test-kafka1");
        var userId = Identifiers.encode("", "other-namespace", userNames.get(0));

        whenRequesting(req -> req.get("{userId}", clusterId1, userId))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    void testDescribeUserNotFoundNoSuchUser() {
        var userId = Identifiers.encode("", NAMESPACE, UUID.randomUUID().toString());

        whenRequesting(req -> req.get("{userId}", clusterId1, userId))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()));
    }
}
