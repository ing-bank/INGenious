package com.ing.engine.plugin.loader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * PluginLoader is responsible for discovering and loading plugin entry classes
 * from plugin JARs located on the plugin search path. It uses a child-first class loader strategy
 * to ensure plugin classes and their dependencies are loaded in isolation from the main application.
 */
public class PluginLoader {
    private static final Logger LOG = Logger.getLogger(PluginLoader.class.getName());

    /**
     * Loads all plugin entry classes from the configured plugin search path.
     * <p>
     * This method scans each plugin folder, collects all plugin JARs and their dependencies,
     * and loads the classes specified as entry points in the JAR manifest (pluginEntryClasses attribute).
     *
     * @return a list of loaded plugin entry classes
     */
    public static List<Class<?>> loadAllPluginsEntryClasses() {
        try {
            return loadAllPluginsEntryClasses(PluginSearchPath.resolve());
        } catch (Exception | LinkageError ex) {
            LOG.log(Level.INFO, "Skipping plugin discovery because the search path failed", ex);
            return new ArrayList<>();
        }
    }

    static List<Class<?>> loadAllPluginsEntryClasses(List<PluginSearchPath.Location> searchPath) {
        List<Class<?>> classes = new ArrayList<>();
        Map<String, File> loadedPluginFolders = new HashMap<>();

        for (PluginSearchPath.Location location : searchPath) {
            File baseDir = location.directory().getAbsoluteFile();
            LOG.log(
                Level.INFO,
                "Plugin search directory source={0}, path={1}, exists={2}, writable={3}",
                new Object[] {
                    location.source(),
                    baseDir.getAbsolutePath(),
                    baseDir.exists(),
                    baseDir.canWrite()
                }
            );

            if (!baseDir.exists()) {
                LOG.log(
                    Level.INFO,
                    "Skipping plugin search directory path={0}, reason=directory absent",
                    baseDir.getAbsolutePath()
                );
                continue;
            }
            if (!baseDir.isDirectory() || !baseDir.canRead()) {
                LOG.log(
                    Level.INFO,
                    "Skipping plugin search directory path={0}, reason=directory unreadable",
                    baseDir.getAbsolutePath()
                );
                continue;
            }

            File[] pluginFolders = baseDir.listFiles(File::isDirectory);
            if (pluginFolders == null) {
                LOG.log(
                    Level.INFO,
                    "Skipping plugin search directory path={0}, reason=directory could not be listed",
                    baseDir.getAbsolutePath()
                );
                continue;
            }
            Arrays.sort(pluginFolders, Comparator.comparing(File::getName));
            for (File pluginFolder : pluginFolders) {
                loadPluginFolder(pluginFolder, loadedPluginFolders, classes);
            }
        }
        return classes;
    }

    private static void loadPluginFolder(
        File pluginFolder,
        Map<String, File> loadedPluginFolders,
        List<Class<?>> classes
    ) {
        File[] jarFiles = pluginFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            LOG.log(
                Level.INFO,
                "Skipping plugin folder path={0}, reason=no JAR in folder",
                pluginFolder.getAbsolutePath()
            );
            return;
        }
        Arrays.sort(jarFiles, Comparator.comparing(File::getName));

        PluginMetadata metadata = readPluginMetadata(pluginFolder, jarFiles);
        File higherPrecedenceFolder = loadedPluginFolders.putIfAbsent(
            metadata.id(),
            pluginFolder.getAbsoluteFile()
        );
        if (higherPrecedenceFolder != null) {
            // User data conventionally precedes shared defaults (as in XDG and layered
            // application configurations). Reversing that order would prevent a user from
            // trying a newer copy of a plugin that is also present in the installed baseline.
            LOG.log(
                Level.INFO,
                "Skipping shadowed plugin id={0}, higher-precedence path={1}, shadowed path={2}",
                new Object[] {
                    metadata.id(),
                    higherPrecedenceFolder.getAbsolutePath(),
                    pluginFolder.getAbsolutePath()
                }
            );
            return;
        }

        List<String> declaredEntryClasses = new ArrayList<>();
        for (File jarFile : jarFiles) {
            try {
                declaredEntryClasses.addAll(getEntryClasses(jarFile));
            } catch (Exception ex) {
                LOG.log(
                    Level.INFO,
                    "Skipping plugin JAR path={0}, reason={1}",
                    new Object[] { jarFile.getAbsolutePath(), ex.getMessage() }
                );
            }
        }
        if (declaredEntryClasses.isEmpty()) {
            LOG.log(
                Level.INFO,
                "Skipping plugin id={0}, path={1}, reason=missing pluginEntryClasses",
                new Object[] { metadata.id(), pluginFolder.getAbsolutePath() }
            );
            return;
        }

