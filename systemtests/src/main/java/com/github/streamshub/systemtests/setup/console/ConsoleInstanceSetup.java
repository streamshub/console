package com.github.streamshub.systemtests.setup.console;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.ConsoleSpecBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.KafkaClusterBuilder;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.ExampleFiles;
import com.github.streamshub.systemtests.utils.ClusterUtils;
import com.github.streamshub.systemtests.utils.ConsoleUtils;
import com.github.streamshub.systemtests.utils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.ResourceUtils;
import com.github.streamshub.systemtests.utils.Utils;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.TestFrameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsoleInstanceSetup {
    private static final Logger LOGGER = LogManager.getLogger(ConsoleInstanceSetup.class);
    private ConsoleInstanceSetup() {}

    public static void setupIfNeeded(Console console) {
        LOGGER.info("----------- Deploy Console Instance -----------");
        if (ResourceUtils.getKubeResource(Deployment.class, console.getMetadata().getNamespace(), console.getMetadata().getName()) != null) {
            LOGGER.warn("Skipping Console Instance deployment. It is already deployed");
            return;
        }
        KubeResourceManager.get().createResourceWithWait(console);
        LOGGER.info("Console deployed and available at {}", ConsoleUtils.getConsoleUiUrl(console.getMetadata().getNamespace(), console.getMetadata().getName(), true));
    }

    public static Console getDefaultConsoleInstance(String namespaceName) {
        return getDefaultConsoleInstance(namespaceName,
        Constants.CONSOLE_INSTANCE + "-" + Utils.hashStub(namespaceName),
            KafkaNamingUtils.kafkaClusterName(namespaceName),
            KafkaNamingUtils.kafkaUserName(KafkaNamingUtils.kafkaClusterName(namespaceName)));
    }

    public static Console getDefaultConsoleInstance(String namespaceName, String instanceName, String kafkaName, String kafkaUserName) {
        ConsoleBuilder builder = new ConsoleBuilder(TestFrameUtils.configFromYaml(ExampleFiles.EXAMPLE_CONSOLE_INSTANCE, Console.class))
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

        return builder.build();
    }
}
