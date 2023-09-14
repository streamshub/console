package com.github.eyefloaters.console.api;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

import com.github.eyefloaters.console.api.model.ListFetchParams;
import com.github.eyefloaters.console.api.support.ErrorCategory;
import com.github.eyefloaters.console.kafka.systemtest.TestPlainProfile;
import com.github.eyefloaters.console.kafka.systemtest.deployment.DeploymentManager;
import com.github.eyefloaters.console.test.TestHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.kubernetes.client.KubernetesServerTestResource;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationCustomBuilder;
import io.strimzi.test.container.StrimziKafkaContainer;

import static com.github.eyefloaters.console.test.TestHelper.whenRequesting;
import static java.util.Objects.isNull;
import static java.util.function.Predicate.not;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(KubernetesServerTestResource.class)
@TestHTTPEndpoint(KafkaClustersResource.class)
@TestProfile(TestPlainProfile.class)
class KafkaClustersResourceIT {

    private static final List<String> STATIC_KAFKAS = List.of("test-kafka1", "test-kafka2");

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @Inject
    SharedIndexInformer<Kafka> kafkaInformer;

    @DeploymentManager.InjectDeploymentManager
    DeploymentManager deployments;

    TestHelper utils;

    StrimziKafkaContainer kafkaContainer;
    String clusterId1;
    String clusterId2;
    URI bootstrapServers;
    ServerSocket randomSocket;
    URI randomBootstrapServers;

    @BeforeEach
    void setup() throws IOException {
        kafkaContainer = deployments.getKafkaContainer();
        bootstrapServers = URI.create(kafkaContainer.getBootstrapServers());
        randomSocket = new ServerSocket(0);
        randomBootstrapServers = URI.create("dummy://localhost:" + randomSocket.getLocalPort());

        utils = new TestHelper(bootstrapServers, config, null);

        clusterId1 = utils.getClusterId();
        clusterId2 = UUID.randomUUID().toString();

        client.resources(Kafka.class).delete();
        client.resources(Kafka.class)
            .resource(utils.buildKafkaResource("test-kafka1", clusterId1, bootstrapServers,
                new KafkaListenerAuthenticationCustomBuilder()
                    .withSasl()
                .build()))
            .create();
        // Second cluster is offline/non-existent
        client.resources(Kafka.class)
            .resource(utils.buildKafkaResource("test-kafka2", clusterId2, randomBootstrapServers))
            .create();
    }

    @AfterEach
    void teardown() throws IOException {
        client.resources(Kafka.class).list()
            .getItems()
            .stream()
            .filter(not(k -> Set.of("test-kafka1", "test-kafka2").contains(k.getMetadata().getName())))
            .forEach(k -> client.resource(k).delete());

        if (randomSocket != null) {
            randomSocket.close();
        }
    }

    @Test
    void testListClusters() {
        whenRequesting(req -> req.get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(2))
            .body("data.id", containsInAnyOrder(clusterId1, clusterId2))
            .body("data.attributes.name", containsInAnyOrder("test-kafka1", "test-kafka2"))
            .body("data.attributes.bootstrapServers", containsInAnyOrder(
                    bootstrapServers.getHost() + ":" + bootstrapServers.getPort(),
                    randomBootstrapServers.getHost() + ":" + randomBootstrapServers.getPort()))
            .body("data.attributes.authType", containsInAnyOrder(equalTo("custom"), nullValue()));
    }

