package com.ing.ide.main.mainui.components.testdesign.or.structureddata;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.structureddata.StructuredDataORObject;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.or.ObjectTree;
import java.util.List;
import javax.swing.tree.TreePath;

/**
 * Tree component for Structured Data Object Repository.
 */
public class StructuredDataObjectTree extends ObjectTree {
    private final StructuredDataORPanel oRPanel;
    private final ORSource source;

    public StructuredDataObjectTree(StructuredDataORPanel panel, ORSource source) {
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
    public void showImpactedTestCases(
        List<TestCase> testcases,
        String pageName,
        String objectName
    ) {
        oRPanel.getTestDesign().getImpactUI().loadForObject(testcases, pageName, objectName);
    }

    @Override
    public ORRootInf getOR() {
        ObjectRepository repo = oRPanel.getProject().getObjectRepository();
        return (source == ORSource.SHARED)
            ? repo.getStructuredDataSharedOR()
            : repo.getStructuredDataOR();
    }

    @Override
    protected void objectRemoved(ORObjectInf object) {
        if (getLoadedObject() != null && getLoadedObject().equals(object)) {
            oRPanel.getObjectTable().reset();
        }
        super.objectRemoved(object);
    }

    public StructuredDataORObject getLoadedObject() {
        return oRPanel.getObjectTable().getObject();
    }

    public enum ORSource {
        PROJECT,
        SHARED
    }

    public ORSource getSource() {
        return source;
    }

    public StructuredDataORPanel getORPanel() {
        return oRPanel;
    }

    @Override
    public TestDesign getTestDesign() {
        return oRPanel.getTestDesign();
    }
}
