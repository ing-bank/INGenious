package com.ing.ide.main.mainui.components.testdesign.or.mobile;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.ide.main.mainui.components.testdesign.or.ObjectTree;
import java.util.List;
import javax.swing.tree.TreePath;

public class MobileObjectTree extends ObjectTree {

    private final MobileORPanel oRPanel;
    private final ORSource source;

    public MobileObjectTree(MobileORPanel panel, ORSource source) {
        this.oRPanel = panel;
        this.source = source;
    }

    @Override
    public void loadTableModelForSelection() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            oRPanel.loadTableModelForSelection(path.getLastPathComponent());
        }
    }

    @Override
    public Project getProject() {
        return oRPanel.getProject();
    }

    @Override
    public void showImpactedTestCases(List<TestCase> testcases, String pageName, String objectName) {
        oRPanel.getTestDesign().getImpactUI().loadForObject(testcases, pageName, objectName);
    }

    @Override
    public ORRootInf getOR() {
        ObjectRepository repo = oRPanel.getProject().getObjectRepository();
        return (source == ORSource.SHARED) ? repo.getMobileSharedOR() : repo.getMobileOR();
    }

    @Override
    protected void objectRemoved(ORObjectInf object) {
        ORObjectInf loaded = getLoadedObject();
        if (loaded != null && loaded.equals(object)) {
            oRPanel.getObjectTable().reset();
        }
        super.objectRemoved(object);
    }

    public MobileORObject getLoadedObject() {
        return oRPanel.getObjectTable().getObject();
    }

    public enum ORSource { 
        PROJECT, SHARED 
    }

    public ORSource getSource() { 
        return source; 
    }
    
    public MobileORPanel getORPanel() {
        return oRPanel;
    }
}