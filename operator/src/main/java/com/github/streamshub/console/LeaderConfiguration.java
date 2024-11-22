package com.github.streamshub.console;

import jakarta.enterprise.context.ApplicationScoped;

import io.javaoperatorsdk.operator.api.config.LeaderElectionConfiguration;

@ApplicationScoped
public class LeaderConfiguration extends LeaderElectionConfiguration {
    public LeaderConfiguration() {
        super("streamshub-console-operator-lease");
    }
}
