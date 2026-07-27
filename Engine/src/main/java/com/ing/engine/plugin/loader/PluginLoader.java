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
 *
 * <p>Discovery is idempotent: a plugin folder keeps one class loader for the lifetime of the
 * process, so every caller that asks which plugins are installed receives the same
 * {@code Class} objects. See {@link #unloadAll()} for the reload path.
 */
public class PluginLoader {
    private static final Logger LOG = Logger.getLogger(PluginLoader.class.getName());

    /**
     * One class loader per plugin folder, keyed by the folder's absolute path.
     *
     * <p>Discovery runs more than once in a session: the engine asks for plugin actions, and
     * every extension point that offers plugins a place in the application asks again. Creating
     * a class loader per call gave each caller a <em>different</em> {@code Class} for the same
     * plugin, so a plugin could not recognise an object it had itself created one lookup
     * earlier, and each lookup got its own copy of the plugin's static state — anything handed
     * to a plugin through one lookup was invisible to the next.
     *
     * <p>Reusing a loader per folder makes plugin classes, and therefore the instances built
     * from them, stable across lookups. Isolation is unaffected: each plugin folder still has
     * its own loader, so two plugins never share classes.
     */
    private static final Map<String, CachedClassLoader> CLASS_LOADERS = new HashMap<>();

    /** Guards {@link #CLASS_LOADERS}; discovery can be triggered from more than one thread. */
    private static final Object CLASS_LOADER_LOCK = new Object();

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
            List<File> pluginJars = collectPluginJars(libDir, jarFiles);
            ClassLoader pluginClassLoader = classLoaderFor(pluginFolder, pluginJars);
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
     * Returns the class loader for a plugin folder, creating it on first use and reusing it
     * afterwards so repeated discovery yields the same plugin classes.
     *
     * <p>A loader is replaced only when the JARs behind it have changed on disk; the previous
     * loader is closed first, so a reload releases its file handles instead of stacking a
     * second loader on the same folder.
     *
     * @param pluginFolder the plugin folder, used as the cache key
     * @param pluginJars the plugin JARs and their dependencies, in class path order
     * @return the class loader for this plugin folder
     * @throws Exception if a JAR cannot be converted to a URL
     */
    private static ClassLoader classLoaderFor(File pluginFolder, List<File> pluginJars)
        throws Exception {
        String key = pluginFolder.getAbsoluteFile().getPath();
        String fingerprint = fingerprint(pluginJars);
        synchronized (CLASS_LOADER_LOCK) {
            CachedClassLoader cached = CLASS_LOADERS.get(key);
            if (cached != null) {
                if (cached.fingerprint().equals(fingerprint)) {
                    return cached.classLoader();
                }
                LOG.log(Level.INFO, "Reloading plugin path={0}, reason=JARs changed on disk", key);
                close(key, cached.classLoader());
                CLASS_LOADERS.remove(key);
            }

            List<URL> urls = new ArrayList<>();
            for (File jar : pluginJars) {
                urls.add(jar.toURI().toURL());
            }
            PluginClassLoader classLoader = new PluginClassLoader(
                urls.toArray(new URL[0]),
                PluginLoader.class.getClassLoader()
            );
            CLASS_LOADERS.put(key, new CachedClassLoader(classLoader, fingerprint));
            return classLoader;
        }
    }

    /**
     * Closes every cached plugin class loader and forgets it, so the next discovery reads the
     * plugin JARs afresh.
     *
     * <p>Classes already loaded stay usable, but objects created from them belong to the
     * discarded loader and will not match the classes discovery returns afterwards. Call this
     * when plugins are being replaced on disk, not to refresh a running plugin.
     */
    public static void unloadAll() {
        synchronized (CLASS_LOADER_LOCK) {
            for (Map.Entry<String, CachedClassLoader> entry : CLASS_LOADERS.entrySet()) {
                close(entry.getKey(), entry.getValue().classLoader());
            }
            CLASS_LOADERS.clear();
        }
    }

    private static void close(String key, PluginClassLoader classLoader) {
        try {
            classLoader.close();
        } catch (IOException ex) {
            LOG.log(
                Level.INFO,
                "Could not close plugin class loader path={0}, reason={1}",
                new Object[] { key, ex.getMessage() }
            );
        }
    }

    /**
     * Describes the JARs behind a plugin folder closely enough to notice that they changed.
     *
     * @param pluginJars the plugin JARs and their dependencies, in class path order
     * @return a value that differs when the set, size or modification time of the JARs differs
     */
    private static String fingerprint(List<File> pluginJars) {
        StringBuilder fingerprint = new StringBuilder();
        for (File jar : pluginJars) {
            fingerprint
                .append(jar.getAbsolutePath())
                .append(' ')
                .append(jar.length())
                .append(' ')
                .append(jar.lastModified())
                .append('\n');
        }
        return fingerprint.toString();
    }

    /**
     * Collects the given plugin JARs and their dependencies in the optional lib directory.
     *
     * @param libDir the directory containing dependency JARs (may be null)
     * @param pluginJars one or more plugin JAR files
     * @return all plugin and dependency JARs, in class path order
     * @throws IllegalArgumentException if a plugin JAR is missing
     */
    private static List<File> collectPluginJars(File libDir, File... pluginJars) {
        List<File> jars = new ArrayList<>();

        // Add all provided plugin JARs
        for (File pluginJar : pluginJars) {
            if (pluginJar.exists()) {
                jars.add(pluginJar);
            } else {
                throw new IllegalArgumentException(
                    "Plugin JAR not found: " + pluginJar.getAbsolutePath()
                );
            }
        }

        // Add all dependency JARs from lib directory
        if (libDir != null && libDir.exists() && libDir.isDirectory()) {
            File[] dependencies = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (dependencies != null) {
                Arrays.sort(dependencies, Comparator.comparing(File::getName));
                jars.addAll(Arrays.asList(dependencies));
            }
        }
        return jars;
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

    /** A plugin folder's class loader, with the state of the JARs it was built from. */
    private record CachedClassLoader(PluginClassLoader classLoader, String fingerprint) {}
}
