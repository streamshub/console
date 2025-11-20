package com.github.streamshub.console.api;

import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.security.scram.ScramLoginModule;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import com.github.streamshub.console.api.model.KafkaCluster;
import com.github.streamshub.console.api.model.ListFetchParams;
import com.github.streamshub.console.api.security.ConsoleAuthenticationMechanism;
import com.github.streamshub.console.api.service.KafkaClusterService;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.Holder;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.config.security.Decision;
import com.github.streamshub.console.config.security.GlobalSecurityConfigBuilder;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.ResourceTypes;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.test.AdminClientSpy;
import com.github.streamshub.console.test.LogCapture;
import com.github.streamshub.console.test.TestHelper;
import com.github.streamshub.console.test.VarargsAggregator;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationCustomBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationOAuthBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationScramSha512Builder;
import io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static java.util.Objects.isNull;
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
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
@TestHTTPEndpoint(KafkaClustersResource.class)
@TestProfile(TestPlainProfile.class)
class KafkaClustersResourceIT {

    /**
     * List of Kafka clusters that are always created. test-kafkaY is configured in {@link TestPlainProfile}
     * but has no associated Strimzi Kafka CR.
     */
    static final List<String> STATIC_KAFKAS = List.of("test-kafka1", "test-kafka2", "test-kafkaY");

    static LogCapture auditLogCapture = LogCapture.with(logRecord -> logRecord
            .getLoggerName()
            .equals(ConsoleAuthenticationMechanism.class.getName()),
            Level.INFO);

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @Inject
    Holder<SharedIndexInformer<Kafka>> kafkaInformer;

    @Inject
    Map<String, KafkaContext> configuredContexts;

    @Inject
    KafkaClusterService kafkaClusterService;

    @Inject
    ConsoleConfig consoleConfig;

    TestHelper utils;

