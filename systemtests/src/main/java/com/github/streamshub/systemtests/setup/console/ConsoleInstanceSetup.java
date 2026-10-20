package com.github.streamshub.systemtests.setup.console;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.ConsoleSpecBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.Credentials;
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
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.console.ConsoleUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.keycloak.KeycloakUtils;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.skodjob.kubetest4j.resources.KubeResourceManager;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

public class ConsoleInstanceSetup {
    private static final Logger LOGGER = LogManager.getLogger(ConsoleInstanceSetup.class);
    private ConsoleInstanceSetup() {}

    public static void setupIfNeeded(Console console) {
        LOGGER.info("----------- Deploy Console Instance -----------");
        String namespace = console.getMetadata().getNamespace();
        String name = console.getMetadata().getName();

        maybeCreateKafkaUserRBAC(console);

        if (ResourceUtils.getKubeResource(Console.class, namespace, name) != null) {
            LOGGER.info("Skipping Console Instance '{}' deployment in namespace '{}', it is already deployed", name, namespace);
            return;
        }

        LOGGER.info("Deploying Console Instance '{}' in namespace '{}'", name, namespace);
        KubeResourceManager.get().createResourceWithWait(console);
        LOGGER.info("Console deployed and available at {}", ConsoleUtils.getConsoleUiUrl(console.getMetadata().getName(), true));
    }

    private static void maybeCreateKafkaUserRBAC(Console console) {
        var kafkaUserCredentials = kafkaUsersByNamespace(console);

        if (!kafkaUserCredentials.isEmpty()) {
            String namespace = console.getMetadata().getNamespace();
            String name = console.getMetadata().getName();

            List<? super HasMetadata> rbacResources = new ArrayList<>();
            var prefix = "%s-%s-kafkauser-reader".formatted(namespace, name);

            for (var entry : kafkaUserCredentials.entrySet()) {
                var userNamespace = entry.getKey();
                var usernames = entry.getValue();
                LOGGER.info("Creating Role/RoleBinding in namespace {} for KafkaUsers {}", userNamespace, usernames);

                rbacResources.add(new io.fabric8.kubernetes.api.model.rbac.RoleBuilder()
                        .withNewMetadata()
                            .withNamespace(userNamespace)
                            .withName(prefix + "-role")
                        .endMetadata()
                        .addNewRule()
                            .withApiGroups(List.of(""))
                            .withResources("secrets")
                            .withResourceNames(usernames)
                            .withVerbs("get")
                        .endRule()
                    .build());

                rbacResources.add(new io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder()
                        .withNewMetadata()
                            .withNamespace(userNamespace)
                            .withName(prefix + "-binding")
                        .endMetadata()
                        .withNewRoleRef()
                            .withApiGroup("rbac.authorization.k8s.io")
                            .withKind("Role")
                            .withName(prefix + "-role")
                        .endRoleRef()
                        .addNewSubject()
                            .withKind("ServiceAccount")
                            .withNamespace(namespace)
                            .withName(name + "-console-serviceaccount")
                        .endSubject()
                    .build());
            }

            KubeResourceManager.get().createOrUpdateResourceAsyncWait(rbacResources.toArray(HasMetadata[]::new));
        }
    }

    public static void removeKafkaUserRBAC(Console console, boolean awaitDeploymentUpdates) {
        var kafkaUserCredentials = kafkaUsersByNamespace(console);
        String namespace = console.getMetadata().getNamespace();
        String name = console.getMetadata().getName();

        List<? super HasMetadata> rbacResources = new ArrayList<>();
        var prefix = "%s-%s-kafkauser-reader".formatted(namespace, name);

        for (var entry : kafkaUserCredentials.entrySet()) {
            var userNamespace = entry.getKey();
            var usernames = entry.getValue();
            LOGGER.info("Removing Role/RoleBinding in namespace {} for KafkaUsers {}", userNamespace, usernames);
            rbacResources.add(ResourceUtils.getKubeResource(io.fabric8.kubernetes.api.model.rbac.Role.class, userNamespace, prefix + "-role"));
            rbacResources.add(ResourceUtils.getKubeResource(io.fabric8.kubernetes.api.model.rbac.RoleBinding.class, userNamespace, prefix + "-binding"));
        }

        KubeResourceManager.get().deleteResourceAsyncWait(rbacResources.toArray(HasMetadata[]::new));

        var modtimeName = "console.streamshub.io/systemtest-modtime";
        var modtime = Instant.now().toString();

        // modify the CR to force a reconcile
        KubeResourceManager.get().kubeClient().getClient()
            .resources(Console.class)
            .inNamespace(console.getMetadata().getNamespace())
            .withName(console.getMetadata().getName())
            .edit(c -> new ConsoleBuilder(c)
                    .editSpec()
                        .editOrNewDeployment()
                            .editOrNewMetadata()
                                .addToAnnotations(modtimeName, modtime)
                            .endMetadata()
                        .endDeployment()
                    .endSpec()
                    .build());

        if (awaitDeploymentUpdates) {
            KubeResourceManager.get().kubeClient().getClient()
                .resources(Deployment.class)
                .inNamespace(console.getMetadata().getNamespace())
                .withName(console.getMetadata().getName() + "-console-deployment")
                .waitUntilCondition(
                        deployment -> {
                            if (Readiness.isDeploymentReady(deployment)) {
                                return modtime.equals(deployment.getMetadata().getAnnotations().get(modtimeName));
                            }

                            return false;
                        },
                        60, TimeUnit.SECONDS);
        }
    }

    private static Map<String, List<String>> kafkaUsersByNamespace(Console console) {
        return console.getSpec()
            .getKafkaClusters()
            .stream()
            .map(cluster -> {
                String clusterNamespace = cluster.getNamespace();

                return Optional.ofNullable(cluster.getCredentials())
                        .map(Credentials::getKafkaUser)
                        .map(user -> Map.entry(
                                Optional.ofNullable(user.getNamespace()).orElse(clusterNamespace),
                                user.getName()
                        ))
                        .orElse(null);
            })
            .filter(Objects::nonNull)
            .collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toList())));
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
                .withNewDeployment()
                    .withNewSpec()
                        .withNewTemplate()
                            .withNewSpec()
                                .withNewServerContainer()
                                    .withImage(Environment.CONSOLE_API_IMAGE)
                                .endServerContainer()
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                .endDeployment()
            .endSpec();
        }

        return builder;
    }

    public static ConsoleBuilder getOidcConsoleInstance(String namespace, String consoleInstanceName, KeycloakInstanceSetup keycloak,
        List<KeycloakTestConfig.GroupRoleMapping> roleMapping, List<KeycloakTestConfig.KafkaConfig> kafkaConfig
    ) {
        LOGGER.debug("Building OIDC Console instance '{}' in namespace '{}' using Keycloak realm '{}' with {} Kafka cluster(s)",
            consoleInstanceName, namespace, keycloak.realmName(), kafkaConfig.size());
        return new ConsoleBuilder()
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
}
