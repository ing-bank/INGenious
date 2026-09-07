package com.ing.ide.main.mainui.components.aichat.ui;

import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A thin breadcrumb strip at the top of the AI sidebar showing the live IDE
 * selection: {@code Project › Scenario › TestCase}. Purely informational so the
 * user (and, via the system prompt, the model) always knows the working context.
 */
public class ContextBar extends JPanel {
    private final JLabel label = new JLabel(" ");
    private String project;
    private String scenario;
    private String testCase;

    public ContextBar() {
        super(new FlowLayout(FlowLayout.LEFT, 4, 2));
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
        label.setEnabled(false);
        add(label);
        render();
    }

    /**
     * Updates the breadcrumb. Returns {@code true} if anything changed (so the
     * caller can avoid redundant repaints).
     */
    public boolean setContext(String project, String scenario, String testCase) {
        if (
            Objects.equals(this.project, project) &&
            Objects.equals(this.scenario, scenario) &&
            Objects.equals(this.testCase, testCase)
        ) {
            return false;
        }
        this.project = project;
        this.scenario = scenario;
        this.testCase = testCase;
        render();
        return true;
    }

    private void render() {
        StringBuilder sb = new StringBuilder();
        sb.append(project == null || project.isEmpty() ? "No project" : project);
        if (scenario != null && !scenario.isEmpty()) {
            sb.append("  \u203A  ").append(scenario);
        }
        if (testCase != null && !testCase.isEmpty()) {
            sb.append("  \u203A  ").append(testCase);
        }
        label.setText(sb.toString());
    }
}
