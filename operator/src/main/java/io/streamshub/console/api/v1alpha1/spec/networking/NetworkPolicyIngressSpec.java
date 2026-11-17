package io.streamshub.console.api.v1alpha1.spec.networking;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeer;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NetworkPolicyIngressSpec {

    @JsonPropertyDescription("Whether to omit the operator-derived default ingress controller source rule.")
    private Boolean disableDefaultFrom;

    @JsonPropertyDescription("List of sources which should be able to access the pod (matching NetworkPolicyIngressRule.from).")
    private List<NetworkPolicyPeer> from;

    public Boolean getDisableDefaultFrom() {
        return disableDefaultFrom;
    }

    public void setDisableDefaultFrom(Boolean disableDefaultFrom) {
        this.disableDefaultFrom = disableDefaultFrom;
    }

    public List<NetworkPolicyPeer> getFrom() {
        return from;
    }

    public void setFrom(List<NetworkPolicyPeer> from) {
        this.from = from;
    }
}
