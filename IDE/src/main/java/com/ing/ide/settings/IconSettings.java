
package com.ing.ide.settings;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 *
 * 
 */
public class IconSettings {

    private final Icon testPlanRoot = new ImageIcon(getClass().getResource("/ui/resources/treeicons/testplan/Root.png"));
    private final Icon testPlanScenario = new ImageIcon(getClass().getResource("/ui/resources/treeicons/testplan/Scenario.png"));
    private final Icon testPlanTestCase = new ImageIcon(getClass().getResource("/ui/resources/treeicons/testplan/TestCase.png"));
    private final Icon testLabRoot = new ImageIcon(getClass().getResource("/ui/resources/treeicons/testlab/Root.png"));
    private final Icon testLabRelease = new ImageIcon(getClass().getResource("/ui/resources/treeicons/testlab/Release.png"));
    private final Icon testLabTestSet = new ImageIcon(getClass().getResource("/ui/resources/treeicons/testlab/TestSet.png"));
    private final Icon oRRoot = new ImageIcon(getClass().getResource("/ui/resources/treeicons/or/Root.png"));
    private final Icon oRPage = new ImageIcon(getClass().getResource("/ui/resources/treeicons/or/Page.png"));
    private final Icon oRObject = new ImageIcon(getClass().getResource("/ui/resources/treeicons/or/Object.png"));
    private final Icon iORRoot = oRRoot;
    private final Icon iORPage = oRPage;
    private final Icon iORObject = oRObject;
    private final Icon iORGroup = new ImageIcon(getClass().getResource("/ui/resources/treeicons/or/Group.PNG"));
    private final Icon reusableRoot = new ImageIcon(getClass().getResource("/ui/resources/treeicons/reusable/Root.gif"));
    private final Icon reusableFolder = new ImageIcon(getClass().getResource("/ui/resources/treeicons/reusable/Folder.gif"));
    private final Icon reusableTestCase = new ImageIcon(getClass().getResource("/ui/resources/treeicons/reusable/TestCase.gif"));
    private final ImageIcon objectSpyLarge = new ImageIcon(getClass().getResource("/ui/resources/ORspy.png"));
    private final ImageIcon objectHealLarge = new ImageIcon(getClass().getResource("/ui/resources/ORHeal.png"));
    private final ImageIcon recorderLarge = new ImageIcon(getClass().getResource("/ui/resources/recorder/recorder.png"));
    private final Icon startIcon = new ImageIcon(getClass().getResource("/ui/resources/exe.png"));
    private final Icon startDebugIcon = new ImageIcon(getClass().getResource("/ui/resources/debug.png"));
    private final Icon stopIcon = new ImageIcon(getClass().getResource("/ui/resources/stop.png"));
    private final Icon recordStartIcon = new ImageIcon(getClass().getResource("/ui/resources/recorder/record_start.png"));
    private final Icon recordStopIcon = new ImageIcon(getClass().getResource("/ui/resources/recorder/record_stop.png"));
    private final ImageIcon settingsGear = new ImageIcon(getClass().getResource("/ui/resources/settings.png"));
    private final ImageIcon mobileObjectGrabb = new ImageIcon(getClass().getResource("/ui/resources/appStore.png"));

    private final ImageIcon helpIcon = new ImageIcon(getClass().getResource("/ui/resources/ask.png"));

    private static IconSettings iconSettings;

    public static IconSettings getIconSettings() {
        if (iconSettings == null) {
            iconSettings = new IconSettings();
        }
        return iconSettings;
    }

    public Icon getTestPlanRoot() {
        return testPlanRoot;
    }

    public Icon getTestPlanScenario() {
        return testPlanScenario;
    }

    public Icon getTestPlanTestCase() {
        return testPlanTestCase;
    }

    public Icon getTestLabRoot() {
        return testLabRoot;
    }

    public Icon getTestLabRelease() {
        return testLabRelease;
    }

    public Icon getTestLabTestSet() {
        return testLabTestSet;
    }

    public Icon getORRoot() {
        return oRRoot;
    }

    public Icon getORPage() {
        return oRPage;
    }

    public Icon getORObject() {
        return oRObject;
    }

    public Icon getIORRoot() {
        return iORRoot;
    }

    public Icon getIORPage() {
        return iORPage;
    }

    public Icon getIORGroup() {
        return iORGroup;
    }

    public Icon getIORObject() {
        return iORObject;
    }

    public Icon getReusableRoot() {
        return reusableRoot;
    }

    public Icon getReusableFolder() {
        return reusableFolder;
    }

    public Icon getReusableTestCase() {
        return reusableTestCase;
    }

    public ImageIcon getObjectSpyLarge() {
        return objectSpyLarge;
    }

    public ImageIcon getObjectHealLarge() {
        return objectHealLarge;
    }

    public Icon getStartIcon() {
        return startIcon;
    }

    public Icon getStartDebugIcon() {
        return startDebugIcon;
    }

    public Icon getStopIcon() {
        return stopIcon;
    }

    public Icon getRecordStartIcon() {
        return recordStartIcon;
    }

    public Icon getRecordStopIcon() {
        return recordStopIcon;
    }

    public ImageIcon getRecorderLarge() {
        return recorderLarge;
    }

    public ImageIcon getSettingsGear() {
        return settingsGear;
    }

    public ImageIcon getMobileObjectGrabb() {
        return mobileObjectGrabb;
    }

    public ImageIcon getHelpIcon() {
        return helpIcon;
    }
}
