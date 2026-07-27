package com.ing.engine.plugin.loader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.ing.engine.support.reflect.Discovery;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
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

    private Path temporaryDirectory;
    private List<String> logMessages;
    private Handler logHandler;

    @BeforeMethod
    public void setUp() throws IOException {
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
        }
        return jarPath.toAbsolutePath();
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
