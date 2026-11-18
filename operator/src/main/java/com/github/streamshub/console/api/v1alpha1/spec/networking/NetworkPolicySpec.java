package com.github.streamshub.console.api.v1alpha1.spec.networking;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NetworkPolicySpec {

    @JsonPropertyDescription("Whether to enable generation of NetworkPolicies. When unset, inherits the operator-level default.")
    private Boolean enabled;

    @JsonPropertyDescription("Ingress configuration for the generated NetworkPolicies.")
    private NetworkPolicyIngressSpec ingress;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public NetworkPolicyIngressSpec getIngress() {
        return ingress;
    }

    public void setIngress(NetworkPolicyIngressSpec ingress) {
        this.ingress = ingress;
    }
}
