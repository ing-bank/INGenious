
package com.ing.ide.main.mainui.components.testdesign.or.sap;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.sap.SapORObject;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.or.ObjectTree;
import java.util.List;
import javax.swing.tree.TreePath;

/**
 * Represents the tree UI component for displaying SAP Object Repository (OR) items.
 * <p>
 * This class links the object tree with the {@link SapORPanel}, enabling:
 * <ul>
 *   <li>loading object details into the properties table</li>
 *   <li>retrieving the appropriate OR source (Project or Shared)</li>
 *   <li>handling impacted test case display</li>
 *   <li>resetting the table when selected objects are removed</li>
 * </ul>
 * It acts as the controller between tree selections and OR object presentation.
 */
public class SapObjectTree extends ObjectTree {

    private final SapORPanel oRPanel;
    private final ORSource source;

    public SapObjectTree(SapORPanel panel, ORSource source) {
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
    public TestDesign getTestDesign() {
        return oRPanel.getTestDesign();
    }

    @Override
    public void showImpactedTestCases(List<TestCase> testcases, String pageName, String objectName) {
        oRPanel.getTestDesign().getImpactUI().loadForObject(testcases, pageName, objectName);
    }

    @Override
    public ORRootInf getOR() {
        ObjectRepository repo = oRPanel.getProject().getObjectRepository();
        return (source == ORSource.SHARED) ? repo.getSapSharedOR() : repo.getSapOR();
    }

    @Override
    protected void objectRemoved(ORObjectInf object) {
        SapORObject loaded = getLoadedObject();
        if (loaded != null && loaded.equals(object)) {
            oRPanel.getObjectTable().reset();
        }
        super.objectRemoved(object);
    }

    public SapORObject getLoadedObject() {
        return oRPanel.getObjectTable().getObject();
    }

    public enum ORSource {
        PROJECT, SHARED
    }

    public ORSource getSource() {
        return source;
    }

    public SapORPanel getORPanel() {
        return oRPanel;
    }
}
