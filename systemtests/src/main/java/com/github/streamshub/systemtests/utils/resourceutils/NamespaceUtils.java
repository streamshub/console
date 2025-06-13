package com.github.streamshub.systemtests.utils.resourceutils;

import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;

public class NamespaceUtils {

    private NamespaceUtils() {}

    public static void prepareNamespace(String namespaceName) {
        Namespace namespace = ResourceUtils.getKubeResource(Namespace.class, namespaceName);
        if (namespace == null) {
            KubeResourceManager.get().createResourceWithWait(
                new NamespaceBuilder()
                    .withNewMetadata()
                        .withName(namespaceName)
                    .endMetadata()
                .build());
        }
    }
}
