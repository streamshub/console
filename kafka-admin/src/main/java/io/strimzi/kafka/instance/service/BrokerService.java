package io.strimzi.kafka.instance.service;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.apache.kafka.common.config.ConfigResource;
import org.eclipse.microprofile.context.ThreadContext;

import io.strimzi.kafka.instance.model.ConfigEntry;

@ApplicationScoped
public class BrokerService {

    @Inject
    ClusterService clusterService;

    @Inject
    ConfigService configService;

    @Inject
    ThreadContext threadContext;

    public CompletionStage<Map<String, ConfigEntry>> describeConfigs(String nodeId) {
        return clusterService.describeCluster()
            .thenApply(cluster -> {
                if (cluster.getNodes().stream().noneMatch(node -> String.valueOf(node.getId()).equals(nodeId))) {
                    throw new NotFoundException("No such broker: " + nodeId);
                }
                return cluster;
            })
            .thenComposeAsync(
                    cluster -> configService.describeConfigs(ConfigResource.Type.BROKER, nodeId),
                    threadContext.currentContextExecutor());
    }
}
