package com.ing.ide.main.mainui.components.apitester.request;

import com.ing.datalib.api.*;
import com.ing.ide.main.mainui.components.apitester.APITesterUI;
import com.ing.ide.main.mainui.components.apitester.util.APITesterColors;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel for building API requests.
 * Contains URL bar, method selector, headers, params, body, and auth tabs.
 */
public class RequestPanel extends JPanel {

    private final APITesterUI parent;
    
    // URL bar components
    private JComboBox<APIRequest.HttpMethod> methodSelector;
    private JTextField urlField;
    private JButton sendButton;
    private JButton saveButton;
    
    // Tab components
    private JTabbedPane tabPane;
    private KeyValueTablePanel paramsPanel;
    private KeyValueTablePanel headersPanel;
    private BodyPanel bodyPanel;
    private AuthPanel authPanel;
    private SettingsPanel settingsPanel;
    
    public RequestPanel(APITesterUI parent) {
        this.parent = parent;
        initComponents();
    }
    
    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        setBorder(new EmptyBorder(0, 0, 0, 0));
        
        // URL bar at top
        JPanel urlBar = createUrlBar();
        add(urlBar, BorderLayout.NORTH);
        
        // Tabbed pane for params, headers, body, auth
        tabPane = new JTabbedPane(JTabbedPane.TOP);
        tabPane.setFont(tabPane.getFont().deriveFont(11f));
        
        // Params tab
        paramsPanel = new KeyValueTablePanel("Query Parameters", "Key", "Value");
        tabPane.addTab("Params", paramsPanel);
        
        // Headers tab
        headersPanel = new KeyValueTablePanel("Headers", "Key", "Value");
        addDefaultHeaders();
        tabPane.addTab("Headers", headersPanel);
        
        // Body tab
        bodyPanel = new BodyPanel();
        tabPane.addTab("Body", bodyPanel);
        
        // Auth tab
        authPanel = new AuthPanel();
        tabPane.addTab("Auth", authPanel);
        
        // Settings tab
        settingsPanel = new SettingsPanel();
        tabPane.addTab("Settings", settingsPanel);
        
