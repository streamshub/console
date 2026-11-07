package com.github.streamshub.console.api;

import java.io.StringReader;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.Uuid;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.test.RecordHelper;
import com.github.streamshub.console.test.TestHelper;
import com.github.streamshub.console.test.TopicHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.kafka.model.kafka.Kafka;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Integration tests of the Server-Sent Events variant of the records endpoint,
 * selected by requesting the {@code text/event-stream} media type, including the
 * server-side search using the {@code like} operator on keys, values and headers.
 */
@QuarkusTest
@TestHTTPEndpoint(RecordsResource.class)
@TestProfile(TestPlainProfile.class)
class RecordsSseResourceIT {

    @Inject
    Config config;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    Map<String, KafkaContext> kafkaContexts;

    @Inject
    KubernetesClient client;

    TestHelper utils;
    TopicHelper topicUtils;
    RecordHelper recordUtils;
    String clusterId1;

    @BeforeEach
    void setup() {
        URI bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));

        topicUtils = new TopicHelper(bootstrapServers, config);
        topicUtils.deleteAllTopics();

        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        recordUtils = new RecordHelper(bootstrapServers, config);

        client.resources(Kafka.class).inAnyNamespace().delete();

        utils.apply(client, utils.buildKafkaResource("test-kafka1", utils.getClusterId(), bootstrapServers));

        // Wait for the context map to be populated with the Kafka configuration
        await().atMost(10, TimeUnit.SECONDS).until(() -> kafkaContexts.values()
                .stream()
                .map(KafkaContext::clusterConfig)
                .map(KafkaClusterConfig::getName)
                .toList()
                .contains("test-kafka1"));

        clusterId1 = consoleConfig.getKafka().getCluster("default/test-kafka1").get().getId();
    }

    /**
     * A single frame of the event stream. Frames without an {@code event:} line
     * are the unnamed data events that carry batches of records.
     */
    record Frame(String event, String data) {
    }

    /**
     * The parsed event stream of a response.
     */
    record Stream(List<Frame> frames) {
        List<JsonObject> records() {
            return frames.stream()
                    .filter(f -> f.event() == null)
                    .flatMap(f -> Json.createReader(new StringReader(f.data())).readArray().stream())
                    .map(JsonValue::asJsonObject)
                    .toList();
        }

        List<JsonObject> events(String name) {
            return frames.stream()
                    .filter(f -> name.equals(f.event()))
                    .map(f -> Json.createReader(new StringReader(f.data())).readObject())
                    .toList();
        }

        JsonObject done() {
            var done = events("done");
            assertThat("Exactly one done event expected", done, hasSize(1));
            return done.get(0);
        }

        /** Values of the records' {@code attributes.value}, in the order received. */
        List<String> values() {
            return records().stream()
                    .map(rec -> rec.getJsonObject("attributes").getString("value"))
                    .toList();
        }
    }

    static Stream parse(String body) {
        List<Frame> frames = new ArrayList<>();

        for (String raw : body.replace("\r\n", "\n").split("\n\n")) {
            String event = null;
            List<String> data = new ArrayList<>();

            for (String line : raw.split("\n")) {
                if (line.startsWith("event:")) {
                    event = line.substring("event:".length()).trim();
                } else if (line.startsWith("data:")) {
                    data.add(line.substring("data:".length()).stripLeading());
                }
            }

            if (!data.isEmpty()) {
                frames.add(new Frame(event, String.join("\n", data)));
            }
        }

        return new Stream(frames);
    }

    private Stream stream(String topicId, Map<String, Object> params) {
        String body = whenRequesting(req -> req
                .accept(MediaType.SERVER_SENT_EVENTS)
                .queryParams(params)
                .get("", clusterId1, topicId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .contentType(containsString(MediaType.SERVER_SENT_EVENTS))
            .extract()
            .asString();

        return parse(body);
    }

    private Stream stream(String topicId) {
        return stream(topicId, Map.of());
    }

    private void awaitTopicSize(String topicName, long size) {
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) >= size);
    }

    /**
     * Produce records with the given values (keyed {@code key-<i>}) to a new
     * single-partition topic and return its ID.
     */
    private String topicWithValues(String topicName, String... values) {
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        for (int i = 0; i < values.length; i++) {
            recordUtils.produceRecord(topicName, null, null, "key-" + i, values[i]);
        }

        awaitTopicSize(topicName, values.length);
        return topicIds.get(topicName);
    }

    @Test
    void testStreamRecordsFromInvalidTopic() {
        final String topicId = Uuid.randomUuid().toString();

        whenRequesting(req -> req
                .accept(MediaType.SERVER_SENT_EVENTS)
                .get("", clusterId1, topicId))
            .assertThat()
            .statusCode(is(Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    void testStreamRecordsWithoutSearchEndsWithDone() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        for (int i = 0; i < 5; i++) {
            recordUtils.produceRecord(topicName, null, null, "key-" + i, "value-" + i);
        }

        awaitTopicSize(topicName, 5);

        Stream result = stream(topicIds.get(topicName));

        assertThat(result.values(), containsInAnyOrder("value-0", "value-1", "value-2", "value-3", "value-4"));
        result.records().forEach(rec -> assertThat(rec.getString("type"), is(equalTo("records"))));

        // Data events come first, the terminal event is last and there is only one
        Frame last = result.frames().get(result.frames().size() - 1);
        assertThat(last.event(), is(equalTo("done")));
        assertThat(result.done().getString("reason"), is(equalTo("complete")));
    }

    @Test
    void testStreamRecordsFromEmptyTopic() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        Stream result = stream(topicIds.get(topicName));

        assertThat(result.records(), is(empty()));
        assertThat(result.frames(), hasSize(1));
        assertThat(result.done().getString("reason"), is(equalTo("complete")));
    }

    @Test
    void testStreamRecordsReportsScanProgress() {
        final String topicName = UUID.randomUUID().toString();
        String topicId = topicWithValues(topicName, "a", "b", "c", "d");

        // No match, so all of the records are scanned
        Stream result = stream(topicId, Map.of("filter[value]", "like,no-such-value"));

        List<JsonObject> heartbeats = result.events("heartbeat");
        assertThat(heartbeats, is(not(empty())));

        JsonObject lastHeartbeat = heartbeats.get(heartbeats.size() - 1);
        assertThat(lastHeartbeat.getInt("scanned"), is(4));
        assertThat(lastHeartbeat.getInt("progress"), is(100));

        // Progress and scanned count never go backwards
        int previousScanned = 0;
        int previousProgress = 0;

        for (JsonObject heartbeat : heartbeats) {
            assertThat(heartbeat.getInt("scanned"), is(greaterThanOrEqualTo(previousScanned)));
            assertThat(heartbeat.getInt("progress"), is(greaterThanOrEqualTo(previousProgress)));
            previousScanned = heartbeat.getInt("scanned");
            previousProgress = heartbeat.getInt("progress");
        }

        assertThat(result.records(), is(empty()));
        assertThat(result.done().getString("reason"), is(equalTo("complete")));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
        // Search is case-sensitive
        "*alpha*;     alpha-one|alpha-three",
        "alpha*;      alpha-one|alpha-three",
        "*-two;       beta-two",
        "?eta-two;    beta-two",
        "*ALPHA*;     ALPHA-upper",
        "*;           alpha-one|beta-two|alpha-three|gamma-four|ALPHA-upper",
        "*no-match*;  ''",
        // Without wildcards, the entire value must match
        "alpha;       ''",
        "alpha-one;   alpha-one",
    })
    void testStreamRecordsSearchByValue(String pattern, String expected) {
        final String topicName = UUID.randomUUID().toString();
        String topicId = topicWithValues(topicName,
                "alpha-one", "beta-two", "alpha-three", "gamma-four", "ALPHA-upper");

        Stream result = stream(topicId, Map.of("filter[value]", "like," + pattern));

        assertThat(result.values(), containsInAnyOrder(expectedValues(expected)));
        assertThat(result.done().getString("reason"), is(equalTo("complete")));
    }

    @Test
    void testStreamRecordsSearchByValueBeyondMaxValueLength() {
        final String topicName = UUID.randomUUID().toString();
        String matching = "x".repeat(50) + "needle";
        String topicId = topicWithValues(topicName, matching, "x".repeat(50) + "haystack");

        // The search term is past the truncation point of the values returned
        Stream result = stream(topicId, Map.of(
                "filter[value]", "like,*needle*",
                "maxValueLength", 10));

        List<JsonObject> records = result.records();
        assertThat(records, hasSize(1));

        JsonObject rec = records.get(0);
        assertThat(rec.getJsonObject("attributes").getString("value"), is(equalTo("x".repeat(10))));
        assertThat(rec.getJsonObject("meta").getJsonObject("content").getJsonObject("value").getBoolean("truncated"), is(true));
    }

    @Test
    void testStreamRecordsSearchByKey() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        recordUtils.produceRecord(topicName, null, null, "order-1", "value-0");
        recordUtils.produceRecord(topicName, null, null, "customer-1", "value-1");
        recordUtils.produceRecord(topicName, null, null, "order-2", "value-2");
        awaitTopicSize(topicName, 3);

        Stream result = stream(topicIds.get(topicName), Map.of("filter[key]", "like,order-*"));

        assertThat(result.values(), containsInAnyOrder("value-0", "value-2"));
    }

    @Test
    void testStreamRecordsSearchByHeaders() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        recordUtils.produceRecord(topicName, null, Map.of("h1", "tenant-a"), "key-0", "value-0");
        recordUtils.produceRecord(topicName, null, Map.of("h1", "tenant-b", "h2", "tenant-a"), "key-1", "value-1");
        recordUtils.produceRecord(topicName, null, Map.of("h1", "tenant-c"), "key-2", "value-2");
        recordUtils.produceRecord(topicName, null, null, "key-3", "value-3");
        awaitTopicSize(topicName, 4);

        // A match in any one of the headers selects the record
        Stream result = stream(topicIds.get(topicName), Map.of("filter[headers]", "like,*tenant-a*"));

        assertThat(result.values(), containsInAnyOrder("value-0", "value-1"));
    }

    @Test
    void testStreamRecordsSearchMatchesAnyOfKeyValueHeaders() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        recordUtils.produceRecord(topicName, null, null, "needle-key", "value-0");
        recordUtils.produceRecord(topicName, null, null, "key-1", "needle-value");
        recordUtils.produceRecord(topicName, null, Map.of("h1", "needle-header"), "key-2", "value-2");
        recordUtils.produceRecord(topicName, null, null, "key-3", "value-3");
        awaitTopicSize(topicName, 4);

        Stream result = stream(topicIds.get(topicName), Map.of(
                "filter[key]", "like,*needle*",
                "filter[value]", "like,*needle*",
                "filter[headers]", "like,*needle*"));

        assertThat(result.values(), containsInAnyOrder("value-0", "needle-value", "value-2"));
    }

    @Test
    void testStreamRecordsSearchNeverMatchesBinaryValue() throws NoSuchAlgorithmException {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        final byte[] data = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(data);
        data[15] = (byte) 0xFF; // ensure at least one byte invalid UTF-8

        recordUtils.produceRecord(topicName, null, null, (byte[]) null, data);
        recordUtils.produceRecord(topicName, null, null, "key-1", "text-value");
        awaitTopicSize(topicName, 2);

        Stream result = stream(topicIds.get(topicName), Map.of("filter[value]", "like,*"));

        assertThat(result.values(), contains("text-value"));
    }

    @Test
    void testStreamRecordsSearchNeverMatchesBinaryKeyOrHeader() throws NoSuchAlgorithmException {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        final byte[] binary = new byte[8];
        SecureRandom.getInstanceStrong().nextBytes(binary);
        binary[7] = (byte) 0xFF; // ensure at least one byte invalid UTF-8

        // Binary key, and binary header value
        recordUtils.produceRecord(topicName, null, null, binary, "text-0".getBytes());
        recordUtils.produceRecord(topicName, null, Map.<String, Object>of("bin", binary), (byte[]) null, "text-1".getBytes());
        awaitTopicSize(topicName, 2);

        Stream byKey = stream(topicIds.get(topicName), Map.of("filter[key]", "like,*"));
        assertThat(byKey.records(), is(empty()));

        Stream byHeaders = stream(topicIds.get(topicName), Map.of("filter[headers]", "like,*"));
        assertThat(byHeaders.records(), is(empty()));
    }

    @Test
    void testStreamRecordsSearchStopsWhenLimitMet() {
        final String topicName = UUID.randomUUID().toString();
        String[] values = new String[50];

        for (int i = 0; i < values.length; i++) {
            values[i] = "match-" + i;
        }

        String topicId = topicWithValues(topicName, values);

        // Read from the beginning, otherwise only the latest page[size] records are scanned
        Stream result = stream(topicId, Map.of(
                "filter[value]", "like,match-*",
                "filter[offset]", "gte,0",
                "page[size]", 5));

        // The whole batch in which the limit was reached may be sent, so only a minimum is asserted
        assertThat(result.records().size(), is(greaterThanOrEqualTo(5)));
        assertThat(result.done().getString("reason"), is(equalTo("limit")));
    }

    @Test
    void testStreamRecordsLimitIsAppliedToMatchesNotScannedRecords() {
        final String topicName = UUID.randomUUID().toString();
        String[] values = new String[30];

        // The matching records are at the start of the topic, followed by many that do not match
        for (int i = 0; i < values.length; i++) {
            values[i] = i < 3 ? "match-" + i : "other-" + i;
        }

        String topicId = topicWithValues(topicName, values);

        Stream result = stream(topicId, Map.of(
                "filter[value]", "like,match-*",
                "filter[offset]", "gte,0",
                "page[size]", 5));

        assertThat(result.values(), containsInAnyOrder("match-0", "match-1", "match-2"));
        assertThat(result.done().getString("reason"), is(equalTo("complete")));
    }

    @Test
    void testStreamRecordsSearchCombinedWithPartitionFilter() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);
        recordUtils.produceRecord(topicName, 0, null, null, "key-0", "match-p0");
        recordUtils.produceRecord(topicName, 1, null, null, "key-1", "match-p1");
        awaitTopicSize(topicName, 2);

        Map<String, Object> params = new HashMap<>();
        params.put("filter[value]", "like,match-*");
        params.put("filter[partition]", 1);

        Stream result = stream(topicIds.get(topicName), params);

        assertThat(result.values(), contains("match-p1"));
    }

    @Test
    void testStreamRecordsRejectsUnsupportedSearchOperator() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        whenRequesting(req -> req
                .accept(MediaType.SERVER_SENT_EVENTS)
                .queryParam("filter[value]", "eq,something")
                .get("", clusterId1, topicIds.get(topicName)))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()));
    }

    private static String[] expectedValues(String expected) {
        if (expected == null || expected.isBlank() || "''".equals(expected.trim())) {
            return new String[0];
        }

        return Set.of(expected.trim().split("\\|")).toArray(String[]::new);
    }
}
