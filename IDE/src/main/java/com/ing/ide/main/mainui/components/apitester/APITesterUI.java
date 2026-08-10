package com.ing.ide.main.mainui.components.apitester;

import com.ing.datalib.api.*;
import com.ing.ide.main.fx.INGIcons;
import com.ing.ide.main.mainui.components.apitester.collections.CollectionTree;
import com.ing.ide.main.mainui.components.apitester.request.RequestPanel;
import com.ing.ide.main.mainui.components.apitester.response.ResponsePanel;
import com.ing.ide.main.mainui.components.apitester.util.APITesterColors;
import com.ing.ide.util.Notification;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Main UI panel for the API Tester feature.
 * Layout: Left panel (collections/history) | Right panel (request builder + response viewer)
 */
public class APITesterUI extends JPanel implements PropertyChangeListener {
    private final APITester apiTester;

    // Left panel components
    private CollectionTree collectionTree;
    private JList<APIRequest> historyList;
    private DefaultListModel<APIRequest> historyModel;
    private JButton environmentSelectorButton;
    private JPopupMenu environmentPopup;
    private boolean updatingEnvironmentSelector;

    // Right panel components
    private RequestPanel requestPanel;
    private ResponsePanel responsePanel;
    private JLabel editingHeaderLabel; // Shows what collection/request is being edited

    // Current state
    private APIRequest currentRequest;
    private APIRequest sourceRequest; // Tracks the original request loaded from collection
    private APICollection sourceCollection; // Tracks which collection the request came from
    private APICollection sourceFolder; // Tracks which folder the request came from (null if in collection root)
    private boolean sourceHistory; // Tracks if request came from history

    private static final Color ENVIRONMENT_PURPLE = new Color(0x6E40C9);

