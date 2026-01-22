package com.github.streamshub.console.api.service;

import java.net.URI;
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
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.model.Metrics;
import com.github.streamshub.console.api.model.Metrics.RangeEntry;
import com.github.streamshub.console.api.support.AuthenticationSupport;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.PrometheusAPI;
import com.github.streamshub.console.api.support.TrustStoreSupport;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.config.PrometheusConfig;
import com.github.streamshub.console.config.PrometheusConfig.Type;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.quarkus.tls.TlsConfiguration;

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
    TrustStoreSupport trustStores;

    @Inject
    KubernetesClient k8s;

    @Inject
    KafkaContext kafkaContext;

    Optional<ClientRequestFilter> additionalFilter = Optional.empty();

    public /* test */ void setAdditionalFilter(Optional<ClientRequestFilter> additionalFilter) {
        this.additionalFilter = additionalFilter;
    }

    ClientRequestFilter createAuthenticationFilter(PrometheusConfig config) {
        AuthenticationSupport authentication = new AuthenticationSupport(config);

        return requestContext -> {
            String authHeader = authentication.get().orElseGet(() -> {
                if (config.getType() == Type.OPENSHIFT_MONITORING) {
                    // ServiceAccount needs cluster role `cluster-monitoring-view`
                    return "Bearer " + k8s.getConfiguration().getAutoOAuthToken();
                }
                return null;
            });

            if (authHeader != null) {
                requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
            }
        };
    }

    public PrometheusAPI createClient(ConsoleConfig consoleConfig, KafkaClusterConfig clusterConfig) {
        PrometheusConfig prometheusConfig;
        String sourceName = clusterConfig.getMetricsSource();

        if (sourceName != null) {
            prometheusConfig = consoleConfig.getMetricsSources()
                    .stream()
                    .filter(source -> source.getName().equals(sourceName))
                    .findFirst()
                    .orElseThrow();

            var trustStore = trustStores.getTlsConfiguration(prometheusConfig, null)
                    .map(TlsConfiguration::getTrustStore)
                    .orElse(null);

            RestClientBuilder builder = RestClientBuilder.newBuilder()
                    .baseUri(URI.create(prometheusConfig.getUrl()))
                    .trustStore(trustStore)
                    .register(createAuthenticationFilter(prometheusConfig));

            additionalFilter.ifPresent(builder::register);

            return builder.build(PrometheusAPI.class);
        }

        return null;
    }

    CompletionStage<Map<String, List<Metrics.ValueMetric>>> queryValues(String query) {
        PrometheusAPI prometheusAPI = kafkaContext.prometheus();

        return fetchMetrics(
            () -> prometheusAPI.query(query, Instant.now()),
            (metric, attributes) -> {
                // ignore timestamp in first position
                String value = metric.getJsonArray("value").getString(1);
                return new Metrics.ValueMetric(value, attributes);
            });
    }

    public CompletionStage<Map<String, List<Metrics.RangeMetric>>> queryRanges(String query, int durationMinutes) {
        PrometheusAPI prometheusAPI = kafkaContext.prometheus();

        return fetchMetrics(
            () -> {
                Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
                Instant start = now.minus(durationMinutes, ChronoUnit.MINUTES);
                Instant end = now;
            
                String step = calculateStep(durationMinutes);

                return prometheusAPI.queryRange(query, start, end, step);
            },
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


    private String calculateStep(int durationMinutes) {
        if (durationMinutes <= 15) return "15s";   
        if (durationMinutes <= 60) return "1m";   
        if (durationMinutes <= 360) return "5m";   
        if (durationMinutes <= 1440) return "15m"; 
        if (durationMinutes <= 2880) return "30m"; 
        return "2h";                               
    }

    <M> CompletionStage<Map<String, List<M>>> fetchMetrics(
            Supplier<JsonObject> operation,
            BiFunction<JsonObject, Map<String, String>, M> builder) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                return extractMetrics(operation.get(), builder);
            } catch (WebApplicationException wae) {
                logger.warnf("Failed to retrieve Kafka cluster metrics, status %d: %s",
                        wae.getResponse().getStatus(),
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
