package com.github.eyefloaters.console.kafka.systemtest.deployment;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import com.github.eyefloaters.console.legacy.KafkaAdminConfigRetriever;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

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

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(deployments, new TestInjector.AnnotatedAndMatchesType(DeploymentManager.InjectDeploymentManager.class, DeploymentManager.class));
    }
}
