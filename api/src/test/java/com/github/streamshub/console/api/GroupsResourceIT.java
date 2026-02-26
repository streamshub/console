package com.github.streamshub.console.api;

import java.io.StringReader;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.ClassicGroupDescription;
import org.apache.kafka.clients.admin.DescribeClassicGroupsOptions;
import org.apache.kafka.clients.admin.DescribeClassicGroupsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.ApiException;
import org.apache.kafka.common.internals.KafkaFutureImpl;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.context.ThreadContext;
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.github.streamshub.console.api.support.Holder;
import com.github.streamshub.console.api.support.Promises;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.kafka.systemtest.utils.ConsumerUtils;
import com.github.streamshub.console.kafka.systemtest.utils.ConsumerUtils.ConsumerType;
import com.github.streamshub.console.support.Identifiers;
import com.github.streamshub.console.test.AdminClientSpy;
import com.github.streamshub.console.test.TestHelper;
import com.github.streamshub.console.test.TopicHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.kafka.model.kafka.Kafka;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static java.util.regex.Pattern.compile;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doAnswer;

@QuarkusTest
@TestHTTPEndpoint(GroupsResource.class)
@TestProfile(TestPlainProfile.class)
class GroupsResourceIT {

    @Inject
    Config config;

    @Inject
    ThreadContext threadContext;

    @Inject
    KubernetesClient client;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    Holder<SharedIndexInformer<Kafka>> kafkaInformer;

    TestHelper utils;
    TopicHelper topicUtils;
    ConsumerUtils groupUtils;
    String clusterId1;
    String clusterId2;

