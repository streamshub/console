package com.github.eyefloaters.console;

import jakarta.enterprise.context.ApplicationScoped;

import io.javaoperatorsdk.operator.api.config.LeaderElectionConfiguration;

@ApplicationScoped
public class LeaderConfiguration extends LeaderElectionConfiguration {
    public LeaderConfiguration() {
        super("console-operator-lease");
    }
}