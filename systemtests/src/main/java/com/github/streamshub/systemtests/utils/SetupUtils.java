package com.github.streamshub.systemtests.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.enums.InstallType;
import com.github.streamshub.systemtests.exceptions.FileOperationException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.NoSuchElementException;

public class SetupUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(SetupUtils.class);
    private SetupUtils() {}

    public static boolean isOlmInstall() {
        return Environment.CONSOLE_INSTALL_TYPE.equalsIgnoreCase(InstallType.OLM.toString());
    }

    public static boolean isYamlInstall() {
        return Environment.CONSOLE_INSTALL_TYPE.equalsIgnoreCase(InstallType.YAML.toString());
    }

    // ---------------
    // Yaml config
    // --------------
    public static boolean isValidUrl(String value) {
        if (value != null && !value.isEmpty()) {
            try {
                new URL(value);
                return true;
            } catch (MalformedURLException var5) {
                return false;
            }
        }
        return false;
    }

    public static <T> boolean isOfKind(String yamlContent, Class<T> resourceKind) {
        YAMLFactory yamlFactory = new YAMLFactory();
        ObjectMapper mapper = new ObjectMapper();
        try (YAMLParser yamlParser = yamlFactory.createParser(yamlContent)) {
            // Read values into map and filter unwanted kinds
            Map<String, Object> resource = mapper
                .readValues(yamlParser, new TypeReference<Map<String, Object>>() { })
                .readAll()
                .stream()
                .filter(r -> r.get("kind").equals(resourceKind.getSimpleName()))
                .findFirst()
                .orElseThrow();

            // Finally try to convert into the wanted kind
            T obj = mapper.convertValue(resource, resourceKind);
            return obj != null;
        } catch (NoSuchElementException | IOException e) {
            return false;
        }
    }

    public static String getYamlContent(String fileUrl) {
        if (isValidUrl(fileUrl)) {
            LOGGER.debug("Loading YAML content from url: {}", fileUrl);
            try (InputStream stream = new URL(fileUrl).openStream()) {
                return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new FileOperationException("Cannot download YAML content from url: " + fileUrl, e);
            }
        } else {
            LOGGER.debug("Loading YAML content from file: {}", fileUrl);
            try {
                return Files.readString(Paths.get(fileUrl), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new FileOperationException("Cannot get YAML content from file: " + fileUrl, e);
            }
        }
    }
}
