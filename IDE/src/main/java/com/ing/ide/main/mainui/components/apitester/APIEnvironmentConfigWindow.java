package com.ing.ide.main.mainui.components.apitester;

import com.ing.datalib.api.APIEnvironment;
import com.ing.ide.main.mainui.components.apitester.util.APITesterColors;
import com.ing.ide.util.Notification;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class APIEnvironmentConfigWindow extends JDialog {
    private static final Color DARK_BACKGROUND = new Color(24, 24, 24);
    private static final Color DARK_PANEL = new Color(30, 30, 30);
    private static final Color DARK_TABLE = new Color(25, 25, 25);
    private static final Color DARK_ROW_SELECTED = new Color(41, 44, 47);
    private static final Color DARK_ROW_HOVER = new Color(36, 36, 36);
    private static final Color DARK_BORDER = new Color(56, 56, 56);

    private static final Color TEXT_PRIMARY = new Color(215, 215, 215);
    private static final Color TEXT_SECONDARY = new Color(145, 145, 145);

    private static final int ROW_HEIGHT = 35;
    private static final int HEADER_HEIGHT = 30;
    private static final int SIDE_PANEL_WIDTH = 250;
    private static final int SECRET_COLUMN_WIDTH = 85;
    private static final int ACTION_COLUMN_WIDTH = 90;
    private static final int ENVIRONMENT_ROW_HEIGHT = 34;

    private static final double NAME_COLUMN_RATIO = 0.38;

    private static final Color PURPLE = new Color(126, 87, 194);
    private static final int TABLE_CORNER_RADIUS = 14;

    private final APITester apiTester;
    private final APITesterUI apiTesterUI;

    private JTextField searchField;
    private JPanel environmentListPanel;
    private JLabel selectedEnvironmentTitle;
    private JButton renameButton;
    private JButton duplicateButton;
    private JButton deleteEnvironmentButton;

    private JPanel rightContentPanel;
    private JPanel variablesTablePanel;
    private JPanel variableRowsPanel;
    private JScrollPane variablesScrollPane;
    private JPanel variablesOuterPanel;
    private JPanel bottomActionsPanel;
    private final List<VariableRowPanel> variableRows = new ArrayList<>();

    private APIEnvironment selectedEnvironment;
    private boolean loadingVariables;

    public APIEnvironmentConfigWindow(Window owner, APITester apiTester, APITesterUI apiTesterUI) {
        super(owner, "Environment Configuration", ModalityType.APPLICATION_MODAL);
        this.apiTester = apiTester;
        this.apiTesterUI = apiTesterUI;

        if (!apiTester.getEnvironments().isEmpty()) {
            APIEnvironment activeEnvironment = apiTester.getActiveEnvironment();
            selectedEnvironment =
                activeEnvironment != null ? activeEnvironment : apiTester.getEnvironments().get(0);
        }

        initComponents();

        setPreferredSize(new Dimension(980, 560));
        setMinimumSize(new Dimension(780, 460));
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        getContentPane().setBackground(getWindowBackground());

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(getWindowBackground());

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setDividerSize(1);
        splitPane.setDividerLocation(SIDE_PANEL_WIDTH);
        splitPane.setResizeWeight(0);
        splitPane.setContinuousLayout(true);
        splitPane.setBackground(getBorderColor());

        splitPane.setLeftComponent(createLeftPanel());
        splitPane.setRightComponent(createRightPanel());

        root.add(splitPane, BorderLayout.CENTER);
        add(root, BorderLayout.CENTER);

        refreshEnvironmentList();
        loadSelectedEnvironment();
    }

    private JPanel createLeftPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 12));
        panel.setPreferredSize(new Dimension(SIDE_PANEL_WIDTH, 0));
        panel.setMinimumSize(new Dimension(220, 0));
        panel.setBackground(getWindowBackground());
        panel.setBorder(new EmptyBorder(16, 14, 16, 14));

        panel.add(createLeftHeader(), BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 12));
        centerPanel.setOpaque(false);

        centerPanel.add(createSearchField(), BorderLayout.NORTH);

        environmentListPanel = new JPanel();
        environmentListPanel.setOpaque(false);
        environmentListPanel.setLayout(new BoxLayout(environmentListPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(environmentListPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);

        centerPanel.add(scrollPane, BorderLayout.CENTER);
        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createLeftHeader() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 30));

        JLabel title = new JLabel("ENVIRONMENTS");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 14f));
        title.setForeground(getPrimaryTextColor());

        JButton addButton = createTextButton("+ New", "New Environment");
        addButton.setPreferredSize(new Dimension(72, 28));
        addButton.addActionListener(e -> createEnvironment());

        panel.add(title, BorderLayout.WEST);
        panel.add(addButton, BorderLayout.EAST);

        return panel;
    }

    private JComponent createSearchField() {
        JPanel wrapper = new JPanel(new BorderLayout(8, 0));
        wrapper.setBackground(getFieldBackground());
        wrapper.setPreferredSize(new Dimension(0, 36));
        wrapper.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(getBorderColor(), 1, true),
                new EmptyBorder(6, 10, 6, 10)
            )
        );

        JLabel searchIcon = new JLabel("\u2315");
        searchIcon.setForeground(getSecondaryTextColor());
        searchIcon.setFont(searchIcon.getFont().deriveFont(Font.PLAIN, 16f));

        searchField = new JTextField();
        searchField.setBorder(BorderFactory.createEmptyBorder());
        searchField.setOpaque(false);
        searchField.setForeground(getPrimaryTextColor());
        searchField.setCaretColor(getPrimaryTextColor());
        searchField.setFont(searchField.getFont().deriveFont(Font.PLAIN, 13f));
        searchField.putClientProperty("JTextField.placeholderText", "Search environments...");

        searchField
            .getDocument()
            .addDocumentListener(
                new DocumentListener() {

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        refreshEnvironmentList();
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        refreshEnvironmentList();
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        refreshEnvironmentList();
                    }
                }
            );

        wrapper.add(searchIcon, BorderLayout.WEST);
        wrapper.add(searchField, BorderLayout.CENTER);

        return wrapper;
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBackground(getWindowBackground());
        panel.setBorder(new EmptyBorder(18, 24, 18, 24));

        panel.add(createRightHeader(), BorderLayout.NORTH);

        rightContentPanel = new JPanel(new BorderLayout());
        rightContentPanel.setOpaque(false);
        panel.add(rightContentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRightHeader() {
        JPanel panel = new JPanel(new BorderLayout(12, 0));
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 34));

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titlePanel.setOpaque(false);

        selectedEnvironmentTitle = new JLabel("No Environment Selected");
        selectedEnvironmentTitle.setFont(
            selectedEnvironmentTitle.getFont().deriveFont(Font.BOLD, 16f)
        );
        selectedEnvironmentTitle.setForeground(getPrimaryTextColor());

        renameButton = createTextButton("Rename", "Rename Environment");
        renameButton.addActionListener(e -> renameSelectedEnvironment());

        titlePanel.add(selectedEnvironmentTitle);
        titlePanel.add(renameButton);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        duplicateButton = createTextButton("Duplicate", "Duplicate Environment");
        deleteEnvironmentButton = createTextButton("Delete", "Delete Environment");

        duplicateButton.addActionListener(e -> duplicateSelectedEnvironment());
        deleteEnvironmentButton.addActionListener(e -> deleteSelectedEnvironment());

        actionsPanel.add(duplicateButton);
        actionsPanel.add(deleteEnvironmentButton);

        panel.add(titlePanel, BorderLayout.WEST);
        panel.add(actionsPanel, BorderLayout.EAST);

        return panel;
    }

    private void rebuildRightContent() {
        if (rightContentPanel == null) {
            return;
        }

        rightContentPanel.removeAll();

        boolean hasEnvironment = selectedEnvironment != null;

        renameButton.setEnabled(hasEnvironment);
        duplicateButton.setEnabled(hasEnvironment);
        deleteEnvironmentButton.setEnabled(hasEnvironment);

        if (!hasEnvironment) {
            rightContentPanel.add(createNoEnvironmentsPanel(), BorderLayout.CENTER);
        } else {
            rightContentPanel.add(createVariablesPanel(), BorderLayout.CENTER);
            loadVariableRowsOnly();
        }

        rightContentPanel.revalidate();
        rightContentPanel.repaint();
    }

    private JPanel createNoEnvironmentsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);

        JLabel label = new JLabel("No Environments");
        label.setForeground(getSecondaryTextColor());
        label.setFont(label.getFont().deriveFont(Font.BOLD, 16f));

        panel.add(label);
        return panel;
    }

    private JPanel createVariablesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);
        variablesOuterPanel = panel;

        panel.addComponentListener(
            new ComponentAdapter() {

                @Override
                public void componentResized(ComponentEvent e) {
                    updateVariablesTableHeight();
                }

                @Override
                public void componentShown(ComponentEvent e) {
                    updateVariablesTableHeight();
                }
            }
        );

        variablesTablePanel = new RoundedPanel(TABLE_CORNER_RADIUS);
        variablesTablePanel.setLayout(new BorderLayout());
        variablesTablePanel.setBackground(getTableBackground());
        variablesTablePanel.setBorder(
            BorderFactory.createCompoundBorder(
                new RoundedLineBorder(getBorderColor(), TABLE_CORNER_RADIUS),
                new EmptyBorder(1, 1, 0, 1)
            )
        );

        variableRowsPanel = new ScrollableRowsPanel();
        variableRowsPanel.setOpaque(true);
        variableRowsPanel.setBackground(getTableBackground());
        variableRowsPanel.setLayout(new BoxLayout(variableRowsPanel, BoxLayout.Y_AXIS));

        variablesScrollPane = new JScrollPane(variableRowsPanel);
        variablesScrollPane.setBorder(BorderFactory.createEmptyBorder());
        variablesScrollPane.setOpaque(false);
        variablesScrollPane.getViewport().setOpaque(true);
        variablesScrollPane.getViewport().setBackground(getTableBackground());
        variablesScrollPane.getVerticalScrollBar().setUnitIncrement(12);

        variablesScrollPane.setVerticalScrollBarPolicy(
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        variablesScrollPane.setColumnHeaderView(createVariablesHeaderRow());

        variablesTablePanel.add(variablesScrollPane, BorderLayout.CENTER);

        // Important: NORTH prevents the table from stretching vertically.
        panel.add(variablesTablePanel, BorderLayout.NORTH);

        bottomActionsPanel = createBottomActions();
        panel.add(bottomActionsPanel, BorderLayout.SOUTH);

        SwingUtilities.invokeLater(this::updateVariablesTableHeight);

        return panel;
    }

    private void updateVariablesTableHeight() {
        if (variablesTablePanel == null || variablesScrollPane == null) {
            return;
        }

        int rowCount = Math.max(1, variableRows.size());

        int rowsHeight = rowCount * ROW_HEIGHT;
        int headerHeight = HEADER_HEIGHT;

        Insets tableInsets = variablesTablePanel.getInsets();
        Insets scrollInsets = variablesScrollPane.getInsets();

        int fullContentHeight =
            tableInsets.top +
            tableInsets.bottom +
            scrollInsets.top +
            scrollInsets.bottom +
            headerHeight +
            rowsHeight;

        int availableHeight = getAvailableVariablesTableHeight();

        int tableHeight = availableHeight > 0
            ? Math.min(fullContentHeight, availableHeight)
            : fullContentHeight;

        Dimension tableSize = new Dimension(0, tableHeight);

        variablesTablePanel.setPreferredSize(tableSize);
        variablesTablePanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, tableHeight));

        variablesTablePanel.revalidate();
        variablesTablePanel.repaint();

        if (variablesScrollPane != null) {
            variablesScrollPane.revalidate();
            variablesScrollPane.repaint();
        }

        if (variablesOuterPanel != null) {
            variablesOuterPanel.revalidate();
            variablesOuterPanel.repaint();
        }
    }

    private int getAvailableVariablesTableHeight() {
        if (variablesOuterPanel == null) {
            return -1;
        }

        int height = variablesOuterPanel.getHeight();

        if (height <= 0) {
            return -1;
        }

        Insets insets = variablesOuterPanel.getInsets();
        height -= insets.top + insets.bottom;

        if (bottomActionsPanel != null) {
            height -= bottomActionsPanel.getPreferredSize().height;
        }

        LayoutManager layout = variablesOuterPanel.getLayout();
        if (layout instanceof BorderLayout) {
            height -= ((BorderLayout) layout).getVgap();
        }

        return Math.max(0, height);
    }

    private JPanel createVariablesHeaderRow() {
        JPanel header = new JPanel(new VariableTableRowLayout());
        header.setOpaque(true);
        header.setBackground(getTableBackground());
        header.setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, HEADER_HEIGHT));

        header.add(createHeaderCell("Name", true));
        header.add(createHeaderCell("Value", true));
        header.add(createHeaderCell("Secret", true, SwingConstants.CENTER));
        header.add(createHeaderCell("", false));

        return header;
    }

    private JPanel createHeaderCell(String text, boolean rightBorder) {
        return createHeaderCell(text, rightBorder, SwingConstants.LEFT);
    }

    private JPanel createHeaderCell(String text, boolean rightBorder, int horizontalAlignment) {
        JPanel cell = new JPanel(new BorderLayout());
        cell.setOpaque(true);
        cell.setBackground(getTableBackground());
        cell.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, rightBorder ? 1 : 0, getBorderColor()),
                new EmptyBorder(0, 20, 0, 20)
            )
        );

        JLabel label = new JLabel(text);
        label.setHorizontalAlignment(horizontalAlignment);
        label.setFont(label.getFont().deriveFont(Font.BOLD, 13f));
        label.setForeground(getPrimaryTextColor());

        cell.add(label, BorderLayout.CENTER);

        return cell;
    }

    private JPanel createBottomActions() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setPreferredSize(new Dimension(0, 42));

        JPanel leftActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftActions.setOpaque(false);

        JButton saveButton = new JButton("Save");
        saveButton.setPreferredSize(new Dimension(76, 34));
        saveButton.setFocusPainted(false);
        saveButton.setBackground(PURPLE);
        saveButton.setForeground(Color.WHITE);
        saveButton.setOpaque(true);
        saveButton.setBorderPainted(false);
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD, 13f));
        saveButton.addActionListener(e -> saveSelectedEnvironment());

        leftActions.add(saveButton);

        panel.add(leftActions, BorderLayout.WEST);

        return panel;
    }

    private JButton createTextButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setForeground(getActionButtonTextColor());
        button.setFont(button.getFont().deriveFont(Font.BOLD, 13f));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }

    private void refreshEnvironmentList() {
        if (environmentListPanel == null) {
            return;
        }

        environmentListPanel.removeAll();

        String searchText = searchField != null ? searchField.getText().trim().toLowerCase() : "";

        for (APIEnvironment environment : apiTester.getEnvironments()) {
            if (environment == null) {
                continue;
            }

            String environmentName = environment.getName();
            if (environmentName == null || environmentName.trim().isEmpty()) {
                environmentName = "Unnamed Environment";
            }

            if (!searchText.isEmpty() && !environmentName.toLowerCase().contains(searchText)) {
                continue;
            }

            environmentListPanel.add(createEnvironmentRow(environment));
            environmentListPanel.add(Box.createVerticalStrut(4));
        }

        environmentListPanel.revalidate();
        environmentListPanel.repaint();
    }

    private JPanel createEnvironmentRow(APIEnvironment environment) {
        boolean selected = isSelectedEnvironment(environment);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ENVIRONMENT_ROW_HEIGHT));
        row.setPreferredSize(new Dimension(220, ENVIRONMENT_ROW_HEIGHT));
        row.setBorder(new EmptyBorder(0, 14, 0, 12));
        row.setBackground(selected ? getSelectedRowColor() : getWindowBackground());
        row.setOpaque(true);
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        String environmentName = environment.getName();
        if (environmentName == null || environmentName.trim().isEmpty()) {
            environmentName = "Unnamed Environment";
        }

        JLabel nameLabel = new JLabel(environmentName);
        nameLabel.setForeground(getPrimaryTextColor());
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 13f));

        row.add(nameLabel, BorderLayout.CENTER);

        row.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedEnvironment = environment;
                    apiTester.setActiveEnvironment(environment);
                    refreshEnvironmentList();
                    loadSelectedEnvironment();
                    apiTesterUI.updateEnvironmentSelector();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isSelectedEnvironment(environment)) {
                        row.setBackground(getHoverRowColor());
                    }
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    row.setBackground(
                        isSelectedEnvironment(environment)
                            ? getSelectedRowColor()
                            : getWindowBackground()
                    );
                }
            }
        );

        return row;
    }

    private void loadSelectedEnvironment() {
        if (apiTester.getEnvironments().isEmpty()) {
            selectedEnvironment = null;
        }

        if (selectedEnvironment == null) {
            selectedEnvironmentTitle.setText("No Environment Selected");
        } else {
            String environmentName = selectedEnvironment.getName();
            if (environmentName == null || environmentName.trim().isEmpty()) {
                environmentName = "Unnamed Environment";
            }
            selectedEnvironmentTitle.setText(environmentName);
        }

        rebuildRightContent();
    }

    private void loadVariableRowsOnly() {
        clearVariableRows();

        if (selectedEnvironment == null) {
            return;
        }

        loadingVariables = true;

        if (selectedEnvironment.getVariables() != null) {
            for (Map.Entry<String, String> entry : selectedEnvironment.getVariables().entrySet()) {
                addVariableRow(entry.getKey(), entry.getValue(), false);
            }
        }

        if (selectedEnvironment.getSecrets() != null) {
            for (Map.Entry<String, String> entry : selectedEnvironment.getSecrets().entrySet()) {
                addVariableRow(entry.getKey(), entry.getValue(), true);
            }
        }

        loadingVariables = false;

        ensureTrailingEmptyRow();
        refreshVariableRows();
    }

    private void clearVariableRows() {
        variableRows.clear();

        if (variableRowsPanel != null) {
            variableRowsPanel.removeAll();
            variableRowsPanel.revalidate();
            variableRowsPanel.repaint();
        }
    }

    private VariableRowPanel addVariableRow(String name, String value, boolean secret) {
        VariableRowPanel rowPanel = new VariableRowPanel(name, value, secret);
        variableRows.add(rowPanel);

        if (variableRowsPanel != null) {
            variableRowsPanel.add(rowPanel);
            updateVariablesTableHeight();
            variableRowsPanel.revalidate();
            variableRowsPanel.repaint();
        }

        return rowPanel;
    }

    private void scheduleNormalizeVariableRows() {
        if (loadingVariables) {
            return;
        }

        SwingUtilities.invokeLater(this::normalizeVariableRows);
    }

    private void normalizeVariableRows() {
        if (selectedEnvironment == null || variableRowsPanel == null) {
            return;
        }

        boolean removedRow = false;

        for (int i = variableRows.size() - 1; i >= 0; i--) {
            VariableRowPanel row = variableRows.get(i);

            if (row.isVacant() && i != variableRows.size() - 1) {
                variableRows.remove(i);
                removedRow = true;
            }
        }

        if (variableRows.isEmpty() || !variableRows.get(variableRows.size() - 1).isVacant()) {
            addVariableRow("", "", false);
        }

        for (VariableRowPanel row : variableRows) {
            row.updateVacantState();
        }

        if (removedRow) {
            refreshVariableRows();
        } else {
            variableRowsPanel.revalidate();
            variableRowsPanel.repaint();
        }
    }

    private void ensureTrailingEmptyRow() {
        if (variableRows.isEmpty() || !variableRows.get(variableRows.size() - 1).isVacant()) {
            addVariableRow("", "", false);
        }

        for (VariableRowPanel row : variableRows) {
            row.updateVacantState();
        }
    }

    private void refreshVariableRows() {
        if (variableRowsPanel == null) {
            return;
        }

        variableRowsPanel.removeAll();

        for (VariableRowPanel row : variableRows) {
            variableRowsPanel.add(row);
        }

        updateVariablesTableHeight();

        variableRowsPanel.revalidate();
        variableRowsPanel.repaint();
    }

    private void removeVariableRow(VariableRowPanel rowPanel) {
        variableRows.remove(rowPanel);
        ensureTrailingEmptyRow();
        refreshVariableRows();
    }

    private void saveSelectedEnvironment() {
        if (selectedEnvironment == null) {
            Notification.show("Create or select an environment first.");
            return;
        }

        Map<String, String> variables = new HashMap<>();
        Map<String, String> secrets = new HashMap<>();

        for (VariableRowPanel rowPanel : variableRows) {
            String name = rowPanel.getVariableName().trim();
            String value = rowPanel.getVariableValue();

            if (rowPanel.isVacant()) {
                continue;
            }

            if (name.isEmpty()) {
                Notification.show("Variable name is required.");
                return;
            }

            if (variables.containsKey(name) || secrets.containsKey(name)) {
                Notification.show("Duplicate variable name: " + name);
                return;
            }

            if (rowPanel.isSecret()) {
                secrets.put(name, value);
            } else {
                variables.put(name, value);
            }
        }

        selectedEnvironment.setVariables(variables);
        selectedEnvironment.setSecrets(secrets);

        apiTester.saveEnvironment(selectedEnvironment);
        apiTesterUI.updateEnvironmentSelector();

        Notification.show("Environment \"" + selectedEnvironment.getName() + "\" saved.");
        refreshEnvironmentList();
        loadSelectedEnvironment();
    }

    private void createEnvironment() {
        String name = JOptionPane.showInputDialog(
            this,
            "Environment name:",
            "New Environment",
            JOptionPane.PLAIN_MESSAGE
        );

        if (name == null) {
            return;
        }

        name = name.trim();

        if (name.isEmpty()) {
            Notification.show("Environment name is required.");
            return;
        }

        try {
            APIEnvironment environment = apiTester.createNewEnvironment(name);
            selectedEnvironment = environment;
            apiTester.setActiveEnvironment(environment);

            refreshEnvironmentList();
            loadSelectedEnvironment();
            apiTesterUI.updateEnvironmentSelector();

            Notification.show("Environment \"" + environment.getName() + "\" created.");
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void renameSelectedEnvironment() {
        if (selectedEnvironment == null) {
            Notification.show("Select an environment to rename.");
            return;
        }

        String name = JOptionPane.showInputDialog(
            this,
            "Environment name:",
            selectedEnvironment.getName()
        );

        if (name == null) {
            return;
        }

        name = name.trim();

        if (name.isEmpty()) {
            Notification.show("Environment name is required.");
            return;
        }

        try {
            apiTester.renameEnvironment(selectedEnvironment, name);

            refreshEnvironmentList();
            loadSelectedEnvironment();
            apiTesterUI.updateEnvironmentSelector();

            Notification.show("Environment renamed to \"" + selectedEnvironment.getName() + "\".");
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void duplicateSelectedEnvironment() {
        if (selectedEnvironment == null) {
            Notification.show("Select an environment to duplicate.");
            return;
        }

        APIEnvironment copy = selectedEnvironment.copy();

        try {
            String baseName = copy.getName();
            String uniqueName = baseName;
            int suffix = 2;

            while (apiTester.environmentNameExists(uniqueName, null)) {
                uniqueName = baseName + " " + suffix;
                suffix++;
            }

            copy.setName(uniqueName);

            apiTester.addEnvironment(copy);
            selectedEnvironment = copy;
            apiTester.setActiveEnvironment(copy);

            refreshEnvironmentList();
            loadSelectedEnvironment();
            apiTesterUI.updateEnvironmentSelector();

            Notification.show("Environment \"" + copy.getName() + "\" created.");
        } catch (IllegalArgumentException ex) {
            Notification.show(ex.getMessage());
        }
    }

    private void deleteSelectedEnvironment() {
        if (selectedEnvironment == null) {
            Notification.show("Select an environment to delete.");
            return;
        }

        int result = JOptionPane.showConfirmDialog(
            this,
            "Delete environment \"" + selectedEnvironment.getName() + "\"?",
            "Delete Environment",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );

        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        String deletedName = selectedEnvironment.getName();

        apiTester.deleteEnvironment(selectedEnvironment);

        selectedEnvironment = apiTester.getActiveEnvironment();

        if (selectedEnvironment == null && !apiTester.getEnvironments().isEmpty()) {
            selectedEnvironment = apiTester.getEnvironments().get(0);
        }

        refreshEnvironmentList();
        loadSelectedEnvironment();
        apiTesterUI.updateEnvironmentSelector();

        Notification.show("Environment \"" + deletedName + "\" deleted.");
    }

    private boolean isSelectedEnvironment(APIEnvironment environment) {
        if (selectedEnvironment == null || environment == null) {
            return false;
        }

        String selectedId = selectedEnvironment.getId();
        String environmentId = environment.getId();

        if (selectedId != null && environmentId != null) {
            return selectedId.equals(environmentId);
        }

        return selectedEnvironment == environment;
    }

    private Color getWindowBackground() {
        return APITesterColors.isDarkMode() ? DARK_BACKGROUND : APITesterColors.panelBackground();
    }

    private Color getTableBackground() {
        return APITesterColors.isDarkMode() ? DARK_TABLE : UIManager.getColor("Table.background");
    }

    private Color getFieldBackground() {
        return APITesterColors.isDarkMode()
            ? DARK_PANEL
            : UIManager.getColor("TextField.background");
    }

    private Color getBorderColor() {
        return APITesterColors.isDarkMode()
            ? DARK_BORDER
            : UIManager.getColor("Separator.foreground");
    }

    private Color getPrimaryTextColor() {
        return APITesterColors.isDarkMode() ? TEXT_PRIMARY : UIManager.getColor("Label.foreground");
    }

    private Color getSecondaryTextColor() {
        return APITesterColors.isDarkMode()
            ? TEXT_SECONDARY
            : UIManager.getColor("Label.disabledForeground");
    }

    private Color getSelectedRowColor() {
        return APITesterColors.isDarkMode() ? DARK_ROW_SELECTED : new Color(235, 238, 242);
    }

    private Color getHoverRowColor() {
        return APITesterColors.isDarkMode() ? DARK_ROW_HOVER : new Color(245, 245, 245);
    }

    private Color getActionButtonTextColor() {
        return APITesterColors.isDarkMode() ? new Color(175, 175, 175) : new Color(90, 90, 90);
    }

    private Color getActionButtonHoverTextColor() {
        return APITesterColors.isDarkMode() ? new Color(220, 220, 220) : new Color(55, 55, 55);
    }

    private class RoundedPanel extends JPanel {
        private final int radius;

        RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fill(
                new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, radius, radius)
            );

            g2.dispose();

            super.paintComponent(g);
        }
    }

    private class RoundedLineBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int radius;

        RoundedLineBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(
            Component component,
            Graphics graphics,
            int x,
            int y,
            int width,
            int height
        ) {
            Graphics2D g2 = (Graphics2D) graphics.create();

            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.draw(new RoundRectangle2D.Double(x, y, width - 1, height - 1, radius, radius));

            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.set(1, 1, 1, 1);
            return insets;
        }
    }

    private class VariableTableRowLayout implements LayoutManager {

        @Override
        public void addLayoutComponent(String name, Component comp) {
            // No-op
        }

        @Override
        public void removeLayoutComponent(Component comp) {
            // No-op
        }

        @Override
        public Dimension preferredLayoutSize(Container parent) {
            int height = parent == variablesTablePanel ? HEADER_HEIGHT : ROW_HEIGHT;
            return new Dimension(SECRET_COLUMN_WIDTH + ACTION_COLUMN_WIDTH + 400, height);
        }

        @Override
        public Dimension minimumLayoutSize(Container parent) {
            int height = parent == variablesTablePanel ? HEADER_HEIGHT : ROW_HEIGHT;
            return new Dimension(SECRET_COLUMN_WIDTH + ACTION_COLUMN_WIDTH + 120, height);
        }

        @Override
        public void layoutContainer(Container parent) {
            Component[] components = parent.getComponents();

            if (components.length < 4) {
                return;
            }

            int width = parent.getWidth();
            int height = parent.getHeight();

            int fixedWidth = SECRET_COLUMN_WIDTH + ACTION_COLUMN_WIDTH;
            int flexibleWidth = Math.max(0, width - fixedWidth);

            int nameWidth = (int) Math.round(flexibleWidth * NAME_COLUMN_RATIO);
            int valueWidth = flexibleWidth - nameWidth;

            int x = 0;

            components[0].setBounds(x, 0, nameWidth, height);
            x += nameWidth;

            components[1].setBounds(x, 0, valueWidth, height);
            x += valueWidth;

            components[2].setBounds(x, 0, SECRET_COLUMN_WIDTH, height);
            x += SECRET_COLUMN_WIDTH;

            components[3].setBounds(x, 0, ACTION_COLUMN_WIDTH, height);
        }
    }

    private class ScrollableRowsPanel extends JPanel implements Scrollable {

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(
            Rectangle visibleRect,
            int orientation,
            int direction
        ) {
            return ROW_HEIGHT;
        }

        @Override
        public int getScrollableBlockIncrement(
            Rectangle visibleRect,
            int orientation,
            int direction
        ) {
            return Math.max(ROW_HEIGHT, visibleRect.height - ROW_HEIGHT);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            if (getParent() instanceof JViewport) {
                JViewport viewport = (JViewport) getParent();
                return getPreferredSize().height <= viewport.getHeight();
            }

            return false;
        }
    }

    private class VariableRowPanel extends JPanel {
        private final JTextField nameField;
        private final JTextField valueField;
        private final JPasswordField passwordField;
        private final JCheckBox secretCheckBox;
        private final JButton visibilityButton;
        private final JButton deleteButton;
        private final JPanel valueEditorPanel;
        private final JPanel visibilityButtonWrapper;
        private JPanel valueCell;
        private JPanel secretCell;
        private JPanel deleteCell;
        private boolean valueVisible;

        public VariableRowPanel(String name, String value, boolean secret) {
            setLayout(new VariableTableRowLayout());
            setOpaque(true);
            setBackground(getTableBackground());
            setPreferredSize(new Dimension(0, ROW_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
            setMinimumSize(new Dimension(0, ROW_HEIGHT));

            valueVisible = !secret;

            nameField = createCellTextField("Name", true);
            nameField.setText(name);

            valueField = createCellTextField("Value", true);
            valueField.setText(value);

            passwordField = createCellPasswordField("Value");
            passwordField.setText(value);

            addTextChangeListener(nameField);
            addTextChangeListener(valueField);
            addTextChangeListener(passwordField);

            valueEditorPanel = new JPanel(new BorderLayout());
            valueEditorPanel.setOpaque(false);

            visibilityButtonWrapper = new JPanel(new BorderLayout());
            visibilityButtonWrapper.setOpaque(false);
            visibilityButtonWrapper.setPreferredSize(new Dimension(76, 30));

            secretCheckBox = new JCheckBox();
            secretCheckBox.setHorizontalAlignment(SwingConstants.CENTER);
            secretCheckBox.setOpaque(false);
            secretCheckBox.setSelected(secret);
            secretCheckBox.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            secretCheckBox.addActionListener(
                e -> {
                    boolean isSecret = secretCheckBox.isSelected();
                    valueVisible = !isSecret;
                    updateValueEditor();
                    updateVacantState();
                    scheduleNormalizeVariableRows();
                }
            );

            visibilityButton = createSmallTextButton(secret ? "Show" : "");
            visibilityButton.addActionListener(
                e -> {
                    valueVisible = !valueVisible;
                    syncValueFields();
                    updateValueEditor();
                }
            );

            deleteButton = createSmallTextButton("Remove");
            deleteButton.setToolTipText("Delete variable");
            deleteButton.addActionListener(e -> removeVariableRow(this));

            add(createNameCell());
            add(createValueCell());
            add(createSecretCell());
            add(createDeleteCell());

            updateValueEditor();
            updateVacantState();
        }

        private void addTextChangeListener(JTextField field) {
            field
                .getDocument()
                .addDocumentListener(
                    new DocumentListener() {

                        @Override
                        public void insertUpdate(DocumentEvent e) {
                            updateVacantState();
                            scheduleNormalizeVariableRows();
                        }

                        @Override
                        public void removeUpdate(DocumentEvent e) {
                            updateVacantState();
                            scheduleNormalizeVariableRows();
                        }

                        @Override
                        public void changedUpdate(DocumentEvent e) {
                            updateVacantState();
                            scheduleNormalizeVariableRows();
                        }
                    }
                );
        }

        private JPanel createNameCell() {
            JPanel cell = createBodyCell(true, new Insets(0, 20, 0, 20));
            cell.add(nameField, BorderLayout.CENTER);
            return cell;
        }

        private JPanel createValueCell() {
            valueCell = createBodyCell(true, new Insets(0, 20, 0, 14));
            valueCell.setLayout(new BorderLayout());
            valueCell.add(valueEditorPanel, BorderLayout.CENTER);
            valueCell.add(visibilityButtonWrapper, BorderLayout.EAST);
            visibilityButtonWrapper.add(visibilityButton, BorderLayout.CENTER);
            return valueCell;
        }

        private JPanel createSecretCell() {
            secretCell = createBodyCell(true, new Insets(0, 0, 0, 0));
            secretCell.add(secretCheckBox, BorderLayout.CENTER);
            return secretCell;
        }

        private JPanel createDeleteCell() {
            deleteCell = createBodyCell(false, new Insets(0, 0, 0, 0));
            deleteCell.add(deleteButton, BorderLayout.CENTER);
            return deleteCell;
        }

        private JPanel createBodyCell(boolean rightBorder, Insets padding) {
            JPanel cell = new JPanel(new BorderLayout());
            cell.setOpaque(true);
            cell.setBackground(getTableBackground());
            cell.setBorder(
                BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, rightBorder ? 1 : 0, getBorderColor()),
                    new EmptyBorder(padding.top, padding.left, padding.bottom, padding.right)
                )
            );
            return cell;
        }

        private JTextField createCellTextField(String placeholder, boolean bold) {
            JTextField field = new JTextField();
            field.setBorder(BorderFactory.createEmptyBorder());
            field.setOpaque(false);
            field.setForeground(getPrimaryTextColor());
            field.setCaretColor(getPrimaryTextColor());
            field.setFont(field.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, 13f));
            field.putClientProperty("JTextField.placeholderText", placeholder);
            return field;
        }

        private JPasswordField createCellPasswordField(String placeholder) {
            JPasswordField field = new JPasswordField();
            field.setBorder(BorderFactory.createEmptyBorder());
            field.setOpaque(false);
            field.setForeground(getPrimaryTextColor());
            field.setCaretColor(getPrimaryTextColor());
            field.setFont(field.getFont().deriveFont(Font.BOLD, 13f));
            field.putClientProperty("JTextField.placeholderText", placeholder);
            return field;
        }

        private void updateValueEditor() {
            syncValueFields();

            valueEditorPanel.removeAll();

            if (isSecret() && !valueVisible) {
                valueEditorPanel.add(passwordField, BorderLayout.CENTER);
            } else {
                valueEditorPanel.add(valueField, BorderLayout.CENTER);
            }

            if (isSecret() && !isVacant()) {
                visibilityButton.setText(valueVisible ? "Hide" : "Show");
                visibilityButton.setVisible(true);
            } else {
                visibilityButton.setVisible(false);
            }

            valueEditorPanel.revalidate();
            valueEditorPanel.repaint();
            visibilityButtonWrapper.revalidate();
            visibilityButtonWrapper.repaint();
        }

        private void syncValueFields() {
            if (valueVisible) {
                valueField.setText(new String(passwordField.getPassword()));
            } else {
                passwordField.setText(valueField.getText());
            }
        }

        private void updateVacantState() {
            boolean vacant = isVacant();

            secretCheckBox.setVisible(!vacant);
            deleteButton.setVisible(!vacant);
            visibilityButton.setVisible(!vacant && isSecret());

            secretCell.revalidate();
            secretCell.repaint();

            deleteCell.revalidate();
            deleteCell.repaint();

            valueCell.revalidate();
            valueCell.repaint();
        }

        public void focusNameField() {
            nameField.requestFocusInWindow();
        }

        public String getVariableName() {
            return nameField.getText() != null ? nameField.getText() : "";
        }

        public String getVariableValue() {
            if (isSecret() && !valueVisible) {
                return new String(passwordField.getPassword());
            }

            return valueField.getText() != null ? valueField.getText() : "";
        }

        public boolean isSecret() {
            return secretCheckBox.isSelected();
        }

        public boolean isVacant() {
            String name = getVariableName().trim();
            String value = getVariableValue().trim();

            return name.isEmpty() && value.isEmpty() && !secretCheckBox.isSelected();
        }
    }

    private JButton createSmallTextButton(String text) {
        JButton button = new JButton(text);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setForeground(getActionButtonTextColor());
        button.setFont(button.getFont().deriveFont(Font.BOLD, 12f));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(72, 30));
        return button;
    }
}
