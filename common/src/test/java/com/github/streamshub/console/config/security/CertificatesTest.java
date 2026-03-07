package com.github.streamshub.console.config.security;

import com.github.streamshub.console.utils.Certificates;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CertificatesTest {

    // ---------------------------------
    // --- Generate self signed cert ---
    // ---------------------------------

    /**
     * Generates a self-signed certificate and verifies:
     * - KeyPair is non-null and uses RSA
     * - Certificate is non-null, has correct subject details, and positive serial number
     * - Certificate is self-signed (verifiable with its own public key)
     */
    @Test
    void generateSelfSignedCertificate_returnsValidKeyPairAndCert() throws Exception {
        Map.Entry<KeyPair, X509Certificate> certEntry =
            Certificates.generateSelfSignedCertificate("localhost", "TestOrg");

        KeyPair keyPair = certEntry.getKey();
        X509Certificate certificate = certEntry.getValue();

        assertThat(keyPair, is(notNullValue()));
        assertThat(keyPair.getPrivate(), is(notNullValue()));
        assertThat(keyPair.getPublic(), is(notNullValue()));
        assertThat(keyPair.getPrivate().getAlgorithm(), equalTo("RSA"));

        assertThat(certificate, is(notNullValue()));
        assertThat(certificate.getSubjectX500Principal().getName(), containsString("CN=localhost"));
        assertThat(certificate.getSubjectX500Principal().getName(), containsString("O=TestOrg"));
        assertThat(certificate.getSerialNumber(), greaterThan(BigInteger.ZERO));

        // Verify the cert is self-signed: signed by its own key
        assertDoesNotThrow(() -> certificate.verify(keyPair.getPublic()));
    }

    /**
     * Ensures that generating multiple self-signed certificates produces unique serial numbers
     */
    @Test
    void generateSelfSignedCertificate_differentCallsProduceDifferentSerials() throws Exception {
        Map.Entry<KeyPair, X509Certificate> firstEntry =
            Certificates.generateSelfSignedCertificate("host1", "Org1");
        Map.Entry<KeyPair, X509Certificate> secondEntry =
            Certificates.generateSelfSignedCertificate("host2", "Org2");

        assertThat(firstEntry.getValue().getSerialNumber(), not(equalTo(secondEntry.getValue().getSerialNumber())));
    }

    /**
     * Converts a KeyPair and X509Certificate to PEM strings and verifies
     * the expected PEM headers are present for key and certificate
     */
    @Test
    void toPemStrings_containsExpectedPemHeaders() throws Exception {
        Map.Entry<KeyPair, X509Certificate> certEntry =
            Certificates.generateSelfSignedCertificate("localhost", "TestOrg");

        Map<String, String> pemStrings = Certificates.toPemStrings(certEntry.getKey(), certEntry.getValue());

        assertThat(pemStrings, hasKey("tls.key"));
        assertThat(pemStrings, hasKey("tls.crt"));
        assertThat(pemStrings.get("tls.key"), containsString("-----BEGIN PRIVATE KEY-----"));
        assertThat(pemStrings.get("tls.key"), containsString("-----END PRIVATE KEY-----"));
        assertThat(pemStrings.get("tls.crt"), containsString("-----BEGIN CERTIFICATE-----"));
        assertThat(pemStrings.get("tls.crt"), containsString("-----END CERTIFICATE-----"));
    }

    /**
     * Verifies that the private key returned in PEM format is properly Base64-encoded
     */
    @Test
    void toPemStrings_privateKeyIsBase64Encoded() throws Exception {
        Map.Entry<KeyPair, X509Certificate> certEntry =
            Certificates.generateSelfSignedCertificate("localhost", "TestOrg");

        Map<String, String> pemStrings = Certificates.toPemStrings(certEntry.getKey(), certEntry.getValue());
        String privateKeyBody = pemStrings.get("tls.key")
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");

        assertDoesNotThrow(() -> java.util.Base64.getDecoder().decode(privateKeyBody));
    }
}