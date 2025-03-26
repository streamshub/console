package com.github.streamshub.systemtests.utils.resources;

import io.fabric8.kubernetes.api.model.Secret;
import io.skodjob.testframe.resources.KubeResourceManager;

public class SecretUtils {

    private SecretUtils() {}

    public static Secret getSecret(String namespace, String name) {
        return KubeResourceManager.getKubeClient().getClient().secrets().inNamespace(namespace).withName(name).get();
    }
}
