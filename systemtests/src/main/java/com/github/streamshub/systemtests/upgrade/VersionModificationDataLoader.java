package com.github.streamshub.systemtests.upgrade;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.utils.FileUtils;

public class VersionModificationDataLoader {
    public enum InstallType {
        YAML
    }

    private static final Logger LOGGER = LogManager.getLogger(VersionModificationDataLoader.class);

    // Output produced by a normal build in .github/workflows/release.yml, which joins the same two files into release operator YAML bundle.
    private static final String OPERATOR_KUBERNETES_DIR = System.getProperty("user.dir") + "/../operator/target/kubernetes/";
    private static final Path OPERATOR_CRD_FILE = Paths.get(OPERATOR_KUBERNETES_DIR, "consoles.console.streamshub.github.com-v1.yml");
    private static final Path OPERATOR_RESOURCES_FILE = Paths.get(OPERATOR_KUBERNETES_DIR, "kubernetes.yml");

    private static final Pattern VERSION_LABEL_PATTERN = Pattern.compile("app\\.kubernetes\\.io/version:\\s*\"?([\\w.-]+)\"?");

    private YamlVersionModificationData yamlUpgradeData;

    public VersionModificationDataLoader(InstallType upgradeType) {
        LOGGER.info("Creating VersionModificationDataLoader for install type {}", upgradeType);
        if (upgradeType == InstallType.YAML) {
            loadYamlUpgradeData();
        }
    }

    private void loadYamlUpgradeData() {
        String oldOperatorVersion = requireNonBlank(Environment.OLD_CONSOLE_OPERATOR_VERSION, "OLD_CONSOLE_OPERATOR_VERSION");
        String oldOperatorCrdsUrl = requireNonBlank(Environment.OLD_CONSOLE_OPERATOR_CRDS_URL, "OLD_CONSOLE_OPERATOR_CRDS_URL");
        String newOperatorVersion = requireNonBlank(Environment.getConsoleOperatorVersion(),
            "CONSOLE_OPERATOR_VERSION (or the 'operator.version' system property)");
        String newOperatorCrdsUrl = resolveNewOperatorCrdsUrl(Environment.NEW_CONSOLE_OPERATOR_CRDS_URL,
            () -> mergeLocalOperatorBundle(newOperatorVersion));

        this.yamlUpgradeData = new YamlVersionModificationData(oldOperatorVersion, newOperatorVersion, oldOperatorCrdsUrl, newOperatorCrdsUrl);
        LOGGER.info("Loaded Yaml upgrade data: operator version {} -> {}", oldOperatorVersion, newOperatorVersion);
    }

    static String requireNonBlank(String value, String sourceName) {
        if (value == null || value.isBlank()) {
            throw new SetupException(sourceName + " is not set");
        }
        return value;
    }

    static String resolveNewOperatorCrdsUrl(String overrideUrl, Supplier<String> localMergeSupplier) {
        if (overrideUrl != null && !overrideUrl.isBlank()) {
            LOGGER.info("Using NEW_CONSOLE_OPERATOR_CRDS_URL override: {}", overrideUrl);
            return overrideUrl;
        }
        String mergedPath = localMergeSupplier.get();
        LOGGER.info("NEW_CONSOLE_OPERATOR_CRDS_URL not set, using locally merged operator manifest: {}", mergedPath);
        return mergedPath;
    }

    private static String mergeLocalOperatorBundle(String expectedVersion) {
        if (Files.notExists(OPERATOR_CRD_FILE) || Files.notExists(OPERATOR_RESOURCES_FILE)) {
            throw new SetupException("Local Console Operator build output not found at '" + OPERATOR_CRD_FILE + "' and '" +
                OPERATOR_RESOURCES_FILE + "'. Build the operator module first before running tests.");
        }

        String actualVersion;
        Path mergedBundle;
        try {
            actualVersion = extractOperatorVersionLabel(Files.readString(OPERATOR_RESOURCES_FILE));
            mergedBundle = FileUtils.mergeYamlFiles(List.of(OPERATOR_CRD_FILE, OPERATOR_RESOURCES_FILE));
        } catch (IOException e) {
            throw new SetupException("Failed to read/merge local Console Operator build output", e);
        }

        if (!expectedVersion.equals(actualVersion)) {
            throw new SetupException("Local Console Operator build output at '" + OPERATOR_RESOURCES_FILE +
                "' has version '" + actualVersion + "' but the expected new operator version is '" + expectedVersion +
                "' (from CONSOLE_OPERATOR_VERSION/operator.version). Rebuild the operator module for the expected version.");
        }

        return mergedBundle.toAbsolutePath().toString();
    }

    /**
     * Extracts the value of the first {@code app.kubernetes.io/version} label found in the given
     * YAML text. Package-private so it can be unit tested directly without a full local build.
     */
    static String extractOperatorVersionLabel(String yamlContent) {
        Matcher matcher = VERSION_LABEL_PATTERN.matcher(yamlContent);
        if (!matcher.find()) {
            throw new SetupException("Could not find an 'app.kubernetes.io/version' label in the locally built Console Operator YAML bundle");
        }
        return matcher.group(1);
    }

    public YamlVersionModificationData getYamlUpgradeData() {
        return yamlUpgradeData;
    }
}
