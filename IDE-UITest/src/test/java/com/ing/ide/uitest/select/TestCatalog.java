package com.ing.ide.uitest.select;

import com.ing.ide.uitest.IngeniousUiTest;
import com.ing.ide.uitest.report.TestIdentity;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.testng.annotations.Test;

/**
 * All UI test cases, found by scanning the compiled test classes.
 * <p>
 * Failsafe runs each test class in its own JVM, so a JVM only executes its own class. Every JVM
 * still loads the whole catalog, so a selection is validated the same way everywhere (e.g. a code
 * that matches nothing fails the run in the first JVM, before any IDE starts).
 */
public final class TestCatalog {
    private static List<CatalogEntry> cached;

    private TestCatalog() {}

    /**
     * @return every test case, sorted by test case code
     */
    public static synchronized List<CatalogEntry> load() {
        if (cached == null) {
            cached = scan(testClassesDir());
        }
        return cached;
    }

    /**
     * @return the folder the test classes and test resources are compiled to
     */
    static Path testClassesDir() {
        try {
            return Paths.get(
                IngeniousUiTest.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Cannot locate the compiled test classes", e);
        }
    }

    private static List<CatalogEntry> scan(final Path root) {
        final List<CatalogEntry> entries = new ArrayList<>();
        final ClassLoader loader = IngeniousUiTest.class.getClassLoader();
        try (Stream<Path> files = Files.walk(root)) {
            for (final Path file : (Iterable<Path>) files.filter(
                TestCatalog::isTopLevelClass
            )::iterator) {
                final String name = root
                    .relativize(file)
                    .toString()
                    .replaceAll("\\.class$", "")
                    .replace(file.getFileSystem().getSeparator(), ".");
                final Class<?> type = loadClass(name, loader);
                if (
                    type == null ||
                    !IngeniousUiTest.class.isAssignableFrom(type) ||
                    Modifier.isAbstract(type.getModifiers())
                ) {
                    continue;
                }
                for (final Method method : type.getMethods()) {
                    if (method.isAnnotationPresent(Test.class)) {
                        entries.add(
                            new CatalogEntry(TestIdentity.of(type, method), type, method.getName())
                        );
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot scan test classes in " + root, e);
        }
        return entries
            .stream()
            .sorted(
                Comparator
                    .comparing((CatalogEntry e) -> e.identity().testCaseCode())
                    .thenComparing(e -> e.key())
            )
            .collect(Collectors.toUnmodifiableList());
    }

    private static boolean isTopLevelClass(final Path file) {
        final String name = file.getFileName().toString();
        return name.endsWith(".class") && !name.contains("$");
    }

    // Loads without initialising, so no static code of the IDE runs while scanning
    private static Class<?> loadClass(final String name, final ClassLoader loader) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }
}
