package org.bf2.admin.kafka.systemtest.utils;

import org.jboss.logging.Logger;

public class TestUtils {
    protected static final Logger LOGGER = Logger.getLogger(TestUtils.class);

    public static void logWithSeparator(String pattern, String text) {
        LOGGER.info("=======================================================================");
        LOGGER.infof(pattern, text);
        LOGGER.info("=======================================================================");
    }

    public static void logDeploymentPhase(String text) {
        LOGGER.info("-----------------------------------------");
        LOGGER.info(text);
        LOGGER.info("-----------------------------------------");
    }
}
