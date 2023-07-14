package org.bf2.admin.kafka.systemtest.listeners;

import org.bf2.admin.kafka.systemtest.utils.TestUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class TestCallbackListener implements BeforeAllCallback, BeforeEachCallback, AfterAllCallback, AfterEachCallback {
    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        TestUtils.logWithSeparator("-> Running test class: %s", extensionContext.getRequiredTestClass().getName());
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        TestUtils.logWithSeparator("-> Running test method: %s", extensionContext.getDisplayName());
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        TestUtils.logWithSeparator("-> End of test class: %s", extensionContext.getRequiredTestClass().getName());
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        TestUtils.logWithSeparator("-> End of test method: %s", extensionContext.getDisplayName());
    }
}
