package com.github.streamshub.systemtests.interfaces;

import com.github.streamshub.systemtests.SystemTestExecutionListener;
import com.github.streamshub.systemtests.annotations.SetupTestBucket;
import com.github.streamshub.systemtests.annotations.TestBucket;
import com.github.streamshub.systemtests.exceptions.SetupException;
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

public class TestBucketExtension implements BeforeTestExecutionCallback, AfterTestExecutionCallback {
    private static final Logger LOGGER = LogWrapper.getLogger(TestBucketExtension.class);
    private static final Map<String, Integer> TOTAL_ANNOTATED_TEST_COUNT_MAP = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> EXECUTED_ANNOTATED_TEST_COUNT_MAP = new ConcurrentHashMap<>();

    // Used to store context of the method which was annotated as a setup - this helps clean up after all the tests finish
    private static final Map<String, ExtensionContext> TEST_BUCKET_SETUP_CONTEXT_MAP = new ConcurrentHashMap<>();

    /**
     * Prepares shared resources before executing a test method annotated with {@link TestBucket}.
     *
     * <p>If the current test is the first one in its {@code @TestBucket} group, this method will:</p>
     * <ul>
     *   <li>Initialize execution counters for the group.</li>
     *   <li>Collect all test methods in the same class that share the same bucket and are scheduled to run.</li>
     *   <li>Determine the total number of planned invocations, including parameterized test cases.</li>
     *   <li>Create and register a dedicated extension context for the group in {@link KubeResourceManager}.</li>
     *   <li>Invoke the setup method annotated with {@link SetupTestBucket} to provision the shared resources.</li>
     * </ul>
     *
     * <p>Subsequent tests in the same group skip the setup step and reuse the resources
     * provisioned during the first execution.</p>
     *
     * @param testMethodContext the JUnit 5 {@link ExtensionContext} for the current test method
     * @throws Exception if reflective invocation of the setup method fails
     */
    @Override
    public void beforeTestExecution(ExtensionContext testMethodContext) throws Exception {
        TestBucket testAnnotation = testMethodContext.getRequiredTestMethod().getAnnotation(TestBucket.class);

        if (testAnnotation == null) {
            return;
        }

        String testBucketGroupName = testAnnotation.value();
        LOGGER.info("Shared resources beforeTestExecution - @TestBucket({})", testBucketGroupName);

        // Init executed test count to 0
        EXECUTED_ANNOTATED_TEST_COUNT_MAP.putIfAbsent(testBucketGroupName, new AtomicInteger(0));

        // First test = setup shared resources
        if (EXECUTED_ANNOTATED_TEST_COUNT_MAP.get(testBucketGroupName).get() == 0) {
            LOGGER.info("Setup shared resources for TestBucket [{}]", testBucketGroupName);
            // Gather count of ALL the tests annotated with the same @TestBucket within the same class that are about to be executed in this run
            List<Method> methodsInSameClassWithCurrentAnno = Arrays.stream(testMethodContext.getRequiredTestClass().getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(TestBucket.class))
                .filter(method -> method.getAnnotation(TestBucket.class).value().equals(testBucketGroupName))
                .filter(method -> SystemTestExecutionListener.TESTS_TO_BE_EXECUTED.contains(testMethodContext.getRequiredTestClass().getName() + "." + method.getName()))
                .toList();

            // Count normal tests + expand parameterized tests
            int totalTestRuns = methodsInSameClassWithCurrentAnno.stream()
                .mapToInt(method -> countPlannedInvocations(method, testMethodContext))
                .sum();

            TOTAL_ANNOTATED_TEST_COUNT_MAP.putIfAbsent(testBucketGroupName, totalTestRuns);

            // Store a new context directed towards setup method for a cleanup later
            TestBucketExtensionContext setupMethodContext = new TestBucketExtensionContext(testMethodContext, testBucketGroupName);
            TEST_BUCKET_SETUP_CONTEXT_MAP.put(testBucketGroupName, setupMethodContext);
            KubeResourceManager.get().setTestContext(setupMethodContext);

            // Call setup method
            for (Method method : testMethodContext.getRequiredTestClass().getDeclaredMethods()) {
                if (method.isAnnotationPresent(SetupTestBucket.class) &&
                    method.getAnnotation(SetupTestBucket.class).value().equals(testBucketGroupName)) {
                    // Called method needs to be either explicitly public or needs to be -> `method.setAccessible(true)`
                    method.invoke(testMethodContext.getRequiredTestInstance());
                }
            }
        }
    }

