package com.github.eyefloaters.console.api.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.WebApplicationException;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.Metrics;
import com.github.eyefloaters.console.api.model.Metrics.RangeEntry;
import com.github.eyefloaters.console.api.support.PrometheusAPI;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@ApplicationScoped
public class MetricsService {

    public static final String METRIC_NAME = "__console_metric_name__";

    @Inject
    Logger logger;

    @Inject
    @ConfigProperty(name = "console.metrics.prometheus-url")
    Supplier<Optional<String>> prometheusUrl;

    @Inject
    @RestClient
    PrometheusAPI prometheusAPI;

    public boolean disabled() {
        return prometheusUrl.get().isEmpty();
    }

    CompletionStage<Map<String, List<Metrics.ValueMetric>>> queryValues(String query) {
        String time = Double.toString(System.currentTimeMillis() / 1000d);

        return fetchMetrics(
            () -> prometheusAPI.query(query, time),
            (metric, attributes) -> {
                // ignore timestamp in first position
                String value = metric.getJsonArray("value").getString(1);
                return new Metrics.ValueMetric(value, attributes);
            });
    }

    CompletionStage<Map<String, List<Metrics.RangeMetric>>> queryRanges(String query) {
        Instant now = Instant.now();
        String start = Double.toString(now.minus(30, ChronoUnit.MINUTES).toEpochMilli() / 1000d);
        String end = Double.toString(now.toEpochMilli() / 1000d);

        return fetchMetrics(
            () -> prometheusAPI.queryRange(query, start, end, "60"),
            (metric, attributes) -> {
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

    <M> CompletionStage<Map<String, List<M>>> fetchMetrics(
            Supplier<JsonObject> operation,
            BiFunction<JsonObject, Map<String, String>, M> builder) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return extractMetrics(operation.get(), builder);
            } catch (WebApplicationException wae) {
                logger.warnf("Failed to retrieve Kafka cluster metrics: %s",
                        wae.getResponse().getEntity());
                return Collections.emptyMap();
            } catch (Exception e) {
                logger.warnf(e, "Failed to retrieve Kafka cluster metrics");
                return Collections.emptyMap();
            }
        });
    }

    <M> Map<String, List<M>> extractMetrics(JsonObject response,
            BiFunction<JsonObject, Map<String, String>, M> builder) {

        return response.getJsonObject("data").getJsonArray("result")
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
