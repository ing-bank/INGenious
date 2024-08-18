
package com.ing.ide.main.mainui.components.testdesign.or.mobile;

import com.ing.datalib.component.Project;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.utils.tree.TreeSearch;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

/**
 *
 * 
 */
public class MobileORPanel extends JPanel {

    private final MobileObjectTree objectTree;
    private final MobileORTable objectTable;

    private final TestDesign testDesign;

    private JSplitPane splitPane;

    public MobileORPanel(TestDesign testDesign) {
        this.testDesign = testDesign;
        this.objectTree = new MobileObjectTree(this);
        this.objectTable = new MobileORTable(this);
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
        if (object instanceof MobileORObject) {
            objectTable.loadObject((MobileORObject) object);
        } else if (object instanceof ObjectGroup) {
            objectTable.loadObject((MobileORObject) ((ObjectGroup) object).getChildAt(0));
        } else {
            objectTable.reset();
        }
    }

    public MobileObjectTree getObjectTree() {
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
        splitPane.setDividerLocation(0.5);
    }

    public Boolean navigateToObject(String objectName, String pageName) {
        return objectTree.navigateToObject(objectName, pageName);
    }

    public MobileORTable getObjectTable() {
        return objectTable;
    }

}
