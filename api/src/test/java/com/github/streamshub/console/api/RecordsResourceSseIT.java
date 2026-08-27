package com.github.streamshub.console.api;

import java.io.StringReader;
import java.net.URI;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.sse.SseEventSource;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.Uuid;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.streamshub.console.api.service.RecordService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.serdes.MultiformatDeserializer;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.test.LogCapture;
import com.github.streamshub.console.test.RecordHelper;
import com.github.streamshub.console.test.TestHelper;
import com.github.streamshub.console.test.TopicHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
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
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;

/**
 * Integration tests of the Server-Sent Events variant of the records endpoint,
 * selected by requesting the {@code text/event-stream} media type, including the
 * server-side search using the {@code like} operator on keys, values and headers.
 */
@QuarkusTest
@TestHTTPEndpoint(RecordsResource.class)
@TestProfile(TestPlainProfile.class)
class RecordsResourceSseIT {

    /** Configured in {@link TestPlainProfile} without an associated schema registry. */
    static final String CLUSTER_ID_Y = "test-kafkaY";

    static LogCapture logCapture = LogCapture.with(logRecord -> logRecord
            .getLoggerName()
            .equals(MultiformatDeserializer.class.getName()),
            Level.FINEST);

    @Inject
    Config config;

    @Inject
    ConsoleConfig consoleConfig;

    @TestHTTPResource
    String resourceUri;

    @Inject
    Map<String, KafkaContext> kafkaContexts;

    @Inject
    KubernetesClient client;

    @Inject
    RecordService recordService;

    TestHelper utils;
    TopicHelper topicUtils;
    RecordHelper recordUtils;
    String clusterId1;

    @BeforeAll
    static void initialize() {
        logCapture.register();
    }

    @AfterAll
    static void cleanup() {
        logCapture.deregister();
    }

    @AfterEach
    void cleanupAfterEach() {
        System.clearProperty(RecordService.STREAM_TIMEOUT_CONFIG);
        System.clearProperty(RecordService.MAX_CONCURRENT_STREAMS_CONFIG);
        if (recordUtils != null) {
            recordUtils.close();
        }
    }

