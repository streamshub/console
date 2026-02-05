package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.testframe.TestFrameEnv;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;


public class FileUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(FileUtils.class);
    private FileUtils() {}

    public static URL resolveLocation(String location) throws SetupException {
        // First check if the location is a local file, if not try parsing it as URL
        try {
            LOGGER.info("Resolving file location {}", location);
            File file = new File(location);

            // If it's not absolute, resolve it as relative path
            if (!file.isAbsolute()) {
                file = new File(Paths.get(TestFrameEnv.USER_PATH, "../", location).toString());
                LOGGER.debug("File location is not an absolute path, resolve it as relative path from the project location. New path {}", file.getAbsolutePath());
            }

            if (file.exists()) {
                LOGGER.debug("File was found on path {}", file.getAbsolutePath());
                // Due to escape characters need to convert to URI first
                return file.toURI().toURL();
            }

            LOGGER.debug("File was not found on path {}. Trying to parse location {} as a URL", file.getAbsolutePath(), location);
            // Location is not a valid
            return URI.create(location).toURL();
        } catch (MalformedURLException e) {
            throw new SetupException("Could not resolve given location: " + location);
        }
    }

    public static String readFile(String filePath) {
        try {
            return Files.readString(Paths.get(filePath));
        } catch (IOException e) {
            throw new SetupException("Unable to read file", e);
        }
    }

    public static byte[] readFileBytes(String filePath) {
        try {
            return Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            throw new SetupException("Unable to read file", e);
        }
    }
}

