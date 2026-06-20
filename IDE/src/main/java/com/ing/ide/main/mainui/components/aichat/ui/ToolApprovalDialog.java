package com.ing.ide.main.mainui.components.aichat.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Confirmation dialog shown before the agent runs a project-modifying tool. It
 * previews the tool name and its arguments and asks the user to approve or
 * decline. Read-only tools never reach this dialog.
 */
public final class ToolApprovalDialog {

    private ToolApprovalDialog() {}

    /**
     * Shows the approval dialog and returns {@code true} if the user approves.
     * Safe to call from any thread; the dialog is shown on the EDT.
     */
    public static boolean confirm(Component parent, String toolName, String argumentsJson) {
        if (SwingUtilities.isEventDispatchThread()) {
            return show(parent, toolName, argumentsJson);
        }
        AtomicBoolean approved = new AtomicBoolean(false);
        try {
            SwingUtilities.invokeAndWait(() -> approved.set(show(parent, toolName, argumentsJson)));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        } catch (InvocationTargetException ex) {
            return false;
        }
        return approved.get();
    }

    private static boolean show(Component parent, String toolName, String argumentsJson) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel header = new JLabel(
            "<html>The AI agent wants to run <b>" +
            toolName +
            "</b>.<br>Review the details and approve to apply the change.</html>"
        );
        panel.add(header, BorderLayout.NORTH);

        JTextArea details = new JTextArea(argumentsJson);
        details.setEditable(false);
        details.setLineWrap(true);
        details.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(details);
        scroll.setPreferredSize(new Dimension(420, 200));
        panel.add(scroll, BorderLayout.CENTER);

        int choice = JOptionPane.showOptionDialog(
            parent,
            panel,
            "Approve agent action",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            new Object[] { "Approve", "Decline" },
            "Decline"
        );
        return choice == JOptionPane.YES_OPTION;
    }
}
