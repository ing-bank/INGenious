package com.ing.ide.main.mainui.components.testdesign.testdata;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Modal dialog for the "Test Data &rarr; Import TestData" action.
 *
 * <p>Lets the user pick one or more CSV datasheets, choose whether to import them into the
 * <b>Project</b> or the <b>Shared</b> Test Data, and tick one or more environments to import
 * into. The caller reads the choices back through the getters after {@link #isConfirmed()}.</p>
 */
class ImportTestDataDialog extends JDialog {
    private final Set<String> projectEnvironments;
    private final Set<String> sharedEnvironments;

    private final JRadioButton projectScope = new JRadioButton("Project Test Data");
    private final JRadioButton sharedScope = new JRadioButton("Shared Test Data");
    private final JLabel filesLabel = new JLabel("No file selected");
    private final JLabel sharedWarning = new JLabel(
        "Shared Test Data is shared across every project that uses it."
    );
    private final JPanel environmentsPanel = new JPanel();
    private final JCheckBox selectAll = new JCheckBox("Select all");
    private final JButton importButton = new JButton("Import");

    private final List<JCheckBox> environmentChecks = new ArrayList<>();
    private File[] selectedFiles = new File[0];
    private boolean confirmed = false;

    private ImportTestDataDialog(
        Frame owner,
        boolean defaultShared,
        Set<String> projectEnvironments,
        Set<String> sharedEnvironments
    ) {
        super(owner, "Import Test Data", true);
        this.projectEnvironments = new LinkedHashSet<>(projectEnvironments);
        this.sharedEnvironments = new LinkedHashSet<>(sharedEnvironments);

        buildLayout();

        ButtonGroup scopeGroup = new ButtonGroup();
        scopeGroup.add(projectScope);
        scopeGroup.add(sharedScope);
        projectScope.addActionListener(e -> repopulateEnvironments());
        sharedScope.addActionListener(e -> repopulateEnvironments());
        selectAll.addActionListener(
            e -> {
                for (JCheckBox cb : environmentChecks) {
                    cb.setSelected(selectAll.isSelected());
                }
                updateImportEnabled();
            }
        );

        (defaultShared ? sharedScope : projectScope).setSelected(true);
        repopulateEnvironments();
        updateImportEnabled();

        pack();
        setMinimumSize(new Dimension(440, getHeight()));
        setLocationRelativeTo(owner);
    }

    /**
     * Builds, shows (modal) and returns the dialog. Inspect {@link #isConfirmed()} afterwards.
     *
     * @param owner parent frame for positioning / modality
     * @param defaultShared pre-select the Shared scope when true, otherwise Project
     * @param projectEnvironments environment names of the project's own Test Data
     * @param sharedEnvironments environment names of the Shared Test Data
     * @return the closed dialog carrying the user's choices
     */
    static ImportTestDataDialog showDialog(
        Frame owner,
        boolean defaultShared,
        Set<String> projectEnvironments,
        Set<String> sharedEnvironments
    ) {
        ImportTestDataDialog dialog = new ImportTestDataDialog(
            owner,
            defaultShared,
            projectEnvironments,
            sharedEnvironments
        );
        dialog.setVisible(true);
        return dialog;
    }

    private void buildLayout() {
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JButton browse = new JButton("Browse…");
        browse.addActionListener(e -> browseForFiles());
        JPanel filesRow = new JPanel(new BorderLayout(8, 0));
        filesRow.add(new JLabel("File(s):"), BorderLayout.WEST);
        filesRow.add(filesLabel, BorderLayout.CENTER);
        filesRow.add(browse, BorderLayout.EAST);
        filesRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        filesRow.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, filesRow.getPreferredSize().height)
        );

        JPanel scopeBox = new JPanel();
        scopeBox.setLayout(new BoxLayout(scopeBox, BoxLayout.Y_AXIS));
        scopeBox.setBorder(BorderFactory.createTitledBorder("Import into"));
        scopeBox.add(projectScope);
        scopeBox.add(sharedScope);
        sharedWarning.setBorder(BorderFactory.createEmptyBorder(4, 24, 0, 0));
        scopeBox.add(sharedWarning);
        scopeBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        environmentsPanel.setLayout(new BoxLayout(environmentsPanel, BoxLayout.Y_AXIS));
        JScrollPane envScroll = new JScrollPane(environmentsPanel);
        envScroll.setPreferredSize(new Dimension(380, 140));
        JPanel envBox = new JPanel(new BorderLayout(0, 4));
        envBox.setBorder(BorderFactory.createTitledBorder("Environments"));
        envBox.add(selectAll, BorderLayout.NORTH);
        envBox.add(envScroll, BorderLayout.CENTER);
        envBox.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(
            e -> {
                confirmed = false;
                dispose();
            }
        );
        importButton.addActionListener(
            e -> {
                confirmed = true;
                dispose();
            }
        );
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0));
        buttons.add(cancel);
        buttons.add(importButton);
        JPanel buttonRow = new JPanel(new BorderLayout());
        buttonRow.add(buttons, BorderLayout.EAST);
        buttonRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttonRow.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, buttonRow.getPreferredSize().height)
        );

        content.add(filesRow);
        content.add(Box.createVerticalStrut(10));
        content.add(scopeBox);
        content.add(Box.createVerticalStrut(10));
        content.add(envBox);
        content.add(Box.createVerticalStrut(12));
        content.add(buttonRow);

        setContentPane(content);
        getRootPane().setDefaultButton(importButton);
    }

    private void browseForFiles() {
        JFileChooser chooser = new JFileChooser();
        try {
            chooser.setCurrentDirectory(
                new File(new File(System.getProperty("user.dir")).getCanonicalPath())
            );
        } catch (IOException ignored) {
            // fall back to the chooser's default directory
        }
        chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooser.setMultiSelectionEnabled(true);
        chooser.setFileFilter(new FileNameExtensionFilter("TestData Files", "csv"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedFiles = chooser.getSelectedFiles();
            filesLabel.setText(describeSelection(selectedFiles));
            updateImportEnabled();
        }
    }

    private static String describeSelection(File[] files) {
        if (files == null || files.length == 0) {
            return "No file selected";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < files.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(files[i].getName());
        }
        return sb.toString();
    }

    private void repopulateEnvironments() {
        environmentsPanel.removeAll();
        environmentChecks.clear();
        Set<String> envs = sharedScope.isSelected() ? sharedEnvironments : projectEnvironments;
        for (String env : envs) {
            JCheckBox cb = new JCheckBox(env);
            cb.addActionListener(
                e -> {
                    syncSelectAllState();
                    updateImportEnabled();
                }
            );
            environmentChecks.add(cb);
            environmentsPanel.add(cb);
        }
        selectAll.setSelected(false);
        sharedWarning.setVisible(sharedScope.isSelected());
        environmentsPanel.revalidate();
        environmentsPanel.repaint();
        updateImportEnabled();
    }

    private void syncSelectAllState() {
        boolean all = !environmentChecks.isEmpty();
        for (JCheckBox cb : environmentChecks) {
            if (!cb.isSelected()) {
                all = false;
                break;
            }
        }
        selectAll.setSelected(all);
    }

    private void updateImportEnabled() {
        importButton.setEnabled(selectedFiles.length > 0 && !getSelectedEnvironments().isEmpty());
    }

    boolean isConfirmed() {
        return confirmed;
    }

    boolean isShared() {
        return sharedScope.isSelected();
    }

    File[] getSelectedFiles() {
        return selectedFiles;
    }

    List<String> getSelectedEnvironments() {
        List<String> selected = new ArrayList<>();
        for (JCheckBox cb : environmentChecks) {
            if (cb.isSelected()) {
                selected.add(cb.getText());
            }
        }
        return selected;
    }
}
