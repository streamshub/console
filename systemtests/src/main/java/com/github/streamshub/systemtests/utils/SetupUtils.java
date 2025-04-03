package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.Environment;
import io.skodjob.testframe.enums.InstallType;

public class SetupUtils {
    private SetupUtils() {}

    public static boolean isOlmInstall() {
        return Environment.CONSOLE_INSTALL_TYPE.equalsIgnoreCase(InstallType.Olm.toString());
    }

    public static boolean isYamlInstall() {
        return Environment.CONSOLE_INSTALL_TYPE.equalsIgnoreCase(InstallType.Yaml.toString());
    }
}
