
package com.ing.ide.main.settings;

import com.ing.datalib.component.Project;
import com.ing.datalib.settings.TestMgmtModule;
import com.ing.datalib.settings.TestMgmtSettings;
import com.ing.datalib.settings.testmgmt.Option;
import com.ing.datalib.settings.testmgmt.TestMgModule;
import com.ing.engine.core.TMIntegration;
import com.ing.engine.reporting.sync.Sync;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.table.XTable;
import com.ing.ide.util.Notification;
import java.awt.event.ItemEvent;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import com.ing.ide.main.fx.INGIcons;
import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

/**
 *
 *
 */
public class TMSettings extends javax.swing.JFrame {

    private static final javax.swing.Icon DEFAULT_ICON = INGIcons.swingColored("icon.bulb_yellow", 16);
    private static final javax.swing.Icon PASS_ICON = INGIcons.swingColored("icon.bulb_green", 16);
    private static final javax.swing.Icon FAIL_ICON = INGIcons.swingColored("icon.bulb_red", 16);
    
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

    AppMainFrame sMainFrame;
    Project sProject;

    TestMgmtModule testMgmtModule;

    /**
     * Creates new form TMSettings
     *
     * @param sMainFrame
     */
    public TMSettings(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
        initComponents();
        setIconImage(com.ing.ide.main.fx.INGIcons.toImage(Utils.getIconByResourceName("/ui/resources/main/AzureDevOpsTestPlanConfiguration")));
        applyThemeStyles();
    }

    public void load() {
        sProject = sMainFrame.getProject();
        testMgmtModule = sProject.getProjectSettings().getTestMgmtModule();
        moduleCombo.setModel(new DefaultComboBoxModel(testMgmtModule.getModuleNames().toArray()));
        if (moduleCombo.getItemCount() > 0) {
            checkAndLoadModule();
        }
    }

