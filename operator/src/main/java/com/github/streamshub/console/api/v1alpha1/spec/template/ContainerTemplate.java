package com.github.streamshub.console.api.v1alpha1.spec.template;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContainerTemplate {

    @JsonPropertyDescription("Container image to be used for the container")
    private String image;

    @JsonPropertyDescription("Image pull policy to be used for the container image")
    private String imagePullPolicy;

    @JsonPropertyDescription("CPU and memory resources to reserve.")
    private ResourceRequirements resources;

    @JsonPropertyDescription("Environment variables which should be applied to the container.")
    private List<EnvVar> env;

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getImagePullPolicy() {
        return imagePullPolicy;
    }

    public void setImagePullPolicy(String imagePullPolicy) {
        this.imagePullPolicy = imagePullPolicy;
    }

    public ResourceRequirements getResources() {
        return resources;
    }

    public void setResources(ResourceRequirements resources) {
        this.resources = resources;
    }

    public List<EnvVar> getEnv() {
        return env;
    }

    public void setEnv(List<EnvVar> env) {
        this.env = env;
    }

}
