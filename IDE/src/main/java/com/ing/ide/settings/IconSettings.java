
package com.ing.ide.settings;

import com.ing.ide.main.fx.INGIcons;
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Singleton that provides all tree/toolbar icons used throughout the IDE.
 * Icons are now Ikonli font icons via INGIcons, with PNG fallbacks.
 */
public class IconSettings {

    private static final int TREE_SIZE = 16;
    private static final int TOOLBAR_SIZE = 18;
    private static final int LARGE_SIZE = 24;

    private final Icon testPlanRoot = icon("testplan.Root", TREE_SIZE);
    private final Icon testPlanScenario = icon("testplan.Scenario", TREE_SIZE);
    private final Icon testPlanTestCase = icon("testplan.TestCase", TREE_SIZE);
    private final Icon testLabRoot = icon("testlab.Root", TREE_SIZE);
    private final Icon testLabRelease = icon("testlab.Release", TREE_SIZE);
    private final Icon testLabTestSet = icon("testlab.TestSet", TREE_SIZE);
    private final Icon oRRoot = icon("or.Root", TREE_SIZE);
    private final Icon oRPage = icon("or.Page", TREE_SIZE);
    private final Icon oRObject = icon("or.Object", TREE_SIZE);
    private final Icon iORRoot = oRRoot;
    private final Icon iORPage = oRPage;
    private final Icon iORObject = oRObject;
    private final Icon iORGroup = icon("or.Group", TREE_SIZE);
    private final Icon reusableRoot = icon("reusable.Root", TREE_SIZE);
    private final Icon reusableFolder = icon("reusable.Folder", TREE_SIZE);
    private final Icon reusableTestCase = icon("reusable.TestCase", TREE_SIZE);
    private final Icon objectSpyLarge = icon("objectSpy", LARGE_SIZE);
    private final Icon objectHealLarge = icon("objectHeal", LARGE_SIZE);
    private final Icon recorderLarge = icon("recorder", LARGE_SIZE);
    private final Icon startIcon = icon("exe", TOOLBAR_SIZE);
    private final Icon startDebugIcon = icon("debug", TOOLBAR_SIZE);
    private final Icon stopIcon = icon("stop", TOOLBAR_SIZE);
    private final Icon recordStartIcon = icon("record_start", TOOLBAR_SIZE);
    private final Icon recordStopIcon = icon("record_stop", TOOLBAR_SIZE);
    private final Icon settingsGear = icon("settings", TOOLBAR_SIZE);
    private final Icon mobileObjectGrabb = icon("appStore", TOOLBAR_SIZE);
    private final Icon helpIcon = icon("ask", TOOLBAR_SIZE);

    /**
     * Creates a colorful Swing icon from INGIcons, using the icon's
     * registered semantic color, falling back to a PNG resource.
     */
    private static Icon icon(String name, int size) {
        Icon ic = INGIcons.swingColored(name, size);
        return ic != null ? ic : new ImageIcon();
    }

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

    public Icon getObjectSpyLarge() {
        return objectSpyLarge;
    }

    public Icon getObjectHealLarge() {
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

    public Icon getRecorderLarge() {
        return recorderLarge;
    }

    public Icon getSettingsGear() {
        return settingsGear;
    }

    public Icon getMobileObjectGrabb() {
        return mobileObjectGrabb;
    }

    public Icon getHelpIcon() {
        return helpIcon;
    }
}
