package com.ing.ide.main.settings;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/**
 * Archetype Selector / Manage Archetypes dialog (RELEASE_STRATEGY.md &sect;9.5 and &sect;9.7).
 *
 * <p>Lets the engineer pick which testing archetypes (Browser, Mobile, API, Kafka,
 * Database, SAP, Accessibility) they need. The selection is persisted to
 * {@code Configuration/ExplorerConfig.properties} under the {@code archetypes} key,
 * matching the format used elsewhere in the tool.</p>
 *
 * <p>On first run &mdash; when the {@code archetypes} key is absent &mdash; the dialog is
 * shown once as a welcome wizard. It can be reopened any time via
 * <b>Help &rarr; Manage Archetypes</b>.</p>
 *
 * <p>A second tab lists the archetype plugin packs currently installed under the
 * {@code plugins/} folder (see the engine {@code PluginLoader}), providing the
 * lightweight Plugin Manager view described in &sect;9.7.</p>
 */
public final class ArchetypeManagerDialog {
    private static final Logger LOGGER = Logger.getLogger(ArchetypeManagerDialog.class.getName());

    /** Config key holding the comma-separated list of enabled archetype ids. */
    public static final String CONFIG_KEY = "archetypes";

    /** Approximate size (MB) of the always-present core (IDE + Common + Datalib). */
    private static final int CORE_SIZE_MB = 35;

    /** Immutable descriptor for a single archetype pack. */
    public static final class Archetype {
        final String id;
        final String label;
        final int sizeMb;
        final boolean mandatory;

        Archetype(String id, String label, int sizeMb, boolean mandatory) {
            this.id = id;
            this.label = label;
            this.sizeMb = sizeMb;
            this.mandatory = mandatory;
        }

        public String id() {
            return id;
        }

        public String label() {
            return label;
        }
    }

    /** The catalog of archetypes, with approximate pack sizes from &sect;9.6. */
    public static final List<Archetype> ARCHETYPES = List.of(
        new Archetype("browser", "Browser (Playwright)", 25, true),
        new Archetype("mobile", "Mobile (Appium)", 15, false),
        new Archetype("api", "API / Web Services", 5, false),
        new Archetype("kafka", "Kafka / JMS Messaging", 20, false),
        new Archetype("database", "Database (JDBC)", 8, false),
        new Archetype("sap", "SAP GUI", 3, false),
        new Archetype("axe", "Accessibility (aXe)", 2, false)
    );

    private ArchetypeManagerDialog() {}

    // ------------------------------------------------------------------
    // Configuration persistence
    // ------------------------------------------------------------------

    private static File configFile() {
        return new File(appRoot(), "Configuration" + File.separator + "ExplorerConfig.properties");
    }

    private static File appRoot() {
        try {
            return new File(System.getProperty("user.dir")).getCanonicalFile();
        } catch (IOException ex) {
            return new File(System.getProperty("user.dir")).getAbsoluteFile();
        }
    }

