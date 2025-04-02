package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.exceptions.FileOperationException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class SetupUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(SetupUtils.class);
    private SetupUtils() {}

    // ---------------
    // Yaml config
    // --------------
    public static String getYamlContentFromUrl(String fileUrl) {
        LOGGER.debug("Loading YAML content from url: {}", fileUrl);
        StringBuilder content;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream(), StandardCharsets.UTF_8))) {
            content = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine).append("\n");
            }
        } catch (IOException e) {
            throw new FileOperationException("Cannot download YAML content from url: " + fileUrl, e);
        }
        return content.toString();
    }

    public static String getYamlContentFromFile(String filePath) {
        LOGGER.debug("Loading YAML content from file: {}", filePath);
        try {
            return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new FileOperationException("Cannot get YAML content from file: " + filePath, e);
        }
    }
}
