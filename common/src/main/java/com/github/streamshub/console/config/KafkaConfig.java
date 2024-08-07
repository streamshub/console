package com.github.streamshub.console.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class KafkaConfig {

    List<KafkaClusterConfig> clusters = new ArrayList<>();

    @JsonIgnore
    @AssertTrue(message = "Kafka cluster names must be unique")
    public boolean hasUniqueClusterNames() {
        return clusters.stream().map(KafkaClusterConfig::getName).distinct().count() == clusters.size();
    }

    @JsonIgnore
    public Optional<KafkaClusterConfig> getCluster(String clusterKey) {
        return clusters.stream()
            .filter(k -> k.clusterKey().equals(clusterKey))
            .findFirst();
    }

    public List<KafkaClusterConfig> getClusters() {
        return clusters;
    }

    public void setClusters(List<KafkaClusterConfig> clusters) {
        this.clusters = clusters;
    }

}
