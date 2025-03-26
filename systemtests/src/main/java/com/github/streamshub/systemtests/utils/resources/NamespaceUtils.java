package com.github.streamshub.systemtests.utils.resources;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.PullSecretNotFound;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

public class NamespaceUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(NamespaceUtils.class);
    private NamespaceUtils() {}

    public static Namespace getNamespace(String name) {
        return KubeResourceManager.getKubeClient().getClient().namespaces().withName(name).get();
    }

    public static void createNamespaceAndPrepare(String namespaceName) {
        createNamespace(namespaceName);
        copyImagePullSecrets(namespaceName);
    }

    public static void createNamespace(String name) {
        KubeResourceManager.getInstance().createOrUpdateResourceWithWait(
                new NamespaceBuilder().withNewMetadata().withName(name).endMetadata().build());
    }

    public static void copyImagePullSecrets(String targetNamespace) {
        if (Environment.TEST_CLIENTS_IMAGE_PULL_SECRET != null && !Environment.TEST_CLIENTS_IMAGE_PULL_SECRET.isEmpty()) {
            LOGGER.info("Checking if Secret: {} is in the default Namespace", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET);
            if (SecretUtils.getSecret("default", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET) == null) {
                throw new PullSecretNotFound(Environment.TEST_CLIENTS_IMAGE_PULL_SECRET + " is not in the default Namespace!");
            }

            // Check the target namespace does not have the pull secret already deplyoed
            if (SecretUtils.getSecret(targetNamespace, Environment.TEST_CLIENTS_IMAGE_PULL_SECRET) != null) {
                LOGGER.warn("Pull secret {} is already present in the target namespace {}", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET, targetNamespace);
                return;
            }

            LOGGER.info("Copy pull secret {} to namespace {}", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET, targetNamespace);

            KubeResourceManager.getInstance().createResourceWithWait(new SecretBuilder()
                .withApiVersion("v1")
                .withKind("Secret")
                .withNewMetadata()
                    .withName(Environment.TEST_CLIENTS_IMAGE_PULL_SECRET)
                    .withNamespace(targetNamespace)
                .endMetadata()
                .withType("kubernetes.io/dockerconfigjson")
                .withData(Collections.singletonMap(".dockerconfigjson",
                    SecretUtils.getSecret("default", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET).getData().get(".dockerconfigjson")))
                .build());
        }
    }
}
