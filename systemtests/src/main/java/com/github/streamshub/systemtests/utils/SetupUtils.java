package com.github.streamshub.systemtests.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.GetStrimziFilesException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SetupUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(SetupUtils.class);
    private SetupUtils() {}

    public static String getYamlFileContent(String fileUrl) {
        LOGGER.debug("Loading yaml file content from url {}", fileUrl);
        StringBuilder content;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(new URL(fileUrl).openStream(), StandardCharsets.UTF_8))) {
            content = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine).append("\n");
            }
        } catch (IOException e) {
            throw new GetStrimziFilesException("Cannot get yaml file content from url: " + fileUrl, e);
        }
        return content.toString();
    }

    // ---------------
    // Yaml config
    // --------------
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


    // ---------------------
    // Strimzi Github fetch tools
    // ---------------------
    public static List<File> fetchStrimziYamlFiles() {
        List<File> yamlFiles = new ArrayList<>();
        for (String fileUrl : extractYamlFileLinksFromGithubDir()) {
            String fileName = fileUrl.substring(fileUrl.lastIndexOf('/') + 1).replace(".yaml", "");
            LOGGER.debug("Fetching strimzi file: {}", fileName);
            Path tempFile = null;
            try {
                tempFile = Files.createTempFile(fileName + "-tmp_", ".yaml");
                Files.write(tempFile, SetupUtils.getYamlFileContent(fileUrl).getBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            yamlFiles.add(tempFile.toFile());
        }
        return yamlFiles;
    }

    private static List<String> extractYamlFileLinksFromGithubDir() {
        return extractYamlFileLinksFromGithubDir(0);
    }

    private static List<String> extractYamlFileLinksFromGithubDir(int tryCount) {
        LOGGER.debug("Extracting Strimzi yaml files from github link {}", Environment.STRIMZI_OPERATOR_CRDS_URL);

        if (tryCount > 10) {
            throw new GetStrimziFilesException("Cannot get Github URL for strimzi files");
        }

        BufferedReader in;

        try {
            in = new BufferedReader(new InputStreamReader(new URL(Environment.STRIMZI_OPERATOR_CRDS_URL).openStream()));

            StringBuilder content = new StringBuilder();
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }

            in.close();

            List<String> yamlFileLinks = new ArrayList<>();
            Matcher matcher = Pattern.compile("href=\"(\\/[a-zA-Z0-9\\.\\/\\-]+\\/.{3}[a-zA-Z\\-]+\\d?\\.yaml)\"").matcher(content.toString());

            while (matcher.find()) {
                // Avoid HTML duplicates
                final String rawUrl = "https://raw.githubusercontent.com" + matcher.group(1).replace("/blob/", "/");
                if (!yamlFileLinks.contains(rawUrl)) {
                    yamlFileLinks.add(rawUrl);
                }
            }

            // Check for github url being empty
            if (yamlFileLinks.isEmpty()) {
                LOGGER.warn("Cannot get Github strimzi files URLs, trying again");
                try {
                    Thread.sleep(10_000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                return extractYamlFileLinksFromGithubDir(tryCount + 1);
            }

            return yamlFileLinks;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
