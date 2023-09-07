package com.github.eyefloaters.console.api.service;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

import com.github.eyefloaters.console.api.model.KafkaCluster;
import com.github.eyefloaters.console.api.model.Node;
import com.github.eyefloaters.console.api.support.ComparatorBuilder;
import com.github.eyefloaters.console.api.support.ListRequestContext;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthentication;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.api.kafka.model.status.ListenerStatus;

import static com.github.eyefloaters.console.api.BlockingSupplier.get;

@ApplicationScoped
public class KafkaClusterService {

    @Inject
    SharedIndexInformer<Kafka> kafkaInformer;

    @Inject
    Supplier<Admin> clientSupplier;

    final ComparatorBuilder<KafkaCluster> comparators =
            new ComparatorBuilder<>(KafkaCluster.Fields::comparator, KafkaCluster.Fields.defaultComparator());

    public List<KafkaCluster> listClusters(ListRequestContext listSupport) {
        return kafkaInformer.getStore()
                .list()
                .stream()
                .map(k -> externalListeners(k).findFirst().map(l -> toKafkaCluster(k, l)).orElse(null))
                .filter(Objects::nonNull)
                .sorted(comparators.fromSort(listSupport.getSort()))
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
                    .forEach(l -> {
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
                .filter(l -> isExternalListener(kafka, l));
    }

    public static boolean isExternalListener(Kafka kafka, ListenerStatus listener) {
        return kafka.getSpec()
                .getKafka()
                .getListeners()
                .stream()
                .filter(sl -> sl.getName().equals(listener.getName()))
                .map(GenericKafkaListener::getType)
                .filter(Objects::nonNull)
                .anyMatch(Predicate.not(KafkaListenerType.INTERNAL::equals));
    }

    public static Optional<String> getAuthType(Kafka kafka, ListenerStatus listener) {
        return kafka.getSpec()
                .getKafka()
                .getListeners()
                .stream()
                .filter(sl -> sl.getName().equals(listener.getName()))
                .findFirst()
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
