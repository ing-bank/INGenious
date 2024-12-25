
package com.ing.ide.main.settings;

import com.ing.datalib.component.Project;
import com.ing.datalib.settings.ExecutionSettings;
import com.ing.datalib.settings.testmgmt.Option;
import com.ing.engine.core.TMIntegration;

import com.ing.engine.reporting.sync.Sync;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.utils.ConnectButton;
import com.ing.ide.main.utils.table.XTable;
import com.ing.ide.main.utils.table.XTablePanel;
import com.ing.ide.settings.IconSettings;
import com.ing.ide.util.Notification;
import com.ing.ide.util.Utility;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.table.DefaultTableModel;
import javax.swing.UIManager;

/**
 *
 *
 */
public class INGeniousSettings extends javax.swing.JFrame {

    private static final ImageIcon DEFAULT_ICON
            = new ImageIcon(TMSettings.class.getResource("/ui/resources/toolbar/bulb_yellow.png"));
    private static final ImageIcon PASS_ICON
            = new ImageIcon(TMSettings.class.getResource("/ui/resources/toolbar/bulb_green.png"));
    private static final ImageIcon FAIL_ICON
            = new ImageIcon(TMSettings.class.getResource("/ui/resources/toolbar/bulb_red.png"));

    private final AppMainFrame sMainFrame;

    private Project sProject;

    private ExecutionSettings execSettings;

    private XTablePanel mailSettingsPanel;

    private XTablePanel databaseSettingsPanel;
    
    private XTablePanel rpSettingsPanel;
    
    private XTablePanel extentSettingsPanel;

    private XTablePanel uDPanel;

    private ConnectButton mailConnect;

    private ConnectButton dbConnect;
    
    public INGeniousSettings(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
        initComponents();
        setIconImage(IconSettings.getIconSettings().getSettingsGear().getImage());
        initTabs();
    }

