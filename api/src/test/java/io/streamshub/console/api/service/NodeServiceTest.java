package io.streamshub.console.api.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.config.ConfigResource;
import org.junit.jupiter.api.Test;

import io.streamshub.console.api.support.ContextualExecutorProvider;
import io.streamshub.console.api.support.KafkaContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NodeServiceTest {

    @Test
    void testDescribeConfigsForwardsDocumentationFlag() throws Exception {
        ConfigService configService = mock(ConfigService.class);
        when(configService.describeConfigs(ConfigResource.Type.BROKER, "0", true))
            .thenReturn(CompletableFuture.completedFuture(Map.of()));

        NodeService nodeService = nodeService(configService);
        nodeService.describeConfigs("0", true).toCompletableFuture().get(10, TimeUnit.SECONDS);

        verify(configService).describeConfigs(ConfigResource.Type.BROKER, "0", true);
    }

    @Test
    void testLegacyDescribeConfigsDoesNotRequestDocumentation() throws Exception {
        ConfigService configService = mock(ConfigService.class);
        when(configService.describeConfigs(ConfigResource.Type.BROKER, "0", false))
            .thenReturn(CompletableFuture.completedFuture(Map.of()));

        NodeService nodeService = nodeService(configService);
        nodeService.describeConfigs("0").toCompletableFuture().get(10, TimeUnit.SECONDS);

        verify(configService).describeConfigs(ConfigResource.Type.BROKER, "0", false);
    }

    NodeService nodeService(ConfigService configService) {
        Admin admin = mock(Admin.class);
        DescribeClusterResult cluster = mock(DescribeClusterResult.class);
        when(admin.describeCluster()).thenReturn(cluster);
        when(cluster.nodes()).thenReturn(KafkaFuture.completedFuture(
                List.of(new Node(0, "localhost", 9092))));

        KafkaContext kafkaContext = mock(KafkaContext.class);
        when(kafkaContext.admin()).thenReturn(admin);

        ContextualExecutorProvider executorProvider = mock(ContextualExecutorProvider.class);
        when(executorProvider.currentContextExecutor()).thenReturn(Runnable::run);

        NodeService nodeService = new NodeService();
        nodeService.configService = configService;
        nodeService.kafkaContext = kafkaContext;
        nodeService.threadContext = executorProvider;
        return nodeService;
    }
}
