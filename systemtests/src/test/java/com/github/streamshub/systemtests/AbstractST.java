package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.cluster.Kubernetes;
import com.github.streamshub.systemtests.resources.NamespaceManager;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;

@ResourceManager
@TestVisualSeparator
public class AbstractST {
    private static final Logger LOGGER = LogManager.getLogger(AbstractST.class);
    protected static final KubeResourceManager RESOURCE_MANAGER = KubeResourceManager.getInstance();
    protected static final NamespaceManager NAMESPACE_MANAGER = NamespaceManager.getInstance();
    protected static Kubernetes cluster = Kubernetes.getInstance();

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

        // Allow storing yaml files
        KubeResourceManager.setStoreYamlPath(Environment.TEST_LOG_DIR);
    }

    @BeforeAll
    static void beforeAll() {
        LOGGER.info("=========== AbstractST - BeforeAll ===========");
    }
}