        add(tabPane, BorderLayout.CENTER);
    }
    
    private JPanel createUrlBar() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        panel.setBackground(APITesterColors.panelBackground());
        panel.setName("urlBar");
        
        // Method selector
        methodSelector = new JComboBox<>(APIRequest.HttpMethod.values());
        methodSelector.setFont(methodSelector.getFont().deriveFont(Font.BOLD, 12f));
        methodSelector.setPreferredSize(new Dimension(100, 36));
        methodSelector.setRenderer(new MethodComboRenderer());
        
        // URL field
        urlField = new JTextField();
        urlField.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        urlField.putClientProperty("JTextField.placeholderText", "Enter request URL (e.g., https://api.example.com/users)");
        urlField.setPreferredSize(new Dimension(100, 36));
        
        // Send button
        sendButton = new JButton("Send");
        sendButton.setFont(sendButton.getFont().deriveFont(Font.BOLD, 12f));
        sendButton.setBackground(APITesterColors.buttonPrimary());
        sendButton.setForeground(APITesterColors.buttonPrimaryText());
        sendButton.setPreferredSize(new Dimension(80, 36));
        sendButton.setFocusPainted(false);
        sendButton.addActionListener(e -> parent.sendRequest());
        
        // Save button
        saveButton = new JButton("Save");
        saveButton.setFont(saveButton.getFont().deriveFont(12f));
        saveButton.setPreferredSize(new Dimension(70, 36));
        saveButton.addActionListener(e -> parent.saveRequest());
        
        // Convert to Test button
        JButton convertButton = new JButton("⇢ Test");
        convertButton.setFont(convertButton.getFont().deriveFont(11f));
        convertButton.setPreferredSize(new Dimension(75, 36));
        convertButton.setToolTipText("Convert to INGenious Test Case");
        convertButton.addActionListener(e -> showConvertToTestDialog());
        
        // Layout
        JPanel leftPart = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftPart.setOpaque(false);
        leftPart.add(methodSelector);
        
        JPanel rightPart = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        rightPart.setOpaque(false);
        rightPart.add(convertButton);
        rightPart.add(saveButton);
        rightPart.add(sendButton);
        
        panel.add(leftPart, BorderLayout.WEST);
        panel.add(urlField, BorderLayout.CENTER);
        panel.add(rightPart, BorderLayout.EAST);
        
        return panel;
    }
    
    private void addDefaultHeaders() {
        headersPanel.addRow("Content-Type", "application/json", true);
        headersPanel.addRow("Accept", "application/json", true);
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * Loads a request into the panel.
     */
    public void loadRequest(APIRequest request) {
        // Method and URL
        methodSelector.setSelectedItem(request.getMethod());
        urlField.setText(request.getUrl() != null ? request.getUrl() : "");
        
        // Query params
        paramsPanel.clear();
        if (request.getQueryParams() != null) {
            for (KeyValuePair kvp : request.getQueryParams()) {
                paramsPanel.addRow(kvp.getKey(), kvp.getValue(), kvp.isEnabled());
            }
        }
        
        // Headers
        headersPanel.clear();
        if (request.getHeaders() != null && !request.getHeaders().isEmpty()) {
            for (KeyValuePair kvp : request.getHeaders()) {
                headersPanel.addRow(kvp.getKey(), kvp.getValue(), kvp.isEnabled());
            }
        } else {
            addDefaultHeaders();
        }
        
        // Body
        bodyPanel.loadBody(request.getBody());
        
        // Auth
        authPanel.loadAuth(request.getAuth());
        
        // Settings
        settingsPanel.loadSettings(request);
    }
    
    /**
     * Updates a request object from the panel values.
     */
    public void updateRequest(APIRequest request) {
        // Method and URL
        request.setMethod((APIRequest.HttpMethod) methodSelector.getSelectedItem());
        request.setUrl(urlField.getText().trim());
        
        // Query params
        request.setQueryParams(paramsPanel.getKeyValuePairs());
        
        // Headers
        request.setHeaders(headersPanel.getKeyValuePairs());
        
        // Body
        request.setBody(bodyPanel.getBody());
        
        // Auth
        request.setAuth(authPanel.getAuth());
        
        // Settings
        settingsPanel.updateRequest(request);
    }
    
    /**
     * Sets focus to the URL field.
     */
    public void focusUrl() {
        urlField.requestFocusInWindow();
    }
    
    /**
     * Enables/disables the send button.
     */
    public void setSendEnabled(boolean enabled) {
        sendButton.setEnabled(enabled);
    }
    
    /**
     * Shows dialog to convert current request to an INGenious test case.
     */
    private void showConvertToTestDialog() {
        // Update the current request from UI
        updateRequest(parent.getCurrentRequest());
        APIRequest request = parent.getCurrentRequest();
        
        // Get available scenarios
        java.util.List<com.ing.datalib.component.Scenario> scenarios = parent.getApiTester().getAvailableScenarios();
        
        if (scenarios.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "No scenarios available. Please open a project and create a scenario first.",
                    "No Scenarios", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Create dialog
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridLayout(3, 2, 10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        panel.add(new javax.swing.JLabel("Target Scenario:"));
        javax.swing.JComboBox<com.ing.datalib.component.Scenario> scenarioCombo = 
                new javax.swing.JComboBox<>(scenarios.toArray(new com.ing.datalib.component.Scenario[0]));
        scenarioCombo.setRenderer(new javax.swing.DefaultListCellRenderer() {
            @Override
            public java.awt.Component getListCellRendererComponent(javax.swing.JList<?> list, Object value, 
                    int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof com.ing.datalib.component.Scenario) {
                    setText(((com.ing.datalib.component.Scenario) value).getName());
                }
                return this;
            }
        });
        panel.add(scenarioCombo);
        
        panel.add(new javax.swing.JLabel("Test Case Name:"));
        String defaultName = request.getName() != null ? request.getName() : 
                request.getMethod() + "_" + extractPathName(request.getUrl());
        javax.swing.JTextField nameField = new javax.swing.JTextField(defaultName);
        panel.add(nameField);
        
        panel.add(new javax.swing.JLabel(""));
        javax.swing.JLabel infoLabel = new javax.swing.JLabel(
                "<html><small>Creates test steps using Webservice actions</small></html>");
        infoLabel.setForeground(APITesterColors.textSecondary());
        panel.add(infoLabel);
        
        int result = javax.swing.JOptionPane.showConfirmDialog(this, panel,
                "Convert to INGenious Test", javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.PLAIN_MESSAGE);
        
        if (result == javax.swing.JOptionPane.OK_OPTION) {
            com.ing.datalib.component.Scenario selectedScenario = 
                    (com.ing.datalib.component.Scenario) scenarioCombo.getSelectedItem();
            String testCaseName = nameField.getText().trim();
            
            if (testCaseName.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Please enter a test case name.",
                        "Invalid Name", javax.swing.JOptionPane.WARNING_MESSAGE);
                return;
            }
            
            // Perform conversion
            com.ing.datalib.component.TestCase testCase = 
                    parent.getApiTester().convertRequestToTestCase(request, selectedScenario, testCaseName);
            
            if (testCase != null) {
                // Ask user if they want to navigate to Test Design
                int navigateResult = javax.swing.JOptionPane.showConfirmDialog(this,
                        "Successfully created test case '" + testCaseName + "' in scenario '" + 
                        selectedScenario.getName() + "'.\n\nWould you like to open it in Test Design?",
                        "Conversion Successful", javax.swing.JOptionPane.YES_NO_OPTION,
                        javax.swing.JOptionPane.INFORMATION_MESSAGE);
                
                if (navigateResult == javax.swing.JOptionPane.YES_OPTION) {
                    parent.getApiTester().navigateToTestCase(testCase);
                }
            } else {
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Failed to convert request to test case. Check the logs for details.",
                        "Conversion Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    private String extractPathName(String url) {
        if (url == null || url.isEmpty()) return "Request";
        try {
            java.net.URI uri = java.net.URI.create(url);
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                String[] parts = path.split("/");
                for (int i = parts.length - 1; i >= 0; i--) {
                    if (!parts[i].isEmpty()) {
                        return parts[i].replaceAll("[^a-zA-Z0-9]", "_");
                    }
                }
            }
            return uri.getHost() != null ? uri.getHost().replaceAll("[^a-zA-Z0-9]", "_") : "Request";
        } catch (Exception e) {
            return "Request";
        }
    }
    
    /**
     * Refresh all theme-sensitive colors. Called when theme changes.
     */
    public void refreshThemeColors() {
        if (sendButton == null) return;
        
        // Refresh button colors - keep Send button green
        sendButton.setBackground(APITesterColors.buttonPrimary());
        sendButton.setForeground(APITesterColors.buttonPrimaryText());
        
        // Refresh all panels using UIManager colors
        refreshPanelColors(this);
        
        // Refresh URL field
        if (urlField != null) {
            urlField.setBackground(UIManager.getColor("TextField.background"));
            urlField.setForeground(UIManager.getColor("TextField.foreground"));
        }
        
        // Refresh method selector
        if (methodSelector != null) {
            methodSelector.setBackground(UIManager.getColor("ComboBox.background"));
        }
        
        // Refresh body panel colors
        if (bodyPanel != null) {
            bodyPanel.refreshThemeColors();
        }
        
        // Refresh auth panel colors
        if (authPanel != null) {
            authPanel.refreshThemeColors();
        }
        
        // Refresh settings panel colors
        if (settingsPanel != null) {
            settingsPanel.refreshThemeColors();
        }
        
        // Refresh params and headers panels
        if (paramsPanel != null) {
            paramsPanel.refreshThemeColors();
        }
        if (headersPanel != null) {
            headersPanel.refreshThemeColors();
        }
        
        repaint();
    }
    
    /**
     * Recursively refresh panel backgrounds.
     */
    private void refreshPanelColors(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JPanel && c != sendButton.getParent()) {
                c.setBackground(UIManager.getColor("Panel.background"));
            }
            if (c instanceof JScrollPane) {
                JScrollPane sp = (JScrollPane) c;
                sp.getViewport().setBackground(UIManager.getColor("Panel.background"));
            }
            if (c instanceof JTabbedPane) {
                c.setBackground(UIManager.getColor("TabbedPane.background"));
            }
            if (c instanceof Container) {
                refreshPanelColors((Container) c);
            }
        }
    }
    
    /**
     * Called when theme changes to refresh colors.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        refreshThemeColors();
    }

    // ═══════════════════════════════════════════════════════════════════
    // Method Combo Renderer
    // ═══════════════════════════════════════════════════════════════════
    
    private static class MethodComboRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof APIRequest.HttpMethod) {
                APIRequest.HttpMethod method = (APIRequest.HttpMethod) value;
                setForeground(getMethodColor(method));
                setFont(getFont().deriveFont(Font.BOLD));
            }
            
            return this;
        }
        
        private Color getMethodColor(APIRequest.HttpMethod method) {
            switch (method) {
                case GET: return APITesterColors.methodGet();
                case POST: return APITesterColors.methodPost();
                case PUT: return APITesterColors.methodPut();
                case PATCH: return APITesterColors.methodPatch();
                case DELETE: return APITesterColors.methodDelete();
                default: return APITesterColors.statusNeutral();
            }
        }
    }
}

/**
 * Reusable key-value table panel for headers, params, etc.
 */
