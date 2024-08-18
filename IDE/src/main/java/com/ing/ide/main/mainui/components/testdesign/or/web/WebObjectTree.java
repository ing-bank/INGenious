
package com.ing.ide.main.mainui.components.testdesign.or.web;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.web.WebORObject;
import com.ing.ide.main.mainui.components.testdesign.or.ObjectTree;
import java.util.List;
import javax.swing.tree.TreePath;

/**
 *
 * 
 */
public class WebObjectTree extends ObjectTree {

    private final WebORPanel oRPanel;

    public WebObjectTree(WebORPanel sProxy) {
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

    void changeFrameData(String frameText) {
        WebORObject obj = (WebORObject) getSelectedObject();
        if (obj != null) {
            obj.setFrame(frameText);
        }
    }

    @Override
    public void showImpactedTestCases(List<TestCase> testcases, String pageName, String objectName) {
        oRPanel.getTestDesign().getImpactUI().loadForObject(testcases, pageName, objectName);
    }

    @Override
    public ORRootInf getOR() {
        return oRPanel.getProject().getObjectRepository().getWebOR();
    }

    @Override
    protected void objectRemoved(ORObjectInf object) {
        if (getLoadedObject() != null
                && getLoadedObject().equals(object)) {
            oRPanel.getObjectTable().reset();
        }
        super.objectRemoved(object);
    }

    public WebORObject getLoadedObject() {
        return oRPanel.getObjectTable().getObject();
    }

}
