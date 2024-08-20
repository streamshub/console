package com.github.streamshub.console.api.support;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.jboss.logging.Logger;

import io.quarkus.arc.profile.IfBuildProfile;
import io.quarkus.arc.properties.IfBuildProperty;

@ApplicationScoped
@IfBuildProfile("dev")
@IfBuildProperty(name = "console.security.trust-certificates", stringValue = "true")
public class TrustAllCertificateManager implements X509TrustManager {

    @Inject
    Logger log;

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
        TrustManager[] trustAllCerts = {this};
        String certificates = null;

        try {
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, trustAllCerts, new SecureRandom());
            SSLSocketFactory factory = sc.getSocketFactory();
            String[] hostport = bootstrap.split(",")[0].split(":");
            ByteArrayOutputStream certificateOut = new ByteArrayOutputStream();

            try (SSLSocket socket = (SSLSocket) factory.createSocket(hostport[0], Integer.parseInt(hostport[1]))) {
                Certificate[] chain = socket.getSession().getPeerCertificates();
                for (Certificate certificate : chain) {
                    certificateOut.write("-----BEGIN CERTIFICATE-----\n".getBytes(StandardCharsets.UTF_8));
                    certificateOut.write(Base64.getMimeEncoder(80, new byte[] {'\n'}).encode(certificate.getEncoded()));
                    certificateOut.write("\n-----END CERTIFICATE-----\n".getBytes(StandardCharsets.UTF_8));
                }
            }

            certificates = new String(certificateOut.toByteArray(), StandardCharsets.UTF_8).trim();
            log.warnf("Certificate hosted at %s:%s is automatically trusted", hostport[0], hostport[1]);
        } catch (Exception e) {
            log.infof("Exception setting up trusted certificate for host %s: %s", bootstrap, e.getMessage());
        }

        return certificates;
    }

    public X509Certificate[] getAcceptedIssuers() {
        return null; // NOSONAR
    }

    public void checkClientTrusted(X509Certificate[] certs, String authType) { // NOSONAR
        // all trusted
    }

    public void checkServerTrusted(X509Certificate[] certs, String authType) { // NOSONAR
        // all trusted
    }
}
