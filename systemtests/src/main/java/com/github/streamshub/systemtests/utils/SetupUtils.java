package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.enums.InstallType;

public class SetupUtils {
    private SetupUtils() {}

    public static boolean isOlmInstall() {
        return Environment.CONSOLE_INSTALL_TYPE.equalsIgnoreCase(InstallType.OLM.toString());
    }

    public static boolean isYamlInstall() {
        return Environment.CONSOLE_INSTALL_TYPE.equalsIgnoreCase(InstallType.YAML.toString());
    }
}
