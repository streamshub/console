package com.github.streamshub.console.kafka.systemtest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.streamshub.console.test.TlsHelper;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Minimal test profile that starts Quarkus with a TLS-enabled console configuration.
 * <p>
 * No Kubernetes, Kafka, Keycloak, or Strimzi resources are required — this profile only
 * exercises the HTTP server TLS binding.  The generated certificate is available statically
 * via {@link #TLS} so that tests can configure RestAssured with the correct trust store.
 */
public class TestTlsProfile implements QuarkusTestProfile {

    /** TLS material generated once per JVM for this profile. */
    public static final TlsHelper TLS = TlsHelper.newInstance("localhost");

    private static final Path CONFIG_FILE = writeTempConfig();

    @Override
    public String getConfigProfile() {
        // Re-use the "testplain" profile so dev-services and Keycloak are still disabled,
        // but override specific properties below.
        return "testplain";
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "console.config-path", CONFIG_FILE.toAbsolutePath().toString(),
                // Kubernetes dev-services not needed for a pure TLS binding test
                "quarkus.kubernetes-client.devservices.enabled", "false");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Path writeTempConfig() {
        try {
            // Escape PEM newlines for inline YAML block scalar
            String certPem = TLS.getServerCertificatePem();
            String keyPem  = TLS.getServerPrivateKeyPem();

            // Use YAML literal block scalars (|) so multi-line PEM content is preserved exactly
            String yaml = """
                    kubernetes:
                      enabled: false
                    tls:
                      certificate:
                        value: |
                    %s
                      key:
                        value: |
                    %s
                    """.formatted(indent(certPem, 8), indent(keyPem, 8));

            Path file = Files.createTempFile("console-config-tls-", ".yaml");
            file.toFile().deleteOnExit();
            Files.writeString(file, yaml);
            return file;
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to write temporary TLS console config", e);
        }
    }

    /** Indents every line of {@code text} by {@code spaces} spaces. */
    private static String indent(String text, int spaces) {
        String prefix = " ".repeat(spaces);
        return text.lines()
                .map(line -> prefix + line)
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
    }
}
