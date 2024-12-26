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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import javax.swing.table.DefaultTableModel;

/**
 *
 *
 */
public class DriverSettings extends javax.swing.JFrame {

    private final AppMainFrame sMainFrame;
    Project sProject;
    ProjectSettings settings;

    /**
     * Creates new form NewJFrame
     *
     * @param sMainFrame
     */
    public DriverSettings(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
        initComponents();

        setIconImage(((ImageIcon) Utils.getIconByResourceName("/ui/resources/main/BrowserConfiguration")).getImage());

        //loadChromeEmulators();
        initAddEmulatorListener();
        initAddNewDBListener();
        initAddNewContextListener();

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
    }

    private void initAddEmulatorListener() {
        browserCombo.getEditor().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                addNewEmulator();
            }
        });
    }

    private void initAddNewDBListener() {
        dbCombo.getEditor().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                addNewDB();
            }
        });
    }

    private void initAddNewContextListener() {
        contextCombo.getEditor().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                addNewContext();
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
    }

    private void loadDriverPropTable() {
        DefaultTableModel model = (DefaultTableModel) driverPropTable.getModel();
        model.setRowCount(0);
        for (Object key : settings.getDriverSettings().orderedKeys()) {
            Object value = settings.getDriverSettings().get(key);
            model.addRow(new Object[]{key, value});
        }
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

    private void loadCapabilities(String browserName) {
        DefaultTableModel model = (DefaultTableModel) capTable.getModel();
        model.setRowCount(0);
        LinkedProperties prop = settings.getCapabilities().getCapabiltiesFor(browserName);
        if (prop != null) {
            for (Object key : prop.orderedKeys()) {
                Object value = prop.get(key);
                model.addRow(new Object[]{key, value});
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
            settings.getEmulators().addEmulator(newEmName);
            browserCombo.addItem(newEmName);
            //dupDriverCombo.addItem(newEmName);
            browserCombo.setSelectedItem(newEmName);
        } else {
            Notification.show("Emulator/Browser [" + newEmName + "] already Present");
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
        saveCommonSettings();
        saveContextProperties();
        saveCapabilities();
        saveDBProperties();
        saveEmulator();

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
        if (driverPropTable.isEditing()) {
            driverPropTable.getCellEditor().stopCellEditing();
        }
        Properties driveProps = encryptpassword(PropUtils.getPropertiesFromTable(driverPropTable));
        PropUtils.loadPropertiesInTable(driveProps, driverPropTable, "");

        DefaultTableModel model = (DefaultTableModel) driverPropTable.getModel();
        settings.getDriverSettings().clear();
        for (int i = 0; i < model.getRowCount(); i++) {
            String prop = Objects.toString(model.getValueAt(i, 0), "").trim();
            if (!prop.isEmpty()) {
                String value = Objects.toString(model.getValueAt(i, 1), "");
                settings.getDriverSettings().setProperty(prop, value);
            }
        }
    }

    private void saveDBProperties() {
        if (dbCombo.getSelectedIndex() != -1) {
            if (dbPropTable.isEditing()) {
                dbPropTable.getCellEditor().stopCellEditing();
            }

            Properties driveProps = encryptpassword(PropUtils.getPropertiesFromTable(dbPropTable));
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

            Properties contextProps = encryptpassword(PropUtils.getPropertiesFromTable(contextPropTable));
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
                if (key.toLowerCase().contains("passw")) {
                    properties.setProperty(key, Utility.encrypt(value));
                }
            }
        });
        return properties;
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
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
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

        jToolBar1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jToolBar1.setRollover(true);
        jToolBar1.add(filler1);

        addPropButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/add.png"))); // NOI18N
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

        removePropButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/remove.png"))); // NOI18N
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

        addCap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/add.png"))); // NOI18N
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

        removeCap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/remove.png"))); // NOI18N
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


        addNewDB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/addIcon.png")));
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

        deleteDB.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/deleteIcon.png")));
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


        addNewContext.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/addIcon.png")));
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

        deleteContext.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/deleteIcon.png")));
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

        addContextPropButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/add.png")));
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

        removeContextPropButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/remove.png")));
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
        if (evt.getStateChange() == ItemEvent.SELECTED) {
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
    }//GEN-LAST:event_saveSettingsActionPerformed

    private void resetSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetSettingsActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_resetSettingsActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
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
    private javax.swing.JPanel databasePanel;
    private javax.swing.JTable contextPropTable;
    private javax.swing.JButton addNewContext;
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
    // End of variables declaration//GEN-END:variables
}
