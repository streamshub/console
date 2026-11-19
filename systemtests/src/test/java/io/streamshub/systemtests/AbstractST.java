package io.streamshub.systemtests;

import io.streamshub.systemtests.constants.Constants;
import io.streamshub.systemtests.interfaces.BucketMethodsOrderRandomizer;
import io.streamshub.systemtests.interfaces.ExtensionContextParameterResolver;
import io.streamshub.systemtests.setup.console.ConsoleOperatorSetup;
import io.streamshub.systemtests.setup.strimzi.StrimziOperatorSetup;
import io.streamshub.systemtests.utils.SetupUtils;
import io.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import io.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import io.skodjob.kubetest4j.annotations.ResourceManager;
import io.skodjob.kubetest4j.annotations.TestVisualSeparator;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

@TestVisualSeparator
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ResourceManager(cleanResources = false)
@SuppressWarnings("ClassDataAbstractionCoupling")
@ExtendWith({TestExecutionWatcher.class, ResetTracingExtension.class})
@ExtendWith(ExtensionContextParameterResolver.class)
@TestMethodOrder(BucketMethodsOrderRandomizer.class)
public abstract class AbstractST {
    private static boolean initialized = false;
    // Operators
    protected StrimziOperatorSetup strimziOperatorSetup;
    protected ConsoleOperatorSetup consoleOperatorSetup;

    @BeforeAll
    void setupTestSuite(ExtensionContext extensionContext) {
        if (!initialized) {
            SetupUtils.initializeSystemTests();
            initialized = true;
        }

        KubeResourceManager.get().setTestContext(extensionContext);
        // Now ENV are set, install operators

        NamespaceUtils.prepareNamespace(Constants.CO_NAMESPACE);
        strimziOperatorSetup = new StrimziOperatorSetup(Constants.CO_NAMESPACE);
        consoleOperatorSetup = new ConsoleOperatorSetup(Constants.CO_NAMESPACE);

        strimziOperatorSetup.install();
        consoleOperatorSetup.install();
    }

    @BeforeEach
    void setupTestCase(ExtensionContext extensionContext) {
        KubeResourceManager.get().setTestContext(extensionContext);
        ClusterUtils.checkClusterHealth();
    }

    @AfterEach
    void teardownTestCase(ExtensionContext extensionContext) {
        KubeResourceManager.get().setTestContext(extensionContext);
        SetupUtils.cleanupIfNeeded();
    }

    @AfterAll
    void teardownTestSuite(ExtensionContext extensionContext) {
        KubeResourceManager.get().setTestContext(extensionContext);
        SetupUtils.cleanupIfNeeded();
    }
}
