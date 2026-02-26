package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.KafkaCluster;
import com.github.streamshub.console.api.v1alpha1.spec.KafkaClusterBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;
import com.github.streamshub.console.api.v1alpha1.spec.security.Rule;
import com.github.streamshub.console.config.security.ResourceTypes;
import com.github.streamshub.systemtests.constants.AuthTestConstants;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakConfig;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.keycloak.KeycloakUtils;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import java.util.List;

public class AuthTestSetupUtils {
    private AuthTestSetupUtils() {}

    public static List<KafkaCluster> getOidcKafkaClusters(String namespace) {
        return List.of(
            new KafkaClusterBuilder()
                .withId(AuthTestConstants.TEAM_DEV_KAFKA_NAME)
                .withName(AuthTestConstants.TEAM_DEV_KAFKA_NAME)
                .withListener(Constants.SECURE_LISTENER_NAME)
                .withNamespace(namespace)
                .withNewCredentials()
                    .withNewKafkaUser()
                        .withName(KafkaNamingUtils.kafkaUserName(AuthTestConstants.TEAM_DEV_KAFKA_NAME))
                    .endKafkaUser()
                .endCredentials()
                // Map KafkaResources-to-ConsoleRoles with privileges
                .withNewSecurity()
                    .addNewRole()
                        .withName(AuthTestConstants.DEV_ROLE_NAME)
                        .addNewRule()
                            .withResources(ResourceTypes.Kafka.ALL.expand().stream().map(ResourceTypes.ResourceType::value).toList())
                            .withPrivileges(Rule.Privilege.GET, Rule.Privilege.LIST)
                        .endRule()
                    .endRole()
                    .addNewRole()
                        .withName(AuthTestConstants.ADMIN_ROLE_NAME)
                        .addNewRule()
                            .withResources(ResourceTypes.Kafka.ALL.expand().stream().map(ResourceTypes.ResourceType::value).toList())
                            .withPrivileges(Rule.Privilege.ALL)
                        .endRule()
                    .endRole()
                    .addNewRole()
                        .withName(AuthTestConstants.TOPICS_VIEW_ROLE_NAME)
                        .addNewRule()
                            .addToResources(
                                ResourceTypes.Kafka.TOPICS.value(),
                                ResourceTypes.Kafka.TOPIC_RECORDS.value())
                            .withPrivileges(Rule.Privilege.GET, Rule.Privilege.LIST)
                        .endRule()
                    .endRole()
                    .addNewRole()
                        .withName(AuthTestConstants.CONSUMERGROUPS_VIEW_ROLE_NAME)
                        .addNewRule()
                            .addToResources(
                                ResourceTypes.Kafka.CONSUMER_GROUPS.value())
                            .withPrivileges(Rule.Privilege.GET, Rule.Privilege.LIST)
                        .endRule()
                    .endRole()
                .endSecurity()
            .build(),
            new KafkaClusterBuilder()
                .withId(AuthTestConstants.TEAM_ADMIN_KAFKA_NAME)
                .withName(AuthTestConstants.TEAM_ADMIN_KAFKA_NAME)
                .withListener(Constants.SECURE_LISTENER_NAME)
                .withNamespace(namespace)
                .withNewCredentials()
                    .withNewKafkaUser()
                        .withName(KafkaNamingUtils.kafkaUserName(AuthTestConstants.TEAM_ADMIN_KAFKA_NAME))
                    .endKafkaUser()
                .endCredentials()
                // Map KafkaResources-to-ConsoleRoles with privileges
                .withNewSecurity()
                    .addNewRole()
                        .withName(AuthTestConstants.ADMIN_ROLE_NAME)
                        .addNewRule()
                            .withResources(ResourceTypes.Kafka.ALL.expand().stream().map(ResourceTypes.ResourceType::value).toList())
                            .withPrivileges(Rule.Privilege.ALL)
                        .endRule()
                    .endRole()
                .endSecurity()
            .build());
    }

