package com.github.eyefloaters.console.api.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class KafkaConfig {

    List<KafkaClusterConfig> clusters = new ArrayList<>();

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
