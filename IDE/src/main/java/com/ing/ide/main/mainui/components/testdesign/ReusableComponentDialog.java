package com.ing.ide.main.mainui.components.testdesign;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Modal dialog used while creating a Reusable from selected steps.
 * <p>
 * The dialog lets the user pick an existing Reusable Components scenario from a
 * drop-down or type a brand new scenario name (useful when no reusable
 * scenarios exist yet), and then enter the name for the new reusable.
 */
public final class ReusableComponentDialog {

    private ReusableComponentDialog() {}

    /** Holds the user's selection from {@link #prompt}. */
    public static final class Result {
        private final String scenarioName;
        private final String reusableName;

        public Result(String scenarioName, String reusableName) {
            this.scenarioName = scenarioName;
            this.reusableName = reusableName;
        }

        public String getScenarioName() {
            return scenarioName;
        }

        public String getReusableName() {
            return reusableName;
        }
    }

    /**
     * Shows the Create Reusable dialog and returns the user's input.
     *
     * @param parent  component used to anchor the modal dialog
     * @param project current project (used to list existing reusable scenarios)
     * @return the chosen scenario / reusable names, or {@code null} if the user
     *         cancelled or left a required field empty
     */
    public static Result prompt(Component parent, Project project) {
        JComboBox<String> scenarioBox = new JComboBox<>();
        scenarioBox.setEditable(true);
        if (project != null) {
            for (Scenario scenario : project.getReusableScenarios()) {
                scenarioBox.addItem(scenario.getName());
            }
        }
        if (scenarioBox.getItemCount() > 0) {
            scenarioBox.setSelectedIndex(0);
        } else {
            scenarioBox.setSelectedItem("");
        }

        JTextField nameField = new JTextField(20);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        panel.add(new JLabel("Reusable Scenario Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(scenarioBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        panel.add(new JLabel("Reusable Name:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(nameField, gbc);

        JOptionPane optionPane = new JOptionPane(
            panel,
            JOptionPane.PLAIN_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION
        );
        JDialog dialog = optionPane.createDialog(parent, "Create Reusable");

        // Hitting Enter inside the name field confirms the dialog.
        nameField.addActionListener(
            e -> {
                optionPane.setValue(JOptionPane.OK_OPTION);
                dialog.dispose();
            }
        );

        SwingUtilities.invokeLater(
            () -> {
                if (scenarioBox.getItemCount() == 0) {
                    scenarioBox.requestFocusInWindow();
                } else {
                    nameField.requestFocusInWindow();
                }
            }
        );

        dialog.setVisible(true);
        dialog.dispose();

        Object value = optionPane.getValue();
        int option = (value instanceof Integer) ? (Integer) value : JOptionPane.CLOSED_OPTION;
        if (option != JOptionPane.OK_OPTION) {
            return null;
        }

        Object selectedScenario = scenarioBox.getSelectedItem();
        String scenarioName = selectedScenario == null ? "" : selectedScenario.toString().trim();
        String reusableName = nameField.getText().trim();
        if (scenarioName.isEmpty() || reusableName.isEmpty()) {
            return null;
        }
        return new Result(scenarioName, reusableName);
    }
}
