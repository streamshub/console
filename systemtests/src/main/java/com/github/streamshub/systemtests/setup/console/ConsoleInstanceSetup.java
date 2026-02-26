package com.github.streamshub.systemtests.setup.console;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.ConsoleSpecBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.KafkaClusterBuilder;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.console.ConsoleUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsoleInstanceSetup {
    private static final Logger LOGGER = LogManager.getLogger(ConsoleInstanceSetup.class);
    private ConsoleInstanceSetup() {}

    public static void setupIfNeeded(Console console) {
        LOGGER.info("----------- Deploy Console Instance -----------");
        if (ResourceUtils.getKubeResource(Console.class, console.getMetadata().getNamespace(), console.getMetadata().getName()) != null) {
            LOGGER.warn("Skipping Console Instance deployment. It is already deployed");
            return;
        }
        KubeResourceManager.get().createResourceWithWait(console);
        LOGGER.info("Console deployed and available at {}", ConsoleUtils.getConsoleUiUrl(console.getMetadata().getNamespace(), console.getMetadata().getName(), true));
    }

    public static ConsoleBuilder getDefaultConsoleInstance(String namespaceName, String instanceName, String kafkaName, String kafkaUserName) {
        ConsoleBuilder builder = new ConsoleBuilder()
            .withMetadata(new ObjectMetaBuilder()
                .withName(instanceName)
                .withNamespace(namespaceName)
                .build())
            .withSpec(new ConsoleSpecBuilder()
                .withHostname(instanceName + "." + ClusterUtils.getClusterDomain())
                .withKafkaClusters(
                    new KafkaClusterBuilder()
                        .withId(kafkaName)
                        .withName(kafkaName)
                        .withListener(Constants.SECURE_LISTENER_NAME)
                        .withNamespace(namespaceName)
                        .withNewCredentials()
                            .withNewKafkaUser()
                                .withName(kafkaUserName)
                            .endKafkaUser()
                        .endCredentials()
                    .build()
                    )
                .build());

        if (!Environment.CONSOLE_API_IMAGE.isEmpty()) {
            builder = builder.editSpec()
                .withNewImages()
                    .withApi(Environment.CONSOLE_API_IMAGE)
                .endImages()
            .endSpec();
        }

        if (!Environment.CONSOLE_UI_IMAGE.isEmpty()) {
            builder = builder.editSpec()
                .editImages()
                    .withUi(Environment.CONSOLE_UI_IMAGE)
                .endImages()
            .endSpec();
        }

        return builder;
    }
}
