package com.github.streamshub.console.kafka.systemtest;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.streamshub.console.kafka.systemtest.deployment.ApicurioResourceManager;
import com.github.streamshub.console.kafka.systemtest.deployment.KafkaResourceManager;
import com.github.streamshub.console.kafka.systemtest.deployment.KeycloakResourceManager;
import com.github.streamshub.console.kafka.systemtest.deployment.ResourceManagerBase;
import com.github.streamshub.console.kafka.systemtest.deployment.StrimziCrdResourceManager;

import io.quarkus.test.junit.QuarkusTestProfile;

public class TestPlainProfile implements QuarkusTestProfile {

    static final String PROFILE = "testplain";
    public static final int MAX_PARTITIONS = 100;
    public static final int EXCESSIVE_PARTITIONS = 101;

    static {
        /*
         * Requires JDK 11.0.4+. If the `Host` header is not set, Keycloak will
         * generate tokens with an issuer URI containing localhost:<random port>.
         */
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host");
    }

    private final List<TestResourceEntry> testResources;

    public TestPlainProfile() {
        testResources = List.of(
                new TestResourceEntry(ApicurioResourceManager.class, Map.ofEntries(
                        Map.entry(ResourceManagerBase.DEPENDENCIES, KeycloakResourceManager.class.getName())
                    ), true),
                new TestResourceEntry(StrimziCrdResourceManager.class, Collections.emptyMap(), true),
                new TestResourceEntry(KeycloakResourceManager.class, Collections.emptyMap(), true),
                new TestResourceEntry(KafkaResourceManager.class, Collections.emptyMap(), true)
            );
    }

    @Override
    public String getConfigProfile() {
        return PROFILE;
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return testResources;
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        var configFile = writeConfiguration("""
                kubernetes:
                  enabled: true

                metricsSources:
                 - name: test-openshift-monitoring
                   type: openshift-monitoring
                   url: http://prometheus.example.com
                 - name: test-unauthenticated
                   type: standalone
                   url: http://prometheus.example.com
                 - name: test-basic
                   type: standalone
                   url: http://prometheus.example.com
                   authentication:
                     basic:
                       username: pr0m3th3u5
                       password:
                         value: password42
                 - name: test-bearer-token
                   type: standalone
                   url: http://prometheus.example.com
                   authentication:
                     bearer:
                       token:
                         value: my-bearer-token
                 - name: test-oidc
                   type: standalone
                   url: http://prometheus.example.com
                   authentication:
                     oidc:
                       authServerUrl: ${console.test.oidc-url}
                       clientId: registry-api
                       clientSecret:
                         value: registry-api-secret
                       method: BASIC
                       grantType: CLIENT
                       trustStore:
                         type: ${console.test.oidc-trust-store.type}
                         content:
                           valueFrom: ${console.test.oidc-trust-store.path}
                         password:
                           value: ${console.test.oidc-trust-store.password}

                schemaRegistries:
                  - name: test-registry
                    url: ${console.test.apicurio-url}
                    authentication:
                      oidc:
                        authServerUrl: ${console.test.oidc-url}
                        clientId: registry-api
                        clientSecret:
                          value: registry-api-secret
                        method: POST
                        grantType: CLIENT
                        grantOptions:
                          audience: ${console.test.apicurio-url}
                        trustStore:
                          type: ${console.test.oidc-trust-store.type}
                          content:
                            valueFrom: ${console.test.oidc-trust-store.path}
                          password:
                            value: ${console.test.oidc-trust-store.password}
                    trustStore:
                      type: ${console.test.apicurio-trust-store.type}
                      content:
                        valueFrom: ${console.test.apicurio-trust-store.path}
                      password:
                        valueFrom: ${console.test.apicurio-trust-store.password-path}

                kafka:
                  clusters:
                    - name: test-kafka1
                      namespace: default
                      id: k1-id
                      schemaRegistry: test-registry
                      properties:
                        bootstrap.servers: ${console.test.external-bootstrap}

                    - name: test-kafka2
                      namespace: default
                      id: k2-id
                      properties:
                        bootstrap.servers: ${console.test.random-bootstrap}

                    - name: test-kafka3
                      namespace: default
                      # listener is named and bootstrap.servers not set (will be retrieved from Kafka CR)
                      listener: listener0

                    # missing required bootstrap.servers and sasl.mechanism
                    - name: test-kafkaX
                      properties:
                        sasl.jaas.config: something

                    - name: test-kafkaY
                      id: test-kafkaY
                      properties:
                        bootstrap.servers: ${console.test.external-bootstrap}

                # Like metricsSources, REST calls to the connect clusters is mocked in the test cases
                kafkaConnectClusters:
                  - name: test-connect1
                    namespace: default
                    url: http://test-connect1.example.com
                    mirrorMaker: false
                    kafkaClusters:
                      - default/test-kafka1

                  - name: test-connect2
                    namespace: default
                    url: http://test-connect2.example.com
                    mirrorMaker: true
                    kafkaClusters:
                      - default/test-kafka2

                  - name: test-connect3
                    url: http://test-connect3.example.com
                    mirrorMaker: false
                    kafkaClusters:
                      - test-kafkaY

                """);

        return Map.of("console.config-path", configFile.getAbsolutePath());
    }

    protected File writeConfiguration(String configurationYaml) {
        File configFile;

        try {
            configFile = File.createTempFile("console-test-config-", ".yaml");
            configFile.deleteOnExit();

            Files.writeString(configFile.toPath(), configurationYaml, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return configFile;
    }
}
