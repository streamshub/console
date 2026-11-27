package io.streamshub.console.api.support;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaCluster;
import io.kroxylicious.kubernetes.api.v1alpha1.virtualkafkaclusterstatus.Ingresses;
import io.kroxylicious.kubernetes.api.v1alpha1.virtualkafkaclusterstatus.ingresses.LoadBalancerIngressPoints;

/**
 * Matches a configured Kafka cluster's {@code bootstrap.servers} against the
 * bootstrap addresses from discovered {@link VirtualKafkaCluster} resource.
 */
public final class VirtualKafkaClusterMatcher {

    private VirtualKafkaClusterMatcher() {
    }

    public static boolean matches(String configBootstrapServers, VirtualKafkaCluster vkc) {
        if (configBootstrapServers == null || vkc == null || vkc.getStatus() == null) {
            return false;
        }

        // Real ingresses
        List<Ingresses> ingresses = vkc.getStatus().getIngresses();

        if (ingresses == null || ingresses.isEmpty()) {
            return false;
        }

        // Prepare candidates from config
        List<String> configCandidates = new ArrayList<>();
        for (String entry : configBootstrapServers.split(",")) {
            configCandidates.add(entry.trim().toLowerCase(Locale.ROOT));
        }

        // Compare config vs real
        for (Ingresses ingress : ingresses) {
            for (String candidate : ingressCandidates(ingress)) {
                if (configCandidates.contains(candidate)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static List<String> ingressCandidates(Ingresses ingress) {
        List<String> candidates = new ArrayList<>();

        String bootstrapServer = ingress.getBootstrapServer();

        // No bootstrap reported yet
        if (bootstrapServer == null) {
            return candidates;
        }

        int portSeparator = bootstrapServer.lastIndexOf(':');

        if (portSeparator < 0) {
            return candidates;
        }

        String port = bootstrapServer.substring(portSeparator + 1);

        // Bootstrap address exactly as the ingress reports it.
        candidates.add(bootstrapServer.toLowerCase(Locale.ROOT));

        // A LoadBalancer can also be reached via a hostname or IP, so build a candidate for each one and add port
        List<LoadBalancerIngressPoints> loadBalancerIngressPoints = ingress.getLoadBalancerIngressPoints();

        if (loadBalancerIngressPoints != null) {
            for (LoadBalancerIngressPoints point : loadBalancerIngressPoints) {
                // Candidate: "<hostname>:<port>", only if a hostname was reported.
                Optional.ofNullable(point.getHostname())
                        .ifPresent(hostname -> candidates.add((hostname + ":" + port).toLowerCase(Locale.ROOT)));
                // Candidate: "<ip>:<port>", only if an IP was reported.
                Optional.ofNullable(point.getIp())
                        .ifPresent(ip -> candidates.add((ip + ":" + port).toLowerCase(Locale.ROOT)));
            }
        }

        return candidates;
    }
}
