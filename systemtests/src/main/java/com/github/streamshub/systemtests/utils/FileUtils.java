package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.testframe.TestFrameEnv;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.apache.commons.io.FileUtils.copyURLToFile;


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

    public static FileAttribute<Set<PosixFilePermission>> getDefaultPosixFilePermissions() {
        return PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
    }

    public static FileInputStream getDeploymentFileFromURL(String url) throws IOException {
        File deploymentFile = Files.createTempFile("tempfile", ".yaml", getDefaultPosixFilePermissions()).toFile();
        copyURLToFile(
                URI.create(url).toURL(),
                deploymentFile,
                5000,
                10000);
        deploymentFile.deleteOnExit();

        return new FileInputStream(deploymentFile);
    }

    public static Path downloadAndExtractTarGz(String url, String tempFileprefix) throws IOException {
        File tempArchive = Files.createTempFile(tempFileprefix, ".tar.gz", getDefaultPosixFilePermissions()).toFile();
        tempArchive.deleteOnExit();

        LOGGER.info("Downloading tar archive from: {}", url);
        copyURLToFile(
            URI.create(url).toURL(),
            tempArchive,
            5000,
            30000);

        Path extractDir = Files.createTempDirectory(tempFileprefix + "_extracted", getDefaultPosixFilePermissions());
        extractDir.toFile().deleteOnExit();

        try (TarArchiveInputStream tar = new TarArchiveInputStream(
            new GzipCompressorInputStream(Files.newInputStream(tempArchive.toPath())))) {

            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                Path entryPath = extractDir.resolve(entry.getName()).normalize();

                if (!entryPath.startsWith(extractDir)) {
                    throw new IOException("Tar entry outside of extraction dir: " + entry.getName());
                }

                if (entry.isDirectory()) {
                    Files.createDirectories(entryPath);
                } else {
                    Files.createDirectories(entryPath.getParent());
                    Files.write(entryPath, tar.readAllBytes());
                }
            }
        }

        LOGGER.info("Extracted archive to: {}", extractDir);
        return extractDir;
    }

    public static InputStream loadYamlsFromPath(Path targetPath) throws IOException {
        ByteArrayOutputStream combined = new ByteArrayOutputStream();

        try (Stream<Path> files = Files.walk(targetPath)) {
            List<Path> yamPaths = files.filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                .sorted()
                .toList();

            for (Path yaml : yamPaths) {
                LOGGER.debug("Adding to multi-YAML: {}", yaml);
                combined.write(Files.readAllBytes(yaml));
                combined.write('\n');
            }
        }

        LOGGER.info("Combined total of {} YAML files from dir: {}", combined.size(), targetPath);
        return new ByteArrayInputStream(combined.toByteArray());
    }
}

