package com.github.streamshub.systemtests.utils.resources.kubernetes;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.utils.CommonUtils;
import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.NamespaceList;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

public class NamespaceUtils {
    private static final Logger LOGGER = LogManager.getLogger(NamespaceUtils.class);

    // -------------
    // Client
    // -------------
    private static NonNamespaceOperation<Namespace, NamespaceList, Resource<Namespace>> namespaceClient() {
        return KubeResourceManager.getKubeClient().getClient().namespaces();
    }

    // -------------
    // Get
    // -------------
    public static Namespace getNamespace(String namespace) {
        return namespaceClient().withName(namespace).get();
    }

    public static Namespace getNewNamespace(String name) {
        return new NamespaceBuilder().withNewMetadata().withName(name).endMetadata().build();
    }

    // -------------
    // Create
    // -------------
    public static void createNamespace(String namespaceName) {
        if (getNamespace(namespaceName) != null) {
            LOGGER.warn("Namespace was already created!");
            return;
        }
        namespaceClient().resource(getNewNamespace(namespaceName)).create();
    }

    // -------------
    // Wait
    // -------------
    private static void waitForNamespaceCreation(String namespaceName) {
        CommonUtils.waitFor("Namespace: " + namespaceName + " to be created", TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, TimeConstants.GLOBAL_TIMEOUT,
            () -> getNamespace(namespaceName) != null);
    }

    // -------------
    // Delete
    // -------------
    public static void deleteNamespace(Namespace resource) {
        namespaceClient().resource(resource).withPropagationPolicy(DeletionPropagation.FOREGROUND).delete();
        CommonUtils.waitFor("namespace to be deleted", TimeConstants.GLOBAL_POLL_INTERVAL, TimeConstants.GLOBAL_TIMEOUT,
            () -> getNamespace(resource.getMetadata().getName()) == null);
    }

    public static void deleteNamespace(String name) {
        Namespace namespace = getNamespace(name);
        if (namespace != null) {
            deleteNamespace(namespace);
        }
    }

    // ------------------
    // Copy Secrets Util
    // ------------------
    public static void copyImagePullSecrets(String namespace) {
        if (Environment.TEST_CLIENTS_IMAGE_PULL_SECRET != null && !Environment.TEST_CLIENTS_IMAGE_PULL_SECRET.isEmpty()) {
            LOGGER.info("Checking if Secret: {} is in the default Namespace", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET);
            if (SecretUtils.getSecret("default", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET) == null) {
                throw new RuntimeException(Environment.TEST_CLIENTS_IMAGE_PULL_SECRET + " is not in the default Namespace!");
            }

            LOGGER.info("Creating pull Secret: {}/{}", namespace, Environment.TEST_CLIENTS_IMAGE_PULL_SECRET);
            Secret pullSecret = SecretUtils.getSecret("default", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET);

            if (SecretUtils.getSecret(namespace, Environment.TEST_CLIENTS_IMAGE_PULL_SECRET) != null) {
                LOGGER.warn("Pull secret {} is already present in namespace {}", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET, namespace);
                return;
            }

            SecretUtils.createSecret(new SecretBuilder()
                .withApiVersion("v1")
                .withKind("Secret")
                .withNewMetadata()
                    .withName(Environment.TEST_CLIENTS_IMAGE_PULL_SECRET)
                    .withNamespace(namespace)
                .endMetadata()
                .withType("kubernetes.io/dockerconfigjson")
                .withData(Collections.singletonMap(".dockerconfigjson", pullSecret.getData().get(".dockerconfigjson")))
                .build());
        }
    }
}
