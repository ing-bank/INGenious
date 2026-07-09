package com.ing.ide.main.mainui.components.apitester.request;

import com.ing.datalib.api.*;
import com.ing.ide.main.mainui.components.apitester.APITesterUI;
import com.ing.ide.main.mainui.components.apitester.util.APITesterColors;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rtextarea.RTextScrollPane;

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
    private ProxyPanel proxyPanel;

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

        // Proxy tab
        proxyPanel = new ProxyPanel();
        tabPane.addTab("Proxy", proxyPanel);

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
        urlField.putClientProperty(
            "JTextField.placeholderText",
            "Enter request URL or paste a curl command"
        );
        urlField.setPreferredSize(new Dimension(100, 36));
        installCurlPasteHandler(urlField);

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

        // Convert to Automation button
        JButton convertButton = new JButton("⇢ Automation");
        convertButton.setFont(convertButton.getFont().deriveFont(11f));
        convertButton.setPreferredSize(new Dimension(110, 36));
        convertButton.setToolTipText("Convert to INGenious Test Case or User Intent (Reusable)");
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

        // Proxy
        proxyPanel.loadProxy(request.getProxyConfig());
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

        // Proxy
        proxyPanel.updateRequest(request);
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

        // Get available scenarios from both Test Plan and Reusable Components
        final java.util.List<com.ing.datalib.component.Scenario> testPlanScenarios = parent
            .getApiTester()
            .getAvailableScenarios();
        final java.util.List<com.ing.datalib.component.Scenario> reusableScenarios = parent
            .getApiTester()
            .getAvailableReusableScenarios();

        if (testPlanScenarios.isEmpty() && reusableScenarios.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(
                this,
                "No scenarios available. Please open a project and create a scenario first.",
                "No Scenarios",
                javax.swing.JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        // Automation target options
        final String TYPE_TEST_CASE = "Test Case";
        final String TYPE_USER_INTENT = "User Intent (Reusable)";

        // Create dialog
        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridLayout(4, 2, 10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));

        panel.add(new javax.swing.JLabel("Automation Type:"));
        javax.swing.JComboBox<String> typeCombo = new javax.swing.JComboBox<>(
            new String[] { TYPE_TEST_CASE, TYPE_USER_INTENT }
        );
        panel.add(typeCombo);

        final javax.swing.JLabel scenarioLabel = new javax.swing.JLabel("Target Scenario:");
        panel.add(scenarioLabel);
        final javax.swing.JComboBox<com.ing.datalib.component.Scenario> scenarioCombo = new javax.swing.JComboBox<>();
        scenarioCombo.setRenderer(
            new javax.swing.DefaultListCellRenderer() {

                @Override
                public java.awt.Component getListCellRendererComponent(
                    javax.swing.JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
                ) {
                    super.getListCellRendererComponent(
                        list,
                        value,
                        index,
                        isSelected,
                        cellHasFocus
                    );
                    if (value instanceof com.ing.datalib.component.Scenario) {
                        setText(((com.ing.datalib.component.Scenario) value).getName());
                    }
                    return this;
                }
            }
        );
        panel.add(scenarioCombo);

        final javax.swing.JLabel nameLabel = new javax.swing.JLabel("Test Case Name:");
        panel.add(nameLabel);
        String defaultName = request.getName() != null
            ? request.getName()
            : request.getMethod() + "_" + extractPathName(request.getUrl());
        final javax.swing.JTextField nameField = new javax.swing.JTextField(defaultName);
        panel.add(nameField);

        panel.add(new javax.swing.JLabel(""));
        javax.swing.JLabel infoLabel = new javax.swing.JLabel(
            "<html><small>Creates test steps using Webservice actions</small></html>"
        );
        infoLabel.setForeground(APITesterColors.textSecondary());
        panel.add(infoLabel);

        // Populate the scenario combo (and name label) based on the selected type
        typeCombo.addActionListener(
            e -> {
                boolean reusable = TYPE_USER_INTENT.equals(typeCombo.getSelectedItem());
                java.util.List<com.ing.datalib.component.Scenario> list = reusable
                    ? reusableScenarios
                    : testPlanScenarios;
                scenarioCombo.setModel(
                    new javax.swing.DefaultComboBoxModel<>(
                        list.toArray(new com.ing.datalib.component.Scenario[0])
                    )
                );
                nameLabel.setText(reusable ? "User Intent Name:" : "Test Case Name:");
            }
        );
        // Initialise for the default selection (Test Case)
        scenarioCombo.setModel(
            new javax.swing.DefaultComboBoxModel<>(
                testPlanScenarios.toArray(new com.ing.datalib.component.Scenario[0])
            )
        );

        int result = javax.swing.JOptionPane.showConfirmDialog(
            this,
            panel,
            "Convert to Automation",
            javax.swing.JOptionPane.OK_CANCEL_OPTION,
            javax.swing.JOptionPane.PLAIN_MESSAGE
        );

        if (result == javax.swing.JOptionPane.OK_OPTION) {
            boolean reusable = TYPE_USER_INTENT.equals(typeCombo.getSelectedItem());
            com.ing.datalib.component.Scenario selectedScenario = (com.ing.datalib.component.Scenario) scenarioCombo.getSelectedItem();

            // Validate scenario is selected
            if (selectedScenario == null) {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Please select a scenario.",
                    "No Scenario Selected",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            String testCaseName = nameField.getText().trim();

            if (testCaseName.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Please enter a " + (reusable ? "user intent" : "test case") + " name.",
                    "Invalid Name",
                    javax.swing.JOptionPane.WARNING_MESSAGE
                );
                return;
            }

            // User Intent (Reusable) path: create a reusable component and finish
            if (reusable) {
                com.ing.datalib.component.TestCase reusableCase = parent
                    .getApiTester()
                    .convertRequestToReusable(request, selectedScenario, testCaseName);

                if (reusableCase != null) {
                    int navigateResult = javax.swing.JOptionPane.showConfirmDialog(
                        this,
                        "Successfully created user intent '" +
                        testCaseName +
                        "' in reusable scenario '" +
                        selectedScenario.getName() +
                        "'.\n\nWould you like to open it in Test Design?",
                        "Conversion Successful",
                        javax.swing.JOptionPane.YES_NO_OPTION,
                        javax.swing.JOptionPane.INFORMATION_MESSAGE
                    );

                    if (navigateResult == javax.swing.JOptionPane.YES_OPTION) {
                        parent.getApiTester().navigateToTestCase(reusableCase);
                    }
                } else {
                    javax.swing.JOptionPane.showMessageDialog(
                        this,
                        "Failed to convert request to user intent. Check the logs for details.",
                        "Conversion Failed",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                    );
                }
                return;
            }
            // Resolve environment variables for conversion only.
            // Do not mutate the original request; collections should keep {{var}} placeholders.
            APIEnvironment activeEnvironment = parent.getApiTester().getActiveEnvironment();
            APIRequest requestForConversion = createResolvedRequestForConversion(
                request,
                activeEnvironment
            );

            // Proxy handling: if the request uses a proxy, ask where to persist the details
            String proxyConfigAlias = null;
            ProxyConfig proxyConfig = requestForConversion.getProxyConfig();
            if (proxyConfig != null && proxyConfig.hasValidConfig()) {
                Object[] options = { "Default API Config", "New API Config", "Cancel" };
                int proxyChoice = javax.swing.JOptionPane.showOptionDialog(
                    this,
                    "This request uses a proxy.\nWhere would you like to save the proxy details?",
                    "Save Proxy Details",
                    javax.swing.JOptionPane.YES_NO_CANCEL_OPTION,
                    javax.swing.JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
                );

                if (proxyChoice == javax.swing.JOptionPane.CLOSED_OPTION || proxyChoice == 2) {
                    return; // user cancelled
                }

                if (proxyChoice == 0) {
                    // Save into the default API config
                    proxyConfigAlias = "default";
                } else {
                    // Create a new API config — prompt for an alias name
                    String alias = javax.swing.JOptionPane.showInputDialog(
                        this,
                        "Enter a name/alias for the new API config:",
                        "New API Config",
                        javax.swing.JOptionPane.QUESTION_MESSAGE
                    );
                    if (alias == null || alias.trim().isEmpty()) {
                        return; // user cancelled
                    }
                    proxyConfigAlias = alias.trim();
                }

                // Persist the proxy details into the chosen API config
                boolean saved = parent
                    .getApiTester()
                    .saveProxyToApiConfig(proxyConfig, proxyConfigAlias);
                if (!saved) {
                    javax.swing.JOptionPane.showMessageDialog(
                        this,
                        "Failed to save proxy details to the API config. Check the logs for details.",
                        "Proxy Save Failed",
                        javax.swing.JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
            }

            // Perform conversion
            com.ing.datalib.component.TestCase testCase = parent
                .getApiTester()
                .convertRequestToTestCase(
                    requestForConversion,
                    selectedScenario,
                    testCaseName,
                    proxyConfigAlias
                );

            if (testCase != null) {
                // Ask user if they want to navigate to Test Design
                int navigateResult = javax.swing.JOptionPane.showConfirmDialog(
                    this,
                    "Successfully created test case '" +
                    testCaseName +
                    "' in scenario '" +
                    selectedScenario.getName() +
                    "'.\n\nWould you like to open it in Test Design?",
                    "Conversion Successful",
                    javax.swing.JOptionPane.YES_NO_OPTION,
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                );

                if (navigateResult == javax.swing.JOptionPane.YES_OPTION) {
                    parent.getApiTester().navigateToTestCase(testCase);
                }
            } else {
                javax.swing.JOptionPane.showMessageDialog(
                    this,
                    "Failed to convert request to test case. Check the logs for details.",
                    "Conversion Failed",
                    javax.swing.JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    /**
     * Creates a resolved copy of the request for test-case conversion.
     *
     * Important: this does not mutate the original API request. The API Tester should
     * keep {{variable}} placeholders in the saved request, but the generated test case
     * should receive concrete values from the active environment.
     */
    private APIRequest createResolvedRequestForConversion(
        APIRequest source,
        APIEnvironment environment
    ) {
        if (source == null || environment == null) {
            return source;
        }

        APIRequest resolved = new APIRequest();

        resolved.setId(source.getId());
        resolved.setName(source.getName());
        resolved.setMethod(source.getMethod());
        resolved.setUrl(resolveValue(source.getUrl(), environment));

        resolved.setQueryParams(resolveKeyValuePairs(source.getQueryParams(), environment));
        resolved.setHeaders(resolveKeyValuePairs(source.getHeaders(), environment));
        resolved.setBody(resolveBody(source.getBody(), environment));
        resolved.setAuth(resolveAuth(source.getAuth(), environment));

        resolved.setFollowRedirects(source.isFollowRedirects());
        resolved.setSslVerificationEnabled(source.isSslVerificationEnabled());
        resolved.setTimeout(source.getTimeout());

        resolved.setProxyConfig(resolveProxyConfig(source.getProxyConfig(), environment));
        resolved.setCertificateConfig(
            resolveCertificateConfig(source.getCertificateConfig(), environment)
        );

        return resolved;
    }

    private String resolveValue(String value, APIEnvironment environment) {
        if (value == null || environment == null) {
            return value;
        }
        return environment.resolve(value);
    }

    private List<KeyValuePair> resolveKeyValuePairs(
        List<KeyValuePair> pairs,
        APIEnvironment environment
    ) {
        if (pairs == null) {
            return null;
        }

        List<KeyValuePair> resolvedPairs = new ArrayList<>();

        for (KeyValuePair pair : pairs) {
            if (pair == null) {
                continue;
            }

            resolvedPairs.add(
                new KeyValuePair(
                    resolveValue(pair.getKey(), environment),
                    resolveValue(pair.getValue(), environment),
                    pair.isEnabled()
                )
            );
        }

        return resolvedPairs;
    }

    private RequestBody resolveBody(RequestBody body, APIEnvironment environment) {
        if (body == null) {
            return null;
        }

        RequestBody resolvedBody = new RequestBody();
        resolvedBody.setBodyType(body.getBodyType());
        resolvedBody.setRawFormat(body.getRawFormat());
        resolvedBody.setRawContent(resolveValue(body.getRawContent(), environment));

        return resolvedBody;
    }

    private AuthConfig resolveAuth(AuthConfig auth, APIEnvironment environment) {
        if (auth == null) {
            return null;
        }

        AuthConfig resolvedAuth = new AuthConfig();
        resolvedAuth.setAuthType(auth.getAuthType());

        resolvedAuth.setBasicUsername(resolveValue(auth.getBasicUsername(), environment));
        resolvedAuth.setBasicPassword(resolveValue(auth.getBasicPassword(), environment));

        resolvedAuth.setBearerToken(resolveValue(auth.getBearerToken(), environment));
        resolvedAuth.setBearerPrefix(resolveValue(auth.getBearerPrefix(), environment));

        resolvedAuth.setApiKeyName(resolveValue(auth.getApiKeyName(), environment));
        resolvedAuth.setApiKeyValue(resolveValue(auth.getApiKeyValue(), environment));
        resolvedAuth.setApiKeyLocation(auth.getApiKeyLocation());

        return resolvedAuth;
    }

    private ProxyConfig resolveProxyConfig(ProxyConfig proxyConfig, APIEnvironment environment) {
        if (proxyConfig == null) {
            return null;
        }

        ProxyConfig resolvedProxyConfig = new ProxyConfig();
        resolvedProxyConfig.setEnabled(proxyConfig.isEnabled());
        resolvedProxyConfig.setHost(resolveValue(proxyConfig.getHost(), environment));
        resolvedProxyConfig.setPort(resolveValue(proxyConfig.getPort(), environment));

        return resolvedProxyConfig;
    }

    private CertificateConfig resolveCertificateConfig(
        CertificateConfig certificateConfig,
        APIEnvironment environment
    ) {
        if (certificateConfig == null) {
            return null;
        }

        CertificateConfig resolvedCertificateConfig = new CertificateConfig();

        resolvedCertificateConfig.setEnabled(certificateConfig.isEnabled());
        resolvedCertificateConfig.setCertificateType(certificateConfig.getCertificateType());

        resolvedCertificateConfig.setCaCertPath(
            resolveValue(certificateConfig.getCaCertPath(), environment)
        );
        resolvedCertificateConfig.setClientCertPath(
            resolveValue(certificateConfig.getClientCertPath(), environment)
        );
        resolvedCertificateConfig.setClientKeyPath(
            resolveValue(certificateConfig.getClientKeyPath(), environment)
        );
        resolvedCertificateConfig.setPfxPath(
            resolveValue(certificateConfig.getPfxPath(), environment)
        );
        resolvedCertificateConfig.setPassphrase(
            resolveValue(certificateConfig.getPassphrase(), environment)
        );

        return resolvedCertificateConfig;
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
            return uri.getHost() != null
                ? uri.getHost().replaceAll("[^a-zA-Z0-9]", "_")
                : "Request";
        } catch (Exception e) {
            return "Request";
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Curl paste support
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Installs a {@link TransferHandler} on the URL field that intercepts paste
     * (and drag-and-drop) of text that looks like a {@code curl} command. When
     * detected, the entire request is rebuilt from the curl command — mirroring
     * the behaviour of Postman's URL bar. Non-curl text falls through to the
     * default text-field paste behaviour.
     */
    private void installCurlPasteHandler(JTextField field) {
        final TransferHandler delegate = field.getTransferHandler();
        field.setTransferHandler(
            new TransferHandler() {

                @Override
                public boolean canImport(TransferSupport support) {
                    return (
                        support.isDataFlavorSupported(
                            java.awt.datatransfer.DataFlavor.stringFlavor
                        ) ||
                        (delegate != null && delegate.canImport(support))
                    );
                }

                @Override
                public boolean importData(TransferSupport support) {
                    if (
                        support.isDataFlavorSupported(java.awt.datatransfer.DataFlavor.stringFlavor)
                    ) {
                        try {
                            String text = (String) support
                                .getTransferable()
                                .getTransferData(java.awt.datatransfer.DataFlavor.stringFlavor);
                            if (com.ing.datalib.api.CurlParser.looksLikeCurl(text)) {
                                applyCurlCommand(text);
                                return true;
                            }
                        } catch (Exception ignore) {
                            // Fall through to default handler below.
                        }
                    }
                    return delegate != null && delegate.importData(support);
                }

                @Override
                public int getSourceActions(JComponent c) {
                    return delegate != null ? delegate.getSourceActions(c) : COPY;
                }
            }
        );
    }

    /**
     * Parses {@code curlCommand} and applies the resulting request to the form
     * and the underlying current request model.
     */
    private void applyCurlCommand(String curlCommand) {
        try {
            APIRequest parsed = com.ing.datalib.api.CurlParser.parse(curlCommand);
            APIRequest current = parent.getCurrentRequest();
            if (current != null) {
                // Preserve identity/name so the user doesn't lose their saved request entry.
                parsed.setId(current.getId());
                if (current.getName() != null && !current.getName().isEmpty()) {
                    parsed.setName(current.getName());
                }
                copyRequestFields(parsed, current);
            }
            loadRequest(parsed);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Could not parse curl command: " + ex.getMessage(),
                "Invalid curl",
                JOptionPane.WARNING_MESSAGE
            );
        }
    }

    /**
     * Copies parsed fields onto the existing current-request instance so any
     * listeners holding the original reference observe the change.
     */
    private void copyRequestFields(APIRequest src, APIRequest dst) {
        dst.setMethod(src.getMethod());
        dst.setUrl(src.getUrl());
        dst.setHeaders(src.getHeaders());
        dst.setQueryParams(src.getQueryParams());
        dst.setBody(src.getBody());
        dst.setAuth(src.getAuth());
        dst.setFollowRedirects(src.isFollowRedirects());
        dst.setSslVerificationEnabled(src.isSslVerificationEnabled());
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

        // Refresh proxy panel colors
        if (proxyPanel != null) {
            proxyPanel.refreshThemeColors();
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
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
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
                case GET:
                    return APITesterColors.methodGet();
                case POST:
                    return APITesterColors.methodPost();
                case PUT:
                    return APITesterColors.methodPut();
                case PATCH:
                    return APITesterColors.methodPatch();
                case DELETE:
                    return APITesterColors.methodDelete();
                default:
                    return APITesterColors.statusNeutral();
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
        tableModel =
            new DefaultTableModel(new String[] { "✓", keyHeader, valueHeader }, 0) {

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
        removeBtn.addActionListener(
            e -> {
                int row = table.getSelectedRow();
                if (row >= 0) {
                    tableModel.removeRow(row);
                }
            }
        );

        buttonPanel.add(addBtn);
        buttonPanel.add(removeBtn);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))
        );

        add(scroll, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    public void addRow(String key, String value, boolean enabled) {
        tableModel.addRow(new Object[] { enabled, key, value });
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
        textScroll.setBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor"))
        );
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
    private JToggleButton basicPasswordVisibilityToggle;
    private char basicPasswordDefaultEchoChar;

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

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Username:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        basicUsername = new JTextField(30);
        panel.add(basicUsername, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Password:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        basicPassword = new JPasswordField(30);
        basicPasswordDefaultEchoChar = basicPassword.getEchoChar();

        basicPasswordVisibilityToggle = new JToggleButton("Show");
        basicPasswordVisibilityToggle.setFont(
            basicPasswordVisibilityToggle.getFont().deriveFont(11f)
        );
        basicPasswordVisibilityToggle.setFocusPainted(false);
        basicPasswordVisibilityToggle.setToolTipText("Show or hide password");
        basicPasswordVisibilityToggle.addActionListener(e -> updateBasicPasswordVisibility());

        JPanel passwordPanel = new JPanel(new BorderLayout(5, 0));
        passwordPanel.setOpaque(false);
        passwordPanel.add(basicPassword, BorderLayout.CENTER);
        passwordPanel.add(basicPasswordVisibilityToggle, BorderLayout.EAST);

        panel.add(passwordPanel, gbc);

        return panel;
    }

    private JPanel createBearerPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Token:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        bearerToken = new JTextField(40);
        panel.add(bearerToken, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Prefix:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        bearerPrefix = new JTextField("Bearer", 20);
        panel.add(bearerPrefix, gbc);

        return panel;
    }

    private JPanel createApiKeyPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Key Name:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        apiKeyName = new JTextField(30);
        panel.add(apiKeyName, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Key Value:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        apiKeyValue = new JTextField(30);
        panel.add(apiKeyValue, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(new JLabel("Add to:"), gbc);
        gbc.gridx = 1;
        apiKeyLocation = new JComboBox<>(AuthConfig.ApiKeyLocation.values());
        panel.add(apiKeyLocation, gbc);

        return panel;
    }

    private void updateBasicPasswordVisibility() {
        if (basicPassword == null || basicPasswordVisibilityToggle == null) {
            return;
        }

        boolean showPassword = basicPasswordVisibilityToggle.isSelected();

        basicPassword.setEchoChar(showPassword ? (char) 0 : basicPasswordDefaultEchoChar);
        basicPasswordVisibilityToggle.setText(showPassword ? "Hide" : "Show");
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
        if (basicPasswordVisibilityToggle != null) {
            basicPasswordVisibilityToggle.setSelected(false);
            updateBasicPasswordVisibility();
        }
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
        if (basicPasswordVisibilityToggle != null) {
            basicPasswordVisibilityToggle.setBackground(UIManager.getColor("Button.background"));
            basicPasswordVisibilityToggle.setForeground(UIManager.getColor("Button.foreground"));
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
                refreshAuthChildColors((Container) c);
            }
        }

        repaint();
    }

    private void refreshAuthChildColors(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JPanel) {
                c.setBackground(UIManager.getColor("Panel.background"));
            }
            if (c instanceof JTextField) {
                c.setBackground(UIManager.getColor("TextField.background"));
                c.setForeground(UIManager.getColor("TextField.foreground"));
            }
            if (c instanceof JPasswordField) {
                c.setBackground(UIManager.getColor("TextField.background"));
                c.setForeground(UIManager.getColor("TextField.foreground"));
            }
            if (c instanceof AbstractButton) {
                c.setBackground(UIManager.getColor("Button.background"));
                c.setForeground(UIManager.getColor("Button.foreground"));
            }
            if (c instanceof JComboBox) {
                c.setBackground(UIManager.getColor("ComboBox.background"));
            }
            if (c instanceof Container) {
                refreshAuthChildColors((Container) c);
            }
        }
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
        JScrollPane scrollFrame = new JScrollPane(settingsGrid);

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
            "When disabled, requests will skip SSL certificate validation. Use this for self-signed certificates or test environments."
        );
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

        scrollFrame.add(Box.createVerticalGlue(), gbc);
        add(scrollFrame, BorderLayout.CENTER);

        updateCertificatePanel();
    }

    private JPanel createPemPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // CA Certificate
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("CA Certificate:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        caCertField = new JTextField(30);
        caCertField.setToolTipText("Path to CA certificate file (.pem/.crt)");
        panel.add(caCertField, gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton caCertBrowse = new JButton("Browse...");
        caCertBrowse.addActionListener(
            e -> browseCertificateFile(caCertField, "Select CA Certificate")
        );
        panel.add(caCertBrowse, gbc);

        // Client Certificate
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Client Certificate:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        clientCertField = new JTextField(30);
        clientCertField.setToolTipText("Path to client certificate file (.pem/.crt)");
        panel.add(clientCertField, gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton clientCertBrowse = new JButton("Browse...");
        clientCertBrowse.addActionListener(
            e -> browseCertificateFile(clientCertField, "Select Client Certificate")
        );
        panel.add(clientCertBrowse, gbc);

        // Client Private Key
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Client Private Key:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        clientKeyField = new JTextField(30);
        clientKeyField.setToolTipText("Path to client private key file (.key/.pem)");
        panel.add(clientKeyField, gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        JButton clientKeyBrowse = new JButton("Browse...");
        clientKeyBrowse.addActionListener(
            e -> browseCertificateFile(clientKeyField, "Select Client Private Key")
        );
        panel.add(clientKeyBrowse, gbc);

        // Passphrase
        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Passphrase:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
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
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("PFX/PKCS12 File:"), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        pfxField = new JTextField(30);
        pfxField.setToolTipText("Path to PFX/PKCS12 keystore file (.pfx/.p12)");
        panel.add(pfxField, gbc);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
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
            caCertField.setText(
                certConfig.getCaCertPath() != null ? certConfig.getCaCertPath() : ""
            );
            clientCertField.setText(
                certConfig.getClientCertPath() != null ? certConfig.getClientCertPath() : ""
            );
            clientKeyField.setText(
                certConfig.getClientKeyPath() != null ? certConfig.getClientKeyPath() : ""
            );
            pfxField.setText(certConfig.getPfxPath() != null ? certConfig.getPfxPath() : "");
            passphraseField.setText(
                certConfig.getPassphrase() != null ? certConfig.getPassphrase() : ""
            );
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
        certConfig.setCertificateType(
            (CertificateConfig.CertificateType) certTypeSelector.getSelectedItem()
        );
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

/**
 * Panel for configuring an HTTP proxy for the request.
 * The host and port fields are enabled only when "Use Proxy" is checked.
 */
class ProxyPanel extends JPanel {
    private JCheckBox useProxyCheckbox;
    private JTextField hostField;
    private JTextField portField;
    private JLabel hostLabel;
    private JLabel portLabel;

    public ProxyPanel() {
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(12, 12, 12, 12));
        initComponents();
    }

    private void initComponents() {
        JPanel grid = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Section header
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        JLabel header = new JLabel("Proxy");
        header.setFont(header.getFont().deriveFont(Font.BOLD, 13f));
        grid.add(header, gbc);

        // Use Proxy checkbox
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        useProxyCheckbox = new JCheckBox("Use Proxy");
        useProxyCheckbox.setFont(useProxyCheckbox.getFont().deriveFont(12f));
        useProxyCheckbox.addActionListener(e -> updateFieldsEnabled());
        grid.add(useProxyCheckbox, gbc);

        // Host
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        hostLabel = new JLabel("Host:");
        hostLabel.setFont(hostLabel.getFont().deriveFont(12f));
        grid.add(hostLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        hostField = new JTextField(30);
        hostField.setToolTipText("Proxy host (e.g., proxy.example.com or 127.0.0.1)");
        grid.add(hostField, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;

        // Port
        gbc.gridx = 0;
        gbc.gridy = 3;
        portLabel = new JLabel("Port:");
        portLabel.setFont(portLabel.getFont().deriveFont(12f));
        grid.add(portLabel, gbc);

        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        portField = new JTextField(10);
        portField.setToolTipText("Proxy port (e.g., 8080)");
        grid.add(portField, gbc);
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;

        // Note
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        JLabel note = new JLabel(
            "When enabled, the request is routed through the configured proxy."
        );
        note.setFont(note.getFont().deriveFont(Font.ITALIC, 11f));
        note.setForeground(UIManager.getColor("Label.disabledForeground"));
        note.setName("proxyNoteLabel");
        grid.add(note, gbc);

        // Push to top
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.VERTICAL;
        grid.add(Box.createVerticalGlue(), gbc);

        add(grid, BorderLayout.CENTER);
        updateFieldsEnabled();
    }

    private void updateFieldsEnabled() {
        boolean enabled = useProxyCheckbox.isSelected();
        hostField.setEnabled(enabled);
        portField.setEnabled(enabled);
        hostLabel.setEnabled(enabled);
        portLabel.setEnabled(enabled);
    }

    /**
     * Loads proxy settings from a ProxyConfig into this panel.
     */
    public void loadProxy(ProxyConfig config) {
        if (config == null) {
            useProxyCheckbox.setSelected(false);
            hostField.setText("");
            portField.setText("");
        } else {
            useProxyCheckbox.setSelected(config.isEnabled());
            hostField.setText(config.getHost() != null ? config.getHost() : "");
            portField.setText(config.getPort() != null ? config.getPort() : "");
        }
        updateFieldsEnabled();
    }

    /**
     * Updates an APIRequest with the values from this panel.
     */
    public void updateRequest(APIRequest request) {
        ProxyConfig config = request.getProxyConfig();
        if (config == null) {
            config = new ProxyConfig();
            request.setProxyConfig(config);
        }
        config.setEnabled(useProxyCheckbox.isSelected());
        config.setHost(hostField.getText().trim());
        config.setPort(portField.getText().trim());
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
            if (c instanceof JLabel && "proxyNoteLabel".equals(c.getName())) {
                c.setForeground(UIManager.getColor("Label.disabledForeground"));
            }
            if (c instanceof JTextField) {
                c.setBackground(UIManager.getColor("TextField.background"));
                c.setForeground(UIManager.getColor("TextField.foreground"));
            }
            if (c instanceof Container) {
                refreshChildColors((Container) c);
            }
        }
    }
}
