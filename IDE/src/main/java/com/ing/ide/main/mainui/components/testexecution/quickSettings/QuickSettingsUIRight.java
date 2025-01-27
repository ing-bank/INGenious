
package com.ing.ide.main.mainui.components.testexecution.quickSettings;

import com.ing.datalib.settings.RunSettings;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.UIManager;

/**
 * QuickSettings panel for right side
 *
 * <br><br><br>
 *
 *
 */
public abstract class QuickSettingsUIRight extends QuickSettingsUI {

    RunSettings runSettings;

    /**
     * Creates new form QuickSettingsUI
     *
     */
    public QuickSettingsUIRight() {
        initComponents();
        listenToGridUrl();
        alterDefaultKeyBindings(gridUrl);
    }

    private void listenToGridUrl() {
        gridUrl.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent de) {
                changeGridUrl();
            }

            @Override
            public void removeUpdate(DocumentEvent de) {
                changeGridUrl();
            }

            @Override
            public void changedUpdate(DocumentEvent de) {
                changeGridUrl();
            }
        });
    }

    private void changeGridUrl() {
        if (gridMode.isSelected()) {
            runSettings.setRemoteGridURL(gridUrl.getText());
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

        buttonGroup1 = new javax.swing.ButtonGroup();
        jLabel4 = new javax.swing.JLabel();
        testEnv = new javax.swing.JComboBox<>();
        qsParrelExelabel = new javax.swing.JLabel();
        threadCount = new javax.swing.JSpinner();
        jLabel1 = new javax.swing.JLabel();
        qsExeTimoutLabel = new javax.swing.JLabel();
        executionTimeOut = new javax.swing.JSpinner();
        jLabel2 = new javax.swing.JLabel();
        qsRetryExeLabel = new javax.swing.JLabel();
        reRunNo = new javax.swing.JSpinner();
        jLabel3 = new javax.swing.JLabel();
        exeModeLabel = new javax.swing.JLabel();
        localMode = new javax.swing.JRadioButton();
        gridMode = new javax.swing.JRadioButton();
        gridUrl = new javax.swing.JTextField();

        jLabel4.setFont(UIManager.getFont("Table.font"));
        jLabel4.setText("Environment");

        testEnv.setFont(UIManager.getFont("Table.font"));
        testEnv.setForeground(new java.awt.Color(0, 0, 255));
        testEnv.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Default", "QA", "UAT" }));
        testEnv.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                testEnvItemStateChanged(evt);
            }
        });

        qsParrelExelabel.setFont(UIManager.getFont("Table.font"));
        qsParrelExelabel.setText("Parallel Execution");

        threadCount.setFont(new java.awt.Font("sansserif", 0, 11)); // NOI18N
        threadCount.setModel(new javax.swing.SpinnerNumberModel(1, 1, 100, 1));
        threadCount.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                threadCountStateChanged(evt);
            }
        });

        jLabel1.setText("Threads");

        qsExeTimoutLabel.setFont(UIManager.getFont("Table.font"));
        qsExeTimoutLabel.setText("ExecutionTimeOut");

        executionTimeOut.setFont(new java.awt.Font("sansserif", 0, 11)); // NOI18N
        executionTimeOut.setModel(new javax.swing.SpinnerNumberModel(300, 1, null, 1));
        executionTimeOut.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                executionTimeOutStateChanged(evt);
            }
        });

        jLabel2.setText("Minutes");

        qsRetryExeLabel.setFont(UIManager.getFont("Table.font"));
        qsRetryExeLabel.setText("Retry Execution");

        reRunNo.setFont(new java.awt.Font("sansserif", 0, 11)); // NOI18N
        reRunNo.setModel(new javax.swing.SpinnerNumberModel(0, 0, 5, 1));
        reRunNo.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                reRunNoStateChanged(evt);
            }
        });

        jLabel3.setText("Times");

        exeModeLabel.setFont(UIManager.getFont("Table.font"));
        exeModeLabel.setText("Execution Mode");

        buttonGroup1.add(localMode);
        localMode.setFont(UIManager.getFont("TableMenu.font"));
        localMode.setText("Local");

        buttonGroup1.add(gridMode);
        gridMode.setFont(UIManager.getFont("TableMenu.font"));
        gridMode.setSelected(true);
        gridMode.setText("Grid");
        gridMode.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                gridModeItemStateChanged(evt);
            }
        });

        gridUrl.setFont(UIManager.getFont("TableMenu.font"));
        gridUrl.setForeground(new java.awt.Color(0, 0, 255));
        gridUrl.setText("http://localhost:4444/wd/hub ");
        gridUrl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                gridUrlActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(testEnv, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                        .addComponent(qsRetryExeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(reRunNo))
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                        .addComponent(qsExeTimoutLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(executionTimeOut))
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                        .addComponent(qsParrelExelabel, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(threadCount, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel1)))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(exeModeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 111, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(localMode)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(gridMode))
                    .addComponent(gridUrl))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(testEnv, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(qsParrelExelabel, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(threadCount, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(qsExeTimoutLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(executionTimeOut, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(qsRetryExeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(reRunNo, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exeModeLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(localMode)
                    .addComponent(gridMode))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(gridUrl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void threadCountStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_threadCountStateChanged
        runSettings.setThreadCount(String.valueOf(threadCount.getValue()));
    }//GEN-LAST:event_threadCountStateChanged

    private void reRunNoStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_reRunNoStateChanged
        runSettings.setRerunTimes(String.valueOf(reRunNo.getValue()));
    }//GEN-LAST:event_reRunNoStateChanged

    private void executionTimeOutStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_executionTimeOutStateChanged
        runSettings.setExecutionTimeOut(String.valueOf(executionTimeOut.getValue()));
    }//GEN-LAST:event_executionTimeOutStateChanged

    private void testEnvItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_testEnvItemStateChanged
        runSettings.setTestEnv(testEnv.getSelectedItem().toString());
    }//GEN-LAST:event_testEnvItemStateChanged

    private void gridModeItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_gridModeItemStateChanged
        gridUrl.setEnabled(evt.getStateChange() == ItemEvent.SELECTED);
        if (gridMode.isSelected()) {
            runSettings.setExecutionMode("Grid");
        } else {
            runSettings.setExecutionMode("Local");
        }
    }//GEN-LAST:event_gridModeItemStateChanged

    private void gridUrlActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_gridUrlActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_gridUrlActionPerformed

    @Override
    protected void updateUI(RunSettings x) {
        this.runSettings = x;
        threadCount.setValue(runSettings.getThreadCount());
        executionTimeOut.setValue(runSettings.getExecutionTimeOut());
        reRunNo.setValue(runSettings.getRerunTimes());
        testEnv.setModel(new DefaultComboBoxModel(getEnvList()));
        testEnv.setSelectedItem(runSettings.getTestEnv());
        if (runSettings.getExecutionMode().equalsIgnoreCase("grid")) {
            gridUrl.setText(runSettings.getRemoteGridURL());
            gridMode.setSelected(true);
        } else {
            localMode.setSelected(true);
        }
    }
    
    private void alterDefaultKeyBindings(JTextField textField) {
        // Customize key bindings
        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Remove default Ctrl key bindings
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "none");
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "none");
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "none");
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "none");

        // Add Cmd key bindings
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "cut");
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "copy");
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "paste");
        textField.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "selectAll");
        textField.getActionMap().put("selectAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textField.selectAll();
            }
        });

    }

     public abstract Object[] getEnvList() ;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JLabel exeModeLabel;
    private javax.swing.JSpinner executionTimeOut;
    private javax.swing.JRadioButton gridMode;
    private javax.swing.JTextField gridUrl;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JRadioButton localMode;
    private javax.swing.JLabel qsExeTimoutLabel;
    private javax.swing.JLabel qsParrelExelabel;
    private javax.swing.JLabel qsRetryExeLabel;
    private javax.swing.JSpinner reRunNo;
    private javax.swing.JComboBox<String> testEnv;
    private javax.swing.JSpinner threadCount;
    // End of variables declaration//GEN-END:variables
}
