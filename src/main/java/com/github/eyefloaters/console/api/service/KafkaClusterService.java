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

    public List<KafkaCluster> listClusters() {
        return kafkaInformer.getStore()
                .list()
                .stream()
                .flatMap(k -> externalListeners(k)
                        .map(l -> {
                            KafkaCluster c = new KafkaCluster(k.getStatus().getClusterId(), null, null, null);
                            c.setName(k.getMetadata().getName());
                            c.setBootstrapServers(l.getBootstrapServers());
                            c.setAuthType(getAuthType(k, l).orElse(null));
                            return c;
                        }))
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

    KafkaCluster addKafkaResourceData(KafkaCluster cluster) {
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

    static List<String> enumNames(Collection<? extends Enum<?>> values) {
        return Optional.ofNullable(values)
                .map(Collection::stream)
                .map(ops -> ops.map(Enum::name).toList())
                .orElse(null);
    }
}
