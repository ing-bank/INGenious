package com.ing.ide.main.mainui.components.apitester;

import com.ing.datalib.api.APIEnvironment;
import com.ing.ide.main.mainui.components.apitester.util.APITesterColors;
import com.ing.ide.util.Notification;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class APIEnvironmentConfigWindow extends JDialog {
    private static final Color DARK_BACKGROUND = new Color(24, 24, 24);
    private static final Color DARK_PANEL = new Color(30, 30, 30);
    private static final Color DARK_TABLE = new Color(25, 25, 25);
    private static final Color DARK_ROW_SELECTED = new Color(41, 44, 47);
    private static final Color DARK_ROW_HOVER = new Color(36, 36, 36);
    private static final Color DARK_BORDER = new Color(56, 56, 56);

    private static final Color TEXT_PRIMARY = new Color(215, 215, 215);
    private static final Color TEXT_SECONDARY = new Color(145, 145, 145);
    private static final Color ACCENT = new Color(225, 180, 72);

    private static final int ROW_HEIGHT = 48;
    private static final int HEADER_HEIGHT = 44;
    private static final int SIDE_PANEL_WIDTH = 255;
    private static final int SECRET_COLUMN_WIDTH = 100;
    private static final int ACTION_COLUMN_WIDTH = 64;

    private final APITester apiTester;
    private final APITesterUI apiTesterUI;

    private JTextField searchField;
    private JPanel environmentListPanel;
    private JLabel selectedEnvironmentTitle;

    private JPanel variablesTablePanel;
    private JPanel variableRowsPanel;
    private final List<VariableRowPanel> variableRows = new ArrayList<>();

    private APIEnvironment selectedEnvironment;

    public APIEnvironmentConfigWindow(Window owner, APITester apiTester, APITesterUI apiTesterUI) {
        super(owner, "Environment Configuration", ModalityType.APPLICATION_MODAL);
        this.apiTester = apiTester;
        this.apiTesterUI = apiTesterUI;

        if (!apiTester.getEnvironments().isEmpty()) {
            APIEnvironment activeEnvironment = apiTester.getActiveEnvironment();
            this.selectedEnvironment =
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

        JButton addButton = createIconButton("+", "New Environment");
        addButton.setFont(addButton.getFont().deriveFont(Font.PLAIN, 22f));
        addButton.setPreferredSize(new Dimension(28, 28));
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
                new javax.swing.event.DocumentListener() {

                    @Override
                    public void insertUpdate(javax.swing.event.DocumentEvent e) {
                        refreshEnvironmentList();
                    }

                    @Override
                    public void removeUpdate(javax.swing.event.DocumentEvent e) {
                        refreshEnvironmentList();
                    }

                    @Override
                    public void changedUpdate(javax.swing.event.DocumentEvent e) {
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
        panel.add(createVariablesPanel(), BorderLayout.CENTER);

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
            selectedEnvironmentTitle.getFont().deriveFont(Font.BOLD, 18f)
        );
        selectedEnvironmentTitle.setForeground(getPrimaryTextColor());

        JButton renameButton = createIconButton("\u270E", "Rename Environment");
        renameButton.addActionListener(e -> renameSelectedEnvironment());

        titlePanel.add(selectedEnvironmentTitle);
        titlePanel.add(renameButton);

        JPanel actionsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actionsPanel.setOpaque(false);

        JButton duplicateButton = createIconButton("\u29C9", "Duplicate Environment");
        JButton deleteButton = createIconButton("\u00D7", "Delete Environment");

        duplicateButton.addActionListener(e -> duplicateSelectedEnvironment());
        deleteButton.addActionListener(e -> deleteSelectedEnvironment());

        actionsPanel.add(duplicateButton);
        actionsPanel.add(deleteButton);

        panel.add(titlePanel, BorderLayout.WEST);
        panel.add(actionsPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createVariablesPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setOpaque(false);

        variablesTablePanel = new JPanel(new BorderLayout());
        variablesTablePanel.setBackground(getTableBackground());
        variablesTablePanel.setBorder(BorderFactory.createLineBorder(getBorderColor(), 1, true));

        variablesTablePanel.add(createVariablesHeaderRow(), BorderLayout.NORTH);

        variableRowsPanel = new JPanel();
        variableRowsPanel.setOpaque(true);
        variableRowsPanel.setBackground(getTableBackground());
        variableRowsPanel.setLayout(new BoxLayout(variableRowsPanel, BoxLayout.Y_AXIS));

        JScrollPane scrollPane = new JScrollPane(variableRowsPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setBackground(getTableBackground());
        scrollPane.getVerticalScrollBar().setUnitIncrement(12);

        variablesTablePanel.add(scrollPane, BorderLayout.CENTER);

        panel.add(variablesTablePanel, BorderLayout.CENTER);
        panel.add(createBottomActions(), BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createVariablesHeaderRow() {
        JPanel header = new JPanel(new GridBagLayout());
        header.setOpaque(true);
        header.setBackground(getTableBackground());
        header.setPreferredSize(new Dimension(0, HEADER_HEIGHT));
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, HEADER_HEIGHT));

        header.add(
            createHeaderCell("Name", true),
            createCellConstraints(0, 0.34, GridBagConstraints.BOTH)
        );
        header.add(
            createHeaderCell("Value", true),
            createCellConstraints(1, 0.50, GridBagConstraints.BOTH)
        );
        header.add(
            createHeaderCell("Secret", true),
            createFixedCellConstraints(2, SECRET_COLUMN_WIDTH)
        );
        header.add(createHeaderCell("", false), createFixedCellConstraints(3, ACTION_COLUMN_WIDTH));

        return header;
    }

    private JPanel createHeaderCell(String text, boolean rightBorder) {
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
        saveButton.setBackground(ACCENT);
        saveButton.setForeground(Color.BLACK);
        saveButton.setFont(saveButton.getFont().deriveFont(Font.BOLD, 13f));
        saveButton.addActionListener(e -> saveSelectedEnvironment());

        leftActions.add(saveButton);

        JPanel rightActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightActions.setOpaque(false);

        JButton addVariableButton = new JButton("+ Add Variable");
        addVariableButton.setPreferredSize(new Dimension(130, 34));
        addVariableButton.setFocusPainted(false);
        addVariableButton.setBorderPainted(false);
        addVariableButton.setContentAreaFilled(false);
        addVariableButton.setForeground(ACCENT);
        addVariableButton.setFont(addVariableButton.getFont().deriveFont(Font.BOLD, 13f));
        addVariableButton.addActionListener(e -> addVariableRow());

        rightActions.add(addVariableButton);

        panel.add(leftActions, BorderLayout.WEST);
        panel.add(rightActions, BorderLayout.EAST);

        return panel;
    }

    private GridBagConstraints createCellConstraints(int gridX, double weightX, int fill) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = gridX;
        gbc.gridy = 0;
        gbc.weightx = weightX;
        gbc.weighty = 1;
        gbc.fill = fill;
        return gbc;
    }

    private GridBagConstraints createFixedCellConstraints(int gridX, int width) {
        GridBagConstraints gbc = createCellConstraints(gridX, 0, GridBagConstraints.BOTH);
        gbc.weightx = 0;
        gbc.ipadx = 0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.anchor = GridBagConstraints.CENTER;
        return gbc;
    }

    private JButton createIconButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setForeground(getSecondaryTextColor());
        button.setFont(button.getFont().deriveFont(Font.BOLD, 16f));
        button.setPreferredSize(new Dimension(30, 30));
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
            environmentListPanel.add(Box.createVerticalStrut(6));
        }

        environmentListPanel.revalidate();
        environmentListPanel.repaint();
    }

    private JPanel createEnvironmentRow(APIEnvironment environment) {
        boolean selected = isSelectedEnvironment(environment);

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        row.setPreferredSize(new Dimension(220, 42));
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
        clearVariableRows();

        if (selectedEnvironment == null) {
            selectedEnvironmentTitle.setText("No Environment Selected");
            addPlaceholderRow();
            return;
        }

        String environmentName = selectedEnvironment.getName();
        if (environmentName == null || environmentName.trim().isEmpty()) {
            environmentName = "Unnamed Environment";
        }

        selectedEnvironmentTitle.setText(environmentName);

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

        if (variableRows.isEmpty()) {
            addPlaceholderRow();
        }

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

    private void addPlaceholderRow() {
        addVariableRow("", "", false);
    }

    private void addVariableRow() {
        if (selectedEnvironment == null) {
            Notification.show("Create or select an environment first.");
            return;
        }

        VariableRowPanel rowPanel = addVariableRow("", "", false);
        refreshVariableRows();
        rowPanel.focusNameField();
    }

    private VariableRowPanel addVariableRow(String name, String value, boolean secret) {
        VariableRowPanel rowPanel = new VariableRowPanel(name, value, secret);
        variableRows.add(rowPanel);

        if (variableRowsPanel != null) {
            variableRowsPanel.add(rowPanel);
            variableRowsPanel.revalidate();
            variableRowsPanel.repaint();
        }

        return rowPanel;
    }

    private void refreshVariableRows() {
        if (variableRowsPanel == null) {
            return;
        }

        variableRowsPanel.removeAll();

        for (VariableRowPanel row : variableRows) {
            variableRowsPanel.add(row);
        }

        variableRowsPanel.revalidate();
        variableRowsPanel.repaint();
    }

    private void removeVariableRow(VariableRowPanel rowPanel) {
        variableRows.remove(rowPanel);

        if (variableRows.isEmpty()) {
            addPlaceholderRow();
        }

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

            if (name.isEmpty()) {
                continue;
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

    private class VariableRowPanel extends JPanel {
        private final JTextField nameField;
        private final JTextField valueField;
        private final JPasswordField passwordField;
        private final JCheckBox secretCheckBox;
        private final JButton visibilityButton;
        private final JButton deleteButton;
        private JPanel valueCell;
        private boolean valueVisible;

        public VariableRowPanel(String name, String value, boolean secret) {
            setLayout(new GridBagLayout());
            setOpaque(true);
            setBackground(getTableBackground());
            setPreferredSize(new Dimension(0, ROW_HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
            setMinimumSize(new Dimension(0, ROW_HEIGHT));

            this.valueVisible = !secret;

            nameField = createCellTextField("Name", true);
            nameField.setText(name);

            valueField = createCellTextField("Value", true);
            valueField.setText(value);

            passwordField = createCellPasswordField("Value");
            passwordField.setText(value);

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
                }
            );

            visibilityButton = createInlineIconButton("");
            visibilityButton.addActionListener(
                e -> {
                    valueVisible = !valueVisible;
                    syncValueFields();
                    updateValueEditor();
                }
            );

            deleteButton = createInlineIconButton("\u232B");
            deleteButton.setToolTipText("Delete variable");
            deleteButton.addActionListener(e -> removeVariableRow(this));

            add(createNameCell(), createCellConstraints(0, 0.34, GridBagConstraints.BOTH));
            add(createValueCell(), createCellConstraints(1, 0.50, GridBagConstraints.BOTH));
            add(createSecretCell(), createFixedCellConstraints(2, SECRET_COLUMN_WIDTH));
            add(createDeleteCell(), createFixedCellConstraints(3, ACTION_COLUMN_WIDTH));

            updateValueEditor();
        }

        private JPanel createNameCell() {
            JPanel cell = createBodyCell(true, new Insets(0, 20, 0, 20));
            cell.add(nameField, BorderLayout.CENTER);
            return cell;
        }

        private JPanel createValueCell() {
            valueCell = createBodyCell(true, new Insets(0, 20, 0, 14));
            return valueCell;
        }

        private JPanel createSecretCell() {
            JPanel cell = createBodyCell(true, new Insets(0, 0, 0, 0));
            cell.add(secretCheckBox, BorderLayout.CENTER);
            return cell;
        }

        private JPanel createDeleteCell() {
            JPanel cell = createBodyCell(false, new Insets(0, 0, 0, 0));
            cell.add(deleteButton, BorderLayout.CENTER);
            return cell;
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

            valueCell.removeAll();

            if (isSecret() && !valueVisible) {
                valueCell.add(passwordField, BorderLayout.CENTER);
            } else {
                valueCell.add(valueField, BorderLayout.CENTER);
            }

            if (isSecret()) {
                visibilityButton.setText(valueVisible ? "\u25C9" : "\u25CE");
                visibilityButton.setToolTipText(valueVisible ? "Hide value" : "Show value");
                visibilityButton.setVisible(true);
                valueCell.add(visibilityButton, BorderLayout.EAST);
            } else {
                visibilityButton.setVisible(false);
            }

            valueCell.revalidate();
            valueCell.repaint();
        }

        private void syncValueFields() {
            if (valueVisible) {
                valueField.setText(new String(passwordField.getPassword()));
            } else {
                passwordField.setText(valueField.getText());
            }
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
    }

    private JButton createInlineIconButton(String text) {
        JButton button = new JButton(text);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setForeground(getSecondaryTextColor());
        button.setFont(button.getFont().deriveFont(Font.BOLD, 15f));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(34, 30));
        return button;
    }
}
