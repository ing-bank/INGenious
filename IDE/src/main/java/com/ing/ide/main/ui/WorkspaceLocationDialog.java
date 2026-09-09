package com.ing.ide.main.ui;

import com.ing.datalib.util.WorkspacePath;
import com.ing.datalib.util.WorkspacePreference;
import com.ing.datalib.util.WorkspaceRelocator;
import com.ing.ide.main.mainui.AppMainFrame;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public final class WorkspaceLocationDialog extends JDialog {
    private final AppMainFrame mainFrame;
    private final JTextField baseField = new JTextField();
    private final JTextField workspaceField = new JTextField();

    public WorkspaceLocationDialog(AppMainFrame mainFrame) {
        super(mainFrame, "Workspace Location", true);
        this.mainFrame = mainFrame;

        initializeComponents();
        loadCurrentBase();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(640, 230));
        pack();
        setLocationRelativeTo(mainFrame);
    }

    public static void open(AppMainFrame mainFrame) {
        if (!WorkspacePath.isWorkspaceCustomizationAvailable()) {
            JOptionPane.showMessageDialog(
                mainFrame,
                "Workspace location cannot be changed for this INGenious installation.",
                "Workspace Location",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        new WorkspaceLocationDialog(mainFrame).setVisible(true);
    }

    private void initializeComponents() {
        JPanel content = new JPanel(new BorderLayout(12, 12));
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel description = new JLabel(
            "<html>Select the base folder where INGenious should store its Workspace." +
            "<br>The current Workspace will be moved to the selected location.</html>"
        );

        JPanel fields = new JPanel(new GridLayout(2, 1, 8, 8));
        fields.add(createBasePanel());
        fields.add(createWorkspacePanel());

        JButton apply = new JButton("Apply");
        apply.addActionListener(event -> saveSelection());

        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(event -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(cancel);
        buttons.add(apply);

        content.add(description, BorderLayout.NORTH);
        content.add(fields, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        setContentPane(content);
    }

    private JPanel createBasePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));

        JLabel label = new JLabel("Base location:", SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(125, 28));

        baseField.setEditable(false);

        JButton browse = new JButton("Browse...");
        browse.addActionListener(event -> chooseBase());

        panel.add(label, BorderLayout.WEST);
        panel.add(baseField, BorderLayout.CENTER);
        panel.add(browse, BorderLayout.EAST);

        return panel;
    }

    private JPanel createWorkspacePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));

        JLabel label = new JLabel("Workspace:", SwingConstants.RIGHT);
        label.setPreferredSize(new Dimension(125, 28));

        workspaceField.setEditable(false);

        panel.add(label, BorderLayout.WEST);
        panel.add(workspaceField, BorderLayout.CENTER);

        return panel;
    }

    private void loadCurrentBase() {
        baseField.setText(WorkspacePath.getInstalledWorkspaceBase());
        updateWorkspacePreview();
    }

    private void chooseBase() {
        JFileChooser chooser = new JFileChooser(baseField.getText());
        chooser.setDialogTitle("Select INGenious Workspace Base Location");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
        chooser.setAcceptAllFileFilterUsed(false);

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            Path selectedBase = chooser.getSelectedFile().toPath().toAbsolutePath().normalize();

            baseField.setText(selectedBase.toString());
            updateWorkspacePreview();
        }
    }

    private void updateWorkspacePreview() {
        String base = baseField.getText();

        workspaceField.setText(
            base == null || base.isBlank() ? "" : WorkspacePath.deriveInstalledWorkspace(base)
        );
    }

    private void saveSelection() {
        String baseValue = baseField.getText();

        if (baseValue == null || baseValue.isBlank()) {
            showError("Select a base location.");
            return;
        }

        Path base = Path.of(baseValue).toAbsolutePath().normalize();

        if (!Files.isDirectory(base)) {
            showError("The selected base location is not a directory:\n" + base);
            return;
        }

        if (!Files.isWritable(base)) {
            showError("The selected base location is not writable:\n" + base);
            return;
        }

        Path currentWorkspace = Path
            .of(WorkspacePath.getWorkspaceRoot())
            .toAbsolutePath()
            .normalize();

        Path workspace = Path
            .of(WorkspacePath.deriveInstalledWorkspace(base.toString()))
            .toAbsolutePath()
            .normalize();

        if (currentWorkspace.equals(workspace)) {
            JOptionPane.showMessageDialog(
                this,
                "The selected location is already the active Workspace.",
                "Workspace Location",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }

        if (Files.exists(workspace)) {
            showError(
                "A Workspace already exists at the selected location:\n" +
                workspace +
                "\n\nChoose another base location."
            );
            return;
        }

        try {
            WorkspaceRelocator.validateRelocationPaths(currentWorkspace, workspace);
        } catch (IOException ex) {
            showError("The selected location cannot be used.\n\n" + ex.getMessage());
            return;
        }

        Object[] options = { "Move Workspace", "Cancel" };

        int confirmation = JOptionPane.showOptionDialog(
            this,
            "Move the current Workspace?\n\n" +
            "From:\n" +
            currentWorkspace +
            "\n\nTo:\n" +
            workspace +
            "\n\nThe previous Workspace will be deleted only after " +
            "the copied content has been verified successfully.\n\n" +
            "INGenious will restart automatically after the move.",
            "Confirm Workspace Move",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            options,
            options[1]
        );

        if (confirmation != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            mainFrame.saveLoadedProject();
        } catch (RuntimeException ex) {
            showError(
                "Could not save the current project.\n\n" +
                "The Workspace move was cancelled.\n\n" +
                ex.getMessage()
            );
            return;
        }

        boolean destinationCreated = false;

        try {
            WorkspaceRelocator.copyWorkspace(currentWorkspace, workspace);
            destinationCreated = true;

            WorkspaceRelocator.verifyWorkspaceCopy(currentWorkspace, workspace);

            WorkspacePreference.saveWorkspaceBase(System.getProperty("user.home"), base);
        } catch (IOException | RuntimeException ex) {
            String cleanupMessage = "";

            if (destinationCreated) {
                try {
                    WorkspaceRelocator.removeFailedDestination(workspace);
                } catch (IOException cleanupError) {
                    cleanupMessage =
                        "\n\nThe incomplete destination could not be removed:\n" +
                        workspace +
                        "\n" +
                        cleanupError.getMessage();
                }
            }

            showError(
                "Could not prepare the new Workspace:\n" +
                workspace +
                "\n\n" +
                ex.getMessage() +
                "\n\nThe current Workspace remains unchanged." +
                cleanupMessage
            );
            return;
        }

        try {
            WorkspaceRelocator.deleteVerifiedSource(currentWorkspace, workspace);
        } catch (IOException | RuntimeException ex) {
            JOptionPane.showMessageDialog(
                this,
                "The new Workspace was created and selected successfully, " +
                "but the previous Workspace could not be deleted.\n\n" +
                "New Workspace:\n" +
                workspace +
                "\n\nPrevious Workspace:\n" +
                currentWorkspace +
                "\n\n" +
                ex.getMessage() +
                "\n\nINGenious will restart using the new Workspace.",
                "Workspace Move Completed with Warning",
                JOptionPane.WARNING_MESSAGE
            );

            dispose();
            mainFrame.restartAfterWorkspaceMove();
            return;
        }

        JOptionPane.showMessageDialog(
            this,
            "The Workspace was moved successfully.\n\n" +
            "New Workspace:\n" +
            workspace +
            "\n\nINGenious will now restart.",
            "Workspace Location",
            JOptionPane.INFORMATION_MESSAGE
        );

        dispose();
        mainFrame.restartAfterWorkspaceMove();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
            this,
            message,
            "Workspace Location",
            JOptionPane.ERROR_MESSAGE
        );
    }
}
