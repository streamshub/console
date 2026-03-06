package com.github.streamshub.systemtests.utils;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
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
     * Generates a self-signed RSA-4096 X.509 certificate for the given hostname.
     *
     * @param hostname the CN to embed in the certificate subject
     * @param organization the O field in the certificate subject
     * @return a KeyPair and X509Certificate pair
     */
    public static Map.Entry<KeyPair, X509Certificate> generateSelfSignedCertificate(String hostname, String organization) throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(4096, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();

        X500Name subject = new X500Name("CN=" + hostname + ", O=" + organization + ", C=US");
        BigInteger serial = new BigInteger(64, new SecureRandom());
        Date notBefore = Date.from(Instant.now());
        Date notAfter = Date.from(Instant.now().plus(365, ChronoUnit.DAYS));

        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            subject, serial, notBefore, notAfter, subject, keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
            .build(keyPair.getPrivate());

        X509Certificate cert = new JcaX509CertificateConverter()
            .getCertificate(certBuilder.build(signer));

        return Map.entry(keyPair, cert);
    }

    /**
     * Encodes a KeyPair and X509Certificate into PEM strings.
     *
     * @return a Map with keys "tls.key" and "tls.crt"
     */
    public static Map<String, String> toPemStrings(KeyPair keyPair, X509Certificate cert) throws Exception {
        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(keyPair.getPrivate().getEncoded())
            + "\n-----END PRIVATE KEY-----\n";

        String certPem = "-----BEGIN CERTIFICATE-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(cert.getEncoded())
            + "\n-----END CERTIFICATE-----\n";

        return Map.of("tls.key", privateKeyPem, "tls.crt", certPem);
    }

    /**
     * Fetches the live TLS certificate from {@code host:port} and imports it
     * into the JKS truststore at {@code trustStorePath}.
     *
     * @param host           the hostname to fetch the certificate from
     * @param port           the TLS port (typically 443)
     * @param trustStorePath path to the JKS truststore file (created if absent)
     * @param password       truststore password
     * @param alias          alias to use when importing the certificate
     */
    public static void importCertificateIntoTrustStore(String host, int port, Path trustStorePath, String password, String alias) throws Exception {
        LOGGER.info("Retrieving HTTPS certificate from '{}:{}' for truststore", host, port);

        String pemCertificate = loadFromHost(host, port);
        if (pemCertificate == null || pemCertificate.isBlank()) {
            throw new IllegalStateException("No certificate retrieved from " + host + ":" + port);
        }

        KeyStore trustStore = KeyStore.getInstance("JKS");
        if (Files.exists(trustStorePath)) {
            try (var fis = Files.newInputStream(trustStorePath)) {
                trustStore.load(fis, password.toCharArray());
            }
        } else {
            trustStore.load(null, password.toCharArray());
            Files.createDirectories(trustStorePath.getParent());
        }

        if (trustStore.containsAlias(alias)) {
            LOGGER.info("Removing existing alias '{}' from truststore", alias);
            trustStore.deleteEntry(alias);
        }

        byte[] derBytes = Base64.getDecoder().decode(
            pemCertificate
                .replace("-----BEGIN CERTIFICATE-----", "")
                .replace("-----END CERTIFICATE-----", "")
                .replaceAll("\\s", ""));

        Certificate cert = CertificateFactory.getInstance("X.509")
            .generateCertificate(new ByteArrayInputStream(derBytes));

        trustStore.setCertificateEntry(alias, cert);

        try (var fos = Files.newOutputStream(trustStorePath)) {
            trustStore.store(fos, password.toCharArray());
        }

        LOGGER.info("Certificate imported into truststore as alias '{}'", alias);
    }
}