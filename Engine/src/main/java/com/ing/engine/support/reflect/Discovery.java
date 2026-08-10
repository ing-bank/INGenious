package com.ing.engine.support.reflect;

import com.ing.engine.constants.FilePath;
import com.ing.engine.plugin.loader.PluginLoader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for discovering and loading classes and methods from the main application,
 * user-defined directories, and plugins. Provides methods to scan packages, load classes,
 * and retrieve user-defined methods for command execution and reflection-based operations.
 */
public class Discovery {
    private static final Logger LOG = Logger.getLogger(Discovery.class.getName());

    private static List<Class<?>> classList;

    public static String[] packages;

    /**
     * Returns a combined list of all classes found in the configured packages, user-defined directory,
     * and plugin entry classes.
     *
     * @return list of discovered classes
     */
    public static List<Class<?>> getClassesForPackage() {
        ArrayList<Class<?>> clazz = new ArrayList<>();
        clazz.addAll(getClassesFromPackageList());
        clazz.addAll(getClassesFromUserDefinedPackage());
        clazz.addAll(PluginLoader.loadAllPluginsEntryClasses());
        return clazz;
    }

    /**
     * Returns all classes found in the configured packages using the ClassFinder utility.
     *
     * @return list of classes from configured packages
     */
    public static ArrayList<Class<?>> getClassesFromPackageList() {
        ArrayList<Class<?>> clazz = new ArrayList<>();
        try {
            clazz.addAll(ClassFinder.getClasses(packages));
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return clazz;
    }

    /**
     * Returns all classes found in the user-defined packages.
     *
     * @return list of user-defined classes
     */
    public static List<Class<?>> getClassesFromUserDefinedPackage() {
        List<Class<?>> classes = new ArrayList<>();
        try {
            File directory = new File(FilePath.getAppRoot(), "userdefined");
            if (directory.exists()) {
                URL[] urls = new URL[] { directory.toURI().toURL() };
                String[] files = directory.list();
                for (String file : files) {
                    if (file.endsWith(".class")) {
                        String className = file.substring(0, file.length() - 6);
                        try {
                            try (URLClassLoader uCl = new URLClassLoader(urls);) {
                                classes.add(uCl.loadClass(className));
                            } catch (IOException e) {
                                LOG.log(Level.SEVERE, null, e);
                            }
                        } catch (ClassNotFoundException e) {
                            LOG.log(Level.SEVERE, null, e);
                            throw new RuntimeException(
                                "ClassNotFoundException loading " + className
                            );
                        }
                    }
                }
            }
        } catch (MalformedURLException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return classes;
    }

    /**
     * Loads package configuration and populates the class list with all discovered classes.
     */
    public static void discoverCommands() {
        loadPackageFromProperties();
        classList = getClassesForPackage();
    }

    /**
     * Loads the list of packages to scan from the package.properties configuration file.
     * If not found, defaults to "com.ing.engine.commands".
     */
    private static void loadPackageFromProperties() {
        try {
            packages = null;
            Properties prop = new Properties();
            File file = new File("Configuration", "package.properties");
            if (file.exists()) {
                prop.load(new FileInputStream(file));
                if (prop.containsKey("actions")) {
                    packages = prop.getProperty("actions").split(",");
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Discovery.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (packages == null) {
            packages = new String[] { "com.ing.engine.commands" };
        }
    }

    /**
     * Returns the cached list of discovered classes.
     *
     * @return list of discovered classes
     */
    public static List<Class<?>> getClassList() {
        return classList;
    }

    /**
     * Returns the class object for the given fully qualified class name from the cached class list.
     *
     * @param name fully qualified class name
     * @return the class object, or null if not found
     */
    public static Class<?> getClassByName(String name) {
        for (Class<?> class1 : classList) {
            if (class1.getName().equals(name)) {
                return class1;
            }
        }
        return null;
    }

    /**
     * Returns a list of user-defined method names (no-arg, void, non-final) from user-defined classes.
     *
     * @return list of user-defined method names
     */
    public static List<String> getUserMethods() {
        List<String> userMethods = new ArrayList<>();
        List<Class<?>> clazzes = getClassesFromUserDefinedPackage();
        for (Class<?> classs : clazzes) {
            Method[] method = classs.getMethods();
            for (Method m : method) {
                if (
                    m.getParameterTypes().length == 0 &&
                    m.getReturnType() == void.class &&
                    !Modifier.isFinal(m.getModifiers())
                ) {
                    userMethods.add(m.getName());
                }
            }
        }
        return userMethods;
    }
}
