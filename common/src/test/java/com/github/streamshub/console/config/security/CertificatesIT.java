package com.github.streamshub.console.config.security;

import com.github.streamshub.console.utils.Certificates;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Map;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CertificatesIT {

    // ---------------------------------
    // ------- Loading from host -------
    // ---------------------------------

    /**
     * Tests that loadFromHost returns null when connecting to a port where no server is listening
     */
    @Test
    void loadFromHost_returnsNullOnConnectionRefused() {
        int unusedPort = findUnusedPort();
        String pemResult = Certificates.loadFromHost("127.0.0.1", unusedPort);
        assertThat(pemResult, is(nullValue()));
    }

    /**
     * Tests that loadFromHost returns null when connecting to a non-resolvable hostname
     */
    @Test
    void loadFromHost_returnsNullOnInvalidHost() {
        String pemResult = Certificates.loadFromHost("invalid.local.host", 443);
        assertThat(pemResult, is(nullValue()));
    }

    /**
     * Tests that loadFromHost retrieves a PEM certificate from a live TLS server
     */
    @Test
    void loadFromHost_returnsPemFromLiveTlsServer() throws Exception {
        Map.Entry<KeyPair, X509Certificate> certEntry =
            Certificates.generateSelfSignedCertificate("localhost", "TestOrg");

        int tlsPort = startTlsServer(certEntry.getKey(), certEntry.getValue());
        String pemResult = Certificates.loadFromHost("localhost", tlsPort);

        assertThat(pemResult, is(notNullValue()));
        assertThat(pemResult, containsString("-----BEGIN CERTIFICATE-----"));
        assertThat(pemResult, containsString("-----END CERTIFICATE-----"));
    }

    // ---------------------------------
    // --- Import cert to truststore ---
    // ---------------------------------

    /**
     * Verifies that a certificate can be imported into a new truststore
     * Checks that the truststore file is created and contains the certificate under the specified alias
     */
    @Test
    void importCertificateIntoTrustStore_createsNewTrustStore(@TempDir Path tempDir) throws Exception {
        Map.Entry<KeyPair, X509Certificate> certEntry =
            Certificates.generateSelfSignedCertificate("localhost", "TestOrg");

        int tlsPort = startTlsServer(certEntry.getKey(), certEntry.getValue());
        Path trustStorePath = tempDir.resolve("truststore.jks");
        String password = "changeit";

        Certificates.importCertificateIntoTrustStore("localhost", tlsPort, trustStorePath, password, "test-alias");

        assertThat(Files.exists(trustStorePath), is(true));

        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream inputStream = Files.newInputStream(trustStorePath)) {
            trustStore.load(inputStream, password.toCharArray());
        }
        assertThat(trustStore.containsAlias("test-alias"), is(true));
        assertThat(trustStore.getCertificate("test-alias"), is(notNullValue()));
    }

    /**
     * Verifies that importing a certificate with an existing alias replaces the previous entry
     */
    @Test
    void importCertificateIntoTrustStore_replacesExistingAlias(@TempDir Path tempDir) throws Exception {
        Map.Entry<KeyPair, X509Certificate> certEntry =
            Certificates.generateSelfSignedCertificate("localhost", "TestOrg");

        Path trustStorePath = tempDir.resolve("truststore.jks");
        String password = "changeit";

        Certificates.importCertificateIntoTrustStore("localhost", startTlsServer(certEntry.getKey(), certEntry.getValue()), trustStorePath, password, "alias");
        Certificates.importCertificateIntoTrustStore("localhost", startTlsServer(certEntry.getKey(), certEntry.getValue()), trustStorePath, password, "alias");

        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream inputStream = Files.newInputStream(trustStorePath)) {
            trustStore.load(inputStream, password.toCharArray());
        }
        assertThat(trustStore.containsAlias("alias"), is(true));
    }

    /**
     * Verifies that importing a certificate into an existing truststore preserves pre-existing aliases
     */
    @Test
    void importCertificateIntoTrustStore_loadsExistingTrustStore(@TempDir Path tempDir) throws Exception {
        Map.Entry<KeyPair, X509Certificate> certEntry =
            Certificates.generateSelfSignedCertificate("localhost", "TestOrg");

        int tlsPort = startTlsServer(certEntry.getKey(), certEntry.getValue());
        Path trustStorePath = tempDir.resolve("truststore.jks");
        String password = "changeit";

        // Pre-create the truststore with a different alias
        KeyStore existingTrustStore = KeyStore.getInstance("JKS");
        existingTrustStore.load(null, password.toCharArray());
        existingTrustStore.setCertificateEntry("pre-existing", certEntry.getValue());
        try (OutputStream outputStream = Files.newOutputStream(trustStorePath)) {
            existingTrustStore.store(outputStream, password.toCharArray());
        }

        Certificates.importCertificateIntoTrustStore("localhost", tlsPort, trustStorePath, password, "new-alias");

        KeyStore trustStore = KeyStore.getInstance("JKS");
        try (InputStream inputStream = Files.newInputStream(trustStorePath)) {
            trustStore.load(inputStream, password.toCharArray());
        }
        // Both aliases should be present
        assertThat(trustStore.containsAlias("pre-existing"), is(true));
        assertThat(trustStore.containsAlias("new-alias"), is(true));
    }

    /**
     * Ensures that an exception is thrown if no certificate could be retrieved from the host
     */
    @Test
    void importCertificateIntoTrustStore_throwsWhenNoCertRetrieved(@TempDir Path tempDir) {
        int unusedPort = findUnusedPort();
        Path trustStorePath = tempDir.resolve("truststore.jks");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () ->
            Certificates.importCertificateIntoTrustStore("127.0.0.1", unusedPort, trustStorePath, "changeit", "alias"));

        assertThat(exception.getMessage(), containsString("No certificate retrieved"));
    }

    // --------------------------------
    // ------------- Utils ------------
    // --------------------------------

    private static int findUnusedPort() {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Starts a minimal TLS server using the provided key/cert.
     * The server is run on a daemon thread so it does not block JVM shutdown.
     *
     * @param keyPair     The server key pair
     * @param certificate The server certificate
     * @return The port number where the TLS server is listening
     */
    private static int startTlsServer(KeyPair keyPair, X509Certificate certificate) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("JKS");
        keyStore.load(null, "password".toCharArray());
        keyStore.setKeyEntry("server", keyPair.getPrivate(), "password".toCharArray(),
            new java.security.cert.Certificate[]{certificate});

        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, "password".toCharArray());

        SSLContext sslContext = SSLContext.getInstance("TLSv1.2");
        sslContext.init(keyManagerFactory.getKeyManagers(), null, new SecureRandom());

        SSLServerSocket serverSocket =
            (SSLServerSocket) sslContext.getServerSocketFactory().createServerSocket(0);

        Thread serverThread = new Thread(() -> {
            try (SSLSocket clientSocket = (SSLSocket) serverSocket.accept()) {
                clientSocket.startHandshake();
            } catch (IOException ignored) {
                // Server socket closed or client disconnected during test
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        return serverSocket.getLocalPort();
    }
}