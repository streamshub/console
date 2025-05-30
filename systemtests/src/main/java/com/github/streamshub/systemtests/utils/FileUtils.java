package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.exceptions.SetupException;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FileUtils {
    private FileUtils() {}

    public static URL resolveLocation(String location) throws SetupException {
        // First check if the location is a valid local path
        try {
            File file = new File(location).getAbsoluteFile();
            if (file.exists()) {
                // Due to escape characters need to convert to URI first
                return file.toURI().toURL();
            }

            // Check if the location is a valid URL
            return new URL(location);
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