    private void initTabs() {
        uDPanel = new XTablePanel(false);
        runSettingsTab.setFont(UIManager.getFont("Table.font"));
        runSettingsTab.addTab("UserDefined", uDPanel);
        mailSettingsPanel = new XTablePanel(true);
        //runSettingsTab.addTab("Mail Settings", mailSettingsPanel);
        databaseSettingsPanel = new XTablePanel(true);
        //runSettingsTab.addTab("Database Settings", databaseSettingsPanel);
        rpSettingsPanel= new XTablePanel(true);
        //runSettingsTab.addTab("Report Portal Settings", rpSettingsPanel);
        extentSettingsPanel = new XTablePanel(true);
        runSettingsTab.addTab("Extent Report Settings", extentSettingsPanel);
        
        
        mailConnect = new ConnectButton() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
//                    if (Mailer.connect(PropUtils.getPropertiesFromTable(((XTablePanel) mailSettingsPanel).table))) {
//                        success();
//                    }
                } catch (Exception ex) {
                    Logger.getLogger(INGeniousSettings.class.getName()).log(Level.SEVERE, null, ex);
                    failure();
                }
            }
        };
        
        dbConnect = new ConnectButton() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                Properties encrypted = PropUtils.getPropertiesFromTable(
                        ((XTablePanel) databaseSettingsPanel).table);
                Properties prop = new Properties();
                Optional.ofNullable(prop)
                        .filter((p) -> {
                            return Optional
                                    .ofNullable(p.getProperty("db.driver"))
                                    .filter((val) -> {
                                        return !val.trim().isEmpty();
                                    })
                                    .isPresent()
                                    && Optional
                                            .ofNullable(p.getProperty("db.connection.string"))
                                            .filter((val) -> {
                                                return !val.trim().isEmpty();
                                            })
                                            .isPresent();
                        })
                        .ifPresent((p) -> {
                            String driver = p.getProperty("db.driver");
                            String connStr = p.getProperty("db.connection.string");
                            String user = p.getProperty("db.user");
                            String pwd = p.getProperty("db.secret");
                            try {
                                Class.forName(driver);
                                if (user != null && pwd != null) {
                                    DriverManager.getConnection(connStr, user, pwd);
                                } else {
                                    DriverManager.getConnection(connStr);
                                }
                                success();
                            } catch (ClassNotFoundException | SQLException ex) {
                                Logger.getLogger(INGeniousSettings.class.getName()).log(Level.SEVERE, null, ex);
                                failure();
                            }
                        });
            }
        };

        mailSettingsPanel.addToolBarComp(mailConnect);
        databaseSettingsPanel.addToolBarComp(dbConnect);
    }

    public void afterProjectChange() {
        sProject = sMainFrame.getProject();
        loadAll();
    }

    public void loadSettings() {
        this.execSettings = sProject.getProjectSettings().getExecSettings();
        loadRunSettings();
        loadTestSetTMSettings();
        loadRPSettings();
        loadExtentSettings();
        showSettings();
    }

    public void loadSettings(ExecutionSettings execSettings) {
        this.execSettings = execSettings;
        loadRunSettings();
        loadTestSetTMSettings();
        showSettings();
    }

    public void showSettings() {
        setLocationRelativeTo(null);
        if (!isVisible()) {
            setVisible(true);
        } else {
            toFront();
        }
    }

    private void loadAll() {
        loadTMSettings();
        PropUtils.loadPropertiesInTable(sProject.getProjectSettings()
                .getUserDefinedSettings(), uDPanel.table);
        loadRPSettings();
        loadExtentSettings();
    }

    private void loadRunSettings() {
        executionTimeOut.setText(execSettings.getRunSettings().getExecutionTimeOut() + "");
        threadCount.setValue(execSettings.getRunSettings().getThreadCount());
        remoteGridURL.setText(execSettings.getRunSettings().getRemoteGridURL());
        String iterMode = execSettings.getRunSettings().getIterationMode();
        setButtonModelFromText(iterMode, iModeBgroup);
        String execMode = execSettings.getRunSettings().getExecutionMode();
        setButtonModelFromText(execMode, eModeBgroup);
        String screenshot = execSettings.getRunSettings().getScreenShotFor();
        if (screenshot.matches("(Pass|Both)")) {
            passCheckBox.setSelected(true);
        }
        if (screenshot.matches("(Fail|Both)")) {
            failCheckBox.setSelected(true);
        }
        fullpagescreenshot.setSelected(execSettings.getRunSettings().getTakeFullPageScreenShot());

        reRunNo.getModel().setValue(execSettings.getRunSettings().getRerunTimes());
       // useExistingDriver.setSelected(execSettings.getRunSettings().useExistingDriver());
        reportPerformanceLog.setSelected(execSettings.getRunSettings().isPerformanceLogEnabled());
        bddReport.setSelected(execSettings.getRunSettings().isBddReportEnabled());
       // sendMail.setSelected(execSettings.getRunSettings().isMailSend());
       // excelReporting.setSelected(execSettings.getRunSettings().isExcelReport());
        extent.setSelected(execSettings.getRunSettings().isExtentReport());
        azure.setSelected(execSettings.getRunSettings().isAzureEnabled());
        rpUpdate.setSelected(execSettings.getRunSettings().isRPUpdate());
        slackNotify.setSelected(execSettings.getRunSettings().isSendNotification());

        /**
         * loading environments
         */
        testEnv.setModel(new DefaultComboBoxModel(getEnvList()));
        testEnv.setSelectedItem(execSettings.getRunSettings().getTestEnv());
    }

    private Object[] getEnvList() {
        return sProject.getTestData().getEnvironments().toArray();
    }

    private void loadTMSettings() {
        testMgmtModuleCombo.setModel(new DefaultComboBoxModel(
                sProject.getProjectSettings().getTestMgmtModule().getModuleNames().toArray()));
        if (testMgmtModuleCombo.getItemCount() > 0) {
            loadTMTestSetSettings(testMgmtModuleCombo.getSelectedItem().toString());
        }
    }

    private void loadTestSetTMSettings() {
        loadTMSettings();
        String updateToTM = execSettings.getTestMgmgtSettings().getUpdateResultsToTM();
        if (!"None".equals(updateToTM)) {
            updateresultscheckbox.setSelected(true);
            testMgmtModuleCombo.setSelectedItem(updateToTM);
        } else {
            updateresultscheckbox.setSelected(false);
        }
        updateresultscheckboxItemStateChanged(null);
    }
    
    private void loadRPSettings() {
        PropUtils.loadPropertiesInTable(
                sProject.getProjectSettings().getRPSettings(),
                rpSettingsPanel.table);
    }
    
    private void loadExtentSettings() {
        PropUtils.loadPropertiesInTable(
                sProject.getProjectSettings().getExtentSettings(),
                extentSettingsPanel.table);
    }

    private void setButtonModelFromText(String text, ButtonGroup Bgroup) {
        for (Enumeration<AbstractButton> buttons = Bgroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();
            if (button.getText().equals(text)) {
                button.setSelected(true);
            }
        }
    }

    private void saveRunSettings() {
        execSettings.getRunSettings().setExecutionTimeOut(executionTimeOut.getText());
        execSettings.getRunSettings().setExecutionMode(getSelectedButton(eModeBgroup));
        execSettings.getRunSettings().setThreadCount(threadCount.getValue().toString());
        execSettings.getRunSettings().setIterationMode(getSelectedButton(iModeBgroup));
        execSettings.getRunSettings().setRemoteGridURL(remoteGridURL.getText());
        execSettings.getRunSettings().setScreenShotFor(getPassFail());
        execSettings.getRunSettings().setTakeFullPageScreenShot(fullpagescreenshot.isSelected());
        execSettings.getRunSettings().setVideoEnabled(recordVideo.isSelected());
        execSettings.getRunSettings().setTracingEnabled(enableTracing.isSelected());
        execSettings.getRunSettings().setHARrecordingEnabled(enableHAR.isSelected());
        execSettings.getRunSettings().setRerunTimes(reRunNo.getModel().getValue().toString());
       // execSettings.getRunSettings().useExistingDriver(useExistingDriver.isSelected());
        execSettings.getRunSettings().setReportPerformanceLog(reportPerformanceLog.isSelected());
        execSettings.getRunSettings().setBddReport(bddReport.isSelected());
       // execSettings.getRunSettings().setMailSend(sendMail.isSelected());
       // execSettings.getRunSettings().setExcelReport(excelReporting.isSelected());
        execSettings.getRunSettings().setRPUpdate(rpUpdate.isSelected());
        execSettings.getRunSettings().setExtentReport(extent.isSelected());
        execSettings.getRunSettings().setAzureReport(azure.isSelected());
        execSettings.getRunSettings().setSlackNotification(slackNotify.isSelected());
        execSettings.getRunSettings().setTestEnv(testEnv.getSelectedItem().toString());
       // execSettings.getRunSettings().setAutoHealMode(autoHeal.isSelected());
        execSettings.getRunSettings().save();
        sMainFrame.reloadSettings();
    }

    private String getSelectedButton(ButtonGroup bGroup) {
        for (Enumeration<AbstractButton> buttons = bGroup.getElements(); buttons.hasMoreElements();) {
            AbstractButton button = buttons.nextElement();
            if (button.isSelected()) {
                return button.getText();
            }
        }
        return "None";
    }

    private String getPassFail() {
        if (passCheckBox.isSelected() && failCheckBox.isSelected()) {
            return "Both";
        }
        if (passCheckBox.isSelected()) {
            return "Pass";
        }
        if (failCheckBox.isSelected()) {
            return "Fail";
        }
        return "None";
    }

    private void saveTestSetTMSettings() {
        Properties properties = encryptpassword(PropUtils.getPropertiesFromTable(tsTMTable), "TMENC:");
        PropUtils.loadPropertiesInTable(properties, tsTMTable, "");
        if (updateresultscheckbox.isSelected()) {
            execSettings.getTestMgmgtSettings().set(PropUtils.getPropertiesFromTable(tsTMTable));
            execSettings.getTestMgmgtSettings().setUpdateResultsToTM(testMgmtModuleCombo.getSelectedItem().toString());
        } else {
            execSettings.getTestMgmgtSettings().setUpdateResultsToTM("None");
        }
    }

    private Properties encryptpassword(Properties properties, String feature) {
        properties.entrySet().forEach((e) -> {
            String key = (String) e.getKey();
            String value = (String) e.getValue();
            if (value != null && !value.isEmpty()) {
                if (key.toLowerCase().contains("passw")) {
                    if (feature.equals("TMENC:")) {
                        properties.setProperty(key, TMIntegration.encrypt(value));
                    } else {
                        properties.setProperty(key, Utility.encrypt(value));
                    }
                }
            }
        });
        return properties;
    }

    private void saveTMSettings() {
        sProject.getProjectSettings().getTestMgmtModule().save();
    }

    private void saveuserDefinedSettings() {
        sProject.getProjectSettings().getUserDefinedSettings().set(
                PropUtils.getPropertiesFromTable(uDPanel.table));
        sProject.getProjectSettings().getUserDefinedSettings().save();
    }

    
    private void saveRPSettings() {
        Properties properties = encryptpassword(PropUtils.getPropertiesFromTable(((XTablePanel) rpSettingsPanel).table), " Enc");
        PropUtils.loadPropertiesInTable(properties, rpSettingsPanel.table, "");
        sProject.getProjectSettings().getRPSettings().set(properties);
        sProject.getProjectSettings().getRPSettings().save();
    }
    
    private void saveExtentSettings() {
        Properties properties = encryptpassword(PropUtils.getPropertiesFromTable(((XTablePanel) extentSettingsPanel).table), " Enc");
        PropUtils.loadPropertiesInTable(properties, extentSettingsPanel.table, "");
        sProject.getProjectSettings().getExtentSettings().set(properties);
        sProject.getProjectSettings().getExtentSettings().save();
    }

    
    public void saveAll() {
        saveRunSettings();
        saveTestSetTMSettings();
        saveTMSettings();
        saveuserDefinedSettings();
        saveRPSettings();
        saveExtentSettings();
    }

    private void loadTMTestSetSettings(String module) {
        ((DefaultTableModel) tsTMTable.getModel()).setRowCount(0);
        if (execSettings != null) {
            if (execSettings.getTestMgmgtSettings().getUpdateResultsToTM().equals(module)) {
                PropUtils.loadPropertiesInTable(execSettings.getTestMgmgtSettings(), tsTMTable, "UpdateResultsToTM");
            }
        }
        if (tsTMTable.getRowCount() == 0) {
            List<Option> options = sProject.getProjectSettings().getTestMgmtModule().getModule(module).getOptions();
            for (Option option : options) {
                PropUtils.addValueinTable(tsTMTable, option.getName(), option.getValue());
            }
        }
        testConn.setIcon(DEFAULT_ICON);
    }

    public List<String> getUserDefinedList() {
        Set<String> udSet = new HashSet<>();
        for (int i = 0; i < uDPanel.table.getRowCount(); i++) {
            String key = Objects.toString(uDPanel.table.getValueAt(i, 0), "").trim();
            if (!key.isEmpty()) {
                udSet.add("%".concat(key).concat("%"));
            }

        }
        return new ArrayList<>(udSet);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        iModeBgroup = new javax.swing.ButtonGroup();
        eModeBgroup = new javax.swing.ButtonGroup();
        savePanel = new javax.swing.JPanel();
        saveSettings = new javax.swing.JButton();
        resetSettings = new javax.swing.JButton();
        runSettingsTab = new javax.swing.JTabbedPane();
        globalSettings = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        executionTimeOut = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        remoteGridURL = new javax.swing.JTextField();
        jRadioButton1 = new javax.swing.JRadioButton();
        jRadioButton2 = new javax.swing.JRadioButton();
        jRadioButton3 = new javax.swing.JRadioButton();
        jRadioButton4 = new javax.swing.JRadioButton();
        jLabel9 = new javax.swing.JLabel();
        passCheckBox = new javax.swing.JCheckBox();
        failCheckBox = new javax.swing.JCheckBox();
        fullpagescreenshot = new javax.swing.JCheckBox();
        threadCount = new javax.swing.JSpinner();
        jLabel29 = new javax.swing.JLabel();
        reRunNo = new javax.swing.JSpinner();
        jLabel30 = new javax.swing.JLabel();
        reportPerformanceLog = new javax.swing.JCheckBox();
        envLabel = new javax.swing.JLabel();
        testEnv = new javax.swing.JComboBox<>();
        bddReport = new javax.swing.JCheckBox();
        slackNotify = new javax.swing.JCheckBox();
        rpUpdate = new javax.swing.JCheckBox();
        extent = new javax.swing.JCheckBox();
        azure = new javax.swing.JCheckBox();
        recordVideo = new javax.swing.JCheckBox();
        enableTracing = new javax.swing.JCheckBox();
        enableHAR = new javax.swing.JCheckBox();
        qcrunSettings = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        tsTMTable = new XTable();
        jToolBar1 = new javax.swing.JToolBar();
        updateresultscheckbox = new javax.swing.JCheckBox();
        filler4 = new javax.swing.Box.Filler(new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 0), new java.awt.Dimension(20, 32767));
        testMgmtModuleCombo = new javax.swing.JComboBox();
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        testConn = new javax.swing.JButton();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        reset = new javax.swing.JButton();
        filler5 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 10), new java.awt.Dimension(0, 10), new java.awt.Dimension(32767, 10));

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Run Settings");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        savePanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        savePanel.setFont(new java.awt.Font("sansserif", 0, 11)); // NOI18N

        saveSettings.setFont(UIManager.getFont("TableMenu.font"));
        saveSettings.setText("Save");
        saveSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveSettingsActionPerformed(evt);
            }
        });

        resetSettings.setFont(UIManager.getFont("TableMenu.font"));
        resetSettings.setText("Restore");
        resetSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetSettingsActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout savePanelLayout = new javax.swing.GroupLayout(savePanel);
        savePanel.setLayout(savePanelLayout);
        savePanelLayout.setHorizontalGroup(
            savePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(savePanelLayout.createSequentialGroup()
                .addContainerGap(332, Short.MAX_VALUE)
                .addComponent(saveSettings)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(resetSettings)
                .addContainerGap())
        );
        savePanelLayout.setVerticalGroup(
            savePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(savePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(savePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(saveSettings)
                    .addComponent(resetSettings))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(savePanel, java.awt.BorderLayout.SOUTH);

        runSettingsTab.setFont(new java.awt.Font("sansserif", 0, 11)); // NOI18N

        globalSettings.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));

        jLabel1.setFont(UIManager.getFont("TableMenu.font"));
        jLabel1.setText("ExecutionTimeOut");

        executionTimeOut.setFont(UIManager.getFont("TableMenu.font"));
        executionTimeOut.setText("jTextField4");
        executionTimeOut.setToolTipText("in Minutes");

        jLabel2.setFont(UIManager.getFont("TableMenu.font"));
        jLabel2.setText("Parallel Execution");

        jLabel3.setFont(UIManager.getFont("TableMenu.font"));
        jLabel3.setText("Iteration Mode");

        jLabel4.setFont(UIManager.getFont("TableMenu.font"));
        jLabel4.setText("Execution Mode");

        jLabel5.setFont(UIManager.getFont("TableMenu.font"));
        jLabel5.setText("Remote Grid Url");

        remoteGridURL.setFont(UIManager.getFont("TableMenu.font"));
        remoteGridURL.setForeground(new java.awt.Color(0, 0, 255));

        eModeBgroup.add(jRadioButton1);
        jRadioButton1.setFont(new java.awt.Font("sansserif", 0, 11)); // NOI18N
        jRadioButton1.setText("Local");
        jRadioButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButton1ActionPerformed(evt);
            }
        });

        eModeBgroup.add(jRadioButton2);
        jRadioButton2.setFont(new java.awt.Font("sansserif", 0, 11)); // NOI18N
        jRadioButton2.setText("Grid");

        iModeBgroup.add(jRadioButton3);
        jRadioButton3.setFont(UIManager.getFont("TableMenu.font"));
        jRadioButton3.setText("ContinueOnError");

        iModeBgroup.add(jRadioButton4);
        jRadioButton4.setFont(UIManager.getFont("TableMenu.font"));
        jRadioButton4.setText("BreakOnError");

        jLabel9.setFont(UIManager.getFont("TableMenu.font"));
        jLabel9.setText("Screenshot");

        passCheckBox.setFont(UIManager.getFont("TableMenu.font"));
        passCheckBox.setText("Pass");
        passCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                passCheckBoxActionPerformed(evt);
            }
        });

        failCheckBox.setFont(UIManager.getFont("TableMenu.font"));
        failCheckBox.setText("Fail");

        fullpagescreenshot.setFont(UIManager.getFont("TableMenu.font"));
        fullpagescreenshot.setText("Take Full Page Screenshot");

        threadCount.setFont(new java.awt.Font("sansserif", 0, 11)); // NOI18N
        threadCount.setModel(new javax.swing.SpinnerNumberModel(1, 1, 100, 1));

        jLabel29.setFont(UIManager.getFont("TableMenu.font"));
        jLabel29.setText("Retry Execution");

        reRunNo.setFont(new java.awt.Font("sansserif", 0, 11)); // NOI18N
        reRunNo.setModel(new javax.swing.SpinnerNumberModel(0, 0, 5, 1));

        jLabel30.setFont(UIManager.getFont("TableMenu.font"));
        jLabel30.setText("Times");

        reportPerformanceLog.setFont(UIManager.getFont("TableMenu.font"));
        reportPerformanceLog.setText("Performance Reporting");
        reportPerformanceLog.setToolTipText("Report page performance logs.");

        envLabel.setFont(UIManager.getFont("TableMenu.font"));
        envLabel.setText("Environment");

        testEnv.setFont(UIManager.getFont("TableMenu.font"));
        testEnv.setForeground(new java.awt.Color(0, 0, 255));
        testEnv.setToolTipText("Select the execution Environment");

        bddReport.setFont(UIManager.getFont("TableMenu.font"));
        bddReport.setText("Bdd Reporting");

        slackNotify.setFont(UIManager.getFont("TableMenu.font"));
        slackNotify.setText("Slack Notification");
        slackNotify.setToolTipText("Send notification to slack");
        slackNotify.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                slackNotifyActionPerformed(evt);
            }
        });

        rpUpdate.setFont(UIManager.getFont("TableMenu.font"));
        rpUpdate.setText("Report Portal");

        extent.setFont(UIManager.getFont("TableMenu.font"));
        extent.setSelected(true);
        extent.setText("Extent Report");
        extent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                extentActionPerformed(evt);
            }
        });

        azure.setFont(UIManager.getFont("TableMenu.font"));
        azure.setText("Azure Nunit Report");
        azure.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                azureActionPerformed(evt);
            }
        });

        recordVideo.setFont(UIManager.getFont("TableMenu.font"));
        recordVideo.setText("Video");
        recordVideo.setToolTipText("Record Execution Video");
        recordVideo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recordVideoActionPerformed(evt);
            }
        });

        enableTracing.setFont(UIManager.getFont("TableMenu.font"));
        enableTracing.setText("Trace");
        enableTracing.setToolTipText("Enable Tracing");
        enableTracing.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableTracingActionPerformed(evt);
            }
        });

        enableHAR.setFont(UIManager.getFont("TableMenu.font"));
        enableHAR.setText("HAR");
        enableHAR.setToolTipText("Enable HAR Recording");
        enableHAR.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                enableHARActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout globalSettingsLayout = new javax.swing.GroupLayout(globalSettings);
        globalSettings.setLayout(globalSettingsLayout);
        globalSettingsLayout.setHorizontalGroup(
            globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalSettingsLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(globalSettingsLayout.createSequentialGroup()
                        .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel29)
                            .addGroup(globalSettingsLayout.createSequentialGroup()
                                .addGap(111, 111, 111)
                                .addComponent(reRunNo, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel30)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(globalSettingsLayout.createSequentialGroup()
                        .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(envLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(globalSettingsLayout.createSequentialGroup()
                                .addGap(4, 4, 4)
                                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(reportPerformanceLog)
                                    .addComponent(rpUpdate)
                                    .addComponent(slackNotify))
                                .addGap(60, 60, 60)
                                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(extent)
                                    .addComponent(bddReport)
                                    .addComponent(azure)))
                            .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, globalSettingsLayout.createSequentialGroup()
                                    .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel5)
                                        .addComponent(jLabel9))
                                    .addGap(81, 81, 81)
                                    .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(remoteGridURL, javax.swing.GroupLayout.PREFERRED_SIZE, 252, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(fullpagescreenshot)
                                        .addGroup(globalSettingsLayout.createSequentialGroup()
                                            .addComponent(passCheckBox)
                                            .addGap(18, 18, 18)
                                            .addComponent(failCheckBox)))
                                    .addGap(0, 29, Short.MAX_VALUE))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(enableTracing)
                                    .addComponent(recordVideo)
                                    .addComponent(enableHAR))
                                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, globalSettingsLayout.createSequentialGroup()
                                    .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel2)
                                        .addComponent(jLabel3)
                                        .addComponent(jLabel4)
                                        .addComponent(jLabel1))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(executionTimeOut, javax.swing.GroupLayout.PREFERRED_SIZE, 252, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(globalSettingsLayout.createSequentialGroup()
                                            .addComponent(jRadioButton1)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                            .addComponent(jRadioButton2))
                                        .addGroup(globalSettingsLayout.createSequentialGroup()
                                            .addComponent(jRadioButton3)
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addComponent(jRadioButton4))
                                        .addComponent(threadCount, javax.swing.GroupLayout.PREFERRED_SIZE, 116, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(testEnv, javax.swing.GroupLayout.PREFERRED_SIZE, 121, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGap(29, 29, 29))))
                        .addContainerGap(29, Short.MAX_VALUE))))
        );
        globalSettingsLayout.setVerticalGroup(
            globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(globalSettingsLayout.createSequentialGroup()
                .addGap(46, 46, 46)
                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(executionTimeOut, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(threadCount, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(jRadioButton3)
                    .addComponent(jRadioButton4))
                .addGap(18, 18, 18)
                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(envLabel)
                    .addComponent(testEnv, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jRadioButton1)
                    .addComponent(jRadioButton2))
                .addGap(18, 18, 18)
                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(remoteGridURL, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addGroup(globalSettingsLayout.createSequentialGroup()
                        .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(passCheckBox)
                            .addComponent(failCheckBox))
                        .addGap(18, 18, 18)
                        .addComponent(fullpagescreenshot)))
                .addGap(39, 39, 39)
                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel29)
                    .addComponent(reRunNo, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel30))
                .addGap(26, 26, 26)
                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(reportPerformanceLog)
                    .addComponent(recordVideo)
                    .addComponent(bddReport))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(rpUpdate)
                    .addComponent(enableTracing)
                    .addComponent(extent))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(globalSettingsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(azure)
                    .addComponent(enableHAR)
                    .addComponent(slackNotify))
                .addContainerGap(44, Short.MAX_VALUE))
        );

        runSettingsTab.addTab("Run Settings", globalSettings);

        qcrunSettings.setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
        qcrunSettings.setLayout(new java.awt.BorderLayout());

        tsTMTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null},
                {null, null},
                {null, null},
                {null, null}
            },
            new String [] {
                "Key", "Value"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        tsTMTable.setColumnSelectionAllowed(true);
        tsTMTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane5.setViewportView(tsTMTable);
        tsTMTable.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        TMSettingsControl.initTMTable(tsTMTable);

        qcrunSettings.add(jScrollPane5, java.awt.BorderLayout.CENTER);

        jToolBar1.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jToolBar1.setRollover(true);
        jToolBar1.setPreferredSize(new java.awt.Dimension(100, 50));

        updateresultscheckbox.setFont(new java.awt.Font("sansserif", 0, 11)); // NOI18N
        updateresultscheckbox.setText("Update Results back to");
        updateresultscheckbox.setFocusable(false);
        updateresultscheckbox.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        updateresultscheckbox.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        updateresultscheckbox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                updateresultscheckboxItemStateChanged(evt);
            }
        });
        jToolBar1.add(updateresultscheckbox);
        jToolBar1.add(filler4);

        testMgmtModuleCombo.setFont(new java.awt.Font("sansserif", 0, 11)); // NOI18N
        testMgmtModuleCombo.setMinimumSize(new java.awt.Dimension(150, 25));
        testMgmtModuleCombo.setPreferredSize(new java.awt.Dimension(150, 25));
        testMgmtModuleCombo.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                testMgmtModuleComboItemStateChanged(evt);
            }
        });
        jToolBar1.add(testMgmtModuleCombo);
        jToolBar1.add(filler3);

        testConn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/ui/resources/toolbar/bulb_yellow.png"))); // NOI18N
        testConn.setText("Test Connection");
        testConn.setFocusable(false);
        testConn.setHorizontalTextPosition(javax.swing.SwingConstants.RIGHT);
        testConn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                testConnActionPerformed(evt);
            }
        });
        jToolBar1.add(testConn);
        jToolBar1.add(jSeparator1);

        reset.setText("Reset");
        reset.setFocusable(false);
        reset.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        reset.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        reset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resetActionPerformed(evt);
            }
        });
        jToolBar1.add(reset);

        qcrunSettings.add(jToolBar1, java.awt.BorderLayout.NORTH);

        runSettingsTab.addTab("TM Settings", qcrunSettings);

        getContentPane().add(runSettingsTab, java.awt.BorderLayout.CENTER);
        getContentPane().add(filler5, java.awt.BorderLayout.PAGE_START);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void resetSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetSettingsActionPerformed
        loadAll();
        Notification.show("Old Values Loaded");
    }//GEN-LAST:event_resetSettingsActionPerformed

    private void saveSettingsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveSettingsActionPerformed
        saveAll();
        Notification.show("Settings Saved");
    }//GEN-LAST:event_saveSettingsActionPerformed

    private void updateresultscheckboxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_updateresultscheckboxItemStateChanged
        tsTMTable.setEnabled(updateresultscheckbox.isSelected());
        testConn.setEnabled(updateresultscheckbox.isSelected());
        reset.setEnabled(updateresultscheckbox.isSelected());
    }//GEN-LAST:event_updateresultscheckboxItemStateChanged

    private void testMgmtModuleComboItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_testMgmtModuleComboItemStateChanged
        if (evt.getStateChange() == ItemEvent.SELECTED) {
            loadTMTestSetSettings(testMgmtModuleCombo.getSelectedItem().toString());
        }
    }//GEN-LAST:event_testMgmtModuleComboItemStateChanged

    private void testConnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_testConnActionPerformed
        String module = Objects.toString(testMgmtModuleCombo.getSelectedItem(), "");
        execSettings.getTestMgmgtSettings().set(PropUtils.getPropertiesFromTable(tsTMTable));
        execSettings.getTestMgmgtSettings().setUpdateResultsToTM(module);
        Sync tm = TMIntegration.getInstance(execSettings.getTestMgmgtSettings());
        testConnection(tm);
    }//GEN-LAST:event_testConnActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        sMainFrame.getTestExecution().getTestSetComp().reloadSettings();
    }//GEN-LAST:event_formWindowClosing

    private void resetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resetActionPerformed
        if (testMgmtModuleCombo.getSelectedItem() != null) {
            String module = testMgmtModuleCombo.getSelectedItem().toString();
            ((DefaultTableModel) tsTMTable.getModel()).setRowCount(0);
            List<Option> options = sProject.getProjectSettings().getTestMgmtModule().getModule(module).getOptions();
            for (Option option : options) {
                PropUtils.addValueinTable(tsTMTable, option.getName(), option.getValue());
            }
        }
    }//GEN-LAST:event_resetActionPerformed

    private void slackNotifyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_slackNotifyActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_slackNotifyActionPerformed

    private void extentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_extentActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_extentActionPerformed

    private void azureActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_azureActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_azureActionPerformed

    private void passCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_passCheckBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_passCheckBoxActionPerformed

    private void jRadioButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButton1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jRadioButton1ActionPerformed

    private void recordVideoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recordVideoActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_recordVideoActionPerformed

    private void enableTracingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableTracingActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_enableTracingActionPerformed

    private void enableHARActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_enableHARActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_enableHARActionPerformed

    private void testConnection(final Sync connection) {
        try {
            if (connection != null) {
                this.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
                if (connection.isConnected()) {
                    testConn.setIcon(PASS_ICON);
                    this.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
                    return;

                }
            }
        } catch (Exception ex) {
            Logger.getLogger(INGeniousSettings.class
                    .getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
        this.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.DEFAULT_CURSOR));
        testConn.setIcon(FAIL_ICON);
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox azure;
    private javax.swing.JCheckBox bddReport;
    private javax.swing.ButtonGroup eModeBgroup;
    private javax.swing.JCheckBox enableHAR;
    private javax.swing.JCheckBox enableTracing;
    private javax.swing.JLabel envLabel;
    private javax.swing.JTextField executionTimeOut;
    private javax.swing.JCheckBox extent;
    private javax.swing.JCheckBox failCheckBox;
    private javax.swing.Box.Filler filler3;
    private javax.swing.Box.Filler filler4;
    private javax.swing.Box.Filler filler5;
    private javax.swing.JCheckBox fullpagescreenshot;
    private javax.swing.JPanel globalSettings;
    private javax.swing.ButtonGroup iModeBgroup;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel29;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel30;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JRadioButton jRadioButton1;
    private javax.swing.JRadioButton jRadioButton2;
    private javax.swing.JRadioButton jRadioButton3;
    private javax.swing.JRadioButton jRadioButton4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JCheckBox passCheckBox;
    private javax.swing.JPanel qcrunSettings;
    private javax.swing.JSpinner reRunNo;
    private javax.swing.JCheckBox recordVideo;
    private javax.swing.JTextField remoteGridURL;
    private javax.swing.JCheckBox reportPerformanceLog;
    private javax.swing.JButton reset;
    private javax.swing.JButton resetSettings;
    private javax.swing.JCheckBox rpUpdate;
    private javax.swing.JTabbedPane runSettingsTab;
    private javax.swing.JPanel savePanel;
    private javax.swing.JButton saveSettings;
    private javax.swing.JCheckBox slackNotify;
    private javax.swing.JButton testConn;
    private javax.swing.JComboBox<String> testEnv;
    private javax.swing.JComboBox testMgmtModuleCombo;
    private javax.swing.JSpinner threadCount;
    private javax.swing.JTable tsTMTable;
    private javax.swing.JCheckBox updateresultscheckbox;
    // End of variables declaration//GEN-END:variables
}
