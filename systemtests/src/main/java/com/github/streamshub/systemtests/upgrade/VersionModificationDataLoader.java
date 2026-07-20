package com.github.streamshub.systemtests.upgrade;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class VersionModificationDataLoader {
    public enum InstallType {
        YAML
    }

    private static final Logger LOGGER = LogManager.getLogger(VersionModificationDataLoader.class);
    private static final String UPGRADE_FILE_PATH = System.getProperty("user.dir") + "/src/test/resources/upgrade/";
    private static final String YAML_UPGRADE_FILE = UPGRADE_FILE_PATH + "YamlUpgrade.yaml";

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
            LOGGER.info("Loaded Yaml upgrade data: operator version {} -> {}",
                yamlUpgradeData.getOldOperatorVersion(), yamlUpgradeData.getNewOperatorVersion());
        } catch (IOException e) {
            LOGGER.error("Failed to parse Yaml upgrade data from file {}: {}", YAML_UPGRADE_FILE, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public YamlVersionModificationData getYamlUpgradeData() {
        return yamlUpgradeData;
    }
}
