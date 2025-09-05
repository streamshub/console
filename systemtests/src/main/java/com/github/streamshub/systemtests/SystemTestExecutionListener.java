package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.annotations.TestBucket;
import com.github.streamshub.systemtests.logs.LogWrapper;
import org.apache.logging.log4j.Logger;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SystemTestExecutionListener implements TestExecutionListener {
    private static final Logger LOGGER = LogWrapper.getLogger(SystemTestExecutionListener.class);
    public static final List<String> TESTS_TO_BE_EXECUTED = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void testPlanExecutionStarted(TestPlan testPlan) {
        LOGGER.info("#".repeat(76));
        testPlan.getRoots().forEach(root -> {
            LOGGER.info("======================== TEST CLASSES TO BE EXECUTED ========================");
            LOGGER.info(
                testPlan.getDescendants(root)
                    .stream()
                    .filter(ti -> ti.isContainer() && !(ti.getSource().get() instanceof MethodSource))
                    .map(ti -> ti.getDisplayName())
                    .collect(Collectors.joining(", "))
            );

            LOGGER.info("========================= TEST CASES TO BE EXECUTED =========================");
            testPlan.getDescendants(root).forEach(testIdentifier -> {
                if (testIdentifier.isTest()) {
                    // Regular test
                    testIdentifier.getSource().ifPresent(source -> {
                        String testName = testIdentifier.getDisplayName().replace("()", "");
                        String className = ((MethodSource) source).getClassName();
                        String fullTestName = className + "." + testName;
                        LOGGER.info("{} {}", testName, getAnnotationGroupIfPresent(className, testName));
                        TESTS_TO_BE_EXECUTED.add(fullTestName);
                    });
                } else if (testIdentifier.isContainer() && testIdentifier.getSource().get() instanceof MethodSource) {
                    // Parametrized test
                    String testName = ((MethodSource) testIdentifier.getSource().get()).getMethodName();
                    String className = ((MethodSource) testIdentifier.getSource().get()).getClassName();
                    String fullTestName =  className + "." + testName;
                    LOGGER.info("{} {}", testName, getAnnotationGroupIfPresent(className, testName));
                    TESTS_TO_BE_EXECUTED.add(fullTestName);
                }
            });
        });
        LOGGER.info("#".repeat(76));
        LOGGER.info("======================= Total count of tests to be executed [{}] =================", TESTS_TO_BE_EXECUTED.size());
        TestExecutionListener.super.testPlanExecutionStarted(testPlan);
    }

    private String getAnnotationGroupIfPresent(String className, String methodName) {
        String groupSuffix = "";
        Method method = null;

        try {
            method = Arrays.stream(Class.forName(className).getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst()
                .orElse(null);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Class {} method {} was not found", className, methodName);
        }

        if (method != null && method.isAnnotationPresent(TestBucket.class)) {
            groupSuffix =  " [@TestBucket(\"" + method.getAnnotation(TestBucket.class).value() + "\")]";
        }

        return groupSuffix;
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        // Do nothing
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
        // Do nothing
    }
}
