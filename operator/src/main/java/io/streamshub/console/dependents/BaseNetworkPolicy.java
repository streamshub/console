package io.streamshub.console.dependents;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.streamshub.console.api.v1alpha1.Console;
import io.streamshub.console.api.v1alpha1.spec.networking.NetworkPolicyIngressSpec;
import io.streamshub.console.api.v1alpha1.spec.networking.NetworkPolicySpec;
import io.streamshub.console.api.v1alpha1.spec.networking.Networking;
import io.streamshub.console.dependents.support.ConfigSupport;

import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeer;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeerBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPort;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPortBuilder;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

abstract class BaseNetworkPolicy extends CRUDKubernetesDependentResource<NetworkPolicy, Console>
    implements ConsoleResource<NetworkPolicy> {

    private final String resourceName;

    protected BaseNetworkPolicy(String resourceName) {
        super(NetworkPolicy.class);
        this.resourceName = resourceName;
    }

    @Override
    public Optional<NetworkPolicy> getSecondaryResource(Console primary, Context<Console> context) {
        return ConsoleResource.super.getSecondaryResource(primary, context);
    }

    @Override
    public String resourceName() {
        return resourceName;
    }

    protected List<NetworkPolicyPeer> getRouterIngressPeers(Console primary, Context<Console> context) {
        boolean disableDefaultFrom = Optional.ofNullable(primary.getSpec().getNetworking())
                .map(Networking::getNetworkPolicy)
                .map(NetworkPolicySpec::getIngress)
                .map(NetworkPolicyIngressSpec::getDisableDefaultFrom)
                .orElse(false);

        if (!disableDefaultFrom && ConfigSupport.resourceSupported(context, Route.class)) {
            return List.of(
                new NetworkPolicyPeerBuilder()
                    .withNewNamespaceSelector()
                        .addToMatchLabels("policy-group.network.openshift.io/ingress", "")
                    .endNamespaceSelector()
                    .build(),
                new NetworkPolicyPeerBuilder()
                    .withNewNamespaceSelector()
                        .addToMatchLabels("network.openshift.io/policy-group", "ingress")
                    .endNamespaceSelector()
                    .build()
            );
        }

        return Collections.emptyList();
    }

    protected List<NetworkPolicyPeer> getCustomIngressPeers(Console primary) {
        return Optional.ofNullable(primary.getSpec().getNetworking())
                .map(Networking::getNetworkPolicy)
                .map(NetworkPolicySpec::getIngress)
                .map(NetworkPolicyIngressSpec::getFrom)
                .orElseGet(Collections::emptyList);
    }

    protected NetworkPolicyPeer getConsolePodPeer(Console primary) {
        return new NetworkPolicyPeerBuilder()
            .withNewPodSelector()
                .addToMatchLabels("app.kubernetes.io/name", "console")
                .addToMatchLabels("app.kubernetes.io/instance", primary.getMetadata().getName())
            .endPodSelector()
            .build();
    }

    protected NetworkPolicyPort getPort(int portNumber) {
        return new NetworkPolicyPortBuilder()
            .withProtocol("TCP")
            .withNewPort(portNumber)
            .build();
    }
}
