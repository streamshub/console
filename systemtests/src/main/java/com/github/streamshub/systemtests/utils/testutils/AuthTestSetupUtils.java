package com.github.streamshub.systemtests.utils.testutils;

import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.KafkaCluster;
import com.github.streamshub.console.api.v1alpha1.spec.KafkaClusterBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;
import com.github.streamshub.console.api.v1alpha1.spec.security.Role;
import com.github.streamshub.console.api.v1alpha1.spec.security.RoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.security.Rule;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakTestConfig;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.keycloak.KeycloakUtils;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;

import java.util.List;

public class AuthTestSetupUtils {
    private AuthTestSetupUtils() {}

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

    public static ConsoleBuilder getOidcConsoleInstance(String namespace, String httpsHostname, String consoleInstanceName, String userName, String password,
        String trustStoreSecretName, String trustStoreConfigMap, List<KeycloakTestConfig.GroupRoleMapping> roleMapping,
        List<KeycloakTestConfig.KafkaConfig> kafkaConfig
    ) {
        ConsoleBuilder builder = new ConsoleBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(consoleInstanceName)
                .withNamespace(namespace)
                .build())
            .withNewSpec()
                .withHostname(consoleInstanceName + "." + ClusterUtils.getClusterDomain())
                .addAllToKafkaClusters(getOidcKafkaClusters(namespace, kafkaConfig))
                // OIDC Config
                .withNewSecurity()
                    .withNewOidc()
                        .withAuthServerUrl(KeycloakUtils.getKeycloakRealmUri(httpsHostname, Constants.KEYCLOAK_REALM))
                        .withClientId(Constants.KEYCLOAK_CLIENT_ID)
                        .withNewClientSecret()
                            .withValue(KeycloakUtils.getClientSecret(httpsHostname, userName, password, Constants.KEYCLOAK_REALM, Constants.KEYCLOAK_CLIENT_ID))
                        .endClientSecret()
                        .withNewTrustStore()
                            .withType(TrustStore.Type.JKS)
                            .withNewPassword()
                                .withNewValueFrom()
                                    .withNewSecretKeyRef(Constants.PASSWORD_KEY_NAME, trustStoreSecretName, false)
                                .endValueFrom()
                            .endPassword()
                            .withNewContent()
                                .withNewValueFrom()
                                    .withNewConfigMapKeyRef(Constants.TRUST_STORE_KEY_NAME, trustStoreConfigMap, false)
                                .endValueFrom()
                            .endContent()
                        .endTrustStore()
                    .endOidc()
                    // Subjects (Keycloak Groups → Console Roles)
                    // Map JWT claims to console roles
                    .addAllToSubjects(KeycloakTestConfig.getSubjects(roleMapping))
                    // Roles (Console Role → Kafka Privileges)
                    // Map which role can see which kafka
                    .addAllToRoles(KeycloakTestConfig.getRoles(roleMapping))
                .endSecurity()
            .endSpec();

        builder = forceNodeJsToAcceptSelfSignedCerts(builder);

        return builder;
    }

    public static ConsoleBuilder forceNodeJsToAcceptSelfSignedCerts(ConsoleBuilder builder) {
        if (!ClusterUtils.isOcp()) {
            return builder;
        }

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
