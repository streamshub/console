package com.github.streamshub.systemtests.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

public class SetupUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(SetupUtils.class);
    private SetupUtils() {}

    // ---------------
    // Yaml config
    // --------------
    public static String getYamlFileContent(String fileUrl) {
        LOGGER.debug("Loading YAML file content from url {}", fileUrl);
        StringBuilder content;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream(), StandardCharsets.UTF_8))) {
            content = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot download YAML content from url: " + fileUrl, e);
        }
        return content.toString();
    }

    public static <T> T configFromYaml(File yamlFile, Class<T> c) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(yamlFile, c);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // --------------
    // Namespace
    // --------------
    public static void copyImagePullSecrets(String targetNamespace) {
        if (!Environment.TEST_CLIENTS_IMAGE_PULL_SECRET.isEmpty()) {
            LOGGER.info("Checking if test clients pull secret: {} is in the default namespace", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET);
            if (ResourceUtils.getKubeResource(Secret.class, "default", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET) == null) {
                throw new RuntimeException(Environment.TEST_CLIENTS_IMAGE_PULL_SECRET + " pull secret is not in the default namespace! " +
                 "Tests would not be able to use Kafka clients to produce or consume messages");
            }

            // Check the target namespace does not have the pull secret already deployed
            if (ResourceUtils.getKubeResource(Secret.class, targetNamespace, Environment.TEST_CLIENTS_IMAGE_PULL_SECRET) != null) {
                LOGGER.warn("Pull secret {} is already present in the target namespace {}", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET, targetNamespace);
                return;
            }

            LOGGER.info("Copy pull secret {} to namespace {}", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET, targetNamespace);

            KubeResourceManager.get().createResourceWithWait(new SecretBuilder()
                .withApiVersion("v1")
                .withKind("Secret")
                .withNewMetadata()
                    .withName(Environment.TEST_CLIENTS_IMAGE_PULL_SECRET)
                    .withNamespace(targetNamespace)
                .endMetadata()
                .withType("kubernetes.io/dockerconfigjson")
                .withData(Collections.singletonMap(".dockerconfigjson",
                    ResourceUtils.getKubeResource(Secret.class, "default", Environment.TEST_CLIENTS_IMAGE_PULL_SECRET).getData().get(".dockerconfigjson")))
                .build());
        }
    }
}
