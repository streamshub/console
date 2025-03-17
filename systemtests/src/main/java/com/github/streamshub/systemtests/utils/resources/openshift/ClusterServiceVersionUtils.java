package com.github.streamshub.systemtests.utils.resources.openshift;

import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.utils.CommonUtils;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.ClusterServiceVersion;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.ClusterServiceVersionList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ClusterServiceVersionUtils {

    private static final Logger LOGGER = LogManager.getLogger(ClusterServiceVersionUtils.class);

    // -------------
    // Client
    // -------------
    private static MixedOperation<ClusterServiceVersion, ClusterServiceVersionList, Resource<ClusterServiceVersion>> clusterServiceVersionClient() {
        return KubeResourceManager.getKubeClient().getClient().adapt(OpenShiftClient.class).operatorHub().clusterServiceVersions();
    }

    // -------------
    // Get
    // -------------
    public static ClusterServiceVersion getCsv(String namespace, String name) {
        return clusterServiceVersionClient().inNamespace(namespace).withName(name).get();
    }

    // -------------
    // List
    // -------------
    public static List<ClusterServiceVersion> list(String namespace) {
        return clusterServiceVersionClient().inNamespace(namespace).list().getItems();
    }

    // -------------
    // Delete
    // -------------
    public static void deleteCsv(ClusterServiceVersion resource) {
        clusterServiceVersionClient().inNamespace(resource.getMetadata().getNamespace()).delete();
        CommonUtils.waitFor("CSV to be deleted", TimeConstants.GLOBAL_POLL_INTERVAL, TimeConstants.CO_OPERATION_TIMEOUT_MEDIUM,
            () -> getCsv(resource.getMetadata().getNamespace(), resource.getMetadata().getName()) == null,
            () -> LOGGER.warn("{}", list(resource.getMetadata().getNamespace())));
    }

    public static void deleteByPrefix(String namespace, String namePrefix) {
        clusterServiceVersionClient().inNamespace(namespace).resource(clusterServiceVersionClient().inNamespace(namespace).list().getItems().stream()
                .filter(p -> p.getMetadata().getName().startsWith(namePrefix))
                .toList().get(0)).delete();
    }

    public static void deleteIfExists(String namespace, String namePrefix) {
        List<ClusterServiceVersion> csv = clusterServiceVersionClient().inNamespace(namespace).list().getItems().stream()
                .filter(p -> p.getMetadata().getName().startsWith(namePrefix))
                .toList();

        if (!csv.isEmpty()) {
            LOGGER.info("Deleting CSV {}", csv.get(0).getMetadata().getName());
            deleteCsv(csv.get(0));
        }
    }
}