class KeyValueTablePanel extends JPanel {
    
    private final JTable table;
    private final DefaultTableModel tableModel;
    
    public KeyValueTablePanel(String title, String keyHeader, String valueHeader) {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));
        
        // Table model with checkbox column
        tableModel = new DefaultTableModel(new String[]{"✓", keyHeader, valueHeader}, 0) {
            @Override
            public Class<?> getColumnClass(int column) {
                return column == 0 ? Boolean.class : String.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return true;
            }
        };
        
        table = new JTable(tableModel);
        table.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        table.setRowHeight(28);
        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(0).setMaxWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(200);
        table.getColumnModel().getColumn(2).setPreferredWidth(300);
        table.putClientProperty("terminateEditOnFocusLost", true);
        
        // Add button panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.setBorder(new EmptyBorder(5, 0, 0, 0));
        
        JButton addBtn = new JButton("+ Add");
        addBtn.setFont(addBtn.getFont().deriveFont(11f));
        addBtn.addActionListener(e -> addRow("", "", true));
        
        JButton removeBtn = new JButton("- Remove");
        removeBtn.setFont(removeBtn.getFont().deriveFont(11f));
        removeBtn.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row >= 0) {
                tableModel.removeRow(row);
            }
        });
        
        buttonPanel.add(addBtn);
        buttonPanel.add(removeBtn);
        
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
        
        add(scroll, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    public void addRow(String key, String value, boolean enabled) {
        tableModel.addRow(new Object[]{enabled, key, value});
    }
    
    public void clear() {
        tableModel.setRowCount(0);
    }
    
    public List<KeyValuePair> getKeyValuePairs() {
        List<KeyValuePair> pairs = new ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            Boolean enabled = (Boolean) tableModel.getValueAt(i, 0);
            String key = (String) tableModel.getValueAt(i, 1);
            String value = (String) tableModel.getValueAt(i, 2);
            if (key != null && !key.trim().isEmpty()) {
                pairs.add(new KeyValuePair(key, value, enabled != null && enabled));
            }
        }
        return pairs;
    }
    
    /**
     * Refresh theme colors.
     */
    public void refreshThemeColors() {
        setBackground(UIManager.getColor("Panel.background"));
        table.setBackground(UIManager.getColor("Table.background"));
        table.setForeground(UIManager.getColor("Table.foreground"));
        table.getTableHeader().setBackground(UIManager.getColor("TableHeader.background"));
        table.getTableHeader().setForeground(UIManager.getColor("TableHeader.foreground"));
        repaint();
    }
}

