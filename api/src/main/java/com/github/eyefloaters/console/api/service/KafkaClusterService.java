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
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.Annotations;
import com.github.eyefloaters.console.api.model.KafkaCluster;
import com.github.eyefloaters.console.api.model.Node;
import com.github.eyefloaters.console.api.support.ListRequestContext;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthentication;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationCustom;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationOAuth;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListenerConfiguration;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.api.kafka.model.status.KafkaStatus;
import io.strimzi.api.kafka.model.status.ListenerStatus;

import static com.github.eyefloaters.console.api.BlockingSupplier.get;

@ApplicationScoped
public class KafkaClusterService {

    private static final String METRICS_QUERY_TEMPLATE = """
            sum by (node, __console_metric_name__) (
            label_replace(
              label_replace(
                rate(container_cpu_usage_seconds_total{namespace="%1$s",pod=~"%2$s-kafka-\\\\d+"}[1m]),
                "node",
                "$1",
                "pod",
                ".+-kafka-(\\\\d+)"
              ),
              "__console_metric_name__",
              "cpu_usage_seconds",
              "",
              ""
            )
          )
          or
          sum by (node, __console_metric_name__) (
            label_replace(
              label_replace(
                container_memory_usage_bytes{namespace="%1$s",pod=~"%2$s-kafka-\\\\d+"},
                "node",
                "$1",
                "pod",
                ".+-kafka-(\\\\d+)"
              ),
              "__console_metric_name__",
              "memory_usage_bytes",
              "",
              ""
            )
          )
          or
          sum by (node, __console_metric_name__) (
            label_replace(
              label_replace(
                kafka_server_socket_server_metrics_incoming_byte_rate{listener="%3$s",namespace="%1$s",pod=~"%2$s-kafka-\\\\d+"},
                "node",
                "$1",
                "pod",
                ".+-kafka-(\\\\d+)"
              ),
              "__console_metric_name__",
              "incoming_byte_rate",
              "",
              ""
            )
          )
          or
          sum by (node, __console_metric_name__) (
            label_replace(
              label_replace(
                kafka_server_socket_server_metrics_outgoing_byte_rate{listener="%3$s",namespace="%1$s",pod=~"%2$s-kafka-\\\\d+"},
                "node",
                "$1",
                "pod",
                ".+-kafka-(\\\\d+)"
              ),
              "__console_metric_name__",
              "outgoing_byte_rate",
              "",
              ""
            )
          )
          or
          sum by (node, __console_metric_name__) (
            label_replace(
              label_replace(
                kubelet_volume_stats_capacity_bytes{namespace="%1$s",persistentvolumeclaim=~"data(?:-\\\\d+)?-%2$s-kafka-\\\\d+"},
                "node",
                "$1",
                "persistentvolumeclaim",
                ".+-kafka-(\\\\d+)"
              ),
              "__console_metric_name__",
              "volume_stats_capacity_bytes",
              "",
              ""
            )
          )
          or
          sum by (node, __console_metric_name__) (
            label_replace(
              label_replace(
                kubelet_volume_stats_used_bytes{namespace="%1$s",persistentvolumeclaim=~"data(?:-\\\\d+)?-%2$s-kafka-\\\\d+"},
                "node",
                "$1",
                "persistentvolumeclaim",
                ".+-kafka-(\\\\d+)"
              ),
              "__console_metric_name__",
              "volume_stats_used_bytes",
              "",
              ""
            )
          )
          """;

    @Inject
    Logger logger;

    @Inject
    SharedIndexInformer<Kafka> kafkaInformer;

    @Inject
    Supplier<Admin> clientSupplier;

    @Inject
    @ConfigProperty(name = "console.metrics.prometheus-url")
    Optional<String> prometheusUrl;

    public List<KafkaCluster> listClusters(ListRequestContext<KafkaCluster> listSupport) {
        return kafkaInformer.getStore()
                .list()
                .stream()
                .map(k -> exposedListener(k)
                        .map(l -> listenerStatus(k, l))
                        .map(l -> toKafkaCluster(k, l))
                        .orElse(null))
                .filter(Objects::nonNull)
                .map(listSupport::tally)
                .filter(listSupport::betweenCursors)
                .sorted(listSupport.getSortComparator())
                .dropWhile(listSupport::beforePageBegin)
                .takeWhile(listSupport::pageCapacityAvailable)
                .toList();
    }

