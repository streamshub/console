package com.github.streamshub.console;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;
import com.github.streamshub.console.api.v1alpha1.spec.security.Audit.Decision;
import com.github.streamshub.console.api.v1alpha1.spec.security.Rule;
import com.github.streamshub.console.api.v1alpha1.status.Condition;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.dependents.ConsoleDeployment;
import com.github.streamshub.console.dependents.ConsoleResource;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ConsoleReconcilerSecurityTest extends ConsoleReconcilerTestBase {

    @Test
    void testConsoleReconciliationWithSecurity() {
        createConsole(new ConsoleBuilder()
                .withNewSpec()
                    .withHostname("example.com")
                    .withNewSecurity()
                        .withNewOidc()
                            .withAuthServerUrl("https://example.com/.well-known/openid-connect")
                            .withIssuer("https://example.com")
                            .withClientId("client-id")
                            .withClientSecret("client-secret")
                        .endOidc()
                        .addNewSubject()
                            .addToInclude("user-1")
                            .addToRoleNames("role-1")
                        .endSubject()
                        .addNewRole()
                            .withName("role-1")
                            .addNewRule()
                                .addToResources("kafkas")
                                .addToPrivileges(Rule.Privilege.ALL)
                            .endRule()
                        .endRole()
                    .endSecurity()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                        .withNewSecurity()
                            .addNewRole()
                                .withName("role-1")
                                .addNewRule()
                                    .addToResources("topics", "consumerGroups")
                                    .addToPrivileges(Rule.Privilege.ALL)
                                .endRule()
                            .endRole()
                        .endSecurity()
                    .endKafkaCluster()
                .endSpec());

        assertConsoleConfig(consoleConfig -> {
            var securityConfig = consoleConfig.getSecurity();

            var oidc = securityConfig.getOidc();
            assertEquals("https://example.com/.well-known/openid-connect", oidc.getAuthServerUrl());
            assertEquals("https://example.com", oidc.getIssuer());
            assertEquals("client-id", oidc.getClientId());
            assertEquals("client-secret", oidc.getClientSecret());

            var subjects = securityConfig.getSubjects();
            assertEquals(1, subjects.size());
            assertEquals(List.of("user-1"), subjects.get(0).getInclude());
            assertEquals(List.of("role-1"), subjects.get(0).getRoleNames());

            var roles = securityConfig.getRoles();
            assertEquals(1, roles.size());
            assertEquals("role-1", roles.get(0).getName());

            var rules = roles.get(0).getRules();
            assertEquals(1, rules.size());
            assertEquals(List.of("kafkas"), rules.get(0).getResources());
            assertEquals(List.of(Privilege.ALL), rules.get(0).getPrivileges());

            var kafkaSecurity = consoleConfig.getKafka().getClusters().get(0).getSecurity();
            var kafkaSubjects = kafkaSecurity.getSubjects();
            assertEquals(0, kafkaSubjects.size());

            var kafkaRoles = kafkaSecurity.getRoles();
            assertEquals(1, kafkaRoles.size());
            assertEquals("role-1", kafkaRoles.get(0).getName());

            var kafkaRules = kafkaRoles.get(0).getRules();
            assertEquals(1, kafkaRules.size());
            assertEquals(List.of("topics", "consumerGroups"), kafkaRules.get(0).getResources());
            assertEquals(List.of(Privilege.ALL), kafkaRules.get(0).getPrivileges());
        });
    }

    @Test
    void testConsoleReconciliationWithKafkaSecurityAudit() {
        createConsole(new ConsoleBuilder()
                .withNewSpec()
                    .withHostname("example.com")
                    .withNewSecurity()
                        .addNewRole()
                            .withName("role-1")
                            .addNewRule()
                                .addToResources("kafkas")
                                .addToPrivileges(Rule.Privilege.ALL)
                            .endRule()
                        .endRole()
                    .endSecurity()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                        .withNewSecurity()
                            .addNewSubject()
                                .addToInclude("kafka-user-1")
                                .addToRoleNames("role-1")
                            .endSubject()
                            .addNewRole()
                                .withName("role-1")
                                .addNewRule()
                                    .addToResources("topics", "consumerGroups")
                                    .addToPrivileges(Rule.Privilege.ALL)
                                .endRule()
                            .endRole()
                            .addNewAudit()
                                .withDecision(Decision.ALLOWED)
                                .withResources("topics")
                                .withResourceNames("top-secret")
                                .withPrivileges(Rule.Privilege.GET)
                            .endAudit()
                        .endSecurity()
                    .endKafkaCluster()
                .endSpec());

        assertConsoleConfig(consoleConfig -> {
            var securityConfig = consoleConfig.getSecurity();

            var roles = securityConfig.getRoles();
            assertEquals(1, roles.size());
            assertEquals("role-1", roles.get(0).getName());

            var rules = roles.get(0).getRules();
            assertEquals(1, rules.size());
            assertEquals(List.of("kafkas"), rules.get(0).getResources());
            assertEquals(List.of(Privilege.ALL), rules.get(0).getPrivileges());

            var kafkaSecurity = consoleConfig.getKafka().getClusters().get(0).getSecurity();
            var kafkaSubjects = kafkaSecurity.getSubjects();
            assertEquals(1, kafkaSubjects.size());
            assertEquals(List.of("kafka-user-1"), kafkaSubjects.get(0).getInclude());
            assertEquals(List.of("role-1"), kafkaSubjects.get(0).getRoleNames());

            var kafkaRoles = kafkaSecurity.getRoles();
            assertEquals(1, kafkaRoles.size());
            assertEquals("role-1", kafkaRoles.get(0).getName());

            var kafkaRules = kafkaRoles.get(0).getRules();
            assertEquals(1, kafkaRules.size());
            assertEquals(List.of("topics", "consumerGroups"), kafkaRules.get(0).getResources());
            assertEquals(List.of(Privilege.ALL), kafkaRules.get(0).getPrivileges());

            var kafkaAudit = kafkaSecurity.getAudit();
            assertEquals(1, kafkaAudit.size());
            assertEquals(List.of("topics"), kafkaAudit.get(0).getResources());
            assertEquals(List.of("top-secret"), kafkaAudit.get(0).getResourceNames());
            assertEquals(List.of(Privilege.GET), kafkaAudit.get(0).getPrivileges());
        });
    }

    @Test
    void testConsoleReconciliationWithMissingRules() {
        var consoleCR = createConsole(new ConsoleBuilder()
                .withNewSpec()
                    .withHostname("example.com")
                    .withNewSecurity()
                        .addNewSubject()
                            .addToInclude("user-1")
                            .addToRoleNames("role-1")
                        .endSubject()
                        .addNewRole()
                            .withName("role-1")
                        .endRole()
                    .endSecurity()
                .endSpec());

        assertInvalidConfiguration(consoleCR, errorConditions -> {
            assertEquals(1, errorConditions.size());
            var errorCondition = errorConditions.get(0);
            Supplier<String> errorString = errorCondition::toString;

            assertEquals(Condition.Types.ERROR, errorCondition.getType(), errorString);
            assertEquals("True", errorCondition.getStatus(), errorString);
            assertEquals(Condition.Reasons.INVALID_CONFIGURATION, errorCondition.getReason(), errorString);
            assertEquals("security.roles[0].rules must not be empty", errorCondition.getMessage(), errorString);
        });
    }

    @Test
    void testConsoleReconciliationWithOidcTrustStore() throws Exception {
        Secret passwordSecret = new SecretBuilder()
                .withNewMetadata()
                    .withName("my-secret")
                    .withNamespace("ns2")
                    .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
                .endMetadata()
                .addToData("pass", Base64.getEncoder().encodeToString("changeit".getBytes()))
                .build();

        client.resource(passwordSecret).create();

        try (InputStream in = getClass().getResourceAsStream("kube-certs.jks")) {
            byte[] truststore = in.readAllBytes();

            ConfigMap contentConfigMap = new ConfigMapBuilder()
                    .withNewMetadata()
                        .withName("my-configmap")
                        .withNamespace(CONSOLE_NS)
                        .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
                    .endMetadata()
                    .addToBinaryData("truststore", Base64.getEncoder().encodeToString(truststore))
                    .build();
            client.resource(contentConfigMap).create();
        }

        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .withNewSecurity()
                        .withNewOidc()
                            .withAuthServerUrl("https://example.com/.well-known/openid-connect")
                            .withIssuer("https://example.com")
                            .withClientId("client-id")
                            .withClientSecret("client-secret")
                            .withNewTrustStore()
                                .withType(TrustStore.Type.JKS)
                                .withNewPassword()
                                    .withNewValueFrom()
                                        .withNewSecretKeyRef("pass", "my-secret", Boolean.FALSE)
                                    .endValueFrom()
                                .endPassword()
                                .withNewContent()
                                    .withNewValueFrom()
                                        .withNewConfigMapKeyRef("truststore", "my-configmap", Boolean.FALSE)
                                    .endValueFrom()
                                .endContent()
                            .endTrustStore()
                        .endOidc()
                    .endSecurity()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "ConsoleIngress");
        setConsoleIngressReady(consoleCR);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        var consoleDeployment = setDeploymentReady(consoleCR, ConsoleDeployment.NAME);

        var podSpec = consoleDeployment.getSpec().getTemplate().getSpec();
        var containerSpecAPI = podSpec.getContainers().get(0);

        var volumes = podSpec.getVolumes().stream().collect(Collectors.toMap(Volume::getName, Function.identity()));
        assertEquals(3, volumes.size()); // cache, config + 1 volume for truststore

        var truststoreVolName = "oidc-truststore-trust";

        var truststoreVolume = volumes.get(truststoreVolName);
        assertEquals("oidc-truststore.trust.content", truststoreVolume.getSecret().getItems().get(0).getKey());
        assertEquals("oidc-truststore.trust.jks", truststoreVolume.getSecret().getItems().get(0).getPath());

        var mounts = containerSpecAPI.getVolumeMounts().stream().collect(Collectors.toMap(VolumeMount::getName, Function.identity()));
        assertEquals(3, mounts.size(), mounts::toString);

        var truststoreMount = mounts.get(truststoreVolName);
        var truststoreMountPath = "/etc/ssl/oidc-truststore.trust.jks";
        assertEquals(truststoreMountPath, truststoreMount.getMountPath());
        assertEquals("oidc-truststore.trust.jks", truststoreMount.getSubPath());

        var envVarsAPI = containerSpecAPI.getEnv().stream().collect(Collectors.toMap(EnvVar::getName, Function.identity()));

        var truststorePath = envVarsAPI.get("QUARKUS_TLS__OIDC_PROVIDER_TRUST__TRUST_STORE_JKS_PATH");
        assertEquals(truststoreMountPath, truststorePath.getValue());
        var truststorePasswordSource = envVarsAPI.get("QUARKUS_TLS__OIDC_PROVIDER_TRUST__TRUST_STORE_JKS_PASSWORD");
        assertEquals("console-1-console-secret", truststorePasswordSource.getValueFrom().getSecretKeyRef().getName());
        assertEquals("oidc-truststore.trust.password", truststorePasswordSource.getValueFrom().getSecretKeyRef().getKey());

        var containerSpecUI = podSpec.getContainers().get(1);
        var envVarsUI = containerSpecUI.getEnv().stream().collect(Collectors.toMap(EnvVar::getName, Function.identity()));
        var truststorePemRef = envVarsUI.get("CONSOLE_SECURITY_OIDC_TRUSTSTORE").getValueFrom().getSecretKeyRef();
        var truststorePemSecret = client.resources(Secret.class)
                .inNamespace(CONSOLE_NS)
                .withName(truststorePemRef.getName())
                .get();
        var truststorePemValue = Base64.getDecoder().decode(truststorePemSecret.getData().get(truststorePemRef.getKey()));

        CertificateFactory fact = CertificateFactory.getInstance("X.509");
        Collection<? extends Certificate> expectedCertificates;
        Collection<? extends Certificate> actualCertificates;

        try (InputStream in = getClass().getResourceAsStream("kube-certs.pem")) {
            expectedCertificates = fact.generateCertificates(in);
        }

        try (InputStream in = new ByteArrayInputStream(truststorePemValue)) {
            actualCertificates = fact.generateCertificates(in);
        }

        assertEquals(expectedCertificates.size(), actualCertificates.size());

        for (Certificate exp : expectedCertificates) {
            assertTrue(actualCertificates.contains(exp));
        }
    }
}
