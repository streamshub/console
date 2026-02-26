package com.github.streamshub.systemtests.utils.testutils;

public class KroxyTestUtils {

    private KroxyTestUtils() {}

    // To simulate kafka.server.common MetadataVersion
    public static String normalizeVirtualClusterVersionToMajorMinor(String version) {
        if (version == null) return null;

        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return version;
    }
}
