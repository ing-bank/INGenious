
package com.ing.ide.main.settings;

import com.ing.datalib.component.Project;
import com.ing.datalib.settings.ProjectSettings;
import com.ing.datalib.settings.emulators.Emulator;
import com.ing.datalib.util.data.LinkedProperties;
import com.ing.engine.drivers.ChromeEmulators;
import com.ing.engine.drivers.WebDriverFactory;
import com.ing.ide.main.help.Help;
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
import java.net.URI;
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

        loadChromeEmulators();
        initAddEmulatorListener();

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

    private void loadChromeEmulators() {
        chromeEmulatorList.setModel(
                new DefaultComboBoxModel(
                        ChromeEmulators.getEmulatorsList().toArray()));
        if (chromeEmulatorList.getItemCount() == 0) {
            chromeEmulator.setEnabled(false);
            chromeEmulator.setToolTipText("Not Supported");
        }
    }

    public void load() {
        this.sProject = sMainFrame.getProject();
        settings = sProject.getProjectSettings();
        loadSettings();
    }

    private void loadSettings() {
        loadDriverPropTable();
        loadBrowsers();
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
        dupDriverCombo.setModel(new DefaultComboBoxModel(
                getTotalBrowserList().toArray()));
        checkAndLoadCapabilities();
    }

    private List<String> getTotalBrowserList() {
        List<String> list = WebDriverFactory.Browser.getValuesAsList();
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

    private void loadEmulator(Emulator emulator) {
        String type = emulator.getType();
        setButtonGroup(type, customDeviceGroup);
        switch (type) {
            case "Duplicate":
                dupDriverCombo.setSelectedItem(emulator.getDriver());
                break;
            case "Emulator":
                setButtonGroup(emulator.getDriver(), emulatorGroup);
                resolution.setText(emulator.getSize());
                userAgent.setText(emulator.getUserAgent());
                break;
            case "Chrome Emulator":
                chromeEmulatorList.setSelectedItem(emulator.getDriver());
                break;
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
            dupDriverCombo.addItem(newEmName);
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
                DefaultComboBoxModel dupCombomodel = (DefaultComboBoxModel) dupDriverCombo.getModel();
                int index = browserCombo.getSelectedIndex();
                combomodel.removeElement(oldName);
                dupCombomodel.removeElement(oldName);
                combomodel.insertElementAt(newEmName, index);
                dupCombomodel.insertElementAt(newEmName, index);
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
            dupDriverCombo.removeItem(emName);
        } else {

        }
    }

    private void saveSettings() {
        if (mainTab.getSelectedIndex() == 0) {
            saveCommonSettings();
        } else if (emCapTab.getSelectedIndex() == 0) {
            saveEmulator();
        } else {
            saveCapabilities();
        }
    }

    private void saveEmulator() {
        settings.getEmulators().addEmulator(browserCombo.getSelectedItem().toString());
        Emulator emulator = settings.getEmulators().
                getEmulator(browserCombo.getSelectedItem().toString());
        emulator.setType(customDeviceGroup.getSelection().getActionCommand());
        switch (emulator.getType()) {
            case "Duplicate":
                emulator.setDriver(dupDriverCombo.getSelectedItem().toString());
                break;
            case "Emulator":
                emulator.setDriver(emulatorGroup.getSelection().getActionCommand());
                emulator.setSize(Objects.toString(resolution.getText(), ""));
                emulator.setUserAgent(userAgent.getText());
                break;
            case "Chrome Emulator":
                emulator.setDriver(chromeEmulatorList.getSelectedItem().toString());
                break;
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
        jScrollPane2 = new javax.swing.JScrollPane();
        userAgent = new javax.swing.JTextArea();
        appiumConnectionString = new javax.swing.JTextField();
        chromeBrowser = new javax.swing.JRadioButton();
        firefoxBrowser = new javax.swing.JRadioButton();
        uaEmulator = new javax.swing.JRadioButton();
        chromeEmulator = new javax.swing.JRadioButton();
        appiumEmulator = new javax.swing.JRadioButton();
        chromeEmulatorList = new javax.swing.JComboBox<>();
        chromeEmulatorHint = new javax.swing.JLabel();
        duplicateEmulator = new javax.swing.JRadioButton();
        dupDriverCombo = new javax.swing.JComboBox<>();
        pxLabel = new javax.swing.JLabel();
        syncChromeEmulators = new javax.swing.JButton();
        resolution = new javax.swing.JTextField();
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
        addNewEmulator = new javax.swing.JButton();

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

	    addNewEmulator.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/addIcon.png"))); // NOI18N
        addNewEmulator.setToolTipText("Add New Emulator");
        addNewEmulator.setFocusable(false);
        addNewEmulator.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addNewEmulator.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addNewEmulator.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
                addNewEmulatorActionPerformed(evt);
            }
        });
        jToolBar3.add(addNewEmulator);
        editEmulator.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/editIcon.png")));

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

        deleteEmulator.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/deleteIcon.png")));
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

        userAgent.setColumns(20);
        userAgent.setRows(5);
        userAgent.setText("User Agent\n");
        userAgent.setToolTipText("User Agent String");
        userAgent.setEnabled(false);
        jScrollPane2.setViewportView(userAgent);

        appiumConnectionString.setText("http://127.0.0.1:4723/wd/hub");
        appiumConnectionString.setEnabled(false);

        emulatorGroup.add(chromeBrowser);
        chromeBrowser.setText("Chrome");
        chromeBrowser.setEnabled(false);
        chromeBrowser.setActionCommand("Chrome");

        emulatorGroup.add(firefoxBrowser);
        firefoxBrowser.setText("Firefox");
        firefoxBrowser.setEnabled(false);
        firefoxBrowser.setActionCommand("Firefox");

        customDeviceGroup.add(uaEmulator);
        uaEmulator.setText("Emulator");
        uaEmulator.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                uaEmulatorItemStateChanged(evt);
            }
        });
        uaEmulator.setActionCommand("Emulator");

        customDeviceGroup.add(chromeEmulator);
        chromeEmulator.setText("Chrome Emulator");
        chromeEmulator.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                chromeEmulatorItemStateChanged(evt);
            }
        });
        chromeEmulator.setActionCommand("Chrome Emulator");

        customDeviceGroup.add(appiumEmulator);
        appiumEmulator.setText("Remote URL/Appium");
        appiumEmulator.setActionCommand("Remote URL");
        appiumEmulator.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                appiumEmulatorItemStateChanged(evt);
            }
        });

        chromeEmulatorList.setEnabled(false);

        chromeEmulatorHint.setText("<html><a href=\"https://sites.google.com/a/chromium.org/chromedriver/mobile-emulation\">What's this?</a></html>");
        chromeEmulatorHint.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        chromeEmulatorHint.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                chromeEmulatorHintMouseClicked(evt);
            }
        });

        customDeviceGroup.add(duplicateEmulator);
        duplicateEmulator.setText("Duplicate");
        duplicateEmulator.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                duplicateEmulatorItemStateChanged(evt);
            }
        });
        duplicateEmulator.setActionCommand("Duplicate");

        dupDriverCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        dupDriverCombo.setEnabled(false);

        pxLabel.setFont(new java.awt.Font("sansserif", 1, 12)); // NOI18N
        pxLabel.setText("PT");
        pxLabel.setEnabled(false);

        syncChromeEmulators.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/refresh.png"))); // NOI18N
        syncChromeEmulators.setToolTipText("Refresh the list of Emulators from Chrome");
        syncChromeEmulators.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                syncChromeEmulatorsActionPerformed(evt);
            }
        });

        resolution.setToolTipText("Please enter viewport size of device. Eg: 414 x 736");

        javax.swing.GroupLayout emulatorPanelLayout = new javax.swing.GroupLayout(emulatorPanel);
        emulatorPanel.setLayout(emulatorPanelLayout);
        emulatorPanelLayout.setHorizontalGroup(
            emulatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(emulatorPanelLayout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(emulatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(duplicateEmulator)
                    .addGroup(emulatorPanelLayout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 388, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(emulatorPanelLayout.createSequentialGroup()
                        .addComponent(chromeEmulator)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(chromeEmulatorHint, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(appiumEmulator)
                    .addGroup(emulatorPanelLayout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(appiumConnectionString, javax.swing.GroupLayout.PREFERRED_SIZE, 383, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(emulatorPanelLayout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(chromeBrowser)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(firefoxBrowser)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(resolution, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(pxLabel))
                    .addGroup(emulatorPanelLayout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addComponent(chromeEmulatorList, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(syncChromeEmulators, javax.swing.GroupLayout.PREFERRED_SIZE, 31, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(emulatorPanelLayout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(dupDriverCombo, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(uaEmulator))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        emulatorPanelLayout.setVerticalGroup(
            emulatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, emulatorPanelLayout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(duplicateEmulator)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(dupDriverCombo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, Short.MAX_VALUE)
                .addComponent(uaEmulator)
                .addGap(16, 16, 16)
                .addGroup(emulatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(emulatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(chromeBrowser)
                        .addComponent(firefoxBrowser))
                    .addGroup(emulatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(resolution, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(pxLabel)))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(20, 20, 20)
                .addGroup(emulatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(chromeEmulator)
                    .addComponent(chromeEmulatorHint, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(15, 15, 15)
                .addGroup(emulatorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(chromeEmulatorList, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(syncChromeEmulators))
                .addGap(24, 24, 24)
                .addComponent(appiumEmulator)
                .addGap(18, 18, 18)
                .addComponent(appiumConnectionString, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(51, 51, 51))
        );

        emCapTab.addTab("Emulator", emulatorPanel);

        capabilityPanel.setLayout(new java.awt.BorderLayout());

        capTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null},
                {null, null}
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
        jPanel2.add(filler8, java.awt.BorderLayout.PAGE_START);

        browserPanel.add(jPanel2, java.awt.BorderLayout.CENTER);

        mainTab.addTab("Manage Browsers", browserPanel);

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

        getAccessibleContext().setAccessibleName("Configurations");

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

    private void syncChromeEmulatorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_syncChromeEmulatorsActionPerformed
        ChromeEmulators.sync();
        loadChromeEmulators();
    }//GEN-LAST:event_syncChromeEmulatorsActionPerformed

    private void duplicateEmulatorItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_duplicateEmulatorItemStateChanged
        dupDriverCombo.setEnabled(duplicateEmulator.isSelected());
    }//GEN-LAST:event_duplicateEmulatorItemStateChanged

    private void chromeEmulatorHintMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_chromeEmulatorHintMouseClicked
        Help.openInBrowser("Couldn't Open Help in default Browser",
            URI.create("https://sites.google.com/a/chromium.org/chromedriver/mobile-emulation"));
    }//GEN-LAST:event_chromeEmulatorHintMouseClicked

    private void appiumEmulatorItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_appiumEmulatorItemStateChanged
        appiumConnectionString.setEnabled(appiumEmulator.isSelected());
    }//GEN-LAST:event_appiumEmulatorItemStateChanged

    private void chromeEmulatorItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_chromeEmulatorItemStateChanged
        chromeEmulatorList.setEnabled(chromeEmulator.isSelected());
    }//GEN-LAST:event_chromeEmulatorItemStateChanged

    private void uaEmulatorItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_uaEmulatorItemStateChanged
        chromeBrowser.setEnabled(uaEmulator.isSelected());
        firefoxBrowser.setEnabled(uaEmulator.isSelected());
        resolution.setEnabled(uaEmulator.isSelected());
        userAgent.setEnabled(uaEmulator.isSelected());
        pxLabel.setEnabled(uaEmulator.isSelected());
    }//GEN-LAST:event_uaEmulatorItemStateChanged


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addCap;
    private javax.swing.JButton addPropButton;
    private javax.swing.JTextField appiumConnectionString;
    private javax.swing.JRadioButton appiumEmulator;
    private javax.swing.JComboBox<String> browserCombo;
    private javax.swing.JPanel browserPanel;
    private javax.swing.JTable capTable;
    private javax.swing.JPanel capabilityPanel;
    private javax.swing.JRadioButton chromeBrowser;
    private javax.swing.JRadioButton chromeEmulator;
    private javax.swing.JLabel chromeEmulatorHint;
    private javax.swing.JComboBox<String> chromeEmulatorList;
    private javax.swing.JPanel commonPanel;
    private javax.swing.ButtonGroup customDeviceGroup;
    private javax.swing.JButton deleteEmulator;
    private javax.swing.JTable driverPropTable;
    private javax.swing.JComboBox<String> dupDriverCombo;
    private javax.swing.JRadioButton duplicateEmulator;
    private javax.swing.JButton editEmulator;
    private javax.swing.JButton addNewEmulator;
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
    private javax.swing.JRadioButton firefoxBrowser;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar2;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JTabbedPane mainTab;
    private javax.swing.JLabel pxLabel;
    private javax.swing.JButton removeCap;
    private javax.swing.JButton removePropButton;
    private javax.swing.JButton resetSettings;
    private javax.swing.JTextField resolution;
    private javax.swing.JButton saveSettings;
    private javax.swing.JButton syncChromeEmulators;
    private javax.swing.JRadioButton uaEmulator;
    private javax.swing.JTextArea userAgent;
    // End of variables declaration//GEN-END:variables
}
