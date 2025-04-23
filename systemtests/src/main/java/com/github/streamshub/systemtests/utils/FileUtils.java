package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.exceptions.SetupException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

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
}
