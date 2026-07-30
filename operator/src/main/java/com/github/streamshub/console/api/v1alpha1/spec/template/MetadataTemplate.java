package com.github.streamshub.console.api.v1alpha1.spec.template;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataTemplate {

    @JsonPropertyDescription("Additional labels to apply to the resource.")
    private Map<String, String> labels;

    @JsonPropertyDescription("Additional annotations to apply to the resource.")
    private Map<String, String> annotations;

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Map<String, String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(Map<String, String> annotations) {
        this.annotations = annotations;
    }

}