    @BeforeEach
    void setup() {
        logCapture.records().clear();

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
    record FrameSet(List<Frame> frames) {
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

    static FrameSet parse(String body) {
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

        return new FrameSet(frames);
    }

    private FrameSet stream(String topicId, Map<String, Object> params) {
        return stream(clusterId1, topicId, params);
    }

    private WebTarget recordsTarget(Client client, String clusterId, String topicId, Map<String, Object> params) {
        WebTarget target = client.target(resourceUri)
                .resolveTemplate("clusterId", clusterId)
                .resolveTemplate("topicId", topicId);

        for (var entry : params.entrySet()) {
            target = target.queryParam(entry.getKey(), entry.getValue());
        }

        return target;
    }

    private FrameSet stream(String clusterId, String topicId, Map<String, Object> params) {
        try (Client webClient = ClientBuilder.newClient()) {
            WebTarget target = recordsTarget(webClient, clusterId, topicId, params);
            List<Frame> frames = new CopyOnWriteArrayList<>();
            CompletableFuture<Void> completed = new CompletableFuture<>();

            try (SseEventSource source = SseEventSource.target(target).build()) {
                source.register(
                    event -> {
                        String eventName = event.getName();
                        String data = event.readData(String.class);
                        frames.add(new Frame(eventName, data));
                        if ("done".equals(eventName)) {
                            completed.complete(null);
                        }
                    },
                    completed::completeExceptionally,
                    () -> completed.complete(null)
                );
                source.open();
                completed.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException("Error reading SSE stream", e);
            }

            return new FrameSet(frames);
        }
    }

    private FrameSet stream(String topicId) {
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

        var futures = new CompletableFuture<?>[values.length];
        for (int i = 0; i < values.length; i++) {
            futures[i] = recordUtils.produceRecordAsync(topicName, null, null, "key-" + i, values[i]);
        }
        CompletableFuture.allOf(futures).join();

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

        FrameSet result = stream(topicIds.get(topicName));

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

        FrameSet result = stream(topicIds.get(topicName));

        assertThat(result.records(), is(empty()));
        assertThat(result.frames(), hasSize(1));
        assertThat(result.done().getString("reason"), is(equalTo("complete")));
    }

    @Test
    void testStreamRecordsReportsScanProgress() {
        final String topicName = UUID.randomUUID().toString();
        String topicId = topicWithValues(topicName, "a", "b", "c", "d");

        // No match, so all of the records are scanned
        FrameSet result = stream(topicId, Map.of("filter[value]", "like,no-such-value"));

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

    @Test
    void testStreamRecordsReportsElapsedAndRemainingTime() {
        final String topicName = UUID.randomUUID().toString();
        String topicId = topicWithValues(topicName, "a", "b", "c", "d");

        final long streamTimeoutMillis = 30_000;
        System.setProperty(RecordService.STREAM_TIMEOUT_CONFIG, Duration.ofMillis(streamTimeoutMillis).toString());

        // No match, so all of the records are scanned and heartbeats are sent
        FrameSet result = stream(topicId, Map.of("filter[value]", "like,no-such-value"));

        List<JsonObject> heartbeats = result.events("heartbeat");
        assertThat(heartbeats, is(not(empty())));

        long previousElapsed = 0;
        long previousRemaining = streamTimeoutMillis;

        for (JsonObject heartbeat : heartbeats) {
            long elapsed = heartbeat.getJsonNumber("elapsed").longValue();
            long remaining = heartbeat.getJsonNumber("remaining").longValue();

            // Time only moves forward, and never past the configured timeout
            assertThat(elapsed, is(greaterThanOrEqualTo(previousElapsed)));
            assertThat(remaining, is(lessThanOrEqualTo(previousRemaining)));
            assertThat(remaining, is(greaterThanOrEqualTo(0L)));
            assertThat(elapsed + remaining, is(lessThanOrEqualTo(streamTimeoutMillis)));

            previousElapsed = elapsed;
            previousRemaining = remaining;
        }

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

        FrameSet result = stream(topicId, Map.of("filter[value]", "like," + pattern));

        assertThat(result.values(), containsInAnyOrder(expectedValues(expected)));
        assertThat(result.done().getString("reason"), is(equalTo("complete")));
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
        // A comma escaped with a backslash is part of the pattern, as the UI sends it, and ilike ignores case
        "ilike,*a\\,b*;       a,b|xa,by|A,B",
        "ilike,*A\\,B*;       a,b|xa,by|A,B",
        // Without a leading wildcard the pattern is anchored, and ? matches one character
        "ilike,a\\,b*;        a,b|A,B",
        "ilike,?\\,b*;        a,b|A,B",
        // A backslash escaped with a backslash is a literal backslash
        "ilike,*c\\\\d*;      c\\d",
    })
    void testStreamRecordsSearchByValueWithEscapedCharacters(String filter, String expected) {
        final String topicName = UUID.randomUUID().toString();
        String topicId = topicWithValues(topicName, "a,b", "xa,by", "A,B", "a b", "c\\d", "cd");

        FrameSet result = stream(topicId, Map.of("filter[value]", filter));

        assertThat(result.values(), containsInAnyOrder(expectedValues(expected)));
        assertThat(result.done().getString("reason"), is(equalTo("complete")));
    }

    @Test
    void testStreamRecordsSearchByValueWithUnescapedCommaIsRejected() {
        final String topicName = UUID.randomUUID().toString();
        String topicId = topicWithValues(topicName, "a,b");

        // The comma separates operands, so this is 2 operands rather than a pattern containing a comma
        whenRequesting(req -> req
                .accept(MediaType.SERVER_SENT_EVENTS)
                .queryParam("filter[value]", "ilike,*a,b*")
                .get("", clusterId1, topicId))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    void testStreamRecordsSearchByValueBeyondMaxValueLength() {
        final String topicName = UUID.randomUUID().toString();
        String matching = "x".repeat(50) + "needle";
        String topicId = topicWithValues(topicName, matching, "x".repeat(50) + "haystack");

        // The search term is past the truncation point of the values returned
        FrameSet result = stream(topicId, Map.of(
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

        FrameSet result = stream(topicIds.get(topicName), Map.of("filter[key]", "like,order-*"));

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
        FrameSet result = stream(topicIds.get(topicName), Map.of("filter[headers]", "like,*tenant-a*"));

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

        FrameSet result = stream(topicIds.get(topicName), Map.of(
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

        FrameSet result = stream(topicIds.get(topicName), Map.of("filter[value]", "like,*"));

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

        FrameSet byKey = stream(topicIds.get(topicName), Map.of("filter[key]", "like,*"));
        assertThat(byKey.records(), is(empty()));

        FrameSet byHeaders = stream(topicIds.get(topicName), Map.of("filter[headers]", "like,*"));
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
        FrameSet result = stream(topicId, Map.of(
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

        FrameSet result = stream(topicId, Map.of(
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

        FrameSet result = stream(topicIds.get(topicName), params);

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

    @Test
    void testStreamRecordsEndsWithTimeoutWhenStreamTimeoutElapses() {
        final String topicName = UUID.randomUUID().toString();
        String topicId = topicWithValues(topicName, "a", "b", "c");

        // The setting is read for each request, a zero timeout elapses before any record is read
        System.setProperty(RecordService.STREAM_TIMEOUT_CONFIG, "PT0S");

        FrameSet result = stream(topicId);

        assertThat(result.values(), is(empty()));
        assertThat(result.done().getString("reason"), is(equalTo("timeout")));
    }

    @Test
    void testStreamRecordsRejectedWhenConcurrentStreamLimitReached() {
        final String topicName = UUID.randomUUID().toString();
        String topicId = topicWithValues(topicName, "a", "b", "c");

        System.setProperty(RecordService.MAX_CONCURRENT_STREAMS_CONFIG, "0");

        whenRequesting(req -> req
                .accept(MediaType.SERVER_SENT_EVENTS)
                .get("", clusterId1, topicId))
            .assertThat()
            .statusCode(is(Status.TOO_MANY_REQUESTS.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors[0].status", is("429"))
            .body("errors[0].code", is("4291"));

        // The rejected request must not hold on to a stream slot
        System.setProperty(RecordService.MAX_CONCURRENT_STREAMS_CONFIG, "1");

        FrameSet result = stream(topicId);
        assertThat(result.values(), containsInAnyOrder("a", "b", "c"));
        assertThat(result.done().getString("reason"), is(equalTo("complete")));

        // Account for small delay between sending of "done" event, closing of the
        // Consumer, and decrement of the activeStreams counter. In practice this will
        // take milliseconds, but races are possible when activeStreams is near the limit.
        // Because the limit is only 1 for this test, we need to make sure server-side
        // processing completes before proceeding.
        await().atMost(2, TimeUnit.SECONDS)
            .until(() -> recordService.activeStreams() == 0);

        // Nor must a completed the stream above
        FrameSet again = stream(topicId);
        assertThat(again.values(), containsInAnyOrder("a", "b", "c"));
    }

    @Test
    void testStreamRecordsClientCancellationReleasesStreamSlot() throws InterruptedException {
        final String topicName = UUID.randomUUID().toString();
        String topicId = topicWithValues(topicName, "a", "b", "c", "d", "e");

        // Allow at most 1 concurrent stream
        System.setProperty(RecordService.MAX_CONCURRENT_STREAMS_CONFIG, "1");

        CountDownLatch firstEventReceived = new CountDownLatch(1);

        try (Client webClient = ClientBuilder.newClient()) {
            WebTarget target = recordsTarget(webClient, clusterId1, topicId, Map.of("filter[value]", "like,no-such-value"));
            try (SseEventSource source = SseEventSource.target(target).build()) {
                source.register(event -> firstEventReceived.countDown());
                source.open();

                // Wait until the client starts receiving events from the server
                assertThat(firstEventReceived.await(5, TimeUnit.SECONDS), is(true));

                // Close the client side of the stream to cancel it
                source.close();
            }
        }

        // The server must detect client closure, stop scanning, and release the active stream slot
        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> recordService.activeStreams() == 0);

        // A subsequent stream must succeed because the cancelled stream slot was released
        FrameSet result = stream(topicId);
        assertThat(result.values(), containsInAnyOrder("a", "b", "c", "d", "e"));
        assertThat(result.done().getString("reason"), is(equalTo("complete")));
    }

    @Test
    void testStreamRecordsClientCancellationStopsReceivingMoreRecords() throws InterruptedException {
        final String topicName = UUID.randomUUID().toString();
        String[] values = new String[20];
        for (int i = 0; i < values.length; i++) {
            values[i] = "item-" + i;
        }
        String topicId = topicWithValues(topicName, values);

        List<JsonObject> receivedRecords = new CopyOnWriteArrayList<>();
        List<String> receivedEvents = new CopyOnWriteArrayList<>();
        CountDownLatch recordsReceived = new CountDownLatch(1);

        try (Client webClient = ClientBuilder.newClient()) {
            WebTarget target = recordsTarget(webClient, clusterId1, topicId, Map.of("filter[offset]", "gte,0"));
            try (SseEventSource source = SseEventSource.target(target).build()) {
                source.register(event -> {
                    String eventName = event.getName();
                    receivedEvents.add(eventName);
                    if (eventName == null) {
                        Json.createReader(new StringReader(event.readData(String.class))).readArray()
                                .stream()
                                .map(JsonValue::asJsonObject)
                                .forEach(receivedRecords::add);
                        recordsReceived.countDown();
                    }
                });
                source.open();

                assertThat(recordsReceived.await(5, TimeUnit.SECONDS), is(true));
                source.close();
            }
        }

        // Server detects cancellation and finishes cleanly
        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> recordService.activeStreams() == 0);

        // Verify that initial records were received prior to cancellation
        assertThat(receivedRecords, is(not(empty())));
        receivedRecords.forEach(rec -> assertThat(rec.getString("type"), is(equalTo("records"))));

        // The client closed the stream before receiving the terminal "done" event
        assertThat(receivedEvents, not(hasItem("done")));
    }

    private void assertInvalidQueryParameter(String topicId, Map<String, Object> params, String parameter) {
        whenRequesting(req -> req
                .accept(MediaType.SERVER_SENT_EVENTS)
                .queryParams(params)
                .get("", clusterId1, topicId))
            .assertThat()
            .statusCode(is(Status.BAD_REQUEST.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.source.parameter", contains(parameter));
    }

    @Test
    void testStreamRecordsWithInvalidPartition() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        assertInvalidQueryParameter(topicIds.get(topicName), Map.of("filter[partition]", -1), "filter[partition]");
    }

    @Test
    void testStreamRecordsFromNonexistentPartition() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        FrameSet result = stream(topicIds.get(topicName), Map.of("filter[partition]", 2));

        assertThat(result.records(), is(empty()));
        assertThat(result.done().getString("reason"), is(equalTo("complete")));
    }

    @ParameterizedTest
    @ValueSource(ints = { -1, 0, 1001 })
    void testStreamRecordsWithInvalidPageSize(int pageSize) {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        assertInvalidQueryParameter(topicIds.get(topicName), Map.of("page[size]", pageSize), "page[size]");
    }

    @ParameterizedTest
    @CsvSource({
        "2000-01-01T00:00:00.000Z, 2000-01-02T00:00:00.000Z, 2000-01-01T00:00:00.000Z, 2",
        "2000-01-01T00:00:00.000Z, 2000-01-02T00:00:00.000Z, 2000-01-02T00:00:00.000Z, 1",
        "2000-01-01T00:00:00.000Z, 2000-01-02T00:00:00.000Z, 2000-01-03T00:00:00.000Z, 0"
    })
    void testStreamRecordsAsOfTimestamp(Instant ts1, Instant ts2, Instant tsSearch, int expectedResults) {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2, Map.of("retention.ms", "-1"));
        recordUtils.produceRecord(topicName, ts1, null, "the-key1", "the-value1");
        recordUtils.produceRecord(topicName, ts2, null, "the-key2", "the-value2");
        awaitTopicSize(topicName, 2);

        FrameSet result = stream(topicIds.get(topicName), Map.of("filter[timestamp]", "gte," + tsSearch));

        assertThat(result.records(), hasSize(expectedResults));
        assertThat(result.done().getString("reason"), is(not(equalTo("timeout"))));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "2000-01-01T00:00:00.000Z",
        "eq,2000-01-02T00:00:00.000Z",
        "gte,1969-12-31T23:59:59.999Z"
    })
    void testStreamRecordsAsOfInvalidTimestamp(String tsSearch) {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        assertInvalidQueryParameter(topicIds.get(topicName), Map.of("filter[timestamp]", tsSearch), "filter[timestamp]");
    }

    @ParameterizedTest
    @CsvSource({
        "0, 3",
        "1, 2",
        "2, 1",
        "3, 0",
        "4, 0"
    })
    void testStreamRecordsByStartingOffset(int startingOffset, int expectedResults) {
        final String topicName = UUID.randomUUID().toString();
        String topicId = topicWithValues(topicName, "the-value-0", "the-value-1", "the-value-2");

        FrameSet result = stream(topicId, Map.of(
                "filter[partition]", 0,
                "filter[offset]", "gte," + startingOffset));

        assertThat(result.records(), hasSize(expectedResults));
        assertThat(result.records().stream().map(rec -> attributes(rec).getInt("offset")).toList(),
                is(equalTo(IntStream.range(startingOffset, startingOffset + expectedResults).boxed().toList())));
        assertThat(result.done().getString("reason"), is(equalTo("complete")));
    }

    @ParameterizedTest
    @ValueSource(strings = { "-1", "gte,-1", "eq,2" })
    void testStreamRecordsByInvalidStartingOffset(String startingOffset) {
        final String topicName = UUID.randomUUID().toString();
        String topicId = topicWithValues(topicName, "the-value-0", "the-value-1", "the-value-2");

        assertInvalidQueryParameter(topicId, Map.of(
                "filter[partition]", 0,
                "filter[offset]", startingOffset), "filter[offset]");
    }

    @ParameterizedTest
    @ValueSource(ints = { 10, 20, 30, 40, 200 })
    void testStreamLatestRecords(int limit) {
        final String topicName = UUID.randomUUID().toString();
        final int totalRecords = 100;
        String[] values = new String[totalRecords];

        for (int i = 0; i < totalRecords; i++) {
            values[i] = "the-value-" + i;
        }

        // A single partition, the page size limits the records read from each partition
        String topicId = topicWithValues(topicName, values);

        FrameSet result = stream(topicId, Map.of("page[size]", limit));

        // The latest `limit` records, in the order they were written
        String[] expected = Arrays.copyOfRange(values, Math.max(0, totalRecords - limit), totalRecords);
        assertThat(result.values(), contains(expected));
    }

    @Test
    void testStreamRecordsIncludeOnlyHeaders() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);
        for (int i = 0; i < 3; i++) {
            recordUtils.produceRecord(topicName, null, Map.of("h1", "h1-value-" + i), "the-key-" + i, "the-value-" + i);
        }
        awaitTopicSize(topicName, 3);

        FrameSet result = stream(topicIds.get(topicName), Map.of("fields[records]", "headers"));

        assertThat(result.records(), hasSize(3));

        for (JsonObject rec : result.records()) {
            assertThat(rec.getString("type"), is(equalTo("records")));
            assertThat(attributes(rec).keySet(), is(equalTo(Set.of("headers"))));
            assertThat(attributes(rec).getJsonObject("headers").containsKey("h1"), is(true));
        }
    }

    @Test
    void testStreamRecordWithEmptyValue() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        recordUtils.produceRecord(topicName, null, null, null, "");
        awaitTopicSize(topicName, 1);

        FrameSet result = stream(topicIds.get(topicName));

        assertThat(result.records(), hasSize(1));
        assertThat(attributes(result.records().get(0)).getString("value"), is(equalTo("")));
    }

    @Test
    void testStreamRecordWithBinaryValueWithinLimit() throws NoSuchAlgorithmException {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        final byte[] data = new byte[16];
        SecureRandom.getInstanceStrong().nextBytes(data);
        data[15] = (byte) 0xFF; // ensure at least one byte invalid UTF-8

        recordUtils.produceRecord(topicName, null, null, null, data);
        awaitTopicSize(topicName, 1);

        FrameSet result = stream(topicIds.get(topicName));

        assertThat(result.records(), hasSize(1));
        JsonObject rec = result.records().get(0);
        JsonObject valueMeta = rec.getJsonObject("meta").getJsonObject("content").getJsonObject("value");

        assertThat(attributes(rec).getString("value"), is(equalTo(Base64.getEncoder().encodeToString(data))));
        assertThat(valueMeta.getString("type"), is(equalTo("application/octet-stream")));
        assertThat(valueMeta.getString("encoding"), is(equalTo("base64")));
    }

    @Test
    void testStreamRecordWithBinaryValueExceedingLimit() throws NoSuchAlgorithmException {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 2);

        final byte[] data = new byte[512];
        SecureRandom.getInstanceStrong().nextBytes(data);
        data[511] = (byte) 0xFF; // ensure at least one byte invalid UTF-8

        recordUtils.produceRecord(topicName, null, null, null, data);
        awaitTopicSize(topicName, 1);

        FrameSet result = stream(topicIds.get(topicName), Map.of("maxValueLength", 100));

        assertThat(result.records(), hasSize(1));
        JsonObject rec = result.records().get(0);
        JsonObject valueMeta = rec.getJsonObject("meta").getJsonObject("content").getJsonObject("value");

        assertThat(isNullOrAbsent(attributes(rec), "value"), is(true));
        assertThat(valueMeta.getString("type"), is(equalTo("application/octet-stream")));
        assertThat(valueMeta.getBoolean("omitted"), is(true));
    }

    @Test
    void testStreamRecordWithBinaryHeader() throws NoSuchAlgorithmException {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);

        final byte[] binaryHeaderValue = new byte[8];
        SecureRandom.getInstanceStrong().nextBytes(binaryHeaderValue);
        binaryHeaderValue[7] = (byte) 0xFF; // ensure at least one byte invalid UTF-8

        recordUtils.produceRecord(topicName, null, Map.<String, Object>of("binHeader", binaryHeaderValue), null, (byte[]) null);
        awaitTopicSize(topicName, 1);

        FrameSet result = stream(topicIds.get(topicName));

        assertThat(result.records(), hasSize(1));
        JsonObject rec = result.records().get(0);
        JsonObject headerMeta = rec.getJsonObject("meta").getJsonObject("content")
                .getJsonObject("headers").getJsonObject("binHeader");

        assertThat(attributes(rec).getJsonObject("headers").getString("binHeader"),
                is(equalTo(Base64.getEncoder().encodeToString(binaryHeaderValue))));
        assertThat(headerMeta.getString("type"), is(equalTo("application/octet-stream")));
        assertThat(headerMeta.getString("encoding"), is(equalTo("base64")));
    }

    @ParameterizedTest
    @CsvSource({
        "  1,   1, true",
        "  5,   5, true",
        " 99,  99, true",
        "100, 100, false",
        "101, 100, false",
        "200, 100, false",
        "   , 100, false",
    })
    void testStreamRecordWithValueLengthLimit(Integer maxValueLength, int responseValueLength, boolean expectTruncated) {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        String h1Value = "h".repeat(100);
        String key = "k".repeat(100);
        String value = "v".repeat(100);
        recordUtils.produceRecord(topicName, null, Map.of("h1", h1Value), key, value);
        awaitTopicSize(topicName, 1);

        Map<String, Object> params = new HashMap<>(1);
        if (maxValueLength != null) {
            params.put("maxValueLength", maxValueLength);
        }

        FrameSet result = stream(topicIds.get(topicName), params);

        assertThat(result.records(), hasSize(1));
        JsonObject rec = result.records().get(0);
        JsonObject attrs = attributes(rec);

        assertThat(attrs.getJsonObject("headers").getString("h1"), is(equalTo(h1Value.substring(0, responseValueLength))));
        assertThat(attrs.getString("key"), is(equalTo(key.substring(0, responseValueLength))));
        assertThat(attrs.getString("value"), is(equalTo(value.substring(0, responseValueLength))));

        if (expectTruncated) {
            JsonObject content = rec.getJsonObject("meta").getJsonObject("content");
            assertThat(content.getJsonObject("key").getBoolean("truncated"), is(true));
            assertThat(content.getJsonObject("value").getBoolean("truncated"), is(true));
            assertThat(content.getJsonObject("headers").getJsonObject("h1").getBoolean("truncated"), is(true));
        } else {
            assertThat(isNullOrAbsent(rec, "meta"), is(true));
        }
    }

    @Test
    void testStreamRecordWithOffsetBeforeBeginning() {
        final String topicName = UUID.randomUUID().toString();
        String topicId = topicWithValues(topicName, "first", "second", "third", "fourth");

        topicUtils.deleteRecords(topicName, 0, 3);

        await().atMost(5, TimeUnit.SECONDS)
            .until(() -> topicUtils.getTopicSize(topicName) == 1);

        FrameSet result = stream(topicId, Map.of("filter[offset]", "gte,0"));

        assertThat(result.records(), hasSize(1));
        assertThat(attributes(result.records().get(0)).getInt("offset"), is(equalTo(3)));
        assertThat(attributes(result.records().get(0)).getString("value"), is(equalTo("fourth")));
    }

    @Test
    void testStreamRecordMagicByteWithoutRegistry() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        var value = "\u0000rest of value";
        recordUtils.produceRecord(topicName, null, null, null, value);
        awaitTopicSize(topicName, 1);

        // Kafka Y has no associated registry
        FrameSet result = stream(CLUSTER_ID_Y, topicIds.get(topicName), Map.of());

        assertThat(result.records(), hasSize(1));
        assertThat(attributes(result.records().get(0)).getInt("offset"), is(equalTo(0)));
        // The value is treated as binary data
        assertThat(attributes(result.records().get(0)).getString("value"),
                is(equalTo(Base64.getEncoder().encodeToString(value.getBytes()))));
    }

    @Test
    void testStreamRecordWithSchemaNotFound() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 1);
        var value = "\u0000rest of value";
        recordUtils.produceRecord(topicName, null, null, null, value);
        awaitTopicSize(topicName, 1);

        FrameSet result = stream(topicIds.get(topicName));

        assertThat(result.records(), hasSize(1));
        assertThat(attributes(result.records().get(0)).getInt("offset"), is(equalTo(0)));
        // The value is treated as binary data
        assertThat(attributes(result.records().get(0)).getString("value"),
                is(equalTo(Base64.getEncoder().encodeToString(value.getBytes()))));

        assertThat(logCapture.records(), hasItem(hasProperty("message", containsString(
                // 1919251316 = 64-bit long integer from bytes of `rest` string
                "Message: No content with id/hash 'contentId-1919251316' was found"
        ))));
    }

    @Test
    void testStreamRecordsFromTimestampAcrossPartitions() {
        final String topicName = UUID.randomUUID().toString();
        var topicIds = topicUtils.createTopics(List.of(topicName), 3);

        Instant ts1 = Instant.now();
        List<Instant> timestamps = List.of(ts1, ts1.plusSeconds(1), ts1.plusSeconds(2));

        for (int i = 0; i < timestamps.size(); i++) {
            for (int partition = 0; partition < 3; partition++) {
                recordUtils.produceRecord(topicName, partition, timestamps.get(i), null, null, "message " + i);
            }
        }

        awaitTopicSize(topicName, 9);

        for (int i = 0; i < timestamps.size(); i++) {
            // The page size is applied to each partition, so all records at or after the timestamp are read
            FrameSet result = stream(topicIds.get(topicName), Map.of(
                    "filter[timestamp]", "gte," + timestamps.get(i),
                    "page[size]", 3));

            Set<String> expected = new HashSet<>();
            for (int partition = 0; partition < 3; partition++) {
                for (int offset = i; offset < 3; offset++) {
                    expected.add(partition + ":" + offset);
                }
            }

            List<String> received = result.records().stream()
                    .map(rec -> attributes(rec).getInt("partition") + ":" + attributes(rec).getInt("offset"))
                    .toList();

            assertThat(received, containsInAnyOrder(expected.toArray(String[]::new)));
        }
    }

    private static String[] expectedValues(String expected) {
        if (expected == null || expected.isBlank() || "''".equals(expected.trim())) {
            return new String[0];
        }

        return Set.of(expected.trim().split("\\|")).toArray(String[]::new);
    }

    private static JsonObject attributes(JsonObject rec) {
        return rec.getJsonObject("attributes");
    }

    private static boolean isNullOrAbsent(JsonObject object, String name) {
        return !object.containsKey(name) || object.isNull(name);
    }
}
