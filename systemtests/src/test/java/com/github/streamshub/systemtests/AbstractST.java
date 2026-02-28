package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.interfaces.BucketMethodsOrderRandomizer;
import com.github.streamshub.systemtests.interfaces.ExtensionContextParameterResolver;
import com.github.streamshub.systemtests.setup.console.ConsoleOperatorSetup;
import com.github.streamshub.systemtests.setup.strimzi.StrimziOperatorSetup;
import com.github.streamshub.systemtests.utils.SetupUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import io.skodjob.testframe.annotations.ResourceManager;
import io.skodjob.testframe.annotations.TestVisualSeparator;
import io.skodjob.testframe.resources.KubeResourceManager;
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
@ExtendWith({TestExecutionWatcher.class})
@ExtendWith(ExtensionContextParameterResolver.class)
@TestMethodOrder(BucketMethodsOrderRandomizer.class)
public abstract class AbstractST {
    private static boolean initialized = false;
    // Operators
    protected final StrimziOperatorSetup strimziOperatorSetup = new StrimziOperatorSetup(Constants.CO_NAMESPACE);
    protected final ConsoleOperatorSetup consoleOperatorSetup = new ConsoleOperatorSetup(Constants.CO_NAMESPACE);

    @BeforeAll
    void setupTestSuite(ExtensionContext extensionContext) {
        if (!initialized) {
            SetupUtils.initializeSystemTests();
            initialized = true;
        }

        KubeResourceManager.get().setTestContext(extensionContext);
        NamespaceUtils.prepareNamespace(Constants.CO_NAMESPACE);
        strimziOperatorSetup.install();
        //consoleOperatorSetup.install();
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
