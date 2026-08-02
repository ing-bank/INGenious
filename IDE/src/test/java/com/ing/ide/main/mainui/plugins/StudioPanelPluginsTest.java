package com.ing.ide.main.mainui.plugins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.ing.engine.plugin.loader.PluginClassLoader;
import com.ing.ingenious.api.contract.data.ProjectTestDataApi;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.JComponent;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises the Studio panel SPI against real plugin JARs.
 *
 * <p>Every plugin here is built as a JAR at run time and loaded through the production
 * {@link PluginClassLoader}, so the manifest that is read is a real JAR manifest and the class
 * that is inspected really did come from that JAR. Nothing about the panel contract is
 * simulated.
 */
public class StudioPanelPluginsTest {
    private static final String PANEL_CLASS = "com.example.panel.SamplePanel";
    private static final String UNBUILDABLE_PANEL_CLASS = "com.example.panel.UnbuildablePanel";

    private Path temporaryDirectory;
    private List<PluginClassLoader> classLoaders;
    private List<String> logMessages;
    private Handler logHandler;

    @BeforeMethod
    public void setUp() throws IOException {
        temporaryDirectory = Files.createTempDirectory("studio-panel-test-");
        classLoaders = new ArrayList<>();
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
        Logger.getLogger(StudioPanelPlugins.class.getName()).addHandler(logHandler);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws IOException {
        Logger.getLogger(StudioPanelPlugins.class.getName()).removeHandler(logHandler);
        for (PluginClassLoader classLoader : classLoaders) {
            classLoader.close();
        }
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
    public void manifestMetadataPlacesThePanelWithoutInstantiatingThePlugin() throws Exception {
        Map<String, String> manifest = new LinkedHashMap<>();
        manifest.put("pluginId", "sample.panel");
        manifest.put("pluginVersion", "1.2.3");
        manifest.put("studioPanelTitle", "Test Data");
        manifest.put("studioPanelTooltip", "Pick test data for this test case");
        manifest.put("studioPanelOrder", "20");
        Class<?> panelClass = loadPanel("complete", PANEL_CLASS, manifest);

        StudioPanelPlugins.Panel panel = StudioPanelPlugins.discover(panelClass);

        assertThat(panel).isNotNull();
        assertThat(panel.getIdentity()).isEqualTo("sample.panel");
        assertThat(panel.getVersion()).isEqualTo("1.2.3");
        assertThat(panel.getOrder()).isEqualTo(20);
        // The class answers "Title from the class"/"Tooltip from the class"; reading the JAR's
        // manifest is the only way to arrive at these two values.
        assertThat(panel.getTitle()).isEqualTo("Test Data");
        assertThat(panel.getTooltip()).isEqualTo("Pick test data for this test case");
        assertThat(instances(panelClass))
            .as("a fully declared panel is placed on the toolbar without being constructed")
            .isZero();

        Reporter.log(
            "manifestMetadataPlacesThePanelWithoutInstantiatingThePlugin EVIDENCE: title/tooltip/order/id/version all came from the JAR manifest and the plugin was constructed " +
            instances(panelClass) +
            " times",
            true
        );
    }

    @Test
    public void panelWithoutManifestMetadataFallsBackToTheClass() throws Exception {
        Class<?> panelClass = loadPanel("bare", PANEL_CLASS, new LinkedHashMap<>());

        StudioPanelPlugins.Panel panel = StudioPanelPlugins.discover(panelClass);

        assertThat(panel).isNotNull();
        assertThat(panel.getIdentity()).isEqualTo(PANEL_CLASS);
        assertThat(panel.getTitle()).isEqualTo("Title from the class");
        assertThat(panel.getTooltip()).isEqualTo("Tooltip from the class");
        assertThat(panel.getOrder()).isNull();
        assertThat(panel.getVersion()).isNull();
        assertThat(instances(panelClass))
            .as("the plugin is constructed once to supply the metadata the manifest omitted")
            .isEqualTo(1);

        Reporter.log(
            "panelWithoutManifestMetadataFallsBackToTheClass EVIDENCE: identity fell back to " +
            panel.getIdentity() +
            " and the title came from the class",
            true
        );
    }

    @Test
    public void panelInAJarWithoutAManifestIsStillPlaced() throws Exception {
        Class<?> panelClass = loadPanel("no-manifest", PANEL_CLASS, null);

        StudioPanelPlugins.Panel panel = StudioPanelPlugins.discover(panelClass);

        assertThat(panel).isNotNull();
        assertThat(panel.getIdentity()).isEqualTo(PANEL_CLASS);
        assertThat(panel.getTitle()).isEqualTo("Title from the class");
        assertThat(panel.getVersion()).isNull();

        Reporter.log(
            "panelInAJarWithoutAManifestIsStillPlaced EVIDENCE: a JAR with no manifest at all produced panel " +
            panel.getIdentity() +
            " instead of an error",
            true
        );
    }

    @Test
    public void malformedManifestValuesAreIgnoredWithoutLosingTheRest() throws Exception {
        Map<String, String> manifest = new LinkedHashMap<>();
        manifest.put("studioPanelTitle", "Reports");
        manifest.put("studioPanelOrder", "soon");
        Class<?> panelClass = loadPanel("malformed", PANEL_CLASS, manifest);

        StudioPanelPlugins.Panel panel = StudioPanelPlugins.discover(panelClass);

        assertThat(panel).isNotNull();
        assertThat(panel.getTitle())
            .as("the readable half of the manifest survives")
            .isEqualTo("Reports");
        assertThat(panel.getOrder()).as("a non-integer order is dropped").isNull();
        assertThat(panel.getTooltip()).isEqualTo("Tooltip from the class");
        assertThat(logMessages)
            .anySatisfy(
                message ->
                    assertThat(message)
                        .contains("studioPanelOrder")
                        .contains("soon")
                        .contains(PANEL_CLASS)
            );

        Reporter.log(
            "malformedManifestValuesAreIgnoredWithoutLosingTheRest EVIDENCE: order 'soon' was dropped and logged, title 'Reports' was kept",
            true
        );
    }

    @Test
    public void panelDeclaringAnUnsupportedSurfaceIsRejected() throws Exception {
        Map<String, String> manifest = new LinkedHashMap<>();
        manifest.put("pluginId", "web.panel");
        manifest.put("studioPanelTitle", "Browser Screen");
        manifest.put("studioPanelSurface", "web");
        Class<?> panelClass = loadPanel("web-surface", PANEL_CLASS, manifest);

        assertThat(StudioPanelPlugins.discover(panelClass)).isNull();
        assertThat(logMessages)
            .anySatisfy(
                message -> assertThat(message).contains("web.panel").contains("unsupported surface")
            );
        assertThat(instances(panelClass))
            .as("a panel Studio cannot host is never constructed")
            .isZero();

        Reporter.log(
            "panelDeclaringAnUnsupportedSurfaceIsRejected EVIDENCE: studioPanelSurface=web was refused before construction",
            true
        );
    }

    @Test
    public void aPluginThatCannotBeConstructedIsRejectedWithoutTakingTheOthersDown()
        throws Exception {
        Class<?> unbuildable = loadPanel(
            "unbuildable",
            UNBUILDABLE_PANEL_CLASS,
            new LinkedHashMap<>()
        );
        Map<String, String> manifest = new LinkedHashMap<>();
        manifest.put("pluginId", "healthy.panel");
        manifest.put("studioPanelTitle", "Healthy");
        manifest.put("studioPanelTooltip", "Still here");
        Class<?> healthy = loadPanel("healthy", PANEL_CLASS, manifest);

        List<String> survivors = new ArrayList<>();
        // String.class stands in for a plugin entry class that is not a panel at all - the
        // ordinary case, since action plugins are discovered through the same list.
        for (Class<?> candidate : List.of(unbuildable, String.class, healthy)) {
            StudioPanelPlugins.Panel panel = StudioPanelPlugins.discover(candidate);
            if (panel != null) {
                survivors.add(panel.getIdentity());
            }
        }

        assertThat(survivors).containsExactly("healthy.panel");
        assertThat(logMessages)
            .anySatisfy(
                message ->
                    assertThat(message)
                        .contains("Cannot instantiate panel plugin")
                        .contains(UNBUILDABLE_PANEL_CLASS)
            );

        Reporter.log(
            "aPluginThatCannotBeConstructedIsRejectedWithoutTakingTheOthersDown EVIDENCE: survivors=" +
            survivors +
            " after a throwing constructor and a non-panel entry class",
            true
        );
    }

    @Test
    public void eachPluginGetsItsOwnClassLoaderAndReadsItsOwnManifest() throws Exception {
        Map<String, String> firstManifest = new LinkedHashMap<>();
        firstManifest.put("pluginId", "first.panel");
        firstManifest.put("studioPanelTitle", "First");
        firstManifest.put("studioPanelTooltip", "First screen");
        Map<String, String> secondManifest = new LinkedHashMap<>();
        secondManifest.put("pluginId", "second.panel");
        secondManifest.put("studioPanelTitle", "Second");
        secondManifest.put("studioPanelTooltip", "Second screen");

        // The same class name, shipped by two different plugins.
        Class<?> first = loadPanel("first", PANEL_CLASS, firstManifest);
        Class<?> second = loadPanel("second", PANEL_CLASS, secondManifest);

        assertThat(first).isNotSameAs(second);
        assertThat(first.getClassLoader()).isNotSameAs(second.getClassLoader());
        assertThat(first.getClassLoader()).isInstanceOf(PluginClassLoader.class);
        assertThat(second.getClassLoader()).isInstanceOf(PluginClassLoader.class);

        StudioPanelPlugins.Panel firstPanel = StudioPanelPlugins.discover(first);
        StudioPanelPlugins.Panel secondPanel = StudioPanelPlugins.discover(second);

        assertThat(firstPanel.getIdentity()).isEqualTo("first.panel");
        assertThat(firstPanel.getTitle()).isEqualTo("First");
        assertThat(secondPanel.getIdentity()).isEqualTo("second.panel");
        assertThat(secondPanel.getTitle()).isEqualTo("Second");

        // Separate loaders mean separate static state, so one plugin's trouble is its own.
        first.getField("failToCreatePanel").setBoolean(null, true);
        assertThat(second.getField("failToCreatePanel").getBoolean(null)).isFalse();

        Reporter.log(
            "eachPluginGetsItsOwnClassLoaderAndReadsItsOwnManifest EVIDENCE: loaders " +
            System.identityHashCode(first.getClassLoader()) +
            " and " +
            System.identityHashCode(second.getClassLoader()) +
            " gave the same class name the identities first.panel and second.panel, with separate static state",
            true
        );
    }

    @Test
    public void activationBuildsTheScreenOnceAndHandsOverTheProjectTestData() throws Exception {
        Map<String, String> manifest = new LinkedHashMap<>();
        manifest.put("pluginId", "activating.panel");
        manifest.put("studioPanelTitle", "Test Data");
        manifest.put("studioPanelTooltip", "Test Data");
        Class<?> panelClass = loadPanel("activating", PANEL_CLASS, manifest);
        ProjectTestDataApi testData = mock(ProjectTestDataApi.class);

        StudioPanelPlugins.Panel panel = StudioPanelPlugins.discover(panelClass);
        JComponent component = panel.activate(testData);

        assertThat(component).isNotNull();
        assertThat(panelClass.getField("lastTestData").get(null)).isSameAs(testData);
        assertThat(panelsCreated(panelClass)).isEqualTo(1);

        assertThat(panel.activate(testData))
            .as("the screen is built once and reused")
            .isSameAs(component);
        assertThat(panelsCreated(panelClass)).isEqualTo(1);

        Reporter.log(
            "activationBuildsTheScreenOnceAndHandsOverTheProjectTestData EVIDENCE: createPanel() ran " +
            panelsCreated(panelClass) +
            " time across two activations, and the panel received the ProjectTestDataApi instance",
            true
        );
    }

    @Test
    public void aPanelThatFailsToBuildReturnsNoComponentAndIsNotRetried() throws Exception {
        Map<String, String> manifest = new LinkedHashMap<>();
        manifest.put("pluginId", "failing.panel");
        manifest.put("studioPanelTitle", "Broken");
        manifest.put("studioPanelTooltip", "Broken");
        Class<?> panelClass = loadPanel("failing", PANEL_CLASS, manifest);
        panelClass.getField("failToCreatePanel").setBoolean(null, true);

        StudioPanelPlugins.Panel panel = StudioPanelPlugins.discover(panelClass);

        assertThat(panel.activate(null)).isNull();
        assertThat(panel.activate(null)).isNull();
        assertThat(panelsCreated(panelClass))
            .as("a failed activation is not retried on every toolbar click")
            .isEqualTo(1);
        assertThat(logMessages)
            .anySatisfy(
                message ->
                    assertThat(message)
                        .contains("Panel plugin failed during activation")
                        .contains("failing.panel")
            );

        Reporter.log(
            "aPanelThatFailsToBuildReturnsNoComponentAndIsNotRetried EVIDENCE: createPanel() ran " +
            panelsCreated(panelClass) +
            " time across two activations and both returned no component",
            true
        );
    }

    /**
     * Packs the compiled fixture class into a JAR and loads it through the production plugin
     * class loader, so the loaded class's code source is that JAR and its manifest is read from
     * it.
     *
     * @param folderName a name unique within this test, so each plugin gets its own JAR
     * @param className the fixture class to pack
     * @param manifestAttributes the manifest entries, or {@code null} for a JAR with no manifest
     * @return the loaded class
     */
    private Class<?> loadPanel(
        String folderName,
        String className,
        Map<String, String> manifestAttributes
    )
        throws Exception {
        Path pluginFolder = Files.createDirectories(temporaryDirectory.resolve(folderName));
        Path jarPath = pluginFolder.resolve("plugin.jar");
        String resource = "/" + className.replace('.', '/') + ".class";

        try (InputStream classBytes = getClass().getResourceAsStream(resource)) {
            assertThat(classBytes).as("compiled fixture " + className).isNotNull();
            try (
                JarOutputStream jar = manifestAttributes == null
                    ? new JarOutputStream(Files.newOutputStream(jarPath))
                    : new JarOutputStream(
                        Files.newOutputStream(jarPath),
                        manifestOf(className, manifestAttributes)
                    )
            ) {
                jar.putNextEntry(new JarEntry(resource.substring(1)));
                classBytes.transferTo(jar);
                jar.closeEntry();
            }
        }

        PluginClassLoader classLoader = new PluginClassLoader(
            new URL[] { jarPath.toUri().toURL() },
            getClass().getClassLoader()
        );
        classLoaders.add(classLoader);
        Class<?> loaded = classLoader.loadClass(className);
        assertThat(loaded.getClassLoader())
            .as("the fixture must come from the JAR, not from the test class path")
            .isSameAs(classLoader);
        return loaded;
    }

    private Manifest manifestOf(String className, Map<String, String> manifestAttributes) {
        Manifest manifest = new Manifest();
        Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.putValue("pluginEntryClasses", className);
        manifestAttributes.forEach(attributes::putValue);
        return manifest;
    }

    private int instances(Class<?> panelClass) throws Exception {
        return panelClass.getField("instances").getInt(null);
    }

    private int panelsCreated(Class<?> panelClass) throws Exception {
        return panelClass.getField("panelsCreated").getInt(null);
    }

    private String format(LogRecord record) {
        Object[] parameters = record.getParameters();
        String message = parameters == null
            ? record.getMessage()
            : MessageFormat.format(record.getMessage(), parameters);
        return record.getThrown() == null ? message : message + " | " + record.getThrown();
    }
}
