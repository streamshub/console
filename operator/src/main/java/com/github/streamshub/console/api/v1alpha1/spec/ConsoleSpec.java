package com.github.streamshub.console.api.v1alpha1.spec;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsoleSpec {

    String version;
    String hostname;

    List<KafkaCluster> kafkaClusters = new ArrayList<>();

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public List<KafkaCluster> getKafkaClusters() {
        return kafkaClusters;
    }

    public void setKafkaClusters(List<KafkaCluster> kafkaClusters) {
        this.kafkaClusters = kafkaClusters;
    }

}
