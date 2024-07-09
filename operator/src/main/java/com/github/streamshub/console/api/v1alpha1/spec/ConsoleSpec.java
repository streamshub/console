package com.github.streamshub.console.api.v1alpha1.spec;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.generator.annotation.Required;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsoleSpec {

    @Required
    String hostname;

    Images images = new Images();

    List<KafkaCluster> kafkaClusters = new ArrayList<>();

    // TODO: copy EnvVar into console's API to avoid unexpected changes
    List<EnvVar> env = new ArrayList<>();

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public Images getImages() {
        return images;
    }

    public void setImages(Images images) {
        this.images = images;
    }

    public List<KafkaCluster> getKafkaClusters() {
        return kafkaClusters;
    }

    public void setKafkaClusters(List<KafkaCluster> kafkaClusters) {
        this.kafkaClusters = kafkaClusters;
    }

    public List<EnvVar> getEnv() {
        return env;
    }

    public void setEnv(List<EnvVar> env) {
        this.env = env;
    }
}