    public void open() {
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void checkAndLoadModule() {
        if (moduleCombo.getSelectedIndex() != -1) {
            String selModule = moduleCombo.getSelectedItem().toString();
            TestMgModule testModule = testMgmtModule.getModule(selModule);
            if (testModule != null) {
                loadModuleInTable(testModule);
            }
            reset();
        }
    }

    private void loadModuleInTable(TestMgModule testModule) {
        DefaultTableModel model = (DefaultTableModel) moduleTable.getModel();
        model.setRowCount(0);
        List<Option> option = testModule.getOptions();
        for (Option op : option) {
            model.addRow(new Object[]{op.getName(), op.getValue()});
        }
    }

    private void addNewModule() {
        String moduleName = moduleCombo.getEditor().getItem().toString();
        if (!moduleName.trim().isEmpty()) {
            if (testMgmtModule.getModule(moduleName) == null) {
                testMgmtModule.addModule(moduleName);
                moduleCombo.addItem(moduleName);
                moduleCombo.setSelectedItem(moduleName);
            }
        }
    }

    private void renameModule() {
        if (moduleCombo.getSelectedIndex() != -1) {
            String moduleName = moduleCombo.getSelectedItem().toString();
            String newModuleName = moduleCombo.getEditor().getItem().toString();
            if (!newModuleName.trim().isEmpty()) {
                testMgmtModule.getModule(moduleName).setModule(newModuleName);
                DefaultComboBoxModel combomodel = (DefaultComboBoxModel) moduleCombo.getModel();
                int index = moduleCombo.getSelectedIndex();
                combomodel.removeElement(moduleName);
                combomodel.insertElementAt(newModuleName, index);
                moduleCombo.setSelectedIndex(index);
            }
        }
    }

    private void deleteModule() {
        if (moduleCombo.getSelectedIndex() != -1) {
            String moduleName = moduleCombo.getSelectedItem().toString();
            testMgmtModule.removeModule(moduleName);
            moduleCombo.removeItem(moduleName);
        }
    }

    private void checkConnection() {
        if (moduleCombo.getSelectedIndex() != -1) {
            TestMgmtSettings tsSettings = new TestMgmtSettings("");
            tsSettings.set(PropUtils.getPropertiesFromTable(moduleTable));
            tsSettings.setUpdateResultsToTM(moduleCombo.getSelectedItem().toString());
            Sync connection = TMIntegration.getInstance(tsSettings);
            if (connection != null) {
                try {
                    if (connection.isConnected()) {
                        success();
                    } else {
                        Notification.show("Couldn't Connect.Check the credentials and log");
                        failure();
                    }
                } catch (Exception ex) {
                    Logger.getLogger(TMSettings.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
                    Notification.show("Couldn't Connect.Check the credentials and log");
                    failure();
                }
            } else {
                Notification.show("TestManagement haven't been configured");
                failure();
            }
        }
    }

    private void reset() {
        checkConnection.setIcon(DEFAULT_ICON);
    }

    private void success() {
        checkConnection.setIcon(PASS_ICON);
    }

    private void failure() {
        checkConnection.setIcon(FAIL_ICON);
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
        
        // Style panels
        if (jPanel2 != null) {
            jPanel2.setBackground(panelBgColor);
            jPanel2.setOpaque(true);
            jPanel2.setBorder(BorderFactory.createCompoundBorder(
                new MatteBorder(1, 0, 0, 0, borderColor),
                new EmptyBorder(8, 16, 8, 16)));
        }
        
        // Style toolbar
        if (jToolBar3 != null) {
            jToolBar3.setBackground(panelBgColor);
            jToolBar3.setOpaque(true);
            jToolBar3.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderColor));
        }
        
        // Style table
        if (moduleTable != null) {
            moduleTable.setBackground(inputBgColor);
            moduleTable.setForeground(textColor);
            moduleTable.setGridColor(borderColor);
            moduleTable.setSelectionBackground(getAccentColor());
            moduleTable.setSelectionForeground(Color.WHITE);
            if (moduleTable.getTableHeader() != null) {
                moduleTable.getTableHeader().setBackground(panelBgColor);
                moduleTable.getTableHeader().setForeground(textColor);
            }
        }
        
        // Style combo box
        if (moduleCombo != null) {
            moduleCombo.setBackground(inputBgColor);
            moduleCombo.setForeground(textColor);
        }
        
        // Style labels
        if (jLabel2 != null) {
            jLabel2.setForeground(textColor);
        }
        
        // Style buttons
        styleButton(save, true);
        styleButton(checkConnection, true);
        styleButton(reset, false);
        
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
            if (comp instanceof java.awt.Container) {
                styleComponentsRecursively((java.awt.Container) comp);
            }
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

        jPanel2 = new javax.swing.JPanel();
        save = new javax.swing.JButton();
        reset = new javax.swing.JButton();
        jToolBar3 = new javax.swing.JToolBar();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        jLabel2 = new javax.swing.JLabel();
        filler6 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        moduleCombo = new javax.swing.JComboBox<>();
        filler7 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        addNewModule = new javax.swing.JButton();
        editModule = new javax.swing.JButton();
        deleteModule = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        checkConnection = new javax.swing.JButton();
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 0), new java.awt.Dimension(10, 32767));
        jPanel1 = new javax.swing.JPanel();
        jToolBar1 = new javax.swing.JToolBar();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        encrypt = new javax.swing.JButton();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        add = new javax.swing.JButton();
        remove = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        moduleTable = new XTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("AzureDevOps TestPlan Configuration");
        setMinimumSize(new java.awt.Dimension(500, 149));

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        save.setText("Save");
        save.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveActionPerformed(evt);
            }
        });
        jPanel2.add(save);

        reset.setText("Reset");
        reset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetActionPerformed(evt);
            }
        });
        jPanel2.add(reset);

        getContentPane().add(jPanel2, java.awt.BorderLayout.SOUTH);

        jToolBar3.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jToolBar3.setRollover(true);
        jToolBar3.setMinimumSize(new java.awt.Dimension(357, 50));
        jToolBar3.setPreferredSize(new java.awt.Dimension(100, 50));
        jToolBar3.add(filler3);

        jLabel2.setText("Module Name");
        jToolBar3.add(jLabel2);
        jToolBar3.add(filler6);

        moduleCombo.setEditable(true);
        moduleCombo.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        moduleCombo.setMinimumSize(new java.awt.Dimension(150, 26));
        moduleCombo.setPreferredSize(new java.awt.Dimension(180, 26));
        moduleCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                moduleComboItemStateChanged(evt);
            }
        });
        moduleCombo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moduleComboActionPerformed(evt);
            }
        });
        jToolBar3.add(moduleCombo);
        jToolBar3.add(filler7);

        addNewModule.setIcon(INGIcons.swingColored("icon.add", 16));
        addNewModule.setToolTipText("Add New Module");
        addNewModule.setFocusable(false);
        addNewModule.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        addNewModule.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        addNewModule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addNewModuleActionPerformed(evt);
            }
        });
        jToolBar3.add(addNewModule);

        editModule.setIcon(INGIcons.swingColored("icon.edit", 16));
        editModule.setToolTipText("Rename Module");
        editModule.setFocusable(false);
        editModule.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        editModule.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        editModule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editModuleActionPerformed(evt);
            }
        });
        jToolBar3.add(editModule);

        deleteModule.setIcon(INGIcons.swingColored("icon.delete", 16));
        deleteModule.setToolTipText("Remove Module");
        deleteModule.setFocusable(false);
        deleteModule.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        deleteModule.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        deleteModule.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteModuleActionPerformed(evt);
            }
        });
        jToolBar3.add(deleteModule);
        jToolBar3.add(jSeparator1);

        checkConnection.setIcon(INGIcons.swingColored("icon.bulb_yellow", 16));
        checkConnection.setText("Test Connection");
        checkConnection.setToolTipText("Check Connection");
        checkConnection.setFocusable(false);
        checkConnection.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        checkConnection.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                checkConnectionActionPerformed(evt);
            }
        });
        jToolBar3.add(checkConnection);
        jToolBar3.add(filler2);
        jToolBar3.add(filler5);

        getContentPane().add(jToolBar3, java.awt.BorderLayout.PAGE_START);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jToolBar1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jToolBar1.setRollover(true);
        jToolBar1.add(filler1);

        encrypt.setIcon(INGIcons.swingColored("icon.lock", 16));
        encrypt.setToolTipText("Encrypt Selected Fields");
        encrypt.setFocusable(false);
        encrypt.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        encrypt.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        encrypt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                encryptActionPerformed(evt);
            }
        });
        jToolBar1.add(encrypt);
        jToolBar1.add(jSeparator3);

        add.setIcon(INGIcons.swingColored("icon.add", 16));
        add.setFocusable(false);
        add.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        add.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        add.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addActionPerformed(evt);
            }
        });
        jToolBar1.add(add);

        remove.setIcon(INGIcons.swingColored("icon.remove", 16));
        remove.setFocusable(false);
        remove.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        remove.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        remove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeActionPerformed(evt);
            }
        });
        jToolBar1.add(remove);

        jPanel1.add(jToolBar1, java.awt.BorderLayout.NORTH);

        moduleTable.setModel(new javax.swing.table.DefaultTableModel(
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
        moduleTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(moduleTable);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel1, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void moduleComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_moduleComboItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    checkAndLoadModule();
                }

            });
        }
    }//GEN-LAST:event_moduleComboItemStateChanged

    private void addNewModuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addNewModuleActionPerformed
        addNewModule();
    }//GEN-LAST:event_addNewModuleActionPerformed

    private void editModuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editModuleActionPerformed
        saveModule();
        renameModule();
    }//GEN-LAST:event_editModuleActionPerformed

    private void deleteModuleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteModuleActionPerformed
        deleteModule();
    }//GEN-LAST:event_deleteModuleActionPerformed

    private void saveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveActionPerformed
        saveModule();
    }//GEN-LAST:event_saveActionPerformed

    private void saveModule() {
        if (moduleCombo.getSelectedIndex() != -1) {
            String moduleName = moduleCombo.getSelectedItem().toString();
            TestMgModule module = testMgmtModule.getModule(moduleName);
            Properties properties;
            properties = encryptpassword(PropUtils.getPropertiesFromTable(moduleTable));
            PropUtils.loadPropertiesInTable(properties, moduleTable, "");
            if (moduleTable.isEditing()) {
                moduleTable.getCellEditor().stopCellEditing();
            }
            module.getOptions().clear();
            DefaultTableModel model = (DefaultTableModel) moduleTable.getModel();
            for (int i = 0; i < model.getRowCount(); i++) {
                String prop = Objects.toString(model.getValueAt(i, 0), "").trim();
                if (!prop.isEmpty()) {
                    String value = Objects.toString(model.getValueAt(i, 1), "");
                    module.getOptions().add(new Option(prop, value));
                }
            }
        }
    }

    private Properties encryptpassword(Properties properties) {
        properties.entrySet().forEach((e) -> {
            String key = (String) e.getKey();
            String value = (String) e.getValue();
            if (value != null && !value.isEmpty()) {
                if (key.toLowerCase().contains("passw")) {
                    properties.setProperty(key, TMIntegration.encrypt(value));
                }
            }
        });
        return properties;
    }

    private void resetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetActionPerformed
        checkAndLoadModule();
    }//GEN-LAST:event_resetActionPerformed

    private void addActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addActionPerformed
        DefaultTableModel model = (DefaultTableModel) moduleTable.getModel();
        model.addRow(new Object[]{});
    }//GEN-LAST:event_addActionPerformed

    private void removeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeActionPerformed
        int[] rows = moduleTable.getSelectedRows();
        if (rows != null) {
            DefaultTableModel model = (DefaultTableModel) moduleTable.getModel();
            for (int i = rows.length - 1; i >= 0; i--) {
                model.removeRow(rows[i]);
            }
        }
    }//GEN-LAST:event_removeActionPerformed

    private void checkConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkConnectionActionPerformed
        checkConnection();
    }//GEN-LAST:event_checkConnectionActionPerformed

    private void encryptActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_encryptActionPerformed
        for (int selectedRow : moduleTable.getSelectedRows()) {
            String value = Objects.toString(moduleTable.getValueAt(selectedRow, 1), "");
            if (!value.isEmpty()) {
                moduleTable.setValueAt(
                        TMIntegration.encrypt(value),
                        selectedRow, 1);
            }
        }
    }//GEN-LAST:event_encryptActionPerformed

    private void moduleComboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moduleComboActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_moduleComboActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton add;
    private javax.swing.JButton addNewModule;
    private javax.swing.JButton checkConnection;
    private javax.swing.JButton deleteModule;
    private javax.swing.JButton editModule;
    private javax.swing.JButton encrypt;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler5;
    private javax.swing.Box.Filler filler6;
    private javax.swing.Box.Filler filler7;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JToolBar jToolBar3;
    private javax.swing.JComboBox<String> moduleCombo;
    private javax.swing.JTable moduleTable;
    private javax.swing.JButton remove;
    private javax.swing.JButton reset;
    private javax.swing.JButton save;
    // End of variables declaration//GEN-END:variables

}
