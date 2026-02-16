package com.ing.ide.main.mainui.components.apitester.response;

import com.ing.datalib.api.APIResponse;
import com.ing.ide.main.mainui.components.apitester.APITesterUI;
import com.ing.ide.main.mainui.components.apitester.util.APITesterColors;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * Panel for displaying API response.
 */
public class ResponsePanel extends JPanel {

    private final APITesterUI parent;
    
    // Status bar
    private JLabel statusLabel;
    private JLabel timeLabel;
    private JLabel sizeLabel;
    
    // Content tabs
    private JTabbedPane tabPane;
    private JTextArea bodyTextArea;
    private JTable headersTable;
    private DefaultTableModel headersModel;
    private JPanel testResultsPanel;
    private JLabel testResultsLabel;
    private JList<String> testResultsList;
    private DefaultListModel<String> testResultsModel;
    
    // Card layout for loading/response states
    private CardLayout cardLayout;
    private JPanel cardPanel;
    
    public ResponsePanel(APITesterUI parent) {
        this.parent = parent;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(0, 0, 0, 0));
        
        // Status bar at top
        JPanel statusBar = createStatusBar();
        add(statusBar, BorderLayout.NORTH);
        
        // Card layout for different states
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        
        // Empty state
        JLabel emptyLabel = new JLabel("Send a request to see the response", JLabel.CENTER);
        emptyLabel.setForeground(APITesterColors.textSecondary());
        emptyLabel.setFont(emptyLabel.getFont().deriveFont(14f));
        emptyLabel.setName("emptyLabel");
        cardPanel.add(emptyLabel, "EMPTY");
        
