package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

public class NamespaceUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(NamespaceUtils.class);

    private NamespaceUtils() {}

    /**
     * Ensures that the specified Kubernetes namespace exists. If the namespace does not exist,
     * it creates the namespace and waits for its creation to complete.
     *
     * @param namespaceName the name of the Kubernetes namespace to prepare
     */
    public static void prepareNamespace(String namespaceName) {
        Namespace namespace = ResourceUtils.getKubeResource(Namespace.class, namespaceName);
        if (namespace == null) {
            KubeResourceManager.get().createResourceWithWait(
                new NamespaceBuilder()
                    .withNewMetadata()
                        .withName(namespaceName)
                    .endMetadata()
                .build());
        }

        copyTestClientsImagePullSecret(namespaceName);
    }

    /**
     * Ensures that if there is a pull secret deployed in the default namespace, it will be copied into a desired namespace.
     * That way test client Job is able to pull custom images from a private registries
     * @param namespace the name of the Kubernetes namespace to copy the secret into
     */
    public static void copyTestClientsImagePullSecret(String namespace) {
        if (Environment.isTestClientsPullSecretPresent()) {
            LOGGER.info("Checking if Secret: {} is in the default Namespace", Environment.TEST_CLIENTS_PULL_SECRET);
            if (ResourceUtils.getKubeResource(Secret.class, "default", Environment.TEST_CLIENTS_PULL_SECRET) == null) {
                throw new SetupException(Environment.TEST_CLIENTS_PULL_SECRET + " secret is not in the default Namespace!");
            }

            LOGGER.info("Copying pull Secret {}/{} to {}/{}", "default", Environment.TEST_CLIENTS_PULL_SECRET, namespace, Environment.TEST_CLIENTS_PULL_SECRET);
            Secret pullSecretToCopy = ResourceUtils.getKubeResource(Secret.class, "default", Environment.TEST_CLIENTS_PULL_SECRET);

            if (ResourceUtils.getKubeResource(Secret.class, namespace, Environment.TEST_CLIENTS_PULL_SECRET) != null) {
                LOGGER.warn("Pull secret {} is already present in namespace {}", Environment.TEST_CLIENTS_PULL_SECRET, namespace);
                return;
            }

            KubeResourceManager.get()
                .createOrUpdateResourceWithWait(new SecretBuilder()
                    .withApiVersion("v1")
                    .withKind("Secret")
                    .withNewMetadata()
                    .withName(Environment.TEST_CLIENTS_PULL_SECRET)
                    .withNamespace(namespace)
                    .endMetadata()
                    .withType("kubernetes.io/dockerconfigjson")
                    .withData(Collections.singletonMap(".dockerconfigjson", pullSecretToCopy.getData()
                        .get(".dockerconfigjson")))
                    .build());
        }
    }
}
