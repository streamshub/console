package com.github.eyefloaters.console.api.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;

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

    @Inject
    SharedIndexInformer<Kafka> kafkaInformer;

    @Inject
    Supplier<Admin> clientSupplier;

    public List<KafkaCluster> listClusters(ListRequestContext<KafkaCluster> listSupport) {
        return kafkaInformer.getStore()
                .list()
                .stream()
                .map(k -> consoleListener(k).map(l -> toKafkaCluster(k, l)).orElse(null))
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
            .thenApply(nothing -> new KafkaCluster(
                        get(result::clusterId),
                        get(result::nodes).stream().map(Node::fromKafkaModel).toList(),
                        Node.fromKafkaModel(get(result::controller)),
                        enumNames(get(result::authorizedOperations))))
            .thenApply(this::addKafkaResourceData)
            .toCompletionStage();
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
            .ifPresent(kafka -> consoleListener(kafka)
                    .ifPresent(l -> {
                        cluster.setName(kafka.getMetadata().getName());
                        cluster.setNamespace(kafka.getMetadata().getNamespace());
                        cluster.setCreationTimestamp(kafka.getMetadata().getCreationTimestamp());
                        cluster.setBootstrapServers(l.getBootstrapServers());
                        cluster.setAuthType(getAuthType(kafka, l).orElse(null));
                    }));

        return cluster;
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
            .sorted((l1, l2) -> Integer.compare(listenerSortKey(l1), listenerSortKey(l2)))
            .findFirst()
            .map(listener -> listenerStatus(kafka, listener));
    }

    static boolean supportedAuthentication(GenericKafkaListener listener) {
        Optional<KafkaListenerAuthentication> listenerAuth = Optional.ofNullable(listener.getAuth());
        String authType = listenerAuth.map(auth -> auth.getType()).orElse("");

        if (authType.isBlank()) {
            return true;
        } else if (KafkaListenerAuthenticationOAuth.TYPE_OAUTH.equals(authType)) {
            return true;
        } else if (KafkaListenerAuthenticationCustom.TYPE_CUSTOM.equals(authType)) {
            return listenerAuth
                .map(KafkaListenerAuthenticationCustom.class::cast)
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

    static int listenerSortKey(GenericKafkaListener listener) {
        return annotatedListener(listener) ? -1 : 1;
    }

    static boolean annotatedListener(GenericKafkaListener listener) {
        return Optional.ofNullable(listener.getConfiguration())
            .map(GenericKafkaListenerConfiguration::getBootstrap)
            .map(config -> config.getAnnotations())
            .map(annotations -> annotations.get("eyefloaters.github.com/console-listener"))
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
