package com.ing.ide.main.settings;

import com.ing.datalib.component.Project;
import com.ing.datalib.settings.ProjectSettings;
import com.ing.datalib.settings.emulators.Emulator;
import com.ing.datalib.util.data.LinkedProperties;
import com.ing.engine.drivers.PlaywrightDriverFactory;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.table.XTable;
import com.ing.ide.util.Notification;
import com.ing.ide.util.Utility;
import java.awt.Color;
import java.awt.Toolkit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import com.ing.ide.main.fx.INGIcons;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import javax.swing.table.DefaultTableModel;

/**
 *
 *
 */
public class DriverSettings extends javax.swing.JFrame {

    // Theme-aware color constants
    private static final Color DARK_BG = new Color(30, 26, 36);
    private static final Color DARK_PANEL_BG = new Color(37, 32, 48);
    private static final Color DARK_BORDER = new Color(60, 50, 80);
    private static final Color DARK_INPUT_BG = new Color(45, 40, 55);
    private static final Color DARK_TEXT = new Color(232, 226, 229);
    private static final Color DARK_LABEL = new Color(200, 195, 210);
    private static final Color ING_ORANGE_DARK = new Color(255, 102, 0);
    private static final Color ING_PURPLE = new Color(119, 36, 255);
    private static final Color ING_BURGUNDY = Color.decode("#4D0020");
    private static final Color WARM_BG = Color.decode("#FAFAF8");
    private static final Color PURPLE_VERY_LIGHT = Color.decode("#F5F0FF");
    private static final Color PURPLE_LIGHT = Color.decode("#E5D6FF");

    private final AppMainFrame sMainFrame;
    Project sProject;
    ProjectSettings settings;
    private SaveSettingsListeners saveSettingsListeners;
    private boolean isAddingEmulator = false;

