package com.github.streamshub.console;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;
import com.github.streamshub.console.api.v1alpha1.spec.security.LdapUserMatcherBuilder;
import com.github.streamshub.console.api.v1alpha1.status.Condition;
import com.github.streamshub.console.dependents.ConsoleDeployment;
import com.github.streamshub.console.dependents.ConsoleIngress;
import com.github.streamshub.console.dependents.ConsoleIngressTlsSecret;
import com.github.streamshub.console.dependents.ConsoleRoute;
import com.github.streamshub.console.dependents.ConsoleService;
import com.github.streamshub.console.dependents.DexDeployment;
import com.github.streamshub.console.dependents.DexRoute;
import com.github.streamshub.console.dependents.DexSecret;
import com.github.streamshub.console.dependents.DexService;
import com.github.streamshub.console.dependents.PrometheusDeployment;
import com.github.streamshub.console.dependents.conditions.ServingCertReadyCondition;
import com.github.streamshub.console.dependents.support.DexConfig;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.Route;
import io.quarkus.test.junit.QuarkusTest;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ConsoleReconcilerDexTest extends ConsoleReconcilerTestBase {

    // Contains '$' to guard against Dex's environment variable expansion altering the value
    private static final String BIND_PW = "a-value-$with-embedded-${HOME}-variables-$$";

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
                            .withNewBindSecret()
                                .withValue(BIND_PW)
                            .endBindSecret()
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
            // Plain Kubernetes: no serving certificate, Dex listens on HTTP
            JSONAssert.assertEquals("""
                    {
                      "issuer": "https://example.com/dex",
                      "storage": {
                        "type": "kubernetes",
                        "config": {
                          "inCluster": true
                        }
                      },
                      "web": {
                        "http": "0.0.0.0:5556",
                        "pathPrefix": "/dex"
                      },
                      "oauth2": {
                        "responseTypes": [ "code" ],
                        "skipApprovalScreen": true,
                        "pkce": {
                          "enforce": true,
                          "codeChallengeMethodsSupported": [ "S256" ]
                        }
                      },
                      "staticClients": [{
                        "id": "streamshub-console",
                        "name": "StreamsHub Console",
                        "public": true,
                        "redirectURIs": [
                          "https://example.com/"
                        ]
                      }],
                      "connectors": [{
                        "id": "ldap",
                        "type": "ldap",
                        "name": "LDAP",
                        "config": {
                          "host": "ldap.example.com:636",
                          "bindDN": "${LDAP_BIND_DN}",
                          "bindPW": "${LDAP_BIND_PW}",
                          "startTLS": true,
                          "userSearch": {
                            "baseDN": "ou=people,dc=example,dc=com",
                            "username": "uid"
                          },
                          "groupSearch": {
                            "baseDN": "ou=groups,dc=example,dc=com",
                            "userMatchers": [{
                              "userAttr": "DN",
                              "groupAttr": "member"
                            }]
                          }
                        }
                      }]
                    }
                    """,
                    dex,
                    JSONCompareMode.STRICT);
        });

        // The bind credentials are kept out of config.yaml (Dex expands '$' in config values)
        // and are passed to Dex as environment variables sourced from the Dex Secret.
        var dexSecret = client.secrets()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + '-' + DexSecret.NAME)
                .get();
        assertEquals("cn=admin,dc=example,dc=com", decode(dexSecret.getData().get(DexConfig.LDAP_BIND_DN_KEY)));
        assertEquals(BIND_PW, decode(dexSecret.getData().get(DexConfig.LDAP_BIND_PW_KEY)));
        assertFalse(decode(dexSecret.getData().get("config.yaml")).contains(BIND_PW));

        var dexDeployment = client.apps().deployments()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + '-' + DexDeployment.NAME)
                .get();
        var dexEnv = dexDeployment.getSpec().getTemplate().getSpec().getContainers().stream()
                .filter(c -> "dex".equals(c.getName()))
                .findFirst()
                .orElseThrow()
                .getEnv();
        assertEquals(2, dexEnv.size());
        assertEnvFromSecret(dexEnv, DexConfig.LDAP_BIND_DN_ENV, dexSecret.getMetadata().getName(), DexConfig.LDAP_BIND_DN_KEY);
        assertEnvFromSecret(dexEnv, DexConfig.LDAP_BIND_PW_ENV, dexSecret.getMetadata().getName(), DexConfig.LDAP_BIND_PW_KEY);

        assertConsoleConfig(consoleConfig -> {
            var oidc = consoleConfig.getSecurity().getOidc();
            assertEquals("http://" + DEX_SERVICE_HOST + ":5556/dex", oidc.getAuthServerUrl());
            assertEquals("streamshub-console", oidc.getClientId());
            assertEquals("openid email profile groups offline_access", oidc.getScopes());
            assertNull(oidc.getClientSecret());
            assertNull(oidc.getTrustStore());
            assertNull(consoleConfig.getTls());
        });
    }

    @Test
    void testDexWithOpenshiftRejectedOnKubernetes() {
        var consoleCR = createConsole(new ConsoleBuilder()
                .withNewSpec()
                    .withHostname("example.com")
                    .withNewSecurity()
                        .withNewOpenshift()
                        .endOpenshift()
                    .endSecurity()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec());

        assertInvalidConfiguration(consoleCR, conditions -> {
            var errorCondition = conditions.get(0);
            assertEquals(Condition.Types.ERROR, errorCondition.getType(), errorCondition::toString);
            assertEquals("True", errorCondition.getStatus(), errorCondition::toString);
            assertEquals(Condition.Reasons.RECONCILIATION_EXCEPTION, errorCondition.getReason(), errorCondition::toString);
            assertEquals("spec.security.openshift is only supported when the cluster is OpenShift",
                    errorCondition.getMessage(), errorCondition::toString);
        });
    }

    @Test
    void testDexWithLdapUsesSpecTlsForIngress() {
        String cert = "-----BEGIN CERTIFICATE-----\nMIIBcert\n-----END CERTIFICATE-----\n";
        String key  = "-----BEGIN PRIVATE KEY-----\nMIIBkey\n-----END PRIVATE KEY-----\n";

        var consoleCR = createConsole(ldapConsole()
                .editSpec()
                    .withNewTls()
                        .withNewCertificate()
                            .withValue(cert)
                        .endCertificate()
                        .withNewKey()
                            .withValue(key)
                        .endKey()
                    .endTls()
                .endSpec());

        awaitDependentsNotReady(consoleCR, "PrometheusDeployment", "ConsoleDeployment", "DexDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        setDeploymentReady(consoleCR, DexDeployment.NAME);
        setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleIngress");

        // With Dex, spec.tls is terminated by the Ingress and the pods are served over HTTP
        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var ingress = client.network().v1().ingresses()
                    .inNamespace(CONSOLE_NS)
                    .withName(CONSOLE_NAME + '-' + ConsoleIngress.NAME)
                    .get();
            assertNotNull(ingress);
            assertEquals("HTTP", ingress.getMetadata().getAnnotations()
                    .get("nginx.ingress.kubernetes.io/backend-protocol"));
            assertEquals(80, ingress.getSpec().getRules().get(0).getHttp()
                    .getPaths().get(0).getBackend().getService().getPort().getNumber());
            assertEquals(1, ingress.getSpec().getTls().size());
            assertEquals(List.of("example.com"), ingress.getSpec().getTls().get(0).getHosts());
            assertEquals(CONSOLE_NAME + '-' + ConsoleIngressTlsSecret.NAME,
                    ingress.getSpec().getTls().get(0).getSecretName());
        });

        var tlsSecret = client.secrets()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + '-' + ConsoleIngressTlsSecret.NAME)
                .get();
        assertNotNull(tlsSecret);
        assertEquals("kubernetes.io/tls", tlsSecret.getType());
        assertEquals(cert, decode(tlsSecret.getData().get("tls.crt")));
        assertEquals(key, decode(tlsSecret.getData().get("tls.key")));

        // The Console pod does not serve TLS itself
        assertConsoleConfig(consoleConfig -> assertNull(consoleConfig.getTls()));
    }

    @Test
    void testDexWithLdapTrustStore() {
        String ca = "-----BEGIN CERTIFICATE-----\nMIIBca\n-----END CERTIFICATE-----\n";

        createConsole(ldapConsole()
                .editSpec()
                    .editSecurity()
                        .editLdap()
                            .withNewTrustStore()
                                .withType(TrustStore.Type.PEM)
                                .withNewContent()
                                    .withValue(ca)
                                .endContent()
                            .endTrustStore()
                        .endLdap()
                    .endSecurity()
                .endSpec());

        assertDexConfig(dex -> {
            var config = YAML.readTree(dex).get("connectors").get(0).get("config");
            assertEquals(DexConfig.CA_PATH, config.get("rootCA").asText());
        });

        var dexSecret = client.secrets()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + '-' + DexSecret.NAME)
                .get();
        assertEquals(ca, decode(dexSecret.getData().get(DexConfig.CA_KEY)));
    }

    @Test
    void testDexWithLdapUnresolvedBindSecret() {
        var consoleCR = createConsole(ldapConsole()
                .editSpec()
                    .editSecurity()
                        .editLdap()
                            .withNewBindSecret()
                                .withNewValueFrom()
                                    .withNewSecretKeyRef("password", "missing-bind-secret", Boolean.TRUE)
                                .endValueFrom()
                            .endBindSecret()
                        .endLdap()
                    .endSecurity()
                .endSpec());

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(CONSOLE_NS)
                    .withName(consoleCR.getMetadata().getName())
                    .get();

            var error = console.getStatus().getConditions().stream()
                    .filter(c -> Condition.Types.ERROR.equals(c.getType()))
                    .findFirst()
                    .orElseThrow();
            assertEquals("True", error.getStatus(), error::toString);
            assertEquals("spec.security.ldap.bindSecret was specified, but did not resolve to a value",
                    error.getMessage(), error::toString);
        });

        // Dex must not be configured with a bindDN and no password
        assertNull(client.secrets()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + '-' + DexSecret.NAME)
                .get());
    }

    @Test
    void testIdentityProvidersMutuallyExclusive() {
        var builder = ldapConsole()
                .editSpec()
                    .editSecurity()
                        .withNewOpenshift()
                        .endOpenshift()
                    .endSecurity()
                .endSpec();

        assertRejected(builder);
    }

    @Test
    void testLdapInsecureNoSSLWithStartTLSRejected() {
        assertRejected(ldapConsole()
                .editSpec().editSecurity().editLdap()
                    .withInsecureNoSSL(Boolean.TRUE)
                    .withStartTLS(Boolean.TRUE)
                .endLdap().endSecurity().endSpec());
    }

    @Test
    void testLdapInsecureSkipVerifyWithTrustStoreRejected() {
        assertRejected(ldapConsole()
                .editSpec().editSecurity().editLdap()
                    .withInsecureSkipVerify(Boolean.TRUE)
                    .withNewTrustStore()
                        .withType(TrustStore.Type.PEM)
                        .withNewContent().withValue("ca").endContent()
                    .endTrustStore()
                .endLdap().endSecurity().endSpec());
    }

    @Test
    void testLdapBindSecretWithoutBindDNRejected() {
        assertRejected(ldapConsole()
                .editSpec().editSecurity().editLdap()
                    .withBindDN(null)
                .endLdap().endSecurity().endSpec());
    }

    private void assertRejected(ConsoleBuilder builder) {
        var thrown = assertThrows(KubernetesClientException.class, () -> createConsole(builder));
        assertEquals(422, thrown.getStatus().getCode());
    }

    private ConsoleBuilder ldapConsole() {
        return new ConsoleBuilder()
                .withNewSpec()
                    .withHostname("example.com")
                    .withNewSecurity()
                        .withNewLdap()
                            .withHost("ldap.example.com:636")
                            .withBindDN("cn=admin,dc=example,dc=com")
                            .withNewBindSecret()
                                .withValue(BIND_PW)
                            .endBindSecret()
                            .withNewUserSearch()
                                .withBaseDN("ou=people,dc=example,dc=com")
                                .withUsername("uid")
                            .endUserSearch()
                        .endLdap()
                    .endSecurity()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec();
    }

    static Stream<Arguments> openShiftUserProvidedCertificates() {
        String cert = "-----BEGIN CERTIFICATE-----\nMIIBcert\n-----END CERTIFICATE-----\n";
        String key  = "-----BEGIN PRIVATE KEY-----\nMIIBkey\n-----END PRIVATE KEY-----\n";

        return Stream.of(
            Arguments.of(cert, key),
            Arguments.of(null, null)
        );
    }

    @ParameterizedTest
    @Tag(REQUIRES_OPENSHIFT_ROUTE)
    @MethodSource("openShiftUserProvidedCertificates")
    void testDexWithOpenshiftOnOpenShift(String edgeCert, String edgeKey) {
        var builder = new ConsoleBuilder()
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
            .endSpec();

        if (edgeCert != null) {
            builder = builder.editSpec()
                    .withNewTls()
                        .withNewCertificate()
                            .withValue(edgeCert)
                        .endCertificate()
                        .withNewKey()
                            .withValue(edgeKey)
                        .endKey()
                    .endTls()
                    .endSpec();
        }

        var consoleCR = createConsole(builder);

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
        assertDexConfig(basicOpenShiftDexConfig());

        // Pods only read the certificates at startup, so digests of them are pod annotations
        var consoleAnnotations = podTemplateAnnotations(ConsoleDeployment.NAME);
        assertDigest(consoleAnnotations.get("streamshub.github.com/tls-digest"));
        assertDigest(consoleAnnotations.get("streamshub.github.com/ca-digest"));

        // Dex does not read the service CA bundle
        var dexAnnotations = podTemplateAnnotations(DexDeployment.NAME);
        assertDigest(dexAnnotations.get("streamshub.github.com/tls-digest"));
        assertNull(dexAnnotations.get("streamshub.github.com/ca-digest"));

        assertConsoleConfig(consoleConfig -> {
            // The Console trusts the service CA used by Dex's serving certificate
            var oidc = consoleConfig.getSecurity().getOidc();
            assertEquals("https://" + DEX_SERVICE_HOST + ":5556/dex", oidc.getAuthServerUrl());
            assertEquals("streamshub-console", oidc.getClientId());
            assertEquals("openid email profile groups offline_access", oidc.getScopes());
            assertEquals("/etc/service-ca/service-ca.crt", oidc.getTrustStore().getContent().getValueFrom());

            // The Console pod serves the OpenShift service CA serving certificate
            assertEquals("/etc/tls/tls.crt", consoleConfig.getTls().getCertificate().getValueFrom());
            assertEquals("/etc/tls/tls.key", consoleConfig.getTls().getKey().getValueFrom());
        });

        // Verify the Service exposes port 443 targeting 8443
        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var service = client.services()
                    .inNamespace(CONSOLE_NS)
                    .withName(CONSOLE_NAME + "-console-service")
                    .get();
            assertNotNull(service);
            var port = service.getSpec().getPorts().get(0);
            assertEquals("https", port.getName());
            assertEquals(443, port.getPort());
            assertEquals(8443, port.getTargetPort().getIntVal());
        });

        assertServingCertAnnotation(ConsoleService.NAME);
        assertServingCertAnnotation(DexService.NAME);

        assertRouteConfiguration(ConsoleRoute.NAME, edgeCert, edgeKey);
        assertRouteConfiguration(DexRoute.NAME, edgeCert, edgeKey);
    }

    private void assertServingCertAnnotation(String serviceName) {
        var name = CONSOLE_NAME + '-' + serviceName;

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var service = client.services().inNamespace(CONSOLE_NS).withName(name).get();
            assertNotNull(service);
            assertEquals(name + "-tls", service.getMetadata()
                    .getAnnotations()
                    .get(ServingCertReadyCondition.ANNOTATION));
        });
    }

    private void assertRouteConfiguration(String routeName, String edgeCert, String edgeKey) {
        var name = CONSOLE_NAME + '-' + routeName;

        // Verify the Route uses reencrypt TLS termination (insecureEdgeTerminationPolicy = Redirect)
        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var route = client.resources(Route.class).inNamespace(CONSOLE_NS).withName(name).get();
            assertNotNull(route);
            assertEquals("reencrypt", route.getSpec().getTls().getTermination());
            assertEquals("Redirect", route.getSpec().getTls().getInsecureEdgeTerminationPolicy());
            assertEquals(edgeCert, route.getSpec().getTls().getCertificate());
            assertEquals(edgeKey, route.getSpec().getTls().getKey());
        });
    }

    private Map<String, String> podTemplateAnnotations(String deploymentName) {
        return client.apps().deployments()
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + '-' + deploymentName)
                .get()
                .getSpec()
                .getTemplate()
                .getMetadata()
                .getAnnotations();
    }

    private static void assertDigest(String digest) {
        assertNotNull(digest);
        assertTrue(digest.matches("[0-9a-f]{40}"), () -> "Not a hex digest: " + digest);
    }

    private static String decode(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    private static void assertEnvFromSecret(List<EnvVar> env, String name, String secretName, String key) {
        var ref = env.stream()
                .filter(e -> name.equals(e.getName()))
                .findFirst()
                .orElseThrow()
                .getValueFrom()
                .getSecretKeyRef();
        assertEquals(secretName, ref.getName());
        assertEquals(key, ref.getKey());
    }

    private void assertDexConfig(ThrowingConsumer<String> assertion) {
        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var dexSecret = client.secrets()
                    .inNamespace(CONSOLE_NS)
                    .withName(CONSOLE_NAME + '-' + DexSecret.NAME)
                    .get();

            assertNotNull(dexSecret);

            byte[] configDecoded = Base64.getDecoder().decode(dexSecret.getData().get("config.yaml"));
            assertion.accept(YAML.readTree(configDecoded).toPrettyString());
        });
    }

    private String issuerUrl() {
        String apiServerUrl = client.getMasterUrl().toString();
        // Strip trailing slash that getMasterUrl() may include
        if (apiServerUrl.endsWith("/")) {
            apiServerUrl = apiServerUrl.substring(0, apiServerUrl.length() - 1);
        }
        return apiServerUrl;
    }

    private ThrowingConsumer<String> basicOpenShiftDexConfig() {
        return dex -> {
            // OpenShift: Dex serves the service CA serving certificate
            JSONAssert.assertEquals("""
                    {
                      "issuer": "https://example.com/dex",
                      "storage": {
                        "type": "kubernetes",
                        "config": {
                          "inCluster": true
                        }
                      },
                      "web": {
                        "https": "0.0.0.0:5556",
                        "tlsCert": "/etc/dex/tls/tls.crt",
                        "tlsKey": "/etc/dex/tls/tls.key",
                        "pathPrefix": "/dex"
                      },
                      "oauth2": {
                        "responseTypes": [ "code" ],
                        "skipApprovalScreen": true,
                        "pkce": {
                          "enforce": true,
                          "codeChallengeMethodsSupported": [ "S256" ]
                        }
                      },
                      "staticClients": [{
                        "id": "streamshub-console",
                        "name": "StreamsHub Console",
                        "public": true,
                        "redirectURIs": [
                          "https://example.com/"
                        ]
                      }],
                      "connectors": [{
                        "id": "openshift",
                        "type": "openshift",
                        "name": "OpenShift",
                        "config": {
                          "issuer": "%s",
                          "clientID": "system:serviceaccount:%s:%s-dex-serviceaccount",
                          "clientSecretFile": "/var/run/secrets/kubernetes.io/serviceaccount/token",
                          "redirectURI": "https://example.com/dex/callback",
                          "rootCA": "/etc/ssl/openshift.pem",
                          "groups": [ "group-1" ]
                        }
                      }]
                    }
                    """.formatted(issuerUrl(), CONSOLE_NS, CONSOLE_NAME),
                    dex,
                    JSONCompareMode.STRICT);
        };
    }
}
