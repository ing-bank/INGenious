package com.ing.ide.main.mainui.components.testdesign.or.structureddata;

import com.ing.datalib.component.Project;
import com.ing.datalib.or.structureddata.StructuredDataORObject;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.utils.tree.TreeSearch;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 * Panel for Structured Data Object Repository.
 * Contains tree view and property table for API objects.
 */
public class StructuredDataORPanel extends JPanel {

    private final StructuredDataObjectTree objectTree;
    private final StructuredDataORTable objectTable;

    private final TestDesign testDesign;

    private JSplitPane splitPane;

    public StructuredDataORPanel(TestDesign testDesign) {
        this.testDesign = testDesign;
        this.objectTree = new StructuredDataObjectTree(this);
        this.objectTable = new StructuredDataORTable(this);
        init();
    }

    private void init() {
        setLayout(new BorderLayout());
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setOneTouchExpandable(true);
        splitPane.setBottomComponent(objectTable);
        TreeSearch tSearch = TreeSearch.installForOR(objectTree.getTree());
        splitPane.setTopComponent(tSearch);
        splitPane.setResizeWeight(.5);
        splitPane.setDividerLocation(.5);
        add(splitPane);
    }

    void loadTableModelForSelection(Object object) {
        if (object instanceof StructuredDataORObject) {
            objectTable.loadObject((StructuredDataORObject) object);
        } else if (object instanceof ObjectGroup) {
            objectTable.loadObject((StructuredDataORObject) ((ObjectGroup) object).getChildAt(0));
        } else {
            objectTable.reset();
        }
    }

    public StructuredDataObjectTree getObjectTree() {
        return objectTree;
    }

    public TestDesign getTestDesign() {
        return testDesign;
    }

    public Project getProject() {
        return testDesign.getProject();
    }

    public void load() {
        objectTable.reset();
        objectTree.load();
        splitPane.setDividerLocation(.5);
    }

    public void adjustUI() {
        int height = splitPane.getHeight();
        if (height > 0) {
            splitPane.setDividerLocation(height / 2);
        } else {
            splitPane.setDividerLocation(0.5);
        }
    }

    public Boolean navigateToObject(String objectName, String pageName) {
        return objectTree.navigateToObject(objectName, pageName);
    }

    public StructuredDataORTable getObjectTable() {
        return objectTable;
    }

}
