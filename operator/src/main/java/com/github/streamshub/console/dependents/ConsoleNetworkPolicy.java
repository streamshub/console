package com.github.streamshub.console.dependents;

import java.util.ArrayList;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.support.ConfigSupport;

import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRule;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyIngressRuleBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeer;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class ConsoleNetworkPolicy extends BaseNetworkPolicy {

    public static final String NAME = "console-network-policy";

    public ConsoleNetworkPolicy() {
        super(NAME);
    }

    @Override
    protected NetworkPolicy desired(Console primary, Context<Console> context) {
        boolean servingCert = ConfigSupport.servingCertificateReencryptTLS(primary, context);
        boolean tls = servingCert || ConfigSupport.userProvidedPassthroughTLS(primary);
        int containerPort = tls ? 8443 : 8080;

        List<NetworkPolicyPeer> peers = new ArrayList<>();
        peers.addAll(getRouterIngressPeers(primary, context));
        peers.addAll(getCustomIngressPeers(primary));

        NetworkPolicyIngressRuleBuilder ruleBuilder = new NetworkPolicyIngressRuleBuilder()
                .withPorts(getPort(containerPort));

        if (!peers.isEmpty()) {
            ruleBuilder.withFrom(peers);
        }

        NetworkPolicyIngressRule ingressRule = ruleBuilder.build();

        return new NetworkPolicyBuilder()
            .withNewMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels("console"))
            .endMetadata()
            .withNewSpec()
                .withNewPodSelector()
                    .addToMatchLabels("app.kubernetes.io/name", "console")
                    .addToMatchLabels("app.kubernetes.io/instance", primary.getMetadata().getName())
                .endPodSelector()
                .withPolicyTypes("Ingress")
                .withIngress(ingressRule)
            .endSpec()
            .build();
    }
}
