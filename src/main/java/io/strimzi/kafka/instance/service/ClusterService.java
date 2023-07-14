package io.strimzi.kafka.instance.service;

import static io.strimzi.kafka.instance.BlockingSupplier.get;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;

import io.strimzi.kafka.instance.model.Cluster;
import io.strimzi.kafka.instance.model.Node;

@ApplicationScoped
public class ClusterService {

    @Inject
    Supplier<Admin> clientSupplier;

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
            .toCompletionStage();
    }

    List<String> enumNames(Collection<? extends Enum<?>> values) {
        return Optional.ofNullable(values)
                .map(Collection::stream)
                .map(ops -> ops.map(Enum::name).toList())
                .orElse(null);
    }
}
