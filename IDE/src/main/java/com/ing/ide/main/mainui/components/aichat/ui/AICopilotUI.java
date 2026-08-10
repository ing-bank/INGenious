package com.ing.ide.main.mainui.components.aichat.ui;

import com.ing.ide.main.mainui.components.aichat.AICopilot;
import com.ing.ide.main.mainui.components.aichat.model.ModelInfo;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

/**
 * Swing panel hosting the AI assistant: a top bar with model selector and
 * sign-in control, a JavaFX WebView transcript in the centre, a multiline input
 * with send/stop at the bottom, and a footer showing token usage.
 */
public class AICopilotUI extends JPanel {
    private final AICopilot controller;
    private final ChatWebView chatWebView = new ChatWebView();

    private final JComboBox<ModelInfo> modelSelector = new JComboBox<>();
    private final JCheckBox agentModeToggle = new JCheckBox("Agent");
    private final JButton signInButton = new JButton("Sign in");
    private final JButton settingsButton = new JButton("Settings");
    private final JButton sendButton = new JButton("Send");
    private final JButton stopButton = new JButton("Stop");
    private final JButton clearButton = new JButton("Clear");
    private final JTextArea inputArea = new JTextArea(3, 40);
    private final JLabel footerLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel("Not signed in");

    public AICopilotUI(AICopilot controller) {
        this.controller = controller;
        setLayout(new BorderLayout());
        add(buildTopBar(), BorderLayout.NORTH);
        add(chatWebView.getComponent(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);
        wireActions();
        stopButton.setEnabled(false);
    }

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.add(new JLabel("Model:"));
        modelSelector.setPreferredSize(new Dimension(260, 26));
        left.add(modelSelector);
        agentModeToggle.setToolTipText(
            "Agent mode: let the AI create/edit scenarios, test cases, steps and OR entries (with approval)."
        );
        left.add(agentModeToggle);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.add(statusLabel);
        right.add(signInButton);
        right.add(settingsButton);

        bar.add(left, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildBottom() {
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setBorder(BorderFactory.createEmptyBorder(4, 8, 8, 8));

        inputArea.setLineWrap(true);
        inputArea.setWrapStyleWord(true);
        JScrollPane inputScroll = new JScrollPane(inputArea);
        inputScroll.setPreferredSize(new Dimension(100, 72));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        buttons.add(clearButton);
        buttons.add(stopButton);
        buttons.add(sendButton);

        JPanel inputRow = new JPanel(new BorderLayout());
        inputRow.add(inputScroll, BorderLayout.CENTER);
        inputRow.add(buttons, BorderLayout.SOUTH);

        footerLabel.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 2));
        footerLabel.setEnabled(false);

        bottom.add(inputRow, BorderLayout.CENTER);
        bottom.add(footerLabel, BorderLayout.SOUTH);
        return bottom;
    }

    private void wireActions() {
        sendButton.addActionListener(e -> doSend());
        stopButton.addActionListener(e -> controller.cancelGeneration());
        clearButton.addActionListener(e -> controller.clearConversation());
        signInButton.addActionListener(e -> controller.toggleSignIn());
        settingsButton.addActionListener(e -> controller.openSettings());
        modelSelector.addActionListener(
            e -> {
                Object sel = modelSelector.getSelectedItem();
                if (sel instanceof ModelInfo) {
                    controller.onModelSelected(((ModelInfo) sel).getId());
                }
            }
        );

        // Ctrl+Enter sends; plain Enter inserts a newline.
        inputArea
            .getInputMap()
            .put(
                KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, java.awt.event.InputEvent.CTRL_DOWN_MASK),
                "send"
            );
        inputArea
            .getActionMap()
            .put(
                "send",
                new javax.swing.AbstractAction() {

                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        doSend();
                    }
                }
            );
    }

    private void doSend() {
        String text = inputArea.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        inputArea.setText("");
        controller.sendUserMessage(text);
    }

    public ChatWebView getChatWebView() {
        return chatWebView;
    }

    /** Whether the agent (tool-calling) mode is enabled. */
    public boolean isAgentMode() {
        return agentModeToggle.isSelected();
    }

    public void setModels(List<ModelInfo> models, String selectedId) {
        SwingUtilities.invokeLater(
            () -> {
                DefaultComboBoxModel<ModelInfo> model = new DefaultComboBoxModel<>();
                ModelInfo toSelect = null;
                for (ModelInfo m : models) {
                    model.addElement(m);
                    if (m.getId() != null && m.getId().equals(selectedId)) {
                        toSelect = m;
                    }
                }
                modelSelector.setModel(model);
                if (toSelect != null) {
                    modelSelector.setSelectedItem(toSelect);
                }
            }
        );
    }

    public void setSignedIn(boolean signedIn, String login) {
        SwingUtilities.invokeLater(
            () -> {
                signInButton.setText(signedIn ? "Sign out" : "Sign in");
                statusLabel.setText(
                    signedIn
                        ? (login != null && !login.isEmpty() ? "Signed in: " + login : "Signed in")
                        : "Not signed in"
                );
            }
        );
    }

    public void setGenerating(boolean generating) {
        SwingUtilities.invokeLater(
            () -> {
                sendButton.setEnabled(!generating);
                stopButton.setEnabled(generating);
                inputArea.setEnabled(!generating);
            }
        );
    }

    public void setFooter(String text) {
        SwingUtilities.invokeLater(() -> footerLabel.setText(text == null ? " " : text));
    }

    public void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }
}
