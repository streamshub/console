package com.github.streamshub.console.api.v1alpha1.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.fabric8.generator.annotation.ValidationRule;
import io.fabric8.kubernetes.api.model.ConfigMapEnvSource;
import io.fabric8.kubernetes.api.model.SecretEnvSource;
import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
@ValidationRule(
        value = "has(self.configMapRef) || has(self.secretRef)",
        message = "One of `configMapRef` or `secretRef` is required")
public class ConfigVarSource {

    @JsonProperty("configMapRef")
    private ConfigMapEnvSource configMapRef;

    @JsonProperty("prefix")
    private String prefix;

    @JsonProperty("secretRef")
    private SecretEnvSource secretRef;

    public ConfigMapEnvSource getConfigMapRef() {
        return configMapRef;
    }

    public void setConfigMapRef(ConfigMapEnvSource configMapRef) {
        this.configMapRef = configMapRef;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public SecretEnvSource getSecretRef() {
        return secretRef;
    }

    public void setSecretRef(SecretEnvSource secretRef) {
        this.secretRef = secretRef;
    }

}