    /**
     * Creates new form NewJFrame
     *
     * @param sMainFrame
     */
    public DriverSettings(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
        initComponents();

        setIconImage(com.ing.ide.main.fx.INGIcons.toImage(Utils.getIconByResourceName("/ui/resources/main/BrowserConfiguration")));

        //loadChromeEmulators();
        initAddEmulatorListener();
        initAddNewDBListener();
        initAddNewContextListener();
        initAddNewDBAPIListener();

        final JTextField resolutionText = new JTextField();
        //final JTextField resolutionText = (JTextField) resolution.getEditor().getEditorComponent();
        resolutionText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent ke) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        resFilter();
                    }
                });
            }
        });
        final JTextField brText = (JTextField) browserCombo.getEditor().getEditorComponent();
        brText.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent ke) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        brFilter();
                    }
                });
            }
        });
        
        applyThemeStyles();
    }
    
    // Theme-aware color getters
    private boolean isDarkMode() {
        return com.ing.ide.main.Main.isDarkMode();
    }
    
    private Color getBgColor() {
        return isDarkMode() ? DARK_BG : WARM_BG;
    }
    
    private Color getPanelBgColor() {
        return isDarkMode() ? DARK_PANEL_BG : PURPLE_VERY_LIGHT;
    }
    
    private Color getBorderColor() {
        return isDarkMode() ? DARK_BORDER : PURPLE_LIGHT;
    }
    
    private Color getInputBgColor() {
        return isDarkMode() ? DARK_INPUT_BG : Color.WHITE;
    }
    
    private Color getTextColor() {
        return isDarkMode() ? DARK_TEXT : ING_BURGUNDY;
    }
    
    private Color getLabelColor() {
        return isDarkMode() ? DARK_LABEL : ING_BURGUNDY;
    }
    
    private Color getAccentColor() {
        return isDarkMode() ? ING_ORANGE_DARK : ING_PURPLE;
    }
    
    /**
     * Apply theme-aware styling to the window.
     */
    private void applyThemeStyles() {
        Color bgColor = getBgColor();
        Color panelBgColor = getPanelBgColor();
        Color borderColor = getBorderColor();
        Color textColor = getTextColor();
        Color inputBgColor = getInputBgColor();
        
        // Style main frame
        this.setBackground(bgColor);
        getContentPane().setBackground(bgColor);
        if (getContentPane() instanceof JComponent) {
            ((JComponent) getContentPane()).setOpaque(true);
        }
        
        // Style the tabbed pane
        if (mainTab != null) {
            mainTab.setBackground(bgColor);
            mainTab.setForeground(textColor);
            mainTab.setOpaque(true);
            mainTab.putClientProperty("JTabbedPane.tabAreaAlignment", "leading");
            mainTab.putClientProperty("JTabbedPane.showTabSeparators", false);
            mainTab.putClientProperty("JTabbedPane.tabsOpaque", true);
            mainTab.putClientProperty("JTabbedPane.contentOpaque", true);
            mainTab.putClientProperty("JTabbedPane.tabAreaBackground", panelBgColor);
        }
        
        // Style save panel (jPanel1)
        if (jPanel1 != null) {
            jPanel1.setBackground(panelBgColor);
            jPanel1.setOpaque(true);
            jPanel1.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, borderColor),
                new EmptyBorder(8, 16, 8, 16)));
        }
        
        // Style buttons
        styleButton(saveSettings, true);
        styleButton(resetSettings, false);
        
        // Recursively style all components
        styleComponentsRecursively(getContentPane());
        
        revalidate();
        repaint();
    }
    
    private void styleButton(javax.swing.JButton button, boolean primary) {
        if (button == null) return;
        Color buttonBg = primary ? getAccentColor() : getInputBgColor();
        Color buttonFg = primary ? Color.WHITE : getAccentColor();
        button.setBackground(buttonBg);
        button.setForeground(buttonFg);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(getAccentColor(), 1),
            new EmptyBorder(8, 20, 8, 20)));
    }
    
    private void styleComponentsRecursively(java.awt.Container container) {
        if (container == null) return;
        
        Color bgColor = getBgColor();
        Color textColor = getTextColor();
        Color inputBgColor = getInputBgColor();
        Color borderColor = getBorderColor();
        
        for (java.awt.Component comp : container.getComponents()) {
            if (comp instanceof javax.swing.JPanel) {
                ((javax.swing.JPanel) comp).setBackground(bgColor);
                ((javax.swing.JPanel) comp).setOpaque(true);
            }
            if (comp instanceof javax.swing.JScrollPane) {
                javax.swing.JScrollPane sp = (javax.swing.JScrollPane) comp;
                sp.setBackground(bgColor);
                sp.setOpaque(true);
                sp.getViewport().setBackground(inputBgColor);
                sp.getViewport().setOpaque(true);
                sp.setBorder(BorderFactory.createLineBorder(borderColor, 1));
            }
            if (comp instanceof javax.swing.JLabel) {
                ((javax.swing.JLabel) comp).setForeground(textColor);
            }
            if (comp instanceof javax.swing.JToolBar) {
                ((javax.swing.JToolBar) comp).setBackground(getPanelBgColor());
                ((javax.swing.JToolBar) comp).setOpaque(true);
            }
            if (comp instanceof javax.swing.JTable) {
                javax.swing.JTable table = (javax.swing.JTable) comp;
                table.setBackground(inputBgColor);
                table.setForeground(textColor);
                table.setGridColor(borderColor);
                table.setSelectionBackground(getAccentColor());
                table.setSelectionForeground(Color.WHITE);
                if (table.getTableHeader() != null) {
                    table.getTableHeader().setBackground(getPanelBgColor());
                    table.getTableHeader().setForeground(textColor);
                }
            }
            if (comp instanceof javax.swing.JComboBox) {
                ((javax.swing.JComboBox<?>) comp).setBackground(inputBgColor);
                ((javax.swing.JComboBox<?>) comp).setForeground(textColor);
            }
            if (comp instanceof javax.swing.JTextField) {
                javax.swing.JTextField tf = (javax.swing.JTextField) comp;
                tf.setBackground(inputBgColor);
                tf.setForeground(textColor);
                tf.setCaretColor(getAccentColor());
            }
            if (comp instanceof javax.swing.JCheckBox) {
                javax.swing.JCheckBox cb = (javax.swing.JCheckBox) comp;
                cb.setForeground(textColor);
                cb.setOpaque(false);
            }
            if (comp instanceof javax.swing.JRadioButton) {
                javax.swing.JRadioButton rb = (javax.swing.JRadioButton) comp;
                rb.setForeground(textColor);
                rb.setOpaque(false);
            }
            if (comp instanceof java.awt.Container) {
                styleComponentsRecursively((java.awt.Container) comp);
            }
        }
    }

    private void initAddEmulatorListener() {
        browserCombo.getEditor().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                addNewEmulator();
                saveSettings.setEnabled(true);
            }
        });
    }

    private void initAddNewDBListener() {
        dbCombo.getEditor().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                addNewDB();
                saveSettings.setEnabled(true);
            }
        });
    }

    private void initAddNewContextListener() {
        contextCombo.getEditor().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                addNewContext();
                saveSettings.setEnabled(true);
            }
        });
    }
    
    private void initAddNewDBAPIListener() {
        apiCombo.getEditor().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                addNewAPI();
                saveSettings.setEnabled(true);
            }
        });
    }

    public void load() {
        this.sProject = sMainFrame.getProject();
        settings = sProject.getProjectSettings();
        loadSettings();
    }

    private void loadSettings() {
        loadDriverPropTable();
        loadBrowsers();
        loadDatabases();
        loadContexts();
        loadAPI();;
    }

    private void loadDriverPropTable() {
        DefaultTableModel model = (DefaultTableModel) driverPropTable.getModel();
        model.setRowCount(0);
        for (Object key : settings.getDriverSettings().orderedKeys()) {
            Object value = settings.getDriverSettings().get(key);
            model.addRow(new Object[]{key, value});
        }
    }

    //Initializes the list of available API configurations into the combo box and selects the default.
    private void loadAPI() {
        apiCombo.setModel(new DefaultComboBoxModel(getAPIList().toArray()));
        apiCombo.setSelectedItem("default");
        checkAndLoadApi();
    }

    // Retrieves a list of API alias from the backend configurations
    private List<String> getAPIList() {
        List<String> list = settings.getDriverSettings().getAPIList();
        return list;
    }

    private void loadBrowsers() {
        DefaultComboBoxModel model = new DefaultComboBoxModel(getTotalBrowserList().toArray());
        browserCombo.setModel(model);
//        dupDriverCombo.setModel(new DefaultComboBoxModel(
//                getTotalBrowserList().toArray()));
        browserCombo.setSelectedItem(getTotalBrowserList().get(0));
        checkAndLoadCapabilities();
    }

    private void loadDatabases() {

        dbCombo.setModel(new DefaultComboBoxModel(getTotalDBList().toArray()));
        dbCombo.setSelectedItem("default");
        checkAndLoadDatabases();
    }

    private void loadContexts() {
        contextCombo.setModel(new DefaultComboBoxModel(getTotalContextList().toArray()));
        contextCombo.setSelectedItem("default");
        checkAndLoadContexts();
    }

    private List<String> getTotalDBList() {
        List<String> list = settings.getDatabaseSettings().getDbList();
        return list;
    }

    private List<String> getTotalContextList() {
        List<String> list = settings.getContextSettings().getContextList();
        return list;
    }

    private List<String> getTotalBrowserList() {
        List<String> list = PlaywrightDriverFactory.Browser.getValuesAsList();
        List<String> list2 = settings.getEmulators().getEmulatorNames();
        list.addAll(list2);
        return list;
    }

    // Loads the API configurations based on the selected API alias API combo box
    private void checkAndLoadApi() {
        String apiName = apiCombo.getSelectedItem().toString();
        if (settings.getDriverSettings().getAPIList() != null) {
            deleteAPIConfig.setEnabled(true);
            loadAPIConfiguration(apiName);
        } else {
            deleteAPIConfig.setEnabled(false);
        }
        loadAPIConfiguration(apiName);
    }

    private void checkAndLoadCapabilities() {
        String selBrowser = browserCombo.getSelectedItem().toString();
        Emulator emulator = settings.getEmulators().getEmulator(selBrowser);
        if (emulator == null) {
            emCapTab.setEnabledAt(0, false);
            editEmulator.setEnabled(false);
            deleteEmulator.setEnabled(false);
            emCapTab.setSelectedIndex(1);
        } else {
            emCapTab.setEnabledAt(0, true);
            editEmulator.setEnabled(true);
            deleteEmulator.setEnabled(true);
            emCapTab.setSelectedIndex(0);
            loadEmulator(emulator);
        }
        loadCapabilities(selBrowser);
    }

    private void checkAndLoadDatabases() {
        String dbName = dbCombo.getSelectedItem().toString();
        if (settings.getDatabaseSettings().getDbList() != null) {
            deleteDB.setEnabled(true);
            loadDB(dbName);
        } else {
            deleteDB.setEnabled(false);
        }
        loadDB(dbName);
    }

    private void checkAndLoadContexts() {
        String contextName = contextCombo.getSelectedItem().toString();
        if (settings.getContextSettings().getContextList() != null) {
            deleteContext.setEnabled(true);
            loadContext(contextName);
        } else {
            deleteContext.setEnabled(false);
        }
        loadContext(contextName);
    }

    // Loads the API configurations on the Table Model
    private void loadAPIConfiguration(String apiName) {
        DefaultTableModel model = (DefaultTableModel) driverPropTable.getModel();
        model.setRowCount(0);
        Properties prop = settings.getDriverSettings().getAPIPropertiesFor(apiName);
        if (prop != null) {
            for (Object key : prop.keySet()) {
                Object value = prop.get(key);
                model.addRow(new Object[]{key, value});
            }
        }
    }

    private void loadCapabilities(String browserName) {
        DefaultTableModel model = (DefaultTableModel) capTable.getModel();
        model.setRowCount(0);
        LinkedProperties prop = settings.getCapabilities().getCapabiltiesFor(browserName);
        if (prop != null) {
            for (Object key : prop.orderedKeys()) {
                Object value = prop.get(key);
                model.addRow(new Object[]{key, value});
            }
        } else {
            if(!browserName.equals("No Browser")){
                addDefaultCapsNewEmulator();
            }
        }
    }

    private void loadDB(String dbName) {
        DefaultTableModel model = (DefaultTableModel) dbPropTable.getModel();
        model.setRowCount(0);
        Properties prop = settings.getDatabaseSettings().getDBPropertiesFor(dbName);
        if (prop != null) {
            for (Object key : prop.keySet()) {
                Object value = prop.get(key);
                model.addRow(new Object[]{key, value});
            }
        }
    }

    private void loadContext(String contextName) {
        DefaultTableModel model = (DefaultTableModel) contextPropTable.getModel();
        model.setRowCount(0);
        Properties prop = settings.getContextSettings().getContextOptionsFor(contextName);

        if (prop != null) {
            for (Object key : prop.keySet()) {
                Object value = prop.get(key);
                model.addRow(new Object[]{key, value});
            }
        }
    }

    private void loadEmulator(Emulator emulator) {
        String type = emulator.getType();
        setButtonGroup(type, customDeviceGroup);
        switch (type) {
            case "Remote URL":
                appiumConnectionString.setText(emulator.getRemoteUrl());
                break;
        }
    }

    private void setButtonGroup(String rdValue, ButtonGroup buttongroup) {
        Enumeration enumeration = buttongroup.getElements();
        while (enumeration.hasMoreElements()) {
            AbstractButton button = (AbstractButton) enumeration.nextElement();
            if (button.getActionCommand().equals(rdValue)) {
                buttongroup.setSelected(button.getModel(), true);
                break;
            }
        }
    }

    public void open() {
        loadBrowsers();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void addNewEmulator() {
        String newEmName = browserCombo.getEditor().getItem().toString();
        if (!getTotalBrowserList().contains(newEmName)) {
            isAddingEmulator = true;
            saveSettings.setEnabled(true);
            settings.getEmulators().addEmulator(newEmName);
            browserCombo.addItem(newEmName);
            //dupDriverCombo.addItem(newEmName);
            browserCombo.setSelectedItem(newEmName);
            addDefaultCapsNewEmulator();
            
            emCapTab.setEnabledAt(0, true);
            editEmulator.setEnabled(true);
            deleteEmulator.setEnabled(true);
            emCapTab.setSelectedIndex(0);
            appiumEmulator.setSelected(true);
            appiumConnectionString.setEnabled(true);
            appiumConnectionString.setText("http://127.0.0.1:4723/");
            
            isAddingEmulator = false;
        } else {
            Notification.show("Emulator/Browser [" + newEmName + "] already Present");
            isAddingEmulator = false;
        }
    }
    
    private void addDefaultCapsNewEmulator(){
            DefaultTableModel model = (DefaultTableModel) capTable.getModel();
            model.setRowCount(0);
            
            LinkedProperties properties = settings.getEmulators().defaultEmulatorCap();
            for (Object key : properties.orderedKeys()) {
                Object value = properties.get(key);
                model.addRow(new Object[]{key, value});
            }
    }

    private void renameEmulator() {
        String oldName = browserCombo.getSelectedItem().toString();
        String newEmName = browserCombo.getEditor().getItem().toString();
        if (!oldName.equals(newEmName)) {
            if (!getTotalBrowserList().contains(newEmName)) {
                Emulator emulator = settings.getEmulators().getEmulator(oldName);
                emulator.setName(newEmName);
                DefaultComboBoxModel combomodel = (DefaultComboBoxModel) browserCombo.getModel();
                //  DefaultComboBoxModel dupCombomodel = (DefaultComboBoxModel) dupDriverCombo.getModel();
                int index = browserCombo.getSelectedIndex();
                combomodel.removeElement(oldName);
                //   dupCombomodel.removeElement(oldName);
                combomodel.insertElementAt(newEmName, index);
                //  dupCombomodel.insertElementAt(newEmName, index);
                browserCombo.setSelectedIndex(index);
            } else {
                Notification.show("Emulator/Browser [" + newEmName + "] already Present");
            }
        }
    }

    private void deleteEmulator() {
        String emName = browserCombo.getSelectedItem().toString();
        Emulator emulator = settings.getEmulators().getEmulator(emName);
        if (emulator != null) {
            settings.getEmulators().deleteEmulator(emName);
            browserCombo.removeItem(emName);
            //  dupDriverCombo.removeItem(emName);
        } else {

        }
    }

    private void saveSettings() {
        if (mainTab.getSelectedIndex() == 0) {
            saveContextProperties();
        } else if(mainTab.getSelectedIndex() == 1){
            saveDBProperties();
        } else if(mainTab.getSelectedIndex() == 2){
            saveCommonSettings();
        } else if (emCapTab.getSelectedIndex() == 0) {
            saveEmulator();
            settings.getEmulators().save();
            saveCapabilities();
        } else {
            if(emCapTab.isEnabledAt(0)){
                saveEmulator();
            }
            settings.getEmulators().save();
            saveCapabilities();
        }
    }

    private void saveEmulator() {
        settings.getEmulators().addEmulator(browserCombo.getSelectedItem().toString());
        Emulator emulator = settings.getEmulators().
                getEmulator(browserCombo.getSelectedItem().toString());
        emulator.setType(customDeviceGroup.getSelection().getActionCommand());
        switch (emulator.getType()) {
            case "Remote URL":
                emulator.setRemoteUrl(appiumConnectionString.getText());
                break;
        }
    }

    private void saveCapabilities() {
        if (capTable.isEditing()) {
            capTable.getCellEditor().stopCellEditing();
        }
        DefaultTableModel model = (DefaultTableModel) capTable.getModel();
        LinkedProperties properties = new LinkedProperties();
        for (int i = 0; i < model.getRowCount(); i++) {
            String prop = Objects.toString(model.getValueAt(i, 0), "").trim();
            if (!prop.isEmpty()) {
                String value = Objects.toString(model.getValueAt(i, 1), "");
                properties.setProperty(prop, value);
            }
        }
        settings.getCapabilities().addCapability(browserCombo.getSelectedItem().toString(),
                properties);
    }

    private void saveCommonSettings() {
        if(apiCombo.getSelectedIndex() != -1) {
            if (driverPropTable.isEditing()) {
                driverPropTable.getCellEditor().stopCellEditing();
            }
            //Properties driveProps = encryptpassword(PropUtils.getPropertiesFromTable(driverPropTable));
            Properties driveProps = PropUtils.getPropertiesFromTable(driverPropTable);
            PropUtils.loadPropertiesInTable(driveProps, driverPropTable, "");

            DefaultTableModel model = (DefaultTableModel) driverPropTable.getModel();
            settings.getDriverSettings().clear();
            LinkedProperties properties = new LinkedProperties();
            for (int i = 0; i < model.getRowCount(); i++) {
                String prop = Objects.toString(model.getValueAt(i, 0), "").trim();
                if (!prop.isEmpty()) {
                    String value = Objects.toString(model.getValueAt(i, 1), "");
                    settings.getDriverSettings().setProperty(prop, value);
                    properties.setProperty(prop, value);
                }
            }
            settings.getDriverSettings().addAPI(apiCombo.getSelectedItem().toString(), properties);
        }
    }

    private void saveDBProperties() {
        if (dbCombo.getSelectedIndex() != -1) {
            if (dbPropTable.isEditing()) {
                dbPropTable.getCellEditor().stopCellEditing();
            }

            //Properties driveProps = encryptpassword(PropUtils.getPropertiesFromTable(dbPropTable));
            Properties driveProps = PropUtils.getPropertiesFromTable(dbPropTable);
            PropUtils.loadPropertiesInTable(driveProps, dbPropTable, "");

            DefaultTableModel model = (DefaultTableModel) dbPropTable.getModel();
            LinkedProperties properties = new LinkedProperties();
            for (int i = 0; i < model.getRowCount(); i++) {
                String prop = Objects.toString(model.getValueAt(i, 0), "").trim();
                if (!prop.isEmpty()) {
                    String value = Objects.toString(model.getValueAt(i, 1), "");
                    properties.setProperty(prop, value);
                }
            }
            settings.getDatabaseSettings().addDB(dbCombo.getSelectedItem().toString(), properties);
        }
    }

    private void saveContextProperties() {
        if (contextCombo.getSelectedIndex() != -1) {
            if (contextPropTable.isEditing()) {
                contextPropTable.getCellEditor().stopCellEditing();
            }

            //Properties contextProps = encryptpassword(PropUtils.getPropertiesFromTable(contextPropTable));
            Properties contextProps = PropUtils.getPropertiesFromTable(contextPropTable);
            PropUtils.loadPropertiesInTable(contextProps, contextPropTable, "");

            DefaultTableModel model = (DefaultTableModel) contextPropTable.getModel();
            LinkedProperties properties = new LinkedProperties();
            for (int i = 0; i < model.getRowCount(); i++) {
                String prop = Objects.toString(model.getValueAt(i, 0), "").trim();
                if (!prop.isEmpty()) {
                    String value = Objects.toString(model.getValueAt(i, 1), "");
                    properties.setProperty(prop, value);
                }
            }
            settings.getContextSettings().addContext(contextCombo.getSelectedItem().toString(), properties);
        }
    }

    private Properties encryptpassword(Properties properties) {
        properties.entrySet().forEach((e) -> {
            String key = (String) e.getKey();
            String value = (String) e.getValue();
            if (value != null && !value.isEmpty()) {
                if (key.toLowerCase().contains("passw") && !isDatasheetOrVariable(value)) {
                    properties.setProperty(key, Utility.encrypt(value));
                }
            }
        });
        return properties;
    }

    private Boolean isDatasheetOrVariable(String value) {
        String pattern1 = "\\{[^:]+:[^}]+\\}";
        String pattern2 = "%[^%]+%";
        Pattern regex1 = Pattern.compile(pattern1);
        Pattern regex2 = Pattern.compile(pattern2);
        Matcher matcher1 = regex1.matcher(value);
        Matcher matcher2 = regex2.matcher(value);
        return  matcher1.matches() || matcher2.matches();
    }

    private void resFilter() {
//        if (resolution.getModel().getSize() > 0) {
//            resolution.showPopup();
//        } else {
//            resolution.hidePopup();
//        }
    }

    private void brFilter() {
        if (browserCombo.getModel().getSize() > 0) {
            browserCombo.showPopup();
        } else {
            browserCombo.hidePopup();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        customDeviceGroup = new javax.swing.ButtonGroup();
        emulatorGroup = new javax.swing.ButtonGroup();
        mainTab = new javax.swing.JTabbedPane();
        commonPanel = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        driverPropTable = new XTable();
        jToolBar1 = new javax.swing.JToolBar();
        apiJLabel = new javax.swing.JLabel();
        apiCombo = new javax.swing.JComboBox<>();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        addNewAPIConfig = new javax.swing.JButton();
        deleteAPIConfig = new javax.swing.JButton();
        addPropButton = new javax.swing.JButton();
        removePropButton = new javax.swing.JButton();
        browserPanel = new javax.swing.JPanel();
        jToolBar3 = new javax.swing.JToolBar();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        jLabel2 = new javax.swing.JLabel();
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        browserCombo = new javax.swing.JComboBox<>();
        filler7 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        editEmulator = new javax.swing.JButton();
        deleteEmulator = new javax.swing.JButton();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        jPanel2 = new javax.swing.JPanel();
        emCapTab = new javax.swing.JTabbedPane();
        emulatorPanel = new javax.swing.JPanel();
        appiumConnectionString = new javax.swing.JTextField();
        alterDefaultKeyBindings();
        appiumEmulator = new javax.swing.JRadioButton();
        capabilityPanel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        capTable = new XTable();
        jToolBar2 = new javax.swing.JToolBar();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        jSeparator1 = new javax.swing.JToolBar.Separator();
        addCap = new javax.swing.JButton();
        removeCap = new javax.swing.JButton();
        filler8 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 10), new java.awt.Dimension(0, 10), new java.awt.Dimension(32767, 10));
        jPanel1 = new javax.swing.JPanel();
        saveSettings = new javax.swing.JButton();
        resetSettings = new javax.swing.JButton();
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 10), new java.awt.Dimension(0, 10), new java.awt.Dimension(32767, 10));

        databasePanel = new javax.swing.JPanel();
        dbCombo = new javax.swing.JComboBox<>();
        addNewDB = new javax.swing.JButton();
        deleteDB = new javax.swing.JButton();
        addDBPropbutton = new javax.swing.JButton();
        deleteDBPropbutton = new javax.swing.JButton();
        dbPropTable = new XTable();
        contextCombo = new javax.swing.JComboBox<>();
        contextjLabel = new javax.swing.JLabel();
        contextJToolBar = new javax.swing.JToolBar();
        addNewContext = new javax.swing.JButton();
        deleteContext = new javax.swing.JButton();
        contextPropTable = new XTable();
        contextPanel = new javax.swing.JPanel();
        addContextPropButton = new javax.swing.JButton();
        removeContextPropButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jToolBar5 = new javax.swing.JToolBar();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        jScrollPane4 = new javax.swing.JScrollPane();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jPanel6 = new javax.swing.JPanel();
        filler9 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        filler10 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        filler11 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        filler14 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        filler16 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        filler18 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        filler19 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        filler20 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        filler21 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));

        mainTab.addTab("Launch Configurations", browserPanel);
        mainTab.addTab("Context Configurations", contextPanel);
        mainTab.addTab("API Configurations", commonPanel);
        mainTab.addTab("Database Configurations", databasePanel);

        browserPanel.setLayout(new java.awt.BorderLayout());
        contextPanel.setLayout(new java.awt.BorderLayout());
        commonPanel.setLayout(new java.awt.BorderLayout());
        databasePanel.setLayout(new java.awt.BorderLayout());


        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Configurations");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowActivated(java.awt.event.WindowEvent evt) {
                formWindowActivated(evt);
            }
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        commonPanel.setLayout(new java.awt.BorderLayout());

        driverPropTable.setModel(new javax.swing.table.DefaultTableModel(
                new Object [][] {
                        {null, null},
                        {null, null},
                        {null, null},
                        {null, null}
                },
                new String [] {
                        "Property", "Value"
                }
        ));
        jScrollPane3.setViewportView(driverPropTable);

        commonPanel.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        //Setting up API configurations window
        jToolBar1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jToolBar1.setRollover(true);
        jToolBar1.add(filler21);
        apiJLabel.setText("API Alias");
        jToolBar1.add(apiJLabel);
        jToolBar1.add(filler19);
        jToolBar1.add(apiCombo);
        jToolBar1.add(filler20);

        apiCombo.setEditable(true);
        apiCombo.setMinimumSize(new java.awt.Dimension(150, 26));
        apiCombo.setPreferredSize(new java.awt.Dimension(150, 26));
        apiCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                apiComboItemStateChanged(evt);
            }
        });
        
        addNewAPIConfig.setIcon(INGIcons.swingColored("icon.addIcon", 16));
        addNewAPIConfig.setToolTipText("Add New Context");
        addNewAPIConfig.setFocusable(false);
        addNewAPIConfig.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addNewAPIConfig.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addNewAPIConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addNewAPIActionPerformed(evt);
            }
        });
        jToolBar1.add(addNewAPIConfig);

        deleteAPIConfig.setIcon(INGIcons.swingColored("icon.deleteIcon", 16));
        deleteAPIConfig.setToolTipText("Delete Context");
        deleteAPIConfig.setFocusable(false);
        deleteAPIConfig.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteAPIConfig.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteAPIConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteAPIActionPerformed(evt);
            }
        });
        jToolBar1.add(deleteAPIConfig);

        addPropButton.setIcon(INGIcons.swingColored("icon.add", 16));
        addPropButton.setToolTipText("Add Property");
        addPropButton.setFocusable(false);
        addPropButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addPropButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addPropButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addPropButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(addPropButton);

        removePropButton.setIcon(INGIcons.swingColored("icon.remove", 16));
        removePropButton.setToolTipText("Remove Property");
        removePropButton.setFocusable(false);
        removePropButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        removePropButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        removePropButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removePropButtonActionPerformed(evt);
            }
        });
        jToolBar1.add(removePropButton);

        commonPanel.add(jToolBar1, java.awt.BorderLayout.PAGE_START);

        mainTab.addTab("API Configurations", commonPanel);

        browserPanel.setLayout(new java.awt.BorderLayout());

        jToolBar3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jToolBar3.setRollover(true);
        jToolBar3.setMinimumSize(new java.awt.Dimension(357, 50));
        jToolBar3.setPreferredSize(new java.awt.Dimension(100, 50));
        jToolBar3.add(filler3);

        jLabel2.setText("Browser");
        jToolBar3.add(jLabel2);
        jToolBar3.add(filler6);

        browserCombo.setEditable(true);
        browserCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        browserCombo.setMinimumSize(new java.awt.Dimension(150, 26));
        browserCombo.setPreferredSize(new java.awt.Dimension(150, 26));
        browserCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                browserComboItemStateChanged(evt);
            }
        });
        jToolBar3.add(browserCombo);
        jToolBar3.add(filler7);

        editEmulator.setToolTipText("Rename Emulator");
        editEmulator.setContentAreaFilled(false);
        editEmulator.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        editEmulator.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        editEmulator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editEmulatorActionPerformed(evt);
            }
        });
        jToolBar3.add(editEmulator);

        deleteEmulator.setToolTipText("Remove Emulator");
        deleteEmulator.setContentAreaFilled(false);
        deleteEmulator.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteEmulator.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteEmulator.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteEmulatorActionPerformed(evt);
            }
        });
        jToolBar3.add(deleteEmulator);
        jToolBar3.add(filler4);

        browserPanel.add(jToolBar3, java.awt.BorderLayout.PAGE_START);

        jPanel2.setLayout(new java.awt.BorderLayout());

        emulatorPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        appiumConnectionString.setText("http://127.0.0.1:4723/");
        appiumConnectionString.setEnabled(false);

        customDeviceGroup.add(appiumEmulator);
        appiumEmulator.setText("Remote URL/Appium");
        appiumEmulator.setActionCommand("Remote URL");
        appiumEmulator.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                appiumEmulatorItemStateChanged(evt);
            }
        });

        javax.swing.GroupLayout emulatorPanelLayout = new javax.swing.GroupLayout(emulatorPanel);
        emulatorPanel.setLayout(emulatorPanelLayout);
        emulatorPanelLayout.setHorizontalGroup(
                emulatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(emulatorPanelLayout.createSequentialGroup()
                                .addGap(17, 17, 17)
                                .addGroup(emulatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(appiumEmulator)
                                        .addGroup(emulatorPanelLayout.createSequentialGroup()
                                                .addGap(24, 24, 24)
                                                .addComponent(appiumConnectionString, javax.swing.GroupLayout.PREFERRED_SIZE, 383, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(20, Short.MAX_VALUE))
        );
        emulatorPanelLayout.setVerticalGroup(
                emulatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(emulatorPanelLayout.createSequentialGroup()
                                .addGap(41, 41, 41)
                                .addComponent(appiumEmulator)
                                .addGap(18, 18, 18)
                                .addComponent(appiumConnectionString, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(352, Short.MAX_VALUE))
        );

        emCapTab.addTab("Mobile", emulatorPanel);

        capabilityPanel.setLayout(new java.awt.BorderLayout());

        capTable.setModel(new javax.swing.table.DefaultTableModel(
                new Object [][] {

                },
                new String [] {
                        "Property", "Value"
                }
        ));
        jScrollPane1.setViewportView(capTable);

        capabilityPanel.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jToolBar2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jToolBar2.setRollover(true);
        jToolBar2.add(filler2);
        jToolBar2.add(jSeparator1);

        addCap.setIcon(INGIcons.swingColored("icon.add", 16));
        addCap.setToolTipText("Add Capability");
        addCap.setFocusable(false);
        addCap.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addCap.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addCap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addCapActionPerformed(evt);
            }
        });
        jToolBar2.add(addCap);

        removeCap.setIcon(INGIcons.swingColored("icon.remove", 16));
        removeCap.setToolTipText("Remove Capability");
        removeCap.setFocusable(false);
        removeCap.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        removeCap.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        removeCap.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeCapActionPerformed(evt);
            }
        });
        jToolBar2.add(removeCap);

        capabilityPanel.add(jToolBar2, java.awt.BorderLayout.PAGE_START);

        emCapTab.addTab("Capabilities/Options", capabilityPanel);

        jPanel2.add(emCapTab, java.awt.BorderLayout.CENTER);
        emCapTab.getAccessibleContext().setAccessibleName("Mobile");

        jPanel2.add(filler8, java.awt.BorderLayout.PAGE_START);

        browserPanel.add(jPanel2, java.awt.BorderLayout.CENTER);

        mainTab.addTab("Manage Browsers", browserPanel);
        mainTab.setFont(UIManager.getFont("Table.font"));

        jToolBar5.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jToolBar5.setRollover(true);
        jToolBar5.add(filler9);
        jToolBar5.add(jSeparator2);

        jLabel1.setText("Database Alias");
        jToolBar5.add(jLabel1);
        jToolBar5.add(filler10);
        jToolBar5.add(dbCombo);
        jToolBar5.add(filler11);

        dbCombo.setEditable(true);
        //dbCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"No Database"}));
        dbCombo.setMinimumSize(new java.awt.Dimension(150, 26));
        dbCombo.setPreferredSize(new java.awt.Dimension(150, 26));
        dbCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                dbComboItemStateChanged(evt);
            }
        });



       /* testConn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/bulb_yellow.png")));
        testConn.setText("Test Connection");
        testConn.setFocusable(false);
        testConn.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
       testConn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
               testConnActionPerformed(evt);
            }
        });
        jToolBar5.add(testConn);*/


        addNewDB.setIcon(INGIcons.swingColored("icon.addIcon", 16));
        addNewDB.setToolTipText("Add New Database");
        addNewDB.setFocusable(false);
        addNewDB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addNewDB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addNewDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addNewDBActionPerformed(evt);
            }
        });
        jToolBar5.add(addNewDB);

        deleteDB.setIcon(INGIcons.swingColored("icon.deleteIcon", 16));
        deleteDB.setToolTipText("Delete Database");
        deleteDB.setFocusable(false);
        deleteDB.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteDB.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteDB.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteDBActionPerformed(evt);
            }
        });
        jToolBar5.add(deleteDB);

        // For adding Database Property
        addDBPropbutton.setIcon(INGIcons.swingColored("icon.add", 16));
        addDBPropbutton.setToolTipText("Add Property");
        addDBPropbutton.setFocusable(false);
        addDBPropbutton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addDBPropbutton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addDBPropbutton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDBPropButtonActionPerformed(evt);
            }
        });
        jToolBar5.add(addDBPropbutton);

        //For deleting database property
        deleteDBPropbutton.setIcon(INGIcons.swingColored("icon.remove", 16));
        deleteDBPropbutton.setToolTipText("Remove Property");
        deleteDBPropbutton.setFocusable(false);
        deleteDBPropbutton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteDBPropbutton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteDBPropbutton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {

                removeDBPropButtonActionPerformed(evt);
            }
        });
        jToolBar5.add(deleteDBPropbutton);


        dbPropTable.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{
                        "Property", "Value"
                }
        ) {
            boolean[] canEdit = new boolean[]{
                    true, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        jScrollPane4.setViewportView(dbPropTable);

        dbPropTable.setMinimumSize(new java.awt.Dimension(30, 120));
        dbPropTable.setOpaque(false);
        dbPropTable.setPreferredSize(new java.awt.Dimension(150, 120));
        databasePanel.add(jScrollPane4, java.awt.BorderLayout.CENTER);


        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
                jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 652, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
                jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, 557, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout databasePanelLayout = new javax.swing.GroupLayout(databasePanel);
        databasePanel.setLayout(databasePanelLayout);
        databasePanelLayout.setHorizontalGroup(
                databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jToolBar5, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        databasePanelLayout.setVerticalGroup(
                databasePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(databasePanelLayout.createSequentialGroup()
                                .addComponent(jToolBar5, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );


        contextJToolBar.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        contextJToolBar.setRollover(true);
        contextJToolBar.add(filler18);

        contextjLabel.setText("Context Alias");
        contextJToolBar.add(contextjLabel);
        contextJToolBar.add(filler14);
        contextJToolBar.add(contextCombo);
        contextJToolBar.add(filler16);

        contextCombo.setEditable(true);
        //contextCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[]{"Item 1", "Item 2", "Item 3", "Item 4"}));
        contextCombo.setMinimumSize(new java.awt.Dimension(150, 26));
        contextCombo.setPreferredSize(new java.awt.Dimension(150, 26));
        contextCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                contextComboItemStateChanged(evt);
            }
        });


        addNewContext.setIcon(INGIcons.swingColored("icon.addIcon", 16));
        addNewContext.setToolTipText("Add New Context");
        addNewContext.setFocusable(false);
        addNewContext.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addNewContext.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addNewContext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addNewContextActionPerformed(evt);
            }
        });
        contextJToolBar.add(addNewContext);

        deleteContext.setIcon(INGIcons.swingColored("icon.deleteIcon", 16));
        deleteContext.setToolTipText("Delete Context");
        deleteContext.setFocusable(false);
        deleteContext.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteContext.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteContext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteContextActionPerformed(evt);
            }
        });
        contextJToolBar.add(deleteContext);

        addContextPropButton.setIcon(INGIcons.swingColored("icon.add", 16));
        addContextPropButton.setToolTipText("Add Property");
        addContextPropButton.setFocusable(false);
        addContextPropButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addContextPropButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addContextPropButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addContextPropButtonActionPerformed(evt);
            }
        });
        contextJToolBar.add(addContextPropButton);

        removeContextPropButton.setIcon(INGIcons.swingColored("icon.remove", 16));
        removeContextPropButton.setToolTipText("Remove Property");
        removeContextPropButton.setFocusable(false);
        removeContextPropButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        removeContextPropButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        removeContextPropButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {

                removeContextPropButtonActionPerformed(evt);
            }
        });
        contextJToolBar.add(removeContextPropButton);

        contextPropTable.setModel(new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{
                        "Property", "Value"
                }
        ) {
            boolean[] canEdit = new boolean[]{
                    true, true
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit[columnIndex];
            }
        });
        contextPropTable.setMinimumSize(new java.awt.Dimension(30, 120));
        contextPropTable.setOpaque(false);
        contextPropTable.setPreferredSize(new java.awt.Dimension(150, 120));
        jScrollPane5.setViewportView(contextPropTable);
        contextPanel.add(jScrollPane5, java.awt.BorderLayout.CENTER);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
                jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 652, Short.MAX_VALUE)
        );
        jPanel6Layout.setVerticalGroup(
                jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 557, Short.MAX_VALUE)
        );
        javax.swing.GroupLayout contextPanelLayout = new javax.swing.GroupLayout(contextPanel);
        contextPanel.setLayout(contextPanelLayout);
        contextPanelLayout.setHorizontalGroup(
                contextPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(contextJToolBar, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        contextPanelLayout.setVerticalGroup(
                contextPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(contextPanelLayout.createSequentialGroup()
                                .addComponent(contextJToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(mainTab, java.awt.BorderLayout.CENTER);

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        saveSettings.setText("Save");
        saveSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveSettingsActionPerformed(evt);
            }
        });
        jPanel1.add(saveSettings);

        resetSettings.setText("Reset");
        resetSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetSettingsActionPerformed(evt);
            }
        });
        jPanel1.add(resetSettings);

        getContentPane().add(jPanel1, java.awt.BorderLayout.SOUTH);
        getContentPane().add(filler5, java.awt.BorderLayout.PAGE_START);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void browserComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_browserComboItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED && !isAddingEmulator) {
            SwingUtilities.invokeLater(() -> {
                checkAndLoadCapabilities();
            });
        }
    }//GEN-LAST:event_browserComboItemStateChanged

    private void addPropButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addPropButtonActionPerformed
        DefaultTableModel model = (DefaultTableModel) driverPropTable.getModel();
        model.addRow(new Object[]{});
    }//GEN-LAST:event_addPropButtonActionPerformed

    private void removePropButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removePropButtonActionPerformed
        int[] rows = driverPropTable.getSelectedRows();
        if (rows != null) {
            DefaultTableModel model = (DefaultTableModel) driverPropTable.getModel();
            for (int i = rows.length - 1; i >= 0; i--) {
                model.removeRow(rows[i]);
            }
        }
    }//GEN-LAST:event_removePropButtonActionPerformed

    private void addContextPropButtonActionPerformed(java.awt.event.ActionEvent evt) {
        DefaultTableModel model = (DefaultTableModel) contextPropTable.getModel();
        model.addRow(new Object[]{});
    }

    private void removeContextPropButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int[] rows = contextPropTable.getSelectedRows();
        if (rows != null) {
            DefaultTableModel model = (DefaultTableModel) contextPropTable.getModel();
            for (int i = rows.length - 1; i >= 0; i--) {
                model.removeRow(rows[i]);
            }
        }
    }


    private void saveSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveSettingsActionPerformed
        saveSettings();
        saveSettings.setEnabled(false);
    }//GEN-LAST:event_saveSettingsActionPerformed

    private void resetSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetSettingsActionPerformed
        // TODO add your handling code here:
        saveSettings.setEnabled(false);
    }//GEN-LAST:event_resetSettingsActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        settings.getEmulators().reload();
        sMainFrame.reloadBrowsers();
    }//GEN-LAST:event_formWindowClosing

    private void addNewEmulatorActionPerformed(java.awt.event.ActionEvent evt) {
        addNewEmulator();
    }
    private void editEmulatorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editEmulatorActionPerformed
        renameEmulator();
    }//GEN-LAST:event_editEmulatorActionPerformed

    private void deleteEmulatorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteEmulatorActionPerformed
        deleteEmulator();
    }//GEN-LAST:event_deleteEmulatorActionPerformed

    private void removeCapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeCapActionPerformed
        int[] rows = capTable.getSelectedRows();
        if (rows != null) {
            DefaultTableModel model = (DefaultTableModel) capTable.getModel();
            for (int i = rows.length - 1; i >= 0; i--) {
                model.removeRow(rows[i]);
            }
        }
    }//GEN-LAST:event_removeCapActionPerformed

    private void addCapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addCapActionPerformed
        DefaultTableModel model = (DefaultTableModel) capTable.getModel();
        model.addRow(new Object[]{});
    }//GEN-LAST:event_addCapActionPerformed

    private void appiumEmulatorItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_appiumEmulatorItemStateChanged
        appiumConnectionString.setEnabled(appiumEmulator.isSelected());
    }//GEN-LAST:event_appiumEmulatorItemStateChanged

    private void addNewDBActionPerformed(java.awt.event.ActionEvent evt) {
        addNewDB();
    }

    private void deleteDBActionPerformed(java.awt.event.ActionEvent evt) {
        deleteDB();
    }

    private void dbComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_dbComboItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            SwingUtilities.invokeLater(() -> {
                checkAndLoadDatabases();
            });
        }
    }//GEN-LAST:event_dbComboItemStateChanged

    private void formWindowActivated(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowActivated
        // TODO add your handling code here:
        saveSettings.setEnabled(false);
        addListeners();
    }//GEN-LAST:event_formWindowActivated

    private void addNewDB() {
        String newdbName = dbCombo.getEditor().getItem().toString();
        if (!getTotalDBList().contains(newdbName)) {
            settings.getDatabaseSettings().addDBName(newdbName);
            dbCombo.addItem(newdbName);
            dbCombo.setSelectedItem(newdbName);
            settings.getDatabaseSettings().addDBProperty(newdbName);
            loadDB(newdbName);
        } else {
            Notification.show("Database [" + newdbName + "] already Present");
        }
    }

    private void deleteDB() {
        if (dbCombo.getSelectedIndex() != -1) {
            String dbName = dbCombo.getSelectedItem().toString();
            settings.getDatabaseSettings().delete(dbName);
            dbCombo.removeItem(dbName);
        }
    }
    

    /**
     * Handles the action event triggered when the "Add DB Property" button is clicked.
     * <p>
     * This method adds a new, empty row to the {@code dbPropTable} using its table model.
     * It is typically used to allow users to input a new database property entry.
     *
     * @param evt the action event triggered by the button click
     */
    private void addDBPropButtonActionPerformed(java.awt.event.ActionEvent evt) {
        DefaultTableModel model = (DefaultTableModel) dbPropTable.getModel();
        model.addRow(new Object[]{});
    }

    
    /**
     * Handles the action event triggered when the "Remove DB Property" button is clicked.
     * <p>
     * This method removes all selected rows from the {@code dbPropTable}. It iterates
     * through the selected rows in reverse order to avoid index shifting issues during removal.
     *
     * @param evt the action event triggered by the button click
     */
    private void removeDBPropButtonActionPerformed(java.awt.event.ActionEvent evt) {
        int[] rows = dbPropTable.getSelectedRows();
        if (rows != null) {
            DefaultTableModel model = (DefaultTableModel) dbPropTable.getModel();
            for (int i = rows.length - 1; i >= 0; i--) {
                model.removeRow(rows[i]);
            }
        }
    }

    private void addNewContextActionPerformed(java.awt.event.ActionEvent evt) {
        addNewContext();
    }

    private void deleteContextActionPerformed(java.awt.event.ActionEvent evt) {
        deleteContext();
    }

    private void contextComboItemStateChanged(java.awt.event.ItemEvent evt) {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            SwingUtilities.invokeLater(() -> {
                checkAndLoadContexts();
            });
        }
    }

    private void addNewContext() {
        String newContextName = contextCombo.getEditor().getItem().toString();
        if (!newContextName.isBlank()) {
            if (!getTotalContextList().contains(newContextName) || getTotalContextList().isEmpty()) {
                settings.getContextSettings().addContextName(newContextName);
                contextCombo.addItem(newContextName);
                contextCombo.setSelectedItem(newContextName);
                settings.getContextSettings().addContextOptions(newContextName);
                loadContext(newContextName);
            } else {
                Notification.show("Context [" + newContextName + "] already Present");
            }
        } else {
            Notification.show("Context Alias is blank");
        }
    }

    private void deleteContext() {
        if (contextCombo.getSelectedIndex() != -1) {
            String contextName = contextCombo.getSelectedItem().toString();
            settings.getContextSettings().delete(contextName);
            contextCombo.removeItem(contextName);
        }
    }

    // Triggered when add API configuration is clicked
    private void addNewAPIActionPerformed(java.awt.event.ActionEvent evt) {
        addNewAPI();
    }

    /**
     * Adds a new API configuration based on the user's input from the combo box editor.
     * <p>
     * If the entered API name does not already exist in the configuration list, this method:
     * <ul>
     *     <li>Adds the new alias to the settings</li>
     *     <liUpdates the combo box with the new item and selects it</li>
     *     <li>Creates a new default properties configuration for the alias</li>
     *     <li>Loads the driver settings for the new API</li>
     * </ul>
     * If the alias already exists, a notification is shown to inform the user.
     */
    private void addNewAPI() {
        String newAPIName = apiCombo.getEditor().getItem().toString();
        if (!getAPIList().contains(newAPIName)) {
            settings.getDriverSettings().addAPIName(newAPIName);
            apiCombo.addItem(newAPIName);
            apiCombo.setSelectedItem(newAPIName);
            settings.getDriverSettings().addAPIProperty(newAPIName);
            loadAPIConfiguration(newAPIName);
        } else {
            Notification.show("API configuration [" + newAPIName + "] already Present");
        }
    }

    // Triggered when remove API configuration is clicked
    private void deleteAPIActionPerformed(java.awt.event.ActionEvent evt) {
        deleteAPI();
    }

    /**
     * Deletes the currently selected API configuration from the combo box.
     * <p>
     * This method:
     * <ul>
     *     <li>Retrieves the selected API alias</li>
     *     <li>Deletes the corresponding configuration from the settings</li>
     *     <li>Removes the alias from the combo box</li>
     * </ul>
     *
     */ 
    private void deleteAPI() {
        if (apiCombo.getSelectedIndex() != -1) {
            String apiName = apiCombo.getSelectedItem().toString();
            settings.getDriverSettings().delete(apiName);
            apiCombo.removeItem(apiName);
        }
    }

    // Triggered when a change in the apiCombobox is detected
    private void apiComboItemStateChanged(java.awt.event.ItemEvent evt) {
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            SwingUtilities.invokeLater(() -> {
                checkAndLoadApi();
            });
        }
    }
    

    private void alterDefaultKeyBindings() {

        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        appiumConnectionString.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "none");
        appiumConnectionString.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "none");
        appiumConnectionString.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "none");

        appiumConnectionString.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "cut");
        appiumConnectionString.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "copy");
        appiumConnectionString.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "paste");


    }
    
    private void addListeners(){
        // Add SaveSettings listeners
        saveSettingsListeners = new SaveSettingsListeners(saveSettings);
        
        driverPropTable.getModel().addTableModelListener(saveSettingsListeners.new  SaveTableModelListener());
        appiumConnectionString.getDocument().addDocumentListener(saveSettingsListeners.new SaveDocListener());
        appiumEmulator.addItemListener(saveSettingsListeners.new  SaveItemListener());
        capTable.getModel().addTableModelListener(saveSettingsListeners.new  SaveTableModelListener());
        dbCombo.addItemListener(saveSettingsListeners.new  SaveItemListener());
        dbPropTable.getModel().addTableModelListener(saveSettingsListeners.new  SaveTableModelListener());
        contextCombo.addItemListener(saveSettingsListeners.new  SaveItemListener());
        contextPropTable.getModel().addTableModelListener(saveSettingsListeners.new  SaveTableModelListener());
        // End of SaveSettings Listeners
    }
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> apiCombo;
    private javax.swing.JLabel apiJLabel;
    private javax.swing.JButton addCap;
    private javax.swing.JButton addPropButton;
    private javax.swing.JTextField appiumConnectionString;
    private javax.swing.JRadioButton appiumEmulator;
    private javax.swing.JComboBox<String> browserCombo;
    private javax.swing.JPanel browserPanel;
    private javax.swing.JTable capTable;
    private javax.swing.JPanel capabilityPanel;
    private javax.swing.JPanel commonPanel;
    private javax.swing.ButtonGroup customDeviceGroup;
    private javax.swing.JButton deleteEmulator;
    private javax.swing.JTable driverPropTable;
    private javax.swing.JButton editEmulator;
    private javax.swing.JTabbedPane emCapTab;
    private javax.swing.ButtonGroup emulatorGroup;
    private javax.swing.JPanel emulatorPanel;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.Box.Filler filler8;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JTabbedPane mainTab;
    private javax.swing.JButton removeCap;
    private javax.swing.JButton removePropButton;
    private javax.swing.JButton resetSettings;
    private javax.swing.JButton saveSettings;
    private javax.swing.JButton addNewDB;
    private javax.swing.JComboBox<String> dbCombo;
    private javax.swing.JTable dbPropTable;
    private javax.swing.JButton deleteDB;
    private javax.swing.JButton addDBPropbutton;
    private javax.swing.JButton deleteDBPropbutton;
    private javax.swing.JPanel databasePanel;
    private javax.swing.JTable contextPropTable;
    private javax.swing.JButton addNewContext;
    private javax.swing.JButton addNewAPIConfig;
    private javax.swing.JButton deleteAPIConfig;
    private javax.swing.JComboBox<String> contextCombo;
    private javax.swing.JButton deleteContext;
    private javax.swing.JPanel contextPanel;
    private javax.swing.JToolBar contextJToolBar;
    private javax.swing.JLabel contextjLabel;
    private javax.swing.JButton addContextPropButton;
    private javax.swing.JButton removeContextPropButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JToolBar jToolBar5;
    private javax.swing.JToolBar.Separator jSeparator2;

    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.Box.Filler filler9;
    private javax.swing.Box.Filler filler10;
    private javax.swing.Box.Filler filler11;
    private javax.swing.Box.Filler filler14;
    private javax.swing.Box.Filler filler16;
    private javax.swing.Box.Filler filler18;
    private javax.swing.Box.Filler filler19;
    private javax.swing.Box.Filler filler20;
    private javax.swing.Box.Filler filler21;
    // End of variables declaration//GEN-END:variables
}