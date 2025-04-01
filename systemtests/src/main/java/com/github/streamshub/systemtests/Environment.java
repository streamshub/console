package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.enums.BrowserTypes;
import io.skodjob.testframe.environment.TestEnvironmentVariables;

import java.io.IOException;

import static io.skodjob.testframe.TestFrameEnv.USER_PATH;

public class Environment {
    private static final TestEnvironmentVariables ENVS = new TestEnvironmentVariables();
    // ------------------------------------------------------------------------------------------------------------------------------------------------------------
    public static final String BUILD_ID = ENVS.getOrDefault("BUILD_ID", "0");
    public static final String CLIENT_TYPE = ENVS.getOrDefault("CLIENT_TYPE", "kubectl");
    public static final String BROWSER_TYPE = ENVS.getOrDefault("BROWSER_TYPE", BrowserTypes.CHROMIUM.toString());
    public static final String CONSOLE_DEPLOYMENT_NAME = ENVS.getOrDefault("CONSOLE_DEPLOYMENT_NAME", "streamshub-console");
    public static final String STRIMZI_OPERATOR_NAME = ENVS.getOrDefault("STRIMZI_OPERATOR_NAME", "strimzi-cluster-operator");
    public static final String STRIMZI_OPERATOR_CRDS_URL = ENVS.getOrDefault("STRIMZI_OPERATOR_CRDS_URL", "");
    public static final String CONSOLE_OPERATOR_BUNDLE_URL = USER_PATH + "/" + ENVS.getOrDefault("CONSOLE_OPERATOR_BUNDLE_URL", "../install/console-operator/non-olm/console-operator.yaml");
    public static final String CONSOLE_CATALOG_SOURCE_NAME = ENVS.getOrDefault("CONSOLE_CATALOG_SOURCE_NAME", "streamshub-console-catalog");
    public static final String TEST_LOG_DIR = ENVS.getOrDefault("TEST_LOG_DIR",  USER_PATH + "/target/logs/");
    public static final String TEST_FILE_LOG_LEVEL = ENVS.getOrDefault("TEST_FILE_LOG_LEVEL",  "INFO");
    public static final String TEST_CONSOLE_LOG_LEVEL = ENVS.getOrDefault("TEST_CONSOLE_LOG_LEVEL",  "");
    public static final boolean SKIP_TEARDOWN = ENVS.getOrDefault("SKIP_TEARDOWN", Boolean::parseBoolean, false);
    public static final String ST_KAFKA_VERSION = ENVS.getOrDefault("ST_KAFKA_VERSION", "");
    public static final String CONSOLE_OLM_PACKAGE_NAME = ENVS.getOrDefault("CONSOLE_OLM_PACKAGE_NAME", "streamshub-console-operator");
    public static final boolean SKIP_STRIMZI_INSTALLATION = ENVS.getOrDefault("SKIP_STRIMZI_INSTALLATION", Boolean::parseBoolean, false);
    public static final String CONSOLE_API_IMAGE = ENVS.getOrDefault("CONSOLE_API_IMAGE", "");
    public static final String CONSOLE_UI_IMAGE = ENVS.getOrDefault("CONSOLE_UI_IMAGE", "");
    public static final String CONSOLE_OPERATOR_IMAGE = ENVS.getOrDefault("CONSOLE_OPERATOR_IMAGE", "");
    public static final boolean RUN_HEADLESS = ENVS.getOrDefault("RUN_HEADLESS", Boolean::parseBoolean, true);
    public static final String SCREENSHOTS_DIR_PATH = ENVS.getOrDefault("SCREENSHOTS_DIR_PATH", USER_PATH + "/screenshots");
    public static final boolean INSTALL_USING_OLM = ENVS.getOrDefault("INSTALL_USING_OLM", Boolean::parseBoolean, false);
    public static final String TEST_CLIENTS_IMAGE = ENVS.getOrDefault("TEST_CLIENTS_IMAGE", "");
    public static final String OLM_OPERATOR_CHANNEL = ENVS.getOrDefault("OLM_OPERATOR_CHANNEL", "alpha");
    // ------------------------------------------------------------------------------------------------------------------------------------------------------------
    // Deny instantiation
    private Environment() {}

    public static void saveConfigToFile() throws IOException {
        ENVS.saveConfigurationFile(TEST_LOG_DIR);
    }

    static {
        ENVS.logEnvironmentVariables();
    }
}
