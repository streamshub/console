package com.github.streamshub.systemtests.utils.resources.openshift;

import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.utils.CommonUtils;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.InstallPlan;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.InstallPlanList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class InstallPlanUtils {

    private static final Logger LOGGER = LogManager.getLogger(InstallPlanUtils.class);

    // -------------
    // Client
    // -------------
    private static MixedOperation<InstallPlan, InstallPlanList, Resource<InstallPlan>> installPlanClient() {
        return KubeResourceManager.getKubeClient().getClient().adapt(OpenShiftClient.class).operatorHub().installPlans();
    }

    // -------------
    // Get
    // -------------
    public static InstallPlan getInstallPlan(String namespace, String name) {
        return installPlanClient().inNamespace(namespace).withName(name).get();
    }

    // -------------
    // List
    // -------------
    public static List<InstallPlan> list(String namespace) {
        return installPlanClient().inNamespace(namespace).list().getItems();
    }

    // -------------
    // Delete
    // -------------
    public static void deleteInstallPlan(String namespace, String name) {
        InstallPlan ip = getInstallPlan(namespace, name);
        deleteInstallPlan(ip);
    }

    public static void deleteInstallPlan(InstallPlan resource) {
        installPlanClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).delete();
        CommonUtils.waitFor("InstallPlan to be deleted", TimeConstants.GLOBAL_POLL_INTERVAL, TimeConstants.CO_OPERATION_TIMEOUT_MEDIUM,
            () -> getInstallPlan(resource.getMetadata().getNamespace(), resource.getMetadata().getName()) == null,
            () -> LOGGER.warn("{}", list(resource.getMetadata().getNamespace())));
    }

    public static void deleteInstallPlanByCsvPrefixIfExists(String namespace, String csvNamePrefix) {
        for (InstallPlan ip : list(namespace)) {
            if (ip.getSpec().getClusterServiceVersionNames().get(0).startsWith(csvNamePrefix)) {
                LOGGER.info("Deleting InstallPlan {}/{}", ip.getMetadata().getNamespace(), ip.getMetadata().getName());
                InstallPlanUtils.deleteInstallPlan(ip);
            }
        }
    }
}


