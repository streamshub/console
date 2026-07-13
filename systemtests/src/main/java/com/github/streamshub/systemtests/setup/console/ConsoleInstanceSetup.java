package com.github.streamshub.systemtests.setup.console;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.ConsoleSpecBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.KafkaCluster;
import com.github.streamshub.console.api.v1alpha1.spec.KafkaClusterBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;
import com.github.streamshub.console.api.v1alpha1.spec.security.Role;
import com.github.streamshub.console.api.v1alpha1.spec.security.RoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.security.Rule;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakInstanceSetup;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakTestConfig;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.console.ConsoleUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.keycloak.KeycloakUtils;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ConsoleInstanceSetup {
    private static final Logger LOGGER = LogManager.getLogger(ConsoleInstanceSetup.class);
    private ConsoleInstanceSetup() {}

    public static void setupIfNeeded(Console console) {
        LOGGER.info("----------- Deploy Console Instance -----------");
        String namespace = console.getMetadata().getNamespace();
        String name = console.getMetadata().getName();
        if (ResourceUtils.getKubeResource(Console.class, namespace, name) != null) {
            LOGGER.info("Skipping Console Instance '{}' deployment in namespace '{}', it is already deployed", name, namespace);
            return;
        }
        LOGGER.info("Deploying Console Instance '{}' in namespace '{}'", name, namespace);
        KubeResourceManager.get().createResourceWithWait(console);
        LOGGER.info("Console deployed and available at {}", ConsoleUtils.getConsoleUiUrl(console.getMetadata().getName(), true));
    }

    public static ConsoleBuilder getDefaultConsoleInstance(String namespaceName, String instanceName, String kafkaName, String kafkaUserName) {
        LOGGER.debug("Building default Console instance '{}' in namespace '{}' for Kafka cluster '{}' with user '{}'", instanceName, namespaceName, kafkaName, kafkaUserName);
        ConsoleBuilder builder = new ConsoleBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(instanceName)
                .withNamespace(namespaceName)
                .build())
            .withSpec(new ConsoleSpecBuilder()
                .withHostname(ConsoleUtils.getConsoleUiHostname(instanceName))
                .withKafkaClusters(
                    new KafkaClusterBuilder()
                        .withId(kafkaName)
                        .withName(kafkaName)
                        .withListener(Constants.SECURE_LISTENER_NAME)
                        .withNamespace(namespaceName)
                        .withNewCredentials()
                            .withNewKafkaUser()
                                .withName(kafkaUserName)
                            .endKafkaUser()
                        .endCredentials()
                    .build()
                    )
                .build());

        if (!Environment.CONSOLE_API_IMAGE.isEmpty()) {
            LOGGER.info("Using custom Console API image: {}", Environment.CONSOLE_API_IMAGE);
            builder = builder.editSpec()
                .editOrNewContainers()
                    .editOrNewApi()
                        .editOrNewSpec()
                            .withImage(Environment.CONSOLE_API_IMAGE)
                        .endSpec()
                    .endApi()
                .endContainers()
            .endSpec();
        }

        return builder;
    }

    public static ConsoleBuilder getOidcConsoleInstance(String namespace, String consoleInstanceName, KeycloakInstanceSetup keycloak,
        List<KeycloakTestConfig.GroupRoleMapping> roleMapping, List<KeycloakTestConfig.KafkaConfig> kafkaConfig
    ) {
        LOGGER.debug("Building OIDC Console instance '{}' in namespace '{}' using Keycloak realm '{}' with {} Kafka cluster(s)",
            consoleInstanceName, namespace, keycloak.realmName(), kafkaConfig.size());
        ConsoleBuilder builder = new ConsoleBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(consoleInstanceName)
                .withNamespace(namespace)
                .build())
            .withNewSpec()
                .withHostname(ConsoleUtils.getConsoleUiHostname(consoleInstanceName))
                .addAllToKafkaClusters(getOidcKafkaClusters(namespace, kafkaConfig))
                .withNewSecurity()
                    .withNewOidc()
                        .withAuthServerUrl(KeycloakUtils.getKeycloakRealmUri(keycloak.httpsHostname(), keycloak.realmName()))
                        .withClientId(keycloak.clientId())
                        .withNewClientSecret()
                            .withValue(keycloak.clientSecret())
                        .endClientSecret()
                        .withNewTrustStore()
                            .withType(TrustStore.Type.JKS)
                            .withNewPassword()
                                .withNewValueFrom()
                                    .withNewSecretKeyRef(Constants.PASSWORD_KEY_NAME, keycloak.getTrustStoreSecretName(), false)
                                .endValueFrom()
                            .endPassword()
                            .withNewContent()
                                .withNewValueFrom()
                                    .withNewConfigMapKeyRef(Constants.TRUST_STORE_KEY_NAME, keycloak.getTrustStoreConfigMap(), false)
                                .endValueFrom()
                            .endContent()
                        .endTrustStore()
                    .endOidc()
                    .addAllToSubjects(KeycloakTestConfig.getSubjects(roleMapping))
                    .addAllToRoles(KeycloakTestConfig.getRoles(roleMapping))
                .endSecurity()
            .endSpec();

        return builder;
    }

    public static List<KafkaCluster> getOidcKafkaClusters(String namespace, List<KeycloakTestConfig.KafkaConfig> kafkaConfig) {
        return kafkaConfig.stream()
            .map(kc -> {
                List<Role> roles = kc.roles().stream()
                    .map(r -> new RoleBuilder()
                        .withName(r.roleName())
                        .addNewRule()
                            .withResources(r.resources())
                            .withPrivileges(r.privileges().toArray(new Rule.Privilege[0]))
                        .endRule()
                        .build())
                    .toList();

                return new KafkaClusterBuilder()
                    .withId(kc.kafkaName())
                    .withName(kc.kafkaName())
                    .withListener(Constants.SECURE_LISTENER_NAME)
                    .withNamespace(namespace)
                    .withNewCredentials()
                        .withNewKafkaUser()
                            .withName(KafkaNamingUtils.kafkaUserName(kc.kafkaName()))
                        .endKafkaUser()
                    .endCredentials()
                    .withNewSecurity()
                        .withRoles(roles)
                    .endSecurity()
                    .build();
            })
            .toList();
    }

    public static ConsoleBuilder forceNodeJsToAcceptSelfSignedCerts(ConsoleBuilder builder) {
        if (!ClusterUtils.isOcp()) {
            return builder;
        }

        LOGGER.debug("Cluster is OpenShift, forcing Console UI Node.js container to accept self-signed certificates");
        // Force NextJS to accept self signed certs
        return builder
            .editSpec()
                .editContainers()
                    .withNewUi()
                        .editSpec()
                            .addToEnv(new EnvVarBuilder()
                                .withName("NODE_TLS_REJECT_UNAUTHORIZED")
                                .withValue("0")
                                .build())
                        .endSpec()
                    .endUi()
                .endContainers()
            .endSpec();
    }
}
