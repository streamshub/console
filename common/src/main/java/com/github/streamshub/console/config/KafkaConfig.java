package com.github.streamshub.console.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.sundr.builder.annotations.Buildable;

@JsonInclude(Include.NON_NULL)
@Buildable(editableEnabled = false)
public class KafkaConfig {

    @Valid
    List<KafkaClusterConfig> clusters = new ArrayList<>();

    @JsonIgnore
    @AssertTrue(message = "Kafka cluster names must be unique")
    public boolean hasUniqueClusterNames() {
        return Named.uniqueNames(clusters);
    }

    @JsonIgnore
    public Optional<KafkaClusterConfig> getCluster(String clusterKey) {
        return clusters.stream()
            .filter(k -> k.clusterKey().equals(clusterKey))
            .findFirst();
    }

    @JsonIgnore
    public Optional<KafkaClusterConfig> getClusterById(String clusterId) {
        return clusters.stream()
            .filter(k -> clusterId.equals(k.getId()))
            .findFirst();
    }

    public List<KafkaClusterConfig> getClusters() {
        return clusters;
    }

    public void setClusters(List<KafkaClusterConfig> clusters) {
        this.clusters = clusters;
    }

}
