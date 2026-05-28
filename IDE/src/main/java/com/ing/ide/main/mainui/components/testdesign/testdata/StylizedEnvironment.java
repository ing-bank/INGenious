package com.ing.ide.main.mainui.components.testdesign.testdata;

import org.kordamp.ikonli.materialdesign2.*;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;

/**
 * Stylized panel for creating new test environments with modern theme styling.
 */
public class StylizedEnvironment extends JPanel {
    
    private final TestDataComponent tdProxy;
    
    // UI Components
    private JTextField envName;
    private JCheckBox copyFromOthers;
    private JComboBox<String> environments;
    private JList<String> testDataList;
    private JButton createEnv;
    private JCheckBox copyGlobalData;
    
    // Labels that need theme updates
    private JLabel titleLabel;
    private JLabel subtitleLabel;
    private JLabel envNameSectionLabel;
    private JLabel envLabel;
    private JLabel dataLabel;
    
    // Theme colors - Light mode
    private static final Color ACCENT_COLOR = new Color(119, 36, 255);
    private static final Color ACCENT_LIGHT = new Color(240, 234, 255);
    private static final Color BORDER_COLOR_LIGHT = new Color(209, 213, 219);
    private static final Color BACKGROUND_COLOR_LIGHT = new Color(250, 251, 252);
    private static final Color TEXT_COLOR_LIGHT = new Color(77, 0, 32);  // Burgundy for light mode
    private static final Color MUTED_COLOR_LIGHT = new Color(107, 114, 128);
    
    // Theme colors - Dark mode
    private static final Color BORDER_COLOR_DARK = new Color(70, 70, 80);
    private static final Color BACKGROUND_COLOR_DARK = new Color(43, 43, 43);
    private static final Color TEXT_COLOR_DARK = Color.WHITE;
    private static final Color MUTED_COLOR_DARK = new Color(156, 163, 175);
    private static final Color FIELD_BG_DARK = new Color(60, 63, 65);  // Darker field background
    
    // Theme-aware color getters
    private static boolean isDarkMode() {
        return com.ing.ide.main.Main.isDarkMode();
    }
    
    private static Color getTextColor() {
        return isDarkMode() ? TEXT_COLOR_DARK : TEXT_COLOR_LIGHT;
    }
    
    private static Color getBackgroundColor() {
        return isDarkMode() ? BACKGROUND_COLOR_DARK : BACKGROUND_COLOR_LIGHT;
    }
    
    private static Color getBorderColor() {
        return isDarkMode() ? BORDER_COLOR_DARK : BORDER_COLOR_LIGHT;
    }
    
    private static Color getMutedColor() {
        return isDarkMode() ? MUTED_COLOR_DARK : MUTED_COLOR_LIGHT;
    }
    
    private static Color getFieldBackground() {
        return isDarkMode() ? FIELD_BG_DARK : Color.WHITE;
    }
    
    public StylizedEnvironment(TestDataComponent tdProxy) {
        this.tdProxy = tdProxy;
        initComponents();
        layoutComponents();
        applyTheme();
    }
    
    @Override
    public void updateUI() {
        super.updateUI();
        // Re-apply theme colors when UI is updated (e.g., theme change)
        applyTheme();
    }
    
    private void applyTheme() {
        setBackground(getBackgroundColor());
        
        // Update labels if they exist (they won't exist during initial construction)
        if (titleLabel != null) {
            titleLabel.setForeground(getTextColor());
        }
        if (subtitleLabel != null) {
            subtitleLabel.setForeground(getMutedColor());
        }
        if (envNameSectionLabel != null) {
            envNameSectionLabel.setForeground(getTextColor());
        }
        if (envLabel != null) {
            envLabel.setForeground(getMutedColor());
        }
        if (dataLabel != null) {
            dataLabel.setForeground(getMutedColor());
        }
        if (envName != null) {
            envName.setForeground(getTextColor());
        }
        if (copyFromOthers != null) {
            copyFromOthers.setForeground(getTextColor());
        }
        if (copyGlobalData != null) {
            copyGlobalData.setForeground(getTextColor());
        }
    }
    