    @BeforeEach
    void setup() {
        URI bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));

        groupUtils = new ConsumerUtils(config);
        groupUtils.deleteGroups();

        topicUtils = new TopicHelper(bootstrapServers, config);
        topicUtils.deleteAllTopics();

        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);

        client.resources(Kafka.class).inAnyNamespace().delete();

        utils.apply(client, utils.buildKafkaResource("test-kafka1", utils.getClusterId(), bootstrapServers));

        // Wait for the informer cache to be populated with all Kafka CRs
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> Objects.equals(kafkaInformer.get().getStore().list().size(), 1));

        clusterId1 = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
        clusterId2 = consoleConfig.getKafka().getCluster("default/test-kafka2").get().getId();
    }

    @Test
    void testListGroupsDefault() throws Exception {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String client1 = "c1-" + UUID.randomUUID().toString();
        String topic2 = "t2-" + UUID.randomUUID().toString();
        String group2 = "g2-" + UUID.randomUUID().toString();
        String client2 = "c2-" + UUID.randomUUID().toString();

        try (var consumer = groupUtils.consume(group1, topic1, client1, 2, false);
            var shareConsumer = groupUtils.consume(ConsumerType.SHARE, group2, topic2, client2, 2, false)) {
            whenRequesting(req -> req.get("", clusterId1))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data", hasSize(2))
                .body("data[0].attributes.type", is("Classic"))
                .body("data[0].attributes.state", is(notNullValue(String.class)))
                .body("data[0].attributes.simpleConsumerGroup", is(notNullValue(Boolean.class)))
                .body("data[1].attributes.type", is("Share"))
                .body("data[1].attributes.state", is(notNullValue(String.class)));
        }
    }

    @Test
    void testListGroupsEmpty() {
        whenRequesting(req -> req.get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("links.size()", is(4))
            .body("links", allOf(
                    hasEntry(is("first"), nullValue()),
                    hasEntry(is("prev"), nullValue()),
                    hasEntry(is("next"), nullValue()),
                    hasEntry(is("last"), nullValue())))
            .body("meta.page.total", is(0))
            .body("data.size()", is(0));
    }

    @Test
    void testListGroupsWithIdFilter() throws Exception {
        IntStream.range(2, 10)
                .mapToObj("grp-%02d-"::formatted)
                .map(prefix -> prefix + UUID.randomUUID().toString())
                .sorted()
                .map(groupId -> CompletableFuture.runAsync(() -> {
                    String topic = "t-" + UUID.randomUUID().toString();
                    String clientId = "c-" + UUID.randomUUID().toString();
                    groupUtils.consume(groupId, topic, clientId, 1, true);
                }, threadContext.currentContextExecutor()))
                .collect(Promises.awaitingAll())
                .join();

        String topic01 = "t-" + UUID.randomUUID().toString();
        String group01 = "grp-01-FLAG-" + UUID.randomUUID().toString();
        String client01 = "c-" + UUID.randomUUID().toString();

        String topic10 = "t-" + UUID.randomUUID().toString();
        String group10 = "grp-10-FLAG-" + UUID.randomUUID().toString();
        String client10 = "c-" + UUID.randomUUID().toString();

        try (var consumer01 = groupUtils.consume(group01, topic01, client01, 2, false);
             var share10 = groupUtils.consume(ConsumerType.SHARE, group10, topic10, client10, 2, false)) {
            whenRequesting(req -> req
                    .param("filter[id]", "like,*FLAG*")
                    .get("", clusterId1))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.size()", is(2))
                .body("data.attributes.groupId", everyItem(matchesPattern(compile("^grp-\\d{2}-FLAG-.*$"))));
        }
    }

    @Test
    void testListGroupsWithStateFilter() {
        IntStream.range(2, 10)
                .mapToObj("grp-%02d-"::formatted)
                .map(prefix -> prefix + UUID.randomUUID().toString())
                .sorted()
                .map(groupId -> CompletableFuture.runAsync(() -> {
                    String topic = "t-" + UUID.randomUUID().toString();
                    String clientId = "c-" + UUID.randomUUID().toString();
                    groupUtils.consume(groupId, topic, clientId, 1, true);
                }, threadContext.currentContextExecutor()))
                .collect(Promises.awaitingAll())
                .join();

        String topic01 = "t-" + UUID.randomUUID().toString();
        String group01 = "grp-01-FLAG-" + UUID.randomUUID().toString();
        String client01 = "c-" + UUID.randomUUID().toString();

        String topic10 = "t-" + UUID.randomUUID().toString();
        String group10 = "grp-10-FLAG-" + UUID.randomUUID().toString();
        String client10 = "c-" + UUID.randomUUID().toString();

        try (var consumer01 = groupUtils.consume(group01, topic01, client01, 2, false);
             var consumer10 = groupUtils.consume(group10, topic10, client10, 2, false)) {
            whenRequesting(req -> req
                    .param("filter[state]", "eq,STABLE")
                    .get("", clusterId1))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.size()", is(2))
                .body("data.attributes.state", everyItem(is("STABLE")));
        }
    }

    @Test
    void testListConsumerGroupsWithTwoAssignments() {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String client1 = "c1-" + UUID.randomUUID().toString();

        try (var consumer = groupUtils.consume(group1, topic1, client1, 2, false)) {
            whenRequesting(req -> req
                    .param("fields[groups]", "simpleConsumerGroup,"
                            + "state,"
                            + "members,"
                            + "offsets,"
                            + "coordinator,"
                            + "authorizedOperations,"
                            + "partitionAssignor")
                    .get("", clusterId1))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.size()", is(1))
                .body("data[0].attributes.state", is(notNullValue(String.class)))
                .body("data[0].attributes.simpleConsumerGroup", is(notNullValue(Boolean.class)))
                .body("data[0].attributes.coordinator.id", is("0"))
                .body("data[0].attributes.authorizedOperations.size()", is(greaterThanOrEqualTo(1)))
                .body("data[0].attributes.offsets.size()", is(2))
                .body("data[0].attributes.offsets", everyItem(allOf(
                        hasKey("topicId"),
                        hasKey("topicName"),
                        hasKey("partition"),
                        hasKey("offset"),
                        hasKey("lag"),
                        hasKey("metadata"))))
                .body("data[0].attributes.members.size()", is(1))
                .body("data[0].attributes.members.clientId", contains(client1))
                .body("data[0].attributes.members.find { it.clientId == '%s' }.assignments.size()".formatted(client1), is(2));
        }
    }

    @Test
    void testListConsumerGroupsWithPagination() {
        var groupIds = IntStream.range(0, 10)
                .mapToObj("grp-%02d-"::formatted)
                .map(prefix -> prefix + UUID.randomUUID().toString())
                .sorted()
                .toList();

        groupIds.stream()
                .map(groupId -> CompletableFuture.runAsync(() -> {
                    String topic = "t-" + UUID.randomUUID().toString();
                    String clientId = "c-" + UUID.randomUUID().toString();
                    groupUtils.consume(groupId, topic, clientId, 1, true);
                }, threadContext.currentContextExecutor()))
                .collect(Promises.awaitingAll())
                .join();

        Function<String, JsonObject> linkExtract = response -> {
            try (JsonReader reader = Json.createReader(new StringReader(response))) {
                return reader.readObject().getJsonObject("links");
            }
        };

        // Page 1
        String response1 = whenRequesting(req -> req
                .param("sort", "groupId,state,someIgnoredField,-simpleConsumerGroup")
                .param("page[size]", 2)
                .param("fields[groups]", "state,simpleConsumerGroup")
                .get("", clusterId1))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("links.size()", is(4))
            .body("links", allOf(
                    hasEntry(is("first"), notNullValue()),
                    hasEntry(is("prev"), nullValue()),
                    hasEntry(is("next"), notNullValue()),
                    hasEntry(is("last"), notNullValue())))
            .body("meta.page.total", is(10))
            .body("meta.page.pageNumber", is(1))
            .body("data.size()", is(2))
            .body("data[0].id", is(Identifiers.encode(groupIds.get(0))))
            .body("data[1].id", is(Identifiers.encode(groupIds.get(1))))
            .extract()
            .asString();

        JsonObject links1 = linkExtract.apply(response1);
        String links1First = links1.getString("first");
        String links1Last = links1.getString("last");

        // Advance to page 2, using `next` link from page 1
        URI request2 = URI.create(links1.getString("next"));
        String response2 = whenRequesting(req -> req
                .urlEncodingEnabled(false)
                .get(request2))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("links.size()", is(4))
            .body("links", allOf(
                    hasEntry(is("first"), is(links1First)),
                    hasEntry(is("prev"), notNullValue()),
                    hasEntry(is("next"), notNullValue()),
                    hasEntry(is("last"), is(links1Last))))
            .body("meta.page.total", is(10))
            .body("meta.page.pageNumber", is(2))
            .body("data.size()", is(2))
            .body("data[0].id", is(Identifiers.encode(groupIds.get(2))))
            .body("data[1].id", is(Identifiers.encode(groupIds.get(3))))
            .extract()
            .asString();

        // Jump to final page 5 using `last` link from page 2
        JsonObject links2 = linkExtract.apply(response2);
        URI request3 = URI.create(links2.getString("last"));
        String response3 = whenRequesting(req -> req
                .urlEncodingEnabled(false)
                .get(request3))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("links.size()", is(4))
            .body("links", allOf(
                    hasEntry(is("first"), is(links1First)),
                    hasEntry(is("prev"), notNullValue()),
                    hasEntry(is("next"), nullValue()),
                    hasEntry(is("last"), is(links1Last))))
            .body("meta.page.total", is(10))
            .body("meta.page.pageNumber", is(5))
            .body("data.size()", is(2))
            .body("data[0].id", is(Identifiers.encode(groupIds.get(8))))
            .body("data[1].id", is(Identifiers.encode(groupIds.get(9))))
            .extract()
            .asString();

        // Return to page 1 using the `first` link provided by the last page, 5
        JsonObject links3 = linkExtract.apply(response3);
        URI request4 = URI.create(links3.getString("first"));
        String response4 = whenRequesting(req -> req
                .urlEncodingEnabled(false)
                .get(request4))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("links.size()", is(4))
            .body("links", allOf(
                    hasEntry(is("first"), is(links1First)),
                    hasEntry(is("prev"), nullValue()),
                    hasEntry(is("next"), notNullValue()),
                    hasEntry(is("last"), is(links1Last))))
            .body("meta.page.total", is(10))
            .body("meta.page.pageNumber", is(1))
            .body("data.size()", is(2))
            .body("data[0].id", is(Identifiers.encode(groupIds.get(0))))
            .body("data[1].id", is(Identifiers.encode(groupIds.get(1))))
            .extract()
            .asString();

        assertEquals(response1, response4);
    }

    @Test
    void testListConsumerGroupsWithDescribeError() {
        Answer<DescribeClassicGroupsResult> describeGroupsFailed = args -> {
            @SuppressWarnings("unchecked")
            Collection<String> groupIds = args.getArgument(0, Collection.class);
            Map<String, KafkaFuture<ClassicGroupDescription>> futures = new HashMap<>(groupIds.size());

            KafkaFutureImpl<ClassicGroupDescription> failure = new KafkaFutureImpl<>();
            failure.completeExceptionally(new ApiException("EXPECTED TEST EXCEPTION"));

            groupIds.forEach(id -> futures.put(id, failure));

            return new DescribeClassicGroupsResult(futures);
        };

        AdminClientSpy.install(adminClient -> {
            // Mock listOffsets
            doAnswer(describeGroupsFailed)
                .when(adminClient)
                .describeClassicGroups(anyCollection(), any(DescribeClassicGroupsOptions.class));
        });

        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String group1Id = Identifiers.encode(group1);
        String client1 = "c1-" + UUID.randomUUID().toString();

        try (var consumer = groupUtils.consume(group1, topic1, client1, 2, false)) {
            whenRequesting(req -> req
                    .param("fields[groups]", "members")
                    .get("", clusterId1))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.size()", is(1))
                .body("data[0].id", is(group1Id))
                .body("data[0].meta.errors", hasSize(1))
                .body("data[0].meta.errors[0].detail", is("EXPECTED TEST EXCEPTION"));
        }
    }

    @ParameterizedTest
    @CsvSource({
        "CLASSIC",
        "CONSUMER",
        "SHARE",
    })
    void testDescribeGroupDefault(ConsumerType consumerType) throws Exception {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String group1Id = Identifiers.encode(group1);
        String client1 = "c1-" + UUID.randomUUID().toString();

        try (var consumer = groupUtils.consume(consumerType, group1, topic1, client1, 2, false)) {
            whenRequesting(req -> req.get("{groupId}", clusterId1, group1Id))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.attributes.type", is(consumerType.groupType().toString()))
                .body("data.attributes.state", is(Matchers.notNullValue(String.class)))
                .body("data.attributes.simpleConsumerGroup", is(Matchers.notNullValue(Boolean.class)));
        }
    }

    @Test
    void testDescribeConsumerGroupWithNoSuchGroup() {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String client1 = "c1-" + UUID.randomUUID().toString();

        try (var consumer = groupUtils.consume(group1, topic1, client1, 2, false)) {
            whenRequesting(req -> req.get("{groupId}", clusterId1, UUID.randomUUID().toString()))
                .assertThat()
                .statusCode(is(Status.NOT_FOUND.getStatusCode()))
                .body("errors.size()", is(1))
                .body("errors.status", contains("404"))
                .body("errors.code", contains("4041"));
        }
    }

    @Test
    void testDescribeConsumerGroupWithFetchGroupOffsetsError() {
        Answer<ListConsumerGroupOffsetsResult> listConsumerGroupOffsetsFailed = args -> {
            KafkaFutureImpl<Map<TopicPartition, OffsetAndMetadata>> failure = new KafkaFutureImpl<>();
            failure.completeExceptionally(new ApiException("EXPECTED TEST EXCEPTION"));

            ListConsumerGroupOffsetsResult resultMock = Mockito.mock(ListConsumerGroupOffsetsResult.class);

            doAnswer(partitionsToOffsetAndMetadataArgs -> failure)
                .when(resultMock)
                .partitionsToOffsetAndMetadata(Mockito.anyString());

            return resultMock;
        };

        AdminClientSpy.install(adminClient -> {
            // Mock listOffsets
            doAnswer(listConsumerGroupOffsetsFailed)
                .when(adminClient)
                .listConsumerGroupOffsets(anyMap());
        });

        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String group1Id = Identifiers.encode(group1);
        String client1 = "c1-" + UUID.randomUUID().toString();

        try (var consumer = groupUtils.consume(group1, topic1, client1, 2, false)) {
            whenRequesting(req -> req
                    .param("fields[groups]", "offsets")
                    .get("{groupId}", clusterId1, group1Id))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.id", is(group1Id))
                .body("data.meta.errors.size()", is(1))
                .body("data.meta.errors[0].title", is("Unable to list consumer group offsets"))
                .body("data.meta.errors[0].detail", is("EXPECTED TEST EXCEPTION"));
        }
    }


    @Test
    void testDescribeConsumerGroupWithFetchTopicOffsetsError() {
        Answer<ListOffsetsResult> listOffsetsFailed = args -> {
            Map<TopicPartition, OffsetSpec> topicPartitionOffsets = args.getArgument(0);
            KafkaFutureImpl<ListOffsetsResultInfo> failure = new KafkaFutureImpl<>();
            failure.completeExceptionally(new ApiException("EXPECTED TEST EXCEPTION"));

            Map<TopicPartition, KafkaFuture<ListOffsetsResultInfo>> futures = new HashMap<>();
            topicPartitionOffsets.keySet().forEach(key -> futures.put(key, failure));

            return new ListOffsetsResult(futures);
        };

        AdminClientSpy.install(adminClient -> {
            // Mock listOffsets
            doAnswer(listOffsetsFailed)
                .when(adminClient)
                .listOffsets(anyMap());
        });

        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String group1Id = Identifiers.encode(group1);
        String client1 = "c1-" + UUID.randomUUID().toString();

        try (var consumer = groupUtils.consume(group1, topic1, client1, 2, false)) {
            whenRequesting(req -> req
                    .param("fields[groups]", "offsets")
                    .get("{groupId}", clusterId1, group1Id))
                .assertThat()
                .statusCode(is(Status.OK.getStatusCode()))
                .body("data.id", is(group1Id))
                .body("data.meta.errors.size()", is(2)) // 2 partitions, both failed
                .body("data.meta.errors.title", everyItem(startsWith("Unable to list offsets for topic/partition")))
                .body("data.meta.errors.detail", everyItem(is("EXPECTED TEST EXCEPTION")));
        }
    }

    @Test
    void testDeleteConsumerGroupWithMembers() {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String group1Id = Identifiers.encode(group1);
        String client1 = "c1-" + UUID.randomUUID().toString();

        try (var consumer = groupUtils.consume(group1, topic1, client1, 2, false)) {
            whenRequesting(req -> req.delete("{groupId}", clusterId1, group1Id))
                .assertThat()
                .statusCode(is(Status.CONFLICT.getStatusCode()))
                .body("errors.size()", is(1))
                .body("errors.status", contains("409"))
                .body("errors.code", contains("4091"));

            assertEquals(GroupState.STABLE, groupUtils.consumerGroupState(group1));
        }
    }

    @Test
    void testDeleteConsumerGroupWithNoSuchGroup() {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String client1 = "c1-" + UUID.randomUUID().toString();

        try (var consumer = groupUtils.consume(group1, topic1, client1, 2, false)) {
            whenRequesting(req -> req.delete("{groupId}", clusterId1, UUID.randomUUID().toString()))
                .assertThat()
                .statusCode(is(Status.NOT_FOUND.getStatusCode()))
                .body("errors.size()", is(1))
                .body("errors.status", contains("404"))
                .body("errors.code", contains("4041"));

            assertEquals(GroupState.STABLE, groupUtils.consumerGroupState(group1));
        }
    }

    @Test
    void testDeleteConsumerGroupSucceeds() {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String group1Id = Identifiers.encode(group1);
        String client1 = "c1-" + UUID.randomUUID().toString();

        try (var consumer = groupUtils.consume(group1, topic1, client1, 2, false)) {
            await().atMost(10, TimeUnit.SECONDS)
                .until(() -> GroupState.STABLE == groupUtils.consumerGroupState(group1));
        }

        whenRequesting(req -> req.delete("{groupId}", clusterId1, group1Id))
            .assertThat()
            .statusCode(is(Status.NO_CONTENT.getStatusCode()));

        whenRequesting(req -> req.get("{groupId}", clusterId1, group1Id))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    void testPatchConsumerGroupWithNoSuchGroup() {
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String group1 = "g1-" + UUID.randomUUID().toString();
        String client1 = "c1-" + UUID.randomUUID().toString();
        String noSuchGroupId = UUID.randomUUID().toString();

        try (var consumer = groupUtils.consume(group1, topic1, client1, 2, false)) {
            whenRequesting(req -> req
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .body(Json.createObjectBuilder()
                            .add("data", Json.createObjectBuilder()
                                    .add("id", noSuchGroupId)
                                    .add("type", "groups")
                                    .add("attributes", Json.createObjectBuilder()))
                            .build()
                            .toString())
                    .patch("{groupId}", clusterId1, noSuchGroupId))
                .assertThat()
                .statusCode(is(Status.NOT_FOUND.getStatusCode()))
                .body("errors.size()", is(1))
                .body("errors.status", contains("404"))
                .body("errors.code", contains("4041"));
        }
    }

    @ParameterizedTest
    @CsvFileSource(
        delimiter = '|',
        lineSeparator = "@\n",
        resources = { "/patchConsumerGroup-invalid-requests.txt" })
    void testPatchConsumerGroupWithInvalidRequest(String label, String requestBody, Status responseStatus, String expectedResponse)
            throws JSONException {

        String topic1 = "t1-" + UUID.randomUUID().toString();
        String topic1Id = topicUtils.createTopics(List.of(topic1), 2).get(topic1);
        String group1 = "g1-" + UUID.randomUUID().toString();
        String group1Id = Identifiers.encode(group1);
        String client1 = "c1-" + UUID.randomUUID().toString();

        String preparedRequest = requestBody
                .replace("$groupId", group1Id)
                .replace("$topicId", topic1Id);

        var consumer = groupUtils.request(ConsumerType.CLASSIC)
            .groupId(group1)
            .topic(topic1)
            .createTopic(false)
            .clientId(client1)
            .messagesPerTopic(10)
            .consumeMessages(10)
            .autoClose(false)
            .consume();

        try {
            whenRequesting(req -> req
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .body(preparedRequest)
                    .patch("{groupId}", clusterId1, group1Id))
                .assertThat()
                .statusCode(is(responseStatus.getStatusCode()))
                .body(new TypeSafeMatcher<String>() {
                    @Override
                    public boolean matchesSafely(String response) {
                        try {
                            JSONAssert.assertEquals(expectedResponse, response, JSONCompareMode.LENIENT);
                        } catch (JSONException e) {
                            return false;
                        }
                        return true;
                    }

                    @Override
                    public void describeTo(Description description) {
                        description.appendValue(expectedResponse);
                    }
                });
        } finally {
            consumer.close();
        }
    }

    @ParameterizedTest
    @CsvSource({
        "false, 5, 'earliest'                 , 0",
        "false, 5, '2023-01-01T00:00:00.000Z' , 0",
        "true , 0, 'latest'                   , 5", // latest resets to after the last offset
        "true , 0, 'maxTimestamp'             , 4", // maxTimestamp resets to before the offset of latest timestamp
    })
    void testPatchConsumerGroupToOffsetSpecWithMultiplePartitions(
            boolean resetEarliestBefore,
            long beforeOffset,
            String offsetSpec,
            long afterOffset) {
        final int partitionCount = 2;
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String topic1Id = topicUtils.createTopics(List.of(topic1), partitionCount).get(topic1);
        String group1 = "g1-" + UUID.randomUUID().toString();
        String group1Id = Identifiers.encode(group1);
        String client1 = "c1-" + UUID.randomUUID().toString();

        groupUtils.request(ConsumerType.CLASSIC)
                .groupId(group1)
                .topic(topic1, partitionCount)
                .createTopic(false)
                .clientId(client1)
                .messagesPerTopic(10)
                .consumeMessages(10)
                .autoClose(true)
                .consume();

        if (resetEarliestBefore) {
            groupUtils.alterConsumerGroupOffsets(group1, Map.ofEntries(
                    Map.entry(new TopicPartition(topic1, 0), new OffsetAndMetadata(0)),
                    Map.entry(new TopicPartition(topic1, 1), new OffsetAndMetadata(0))));
        }

        var offsetBefore = groupUtils.consumerGroupOffsets(group1);

        assertEquals(partitionCount, offsetBefore.size());
        offsetBefore.forEach((partition, offset) -> {
            assertEquals(beforeOffset, offset.offset());
        });

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("data", Json.createObjectBuilder()
                                .add("id", group1Id)
                                .add("type", "groups")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("offsets", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("topicId", topic1Id)
                                                        .add("offset", offsetSpec)))))
                        .build()
                        .toString())
                .patch("{groupId}", clusterId1, group1Id))
            .assertThat()
            .statusCode(is(Status.NO_CONTENT.getStatusCode()));

        var offsetAfter = groupUtils.consumerGroupOffsets(group1);

        assertEquals(partitionCount, offsetAfter.size());
        offsetAfter.forEach((partition, offset) -> {
            assertEquals(afterOffset, offset.offset());
        });
    }

    @ParameterizedTest
    @CsvSource({
        "false, 5, 'earliest'                 , 0",
        "false, 5, '2023-01-01T00:00:00.000Z' , 0",
        "true , 0, 'latest'                   , 5", // latest resets to after the last offset
        "true , 0, 'maxTimestamp'             , 4", // maxTimestamp resets to before the offset of latest timestamp
    })
    void testPatchConsumerGroupToOffsetSpecWithMultiplePartitionsDryRun(
            boolean resetEarliestBefore,
            long beforeOffset,
            String offsetSpec,
            int afterOffset) {
        final int partitionCount = 2;
        String topic1 = "t1-" + UUID.randomUUID().toString();
        String topic1Id = topicUtils.createTopics(List.of(topic1), partitionCount).get(topic1);
        String group1 = "g1-" + UUID.randomUUID().toString();
        String group1Id = Identifiers.encode(group1);
        String client1 = "c1-" + UUID.randomUUID().toString();

        groupUtils.request(ConsumerType.CLASSIC)
                .groupId(group1)
                .topic(topic1, partitionCount)
                .createTopic(false)
                .clientId(client1)
                .messagesPerTopic(10)
                .consumeMessages(10)
                .autoClose(true)
                .consume();

        if (resetEarliestBefore) {
            groupUtils.alterConsumerGroupOffsets(group1, Map.ofEntries(
                    Map.entry(new TopicPartition(topic1, 0), new OffsetAndMetadata(0)),
                    Map.entry(new TopicPartition(topic1, 1), new OffsetAndMetadata(0))));
        }

        var offsetBefore = groupUtils.consumerGroupOffsets(group1);

        assertEquals(partitionCount, offsetBefore.size());
        offsetBefore.forEach((partition, offset) -> {
            assertEquals(beforeOffset, offset.offset());
        });

        whenRequesting(req -> req
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(Json.createObjectBuilder()
                        .add("meta", Json.createObjectBuilder()
                                .add("dryRun", true))
                        .add("data", Json.createObjectBuilder()
                                .add("id", group1Id)
                                .add("type", "groups")
                                .add("attributes", Json.createObjectBuilder()
                                        .add("offsets", Json.createArrayBuilder()
                                                .add(Json.createObjectBuilder()
                                                        .add("topicId", topic1Id)
                                                        .add("offset", offsetSpec)))))
                        .build()
                        .toString())
                .patch("{groupId}", clusterId1, group1Id))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.attributes.state", is(GroupState.EMPTY.name()))
            .body("data.attributes.offsets.topicId", everyItem(is(topic1Id)))
            .body("data.attributes.offsets.topicName", everyItem(is(topic1)))
            .body("data.attributes.offsets.partition", containsInAnyOrder(0, 1))
            .body("data.attributes.offsets.offset", everyItem(is(afterOffset)));

        var offsetAfter = groupUtils.consumerGroupOffsets(group1);

        assertEquals(partitionCount, offsetAfter.size());
        offsetAfter.forEach((partition, offset) -> {
            // unchanged
            assertEquals(beforeOffset, offset.offset());
        });
    }
}
