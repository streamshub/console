package com.github.eyefloaters.console.api.service;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Predicate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.Metrics;
import com.github.eyefloaters.console.api.model.Metrics.RangeEntry;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@ApplicationScoped
public class MetricsService {

    private static final String METRIC_NAME = "__console_metric_name__";

    @Inject
    Logger logger;

    @Inject
    @ConfigProperty(name = "console.metrics.prometheus-url")
    Optional<String> prometheusUrl;

    public boolean isDisabled() {
        return prometheusUrl.isEmpty();
    }

    CompletionStage<Map<String, List<Metrics.ValueMetric>>> queryValues(String query) {
        Instant now = Instant.now();
        String time = Double.toString(now.toEpochMilli() / 1000d);
        BodyPublisher body = HttpRequest.BodyPublishers.ofString(
                "query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&" +
                "time=" + time);

        return fetchMetrics("query", body, (metric, attributes) -> {
            String value = metric.getJsonArray("value").getString(1); // ignore timestamp in first position
            return new Metrics.ValueMetric(value, attributes);
        });
    }

    CompletionStage<Map<String, List<Metrics.RangeMetric>>> queryRanges(String query) {
        Instant now = Instant.now();
        String start = Double.toString(now.minus(30, ChronoUnit.MINUTES).toEpochMilli() / 1000d);
        String end = Double.toString(now.toEpochMilli() / 1000d);
        BodyPublisher body = HttpRequest.BodyPublishers.ofString(
                "query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&" +
                "start=" + start + "&" +
                "end=" + end + "&" +
                "step=60");

        return fetchMetrics("query_range", body, (metric, attributes) -> {
            List<RangeEntry> values = metric.getJsonArray("values")
                    .stream()
                    .map(JsonArray.class::cast)
                    .map(e -> new Metrics.RangeEntry(
                            Instant.ofEpochMilli((long) (e.getJsonNumber(0).doubleValue() * 1000d)),
                            e.getString(1)
                    ))
                    .toList();

            return new Metrics.RangeMetric(values, attributes);
        });
    }

    <M> CompletionStage<Map<String, List<M>>> fetchMetrics(String path,
            BodyPublisher body,
            BiFunction<JsonObject, Map<String, String>, M> builder) {

        HttpClient client = HttpClient.newBuilder()
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(prometheusUrl.get() + "/api/v1/" + path))
                .header("Content-type", "application/x-www-form-urlencoded")
                .POST(body)
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> extractMetrics(response, builder));
    }

    <M> Map<String, List<M>> extractMetrics(HttpResponse<String> response,
            BiFunction<JsonObject, Map<String, String>, M> builder) {

        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            logger.warnf("Failed to retrieve Kafka cluster metrics: %s", response.body());
            return Collections.emptyMap();
        }

        JsonObject metrics;

        try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
            metrics = reader.readObject();
        }

        return metrics.getJsonObject("data").getJsonArray("result")
            .stream()
            .map(JsonObject.class::cast)
            .map(metric -> {
                JsonObject meta = metric.getJsonObject("metric");
                String metricName = meta.getString(METRIC_NAME);

                Map<String, String> attributes = meta.keySet()
                        .stream()
                        .filter(Predicate.not(METRIC_NAME::equals))
                        .map(key -> Map.entry(key, meta.getString(key)))
                        .collect(toMap(Map.Entry::getKey, Map.Entry::getValue));

                return Map.entry(metricName, builder.apply(metric, attributes));
            })
            .collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));
    }
}
