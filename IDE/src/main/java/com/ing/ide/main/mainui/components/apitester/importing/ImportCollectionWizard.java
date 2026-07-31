package com.ing.ide.main.mainui.components.apitester.importing;

import com.ing.datalib.api.importer.ImportOptions;
import com.ing.datalib.api.importer.ImportResult;
import com.ing.datalib.api.importer.ImportSource;
import com.ing.datalib.api.importer.ImportWarning;
import com.ing.datalib.api.importer.NormalizedCollection;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.io.File;
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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
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

    private final ImportSource initialSource;

    private File selectedFile;
    private final JTextField fileField = new JTextField(40);

    private final JTextField targetScenario = new JTextField(25);
    private final JTextField scenarioPrefix = new JTextField("API_", 10);
    private final JComboBox<ImportOptions.HierarchyStrategy> hierarchy = new JComboBox<>(
        ImportOptions.HierarchyStrategy.values()
    );
    private final JComboBox<ImportOptions.ConflictPolicy> conflict = new JComboBox<>(
        ImportOptions.ConflictPolicy.values()
    );
    private final JComboBox<ImportOptions.NamingConvention> namingConvention = new JComboBox<>(
        ImportOptions.NamingConvention.values()
    );
    private final JCheckBox importEnv = new JCheckBox("Import Environments", false);

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
        opts.add(new JLabel("Naming convention:"));
        opts.add(namingConvention);
        namingConvention.setSelectedItem(ImportOptions.NamingConvention.SNAKE_CASE);
        namingConvention.setToolTipText(
            "<html>Naming style for generated scenarios, test cases, and datasheets:<br>" +
            "- snake_case: customer_management_apis<br>" +
            "- PascalCase: CustomerManagementApis<br>" +
            "- camelCase: customerManagementApis</html>"
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

    /** Shows a non-modal result panel summarising an {@link ImportResult}. */
    public static void showResult(
        Frame owner,
        NormalizedCollection nc,
        ImportResult result,
        File reportFile
    ) {
        SwingUtilities.invokeLater(
            () -> {
                JDialog d = new JDialog(owner, "Import Result — " + nc.getName(), false);
                JTextArea ta = new JTextArea();
                ta.setEditable(false);
                ta.setFont(new java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12));
                StringBuilder sb = new StringBuilder();

                // Header
                sb.append("═══════════════════════════════════════════════════════════════\n");
                sb.append("                    IMPORT RESULT SUMMARY\n");
                sb.append("═══════════════════════════════════════════════════════════════\n\n");

                // Collection Info
                sb.append("► COLLECTION INFO\n");
                sb.append("  ├─ Name: ").append(nc.getName()).append('\n');
                sb.append("  ├─ Source: ").append(nc.getSource()).append('\n');
                sb.append("  └─ Requests found: ").append(result.getRequestsRead()).append('\n');
                sb.append('\n');

                // Import Results
                sb.append("► IMPORT RESULTS\n");
                sb.append("  ├─ Items created: ").append(result.getReusablesCreated()).append('\n');
                sb.append("  └─ Items skipped: ").append(result.getReusablesSkipped()).append('\n');
                sb.append('\n');

                // Created Scenarios
                if (!result.getCreatedScenarios().isEmpty()) {
                    sb.append("► CREATED SCENARIOS\n");
                    for (int i = 0; i < result.getCreatedScenarios().size(); i++) {
                        String prefix = (i == result.getCreatedScenarios().size() - 1)
                            ? "  └─ "
                            : "  ├─ ";
                        sb.append(prefix).append(result.getCreatedScenarios().get(i)).append('\n');
                    }
                    sb.append('\n');
                }

                // Created Items
                if (!result.getCreatedReusables().isEmpty()) {
                    sb.append("► CREATED ITEMS\n");
                    int limit = Math.min(result.getCreatedReusables().size(), 20);
                    for (int i = 0; i < limit; i++) {
                        String prefix = (
                                i == limit - 1 && limit == result.getCreatedReusables().size()
                            )
                            ? "  └─ "
                            : "  ├─ ";
                        sb.append(prefix).append(result.getCreatedReusables().get(i)).append('\n');
                    }
                    if (result.getCreatedReusables().size() > 20) {
                        sb
                            .append("  └─ ... and ")
                            .append(result.getCreatedReusables().size() - 20)
                            .append(" more\n");
                    }
                    sb.append('\n');
                }

                // Datasheet import results
                if (result.getDatasheetsCreated() > 0 || result.getDataEnvironmentsCreated() > 0) {
                    sb.append("► DATASHEET IMPORT\n");
                    if (result.getDatasheetName() != null) {
                        sb
                            .append("  ├─ Datasheet name: ")
                            .append(result.getDatasheetName())
                            .append('\n');
                    }
                    sb
                        .append("  ├─ Datasheets created: ")
                        .append(result.getDatasheetsCreated())
                        .append('\n');
                    sb
                        .append("  ├─ Data environments created: ")
                        .append(result.getDataEnvironmentsCreated())
                        .append('\n');
                    sb
                        .append("  ├─ Columns created: ")
                        .append(result.getDatasheetColumnsCreated())
                        .append('\n');
                    sb
                        .append("  └─ Rows created: ")
                        .append(result.getDatasheetRowsCreated())
                        .append('\n');
                    sb.append('\n');

                    if (!result.getCreatedDataEnvironments().isEmpty()) {
                        sb.append("► DATA ENVIRONMENTS\n");
                        for (int i = 0; i < result.getCreatedDataEnvironments().size(); i++) {
                            String prefix = (i == result.getCreatedDataEnvironments().size() - 1)
                                ? "  └─ "
                                : "  ├─ ";
                            sb
                                .append(prefix)
                                .append(result.getCreatedDataEnvironments().get(i))
                                .append('\n');
                        }
                        sb.append('\n');
                    }
                }

                // Report file
                if (reportFile != null) {
                    sb.append("► REPORT\n");
                    sb.append("  └─ ").append(reportFile.getAbsolutePath()).append('\n');
                    sb.append('\n');
                }

                // Warnings
                sb.append("► WARNINGS\n");
                if (result.getWarnings().isEmpty()) {
                    sb.append("  └─ (none)\n");
                } else {
                    for (int i = 0; i < result.getWarnings().size(); i++) {
                        ImportWarning w = result.getWarnings().get(i);
                        String prefix = (i == result.getWarnings().size() - 1) ? "  └─ " : "  ├─ ";
                        sb
                            .append(prefix)
                            .append("[")
                            .append(w.getSeverity())
                            .append("] ")
                            .append(w.getLocation())
                            .append(" — ")
                            .append(w.getMessage())
                            .append('\n');
                    }
                }

                sb.append('\n');
                sb.append("═══════════════════════════════════════════════════════════════\n");
                sb.append("                      IMPORT COMPLETE\n");
                sb.append("═══════════════════════════════════════════════════════════════\n");

                ta.setText(sb.toString());
                ta.setCaretPosition(0);
                d.add(new JScrollPane(ta));
                d
                    .getRootPane()
                    .registerKeyboardAction(
                        e -> d.dispose(),
                        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                        JComponent.WHEN_IN_FOCUSED_WINDOW
                    );
                d.setSize(750, 550);
                d.setLocationRelativeTo(owner);
                d.setVisible(true);
            }
        );
    }
}