    @Test
    void testListClustersWithInformerError() {
        SharedIndexInformer<Kafka> informer = Mockito.mock();

        var informerType = new TypeLiteral<SharedIndexInformer<Kafka>>() {
            private static final long serialVersionUID = 1L;
        };

        @SuppressWarnings("all")
        class NamedLiteral extends AnnotationLiteral<jakarta.inject.Named> implements jakarta.inject.Named {
            private static final long serialVersionUID = 1L;

            @Override
            public String value() {
                return "KafkaInformer";
            }
        }

        // Force an unhandled exception
        Mockito.when(informer.getStore()).thenThrow(new RuntimeException("EXPECTED TEST EXCEPTION") {
            private static final long serialVersionUID = 1L;

            @Override
            public synchronized Throwable fillInStackTrace() {
                return this;
            }
        });

        QuarkusMock.installMockForType(informer, informerType, new NamedLiteral());

        whenRequesting(req -> req.get("{clusterId}", UUID.randomUUID().toString()))
            .assertThat()
            .statusCode(is(Status.INTERNAL_SERVER_ERROR.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("500"))
            .body("errors.code", contains("5001"));
    }

    @ParameterizedTest
    @CsvSource({
        "'name' , 'test-kafka1,test-kafka2'",
        "'-name', 'test-kafka2,test-kafka1'"
    })
    void testListClustersSortedByName(String sortParam, String expectedNameList) {
        whenRequesting(req -> req.queryParam("sort", sortParam).get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(2))
            .body("data.attributes.name", contains(expectedNameList.split(",")));
    }

    @Test
    void testListClustersContainsPageMetaData() {
        whenRequesting(req -> req.get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.page.total", is(2))
            .body("data.size()", is(2))
            .body("data.meta.page", everyItem(hasKey(equalTo("cursor"))));
    }

    @Test
    void testListClustersWithRangePaginationTruncated() {
        List<String> allKafkaNames = Stream.concat(STATIC_KAFKAS.stream(),
                IntStream.range(3, 10)
                    .mapToObj(i -> "test-kafka" + i)
                    .map(name -> utils.buildKafkaResource(name, randomBootstrapServers))
                    .map(kafka -> client.resources(Kafka.class).resource(kafka).create())
                    .map(kafka -> kafka.getMetadata().getName()))
            .toList();

        // Wait for the informer cache to be populated with all Kafka CRs
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Objects.equals(kafkaInformer.getStore().list().size(), allKafkaNames.size()));

        var fullResponse = whenRequesting(req -> req.queryParam("sort", "name").get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.page.total", is(9))
            .body("data.size()", is(9))
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
            .body("meta.page.total", is(9)) // total is the count for the full/unpaged result set
            // requested range has 7 recs, but truncated to 6 to satisfy page[size]
            .body("meta.page.rangeTruncated", is(true))
            .body("data.size()", is(6))
            .body("data.meta.page", everyItem(hasKey(equalTo("cursor"))));
    }

    @ParameterizedTest
    @CsvSource({
        "1, 8,   , 6", // skip first two and last one (6 remain)
        "1,  , 10, 7", // skip first two (7 remain)
        " , 8,   , 8", // skip last one (8 remain)
        " , 8,  5, 5", // skip last one (8 remain), limit to 5 on page
    })
    void testListClustersWithPaginationCursors(Integer afterIndex, Integer beforeIndex, Integer pageSize, int expectedResultCount) {
        List<String> allKafkaNames = Stream.concat(STATIC_KAFKAS.stream(),
                IntStream.range(3, 10)
                    .mapToObj(i -> "test-kafka" + i)
                    .map(name -> utils.buildKafkaResource(name, randomBootstrapServers))
                    .map(kafka -> client.resources(Kafka.class).resource(kafka).create())
                    .map(kafka -> kafka.getMetadata().getName()))
            .toList();

        // Wait for the informer cache to be populated with all Kafka CRs
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Objects.equals(kafkaInformer.getStore().list().size(), allKafkaNames.size()));

        var fullResponse = whenRequesting(req -> req.queryParam("sort", "name").get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("meta.page.total", is(9))
            .body("data.size()", is(9))
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
            .body("meta.page.total", is(9)) // total is the count for the full/unpaged result set
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
                .param("sort", "-someObject.\"dot.separated.key\",authType")
                .param("page[before]", Base64.getEncoder()
                        .encodeToString(Json.createObjectBuilder()
                                .add("id", new UUID(0, 0).toString())
                                .add("attributes", Json.createObjectBuilder()
                                        .addNull("authType")
                                        .add("someObject", Json.createObjectBuilder()
                                                .add("dot.separated.key", 1)))
                                .build()
                                .toString()
                                .getBytes()))
                .get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", is(1))
            .body("data.attributes.name[0]", is("test-kafka1"))
            .body("data.attributes.authType[0]", is("custom"));
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

    @Test
    void testDescribeCluster() {
        whenRequesting(req -> req.get("{clusterId}", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", equalTo(clusterId1))
            .body("data.attributes.name", equalTo("test-kafka1"))
            .body("data.attributes.bootstrapServers", equalTo(bootstrapServers.getHost() + ":" + bootstrapServers.getPort()))
            .body("data.attributes.authType", equalTo("custom"));
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
}
