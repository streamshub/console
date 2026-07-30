package com.github.streamshub.console.api.v1alpha1.spec.template;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonValue;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContainerTemplate {

    @JsonPropertyDescription("Container image to be used for the container.")
    private String image;

    @JsonPropertyDescription("""
            Image pull policy to be used for the container image. \
            One of Always, IfNotPresent, or Never. \
            Defaults to Always when the image tag is not a digest, \
            and IfNotPresent when a digest is specified.
            """)
    private ImagePullPolicy imagePullPolicy;

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

    public ImagePullPolicy getImagePullPolicy() {
        return imagePullPolicy;
    }

    public void setImagePullPolicy(ImagePullPolicy imagePullPolicy) {
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

    public enum ImagePullPolicy {
        ALWAYS("Always"),
        IF_NOT_PRESENT("IfNotPresent"),
        NEVER("Never");

        private final String value;

        ImagePullPolicy(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }

        @JsonCreator
        public static ImagePullPolicy fromValue(String value) {
            for (var policy : values()) {
                if (policy.value.equals(value)) {
                    return policy;
                }
            }
            throw new IllegalArgumentException("Invalid imagePullPolicy: " + value);
        }
    }
}
