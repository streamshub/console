package io.streamshub.console.api.support;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaCluster;
import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaClusterBuilder;
import io.kroxylicious.kubernetes.api.v1alpha1.virtualkafkaclusterstatus.ingresses.LoadBalancerIngressPointsBuilder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VirtualKafkaClusterMatcherTest {

    @Test
    void exactSingleAddressMatches() {
        var vkc = vkcWithIngress("vkc-bootstrap.ns.svc:9092");
        assertTrue(VirtualKafkaClusterMatcher.matches("vkc-bootstrap.ns.svc:9092", vkc));
    }

    @Test
    void hostIsCaseInsensitive() {
        var vkc = vkcWithIngress("VKC-Bootstrap.ns.svc:9092");
        assertTrue(VirtualKafkaClusterMatcher.matches("vkc-bootstrap.ns.svc:9092", vkc));
    }

    @Test
    void differentPortDoesNotMatch() {
        var vkc = vkcWithIngress("vkc-bootstrap.ns.svc:9092");
        assertFalse(VirtualKafkaClusterMatcher.matches("vkc-bootstrap.ns.svc:9093", vkc));
    }

    @Test
    void anyEntryInCommaSeparatedListCanMatch() {
        var vkc = vkcWithIngress("vkc-bootstrap.ns.svc:9092");
        assertTrue(VirtualKafkaClusterMatcher.matches("other:9092,vkc-bootstrap.ns.svc:9092", vkc));
    }

    @Test
    void matchesViaLoadBalancerIpWhenBootstrapServerIsADnsName() {
        var vkc = vkcWithIngressAndLoadBalancerPoint("vkc-lb.example.com:9092", null, "203.0.113.5");
        assertTrue(VirtualKafkaClusterMatcher.matches("203.0.113.5:9092", vkc));
    }

    @Test
    void matchesViaLoadBalancerHostnameWhenBootstrapServerIsAnIp() {
        var vkc = vkcWithIngressAndLoadBalancerPoint("203.0.113.5:9092", "vkc-lb.example.com", null);
        assertTrue(VirtualKafkaClusterMatcher.matches("vkc-lb.example.com:9092", vkc));
    }

    @Test
    void checksAllIngressesNotJustTheFirst() {
        var vkc = vkcWithIngresses("first.ns.svc:9092", "second.ns.svc:9092");
        assertTrue(VirtualKafkaClusterMatcher.matches("second.ns.svc:9092", vkc));
    }

    @Test
    void noIngressesYetDoesNotMatchAndDoesNotThrow() {
        var vkc = vkcWithNoIngresses();
        assertFalse(VirtualKafkaClusterMatcher.matches("vkc-bootstrap.ns.svc:9092", vkc));
    }

    @Test
    void nullConfiguredBootstrapServersDoesNotMatchAndDoesNotThrow() {
        var vkc = vkcWithIngress("vkc-bootstrap.ns.svc:9092");
        assertFalse(VirtualKafkaClusterMatcher.matches(null, vkc));
    }

    private static VirtualKafkaCluster vkcWithIngress(String bootstrapServer) {
        return new VirtualKafkaClusterBuilder()
                .withNewMetadata().withName("test-vkc").withNamespace("default").endMetadata()
                .withNewStatus()
                    .addNewIngress()
                        .withName("test-ingress")
                        .withBootstrapServer(bootstrapServer)
                    .endIngress()
                .endStatus()
                .build();
    }

    private static VirtualKafkaCluster vkcWithIngresses(String... bootstrapServers) {
        var builder = new VirtualKafkaClusterBuilder()
                .withNewMetadata().withName("test-vkc").withNamespace("default").endMetadata()
                .withNewStatus();

        for (int i = 0; i < bootstrapServers.length; i++) {
            builder = builder.addNewIngress()
                    .withName("ingress-" + i)
                    .withBootstrapServer(bootstrapServers[i])
                .endIngress();
        }

        return builder.endStatus().build();
    }

    private static VirtualKafkaCluster vkcWithIngressAndLoadBalancerPoint(String bootstrapServer, String hostname, String ip) {
        return new VirtualKafkaClusterBuilder()
                .withNewMetadata().withName("test-vkc").withNamespace("default").endMetadata()
                .withNewStatus()
                    .addNewIngress()
                        .withName("test-ingress")
                        .withBootstrapServer(bootstrapServer)
                        .withLoadBalancerIngressPoints(List.of(new LoadBalancerIngressPointsBuilder()
                                .withHostname(hostname)
                                .withIp(ip)
                                .build()))
                    .endIngress()
                .endStatus()
                .build();
    }

    private static VirtualKafkaCluster vkcWithNoIngresses() {
        return new VirtualKafkaClusterBuilder()
                .withNewMetadata().withName("test-vkc").withNamespace("default").endMetadata()
                .withNewStatus()
                .endStatus()
                .build();
    }
}
