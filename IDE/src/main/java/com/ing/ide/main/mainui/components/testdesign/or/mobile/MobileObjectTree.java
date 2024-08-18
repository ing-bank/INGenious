
package com.ing.ide.main.mainui.components.testdesign.or.mobile;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.ide.main.mainui.components.testdesign.or.ObjectTree;
import java.util.List;
import javax.swing.tree.TreePath;

/**
 *
 * 
 */
public class MobileObjectTree extends ObjectTree {

    private final MobileORPanel oRPanel;

    public MobileObjectTree(MobileORPanel sProxy) {
        this.oRPanel = sProxy;
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
        return oRPanel.getProject().getObjectRepository().getMobileOR();
    }

    @Override
    protected void objectRemoved(ORObjectInf object) {
        if (getLoadedObject() != null
                && getLoadedObject().equals(object)) {
            oRPanel.getObjectTable().reset();
        }
        super.objectRemoved(object);
    }

    public MobileORObject getLoadedObject() {
        return oRPanel.getObjectTable().getObject();
    }

    
}
