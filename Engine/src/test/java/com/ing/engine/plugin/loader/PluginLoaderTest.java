package com.ing.engine.plugin.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.ing.engine.support.reflect.Discovery;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PluginLoaderTest {
    private static final String ENTRY_CLASS = "com.example.plugin.SamplePlugin";
    private static final String PLUGIN_MARKER_RESOURCE = "plugin-marker.txt";

    private Path temporaryDirectory;
    private List<String> logMessages;
    private Handler logHandler;

    @BeforeMethod
    public void setUp() throws IOException {
        PluginLoader.unloadAll();
        temporaryDirectory = Files.createTempDirectory("plugin-loader-test-");
        logMessages = new CopyOnWriteArrayList<>();
        logHandler =
            new Handler() {

                @Override
                public void publish(LogRecord record) {
                    logMessages.add(format(record));
                }

                @Override
                public void flush() {}

                @Override
                public void close() {}
            };
        Logger.getLogger(PluginLoader.class.getName()).addHandler(logHandler);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws IOException {
        Logger.getLogger(PluginLoader.class.getName()).removeHandler(logHandler);
        // Release the cached loaders' handles on the generated JARs before deleting them.
        PluginLoader.unloadAll();
        if (temporaryDirectory != null) {
            try (var paths = Files.walk(temporaryDirectory)) {
                for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException ex) {
                        path.toFile().deleteOnExit();
                    }
                }
            }
        }
    }

    @Test
    public void discoversPluginFromUserDirectoryWhenInstallDirectoryIsEmpty() throws Exception {
        Path userDirectory = temporaryDirectory.resolve("user-plugins");
        Path installDirectory = temporaryDirectory.resolve("install-plugins");
        Path userJar = createPlugin(userDirectory, "sample", "sample.plugin", "1.0.0");

        List<Class<?>> classes = PluginLoader.loadAllPluginsEntryClasses(
            locations(userDirectory, installDirectory)
        );

        assertThat(classes).hasSize(1);
        assertThat(codeSource(classes.get(0))).isEqualTo(userJar);
        Reporter.log(
            "discoversPluginFromUserDirectoryWhenInstallDirectoryIsEmpty EVIDENCE 1: user plugin discovered while install directory was absent; source=" +
            userDirectory.toAbsolutePath(),
            true
        );
    }

    @Test
    public void loadsDistinctPluginsAndUserCopyShadowsInstallCopyWithPathsLogged()
        throws Exception {
        Path userDirectory = temporaryDirectory.resolve("user-plugins");
        Path installDirectory = temporaryDirectory.resolve("install-plugins");
        Path userJar = createPlugin(userDirectory, "replacement", "shared.plugin", "2.0.0");
        createPlugin(installDirectory, "baseline", "shared.plugin", "1.0.0");
        Path distinctJar = createPlugin(installDirectory, "distinct", "distinct.plugin", "1.0.0");

        List<Class<?>> classes = PluginLoader.loadAllPluginsEntryClasses(
            locations(userDirectory, installDirectory)
        );

        Set<Path> sources = classes.stream().map(this::codeSource).collect(Collectors.toSet());
        assertThat(classes).hasSize(2);
        assertThat(sources).containsExactlyInAnyOrder(userJar, distinctJar);

        Path higherPrecedenceFolder = userDirectory.resolve("replacement").toAbsolutePath();
        Path shadowedFolder = installDirectory.resolve("baseline").toAbsolutePath();
        assertThat(logMessages)
            .anySatisfy(
                message ->
                    assertThat(message)
                        .contains("shared.plugin")
                        .contains(higherPrecedenceFolder.toString())
                        .contains(shadowedFolder.toString())
            );
        Reporter.log(
            "loadsDistinctPluginsAndUserCopyShadowsInstallCopyWithPathsLogged EVIDENCE 2: distinct plugins loaded=2; shared.plugin winner=" +
            higherPrecedenceFolder +
            "; shadowed=" +
            shadowedFolder,
            true
        );
    }

    @Test
    public void disableUserPluginsDoesNotScanUserDirectory() throws Exception {
        Path appRoot = temporaryDirectory.resolve("install");
        Path localAppData = temporaryDirectory.resolve("local");
        Path userDirectory = localAppData.resolve("INGenious").resolve("plugins");
        createPlugin(userDirectory, "sample", "sample.plugin", "1.0.0");
        Map<String, String> environment = new HashMap<>();
        environment.put("LOCALAPPDATA", localAppData.toString());
        environment.put(PluginSearchPath.DISABLE_USER_PLUGINS_ENV, "TrUe");

        List<PluginSearchPath.Location> searchPath = PluginSearchPath.resolve(
            environment::get,
            appRoot.toFile(),
            "Windows 11",
            temporaryDirectory.resolve("home").toString()
        );
        List<Class<?>> classes = PluginLoader.loadAllPluginsEntryClasses(searchPath);

        assertThat(searchPath)
            .extracting(PluginSearchPath.Location::source)
            .containsExactly("install");
        assertThat(classes).isEmpty();
        assertThat(logMessages).noneMatch(message -> message.contains(userDirectory.toString()));
        Reporter.log(
            "disableUserPluginsDoesNotScanUserDirectory EVIDENCE 3: INGENIOUS_DISABLE_USER_PLUGINS=true resolved only the install directory",
            true
        );
    }

    @Test
    public void configuredPluginPathReplacesDefaultDirectories() throws Exception {
        Path appRoot = temporaryDirectory.resolve("install");
        Path configuredDirectory = temporaryDirectory.resolve("configured");
        Path localAppData = temporaryDirectory.resolve("local");
        Path userDirectory = localAppData.resolve("INGenious").resolve("plugins");
        Path configuredJar = createPlugin(
            configuredDirectory,
            "configured-plugin",
            "configured.plugin",
            "1.0.0"
        );
        createPlugin(userDirectory, "user-plugin", "user.plugin", "1.0.0");
        createPlugin(appRoot.resolve("plugins"), "install-plugin", "install.plugin", "1.0.0");
        Map<String, String> environment = new HashMap<>();
        environment.put(PluginSearchPath.PLUGIN_PATH_ENV, configuredDirectory.toString());
        environment.put("LOCALAPPDATA", localAppData.toString());

        List<PluginSearchPath.Location> searchPath = PluginSearchPath.resolve(
            environment::get,
            appRoot.toFile(),
            "Windows 11",
            temporaryDirectory.resolve("home").toString()
        );
        List<Class<?>> classes = PluginLoader.loadAllPluginsEntryClasses(searchPath);

        assertThat(searchPath).hasSize(1);
        assertThat(searchPath.get(0).source()).isEqualTo("env");
        assertThat(classes).hasSize(1);
        assertThat(codeSource(classes.get(0))).isEqualTo(configuredJar);
        Reporter.log(
            "configuredPluginPathReplacesDefaultDirectories EVIDENCE 4: INGENIOUS_PLUGIN_PATH replaced user and install defaults; source=" +
            configuredDirectory.toAbsolutePath(),
            true
        );
    }

    @Test
    public void missingPluginDirectoriesDoNotAbortDiscovery() {
        Path firstMissingDirectory = temporaryDirectory.resolve("missing-one");
        Path secondMissingDirectory = temporaryDirectory.resolve("missing-two");

        assertThat(
                PluginLoader.loadAllPluginsEntryClasses(
                    locations(firstMissingDirectory, secondMissingDirectory)
                )
            )
            .isEmpty();

        String[] originalPackages = Discovery.packages;
        try {
            Discovery.packages = new String[] { "com.example.package.that.does.not.exist" };
            assertThatCode(Discovery::getClassesForPackage).doesNotThrowAnyException();
        } finally {
            Discovery.packages = originalPackages;
        }
        Reporter.log(
            "missingPluginDirectoriesDoNotAbortDiscovery EVIDENCE 5: two absent plugin directories returned an empty result and Discovery.getClassesForPackage completed",
            true
        );
    }

    @Test
    public void missingPluginIdFallsBackToFolderName() throws Exception {
        Path firstDirectory = temporaryDirectory.resolve("first");
        Path secondDirectory = temporaryDirectory.resolve("second");
        Path firstJar = createPlugin(firstDirectory, "legacy-plugin", null, "1.0.0");
        createPlugin(secondDirectory, "legacy-plugin", null, "2.0.0");

        List<Class<?>> classes = PluginLoader.loadAllPluginsEntryClasses(
            locations(firstDirectory, secondDirectory)
        );

        assertThat(classes).hasSize(1);
        assertThat(codeSource(classes.get(0))).isEqualTo(firstJar);
        assertThat(logMessages)
            .anyMatch(
                message ->
                    message.contains("shadowed plugin id=legacy-plugin") &&
                    message.contains(
                        firstDirectory.resolve("legacy-plugin").toAbsolutePath().toString()
                    ) &&
                    message.contains(
                        secondDirectory.resolve("legacy-plugin").toAbsolutePath().toString()
                    )
            );
        Reporter.log(
            "missingPluginIdFallsBackToFolderName EVIDENCE fallback: missing pluginId used folder name legacy-plugin for precedence",
            true
        );
    }

    @Test
    public void expandsHomePrefixesInConfiguredPluginPath() {
        Path home = temporaryDirectory.resolve("home").toAbsolutePath();
        Map<String, String> environment = Map.of(
            PluginSearchPath.PLUGIN_PATH_ENV,
            "~" +
            java.io.File.separator +
            "first" +
            java.io.File.pathSeparator +
            "${user.home}" +
            java.io.File.separator +
            "second"
        );

        List<PluginSearchPath.Location> searchPath = PluginSearchPath.resolve(
            environment::get,
            temporaryDirectory.resolve("install").toFile(),
            "Windows 11",
            home.toString()
        );

        assertThat(searchPath)
            .extracting(location -> location.directory().toPath())
            .containsExactly(home.resolve("first"), home.resolve("second"));
        Reporter.log(
            "expandsHomePrefixesInConfiguredPluginPath EVIDENCE expansion: leading ~ and ${user.home} resolved to " +
            home,
            true
        );
    }

    @Test
    public void repeatedDiscoveryReturnsTheSamePluginClassAndStaticState() throws Exception {
        Path pluginDirectory = temporaryDirectory.resolve("plugins");
        createPlugin(pluginDirectory, "sample", "sample.plugin", "1.0.0");

        List<Class<?>> first = PluginLoader.loadAllPluginsEntryClasses(locations(pluginDirectory));
        List<Class<?>> second = PluginLoader.loadAllPluginsEntryClasses(locations(pluginDirectory));

        assertThat(first).hasSize(1);
        assertThat(second).hasSize(1);
        assertThat(second.get(0)).isSameAs(first.get(0));
        assertThat(second.get(0).getClassLoader()).isSameAs(first.get(0).getClassLoader());

        // An object created from the first lookup is recognised by the second lookup's class:
        // the property a plugin needs before anything can be handed to it across lookups.
        Object instanceFromFirstLookup = first.get(0).getConstructor().newInstance();
        assertThat(second.get(0).isInstance(instanceFromFirstLookup)).isTrue();

        // And there is one copy of the plugin's static state, not one per lookup.
        first.get(0).getField("sharedHandle").set(null, "handed-over-at-first-lookup");
        assertThat(second.get(0).getField("sharedHandle").get(null))
            .isEqualTo("handed-over-at-first-lookup");

        Reporter.log(
            "repeatedDiscoveryReturnsTheSamePluginClassAndStaticState EVIDENCE identity: two lookups returned the same Class (" +
            System.identityHashCode(first.get(0)) +
            ") from the same loader (" +
            System.identityHashCode(first.get(0).getClassLoader()) +
            "); an instance from lookup 1 is an instance of lookup 2's class; static state carried over",
            true
        );
    }

    @Test
    public void differentPluginsKeepIsolatedClassLoaders() throws Exception {
        Path pluginDirectory = temporaryDirectory.resolve("plugins");
        createPlugin(pluginDirectory, "first-plugin", "first.plugin", "1.0.0");
        createPlugin(pluginDirectory, "second-plugin", "second.plugin", "1.0.0");

        List<Class<?>> classes = PluginLoader.loadAllPluginsEntryClasses(
            locations(pluginDirectory)
        );

        assertThat(classes).hasSize(2);
        assertThat(classes.get(0).getClassLoader()).isNotSameAs(classes.get(1).getClassLoader());
        assertThat(classes.get(0)).isNotSameAs(classes.get(1));
        assertThat(classes.get(0).getClassLoader()).isInstanceOf(PluginClassLoader.class);
        assertThat(classes.get(1).getClassLoader()).isInstanceOf(PluginClassLoader.class);

        // Caching per folder must not merge two plugins: static state stays separate.
        classes.get(0).getField("sharedHandle").set(null, "first");
        classes.get(1).getField("sharedHandle").set(null, "second");
        assertThat(classes.get(0).getField("sharedHandle").get(null)).isEqualTo("first");

        Reporter.log(
            "differentPluginsKeepIsolatedClassLoaders EVIDENCE isolation: first.plugin loader=" +
            System.identityHashCode(classes.get(0).getClassLoader()) +
            ", second.plugin loader=" +
            System.identityHashCode(classes.get(1).getClassLoader()) +
            "; same class name, separate classes and separate static state",
            true
        );
    }

    @Test
    public void changedPluginJarsAreReloadedAndThePreviousClassLoaderIsClosed() throws Exception {
        Path pluginDirectory = temporaryDirectory.resolve("plugins");
        Path jar = createPlugin(pluginDirectory, "sample", "sample.plugin", "1.0.0");

        List<Class<?>> before = PluginLoader.loadAllPluginsEntryClasses(locations(pluginDirectory));
        ClassLoader previousClassLoader = before.get(0).getClassLoader();
        assertThat(previousClassLoader.getResource(PLUGIN_MARKER_RESOURCE)).isNotNull();

        // Install a dependency beside the plugin, as shipping a new build of it would.
        createDependencyJar(jar.getParent().resolve("lib").resolve("extra.jar"));

        List<Class<?>> after = PluginLoader.loadAllPluginsEntryClasses(locations(pluginDirectory));

        assertThat(after.get(0).getClassLoader()).isNotSameAs(previousClassLoader);
        assertThat(previousClassLoader.getResource(PLUGIN_MARKER_RESOURCE))
            .as("the replaced loader is closed, not left holding the old JAR")
            .isNull();
        assertThat(logMessages).anyMatch(message -> message.contains("Reloading plugin path="));

        Reporter.log(
            "changedPluginJarsAreReloadedAndThePreviousClassLoaderIsClosed EVIDENCE reload: changed JARs produced loader " +
            System.identityHashCode(after.get(0).getClassLoader()) +
            " and closed loader " +
            System.identityHashCode(previousClassLoader),
            true
        );
    }

    @Test
    public void unloadAllClosesCachedClassLoadersAndForcesAFreshLoad() throws Exception {
        Path pluginDirectory = temporaryDirectory.resolve("plugins");
        createPlugin(pluginDirectory, "sample", "sample.plugin", "1.0.0");

        List<Class<?>> before = PluginLoader.loadAllPluginsEntryClasses(locations(pluginDirectory));
        ClassLoader previousClassLoader = before.get(0).getClassLoader();

        PluginLoader.unloadAll();

        assertThat(previousClassLoader.getResource(PLUGIN_MARKER_RESOURCE)).isNull();

        List<Class<?>> after = PluginLoader.loadAllPluginsEntryClasses(locations(pluginDirectory));
        assertThat(after.get(0).getClassLoader()).isNotSameAs(previousClassLoader);
        assertThat(after.get(0).getClassLoader().getResource(PLUGIN_MARKER_RESOURCE)).isNotNull();

        Reporter.log(
            "unloadAllClosesCachedClassLoadersAndForcesAFreshLoad EVIDENCE unload: loader " +
            System.identityHashCode(previousClassLoader) +
            " closed; the next discovery built loader " +
            System.identityHashCode(after.get(0).getClassLoader()),
            true
        );
    }

    @Test
    public void concurrentDiscoveryShareTheSamePluginClass() throws Exception {
        Path pluginDirectory = temporaryDirectory.resolve("plugins");
        createPlugin(pluginDirectory, "sample", "sample.plugin", "1.0.0");

        int threads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            CountDownLatch startLine = new CountDownLatch(1);
            List<Future<Class<?>>> results = new ArrayList<>();
            for (int index = 0; index < threads; index++) {
                results.add(
                    executor.submit(
                        () -> {
                            startLine.await();
                            return PluginLoader
                                .loadAllPluginsEntryClasses(locations(pluginDirectory))
                                .get(0);
                        }
                    )
                );
            }
            startLine.countDown();

            Class<?> expected = results.get(0).get(30, TimeUnit.SECONDS);
            for (Future<Class<?>> result : results) {
                assertThat(result.get(30, TimeUnit.SECONDS)).isSameAs(expected);
            }
            Reporter.log(
                "concurrentDiscoveryShareTheSamePluginClass EVIDENCE thread-safety: " +
                threads +
                " concurrent discoveries all returned Class " +
                System.identityHashCode(expected),
                true
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private List<PluginSearchPath.Location> locations(Path... directories) {
        List<PluginSearchPath.Location> locations = new ArrayList<>();
        for (int index = 0; index < directories.length; index++) {
            locations.add(
                new PluginSearchPath.Location(
                    directories[index].toFile(),
                    index == 0 ? "user" : "install"
                )
            );
        }
        return locations;
    }

    private Path createPlugin(
        Path pluginRoot,
        String folderName,
        String pluginId,
        String pluginVersion
    )
        throws IOException {
        Path pluginFolder = Files.createDirectories(pluginRoot.resolve(folderName));
        Path jarPath = pluginFolder.resolve("plugin.jar");
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("pluginEntryClasses", ENTRY_CLASS);
        if (pluginId != null) {
            attributes.putValue("pluginId", pluginId);
        }
        if (pluginVersion != null) {
            attributes.putValue("pluginVersion", pluginVersion);
        }

        try (
            InputStream classBytes = getClass()
                .getResourceAsStream("/com/example/plugin/SamplePlugin.class");
            JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath), manifest)
        ) {
            assertThat(classBytes).as("compiled sample plugin class").isNotNull();
            jar.putNextEntry(new JarEntry("com/example/plugin/SamplePlugin.class"));
            classBytes.transferTo(jar);
            jar.closeEntry();
            // A resource that exists only inside the JAR, so a lookup on it cannot be answered
            // by the parent class loader. Reading it proves a loader is still open.
            jar.putNextEntry(new JarEntry(PLUGIN_MARKER_RESOURCE));
            jar.write(folderName.getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        return jarPath.toAbsolutePath();
    }

    private void createDependencyJar(Path jarPath) throws IOException {
        Files.createDirectories(jarPath.getParent());
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        try (JarOutputStream jar = new JarOutputStream(Files.newOutputStream(jarPath), manifest)) {
            jar.putNextEntry(new JarEntry("dependency-marker.txt"));
            jar.write("dependency".getBytes(StandardCharsets.UTF_8));
            jar.closeEntry();
        }
    }

    private Path codeSource(Class<?> pluginClass) {
        try {
            return Path
                .of(pluginClass.getProtectionDomain().getCodeSource().getLocation().toURI())
                .toAbsolutePath();
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private String format(LogRecord record) {
        Object[] parameters = record.getParameters();
        return parameters == null
            ? record.getMessage()
            : MessageFormat.format(record.getMessage(), parameters);
    }
}
