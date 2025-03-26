package com.github.streamshub.systemtests.utils.resources;

import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.skodjob.testframe.resources.KubeResourceManager;

import java.util.List;

public class OperatorGroupUtils {
    private OperatorGroupUtils() {}

    public static List<OperatorGroup> listOperatorGroupByName(String namespace, String name) {
        return KubeResourceManager.getKubeClient().getOpenShiftClient().operatorHub().operatorGroups()
            .inNamespace(namespace).list().getItems().stream()
            .filter(it -> it.getMetadata().getName().equals(name))
            .toList();
    }
}
