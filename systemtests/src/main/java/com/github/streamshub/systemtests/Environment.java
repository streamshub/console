package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.enums.BrowserTypes;
import io.skodjob.testframe.enums.InstallType;
import io.skodjob.testframe.environment.TestEnvironmentVariables;

import java.io.IOException;

import static io.skodjob.testframe.TestFrameEnv.USER_PATH;

public class Environment {
    private static final TestEnvironmentVariables ENVS = new TestEnvironmentVariables();
    // ------------------------------------------------------------------------------------------------------------------------------------------------------------

    public static final String CLIENT_TYPE = ENVS.getOrDefault("CLIENT_TYPE", "kubectl");

    // Strimzi
    public static final String STRIMZI_OPERATOR_NAME = ENVS.getOrDefault("STRIMZI_OPERATOR_NAME", "strimzi-cluster-operator");
    public static final String STRIMZI_OPERATOR_VERSION = ENVS.getOrDefault("STRIMZI_OPERATOR_VERSION", "");
    public static final boolean SKIP_STRIMZI_INSTALLATION = ENVS.getOrDefault("SKIP_STRIMZI_INSTALLATION", Boolean::parseBoolean, false);

    // Console
    public static final String CONSOLE_API_IMAGE = ENVS.getOrDefault("CONSOLE_API_IMAGE", "");
    public static final String CONSOLE_UI_IMAGE = ENVS.getOrDefault("CONSOLE_UI_IMAGE", "");
    public static final String CONSOLE_OPERATOR_IMAGE = ENVS.getOrDefault("CONSOLE_OPERATOR_IMAGE", "");
    public static final InstallType CONSOLE_INSTALL_TYPE = ENVS.getOrDefault("CONSOLE_INSTALL_TYPE", InstallType::fromString, InstallType.Yaml);
    public static final String CONSOLE_DEPLOYMENT_NAME = ENVS.getOrDefault("CONSOLE_DEPLOYMENT_NAME", "streamshub-console");
    public static final String CONSOLE_CLUSTER_DOMAIN = ENVS.getOrDefault("CONSOLE_CLUSTER_DOMAIN", "");

     // YAML bundle
    public static final String CONSOLE_OPERATOR_BUNDLE_URL = ENVS.getOrDefault("CONSOLE_OPERATOR_BUNDLE_URL", "");

    // OLM
    public static final String CONSOLE_OLM_CATALOG_SOURCE_NAME = ENVS.getOrDefault("CONSOLE_OLM_CATALOG_SOURCE_NAME", "streamshub-console-catalog");
    public static final String CONSOLE_OLM_PACKAGE_NAME = ENVS.getOrDefault("CONSOLE_OLM_PACKAGE_NAME", "streamshub-console-operator");
    public static final String CONSOLE_OLM_CHANNEL_NAME = ENVS.getOrDefault("CONSOLE_OLM_CHANNEL_NAME", "alpha");
    public static final String CONSOLE_OLM_CATALOG_SOURCE_NAMESPACE = ENVS.getOrDefault("CONSOLE_OLM_CATALOG_SOURCE_NAMESPACE", Constants.OPENSHIFT_MARKETPLACE_NAMESPACE);

    // Logs and debug
    public static final boolean CLEANUP_ENVIRONMENT = ENVS.getOrDefault("CLEANUP_ENVIRONMENT", Boolean::parseBoolean, true);
    public static final String BUILD_ID = ENVS.getOrDefault("BUILD_ID", "0");
    public static final String TEST_LOG_DIR = ENVS.getOrDefault("TEST_LOG_DIR",  USER_PATH + "/target/logs/");
    public static final String TEST_FILE_LOG_LEVEL = ENVS.getOrDefault("TEST_FILE_LOG_LEVEL", "INFO");
    public static final String TEST_CONSOLE_LOG_LEVEL = ENVS.getOrDefault("TEST_CONSOLE_LOG_LEVEL", "INFO");
    public static final String SCREENSHOTS_DIR_PATH = ENVS.getOrDefault("SCREENSHOTS_DIR_PATH", USER_PATH + "/screenshots");
    public static final String TRACING_DIR_PATH = ENVS.getOrDefault("TRACING_DIR_PATH", USER_PATH + "/tracing");
    // Playwright
    public static final String BROWSER_TYPE = ENVS.getOrDefault("BROWSER_TYPE", BrowserTypes.CHROMIUM.toString());
    public static final boolean RUN_HEADLESS = ENVS.getOrDefault("RUN_HEADLESS", Boolean::parseBoolean, true);

    // Kafka
    public static final String TEST_CLIENTS_IMAGE = ENVS.getOrDefault("TEST_CLIENTS_IMAGE", "");
    public static final String ST_KAFKA_VERSION = ENVS.getOrDefault("ST_KAFKA_VERSION", "");

    public static final String TEST_CLIENTS_PULL_SECRET = ENVS.getOrDefault("TEST_CLIENTS_PULL_SECRET", "");

    // ------------------------------------------------------------------------------------------------------------------------------------------------------------
    // Deny instantiation
    private Environment() {}

    public static void logConfigAndSaveToFile() throws IOException {
        ENVS.logEnvironmentVariables();
        ENVS.saveConfigurationFile(TEST_LOG_DIR);
    }

    public static boolean isOlmInstall() {
        return CONSOLE_INSTALL_TYPE.equals(InstallType.Olm);
    }

    public static boolean isTestClientsPullSecretPresent() {
        return !Environment.TEST_CLIENTS_PULL_SECRET.isEmpty();
    }
}