    public CompletionStage<KafkaCluster> describeCluster(List<String> fields) {
        Admin adminClient = clientSupplier.get();
        DescribeClusterOptions options = new DescribeClusterOptions()
                .includeAuthorizedOperations(fields.contains(KafkaCluster.Fields.AUTHORIZED_OPERATIONS));
        DescribeClusterResult result = adminClient.describeCluster(options);

        return KafkaFuture.allOf(
                result.authorizedOperations(),
                result.clusterId(),
                result.controller(),
                result.nodes())
            .toCompletionStage()
            .thenApply(nothing -> new KafkaCluster(
                        get(result::clusterId),
                        get(result::nodes).stream().map(Node::fromKafkaModel).toList(),
                        Node.fromKafkaModel(get(result::controller)),
                        enumNames(get(result::authorizedOperations))))
            .thenApply(this::addKafkaResourceData)
            .thenCompose(cluster -> addMetrics(cluster, fields));
    }

    KafkaCluster toKafkaCluster(Kafka kafka, ListenerStatus listener) {
        KafkaCluster cluster = new KafkaCluster(kafka.getStatus().getClusterId(), null, null, null);
        cluster.setName(kafka.getMetadata().getName());
        cluster.setNamespace(kafka.getMetadata().getNamespace());
        cluster.setCreationTimestamp(kafka.getMetadata().getCreationTimestamp());
        cluster.setBootstrapServers(listener.getBootstrapServers());
        cluster.setAuthType(getAuthType(kafka, listener).orElse(null));
        return cluster;
    }

    KafkaCluster addKafkaResourceData(KafkaCluster cluster) {
        findCluster(kafkaInformer, cluster.getId())
            .ifPresent(kafka -> exposedListener(kafka)
                    .map(l -> listenerStatus(kafka, l))
                    .ifPresent(l -> {
                        cluster.setName(kafka.getMetadata().getName());
                        cluster.setNamespace(kafka.getMetadata().getNamespace());
                        cluster.setCreationTimestamp(kafka.getMetadata().getCreationTimestamp());
                        cluster.setBootstrapServers(l.getBootstrapServers());
                        cluster.setAuthType(getAuthType(kafka, l).orElse(null));
                    }));

        return cluster;
    }

