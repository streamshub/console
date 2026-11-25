package io.streamshub.console.dependents;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import io.streamshub.console.api.v1alpha1.Console;
import io.streamshub.console.dependents.conditions.NetworkPolicyPrecondition;
import io.streamshub.console.dependents.conditions.PrometheusPrecondition;

import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeer;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class PrometheusNetworkPolicy extends BaseNetworkPolicy {

    public static final String NAME = "prometheus-network-policy";

    public PrometheusNetworkPolicy() {
        super(NAME);
    }

    @Override
    protected NetworkPolicy desired(Console primary, Context<Console> context) {
        List<NetworkPolicyPeer> peers = new ArrayList<>();
        peers.add(getConsolePodPeer(primary));
        peers.addAll(getCustomIngressPeers(primary));

        NetworkPolicyIngressRule ingressRule = new NetworkPolicyIngressRuleBuilder()
                .withPorts(getPort(9090))
                .withFrom(peers)
                .build();

        return new NetworkPolicyBuilder()
            .withNewMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels("prometheus"))
            .endMetadata()
            .withNewSpec()
                .withNewPodSelector()
                    .addToMatchLabels("app.kubernetes.io/name", "prometheus")
                    .addToMatchLabels("app.kubernetes.io/instance", primary.getMetadata().getName())
                .endPodSelector()
                .withPolicyTypes("Ingress")
                .withIngress(ingressRule)
            .endSpec()
            .build();
    }

    public static class Precondition implements Condition<NetworkPolicy, Console> {
        private static final PrometheusPrecondition<NetworkPolicy> PROMETHEUS = new PrometheusPrecondition<>();
        private static final NetworkPolicyPrecondition NETWORK_POLICY = new NetworkPolicyPrecondition();

        @Override
        public boolean isMet(DependentResource<NetworkPolicy, Console> dependentResource, Console primary, Context<Console> context) {
            return PROMETHEUS.isMet(dependentResource, primary, context)
                    && NETWORK_POLICY.isMet(dependentResource, primary, context);
        }
    }
}
