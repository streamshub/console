package com.github.streamshub.systemtests.utils.resources.kubernetes;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;

import java.util.List;

public class ConfigMapUtils {

    // -------------
    // Client
    // -------------
    public static MixedOperation<ConfigMap, ConfigMapList, Resource<ConfigMap>> configClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(ConfigMap.class, ConfigMapList.class);
    }

    // -------------
    // Get
    // -------------
    public static ConfigMap getConfigMap(String namespace, String name) {
        return configClient().inNamespace(namespace).withName(name).get();
    }

    // -------------
    // List
    // -------------
    public static List<ConfigMap> listConfigMaps(String namespace) {
        return configClient().inNamespace(namespace).list().getItems();
    }
}
