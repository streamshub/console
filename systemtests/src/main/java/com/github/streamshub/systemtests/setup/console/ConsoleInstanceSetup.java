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
import com.github.streamshub.systemtests.utils.KafkaUtils;
import com.github.streamshub.systemtests.utils.ResourceUtils;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.TestFrameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConsoleInstanceSetup {
    private static final Logger LOGGER = LogManager.getLogger(ConsoleInstanceSetup.class);
    private final String deploymentNamespace;
    private final String instanceName;
    private final String kafkaClusterName;

    public ConsoleInstanceSetup(String namespace, String kafkaClusterName) {
        this.deploymentNamespace = namespace;
        this.instanceName = Constants.CONSOLE_INSTANCE + "-" + Utils.hashStub(namespace);
        this.kafkaClusterName = kafkaClusterName;
    }

    public void deploy() {
        LOGGER.info("----------- Deploy Console Instance -----------");
        if (ResourceUtils.getKubeResource(Deployment.class, deploymentNamespace, instanceName) != null) {
            LOGGER.warn("Skipping Console Instance deployment. It is already deployed");
            return;
        }

        KubeResourceManager.get().createResourceWithWait(getDefaultConsoleInstance());
        WaitUtils.waitForDeploymentWithPrefixIsReady(deploymentNamespace, instanceName);
        LOGGER.info("Console deployed and available at {}", ConsoleUtils.getConsoleUiUrl(deploymentNamespace, instanceName, true));
    }

    private Console getDefaultConsoleInstance() {
        ConsoleBuilder builder = new ConsoleBuilder(TestFrameUtils.configFromYaml(ExampleFiles.EXAMPLE_CONSOLE_INSTANCE, Console.class))
            .withMetadata(new ObjectMetaBuilder()
                .withName(instanceName)
                .withNamespace(deploymentNamespace)
                .build())
            .withSpec(new ConsoleSpecBuilder()
                .withHostname(instanceName + "." + ClusterUtils.getDefaultClusterHostname())
                .withKafkaClusters(
                    new KafkaClusterBuilder()
                        .withId(kafkaClusterName)
                        .withName(kafkaClusterName)
                        .withListener(Constants.SECURE_LISTENER_NAME)
                        .withNamespace(deploymentNamespace)
                        .withNewCredentials()
                            .withNewKafkaUser()
                                .withName(KafkaUtils.kafkaUserName(kafkaClusterName))
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
