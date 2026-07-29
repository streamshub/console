package com.github.streamshub.console.api.v1alpha1.spec.template;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DeploymentSpecTemplate {

    @JsonPropertyDescription("""
            Template for the console pod. Allows configuration of pod metadata, \
            scheduling constraints (affinity, tolerations, topology spread constraints, \
            and node selector), and the server container image, pull policy, resources, \
            and environment variables.
            """)
    private PodTemplate template;

    public PodTemplate getTemplate() {
        return template;
    }

    public void setTemplate(PodTemplate template) {
        this.template = template;
    }

}