    private static Properties loadConfig() {
        Properties props = new Properties();
        File file = configFile();
        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                props.load(in);
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Unable to read ExplorerConfig.properties", ex);
            }
        }
        return props;
    }

    /** @return {@code true} when the archetype selection has never been saved. */
    public static boolean isConfigured() {
        return loadConfig().getProperty(CONFIG_KEY) != null;
    }

    /** @return the currently enabled archetype ids; defaults to all available archetypes. */
    public static List<String> getSelectedArchetypes() {
        String value = loadConfig().getProperty(CONFIG_KEY);
        if (value == null || value.trim().isEmpty()) {
            return ARCHETYPES
                .stream()
                .map(Archetype::id)
                .collect(Collectors.toCollection(ArrayList::new));
        }
        List<String> selected = Arrays
            .stream(value.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        if (!selected.contains("browser")) {
            selected.add(0, "browser");
        }
        return selected;
    }

    /** Persists the selection, preserving all other properties in the file. */
    public static void saveSelection(List<String> ids) {
        // Preserve insertion order of existing keys where possible.
        Properties existing = loadConfig();
        Map<String, String> ordered = new LinkedHashMap<>();
        for (String key : existing.stringPropertyNames()) {
            ordered.put(key, existing.getProperty(key));
        }
        String value = ids.stream().distinct().collect(Collectors.joining(","));
        ordered.put(CONFIG_KEY, value);

        File file = configFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        Properties out = new Properties();
        ordered.forEach(out::setProperty);
        try (OutputStream os = new FileOutputStream(file)) {
            out.store(os, "INGenious configuration");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Unable to save archetype selection", ex);
        }
    }

    // ------------------------------------------------------------------
    // Entry points
    // ------------------------------------------------------------------

    /** Shows the manage-archetypes dialog. */
    public static void open(Component parent) {
        SwingUtilities.invokeLater(() -> buildDialog(parent, false).setVisible(true));
    }

    /** Shows the first-run welcome wizard once, when no selection has been saved yet. */
    public static void maybeShowFirstRun(Component parent) {
        if (!isConfigured()) {
            SwingUtilities.invokeLater(() -> buildDialog(parent, true).setVisible(true));
        }
    }

    // ------------------------------------------------------------------
    // UI
    // ------------------------------------------------------------------

    private static JDialog buildDialog(Component parent, boolean firstRun) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(
            owner,
            firstRun ? "Welcome to INGenious" : "Manage Archetypes"
        );
        dialog.setModal(true);
        dialog.setLayout(new BorderLayout(10, 10));

        JTabbedPane tabs = new JTabbedPane();
        List<String> selected = getSelectedArchetypes();

        List<JCheckBox> boxes = new ArrayList<>();
        JLabel sizeLabel = new JLabel();
        JPanel archetypesTab = buildArchetypesTab(selected, boxes, sizeLabel, firstRun);
        tabs.addTab("Archetypes", archetypesTab);
        tabs.addTab("Installed Plugins", buildPluginsTab());

        dialog.add(tabs, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton save = new JButton(firstRun ? "Download & Configure" : "Save");
        JButton cancel = new JButton(firstRun ? "Skip" : "Cancel");
        save.addActionListener(
            e -> {
                List<String> ids = new ArrayList<>();
                for (JCheckBox box : boxes) {
                    if (box.isSelected()) {
                        ids.add(box.getActionCommand());
                    }
                }
                if (!ids.contains("browser")) {
                    ids.add(0, "browser");
                }
                saveSelection(ids);
                JOptionPane.showMessageDialog(
                    dialog,
                    "Archetype selection saved:\n  " +
                    String.join(", ", ids) +
                    "\n\nRebuild / re-download the matching archetype packs to apply.",
                    "Archetypes",
                    JOptionPane.INFORMATION_MESSAGE
                );
                dialog.dispose();
            }
        );
        cancel.addActionListener(
            e -> {
                if (firstRun) {
                    // Persist the default so the wizard doesn't reappear every launch.
                    saveSelection(List.of("browser"));
                }
                dialog.dispose();
            }
        );
        buttons.add(cancel);
        buttons.add(save);
        dialog.add(buttons, BorderLayout.SOUTH);

        dialog.setPreferredSize(new Dimension(460, 420));
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        return dialog;
    }

    private static JPanel buildArchetypesTab(
        List<String> selected,
        List<JCheckBox> boxes,
        JLabel sizeLabel,
        boolean firstRun
    ) {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel heading = new JLabel(
            firstRun
                ? "<html><b>Select the testing archetypes you need:</b><br>" +
                "You can change this any time via Help &rarr; Manage Archetypes.</html>"
                : "<html><b>Select the testing archetypes you need:</b></html>"
        );
        panel.add(heading, BorderLayout.NORTH);

        JPanel list = new JPanel(new GridLayout(0, 1, 4, 4));
        for (Archetype a : ARCHETYPES) {
            JCheckBox box = new JCheckBox(a.label + " (~" + a.sizeMb + " MB)");
            box.setActionCommand(a.id);
            box.setSelected(a.mandatory || selected.contains(a.id));
            if (a.mandatory) {
                box.setEnabled(false); // Browser is always included.
                box.setToolTipText("Included by default");
            }
            box.addActionListener(e -> updateSizeLabel(boxes, sizeLabel));
            boxes.add(box);
            list.add(box);
        }
        panel.add(list, BorderLayout.CENTER);

        JPanel footer = new JPanel();
        footer.setLayout(new BoxLayout(footer, BoxLayout.Y_AXIS));
        sizeLabel.setFont(sizeLabel.getFont().deriveFont(Font.BOLD));
        footer.add(sizeLabel);
        footer.add(Box.createVerticalStrut(4));
        panel.add(footer, BorderLayout.SOUTH);

        updateSizeLabel(boxes, sizeLabel);
        return panel;
    }

    private static void updateSizeLabel(List<JCheckBox> boxes, JLabel sizeLabel) {
        int total = CORE_SIZE_MB;
        for (JCheckBox box : boxes) {
            if (box.isSelected()) {
                for (Archetype a : ARCHETYPES) {
                    if (a.id.equals(box.getActionCommand())) {
                        total += a.sizeMb;
                    }
                }
            }
        }
        sizeLabel.setText("Estimated download: " + total + " MB");
    }

    private static JPanel buildPluginsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        DefaultTableModel model = new DefaultTableModel(
            new Object[] { "Plugin", "Version", "Status" },
            0
        ) {

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        JTable table = new JTable(model);
        loadInstalledPlugins(model);

        panel.add(
            new JLabel("Archetype plugin packs found in the plugins/ folder:"),
            BorderLayout.NORTH
        );
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(
            e -> {
                model.setRowCount(0);
                loadInstalledPlugins(model);
            }
        );
        JButton openFolder = new JButton("Open Plugins Folder");
        openFolder.addActionListener(e -> openPluginsFolder(panel));
        actions.add(openFolder);
        actions.add(refresh);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    private static File pluginsDir() {
        return new File(appRoot(), "plugins");
    }

    private static void loadInstalledPlugins(DefaultTableModel model) {
        File dir = pluginsDir();
        if (!dir.exists() || !dir.isDirectory()) {
            model.addRow(new Object[] { "(no plugins folder)", "-", "-" });
            return;
        }
        File[] folders = dir.listFiles(File::isDirectory);
        if (folders == null || folders.length == 0) {
            model.addRow(new Object[] { "(none installed)", "-", "-" });
            return;
        }
        for (File folder : folders) {
            File[] jars = folder.listFiles((d, name) -> name.endsWith(".jar"));
            if (jars == null || jars.length == 0) {
                continue;
            }
            for (File jar : jars) {
                model.addRow(new Object[] { jar.getName(), readJarVersion(jar), "Installed" });
            }
        }
        if (model.getRowCount() == 0) {
            model.addRow(new Object[] { "(none installed)", "-", "-" });
        }
    }

    private static String readJarVersion(File jar) {
        try (JarFile jarFile = new JarFile(jar)) {
            Manifest manifest = jarFile.getManifest();
            if (manifest != null) {
                Attributes attrs = manifest.getMainAttributes();
                String version = attrs.getValue("Implementation-Version");
                if (version == null) {
                    version = attrs.getValue("Bundle-Version");
                }
                if (version != null) {
                    return version;
                }
            }
        } catch (IOException ex) {
            LOGGER.log(Level.FINE, "Unable to read manifest for " + jar, ex);
        }
        return "unknown";
    }

    private static void openPluginsFolder(Component parent) {
        File dir = pluginsDir();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(dir);
            } else {
                JOptionPane.showMessageDialog(parent, "Plugins folder: " + dir.getAbsolutePath());
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(parent, "Plugins folder: " + dir.getAbsolutePath());
        }
    }
}
