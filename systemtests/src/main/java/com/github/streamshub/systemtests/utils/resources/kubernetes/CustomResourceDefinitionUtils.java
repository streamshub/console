package com.github.streamshub.systemtests.utils.resources.kubernetes;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;


public class CustomResourceDefinitionUtils {

    private static final Logger LOGGER = LogManager.getLogger(CustomResourceDefinitionUtils.class);

    // -------------
    // Client
    // -------------
    private static MixedOperation<CustomResourceDefinition, CustomResourceDefinitionList, Resource<CustomResourceDefinition>> customResourceDefinitionClient() {
        return KubeResourceManager.getKubeClient().getClient().resources(CustomResourceDefinition.class, CustomResourceDefinitionList.class);
    }

    // -------------
    // Get
    // -------------
    public static List<CustomResourceDefinition> getCrdsByContains(String namePrefix) {
        return customResourceDefinitionClient().list().getItems().stream().filter(p -> p.getMetadata().getName().contains(namePrefix)).toList();
    }

    public static List<CustomResourceDefinition> getCrdsByKind(String kind) {
        return customResourceDefinitionClient().list().getItems().stream().filter(p -> p.getSpec().getNames().getKind().equals(kind)).toList();
    }

    // -------------
    // Delete
    // -------------
    public static void deleteCrd(CustomResourceDefinition resource) {
        customResourceDefinitionClient().resource(resource).delete();
    }

    public static void deleteIfExists(String containsName) {
        List<CustomResourceDefinition> crds = CustomResourceDefinitionUtils.getCrdsByContains(containsName);
        for (CustomResourceDefinition crd : crds) {
            CustomResourceDefinitionUtils.deleteCrd(crd);
        }
    }
}