        // Loading state
        JPanel loadingPanel = new JPanel(new BorderLayout());
        loadingPanel.setName("loadingPanel");
        JLabel loadingLabel = new JLabel("Sending request...", JLabel.CENTER);
        loadingLabel.setForeground(APITesterColors.textSecondary());
        loadingLabel.setFont(loadingLabel.getFont().deriveFont(14f));
        loadingLabel.setName("loadingLabel");
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setPreferredSize(new Dimension(200, 4));
        JPanel progressWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));
        progressWrapper.add(progressBar);
        loadingPanel.add(loadingLabel, BorderLayout.CENTER);
        loadingPanel.add(progressWrapper, BorderLayout.SOUTH);
        cardPanel.add(loadingPanel, "LOADING");
        
        // Response content
        JPanel responseContent = createResponseContent();
        cardPanel.add(responseContent, "RESPONSE");
        
        // Error state
        JTextArea errorArea = new JTextArea();
        errorArea.setEditable(false);
        errorArea.setForeground(APITesterColors.statusError());
        errorArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        errorArea.setLineWrap(true);
        errorArea.setWrapStyleWord(true);
        errorArea.setBackground(APITesterColors.panelBackground());
        errorArea.setName("errorArea");
        JScrollPane errorScroll = new JScrollPane(errorArea);
        errorScroll.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        cardPanel.add(errorScroll, "ERROR");
        
        add(cardPanel, BorderLayout.CENTER);
        
        cardLayout.show(cardPanel, "EMPTY");
    }
    
    private JPanel createStatusBar() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));
        panel.setBackground(APITesterColors.panelBackground());
        panel.setName("statusBar");
        
        JLabel responseLabel = new JLabel("Response");
        responseLabel.setFont(responseLabel.getFont().deriveFont(Font.BOLD, 13f));
        
        statusLabel = new JLabel();
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 12f));
        statusLabel.setOpaque(true);
        statusLabel.setBorder(new EmptyBorder(2, 8, 2, 8));
        
        timeLabel = new JLabel();
        timeLabel.setFont(timeLabel.getFont().deriveFont(11f));
        timeLabel.setForeground(APITesterColors.textSecondary());
        
        sizeLabel = new JLabel();
        sizeLabel.setFont(sizeLabel.getFont().deriveFont(11f));
        sizeLabel.setForeground(APITesterColors.textSecondary());
        
        panel.add(responseLabel);
        panel.add(statusLabel);
        panel.add(timeLabel);
        panel.add(sizeLabel);
        
        return panel;
    }
    
    private JPanel createResponseContent() {
        JPanel panel = new JPanel(new BorderLayout());
        
        tabPane = new JTabbedPane(JTabbedPane.TOP);
        tabPane.setFont(tabPane.getFont().deriveFont(11f));
        
        // Body tab
        bodyTextArea = new JTextArea();
        bodyTextArea.setEditable(false);
        bodyTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        bodyTextArea.setTabSize(2);
        JScrollPane bodyScroll = new JScrollPane(bodyTextArea);
        bodyScroll.setBorder(BorderFactory.createEmptyBorder());
        
        // Body tab with format options
        JPanel bodyPanel = new JPanel(new BorderLayout());
        JPanel bodyToolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        bodyToolbar.setBorder(new EmptyBorder(5, 8, 5, 8));
        
        JButton prettyBtn = new JButton("Pretty");
        prettyBtn.setFont(prettyBtn.getFont().deriveFont(10f));
        prettyBtn.addActionListener(e -> formatBody(true));
        
        JButton rawBtn = new JButton("Raw");
        rawBtn.setFont(rawBtn.getFont().deriveFont(10f));
        rawBtn.addActionListener(e -> formatBody(false));
        
        JButton copyBtn = new JButton("Copy");
        copyBtn.setFont(copyBtn.getFont().deriveFont(10f));
        copyBtn.addActionListener(e -> {
            bodyTextArea.selectAll();
            bodyTextArea.copy();
            bodyTextArea.setCaretPosition(0);
        });
        
        bodyToolbar.add(prettyBtn);
        bodyToolbar.add(rawBtn);
        bodyToolbar.add(Box.createHorizontalStrut(20));
        bodyToolbar.add(copyBtn);
        
        bodyPanel.add(bodyToolbar, BorderLayout.NORTH);
        bodyPanel.add(bodyScroll, BorderLayout.CENTER);
        tabPane.addTab("Body", bodyPanel);
        
        // Headers tab
        headersModel = new DefaultTableModel(new String[]{"Header", "Value"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        headersTable = new JTable(headersModel);
        headersTable.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        headersTable.setRowHeight(24);
        headersTable.getColumnModel().getColumn(0).setPreferredWidth(200);
        headersTable.getColumnModel().getColumn(1).setPreferredWidth(400);
        JScrollPane headersScroll = new JScrollPane(headersTable);
        headersScroll.setBorder(BorderFactory.createEmptyBorder());
        tabPane.addTab("Headers", headersScroll);
        
        // Test Results tab
        testResultsPanel = new JPanel(new BorderLayout());
        testResultsLabel = new JLabel("", JLabel.LEFT);
        testResultsLabel.setFont(testResultsLabel.getFont().deriveFont(Font.BOLD, 12f));
        testResultsLabel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        testResultsModel = new DefaultListModel<>();
        testResultsList = new JList<>(testResultsModel);
        testResultsList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        testResultsList.setCellRenderer(new TestResultRenderer());
        JScrollPane testResultsScroll = new JScrollPane(testResultsList);
        testResultsScroll.setBorder(BorderFactory.createEmptyBorder());
        
        testResultsPanel.add(testResultsLabel, BorderLayout.NORTH);
        testResultsPanel.add(testResultsScroll, BorderLayout.CENTER);
        tabPane.addTab("Test Results", testResultsPanel);
        
        panel.add(tabPane, BorderLayout.CENTER);
        return panel;
    }
    
    private String rawBody;
    
    private void formatBody(boolean pretty) {
        if (rawBody == null) return;
        
        if (pretty) {
            bodyTextArea.setText(formatJson(rawBody));
        } else {
            bodyTextArea.setText(rawBody);
        }
        bodyTextArea.setCaretPosition(0);
    }
    
    private String formatJson(String json) {
        if (json == null || json.isEmpty()) return json;
        
        try {
            // Simple JSON formatting
            StringBuilder formatted = new StringBuilder();
            int indent = 0;
            boolean inString = false;
            
            for (int i = 0; i < json.length(); i++) {
                char c = json.charAt(i);
                
                if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                    inString = !inString;
                    formatted.append(c);
                } else if (!inString) {
                    switch (c) {
                        case '{':
                        case '[':
                            formatted.append(c);
                            formatted.append('\n');
                            indent++;
                            appendIndent(formatted, indent);
                            break;
                        case '}':
                        case ']':
                            formatted.append('\n');
                            indent--;
                            appendIndent(formatted, indent);
                            formatted.append(c);
                            break;
                        case ',':
                            formatted.append(c);
                            formatted.append('\n');
                            appendIndent(formatted, indent);
                            break;
                        case ':':
                            formatted.append(c);
                            formatted.append(' ');
                            break;
                        case ' ':
                        case '\n':
                        case '\r':
                        case '\t':
                            // Skip whitespace
                            break;
                        default:
                            formatted.append(c);
                    }
                } else {
                    formatted.append(c);
                }
            }
            
            return formatted.toString();
        } catch (Exception e) {
            return json;
        }
    }
    
    private void appendIndent(StringBuilder sb, int indent) {
        for (int i = 0; i < indent; i++) {
            sb.append("  ");
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Shows the loading state.
     */
    public void showLoading() {
        statusLabel.setText("");
        statusLabel.setBackground(null);
        timeLabel.setText("");
        sizeLabel.setText("");
        cardLayout.show(cardPanel, "LOADING");
    }
    
    /**
     * Shows a response.
     */
    public void showResponse(APIResponse response) {
        if (response.isError()) {
            showError(response.getErrorMessage());
            return;
        }
        
        // Update status bar
        int statusCode = response.getStatusCode();
        statusLabel.setText(statusCode + " " + response.getStatusText());
        statusLabel.setForeground(APITesterColors.buttonPrimaryText());
        if (statusCode >= 200 && statusCode < 300) {
            statusLabel.setBackground(APITesterColors.statusSuccess());
        } else if (statusCode >= 400 && statusCode < 500) {
            statusLabel.setBackground(APITesterColors.statusWarning());
        } else if (statusCode >= 500) {
            statusLabel.setBackground(APITesterColors.statusError());
        } else {
            statusLabel.setBackground(APITesterColors.statusNeutral());
        }
        
        timeLabel.setText(response.getFormattedTime());
        sizeLabel.setText(response.getFormattedSize());
        
        // Update body
        rawBody = response.getBody();
        if (response.isJsonBody()) {
            bodyTextArea.setText(formatJson(rawBody));
        } else {
            bodyTextArea.setText(rawBody != null ? rawBody : "");
        }
        bodyTextArea.setCaretPosition(0);
        
        // Update headers
        headersModel.setRowCount(0);
        Map<String, List<String>> headers = response.getHeaders();
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String value = String.join(", ", entry.getValue());
                headersModel.addRow(new Object[]{entry.getKey(), value});
            }
        }
        
        // Update test results
        testResultsModel.clear();
        List<APIResponse.AssertionResult> results = response.getAssertionResults();
        if (results != null && !results.isEmpty()) {
            int passed = response.getPassedAssertionsCount();
            int failed = response.getFailedAssertionsCount();
            
            if (failed == 0) {
                testResultsLabel.setText("All tests passed (" + passed + "/" + results.size() + ")");
                testResultsLabel.setForeground(APITesterColors.statusSuccess());
            } else {
                testResultsLabel.setText("Tests: " + passed + " passed, " + failed + " failed");
                testResultsLabel.setForeground(APITesterColors.statusError());
            }
            
            for (APIResponse.AssertionResult result : results) {
                String icon = result.isPassed() ? "✓" : "✗";
                String msg = icon + " " + result.getAssertionName();
                if (!result.isPassed() && result.getMessage() != null) {
                    msg += " - " + result.getMessage();
                }
                testResultsModel.addElement((result.isPassed() ? "PASS:" : "FAIL:") + msg);
            }
        } else {
            testResultsLabel.setText("No tests configured");
            testResultsLabel.setForeground(APITesterColors.textSecondary());
        }
        
        cardLayout.show(cardPanel, "RESPONSE");
    }
    
    /**
     * Shows an error message.
     */
    public void showError(String message) {
        statusLabel.setText("Error");
        statusLabel.setForeground(APITesterColors.buttonPrimaryText());
        statusLabel.setBackground(APITesterColors.statusError());
        timeLabel.setText("");
        sizeLabel.setText("");
        
        // Find and update the error area
        for (Component c : cardPanel.getComponents()) {
            if (c instanceof JScrollPane) {
                JScrollPane scroll = (JScrollPane) c;
                Component view = scroll.getViewport().getView();
                if (view instanceof JTextArea) {
                    JTextArea errorArea = (JTextArea) view;
                    if ("errorArea".equals(errorArea.getName())) {
                        errorArea.setText("Error: " + message);
                        break;
                    }
                }
            }
        }
        
        cardLayout.show(cardPanel, "ERROR");
    }
    
    /**
     * Clears the response panel.
     */
    public void clear() {
        statusLabel.setText("");
        statusLabel.setBackground(null);
        timeLabel.setText("");
        sizeLabel.setText("");
        bodyTextArea.setText("");
        rawBody = null;
        headersModel.setRowCount(0);
        testResultsModel.clear();
        testResultsLabel.setText("");
        cardLayout.show(cardPanel, "EMPTY");
    }
    
    /**
     * Called when theme changes to refresh colors.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        // Guard against calls during super constructor before fields are initialized
        if (cardPanel == null) return;
        
        // Refresh colors for all dynamically colored components
        refreshThemeColors();
    }
    
    /**
     * Refresh theme-sensitive colors on all components.
     * Public so it can be called when theme changes.
     */
    public void refreshThemeColors() {
        if (cardPanel == null) return;
        
        // Update time/size labels
        if (timeLabel != null) {
            timeLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
        if (sizeLabel != null) {
            sizeLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
        }
        
        // Update labeled components by name
        updateComponentColors(cardPanel);
        
        // Refresh text area colors using UIManager
        if (bodyTextArea != null) {
            bodyTextArea.setBackground(UIManager.getColor("TextArea.background"));
            bodyTextArea.setForeground(UIManager.getColor("TextArea.foreground"));
        }
        
        // Refresh headers table
        if (headersTable != null) {
            headersTable.setBackground(UIManager.getColor("Table.background"));
            headersTable.setForeground(UIManager.getColor("Table.foreground"));
        }
        
        // Refresh all panels recursively
        refreshPanelColors(this);
        
        repaint();
    }
    
    /**
     * Recursively refresh panel backgrounds.
     */
    private void refreshPanelColors(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JPanel) {
                c.setBackground(UIManager.getColor("Panel.background"));
            }
            if (c instanceof JScrollPane) {
                JScrollPane sp = (JScrollPane) c;
                sp.getViewport().setBackground(UIManager.getColor("Panel.background"));
            }
            if (c instanceof Container) {
                refreshPanelColors((Container) c);
            }
        }
    }
    
    /**
     * Recursively update colors in component tree.
     */
    private void updateComponentColors(Container container) {
        if (container == null) return;
        
        for (Component c : container.getComponents()) {
            if ("emptyLabel".equals(c.getName()) || "loadingLabel".equals(c.getName())) {
                c.setForeground(APITesterColors.textSecondary());
            } else if ("errorArea".equals(c.getName()) && c instanceof JTextArea) {
                JTextArea ta = (JTextArea) c;
                ta.setForeground(APITesterColors.statusError());
                ta.setBackground(APITesterColors.panelBackground());
            } else if ("statusBar".equals(c.getName())) {
                c.setBackground(APITesterColors.panelBackground());
            }
            
            if (c instanceof Container) {
                updateComponentColors((Container) c);
            }
        }
    }
    
    /**
     * Custom renderer for test results.
     */
    private static class TestResultRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            String text = (String) value;
            if (text.startsWith("PASS:")) {
                setText(text.substring(5));
                setForeground(APITesterColors.statusSuccess());
            } else if (text.startsWith("FAIL:")) {
                setText(text.substring(5));
                setForeground(APITesterColors.statusError());
            }
            
            setBorder(new EmptyBorder(5, 10, 5, 10));
            return this;
        }
    }
}