        List<String> loadedEntryClasses = new ArrayList<>();
        try {
            File libDir = new File(pluginFolder, "lib");
            List<URL> jarUrls = collectPluginJarsUrls(libDir, jarFiles);
            ClassLoader pluginClassLoader = new PluginClassLoader(
                jarUrls.toArray(new URL[0]),
                PluginLoader.class.getClassLoader()
            );
            for (String entryClass : declaredEntryClasses) {
                try {
                    classes.add(pluginClassLoader.loadClass(entryClass));
                    loadedEntryClasses.add(entryClass);
                } catch (Exception | LinkageError ex) {
                    LOG.log(
                        Level.INFO,
                        "Skipping plugin entry class name={0}, plugin id={1}, path={2}, reason={3}",
                        new Object[] {
                            entryClass,
                            metadata.id(),
                            pluginFolder.getAbsolutePath(),
                            ex.toString()
                        }
                    );
                }
            }
        } catch (Exception | LinkageError ex) {
            LOG.log(
                Level.INFO,
                "Skipping plugin id={0}, path={1}, reason={2}",
                new Object[] { metadata.id(), pluginFolder.getAbsolutePath(), ex.toString() }
            );
            return;
        }

        if (loadedEntryClasses.isEmpty()) {
            LOG.log(
                Level.INFO,
                "Skipping plugin id={0}, path={1}, reason=no entry classes loaded",
                new Object[] { metadata.id(), pluginFolder.getAbsolutePath() }
            );
            return;
        }
        LOG.log(
            Level.INFO,
            "Loaded plugin id={0}, version={1}, source={2}, entry classes={3}",
            new Object[] {
                metadata.id(),
                metadata.version(),
                pluginFolder.getAbsolutePath(),
                loadedEntryClasses
            }
        );
    }

    private static PluginMetadata readPluginMetadata(File pluginFolder, File[] jarFiles) {
        String pluginId = null;
        String pluginVersion = null;
        for (File jarFile : jarFiles) {
            try (JarFile jar = new JarFile(jarFile)) {
                Manifest manifest = jar.getManifest();
                if (manifest == null) {
                    continue;
                }
                Attributes attributes = manifest.getMainAttributes();
                if (pluginVersion == null) {
                    pluginVersion = nonBlank(attributes.getValue("pluginVersion"));
                }
                String candidateId = nonBlank(attributes.getValue("pluginId"));
                if (candidateId != null) {
                    pluginId = candidateId;
                    String candidateVersion = nonBlank(attributes.getValue("pluginVersion"));
                    if (candidateVersion != null) {
                        pluginVersion = candidateVersion;
                    }
                    break;
                }
            } catch (IOException ex) {
                LOG.log(
                    Level.INFO,
                    "Skipping plugin manifest path={0}, reason={1}",
                    new Object[] { jarFile.getAbsolutePath(), ex.getMessage() }
                );
            }
        }
        if (pluginId == null) {
            pluginId = pluginFolder.getName();
        }
        return new PluginMetadata(
            pluginId,
            pluginVersion == null ? "<unspecified>" : pluginVersion
        );
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    /**
     * Collects URLs for the given plugin JARs and their dependencies in the optional lib directory.
     *
     * @param libDir the directory containing dependency JARs (may be null)
     * @param pluginJars one or more plugin JAR files
     * @return a list of URLs for all plugin and dependency JARs
     * @throws Exception if a JAR file is missing or cannot be converted to a URL
     */
    private static List<URL> collectPluginJarsUrls(File libDir, File... pluginJars)
        throws Exception {
        List<URL> urls = new ArrayList<>();

        // Add all provided plugin JARs
        for (File pluginJar : pluginJars) {
            if (pluginJar.exists()) {
                urls.add(pluginJar.toURI().toURL());
            } else {
                throw new IllegalArgumentException(
                    "Plugin JAR not found: " + pluginJar.getAbsolutePath()
                );
            }
        }

        // Add all dependency JARs from lib directory
        if (libDir != null && libDir.exists() && libDir.isDirectory()) {
            File[] jars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars != null) {
                Arrays.sort(jars, Comparator.comparing(File::getName));
                for (File jar : jars) {
                    urls.add(jar.toURI().toURL());
                }
            }
        }
        return urls;
    }

    /**
     * Reads the pluginEntryClasses attribute from the manifest of the given plugin JAR.
     *
     * @param pluginJar the plugin JAR file
     * @return a list of entry class names specified in the manifest
     * @throws Exception if the manifest is missing or the attribute is not found
     */
    private static List<String> getEntryClasses(File pluginJar) throws Exception {
        try (JarFile jar = new JarFile(pluginJar)) {
            Manifest manifest = jar.getManifest();
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();
                String entries = attrs.getValue("pluginEntryClasses");
                if (entries != null) {
                    return Arrays.asList(entries.split(","));
                }
            }
        }
        throw new IllegalStateException("No pluginEntryClasses attribute found in manifest");
    }

    private record PluginMetadata(String id, String version) {}
}