    private void initComponents() {
        // Environment name field
        envName = createStyledTextField("New Environment");
        envName.addActionListener(e -> createEnv.doClick());
        
        // Copy from others checkbox with icon
        copyFromOthers = createStyledCheckBox("Copy data from other Environment");
        copyFromOthers.addItemListener(e -> {
            boolean selected = copyFromOthers.isSelected();
            environments.setEnabled(selected);
            testDataList.setEnabled(selected);
            copyGlobalData.setEnabled(selected);
            if (selected) {
                environments.setModel(new DefaultComboBoxModel<>(
                    tdProxy.getListOfEnvironements().toArray(new String[0])));
                loadTestDataLists();
            }
        });
        
        // Environments dropdown
        environments = new JComboBox<>();
        environments.setEnabled(false);
        environments.setFont(UIManager.getFont("Table.font"));
        environments.addItemListener(e -> {
            if (e.getStateChange() == java.awt.event.ItemEvent.SELECTED) {
                loadTestDataLists();
            }
        });
        
        // Test data list
        testDataList = new JList<>(new DefaultListModel<>());
        testDataList.setEnabled(false);
        testDataList.setFont(UIManager.getFont("Table.font"));
        testDataList.setBackground(UIManager.getColor("List.background"));
        testDataList.setSelectionBackground(UIManager.getColor("List.selectionBackground"));
        testDataList.setSelectionForeground(UIManager.getColor("List.selectionForeground"));
        
        // Copy global data checkbox
        copyGlobalData = createStyledCheckBox("Include Global Data");
        copyGlobalData.setEnabled(false);
        
        // Create button with icon
        createEnv = createStyledButton("Create Environment", MaterialDesignP.PLUS_CIRCLE);
        createEnv.addActionListener(e -> createNewEnvironment());
    }
    
