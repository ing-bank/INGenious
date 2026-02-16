package com.ing.ide.main.mainui.components.apitester;

import com.ing.datalib.api.*;
import com.ing.ide.main.mainui.components.apitester.request.RequestPanel;
import com.ing.ide.main.mainui.components.apitester.response.ResponsePanel;
import com.ing.ide.main.mainui.components.apitester.collections.CollectionTree;
import com.ing.ide.main.mainui.components.apitester.util.APITesterColors;
import com.ing.ide.main.fx.INGIcons;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

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
    private JComboBox<APIEnvironment> environmentSelector;
    
    // Right panel components  
    private RequestPanel requestPanel;
    private ResponsePanel responsePanel;
    
    // Current state
    private APIRequest currentRequest;
    
    public APITesterUI(APITester apiTester) {
        this.apiTester = apiTester;
        this.currentRequest = new APIRequest();
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
                if (name != null && (name.contains("None") || name.contains("empty") || name.contains("loading"))) {
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
        historyList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                APIRequest selected = historyList.getSelectedValue();
                if (selected != null) {
                    loadRequest(selected.copy());
                }
            }
        });
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
        
        environmentSelector = new JComboBox<>();
        environmentSelector.setFont(environmentSelector.getFont().deriveFont(11f));
        environmentSelector.setPreferredSize(new Dimension(150, 26));
        environmentSelector.addItem(null); // No environment option
        environmentSelector.setRenderer(new EnvironmentComboRenderer());
        environmentSelector.addActionListener(e -> {
            APIEnvironment selected = (APIEnvironment) environmentSelector.getSelectedItem();
            apiTester.setActiveEnvironment(selected);
        });
        
        // New collection button
        JButton newCollectionBtn = new JButton("+");
        newCollectionBtn.setToolTipText("New Collection");
        newCollectionBtn.setFont(newCollectionBtn.getFont().deriveFont(Font.BOLD, 14f));
        newCollectionBtn.setPreferredSize(new Dimension(32, 26));
        newCollectionBtn.addActionListener(e -> showNewCollectionDialog());
        
        JPanel leftPart = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        leftPart.setOpaque(false);
        leftPart.add(envLabel);
        leftPart.add(environmentSelector);
        
        toolbar.add(leftPart, BorderLayout.CENTER);
        toolbar.add(newCollectionBtn, BorderLayout.EAST);
        
        return toolbar;
    }
    
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 0));
        
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
    
    public void setCurrentRequest(APIRequest request) {
        this.currentRequest = request;
        requestPanel.loadRequest(request);
    }
    
    /**
     * Loads a request into the editor.
     */
    public void loadRequest(APIRequest request) {
        this.currentRequest = request;
        requestPanel.loadRequest(request);
        responsePanel.clear();
    }
    
    /**
     * Loads a request from a collection into the editor.
     */
    public void loadRequest(APIRequest request, APICollection collection) {
        this.currentRequest = request;
        requestPanel.loadRequest(request);
        responsePanel.clear();
    }
    
    /**
     * Creates a new empty request.
     */
    public void newRequest() {
        this.currentRequest = new APIRequest();
        requestPanel.loadRequest(currentRequest);
        responsePanel.clear();
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
        apiTester.executeRequest(currentRequest, new APITester.RequestCallback() {
            @Override
            public void onResponse(APIResponse response) {
                responsePanel.showResponse(response);
            }
            
            @Override
            public void onError(Exception error) {
                responsePanel.showError(error.getMessage());
            }
        });
    }
    
    /**
     * Saves the current request to a collection.
     */
    public void saveRequest() {
        requestPanel.updateRequest(currentRequest);
        
        // Show save dialog
        List<APICollection> collections = apiTester.getCollections();
        if (collections.isEmpty()) {
            // Create a default collection
            APICollection defaultCollection = apiTester.createNewCollection("My Collection");
            saveToCollection(defaultCollection);
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
                saveToCollection(selected);
            }
        }
    }
    
    private void saveToCollection(APICollection collection) {
        // Ask for request name
        String name = JOptionPane.showInputDialog(
                this,
                "Request name:",
                currentRequest.getName() != null ? currentRequest.getName() : currentRequest.getMethod() + " Request"
        );
        if (name != null && !name.trim().isEmpty()) {
            currentRequest.setName(name.trim());
            apiTester.saveRequestToCollection(currentRequest.copy(), collection);
            JOptionPane.showMessageDialog(this, "Request saved to " + collection.getName());
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
        if (environmentSelector != null) {
            environmentSelector.removeAllItems();
            environmentSelector.addItem(null); // No environment
            for (APIEnvironment env : apiTester.getEnvironments()) {
                environmentSelector.addItem(env);
            }
            environmentSelector.setSelectedItem(apiTester.getActiveEnvironment());
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
    
    // ═══════════════════════════════════════════════════════════════════
    // Custom Renderers
    // ═══════════════════════════════════════════════════════════════════
    
    private static class HistoryListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
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
                setText("<html><b style='color:" + color + "'>" + method + "</b> " + url + "</html>");
                
                setFont(getFont().deriveFont(11f));
                setBorder(new EmptyBorder(4, 8, 4, 8));
            }
            
            return this;
        }
        
        private String getMethodColor(APIRequest.HttpMethod method) {
            // Return hex color strings that adapt to theme
            boolean dark = APITesterColors.isDarkMode();
            switch (method) {
                case GET: return dark ? "#4AD98F" : "#38A169";
                case POST: return dark ? "#FFB847" : "#D97A06";
                case PUT: return dark ? "#60A5FA" : "#2563EB";
                case PATCH: return dark ? "#A78BFA" : "#7C3AED";
                case DELETE: return dark ? "#FF6B6B" : "#DC2626";
                default: return dark ? "#8B8898" : "#6B7280";
            }
        }
    }
    
    private static class EnvironmentComboRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
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
