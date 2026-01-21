package com.ing.engine.plugin.loader;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

/**
 * PluginClassLoader is a custom class loader for loading plugin classes and resources.
 * It uses a child-first strategy for class and resource loading, allowing plugins to override
 * classes/resources from the parent class loader if needed.
 */
public class PluginClassLoader extends URLClassLoader {

    /**
     * The parent class loader for delegation.
     */
    private final ClassLoader parent;

    /**
     * Constructs a new PluginClassLoader.
     *
     * @param urls   the URLs from which to load classes and resources
     * @param parent the parent class loader for delegation
     */
    public PluginClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent); 
        this.parent = parent;
    }

    /**
     * Loads the class with the specified name. This implementation uses a child-first
     * strategy: it attempts to load the class from the plugin JARs first, then delegates
     * to the parent class loader if not found.
     *
     * @param name    the name of the class
     * @param resolve if true, then resolve the class
     * @return the resulting Class object
     * @throws ClassNotFoundException if the class could not be found
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            // Check if class is already loaded
            Class<?> c = findLoadedClass(name);

            if (c == null) {
                try {
                    // Try to load from plugin first
                    c = findClass(name);
                } catch (ClassNotFoundException e) {
                    // Delegate to parent if not found
                    c = parent.loadClass(name);
                }
            }

            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    /**
     * Finds the class with the specified name from the plugin JARs.
     *
     * @param name the name of the class
     * @return the resulting Class object
     * @throws ClassNotFoundException if the class could not be found
     */
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    /**
     * Finds the resource with the given name using a child-first strategy.
     *
     * @param name the resource name
     * @return a URL for reading the resource, or null if not found
     */
    @Override
    public URL getResource(String name) {
        URL url = findResource(name);
        if (url == null) {
            url = parent.getResource(name);
        }
        return url;
    }

    /**
     * Returns an enumeration of URLs representing all the resources with the given name.
     * Combines resources from both the plugin and parent class loaders.
     *
     * @param name the resource name
     * @return an enumeration of URLs
     * @throws IOException if I/O errors occur
     */
    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> pluginResources = findResources(name);
        Enumeration<URL> parentResources = parent.getResources(name);
        return new CompoundEnumeration<>(new Enumeration[]{pluginResources, parentResources});
    }

    /**
     * Utility class for combining multiple enumerations into one.
     *
     * @param <E> the type of elements returned by this enumeration
     */
    private static class CompoundEnumeration<E> implements Enumeration<E> {
        private final Enumeration<E>[] enums;
        private int index = 0;

        /**
         * Constructs a CompoundEnumeration from the given enumerations.
         *
         * @param enums the enumerations to combine
         */
        @SafeVarargs
        CompoundEnumeration(Enumeration<E>... enums) {
            this.enums = enums;
        }

        /**
         * Returns true if there are more elements in any of the combined enumerations.
         *
         * @return true if more elements exist
         */
        public boolean hasMoreElements() {
            while (index < enums.length) {
                if (enums[index] != null && enums[index].hasMoreElements()) {
                    return true;
                }
                index++;
            }
            return false;
        }

        /**
         * Returns the next element from the combined enumerations.
         *
         * @return the next element
         * @throws java.util.NoSuchElementException if no more elements exist
         */
        public E nextElement() {
            if (hasMoreElements()) {
                return enums[index].nextElement();
            }
            throw new java.util.NoSuchElementException();
        }
    }
}