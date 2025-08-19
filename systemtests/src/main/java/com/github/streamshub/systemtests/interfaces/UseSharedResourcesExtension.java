package com.github.streamshub.systemtests.interfaces;

import com.github.streamshub.systemtests.SystemTestExecutionListener;
import com.github.streamshub.systemtests.annotations.SetupSharedResources;
import com.github.streamshub.systemtests.annotations.UseSharedResources;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UseSharedResourcesExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final Logger LOGGER = LogWrapper.getLogger(UseSharedResourcesExtension.class);
    private static final Map<String, Integer> TOTAL_ANNOTATED_TEST_COUNT_MAP = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> EXECUTED_ANNOTATED_TEST_COUNT_MAP = new ConcurrentHashMap<>();

    // Used to store context of the method which was annotated as a setup - this helps clean up after all the tests finish
    private static final Map<String, ExtensionContext> SHARED_RESOURCES_SETUP_CONTEXT_MAP = new ConcurrentHashMap<>();

    private static final Map<String, Object[]> SHARED_SETUP_RESULTS = new ConcurrentHashMap<>();
    /**
     * Serves as @BeforeEach executed as the last @BeforeEach before the test runs
     */
    @Override
    public void beforeTestExecution(ExtensionContext testMethodContext) throws Exception {
        UseSharedResources testAnnotation = testMethodContext.getRequiredTestMethod().getAnnotation(UseSharedResources.class);

        if (testAnnotation == null) {
            return;
        }

        String sharedResourceContextName = testAnnotation.value();
        LOGGER.info("Shared resources - @ReuseSharedResources({})", sharedResourceContextName);

        // Gather count of ALL the tests annotated with the same @UseSharedResources within the same class that are about to be executed in this run
        List<Method> methodsInSameClassWithCurrentAnno = Arrays.stream(testMethodContext.getRequiredTestClass().getDeclaredMethods())
            .filter(method -> method.isAnnotationPresent(UseSharedResources.class))
            .filter(method -> SystemTestExecutionListener.TESTS_TO_BE_EXECUTED.contains(testMethodContext.getRequiredTestClass().getName() + "." + method.getName()))
            .toList();

        TOTAL_ANNOTATED_TEST_COUNT_MAP.putIfAbsent(sharedResourceContextName, methodsInSameClassWithCurrentAnno.size());

        // Init executed test count to 0
        EXECUTED_ANNOTATED_TEST_COUNT_MAP.putIfAbsent(sharedResourceContextName, new AtomicInteger(0));

        // Store a new context directed towards setup method for a cleanup later
        SharedSetupExtensionContext setupMethodContext = new SharedSetupExtensionContext(testMethodContext, sharedResourceContextName);
        SHARED_RESOURCES_SETUP_CONTEXT_MAP.putIfAbsent(sharedResourceContextName, setupMethodContext);

        // First test = setup shared resources
        if (EXECUTED_ANNOTATED_TEST_COUNT_MAP.get(sharedResourceContextName).get() == 0) {
            invokeSetupMethod(setupMethodContext, sharedResourceContextName);
        }
    }

    /**
     * Main purpose is to keep track of executed tests and to cleanup after all tests with the same annotation finish
     * and their shared resources are no longer needed.
     */
    @Override
    public void afterTestExecution(ExtensionContext context) {
        UseSharedResources testAnnotation = context.getRequiredTestMethod().getAnnotation(UseSharedResources.class);

        if (testAnnotation == null) {
            return;
        }

        String sharedResourceContextName = testAnnotation.value();
        int executedTestCount = EXECUTED_ANNOTATED_TEST_COUNT_MAP.get(sharedResourceContextName).incrementAndGet();
        int totalTestCount = TOTAL_ANNOTATED_TEST_COUNT_MAP.get(sharedResourceContextName);

        if (executedTestCount == totalTestCount) {
            ExtensionContext setupContext = SHARED_RESOURCES_SETUP_CONTEXT_MAP.get(sharedResourceContextName);

            if (setupContext != null) {
                KubeResourceManager.get().setTestContext(setupContext);
                KubeResourceManager.get().deleteResources(false);
            }
        }
    }

    private void invokeSetupMethod(ExtensionContext extensionContext, String sharedResourceContextName, Object ...argst) throws Exception {
        for (Method method : extensionContext.getRequiredTestClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent(SetupSharedResources.class) &&
                method.getAnnotation(SetupSharedResources.class).value().equals(sharedResourceContextName)) {
                    // Called method needs to be either explicitly public or needs to be -> setAccessible
                    // method.setAccessible(true);
                    method.invoke(extensionContext.getRequiredTestInstance(), extensionContext, argst);
                }

        }
    }
}
