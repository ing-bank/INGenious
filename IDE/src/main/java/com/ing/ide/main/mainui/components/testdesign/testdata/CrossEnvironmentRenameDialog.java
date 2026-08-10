package com.ing.ide.main.mainui.components.testdesign.testdata;

import com.ing.ide.main.utils.Utils;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

/**
 * Dialog for confirming cross-environment datasheet rename operations.
 * Shows affected environments and allows user to select which ones to rename.
 */
public class CrossEnvironmentRenameDialog extends JDialog {
    private final List<String> affectedEnvironments;
    private final List<JCheckBox> environmentCheckBoxes;
    private boolean confirmed = false;
    private List<String> selectedEnvironments;

    /**
     * Creates a dialog for confirming cross-environment rename.
     *
     * @param parent parent frame
     * @param datasheetName name of the datasheet being renamed
     * @param currentEnv current environment where rename was initiated
     * @param affectedEnvironments list of other environments containing the datasheet
     */
    public CrossEnvironmentRenameDialog(
        Frame parent,
        String datasheetName,
        String currentEnv,
        List<String> affectedEnvironments
    ) {
        super(parent, "Cross-Environment Datasheet Rename", true);
        this.affectedEnvironments = affectedEnvironments;
        this.environmentCheckBoxes = new ArrayList<>();
        initComponents(datasheetName, currentEnv);
        pack();
        setLocationRelativeTo(parent);
    }

    private void initComponents(String datasheetName, String currentEnv) {
        setLayout(new BorderLayout(10, 10));
        setMinimumSize(new Dimension(500, 300));

        // Header panel with icon and message
        JPanel headerPanel = new JPanel(new BorderLayout(10, 10));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 10, 15));

        JLabel iconLabel = new JLabel(Utils.getIconByResourceName("/ui/resources/rename"));
        iconLabel.setVerticalAlignment(SwingConstants.TOP);
        headerPanel.add(iconLabel, BorderLayout.WEST);

        JPanel messagePanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 5, 0);
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel titleLabel = new JLabel(
            "<html><b>Datasheet with Same Name Found in Other Environments</b></html>"
        );
        messagePanel.add(titleLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(5, 0, 10, 0);
        JLabel descLabel = new JLabel(
            "<html>The datasheet <b>'" +
            datasheetName +
            "'</b> exists in multiple environments.<br>" +
            "Do you want to rename it in other environments as well?</html>"
        );
        messagePanel.add(descLabel, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 5, 0);
        JLabel currentEnvLabel = new JLabel(
            "<html><b>Current Environment:</b> " + currentEnv + "</html>"
        );
        messagePanel.add(currentEnvLabel, gbc);

        headerPanel.add(messagePanel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

        // Environment selection panel
        JPanel envPanel = new JPanel();
        envPanel.setLayout(new GridBagLayout());
        envPanel.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 15, 10, 15),
                BorderFactory.createTitledBorder("Affected Environments")
            )
        );

        gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(5, 10, 5, 10);
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JCheckBox selectAllCheckBox = new JCheckBox("Select All", true);
        selectAllCheckBox.setFont(selectAllCheckBox.getFont().deriveFont(java.awt.Font.BOLD));
        envPanel.add(selectAllCheckBox, gbc);

        for (String env : affectedEnvironments) {
            gbc.gridy++;
            JCheckBox checkBox = new JCheckBox(env, true);
            environmentCheckBoxes.add(checkBox);
            envPanel.add(checkBox, gbc);
        }

        // Handle select all functionality
        selectAllCheckBox.addActionListener(
            new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    boolean selected = selectAllCheckBox.isSelected();
                    for (JCheckBox cb : environmentCheckBoxes) {
                        cb.setSelected(selected);
                    }
                }
            }
        );

        // Add listener to update select all checkbox when individual checkboxes change
        ActionListener checkBoxListener = new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                boolean allSelected = true;
                for (JCheckBox cb : environmentCheckBoxes) {
                    if (!cb.isSelected()) {
                        allSelected = false;
                        break;
                    }
                }
                selectAllCheckBox.setSelected(allSelected);
            }
        };

        for (JCheckBox cb : environmentCheckBoxes) {
            cb.addActionListener(checkBoxListener);
        }

        JScrollPane scrollPane = new JScrollPane(envPanel);
        scrollPane.setBorder(null);
        add(scrollPane, BorderLayout.CENTER);

        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));

        JButton renameCurrentOnlyBtn = new JButton("Rename Current Environment Only");
        renameCurrentOnlyBtn.addActionListener(
            new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    confirmed = true;
                    selectedEnvironments = new ArrayList<>();
                    dispose();
                }
            }
        );

        JButton renameSelectedBtn = new JButton("Rename Selected Environments");
        renameSelectedBtn.addActionListener(
            new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    confirmed = true;
                    selectedEnvironments = getSelectedEnvironments();
                    dispose();
                }
            }
        );

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.addActionListener(
            new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    confirmed = false;
                    dispose();
                }
            }
        );

        buttonPanel.add(renameCurrentOnlyBtn);
        buttonPanel.add(renameSelectedBtn);
        buttonPanel.add(cancelBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        // Set default button
        getRootPane().setDefaultButton(renameSelectedBtn);
    }

    /**
     * Gets the list of selected environments.
     *
     * @return list of selected environment names
     */
    private List<String> getSelectedEnvironments() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < environmentCheckBoxes.size(); i++) {
            if (environmentCheckBoxes.get(i).isSelected()) {
                selected.add(affectedEnvironments.get(i));
            }
        }
        return selected;
    }

    /**
     * Checks if the user confirmed the rename operation.
     *
     * @return true if confirmed, false if cancelled
     */
    public boolean isConfirmed() {
        return confirmed;
    }

    /**
     * Gets the list of environments selected for renaming.
     * Returns null if user chose to rename current environment only.
     * Returns empty list if user cancelled.
     *
     * @return list of selected environment names, null for current only, empty for cancel
     */
    public List<String> getSelectedEnvironmentsForRename() {
        return selectedEnvironments;
    }

    /**
     * Shows the dialog and returns the result.
     *
     * @param parent parent frame
     * @param datasheetName name of the datasheet being renamed
     * @param currentEnv current environment
     * @param affectedEnvironments list of other environments with the same datasheet
     * @return dialog instance with user selections
     */
    public static CrossEnvironmentRenameDialog showDialog(
        Frame parent,
        String datasheetName,
        String currentEnv,
        List<String> affectedEnvironments
    ) {
        CrossEnvironmentRenameDialog dialog = new CrossEnvironmentRenameDialog(
            parent,
            datasheetName,
            currentEnv,
            affectedEnvironments
        );
        dialog.setVisible(true);
        return dialog;
    }
}
