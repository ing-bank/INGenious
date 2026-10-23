package com.ing.ide.main.mainui.components.perfstudio;

import com.ing.datalib.component.Project;
import com.ing.engine.perf.HarReader;
import com.ing.engine.perf.K6BrowserScriptGenerator;
import com.ing.engine.perf.K6HttpScriptGenerator;
import com.ing.engine.perf.K6Locator;
import com.ing.engine.perf.K6MetricsTap;
import com.ing.engine.perf.K6Runner;
import com.ing.engine.perf.PerfProfile;
import com.ing.engine.perf.PerfReportStore;
import com.ing.engine.perf.PerfRule;
import com.ing.engine.perf.PerfRunHandle;
import com.ing.engine.perf.PerfRunRegistry;
import com.ing.engine.perf.PerfWorkspace;
import com.ing.engine.perf.RuleEngine;
import com.ing.engine.perf.ScriptProvenance;
import com.ing.ide.main.mainui.AppMainFrame;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

/**
 * Performance Studio — k6 authoring and live execution inside the IDE
 * (k6-studio style): export scripts from test cases, validate, run with
 * live graphs (polled from the k6 REST API), open the k6 web dashboard,
 * and browse run history.
 *
 * <p>All heavy lifting delegates to {@code com.ing.engine.perf}; this class
 * is pure Swing wiring.
 */
public class PerfStudioUI extends JPanel {
    private final AppMainFrame mainFrame;

    private final JComboBox<String> scriptCombo = new JComboBox<>();
    private final JComboBox<String> profileCombo = new JComboBox<>();
    private final JLabel k6Label = new JLabel();
    private final StatusPill statusPill = new StatusPill();
    private final JLabel liveLabel = new JLabel(" ");
    private final JLabel reportLink = new JLabel();
    private File lastReportFile;
    private boolean reloading;
    private final RSyntaxTextArea scriptPreview = new RSyntaxTextArea();
    private final JTextArea outputArea = new JTextArea();
    private final LiveChartPanel loadChart = new LiveChartPanel("VUs / req/s", 150);
    private final LiveChartPanel latencyChart = new LiveChartPanel("p95 / avg (ms)", 150);
    private final DefaultTableModel historyModel = new DefaultTableModel(
        new Object[] { "Script", "Timestamp", "Status", "Profile" },
        0
    ) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final Timer pollTimer;
    private PerfRunHandle currentRun;

    public PerfStudioUI(AppMainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        add(buildToolbar(), BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);
        pollTimer = new Timer(2000, e -> pollLiveMetrics());
        pollTimer.setRepeats(true);
        refreshK6Status();
    }

    /** Reload scripts/profiles/history from the current project. Safe to call anytime. */
    public final void reload() {
        reloading = true;
        try {
            refreshK6Status();
            File projectDir = projectDir();
            DefaultComboBoxModel<String> scripts = new DefaultComboBoxModel<>();
            DefaultComboBoxModel<String> profiles = new DefaultComboBoxModel<>();
            Object selectedProfile = profileCombo.getSelectedItem();
            Object selectedScript = scriptCombo.getSelectedItem();
            for (PerfProfile p : PerfProfile.builtIns()) {
                profiles.addElement(p.name);
            }
            historyModel.setRowCount(0);
            if (projectDir != null) {
                PerfWorkspace ws = new PerfWorkspace(projectDir);
                for (File f : ws.listScripts()) {
                    scripts.addElement(f.getName());
                }
                for (File f : ws.listProfiles()) {
                    String name = f.getName().replaceAll("\\.ya?ml$", "");
                    if (profiles.getIndexOf(name) < 0) {
                        profiles.addElement(name);
                    }
                }
                for (File run : ws.listRuns()) {
                    com.fasterxml.jackson.databind.JsonNode meta = PerfReportStore.runMeta(run);
                    historyModel.addRow(
                        new Object[] {
                            run.getParentFile().getName(),
                            run.getName(),
                            meta == null ? "?" : meta.path("status").asText(statusFromExit(meta)),
                            meta == null ? "?" : meta.path("profile").asText("?")
                        }
                    );
                }
            }
            scriptCombo.setModel(scripts);
            if (selectedScript != null && scripts.getIndexOf(selectedScript) >= 0) {
                scriptCombo.setSelectedItem(selectedScript);
            }
            profileCombo.setModel(profiles);
            if (selectedProfile != null && profiles.getIndexOf(selectedProfile) >= 0) {
                profileCombo.setSelectedItem(selectedProfile);
            }
            loadPreview();
        } finally {
            reloading = false;
        }
    }

