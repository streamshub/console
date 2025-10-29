package com.github.streamshub.systemtests.upgrade;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class VersionModificationDataLoader {
    public enum InstallType {
        OLM,
        YAML
    }

    private static final Logger LOGGER = LogManager.getLogger(VersionModificationDataLoader.class);
    private static final String UPGRADE_FILE_PATH = System.getProperty("user.dir") + "/src/test/resources/upgrade/";
    private static final String OLM_UPGRADE_FILE = UPGRADE_FILE_PATH + "OlmUpgrade.yaml";
    private static final String YAML_UPGRADE_FILE = UPGRADE_FILE_PATH + "YamlUpgrade.yaml";
    private OlmVersionModificationData olmUpgradeData;
    private YamlVersionModificationData yamlUpgradeData;

    public VersionModificationDataLoader(InstallType upgradeType) {
        if (upgradeType == InstallType.OLM) {
            loadOlmData();
        } else if (upgradeType == InstallType.YAML) {
            loadYamlUpgradeData();
        }
    }

    private void loadYamlUpgradeData() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            this.yamlUpgradeData = mapper.readValue(new File(YAML_UPGRADE_FILE), YamlVersionModificationData.class);
        } catch (IOException e) {
            LOGGER.error("Error while parsing Yaml upgrade data from Yaml");
            throw new RuntimeException(e);
        }
    }

    public void loadOlmData() {
        try {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            this.olmUpgradeData = mapper.readValue(new File(OLM_UPGRADE_FILE), OlmVersionModificationData.class);
        } catch (IOException e) {
            LOGGER.error("Error while parsing OLM upgrade data from Yaml");
            throw new RuntimeException(e);
        }
    }

    public OlmVersionModificationData getOlmUpgradeData() {
        return olmUpgradeData;
    }
    public YamlVersionModificationData getYamlUpgradeData() {
        return yamlUpgradeData;
    }
}