/**
 * Panel for editing request body.
 */
class BodyPanel extends JPanel {
    
    private JComboBox<RequestBody.BodyType> typeSelector;
    private JComboBox<RequestBody.RawFormat> formatSelector;
    private RSyntaxTextArea bodyTextArea;
    private CardLayout cardLayout;
    private JPanel contentPanel;
    
    public BodyPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));
        
        // Type selector
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topPanel.setBorder(new EmptyBorder(0, 0, 8, 0));
        
        typeSelector = new JComboBox<>(RequestBody.BodyType.values());
        typeSelector.setFont(typeSelector.getFont().deriveFont(11f));
        typeSelector.addActionListener(e -> updateBodyPanel());
        
        formatSelector = new JComboBox<>(RequestBody.RawFormat.values());
        formatSelector.setFont(formatSelector.getFont().deriveFont(11f));
        formatSelector.addActionListener(e -> updateSyntaxStyle());
        
        topPanel.add(new JLabel("Type:"));
        topPanel.add(typeSelector);
        topPanel.add(new JLabel("Format:"));
        topPanel.add(formatSelector);
        
        // Content panel with card layout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        
        // Raw body editor with syntax highlighting
        bodyTextArea = new RSyntaxTextArea();
        bodyTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        bodyTextArea.setTabSize(2);
        bodyTextArea.setCodeFoldingEnabled(true);
        bodyTextArea.setAntiAliasingEnabled(true);
        bodyTextArea.setBracketMatchingEnabled(true);
        bodyTextArea.setAutoIndentEnabled(true);
        bodyTextArea.setMarkOccurrences(true);
        updateSyntaxStyle();
        RTextScrollPane textScroll = new RTextScrollPane(bodyTextArea);
        textScroll.setLineNumbersEnabled(true);
        textScroll.setBorder(BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")));
        contentPanel.add(textScroll, "RAW");
        
        // None placeholder
        JLabel noneLabel = new JLabel("This request does not have a body", JLabel.CENTER);
        noneLabel.setForeground(APITesterColors.textSecondary());
        noneLabel.setName("bodyNoneLabel");
        contentPanel.add(noneLabel, "NONE");
        
        add(topPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
        
        updateBodyPanel();
    }
    
    private void updateBodyPanel() {
        RequestBody.BodyType type = (RequestBody.BodyType) typeSelector.getSelectedItem();
        if (type == RequestBody.BodyType.NONE) {
            cardLayout.show(contentPanel, "NONE");
            formatSelector.setEnabled(false);
        } else {
            cardLayout.show(contentPanel, "RAW");
            formatSelector.setEnabled(type == RequestBody.BodyType.RAW);
        }
        updateSyntaxStyle();
    }
    
    private void updateSyntaxStyle() {
        if (bodyTextArea == null) return;
        RequestBody.RawFormat format = (RequestBody.RawFormat) formatSelector.getSelectedItem();
        if (format == null) format = RequestBody.RawFormat.JSON;
        switch (format) {
            case JSON:
                bodyTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JSON);
                break;
            case XML:
                bodyTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
                break;
            case HTML:
                bodyTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_HTML);
                break;
            case JAVASCRIPT:
                bodyTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
                break;
            default:
                bodyTextArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_NONE);
                break;
        }
    }
    
    public void loadBody(RequestBody body) {
        if (body == null) {
            typeSelector.setSelectedItem(RequestBody.BodyType.NONE);
            bodyTextArea.setText("");
            return;
        }
        
        typeSelector.setSelectedItem(body.getBodyType());
        formatSelector.setSelectedItem(body.getRawFormat());
        bodyTextArea.setText(body.getRawContent() != null ? body.getRawContent() : "");
        updateBodyPanel();
    }
    
    public RequestBody getBody() {
        RequestBody body = new RequestBody();
        body.setBodyType((RequestBody.BodyType) typeSelector.getSelectedItem());
        body.setRawFormat((RequestBody.RawFormat) formatSelector.getSelectedItem());
        body.setRawContent(bodyTextArea.getText());
        return body;
    }
    
    /**
     * Refresh theme colors.
     */
    public void refreshThemeColors() {
        setBackground(UIManager.getColor("Panel.background"));
        contentPanel.setBackground(UIManager.getColor("Panel.background"));
        
        // Refresh text area colors
        if (bodyTextArea != null) {
            bodyTextArea.setBackground(UIManager.getColor("TextArea.background"));
            bodyTextArea.setForeground(UIManager.getColor("TextArea.foreground"));
        }
        
        // Refresh combo boxes
        if (typeSelector != null) {
            typeSelector.setBackground(UIManager.getColor("ComboBox.background"));
        }
        if (formatSelector != null) {
            formatSelector.setBackground(UIManager.getColor("ComboBox.background"));
        }
        
        // Refresh none label
        for (Component c : contentPanel.getComponents()) {
            if ("bodyNoneLabel".equals(c.getName())) {
                c.setForeground(UIManager.getColor("Label.disabledForeground"));
            }
        }
        repaint();
    }
}

