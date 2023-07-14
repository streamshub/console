package org.bf2.admin.kafka.systemtest.deployment;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.bf2.admin.kafka.admin.KafkaAdminConfigRetriever;
import org.testcontainers.containers.GenericContainer;

import java.util.Map;

public class KafkaUnsecuredResourceManager implements QuarkusTestResourceLifecycleManager {

    public static final int MAX_PARTITIONS = 100;
    public static final int EXCESSIVE_PARTITIONS = 101;

    Map<String, String> initArgs;
    DeploymentManager deployments;
    GenericContainer<?> kafkaContainer;

    @Override
    public void init(Map<String, String> initArgs) {
        this.initArgs = Map.copyOf(initArgs);
    }

    @Override
    public Map<String, String> start() {
        deployments = DeploymentManager.newInstance(false);
        kafkaContainer = deployments.getKafkaContainer();
        String externalBootstrap = deployments.getExternalBootstrapServers();
        String profile = "%" + initArgs.get("profile") + ".";

        return Map.of(profile + KafkaAdminConfigRetriever.BOOTSTRAP_SERVERS, externalBootstrap);
    }

    @Override
    public void stop() {
        deployments.shutdown();
    }

}
