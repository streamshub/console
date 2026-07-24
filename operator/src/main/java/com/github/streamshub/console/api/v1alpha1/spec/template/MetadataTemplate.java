package com.github.streamshub.console.api.v1alpha1.spec.template;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetadataTemplate {

    private Map<String, String> labels;
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
