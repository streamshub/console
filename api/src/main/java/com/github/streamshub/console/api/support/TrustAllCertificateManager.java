package com.github.streamshub.console.api.support;

import jakarta.enterprise.context.ApplicationScoped;
import com.github.streamshub.console.utils.Certificates;
import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.properties.IfBuildProperty;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@IfBuildProfile("dev")
@IfBuildProperty(name = "console.security.trust-certificates", stringValue = "true")
public class TrustAllCertificateManager {

    Map<String, String> trustedCertificates = new ConcurrentHashMap<>();

    public void trustClusterCertificate(Map<String, Object> cfg) {
        String bootstrap = (String) cfg.get(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG);
        String trusted = trustedCertificates.computeIfAbsent(bootstrap, this::loadCertificates);

        if (trusted != null) {
            cfg.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, trusted);
            cfg.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
        }
    }

    private String loadCertificates(String bootstrap) {
        String[] hostport = bootstrap.split(",")[0].split(":");
        return Certificates.loadFromHost(hostport[0], Integer.parseInt(hostport[1]));
    }
}
