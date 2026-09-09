package com.ing.datalib.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public final class WorkspacePreference {
    public static final String WORKSPACE_BASE_KEY = "workspace.base";

    private WorkspacePreference() {}

    public static String loadWorkspaceBase(String userHome) {
        Path config = configPath(userHome);
        if (!Files.isRegularFile(config)) {
            return null;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(config)) {
            properties.load(input);
            String value = properties.getProperty(WORKSPACE_BASE_KEY);
            return value == null || value.isBlank() ? null : value.trim();
        } catch (IOException ex) {
            return null;
        }
    }

    public static void saveWorkspaceBase(String userHome, Path base) throws IOException {
        Path config = configPath(userHome);
        Properties properties = new Properties();

        if (Files.isRegularFile(config)) {
            try (InputStream input = Files.newInputStream(config)) {
                properties.load(input);
            }
        }

        Files.createDirectories(config.getParent());
        properties.setProperty(WORKSPACE_BASE_KEY, base.toAbsolutePath().normalize().toString());

        Path temporary = Files.createTempFile(config.getParent(), "config-", ".tmp");

        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "INGenious Configuration");
            }

            Files.move(temporary, config, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    static Path configPath(String userHome) {
        return Path.of(userHome, ".ingenious", "config.properties");
    }
}
