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
import org.junit.jupiter.params.provider.MethodSource;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

public class UseSharedResourcesExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final Logger LOGGER = LogWrapper.getLogger(UseSharedResourcesExtension.class);
    private static final Map<String, Integer> TOTAL_ANNOTATED_TEST_COUNT_MAP = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> EXECUTED_ANNOTATED_TEST_COUNT_MAP = new ConcurrentHashMap<>();

    // Used to store context of the method which was annotated as a setup - this helps clean up after all the tests finish
    private static final Map<String, ExtensionContext> SHARED_RESOURCES_SETUP_CONTEXT_MAP = new ConcurrentHashMap<>();

    /**
     * Serves as @BeforeEach executed as the last @BeforeEach before the test runs
     */
    @Override
    public void beforeTestExecution(ExtensionContext testMethodContext) throws Exception {
        UseSharedResources testAnnotation = testMethodContext.getRequiredTestMethod().getAnnotation(UseSharedResources.class);

        if (testAnnotation == null) {
            return;
        }

        String sharedResourceGroupName = testAnnotation.value();
        LOGGER.info("Shared resources beforeTestExecution - @ReuseSharedResources({})", sharedResourceGroupName);

        // Init executed test count to 0
        EXECUTED_ANNOTATED_TEST_COUNT_MAP.putIfAbsent(sharedResourceGroupName, new AtomicInteger(0));

        // First test = setup shared resources
        if (EXECUTED_ANNOTATED_TEST_COUNT_MAP.get(sharedResourceGroupName).get() == 0) {
            LOGGER.info("Setup shared resources for group [{}]", sharedResourceGroupName);
            // Gather count of ALL the tests annotated with the same @UseSharedResources within the same class that are about to be executed in this run
            List<Method> methodsInSameClassWithCurrentAnno = Arrays.stream(testMethodContext.getRequiredTestClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(UseSharedResources.class))
                .filter(method -> method.getAnnotation(UseSharedResources.class).value().equals(sharedResourceGroupName))
                .filter(method -> SystemTestExecutionListener.TESTS_TO_BE_EXECUTED.contains(testMethodContext.getRequiredTestClass().getName() + "." + method.getName()))
                .toList();

            // Count normal tests + expand parameterized tests
            int totalTestRuns = methodsInSameClassWithCurrentAnno.stream()
                .mapToInt(method -> countPlannedInvocations(method, testMethodContext))
                .sum();

            TOTAL_ANNOTATED_TEST_COUNT_MAP.putIfAbsent(sharedResourceGroupName, totalTestRuns);

            // Store a new context directed towards setup method for a cleanup later
            SharedSetupExtensionContext setupMethodContext = new SharedSetupExtensionContext(testMethodContext, sharedResourceGroupName);
            SHARED_RESOURCES_SETUP_CONTEXT_MAP.put(sharedResourceGroupName, setupMethodContext);
            KubeResourceManager.get().setTestContext(setupMethodContext);

            // Call setup method
            for (Method method : testMethodContext.getRequiredTestClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(SetupSharedResources.class) &&
                    method.getAnnotation(SetupSharedResources.class).value().equals(sharedResourceGroupName)) {
                    // Called method needs to be either explicitly public or needs to be -> `method.setAccessible(true)`
                    method.invoke(testMethodContext.getRequiredTestInstance());
                }
            }
        }
    }

    private int countPlannedInvocations(Method testMethod, ExtensionContext testMethodContext) {
        if (!testMethod.isAnnotationPresent(MethodSource.class)) {
            // Normal test method
            return 1;
        }

        int invocationCount = Arrays.stream(testMethod.getAnnotation(MethodSource.class).value())
            .mapToInt(sourceName -> {
                try {
                    Method sourceMethod = testMethod.getDeclaringClass().getDeclaredMethod(sourceName);
                    // sourceMethod.setAccessible(true);
                    Object value = sourceMethod.invoke(testMethodContext.getRequiredTestInstance());

                    if (value instanceof Stream<?> stream) {
                        return (int) stream.count();
                    } else if (value instanceof Collection<?> collection) {
                        return collection.size();
                    } else {
                        throw new IllegalStateException("Unsupported MethodSource return type: " + value);
                    }
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException("Failed to resolve @MethodSource: " + sourceName, e);
                }
            })
            .sum();
        // Fall back to 1 if no values
        return Math.max(1, invocationCount);
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

        String sharedResourceGroupName = testAnnotation.value();
        LOGGER.info("Shared resources afterTestExecution - @ReuseSharedResources({})", sharedResourceGroupName);

        int executedTestCount = EXECUTED_ANNOTATED_TEST_COUNT_MAP.get(sharedResourceGroupName).incrementAndGet();
        int totalTestCount = TOTAL_ANNOTATED_TEST_COUNT_MAP.get(sharedResourceGroupName);

        if (executedTestCount == totalTestCount) {
            LOGGER.info("Teardown shared resources for group [{}]", sharedResourceGroupName);
            ExtensionContext setupContext = SHARED_RESOURCES_SETUP_CONTEXT_MAP.get(sharedResourceGroupName);

            if (setupContext != null) {
                KubeResourceManager.get().setTestContext(setupContext);
                KubeResourceManager.get().deleteResources(false);
            }
        }
    }
}
