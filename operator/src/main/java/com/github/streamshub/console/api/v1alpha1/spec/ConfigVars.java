package com.github.streamshub.console.api.v1alpha1.spec;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConfigVars {

    List<ConfigVar> values = new ArrayList<>();

    List<ConfigVarSource> valuesFrom = new ArrayList<>();

    public List<ConfigVar> getValues() {
        return values;
    }

    public void setValues(List<ConfigVar> values) {
        this.values = values;
    }

    public List<ConfigVarSource> getValuesFrom() {
        return valuesFrom;
    }

    public void setValuesFrom(List<ConfigVarSource> valuesFrom) {
        this.valuesFrom = valuesFrom;
    }

}