    public APITesterUI(APITester apiTester) {
        this.apiTester = apiTester;
        this.currentRequest = new APIRequest();
        this.sourceRequest = null;
        this.sourceCollection = null;
        this.sourceFolder = null;
        this.sourceHistory = false;
        initComponents();

        // Listen for L&F changes to refresh colors
        UIManager.addPropertyChangeListener(this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if ("lookAndFeel".equals(evt.getPropertyName())) {
            // Theme changed - refresh all colors on EDT
            SwingUtilities.invokeLater(this::refreshAllColors);
        }
    }

    /**
     * Refresh all colors in the API Tester when theme changes.
     */
    public void refreshAllColors() {
        // Refresh this panel using UIManager color
        setBackground(UIManager.getColor("Panel.background"));

        // Refresh child panels with their specific theme colors
        if (requestPanel != null) {
            requestPanel.refreshThemeColors();
        }
        if (responsePanel != null) {
            responsePanel.refreshThemeColors();
        }
        if (collectionTree != null) {
            collectionTree.refreshThemeColors();
        }

        // Recursively refresh all child panels
        refreshColorsRecursive(this);

        updateEnvironmentSelectorButtonStyle();

        // Force repaint
        revalidate();
        repaint();
    }

    /**
     * Recursively refresh background colors on all child components.
     */
    private void refreshColorsRecursive(Container container) {
        for (Component c : container.getComponents()) {
            // Refresh known panel types
            if (c instanceof JPanel) {
                JPanel panel = (JPanel) c;
                // Only refresh panels that should have themed backgrounds
                if (panel.isOpaque()) {
                    panel.setBackground(UIManager.getColor("Panel.background"));
                }
            }

            // Refresh text areas
            if (c instanceof JTextArea) {
                JTextArea ta = (JTextArea) c;
                ta.setBackground(UIManager.getColor("TextArea.background"));
                ta.setForeground(UIManager.getColor("TextArea.foreground"));
            }

            // Refresh text fields
            if (c instanceof JTextField) {
                JTextField tf = (JTextField) c;
                tf.setBackground(UIManager.getColor("TextField.background"));
                tf.setForeground(UIManager.getColor("TextField.foreground"));
            }

            // Refresh combo boxes
            if (c instanceof JComboBox) {
                JComboBox<?> cb = (JComboBox<?>) c;
                cb.setBackground(UIManager.getColor("ComboBox.background"));
                cb.setForeground(UIManager.getColor("ComboBox.foreground"));
            }

            // Refresh tables
            if (c instanceof JTable) {
                JTable table = (JTable) c;
                table.setBackground(UIManager.getColor("Table.background"));
                table.setForeground(UIManager.getColor("Table.foreground"));
                table.setGridColor(UIManager.getColor("Table.gridColor"));
            }

            // Refresh lists
            if (c instanceof JList) {
                JList<?> list = (JList<?>) c;
                list.setBackground(UIManager.getColor("List.background"));
                list.setForeground(UIManager.getColor("List.foreground"));
            }

            // Refresh scroll panes
            if (c instanceof JScrollPane) {
                JScrollPane sp = (JScrollPane) c;
                sp.getViewport().setBackground(UIManager.getColor("Panel.background"));
            }

            // Refresh tabbed panes
            if (c instanceof JTabbedPane) {
                JTabbedPane tp = (JTabbedPane) c;
                tp.setBackground(UIManager.getColor("TabbedPane.background"));
            }

            // Refresh split panes
            if (c instanceof JSplitPane) {
                JSplitPane sp = (JSplitPane) c;
                sp.setBackground(UIManager.getColor("SplitPane.background"));
            }

            // Refresh labels with secondary color
            if (c instanceof JLabel) {
                JLabel label = (JLabel) c;
                String name = label.getName();
                if (
                    name != null &&
                    (name.contains("None") || name.contains("empty") || name.contains("loading"))
                ) {
                    label.setForeground(APITesterColors.textSecondary());
                }
            }

            // Recurse into containers
            if (c instanceof Container) {
                refreshColorsRecursive((Container) c);
            }
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));
        setBackground(APITesterColors.panelBackground());
        setOpaque(true);

        // Create the main split pane
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainSplit.setDividerLocation(280);
        mainSplit.setDividerSize(4);
        mainSplit.setContinuousLayout(true);

        // Left panel: Collections and History
        JPanel leftPanel = createLeftPanel();
        mainSplit.setLeftComponent(leftPanel);

        // Right panel: Request builder and Response viewer
        JPanel rightPanel = createRightPanel();
        mainSplit.setRightComponent(rightPanel);

        add(mainSplit, BorderLayout.CENTER);
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        panel.setMinimumSize(new Dimension(200, 0));

        // Note: Temporarily disabled until Environment parameter is implemented.
        // Toolbar with environment selector
        JPanel toolbar = createLeftToolbar();
        panel.add(toolbar, BorderLayout.NORTH);

        // Tabbed pane for Collections and History
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.setFont(tabbedPane.getFont().deriveFont(11f));

        // Collections tab
        collectionTree = new CollectionTree(this, apiTester);
        tabbedPane.addTab("Collections", collectionTree);

        // History tab
        historyModel = new DefaultListModel<>();
        historyList = new JList<>(historyModel);
        historyList.setCellRenderer(new HistoryListRenderer());
        historyList.addListSelectionListener(
            e -> {
                if (!e.getValueIsAdjusting()) {
                    APIRequest selected = historyList.getSelectedValue();
                    if (selected != null) {
                        loadRequestFromHistory(selected);
                    }
                }
            }
        );
        JScrollPane historyScroll = new JScrollPane(historyList);
        historyScroll.setBorder(BorderFactory.createEmptyBorder());
        tabbedPane.addTab("History", historyScroll);

        panel.add(tabbedPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createLeftToolbar() {
        JPanel toolbar = new JPanel(new BorderLayout(5, 0));
        toolbar.setBorder(new EmptyBorder(8, 8, 8, 8));
        toolbar.setBackground(APITesterColors.panelBackground());
        toolbar.setName("leftToolbar");

        // Environment selector
        JLabel envLabel = new JLabel("Environment:");
        envLabel.setFont(envLabel.getFont().deriveFont(11f));

        environmentSelectorButton = new JButton();
        environmentSelectorButton.setFont(environmentSelectorButton.getFont().deriveFont(11f));
        environmentSelectorButton.setPreferredSize(new Dimension(170, 28));
        environmentSelectorButton.setHorizontalAlignment(SwingConstants.LEFT);
        environmentSelectorButton.setFocusPainted(false);
        environmentSelectorButton.setText(getEnvironmentSelectorButtonText());
        environmentSelectorButton.addActionListener(e -> showEnvironmentDropdown());
        updateEnvironmentSelectorButtonStyle();

        JPanel leftPart = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftPart.setOpaque(false);
        leftPart.add(envLabel);
        leftPart.add(environmentSelectorButton);

        toolbar.add(leftPart, BorderLayout.CENTER);

        return toolbar;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));

        // Header showing what is being edited
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("Separator.foreground"))
        );
        headerPanel.setBackground(APITesterColors.panelBackground());
        headerPanel.setPreferredSize(new Dimension(0, 35));

        editingHeaderLabel = new JLabel("No request selected");
        editingHeaderLabel.setFont(editingHeaderLabel.getFont().deriveFont(Font.BOLD, 12f));
        editingHeaderLabel.setBorder(new EmptyBorder(8, 12, 8, 12));
        editingHeaderLabel.setForeground(UIManager.getColor("Label.foreground"));
        headerPanel.add(editingHeaderLabel, BorderLayout.WEST);

        // "New Request" button — opens a fresh blank request in the editor without
        // discarding the current one (it is auto-saved if it came from a collection).
        JButton newRequestBtn = new JButton("+ New Request");
        newRequestBtn.setToolTipText("Open a new blank request in the editor");
        newRequestBtn.setFont(newRequestBtn.getFont().deriveFont(11f));
        newRequestBtn.setFocusPainted(false);
        newRequestBtn.addActionListener(e -> newBlankRequest());
        JPanel headerRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        headerRight.setOpaque(false);
        headerRight.setBorder(new EmptyBorder(4, 4, 4, 8));
        headerRight.add(newRequestBtn);
        headerPanel.add(headerRight, BorderLayout.EAST);

        panel.add(headerPanel, BorderLayout.NORTH);

        // Vertical split: Request builder on top, Response viewer on bottom
        JSplitPane verticalSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        verticalSplit.setDividerLocation(350);
        verticalSplit.setDividerSize(4);
        verticalSplit.setContinuousLayout(true);
        verticalSplit.setResizeWeight(0.5);

        // Request panel
        requestPanel = new RequestPanel(this);
        verticalSplit.setTopComponent(requestPanel);

        // Response panel
        responsePanel = new ResponsePanel(this);
        verticalSplit.setBottomComponent(responsePanel);

        panel.add(verticalSplit, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Called when theme changes to refresh colors.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        // Guard against calls during super constructor before fields are initialized
        if (requestPanel == null) return;

        // Refresh panel backgrounds
        setBackground(APITesterColors.panelBackground());

        // Refresh toolbar backgrounds by name
        refreshToolbarColors(this);

        repaint();
    }

    /**
     * Recursively refresh toolbar colors.
     */
    private void refreshToolbarColors(Container container) {
        if (container == null) return;
        for (Component c : container.getComponents()) {
            if ("leftToolbar".equals(c.getName())) {
                c.setBackground(APITesterColors.panelBackground());
            }
            if (c instanceof Container) {
                refreshToolbarColors((Container) c);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════

    public APITester getApiTester() {
        return apiTester;
    }

    public APIRequest getCurrentRequest() {
        return currentRequest;
    }

    /**
     * Auto-saves the current request if it has unsaved changes and came from a collection.
     * Called automatically when opening a different request.
     */
    public void autoSaveCurrentRequest() {
        // Always save the current request when switching to a different request
        // This ensures all changes (URL, method, body, headers, params, auth, etc.) are persisted
        forceSaveCurrentRequest();
    }

    /**
     * Detects if the current request has unsaved changes.
     */
    private boolean hasUnsavedChanges() {
        if (sourceRequest == null) return false;

        // Check if URL changed
        String currentUrl = currentRequest.getUrl() != null ? currentRequest.getUrl() : "";
        String sourceUrl = sourceRequest.getUrl() != null ? sourceRequest.getUrl() : "";
        if (!currentUrl.equals(sourceUrl)) return true;

        // Check if method changed
        if (currentRequest.getMethod() != sourceRequest.getMethod()) return true;

        // Check if body changed - compare the raw content
        if (currentRequest.getBody() != null && sourceRequest.getBody() != null) {
            String currentBodyContent = currentRequest.getBody().getRawContent() != null
                ? currentRequest.getBody().getRawContent()
                : "";
            String sourceBodyContent = sourceRequest.getBody().getRawContent() != null
                ? sourceRequest.getBody().getRawContent()
                : "";
            if (!currentBodyContent.equals(sourceBodyContent)) return true;
        } else if ((currentRequest.getBody() == null) != (sourceRequest.getBody() == null)) {
            // One is null and the other isn't
            return true;
        }

        return false;
    }

    public void notifyRequestDeleted(APIRequest deletedRequest) {
        if (deletedRequest == null || sourceRequest == null) {
            return;
        }

        if (sameRequest(sourceRequest, deletedRequest)) {
            clearSourceTracking();

            currentRequest = new APIRequest();
            requestPanel.loadRequest(currentRequest);
            responsePanel.clear();
            updateEditingHeader();
        }
    }

    private boolean sameRequest(APIRequest a, APIRequest b) {
        if (a == null || b == null) {
            return false;
        }

        String aId = a.getId();
        String bId = b.getId();

        if (aId != null && bId != null) {
            return aId.equals(bId);
        }

        return a == b;
    }

    private void removeRequestFromRoot(APICollection collection, APIRequest requestToRemove) {
        if (collection == null || requestToRemove == null || collection.getRequests() == null) {
            return;
        }

        collection.getRequests().removeIf(request -> sameRequest(request, requestToRemove));
    }

    private void saveRequestToFolder(
        APIRequest request,
        APICollection parentCollection,
        APICollection folder
    ) {
        if (request == null || parentCollection == null || folder == null) {
            return;
        }

        String requestId = request.getId();

        boolean updated = false;

        for (int i = 0; i < folder.getRequests().size(); i++) {
            APIRequest existing = folder.getRequests().get(i);

            if (sameRequest(existing, request)) {
                folder.getRequests().set(i, request);
                updated = true;
                break;
            }
        }

        if (!updated) {
            folder.addRequest(request);
        }

        // Important: remove accidental root-level duplicate
        removeRequestFromRoot(parentCollection, request);

        apiTester.saveCollection(parentCollection);
        refreshCollectionsTree();
    }

    /**
     * Force saves the current request to backend file if it's from a collection.
     * Called by IDE's save/autosave to persist all edited requests to disk.
     * This is public so AppMainFrame can call it during project save.
     */
    public void forceSaveCurrentRequest() {
        if (sourceCollection != null && sourceRequest != null && currentRequest != null) {
            // Do not resurrect a request that was deleted from its collection/folder.
            if (!sourceRequestStillExists()) {
                clearSourceTracking();
                return;
            }

            // Update the request with current UI values
            requestPanel.updateRequest(currentRequest);

            // Save to the correct location
            if (sourceFolder != null) {
                saveRequestToFolder(currentRequest, sourceCollection, sourceFolder);
            } else {
                apiTester.saveRequestToCollection(currentRequest, sourceCollection);
            }
        }
    }

    private boolean sourceRequestStillExists() {
        if (sourceCollection == null || sourceRequest == null) {
            return false;
        }

        if (sourceFolder != null) {
            return containsRequest(sourceFolder.getRequests(), sourceRequest);
        }

        return containsRequest(sourceCollection.getRequests(), sourceRequest);
    }

    private boolean containsRequest(List<APIRequest> requests, APIRequest target) {
        if (requests == null || target == null) {
            return false;
        }

        String targetId = target.getId();

        for (APIRequest request : requests) {
            if (request == null) {
                continue;
            }

            String requestId = request.getId();

            if (targetId != null && requestId != null) {
                if (targetId.equals(requestId)) {
                    return true;
                }
            } else if (request == target) {
                return true;
            }
        }

        return false;
    }

    private void clearSourceTracking() {
        this.sourceRequest = null;
        this.sourceCollection = null;
        this.sourceFolder = null;
        this.sourceHistory = false;
    }

    public void setCurrentRequest(APIRequest request) {
        this.currentRequest = request;
        requestPanel.loadRequest(request);
    }

    /**
     * Opens a fresh, blank request in the editor. Any current request that came
     * from a collection is auto-saved first, so no work is lost. After this call
     * the editor is on a brand-new {@link APIRequest} that is not yet attached to
     * any collection — the user can either save it via the Save button (which
     * will prompt for a target collection) or discard it by loading another.
     */
    public void newBlankRequest() {
        loadRequest(new APIRequest());
        requestPanel.focusUrl();
    }

    /**
     * Loads a request into the editor.
     */
    public void loadRequest(APIRequest request) {
        autoSaveCurrentRequest(); // Auto-save before loading new request
        this.currentRequest = request;
        this.sourceRequest = null; // Not from collection
        this.sourceCollection = null;
        this.sourceFolder = null; // Not from folder
        this.sourceHistory = false; // Not from history
        requestPanel.loadRequest(request);
        responsePanel.clear();
        updateEditingHeader();
    }

    /**
     * Loads a request from a collection into the editor.
     * Tracks the source so Save can update instead of creating new.
     */
    public void loadRequest(APIRequest request, APICollection collection) {
        autoSaveCurrentRequest(); // Auto-save before loading new request
        this.currentRequest = request;
        this.sourceRequest = request; // Remember this came from collection
        this.sourceCollection = collection;
        this.sourceFolder = null; // No folder
        this.sourceHistory = false; // Not from history
        requestPanel.loadRequest(request);
        responsePanel.clear();
        updateEditingHeader();
    }

    /**
     * Loads a request from a folder inside a collection into the editor.
     * Tracks both the collection and folder for proper save behavior.
     */
    public void loadRequest(APIRequest request, APICollection collection, APICollection folder) {
        autoSaveCurrentRequest(); // Auto-save before loading new request
        this.currentRequest = request;
        this.sourceRequest = request; // Remember this came from collection
        this.sourceCollection = collection;
        this.sourceFolder = folder; // Remember which folder
        this.sourceHistory = false; // Not from history
        requestPanel.loadRequest(request);
        responsePanel.clear();
        updateEditingHeader();
    }

    /**
     * Loads a request from history into the editor.
     */
    public void loadRequestFromHistory(APIRequest request) {
        autoSaveCurrentRequest(); // Auto-save before loading new request
        this.currentRequest = request;
        this.sourceRequest = null; // Not from collection
        this.sourceCollection = null;
        this.sourceFolder = null;
        this.sourceHistory = true; // From history
        requestPanel.loadRequest(request);
        responsePanel.clear();
        updateEditingHeader();
    }

    /**
     * Updates the header label to show what is being edited.
     */
    private void updateEditingHeader() {
        if (editingHeaderLabel == null) return;

        String headerText;
        if (sourceCollection != null && sourceRequest != null) {
            // Build path: Collection / [Folder /] Request
            if (sourceFolder != null) {
                headerText =
                    sourceCollection.getName() +
                    " / " +
                    sourceFolder.getName() +
                    " / " +
                    currentRequest.getName();
            } else {
                headerText = sourceCollection.getName() + " / " + currentRequest.getName();
            }
        } else if (currentRequest.getName() != null && !currentRequest.getName().isEmpty()) {
            headerText = "New Request: " + currentRequest.getName();
        } else {
            headerText = "New Request";
        }

        editingHeaderLabel.setText(headerText);
        editingHeaderLabel.setToolTipText(headerText); // Full text in tooltip
    }

    /**
     * Creates a new empty request.
     */
    public void newRequest() {
        this.currentRequest = new APIRequest();
        this.sourceRequest = null; // Not from collection
        this.sourceCollection = null;
        this.sourceFolder = null; // Not from folder
        this.sourceHistory = false; // Not from history
        requestPanel.loadRequest(currentRequest);
        responsePanel.clear();
        updateEditingHeader();
    }

    /**
     * Sends the current request.
     */
    public void sendRequest() {
        // Update request from UI
        requestPanel.updateRequest(currentRequest);

        // Show loading state
        responsePanel.showLoading();

        // Execute request
        apiTester.executeRequest(
            currentRequest,
            new APITester.RequestCallback() {

                @Override
                public void onResponse(APIResponse response) {
                    responsePanel.showResponse(response);
                }

                @Override
                public void onError(Exception error) {
                    responsePanel.showError(error.getMessage());
                }
            }
        );
    }

    /**
     * Saves the current request to a collection.
     *
     * Implements the Save behavior based on state:
     * - If current request came from collection: update it (no prompt)
     * - If current request is new: prompt for collection and name
     */
    public void saveRequest() {
        requestPanel.updateRequest(currentRequest);

        // Scenario: Editing an existing request from a collection
        if (sourceRequest != null && sourceCollection != null) {
            if (sourceFolder != null) {
                saveRequestToFolder(currentRequest, sourceCollection, sourceFolder);
                Notification.show(
                    "Request \"" +
                    currentRequest.getName() +
                    "\" updated in " +
                    sourceCollection.getName() +
                    " / " +
                    sourceFolder.getName()
                );
            } else {
                apiTester.saveRequestToCollection(currentRequest, sourceCollection);
                Notification.show(
                    "Request \"" +
                    currentRequest.getName() +
                    "\" updated in " +
                    sourceCollection.getName()
                );
            }
            return;
        }

        // Scenario: Saving a new request - need to ask for collection and name
        List<APICollection> collections = apiTester.getCollections();
        if (collections.isEmpty()) {
            // Create a default collection
            APICollection defaultCollection = apiTester.createNewCollection("My Collection");
            saveNewRequest(defaultCollection);
        } else {
            // Show collection chooser
            APICollection[] options = collections.toArray(new APICollection[0]);
            APICollection selected = (APICollection) JOptionPane.showInputDialog(
                this,
                "Select collection to save request:",
                "Save Request",
                JOptionPane.PLAIN_MESSAGE,
                null,
                options,
                options[0]
            );
            if (selected != null) {
                saveNewRequest(selected);
            }
        }
    }

    /**
     * Saves a new request to the specified collection.
     * Prompts user for request name.
     */
    private void saveNewRequest(APICollection collection) {
        String name = JOptionPane.showInputDialog(
            this,
            "Request name:",
            currentRequest.getName() != null
                ? currentRequest.getName()
                : currentRequest.getMethod() + " Request"
        );
        if (name != null && !name.trim().isEmpty()) {
            currentRequest.setName(name.trim());
            apiTester.saveRequestToCollection(currentRequest, collection);

            // Update source tracking so subsequent saves don't prompt
            this.sourceRequest = currentRequest;
            this.sourceCollection = collection;

            updateEditingHeader();
            Notification.show("Request saved to " + collection.getName());
        }
    }

    /**
     * Shows the response for a request.
     */
    public void showResponse(APIResponse response) {
        responsePanel.showResponse(response);
    }

    /**
     * Refreshes the collections tree.
     */
    public void refreshCollectionsTree() {
        if (collectionTree != null) {
            collectionTree.refreshTree();
        }
    }

    /**
     * Refreshes the history list.
     */
    public void refreshHistory() {
        if (historyModel != null) {
            historyModel.clear();
            for (APIRequest r : apiTester.getHistory()) {
                historyModel.addElement(r);
            }
        }
    }

    /**
     * Updates the environment selector.
     */
    public void updateEnvironmentSelector() {
        if (environmentSelectorButton != null) {
            updatingEnvironmentSelector = true;
            try {
                environmentSelectorButton.setText(getEnvironmentSelectorButtonText());
                environmentSelectorButton.setToolTipText(getEnvironmentSelectorToolTipText());
                updateEnvironmentSelectorButtonStyle();
            } finally {
                updatingEnvironmentSelector = false;
            }
        }
    }

    /**
     * Refreshes all UI components.
     */
    public void refresh() {
        refreshCollectionsTree();
        refreshHistory();
        updateEnvironmentSelector();
    }

    private void showNewCollectionDialog() {
        String name = JOptionPane.showInputDialog(this, "Collection name:", "New Collection");
        if (name != null && !name.trim().isEmpty()) {
            apiTester.createNewCollection(name.trim());
        }
    }

    private String getEnvironmentSelectorButtonText() {
        APIEnvironment activeEnvironment = apiTester.getActiveEnvironment();

        if (activeEnvironment == null || activeEnvironment.getName() == null) {
            return "No Environment  \u25BE";
        }

        String name = activeEnvironment.getName();

        if (name.length() > 18) {
            name = name.substring(0, 18) + "...";
        }

        return name + "  \u25BE";
    }

    private String getEnvironmentSelectorToolTipText() {
        APIEnvironment activeEnvironment = apiTester.getActiveEnvironment();

        if (activeEnvironment == null || activeEnvironment.getName() == null) {
            return "No Environment";
        }

        return activeEnvironment.getName();
    }

    private void showEnvironmentDropdown() {
        if (environmentSelectorButton == null) {
            return;
        }

        if (environmentPopup != null && environmentPopup.isVisible()) {
            environmentPopup.setVisible(false);
            return;
        }

        environmentPopup = createEnvironmentPopup();

        int width = Math.max(environmentSelectorButton.getWidth(), 260);
        int height = Math.max(240, Math.min(420, 120 + apiTester.getEnvironments().size() * 42));

        environmentPopup.setPreferredSize(new Dimension(width, height));
        environmentPopup.show(environmentSelectorButton, 0, environmentSelectorButton.getHeight());
    }

    private JPopupMenu createEnvironmentPopup() {
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(
            BorderFactory.createLineBorder(
                UIManager.getColor("Separator.foreground") != null
                    ? UIManager.getColor("Separator.foreground")
                    : Color.GRAY
            )
        );

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(APITesterColors.panelBackground());
        root.setBorder(new EmptyBorder(10, 10, 0, 10));

        root.add(createEnvironmentPopupHeader(), BorderLayout.NORTH);
        root.add(createEnvironmentPopupList(), BorderLayout.CENTER);
        root.add(createEnvironmentPopupFooter(popup), BorderLayout.SOUTH);

        popup.add(root);

        return popup;
    }

    private JPanel createEnvironmentPopupHeader() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0, 6, 8, 6));

        JLabel title = new JLabel("Environments");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setForeground(UIManager.getColor("Label.foreground"));

        JPanel underlineWrapper = new JPanel(new BorderLayout());
        underlineWrapper.setOpaque(false);
        underlineWrapper.setBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(0x6E40C9))
        );
        underlineWrapper.add(title, BorderLayout.CENTER);

        headerPanel.add(underlineWrapper, BorderLayout.WEST);

        return headerPanel;
    }

    private JPanel createEnvironmentPopupList() {
        JPanel listPanel = new JPanel();
        listPanel.setOpaque(false);
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(new EmptyBorder(8, 0, 8, 0));

        APIEnvironment activeEnvironment = apiTester.getActiveEnvironment();

        listPanel.add(
            createEnvironmentPopupRow(
                "No Environment",
                activeEnvironment == null,
                true,
                () -> {
                    apiTester.setActiveEnvironment(null);
                    updateEnvironmentSelector();
                }
            )
        );

        for (APIEnvironment environment : apiTester.getEnvironments()) {
            if (environment == null) {
                continue;
            }

            String environmentName = environment.getName();

            if (environmentName == null || environmentName.trim().isEmpty()) {
                environmentName = "Unnamed Environment";
            }

            APIEnvironment envToSelect = environment;
            String rowText = environmentName;

            listPanel.add(
                createEnvironmentPopupRow(
                    rowText,
                    isActiveEnvironment(envToSelect),
                    false,
                    () -> {
                        apiTester.setActiveEnvironment(envToSelect);
                        updateEnvironmentSelector();
                    }
                )
            );
        }

        return listPanel;
    }

    private JPanel createEnvironmentPopupFooter(JPopupMenu popup) {
        JPanel footerPanel = new JPanel(new BorderLayout());
        footerPanel.setOpaque(false);
        footerPanel.setBorder(
            BorderFactory.createMatteBorder(
                1,
                0,
                0,
                0,
                UIManager.getColor("Separator.foreground") != null
                    ? UIManager.getColor("Separator.foreground")
                    : Color.GRAY
            )
        );

        JPanel configureRow = createEnvironmentPopupRow(
            "\u2699  Configure",
            false,
            false,
            () -> {
                popup.setVisible(false);
                showEnvironmentConfigurationWindow();
            }
        );

        configureRow.setBorder(new EmptyBorder(7, 12, 7, 12));

        JPanel configureWrapper = new JPanel(new BorderLayout());
        configureWrapper.setOpaque(false);
        configureWrapper.setBorder(new EmptyBorder(10, 10, 10, 10));
        configureWrapper.add(configureRow, BorderLayout.CENTER);

        footerPanel.add(configureWrapper, BorderLayout.CENTER);

        return footerPanel;
    }

    private JPanel createEnvironmentPopupRow(
        String text,
        boolean selected,
        boolean dottedSelectedOutline,
        Runnable action
    ) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(true);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.setPreferredSize(new Dimension(220, 34));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Color normalBackground = APITesterColors.panelBackground();

        Color hoverBackground = APITesterColors.isDarkMode()
            ? new Color(70, 70, 70)
            : new Color(235, 235, 235);

        Color selectedBackground = APITesterColors.isDarkMode()
            ? new Color(110, 64, 201, 70)
            : new Color(110, 64, 201, 25);

        row.setBackground(selected ? selectedBackground : normalBackground);

        javax.swing.border.Border paddingBorder = new EmptyBorder(5, 12, 5, 12);

        if (selected && dottedSelectedOutline) {
            row.setBorder(
                BorderFactory.createCompoundBorder(
                    new DottedBorder(new Color(110, 64, 201, 120), 2, 6),
                    paddingBorder
                )
            );
        } else {
            row.setBorder(paddingBorder);
        }

        JLabel label = new JLabel(text);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 12f));

        if (selected) {
            label.setForeground(ENVIRONMENT_PURPLE);
        } else {
            label.setForeground(UIManager.getColor("Label.foreground"));
        }

        row.add(label, BorderLayout.CENTER);

        row.addMouseListener(
            new java.awt.event.MouseAdapter() {

                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (environmentPopup != null) {
                        environmentPopup.setVisible(false);
                    }

                    action.run();
                }

                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (!selected) {
                        row.setBackground(hoverBackground);
                    }
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    row.setBackground(selected ? selectedBackground : normalBackground);
                }
            }
        );

        return row;
    }

    private boolean isActiveEnvironment(APIEnvironment environment) {
        APIEnvironment activeEnvironment = apiTester.getActiveEnvironment();

        if (activeEnvironment == null || environment == null) {
            return false;
        }

        String activeId = activeEnvironment.getId();
        String environmentId = environment.getId();

        if (activeId != null && environmentId != null) {
            return activeId.equals(environmentId);
        }

        return activeEnvironment == environment;
    }

    private void showEnvironmentConfigurationWindow() {
        Window owner = SwingUtilities.getWindowAncestor(this);

        APIEnvironmentConfigWindow window = new APIEnvironmentConfigWindow(owner, apiTester, this);

        window.setVisible(true);
    }

    private static class DottedBorder implements javax.swing.border.Border {
        private final Color color;
        private final int thickness;
        private final int arc;

        DottedBorder(Color color, int thickness, int arc) {
            this.color = color;
            this.thickness = thickness;
            this.arc = arc;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();

            try {
                g2.setColor(color);
                g2.setStroke(
                    new BasicStroke(
                        thickness,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND,
                        0,
                        new float[] { 6f, 4f },
                        0
                    )
                );

                int offset = thickness;
                g2.drawRoundRect(
                    x + offset,
                    y + offset,
                    width - thickness * 2 - 1,
                    height - thickness * 2 - 1,
                    arc,
                    arc
                );
            } finally {
                g2.dispose();
            }
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    private void updateEnvironmentSelectorButtonStyle() {
        if (environmentSelectorButton == null) {
            return;
        }

        boolean noActualEnvironmentSelected = apiTester.getActiveEnvironment() == null;

        javax.swing.border.Border paddingBorder = new EmptyBorder(4, 10, 4, 10);

        if (noActualEnvironmentSelected) {
            environmentSelectorButton.setBorder(
                BorderFactory.createCompoundBorder(
                    new DottedBorder(new Color(110, 64, 201, 120), 2, 6),
                    paddingBorder
                )
            );
        } else {
            environmentSelectorButton.setBorder(
                BorderFactory.createCompoundBorder(
                    UIManager.getBorder("Button.border"),
                    paddingBorder
                )
            );
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Custom Renderers
    // ═══════════════════════════════════════════════════════════════════

    private static class HistoryListRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value instanceof APIRequest) {
                APIRequest req = (APIRequest) value;
                String method = req.getMethod().toString();
                String url = req.getUrl();
                if (url != null && url.length() > 40) {
                    url = url.substring(0, 40) + "...";
                }

                // Color code by method
                String color = getMethodColor(req.getMethod());
                setText(
                    "<html><b style='color:" + color + "'>" + method + "</b> " + url + "</html>"
                );

                setFont(getFont().deriveFont(11f));
                setBorder(new EmptyBorder(4, 8, 4, 8));
            }

            return this;
        }

        private String getMethodColor(APIRequest.HttpMethod method) {
            // Return hex color strings that adapt to theme
            boolean dark = APITesterColors.isDarkMode();
            switch (method) {
                case GET:
                    return dark ? "#4AD98F" : "#38A169";
                case POST:
                    return dark ? "#FFB847" : "#D97A06";
                case PUT:
                    return dark ? "#60A5FA" : "#2563EB";
                case PATCH:
                    return dark ? "#A78BFA" : "#7C3AED";
                case DELETE:
                    return dark ? "#FF6B6B" : "#DC2626";
                default:
                    return dark ? "#8B8898" : "#6B7280";
            }
        }
    }

    private static class EnvironmentComboRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            if (value == null) {
                setText("No Environment");
                setFont(getFont().deriveFont(Font.ITALIC));
            } else if (value instanceof APIEnvironment) {
                APIEnvironment env = (APIEnvironment) value;
                setText(env.getName());
                setFont(getFont().deriveFont(Font.PLAIN));
            }

            return this;
        }
    }
}
