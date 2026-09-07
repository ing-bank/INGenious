package com.ing.ide.main.mainui.components.aichat.ui;

import com.ing.ide.main.mainui.components.aichat.auth.AICredentials;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * Modal dialog for configuring the AI assistant: the GitHub OAuth App client id
 * used for the device-flow sign-in, plus a sign-out action. The selected model
 * is chosen from the panel's top bar and is shown here read-only.
 */
public final class AISettingsDialog {

    private AISettingsDialog() {}

    /** Callback invoked when the user requests sign-out from the dialog. */
    public interface SignOutHandler {
        void signOut();
    }

    public static void show(Component parent, AICredentials credentials, SignOutHandler signOut) {
        Window owner = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(
            owner,
            "INGenie Settings",
            JDialog.ModalityType.APPLICATION_MODAL
        );

        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;

        c.gridx = 0;
        c.gridy = 0;
        form.add(new JLabel("GitHub OAuth Client ID:"), c);

        final JTextField clientIdField = new JTextField(credentials.getClientId(), 28);
        c.gridx = 1;
        c.weightx = 1;
        form.add(clientIdField, c);

        c.gridx = 0;
        c.gridy = 1;
        c.weightx = 0;
        form.add(new JLabel("Selected model:"), c);

        String model = credentials.getSelectedModel();
        c.gridx = 1;
        form.add(new JLabel(model == null || model.isEmpty() ? "(none)" : model), c);

        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 2;
        String status = credentials.isSignedIn()
            ? "Signed in as " + (credentials.getLogin() == null ? "" : credentials.getLogin())
            : "Not signed in";
        JLabel statusLabel = new JLabel(status);
        form.add(statusLabel, c);

        JButton signOutButton = new JButton("Sign out");
        signOutButton.setEnabled(credentials.isSignedIn());
        signOutButton.addActionListener(
            e -> {
                signOut.signOut();
                statusLabel.setText("Not signed in");
                signOutButton.setEnabled(false);
            }
        );

        JButton saveButton = new JButton("Save");
        saveButton.addActionListener(
            e -> {
                credentials.setClientId(clientIdField.getText().trim());
                dialog.dispose();
            }
        );

        JButton cancelButton = new JButton("Close");
        cancelButton.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel();
        buttons.add(signOutButton);
        buttons.add(saveButton);
        buttons.add(cancelButton);

        JPanel content = new JPanel(new BorderLayout());
        content.add(form, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);

        JLabel hint = new JLabel(
            "<html><body style='width:340px'><small>Register a GitHub OAuth App with the " +
            "device-flow option enabled, then paste its Client ID above to enable sign-in." +
            "</small></body></html>"
        );
        hint.setBorder(BorderFactory.createEmptyBorder(8, 12, 0, 12));
        content.add(hint, BorderLayout.NORTH);

        dialog.setContentPane(content);
        dialog.pack();
        dialog.setLocationRelativeTo(owner);
        dialog.setVisible(true);
    }
}
