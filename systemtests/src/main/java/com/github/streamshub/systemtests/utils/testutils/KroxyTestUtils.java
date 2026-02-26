package com.github.streamshub.systemtests.utils.testutils;

public class KroxyTestUtils {

    private KroxyTestUtils() {}

    /**
     * Normalizes a Kafka version string to its major and minor components.
     *
     * <p>For example, a version "4.1.0" will be normalized to "4.1". If the input
     * version is null, null is returned. If the version has fewer than two components,
     * it is returned as-is.</p>
     *
     * <p>This can be used to simulate {@code kafka.server.common.MetadataVersion} comparisons
     * for virtual Kafka clusters.</p>
     *
     * @param version the full Kafka version string (e.g., "4.1.0"")
     * @return a string containing only the major and minor version (e.g., "4.1"), or
     *         the original string if it has fewer than two components, or null if input is null
     */
    public static String normalizeVirtualClusterVersionToMajorMinor(String version) {
        if (version == null) return null;

        String[] parts = version.split("\\.");
        if (parts.length >= 2) {
            return parts[0] + "." + parts[1];
        }
        return version;
    }
}
