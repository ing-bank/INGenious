
package com.ing.ide.main.mainui.components.testexecution.quickSettings;

import com.ing.datalib.settings.RunSettings;
import javax.swing.UIManager;

/**
 * QuickSettings panel for left side
 *
 * <br><br><br>
 *
 *
 */
public class QuickSettingsUILeft extends QuickSettingsUI {

    RunSettings runSettings;

    /**
     * Creates new form QuickSettingsUI
     *
     */
    public QuickSettingsUILeft() {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        iterGroup = new javax.swing.ButtonGroup();
        iterMode = new javax.swing.JLabel();
        ContinueOnError = new javax.swing.JRadioButton();
        BreakOnError = new javax.swing.JRadioButton();
        screenShotFor = new javax.swing.JLabel();
        passCheckBox = new javax.swing.JCheckBox();
        failCheckBox = new javax.swing.JCheckBox();
        useExistingDriver = new javax.swing.JCheckBox();
        reportPerformanceLog = new javax.swing.JCheckBox();
        fullpagescreenshot = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JSeparator();
        jSeparator2 = new javax.swing.JSeparator();

        iterGroup.add(this.ContinueOnError);
        iterGroup.add(this.BreakOnError);

        iterMode.setFont(UIManager.getFont("Table.font"));
        iterMode.setText("Iteration Mode");

        ContinueOnError.setFont(UIManager.getFont("TableMenu.font"));
        ContinueOnError.setText("Continue");
        ContinueOnError.setName("ContinueOnError"); // NOI18N
        ContinueOnError.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ContinueOnErrorActionPerformed(evt);
            }
        });

        BreakOnError.setFont(UIManager.getFont("TableMenu.font"));
        BreakOnError.setText("Break");
        BreakOnError.setName("BreakOnError"); // NOI18N
        BreakOnError.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BreakOnErrorActionPerformed(evt);
            }
        });

        screenShotFor.setFont(UIManager.getFont("Table.font"));
        screenShotFor.setText("Screenshot");

        passCheckBox.setFont(UIManager.getFont("TableMenu.font"));
        passCheckBox.setText("Pass");
        passCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                passCheckBoxActionPerformed(evt);
            }
        });

        failCheckBox.setFont(UIManager.getFont("TableMenu.font"));
        failCheckBox.setText("Fail");
        failCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                failCheckBoxActionPerformed(evt);
            }
        });

        useExistingDriver.setFont(UIManager.getFont("TableMenu.font"));
        useExistingDriver.setText("Use Existing Browser");
        useExistingDriver.setToolTipText("Will use the same browser instance for all the TestCases and Iterations in Testsets");
        useExistingDriver.setEnabled(false);
        useExistingDriver.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useExistingDriverActionPerformed(evt);
            }
        });

        reportPerformanceLog.setFont(UIManager.getFont("TableMenu.font"));
        reportPerformanceLog.setText("Performance Reporting");
        reportPerformanceLog.setToolTipText("Report page performance logs.");
        reportPerformanceLog.setFocusCycleRoot(true);
        reportPerformanceLog.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reportPerformanceLogActionPerformed(evt);
            }
        });

        fullpagescreenshot.setFont(UIManager.getFont("TableMenu.font"));
        fullpagescreenshot.setText("Take Full Page Screenshot");
        fullpagescreenshot.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fullpagescreenshotActionPerformed(evt);
            }
        });

        jLabel1.setText("(On Error)");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fullpagescreenshot)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(passCheckBox)
                        .addGap(18, 18, 18)
                        .addComponent(failCheckBox))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(ContinueOnError)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(BreakOnError))
                    .addComponent(screenShotFor)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(useExistingDriver, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(reportPerformanceLog))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(iterMode)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel1)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jSeparator1)
            .addComponent(jSeparator2)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(iterMode)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ContinueOnError)
                    .addComponent(BreakOnError))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, 5, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(screenShotFor)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(passCheckBox)
                    .addComponent(failCheckBox))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(fullpagescreenshot)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 4, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(7, 7, 7)
                .addComponent(useExistingDriver)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(reportPerformanceLog)
                .addContainerGap(20, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void ContinueOnErrorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ContinueOnErrorActionPerformed
        runSettings.setIterationMode(ContinueOnError.getName());
    }//GEN-LAST:event_ContinueOnErrorActionPerformed

    private void BreakOnErrorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_BreakOnErrorActionPerformed
        runSettings.setIterationMode(BreakOnError.getName());
    }//GEN-LAST:event_BreakOnErrorActionPerformed

    private void passCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_passCheckBoxActionPerformed
        runSettings.setScreenShotFor(getPassFail());
    }//GEN-LAST:event_passCheckBoxActionPerformed

    private void failCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_failCheckBoxActionPerformed
        runSettings.setScreenShotFor(getPassFail());
    }//GEN-LAST:event_failCheckBoxActionPerformed

    private void fullpagescreenshotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fullpagescreenshotActionPerformed
        runSettings.setTakeFullPageScreenShot(fullpagescreenshot.isSelected());
    }//GEN-LAST:event_fullpagescreenshotActionPerformed

    private void useExistingDriverActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useExistingDriverActionPerformed
        runSettings.useExistingDriver(useExistingDriver.isSelected());
    }//GEN-LAST:event_useExistingDriverActionPerformed

    private void reportPerformanceLogActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reportPerformanceLogActionPerformed
        runSettings.setReportPerformanceLog(reportPerformanceLog.isSelected());
    }//GEN-LAST:event_reportPerformanceLogActionPerformed
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

    @Override
    protected void updateUI(RunSettings x) {
        this.runSettings = x;
        String im = x.getIterationMode();
        if ("ContinueOnError".equalsIgnoreCase(im)) {
            ContinueOnError.setSelected(true);
        } else {
            BreakOnError.setSelected(true);
        }
        String screenshot = x.getScreenShotFor();
        passCheckBox.setSelected(false);
        failCheckBox.setSelected(false);
        if (screenshot.matches("(Pass|Both)")) {
            passCheckBox.setSelected(true);
        }
        if (screenshot.matches("(Fail|Both)")) {
            failCheckBox.setSelected(true);
        }
        fullpagescreenshot.setSelected(x.getTakeFullPageScreenShot());
        useExistingDriver.setSelected(x.useExistingDriver());
        reportPerformanceLog.setSelected(x.isPerformanceLogEnabled());
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JRadioButton BreakOnError;
    private javax.swing.JRadioButton ContinueOnError;
    private javax.swing.JCheckBox failCheckBox;
    private javax.swing.JCheckBox fullpagescreenshot;
    private javax.swing.ButtonGroup iterGroup;
    private javax.swing.JLabel iterMode;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JCheckBox passCheckBox;
    private javax.swing.JCheckBox reportPerformanceLog;
    private javax.swing.JLabel screenShotFor;
    private javax.swing.JCheckBox useExistingDriver;
    // End of variables declaration//GEN-END:variables
}