    private void layoutComponents() {
        setLayout(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Header panel with icon and title
        JPanel headerPanel = new JPanel(new BorderLayout(12, 0));
        headerPanel.setOpaque(false);
        
        FontIcon headerIcon = FontIcon.of(MaterialDesignE.EARTH_PLUS, 32, ACCENT_COLOR);
        JLabel iconLabel = new JLabel(headerIcon);
        headerPanel.add(iconLabel, BorderLayout.WEST);
        
        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        titleLabel = new JLabel("Create New Environment");
        titleLabel.setFont(UIManager.getFont("Table.font").deriveFont(Font.BOLD, 16f));
        titleLabel.setForeground(getTextColor());
        titlePanel.add(titleLabel);
        
        subtitleLabel = new JLabel("Add a new test data environment to your project");
        subtitleLabel.setFont(UIManager.getFont("Table.font").deriveFont(12f));
        subtitleLabel.setForeground(getMutedColor());
        titlePanel.add(subtitleLabel);
        
        headerPanel.add(titlePanel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);
        
        // Form panel
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        
        // Environment name section
        formPanel.add(createFormSection("Environment Name", envName));
        formPanel.add(Box.createVerticalStrut(16));
        
        // Copy options section
        JPanel copyPanel = new JPanel();
        copyPanel.setLayout(new BoxLayout(copyPanel, BoxLayout.Y_AXIS));
        copyPanel.setOpaque(false);
        copyPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getBorderColor(), 1),
            new EmptyBorder(12, 12, 12, 12)
        ));
        
        copyFromOthers.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyPanel.add(copyFromOthers);
        copyPanel.add(Box.createVerticalStrut(8));
        
        envLabel = new JLabel("Source Environment:");
        envLabel.setFont(UIManager.getFont("Table.font"));
        envLabel.setForeground(getMutedColor());
        envLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyPanel.add(envLabel);
        copyPanel.add(Box.createVerticalStrut(4));
        
        environments.setAlignmentX(Component.LEFT_ALIGNMENT);
        environments.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        copyPanel.add(environments);
        copyPanel.add(Box.createVerticalStrut(12));
        
        dataLabel = new JLabel("Test Data to Copy:");
        dataLabel.setFont(UIManager.getFont("Table.font"));
        dataLabel.setForeground(getMutedColor());
        dataLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyPanel.add(dataLabel);
        copyPanel.add(Box.createVerticalStrut(4));
        
        JScrollPane listScroll = new JScrollPane(testDataList);
        listScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        listScroll.setPreferredSize(new Dimension(200, 100));
        listScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        copyPanel.add(listScroll);
        copyPanel.add(Box.createVerticalStrut(8));
        
        copyGlobalData.setAlignmentX(Component.LEFT_ALIGNMENT);
        copyPanel.add(copyGlobalData);
        
        formPanel.add(copyPanel);
        formPanel.add(Box.createVerticalStrut(16));
        
        // Create button
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.add(createEnv);
        formPanel.add(buttonPanel);
        
        add(formPanel, BorderLayout.CENTER);
    }
    
    private JPanel createFormSection(String label, JComponent field) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIManager.getFont("Table.font").deriveFont(Font.BOLD));
        lbl.setForeground(getTextColor());
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // Store the "Environment Name" label for theme updates
        if ("Environment Name".equals(label)) {
            envNameSectionLabel = lbl;
        }
        
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(6));
        
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        if (field instanceof JTextField) {
            ((JTextField) field).setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        }
        panel.add(field);
        
        return panel;
    }
    
    private JTextField createStyledTextField(String text) {
        JTextField field = new JTextField(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getFieldBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 6, 6));
                g2.dispose();
                super.paintComponent(g);
            }
            
            @Override
            protected void paintBorder(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hasFocus() ? ACCENT_COLOR : getBorderColor());
                g2.draw(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 6, 6));
                g2.dispose();
            }
        };
        field.setFont(UIManager.getFont("Table.font"));
        field.setForeground(getTextColor());
        field.setOpaque(false);
        field.setBorder(new EmptyBorder(6, 10, 6, 10));
        field.setPreferredSize(new Dimension(200, 32));
        return field;
    }
    
    private JCheckBox createStyledCheckBox(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setFont(UIManager.getFont("Table.font"));
        cb.setForeground(getTextColor());
        cb.setOpaque(false);
        cb.setFocusPainted(false);
        return cb;
    }
    
    private JButton createStyledButton(String text, org.kordamp.ikonli.Ikon icon) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                if (getModel().isPressed()) {
                    g2.setColor(ACCENT_COLOR.darker());
                } else if (getModel().isRollover()) {
                    g2.setColor(new Color(99, 24, 224));
                } else {
                    g2.setColor(ACCENT_COLOR);
                }
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth()-1, getHeight()-1, 8, 8));
                g2.dispose();
                
                super.paintComponent(g);
            }
        };
        button.setIcon(FontIcon.of(icon, 16, Color.WHITE));
        button.setFont(UIManager.getFont("Table.font").deriveFont(Font.BOLD));
        button.setForeground(Color.WHITE);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(new EmptyBorder(10, 20, 10, 20));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return button;
    }
    
    private void loadTestDataLists() {
        DefaultListModel<String> model = (DefaultListModel<String>) testDataList.getModel();
        model.clear();
        if (environments.getSelectedItem() != null) {
            List<String> values = tdProxy.getListOfTestDatas(environments.getSelectedItem().toString());
            values.forEach(model::addElement);
        }
    }
    
    private void createNewEnvironment() {
        String name = getEnvironmentName();
        if (!name.trim().isEmpty()) {
            if (copyFromOthers.isSelected()) {
                if (tdProxy.addNewEnvironment(name,
                        environments.getSelectedItem().toString(),
                        testDataList.getSelectedValuesList(),
                        copyGlobalData.isSelected())) {
                    environments.addItem(name);
                    copyFromOthers.setSelected(false);
                }
            } else {
                tdProxy.addNewEnvironment(name, null, null, false);
            }
        }
    }
    
    private String getEnvironmentName() {
        return envName.getText();
    }
    
    public void selectTextBox() {
        envName.selectAll();
        envName.requestFocusInWindow();
    }
    
    public void reset() {
        copyFromOthers.setSelected(false);
        ((DefaultListModel<String>) testDataList.getModel()).clear();
        environments.removeAllItems();
    }
}
