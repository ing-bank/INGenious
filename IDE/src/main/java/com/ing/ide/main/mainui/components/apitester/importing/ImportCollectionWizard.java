package com.ing.ide.main.mainui.components.apitester.importing;

import com.ing.datalib.api.importer.ImportOptions;
import com.ing.datalib.api.importer.ImportResult;
import com.ing.datalib.api.importer.ImportSource;
import com.ing.datalib.api.importer.NormalizedCollection;
import com.ing.engine.support.DesktopApi;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Modal wizard for "Tools → Import Collection" → Postman / Bruno.
 *
 * <p>Pages: 1) Source 2) Target 3) Confirm 4) Result.
 * The wizard is intentionally Swing-only so it can be hosted inside the existing
 * Swing main frame regardless of whether the user is on the FX or Swing menu bar.</p>
 */
public class ImportCollectionWizard extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(ImportCollectionWizard.class.getName());

    private final ImportSource initialSource;

    private File selectedFile;
    private final JTextField fileField = new JTextField(40);

    private final JTextField targetScenario = new JTextField(25);
    private final JTextField scenarioPrefix = new JTextField(10);
    private final JComboBox<ImportOptions.HierarchyStrategy> hierarchy = new JComboBox<>(
        ImportOptions.HierarchyStrategy.values()
    );
    private final JComboBox<ImportOptions.ConflictPolicy> conflict = new JComboBox<>(
        ImportOptions.ConflictPolicy.values()
    );
    private final JComboBox<ImportOptions.NamingConvention> namingConvention = new JComboBox<>(
        ImportOptions.NamingConvention.values()
    );
    private final JCheckBox importEnv = new JCheckBox("Import Environments", true);

    private final JRadioButton postmanRadio = new JRadioButton("Postman");
    private final JRadioButton brunoRadio = new JRadioButton("Bruno");

    private final JRadioButton targetReusableRadio = new JRadioButton(
        "Reusable (User Intent)",
        true
    );
    private final JRadioButton targetTestCaseRadio = new JRadioButton("Test Case");

    private boolean confirmed;

    public ImportCollectionWizard(Frame owner, ImportSource initialSource) {
        super(owner, "Import Collection", true);
        this.initialSource = initialSource == null ? ImportSource.POSTMAN : initialSource;
        buildUI();
        installEscapeCloseHandler();
        pack();
        setLocationRelativeTo(owner);
    }

    private void installEscapeCloseHandler() {
        getRootPane()
            .registerKeyboardAction(
                e -> {
                    confirmed = false;
                    dispose();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
            );
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ── Source row ──
        JPanel src = new JPanel();
        src.setLayout(new BoxLayout(src, BoxLayout.Y_AXIS));
        src.setBorder(BorderFactory.createTitledBorder("Source"));

        JPanel fmt = new JPanel(new FlowLayout(FlowLayout.LEFT));
        ButtonGroup g = new ButtonGroup();
        g.add(postmanRadio);
        g.add(brunoRadio);
        if (initialSource == ImportSource.BRUNO) brunoRadio.setSelected(
            true
        ); else postmanRadio.setSelected(true);
        fmt.add(new JLabel("Format:"));
        fmt.add(postmanRadio);
        fmt.add(brunoRadio);
        src.add(fmt);

        JPanel chooser = new JPanel(new BorderLayout(5, 0));
        JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        labelPanel.add(new JLabel("File / Folder:"));
        chooser.add(labelPanel, BorderLayout.NORTH);

        JPanel pathPanel = new JPanel(new BorderLayout(5, 0));
        fileField.setToolTipText("Type or paste a path, or use Browse…");
        fileField
            .getDocument()
            .addDocumentListener(
                new javax.swing.event.DocumentListener() {

                    @Override
                    public void insertUpdate(javax.swing.event.DocumentEvent e) {
                        syncFromField();
                    }

                    @Override
                    public void removeUpdate(javax.swing.event.DocumentEvent e) {
                        syncFromField();
                    }

                    @Override
                    public void changedUpdate(javax.swing.event.DocumentEvent e) {
                        syncFromField();
                    }

                    private void syncFromField() {
                        String t = fileField.getText();
                        selectedFile =
                            (t == null || t.trim().isEmpty()) ? null : new File(t.trim());
                    }
                }
            );
        pathPanel.add(fileField, BorderLayout.CENTER);
        JButton browseBtn = new JButton("Browse");
        browseBtn.addActionListener(e -> choosePath());
        pathPanel.add(browseBtn, BorderLayout.EAST);
        chooser.add(pathPanel, BorderLayout.CENTER);
        chooser.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        src.add(chooser);

        main.add(src, BorderLayout.NORTH);

        // ── Options ──
        JPanel opts = new JPanel(new GridLayout(0, 2, 8, 6));
        opts.setBorder(BorderFactory.createTitledBorder("Target"));

        ButtonGroup tgtGroup = new ButtonGroup();
        tgtGroup.add(targetReusableRadio);
        tgtGroup.add(targetTestCaseRadio);
        JPanel tgtPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tgtPanel.add(targetReusableRadio);
        tgtPanel.add(targetTestCaseRadio);
        opts.add(new JLabel("Import as:"));
        opts.add(tgtPanel);

        opts.add(new JLabel("Hierarchy strategy:"));
        opts.add(hierarchy);
        hierarchy.setSelectedItem(ImportOptions.HierarchyStrategy.SCENARIO_PER_TOP_FOLDER);
        opts.add(new JLabel("Naming convention:"));
        opts.add(namingConvention);
        namingConvention.setSelectedItem(ImportOptions.NamingConvention.PASCAL_CASE);
        namingConvention.setToolTipText(
            "<html>Naming style for generated scenarios, test cases, and datasheets:<br>" +
            "- PascalCase: CustomerManagementApis<br>" +
            "- camelCase: customerManagementApis<br>" +
            "- snake_case: customer_management_apis</html>"
        );
        opts.add(new JLabel("Scenario name prefix:"));
        opts.add(scenarioPrefix);
        opts.add(new JLabel("Target scenario (optional):"));
        opts.add(targetScenario);
        opts.add(new JLabel("Conflict policy:"));
        opts.add(conflict);
        opts.add(new JLabel(""));
        opts.add(importEnv);
        importEnv.setToolTipText(
            "<html>When enabled:<br>" +
            "- Creates a datasheet named after the collection<br>" +
            "- Creates data environments for each Postman environment file<br>" +
            "- Populates columns from environment variable keys<br>" +
            "- Converts %var% and {{var}} to {Datasheet:Column} syntax</html>"
        );
        conflict.setSelectedItem(ImportOptions.ConflictPolicy.RENAME_SUFFIX);

        main.add(opts, BorderLayout.CENTER);

        // ── Buttons ──
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(
            e -> {
                confirmed = false;
                dispose();
            }
        );
        JButton ok = new JButton("Import");
        ok.addActionListener(
            e -> {
                if (selectedFile == null) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Please choose a collection file or folder.",
                        "Import Collection",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                if (!selectedFile.exists()) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Path does not exist:\n" + selectedFile.getAbsolutePath(),
                        "Import Collection",
                        JOptionPane.WARNING_MESSAGE
                    );
                    return;
                }
                confirmed = true;
                dispose();
            }
        );
        btns.add(Box.createHorizontalGlue());
        btns.add(cancel);
        btns.add(ok);
        main.add(btns, BorderLayout.SOUTH);

        setContentPane(main);
        setPreferredSize(new Dimension(640, 420));
    }

    private void choosePath() {
        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        if (brunoRadio.isSelected()) {
            fc.setDialogTitle("Select Bruno collection file or folder");
        } else {
            fc.setDialogTitle("Select Postman collection file or folder");
        }
        fc.setFileFilter(new FileNameExtensionFilter("JSON files (*.json)", "json"));
        fc.setAcceptAllFileFilterUsed(true);
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            fileField.setText(selectedFile.getAbsolutePath());
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public File getSelectedFile() {
        return selectedFile;
    }

    public ImportSource getSource() {
        return brunoRadio.isSelected() ? ImportSource.BRUNO : ImportSource.POSTMAN;
    }

    public ImportOptions getOptions() {
        ImportOptions o = new ImportOptions();
        o.setHierarchyStrategy((ImportOptions.HierarchyStrategy) hierarchy.getSelectedItem());
        o.setConflictPolicy((ImportOptions.ConflictPolicy) conflict.getSelectedItem());
        o.setNamingConvention((ImportOptions.NamingConvention) namingConvention.getSelectedItem());
        o.setTargetType(
            targetTestCaseRadio.isSelected()
                ? ImportOptions.TargetType.TEST_CASE
                : ImportOptions.TargetType.REUSABLE
        );
        o.setScenarioPrefix(scenarioPrefix.getText());
        String tgt = targetScenario.getText();
        if (tgt != null && !tgt.trim().isEmpty()) o.setTargetScenarioName(tgt.trim());
        o.setImportEnvironments(importEnv.isSelected());
        return o;
    }

    /**
     * Displays the HTML import report viewer with navigation sidebar.
     * Shows index.html as home page with a list of individual import reports.
     */
    public static void showResult(
        Frame owner,
        NormalizedCollection nc,
        ImportResult result,
        File reportFile
    ) {
        SwingUtilities.invokeLater(
            () -> {
                JDialog d = new JDialog(owner, "Import Reports — INGenious", false);
                d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

                // Get reports directory
                File reportsDir = reportFile != null ? reportFile.getParentFile() : null;
                File indexFile = reportsDir != null ? new File(reportsDir, "index.html") : null;

                // Check if reports directory exists
                if (reportsDir == null || !reportsDir.exists()) {
                    showErrorDialog(
                        d,
                        "The import reports directory could not be found.\n" +
                        "Items created: " +
                        result.getReusablesCreated() +
                        "\n" +
                        "Items skipped: " +
                        result.getReusablesSkipped()
                    );
                    return;
                }

                // Create JFXPanel to host JavaFX content
                JFXPanel fxPanel = new JFXPanel();
                d.setLayout(new BorderLayout());
                d.add(fxPanel, BorderLayout.CENTER);

                // Initialize JavaFX scene on FX thread
                CountDownLatch sceneReady = new CountDownLatch(1);
                final File finalIndexFile = indexFile;
                final File finalReportFile = reportFile;

                Platform.runLater(
                    () -> {
                        try {
                            // ── WebView for rendering reports ──
                            WebView webView = new WebView();
                            WebEngine webEngine = webView.getEngine();

                            // ── Sidebar: Import Reports List ──
                            Label sidebarTitle = new Label("Import Reports");
                            sidebarTitle.setStyle(
                                "-fx-font-weight: bold; -fx-font-size: 14px; " +
                                "-fx-text-fill: #333; -fx-padding: 10 10 5 10;"
                            );

                            // Load list of HTML reports from directory
                            List<File> reportFiles = getReportFiles(reportsDir);
                            ListView<File> reportsList = new ListView<>(
                                FXCollections.observableArrayList(reportFiles)
                            );
                            reportsList.setCellFactory(
                                lv ->
                                    new ListCell<File>() {

                                        @Override
                                        protected void updateItem(File file, boolean empty) {
                                            super.updateItem(file, empty);
                                            if (empty || file == null) {
                                                setText(null);
                                                setGraphic(null);
                                            } else {
                                                String name = file.getName();
                                                // Format: yyyyMMdd-HHmmss-CollectionName.html
                                                if (
                                                    name.endsWith(".html") &&
                                                    !name.equals("index.html")
                                                ) {
                                                    String display = formatReportName(name);
                                                    setText(display);
                                                } else {
                                                    setText(name);
                                                }
                                            }
                                        }
                                    }
                            );

                            // Handle report selection
                            reportsList
                                .getSelectionModel()
                                .selectedItemProperty()
                                .addListener(
                                    (obs, oldVal, newVal) -> {
                                        if (newVal != null && newVal.exists()) {
                                            webEngine.load(newVal.toURI().toString());
                                        }
                                    }
                                );

                            VBox sidebar = new VBox(5, sidebarTitle, reportsList);
                            sidebar.setStyle(
                                "-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 1 0 0;"
                            );
                            sidebar.setPrefWidth(220);
                            sidebar.setMinWidth(180);
                            VBox.setVgrow(reportsList, Priority.ALWAYS);

                            // ── Navigation Toolbar ──
                            Button backBtn = new Button("◀ Back");
                            backBtn.setTooltip(new Tooltip("Navigate Back"));
                            backBtn.setOnAction(
                                e -> {
                                    if (webEngine.getHistory().getCurrentIndex() > 0) {
                                        webEngine.getHistory().go(-1);
                                    }
                                }
                            );

                            Button forwardBtn = new Button("Forward ▶");
                            forwardBtn.setTooltip(new Tooltip("Navigate Forward"));
                            forwardBtn.setOnAction(
                                e -> {
                                    if (
                                        webEngine.getHistory().getCurrentIndex() <
                                        webEngine.getHistory().getEntries().size() -
                                        1
                                    ) {
                                        webEngine.getHistory().go(1);
                                    }
                                }
                            );

                            Button homeBtn = new Button("⌂ Home");
                            homeBtn.setTooltip(new Tooltip("Import History (index.html)"));
                            homeBtn.setStyle("-fx-font-weight: bold;");
                            homeBtn.setOnAction(
                                e -> {
                                    if (finalIndexFile != null && finalIndexFile.exists()) {
                                        reportsList.getSelectionModel().clearSelection();
                                        webEngine.load(finalIndexFile.toURI().toString());
                                    }
                                }
                            );

                            Button openInBrowserBtn = new Button("Open in Browser ↗");
                            openInBrowserBtn.setTooltip(
                                new Tooltip("Open current page in external browser")
                            );
                            openInBrowserBtn.setOnAction(
                                e -> {
                                    try {
                                        String currentUrl = webEngine.getLocation();
                                        if (currentUrl != null && currentUrl.startsWith("file:")) {
                                            File currentFile = new File(
                                                java.net.URI.create(currentUrl)
                                            );
                                            DesktopApi.open(currentFile);
                                        }
                                    } catch (Exception ex) {
                                        LOG.log(
                                            Level.WARNING,
                                            "Failed to open report in browser",
                                            ex
                                        );
                                    }
                                }
                            );

                            Button closeBtn = new Button("Close");
                            closeBtn.setTooltip(new Tooltip("Close this dialog"));
                            closeBtn.setOnAction(e -> SwingUtilities.invokeLater(d::dispose));

                            Label statusLabel = new Label("Loading...");
                            statusLabel.setStyle("-fx-text-fill: #666;");

                            // Update status on page load
                            webEngine
                                .getLoadWorker()
                                .stateProperty()
                                .addListener(
                                    (obs, oldState, newState) -> {
                                        switch (newState) {
                                            case SUCCEEDED:
                                                String title = webEngine.getTitle();
                                                statusLabel.setText(
                                                    title != null ? title : "Report loaded"
                                                );
                                                break;
                                            case FAILED:
                                                statusLabel.setText("Failed to load report");
                                                break;
                                            case RUNNING:
                                                statusLabel.setText("Loading...");
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                );

                            // Layout toolbar
                            Region spacer = new Region();
                            HBox.setHgrow(spacer, Priority.ALWAYS);

                            ToolBar toolbar = new ToolBar(
                                backBtn,
                                forwardBtn,
                                homeBtn,
                                spacer,
                                statusLabel,
                                new Region() {

                                    {
                                        HBox.setHgrow(this, Priority.ALWAYS);
                                    }
                                },
                                openInBrowserBtn,
                                closeBtn
                            );
                            toolbar.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 5;");

                            // ── Main content area with WebView ──
                            BorderPane contentPane = new BorderPane();
                            contentPane.setTop(toolbar);
                            contentPane.setCenter(webView);

                            // ── SplitPane: Sidebar + Content ──
                            SplitPane splitPane = new SplitPane(sidebar, contentPane);
                            splitPane.setOrientation(Orientation.HORIZONTAL);
                            splitPane.setDividerPositions(0.2);
                            SplitPane.setResizableWithParent(sidebar, false);

                            BorderPane root = new BorderPane(splitPane);
                            root.setStyle("-fx-background-color: white;");

                            Scene scene = new Scene(root);
                            fxPanel.setScene(scene);

                            // Load initial page: index.html if exists, otherwise the report file
                            if (finalIndexFile != null && finalIndexFile.exists()) {
                                webEngine.load(finalIndexFile.toURI().toString());
                            } else if (finalReportFile != null && finalReportFile.exists()) {
                                webEngine.load(finalReportFile.toURI().toString());
                            }
                        } catch (Exception ex) {
                            LOG.log(Level.SEVERE, "Failed to initialize report viewer", ex);
                        } finally {
                            sceneReady.countDown();
                        }
                    }
                );

                // Wait for scene to be ready before showing dialog
                try {
                    sceneReady.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Configure and show dialog
                d
                    .getRootPane()
                    .registerKeyboardAction(
                        e -> d.dispose(),
                        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                        JComponent.WHEN_IN_FOCUSED_WINDOW
                    );
                d.setSize(1100, 750);
                d.setLocationRelativeTo(owner);
                d.setVisible(true);
            }
        );
    }

    /**
     * Gets all HTML report files from the reports directory, sorted by name descending (newest first).
     */
    private static List<File> getReportFiles(File reportsDir) {
        List<File> reports = new ArrayList<>();
        if (reportsDir == null || !reportsDir.isDirectory()) {
            return reports;
        }
        try (Stream<Path> paths = Files.list(reportsDir.toPath())) {
            paths
                .filter(p -> p.toString().endsWith(".html"))
                .filter(p -> !p.getFileName().toString().equals("index.html"))
                .sorted(Comparator.reverseOrder())
                .forEach(p -> reports.add(p.toFile()));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to list report files", e);
        }
        return reports;
    }

    /**
     * Formats a report filename for display in the sidebar.
     * Input: "20260804-143052-MyCollection.html"
     * Output: "2026-08-04 14:30 — MyCollection"
     */
    private static String formatReportName(String filename) {
        if (filename == null || filename.length() < 16) {
            return filename;
        }
        try {
            // Parse: yyyyMMdd-HHmmss-CollectionName.html
            String datePart = filename.substring(0, 8);
            String timePart = filename.substring(9, 15);
            String namePart = filename.substring(16, filename.length() - 5);

            String formatted = String.format(
                "%s-%s-%s %s:%s — %s",
                datePart.substring(0, 4),
                datePart.substring(4, 6),
                datePart.substring(6, 8),
                timePart.substring(0, 2),
                timePart.substring(2, 4),
                namePart.replace("_", " ")
            );
            return formatted;
        } catch (Exception e) {
            return filename.replace(".html", "");
        }
    }

    /**
     * Shows an error dialog when the import report cannot be displayed.
     */
    private static void showErrorDialog(JDialog parent, String message) {
        JPanel errorPanel = new JPanel(new BorderLayout(10, 10));
        errorPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel iconLabel = new JLabel("⚠");
        iconLabel.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 48));
        iconLabel.setForeground(new java.awt.Color(255, 152, 0));
        errorPanel.add(iconLabel, BorderLayout.WEST);

        JLabel msgLabel = new JLabel(
            "<html><b>Import Report Unavailable</b><br><br>" +
            message.replace("\n", "<br>") +
            "</html>"
        );
        msgLabel.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 14));
        errorPanel.add(msgLabel, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> parent.dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(closeBtn);
        errorPanel.add(btnPanel, BorderLayout.SOUTH);

        parent.add(errorPanel);
        parent
            .getRootPane()
            .registerKeyboardAction(
                e -> parent.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
            );
        parent.setSize(450, 250);
        parent.setLocationRelativeTo(parent.getOwner());
        parent.setVisible(true);
    }

    // ── Static reference to track open reports viewer to avoid duplicates ──
    private static JDialog openReportsDialog = null;

    /**
     * Opens the Import Reports viewer showing all available import reports.
     * This allows users to access reports after closing the original report window.
     *
     * <p>If the reports viewer is already open, it brings the existing window into focus
     * instead of creating a duplicate.</p>
     *
     * @param owner the parent frame
     * @param projectLocation the project root directory path
     */
    public static void openReportsViewer(Frame owner, String projectLocation) {
        // Check if dialog is already open
        if (openReportsDialog != null && openReportsDialog.isVisible()) {
            openReportsDialog.toFront();
            openReportsDialog.requestFocus();
            return;
        }

        // Build path to import-reports directory
        Path reportsPath = java.nio.file.Paths.get(projectLocation, "api", "import-reports");
        File reportsDir = reportsPath.toFile();
        File indexFile = new File(reportsDir, "index.html");

        SwingUtilities.invokeLater(
            () -> {
                JDialog d = new JDialog(owner, "Import Reports — INGenious", false);
                openReportsDialog = d;
                d.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                d.addWindowListener(
                    new java.awt.event.WindowAdapter() {

                        @Override
                        public void windowClosed(java.awt.event.WindowEvent e) {
                            openReportsDialog = null;
                        }
                    }
                );

                // Check if reports directory exists and has reports
                if (!reportsDir.exists() || !reportsDir.isDirectory()) {
                    showNoReportsDialog(
                        d,
                        "No import reports found.\n\n" +
                        "Import reports will be available after you import a Postman or Bruno collection."
                    );
                    return;
                }

                List<File> reportFiles = getReportFiles(reportsDir);
                if (reportFiles.isEmpty() && !indexFile.exists()) {
                    showNoReportsDialog(
                        d,
                        "No import reports found.\n\n" +
                        "Import reports will be available after you import a Postman or Bruno collection."
                    );
                    return;
                }

                // Find the most recent report to use as fallback if index.html doesn't exist
                File latestReport = reportFiles.isEmpty() ? null : reportFiles.get(0);

                // Create JFXPanel to host JavaFX content
                JFXPanel fxPanel = new JFXPanel();
                d.setLayout(new BorderLayout());
                d.add(fxPanel, BorderLayout.CENTER);

                // Initialize JavaFX scene on FX thread
                CountDownLatch sceneReady = new CountDownLatch(1);

                Platform.runLater(
                    () -> {
                        try {
                            // ── WebView for rendering reports ──
                            WebView webView = new WebView();
                            WebEngine webEngine = webView.getEngine();

                            // ── Sidebar: Import Reports List ──
                            Label sidebarTitle = new Label("Import Reports");
                            sidebarTitle.setStyle(
                                "-fx-font-weight: bold; -fx-font-size: 14px; " +
                                "-fx-text-fill: #333; -fx-padding: 10 10 5 10;"
                            );

                            ListView<File> reportsList = new ListView<>(
                                FXCollections.observableArrayList(reportFiles)
                            );
                            reportsList.setCellFactory(
                                lv ->
                                    new ListCell<File>() {

                                        @Override
                                        protected void updateItem(File file, boolean empty) {
                                            super.updateItem(file, empty);
                                            if (empty || file == null) {
                                                setText(null);
                                                setGraphic(null);
                                            } else {
                                                String name = file.getName();
                                                if (
                                                    name.endsWith(".html") &&
                                                    !name.equals("index.html")
                                                ) {
                                                    setText(formatReportName(name));
                                                } else {
                                                    setText(name);
                                                }
                                            }
                                        }
                                    }
                            );

                            // Handle report selection
                            reportsList
                                .getSelectionModel()
                                .selectedItemProperty()
                                .addListener(
                                    (obs, oldVal, newVal) -> {
                                        if (newVal != null && newVal.exists()) {
                                            webEngine.load(newVal.toURI().toString());
                                        }
                                    }
                                );

                            VBox sidebar = new VBox(5, sidebarTitle, reportsList);
                            sidebar.setStyle(
                                "-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-width: 0 1 0 0;"
                            );
                            sidebar.setPrefWidth(220);
                            sidebar.setMinWidth(180);
                            VBox.setVgrow(reportsList, Priority.ALWAYS);

                            // ── Navigation Toolbar ──
                            Button backBtn = new Button("◀ Back");
                            backBtn.setTooltip(new Tooltip("Navigate Back"));
                            backBtn.setOnAction(
                                e -> {
                                    if (webEngine.getHistory().getCurrentIndex() > 0) {
                                        webEngine.getHistory().go(-1);
                                    }
                                }
                            );

                            Button forwardBtn = new Button("Forward ▶");
                            forwardBtn.setTooltip(new Tooltip("Navigate Forward"));
                            forwardBtn.setOnAction(
                                e -> {
                                    if (
                                        webEngine.getHistory().getCurrentIndex() <
                                        webEngine.getHistory().getEntries().size() -
                                        1
                                    ) {
                                        webEngine.getHistory().go(1);
                                    }
                                }
                            );

                            Button homeBtn = new Button("⌂ Home");
                            homeBtn.setTooltip(new Tooltip("Import History (index.html)"));
                            homeBtn.setStyle("-fx-font-weight: bold;");
                            homeBtn.setOnAction(
                                e -> {
                                    if (indexFile.exists()) {
                                        reportsList.getSelectionModel().clearSelection();
                                        webEngine.load(indexFile.toURI().toString());
                                    }
                                }
                            );

                            Button openInBrowserBtn = new Button("Open in Browser ↗");
                            openInBrowserBtn.setTooltip(
                                new Tooltip("Open current page in external browser")
                            );
                            openInBrowserBtn.setOnAction(
                                e -> {
                                    try {
                                        String currentUrl = webEngine.getLocation();
                                        if (currentUrl != null && currentUrl.startsWith("file:")) {
                                            File currentFile = new File(
                                                java.net.URI.create(currentUrl)
                                            );
                                            DesktopApi.open(currentFile);
                                        }
                                    } catch (Exception ex) {
                                        LOG.log(
                                            Level.WARNING,
                                            "Failed to open report in browser",
                                            ex
                                        );
                                    }
                                }
                            );

                            Button closeBtn = new Button("Close");
                            closeBtn.setTooltip(new Tooltip("Close this dialog"));
                            closeBtn.setOnAction(e -> SwingUtilities.invokeLater(d::dispose));

                            Label statusLabel = new Label("Loading...");
                            statusLabel.setStyle("-fx-text-fill: #666;");

                            // Update status on page load
                            webEngine
                                .getLoadWorker()
                                .stateProperty()
                                .addListener(
                                    (obs, oldState, newState) -> {
                                        switch (newState) {
                                            case SUCCEEDED:
                                                String title = webEngine.getTitle();
                                                statusLabel.setText(
                                                    title != null ? title : "Report loaded"
                                                );
                                                break;
                                            case FAILED:
                                                statusLabel.setText("Failed to load report");
                                                break;
                                            case RUNNING:
                                                statusLabel.setText("Loading...");
                                                break;
                                            default:
                                                break;
                                        }
                                    }
                                );

                            // Layout toolbar
                            Region spacer = new Region();
                            HBox.setHgrow(spacer, Priority.ALWAYS);

                            ToolBar toolbar = new ToolBar(
                                backBtn,
                                forwardBtn,
                                homeBtn,
                                spacer,
                                statusLabel,
                                new Region() {

                                    {
                                        HBox.setHgrow(this, Priority.ALWAYS);
                                    }
                                },
                                openInBrowserBtn,
                                closeBtn
                            );
                            toolbar.setStyle("-fx-background-color: #f5f5f5; -fx-padding: 5;");

                            // ── Main content area with WebView ──
                            BorderPane contentPane = new BorderPane();
                            contentPane.setTop(toolbar);
                            contentPane.setCenter(webView);

                            // ── SplitPane: Sidebar + Content ──
                            SplitPane splitPane = new SplitPane(sidebar, contentPane);
                            splitPane.setOrientation(Orientation.HORIZONTAL);
                            splitPane.setDividerPositions(0.2);
                            SplitPane.setResizableWithParent(sidebar, false);

                            BorderPane root = new BorderPane(splitPane);
                            root.setStyle("-fx-background-color: white;");

                            Scene scene = new Scene(root);
                            fxPanel.setScene(scene);

                            // Load initial page: index.html if exists, otherwise the latest report
                            if (indexFile.exists()) {
                                webEngine.load(indexFile.toURI().toString());
                            } else if (latestReport != null && latestReport.exists()) {
                                webEngine.load(latestReport.toURI().toString());
                            }
                        } catch (Exception ex) {
                            LOG.log(Level.SEVERE, "Failed to initialize report viewer", ex);
                        } finally {
                            sceneReady.countDown();
                        }
                    }
                );

                // Wait for scene to be ready before showing dialog
                try {
                    sceneReady.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Configure and show dialog
                d
                    .getRootPane()
                    .registerKeyboardAction(
                        e -> d.dispose(),
                        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                        JComponent.WHEN_IN_FOCUSED_WINDOW
                    );
                d.setSize(1100, 750);
                d.setLocationRelativeTo(owner);
                d.setVisible(true);
            }
        );
    }

    /**
     * Shows a dialog when no import reports are available.
     */
    private static void showNoReportsDialog(JDialog parent, String message) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel iconLabel = new JLabel("📋");
        iconLabel.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 48));
        panel.add(iconLabel, BorderLayout.WEST);

        JLabel msgLabel = new JLabel(
            "<html><b>Import Reports</b><br><br>" + message.replace("\n", "<br>") + "</html>"
        );
        msgLabel.setFont(new java.awt.Font("Dialog", java.awt.Font.PLAIN, 14));
        panel.add(msgLabel, BorderLayout.CENTER);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(e -> parent.dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.add(closeBtn);
        panel.add(btnPanel, BorderLayout.SOUTH);

        parent.add(panel);
        parent
            .getRootPane()
            .registerKeyboardAction(
                e -> parent.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
            );
        parent.setSize(450, 250);
        parent.setLocationRelativeTo(parent.getOwner());
        parent.setVisible(true);
    }
}
