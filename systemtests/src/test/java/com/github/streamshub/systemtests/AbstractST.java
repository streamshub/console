package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.interfaces.ExtensionContextParameterResolver;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.resourcetypes.ConsoleType;
import com.github.streamshub.systemtests.resourcetypes.KafkaTopicType;
import com.github.streamshub.systemtests.resourcetypes.KafkaType;
import com.github.streamshub.systemtests.resourcetypes.KafkaUserType;
import com.github.streamshub.systemtests.setup.console.ConsoleOperatorSetup;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakConfig;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakSetup;
import com.github.streamshub.systemtests.setup.strimzi.StrimziOperatorSetup;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.event.Level;

import java.io.IOException;

@TestVisualSeparator
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ResourceManager(cleanResources = false)
@SuppressWarnings("ClassDataAbstractionCoupling")
@ExtendWith({TestExecutionWatcher.class})
@ExtendWith(ExtensionContextParameterResolver.class)
public abstract class AbstractST {
    private static final Logger LOGGER = LogWrapper.getLogger(AbstractST.class);
    // Operators
    protected final StrimziOperatorSetup strimziOperatorSetup = new StrimziOperatorSetup(Constants.CO_NAMESPACE);
    protected final ConsoleOperatorSetup consoleOperatorSetup = new ConsoleOperatorSetup(Constants.CO_NAMESPACE);

    // Keycloak
    private final KeycloakSetup keycloakSetup = new KeycloakSetup(Constants.KEYCLOAK_NAMESPACE);
    protected KeycloakConfig keycloakConfig;

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

    @BeforeAll
    void setupTestSuite(ExtensionContext extensionContext) {
        KubeResourceManager.get().setTestContext(extensionContext);
        NamespaceUtils.prepareNamespace(Constants.CO_NAMESPACE);
        strimziOperatorSetup.install();
        consoleOperatorSetup.install();
        keycloakConfig = keycloakSetup.setupKeycloakAndReturnConfig();
    }

    @BeforeEach
    void setupTestCase(ExtensionContext extensionContext) {
        KubeResourceManager.get().setTestContext(extensionContext);
        ClusterUtils.checkClusterHealth();
    }

    @AfterEach
    void teardownTestCase(ExtensionContext extensionContext) {
        KubeResourceManager.get().setTestContext(extensionContext);
        cleanupIfNeeded();
    }

    @AfterAll
    void teardownTestSuite(ExtensionContext extensionContext) {
        KubeResourceManager.get().setTestContext(extensionContext);
        cleanupIfNeeded();
    }

    protected void cleanupIfNeeded() {
        if (Environment.CLEANUP_ENVIRONMENT) {
            KubeResourceManager.get().printCurrentResources(Level.DEBUG);
            KubeResourceManager.get().deleteResources(false);
        }
    }
}
