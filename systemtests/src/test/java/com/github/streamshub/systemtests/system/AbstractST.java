package com.github.streamshub.systemtests.system;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.resources.ConsoleType;
import com.github.streamshub.systemtests.setup.ConsoleOperatorSetup;
import com.github.streamshub.systemtests.setup.StrimziOperatorSetup;
import com.github.streamshub.systemtests.utils.ClusterUtils;
import com.github.streamshub.systemtests.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.annotations.TestVisualSeparator;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

@ResourceManager
@TestVisualSeparator
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(AbstractST.class);
    // Operators
    protected final StrimziOperatorSetup strimziOperatorSetup = new StrimziOperatorSetup(Constants.CO_NAMESPACE);
    protected final ConsoleOperatorSetup consoleOperatorSetup = new ConsoleOperatorSetup(Constants.CO_NAMESPACE);

    static {
        KubeResourceManager.get().setResourceTypes(
            new CustomResourceDefinitionType(),
            new ClusterRoleBindingType(),
            new ClusterRoleType(),
            new ConfigMapType(),
            new ConsoleType(),
            new DeploymentType(),
            new InstallPlanType(),
            new JobType(),
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
            Environment.saveConfigToFile();
        } catch (IOException e) {
            LOGGER.error("Saving of config env file failed");
        }
    }

    @BeforeAll
    void setupTestSuite() {
        LOGGER.info("=========== AbstractST - BeforeAll - Setup TestSuite ===========");
        if (ResourceUtils.getKubeResource(Namespace.class, Constants.CO_NAMESPACE) == null) {
            KubeResourceManager.get().createOrUpdateResourceWithWait(new NamespaceBuilder().withNewMetadata().withName(Constants.CO_NAMESPACE).endMetadata().build());
        }
        strimziOperatorSetup.setup();
        consoleOperatorSetup.setup();
    }

    @BeforeEach
    void setupTestCase() {
        LOGGER.info("=========== AbstractST - BeforeEach - Setup TestCase {} ===========", KubeResourceManager.get().getTestContext().getTestMethod());
        ClusterUtils.checkClusterHealth();
    }

    @AfterAll
    void tearDownTestSuite() {
        LOGGER.info("=========== AbstractST - AfterAll - Tear down the TestSuite ===========");
        if (Environment.SKIP_TEARDOWN) {
            LOGGER.warn("Teardown was skipped because of SKIP_TEARDOWN env");
            return;
        }
    }
}
