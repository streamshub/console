package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.resourcetypes.ConsoleType;
import com.github.streamshub.systemtests.resourcetypes.KafkaTopicType;
import com.github.streamshub.systemtests.resourcetypes.KafkaType;
import com.github.streamshub.systemtests.resourcetypes.KafkaUserType;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.skodjob.testframe.resources.ClusterRoleBindingType;
import io.skodjob.testframe.resources.ClusterRoleType;
import io.skodjob.testframe.resources.ConfigMapType;
import io.skodjob.testframe.resources.CustomResourceDefinitionType;
import io.skodjob.testframe.resources.DeploymentType;
import io.skodjob.testframe.resources.InstallPlanType;
import io.skodjob.testframe.resources.JobType;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.NamespaceType;
import io.skodjob.testframe.resources.OperatorGroupType;
import io.skodjob.testframe.resources.RoleBindingType;
import io.skodjob.testframe.resources.SecretType;
import io.skodjob.testframe.resources.ServiceAccountType;
import io.skodjob.testframe.resources.ServiceType;
import io.skodjob.testframe.resources.SubscriptionType;
import io.skodjob.testframe.utils.KubeUtils;
import org.apache.logging.log4j.Logger;
import org.slf4j.event.Level;

import java.io.IOException;

public class SetupUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(SetupUtils.class);
    /**
     * Initializes the testing tools and Kubernetes resource manager for system tests.
     *
     * <p>This method ensures that resource types, callbacks, and logging
     * configurations are set up only once, at the start of test execution.
     * Calling it multiple times has no effect after the first invocation.</p>
     *
     * <p>Unlike placing this logic in a {@code static {}} block, this method
     * avoids executing prematurely when the test listener scans class names
     * and methods, preventing unwanted logging or side effects before actual
     * test cases run.</p>
     *
     * <p>Specifically, it:</p>
     * <ul>
     *   <li>Registers all custom Kubernetes resource types used in the tests.</li>
     *   <li>Adds a callback to label newly created namespaces for log collection.</li>
     *   <li>Configures the path for storing YAML representations of resources.</li>
     *   <li>Logs the environment configuration and saves it to a file.</li>
     * </ul>
     */
    public static void initializeSystemTests() {
        KubeResourceManager.get().setResourceTypes(
            new CustomResourceDefinitionType(),
            new ClusterRoleBindingType(),
            new ClusterRoleType(),
            new ConfigMapType(),
            new ConsoleType(),
            new DeploymentType(),
            new InstallPlanType(),
            new JobType(),
            new KafkaType(),
            new KafkaTopicType(),
            new KafkaUserType(),
            new NamespaceType(),
            new OperatorGroupType(),
            new RoleBindingType(),
            new SecretType(),
            new ServiceAccountType(),
            new ServiceType(),
            new SubscriptionType());

        KubeResourceManager.get().addCreateCallback(resource -> {
            // Set collect label for every namespace created with TF
            if (resource.getKind().equals(HasMetadata.getKind(Namespace.class))) {
                KubeUtils.labelNamespace(resource.getMetadata().getName(), Labels.COLLECT_ST_LOGS, "true");
            }
        });

        // Allow storing YAML files
        KubeResourceManager.get().setStoreYamlPath(Environment.TEST_LOG_DIR);

        try {
            Environment.logConfigAndSaveToFile();
        } catch (IOException e) {
            LOGGER.error("Saving of config env file failed");
        }
    }

    public static void cleanupIfNeeded() {
        if (Environment.CLEANUP_ENVIRONMENT) {
            KubeResourceManager.get().printCurrentResources(Level.DEBUG);
            KubeResourceManager.get().deleteResources(false);
        }
    }
}
