package com.ing.ide.main.mainui.components.aichat.ui;

import com.ing.ide.main.mainui.components.aichat.AICopilot;
import com.ing.ide.main.mainui.components.aichat.history.ChatHistoryStore;
import com.ing.ide.main.mainui.components.aichat.model.ModelInfo;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
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
    private final JToggleButton promptsToggle = new JToggleButton("Prompts");
    private final JButton historyButton = new JButton("History");
    private final JButton connectButton = new JButton("Connect to VS Code");
    private final JLabel connectionBulb = new JLabel(YELLOW_DOT);
    private final JButton sendButton = new JButton("Send");
    private final JButton stopButton = new JButton("Stop");
    private final JButton clearButton = new JButton("Clear");
    private final JTextArea inputArea = new JTextArea(3, 40);
    private final JLabel footerLabel = new JLabel(" ");
    private final JLabel statusLabel = new JLabel("Not connected");
    private javax.swing.Timer statusRevertTimer;
    private String persistentStatus = "Not connected";
    private final PromptLibraryPanel promptLibrary;
    private final JScrollPane promptScroll;
    private final ContextBar contextBar = new ContextBar();

    /** Non-modal approval banner shown above the input during agent turns. */
    private final JPanel approvalBanner = new JPanel(new BorderLayout(8, 0));
    private final JLabel approvalLabel = new JLabel(" ");
    private final JButton approvalApply = new JButton("Apply");
    private final JButton approvalSkip = new JButton("Skip");
    private Consumer<Boolean> approvalCallback;

    public AICopilotUI(AICopilot controller) {
        this.controller = controller;
        this.promptLibrary =
            new PromptLibraryPanel(this::fillInput, controller::resolveContextToken);
        this.promptScroll = new JScrollPane(promptLibrary);
        this.promptScroll.setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, java.awt.Color.GRAY)
            );
        this.promptScroll.setPreferredSize(new Dimension(320, 190));
        this.promptScroll.setVisible(false);
        setLayout(new BorderLayout());
        JPanel north = new JPanel(new BorderLayout());
        north.add(buildTopBar(), BorderLayout.NORTH);
        north.add(contextBar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(buildCenter(), BorderLayout.CENTER);
        add(buildBottom(), BorderLayout.SOUTH);
        wireActions();
        stopButton.setEnabled(false);
        // Default to Agent (tool-calling) mode so the assistant can actually act.
        agentModeToggle.setSelected(true);
        startContextTimer();
    }

    /** Polls the controller for the live selection and updates the breadcrumb. */
    private void startContextTimer() {
        javax.swing.Timer timer = new javax.swing.Timer(
            1500,
            e -> {
                String project = controller.resolveContextToken("currentProject");
                String scenario = controller.resolveContextToken("currentScenario");
                String testCase = controller.resolveContextToken("currentTestCase");
                if (contextBar.setContext(project, scenario, testCase)) {
                    contextBar.revalidate();
                    contextBar.repaint();
                }
            }
        );
        timer.setRepeats(true);
        timer.start();
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout());
        center.add(promptScroll, BorderLayout.NORTH);
        center.add(chatWebView.getComponent(), BorderLayout.CENTER);
        return center;
    }

    /** Fills the input box with a chosen prompt and focuses it for editing. */
    private void fillInput(String text) {
        SwingUtilities.invokeLater(
            () -> {
                inputArea.setText(text);
                inputArea.requestFocusInWindow();
                inputArea.setCaretPosition(inputArea.getDocument().getLength());
            }
        );
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
        promptsToggle.setToolTipText("Show the prompt library — click a chip to fill the input.");
        left.add(promptsToggle);
        historyButton.setToolTipText("Browse and reload past conversations, or start a new chat.");
        left.add(historyButton);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        right.add(statusLabel);
        right.add(connectionBulb);
        right.add(connectButton);

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

        buildApprovalBanner();

        JPanel stack = new JPanel(new BorderLayout());
        stack.add(approvalBanner, BorderLayout.NORTH);
        stack.add(inputRow, BorderLayout.CENTER);

        bottom.add(stack, BorderLayout.CENTER);
        bottom.add(footerLabel, BorderLayout.SOUTH);
        return bottom;
    }

    private void buildApprovalBanner() {
        approvalBanner.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 3, 0, 0, new Color(0xD7, 0xA0, 0x17)),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)
            )
        );
        approvalBanner.setBackground(new Color(0x3A, 0x2F, 0x16));
        approvalLabel.setForeground(new Color(0xF0, 0xE0, 0xB0));
        JPanel apprButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        apprButtons.setOpaque(false);
        apprButtons.add(approvalApply);
        apprButtons.add(approvalSkip);
        approvalBanner.add(approvalLabel, BorderLayout.CENTER);
        approvalBanner.add(apprButtons, BorderLayout.EAST);
        approvalBanner.setVisible(false);
        approvalApply.addActionListener(e -> resolveApproval(true));
        approvalSkip.addActionListener(e -> resolveApproval(false));
    }

    private void resolveApproval(boolean approved) {
        Consumer<Boolean> cb = approvalCallback;
        approvalCallback = null;
        approvalBanner.setVisible(false);
        revalidate();
        repaint();
        if (cb != null) {
            cb.accept(approved);
        }
    }

    /**
     * Shows the non-modal approval banner for a mutating tool call. The callback
     * is invoked on the EDT with the user's decision. Safe to call from any
     * thread.
     */
    public void showApproval(String toolName, String summary, Consumer<Boolean> callback) {
        SwingUtilities.invokeLater(
            () -> {
                this.approvalCallback = callback;
                approvalLabel.setText(
                    "<html>\u26A0\uFE0F <b>" +
                    escape(toolName) +
                    "</b> — " +
                    escape(summary) +
                    "</html>"
                );
                approvalBanner.setVisible(true);
                revalidate();
                repaint();
            }
        );
    }

    /** Hides the approval banner and clears any pending callback. */
    public void hideApproval() {
        SwingUtilities.invokeLater(
            () -> {
                approvalCallback = null;
                approvalBanner.setVisible(false);
                revalidate();
                repaint();
            }
        );
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private void wireActions() {
        sendButton.addActionListener(e -> doSend());
        stopButton.addActionListener(e -> controller.cancelGeneration());
        clearButton.addActionListener(e -> controller.clearConversation());
        connectButton.addActionListener(e -> controller.connectToVsCode());
        historyButton.addActionListener(e -> showHistoryMenu());
        promptsToggle.addActionListener(
            e -> {
                promptScroll.setVisible(promptsToggle.isSelected());
                revalidate();
                repaint();
            }
        );
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

    /** Popup menu: start a new chat or reload a saved conversation. */
    private void showHistoryMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem neu = new JMenuItem("\u002B New chat");
        neu.addActionListener(a -> controller.newConversation());
        menu.add(neu);

        java.util.List<ChatHistoryStore.Entry> entries = controller.listHistory();
        if (entries.isEmpty()) {
            JMenuItem none = new JMenuItem("No saved chats yet");
            none.setEnabled(false);
            menu.add(none);
        } else {
            menu.addSeparator();
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("MMM d, HH:mm");
            int shown = 0;
            for (ChatHistoryStore.Entry e : entries) {
                if (shown++ >= 20) {
                    break;
                }
                String when = fmt.format(new java.util.Date(e.updatedAt));
                JMenuItem item = new JMenuItem(
                    "<html>" +
                    escape(e.title) +
                    "  <font color='#888888'>" +
                    when +
                    "</font></html>"
                );
                final String id = e.id;
                item.addActionListener(a -> controller.loadConversation(id));
                menu.add(item);
            }
        }
        menu.show(historyButton, 0, historyButton.getHeight());
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
        String status = signedIn
            ? (login != null && !login.isEmpty() ? login : "Connected")
            : "Not connected";
        setConnected(signedIn, status);
    }

    /**
     * Updates the connection indicator: a green bulb + status when connected to
     * the VS Code bridge, a yellow bulb + "Not connected" otherwise.
     */
    public void setConnected(boolean connected, String status) {
        SwingUtilities.invokeLater(
            () -> {
                connectionBulb.setIcon(connected ? GREEN_DOT : YELLOW_DOT);
                connectionBulb.setToolTipText(
                    connected
                        ? "Connected to VS Code"
                        : "Not connected \u2014 click Connect to VS Code"
                );
                connectButton.setText(connected ? "Reconnect" : "Connect to VS Code");
                persistentStatus =
                    status != null ? status : (connected ? "Connected" : "Not connected");
                if (statusRevertTimer == null || !statusRevertTimer.isRunning()) {
                    statusLabel.setForeground(null);
                    statusLabel.setText(persistentStatus);
                }
            }
        );
    }

    /**
     * Shows a highlighted success message (e.g. revealing the port) that reverts
     * to the persistent connection status after a few seconds.
     */
    public void showTemporaryMessage(String message) {
        SwingUtilities.invokeLater(
            () -> {
                statusLabel.setForeground(new Color(0x2E, 0x7D, 0x32));
                statusLabel.setText(message);
                if (statusRevertTimer != null) {
                    statusRevertTimer.stop();
                }
                statusRevertTimer =
                    new javax.swing.Timer(
                        5000,
                        e -> {
                            statusLabel.setForeground(null);
                            statusLabel.setText(persistentStatus);
                        }
                    );
                statusRevertTimer.setRepeats(false);
                statusRevertTimer.start();
            }
        );
    }

    public void setGenerating(boolean generating) {
        SwingUtilities.invokeLater(
            () -> {
                sendButton.setEnabled(!generating);
                stopButton.setEnabled(generating);
                inputArea.setEnabled(!generating);
                if (generating) {
                    chatWebView.showThinking();
                } else {
                    chatWebView.hideThinking();
                }
            }
        );
    }

    public void setFooter(String text) {
        SwingUtilities.invokeLater(() -> footerLabel.setText(text == null ? " " : text));
    }

    public void setStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    // ── Connection status bulb ────────────────────────────────────────────

    private static final Icon YELLOW_DOT = new DotIcon(new Color(0xE5, 0xB1, 0x00));
    private static final Icon GREEN_DOT = new DotIcon(new Color(0x34, 0x96, 0x51));

    /** A small filled circle used as a connection status indicator (bulb). */
    private static final class DotIcon implements Icon {
        private static final int SIZE = 12;
        private final Color color;

        DotIcon(Color color) {
            this.color = color;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.fillOval(x, y, SIZE, SIZE);
            g2.setColor(color.darker());
            g2.drawOval(x, y, SIZE - 1, SIZE - 1);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return SIZE;
        }

        @Override
        public int getIconHeight() {
            return SIZE;
        }
    }
}
