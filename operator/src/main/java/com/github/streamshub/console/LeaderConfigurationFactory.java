package com.github.streamshub.console;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.javaoperatorsdk.operator.api.config.LeaderElectionConfiguration;
import io.javaoperatorsdk.operator.api.config.LeaderElectionConfigurationBuilder;

@ApplicationScoped
public class LeaderConfigurationFactory {

    public static final String LEASE_NAME = "streamshub-console-operator-lease";

    @Inject
    @ConfigProperty(name = "console.operator.lease-identity")
    private Optional<String> leaseIdentity;

    @Produces
    @ApplicationScoped
    public LeaderElectionConfiguration buildLeaderElectionConfiguration() {
        return LeaderElectionConfigurationBuilder.aLeaderElectionConfiguration(LEASE_NAME)
                // Identity defaults to the host name, which in Kube will be the Pod name
                .withIdentity(leaseIdentity.orElse(null))
                .build();
    }
}
