package com.github.streamshub.systemtests.system;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.ResourceKinds;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.StrimziOperatorSetup;
import com.github.streamshub.systemtests.utils.resources.ClusterUtils;
import com.github.streamshub.systemtests.utils.resources.NamespaceUtils;
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

import static com.github.streamshub.systemtests.utils.resources.NamespaceUtils.createNamespaceAndPrepare;
import static com.github.streamshub.systemtests.utils.resources.NamespaceUtils.getNamespace;

@ResourceManager
@TestVisualSeparator
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(AbstractST.class);
    protected static final KubeResourceManager RESOURCE_MANAGER = KubeResourceManager.getInstance();
    // Operators
    protected final StrimziOperatorSetup strimziOperatorSetup = new StrimziOperatorSetup(Constants.CO_NAMESPACE);

    static {
        RESOURCE_MANAGER.setResourceTypes(
            new CustomResourceDefinitionType(),
            new ClusterRoleBindingType(),
            new ClusterRoleType(),
            new ConfigMapType(),
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

        RESOURCE_MANAGER.addCreateCallback(resource -> {
            // Set collect label for every namespace created with TF
            if (resource.getKind().equalsIgnoreCase(ResourceKinds.NAMESPACE)) {
                KubeUtils.labelNamespace(resource.getMetadata().getName(), Labels.COLLECT_ST_LOGS, "true");
            }
        });

        // Allow storing yaml files
        KubeResourceManager.setStoreYamlPath(Environment.TEST_LOG_DIR);

        try {
            Environment.saveConfigToFile();
        } catch (IOException e) {
            LOGGER.error("Saving of config env file failed");
        }
    }

    @BeforeAll
    void setupTestSuite() {
        LOGGER.info("=========== AbstractST - BeforeAll - Setup TestSuite ===========");
        if (NamespaceUtils.getNamespace(Constants.CO_NAMESPACE) == null) {
            createNamespaceAndPrepare(Constants.CO_NAMESPACE);
        }
        strimziOperatorSetup.setup();
    }

    @BeforeEach
    void setupTestCase() {
        LOGGER.info("=========== AbstractST - BeforeEach - Setup TestCase {} ===========", KubeResourceManager.getTestContext().getTestMethod());
        ClusterUtils.checkClusterHealth();
    }

    @AfterAll
    void tearDownTestSuite() {
        LOGGER.info("=========== AbstractST - AfterAll - Tear down the TestSuite ===========");
        if (Environment.SKIP_TEARDOWN) {
            LOGGER.warn("Teardown was skipped because of SKIP_TEARDOWN env");
            return;
        }

        // If Strimzi was installed externally, keep it and the namespace it's deployed in.
        if (!Environment.SKIP_STRIMZI_INSTALLATION) {
            KubeResourceManager.getInstance().deleteResource(getNamespace(Constants.CO_NAMESPACE));
            strimziOperatorSetup.teardown();
        }
    }
}
