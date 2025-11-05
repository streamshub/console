package com.github.streamshub.console.api;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.eclipse.microprofile.config.Config;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.github.streamshub.console.api.model.KafkaCluster;
import com.github.streamshub.console.api.service.MetricsService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
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
import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@TestHTTPEndpoint(KafkaClustersResource.class)
@TestProfile(TestPlainProfile.class)
class KafkaClustersResourceMetricsIT implements ClientRequestFilter {

    static final JsonObject EMPTY_METRICS = Json.createObjectBuilder()
            .add("data", Json.createObjectBuilder()
                    .add("result", Json.createArrayBuilder()))
            .build();

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @Inject
    Map<String, KafkaContext> configuredContexts;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    MetricsService metricsService;

    TestHelper utils;

    String clusterId1;
    URI bootstrapServers;

    Consumer<ClientRequestContext> filterQuery;
    Consumer<ClientRequestContext> filterQueryRange;

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
    void setup(TestInfo testInfo) {
        filterQuery = ctx -> { /* No-op */ };
        filterQueryRange = ctx -> { /* No-op */ };
        metricsService.setAdditionalFilter(Optional.of(this));

        String metricsSource;

        /*
         * Create a mock Prometheus configuration and point test-kafka1 to use it. A client
         * will be created when the Kafka CR is discovered. The request filter mock created
         * above is our way to intercept outbound requests and abort them with the desired
         * response for each test.
         *
         * metricsSources referenced by name here are configured in the TestPlainProfile and
         * are parsed/configured at startup, then referenced by the test-kafka1 configuration
         * for tests here.
         */
        if (testInfo.getTags().contains("openshift-monitoring")) {
            metricsSource = "test-openshift-monitoring";
        } else if (testInfo.getTags().contains("unauthenticated")) {
            metricsSource = "test-unauthenticated";
        } else if (testInfo.getTags().contains("bearer-token")) {
            metricsSource = "test-bearer-token";
        } else if (testInfo.getTags().contains("oidc")) {
            metricsSource = "test-oidc";
        } else {
            metricsSource = "test-basic";
        }

        consoleConfig.getKafka().getCluster("default/test-kafka1").get().setMetricsSource(metricsSource);

        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));

        utils = new TestHelper(bootstrapServers, config);

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

        // Wait for the added cluster to be configured in the context map
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> configuredContexts.values()
                    .stream()
                    .map(KafkaContext::clusterConfig)
                    .map(KafkaClusterConfig::clusterKey)
                    .anyMatch(Cache.metaNamespaceKeyFunc(kafka1)::equals));

        clusterId1 = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
    }

    @AfterEach
    void teardown() {
        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    @Test
    void testDescribeClusterWithMetricsSetsBasicHeader() {
        AtomicReference<String> queryAuthHeader = new AtomicReference<>();
        AtomicReference<String> queryRangeAuthHeader = new AtomicReference<>();

        filterQuery = ctx -> {
            queryAuthHeader.set(ctx.getHeaderString(HttpHeaders.AUTHORIZATION));
            ctx.abortWith(Response.ok(EMPTY_METRICS).build());
        };

        filterQueryRange = ctx -> {
            queryRangeAuthHeader.set(ctx.getHeaderString(HttpHeaders.AUTHORIZATION));
            ctx.abortWith(Response.ok(EMPTY_METRICS).build());
        };

        whenRequesting(req -> req
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name,metrics")
                .get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()));

        String expected = "Basic " + Base64.getEncoder().encodeToString("pr0m3th3u5:password42".getBytes());
        assertEquals(expected, queryAuthHeader.get());
        assertEquals(expected, queryRangeAuthHeader.get());
    }

    @Test
    @Tag("openshift-monitoring")
    void testDescribeClusterWithMetricsSetsOpenShiftBearerHeader() {
        AtomicReference<String> queryAuthHeader = new AtomicReference<>();
        AtomicReference<String> queryRangeAuthHeader = new AtomicReference<>();

        filterQuery = ctx -> {
            queryAuthHeader.set(ctx.getHeaderString(HttpHeaders.AUTHORIZATION));
            ctx.abortWith(Response.ok(EMPTY_METRICS).build());
        };

        filterQueryRange = ctx -> {
            queryRangeAuthHeader.set(ctx.getHeaderString(HttpHeaders.AUTHORIZATION));
            ctx.abortWith(Response.ok(EMPTY_METRICS).build());
        };

        whenRequesting(req -> req
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name,metrics")
                .get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()));

        assertTrue(queryAuthHeader.get().startsWith("Bearer "));
        assertTrue(queryRangeAuthHeader.get().startsWith("Bearer "));
    }

    @Test
    @Tag("bearer-token")
    void testDescribeClusterWithMetricsSetsBearerHeader() {
        AtomicReference<String> queryAuthHeader = new AtomicReference<>();
        AtomicReference<String> queryRangeAuthHeader = new AtomicReference<>();

        filterQuery = ctx -> {
            queryAuthHeader.set(ctx.getHeaderString(HttpHeaders.AUTHORIZATION));
            ctx.abortWith(Response.ok(EMPTY_METRICS).build());
        };

        filterQueryRange = ctx -> {
            queryRangeAuthHeader.set(ctx.getHeaderString(HttpHeaders.AUTHORIZATION));
            ctx.abortWith(Response.ok(EMPTY_METRICS).build());
        };

        whenRequesting(req -> req
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name,metrics")
                .get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()));

        String expected = "Bearer my-bearer-token";
        assertEquals(expected, queryAuthHeader.get());
        assertEquals(expected, queryRangeAuthHeader.get());
    }

    @Test
    @Tag("oidc")
    void testDescribeClusterWithMetricsSetsOIDCBearerHeader() throws Exception {
        AtomicReference<String> queryAuthHeader = new AtomicReference<>();
        AtomicReference<String> queryRangeAuthHeader = new AtomicReference<>();

        filterQuery = ctx -> {
            queryAuthHeader.set(ctx.getHeaderString(HttpHeaders.AUTHORIZATION));
            ctx.abortWith(Response.ok(EMPTY_METRICS).build());
        };

        filterQueryRange = ctx -> {
            queryRangeAuthHeader.set(ctx.getHeaderString(HttpHeaders.AUTHORIZATION));
            ctx.abortWith(Response.ok(EMPTY_METRICS).build());
        };

        whenRequesting(req -> req
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name,metrics")
                .get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()));

        String authHeader = queryAuthHeader.get();
        assertTrue(authHeader.startsWith("Bearer "));
        assertEquals(authHeader, queryRangeAuthHeader.get());
        var claims = new JwtConsumerBuilder()
            .setSkipAllValidators()
            .setSkipSignatureVerification()
            .build()
            .processToClaims(authHeader.substring("Bearer ".length()));
        assertEquals("service-account-registry-api", claims.getClaimValue("preferred_username", String.class));
    }

    @Test
    @Tag("unauthenticated")
    void testDescribeClusterWithMetricsWithoutAuthorizationHeader() {
        AtomicReference<String> queryAuthHeader = new AtomicReference<>();
        AtomicReference<String> queryRangeAuthHeader = new AtomicReference<>();

        filterQuery = ctx -> {
            ctx.abortWith(Response.ok(EMPTY_METRICS).build());
        };

        filterQueryRange = ctx -> {
            ctx.abortWith(Response.ok(EMPTY_METRICS).build());
        };

        whenRequesting(req -> req
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name,metrics")
                .get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()));

        assertNull(queryAuthHeader.get());
        assertNull(queryRangeAuthHeader.get());
    }

    @Test
    void testDescribeClusterWithEmptyMetrics() {
        filterQuery = ctx -> {
            ctx.abortWith(Response.ok(EMPTY_METRICS).build());
        };

        filterQueryRange = ctx -> {
            ctx.abortWith(Response.ok(EMPTY_METRICS).build());
        };

        whenRequesting(req -> req
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name,metrics")
                .get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", equalTo("test-kafka1"))
            .body("data.attributes.metrics", allOf(
                    hasEntry(is("values"), anEmptyMap()),
                    hasEntry(is("ranges"), anEmptyMap())));
    }

    @Test
    void testDescribeClusterWithMetricsErrors() {
        filterQuery = ctx -> {
            Response error = Response.status(Status.SERVICE_UNAVAILABLE)
                    .entity("EXPECTED: Prometheus is not available")
                    .build();
            throw new WebApplicationException(error);
        };

        filterQueryRange = ctx -> {
            throw new RuntimeException("EXPECTED");
        };

        whenRequesting(req -> req
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name,metrics")
                .get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", equalTo("test-kafka1"))
            .body("data.attributes.metrics", allOf(
                    hasEntry(is("values"), anEmptyMap()),
                    hasEntry(is("ranges"), anEmptyMap())));
    }

    @Test
    void testDescribeClusterWithMetricsValues() {
        Instant t1 = Instant.now().minusSeconds(1);
        Instant t2 = Instant.now();

        filterQuery = ctx -> {
            ctx.abortWith(Response.ok(Json.createObjectBuilder()
                    .add("data", Json.createObjectBuilder()
                        .add("result", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                .add("metric", Json.createObjectBuilder()
                                    .add(MetricsService.METRIC_NAME, "value-metric1")
                                    .add("custom-attribute", "attribute-value"))
                                .add("value", Json.createArrayBuilder()
                                    .add(t1.toEpochMilli() / 1000f)
                                    .add("42")))))
                    .build())
                .build());
        };

        filterQueryRange = ctx -> {
            ctx.abortWith(Response.ok(Json.createObjectBuilder()
                    .add("data", Json.createObjectBuilder()
                        .add("result", Json.createArrayBuilder()
                            .add(Json.createObjectBuilder()
                                .add("metric", Json.createObjectBuilder()
                                    .add(MetricsService.METRIC_NAME, "range-metric1")
                                    .add("custom-attribute", "attribute-value"))
                                .add("values", Json.createArrayBuilder()
                                    .add(Json.createArrayBuilder()
                                        .add((double) t1.toEpochMilli() / 1000f)
                                        .add("2.718"))
                                    .add(Json.createArrayBuilder()
                                        .add((double) t2.toEpochMilli() / 1000f)
                                        .add("3.1415"))))))
                    .build())
                .build());
        };

        whenRequesting(req -> req
                .param("fields[" + KafkaCluster.API_TYPE + "]", "name,metrics")
                .get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.name", equalTo("test-kafka1"))
            .body("data.attributes.metrics.values.value-metric1", contains(allOf(
                    aMapWithSize(2),
                    hasEntry("value", "42"),
                    hasEntry("custom-attribute", "attribute-value"))))
            .body("data.attributes.metrics.ranges.range-metric1", contains(allOf(
                    aMapWithSize(2),
                    //hasEntry("range", arrayContaining("", "")),
                    hasEntry("custom-attribute", "attribute-value")
            )));
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
