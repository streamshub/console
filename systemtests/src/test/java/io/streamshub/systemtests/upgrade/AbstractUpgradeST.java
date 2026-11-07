package io.streamshub.systemtests.upgrade;

import io.streamshub.systemtests.ResetTracingExtension;
import io.streamshub.systemtests.TestExecutionWatcher;
import io.streamshub.systemtests.constants.Constants;
import io.streamshub.systemtests.interfaces.BucketMethodsOrderRandomizer;
import io.streamshub.systemtests.interfaces.ExtensionContextParameterResolver;
import io.streamshub.systemtests.logs.LogWrapper;
import io.streamshub.systemtests.setup.strimzi.StrimziOperatorSetup;
import io.streamshub.systemtests.utils.SetupUtils;
import io.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import io.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import io.skodjob.kubetest4j.annotations.ResourceManager;
import io.skodjob.kubetest4j.annotations.TestVisualSeparator;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;
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
class AbstractUpgradeST {
    private static final Logger LOGGER = LogWrapper.getLogger(AbstractUpgradeST.class);
    private static boolean initialized = false;
    protected final StrimziOperatorSetup strimziOperatorSetup = new StrimziOperatorSetup(Constants.CO_NAMESPACE);

    // Topics - shared by OlmUpgradeST and YamlUpgradeST, both pre-create the same 17 topics
    // (12 fully replicated, 3 under-replicated, 2 unavailable) to verify against before/after their upgrade.
    protected static final int REPLICATED_TOPICS_COUNT = 17;
    protected static final int UNMANAGED_REPLICATED_TOPICS_COUNT = 0;
    protected static final int TOTAL_REPLICATED_TOPICS_COUNT = REPLICATED_TOPICS_COUNT + UNMANAGED_REPLICATED_TOPICS_COUNT;
    protected static final int UNDER_REPLICATED_TOPICS_COUNT = 0;
    protected static final int UNAVAILABLE_TOPICS_COUNT = 0;
    protected static final int TOTAL_TOPICS_COUNT = TOTAL_REPLICATED_TOPICS_COUNT + UNDER_REPLICATED_TOPICS_COUNT + UNAVAILABLE_TOPICS_COUNT;

    @BeforeAll
    void setupTestSuite(ExtensionContext extensionContext) {
        if (!initialized) {
            SetupUtils.initializeSystemTests();
            initialized = true;
        }

        KubeResourceManager.get().setTestContext(extensionContext);
        NamespaceUtils.prepareNamespace(Constants.CO_NAMESPACE);
        strimziOperatorSetup.install();
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