/**
 * Panel for configuring authentication.
 */
class AuthPanel extends JPanel {
    
    private JComboBox<AuthConfig.AuthType> typeSelector;
    private CardLayout cardLayout;
    private JPanel authPanel;
    
    // Basic Auth
    private JTextField basicUsername;
    private JPasswordField basicPassword;
    
    // Bearer Token
    private JTextField bearerToken;
    private JTextField bearerPrefix;
    
    // API Key
    private JTextField apiKeyName;
    private JTextField apiKeyValue;
    private JComboBox<AuthConfig.ApiKeyLocation> apiKeyLocation;
    
    public AuthPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(8, 8, 8, 8));
        
        // Type selector
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        topPanel.setBorder(new EmptyBorder(0, 0, 8, 0));
        
        typeSelector = new JComboBox<>(AuthConfig.AuthType.values());
        typeSelector.setFont(typeSelector.getFont().deriveFont(11f));
        typeSelector.addActionListener(e -> updateAuthPanel());
        
        topPanel.add(new JLabel("Type:"));
        topPanel.add(typeSelector);
        
        // Auth details panel
        cardLayout = new CardLayout();
        authPanel = new JPanel(cardLayout);
        
        // None
        JLabel noneLabel = new JLabel("This request does not use any authorization", JLabel.CENTER);
        noneLabel.setForeground(APITesterColors.textSecondary());
        noneLabel.setName("authNoneLabel");
        authPanel.add(noneLabel, "NONE");
        
        // Basic Auth
        JPanel basicPanel = createBasicAuthPanel();
        authPanel.add(basicPanel, "BASIC");
        
        // Bearer Token
        JPanel bearerPanel = createBearerPanel();
        authPanel.add(bearerPanel, "BEARER");
        
        // API Key
        JPanel apiKeyPanel = createApiKeyPanel();
        authPanel.add(apiKeyPanel, "API_KEY");
        
        add(topPanel, BorderLayout.NORTH);
        add(authPanel, BorderLayout.CENTER);
        
        updateAuthPanel();
    }
    
    private JPanel createBasicAuthPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        basicUsername = new JTextField(30);
        panel.add(basicUsername, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        basicPassword = new JPasswordField(30);
        panel.add(basicPassword, gbc);
        
        return panel;
    }
    
    private JPanel createBearerPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Token:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        bearerToken = new JTextField(40);
        panel.add(bearerToken, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Prefix:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        bearerPrefix = new JTextField("Bearer", 20);
        panel.add(bearerPrefix, gbc);
        
        return panel;
    }
    
    private JPanel createApiKeyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("Key Name:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        apiKeyName = new JTextField(30);
        panel.add(apiKeyName, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Key Value:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        apiKeyValue = new JTextField(30);
        panel.add(apiKeyValue, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel("Add to:"), gbc);
        gbc.gridx = 1;
        apiKeyLocation = new JComboBox<>(AuthConfig.ApiKeyLocation.values());
        panel.add(apiKeyLocation, gbc);
        
        return panel;
    }
    
    private void updateAuthPanel() {
        AuthConfig.AuthType type = (AuthConfig.AuthType) typeSelector.getSelectedItem();
        cardLayout.show(authPanel, type.name());
    }
    
    public void loadAuth(AuthConfig auth) {
        if (auth == null) {
            typeSelector.setSelectedItem(AuthConfig.AuthType.NONE);
            return;
        }
        
        typeSelector.setSelectedItem(auth.getAuthType());
        
        basicUsername.setText(auth.getBasicUsername() != null ? auth.getBasicUsername() : "");
        basicPassword.setText(auth.getBasicPassword() != null ? auth.getBasicPassword() : "");
        bearerToken.setText(auth.getBearerToken() != null ? auth.getBearerToken() : "");
        bearerPrefix.setText(auth.getBearerPrefix() != null ? auth.getBearerPrefix() : "Bearer");
        apiKeyName.setText(auth.getApiKeyName() != null ? auth.getApiKeyName() : "");
        apiKeyValue.setText(auth.getApiKeyValue() != null ? auth.getApiKeyValue() : "");
        if (auth.getApiKeyLocation() != null) {
            apiKeyLocation.setSelectedItem(auth.getApiKeyLocation());
        }
        
        updateAuthPanel();
    }
    
    public AuthConfig getAuth() {
        AuthConfig auth = new AuthConfig();
        auth.setAuthType((AuthConfig.AuthType) typeSelector.getSelectedItem());
        auth.setBasicUsername(basicUsername.getText());
        auth.setBasicPassword(new String(basicPassword.getPassword()));
        auth.setBearerToken(bearerToken.getText());
        auth.setBearerPrefix(bearerPrefix.getText());
        auth.setApiKeyName(apiKeyName.getText());
        auth.setApiKeyValue(apiKeyValue.getText());
        auth.setApiKeyLocation((AuthConfig.ApiKeyLocation) apiKeyLocation.getSelectedItem());
        return auth;
    }
    
    /**
     * Refresh theme colors.
     */
    public void refreshThemeColors() {
        setBackground(UIManager.getColor("Panel.background"));
        authPanel.setBackground(UIManager.getColor("Panel.background"));
        
        // Refresh text fields
        if (basicUsername != null) {
            basicUsername.setBackground(UIManager.getColor("TextField.background"));
            basicUsername.setForeground(UIManager.getColor("TextField.foreground"));
        }
        if (basicPassword != null) {
            basicPassword.setBackground(UIManager.getColor("TextField.background"));
            basicPassword.setForeground(UIManager.getColor("TextField.foreground"));
        }
        if (bearerToken != null) {
            bearerToken.setBackground(UIManager.getColor("TextField.background"));
            bearerToken.setForeground(UIManager.getColor("TextField.foreground"));
        }
        if (bearerPrefix != null) {
            bearerPrefix.setBackground(UIManager.getColor("TextField.background"));
            bearerPrefix.setForeground(UIManager.getColor("TextField.foreground"));
        }
        if (apiKeyName != null) {
            apiKeyName.setBackground(UIManager.getColor("TextField.background"));
            apiKeyName.setForeground(UIManager.getColor("TextField.foreground"));
        }
        if (apiKeyValue != null) {
            apiKeyValue.setBackground(UIManager.getColor("TextField.background"));
            apiKeyValue.setForeground(UIManager.getColor("TextField.foreground"));
        }
        
        // Refresh combo boxes
        if (typeSelector != null) {
            typeSelector.setBackground(UIManager.getColor("ComboBox.background"));
        }
        if (apiKeyLocation != null) {
            apiKeyLocation.setBackground(UIManager.getColor("ComboBox.background"));
        }
        
        // Refresh none label
        for (Component c : authPanel.getComponents()) {
            if ("authNoneLabel".equals(c.getName())) {
                c.setForeground(UIManager.getColor("Label.disabledForeground"));
            }
        }
        
        // Refresh child panels
        for (Component c : authPanel.getComponents()) {
            if (c instanceof JPanel) {
                c.setBackground(UIManager.getColor("Panel.background"));
            }
        }
        repaint();
    }
}

