package com.ing.ide.main.mainui.components.apitester.importing;

import com.ing.datalib.api.importer.ImportOptions;
import com.ing.datalib.api.importer.ImportResult;
import com.ing.datalib.api.importer.ImportSource;
import com.ing.datalib.api.importer.ImportWarning;
import com.ing.datalib.api.importer.NormalizedCollection;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.io.File;

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
    private final JComboBox<ImportOptions.HierarchyStrategy> hierarchy =
            new JComboBox<>(ImportOptions.HierarchyStrategy.values());
    private final JComboBox<ImportOptions.ConflictPolicy> conflict =
            new JComboBox<>(ImportOptions.ConflictPolicy.values());
    private final JCheckBox importEnv = new JCheckBox("Import environments", true);

    private final JRadioButton postmanRadio = new JRadioButton("Postman");
    private final JRadioButton brunoRadio = new JRadioButton("Bruno");

    private final JRadioButton targetReusableRadio = new JRadioButton("Reusable (User Intent)", true);
    private final JRadioButton targetTestCaseRadio = new JRadioButton("Test Case");

    private boolean confirmed;

    public ImportCollectionWizard(Frame owner, ImportSource initialSource) {
        super(owner, "Import Collection", true);
        this.initialSource = initialSource == null ? ImportSource.POSTMAN : initialSource;
        buildUI();
        pack();
        setLocationRelativeTo(owner);
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
        if (initialSource == ImportSource.BRUNO) brunoRadio.setSelected(true);
        else postmanRadio.setSelected(true);
        fmt.add(new JLabel("Format:"));
        fmt.add(postmanRadio);
        fmt.add(brunoRadio);
        src.add(fmt);

        JPanel chooser = new JPanel(new FlowLayout(FlowLayout.LEFT));
        chooser.add(new JLabel("File / Folder:"));
        fileField.setToolTipText("Type or paste a path, or use Browse…");
        fileField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            @Override public void insertUpdate(javax.swing.event.DocumentEvent e) { syncFromField(); }
            @Override public void removeUpdate(javax.swing.event.DocumentEvent e) { syncFromField(); }
            @Override public void changedUpdate(javax.swing.event.DocumentEvent e) { syncFromField(); }
            private void syncFromField() {
                String t = fileField.getText();
                selectedFile = (t == null || t.trim().isEmpty()) ? null : new File(t.trim());
            }
        });
        chooser.add(fileField);
        JButton browse = new JButton("Browse…");
        browse.addActionListener(e -> chooseFile());
        chooser.add(browse);
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
        opts.add(new JLabel("Scenario name prefix:"));
        opts.add(scenarioPrefix);
        opts.add(new JLabel("Target scenario (optional):"));
        opts.add(targetScenario);
        opts.add(new JLabel("Conflict policy:"));
        opts.add(conflict);
        opts.add(new JLabel(""));
        opts.add(importEnv);
        conflict.setSelectedItem(ImportOptions.ConflictPolicy.RENAME_SUFFIX);

        main.add(opts, BorderLayout.CENTER);

        // ── Buttons ──
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> { confirmed = false; dispose(); });
        JButton ok = new JButton("Import");
        ok.addActionListener(e -> {
            if (selectedFile == null) {
                JOptionPane.showMessageDialog(this,
                        "Please choose a collection file or folder.",
                        "Import Collection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (!selectedFile.exists()) {
                JOptionPane.showMessageDialog(this,
                        "Path does not exist:\n" + selectedFile.getAbsolutePath(),
                        "Import Collection", JOptionPane.WARNING_MESSAGE);
                return;
            }
            confirmed = true;
            dispose();
        });
        btns.add(Box.createHorizontalGlue());
        btns.add(cancel);
        btns.add(ok);
        main.add(btns, BorderLayout.SOUTH);

        setContentPane(main);
        setPreferredSize(new Dimension(640, 380));
    }

    private void chooseFile() {
        JFileChooser fc = new JFileChooser();
        if (brunoRadio.isSelected()) {
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.setDialogTitle("Select Bruno collection folder (containing bruno.json) or bruno.json");
        } else {
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.setFileFilter(new FileNameExtensionFilter(
                    "Postman Collection (*.json) or folder containing it", "json"));
            fc.setDialogTitle("Select Postman collection JSON or its folder");
        }
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
            fileField.setText(selectedFile.getAbsolutePath());
        }
    }

    public boolean isConfirmed() { return confirmed; }
    public File getSelectedFile() { return selectedFile; }

    public ImportSource getSource() {
        return brunoRadio.isSelected() ? ImportSource.BRUNO : ImportSource.POSTMAN;
    }

    public ImportOptions getOptions() {
        ImportOptions o = new ImportOptions();
        o.setHierarchyStrategy((ImportOptions.HierarchyStrategy) hierarchy.getSelectedItem());
        o.setConflictPolicy((ImportOptions.ConflictPolicy) conflict.getSelectedItem());
        o.setTargetType(targetTestCaseRadio.isSelected()
                ? ImportOptions.TargetType.TEST_CASE
                : ImportOptions.TargetType.REUSABLE);
        o.setScenarioPrefix(scenarioPrefix.getText());
        String tgt = targetScenario.getText();
        if (tgt != null && !tgt.trim().isEmpty()) o.setTargetScenarioName(tgt.trim());
        o.setImportEnvironments(importEnv.isSelected());
        return o;
    }

    /** Shows a non-modal result panel summarising an {@link ImportResult}. */
    public static void showResult(Frame owner, NormalizedCollection nc, ImportResult result, File reportFile) {
        SwingUtilities.invokeLater(() -> {
            JDialog d = new JDialog(owner, "Import Result — " + nc.getName(), false);
            JTextArea ta = new JTextArea();
            ta.setEditable(false);
            StringBuilder sb = new StringBuilder();
            sb.append("Source: ").append(nc.getSource()).append('\n');
            sb.append("Requests read: ").append(result.getRequestsRead()).append('\n');
            sb.append("Items created: ").append(result.getReusablesCreated()).append('\n');
            sb.append("Items skipped: ").append(result.getReusablesSkipped()).append('\n');
            sb.append("Environments created: ").append(result.getEnvironmentsCreated()).append('\n');
            if (reportFile != null) {
                sb.append("Report: ").append(reportFile.getAbsolutePath()).append('\n');
            }
            sb.append('\n').append("Warnings:\n");
            if (result.getWarnings().isEmpty()) {
                sb.append("  (none)\n");
            } else {
                for (ImportWarning w : result.getWarnings()) {
                    sb.append("  [").append(w.getSeverity()).append("] ")
                            .append(w.getLocation()).append(" — ")
                            .append(w.getMessage()).append('\n');
                }
            }
            ta.setText(sb.toString());
            d.add(new JScrollPane(ta));
            d.setSize(700, 480);
            d.setLocationRelativeTo(owner);
            d.setVisible(true);
        });
    }
}
