package com.github.eyefloaters.console.kafka.systemtest.deployment;

import java.util.Map;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public abstract class KafkaResourceManager implements QuarkusTestResourceLifecycleManager {

    protected Map<String, String> initArgs;
    protected DeploymentManager deployments;
    protected KafkaContainer kafkaContainer;

    @Override
    public void init(Map<String, String> initArgs) {
        this.initArgs = Map.copyOf(initArgs);
    }

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(deployments, new TestInjector.MatchesType(DeploymentManager.class));
    }
    @Override
    public void stop() {
        deployments.shutdown();
    }

}
