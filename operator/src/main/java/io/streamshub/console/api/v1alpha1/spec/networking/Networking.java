package io.streamshub.console.api.v1alpha1.spec.networking;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Networking {

    @JsonPropertyDescription("NetworkPolicy configuration for the Console operands.")
    private NetworkPolicySpec networkPolicy;

    @JsonPropertyDescription("Template for the generated Route (OpenShift).")
    private RouteTemplate route;

    @JsonPropertyDescription("Template for the generated Ingress (Kubernetes).")
    private IngressTemplate ingress;

    public NetworkPolicySpec getNetworkPolicy() {
        return networkPolicy;
    }

    public void setNetworkPolicy(NetworkPolicySpec networkPolicy) {
        this.networkPolicy = networkPolicy;
    }

    public RouteTemplate getRoute() {
        return route;
    }

    public void setRoute(RouteTemplate route) {
        this.route = route;
    }

    public IngressTemplate getIngress() {
        return ingress;
    }

    public void setIngress(IngressTemplate ingress) {
        this.ingress = ingress;
    }
}
