package com.github.eyefloaters.console.api.service;

import java.util.Map;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.apache.kafka.common.config.ConfigResource;
import org.eclipse.microprofile.context.ThreadContext;

import com.github.eyefloaters.console.api.model.ConfigEntry;
import com.github.eyefloaters.console.api.model.Node;

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
                if (cluster.getNodes().stream().mapToInt(Node::getId).mapToObj(String::valueOf).noneMatch(nodeId::equals)) {
                    throw new NotFoundException("No such broker: " + nodeId);
                }
                return cluster;
            })
            .thenComposeAsync(
                    cluster -> configService.describeConfigs(ConfigResource.Type.BROKER, nodeId),
                    threadContext.currentContextExecutor());
    }
}
