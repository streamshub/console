package com.github.streamshub.console;

import java.util.Base64;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.security.LdapUserMatcherBuilder;
import com.github.streamshub.console.dependents.ConsoleDeployment;
import com.github.streamshub.console.dependents.ConsoleRoute;
import com.github.streamshub.console.dependents.ConsoleService;
import com.github.streamshub.console.dependents.DexDeployment;
import com.github.streamshub.console.dependents.DexRoute;
import com.github.streamshub.console.dependents.DexSecret;
import com.github.streamshub.console.dependents.DexService;
import com.github.streamshub.console.dependents.PrometheusDeployment;
import com.github.streamshub.console.dependents.conditions.ServingCertReadyCondition;

import io.fabric8.openshift.api.model.Route;
import io.quarkus.test.junit.QuarkusTest;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
                        "skipApprovalScreen": true
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
                          "bindDN": "cn=admin,dc=example,dc=com",
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

        assertConsoleConfig(consoleConfig -> {
            var oidc = consoleConfig.getSecurity().getOidc();
            assertEquals("http://" + DEX_SERVICE_HOST + ":5556/dex", oidc.getAuthServerUrl());
            assertEquals("streamshub-console", oidc.getClientId());
            assertNull(oidc.getClientSecret());
            assertNull(oidc.getTrustStore());
            assertNull(consoleConfig.getTls());
        });
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
                        "skipApprovalScreen": true
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