/**
 * Panel for per-request settings (SSL, timeout, redirects).
 * Similar to Postman's request Settings tab.
 */
class SettingsPanel extends JPanel {

    private JCheckBox sslVerificationCheckbox;
    private JCheckBox followRedirectsCheckbox;
    private JSpinner timeoutSpinner;

    // Certificate fields
    private JCheckBox certEnabledCheckbox;
    private JComboBox<CertificateConfig.CertificateType> certTypeSelector;
    private JTextField caCertField;
    private JTextField clientCertField;
    private JTextField clientKeyField;
    private JTextField pfxField;
    private JPasswordField passphraseField;
    private JPanel pemPanel;
    private JPanel pfxPanel;
    private CardLayout certCardLayout;

    public SettingsPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        initComponents();
    }

    private void initComponents() {
        JPanel settingsGrid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        // Section header: SSL
        JLabel sslHeader = new JLabel("SSL Certificate Verification");
        sslHeader.setFont(sslHeader.getFont().deriveFont(Font.BOLD, 13f));
        settingsGrid.add(sslHeader, gbc);

        gbc.gridy = 1;
        gbc.gridwidth = 1;
        sslVerificationCheckbox = new JCheckBox("Enable SSL certificate verification");
        sslVerificationCheckbox.setSelected(true);
        sslVerificationCheckbox.setFont(sslVerificationCheckbox.getFont().deriveFont(12f));
        settingsGrid.add(sslVerificationCheckbox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        JLabel sslNote = new JLabel(
                "When disabled, requests will skip SSL certificate validation. Use this for self-signed certificates or test environments.");
        sslNote.setFont(sslNote.getFont().deriveFont(Font.ITALIC, 11f));
        sslNote.setForeground(UIManager.getColor("Label.disabledForeground"));
        sslNote.setName("sslNoteLabel");
        settingsGrid.add(sslNote, gbc);

        // Separator
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        settingsGrid.add(new JSeparator(), gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;

        // Section header: Request Behavior
        gbc.gridy = 4;
        JLabel behaviorHeader = new JLabel("Request Behavior");
        behaviorHeader.setFont(behaviorHeader.getFont().deriveFont(Font.BOLD, 13f));
        settingsGrid.add(behaviorHeader, gbc);

        gbc.gridy = 5;
        gbc.gridwidth = 1;
        followRedirectsCheckbox = new JCheckBox("Automatically follow redirects");
        followRedirectsCheckbox.setSelected(true);
        followRedirectsCheckbox.setFont(followRedirectsCheckbox.getFont().deriveFont(12f));
        settingsGrid.add(followRedirectsCheckbox, gbc);

        gbc.gridy = 6;
        gbc.gridwidth = 1;
        JLabel timeoutLabel = new JLabel("Request timeout (ms):");
        timeoutLabel.setFont(timeoutLabel.getFont().deriveFont(12f));
        settingsGrid.add(timeoutLabel, gbc);

        gbc.gridx = 1;
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(30000, 0, 300000, 1000));
        timeoutSpinner.setPreferredSize(new Dimension(100, 28));
        timeoutSpinner.setFont(timeoutSpinner.getFont().deriveFont(12f));
        settingsGrid.add(timeoutSpinner, gbc);

        // Separator
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        settingsGrid.add(new JSeparator(), gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;

        // Section header: Client Certificates
        gbc.gridy = 8;
        JLabel certHeader = new JLabel("Client Certificates");
        certHeader.setFont(certHeader.getFont().deriveFont(Font.BOLD, 13f));
        settingsGrid.add(certHeader, gbc);

        gbc.gridy = 9;
        gbc.gridwidth = 1;
        certEnabledCheckbox = new JCheckBox("Use client certificates");
        certEnabledCheckbox.setSelected(false);
        certEnabledCheckbox.setFont(certEnabledCheckbox.getFont().deriveFont(12f));
        certEnabledCheckbox.addActionListener(e -> updateCertificatePanel());
        settingsGrid.add(certEnabledCheckbox, gbc);

        // Certificate type selector
        gbc.gridx = 0;
        gbc.gridy = 10;
        gbc.gridwidth = 1;
        JLabel certTypeLabel = new JLabel("Certificate type:");
        certTypeLabel.setFont(certTypeLabel.getFont().deriveFont(12f));
        settingsGrid.add(certTypeLabel, gbc);

        gbc.gridx = 1;
        certTypeSelector = new JComboBox<>(CertificateConfig.CertificateType.values());
        certTypeSelector.setFont(certTypeSelector.getFont().deriveFont(11f));
        certTypeSelector.addActionListener(e -> updateCertificatePanel());
        settingsGrid.add(certTypeSelector, gbc);

        // Certificate details panel with CardLayout
        gbc.gridx = 0;
        gbc.gridy = 11;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        JPanel certDetailsContainer = new JPanel();
        certCardLayout = new CardLayout();
        certDetailsContainer.setLayout(certCardLayout);
        
        // Create PEM and PFX panels
        pemPanel = createPemPanel();
        pfxPanel = createPfxPanel();
        
        certDetailsContainer.add(pemPanel, "PEM");
        certDetailsContainer.add(pfxPanel, "PFX");
        settingsGrid.add(certDetailsContainer, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;

        // Push everything to the top
        gbc.gridx = 0;
        gbc.gridy = 12;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        settingsGrid.add(Box.createVerticalGlue(), gbc);

        add(settingsGrid, BorderLayout.CENTER);
        
        updateCertificatePanel();
    }
    
    private JPanel createPemPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // CA Certificate
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("CA Certificate:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        caCertField = new JTextField(30);
        caCertField.setToolTipText("Path to CA certificate file (.pem/.crt)");
        panel.add(caCertField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JButton caCertBrowse = new JButton("Browse...");
        caCertBrowse.addActionListener(e -> browseCertificateFile(caCertField, "Select CA Certificate"));
        panel.add(caCertBrowse, gbc);
        
        // Client Certificate
        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(new JLabel("Client Certificate:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        clientCertField = new JTextField(30);
        clientCertField.setToolTipText("Path to client certificate file (.pem/.crt)");
        panel.add(clientCertField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JButton clientCertBrowse = new JButton("Browse...");
        clientCertBrowse.addActionListener(e -> browseCertificateFile(clientCertField, "Select Client Certificate"));
        panel.add(clientCertBrowse, gbc);
        
        // Client Private Key
        gbc.gridx = 0; gbc.gridy = 2;
        panel.add(new JLabel("Client Private Key:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        clientKeyField = new JTextField(30);
        clientKeyField.setToolTipText("Path to client private key file (.key/.pem)");
        panel.add(clientKeyField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JButton clientKeyBrowse = new JButton("Browse...");
        clientKeyBrowse.addActionListener(e -> browseCertificateFile(clientKeyField, "Select Client Private Key"));
        panel.add(clientKeyBrowse, gbc);
        
        // Passphrase
        gbc.gridx = 0; gbc.gridy = 3;
        panel.add(new JLabel("Passphrase:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        passphraseField = new JPasswordField(30);
        passphraseField.setToolTipText("Private key passphrase (optional)");
        panel.add(passphraseField, gbc);
        
        return panel;
    }
    
    private JPanel createPfxPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // PFX File
        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(new JLabel("PFX/PKCS12 File:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1;
        pfxField = new JTextField(30);
        pfxField.setToolTipText("Path to PFX/PKCS12 keystore file (.pfx/.p12)");
        panel.add(pfxField, gbc);
        gbc.gridx = 2; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        JButton pfxBrowse = new JButton("Browse...");
        pfxBrowse.addActionListener(e -> browseCertificateFile(pfxField, "Select PFX/PKCS12 File"));
        panel.add(pfxBrowse, gbc);
        
        return panel;
    }
    
    private void browseCertificateFile(JTextField targetField, String title) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(title);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        String currentPath = targetField.getText().trim();
        if (!currentPath.isEmpty()) {
            fileChooser.setSelectedFile(new java.io.File(currentPath));
        }
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            targetField.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
    }
    
    private void updateCertificatePanel() {
        boolean enabled = certEnabledCheckbox.isSelected();
        certTypeSelector.setEnabled(enabled);
        
        if (enabled) {
            CertificateConfig.CertificateType type = (CertificateConfig.CertificateType) certTypeSelector.getSelectedItem();
            certCardLayout.show(pemPanel.getParent(), type.name());
        }
        
        // Enable/disable all certificate fields
        enableCertificateFields(enabled);
    }
    
    private void enableCertificateFields(boolean enabled) {
        if (caCertField != null) caCertField.setEnabled(enabled);
        if (clientCertField != null) clientCertField.setEnabled(enabled);
        if (clientKeyField != null) clientKeyField.setEnabled(enabled);
        if (pfxField != null) pfxField.setEnabled(enabled);
        if (passphraseField != null) passphraseField.setEnabled(enabled);
    }

    /**
     * Loads settings from an APIRequest into this panel.
     */
    public void loadSettings(APIRequest request) {
        sslVerificationCheckbox.setSelected(request.isSslVerificationEnabled());
        followRedirectsCheckbox.setSelected(request.isFollowRedirects());
        timeoutSpinner.setValue(request.getTimeout() > 0 ? request.getTimeout() : 30000);
        
        // Load certificate config
        CertificateConfig certConfig = request.getCertificateConfig();
        if (certConfig != null) {
            certEnabledCheckbox.setSelected(certConfig.isEnabled());
            certTypeSelector.setSelectedItem(certConfig.getCertificateType());
            caCertField.setText(certConfig.getCaCertPath() != null ? certConfig.getCaCertPath() : "");
            clientCertField.setText(certConfig.getClientCertPath() != null ? certConfig.getClientCertPath() : "");
            clientKeyField.setText(certConfig.getClientKeyPath() != null ? certConfig.getClientKeyPath() : "");
            pfxField.setText(certConfig.getPfxPath() != null ? certConfig.getPfxPath() : "");
            passphraseField.setText(certConfig.getPassphrase() != null ? certConfig.getPassphrase() : "");
        } else {
            certEnabledCheckbox.setSelected(false);
            certTypeSelector.setSelectedItem(CertificateConfig.CertificateType.PEM);
            caCertField.setText("");
            clientCertField.setText("");
            clientKeyField.setText("");
            pfxField.setText("");
            passphraseField.setText("");
        }
        updateCertificatePanel();
    }

    /**
     * Updates an APIRequest with the values from this panel.
     */
    public void updateRequest(APIRequest request) {
        request.setSslVerificationEnabled(sslVerificationCheckbox.isSelected());
        request.setFollowRedirects(followRedirectsCheckbox.isSelected());
        request.setTimeout((int) timeoutSpinner.getValue());
        
        // Update certificate config
        CertificateConfig certConfig = request.getCertificateConfig();
        if (certConfig == null) {
            certConfig = new CertificateConfig();
            request.setCertificateConfig(certConfig);
        }
        
        certConfig.setEnabled(certEnabledCheckbox.isSelected());
        certConfig.setCertificateType((CertificateConfig.CertificateType) certTypeSelector.getSelectedItem());
        certConfig.setCaCertPath(caCertField.getText().trim());
        certConfig.setClientCertPath(clientCertField.getText().trim());
        certConfig.setClientKeyPath(clientKeyField.getText().trim());
        certConfig.setPfxPath(pfxField.getText().trim());
        certConfig.setPassphrase(new String(passphraseField.getPassword()));
    }

    /**
     * Refresh theme colors.
     */
    public void refreshThemeColors() {
        setBackground(UIManager.getColor("Panel.background"));
        for (Component c : getComponents()) {
            if (c instanceof JPanel) {
                c.setBackground(UIManager.getColor("Panel.background"));
                refreshChildColors((Container) c);
            }
        }
        repaint();
    }

    private void refreshChildColors(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JPanel) {
                c.setBackground(UIManager.getColor("Panel.background"));
            }
            if (c instanceof JCheckBox) {
                c.setBackground(UIManager.getColor("Panel.background"));
            }
            if (c instanceof JLabel && "sslNoteLabel".equals(c.getName())) {
                c.setForeground(UIManager.getColor("Label.disabledForeground"));
            }
            if (c instanceof JTextField) {
                c.setBackground(UIManager.getColor("TextField.background"));
                c.setForeground(UIManager.getColor("TextField.foreground"));
            }
            if (c instanceof JPasswordField) {
                c.setBackground(UIManager.getColor("TextField.background"));
                c.setForeground(UIManager.getColor("TextField.foreground"));
            }
            if (c instanceof JComboBox) {
                c.setBackground(UIManager.getColor("ComboBox.background"));
            }
            if (c instanceof Container) {
                refreshChildColors((Container) c);
            }
        }
    }
}
