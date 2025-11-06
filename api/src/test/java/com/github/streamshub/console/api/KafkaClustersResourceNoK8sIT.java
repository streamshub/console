package com.github.streamshub.console.api;

import java.net.URI;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.clients.CommonClientConfigs;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.streamshub.console.api.service.KafkaClusterService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainNoK8sProfile;
import com.github.streamshub.console.test.TestHelper;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
@TestHTTPEndpoint(KafkaClustersResource.class)
@TestProfile(TestPlainNoK8sProfile.class)
class KafkaClustersResourceNoK8sIT {

    @Inject
    Config config;

    @Inject
    Map<String, KafkaContext> configuredContexts;

    @Inject
    KafkaClusterService kafkaClusterService;

    @Inject
    ConsoleConfig consoleConfig;

    TestHelper utils;

    String clusterId1;
    String clusterId2;
    URI bootstrapServers;
    URI randomBootstrapServers;

    @BeforeEach
    void setup() {
        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        randomBootstrapServers = URI.create(consoleConfig.getKafka()
                .getCluster("test-kafka2")
                .map(k -> k.getProperties().get("bootstrap.servers"))
                .orElseThrow());

        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);

        clusterId1 = consoleConfig.getKafka().getCluster("test-kafka1").get().getId();
        clusterId2 = consoleConfig.getKafka().getCluster("test-kafka2").get().getId();
        kafkaClusterService.setListUnconfigured(false);
    }

    @Test
    void testListClusters() {
        whenRequesting(req -> req.queryParam("fields[kafkas]", "name,status,nodePools,listeners").get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.size()", equalTo(2))
            .body("data.id", containsInAnyOrder(clusterId1, clusterId2))
            .body("data.attributes.name", containsInAnyOrder("test-kafka1", "test-kafka2"))
            .body("data.find { it.attributes.name == 'test-kafka1'}.attributes.status", is(nullValue()))
            .body("data.find { it.attributes.name == 'test-kafka1'}.attributes.nodePools", is(nullValue()))
            .body("data.find { it.attributes.name == 'test-kafka1'}.attributes.listeners", is(nullValue()))
            .body("data.find { it.attributes.name == 'test-kafka2'}.attributes.status", is(nullValue()))
            .body("data.find { it.attributes.name == 'test-kafka2'}.attributes.listeners", is(nullValue()));
    }

}
