package com.github.streamshub.console;

import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.security.Audit.Decision;
import com.github.streamshub.console.api.v1alpha1.spec.security.Rule;
import com.github.streamshub.console.api.v1alpha1.status.Condition;
import com.github.streamshub.console.config.security.Privilege;

import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
