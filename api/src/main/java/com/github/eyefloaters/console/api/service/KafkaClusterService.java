package com.github.eyefloaters.console.api.service;

import java.util.Collection;
import java.util.List;
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
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListenerConfiguration;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListenerConfigurationBootstrap;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
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
                .map(k -> externalListeners(k).findFirst().map(l -> toKafkaCluster(k, l)).orElse(null))
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
            .ifPresent(kafka -> externalListeners(kafka)
                    .findFirst()
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

    public static Stream<ListenerStatus> externalListeners(Kafka kafka) {
        return Optional.ofNullable(kafka.getStatus().getListeners())
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .filter(l -> isExternalListener(kafka, l))
                .sorted((l1, l2) -> Integer.compare(listenerSortKey(kafka, l1), listenerSortKey(kafka, l2)));
    }

    public static boolean isExternalListener(Kafka kafka, ListenerStatus listener) {
        return listenerSpec(kafka, listener)
                .map(GenericKafkaListener::getType)
                .map(type -> !KafkaListenerType.INTERNAL.equals(type))
                .orElse(false);
    }

    static int listenerSortKey(Kafka kafka, ListenerStatus listener) {
        return isConsoleListener(kafka, listener) ? -1 : 1;
    }

    static boolean isConsoleListener(Kafka kafka, ListenerStatus listener) {
        return getBootstrapConfig(kafka, listener)
            .map(config -> config.getAnnotations())
            .map(annotations -> annotations.get("eyefloaters.github.com/console-listener"))
            .map(Boolean::valueOf)
            .orElse(false);
    }

    public static Optional<String> getAuthType(Kafka kafka, ListenerStatus listener) {
        return getAuthentication(kafka, listener)
                .map(KafkaListenerAuthentication::getType);
    }

    public static Optional<KafkaListenerAuthentication> getAuthentication(Kafka kafka, ListenerStatus listener) {
        return listenerSpec(kafka, listener)
                .map(GenericKafkaListener::getAuth);
    }

    public static Optional<GenericKafkaListenerConfigurationBootstrap> getBootstrapConfig(
            Kafka kafka, ListenerStatus listener) {
        return listenerSpec(kafka, listener)
                .map(GenericKafkaListener::getConfiguration)
                .map(GenericKafkaListenerConfiguration::getBootstrap);
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
