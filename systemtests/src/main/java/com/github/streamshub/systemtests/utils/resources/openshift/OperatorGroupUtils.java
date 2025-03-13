package com.github.streamshub.systemtests.utils.resources.openshift;

import com.github.streamshub.systemtests.constants.Constants;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.skodjob.testframe.resources.KubeResourceManager;

import java.util.Collections;
import java.util.List;

public class OperatorGroupUtils {

    // -------------
    // Client
    // -------------
    private static MixedOperation<OperatorGroup, OperatorGroupList, Resource<OperatorGroup>> operatorGroupClient() {
        return KubeResourceManager.getKubeClient().getClient().adapt(OpenShiftClient.class).operatorHub().operatorGroups();
    }

    // -------------
    // Get
    // -------------
    public static OperatorGroup getDefaultOperatorGroup(String namespace) {
        return new OperatorGroupBuilder()
            .editOrNewMetadata()
                .withName(Constants.CONSOLE_OPERATOR_GROUP_NAME)
                .withNamespace(namespace)
                .withLabels(Collections.singletonMap("app", "strimzi"))
            .endMetadata()
            .build();
    }

    public static OperatorGroup getOperatorGroup(String namespace, String name) {
        return operatorGroupClient().inNamespace(namespace).withName(name).get();
    }

    public static List<OperatorGroup> listOperatorGroupByName(String namespace, String name) {
        return operatorGroupClient().inNamespace(namespace).list().getItems().stream().filter(it -> it.getMetadata().getName().equals(name)).toList();
    }
}
