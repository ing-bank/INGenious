package com.ing.ide.main.mainui.plugins;

import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reads optional Studio panel metadata from the JAR that supplied a plugin entry class.
 */
final class StudioPanelManifest {
    private static final Logger LOG = Logger.getLogger(StudioPanelManifest.class.getName());

    private StudioPanelManifest() {}

    static Metadata read(Class<?> entryClass) {
        try {
            CodeSource codeSource = entryClass.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return Metadata.EMPTY;
            }

            URL location = codeSource.getLocation();
            if (location == null || !"file".equalsIgnoreCase(location.getProtocol())) {
                return Metadata.EMPTY;
            }

            URI uri = location.toURI();
            Path path = Path.of(uri);
            if (!Files.isRegularFile(path)) {
                return Metadata.EMPTY;
            }

            try (JarFile jar = new JarFile(path.toFile())) {
                Manifest manifest = jar.getManifest();
                if (manifest == null) {
                    return Metadata.EMPTY;
                }
                Attributes attributes = manifest.getMainAttributes();
                return new Metadata(
                    value(attributes, "pluginId"),
                    value(attributes, "pluginVersion"),
                    value(attributes, "studioPanelTitle"),
                    value(attributes, "studioPanelTooltip"),
                    value(attributes, "studioPanelOrder"),
                    value(attributes, "studioPanelSurface")
                );
            }
        } catch (Exception | LinkageError ex) {
            LOG.log(Level.FINE, "Cannot read panel metadata for " + entryClass.getName(), ex);
            return Metadata.EMPTY;
        }
    }

    private static String value(Attributes attributes, String name) {
        String value = attributes.getValue(name);
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    static final class Metadata {
        private static final Metadata EMPTY = new Metadata(null, null, null, null, null, null);

        final String pluginId;
        final String pluginVersion;
        final String title;
        final String tooltip;
        final String order;
        final String surface;

        private Metadata(
            String pluginId,
            String pluginVersion,
            String title,
            String tooltip,
            String order,
            String surface
        ) {
            this.pluginId = pluginId;
            this.pluginVersion = pluginVersion;
            this.title = title;
            this.tooltip = tooltip;
            this.order = order;
            this.surface = surface;
        }
    }
}
