package com.github.streamshub.console;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.security.LdapUserMatcherBuilder;
import com.github.streamshub.console.api.v1alpha1.status.Condition;
import com.github.streamshub.console.dependents.ConsoleDeployment;
import com.github.streamshub.console.dependents.ConsoleResource;
import com.github.streamshub.console.dependents.DexDeployment;
import com.github.streamshub.console.dependents.PrometheusDeployment;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyPeerBuilder;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ConsoleReconcilerNetworkPolicyTest extends ConsoleReconcilerTestBase {

    @Test
    void testNetworkPolicyDisabledByDefault() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("console.example.com")
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "ConsoleDeployment", "PrometheusDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleIngress");
        setConsoleIngressReady(consoleCR);

        awaitReady(consoleCR);

        var netPols = client.network().v1().networkPolicies().inNamespace(CONSOLE_NS).list().getItems();
        assertTrue(netPols.isEmpty(), "No NetworkPolicies should be created when disabled by default");
    }

    @Test
    void testNetworkPolicyEnabledKubernetesIndeterminateIngressSourceWarning() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("console.example.com")
                    .withNewNetworking()
                        .withNewNetworkPolicy()
                            .withEnabled(true)
                        .endNetworkPolicy()
                    .endNetworking()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "ConsoleDeployment", "PrometheusDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleIngress");
        setConsoleIngressReady(consoleCR);

        // Verify Ready and Warning conditions are present for indeterminate ingress source
        org.awaitility.Awaitility.await().atMost(LIMIT).untilAsserted(() -> {
            var refreshedCR = client.resource(consoleCR).get();
            assertNotNull(refreshedCR.getStatus());
            var conditions = refreshedCR.getStatus().getConditions();
            assertTrue(conditions.stream().anyMatch(c -> Condition.Types.READY.equals(c.getType()) && "True".equals(c.getStatus())));
            var warningCondition = conditions.stream()
                    .filter(c -> Condition.Types.WARNING.equals(c.getType()))
                    .findFirst()
                    .orElse(null);
            assertNotNull(warningCondition);
            assertEquals(Condition.Reasons.INDETERMINATE_INGRESS_SOURCE, warningCondition.getReason());
        });

        // Verify ConsoleNetworkPolicy fails open (ports specified, from is empty)
        NetworkPolicy consoleNetPol = client.network().v1().networkPolicies()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + "-console-network-policy")
                .get();

        assertNotNull(consoleNetPol);
        assertEquals(1, consoleNetPol.getSpec().getIngress().size());
        var ingressRule = consoleNetPol.getSpec().getIngress().get(0);
        assertEquals(8080, ingressRule.getPorts().get(0).getPort().getIntVal());
        assertTrue(ingressRule.getFrom() == null || ingressRule.getFrom().isEmpty());

        // Verify PrometheusNetworkPolicy allows console pod
        NetworkPolicy promNetPol = client.network().v1().networkPolicies()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + "-prometheus-network-policy")
                .get();

        assertNotNull(promNetPol);
        var promIngressRule = promNetPol.getSpec().getIngress().get(0);
        assertEquals(9090, promIngressRule.getPorts().get(0).getPort().getIntVal());
        assertEquals(1, promIngressRule.getFrom().size());
        assertEquals("console", promIngressRule.getFrom().get(0).getPodSelector().getMatchLabels().get("app.kubernetes.io/name"));

        // Now supply a peer in `from` and verify warning is cleared
        edit(consoleCR, c -> {
            c.getSpec().getNetworking().getNetworkPolicy().setIngress(
                    new com.github.streamshub.console.api.v1alpha1.spec.networking.NetworkPolicyIngressSpecBuilder()
                            .withFrom(new NetworkPolicyPeerBuilder()
                                    .withNewNamespaceSelector()
                                        .withMatchLabels(Map.of("kubernetes.io/metadata.name", "my-ingress"))
                                    .endNamespaceSelector()
                                    .build())
                            .build());
            return c;
        });

        awaitReady(consoleCR);

        var updatedCR = client.resource(consoleCR).get();
        boolean hasWarning = updatedCR.getStatus().getConditions().stream()
                .anyMatch(c -> Condition.Types.WARNING.equals(c.getType())
                        && Condition.Reasons.INDETERMINATE_INGRESS_SOURCE.equals(c.getReason()));
        assertFalse(hasWarning, "Warning condition should be cleared once peer is supplied");

        var updatedNetPol = client.network().v1().networkPolicies()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + "-console-network-policy")
                .get();
        assertEquals(1, updatedNetPol.getSpec().getIngress().get(0).getFrom().size());
        assertEquals("my-ingress", updatedNetPol.getSpec().getIngress().get(0).getFrom().get(0).getNamespaceSelector().getMatchLabels().get("kubernetes.io/metadata.name"));
    }

    @Test
    void testNetworkPolicyCustomIngressPeers() {
        var customPeer = new NetworkPolicyPeerBuilder()
                .withNewNamespaceSelector()
                    .withMatchLabels(Map.of("kubernetes.io/metadata.name", "ingress-nginx"))
                .endNamespaceSelector()
                .withNewPodSelector()
                    .withMatchLabels(Map.of("app.kubernetes.io/name", "ingress-nginx"))
                .endPodSelector()
                .build();

        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("console.example.com")
                    .withNewNetworking()
                        .withNewNetworkPolicy()
                            .withEnabled(true)
                            .withNewIngress()
                                .withFrom(customPeer)
                            .endIngress()
                        .endNetworkPolicy()
                    .endNetworking()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "ConsoleDeployment", "PrometheusDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleIngress");
        setConsoleIngressReady(consoleCR);

        awaitReady(consoleCR);

        NetworkPolicy consoleNetPol = client.network().v1().networkPolicies()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + "-console-network-policy")
                .get();

        assertNotNull(consoleNetPol);
        var ingressFrom = consoleNetPol.getSpec().getIngress().get(0).getFrom();
        assertEquals(1, ingressFrom.size());
        assertEquals("ingress-nginx", ingressFrom.get(0).getNamespaceSelector().getMatchLabels().get("kubernetes.io/metadata.name"));
        assertEquals("ingress-nginx", ingressFrom.get(0).getPodSelector().getMatchLabels().get("app.kubernetes.io/name"));

        NetworkPolicy promNetPol = client.network().v1().networkPolicies()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + "-prometheus-network-policy")
                .get();

        assertNotNull(promNetPol);
        var promFrom = promNetPol.getSpec().getIngress().get(0).getFrom();
        assertEquals(2, promFrom.size());
        // First is console pod, second is custom peer
        assertEquals("console", promFrom.get(0).getPodSelector().getMatchLabels().get("app.kubernetes.io/name"));
        assertEquals("ingress-nginx", promFrom.get(1).getNamespaceSelector().getMatchLabels().get("kubernetes.io/metadata.name"));
    }

    @Test
    @Tag(REQUIRES_OPENSHIFT_ROUTE)
    void testNetworkPolicyOpenShiftRouterPeersAndDisableDefault() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("console.example.com")
                    .withNewNetworking()
                        .withNewNetworkPolicy()
                            .withEnabled(true)
                        .endNetworkPolicy()
                    .endNetworking()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "ConsoleTrustBundle");
        setConsoleTrustBundleContent(consoleCR);

        awaitDependentsNotReady(consoleCR, "ConsoleRoute");
        setConsoleRouteReady(consoleCR, "console.example.com");

        awaitDependentsNotReady(consoleCR, "ConsoleService");
        createServingCertContent(consoleCR, com.github.streamshub.console.dependents.ConsoleService.NAME);

        awaitDependentsNotReady(consoleCR, "ConsoleDeployment", "PrometheusDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        setDeploymentReady(consoleCR, ConsoleDeployment.NAME);

        awaitReady(consoleCR);

        NetworkPolicy consoleNetPol = client.network().v1().networkPolicies()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + "-console-network-policy")
                .get();

        assertNotNull(consoleNetPol);
        var fromPeers = consoleNetPol.getSpec().getIngress().get(0).getFrom();
        assertEquals(2, fromPeers.size());
        assertEquals("", fromPeers.get(0).getNamespaceSelector().getMatchLabels().get("policy-group.network.openshift.io/ingress"));
        assertEquals("ingress", fromPeers.get(1).getNamespaceSelector().getMatchLabels().get("network.openshift.io/policy-group"));

        // Disable default router peers
        edit(consoleCR, c -> {
            c.getSpec().getNetworking().getNetworkPolicy().setIngress(
                    new com.github.streamshub.console.api.v1alpha1.spec.networking.NetworkPolicyIngressSpecBuilder()
                            .withDisableDefaultFrom(true)
                            .withFrom(new NetworkPolicyPeerBuilder()
                                    .withNewNamespaceSelector()
                                        .withMatchLabels(Map.of("kubernetes.io/metadata.name", "custom-gateway"))
                                    .endNamespaceSelector()
                                    .build())
                            .build());
            return c;
        });

        awaitReady(consoleCR);

        var updatedNetPol = client.network().v1().networkPolicies()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + "-console-network-policy")
                .get();

        var updatedFrom = updatedNetPol.getSpec().getIngress().get(0).getFrom();
        assertEquals(1, updatedFrom.size());
        assertEquals("custom-gateway", updatedFrom.get(0).getNamespaceSelector().getMatchLabels().get("kubernetes.io/metadata.name"));
    }

    @Test
    void testIngressMetadataTemplateApplied() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("console.example.com")
                    .withNewNetworking()
                        .withNewIngress()
                            .withNewMetadata()
                                .withLabels(Map.of("custom-ingress-label", "ingress-val"))
                                .withAnnotations(Map.of("nginx.ingress.kubernetes.io/whitelist-source-range", "203.0.113.0/24"))
                            .endMetadata()
                        .endIngress()
                    .endNetworking()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "ConsoleDeployment", "PrometheusDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleIngress");
        setConsoleIngressReady(consoleCR);

        awaitReady(consoleCR);

        var ingress = client.network().v1().ingresses()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + "-console-ingress")
                .get();

        assertNotNull(ingress);
        assertEquals("ingress-val", ingress.getMetadata().getLabels().get("custom-ingress-label"));
        assertEquals(ConsoleResource.MANAGER, ingress.getMetadata().getLabels().get("app.kubernetes.io/managed-by"));
        assertEquals("203.0.113.0/24", ingress.getMetadata().getAnnotations().get("nginx.ingress.kubernetes.io/whitelist-source-range"));
        assertEquals("HTTP", ingress.getMetadata().getAnnotations().get("nginx.ingress.kubernetes.io/backend-protocol"));
    }

    @Test
    @Tag(REQUIRES_OPENSHIFT_ROUTE)
    void testRouteMetadataTemplateApplied() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("console.example.com")
                    .withNewNetworking()
                        .withNewRoute()
                            .withNewMetadata()
                                .withLabels(Map.of("custom-route-label", "route-val"))
                                .withAnnotations(Map.of("haproxy.router.openshift.io/ip_whitelist", "203.0.113.0/24"))
                            .endMetadata()
                        .endRoute()
                    .endNetworking()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "ConsoleTrustBundle");
        setConsoleTrustBundleContent(consoleCR);

        awaitDependentsNotReady(consoleCR, "ConsoleRoute");
        setConsoleRouteReady(consoleCR, "console.example.com");

        awaitDependentsNotReady(consoleCR, "ConsoleService");
        createServingCertContent(consoleCR, com.github.streamshub.console.dependents.ConsoleService.NAME);

        awaitDependentsNotReady(consoleCR, "ConsoleDeployment", "PrometheusDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        setDeploymentReady(consoleCR, ConsoleDeployment.NAME);

        awaitReady(consoleCR);

        var route = client.resources(io.fabric8.openshift.api.model.Route.class)
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + "-console-route")
                .get();

        assertNotNull(route);
        assertEquals("route-val", route.getMetadata().getLabels().get("custom-route-label"));
        assertEquals(ConsoleResource.MANAGER, route.getMetadata().getLabels().get("app.kubernetes.io/managed-by"));
        assertEquals("203.0.113.0/24", route.getMetadata().getAnnotations().get("haproxy.router.openshift.io/ip_whitelist"));
    }

    @Test
    void testNetworkPolicyWithDexOnKubernetes() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("console.example.com")
                    .withNewNetworking()
                        .withNewNetworkPolicy()
                            .withEnabled(true)
                            .withNewIngress()
                                .withFrom(new NetworkPolicyPeerBuilder()
                                        .withNewNamespaceSelector()
                                            .withMatchLabels(Map.of("kubernetes.io/metadata.name", "my-ingress"))
                                        .endNamespaceSelector()
                                        .build())
                            .endIngress()
                        .endNetworkPolicy()
                    .endNetworking()
                    .withNewSecurity()
                        .withNewLdap()
                            .withHost("ldap.example.com:636")
                            .withBindDN("cn=admin,dc=example,dc=com")
                            .withNewBindSecret()
                                .withValue("bind-password")
                            .endBindSecret()
                            .withStartTLS(Boolean.TRUE)
                            .withNewUserSearch()
                                .withBaseDN("ou=people,dc=example,dc=com")
                                .withUsername("uid")
                            .endUserSearch()
                            .withNewGroupSearch()
                                .withBaseDN("ou=groups,dc=example,dc=com")
                                .withUserMatchers(List.of(new LdapUserMatcherBuilder()
                                        .withUserAttr("DN")
                                        .withGroupAttr("member")
                                        .build()))
                            .endGroupSearch()
                        .endLdap()
                    .endSecurity()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "PrometheusDeployment", "ConsoleDeployment", "DexDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        setDeploymentReady(consoleCR, DexDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleIngress");
        setConsoleIngressReady(consoleCR);

        awaitReady(consoleCR);

        NetworkPolicy dexNetPol = client.network().v1().networkPolicies()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + "-dex-network-policy")
                .get();

        assertNotNull(dexNetPol);
        assertEquals(1, dexNetPol.getSpec().getIngress().size());
        var ingressRule = dexNetPol.getSpec().getIngress().get(0);
        assertEquals(5556, ingressRule.getPorts().get(0).getPort().getIntVal());

        var from = ingressRule.getFrom();
        // Should contain console pod peer and custom ingress peer
        assertEquals(2, from.size());
        assertEquals("console", from.get(0).getPodSelector().getMatchLabels().get("app.kubernetes.io/name"));
        assertEquals("my-ingress", from.get(1).getNamespaceSelector().getMatchLabels().get("kubernetes.io/metadata.name"));
    }
}
