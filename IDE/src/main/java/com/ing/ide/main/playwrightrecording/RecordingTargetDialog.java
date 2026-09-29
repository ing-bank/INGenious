package com.ing.ide.main.playwrightrecording;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

public class RecordingTargetDialog extends JDialog {

    public enum Mode {
        NEW_TEST_SCENARIO,
        NEW_REUSABLE_SCENARIO
    }

    public static class Selection {
        private final Mode mode;
        private final String scenarioName;
        private final String testCaseName;
        private final String existingScenarioName;
        private final boolean existingReusable;

        public Selection(
            Mode mode,
            String scenarioName,
            String testCaseName,
            String existingScenarioName,
            boolean existingReusable
        ) {
            this.mode = mode;
            this.scenarioName = scenarioName;
            this.testCaseName = testCaseName;
            this.existingScenarioName = existingScenarioName;
            this.existingReusable = existingReusable;
        }

        public Mode getMode() {
            return mode;
        }

        public String getScenarioName() {
            return scenarioName;
        }

        public String getTestCaseName() {
            return testCaseName;
        }

        public String getExistingScenarioName() {
            return existingScenarioName;
        }

        public boolean isExistingReusable() {
            return existingReusable;
        }
    }

    private final Project project;
    private final TestCase currentTestCase;

    private Selection selection;

    private JRadioButton newScenarioRadio;
    private JRadioButton newReusableRadio;

    private javax.swing.JTextField newScenarioNameField;
    private javax.swing.JTextField newScenarioTestCaseField;
    private javax.swing.JTextField newReusableNameField;
    private javax.swing.JTextField newReusableTestCaseField;

    private RecordingTargetDialog(Frame owner, Project project, TestCase currentTestCase) {
        super(owner, "Choose Recording Target", true);
        this.project = project;
        this.currentTestCase = currentTestCase;
        init();
    }

    public static Selection showDialog(
        Component parent,
        Project project,
        TestCase currentTestCase
    ) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        Frame frame = owner instanceof Frame ? (Frame) owner : null;
        RecordingTargetDialog dialog = new RecordingTargetDialog(frame, project, currentTestCase);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return dialog.selection;
    }

    private void init() {
        setLayout(new BorderLayout(8, 8));

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        ButtonGroup group = new ButtonGroup();
        newScenarioRadio = new JRadioButton("New test case under Test Scenario", true);
        newReusableRadio = new JRadioButton("New test case under Reusable Scenario");

        group.add(newScenarioRadio);
        group.add(newReusableRadio);

        content.add(newScenarioRadio, gbc);
        gbc.gridy++;
        content.add(
            formPanel(
                "Scenario",
                newScenarioNameField = new javax.swing.JTextField("LiveRecordingScenario"),
                "Test case",
                newScenarioTestCaseField = new javax.swing.JTextField("LiveRecordingTestCase")
            ),
            gbc
        );

        gbc.gridy++;
        content.add(newReusableRadio, gbc);
        gbc.gridy++;
        content.add(
            formPanel(
                "Reusable scenario",
                newReusableNameField = new javax.swing.JTextField("LiveRecordingReusable"),
                "Test case",
                newReusableTestCaseField =
                    new javax.swing.JTextField("LiveRecordingReusableTestCase")
            ),
            gbc
        );

        add(content, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton cancel = new JButton("Cancel");
        JButton ok = new JButton("Start Recording");
        cancel.addActionListener(e -> dispose());
        ok.addActionListener(this::onOk);
        actions.add(cancel);
        actions.add(ok);
        add(actions, BorderLayout.SOUTH);

        // Make "Start Recording" the default button so pressing Enter starts the recording.
        getRootPane().setDefaultButton(ok);

        java.awt.event.ActionListener listener = e -> updateEnabledState();
        newScenarioRadio.addActionListener(listener);
        newReusableRadio.addActionListener(listener);

        updateEnabledState();
        pack();
    }

    private JPanel formPanel(
        String label1,
        java.awt.Component field1,
        String label2,
        java.awt.Component field2
    ) {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel(label1 + ":"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field1, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel(label2 + ":"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field2, gbc);

        panel.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 0));
        return panel;
    }

    private void updateEnabledState() {
        boolean newScenario = newScenarioRadio.isSelected();
        boolean newReusable = newReusableRadio.isSelected();

        newScenarioNameField.setEnabled(newScenario);
        newScenarioTestCaseField.setEnabled(newScenario);

        newReusableNameField.setEnabled(newReusable);
        newReusableTestCaseField.setEnabled(newReusable);
    }

    private void onOk(ActionEvent e) {
        if (newScenarioRadio.isSelected()) {
            String scenarioName = safeTrim(newScenarioNameField.getText());
            String testCaseName = safeTrim(newScenarioTestCaseField.getText());
            if (scenarioName.isEmpty() || testCaseName.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Scenario and test case names are required.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            selection =
                new Selection(Mode.NEW_TEST_SCENARIO, scenarioName, testCaseName, null, false);
        } else if (newReusableRadio.isSelected()) {
            String scenarioName = safeTrim(newReusableNameField.getText());
            String testCaseName = safeTrim(newReusableTestCaseField.getText());
            if (scenarioName.isEmpty() || testCaseName.isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Reusable scenario and test case names are required.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            selection =
                new Selection(Mode.NEW_REUSABLE_SCENARIO, scenarioName, testCaseName, null, true);
        }

        dispose();
    }

    private String safeTrim(String text) {
        return text == null ? "" : text.trim();
    }
}