    CompletionStage<KafkaCluster> addMetrics(KafkaCluster cluster, List<String> fields) {
        if (!fields.contains(KafkaCluster.Fields.METRICS)) {
            return CompletableFuture.completedStage(cluster);
        }

        if (prometheusUrl.isEmpty()) {
            logger.warnf("Kafka cluster metrics were requested, but Prometheus URL is not configured");
            return CompletableFuture.completedStage(cluster);
        }

        String namespace = cluster.getNamespace();
        String name = cluster.getName();
        String listenerLabel = findCluster(kafkaInformer, cluster.getId())
            .flatMap(kafka -> exposedListener(kafka))
            .map(listener -> "%s-%d".formatted(listener.getName().toUpperCase(Locale.ROOT), listener.getPort()))
            .orElse(""); // listener/throughput metrics will not be available

        HttpClient client = HttpClient.newBuilder()
                .build();

        String query = URLEncoder.encode(
                METRICS_QUERY_TEMPLATE.formatted(namespace, name, listenerLabel),
                StandardCharsets.UTF_8);

        Instant now = Instant.now();

        String start = Double.toString(now.minus(30, ChronoUnit.MINUTES).toEpochMilli() / 1000d);
        String end = Double.toString(now.toEpochMilli() / 1000d);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(prometheusUrl.get() + "/api/v1/query_range"))
                .header("Content-type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(
                        "query=" + query + "&" +
                        "start=" + start + "&" +
                        "end=" + end + "&" +
                        "step=60"))
                .build();

        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                    logger.warnf("Failed to retrieve Kafka cluster metrics: %s", response.body());
                    return cluster;
                }

                JsonObject metrics;

                try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                    metrics = reader.readObject();
                }

                metrics.getJsonObject("data")
                    .getJsonArray("result")
                    .stream()
                    .map(JsonObject.class::cast)
                    .forEach(entry -> {
                        JsonObject metric = entry.getJsonObject("metric");
                        String metricName = metric.getString("__console_metric_name__");
                        String node = metric.getString("node");

                        List<Object[]> values = entry.getJsonArray("values")
                                .stream()
                                .map(JsonArray.class::cast)
                                .map(e -> new Object[] {
                                        Instant.ofEpochMilli((long) (e.getJsonNumber(0).doubleValue() * 1000d)),
                                        e.getString(1)
                                })
                                .toList();

                        cluster.getMetrics()
                            .computeIfAbsent(metricName, k -> new LinkedHashMap<>())
                            .put(node, values);
                    });

                return cluster;
            });
    }

    public static Optional<Kafka> findCluster(SharedIndexInformer<Kafka> kafkaInformer, String clusterId) {
        return kafkaInformer.getStore()
                .list()
                .stream()
                .filter(k -> Objects.equals(clusterId, k.getStatus().getClusterId()))
                .findFirst();
    }

    public static Optional<ListenerStatus> consoleListener(Kafka kafka) {
        return kafka.getSpec().getKafka().getListeners().stream()
            .filter(listener -> !KafkaListenerType.INTERNAL.equals(listener.getType()))
            .filter(KafkaClusterService::supportedAuthentication)
            .sorted((l1, l2) -> Integer.compare(
                    listenerSortKey(l1, Annotations.CONSOLE_LISTENER),
                    listenerSortKey(l2, Annotations.CONSOLE_LISTENER)))
            .findFirst()
            .map(listener -> listenerStatus(kafka, listener));
    }

    /**
     * Find the listener to be exposed via the API for the given Kafka instance.
     * Listeners annotated as the (1) exposed-listener or the (2) console-listener
     * will be preferred, in that order.
     */
    public static Optional<GenericKafkaListener> exposedListener(Kafka kafka) {
        var comparator = Comparator
            .comparingInt((GenericKafkaListener listener) ->
                listenerSortKey(listener, Annotations.EXPOSED_LISTENER))
            .thenComparingInt((GenericKafkaListener listener) ->
                listenerSortKey(listener, Annotations.CONSOLE_LISTENER));

        return kafka.getSpec().getKafka().getListeners().stream()
            .filter(listener -> !KafkaListenerType.INTERNAL.equals(listener.getType()))
            .sorted(comparator)
            .findFirst();
    }

    static boolean supportedAuthentication(GenericKafkaListener listener) {
        KafkaListenerAuthentication listenerAuth = listener.getAuth();

        if (listenerAuth == null) {
            return true;
        } else if (listenerAuth instanceof KafkaListenerAuthenticationOAuth) {
            return true;
        } else if (listenerAuth instanceof KafkaListenerAuthenticationCustom customAuth) {
            return Optional.of(customAuth)
                .filter(KafkaListenerAuthenticationCustom::isSasl)
                .map(KafkaListenerAuthenticationCustom::getListenerConfig)
                .map(listenerConfig -> listenerConfig.get("sasl.enabled.mechanisms"))
                .map(mechanisms -> Arrays.asList(mechanisms.toString().toUpperCase(Locale.ROOT).split(",")))
                .map(mechanisms -> mechanisms.contains("OAUTHBEARER"))
                .orElse(false);
        } else {
            return false;
        }
    }

    static int listenerSortKey(GenericKafkaListener listener, Annotations listenerAnnotation) {
        return annotatedListener(listener, listenerAnnotation) ? -1 : 1;
    }

    static boolean annotatedListener(GenericKafkaListener listener, Annotations listenerAnnotation) {
        return Optional.ofNullable(listener.getConfiguration())
            .map(GenericKafkaListenerConfiguration::getBootstrap)
            .map(config -> config.getAnnotations())
            .map(annotations -> annotations.get(listenerAnnotation.value()))
            .map(Boolean::valueOf)
            .orElse(false);
    }

    static ListenerStatus listenerStatus(Kafka kafka, GenericKafkaListener listener) {
        String listenerName = listener.getName();

        return Optional.ofNullable(kafka.getStatus())
            .map(KafkaStatus::getListeners)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .filter(listenerStatus -> listenerName.equals(listenerStatus.getName()))
            .findFirst()
            .orElse(null);
    }

    public static Optional<String> getAuthType(Kafka kafka, ListenerStatus listener) {
        return listenerSpec(kafka, listener)
                .map(GenericKafkaListener::getAuth)
                .map(KafkaListenerAuthentication::getType);
    }

    static Optional<GenericKafkaListener> listenerSpec(Kafka kafka, ListenerStatus listener) {
        return kafka.getSpec()
                .getKafka()
                .getListeners()
                .stream()
                .filter(sl -> sl.getName().equals(listener.getName()))
                .findFirst();
    }

    static List<String> enumNames(Collection<? extends Enum<?>> values) {
        return Optional.ofNullable(values)
                .map(Collection::stream)
                .map(ops -> ops.map(Enum::name).toList())
                .orElse(null);
    }
}
