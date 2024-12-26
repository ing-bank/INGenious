
package com.ing.engine.support.reflect;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.constants.FilePath;
import com.ing.engine.constants.SystemDefaults;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ClassFinder {

    private static final Logger LOG = Logger.getLogger(ClassFinder.class.getName());
    public static final FileFilter JAR_FILTER = (File file) -> file.getName().endsWith(".jar");

    /**
     * Private helper method
     *
     * @param directory The directory to start with
     * @param pckgname The package name to search for. Will be needed for
     * getting the Class object.
     * @param classes if a file isn't loaded but still is in the directory
     * @throws ClassNotFoundException
     */
    private static void checkDirectory(File directory, String pckgname,
            ArrayList<Class<?>> classes) throws ClassNotFoundException {
        File tmpDirectory;
        pckgname = pckgname.isEmpty() ? "" : pckgname + '.';
        if (directory.exists() && directory.isDirectory()) {
            final String[] files = directory.list();

            for (final String file : files) {
                if (file.endsWith(".class")) {
                    try {
                        Class<?> c = Class.forName(pckgname
                                + file.substring(0, file.length() - 6));
                        if (Command.class.isAssignableFrom(c)) {
                            classes.add(c);
                        }
                    } catch (NoClassDefFoundError ex) {
                        LOG.log(Level.OFF, ex.getMessage(), ex);
                    } catch (ClassCastException ex) {
                        LOG.log(Level.FINEST, ex.getMessage(), ex);
                    }
                } else if ((tmpDirectory = new File(directory, file))
                        .isDirectory()) {
                    checkDirectory(tmpDirectory, pckgname + file, classes);
                }
            }
        }
    }

    private static List<Class<?>> checkJarFile(String jf, String[] pkgs)
            throws IOException {
       // LOG.log(Level.INFO, "Finding Commands in {0}", jf);
        List<Class<?>> classes = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jf)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            URL[] urls = {new URL("jar:file:" + jf + "!/")};
            URLClassLoader cl = URLClassLoader.newInstance(urls);
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith(".class")) {
                    name = name.substring(0, name.length() - 6).replace('/', '.');
                    if (!inPkgs(pkgs, name)) {
                        try {
                            
                            Class<?> c = cl.loadClass(name);
                            c.asSubclass(Command.class);
                            classes.add(c);
                        } catch (ClassNotFoundException ex) {
                            LOG.log(Level.SEVERE, ex.getMessage(), ex);
                        } catch (ClassCastException e) {
                        }
                    }
                }
            }
            //need close if jars will be updated in runtime 
            // as in http://bugs.java.com/bugdatabase/view_bug.do?bug_id=5041014
            cl.close();
        }
        return classes;
    }

    private static boolean inPkgs(String[] pkgs, final String cname) {
        return FluentIterable.from(Arrays.asList(pkgs)).filter(new Predicate<String>() {
            @Override
            public boolean apply(String pkg) {
                return cname.contains(pkg);
            }
        }).isEmpty();
    }

    private static List<Class<?>> getClassesForPackage(String... pkgs) {
        final List<Class<?>> classes = new ArrayList<>();
        for (String pkg : pkgs) {
            try {
                classes.addAll(getClassesForPackage(pkg));
            } catch (ClassNotFoundException ex) {
                LOG.log(Level.SEVERE, ex.getMessage(), ex);
            }
        }
        return classes;
    }

    /**
     * Attempts to list all the classes in the specified package as determined
     * by the context class loader
     *
     * @param pckgname the package name to search
     * @return a list of classes that exist within that package
     * @throws ClassNotFoundException if something went wrong
     */
    private static ArrayList<Class<?>> getClassesForPackage(String pckgname)
            throws ClassNotFoundException {
        final ArrayList<Class<?>> classes = new ArrayList<>();

        try {
            final ClassLoader cld = Thread.currentThread().getContextClassLoader();
            if (cld == null) {
                throw new ClassNotFoundException("Can't get class loader.");
            }
            final Enumeration<URL> resources = cld.getResources(pckgname.replace('.', '/'));
            while (resources.hasMoreElements()) {
                try {
                    URL url = resources.nextElement();
                    checkDirectory(new File(URLDecoder.decode(url.getPath(), "UTF-8")),
                            pckgname, classes);
                } catch (final UnsupportedEncodingException ex) {
                    throw new ClassNotFoundException(pckgname
                            + " does not appear to be a valid package (Unsupported encoding)", ex);
                }
            }
        } catch (final IOException ioex) {
            throw new ClassNotFoundException(
                    "IO Error to get all resources for " + pckgname, ioex);
        }

        return classes;
    }

    public static List<Class<?>> getClasses(String... packageName)
            throws ClassNotFoundException, IOException {
        List<Class<?>> classes = new ArrayList<>();
        if (SystemDefaults.getClassesFromJar.get()) {
            classes.addAll(checkJarFile(FilePath.getEngineJarPath(), new String[]{""}));
        } else {
            classes.addAll(getClassesForPackage(packageName));
        }
        File commands = new File(FilePath.getExternalCommandsConfig());
        if (commands.exists() && commands.listFiles(JAR_FILTER) != null) {
            for (File e : commands.listFiles(JAR_FILTER)) {
                classes.addAll(checkJarFile(e.getAbsolutePath(), new String[]{""}));
            }
        }
        return classes;
    }
}
