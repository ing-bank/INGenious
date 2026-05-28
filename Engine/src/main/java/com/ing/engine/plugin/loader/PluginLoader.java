package com.ing.engine.plugin.loader;

import java.util.ArrayList;

import com.ing.engine.constants.FilePath;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.Arrays;


/**
 * PluginLoader is responsible for discovering and loading plugin entry classes
 * from plugin JARs located in the application's plugin directory. It uses a child-first class loader
 * strategy to ensure plugin classes and their dependencies are loaded in isolation from the main application.
 */
public class PluginLoader {

    /**
     * Loads all plugin entry classes from the plugins directory.
     * <p>
     * This method scans each plugin folder, collects all plugin JARs and their dependencies,
     * and loads the classes specified as entry points in the JAR manifest (pluginEntryClasses attribute).
     *
     * @return a list of loaded plugin entry classes
     * @throws IllegalArgumentException if the plugin directory is missing
     */
    public static List<Class<?>> loadAllPluginsEntryClasses(){
        List<Class<?>> classes = new ArrayList<>();
        File baseDir = new File(FilePath.getAppRoot() + "/plugins"); // root plugin directory

        if (!baseDir.exists() || !baseDir.isDirectory()) {
            throw new IllegalArgumentException("Base plugin directory not found: " + baseDir.getAbsolutePath());
        }

        // Iterate over each plugin folder
        for (File pluginFolder : baseDir.listFiles(File::isDirectory)) {
            // Find all plugin JARs (any *.jar in the plugin folder, not in lib)
            File[] jarFiles = pluginFolder.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jarFiles == null || jarFiles.length == 0) {
                System.err.println("No plugin JAR found in: " + pluginFolder.getAbsolutePath());
                continue;
            }
            File libDir = new File(pluginFolder, "lib"); // Dependencies folder

            // Collect all JARs for this plugin (all found jars)
            List<URL> jarUrls;
            try {
                jarUrls = collectPluginJarsUrls(libDir, jarFiles);
                // Create child-first loader for this plugin
                ClassLoader pluginClassLoader = new PluginClassLoader(jarUrls.toArray(new URL[0]), PluginLoader.class.getClassLoader());

                // get the entry classes for each plugin JAR
                for (File pluginJar : jarFiles) {
                    List<String> entryClasses;
                    try {
                        entryClasses = getEntryClasses(pluginJar);
                        for (String entryClass : entryClasses) {
                            classes.add(pluginClassLoader.loadClass(entryClass));
                        }
                    } catch (Exception ex) {
                        System.err.println("Error loading entry classes from: " + pluginJar.getName() + " -> " + ex.getMessage());
                    }
                }
            } catch (Exception ex) {
            System.getLogger(PluginLoader.class.getName()).log(System.Logger.Level.ERROR, (String) null, ex);
            }
        }
        System.out.println(classes);
        return classes;
    }

    // Accepts one or more plugin JARs, plus an optional libDir for dependencies
    /**
     * Collects URLs for the given plugin JARs and their dependencies in the optional lib directory.
     *
     * @param libDir the directory containing dependency JARs (may be null)
     * @param pluginJars one or more plugin JAR files
     * @return a list of URLs for all plugin and dependency JARs
     * @throws Exception if a JAR file is missing or cannot be converted to a URL
     */
    private static List<URL> collectPluginJarsUrls(File libDir, File... pluginJars) throws Exception {
        List<URL> urls = new ArrayList<>();

        // Add all provided plugin JARs
        for (File pluginJar : pluginJars) {
            if (pluginJar.exists()) {
                urls.add(pluginJar.toURI().toURL());
            } else {
                throw new IllegalArgumentException("Plugin JAR not found: " + pluginJar.getAbsolutePath());
            }
        }

        // Add all dependency JARs from lib directory
        if (libDir != null && libDir.exists() && libDir.isDirectory()) {
            File[] jars = libDir.listFiles((dir, name) -> name.endsWith(".jar"));
            if (jars != null) {
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
        throw new IllegalStateException("No Plugin-Entry-Classes attribute found in manifest");
    }

    

}
