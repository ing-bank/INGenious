package com.ing.ide.main.mainui.components.apitester.request;

import com.ing.datalib.api.*;
import com.ing.ide.main.mainui.components.apitester.APITesterUI;
import com.ing.ide.main.mainui.components.apitester.util.APITesterColors;

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
    private JTextArea bodyTextArea;
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
        
        topPanel.add(new JLabel("Type:"));
        topPanel.add(typeSelector);
        topPanel.add(new JLabel("Format:"));
        topPanel.add(formatSelector);
        
        // Content panel with card layout
        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        
        // Raw body editor
        bodyTextArea = new JTextArea();
        bodyTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        bodyTextArea.setTabSize(2);
        JScrollPane textScroll = new JScrollPane(bodyTextArea);
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
