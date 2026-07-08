package com.ing.ide.main.mainui.components.testdesign.or.web;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.or.ObjectTree;
import java.util.List;
import javax.swing.tree.TreePath;

/**
 * Swing tree component for browsing and editing Web Object Repository (OR) entries in the Test Design UI.
 * <p>
 * {@code WebObjectTree} extends {@link ObjectTree} and delegates most UI actions to the owning
 * {@link WebORPanel}. It loads the object details table based on the current tree selection,
 * routes "impacted test cases" requests to the Impact UI, and resolves the correct Web OR root
 * (project OR vs shared OR) based on {@link ORSource}.
 * </p>
 *
 * <h2>Key Behaviors</h2>
 * <ul>
 *   <li><b>Selection → Table:</b> On selection change, loads the object attributes into the OR table model.</li>
 *   <li><b>Repository Source:</b> Returns either the project Web OR or shared Web OR depending on {@link ORSource}.</li>
 *   <li><b>Frame Editing:</b> Updates the selected {@link WebORObject}'s frame metadata.</li>
 *   <li><b>Removal Handling:</b> If the removed object is currently loaded in the table, resets the table before removal completes.</li>
 * </ul>
 */
public class WebObjectTree extends ObjectTree {
    private final WebORPanel oRPanel;
    private final ORSource source;

    public WebObjectTree(WebORPanel panel, ORSource source) {
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

    void changeFrameData(String frameText) {
        WebORObject obj = getSelectedWebObject();
        if (obj != null) {
            obj.setFrame(frameText);
        }
    }

    /**
     * Resolves the {@link WebORObject} currently shown in the details table. When an
     * {@link ObjectGroup} node is selected, the table loads the group's first child, so the
     * frame edit must be applied to that same object rather than to the (non-object) group node.
     */
    private WebORObject getSelectedWebObject() {
        ORObjectInf selected = getSelectedObject();
        if (selected instanceof WebORObject) {
            return (WebORObject) selected;
        }
        ObjectGroup group = getSelectedObjectGroup();
        if (group != null && group.getChildAt(0) instanceof WebORObject) {
            return (WebORObject) group.getChildAt(0);
        }
        return null;
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
        return (source == ORSource.SHARED) ? repo.getWebSharedOR() : repo.getWebOR();
    }

    /**
     * Reloads this tree and keeps the given page expanded so newly added objects (e.g. during
     * Playwright live recording) remain visible without the user having to expand it manually.
     *
     * @param pageName the page to keep expanded after reload
     */
    public void reloadAndExpandPage(String pageName) {
        reload();
        if (pageName == null || pageName.isEmpty()) {
            return;
        }
        ORRootInf root = getOR();
        if (root instanceof WebOR) {
            WebORPage page = ((WebOR) root).getPageByName(pageName);
            if (page != null) {
                TreePath path = page.getTreePath();
                getTree().expandPath(path);
                getTree().scrollPathToVisible(path);
            }
        }
    }

    @Override
    protected void objectRemoved(ORObjectInf object) {
        ORObjectInf loaded = getAnyLoadedObject();
        if (loaded != null && loaded.equals(object)) {
            oRPanel.getObjectTable().reset();
        }
        super.objectRemoved(object);
    }

    public WebORObject getLoadedObject() {
        return oRPanel.getObjectTable().getObject();
    }

    private ORObjectInf getAnyLoadedObject() {
        return getLoadedObject();
    }

    public enum ORSource {
        PROJECT,
        SHARED
    }

    public WebORPanel getORPanel() {
        return oRPanel;
    }

    @Override
    public TestDesign getTestDesign() {
        return oRPanel.getTestDesign();
    }
}
