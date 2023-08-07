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
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;

import com.github.eyefloaters.console.api.model.Cluster;
import com.github.eyefloaters.console.api.model.Node;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthentication;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
import io.strimzi.api.kafka.model.listener.arraylistener.KafkaListenerType;
import io.strimzi.api.kafka.model.status.ListenerStatus;

import static com.github.eyefloaters.console.api.BlockingSupplier.get;

@ApplicationScoped
public class ClusterService {

    @Inject
    SharedIndexInformer<Kafka> kafkaInformer;

    @Inject
    Supplier<Admin> clientSupplier;

    public List<Cluster> listClusters() {
        return kafkaInformer.getStore()
                .list()
                .stream()
                .flatMap(k -> externalListeners(k)
                        .map(l -> {
                            Cluster c = new Cluster();
                            c.setName(k.getMetadata().getName());
                            c.setClusterId(k.getStatus().getClusterId());
                            c.setBootstrapServers(l.getBootstrapServers());
                            c.setAuthType(getAuthType(k, l).orElse(null));
                            return c;
                        }))
                .toList();
    }

    public CompletionStage<Cluster> describeCluster() {
        Admin adminClient = clientSupplier.get();
        DescribeClusterResult result = adminClient.describeCluster();

        return KafkaFuture.allOf(
                result.authorizedOperations(),
                result.clusterId(),
                result.controller(),
                result.nodes())
            .thenApply(nothing -> new Cluster(
                        get(result::clusterId),
                        get(result::nodes).stream().map(Node::fromKafkaModel).toList(),
                        Node.fromKafkaModel(get(result::controller)),
                        enumNames(get(result::authorizedOperations))))
            .thenApply(this::addKafkaResourceData)
            .toCompletionStage();
    }

    Cluster addKafkaResourceData(Cluster cluster) {
        findCluster(kafkaInformer, cluster.getClusterId())
            .ifPresent(kafka -> externalListeners(kafka)
                    .forEach(l -> {
                        cluster.setName(kafka.getMetadata().getName());
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
        return kafka.getStatus()
                .getListeners()
                .stream()
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

    List<String> enumNames(Collection<? extends Enum<?>> values) {
        return Optional.ofNullable(values)
                .map(Collection::stream)
                .map(ops -> ops.map(Enum::name).toList())
                .orElse(null);
    }
}
