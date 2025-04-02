package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.Environment;

public class ConsoleUtils {
    private ConsoleUtils() {}

    public static String getConsoleOperatorName() {
        return Environment.CONSOLE_DEPLOYMENT_NAME + "-operator";
    }
}
