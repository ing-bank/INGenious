package com.ing.ide.main.playwrightrecording;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
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
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;

public class RecordingTargetDialog extends JDialog {

    public enum Mode {
        NEW_TEST_SCENARIO,
        NEW_REUSABLE_SCENARIO,
        CURRENT_OPEN_TEST_CASE,
        EXISTING_TEST_CASE
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
    private JRadioButton currentOpenRadio;
    private JRadioButton existingRadio;

    private javax.swing.JTextField newScenarioNameField;
    private javax.swing.JTextField newScenarioTestCaseField;
    private javax.swing.JTextField newReusableNameField;
    private javax.swing.JTextField newReusableTestCaseField;

    private JComboBox<ScenarioWrapper> existingScenarioCombo;
    private JComboBox<String> existingTestCaseCombo;

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
        currentOpenRadio = new JRadioButton("Use currently open test case");
        existingRadio = new JRadioButton("Use an existing test case");

        group.add(newScenarioRadio);
        group.add(newReusableRadio);
        group.add(currentOpenRadio);
        group.add(existingRadio);

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
                newReusableTestCaseField = new javax.swing.JTextField("LiveRecordingTestCase")
            ),
            gbc
        );

        gbc.gridy++;
        content.add(currentOpenRadio, gbc);

        gbc.gridy++;
        content.add(existingRadio, gbc);
        gbc.gridy++;
        existingScenarioCombo = new JComboBox<>();
        existingTestCaseCombo = new JComboBox<>();
        populateScenarioCombo();
        JPanel existingPanel = formPanel(
            "Scenario",
            existingScenarioCombo,
            "Test case",
            existingTestCaseCombo
        );
        content.add(existingPanel, gbc);

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
        currentOpenRadio.addActionListener(listener);
        existingRadio.addActionListener(listener);
        existingScenarioCombo.addActionListener(e -> populateTestCaseCombo());

        if (currentTestCase == null) {
            currentOpenRadio.setEnabled(false);
        }

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

    private void populateScenarioCombo() {
        existingScenarioCombo.removeAllItems();
        List<ScenarioWrapper> wrappers = new ArrayList<>();
        for (Scenario scenario : project.getScenarios()) {
            wrappers.add(new ScenarioWrapper(scenario, false));
        }
        for (Scenario scenario : project.getReusableScenarios()) {
            wrappers.add(new ScenarioWrapper(scenario, true));
        }
        for (ScenarioWrapper wrapper : wrappers) {
            existingScenarioCombo.addItem(wrapper);
        }
        populateTestCaseCombo();
    }

    private void populateTestCaseCombo() {
        existingTestCaseCombo.removeAllItems();
        ScenarioWrapper wrapper = (ScenarioWrapper) existingScenarioCombo.getSelectedItem();
        if (wrapper == null) {
            return;
        }
        for (TestCase testCase : wrapper.scenario.getTestCases()) {
            existingTestCaseCombo.addItem(testCase.getName());
        }
    }

    private void updateEnabledState() {
        boolean newScenario = newScenarioRadio.isSelected();
        boolean newReusable = newReusableRadio.isSelected();
        boolean existing = existingRadio.isSelected();

        newScenarioNameField.setEnabled(newScenario);
        newScenarioTestCaseField.setEnabled(newScenario);

        newReusableNameField.setEnabled(newReusable);
        newReusableTestCaseField.setEnabled(newReusable);

        existingScenarioCombo.setEnabled(existing);
        existingTestCaseCombo.setEnabled(existing);
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
        } else if (currentOpenRadio.isSelected()) {
            if (currentTestCase == null) {
                JOptionPane.showMessageDialog(
                    this,
                    "No test case is currently open.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            selection =
                new Selection(
                    Mode.CURRENT_OPEN_TEST_CASE,
                    currentTestCase.getScenario().getName(),
                    currentTestCase.getName(),
                    currentTestCase.getScenario().getName(),
                    currentTestCase.getScenario().isReusableScenario()
                );
        } else {
            ScenarioWrapper wrapper = (ScenarioWrapper) existingScenarioCombo.getSelectedItem();
            String selectedTestCase = (String) existingTestCaseCombo.getSelectedItem();
            if (wrapper == null || selectedTestCase == null || selectedTestCase.trim().isEmpty()) {
                JOptionPane.showMessageDialog(
                    this,
                    "Please choose an existing scenario and test case.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE
                );
                return;
            }
            selection =
                new Selection(
                    Mode.EXISTING_TEST_CASE,
                    wrapper.scenario.getName(),
                    selectedTestCase,
                    wrapper.scenario.getName(),
                    wrapper.reusable
                );
        }

        dispose();
    }

    private String safeTrim(String text) {
        return text == null ? "" : text.trim();
    }

    private static class ScenarioWrapper {
        private final Scenario scenario;
        private final boolean reusable;

        private ScenarioWrapper(Scenario scenario, boolean reusable) {
            this.scenario = scenario;
            this.reusable = reusable;
        }

        @Override
        public String toString() {
            return reusable
                ? "[Reusable] " + scenario.getName()
                : "[Scenario] " + scenario.getName();
        }
    }
}
