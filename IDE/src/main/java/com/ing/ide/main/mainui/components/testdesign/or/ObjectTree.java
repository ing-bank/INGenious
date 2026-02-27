
package com.ing.ide.main.mainui.components.testdesign.or;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.web.ResolvedWebObject;
import com.ing.datalib.or.web.WebOR;
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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
        
        tree.setTransferHandler(new ObjectDnD(tree));

        tree.getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.NEW, "New");
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.DELETE, "Delete");
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
            case "Copy to Shared":
                copyToShared();
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
                for (ORObjectInf object : objects) {
                    objectRemoved(object);
                    object.removeFromParent();
                }
            }
        }
    }
    
    private void removeUnusedObject() {
        try {
            Map<String, List> allORObject = new HashMap<String, List>();
            Map<String, List> unUsedbject = new HashMap<String, List>();
            List<String> objects = new ArrayList<>();
            List<ORPageInf> pages = getSelectedPages();
            if (!pages.isEmpty()) {
                for (ORPageInf selectedPage : pages) {
                    String page = selectedPage.toString();
                    String orFilePath = getProject().getLocation() + "/OR.object";
                    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                    Document doc = documentBuilder.parse(orFilePath);
                    NodeList pageList = doc.getElementsByTagName("Page");
                    for (int i = 0; i < pageList.getLength(); i++) {

                        Node pageNode = pageList.item(i);
                        if (pageNode.getNodeType() == Node.ELEMENT_NODE) {
                            Element pageElement = (Element) pageNode;
                            String pageName = pageElement.getAttribute("ref");
                            if (pageName.equals(page)) {
                                NodeList objectGroupNodeList = pageElement.getChildNodes();
                                for (int j = 0; j < objectGroupNodeList.getLength(); j++) {

                                    Node objectGroupNode = objectGroupNodeList.item(j);
                                    if (objectGroupNode.getNodeType() == Node.ELEMENT_NODE) {
                                        if (pageNode.getNodeType() == Node.ELEMENT_NODE) {
                                            Element objectGroupElement = (Element) objectGroupNode;
                                            NodeList objectList = objectGroupElement.getChildNodes();

                                            for (int k = 0; k < objectList.getLength(); k++) {
                                                Node objectNode = objectList.item(k);
                                                if (objectNode.getNodeType() == Node.ELEMENT_NODE) {

                                                    Element objectElement = (Element) objectNode;
                                                    String objectName = objectElement.getAttribute("ref");
                                                    if (!allORObject.containsKey(page)) {
                                                        allORObject.put(page, new ArrayList<String>());
                                                        allORObject.get(page).add(objectName);
                                                    } else {
                                                        allORObject.get(page).add(objectName);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            unUsedbject = UnusedObject(allORObject, usedObject());
            int unusedObjectCount = 0;
            for (String page : unUsedbject.keySet()) {

                List<String> unUsedobjects = unUsedbject.get(page);
                if (!unUsedobjects.isEmpty()) {
                    unusedObjectCount = unusedObjectCount + 1;
                    int option = JOptionPane.showConfirmDialog(null,
                            "<html><body><p style='width: 200px;'>"
                            + "Are you sure want to delete the following Objects from page [ "
                            + page
                            + " ]"
                            + " ?<br>"
                            + unUsedobjects
                            + "</p></body></html>",
                            "Delete Objects",
                            JOptionPane.YES_NO_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                        for (String objectName : unUsedobjects) {
                            deleteUnusedObject(page, objectName);

                        }

                    }
                }

            }
            if (unusedObjectCount != 0) {
                int option = JOptionPane.showConfirmDialog(null,
                        "<html><body><p style='width: 200px;'>"
                        + "Do you want to restart INGenious to load updated Object Repository ?"
                        + " <br>"
                        + "</p></body></html>",
                        "Restart INGenious",
                        JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    AppMainFrame s = new AppMainFrame();
                    s.restart();
                }
            } else {
                 
                 JOptionPane.showMessageDialog(null,
                        "<html><body><p style='width: 200px;'>"
                        + "No unused object found"
                        + " <br>"
                        + "</p></body></html>",
                        "Unused Object",
                        JOptionPane.OK_OPTION);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

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
                for (ORPageInf page : pages) {
                    pageRemoved(page);
                    page.removeFromParent();
                }
            }
        }
    }
    
    private void getImpactedTestCases() {
        ObjectGroup group = getSelectedObjectGroup();
        if (group == null) {
            if (getSelectedObject() != null) {
                group = getSelectedObject().getParent();
            } else {
                Notification.show("Not supported for the selected");
                return;
            }
        }
        String pageName = group.getParent().getName();
        String objectName = group.getName();
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

    private void copyToShared() {
        ORObjectInf obj = getSelectedObject();
        ObjectGroup group = getSelectedObjectGroup();
        ORPageInf selectedPage = getSelectedPage();

        if (obj == null && group == null && selectedPage == null) {
            com.ing.ide.util.Notification.show("Select an Object, Object Group, or Page.");
            return;
        }

        ORPageInf page = (obj != null) ? obj.getPage()
                : (group != null) ? group.getParent()
                : selectedPage;

        ObjectRepository repo = getProject().getObjectRepository();

        ORRootInf root = getOR();
        boolean isWeb = (root instanceof com.ing.datalib.or.web.WebOR);
        boolean isMobile = (root instanceof com.ing.datalib.or.mobile.MobileOR);

        if (obj == null && group == null && selectedPage != null) {
            if (isWeb) {
                String newName = repo.copyWebPage(page.getName(), page.getName());
                if (newName != null) {
                    com.ing.ide.util.Notification.show("Copied Page '" + page.getName() + "' to Shared Web Object successfully as '" + newName + "'.");
                } else {
                    com.ing.ide.util.Notification.show("Copy failed. Could not copy Page '" + page.getName() + "' to Shared Web Objects.");
                }
                if (newName != null) {
                    if (this instanceof com.ing.ide.main.mainui.components.testdesign.or.web.WebObjectTree) {
                        com.ing.ide.main.mainui.components.testdesign.or.web.WebORPanel panel =
                            ((com.ing.ide.main.mainui.components.testdesign.or.web.WebObjectTree) this).getORPanel();
                        panel.getSharedTree().load();
                    } else {
                        reload();
                    }
                }
                return;
            } else if (isMobile) {
                String newName = repo.copyMobilePage(page.getName(), page.getName());
                if (newName != null) {
                    com.ing.ide.util.Notification.show("Copied Page '" + page.getName() + "' to Shared Mobile Object successfully as '" + newName + "'.");
                } else {
                    com.ing.ide.util.Notification.show("Copy failed. Could not copy Page '" + page.getName() + "' to Shared Mobile Objects.");
                }
                if (newName != null) {
                    if (this instanceof com.ing.ide.main.mainui.components.testdesign.or.mobile.MobileObjectTree) {
                        com.ing.ide.main.mainui.components.testdesign.or.mobile.MobileORPanel panel =
                            ((com.ing.ide.main.mainui.components.testdesign.or.mobile.MobileObjectTree) this).getORPanel();
                        panel.getSharedTree().load();
                    } else {
                        reload();
                    }
                }
                return;
            }
        }

        String objectName = (obj != null) ? obj.getName() : group.getName();

        if (isWeb) {
            ResolvedWebObject resolved =
                repo.resolveWebObject(
                    new ResolvedWebObject.PageRef(page.getName(), com.ing.datalib.or.web.WebOR.ORScope.PROJECT),
                    objectName
                );
            if (resolved == null) {
                com.ing.ide.util.Notification.show("Object '" + objectName + "' not found in Project OR (Page '" + page.getName() + "').");
                return;
            }

            String copiedName = repo.copyWebObject(resolved, page.getName());
            if (copiedName != null) {
                com.ing.ide.util.Notification.show("Copied Object '" + copiedName + "' from Page '" + page.getName() + "' to Shared Web Object successfully.");
            } else {
                com.ing.ide.util.Notification.show("Copy failed. Could not copy Object '" + objectName + "' to Shared Web Object (Page '" + page.getName() + "').");
            }
            if (copiedName != null) {
                if (this instanceof com.ing.ide.main.mainui.components.testdesign.or.web.WebObjectTree) {
                    com.ing.ide.main.mainui.components.testdesign.or.web.WebORPanel panel =
                        ((com.ing.ide.main.mainui.components.testdesign.or.web.WebObjectTree) this).getORPanel();
                    panel.getSharedTree().load();
                } else {
                    reload();
                }
            }
        } else if (isMobile) {
            com.ing.datalib.or.mobile.ResolvedMobileObject mresolved =
                repo.resolveMobileObject(
                    new com.ing.datalib.or.mobile.ResolvedMobileObject.PageRef(
                        page.getName(),
                        com.ing.datalib.or.web.WebOR.ORScope.PROJECT
                    ),
                    objectName
                );
            if (mresolved == null) {
                com.ing.ide.util.Notification.show("Object '" + objectName + "' not found in Project Mobile OR (Page '" + page.getName() + "').");
                return;
            }

            String copiedName = repo.copyMobileObject(mresolved, page.getName());
            if (copiedName != null) {
                com.ing.ide.util.Notification.show("Copied Object '" + copiedName + "' from Page '" + page.getName() + "' to Shared Mobile Object successfully.");
            } else {
                com.ing.ide.util.Notification.show("Copy failed. Could not copy Object '" + objectName + "' to Shared Mobile Object (Page '" + page.getName() + "').");
            }
            if (copiedName != null) {
                if (this instanceof com.ing.ide.main.mainui.components.testdesign.or.mobile.MobileObjectTree) {
                    com.ing.ide.main.mainui.components.testdesign.or.mobile.MobileORPanel panel =
                        ((com.ing.ide.main.mainui.components.testdesign.or.mobile.MobileObjectTree) this).getORPanel();
                    panel.getSharedTree().load();
                } else {
                    reload();
                }
            }
        }
    }

    public Boolean navigateToObject(String objectName, String pageName) {
        ORPageInf page = getOR().getPageByName(pageName);
        if (page != null) {
            ObjectGroup group = page.getObjectGroupByName(objectName);
            if (group != null) {
                selectAndSrollTo(group.getTreePath());
                return true;
            }
        }
        return false;
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

    private boolean isSharedScope() {
        ORRootInf root = getOR();
        if (root instanceof com.ing.datalib.or.web.WebOR) {
            return ((com.ing.datalib.or.web.WebOR) root).isShared();
        }
        if (root instanceof com.ing.datalib.or.mobile.MobileOR) {
            return ((com.ing.datalib.or.mobile.MobileOR) root).isShared();
        }
        return false;
    }

    private boolean confirmSharedRename(String entityLabel, String currentName, String newName) {
        if (!isSharedScope()) return true;
        ORRootInf root = getOR();
        java.util.List<String> projects = null;

        if (root instanceof com.ing.datalib.or.web.WebOR) {
            projects = ((com.ing.datalib.or.web.WebOR) root).getProjects();
        } else if (root instanceof com.ing.datalib.or.mobile.MobileOR) {
            projects = ((com.ing.datalib.or.mobile.MobileOR) root).getProjects();
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
            + "Other projects that use Shared " + (root instanceof com.ing.datalib.or.mobile.MobileOR ? "Mobile" : "Web")
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

        if (root instanceof com.ing.datalib.or.web.WebOR) {
            projects = ((com.ing.datalib.or.web.WebOR) root).getProjects();
        } else if (root instanceof com.ing.datalib.or.mobile.MobileOR) {
            projects = ((com.ing.datalib.or.mobile.MobileOR) root).getProjects();
        }

        if (projects != null && !projects.isEmpty()) {
            return "<br/><br/><b>Before proceeding, please verify whether this page/object is being used by the following project(s):</b><br/>"
                    + String.join(", ", projects);
        }
        return "";
    }
}