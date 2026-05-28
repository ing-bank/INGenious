
package com.ing.ide.main.mainui.components.testdesign.or;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.mobile.MobileOR;
import com.ing.datalib.or.mobile.MobileORObject;
import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.datalib.or.mobile.ResolvedMobileObject;
import com.ing.datalib.or.sap.ResolvedSapObject;
import com.ing.datalib.or.sap.SapOR;
import com.ing.datalib.or.sap.SapORObject;
import com.ing.datalib.or.sap.SapORPage;
import com.ing.datalib.or.structureddata.StructuredDataOR;
import com.ing.datalib.or.structureddata.StructuredDataORObject;
import com.ing.datalib.or.structureddata.StructuredDataORPage;
import com.ing.datalib.or.structureddata.ResolvedStructuredDataObject;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import com.ing.ide.main.help.Help;

import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.main.utils.tree.TreeSelectionRenderer;
import com.ing.ide.settings.IconSettings;
import com.ing.ide.util.Notification;
import com.ing.ide.util.Validator;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.mainui.components.testdesign.TestDesign;
import com.ing.ide.main.mainui.components.testdesign.or.clipboard.ORClipboardManager;
import com.ing.ide.main.mainui.components.testdesign.or.clipboard.ORObjectClipboard;
import com.ing.ide.main.mainui.components.testdesign.or.mobile.MobileORPanel;
import com.ing.ide.main.mainui.components.testdesign.or.mobile.MobileObjectTree;
import com.ing.ide.main.mainui.components.testdesign.or.sap.SapORPanel;
import com.ing.ide.main.mainui.components.testdesign.or.sap.SapObjectTree;
import com.ing.ide.main.mainui.components.testdesign.or.structureddata.StructuredDataORPanel;
import com.ing.ide.main.mainui.components.testdesign.or.structureddata.StructuredDataObjectTree;
import com.ing.ide.main.mainui.components.testdesign.or.web.WebORPanel;
import com.ing.ide.main.mainui.components.testdesign.or.web.WebObjectTree;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Base abstract class representing a fully interactive Object Repository (OR) tree.
 * <p>
 * {@code ObjectTree} provides a complete UI framework for browsing, editing, and
 * maintaining Object Repository structures including pages, object groups, and
 * objects. It manages a {@link JTree} with support for inline editing, drag‑and‑drop,
 * contextual popup menus, keyboard shortcuts, custom icons, and dynamic selection
 * handling.
 * </p>
 *
 * <p>
 * The class defines core behaviors such as:
 * <ul>
 *   <li>Loading repository structures into the tree.</li>
 *   <li>Renaming, adding, sorting, and deleting OR nodes.</li>
 *   <li>Project‑synchronized updates (save, refresh, rename validations).</li>
 *   <li>Shared vs. project‑scoped OR safeguards (shared rename checks, shared copy restrictions).</li>
 *   <li>Finding and selecting OR objects and scrolling them into view.</li>
 *   <li>Right‑click context menu actions through {@link ObjectPopupMenu}.</li>
 *   <li>Coordination with table panels via {@code loadTableModelForSelection()}.</li>
 * </ul>
 * </p>
 *
 * <p>
 * Subclasses must implement methods for loading table models, accessing the active
 * project instance, retrieving the OR root, and showing impacted test cases.
 * </p>
 */
public abstract class ObjectTree implements ActionListener {

    public final JTree tree;

    private final ObjectPopupMenu popupMenu;

    public ObjectTree() {
        tree = new JTree();
        popupMenu = new ObjectPopupMenu(this);
        init();
    }

