package com.github.streamshub.systemtests.upgrade;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.utils.FileUtils;

public class VersionModificationDataLoader {
    public enum InstallType {
        YAML
    }

    private static final Logger LOGGER = LogManager.getLogger(VersionModificationDataLoader.class);
    private static final String UPGRADE_FILE_PATH = System.getProperty("user.dir") + "/src/test/resources/upgrade/";
    private static final String YAML_UPGRADE_FILE = UPGRADE_FILE_PATH + "YamlUpgrade.yaml";

    // Output produced by a normal build in .github/workflows/release.yml, which joins the same two files into release operator YAML bundle.
    private static final String OPERATOR_KUBERNETES_DIR = System.getProperty("user.dir") + "/../operator/target/kubernetes/";
    private static final Path OPERATOR_CRD_FILE = Paths.get(OPERATOR_KUBERNETES_DIR, "consoles.console.streamshub.github.com-v1.yml");
    private static final Path OPERATOR_RESOURCES_FILE = Paths.get(OPERATOR_KUBERNETES_DIR, "kubernetes.yml");

    private static final Pattern VERSION_LABEL_PATTERN = Pattern.compile("app\\.kubernetes\\.io/version:\\s*\"?([\\w.-]+)\"?");

    // Literal upgrade value for 'newOperatorVersion'/'newOperatorCrdsUrl' in YamlUpgrade.yaml - uses the local build
    private static final String HEAD_VERSION = "HEAD";

    private YamlVersionModificationData yamlUpgradeData;

    public VersionModificationDataLoader(InstallType upgradeType) {
        LOGGER.info("Creating VersionModificationDataLoader for install type {}", upgradeType);
        if (upgradeType == InstallType.YAML) {
            loadYamlUpgradeData();
        }
    }

    private void loadYamlUpgradeData() {
        LOGGER.info("Loading Yaml upgrade data from file {}", YAML_UPGRADE_FILE);
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            this.yamlUpgradeData = mapper.readValue(new File(YAML_UPGRADE_FILE), YamlVersionModificationData.class);
            resolveNewOperatorData(yamlUpgradeData);
            LOGGER.info("Loaded Yaml upgrade data: operator version {} -> {}",
                yamlUpgradeData.getOldOperatorVersion(), yamlUpgradeData.getNewOperatorVersion());
        } catch (IOException e) {
            LOGGER.error("Failed to parse Yaml upgrade data from file {}: {}", YAML_UPGRADE_FILE, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * Resolves {@code newOperatorVersion}/{@code newOperatorCrdsUrl} on the given data object.
     *
     * <p>If both are set to the literal value {@value #HEAD_VERSION} in the loaded YAML file,
     * they are auto-derived from {@link Environment#getConsoleOperatorVersion()} and the local operator
     * build output. If both are set to anything else (non-blank, non-head), they are left untouched.
     * Any other combination (blank, missing, or a head explicit mismatch) is treated as a broken config.
     */
    private void resolveNewOperatorData(YamlVersionModificationData data) {
        String newOperatorVersion = data.getNewOperatorVersion();
        String newOperatorCrdsUrl = data.getNewOperatorCrdsUrl();

        boolean versionExplicit = newOperatorVersion != null && !newOperatorVersion.isBlank() && !HEAD_VERSION.equals(newOperatorVersion);
        boolean urlExplicit = newOperatorCrdsUrl != null && !newOperatorCrdsUrl.isBlank() && !HEAD_VERSION.equals(newOperatorCrdsUrl);

        if (versionExplicit && urlExplicit) {
            LOGGER.info("Using explicit new operator version '{}' and bundle '{}' from {}",
                newOperatorVersion, newOperatorCrdsUrl, YAML_UPGRADE_FILE);
            return;
        }

        boolean versionSentinel = HEAD_VERSION.equals(newOperatorVersion);
        boolean urlSentinel = HEAD_VERSION.equals(newOperatorCrdsUrl);

        if (!(versionSentinel && urlSentinel)) {
            throw new SetupException("YamlUpgrade.yaml's 'newOperatorVersion' and 'newOperatorCrdsUrl' must either both be set to the " +
                "literal value '" + HEAD_VERSION + "' (to auto-derive them from the local build) or both be set to explicit. " +
                "Found newOperatorVersion='" + newOperatorVersion + "', newOperatorCrdsUrl='" + newOperatorCrdsUrl + "'.");
        }

        LOGGER.info("New operator version/bundle set to '{}' in {}, auto-deriving from the local build", HEAD_VERSION, YAML_UPGRADE_FILE);

        String derivedVersion = Environment.getConsoleOperatorVersion();
        if (derivedVersion.isBlank()) {
            throw new SetupException("Cannot auto-derive the new Console Operator version: set the CONSOLE_OPERATOR_VERSION environment " +
                "variable (or the 'operator.version' system property), or specify 'newOperatorVersion'/'newOperatorCrdsUrl' explicitly in " +
                YAML_UPGRADE_FILE);
        }

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

        if (!derivedVersion.equals(actualVersion)) {
            throw new SetupException("Local Console Operator build output at '" + OPERATOR_RESOURCES_FILE +
                "' has version '" + actualVersion + "' but the expected new operator version is '" + derivedVersion +
                "' (from CONSOLE_OPERATOR_VERSION/operator.version). Rebuild the operator module for the expected version.");
        }

        data.setNewOperatorVersion(derivedVersion);
        data.setNewOperatorCrdsUrl(mergedBundle.toAbsolutePath().toString());
        LOGGER.info("Auto-derived new operator version '{}' from local build bundle '{}'", derivedVersion, mergedBundle);
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
