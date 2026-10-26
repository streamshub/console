package com.github.streamshub.systemtests.security;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.kafka.common.config.SaslConfigs;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.status.Condition;
import com.github.streamshub.console.dependents.ConsoleClusterRole;
import com.github.streamshub.console.dependents.ConsoleClusterRoleBinding;
import com.github.streamshub.console.dependents.ConsoleSecret;
import com.github.streamshub.console.dependents.ConsoleServiceAccount;
import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.console.ConsoleUtils;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.PolicyRule;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import io.skodjob.kubetest4j.KubeTestConstants;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import io.skodjob.kubetest4j.wait.Wait;

import static java.util.Locale.ROOT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


@Tag(TestTags.REGRESSION)
class SecurityST extends AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(SecurityST.class);
    private static TestCaseConfig tcc;
    private static final String CROSS_NAMESPACE = "cross-st-ns";
    private static final String INVALID_IMAGE = "bad-registry.io/invalid-image:latest";

    @Test
    void testConsoleReconciliationRejectsUnallowedImage() {
        String consoleName = "security-unallowed-image";

        Console consoleCR = new ConsoleBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(consoleName)
                .withNamespace(tcc.namespace())
                .build())
            .withNewSpec()
                .withHostname(ConsoleUtils.getConsoleUiHostname(consoleName))
                .withNewDeployment()
                    .withNewSpec()
                        .withNewTemplate()
                            .withNewSpec()
                                .withNewServerContainer()
                                    .withImage(INVALID_IMAGE)
                                .endServerContainer()
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                .endDeployment()
                .addNewKafkaCluster()
                    .withName(tcc.kafkaName())
                    .withNamespace(tcc.namespace())
                    .withListener(Constants.SECURE_LISTENER_NAME)
                .endKafkaCluster()
            .endSpec()
            .build();

        LOGGER.info("Creating Console {}/{} with disallowed image '{}', expecting reconciliation to reject it",
            tcc.namespace(), consoleName, INVALID_IMAGE);
        KubeResourceManager.get().createResourceWithoutWait(consoleCR);

        Wait.until(String.format("Console %s/%s to report an Error condition for the disallowed image", tcc.namespace(), consoleName),
            KubeTestConstants.GLOBAL_POLL_INTERVAL_SHORT, KubeTestConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> {
                Console console = ResourceUtils.getKubeResource(Console.class, tcc.namespace(), consoleName);
                return console != null
                    && console.getStatus() != null
                    && console.getStatus().getConditions() != null
                    && console.getStatus().getConditions().stream().anyMatch(c -> Condition.Types.ERROR.equals(c.getType()));
            });

        Console console = ResourceUtils.getKubeResource(Console.class, tcc.namespace(), consoleName);
        Set<Condition> conditions = console.getStatus().getConditions();

        Condition ready = conditions.stream()
            .filter(c -> Condition.Types.READY.equals(c.getType()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Console " + consoleName + " has no Ready condition: " + conditions));
        assertEquals("False", ready.getStatus(), "Console with a disallowed image must not report Ready=True: " + ready);

        Condition error = conditions.stream()
            .filter(c -> Condition.Types.ERROR.equals(c.getType()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Console " + consoleName + " has no Error condition: " + conditions));

        assertEquals(Condition.Reasons.RECONCILIATION_EXCEPTION, error.getReason(), error.toString());
        assertTrue(error.getMessage().contains(INVALID_IMAGE),
            "Error message should name the rejected image, was: " + error.getMessage());

        String operatorImage = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, Constants.CO_NAMESPACE, Environment.CONSOLE_DEPLOYMENT_NAME)
            .getFirst()
            .getSpec()
            .getTemplate()
            .getSpec()
            .getContainers()
            .getFirst()
            .getImage();

        String allowedImagePrefix = operatorImage.substring(0, operatorImage.lastIndexOf('/') + 1);

        assertTrue(error.getMessage().contains(allowedImagePrefix),
            "Error message should name the allowed image prefix, was: " + error.getMessage());

        Deployment deployment = ResourceUtils.getKubeResource(Deployment.class, tcc.namespace(), ConsoleUtils.getConsoleDeploymentName(consoleName));
        assertNull(deployment, "No Deployment should be created for a Console with a disallowed image");
    }

    @Test
    void testConsoleServerClusterRoleStaysScopedToStrimziReadAccess() {
        String consoleName = "security-rbac-scope";

        Console console = ConsoleInstanceSetup.getDefaultConsoleInstance(tcc.namespace(), consoleName, tcc.kafkaName(), tcc.kafkaUserName()).build();
        ConsoleInstanceSetup.setupIfNeeded(console);

        String clusterRoleName = tcc.namespace() + "-" + consoleName + "-" + ConsoleClusterRole.NAME;
        String clusterRoleBindingName = tcc.namespace() + "-" + consoleName + "-" + ConsoleClusterRoleBinding.NAME;
        String serviceAccountName = consoleName + "-" + ConsoleServiceAccount.NAME;

        ClusterRole clusterRole = ResourceUtils.getKubeResource(ClusterRole.class, clusterRoleName);
        assertTrue(clusterRole != null && clusterRole.getRules() != null && !clusterRole.getRules().isEmpty(),
            "Expected ClusterRole " + clusterRoleName + " to exist with rules");

        for (PolicyRule rule : clusterRole.getRules()) {
            List<String> resources = rule.getResources() == null ? List.of() : rule.getResources();
            List<String> apiGroups = rule.getApiGroups() == null ? List.of() : rule.getApiGroups();
            List<String> verbs = rule.getVerbs() == null ? List.of() : rule.getVerbs();

            assertFalse(resources.stream().anyMatch(r -> r.toLowerCase(ROOT).contains("secret")),
                "console-server ClusterRole must never grant access to Secrets, found rule: " + rule);
            assertFalse(resources.contains("*") || apiGroups.contains("*") || verbs.contains("*"),
                "console-server ClusterRole must not use wildcard rules, found rule: " + rule);

            if (apiGroups.contains("kafka.strimzi.io")) {
                List<String> disallowedVerbs = verbs.stream()
                    .filter(v -> !List.of("get", "list", "watch", "patch").contains(v.toLowerCase(ROOT)))
                    .toList();
                assertTrue(disallowedVerbs.isEmpty(),
                    "console-server ClusterRole must only read (and narrowly patch) Strimzi resources, "
                        + "found disallowed verb(s) " + disallowedVerbs + " in rule: " + rule);
            }
        }

        ClusterRoleBinding clusterRoleBinding = ResourceUtils.getKubeResource(ClusterRoleBinding.class, clusterRoleBindingName);
        assertNotNull(clusterRoleBinding, "Expected ClusterRoleBinding " + clusterRoleBindingName + " to exist");
        assertEquals("ClusterRole", clusterRoleBinding.getRoleRef().getKind());
        assertEquals(clusterRoleName, clusterRoleBinding.getRoleRef().getName());

        List<Subject> subjects = clusterRoleBinding.getSubjects();
        assertEquals(1, subjects.size(),
            "console-server ClusterRoleBinding must bind exactly one subject, found: " + subjects);
        Subject subject = subjects.get(0);
        assertEquals("ServiceAccount", subject.getKind());
        assertEquals(serviceAccountName, subject.getName());
        assertEquals(tcc.namespace(), subject.getNamespace());
    }

    @Test
    void testConsoleDeniesCrossNamespaceKafkaUserSecretAccessByDefault() {
        String consoleName = "security-confused-deputy";

        Secret baseSecret = ResourceUtils.getKubeResource(Secret.class, tcc.namespace(), tcc.kafkaUserName());
        String baseJaasConfig = baseSecret == null ? null : baseSecret.getData().get(SaslConfigs.SASL_JAAS_CONFIG);
        assertTrue(baseJaasConfig != null && !baseJaasConfig.isEmpty(),
            "Precondition failed: base KafkaUser Secret " + tcc.namespace() + "/" + tcc.kafkaUserName() + " has no SASL JAAS config to attempt exfiltration of");

        Console consoleCR = new ConsoleBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(consoleName)
                .withNamespace(CROSS_NAMESPACE)
                .build())
            .withNewSpec()
                .withHostname(ConsoleUtils.getConsoleUiHostname(consoleName))
                .addNewKafkaCluster()
                    .withName(tcc.kafkaName())
                    .withNamespace(tcc.namespace())
                    .withListener(Constants.SECURE_LISTENER_NAME)
                    .withNewCredentials()
                        .withNewKafkaUser()
                            .withName(tcc.kafkaUserName())
                        .endKafkaUser()
                    .endCredentials()
                .endKafkaCluster()
            .endSpec()
            .build();

        LOGGER.info("Creating secondary Console {}/{} referencing base KafkaUser {}/{} with no RBAC grant",
            CROSS_NAMESPACE, consoleName, tcc.namespace(), tcc.kafkaUserName());
        KubeResourceManager.get().createResourceWithoutWait(consoleCR);

        Wait.until(String.format("Console %s/%s to report a SecurityError condition for the denied cross-namespace Secret",
                CROSS_NAMESPACE, consoleName),
            KubeTestConstants.GLOBAL_POLL_INTERVAL_SHORT, KubeTestConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> {
                Console console = ResourceUtils.getKubeResource(Console.class, CROSS_NAMESPACE, consoleName);
                return console != null
                    && console.getStatus() != null
                    && console.getStatus().getConditions() != null
                    && console.getStatus().getConditions().stream().anyMatch(c -> Condition.Types.SECURITY_ERROR.equals(c.getType()));
            });

        Console console = ResourceUtils.getKubeResource(Console.class, CROSS_NAMESPACE, consoleName);
        Set<Condition> conditions = console.getStatus().getConditions();

        Condition securityError = conditions.stream()
            .filter(c -> Condition.Types.SECURITY_ERROR.equals(c.getType()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Console " + consoleName + " has no SecurityError condition: " + conditions));
        assertEquals(Condition.Reasons.ACCESS_DENIED, securityError.getReason(), securityError.toString());

        String deniedSecretRef = "'" + tcc.namespace() + "/" + tcc.kafkaUserName() + "'";
        assertTrue(securityError.getMessage().contains(deniedSecretRef),
            "SecurityError should name the denied Secret " + deniedSecretRef + ", was: " + securityError.getMessage());

        String secondaryServiceAccount = CROSS_NAMESPACE + ":" + consoleName + "-" + ConsoleServiceAccount.NAME;
        assertTrue(securityError.getMessage().contains(secondaryServiceAccount),
            "SecurityError should name the denied ServiceAccount " + secondaryServiceAccount + ", was: " + securityError.getMessage());

        Secret consoleSecret = ResourceUtils.getKubeResource(Secret.class, CROSS_NAMESPACE, consoleName + "-" + ConsoleSecret.NAME);
        assertNull(consoleSecret, "Console must not materialize a ConsoleSecret when the referenced KafkaUser Secret access is denied");

        List<Secret> secondarySecret = ResourceUtils.listKubeResources(Secret.class, CROSS_NAMESPACE);
        boolean credentialsLeaked = secondarySecret.stream()
            .map(Secret::getData)
            .filter(Objects::nonNull)
            .anyMatch(data -> data.values()
                .stream()
                .anyMatch(v -> v.contains(baseJaasConfig)));
        assertFalse(credentialsLeaked, "Base KafkaUser credentials must never be copied into any Secret in the secondary namespace");
    }

    @ParameterizedTest
    @ValueSource(strings = { "properties", "adminProperties", "consumerProperties", "producerProperties" })
    void testConsoleReconciliationRejectsIllegalKafkaProperty(String propertyMap) {
        String consoleName = "security-illegal-" + propertyMap.toLowerCase(ROOT);

        var consoleBuilder = new ConsoleBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(consoleName)
                .withNamespace(tcc.namespace())
                .build())
            .withNewSpec()
                .withHostname(ConsoleUtils.getConsoleUiHostname(consoleName))
                .addNewKafkaCluster()
                    .withName(tcc.kafkaName())
                    .withNamespace(tcc.namespace())
                    .withListener(Constants.SECURE_LISTENER_NAME);

        switch (propertyMap) {
            case "properties" -> consoleBuilder
                    .withNewProperties()
                        .addNewValue().withName("config.providers").withValue("env").endValue()
                    .endProperties();
            case "adminProperties" -> consoleBuilder
                    .withNewAdminProperties()
                        .addNewValue().withName("config.providers").withValue("env").endValue()
                    .endAdminProperties();
            case "consumerProperties" -> consoleBuilder
                    .withNewConsumerProperties()
                        .addNewValue().withName("config.providers").withValue("env").endValue()
                    .endConsumerProperties();
            case "producerProperties" -> consoleBuilder
                    .withNewProducerProperties()
                        .addNewValue().withName("config.providers").withValue("env").endValue()
                    .endProducerProperties();
            default -> throw new IllegalArgumentException(propertyMap);
        }

        Console consoleCR = consoleBuilder
                .endKafkaCluster()
            .endSpec()
            .build();

        KubeResourceManager.get().createResourceWithoutWait(consoleCR);

        String expectedClusterKey = tcc.namespace() + "/" + tcc.kafkaName();

        Wait.until(String.format("Console %s/%s to report an Error condition for the illegal Kafka property", tcc.namespace(), consoleName),
            KubeTestConstants.GLOBAL_POLL_INTERVAL_SHORT, KubeTestConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> {
                Console console = ResourceUtils.getKubeResource(Console.class, tcc.namespace(), consoleName);
                return console != null
                    && console.getStatus() != null
                    && console.getStatus().getConditions() != null
                    && console.getStatus().getConditions().stream().anyMatch(c -> Condition.Types.ERROR.equals(c.getType()));
            });

        Console console = ResourceUtils.getKubeResource(Console.class, tcc.namespace(), consoleName);
        Set<Condition> conditions = console.getStatus().getConditions();
        assertTrue(conditions.size() > 1, conditions.toString());

        List<Condition> conditionList = List.copyOf(conditions);
        Condition ready = conditionList.getFirst();
        assertEquals(Condition.Types.READY, ready.getType(), ready.toString());
        assertEquals("False", ready.getStatus(), ready.toString());
        assertEquals(Condition.Reasons.INVALID_CONFIGURATION, ready.getReason(), ready.toString());

        List<Condition> errors = conditionList.subList(1, conditionList.size());
        assertEquals(1, errors.size(), errors.toString());

        Condition error = errors.getFirst();
        assertEquals(Condition.Types.ERROR, error.getType(), error.toString());
        assertEquals("True", error.getStatus(), error.toString());
        assertEquals(Condition.Reasons.INVALID_CONFIGURATION, error.getReason(), error.toString());
        assertTrue(error.getMessage().contains(expectedClusterKey), error.getMessage());
        assertTrue(error.getMessage().contains("config.providers"), error.getMessage());
        assertTrue(error.getMessage().contains("illegal configuration properties"), error.getMessage());
    }

    @BeforeAll
    void setupTestClass() {
        tcc = Utils.getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());
        NamespaceUtils.prepareNamespace(CROSS_NAMESPACE);
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
    }
}
