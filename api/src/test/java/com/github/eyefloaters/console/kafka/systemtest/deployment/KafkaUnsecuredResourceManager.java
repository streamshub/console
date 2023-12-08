package com.github.eyefloaters.console.kafka.systemtest.deployment;

import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaUnsecuredResourceManager extends KafkaResourceManager implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        deployments = DeploymentManager.newInstance(false);
        kafkaContainer = deployments.getKafkaContainer();
        String externalBootstrap = deployments.getExternalBootstrapServers();
        String profile = "%" + initArgs.get("profile") + ".";

        return Map.of(profile + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, externalBootstrap);
    }
}
