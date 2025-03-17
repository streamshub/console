package com.github.streamshub.systemtests.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class YamlUtils {
    private static final Logger LOGGER = LogManager.getLogger(YamlUtils.class);

    public static <T> T configFromYaml(String yamlPath, Class<T> c) {
        return configFromYaml(new File(yamlPath), c);
    }

    public static <T> T configFromYaml(File yamlFile, Class<T> c) {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        try {
            return mapper.readValue(yamlFile, c);
        } catch (InvalidFormatException e) {
            throw new IllegalArgumentException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String replacePlaceholdersAndReturnYamlPath(String filePath, Map<String, String> variables) {
        try {
            // Read the file content
            Path path = Paths.get(filePath);
            String content = new String(Files.readAllBytes(path));

            // Replace placeholders with corresponding values
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                content = content.replace(entry.getKey(), entry.getValue());
            }

            // Return the modified file
            Path tempFile = Files.createTempFile("tmp_console-", ".yaml");
            Files.write(tempFile, content.getBytes());

            return tempFile.toString();
        } catch (IOException e) {
            throw new RuntimeException();
        }

    }

    public static String downloadYamlContent(String fileUrl) throws IOException {
        LOGGER.debug("Loading yaml content from url {}", fileUrl);
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream(), StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine)
                .append("\n");
        }

        in.close();
        return content.toString();
    }

    public static String readYamlFromFile(String filePath) throws IOException {
        LOGGER.debug("Loading yaml content from file {}", filePath);
        BufferedReader in = new BufferedReader(new FileReader(filePath, StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        String inputLine;

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine).append("\n");
        }

        in.close();
        return content.toString();
    }
}
