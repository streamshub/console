package com.github.streamshub.systemtests.utils.resources.kubernetes;

import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.utils.CommonUtils;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Resource type implementation for managing Secret resources.
 */
public class SecretUtils {

    private static final Logger LOGGER = LogManager.getLogger(SecretUtils.class);

    // -------------
    // Client
    // -------------
    public static MixedOperation<Secret, SecretList, Resource<Secret>> secretClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(Secret.class, SecretList.class);
    }
    // -------------
    // Get
    // -------------
    public static Secret getSecret(String namespace, String name) {
        return secretClient().inNamespace(namespace).withName(name).get();
    }

    // -------------
    // List
    // -------------
    public static List<Secret> listSecrets(String namespace) {
        return secretClient().inNamespace(namespace).list().getItems();
    }

    // -------------
    // Wait
    // -------------
    public static void waitForSecretReady(String namespaceName, String secretName) {
        LOGGER.info("Waiting for Secret: {}/{} to exist", namespaceName, secretName);
        CommonUtils.waitFor("Secret: " + secretName + " to exist", TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, TimeConstants.READINESS_TIMEOUT,
            () -> SecretUtils.getSecret(namespaceName, secretName) != null
        );
        LOGGER.info("Secret: {}/{} created", namespaceName, secretName);
    }

    // -------------
    // Create
    // -------------
    public static void createSecret(Secret secret) {
        secretClient().inNamespace(secret.getMetadata().getNamespace()).resource(secret).create();
    }
}