    public static ConsoleBuilder getOidcConsoleInstance(String namespace, String consoleInstanceName, KeycloakConfig keycloakConfig) {
        ConsoleBuilder builder = new ConsoleBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(consoleInstanceName)
                .withNamespace(namespace)
                .build())
            .withNewSpec()
                .withHostname(consoleInstanceName + "." + ClusterUtils.getClusterDomain())
                .addAllToKafkaClusters(getOidcKafkaClusters(namespace))
                // OIDC Config
                .withNewSecurity()
                    .withNewOidc()
                        .withAuthServerUrl(KeycloakUtils.getKeycloakRealmUri(Constants.KEYCLOAK_REALM))
                        .withClientId(Constants.KEYCLOAK_CLIENT_ID)
                        .withNewClientSecret()
                            .withValue(KeycloakUtils.getClientSecret(keycloakConfig, Constants.KEYCLOAK_REALM, Constants.KEYCLOAK_CLIENT_ID))
                        .endClientSecret()
                        .withNewTrustStore()
                            .withType(TrustStore.Type.JKS)
                            .withNewPassword()
                                .withNewValueFrom()
                                    .withNewSecretKeyRef(Constants.PASSWORD_KEY_NAME, Constants.KEYCLOAK_TRUST_STORE_ACCCESS_SECRET_NAME, false)
                                .endValueFrom()
                            .endPassword()
                            .withNewContent()
                                .withNewValueFrom()
                                    .withNewConfigMapKeyRef(Constants.TRUST_STORE_KEY_NAME, Constants.KEYCLOAK_TRUST_STORE_CONFIGMAP_NAME, false)
                                .endValueFrom()
                            .endContent()
                        .endTrustStore()
                    .endOidc()
                    // Subjects (Keycloak Groups → Console Roles)
                    // Map JWT claims to console roles
                    .addNewSubject()
                        .withClaim(AuthTestConstants.CLAIM_GROUPS)
                        .addToInclude(AuthTestConstants.DEV_GROUP_NAME)
                        .addToRoleNames(AuthTestConstants.DEV_ROLE_NAME)
                    .endSubject()
                    .addNewSubject()
                        .withClaim(AuthTestConstants.CLAIM_GROUPS)
                        .addToInclude(AuthTestConstants.ADMIN_GROUP_NAME)
                        .addToRoleNames(AuthTestConstants.ADMIN_ROLE_NAME)
                    .endSubject()
                    .addNewSubject()
                        .withClaim(AuthTestConstants.CLAIM_GROUPS)
                        .addToInclude(AuthTestConstants.TOPICS_VIEW_GROUP_NAME)
                        .addToRoleNames(AuthTestConstants.TOPICS_VIEW_ROLE_NAME)
                    .endSubject()
                    .addNewSubject()
                        .withClaim(AuthTestConstants.CLAIM_GROUPS)
                        .addToInclude(AuthTestConstants.CONSUMERGROUPS_VIEW_GROUP_NAME)
                        .addToRoleNames(AuthTestConstants.CONSUMERGROUPS_VIEW_ROLE_NAME)
                    .endSubject()
                    // Roles (Console Role → Kafka Privileges)
                    // Map which role can see which kafka
                    .addNewRole()
                        .withName(AuthTestConstants.DEV_ROLE_NAME)
                        .addNewRule()
                            .withResources(ResourceTypes.Global.KAFKAS.value())
                            .withResourceNames(AuthTestConstants.TEAM_DEV_KAFKA_NAME)
                            .withPrivileges(Rule.Privilege.ALL)
                        .endRule()
                    .endRole()
                    .addNewRole()
                        .withName(AuthTestConstants.ADMIN_ROLE_NAME)
                        .addNewRule()
                            .withResources(ResourceTypes.Global.KAFKAS.value())
                            .withResourceNames(AuthTestConstants.TEAM_ADMIN_KAFKA_NAME, AuthTestConstants.TEAM_DEV_KAFKA_NAME)
                            .withPrivileges(Rule.Privilege.ALL)
                        .endRule()
                    .endRole()
                    .addNewRole()
                        .withName(AuthTestConstants.TOPICS_VIEW_ROLE_NAME)
                        .addNewRule()
                            .withResources(ResourceTypes.Global.KAFKAS.value())
                            .withResourceNames(AuthTestConstants.TEAM_DEV_KAFKA_NAME)
                            .withPrivileges(Rule.Privilege.GET, Rule.Privilege.LIST)
                        .endRule()
                    .endRole()
                    .addNewRole()
                        .withName(AuthTestConstants.CONSUMERGROUPS_VIEW_ROLE_NAME)
                        .addNewRule()
                            .withResources(ResourceTypes.Global.KAFKAS.value())
                            .withResourceNames(AuthTestConstants.TEAM_DEV_KAFKA_NAME)
                            .withPrivileges(Rule.Privilege.GET, Rule.Privilege.LIST)
                        .endRule()
                    .endRole()
                .endSecurity()
            .endSpec();

        if (!ClusterUtils.isOcp()) {
            // Force NextJS to accept self signed certs
            builder = builder
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

        return builder;
    }
}
