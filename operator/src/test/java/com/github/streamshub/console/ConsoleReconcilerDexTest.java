package com.github.streamshub.console;

import java.util.Base64;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.security.LdapUserMatcherBuilder;
import com.github.streamshub.console.dependents.ConsoleDeployment;
import com.github.streamshub.console.dependents.ConsoleService;
import com.github.streamshub.console.dependents.DexDeployment;
import com.github.streamshub.console.dependents.DexSecret;
import com.github.streamshub.console.dependents.DexService;
import com.github.streamshub.console.dependents.PrometheusDeployment;
import com.github.streamshub.console.dependents.conditions.ServingCertReadyCondition;

import io.quarkus.test.junit.QuarkusTest;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ConsoleReconcilerDexTest extends ConsoleReconcilerTestBase {

    private static final String DEX_SERVICE_HOST = CONSOLE_NAME + '-' + DexService.NAME + '.' + CONSOLE_NS + ".svc.cluster.local";

    @Test
    void testDexWithLdapOnKubernetes() {
        var consoleCR = createConsole(new ConsoleBuilder()
                .withNewSpec()
                    .withHostname("example.com")
                    .withNewSecurity()
                        .withNewLdap()
                            .withHost("ldap.example.com:636")
                            .withBindDN("cn=admin,dc=example,dc=com")
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
                .endSpec());

        // No serving certificates on plain Kubernetes, so only the Deployments and Ingress gate readiness
        awaitDependentsNotReady(consoleCR, "PrometheusDeployment", "ConsoleDeployment", "DexDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        setDeploymentReady(consoleCR, DexDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");

        setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleIngress");
        setConsoleIngressReady(consoleCR);

        awaitReady(consoleCR);

        assertDexConfig(dex -> {
            assertEquals("https://example.com/dex", dex.get("issuer").asText());

            // Plain Kubernetes: no serving certificate, Dex listens on HTTP
            var web = dex.get("web");
            assertEquals("0.0.0.0:5556", web.get("http").asText());
            assertEquals("/dex", web.get("pathPrefix").asText());
            assertFalse(web.has("https"));
            assertFalse(web.has("tlsCert"));
            assertFalse(web.has("tlsKey"));

            var client = dex.get("staticClients").get(0);
            assertEquals("streamshub-console", client.get("id").asText());
            assertTrue(client.get("public").asBoolean());
            assertFalse(client.has("isPublic"));
            assertEquals("https://example.com/", client.get("redirectURIs").get(0).asText());

            var connector = dex.get("connectors").get(0);
            assertEquals("ldap", connector.get("type").asText());
            var ldap = connector.get("config");
            assertEquals("ldap.example.com:636", ldap.get("host").asText());
            assertEquals("cn=admin,dc=example,dc=com", ldap.get("bindDN").asText());
            assertTrue(ldap.get("startTLS").asBoolean());
            assertFalse(ldap.has("insecureNoSSL"));
            assertFalse(ldap.has("insecureSkipVerify"));
            assertEquals("ou=people,dc=example,dc=com", ldap.get("userSearch").get("baseDN").asText());
            assertEquals("uid", ldap.get("userSearch").get("username").asText());
            assertEquals("ou=groups,dc=example,dc=com", ldap.get("groupSearch").get("baseDN").asText());
            var matcher = ldap.get("groupSearch").get("userMatchers").get(0);
            assertEquals("DN", matcher.get("userAttr").asText());
            assertEquals("member", matcher.get("groupAttr").asText());
        });

        assertConsoleConfig(consoleConfig -> {
            var oidc = consoleConfig.getSecurity().getOidc();
            assertEquals("http://" + DEX_SERVICE_HOST + ":5556/dex", oidc.getAuthServerUrl());
            assertEquals("streamshub-console", oidc.getClientId());
            assertNull(oidc.getClientSecret());
            assertNull(oidc.getTrustStore());
            assertNull(consoleConfig.getTls());
        });
    }

    @Test
    @Tag(REQUIRES_OPENSHIFT_ROUTE)
    void testDexWithOpenshiftOnOpenShift() {
        var consoleCR = createConsole(new ConsoleBuilder()
                .withNewSpec()
                    .withHostname("example.com")
                    .withNewSecurity()
                        .withNewOpenshift()
                            .withGroups("group-1")
                        .endOpenshift()
                    .endSecurity()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec());

        // The CA bundle must be injected before the Route and anything depending on it proceeds
        awaitDependentsNotReady(consoleCR, "ConsoleTrustBundle");
        setConsoleTrustBundleContent(consoleCR);

        // OpenShift populates the serving certificates for both Services
        awaitDependentsNotReady(consoleCR, "ConsoleService", "DexService");
        createServingCertContent(consoleCR, ConsoleService.NAME);
        createServingCertContent(consoleCR, DexService.NAME);

        awaitDependentsNotReady(consoleCR, "ConsoleRoute");
        setConsoleRouteReady(consoleCR);

        awaitDependentsNotReady(consoleCR, "PrometheusDeployment", "ConsoleDeployment", "DexDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        setDeploymentReady(consoleCR, DexDeployment.NAME);

        awaitReady(consoleCR);

        assertDexConfig(dex -> {
            assertEquals("https://example.com/dex", dex.get("issuer").asText());

            // OpenShift: Dex serves the service CA serving certificate
            var web = dex.get("web");
            assertEquals("0.0.0.0:5556", web.get("https").asText());
            assertEquals("/etc/dex/tls/tls.crt", web.get("tlsCert").asText());
            assertEquals("/etc/dex/tls/tls.key", web.get("tlsKey").asText());
            assertEquals("/dex", web.get("pathPrefix").asText());
            assertFalse(web.has("http"));

            var connector = dex.get("connectors").get(0);
            assertEquals("openshift", connector.get("type").asText());
            var openshift = connector.get("config");
            assertTrue(openshift.get("issuer").asText().startsWith("https://"));
            assertEquals(
                    "system:serviceaccount:" + CONSOLE_NS + ':' + CONSOLE_NAME + "-dex-serviceaccount",
                    openshift.get("clientID").asText());
            assertEquals("/var/run/secrets/kubernetes.io/serviceaccount/token",
                    openshift.get("clientSecretFile").asText());
            assertEquals("https://example.com/dex/callback", openshift.get("redirectURI").asText());
            assertEquals("/etc/ssl/openshift.pem", openshift.get("rootCA").asText());
            assertEquals("group-1", openshift.get("groups").get(0).asText());
            assertFalse(openshift.has("insecureCA"));
        });

        assertConsoleConfig(consoleConfig -> {
            // The Console trusts the service CA used by Dex's serving certificate
            var oidc = consoleConfig.getSecurity().getOidc();
            assertEquals("https://" + DEX_SERVICE_HOST + ":5556/dex", oidc.getAuthServerUrl());
            assertEquals("streamshub-console", oidc.getClientId());
            assertEquals("/etc/service-ca/service-ca.crt", oidc.getTrustStore().getContent().getValueFrom());

            // The Console pod serves the OpenShift service CA serving certificate
            assertEquals("/etc/tls/tls.crt", consoleConfig.getTls().getCertificate().getValueFrom());
            assertEquals("/etc/tls/tls.key", consoleConfig.getTls().getKey().getValueFrom());
        });

        assertServingCertAnnotation(ConsoleService.NAME);
        assertServingCertAnnotation(DexService.NAME);
    }

    private void assertServingCertAnnotation(String serviceName) {
        var name = CONSOLE_NAME + '-' + serviceName;

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var service = client.services().inNamespace(CONSOLE_NS).withName(name).get();
            assertNotNull(service);
            assertEquals(name + "-tls", service.getMetadata().getAnnotations().get(ServingCertReadyCondition.ANNOTATION));
        });
    }

    private void assertDexConfig(Consumer<JsonNode> assertion) {
        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var dexSecret = client.secrets()
                    .inNamespace(CONSOLE_NS)
                    .withName(CONSOLE_NAME + '-' + DexSecret.NAME)
                    .get();

            assertNotNull(dexSecret);

            byte[] configDecoded = Base64.getDecoder().decode(dexSecret.getData().get("config.yaml"));
            assertion.accept(YAML.readTree(configDecoded));
        });
    }
}
