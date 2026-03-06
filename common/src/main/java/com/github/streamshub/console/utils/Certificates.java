package com.github.streamshub.console.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Base64;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Utility class for loading X.509 certificates from live TLS endpoints.
 *
 * <p>The certificate-fetching logic is shared between:
 * <ul>
 *   <li>{@code TrustAllCertificateManager} — which uses it at runtime in dev
 *       mode to dynamically trust Kafka cluster certificates.</li>
 *   <li>System test infrastructure — which uses it to fetch the Keycloak
 *       certificate for import into a truststore.</li>
 * </ul>
 */
public class Certificates {

    private static final Logger LOGGER = LoggerFactory.getLogger(Certificates.class);

    private Certificates() {}

    /**
     * Connects to {@code host:port} over TLS and returns the full server
     * certificate chain as a concatenated PEM string.
     *
     * <p>This is the Java equivalent of:
     * <pre>{@code
     *   openssl s_client -connect ${HOST}:${PORT} -showcerts </dev/null 2>/dev/null \
     *     | openssl x509 -outform PEM
     * }</pre>
     *
     * <p>A trust-all {@link X509TrustManager} is used intentionally — the
     * purpose of this call is to <em>retrieve</em> an untrusted certificate so
     * it can be explicitly imported or trusted, not to validate it.
     *
     * @param host the hostname or IP address to connect to
     * @param port the TLS port (typically {@code 443} or a Kafka SSL port)
     * @return a PEM-encoded string containing all certificates in the chain,
     *         or {@code null} if the connection failed
     */
    public static String loadFromHost(String host, int port) {
        try {
            SSLContext sc = SSLContext.getInstance("TLSv1.2");
            sc.init(null, new TrustManager[]{new TrustAllManager()}, new SecureRandom());
            SSLSocketFactory factory = sc.getSocketFactory();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try (SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) {
                for (Certificate certificate : socket.getSession().getPeerCertificates()) {
                    out.write("-----BEGIN CERTIFICATE-----\n".getBytes(StandardCharsets.UTF_8));
                    out.write(Base64.getMimeEncoder(80, new byte[]{'\n'}).encode(certificate.getEncoded()));
                    out.write("\n-----END CERTIFICATE-----\n".getBytes(StandardCharsets.UTF_8));
                }
            }

            LOGGER.warn("Certificate at {}:{} is being trusted automatically", host, port);
            return out.toString(StandardCharsets.UTF_8).trim();

        } catch (Exception e) {
            LOGGER.info("Failed to retrieve certificate from {}:{} — {}", host, port, e.getMessage());
            return null;
        }
    }

    /**
     * A no-op {@link X509TrustManager} that accepts all certificates.
     *
     * <p>Used intentionally when fetching an unknown certificate for explicit
     * import into a truststore — not for general TLS validation.</p>
     */
    static final class TrustAllManager implements X509TrustManager {
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
}