    private void init() {
        tree.setToggleClickCount(0);
        tree.setEditable(true);
        tree.setComponentPopupMenu(popupMenu);
        tree.setDragEnabled(true);
        tree.setInvokesStopCellEditing(true);
        
        alterDefaultKeyBindings();
        installClipboardActions();
        
        tree.setTransferHandler(new ObjectDnD(tree));

        tree.getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.NEW, "New");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.DELETE, "Delete");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.RENAME, "Rename");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ESCAPE"), "Escape");

        tree.getActionMap().put("New", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (isRootSelected()) {
                    addPage();
                } else if (getSelectedPage() != null || getSelectedObjectGroup() != null || getSelectedObject() != null) {
                    addObject();
                }
            }
        });
        tree.getActionMap().put("Delete", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                deleteObjects();
                deleteObjectGroups();
                deletePages();
            }
        });
        tree.getActionMap().put("Rename", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                ORPageInf page = getSelectedPage();
                if (page != null) {
                    tree.startEditingAtPath(page.getTreePath());
                    return;
                }
                ObjectGroup group = getSelectedObjectGroup();
                if (group != null) {
                    tree.startEditingAtPath(group.getTreePath());
                    return;
                }
                ORObjectInf obj = getSelectedObject();
                if (obj != null) {
                    tree.startEditingAtPath(obj.getTreePath());
                }
            }
        });
        tree.getActionMap().put("Escape", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent ae) {
                if (tree.isEditing()) {
                    tree.cancelEditing();
                }
            }
        });
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent tse) {
                loadTableModelForSelection();
            }
        });

        popupMenu.addPopupMenuListener(new PopupMenuListener() {

            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent pme) {
                onRightClick();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent pme) {
                //
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent pme) {
                //
            }
        });
        setTreeIcon();
        tree.getCellEditor().addCellEditorListener(new CellEditorListener() {
            @Override
            public void editingStopped(ChangeEvent ce) {
                if (!checkAndRename()) {
                    tree.getCellEditor().cancelCellEditing();
                }
            }

            @Override
            public void editingCanceled(ChangeEvent ce) {
//                checkAndRename();
            }
        });
    }

    private void setTreeIcon() {
     try {
            //create the font to use. Specify the size!
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {
          //  e.printStackTrace();
        }
        tree.setFont(new Font("ING Me", Font.PLAIN, 11));
        new TreeSelectionRenderer(tree) {
            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean isLeaf, int row, boolean focused) {
                Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
                if (value instanceof ORPageInf) {
                    setIcons(IconSettings.getIconSettings().getORPage());
                } else if (value instanceof ObjectGroup) {
                    setIcons(IconSettings.getIconSettings().getIORGroup());
                } else if (value instanceof ORObjectInf) {
                    setIcons(IconSettings.getIconSettings().getORObject());
                } else {
                    setIcons(IconSettings.getIconSettings().getORRoot());
                }
                return c;
            }

            void setIcons(Icon icon) {
                setLeafIcon(icon);
                setClosedIcon(icon);
                setOpenIcon(icon);
                setIcon(icon);
            }
        };
    }

    public abstract void loadTableModelForSelection();

    private void onRightClick() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            popupMenu.togglePopupMenu(tree.getSelectionPath().getLastPathComponent());
        } else {
            popupMenu.setVisible(false);
        }
    }

    @Override
    public void actionPerformed(ActionEvent ae) {
        switch (ae.getActionCommand()) {
            case "Add Page":
                addPage();
                break;
            case "Rename Page":
                tree.startEditingAtPath(getSelectedPage().getTreePath());
                break;
            case "Delete Page":
                deletePages();
                break;
            case "Rename Object Group":
                tree.startEditingAtPath(getSelectedObjectGroup().getTreePath());
                break;
            case "Delete Object Group":
                deleteObjectGroups();
                break;
            case "Add Object":
                addObject();
                break;
            case "Rename Object":
                tree.startEditingAtPath(getSelectedObject().getTreePath());
                break;
            case "Delete Object":
                deleteObjects();
                break;
            case "Remove Unused Object":
                removeUnusedObject();
                break;    
            case "Get Impacted TestCases":
                getImpactedTestCases();
                break;
            case "Sort":
                sort();
                break;
            case "Open Page Dump":
                openPageDump();
                break;
            case "Move to Shared":
                moveToShared();
                break;
            case "Copy":
                copySelection();
                break;
            case "Cut":
                cutSelection();
                break;
            case "Paste":
                pasteSelection();
                break;
            default:
                throw new UnsupportedOperationException();
        }
    }

    private Boolean checkAndRename() {
        String name = tree.getCellEditor().getCellEditorValue().toString().trim();
        if (Validator.isValidName(name)) {
            ORPageInf page = getSelectedPage();
            if (page != null && !page.getName().equals(name)) {
                if (!confirmSharedRename("Page", page.getName(), name)) {
                    return false;
                }
                if (page.rename(name)) {
                    nodeRenamed(page);
                    getProject().save();
                    return true;
                } else {
                    Notification.show("Page " + name + " Already present");
                    return false;
                }
            }
            ObjectGroup<ORObjectInf> group = getSelectedObjectGroup();
            if (group != null && !group.getName().equals(name)) {
                if (!confirmSharedRename("Object Group", group.getName(), name)) {
                    return false;
                }
                if (group.rename(name)) {
                    nodeRenamed(group);
                    getProject().save();
                    return true;
                } else {
                    Notification.show("Object " + name + " Already present");
                    return false;
                }
            }

            ORObjectInf obj = getSelectedObject();
            if (obj != null && !obj.getName().equals(name)) {
                if (!confirmSharedRename("Object", obj.getName(), name)) {
                    return false;
                }
                if (obj.rename(name)) {
                    nodeRenamed(obj);
                    getProject().save();
                    return true;
                } else {
                    Notification.show("Object " + name + " Already present");
                    return false;
                }
            }
        }
        return false;
    }

    public ORObjectInf getSelectedObject() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            if (path.getLastPathComponent() instanceof ORObjectInf) {
                return (ORObjectInf) path.getLastPathComponent();
            }
        }
        return null;
    }

    public ObjectGroup getSelectedObjectGroup() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            if (path.getLastPathComponent() instanceof ObjectGroup) {
                return (ObjectGroup) path.getLastPathComponent();
            }
        }
        return null;
    }

    public ORPageInf getSelectedPage() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            if (path.getLastPathComponent() instanceof ORPageInf) {
                return (ORPageInf) path.getLastPathComponent();
            }
        }
        return null;
    }

    public Boolean isRootSelected() {
        TreePath path = tree.getSelectionPath();
        if (path != null) {
            return path.getPathCount() == 1;
        }
        return true;
    }

    public List<ObjectGroup> getSelectedObjectGroups() {
        List<ObjectGroup> groups = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null && paths.length > 0) {
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof ObjectGroup) {
                    groups.add((ObjectGroup) path.getLastPathComponent());
                }
            }
        }
        return groups;
    }

    public List<ORObjectInf> getSelectedObjects() {
        List<ORObjectInf> objects = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null && paths.length > 0) {
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof ORObjectInf) {
                    objects.add((ORObjectInf) path.getLastPathComponent());
                }
            }
        }
        return objects;
    }

    public List<ORPageInf> getSelectedPages() {
        List<ORPageInf> pages = new ArrayList<>();
        TreePath[] paths = tree.getSelectionPaths();
        if (paths != null && paths.length > 0) {
            for (TreePath path : paths) {
                if (path.getLastPathComponent() instanceof ORPageInf) {
                    pages.add((ORPageInf) path.getLastPathComponent());
                }
            }
        }
        return pages;
    }

    private void addObject() {
        if (getSelectedPage() != null) {
            objectAddedPage(getSelectedPage().addObject());
        } else if (getSelectedObjectGroup() != null) {
            objectAddedGroup(getSelectedObjectGroup().addObject());
        } else if (getSelectedObject() != null) {
            objectAdded(getSelectedObject().getParent().addObject());
        }
    }

    private void addPage() {
        pageAdded(getOR().addPage());
    }

    private void deleteObjects() {
        List<ORObjectInf> objects = getSelectedObjects();
        if (!objects.isEmpty()) {
            String extra = isSharedScope() ? sharedProjectsInfo() : "";
            int option = JOptionPane.showConfirmDialog(
                null,
                "<html><body><p style='width: 300px;'>"
                + "Are you sure you want to delete the following Objects?<br/>"
                + objects
                + extra
                + "</p></body></html>",
                isSharedScope() ? "Delete SHARED Object" : "Delete Object",
                JOptionPane.YES_NO_OPTION
            );
            if (option == JOptionPane.YES_OPTION) {
                ObjectRepository repo = getProject().getObjectRepository();
                Set<ORPageInf> affectedPages = new HashSet<>();
                
                for (ORObjectInf object : objects) {
                    ORPageInf page = object.getPage();
                    affectedPages.add(page);
                    objectRemoved(object);
                    object.removeFromParent();
                }
                
                // Save affected pages in YAML format
                for (ORPageInf page : affectedPages) {
                    if (page instanceof WebORPage) {
                        repo.saveWebPageNow((WebORPage) page);
                    } else if (page instanceof MobileORPage) {
                        repo.saveMobilePageNow((MobileORPage) page);
                    } else if (page instanceof SapORPage) {
                        repo.saveSapPageNow((SapORPage) page);
                    }
                }
                
                repo.save();
            }
        }
    }

    private void removeUnusedObject() {
        boolean webDeletionPerformed = false;
        boolean mobileDeletionPerformed = false;
        boolean structuredDataDeletionPerformed = false;
        boolean sapDeletionPerformed = false;
        try {
            List<ORPageInf> selectedPages = getSelectedPages();
            if (selectedPages == null || selectedPages.isEmpty()) {
                return;
            }
            ObjectRepository repo = getProject().getObjectRepository();
            WebOR projectWebOR = repo.getWebOR();
            MobileOR projectMobileOR = repo.getMobileOR();
            StructuredDataOR projectStructuredDataOR = repo.getStructuredDataOR();
            SapOR projectSapOR = repo.getSapOR();
            Set<String> usedProjectObjects = new HashSet<>();
            for (Scenario scenario : getProject().getAllScenarios()) {
                for (TestCase testCase : scenario.getTestCases()) {
                    testCase.loadTableModel();
                    for (TestStep step : testCase.getTestSteps()) {
                        if (!step.isPageObjectStep()) {
                            continue;
                        }
                        String ref = step.getReference();
                        if (ref == null || ref.isBlank()) {
                            continue;
                        }
                        String pageName = normalizePageRef(ref);
                        String objectName = step.getObject();
                        if (!pageName.isBlank() && !objectName.isBlank()) {
                            usedProjectObjects.add(pageName + "@" + objectName);
                        }
                    }
                }
            }
            for (ORPageInf selectedPage : selectedPages) {
                String pageName = selectedPage.getName();
                WebORPage webPage = projectWebOR.getPageByName(pageName);
                if (webPage == null) {
                    continue;
                }
                List<String> unusedWebObjects = new ArrayList<>();
                for (ObjectGroup group : webPage.getObjectGroups()) {
                    if (!usedProjectObjects.contains(pageName + "@" + group.getName())) {
                        unusedWebObjects.add(group.getName());
                    }
                }
                if (!unusedWebObjects.isEmpty()) {
                    int option = JOptionPane.showConfirmDialog(
                        null,
                        "<html><body><p style='width: 260px;'>"
                        + "Delete the following Web objects from page [ "
                        + pageName + " ]?<br>"
                        + unusedWebObjects
                        + "</p></body></html>",
                        "Delete Web Objects",
                        JOptionPane.YES_NO_OPTION
                    );
                    if (option == JOptionPane.YES_OPTION) {
                        Iterator<ObjectGroup<WebORObject>> it = webPage.getObjectGroups().iterator();
                        while (it.hasNext()) {
                            ObjectGroup group = it.next();
                            if (unusedWebObjects.contains(group.getName())) {
                                it.remove();
                                projectWebOR.setSaved(false);
                                webDeletionPerformed = true;
                            }
                            if (webDeletionPerformed) {
                                repo.saveWebPageNow(webPage);
                            }
                        }
                    }
                }
            }
            if (projectMobileOR != null) {
                for (ORPageInf selectedPage : selectedPages) {
                    String pageName = selectedPage.getName();
                    MobileORPage mobilePage = projectMobileOR.getPageByName(pageName);
                    if (mobilePage == null) {
                        continue;
                    }
                    List<String> unusedMobileObjects = new ArrayList<>();
                    for (ObjectGroup<MobileORObject> group : mobilePage.getObjectGroups()) {
                        if (!usedProjectObjects.contains(
                                pageName + "@" + group.getName())) {
                            unusedMobileObjects.add(group.getName());
                        }
                    }
                    if (!unusedMobileObjects.isEmpty()) {
                        int option = JOptionPane.showConfirmDialog(
                            null,
                            "<html><body><p style='width: 260px;'>"
                            + "Delete the following Mobile objects from page [ "
                            + pageName + " ]?<br>"
                            + unusedMobileObjects
                            + "</p></body></html>",
                            "Delete Mobile Objects",
                            JOptionPane.YES_NO_OPTION
                        );
                        if (option == JOptionPane.YES_OPTION) {
                            Iterator<ObjectGroup<MobileORObject>> it =
                                mobilePage.getObjectGroups().iterator();
                            while (it.hasNext()) {
                                ObjectGroup<MobileORObject> group = it.next();
                                if (unusedMobileObjects.contains(group.getName())) {
                                    it.remove();
                                    projectMobileOR.setSaved(false);
                                    mobileDeletionPerformed = true;
                                }
                            }
                            if (mobileDeletionPerformed) {
                                repo.saveMobilePageNow(mobilePage);
                            }
                        }
                    }
                }
            }
            if (projectStructuredDataOR != null) {
                for (ORPageInf selectedPage : selectedPages) {
                    String pageName = selectedPage.getName();
                    StructuredDataORPage structuredDataPage = projectStructuredDataOR.getPageByName(pageName);
                    if (structuredDataPage == null) {
                        continue;
                    }
                    List<String> unusedStructuredDataObjects = new ArrayList<>();
                    for (ObjectGroup<StructuredDataORObject> group : structuredDataPage.getObjectGroups()) {
                        if (!usedProjectObjects.contains(
                                pageName + "@" + group.getName())) {
                            unusedStructuredDataObjects.add(group.getName());
                        }
                    }
                    if (!unusedStructuredDataObjects.isEmpty()) {
                        int option = JOptionPane.showConfirmDialog(
                            null,
                            "<html><body><p style='width: 260px;'>"
                            + "Delete the following Structured Data objects from page [ "
                            + pageName + " ]?<br>"
                            + unusedStructuredDataObjects
                            + "</p></body></html>",
                            "Delete Structured Data Objects",
                            JOptionPane.YES_NO_OPTION
                        );
                        if (option == JOptionPane.YES_OPTION) {
                            Iterator<ObjectGroup<StructuredDataORObject>> it =
                                structuredDataPage.getObjectGroups().iterator();
                            while (it.hasNext()) {
                                ObjectGroup<StructuredDataORObject> group = it.next();
                                if (unusedStructuredDataObjects.contains(group.getName())) {
                                    it.remove();
                                    projectStructuredDataOR.setSaved(false);
                                    structuredDataDeletionPerformed = true;
                                }
                            }
                            if (structuredDataDeletionPerformed) {
                                repo.saveStructuredDataPageNow(structuredDataPage);
                            }
                        }
                    }
                }
            }
            if (projectSapOR != null) {
                for (ORPageInf selectedPage : selectedPages) {
                    String pageName = selectedPage.getName();
                    SapORPage sapPage = projectSapOR.getPageByName(pageName);
                    if (sapPage == null) {
                        continue;
                    }
                    List<String> unusedSapObjects = new ArrayList<>();
                    for (ObjectGroup group : sapPage.getObjectGroups()) {
                        if (!usedProjectObjects.contains(pageName + "@" + group.getName())) {
                            unusedSapObjects.add(group.getName());
                        }
                    }
                    if (!unusedSapObjects.isEmpty()) {
                        int option = JOptionPane.showConfirmDialog(
                            null,
                            "<html><body><p style='width: 260px;'>"
                            + "Delete the following SAP objects from page [ "
                            + pageName + " ]?<br>"
                            + unusedSapObjects
                            + "</p></body></html>",
                            "Delete SAP Objects",
                            JOptionPane.YES_NO_OPTION
                        );
                        if (option == JOptionPane.YES_OPTION) {
                            Iterator<ObjectGroup<SapORObject>> it = sapPage.getObjectGroups().iterator();
                            while (it.hasNext()) {
                                ObjectGroup group = it.next();
                                if (unusedSapObjects.contains(group.getName())) {
                                    it.remove();
                                    projectSapOR.setSaved(false);
                                    sapDeletionPerformed = true;
                                }
                            }
                            if (sapDeletionPerformed) {
                                repo.saveSapPageNow(sapPage);
                            }
                        }
                    }
                }
            }
            if (webDeletionPerformed || mobileDeletionPerformed || structuredDataDeletionPerformed || sapDeletionPerformed) {
                repo.save();
                int option = JOptionPane.showConfirmDialog(
                    null,
                    "<html><body><p style='width: 260px;'>"
                    + "Do you want to restart INGenious to load the updated Object Repository?"
                    + "</p></body></html>",
                    "Restart INGenious",
                    JOptionPane.YES_NO_OPTION
                );
                if (option == JOptionPane.YES_OPTION) {
                    new AppMainFrame().restart();
                }
            } else {
                JOptionPane.showMessageDialog(
                    null,
                    "<html><body><p style='width: 240px;'>"
                    + "No unused object found or removal was cancelled."
                    + "</p></body></html>",
                    "Unused Object",
                    JOptionPane.INFORMATION_MESSAGE
                );
            }
            ((DefaultTreeModel) tree.getModel()).reload();
            tree.updateUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String normalizePageRef(String ref) {
        if (ref == null) return "";
        ref = ref.trim();
        if (ref.startsWith("[Project] ")) return ref.substring(10).trim();
        if (ref.startsWith("[Shared] "))  return ref.substring(9).trim();
        return ref;
    }
    
    public Map usedObject() {   
        Map<String, ArrayList<String>> attributeMap = new HashMap<>();
        ArrayList<String> records = new ArrayList<>();
        try {
            String testPlanPath = getProject().getLocation() + "/TestPlan";
            String[] scenarioList = getFolderOrFileList(testPlanPath);
            for (String scenario : scenarioList) {
                Path path = Paths.get(testPlanPath + "/" + scenario);
                if(Files.isDirectory(path))
                {
                String[] csvList = getFolderOrFileList(testPlanPath + "/" + scenario);
                for (String csv : csvList) {
                    String csvFilePath = testPlanPath + "/" + scenario + "/" + csv;
                    String[] values = null;
                    try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            values = line.split(",");
                            if (!values[1].equals("Browser") && !values[1].equals("ObjectName") && (values.length == 7)) {//&& (values[1])!= "Browser" )
                                records.add(values[1]);
                                if (!(attributeMap.containsKey(values[6]))) {
                                    attributeMap.put(values[6], new ArrayList<>());
                                    attributeMap.get(values[6]).add(values[1]);
                                } else {
                                    attributeMap.get(values[6]).add(values[1]);
                                }
                            }
                        }
                        br.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return attributeMap;
    }

    public static String[] getFolderOrFileList(String path) {
        File directory = new File(path);
        String list[] = directory.list();
        return list;
    }

    public Map UnusedObject(Map<String, List> allSelectedObject, Map<String, List> usedObject) {
        Map<String, List> unUsedObject = new HashMap<>();
        Set selectedPages = allSelectedObject.keySet();
        Set usedPages = usedObject.keySet();

        for (Object selectedPage : selectedPages) {
            int k = 0;
            for (Object usedPage : usedPages) {
                if ((selectedPage).equals(usedPage)) {
                    k++;
                }
            }
            if (!(k == 0)) {
                for (int l = 0; l < allSelectedObject.get(selectedPage).size(); l++) {
                    int n = 0;
                    String selectedObject = (String) allSelectedObject.get(selectedPage).get(l);

                    for (int m = 0; m < usedObject.get(selectedPage).size(); m++) {
                        String verifyObject = (String) usedObject.get(selectedPage).get(m);

                        if (selectedObject.equals(verifyObject)) {
                            n++;
                        }
                    }
                    if (n == 0) {
                        if (!unUsedObject.containsKey(selectedPage.toString())) {
                            unUsedObject.put(selectedPage.toString(), new ArrayList<String>());
                            unUsedObject.get(selectedPage.toString()).add(selectedObject);
                        } else {
                            unUsedObject.get(selectedPage.toString()).add(selectedObject);
                        }
                    }
                }
            }
            else{
                for(int s=0;s<allSelectedObject.get(selectedPage).size();s++)
                        {
                     if (!unUsedObject.containsKey(selectedPage.toString())) {
                            unUsedObject.put(selectedPage.toString(), new ArrayList<String>());
                            unUsedObject.get(selectedPage.toString()).add((String) allSelectedObject.get(selectedPage).get(s));
                        } else {
                             unUsedObject.get(selectedPage.toString()).add((String) allSelectedObject.get(selectedPage).get(s));
                        }
                        }
            }
        }
        return unUsedObject;

    }

    public void deleteUnusedObject(String page, String object) {
        try {
            String orFilePath = getProject().getLocation() + "/OR.object";
            orFilePath = orFilePath.replace("\\", "/");
            String objectXpath = "//Root//Page[@ref='" + page + "']//ObjectGroup[@ref='" + object + "']";
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            Document document = dbf.newDocumentBuilder().parse(new File(orFilePath));
            XPathFactory xpf = XPathFactory.newInstance();
            javax.xml.xpath.XPath xpath = xpf.newXPath();
            XPathExpression expression = xpath.compile(objectXpath);
            Node b13Node = (Node) expression.evaluate(document, XPathConstants.NODE);
            b13Node.getParentNode().removeChild(b13Node);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer t = tf.newTransformer();
            StreamResult result = new StreamResult(new File(orFilePath));
            t.transform(new DOMSource(document), result);
//            result.getOutputStream().close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteObjectGroups() {
        List<ObjectGroup> objects = getSelectedObjectGroups();
        if (!objects.isEmpty()) {
            String extra = isSharedScope() ? sharedProjectsInfo() : "";
            int option = JOptionPane.showConfirmDialog(
                null,
                "<html><body><p style='width: 300px;'>"
                + "Are you sure you want to delete the following ObjectGroups?<br/>"
                + objects
                + extra
                + "</p></body></html>",
                isSharedScope() ? "Delete SHARED ObjectGroup" : "Delete ObjectGroup",
                JOptionPane.YES_NO_OPTION
            );
            if (option == JOptionPane.YES_OPTION) {
                for (ObjectGroup object : objects) {
                    objectGroupRemoved(object);
                    object.removeFromParent();
                }
                // Save immediately to update YAML files
                ObjectRepository repo = getProject().getObjectRepository();
                repo.save();
            }
        }
    }

    private void deletePages() {
        List<ORPageInf> pages = getSelectedPages();
        if (!pages.isEmpty()) {
            String extra = isSharedScope() ? sharedProjectsInfo() : "";
            int option = JOptionPane.showConfirmDialog(
                null,
                "<html><body><p style='width: 300px;'>"
                + "Are you sure you want to delete the following Pages?<br/>"
                + pages
                + extra
                + "</p></body></html>",
                isSharedScope() ? "Delete SHARED Page" : "Delete Page",
                JOptionPane.YES_NO_OPTION
            );
            if (option == JOptionPane.YES_OPTION) {
                ObjectRepository repo = getProject().getObjectRepository();
                
                for (ORPageInf page : pages) {
                    pageRemoved(page);
                    page.removeFromParent();
                }
                
                repo.save();
            }
        }
    }
    
    private void getImpactedTestCases() {
        ObjectGroup group = getSelectedObjectGroup();
        ORObjectInf selectedObject = null;
        
        if (group == null) {
            selectedObject = getSelectedObject();
            if (selectedObject != null) {
                group = selectedObject.getParent();
            } else {
                Notification.show("Not supported for the selected");
                return;
            }
        }
        
        String pageName = group.getParent().getName();
        String objectName;
        
        // If an individual object is selected, use its name, otherwise use group name
        if (selectedObject != null) {
            objectName = selectedObject.getName();
        } else {
            objectName = group.getName();
        }
        
        WebOR.ORScope scope = isSharedScope()
                ? WebOR.ORScope.SHARED
                : WebOR.ORScope.PROJECT;

        List<TestCase> impacted = getProject()
                .getImpactedObjectTestCases(scope, pageName, objectName);

        showImpactedTestCases(impacted, pageName, objectName);
    }

    public abstract void showImpactedTestCases(
            List<TestCase> testcases, String pageName, String objectName);

    private void sort() {
        if (getSelectedPage() != null) {
            getSelectedPage().sort();
            getModel().reload(getSelectedPage());
        } else if (getSelectedObjectGroup() != null) {
            getSelectedObjectGroup().sort();
            getModel().reload(getSelectedObjectGroup());
        } else if (getSelectedObject() != null) {

        } else {
            getOR().sort();
            getModel().reload();
        }
    }

    public JTree getTree() {
        return tree;
    }

    private DefaultTreeModel getModel() {
        return (DefaultTreeModel) tree.getModel();
    }

    public abstract Project getProject();

    public abstract TestDesign getTestDesign();

    public void load() {
        tree.setModel(new DefaultTreeModel(getOR()) {
            @Override
            public void valueForPathChanged(TreePath tp, Object o) {
            }
        });
    }

    public void reload() {
        ((DefaultTreeModel) tree.getModel()).reload();
    }

    public abstract ORRootInf getOR();

    public void openPageDump() {
        String location = getProject().getLocation() + File.separator + "PageDump" + File.separator + "page.html";
        File file = new File(location);
        if (file.exists()) {
            Help.openInBrowser("Couldn't Open", file.toURI());
        } else {
            Notification.show("PageDump not created/available in the Project");
        }
    }

    private void moveToShared() {
        if (isSharedScope()) {
            Notification.show("Objects already in Shared Repository");
            return;
        }
        if (getSelectedObject() != null) {
            moveObjectToShared(getSelectedObject());
            return;
        }
        if (getSelectedPage() != null) {
            movePageToShared(getSelectedPage());
        }
    }

    private void moveObjectToShared(ORObjectInf obj) {
        ObjectRepository repo = getProject().getObjectRepository();
        ORPageInf page = obj.getPage();
        ORRootInf root = getOR();
        boolean isWeb = root instanceof WebOR;
        boolean isMobile = root instanceof MobileOR;
        boolean isStructuredData = root instanceof StructuredDataOR;
        boolean isSap = root instanceof SapOR;
        String objectName = obj.getName();
        String pageName = page.getName();
        
        if (isWeb) {
            ResolvedWebObject resolved =
                repo.resolveWebObject(
                    new ResolvedWebObject.PageRef(
                        page.getName(),
                        WebOR.ORScope.PROJECT
                    ),
                    objectName
                );
            if (resolved == null) {
                Notification.show("Object not found in Project OR");
                return;
            }
            
            String newName = repo.moveWebObject(resolved, pageName);
            if (newName == null) {
                Notification.show("Object with the same name already exists in Shared OR");
                return;
            }
            if (newName != null) {
                // Check if source page is now empty and remove it
                WebOR projectOR = repo.getWebOR();
                WebORPage sourcePage = projectOR.getPageByName(pageName);
                if (sourcePage != null && sourcePage.getObjectGroups().isEmpty()) {
                    sourcePage.removeFromParent();
                }
                repo.save();
                reload();
                refreshSharedTree();
                Notification.show("Moved Object '" + newName + "' to Shared OR");
                promptTestCaseReload();
            }
        }
        if (isMobile) {
            ResolvedMobileObject resolved =
                repo.resolveMobileObject(
                    new ResolvedMobileObject.PageRef(
                        page.getName(),
                        WebOR.ORScope.PROJECT
                    ),
                    objectName
                );
            if (resolved == null || !resolved.isPresent()) {
                Notification.show("Mobile Object not found in Project OR");
                return;
            }
            String newName = repo.moveMobileObject(resolved, pageName);
            if (newName == null) {
                Notification.show("Object with the same name already exists in Shared OR");
                return;
            }
            if (newName != null) {
                // Check if source page is now empty and remove it
                MobileOR projectOR = repo.getMobileOR();
                MobileORPage sourcePage = projectOR.getPageByName(pageName);
                if (sourcePage != null && sourcePage.getObjectGroups().isEmpty()) {
                    sourcePage.removeFromParent();
                }
                repo.save();
                reload();
                refreshSharedTree();
                Notification.show("Moved Mobile Object '" + newName + "' to Shared OR");
                promptTestCaseReload();
            }
        }
        if (isStructuredData) {
            ResolvedStructuredDataObject resolved =
                repo.resolveStructuredDataObject(
                    new ResolvedStructuredDataObject.PageRef(
                        pageName,
                        WebOR.ORScope.PROJECT
                    ),
                    objectName
                );
            if (resolved == null || !resolved.isPresent()) {
                Notification.show("Structured Data Object not found in Project OR");
                return;
            }
            String newName = repo.moveStructuredDataObject(resolved, pageName);
            if (newName == null) {
                Notification.show("Object with the same name already exists in Shared OR");
                return;
            }
            if (newName != null) {
                // Check if source page is now empty and remove it
                StructuredDataOR projectOR = repo.getStructuredDataOR();
                StructuredDataORPage sourcePage = projectOR.getPageByName(pageName);
                if (sourcePage != null && sourcePage.getObjectGroups().isEmpty()) {
                    sourcePage.removeFromParent();
                }
                
                repo.save();
                reload();
                refreshSharedTree();
                Notification.show("Moved Structured Data Object '" + newName + "' to Shared OR");
                promptTestCaseReload();
            }
        }
        if (isSap) {
            ResolvedSapObject resolved =
                repo.resolveSapObject(
                    new ResolvedSapObject.PageRef(
                        page.getName(),
                        SapOR.ORScope.PROJECT
                    ),
                    objectName
                );
            if (resolved == null || !resolved.isPresent()) {
                Notification.show("SAP Object not found in Project OR");
                return;
            }
            String newName = repo.moveSapObject(resolved, pageName);
            if (newName == null) {
                Notification.show("Object with the same name already exists in Shared OR");
                return;
            }
            if (newName != null) {
                // Check if source page is now empty and remove it
                SapOR projectOR = repo.getSapOR();
                SapORPage sourcePage = projectOR.getPageByName(pageName);
                if (sourcePage != null && sourcePage.getObjectGroups().isEmpty()) {
                    sourcePage.removeFromParent();
                }
                repo.save();
                reload();
                refreshSharedTree();
                Notification.show("Moved SAP Object '" + newName + "' to Shared OR");
                promptTestCaseReload();
            }
        }
    }

    private void promptTestCaseReload() {
        try {
            getTestDesign().getTestCaseComp().reload();
            Notification.show("Test cases reloaded successfully");
        } catch (Exception e) {
            // Silently ignore if no test case is currently open
        }
    }

    private void movePageToShared(ORPageInf page) {
        ObjectRepository repo = getProject().getObjectRepository();
        ORRootInf root = getOR();
        boolean isWeb = root instanceof WebOR;
        boolean isMobile = root instanceof MobileOR;
        boolean isStructuredData = root instanceof StructuredDataOR;
        boolean isSap = root instanceof SapOR;
        String pageName = page.getName();
        
        if (isWeb) {
            String newPageName = repo.moveWebPage(page.getName(), page.getName());
            if (newPageName != null) {
                // Move method already removes the page from parent, just reload the tree
                reload();
                repo.save();
                refreshSharedTree();
                Notification.show( "Moved Page '" + page.getName() + "' to Shared OR");
                promptTestCaseReload();
            } else {
                Notification.show("Page '" + pageName + "' and all its objects already exist in Shared OR");
            }
        }
        if (isMobile) {
            String newPageName = repo.moveMobilePage(page.getName(), page.getName());
            if (newPageName != null) {
                // Move method already removes the page from parent, just reload the tree
                reload();
                repo.save();
                refreshSharedTree();
                Notification.show("Moved Mobile Page '" + page.getName() + "' to Shared OR");
                promptTestCaseReload();
            } else {
                Notification.show("Mobile Page '" + pageName + "' and all its objects already exist in Shared OR");
            }
        }
        if (isSap) {
            String newPageName = repo.moveSapPage(page.getName(), page.getName());
            if (newPageName != null) {
                // Move method already removes the page from parent, just reload the tree
                reload();
                repo.save();
                refreshSharedTree();
                Notification.show("Moved SAP Page '" + page.getName() + "' to Shared OR");
                promptTestCaseReload();
            } else {
                Notification.show("SAP Page '" + pageName + "' and all its objects already exist in Shared OR");
            }
        }
        if (isStructuredData) {
            String movedPageName = repo.moveStructuredDataPage(pageName, pageName);
            if (movedPageName != null) {
                repo.save();
                reload();
                refreshSharedTree();
                Notification.show("Moved Structured Data Page '" + pageName + "' to Shared OR");
                promptTestCaseReload();
            } else {
                Notification.show("Structured Data Page '" + pageName + "' and all its objects already exist in Shared OR");
            }
        }
    }

    private void refreshSharedTree() {
        if (this instanceof WebObjectTree) {
            WebORPanel panel = ((WebObjectTree) this).getORPanel();
            panel.getSharedTree().load();
        } else if (this instanceof MobileObjectTree) {
            MobileORPanel panel = ((MobileObjectTree) this).getORPanel();
            panel.getSharedTree().load();
        } else if (this instanceof StructuredDataObjectTree) {
            StructuredDataORPanel panel = ((StructuredDataObjectTree) this).getORPanel();
            panel.getSharedTree().load();
        } else if (this instanceof SapObjectTree) {
            SapORPanel panel = ((SapObjectTree) this).getORPanel();
            panel.getSharedTree().load();
        }
    }

    public Boolean navigateToObject(String objectName, String pageName) {
        // Parse the pageName to extract actual page name (handles scoped references like "[Project] PageName")
        String actualPageName = extractPageName(pageName);
        ORPageInf page = getOR().getPageByName(actualPageName);
        if (page != null) {
            ObjectGroup group = page.getObjectGroupByName(objectName);
            if (group != null) {
                selectAndSrollTo(group.getTreePath());
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts the actual page name from a scoped reference.
     * Handles references like "[Project] PageName" or "[Shared] PageName"
     * by removing the scope prefix and returning just the page name.
     *
     * @param pageReference the page reference token (may contain scope prefix)
     * @return the actual page name without scope prefix
     */
    private String extractPageName(String pageReference) {
        if (pageReference == null || pageReference.trim().isEmpty()) {
            return pageReference;
        }

        String trimmed = pageReference.trim();

        // Check if reference starts with "[" and contains "]"
        if (trimmed.startsWith("[") && trimmed.contains("]")) {
            int endBracket = trimmed.indexOf(']');
            // Return the part after "]", trimming any leading whitespace
            return trimmed.substring(endBracket + 1).trim();
        }
        // No scope prefix, return as-is
        return trimmed;
    }

    private void objectAddedPage(final ORObjectInf object) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ((DefaultTreeModel) tree.getModel()).nodesWereInserted(object.getPage(), new int[]{object.getPage().getChildCount() - 1});
                selectAndSrollTo(object.getTreePath());
            }
        });
    }

    private void objectAddedGroup(final ORObjectInf object) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ((DefaultTreeModel) tree.getModel()).nodesWereInserted(object.getParent(), new int[]{object.getParent().getChildCount() - 1});
                selectAndSrollTo(object.getTreePath());
            }
        });
    }

    private void objectAdded(final ORObjectInf object) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ((DefaultTreeModel) tree.getModel()).nodeStructureChanged(object.getParent());
                selectAndSrollTo(object.getTreePath());
            }
        });
    }

    private void nodeRenamed(final TreeNode node) {
        SwingUtilities.invokeLater(() -> {
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            model.nodeChanged(node);
            TreeNode parent = node.getParent();
            if (parent != null) {
                model.nodeStructureChanged(parent);
            }
            tree.repaint();
        });
    }

    protected void objectRemoved(final ORObjectInf object) {
        if (object.getParent().getChildCount() == 1) {
            ((DefaultTreeModel) tree.getModel())
                    .nodesWereRemoved(object.getPage(),
                            new int[]{object.getPage().getIndex(object.getParent())},
                            new Object[]{object});
        } else {
            ((DefaultTreeModel) tree.getModel())
                    .nodesWereRemoved(object.getParent(),
                            new int[]{object.getParent().getIndex(object)},
                            new Object[]{object});
        }
    }

    protected void objectGroupRemoved(final ObjectGroup objectGroup) {
        ((DefaultTreeModel) tree.getModel()).nodesWereRemoved(objectGroup.getParent(),
                new int[]{objectGroup.getParent().getIndex(objectGroup)},
                new Object[]{objectGroup});
    }

    private void pageAdded(final ORPageInf page) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ((DefaultTreeModel) tree.getModel())
                        .nodesWereInserted(page.getParent(), new int[]{page.getParent().getChildCount() - 1});
                selectAndSrollTo(page.getTreePath());
            }
        });
    }

    protected void pageRemoved(final ORPageInf page) {
        ((DefaultTreeModel) tree.getModel()).nodesWereRemoved(page.getParent(),
                new int[]{page.getParent().getIndex(page)},
                new Object[]{page});
    }

    private void selectAndSrollTo(final TreePath path) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                tree.setSelectionPath(path);
                tree.scrollPathToVisible(path);
                loadTableModelForSelection();
                tree.removeSelectionPath(path);
                tree.addSelectionPaths(new TreePath[]{path.getParentPath(), path});
            }
        });
    }
    
    private void alterDefaultKeyBindings() {
         int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
         tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "none");
         tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "none");
         tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "none");

         tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "cut");
         tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "copy");
         tree.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "paste");   
    }

    private void installClipboardActions() {
        tree.getActionMap().put("cut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cutSelection();
            }
        });
        tree.getActionMap().put("copy", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelection();
            }
        });
        tree.getActionMap().put("paste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pasteSelection();
            }
        });
    }

    private boolean isSharedScope() {
        ORRootInf root = getOR();
        if (root instanceof WebOR) {
            return ((WebOR) root).isShared();
        }
        if (root instanceof MobileOR) {
            return ((MobileOR) root).isShared();
        }
        if (root instanceof StructuredDataOR) {
            return ((StructuredDataOR) root).isShared();
        }
        if (root instanceof SapOR) {
            return ((SapOR) root).isShared();
        }
        return false;
    }

    private boolean confirmSharedRename(String entityLabel, String currentName, String newName) {
        if (!isSharedScope()) return true;
        ORRootInf root = getOR();
        java.util.List<String> projects = null;

        String typeLabel = null;
        if (root instanceof WebOR) {
            projects = ((WebOR) root).getSharedProjects();
            typeLabel = "Web";
        } else if (root instanceof MobileOR) {
            projects = ((MobileOR) root).getSharedProjects();
            typeLabel = "Mobile";
        } else if (root instanceof StructuredDataOR) {
            projects = ((StructuredDataOR) root).getSharedProjects();
            typeLabel = "StructuredData";
        } else if (root instanceof SapOR) {
            projects = ((SapOR) root).getSharedProjects();
            typeLabel = "SAP";
        }

        if (projects == null || projects.isEmpty()) {
            return true;
        }

        String extra = sharedProjectsInfo();
        if (extra == null) extra = "";

        String message =
            "<html><body><p style='width: 360px;'>"
            + "You are about to rename the SHARED " + entityLabel + " "
            + "<b>" + currentName + "</b> to <b>" + newName + "</b>.<br/><br/>"
            + "Other projects that use Shared " + typeLabel
            + " Objects still reference the old name in their test steps."
            + extra
            + "</body></html>";

        int option = javax.swing.JOptionPane.showConfirmDialog(
            null,
            message,
            "Confirm Shared Rename",
            javax.swing.JOptionPane.YES_NO_OPTION
        );
        return option == javax.swing.JOptionPane.YES_OPTION;
    }

    private String sharedProjectsInfo() {
        ORRootInf root = getOR();
        java.util.List<String> projects = null;

        if (root instanceof WebOR) {
            projects = ((WebOR) root).getSharedProjects();
        } else if (root instanceof MobileOR) {
            projects = ((MobileOR) root).getSharedProjects();
        } else if (root instanceof StructuredDataOR) {
            projects = ((StructuredDataOR) root).getSharedProjects();
        } else if (root instanceof SapOR) {
            projects = ((SapOR) root).getSharedProjects();
        }

        if (projects != null && !projects.isEmpty()) {
            return "<br/><br/><b>Before proceeding, please verify whether this page/object is being used by the following project(s):</b><br/>"
                    + String.join(", ", projects);
        }
        return "";
    }

    private void pasteObject() {
        if (!ORClipboardManager.hasData()) {
            return;
        }
        ORObjectInf pastedObject = null;
        ORObjectClipboard cb = ORClipboardManager.get();
        ORObjectInf source = cb.getObject();
        boolean cut = cb.isCut();
        ORPageInf targetPage = getSelectedPage();
        if (targetPage == null && getSelectedObjectGroup() != null) {
            targetPage = getSelectedObjectGroup().getParent();
        }
        if (targetPage == null || source == null) {
            return;
        }
        ORRootInf currentOR = getOR();
        ORRootInf sourceOR  = (ORRootInf) source.getPage().getParent();
        ObjectGroup sourceGroup = source.getParent();
        ObjectRepository repo = getProject().getObjectRepository();
        if (cut && sourceOR instanceof WebOR && ((WebOR) sourceOR).isShared() && currentOR instanceof WebOR && !((WebOR) currentOR).isShared()) {
            Notification.show("Cut is not allowed from Shared to Project Object Repository");
            return;
        }
        if (cut && sourceOR instanceof MobileOR && ((MobileOR) sourceOR).isShared() && currentOR instanceof MobileOR && !((MobileOR) currentOR).isShared()) {
            Notification.show("Cut is not allowed from Shared to Project Object Repository");
            return;
        }
        if (cut && sourceOR instanceof StructuredDataOR && ((StructuredDataOR) sourceOR).isShared() && currentOR instanceof StructuredDataOR && !((StructuredDataOR) currentOR).isShared()) {
            Notification.show("Cut is not allowed from Shared to Project Object Repository");
            return;
        }
        if (cut && sourceOR instanceof SapOR && ((SapOR) sourceOR).isShared() && currentOR instanceof SapOR && !((SapOR) currentOR).isShared()) {
            Notification.show("Cut is not allowed from Shared to Project Object Repository");
            return;
        }
        if (cut && sourceOR instanceof WebOR && !((WebOR) sourceOR).isShared() && currentOR instanceof WebOR && ((WebOR) currentOR).isShared()) {
            Notification.show("Cut is not allowed in Shared Object Repository. Use `Move to Shared` instead.");
            return;
        }
        if (cut && sourceOR instanceof MobileOR && !((MobileOR) sourceOR).isShared() && currentOR instanceof MobileOR && ((MobileOR) currentOR).isShared()) {
            Notification.show("Cut is not allowed in Shared Object Repository. Use `Move to Shared` instead.");
            return;
        }
        if (cut && sourceOR instanceof StructuredDataOR && !((StructuredDataOR) sourceOR).isShared() && currentOR instanceof StructuredDataOR && ((StructuredDataOR) currentOR).isShared()) {
            Notification.show("Cut is not allowed in Shared Object Repository. Use `Move to Shared` instead.");
            return;
        }
        if (cut && sourceOR instanceof SapOR && !((SapOR) sourceOR).isShared() && currentOR instanceof SapOR && ((SapOR) currentOR).isShared()) {
            Notification.show("Cut is not allowed in Shared Object Repository. Use `Move to Shared` instead.");
            return;
        }
        if (sourceOR == currentOR) {
            String newGroupName;
            if (!cut) {
                newGroupName = computeCopyName(
                    targetPage,
                    (ORObjectInf) sourceGroup.getObjects().get(0)
                );
            } else {
                if (targetPage.getObjectGroupByName(sourceGroup.getName()) != null) {
                    newGroupName = computeCopyName(
                        targetPage,
                        (ORObjectInf) sourceGroup.getObjects().get(0)
                    );
                } else {
                    newGroupName = sourceGroup.getName();
                }
            }
            ObjectGroup newGroup = new ObjectGroup(newGroupName, targetPage);
            if (currentOR instanceof WebOR) {
                for (Object o : sourceGroup.getObjects()) {
                    WebORObject srcObj = (WebORObject) o;
                    String newObjectName;
                    if (!cut) {
                        newObjectName = computeCopyName(targetPage, srcObj);
                    } else {
                        if (objectNameExists(targetPage, srcObj.getName())) {
                            newObjectName = computeCopyName(targetPage, srcObj);
                        } else {
                            newObjectName = srcObj.getName();
                        }
                    }
                    WebORObject cloned = new WebORObject();
                    cloned.setName(newObjectName);
                    cloned.setParent(newGroup);
                    srcObj.clone(cloned);
                    newGroup.getObjects().add(cloned);
                }
                targetPage.getObjectGroups().add(newGroup);
                ((WebOR) currentOR).setSaved(false);
                repo.saveWebPageNow((WebORPage) targetPage);
            }
            else if (currentOR instanceof MobileOR) {
                for (Object o : sourceGroup.getObjects()) {
                    MobileORObject srcObj = (MobileORObject) o;
                    String newObjectName;
                    if (!cut) {
                        newObjectName = computeCopyName(targetPage, srcObj);
                    } else {
                        if (objectNameExists(targetPage, srcObj.getName())) {
                            newObjectName = computeCopyName(targetPage, srcObj);
                        } else {
                            newObjectName = srcObj.getName();
                        }
                    }
                    MobileORObject cloned = new MobileORObject();
                    cloned.setName(newObjectName);
                    cloned.setParent(newGroup);
                    srcObj.clone(cloned);
                    newGroup.getObjects().add(cloned);
                }
                targetPage.getObjectGroups().add(newGroup);
                ((MobileOR) currentOR).setSaved(false);
                repo.saveMobilePageNow((MobileORPage) targetPage);
            }
            else if (currentOR instanceof StructuredDataOR) {
                for (Object o : sourceGroup.getObjects()) {
                    StructuredDataORObject srcObj = (StructuredDataORObject) o;
                    String newObjectName;
                    if (!cut) {
                        newObjectName = computeCopyName(targetPage, srcObj);
                    } else {
                        if (objectNameExists(targetPage, srcObj.getName())) {
                            newObjectName = computeCopyName(targetPage, srcObj);
                        } else {
                            newObjectName = srcObj.getName();
                        }
                    }
                    StructuredDataORObject cloned = new StructuredDataORObject();
                    cloned.setName(newObjectName);
                    cloned.setParent(newGroup);
                    srcObj.clone(cloned);
                    newGroup.getObjects().add(cloned);
                    pastedObject = cloned;
                }
                targetPage.getObjectGroups().add(newGroup);
                ((StructuredDataOR) currentOR).setSaved(false);
                repo.saveStructuredDataPageNow((StructuredDataORPage) targetPage);
            }
            else if (currentOR instanceof SapOR) {
                for (Object o : sourceGroup.getObjects()) {
                    SapORObject srcObj = (SapORObject) o;
                    String newObjectName;
                    if (!cut) {
                        newObjectName = computeCopyName(targetPage, srcObj);
                    } else {
                        if (objectNameExists(targetPage, srcObj.getName())) {
                            newObjectName = computeCopyName(targetPage, srcObj);
                        } else {
                            newObjectName = srcObj.getName();
                        }
                    }
                    SapORObject cloned = new SapORObject();
                    cloned.setName(newObjectName);
                    cloned.setParent(newGroup);
                    srcObj.clone(cloned);
                    newGroup.getObjects().add(cloned);
                }
                targetPage.getObjectGroups().add(newGroup);
                ((SapOR) currentOR).setSaved(false);
                repo.saveSapPageNow((SapORPage) targetPage);
            }
            if (cut) {
                ORPageInf sourcePage = source.getPage();
                objectRemoved(source);
                sourceGroup.removeFromParent();
                ORClipboardManager.clear();
                if (sourceOR instanceof WebOR) {
                    ((WebOR) sourceOR).setSaved(false);
                    repo.saveWebPageNow((WebORPage) sourcePage);
                } else if (sourceOR instanceof MobileOR) {
                    ((MobileOR) sourceOR).setSaved(false);
                    repo.saveMobilePageNow((MobileORPage) sourcePage);
                } else if (sourceOR instanceof StructuredDataOR) {
                    ((StructuredDataOR) sourceOR).setSaved(false);
                    repo.saveStructuredDataPageNow((StructuredDataORPage) sourcePage);
                } else if (sourceOR instanceof SapOR) {
                    ((SapOR) sourceOR).setSaved(false);
                    repo.saveSapPageNow((SapORPage) sourcePage);
                }
            }
            reload();
            return;
        }
        if (currentOR instanceof WebOR && !((WebOR) currentOR).isShared() && sourceOR instanceof WebOR && ((WebOR) sourceOR).isShared()) {
            String baseName = sourceGroup.getName().replaceAll("_Copy_\\d+$", "");
            String newGroupName;
            int i = 1;
            do {
                newGroupName = baseName + "_Copy_" + i++;
            } while (targetPage.getObjectGroupByName(newGroupName) != null);
            ObjectGroup<WebORObject> newGroup = new ObjectGroup<>(newGroupName, (WebORPage) targetPage);
            for (Object o : sourceGroup.getObjects()) {
                WebORObject srcObj = (WebORObject) o;
                WebORObject cloned = new WebORObject();
                cloned.setName(newGroupName);
                cloned.setParent(newGroup);
                srcObj.clone(cloned);
                newGroup.getObjects().add(cloned);
            }
            targetPage.getObjectGroups().add(newGroup);
            reload();
            return;
        }
        if (currentOR instanceof MobileOR && !((MobileOR) currentOR).isShared() && sourceOR instanceof MobileOR && ((MobileOR) sourceOR).isShared()) {
            String baseName = sourceGroup.getName().replaceAll("_Copy_\\d+$", "");
            String newGroupName;
            int i = 1;
            do {
                newGroupName = baseName + "_Copy_" + i++;
            } while (targetPage.getObjectGroupByName(newGroupName) != null);
            ObjectGroup<MobileORObject> newGroup = new ObjectGroup<>(newGroupName, (MobileORPage) targetPage);
            for (Object o : sourceGroup.getObjects()) {
                MobileORObject srcObj = (MobileORObject) o;
                MobileORObject cloned = new MobileORObject();
                cloned.setName(newGroupName);
                cloned.setParent(newGroup);
                srcObj.clone(cloned);
                newGroup.getObjects().add(cloned);
            }
            targetPage.getObjectGroups().add(newGroup);
            reload();
            final ORObjectInf highlight = pastedObject;
            if (highlight != null) {
                SwingUtilities.invokeLater(() -> {
                    selectAndSrollTo(highlight.getTreePath());
                });
            }
            return;
        }
        if (currentOR instanceof StructuredDataOR && !((StructuredDataOR) currentOR).isShared() && sourceOR instanceof StructuredDataOR && ((StructuredDataOR) sourceOR).isShared()) {
            String baseName = sourceGroup.getName().replaceAll("_Copy_\\d+$", "");
            String newGroupName;
            int i = 1;
            do {
                newGroupName = baseName + "_Copy_" + i++;
            } while (targetPage.getObjectGroupByName(newGroupName) != null);
            ObjectGroup<StructuredDataORObject> newGroup = new ObjectGroup<>(newGroupName, (StructuredDataORPage) targetPage);
            for (Object o : sourceGroup.getObjects()) {
                StructuredDataORObject srcObj = (StructuredDataORObject) o;
                StructuredDataORObject cloned = new StructuredDataORObject();
                cloned.setName(newGroupName);
                cloned.setParent(newGroup);
                srcObj.clone(cloned);
                newGroup.getObjects().add(cloned);
            }
            targetPage.getObjectGroups().add(newGroup);
            reload();
            final ORObjectInf highlight = pastedObject;
            if (highlight != null) {
                SwingUtilities.invokeLater(() -> {
                    selectAndSrollTo(highlight.getTreePath());
                });
            }
            return;
        }
        if (currentOR instanceof MobileOR && !((MobileOR) currentOR).isShared() && sourceOR instanceof MobileOR && ((MobileOR) sourceOR).isShared()) {
            String baseName = sourceGroup.getName().replaceAll("_Copy_\\d+$", "");
            String newGroupName;
            int i = 1;
            do {
                newGroupName = baseName + "_Copy_" + i++;
            } while (targetPage.getObjectGroupByName(newGroupName) != null);
            ObjectGroup<MobileORObject> newGroup = new ObjectGroup<>(newGroupName, (MobileORPage) targetPage);
            for (Object o : sourceGroup.getObjects()) {
                MobileORObject srcObj = (MobileORObject) o;
                MobileORObject cloned = new MobileORObject();
                cloned.setName(newGroupName);
                cloned.setParent(newGroup);
                srcObj.clone(cloned);
                newGroup.getObjects().add(cloned);
            }
            targetPage.getObjectGroups().add(newGroup);
            reload();
            final ORObjectInf highlight = pastedObject;
            if (highlight != null) {
                SwingUtilities.invokeLater(() -> {
                    selectAndSrollTo(highlight.getTreePath());
                });
            }
            return;
        }
        if (currentOR instanceof StructuredDataOR && !((StructuredDataOR) currentOR).isShared() && sourceOR instanceof StructuredDataOR && ((StructuredDataOR) sourceOR).isShared()) {
            String baseName = sourceGroup.getName().replaceAll("_Copy_\\d+$", "");
            String newGroupName;
            int i = 1;
            do {
                newGroupName = baseName + "_Copy_" + i++;
            } while (targetPage.getObjectGroupByName(newGroupName) != null);
            ObjectGroup<StructuredDataORObject> newGroup = new ObjectGroup<>(newGroupName, (StructuredDataORPage) targetPage);
            for (Object o : sourceGroup.getObjects()) {
                StructuredDataORObject srcObj = (StructuredDataORObject) o;
                StructuredDataORObject cloned = new StructuredDataORObject();
                cloned.setName(newGroupName);
                cloned.setParent(newGroup);
                srcObj.clone(cloned);
                newGroup.getObjects().add(cloned);
            }
            targetPage.getObjectGroups().add(newGroup);
            reload();
            final ORObjectInf highlight = pastedObject;
            if (highlight != null) {
                SwingUtilities.invokeLater(() -> {
                    selectAndSrollTo(highlight.getTreePath());
                });
            }
            return;
        }
        if (currentOR instanceof MobileOR && !((MobileOR) currentOR).isShared() && sourceOR instanceof MobileOR && ((MobileOR) sourceOR).isShared()) {
            String baseName = sourceGroup.getName().replaceAll("_Copy_\\d+$", "");
            String newGroupName;
            int i = 1;
            do {
                newGroupName = baseName + "_Copy_" + i++;
            } while (targetPage.getObjectGroupByName(newGroupName) != null);
            ObjectGroup<MobileORObject> newGroup = new ObjectGroup<>(newGroupName, (MobileORPage) targetPage);
            for (Object o : sourceGroup.getObjects()) {
                MobileORObject srcObj = (MobileORObject) o;
                MobileORObject cloned = new MobileORObject();
                cloned.setName(newGroupName);
                cloned.setParent(newGroup);
                srcObj.clone(cloned);
                newGroup.getObjects().add(cloned);
            }
            targetPage.getObjectGroups().add(newGroup);
            reload();
            final ORObjectInf highlight = pastedObject;
            if (highlight != null) {
                SwingUtilities.invokeLater(() -> {
                    selectAndSrollTo(highlight.getTreePath());
                });
            }
            return;
        }
        if (currentOR instanceof StructuredDataOR && !((StructuredDataOR) currentOR).isShared() && sourceOR instanceof StructuredDataOR && ((StructuredDataOR) sourceOR).isShared()) {
            String baseName = sourceGroup.getName().replaceAll("_Copy_\\d+$", "");
            String newGroupName;
            int i = 1;
            do {
                newGroupName = baseName + "_Copy_" + i++;
            } while (targetPage.getObjectGroupByName(newGroupName) != null);
            ObjectGroup<StructuredDataORObject> newGroup = new ObjectGroup<>(newGroupName, (StructuredDataORPage) targetPage);
            for (Object o : sourceGroup.getObjects()) {
                StructuredDataORObject srcObj = (StructuredDataORObject) o;
                StructuredDataORObject cloned = new StructuredDataORObject();
                cloned.setName(newGroupName);
                cloned.setParent(newGroup);
                srcObj.clone(cloned);
                newGroup.getObjects().add(cloned);
            }
            targetPage.getObjectGroups().add(newGroup);
            reload();
            final ORObjectInf highlight = pastedObject;
            if (highlight != null) {
                SwingUtilities.invokeLater(() -> {
                    selectAndSrollTo(highlight.getTreePath());
                });
            }
            return;
        }
        if (currentOR instanceof MobileOR && !((MobileOR) currentOR).isShared() && sourceOR instanceof MobileOR && ((MobileOR) sourceOR).isShared()) {
            String baseName = sourceGroup.getName().replaceAll("_Copy_\\d+$", "");
            String newGroupName;
            int i = 1;
            do {
                newGroupName = baseName + "_Copy_" + i++;
            } while (targetPage.getObjectGroupByName(newGroupName) != null);
            ObjectGroup<MobileORObject> newGroup = new ObjectGroup<>(newGroupName, (MobileORPage) targetPage);
            for (Object o : sourceGroup.getObjects()) {
                MobileORObject srcObj = (MobileORObject) o;
                MobileORObject cloned = new MobileORObject();
                cloned.setName(newGroupName);
                cloned.setParent(newGroup);
                srcObj.clone(cloned);
                newGroup.getObjects().add(cloned);
            }
            targetPage.getObjectGroups().add(newGroup);
            reload();
            final ORObjectInf highlight = pastedObject;
            if (highlight != null) {
                SwingUtilities.invokeLater(() -> {
                    selectAndSrollTo(highlight.getTreePath());
                });
            }
            return;
        }
        if (currentOR instanceof StructuredDataOR && !((StructuredDataOR) currentOR).isShared() && sourceOR instanceof StructuredDataOR && ((StructuredDataOR) sourceOR).isShared()) {
            String baseName = sourceGroup.getName().replaceAll("_Copy_\\d+$", "");
            String newGroupName;
            int i = 1;
            do {
                newGroupName = baseName + "_Copy_" + i++;
            } while (targetPage.getObjectGroupByName(newGroupName) != null);
            ObjectGroup<StructuredDataORObject> newGroup = new ObjectGroup<>(newGroupName, (StructuredDataORPage) targetPage);
            for (Object o : sourceGroup.getObjects()) {
                StructuredDataORObject srcObj = (StructuredDataORObject) o;
                StructuredDataORObject cloned = new StructuredDataORObject();
                cloned.setName(newGroupName);
                cloned.setParent(newGroup);
                srcObj.clone(cloned);
                newGroup.getObjects().add(cloned);
            }
            targetPage.getObjectGroups().add(newGroup);
            reload();
            final ORObjectInf highlight = pastedObject;
            if (highlight != null) {
                SwingUtilities.invokeLater(() -> {
                    selectAndSrollTo(highlight.getTreePath());
                });
            }
            return;
        }
        if (currentOR instanceof MobileOR && !((MobileOR) currentOR).isShared() && sourceOR instanceof MobileOR && ((MobileOR) sourceOR).isShared()) {
            String baseName = sourceGroup.getName().replaceAll("_Copy_\\d+$", "");
            String newGroupName;
            int i = 1;
            do {
                newGroupName = baseName + "_Copy_" + i++;
            } while (targetPage.getObjectGroupByName(newGroupName) != null);
            ObjectGroup<MobileORObject> newGroup = new ObjectGroup<>(newGroupName, (MobileORPage) targetPage);
            for (Object o : sourceGroup.getObjects()) {
                MobileORObject srcObj = (MobileORObject) o;
                MobileORObject cloned = new MobileORObject();
                cloned.setName(newGroupName);
                cloned.setParent(newGroup);
                srcObj.clone(cloned);
                newGroup.getObjects().add(cloned);
            }
            targetPage.getObjectGroups().add(newGroup);
            reload();
            final ORObjectInf highlight = pastedObject;
            if (highlight != null) {
                SwingUtilities.invokeLater(() -> {
                    selectAndSrollTo(highlight.getTreePath());
                });
            }
            return;
        }
        if (currentOR instanceof StructuredDataOR && !((StructuredDataOR) currentOR).isShared() && sourceOR instanceof StructuredDataOR && ((StructuredDataOR) sourceOR).isShared()) {
            String baseName = sourceGroup.getName().replaceAll("_Copy_\\d+$", "");
            String newGroupName;
            int i = 1;
            do {
                newGroupName = baseName + "_Copy_" + i++;
            } while (targetPage.getObjectGroupByName(newGroupName) != null);
            ObjectGroup<StructuredDataORObject> newGroup = new ObjectGroup<>(newGroupName, (StructuredDataORPage) targetPage);
            for (Object o : sourceGroup.getObjects()) {
                StructuredDataORObject srcObj = (StructuredDataORObject) o;
                StructuredDataORObject cloned = new StructuredDataORObject();
                cloned.setName(newGroupName);
                cloned.setParent(newGroup);
                srcObj.clone(cloned);
                newGroup.getObjects().add(cloned);
            }
            targetPage.getObjectGroups().add(newGroup);
            reload();
            final ORObjectInf highlight = pastedObject;
            if (highlight != null) {
                SwingUtilities.invokeLater(() -> {
                    selectAndSrollTo(highlight.getTreePath());
                });
            }
            return;
        }
        String expectedObjectName = source.getName();
        String targetPageName = targetPage.getName();
        if (currentOR instanceof WebOR) {
            ResolvedWebObject resolved =
                repo.resolveWebObjectWithScope(
                    source.getPage().getName(),
                    sourceGroup.getName()
                );
            if (resolved == null) {
                Notification.show("Failed to resolve Web object");
                return;
            }
            repo.copyWebObject(resolved, targetPage.getName());
            if (cut) {
                objectRemoved(source);
                source.removeFromParent();
                ORClipboardManager.clear();
            }
            reload();
            SwingUtilities.invokeLater(() -> {
                ORPageInf page = getOR().getPageByName(targetPageName);
                if (page != null) {
                    ORObjectInf pasted = findObjectInPage(page, expectedObjectName);
                    if (pasted != null) {
                        selectAndSrollTo(pasted.getTreePath());
                    }
                }
            });
            return;
        }
        if (currentOR instanceof MobileOR) {
            ResolvedMobileObject resolved =
                repo.resolveMobileObjectWithScope(
                    source.getPage().getName(),
                    sourceGroup.getName()
                );
            if (resolved == null || !resolved.isPresent()) {
                Notification.show("Failed to resolve Mobile object");
                return;
            }
            repo.copyMobileObject(resolved, targetPage.getName());
            if (cut) {
                objectRemoved(source);
                source.removeFromParent();
                ORClipboardManager.clear();
            }
            reload();
            SwingUtilities.invokeLater(() -> {
                ORPageInf page = getOR().getPageByName(targetPageName);
                if (page != null) {
                    ORObjectInf pasted = findObjectInPage(page, expectedObjectName);
                    if (pasted != null) {
                        selectAndSrollTo(pasted.getTreePath());
                    }
                }
            });
        }
        if (currentOR instanceof StructuredDataOR) {
            ResolvedStructuredDataObject resolved =
                repo.resolveStructuredDataObjectWithScope(
                    source.getPage().getName(),
                    sourceGroup.getName()
                );
            if (resolved == null || !resolved.isPresent()) {
                Notification.show("Failed to resolve Structured Data object");
                return;
            }
            repo.copyStructuredDataObject(resolved, targetPage.getName());
            if (cut) {
                objectRemoved(source);
                source.removeFromParent();
                ORClipboardManager.clear();
            }
            reload();
            SwingUtilities.invokeLater(() -> {
                ORPageInf page = getOR().getPageByName(targetPageName);
                if (page != null) {
                    ORObjectInf pasted = findObjectInPage(page, expectedObjectName);
                    if (pasted != null) {
                        selectAndSrollTo(pasted.getTreePath());
                    }
                }
            });
        }
        if (currentOR instanceof SapOR) {
            ResolvedSapObject resolved =
                repo.resolveSapObjectWithScope(
                    source.getPage().getName(),
                    sourceGroup.getName()
                );
            if (resolved == null || !resolved.isPresent()) {
                Notification.show("Failed to resolve SAP object");
                return;
            }
            repo.copySapObject(resolved, targetPage.getName());
            if (cut) {
                objectRemoved(source);
                source.removeFromParent();
                ORClipboardManager.clear();
            }
            reload();
            SwingUtilities.invokeLater(() -> {
                ORPageInf page = getOR().getPageByName(targetPageName);
                if (page != null) {
                    ORObjectInf pasted = findObjectInPage(page, expectedObjectName);
                    if (pasted != null) {
                        selectAndSrollTo(pasted.getTreePath());
                    }
                }
            });
        }
    }

    private String computeCopyName(ORPageInf page, ORObjectInf source) {
        String original = source.getName();
        String base = original.replaceAll("_Copy_\\d+$", "");
        int index = 1;
        String candidate;
        do {
            candidate = base + "_Copy_" + index++;
        } while (objectNameExists(page, candidate));
        return candidate;
    }

    private ORObjectInf findObjectInPage(ORPageInf page, String objectName) {
        for (Object groupObj : page.getObjectGroups()) {
            ObjectGroup<?> group = (ObjectGroup<?>) groupObj;
            for (Object obj : group.getObjects()) {
                ORObjectInf orObj = (ORObjectInf) obj;
                if (objectName.equals(orObj.getName())) {
                    return orObj;
                }
            }
        }
        return null;
    }

    private boolean objectNameExists(ORPageInf page, String name) {
        for (Object groupObj : page.getObjectGroups()) {
            ObjectGroup group = (ObjectGroup) groupObj;
            Enumeration<?> children = group.children();
            while (children.hasMoreElements()) {
                Object child = children.nextElement();
                if (child instanceof ORObjectInf) {
                    ORObjectInf obj = (ORObjectInf) child;
                    if (name.equals(obj.getName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    
    private void copySelection() {
        if (getSelectedObject() != null) {
            ORClipboardManager.copy(getSelectedObject());
            return;
        }
        if (getSelectedPage() != null) {
            ORClipboardManager.copy(getSelectedPage());
        }
    }
    private void cutSelection() {
        if (getSelectedObject() != null) {
            ORObjectInf obj = getSelectedObject();
            ORRootInf sourceOR = (ORRootInf) obj.getPage().getParent();
            ORRootInf currentOR = getOR();
            if (sourceOR != currentOR) {
                Notification.show("Cut is allowed only within the same Object Repository.");
                return;
            }
            ORClipboardManager.cut(obj);
            return;
        }
        if (getSelectedPage() != null) {
            ORPageInf page = getSelectedPage();
            ORRootInf sourceOR = (ORRootInf) page.getParent();
            ORRootInf currentOR = getOR();
            if (sourceOR != currentOR) {
                Notification.show("Cut is allowed only within the same Object Repository.");
                return;
            }
            ORClipboardManager.cut(page);
        }
    }

    private void pasteSelection() {
        if (!ORClipboardManager.hasData()) {
            return;
        }
        ORObjectClipboard cb = ORClipboardManager.get();
        if (cb.getType() == ORObjectClipboard.Type.OBJECT) {
            pasteObject();
            return;
        }
        if (cb.getType() == ORObjectClipboard.Type.PAGE) {
            pastePage();
        }
    }

    private void pastePage() {
        if (!ORClipboardManager.hasData()) {
            return;
        }
        ORObjectClipboard cb = ORClipboardManager.get();
        ORPageInf sourcePage = cb.getPage();
        boolean cut = cb.isCut();
        if (sourcePage == null) {
            return;
        }
        ORRootInf currentOR = getOR();
        ORRootInf sourceOR = (ORRootInf) sourcePage.getParent();
        ObjectRepository repo = getProject().getObjectRepository();
        if (cut && sourceOR instanceof WebOR && ((WebOR) sourceOR).isShared() && currentOR instanceof WebOR && !((WebOR) currentOR).isShared()) {
            Notification.show("Cut is not allowed from Shared to Project Object Repository"
            );
            return;
        }
        if (cut && sourceOR instanceof MobileOR && ((MobileOR) sourceOR).isShared() && currentOR instanceof MobileOR && !((MobileOR) currentOR).isShared()) {
            Notification.show("Cut is not allowed from Shared to Project Object Repository"
            );
            return;
        }
        if (cut && sourceOR instanceof StructuredDataOR && ((StructuredDataOR) sourceOR).isShared() && currentOR instanceof StructuredDataOR && !((StructuredDataOR) currentOR).isShared()) {
            Notification.show("Cut is not allowed from Shared to Project Object Repository"
            );
            return;
        }
        if (cut && sourceOR instanceof SapOR && ((SapOR) sourceOR).isShared() && currentOR instanceof SapOR && !((SapOR) currentOR).isShared()) {
            Notification.show("Cut is not allowed from Shared to Project Object Repository"
            );
            return;
        }
        if (cut && sourceOR instanceof WebOR && !((WebOR) sourceOR).isShared() && currentOR instanceof WebOR && ((WebOR) currentOR).isShared()) {
            Notification.show( "Cut is not allowed in Shared Object Repository. Use `Move to Shared` instead.");
            return;
        }
        if (cut && sourceOR instanceof MobileOR && !((MobileOR) sourceOR).isShared() && currentOR instanceof MobileOR && ((MobileOR) currentOR).isShared()) {
            Notification.show( "Cut is not allowed in Shared Object Repository. Use `Move to Shared` instead.");
            return;
        }
        if (cut && sourceOR instanceof StructuredDataOR && !((StructuredDataOR) sourceOR).isShared() && currentOR instanceof StructuredDataOR && ((StructuredDataOR) currentOR).isShared()) {
            Notification.show( "Cut is not allowed in Shared Object Repository. Use `Move to Shared` instead.");
            return;
        }
        if (cut && sourceOR instanceof SapOR && !((SapOR) sourceOR).isShared() && currentOR instanceof SapOR && ((SapOR) currentOR).isShared()) {
            Notification.show( "Cut is not allowed in Shared Object Repository. Use `Move to Shared` instead.");
            return;
        }
        if (sourceOR == currentOR) {
            String newPageName = cut
                ? sourcePage.getName()
                : computeCopyPageName(sourcePage);
            ORPageInf newPage = getOR().addPage(newPageName);
            if (currentOR instanceof WebOR) {
                WebORPage srcPage = (WebORPage) sourcePage;
                WebORPage tgtPage = (WebORPage) newPage;
                for (Object g : srcPage.getObjectGroups()) {
                    ObjectGroup srcGroup = (ObjectGroup) g;
                    ObjectGroup newGroup = new ObjectGroup(srcGroup.getName(), tgtPage);
                    for (Object o : srcGroup.getObjects()) {
                        WebORObject srcObj = (WebORObject) o;
                        WebORObject cloned = new WebORObject();
                        cloned.setName(srcObj.getName());
                        cloned.setParent(newGroup);
                        srcObj.clone(cloned);
                        newGroup.getObjects().add(cloned);
                    }
                    tgtPage.getObjectGroups().add(newGroup);
                }
            }
            else if (currentOR instanceof MobileOR) {
                MobileORPage srcPage = (MobileORPage) sourcePage;
                MobileORPage tgtPage = (MobileORPage) newPage;
                for (Object g : srcPage.getObjectGroups()) {
                    ObjectGroup srcGroup = (ObjectGroup) g;
                    ObjectGroup newGroup = new ObjectGroup(srcGroup.getName(), tgtPage);
                    for (Object o : srcGroup.getObjects()) {
                        MobileORObject srcObj = (MobileORObject) o;
                        MobileORObject cloned = new MobileORObject();
                        cloned.setName(srcObj.getName());
                        cloned.setParent(newGroup);
                        srcObj.clone(cloned);
                        newGroup.getObjects().add(cloned);
                    }
                    tgtPage.getObjectGroups().add(newGroup);
                }
            }
            else if (currentOR instanceof StructuredDataOR) {
                StructuredDataORPage srcPage = (StructuredDataORPage) sourcePage;
                StructuredDataORPage tgtPage = (StructuredDataORPage) newPage;
                for (Object g : srcPage.getObjectGroups()) {
                    ObjectGroup srcGroup = (ObjectGroup) g;
                    ObjectGroup newGroup = new ObjectGroup(srcGroup.getName(), tgtPage);
                    for (Object o : srcGroup.getObjects()) {
                        StructuredDataORObject srcObj = (StructuredDataORObject) o;
                        StructuredDataORObject cloned = new StructuredDataORObject();
                        cloned.setName(srcObj.getName());
                        cloned.setParent(newGroup);
                        srcObj.clone(cloned);
                        newGroup.getObjects().add(cloned);
                    }
                    tgtPage.getObjectGroups().add(newGroup);
                }
            }
            else if (currentOR instanceof SapOR) {
                SapORPage srcPage = (SapORPage) sourcePage;
                SapORPage tgtPage = (SapORPage) newPage;
                for (Object g : srcPage.getObjectGroups()) {
                    ObjectGroup srcGroup = (ObjectGroup) g;
                    ObjectGroup newGroup = new ObjectGroup(srcGroup.getName(), tgtPage);
                    for (Object o : srcGroup.getObjects()) {
                        SapORObject srcObj = (SapORObject) o;
                        SapORObject cloned = new SapORObject();
                        cloned.setName(srcObj.getName());
                        cloned.setParent(newGroup);
                        srcObj.clone(cloned);
                        newGroup.getObjects().add(cloned);
                    }
                    tgtPage.getObjectGroups().add(newGroup);
                }
            }
            pageAdded(newPage);
            if (cut) {
                pageRemoved(sourcePage);
                sourcePage.removeFromParent();
                ORClipboardManager.clear();
            }
            reload();
            return;
        }
        if (currentOR instanceof WebOR && !((WebOR) currentOR).isShared() && sourceOR instanceof WebOR && ((WebOR) sourceOR).isShared()) {
            String newPageName = cut
                ? sourcePage.getName()
                : computeCopyPageName(sourcePage);
            ORPageInf newPage = getOR().addPage(newPageName);
            WebORPage srcPage = (WebORPage) sourcePage;
            WebORPage tgtPage = (WebORPage) newPage;
            for (Object g : srcPage.getObjectGroups()) {
                ObjectGroup srcGroup = (ObjectGroup) g;
                ObjectGroup newGroup = new ObjectGroup(srcGroup.getName(), tgtPage);
                for (Object o : srcGroup.getObjects()) {
                    WebORObject srcObj = (WebORObject) o;
                    WebORObject cloned = new WebORObject();
                    cloned.setName(srcObj.getName());
                    cloned.setParent(newGroup);
                    srcObj.clone(cloned);
                    newGroup.getObjects().add(cloned);
                }
                tgtPage.getObjectGroups().add(newGroup);
            }
            pageAdded(newPage);
            reload();
            return;
        }
        if (currentOR instanceof MobileOR && !((MobileOR) currentOR).isShared() && sourceOR instanceof MobileOR && ((MobileOR) sourceOR).isShared()) {
            String newPageName = cut
                ? sourcePage.getName()
                : computeCopyPageName(sourcePage);
            ORPageInf newPage = getOR().addPage(newPageName);
            MobileORPage srcPage = (MobileORPage) sourcePage;
            MobileORPage tgtPage = (MobileORPage) newPage;
            for (Object g : srcPage.getObjectGroups()) {
                ObjectGroup srcGroup = (ObjectGroup) g;
                ObjectGroup newGroup = new ObjectGroup(srcGroup.getName(), tgtPage);
                for (Object o : srcGroup.getObjects()) {
                    MobileORObject srcObj = (MobileORObject) o;
                    MobileORObject cloned = new MobileORObject();
                    cloned.setName(srcObj.getName());
                    cloned.setParent(newGroup);
                    srcObj.clone(cloned);
                    newGroup.getObjects().add(cloned);
                }
                tgtPage.getObjectGroups().add(newGroup);
            }
            pageAdded(newPage);
            reload();
            SwingUtilities.invokeLater(() -> {selectAndSrollTo(newPage.getTreePath());});
            return;
        }
        if (currentOR instanceof StructuredDataOR && !((StructuredDataOR) currentOR).isShared() && sourceOR instanceof StructuredDataOR && ((StructuredDataOR) sourceOR).isShared()) {
            String newPageName = cut
                ? sourcePage.getName()
                : computeCopyPageName(sourcePage);
            ORPageInf newPage = getOR().addPage(newPageName);
            StructuredDataORPage srcPage = (StructuredDataORPage) sourcePage;
            StructuredDataORPage tgtPage = (StructuredDataORPage) newPage;
            for (Object g : srcPage.getObjectGroups()) {
                ObjectGroup srcGroup = (ObjectGroup) g;
                ObjectGroup newGroup = new ObjectGroup(srcGroup.getName(), tgtPage);
                for (Object o : srcGroup.getObjects()) {
                    StructuredDataORObject srcObj = (StructuredDataORObject) o;
                    StructuredDataORObject cloned = new StructuredDataORObject();
                    cloned.setName(srcObj.getName());
                    cloned.setParent(newGroup);
                    srcObj.clone(cloned);
                    newGroup.getObjects().add(cloned);
                }
                tgtPage.getObjectGroups().add(newGroup);
            }
            pageAdded(newPage);
            reload();
            SwingUtilities.invokeLater(() -> {selectAndSrollTo(newPage.getTreePath());});
            return;
        }
        if (currentOR instanceof SapOR && !((SapOR) currentOR).isShared() && sourceOR instanceof SapOR && ((SapOR) sourceOR).isShared()) {
            String newPageName = cut
                ? sourcePage.getName()
                : computeCopyPageName(sourcePage);
            ORPageInf newPage = getOR().addPage(newPageName);
            SapORPage srcPage = (SapORPage) sourcePage;
            SapORPage tgtPage = (SapORPage) newPage;
            for (Object g : srcPage.getObjectGroups()) {
                ObjectGroup srcGroup = (ObjectGroup) g;
                ObjectGroup newGroup = new ObjectGroup(srcGroup.getName(), tgtPage);
                for (Object o : srcGroup.getObjects()) {
                    SapORObject srcObj = (SapORObject) o;
                    SapORObject cloned = new SapORObject();
                    cloned.setName(srcObj.getName());
                    cloned.setParent(newGroup);
                    srcObj.clone(cloned);
                    newGroup.getObjects().add(cloned);
                }
                tgtPage.getObjectGroups().add(newGroup);
            }
            pageAdded(newPage);
            reload();
            SwingUtilities.invokeLater(() -> {selectAndSrollTo(newPage.getTreePath());});
            return;
        }
        if (currentOR instanceof WebOR) {
            String targetName = cut
                ? sourcePage.getName()
                : computeCopyPageName(sourcePage);
            repo.copyWebPage(sourcePage.getName(), targetName);
            if (cut) {
                pageRemoved(sourcePage);
                sourcePage.removeFromParent();
                ORClipboardManager.clear();
            }
            reload();
            SwingUtilities.invokeLater(() -> {
                ORPageInf pastedPage = getOR().getPageByName(targetName);
                if (pastedPage != null) {
                    selectAndSrollTo(pastedPage.getTreePath());
                }
            });
            return;
        }
        if (currentOR instanceof MobileOR) {
            String targetName = cut
                ? sourcePage.getName()
                : computeCopyPageName(sourcePage);
            repo.copyMobilePage(sourcePage.getName(), targetName);
            if (cut) {
                pageRemoved(sourcePage);
                sourcePage.removeFromParent();
                ORClipboardManager.clear();
            }
            reload();
            SwingUtilities.invokeLater(() -> {
                ORPageInf pastedPage = getOR().getPageByName(targetName);
                if (pastedPage != null) {
                    selectAndSrollTo(pastedPage.getTreePath());
                }
            });
        }
        if (currentOR instanceof StructuredDataOR) {
            String targetName = cut
                ? sourcePage.getName()
                : computeCopyPageName(sourcePage);
            repo.copyMobilePage(sourcePage.getName(), targetName);
            if (cut) {
                pageRemoved(sourcePage);
                sourcePage.removeFromParent();
                ORClipboardManager.clear();
            }
            reload();
            SwingUtilities.invokeLater(() -> {
                ORPageInf pastedPage = getOR().getPageByName(targetName);
                if (pastedPage != null) {
                    selectAndSrollTo(pastedPage.getTreePath());
                }
            });
        }
        if (currentOR instanceof SapOR) {
            String targetName = cut
                ? sourcePage.getName()
                : computeCopyPageName(sourcePage);
            repo.copySapPage(sourcePage.getName(), targetName);
            if (cut) {
                pageRemoved(sourcePage);
                sourcePage.removeFromParent();
                ORClipboardManager.clear();
            }
            reload();
            SwingUtilities.invokeLater(() -> {
                ORPageInf pastedPage = getOR().getPageByName(targetName);
                if (pastedPage != null) {
                    selectAndSrollTo(pastedPage.getTreePath());
                }
            });
        }
    }

    private String computeCopyPageName(ORPageInf source) {
        String base = source.getName().replaceAll("_Copy_\\d+$", "");
        int i = 1;
        String candidate;
        do {
            candidate = base + "_Copy_" + i++;
        } while (getOR().getPageByName(candidate) != null);
        return candidate;
    }
}