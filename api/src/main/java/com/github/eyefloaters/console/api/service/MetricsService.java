package com.github.eyefloaters.console.api.service;

import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
        HttpClient client = HttpClient.newBuilder()
                .build();

        Instant now = Instant.now();
        String time = Double.toString(now.toEpochMilli() / 1000d);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(prometheusUrl.get() + "/api/v1/query"))
                .header("Content-type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&" +
                        "time=" + time))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                    logger.warnf("Failed to retrieve Kafka cluster metrics: %s", response.body());
                    return Collections.emptyMap();
                }

                JsonObject metrics;

                try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                    metrics = reader.readObject();
                }

                return metrics.getJsonObject("data")
                    .getJsonArray("result")
                    .stream()
                    .map(JsonObject.class::cast)
                    .map(entry -> {
                        JsonObject metric = entry.getJsonObject("metric");
                        String metricName = metric.getString(METRIC_NAME);

                        Map<String, String> attributes = metric.keySet()
                                .stream()
                                .filter(Predicate.not(METRIC_NAME::equals))
                                .map(key -> Map.entry(key, metric.getString(key)))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        String value = entry.getJsonArray("value").getString(1);

                        return Map.entry(metricName, new Metrics.ValueMetric(value, attributes));
                    })
                    .collect(Collectors.groupingBy(
                            Map.Entry::getKey,
                            Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
            });
    }

    CompletionStage<Map<String, List<Metrics.RangeMetric>>> queryRanges(String query) {
        HttpClient client = HttpClient.newBuilder()
                .build();

        Instant now = Instant.now();

        String start = Double.toString(now.minus(30, ChronoUnit.MINUTES).toEpochMilli() / 1000d);
        String end = Double.toString(now.toEpochMilli() / 1000d);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(prometheusUrl.get() + "/api/v1/query_range"))
                .header("Content-type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "query=" + URLEncoder.encode(query, StandardCharsets.UTF_8) + "&" +
                        "start=" + start + "&" +
                        "end=" + end + "&" +
                        "step=60"))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                    logger.warnf("Failed to retrieve Kafka cluster metrics: %s", response.body());
                    return Collections.emptyMap();
                }

                JsonObject metrics;

                try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                    metrics = reader.readObject();
                }

                return metrics.getJsonObject("data")
                    .getJsonArray("result")
                    .stream()
                    .map(JsonObject.class::cast)
                    .map(entry -> {
                        JsonObject metric = entry.getJsonObject("metric");
                        String metricName = metric.getString(METRIC_NAME);

                        Map<String, String> attributes = metric.keySet()
                                .stream()
                                .filter(Predicate.not(METRIC_NAME::equals))
                                .map(key -> Map.entry(key, metric.getString(key)))
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                        List<RangeEntry> values = entry.getJsonArray("values")
                                .stream()
                                .map(JsonArray.class::cast)
                                .map(e -> new Metrics.RangeEntry(
                                        Instant.ofEpochMilli((long) (e.getJsonNumber(0).doubleValue() * 1000d)),
                                        e.getString(1)
                                ))
                                .toList();

                        return Map.entry(metricName, new Metrics.RangeMetric(values, attributes));
                    })
                    .collect(Collectors.groupingBy(
                            Map.Entry::getKey,
                            Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
            });
    }
}
