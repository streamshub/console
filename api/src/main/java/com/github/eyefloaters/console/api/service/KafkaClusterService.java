package com.github.eyefloaters.console.api.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.Annotations;
import com.github.eyefloaters.console.api.model.Condition;
import com.github.eyefloaters.console.api.model.KafkaCluster;
import com.github.eyefloaters.console.api.model.KafkaListener;
import com.github.eyefloaters.console.api.model.Node;
import com.github.eyefloaters.console.api.support.ListRequestContext;

import io.fabric8.kubernetes.api.model.ObjectMeta;
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

    @Inject
    Logger logger;

    @Inject
    SharedIndexInformer<Kafka> kafkaInformer;

    @Inject
    Supplier<Admin> clientSupplier;

    @Inject
    MetricsService metricsService;

    public List<KafkaCluster> listClusters(ListRequestContext<KafkaCluster> listSupport) {
        return kafkaInformer.getStore()
                .list()
                .stream()
                .filter(Predicate.not(k -> annotatedKafka(k, Annotations.CONSOLE_HIDDEN)))
                .map(this::toKafkaCluster)
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

    KafkaCluster toKafkaCluster(Kafka kafka) {
        KafkaCluster cluster = new KafkaCluster(kafka.getStatus().getClusterId(), null, null, null);
        setKafkaClusterProperties(cluster, kafka);
        return cluster;
    }

    KafkaCluster addKafkaResourceData(KafkaCluster cluster) {
        findCluster(kafkaInformer, cluster.getId())
            .ifPresent(kafka -> setKafkaClusterProperties(cluster, kafka));

        return cluster;
    }

    void setKafkaClusterProperties(KafkaCluster cluster, Kafka kafka) {
        cluster.setName(kafka.getMetadata().getName());
        cluster.setNamespace(kafka.getMetadata().getNamespace());
        cluster.setCreationTimestamp(kafka.getMetadata().getCreationTimestamp());

        @SuppressWarnings("removal")
        var comparator = Comparator
            .comparingInt((GenericKafkaListener listener) ->
                listenerSortKey(listener, Annotations.EXPOSED_LISTENER))
            .thenComparingInt((GenericKafkaListener listener) ->
                listenerSortKey(listener, Annotations.CONSOLE_LISTENER))
            .thenComparingInt((GenericKafkaListener listener) -> {
                if (KafkaListenerType.INTERNAL.equals(listener.getType())) {
                    // sort internal listeners last
                    return 1;
                }
                return -1;
            });

        var listeners = kafka.getSpec()
            .getKafka()
            .getListeners()
            .stream()
            .filter(Predicate.not(l -> annotatedListener(l, Annotations.CONSOLE_HIDDEN)))
            .sorted(comparator)
            .map(listener -> new KafkaListener(
                        listener.getType().toValue(),
                        listenerStatus(kafka, listener).map(ListenerStatus::getBootstrapServers).orElse(null),
                        getAuthType(listener).orElse(null)))
            .toList();

        cluster.setListeners(listeners);
        setKafkaClusterStatus(cluster, kafka);
    }

    void setKafkaClusterStatus(KafkaCluster cluster, Kafka kafka) {
        Optional.ofNullable(kafka.getStatus())
            .ifPresent(status -> {
                cluster.setKafkaVersion(status.getKafkaVersion());
                Optional.ofNullable(status.getConditions())
                    .ifPresent(conditions -> {
                        cluster.setConditions(conditions.stream().map(Condition::new).toList());

                        conditions.stream()
                            .filter(c -> "NotReady".equals(c.getType()) && "True".equals(c.getStatus()))
                            .findFirst()
                            .ifPresentOrElse(
                                    c -> cluster.setStatus("NotReady"),
                                    () -> cluster.setStatus("Ready"));
                    });
            });
    }

    CompletionStage<KafkaCluster> addMetrics(KafkaCluster cluster, List<String> fields) {
        if (!fields.contains(KafkaCluster.Fields.METRICS)) {
            return CompletableFuture.completedStage(cluster);
        }

        if (metricsService.disabled()) {
            logger.warnf("Kafka cluster metrics were requested, but Prometheus URL is not configured");
            return CompletableFuture.completedStage(cluster);
        }

        String namespace = cluster.getNamespace();
        String name = cluster.getName();

        try (var rangesStream = getClass().getResourceAsStream("/metrics/queries/kafkaCluster_ranges.promql");
             var valuesStream = getClass().getResourceAsStream("/metrics/queries/kafkaCluster_values.promql")) {
            String rangeQuery = new String(rangesStream.readAllBytes(), StandardCharsets.UTF_8)
                    .formatted(namespace, name);
            String valueQuery = new String(valuesStream.readAllBytes(), StandardCharsets.UTF_8)
                    .formatted(namespace, name);

            var rangeResults = metricsService.queryRanges(rangeQuery).toCompletableFuture();
            var valueResults = metricsService.queryValues(valueQuery).toCompletableFuture();

            return CompletableFuture.allOf(
                    rangeResults.thenAccept(cluster.getMetrics().ranges()::putAll),
                    valueResults.thenAccept(cluster.getMetrics().values()::putAll))
                .thenApply(nothing -> cluster);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static Optional<Kafka> findCluster(SharedIndexInformer<Kafka> kafkaInformer, String clusterId) {
        return kafkaInformer.getStore()
                .list()
                .stream()
                .filter(k -> Objects.equals(clusterId, k.getStatus().getClusterId()))
                .filter(Predicate.not(k -> annotatedKafka(k, Annotations.CONSOLE_HIDDEN)))
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
            .flatMap(listener -> listenerStatus(kafka, listener));
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

    static boolean annotatedKafka(Kafka kafka, Annotations listenerAnnotation) {
        return Optional.ofNullable(kafka.getMetadata())
            .map(ObjectMeta::getAnnotations)
            .map(annotations -> annotations.get(listenerAnnotation.value()))
            .map(Boolean::valueOf)
            .orElse(false);
    }

    static boolean annotatedListener(GenericKafkaListener listener, Annotations listenerAnnotation) {
        return Optional.ofNullable(listener.getConfiguration())
            .map(GenericKafkaListenerConfiguration::getBootstrap)
            .map(config -> config.getAnnotations())
            .map(annotations -> annotations.get(listenerAnnotation.value()))
            .map(Boolean::valueOf)
            .orElse(false);
    }

    static Optional<ListenerStatus> listenerStatus(Kafka kafka, GenericKafkaListener listener) {
        String listenerName = listener.getName();

        return Optional.ofNullable(kafka.getStatus())
            .map(KafkaStatus::getListeners)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .filter(listenerStatus -> listenerName.equals(listenerStatus.getName()))
            .findFirst();
    }

    public static Optional<String> getAuthType(Kafka kafka, ListenerStatus listener) {
        return kafka.getSpec()
            .getKafka()
            .getListeners()
            .stream()
            .filter(sl -> sl.getName().equals(listener.getName()))
            .findFirst()
            .flatMap(KafkaClusterService::getAuthType);
    }

    static Optional<String> getAuthType(GenericKafkaListener listener) {
        return Optional.of(listener)
            .map(GenericKafkaListener::getAuth)
            .map(KafkaListenerAuthentication::getType);
    }

    static List<String> enumNames(Collection<? extends Enum<?>> values) {
        return Optional.ofNullable(values)
                .map(Collection::stream)
                .map(ops -> ops.map(Enum::name).toList())
                .orElse(null);
    }
}