    /**
     * Calculates how many times a given test method will run, supporting both
     * standard and parameterized JUnit 5 tests.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>Tests without {@link MethodSource} are counted as a single run.</li>
     *   <li>For {@code @MethodSource}-annotated tests, the source methods are resolved
     *       reflectively to determine the number of provided arguments.</li>
     *   <li>Supported source return types:
     *     <ul>
     *       <li>{@link Stream} — counted by consuming the stream.</li>
     *       <li>{@link Collection} — counted by its size.</li>
     *     </ul>
     *   </li>
     *   <li>If no values are found, defaults to one run.</li>
     *   <li>Unsupported return types or resolution errors result in a {@link RuntimeException}.</li>
     * </ul>
     *
     * <p>This is mainly used to track planned invocations when aggregating test runs
     * for testBucket groups.</p>
     *
     * @param testMethod the method under inspection
     * @param testMethodContext the JUnit {@link ExtensionContext} providing test instance access
     * @return the expected number of invocations for the method
     * @throws RuntimeException if a {@code @MethodSource} cannot be resolved or is invalid
     */
    private int countPlannedInvocations(Method testMethod, ExtensionContext testMethodContext) {
        if (!testMethod.isAnnotationPresent(MethodSource.class)) {
            // Normal test method
            return 1;
        }

        int invocationCount = Arrays.stream(testMethod.getAnnotation(MethodSource.class).value())
            .mapToInt(sourceName -> {
                try {
                    Method sourceMethod = testMethod.getDeclaringClass().getDeclaredMethod(sourceName);
                    // Use only if method is not explicitly public
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
                    throw new SetupException("Failed to resolve @MethodSource: " + sourceName, e);
                }
            })
            .sum();
        // Fall back to 1 if no values
        return Math.max(1, invocationCount);
    }

    /**
     * Cleans up shared resources after testBucket execution finishes, ensuring teardown happens
     * only once all tests in the same {@link TestBucket} group have completed.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>Tests without {@link TestBucket} are ignored.</li>
     *   <li>For annotated tests, the group’s executed count is incremented.</li>
     *   <li>If more tests remain in the group, no action is taken.</li>
     *   <li>If this was the last test:
     *     <ul>
     *       <li>The stored setup context for the group is restored.</li>
     *       <li>{@link KubeResourceManager} is switched to that context.</li>
     *       <li>All resources created for the group are deleted.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>This ensures efficient reuse of shared resources across tests while
     * guaranteeing proper cleanup once they are no longer needed.</p>
     *
     * @param context the JUnit {@link ExtensionContext} for the executed test
     */
    @Override
    public void afterTestExecution(ExtensionContext context) {
        TestBucket testAnnotation = context.getRequiredTestMethod().getAnnotation(TestBucket.class);

        if (testAnnotation == null) {
            return;
        }

        String testBucketGroupName = testAnnotation.value();
        LOGGER.info("Shared resources afterTestExecution - @TestBucket({})", testBucketGroupName);

        int executedTestCount = EXECUTED_ANNOTATED_TEST_COUNT_MAP.get(testBucketGroupName).incrementAndGet();
        int totalTestCount = TOTAL_ANNOTATED_TEST_COUNT_MAP.get(testBucketGroupName);

        if (executedTestCount == totalTestCount) {
            LOGGER.info("Teardown shared resources for TestBucket [{}]", testBucketGroupName);
            ExtensionContext setupContext = TEST_BUCKET_SETUP_CONTEXT_MAP.get(testBucketGroupName);

            if (setupContext != null) {
                KubeResourceManager.get().setTestContext(setupContext);
                KubeResourceManager.get().deleteResources(false);
            }
        }
    }
}
