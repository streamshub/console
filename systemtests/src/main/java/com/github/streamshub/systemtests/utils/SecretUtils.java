package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.logs.LogWrapper;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

public class SecretUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(SecretUtils.class);

    public static void createSecret(String namespaceName, String secretName, String dataKey, String dataValue) {
        LOGGER.info("Creating Secret: {}/{}", namespaceName, secretName);
        KubeResourceManager.get().createResourceWithWait(new SecretBuilder()
            .withApiVersion("v1")
            .withKind("Secret")
            .withNewMetadata()
                .withName(secretName)
                .withNamespace(namespaceName)
            .endMetadata()
            .withType("Opaque")
                .withStringData(Collections.singletonMap(dataKey, dataValue))
            .build());
    }
}
