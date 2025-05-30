package com.github.streamshub.systemtests.setup.console;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.KafkaClusterBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;
import com.github.streamshub.console.dependents.ConsoleResource;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.utils.ClusterUtils;
import com.github.streamshub.systemtests.utils.ConsoleUtils;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.KeycloakUtils;
import com.github.streamshub.systemtests.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Base64;

public class ConsoleInstanceSetup {
    private static final Logger LOGGER = LogManager.getLogger(ConsoleInstanceSetup.class);
    private ConsoleInstanceSetup() {}

    public static void setupIfNeeded(Console console) {
        LOGGER.info("----------- Deploy Console Instance -----------");
        if (ResourceUtils.getKubeResource(Console.class, console.getMetadata().getNamespace(), console.getMetadata().getName()) != null) {
            LOGGER.warn("Skipping Console Instance deployment. It is already deployed");
            return;
        }

        // Secret to truststore
        KubeResourceManager.get().createOrUpdateResourceWithWait(new SecretBuilder()
            .withNewMetadata()
                .withName(Constants.KEYCLOAK_TRUST_STORE_ACCCESS_SECRET_NAME)
                .withNamespace(console.getMetadata().getNamespace())
                .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
            .endMetadata()
            .addToData(Constants.PASSWORD_KEY_NAME, Base64.getEncoder().encodeToString(Constants.TRUST_STORE_PASSWORD.getBytes()))
            .build());

        // Configmap with truststore
        KubeResourceManager.get().createOrUpdateResourceWithWait(new ConfigMapBuilder()
            .withNewMetadata()
                .withName(Constants.KEYCLOAK_TRUST_STORE_CONFIGMAP_NAME)
                .withNamespace(console.getMetadata().getNamespace())
                .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
            .endMetadata()
            .addToBinaryData(Constants.TRUST_STORE_KEY_NAME, Base64.getEncoder().encodeToString(FileUtils.readFileBytes(Constants.TRUST_STORE_FILE_PATH)))
            .build());

        KubeResourceManager.get().createResourceWithWait(console);
        LOGGER.info("Console deployed and available at {}", ConsoleUtils.getConsoleUiUrl(console.getMetadata().getNamespace(), console.getMetadata().getName(), true));
    }

    public static Console getDefaultConsoleInstance(String namespaceName, String instanceName, String kafkaName, String kafkaUserName, String clientSecret) {
        ConsoleBuilder builder = new ConsoleBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(instanceName)
                .withNamespace(namespaceName)
                .build())
            .withNewSpec()
                .withHostname(instanceName + "." + ClusterUtils.getClusterDomain())
                .withNewSecurity()
                    .withNewOidc()
                        .withAuthServerUrl(KeycloakUtils.getKeycloakRealmUri(Constants.KEYCLOAK_REALM))
                        .withClientId(Constants.KEYCLOAK_CLIENT_ID)
                        .withNewClientSecret()
                            .withValue(clientSecret)
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
                .endSecurity()
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
                    .build())
            .endSpec();

        if (!Environment.CONSOLE_API_IMAGE.isEmpty()) {
            builder = builder.editSpec()
                .withNewImages()
                    .withApi(Environment.CONSOLE_API_IMAGE)
                .endImages()
            .endSpec();
        }

        if (!Environment.CONSOLE_UI_IMAGE.isEmpty()) {
            builder = builder.editSpec()
                .editImages()
                    .withUi(Environment.CONSOLE_UI_IMAGE)
                .endImages()
            .endSpec();
        }

        return builder.build();
    }
}
