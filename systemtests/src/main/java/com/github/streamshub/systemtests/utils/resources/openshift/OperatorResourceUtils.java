package com.github.streamshub.systemtests.utils.resources.openshift;

import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.utils.CommonUtils;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1.Operator;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class OperatorResourceUtils {
    private static final Logger LOGGER = LogManager.getLogger(OperatorResourceUtils.class);

    // -------------
    // Client
    // -------------
    private static NonNamespaceOperation<Operator, OperatorList, Resource<Operator>> operatorClient() {
        return KubeResourceManager.getKubeClient().getClient().adapt(OpenShiftClient.class).operatorHub().operators();
    }

    // -------------
    // Delete
    // -------------
    public static void deleteOperator(Operator resource) {
        CommonUtils.waitFor("Operator " + resource.getMetadata().getName() + " to be deleted", TimeConstants.GLOBAL_POLL_INTERVAL_MEDIUM, TimeConstants.GLOBAL_TIMEOUT,
            () -> {
                Operator operator = getOperator(resource.getMetadata().getName());
                if (operator != null) {
                    operatorClient().withName(resource.getMetadata().getName()).delete();
                    return false;
                }
                return true;
            },
            () -> LOGGER.warn("{}", list()));
    }

    public static void delete(String namespace, String name) {
        operatorClient().withName(name + "." + namespace).delete();
    }

    public static void deleteIfExists(String namespace, String name) {
        Operator operator = getOperator(namespace, name);
        if (operator != null) {
            LOGGER.info("Deleting Operator {}", operator.getMetadata().getName());
            deleteOperator(operator);
        }
    }

    // -------------
    // Get
    // -------------
    public Operator get(String name) {
        return operatorClient().withName(name).get();
    }

    public static Operator getOperator(String name) {
        return operatorClient().withName(name).get();
    }

    public static Operator getOperator(String namespace, String name) {
        return getOperatorByNamespaceAndName(namespace, name);
    }

    public static Operator getOperatorByNamespaceAndName(String namespace, String name) {
        return operatorClient().withName(name + "." + namespace).get();
    }

    // -------------
    // List
    // -------------
    public static List<Operator> list() {
        return operatorClient().list().getItems();
    }
}