    // ------------------------------------------------------------------
    // layout
    // ------------------------------------------------------------------

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        bar.add(new JLabel("Script:"));
        scriptCombo.setPrototypeDisplayValue("SomeLongScriptName_20260101.js");
        scriptCombo.addActionListener(e -> loadPreview());
        bar.add(scriptCombo);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> reload());
        bar.add(refresh);
        bar.add(new JLabel("Profile:"));
        profileCombo.addActionListener(
            e -> {
                if (!reloading) {
                    applyProfileToScript(false);
                }
            }
        );
        bar.add(profileCombo);
        JButton editProfile = new JButton("Edit Profile");
        editProfile.setToolTipText("Edit the selected profile's YAML (duration, VUs, thresholds)");
        editProfile.addActionListener(e -> editProfileDialog());
        bar.add(editProfile);
        JButton newProfile = new JButton("New Profile…");
        newProfile.addActionListener(e -> newProfileDialog());
        bar.add(newProfile);
        bar.add(Box.createHorizontalStrut(10));

        JButton export = new JButton("Export from Test Case…");
        export.addActionListener(e -> exportDialog());
        bar.add(export);
        JButton exportHar = new JButton("Export from HAR…");
        exportHar.setToolTipText(
            "Generate a k6 HTTP script from a recorded HAR (with optional auto-correlation)"
        );
        exportHar.addActionListener(e -> exportHarDialog());
        bar.add(exportHar);
        JButton validate = new JButton("Validate");
        validate.setToolTipText("Debug run: 1 VU, 1 iteration");
        validate.addActionListener(e -> runSelected(true, false));
        bar.add(validate);
        JButton run = new JButton("Run");
        run.addActionListener(e -> runSelected(false, false));
        bar.add(run);
        JButton runDash = new JButton("Run + Dashboard");
        runDash.setToolTipText("Load run with the k6 web dashboard (live graphs in browser)");
        runDash.addActionListener(e -> runSelected(false, true));
        bar.add(runDash);
        JButton stop = new JButton("Stop");
        stop.addActionListener(e -> stopCurrent());
        bar.add(stop);
        bar.add(Box.createHorizontalStrut(10));
        bar.add(k6Label);
        return bar;
    }

    private JSplitPane buildCenter() {
        scriptPreview.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        scriptPreview.setEditable(false);
        scriptPreview.setCodeFoldingEnabled(true);
        RTextScrollPane previewScroll = new RTextScrollPane(scriptPreview);

        JPanel charts = new JPanel(new GridLayout(2, 1, 4, 4));
        charts.add(loadChart);
        charts.add(latencyChart);

        outputArea.setEditable(false);
        outputArea.setFont(scriptPreview.getFont());
        JScrollPane outputScroll = new JScrollPane(outputArea);
        outputScroll.setBorder(BorderFactory.createTitledBorder("Run output / summary"));

        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        statusRow.add(new JLabel("Status:"));
        statusRow.add(statusPill);
        statusRow.add(liveLabel);
        reportLink.setText("<html><a href='#'>Open HTML report</a></html>");
        reportLink.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        reportLink.setVisible(false);
        reportLink.addMouseListener(
            new java.awt.event.MouseAdapter() {

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (lastReportFile != null && lastReportFile.isFile()) {
                        openInBrowser(lastReportFile.toURI().toString());
                    }
                }
            }
        );
        statusRow.add(reportLink);

        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.add(statusRow, BorderLayout.NORTH);
        right.add(charts, BorderLayout.CENTER);
        right.add(outputScroll, BorderLayout.SOUTH);
        outputScroll.setPreferredSize(new java.awt.Dimension(100, 170));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, previewScroll, right);
        split.setResizeWeight(0.45);
        return split;
    }

    private JScrollPane buildBottom() {
        JTable history = new JTable(historyModel);
        history.setAutoCreateRowSorter(true);
        JScrollPane scroll = new JScrollPane(history);
        scroll.setBorder(BorderFactory.createTitledBorder("Run history (Results/Performance)"));
        scroll.setPreferredSize(new java.awt.Dimension(100, 140));
        return scroll;
    }

    // ------------------------------------------------------------------
    // actions
    // ------------------------------------------------------------------

    private void refreshK6Status() {
        String k6 = K6Locator.resolve();
        if (k6 == null) {
            k6Label.setText("k6: NOT FOUND — " + K6Locator.installHint());
        } else {
            String version = K6Locator.version(k6);
            k6Label.setText("k6: " + (version == null ? k6 : version));
        }
    }

    private File projectDir() {
        Project project = mainFrame.getProject();
        return project == null ? null : new File(project.getLocation());
    }

    private File selectedScript() {
        Object name = scriptCombo.getSelectedItem();
        File projectDir = projectDir();
        if (name == null || projectDir == null) {
            return null;
        }
        return PerfWorkspace.resolveScript(name.toString(), projectDir);
    }

    private void loadPreview() {
        File script = selectedScript();
        if (script == null) {
            scriptPreview.setText("");
            return;
        }
        try {
            scriptPreview.setText(
                new String(Files.readAllBytes(script.toPath()), StandardCharsets.UTF_8)
            );
            scriptPreview.setCaretPosition(0);
        } catch (Exception e) {
            scriptPreview.setText("// cannot read " + script + ": " + e.getMessage());
        }
    }

    private void exportDialog() {
        Project project = mainFrame.getProject();
        if (project == null) {
            JOptionPane.showMessageDialog(this, "Open a project first.");
            return;
        }
        List<String> scenarioNames = new ArrayList<>();
        for (com.ing.datalib.component.Scenario s : project.getScenarios()) {
            scenarioNames.add(s.getName());
        }
        if (scenarioNames.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No scenarios in this project.");
            return;
        }
        JComboBox<String> scenarioBox = new JComboBox<>(scenarioNames.toArray(new String[0]));
        JComboBox<String> testCaseBox = new JComboBox<>();
        Runnable fillTestCases = () -> {
            testCaseBox.removeAllItems();
            com.ing.datalib.component.Scenario s = project.getScenarioByName(
                (String) scenarioBox.getSelectedItem()
            );
            if (s != null) {
                for (com.ing.datalib.component.TestCase tc : s.getTestCases()) {
                    testCaseBox.addItem(tc.getName());
                }
            }
        };
        scenarioBox.addActionListener(e -> fillTestCases.run());
        fillTestCases.run();
        JComboBox<String> typeBox = new JComboBox<>(new String[] { "http", "browser" });
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Scenario:"));
        form.add(scenarioBox);
        form.add(new JLabel("Test case:"));
        form.add(testCaseBox);
        form.add(new JLabel("Script type:"));
        form.add(typeBox);
        int ok = JOptionPane.showConfirmDialog(
            this,
            form,
            "Export k6 script",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        if (ok != JOptionPane.OK_OPTION || testCaseBox.getSelectedItem() == null) {
            return;
        }
        String scenario = (String) scenarioBox.getSelectedItem();
        String testCase = (String) testCaseBox.getSelectedItem();
        boolean browser = "browser".equals(typeBox.getSelectedItem());
        try {
            File out = exportScript(project, scenario, testCase, browser);
            reload();
            scriptCombo.setSelectedItem(out.getName());
            statusPill.setStatus("Exported " + out.getName(), StatusPill.Kind.OK);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                this,
                e.getMessage(),
                "Export failed",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    private File exportScript(
        Project project,
        String scenarioName,
        String testCaseName,
        boolean browser
    )
        throws Exception {
        com.ing.datalib.component.Scenario scenario = project.getScenarioByName(scenarioName);
        com.ing.datalib.component.TestCase testCase = scenario.getTestCaseByName(testCaseName);
        File projectDir = projectDir();
        PerfProfile profile = PerfProfile.resolve(selectedProfile(), projectDir);
        String source = "TestPlan/" + scenarioName + "/" + testCaseName;
        String regenerate =
            "ingenious perf export \"" +
            projectDir.getName() +
            "/" +
            scenarioName +
            "/" +
            testCaseName +
            "\" --type " +
            (browser ? "browser" : "http") +
            " --profile " +
            profile.name;
        String script;
        if (browser) {
            K6BrowserScriptGenerator.Result gen = K6BrowserScriptGenerator.fromTestCase(
                project,
                testCase
            );
            if (gen.actions == 0) {
                throw new IllegalStateException(
                    "Nothing to export: " + String.join("; ", gen.warnings)
                );
            }
            script =
                K6BrowserScriptGenerator.generate(
                    source,
                    regenerate,
                    profile,
                    gen.lines,
                    gen.warnings
                );
        } else {
            K6HttpScriptGenerator.Result gen = K6HttpScriptGenerator.fromTestCase(
                project,
                testCase
            );
            if (gen.requests.isEmpty()) {
                throw new IllegalStateException(
                    "Nothing to export: " + String.join("; ", gen.warnings)
                );
            }
            script =
                K6HttpScriptGenerator.generate(
                    source,
                    regenerate,
                    profile,
                    gen.requests,
                    gen.warnings
                );
        }
        PerfWorkspace ws = new PerfWorkspace(projectDir);
        ws.ensure();
        File target = new File(ws.scriptsDir(), testCaseName + ".js");
        if (target.exists() && ScriptProvenance.isHandEdited(target)) {
            int overwrite = JOptionPane.showConfirmDialog(
                this,
                target.getName() + " was hand-edited. Overwrite?",
                "Hand-edited script",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (overwrite != JOptionPane.YES_OPTION) {
                throw new IllegalStateException("Export cancelled.");
            }
        }
        Files.write(target.toPath(), script.getBytes(StandardCharsets.UTF_8));
        return target;
    }

    // ------------------------------------------------------------------
    // export from HAR recording
    // ------------------------------------------------------------------

    private void exportHarDialog() {
        File projectDir = projectDir();
        if (projectDir == null) {
            JOptionPane.showMessageDialog(this, "Open a project first.");
            return;
        }
        PerfWorkspace ws = new PerfWorkspace(projectDir);
        ws.ensure();
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser(ws.recordingsDir());
        chooser.setDialogTitle("Choose a HAR recording");
        chooser.setFileFilter(
            new javax.swing.filechooser.FileNameExtensionFilter("HAR recordings (*.har)", "har")
        );
        if (chooser.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        File har = chooser.getSelectedFile();
        if (har == null || !har.isFile()) {
            return;
        }

        javax.swing.JTextField urlFilter = new javax.swing.JTextField();
        javax.swing.JCheckBox autoCorrelate = new javax.swing.JCheckBox(
            "Auto-correlate dynamic tokens (recommended)",
            true
        );
        javax.swing.JCheckBox includeStatic = new javax.swing.JCheckBox(
            "Include static assets (css/js/images)",
            false
        );
        JPanel form = new JPanel(new GridLayout(0, 1, 4, 4));
        JPanel filterRow = new JPanel(new BorderLayout(6, 0));
        filterRow.add(new JLabel("URL filter (optional, e.g. host or path):"), BorderLayout.WEST);
        filterRow.add(urlFilter, BorderLayout.CENTER);
        form.add(filterRow);
        form.add(autoCorrelate);
        form.add(includeStatic);
        form.add(new JLabel("Profile: " + selectedProfile() + "  (change it in the toolbar)"));
        int ok = JOptionPane.showConfirmDialog(
            this,
            form,
            "Export k6 script from " + har.getName(),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        String filter = urlFilter.getText() == null ? "" : urlFilter.getText().trim();
        try {
            File out = exportFromHar(
                projectDir,
                har,
                filter.isEmpty() ? null : filter,
                autoCorrelate.isSelected(),
                includeStatic.isSelected()
            );
            reload();
            scriptCombo.setSelectedItem(out.getName());
            statusPill.setStatus("Exported " + out.getName(), StatusPill.Kind.OK);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                this,
                e.getMessage(),
                "HAR export failed",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }

    /**
     * Mirror of the CLI {@code perf export <har>} pipeline: read + filter,
     * optionally propose & persist correlation rules, apply the rules file,
     * and render with the selected profile.
     */
    private File exportFromHar(
        File projectDir,
        File har,
        String urlFilter,
        boolean autoCorrelate,
        boolean includeStatic
    )
        throws Exception {
        HarReader.Result read = HarReader.read(har, urlFilter, includeStatic);
        if (read.requests.isEmpty()) {
            throw new IllegalStateException(
                "No usable requests in HAR: " + String.join("; ", read.warnings)
            );
        }
        String baseName = har.getName().replaceAll("\\.har$", "");
        PerfWorkspace ws = new PerfWorkspace(projectDir);
        File rulesFile = PerfRule.defaultRulesFile(ws, baseName);
        List<PerfRule> rules = PerfRule.load(rulesFile);
        if (autoCorrelate) {
            int added = 0;
            for (PerfRule proposal : RuleEngine.proposeCorrelations(read.requests)) {
                boolean duplicate = false;
                for (PerfRule existing : rules) {
                    if (
                        existing.type.equals(proposal.type) && existing.value.equals(proposal.value)
                    ) {
                        duplicate = true;
                        break;
                    }
                }
                if (!duplicate) {
                    rules.add(proposal);
                    added++;
                }
            }
            if (added > 0) {
                PerfRule.save(rules, rulesFile);
            }
        }
        RuleEngine.Result appliedRules = null;
        if (!rules.isEmpty()) {
            appliedRules = RuleEngine.apply(read.requests, rules);
            read.warnings.addAll(appliedRules.warnings);
        }
        PerfProfile profile = PerfProfile.resolve(selectedProfile(), projectDir);
        String regenerate =
            "ingenious perf export \"" + har.getPath() + "\" --type http --profile " + profile.name;
        String script = K6HttpScriptGenerator.generate(
            har.getName(),
            regenerate,
            profile,
            read.requests,
            read.warnings,
            appliedRules
        );
        ws.ensure();
        File target = new File(ws.scriptsDir(), baseName + ".js");
        if (target.exists() && ScriptProvenance.isHandEdited(target)) {
            int overwrite = JOptionPane.showConfirmDialog(
                this,
                target.getName() + " was hand-edited. Overwrite?",
                "Hand-edited script",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );
            if (overwrite != JOptionPane.YES_OPTION) {
                throw new IllegalStateException("Export cancelled.");
            }
        }
        Files.write(target.toPath(), script.getBytes(StandardCharsets.UTF_8));
        return target;
    }

    private String selectedProfile() {
        Object profile = profileCombo.getSelectedItem();
        return profile == null ? "smoke" : profile.toString();
    }

    // ------------------------------------------------------------------
    // profiles: edit / create / apply-to-script
    // ------------------------------------------------------------------

    /**
     * Re-render the selected generated script with the selected profile so
     * profile edits reflect immediately in the JS (hand-edited scripts are
     * left alone).
     */
    private void applyProfileToScript(boolean quiet) {
        File script = selectedScript();
        File projectDir = projectDir();
        if (script == null || projectDir == null) {
            return;
        }
        PerfProfile profile;
        try {
            profile = PerfProfile.resolve(selectedProfile(), projectDir);
        } catch (IllegalArgumentException e) {
            statusPill.setStatus("Profile invalid: " + e.getMessage(), StatusPill.Kind.FAILED);
            return;
        }
        if (profile == null) {
            return;
        }
        com.ing.engine.perf.PerfScriptRefresher.Result result = com.ing.engine.perf.PerfScriptRefresher.refresh(
            mainFrame.getProject(),
            projectDir,
            script,
            profile
        );
        if (result.refreshed) {
            loadPreview();
            statusPill.setStatus(
                "Script updated with profile '" + profile.name + "'",
                StatusPill.Kind.OK
            );
        } else if (!quiet) {
            statusPill.setStatus("Not refreshed: " + result.reason, StatusPill.Kind.NEUTRAL);
        }
    }

    /** File backing the selected profile, materializing built-ins on demand. */
    private File profileFile(String name) {
        File projectDir = projectDir();
        if (projectDir == null) {
            return null;
        }
        PerfWorkspace ws = new PerfWorkspace(projectDir);
        ws.ensure(); // creates folders + materializes built-in YAMLs
        for (String ext : new String[] { ".yaml", ".yml" }) {
            File f = new File(ws.profilesDir(), name + ext);
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }

    private void editProfileDialog() {
        if (projectDir() == null) {
            JOptionPane.showMessageDialog(this, "Open a project first.");
            return;
        }
        String name = selectedProfile();
        File file = profileFile(name);
        if (file == null) {
            JOptionPane.showMessageDialog(this, "No YAML found for profile: " + name);
            return;
        }
        String original;
        try {
            original = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Cannot read " + file + ": " + e.getMessage());
            return;
        }
        RSyntaxTextArea editor = new RSyntaxTextArea(original, 22, 60);
        editor.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_YAML);
        RTextScrollPane scroll = new RTextScrollPane(editor);
        int ok = JOptionPane.showConfirmDialog(
            this,
            scroll,
            "Edit profile — " + file.getName(),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        if (ok != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            Files.write(file.toPath(), editor.getText().getBytes(StandardCharsets.UTF_8));
            PerfProfile.fromYaml(file); // validate
        } catch (Exception e) {
            // restore the previous content on invalid YAML
            try {
                Files.write(file.toPath(), original.getBytes(StandardCharsets.UTF_8));
            } catch (Exception ignored) {}
            JOptionPane.showMessageDialog(
                this,
                "Invalid profile YAML — changes were reverted:\n" + e.getMessage(),
                "Profile not saved",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        statusPill.setStatus("Profile '" + name + "' saved", StatusPill.Kind.OK);
        applyProfileToScript(true);
    }

    private void newProfileDialog() {
        File projectDir = projectDir();
        if (projectDir == null) {
            JOptionPane.showMessageDialog(this, "Open a project first.");
            return;
        }
        javax.swing.JTextField nameField = new javax.swing.JTextField();
        List<String> templates = new ArrayList<>();
        for (PerfProfile p : PerfProfile.builtIns()) {
            templates.add(p.name);
        }
        JComboBox<String> templateBox = new JComboBox<>(templates.toArray(new String[0]));
        JPanel form = new JPanel(new GridLayout(0, 2, 6, 6));
        form.add(new JLabel("Profile name:"));
        form.add(nameField);
        form.add(new JLabel("Start from:"));
        form.add(templateBox);
        int ok = JOptionPane.showConfirmDialog(
            this,
            form,
            "New load profile",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        );
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (ok != JOptionPane.OK_OPTION || name.isEmpty()) {
            return;
        }
        if (!name.matches("[A-Za-z0-9._-]+")) {
            JOptionPane.showMessageDialog(
                this,
                "Profile names may contain letters, digits, dot, dash and underscore only."
            );
            return;
        }
        PerfWorkspace ws = new PerfWorkspace(projectDir);
        ws.ensure();
        File target = new File(ws.profilesDir(), name + ".yaml");
        if (target.exists()) {
            JOptionPane.showMessageDialog(this, "Profile already exists: " + target.getName());
            return;
        }
        PerfProfile template = PerfProfile.builtIn((String) templateBox.getSelectedItem());
        try {
            PerfProfile copy = new PerfProfile(
                name,
                "Custom profile derived from '" + template.name + "'. Edit stages/thresholds.",
                template.executor,
                template.vus,
                template.duration,
                template.stages,
                template.thresholds,
                false
            );
            copy.saveTo(target);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Could not create profile: " + e.getMessage());
            return;
        }
        reload();
        profileCombo.setSelectedItem(name);
        statusPill.setStatus("Profile '" + name + "' created", StatusPill.Kind.OK);
        editProfileDialog();
    }

    private void runSelected(boolean validate, boolean dashboard) {
        String k6 = K6Locator.resolve();
        if (k6 == null) {
            JOptionPane.showMessageDialog(this, K6Locator.installHint());
            return;
        }
        File script = selectedScript();
        File projectDir = projectDir();
        if (script == null || projectDir == null) {
            JOptionPane.showMessageDialog(this, "Select a script first (export one if empty).");
            return;
        }
        if (currentRun != null && currentRun.isAlive()) {
            JOptionPane.showMessageDialog(this, "A run is already active — stop it first.");
            return;
        }
        PerfWorkspace ws = new PerfWorkspace(projectDir);
        outputArea.setText("");
        lastReportFile = null;
        reportLink.setVisible(false);
        loadChart.reset();
        latencyChart.reset();
        // make sure the script reflects the currently selected profile
        applyProfileToScript(true);
        if (validate) {
            statusPill.setStatus("Validating (1 VU, 1 iteration)…", StatusPill.Kind.RUNNING);
            SwingWorker<K6Runner.RunResult, Void> worker = new SwingWorker<K6Runner.RunResult, Void>() {

                @Override
                protected K6Runner.RunResult doInBackground() throws Exception {
                    return K6Runner.validate(k6, script, ws);
                }

                @Override
                protected void done() {
                    try {
                        K6Runner.RunResult result = get();
                        if (result.exitCode == 0) {
                            statusPill.setStatus("Validate PASSED", StatusPill.Kind.OK);
                        } else {
                            statusPill.setStatus(
                                "Validate FAILED (exit " + result.exitCode + ")",
                                StatusPill.Kind.FAILED
                            );
                        }
                        outputArea.setText(summaryText(result.runDir));
                        updateReportLink(result.runDir);
                    } catch (Exception e) {
                        statusPill.setStatus(
                            "Validate error: " + e.getMessage(),
                            StatusPill.Kind.FAILED
                        );
                    }
                    reloadHistoryOnly();
                }
            };
            worker.execute();
            return;
        }
        try {
            currentRun =
                K6Runner.startAsync(
                    k6,
                    script,
                    ws,
                    selectedProfile(),
                    new ArrayList<String>(),
                    dashboard
                );
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                this,
                e.getMessage(),
                "k6 start failed",
                JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        statusPill.setStatus("RUNNING " + currentRun.runId, StatusPill.Kind.RUNNING);
        if (dashboard && currentRun.dashboardUrl() != null) {
            openInBrowser(currentRun.dashboardUrl());
        }
        pollTimer.start();
    }

    private void stopCurrent() {
        PerfRunHandle handle = currentRun;
        if (handle == null || !handle.isAlive()) {
            File projectDir = projectDir();
            if (projectDir != null) {
                handle = PerfRunRegistry.latestRunning(new PerfWorkspace(projectDir));
            }
        }
        if (handle == null || !handle.isAlive()) {
            statusPill.setStatus("No active run", StatusPill.Kind.NEUTRAL);
            return;
        }
        final PerfRunHandle target = handle;
        statusPill.setStatus("Stopping " + target.runId + "…", StatusPill.Kind.DRAINING);
        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

            @Override
            protected Boolean doInBackground() {
                boolean down = target.cancel();
                K6Runner.reconcileRunMeta(target);
                return Boolean.valueOf(down);
            }

            @Override
            protected void done() {
                finishRun(target);
            }
        };
        worker.execute();
    }

    // ------------------------------------------------------------------
    // live polling
    // ------------------------------------------------------------------

    private void pollLiveMetrics() {
        PerfRunHandle handle = currentRun;
        if (handle == null) {
            pollTimer.stop();
            return;
        }
        String phase = handle.phase();
        if ("FINISHED".equals(phase)) {
            pollTimer.stop();
            K6Runner.reconcileRunMeta(handle);
            finishRun(handle);
            return;
        }
        if ("DRAINING".equals(phase)) {
            statusPill.setStatus(
                "DRAINING — close the dashboard tab (or Stop) to flush the summary",
                StatusPill.Kind.DRAINING
            );
        }
        Map<String, Double> numbers = K6MetricsTap.numericSnapshot(handle.apiPort);
        if (!numbers.isEmpty()) {
            addIfPresent(loadChart, numbers, "vus", "vus");
            addIfPresent(loadChart, numbers, "rps", "req/s");
            addIfPresent(latencyChart, numbers, "p95", "p95");
            addIfPresent(latencyChart, numbers, "avg", "avg");
            loadChart.tick();
            latencyChart.tick();
            StringBuilder live = new StringBuilder();
            for (Map.Entry<String, String> e : K6MetricsTap.snapshot(handle.apiPort).entrySet()) {
                if (live.length() > 0) {
                    live.append("   ");
                }
                live.append(e.getKey()).append(": ").append(e.getValue());
            }
            liveLabel.setText(live.toString());
        }
    }

    private static void addIfPresent(
        LiveChartPanel chart,
        Map<String, Double> numbers,
        String key,
        String series
    ) {
        Double value = numbers.get(key);
        if (value != null) {
            chart.addPoint(series, value.doubleValue());
        }
    }

    private void finishRun(PerfRunHandle handle) {
        pollTimer.stop();
        currentRun = null;
        liveLabel.setText(" ");
        com.fasterxml.jackson.databind.JsonNode meta = PerfReportStore.runMeta(handle.runDir);
        boolean thresholdsFailed = meta != null && meta.path("thresholdsFailed").asBoolean(false);
        if (thresholdsFailed) {
            statusPill.setStatus(
                "FINISHED " + handle.runId + " — THRESHOLDS FAILED",
                StatusPill.Kind.FAILED
            );
        } else {
            statusPill.setStatus("FINISHED " + handle.runId + " — OK", StatusPill.Kind.OK);
        }
        outputArea.setText(summaryText(handle.runDir));
        updateReportLink(handle.runDir);
        reloadHistoryOnly();
    }

    /** Show the clickable report link when the run produced an HTML report. */
    private void updateReportLink(File runDir) {
        File report = new File(runDir, "report.html");
        lastReportFile = report.isFile() ? report : null;
        reportLink.setVisible(lastReportFile != null);
        reportLink.setToolTipText(lastReportFile == null ? null : lastReportFile.getAbsolutePath());
    }

    private String summaryText(File runDir) {
        StringBuilder sb = new StringBuilder();
        Map<String, String> headline = PerfReportStore.headline(runDir);
        for (Map.Entry<String, String> e : headline.entrySet()) {
            sb.append(String.format("%-16s %s%n", e.getKey(), e.getValue()));
        }
        Map<String, Boolean> thresholds = PerfReportStore.thresholds(runDir);
        if (!thresholds.isEmpty()) {
            sb.append('\n');
            for (Map.Entry<String, Boolean> e : thresholds.entrySet()) {
                sb
                    .append(e.getValue().booleanValue() ? "PASS  " : "FAIL  ")
                    .append(e.getKey())
                    .append('\n');
            }
        }
        File report = new File(runDir, "report.html");
        if (report.isFile()) {
            sb.append("\nHTML report: ").append(report.getAbsolutePath()).append('\n');
        }
        if (sb.length() == 0) {
            sb.append("No summary yet: ").append(runDir);
        }
        return sb.toString();
    }

    private void reloadHistoryOnly() {
        SwingUtilities.invokeLater(this::reload);
    }

    private static String statusFromExit(com.fasterxml.jackson.databind.JsonNode meta) {
        if (!meta.has("exitCode")) {
            return "?";
        }
        int exit = meta.path("exitCode").asInt();
        if (exit == 0) {
            return "PASS";
        }
        return exit == 99 ? "THRESHOLDS" : "EXIT " + exit;
    }

    private static void openInBrowser(String url) {
        try {
            if (
                java.awt.Desktop.isDesktopSupported() &&
                java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)
            ) {
                java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
            }
        } catch (Exception ignored) {
            // URL is visible in the status area anyway
        }
    }
}
