package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.logs.LogWrapper;
import org.apache.logging.log4j.Logger;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;

import java.util.ArrayList;
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
                        String fullTestName = ((MethodSource) source).getClassName() + "." + testName;
                        LOGGER.info(testName);
                        TESTS_TO_BE_EXECUTED.add(fullTestName);
                    });
                } else if (testIdentifier.isContainer() && testIdentifier.getSource().get() instanceof MethodSource) {
                    // Parametrized test
                    String testName = ((MethodSource) testIdentifier.getSource().get()).getMethodName();
                    String fullTestName = ((MethodSource) testIdentifier.getSource().get()).getClassName() + "." + testName;
                    LOGGER.info(testName);
                    TESTS_TO_BE_EXECUTED.add(fullTestName);
                }
            });
        });
        LOGGER.info("#".repeat(76));
        LOGGER.info("======================= Total count of tests to be executed [{}] =================", TESTS_TO_BE_EXECUTED.size());
        TestExecutionListener.super.testPlanExecutionStarted(testPlan);
    }

    @Override
    public void executionStarted(TestIdentifier testIdentifier) {
        //Do nothing
    }

    @Override
    public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
        // Do nothing
    }
}