    String clusterId1;
    String clusterId2;
    URI bootstrapServers;
    URI randomBootstrapServers;

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
        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        randomBootstrapServers = URI.create(consoleConfig.getKafka()
                .getCluster("default/test-kafka2")
                .map(k -> k.getProperties().get("bootstrap.servers"))
                .orElseThrow());

        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);

        client.resources(Kafka.class).inAnyNamespace().delete();
        auditLogCapture.records().clear();

        utils.apply(client, new KafkaBuilder(utils.buildKafkaResource("test-kafka1", utils.getClusterId(), bootstrapServers,
                        new KafkaListenerAuthenticationCustomBuilder()
                        .withSasl()
                        .addToListenerConfig("sasl.enabled.mechanisms", "oauthbearer")
                        .build()))
            .editOrNewStatus()
                .addNewCondition()
                    .withType("Ready")
                    .withStatus("True")
                .endCondition()
                .addNewKafkaNodePool()
                    .withName("my-node-pool")
                .endKafkaNodePool()
            .endStatus()
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

        // Wait for the context map to be populated with all Kafka configurations
        await().atMost(10, TimeUnit.SECONDS).until(() -> configuredContexts.size() == STATIC_KAFKAS.size());

        clusterId1 = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
        clusterId2 = consoleConfig.getKafka().getCluster("default/test-kafka2").get().getId();
        kafkaClusterService.setListUnconfigured(false);
    }

    @Test
    void testListClusters() {
        String k1Bootstrap = bootstrapServers.getHost() + ":" + bootstrapServers.getPort();
        String k2Bootstrap = randomBootstrapServers.getHost() + ":" + randomBootstrapServers.getPort();

        whenRequesting(req -> req.queryParam("fields[kafkas]", "name,status,nodePools,listeners").get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(STATIC_KAFKAS.size()))
            .body("data.id", containsInAnyOrder(clusterId1, clusterId2, "test-kafkaY"))
            .body("data.attributes.name", containsInAnyOrder("test-kafka1", "test-kafka2", "test-kafkaY"))
            .body("data.find { it.attributes.name == 'test-kafka1'}.attributes.status", is("Ready"))
            .body("data.find { it.attributes.name == 'test-kafka1'}.attributes.nodePools", contains("my-node-pool"))
            .body("data.find { it.attributes.name == 'test-kafka1'}.attributes.listeners", hasItem(allOf(
                    hasEntry("bootstrapServers", k1Bootstrap),
                    hasEntry("authType", "custom"))))
            .body("data.find { it.attributes.name == 'test-kafka2'}.attributes.status", is("NotReady"))
            .body("data.find { it.attributes.name == 'test-kafka2'}.attributes.listeners", hasItem(allOf(
                    hasEntry(equalTo("bootstrapServers"), equalTo(k2Bootstrap)),
                    hasEntry(equalTo("authType"), nullValue(String.class)))))
            .body("data.find { it.attributes.name == 'test-kafkaY'}.attributes.status", is(nullValue()))
            .body("data.find { it.attributes.name == 'test-kafkaY'}.attributes.listeners", is(nullValue()));
    }

    @Test
    void testListClustersWithNameFilter() {
        whenRequesting(req ->
            req.queryParam("filter[name]", "test-kafka1")
               .queryParam("fields[kafkas]", "name")
               .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(1))
            .body("data.attributes.name", contains("test-kafka1"));
    }

    @ParameterizedTest
    @CsvSource({
        "'test-kafka1',                   test-kafka1",
        "'test-kafka1,test-kafkaY',       test-kafka1, test-kafkaY",
        "'/(?:.*\\/)?test-kafka[1Y]/',    test-kafka1, test-kafkaY",
        "'/(?:.*\\/)?test-kafka(?:1|Y)/', test-kafka1, test-kafkaY",
        "'/(?:.*\\/)?test-kafkaZ/'",      // matches none, nothing visible
        "'/ '",                           // matches none, nothing visible
        "' /'",                           // matches none, nothing visible
        "'test-kafkaZ'",                  // matches none, nothing visible
        "'*',                             test-kafka1, test-kafka2, test-kafkaY",
    })
    void testListClustersWithAnonymousLimited(String resourceNames, @AggregateWith(VarargsAggregator.class) String... visibleClusters) {
        consoleConfig.setSecurity(new GlobalSecurityConfigBuilder()
                .addNewRole()
                    .withName("unauthenticated")
                    .addNewRule()
                        .withResources("kafkas")
                        .withPrivileges(Privilege.LIST)
                        .withResourceNames(resourceNames.split(","))
                    .endRule()
                .endRole()
                .addNewSubject()
                    // ANONYMOUS is a special subject name for unauthenticated requests
                    .withInclude("ANONYMOUS")
                    .withRoleNames("unauthenticated")
                .endSubject()
            .build());

        whenRequesting(req -> req.param("fields[" + KafkaCluster.API_TYPE + "]", "name")
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(visibleClusters.length))
            .body("data.attributes.name", containsInAnyOrder(visibleClusters))
            .body("data.meta.privileges", everyItem(is(List.of(Privilege.LIST.name()))));
    }

    @Test
    void testListClustersWithInformerError() {
        SharedIndexInformer<Kafka> informer = Mockito.mock();

        var informerType = new TypeLiteral<Holder<SharedIndexInformer<Kafka>>>() {
            private static final long serialVersionUID = 1L;
        };

        // Force an unhandled exception
        Mockito.when(informer.getStore()).thenThrow(new RuntimeException("EXPECTED TEST EXCEPTION") {
            private static final long serialVersionUID = 1L;

            @Override
            public synchronized Throwable fillInStackTrace() {
                return this;
            }
        });

        QuarkusMock.installMockForType(Holder.of(informer), informerType, NamedLiteral.of("KafkaInformer"));

        whenRequesting(req -> req.get())
            .assertThat()
            .statusCode(is(Status.INTERNAL_SERVER_ERROR.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("500"))
            .body("errors.code", contains("5001"));
    }

    @ParameterizedTest
    @CsvSource({
        "'name' , 'test-kafka1,test-kafka2,test-kafkaY'",
        "'-name', 'test-kafkaY,test-kafka2,test-kafka1'"
    })
    void testListClustersSortedByName(String sortParam, String expectedNameList) {
        whenRequesting(req -> req.queryParam("sort", sortParam).get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(STATIC_KAFKAS.size()))
            .body("data.attributes.name", contains(expectedNameList.split(",")));
    }

    @Test
    void testListClustersContainsPageMetaData() {
        whenRequesting(req -> req.get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.page.total", is(STATIC_KAFKAS.size()))
            .body("data.size()", is(STATIC_KAFKAS.size()))
            .body("data.meta.page", everyItem(hasKey(equalTo("cursor"))));
    }

    @Test
    void testListClustersWithRangePaginationTruncated() {
        kafkaClusterService.setListUnconfigured(true);
        mockAdminClient();

        List<String> allKafkaNames = Stream.concat(STATIC_KAFKAS.stream(),
                IntStream.range(3, 10)
                    .mapToObj(i -> "test-kafka" + i)
                    .map(name -> utils.buildKafkaResource(name, randomBootstrapServers))
                    .map(kafka -> utils.apply(client, kafka))
                    .map(kafka -> kafka.getMetadata().getName()))
            .toList();

        // Wait for the informer cache to be populated with all Kafka CRs (subtract 1 to account for kafkaY w/o CR)
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Objects.equals(kafkaInformer.get().getStore().list().size(), allKafkaNames.size() - 1));

        var fullResponse = whenRequesting(req -> req.queryParam("sort", "name").get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.page.total", is(allKafkaNames.size()))
            .body("data.size()", is(allKafkaNames.size()))
            .body("data.meta.page", everyItem(hasKey(equalTo("cursor"))))
            .extract()
            .asInputStream();

        JsonObject responseJson;

        try (var reader = Json.createReader(fullResponse)) {
            responseJson = reader.readObject();
        }

        // Select the sub-list of Kafka clusters between first and last records
        String pageAfter = ((JsonString) responseJson.getValue("/data/0/meta/page/cursor")).getString();
        String pageBefore = ((JsonString) responseJson.getValue("/data/8/meta/page/cursor")).getString();

        whenRequesting(req -> req
                .queryParam("sort", "name")
                .queryParam("page[size]", 6)
                .queryParam("page[after]", pageAfter)
                .queryParam("page[before]", pageBefore)
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.page.total", is(allKafkaNames.size())) // total is the count for the full/unpaged result set
            // requested range has 7 recs, but truncated to 6 to satisfy page[size]
            .body("meta.page.rangeTruncated", is(true))
            .body("data.size()", is(6))
            .body("data.meta.page", everyItem(hasKey(equalTo("cursor"))));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 8,   , 6", // skip first two and last one (6 remain)
        "1,  , 10, 8", // skip first two (8 remain)
        " , 8,   , 8", // skip last one (8 remain)
        " , 8,  5, 5", // skip last one (8 remain), limit to 5 on page
    })
    void testListClustersWithPaginationCursors(Integer afterIndex, Integer beforeIndex, Integer pageSize, int expectedResultCount) {
        kafkaClusterService.setListUnconfigured(true);
        mockAdminClient();

        List<String> allKafkaNames = Stream.concat(STATIC_KAFKAS.stream(),
                IntStream.range(3, 10)
                    .mapToObj(i -> "test-kafka" + i)
                    .map(name -> utils.buildKafkaResource(name, randomBootstrapServers))
                    .map(kafka -> utils.apply(client, kafka))
                    .map(kafka -> kafka.getMetadata().getName()))
                .sorted()
                .toList();

        // Wait for the informer cache to be populated with all Kafka CRs (subtract 1 to account for kafkaY w/o CR)
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Objects.equals(kafkaInformer.get().getStore().list().size(), allKafkaNames.size() - 1));

        var fullResponse = whenRequesting(req -> req.queryParam("sort", "name").get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.page.total", is(allKafkaNames.size()))
            .body("data.size()", is(allKafkaNames.size()))
            .body("data.meta.page", everyItem(hasKey(equalTo("cursor"))))
            .extract()
            .asInputStream();

        JsonObject responseJson;

        try (var reader = Json.createReader(fullResponse)) {
            responseJson = reader.readObject();
        }

        Map<String, Object> parametersMap = new HashMap<>();
        parametersMap.put("sort", "name");
        if (pageSize != null) {
            parametersMap.put("page[size]", pageSize);
        }

        utils.getCursor(responseJson, afterIndex)
            .ifPresent(cursor -> parametersMap.put("page[after]", cursor));
        utils.getCursor(responseJson, beforeIndex)
            .ifPresent(cursor -> parametersMap.put("page[before]", cursor));

        var rangeResponse = whenRequesting(req -> req
                .queryParams(parametersMap)
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.page.total", is(allKafkaNames.size())) // total is the count for the full/unpaged result set
            .body("data.size()", is(expectedResultCount))
            .body("data.meta.page", everyItem(hasKey(equalTo("cursor"))))
            .extract()
            .asInputStream();

        JsonObject rangeResponseJson;

        try (var reader = Json.createReader(rangeResponse)) {
            rangeResponseJson = reader.readObject();
        }

        /*
         * The page size is dynamic depending on whether it is (1) provided
         * by the client or (2) is a range request
         */
        int expectedPageSize = Optional.ofNullable(pageSize)
                .orElseGet(() -> isNull(afterIndex) || isNull(beforeIndex)
                        ? ListFetchParams.PAGE_SIZE_DEFAULT
                        : ListFetchParams.PAGE_SIZE_MAX);
        int toIndex = Optional.ofNullable(beforeIndex).orElse(allKafkaNames.size());
        int fromIndex = Optional.ofNullable(afterIndex).map(a -> a + 1)
                .orElseGet(() -> Math.max(toIndex - expectedPageSize, 0));

        List<String> expectedNames = allKafkaNames.subList(fromIndex, toIndex);

        for (int i = 0; i < expectedResultCount; i++) {
            assertEquals(expectedNames.get(i),
                    ((JsonString) rangeResponseJson.getValue("/data/%d/attributes/name".formatted(i))).getString());
        }
    }

    @Test
    void testListClustersWithUndecodablePageCursor() {
        whenRequesting(req -> req
                .param("page[after]", "This-is-not-base64!")
                .get())
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.source.parameter", contains("page[after]"));
    }

    @Test
    void testListClustersWithUnparseablePageCursor() {
        whenRequesting(req -> req
                .param("page[after]", Base64.getEncoder().encodeToString("{ Not-valid-JSON ]".getBytes()))
                .get())
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.source.parameter", contains("page[after]"));
    }

    @Test
    void testListClustersWithIncorrectlyFormattedPageCursor() {
        whenRequesting(req -> req
                // the "id" is expected as a string, not number
                .param("page[after]", Base64.getEncoder().encodeToString("{ \"id\": 123 }".getBytes()))
                .get())
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.source.parameter", contains("page[after]"));
    }

    @Test
    void testListClustersWithPageSizeTooLarge() {
        whenRequesting(req -> req
                .param("page[size]", ListFetchParams.PAGE_SIZE_MAX + 1)
                .get())
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4002"))
            .body("errors.meta.page.maxSize", contains(ListFetchParams.PAGE_SIZE_MAX))
            .body("errors.links.type", contains(ErrorCategory.MaxPageSizeExceededError.TYPE_LINK))
            .body("errors.source.parameter", contains("page[size]"));
    }

    @Test
    void testListClustersWithQuotedAndNullableSortFields() {
        whenRequesting(req -> req
                .param("sort", "-someObject.\"dot.separated.key\",name")
                .param("page[before]", Base64.getEncoder()
                        .encodeToString(Json.createObjectBuilder()
                                .add("id", new UUID(0, 0).toString())
                                .add("attributes", Json.createObjectBuilder()
                                        .add("name", "test-kafka2")
                                        .add("someObject", Json.createObjectBuilder()
                                                .add("dot.separated.key", 1)))
                                .build()
                                .toString()
                                .getBytes()))
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes.name[0]", is("test-kafka1"));
    }

    @Test
    void testListClustersWithUnexpectedPageCursorData() {
        whenRequesting(req -> req
                .param("page[after]", Base64.getEncoder()
                        .encodeToString(Json.createObjectBuilder()
                                .add("id", "123")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("unexpectedArray", Json.createArrayBuilder()
                                                .add("1st")
                                                .add("2nd")
                                                .add("3rd")))
                                .build()
                                .toString()
                                .getBytes()))
                .get())
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.source.parameter", contains("page[after]"));
    }

    @ParameterizedTest
    @CsvSource({
        "test-kafka1",
        "default/test-kafka1"
    })
    void testListClustersWithAuditLogging(String resourceName) {
        consoleConfig.setSecurity(new GlobalSecurityConfigBuilder()
                .addNewAudit()
                    .withDecision(Decision.ALL)
                    .withResources(ResourceTypes.Global.KAFKAS.value())
                    .withResourceNames(resourceName)
                    .withPrivileges(Privilege.LIST)
                .endAudit()
                .build());

        whenRequesting(req -> req
                .queryParam("fields[kafkas]", "name")
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(STATIC_KAFKAS.size()));

        var auditLogs = auditLogCapture.records();
        final String auditTmpl = "ANONYMOUS allowed console:kafkas:[%s]:[%s]";

        assertThat(auditLogs, not(hasItem(hasProperty("message", containsString("denied")))));
        assertThat(auditLogs, hasItem(both(hasProperty("message", containsString(auditTmpl.formatted("", Privilege.LIST))))
                .and(hasProperty("level", equalTo(Level.INFO)))));

        assertThat(auditLogs, hasItem(both(hasProperty("message", containsString(auditTmpl.formatted("default/test-kafka1", Privilege.LIST))))
                .and(hasProperty("level", equalTo(Level.INFO)))));
        assertThat(auditLogs, not(hasItem(hasProperty("message", containsString(auditTmpl.formatted("test-kafka2", Privilege.LIST))))));
        assertThat(auditLogs, not(hasItem(hasProperty("message", containsString(auditTmpl.formatted("test-kafkaY", Privilege.LIST))))));
    }

    @Test
    void testDescribeClusterWithCustomAuthType() {
        mockAdminClient();

        whenRequesting(req -> req
                .auth()
                    .oauth2("fake-access-token")
                .get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", equalTo(clusterId1))
            .body("data.attributes.name", equalTo("test-kafka1"))
            .body("data.attributes.listeners", hasItem(allOf(
                    hasEntry("bootstrapServers", bootstrapServers.getHost() + ":" + bootstrapServers.getPort()),
                    hasEntry("authType", "custom"))));
    }

    @Test
    void testDescribeClusterWithCertificates() {
        String clusterId = UUID.randomUUID().toString();

        /*
         * Create a Kafka CR that proxies to kafka1.
         * test-kafka3 is predefined in KafkaUnsecuredResourceManager
         */
        Kafka kafka = new KafkaBuilder(utils.buildKafkaResource("test-kafka3", clusterId, bootstrapServers))
                .editSpec()
                    .editKafka()
                        .editMatchingListener(l -> "listener0".equals(l.getName()))
                            .withTls(true)
                        .endListener()
                    .endKafka()
                .endSpec()
                .editStatus()
                    .editMatchingListener(l -> "listener0".equals(l.getName()))
                        .addToCertificates("""
                                -----BEGIN CERTIFICATE-----
                                FAKE/CERTIFICATE
                                -----END CERTIFICATE-----
                                """)
                    .endListener()
                .endStatus()
                .build();

        Map<String, Object> clientConfig = mockAdminClient();

        utils.apply(client, kafka);

        // Wait for the added cluster to be configured in the context map
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> configuredContexts.values()
                    .stream()
                    .map(KafkaContext::clusterConfig)
                    .map(KafkaClusterConfig::clusterKey)
                    .anyMatch(Cache.metaNamespaceKeyFunc(kafka)::equals));

        whenRequesting(req -> req.get("{clusterId}", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", equalTo("test-kafka3"));
            // Ignoring response data since they are from test-kafka-1

        assertEquals("SSL", clientConfig.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        assertEquals("PEM", clientConfig.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG));
        assertThat((String) clientConfig.get(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG), containsString("FAKE/CERTIFICATE"));
    }

    @Test
    void testDescribeClusterWithTLSMissingCertificates() {
        String clusterId = UUID.randomUUID().toString();

        /*
         * Create a Kafka CR without certificates
         * test-kafka3 is predefined in KafkaUnsecuredResourceManager
         */
        Kafka kafka = new KafkaBuilder(utils.buildKafkaResource("test-kafka3", clusterId, bootstrapServers))
                .editSpec()
                    .editKafka()
                        .editMatchingListener(l -> "listener0".equals(l.getName()))
                            .withTls(true)
                        .endListener()
                    .endKafka()
                .endSpec()
                .build();

        Map<String, Object> clientConfig = mockAdminClient();

        utils.apply(client, kafka);

        // Wait for the added cluster to be configured in the context map
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> configuredContexts.values()
                    .stream()
                    .map(KafkaContext::clusterConfig)
                    .map(KafkaClusterConfig::clusterKey)
                    .anyMatch(Cache.metaNamespaceKeyFunc(kafka)::equals));

        whenRequesting(req -> req.get("{clusterId}", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", equalTo("test-kafka3"));
            // Ignoring response data since they are from test-kafka-1

        assertEquals("SSL", clientConfig.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        assertThat(clientConfig, not(hasKey(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)));
        assertThat(clientConfig, not(hasKey(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG)));
    }

    @Test
    void testDescribeClusterWithCustomNonOAuth() {
        String clusterId = UUID.randomUUID().toString();

        // Create a Kafka CR with generic custom authentication that proxies to kafka1
        utils.apply(client, utils.buildKafkaResource("test-kafka-" + clusterId, clusterId, bootstrapServers,
                new KafkaListenerAuthenticationCustomBuilder().build()));

        whenRequesting(req -> req.get("{clusterId}", clusterId))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"));
    }

    @Test
    void testDescribeClusterWithKafkaUnavailable() {
        whenRequesting(req -> req.get("{clusterId}", clusterId2))
            .assertThat()
            .statusCode(is(Status.GATEWAY_TIMEOUT.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("504"))
            .body("errors.code", contains("5041"));
    }

    @Test
    void testDescribeClusterWithNoSuchCluster() {
        whenRequesting(req -> req.get("{clusterId}", UUID.randomUUID().toString()))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("404"))
            .body("errors.code", contains("4041"));
    }

    @ParameterizedTest
    @CsvSource({
        "true, SASL_SSL",
        "false, SASL_PLAINTEXT"
    })
    void testDescribeClusterWithOAuthTokenUrl(boolean tls, String expectedProtocol) {
        String clusterId = UUID.randomUUID().toString();

        /*
         * Create a Kafka CR that proxies to kafka1.
         * test-kafka3 is predefined in KafkaUnsecuredResourceManager
         */
        Kafka kafka = new KafkaBuilder(utils.buildKafkaResource("test-kafka3", clusterId, bootstrapServers,
                    new KafkaListenerAuthenticationOAuthBuilder()
                        .withTokenEndpointUri("https://example.com/token")
                    .build()))
                .editSpec()
                    .editKafka()
                    .editMatchingListener(l -> "listener0".equals(l.getName()))
                        .withTls(tls)
                    .endListener()
                .endKafka()
                .endSpec()
                .build();

        Map<String, Object> clientConfig = mockAdminClient();

        utils.apply(client, kafka);

        // Wait for the added cluster to be configured in the context map
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> configuredContexts.values()
                    .stream()
                    .map(KafkaContext::clusterConfig)
                    .map(KafkaClusterConfig::clusterKey)
                    .anyMatch(Cache.metaNamespaceKeyFunc(kafka)::equals));

        whenRequesting(req -> req
                .auth().oauth2("my-secure-token")
                .get("{clusterId}", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is("test-kafka3"))
            .body("data.meta.authentication.method", is("oauth"))
            .body("data.meta.authentication.tokenUrl", is("https://example.com/token"));

        assertEquals(expectedProtocol, clientConfig.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        assertEquals("OAUTHBEARER", clientConfig.get(SaslConfigs.SASL_MECHANISM));
        assertEquals(JaasClientOauthLoginCallbackHandler.class.getName(),
                clientConfig.get(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS));
    }

    @ParameterizedTest
    @CsvSource({
        "true, SASL_SSL",
        "false, SASL_PLAINTEXT"
    })
    void testDescribeClusterWithScram(boolean tls, String expectedProtocol) {
        String clusterId = UUID.randomUUID().toString();

        /*
         * Create a Kafka CR with SCRAM-SHA that proxies to kafka1
         * test-kafka3 is predefined in KafkaUnsecuredResourceManager
         */
        Kafka kafka = new KafkaBuilder(utils.buildKafkaResource("test-kafka3", clusterId, bootstrapServers,
                    new KafkaListenerAuthenticationScramSha512Builder().build()))
                .editSpec()
                    .editKafka()
                    .editMatchingListener(l -> "listener0".equals(l.getName()))
                        .withTls(tls)
                    .endListener()
                .endKafka()
                .endSpec()
                .build();

        Map<String, Object> clientConfig = mockAdminClient();

        utils.apply(client, kafka);

        // Wait for the added cluster to be configured in the context map
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> configuredContexts.values()
                    .stream()
                    .map(KafkaContext::clusterConfig)
                    .map(KafkaClusterConfig::clusterKey)
                    .anyMatch(Cache.metaNamespaceKeyFunc(kafka)::equals));

        whenRequesting(req -> req
                .auth().basic("u", "p")
                .get("{clusterId}", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is("test-kafka3"))
            .body("data.meta.authentication.method", is("basic"));

        assertEquals(expectedProtocol, clientConfig.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
        assertEquals("SCRAM-SHA-512", clientConfig.get(SaslConfigs.SASL_MECHANISM));
        assertThat(String.valueOf(clientConfig.get(SaslConfigs.SASL_JAAS_CONFIG)),
                containsString(ScramLoginModule.class.getName()));
    }

    @Test
    /*
     * Tests with metrics enabled are in KafkaClustersResourceMetricsIT
     */
    void testDescribeClusterWithMetricsNotEnabled() {
        whenRequesting(req -> req
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name,metrics")
                .get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", equalTo("test-kafka1"))
            .body("data.attributes", hasEntry(is("metrics"), nullValue()));
    }

    @ParameterizedTest
    @CsvSource({
        "true",
        "false"
    })
    void testPatchClusterReconciliationPaused(Boolean paused) {
        whenRequesting(req -> req.get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", is("test-kafka1"))
            .body("data.meta", not(hasKey("reconciliationPaused")));

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("id", clusterId1)
                                .add("type", com.github.streamshub.console.api.model.KafkaCluster.API_TYPE)
                                .add("meta", Json.createObjectBuilder()
                                        .add("reconciliationPaused", paused))
                                .add("attributes", Json.createObjectBuilder()))
                        .build()
                        .toString())
                .patch("{clusterId1}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.meta.reconciliationPaused", is(paused));

        Kafka kafkaCR = client.resources(Kafka.class)
            .inNamespace("default")
            .withName("test-kafka1")
            .get();

        assertEquals(paused.toString(), kafkaCR.getMetadata().getAnnotations().get(ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION));
        // Test custom annotation was untouched
        assertEquals("value-1", kafkaCR.getMetadata().getAnnotations().get("x-custom-annotation"));

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("id", clusterId1)
                                .add("type", com.github.streamshub.console.api.model.KafkaCluster.API_TYPE)
                                .add("meta", Json.createObjectBuilder()) // reconciliationPaused is omitted
                                .add("attributes", Json.createObjectBuilder()))
                        .build()
                        .toString())
                .patch("{clusterId1}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.meta", not(hasKey("reconciliationPaused")));

        kafkaCR = client.resources(Kafka.class)
                .inNamespace("default")
                .withName("test-kafka1")
                .get();

        assertNull(kafkaCR.getMetadata().getAnnotations().get(ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION));
        // Test custom annotation was untouched
        assertEquals("value-1", kafkaCR.getMetadata().getAnnotations().get("x-custom-annotation"));
    }

    @ParameterizedTest
    @CsvSource({
        "true ,      , 3.6.0, 3.6.0",  // status is null due to paused reconciliation, fallback to spec
        "false, 3.5.0, 3.6.0, 3.5.0" // status is present, use it
    })
    void testKafkaVersionWhenReconciliationPaused(
            boolean paused,
            String statusKafkaVersion,
            String specKafkaVersion,
            String expectedVersion) {
        String clusterId = UUID.randomUUID().toString();

        /*
         * Create a Kafka CR that proxies to kafka1.
         * test-kafka3 is predefined in KafkaUnsecuredResourceManager
         */
        Kafka kafka = new KafkaBuilder(utils.buildKafkaResource("test-kafka3", clusterId, bootstrapServers))
                .editSpec()
                    .editKafka()
                        .withVersion(specKafkaVersion)
                    .endKafka()
                .endSpec()
                .editStatus()
                    .withKafkaVersion(statusKafkaVersion)
                .endStatus()
                .build();
        utils.apply(client, kafka);
        // Wait for the added cluster to be configured in the context map
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> configuredContexts.values()
                    .stream()
                    .map(KafkaContext::clusterConfig)
                    .map(KafkaClusterConfig::clusterKey)
                    .anyMatch(Cache.metaNamespaceKeyFunc(kafka)::equals));
        whenRequesting(req -> req
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
            .body(Json.createObjectBuilder()
                    .add("data", Json.createObjectBuilder()
                             .add("id", clusterId)
                            .add("type", com.github.streamshub.console.api.model.KafkaCluster.API_TYPE)
                            .add("meta", Json.createObjectBuilder()
                                    .add("reconciliationPaused", paused))
                            .add("attributes", Json.createObjectBuilder()))
                    .build()
                    .toString())
            .patch("{clusterId1}", clusterId))
        .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.meta.reconciliationPaused", is(paused));
        whenRequesting(req -> req.get("{clusterId}", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.kafkaVersion", is(expectedVersion));
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
