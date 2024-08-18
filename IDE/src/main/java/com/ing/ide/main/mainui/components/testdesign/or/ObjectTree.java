
package com.ing.ide.main.mainui.components.testdesign.or;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORPageInf;
import com.ing.datalib.or.common.ORRootInf;
import com.ing.datalib.or.common.ObjectGroup;
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

/**
 *
 * 
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
            default:
                throw new UnsupportedOperationException();
        }
    }

    private Boolean checkAndRename() {
        String name = tree.getCellEditor().getCellEditorValue().toString().trim();
        if (Validator.isValidName(name)) {
            ORPageInf page = getSelectedPage();
            if (page != null && !page.getName().equals(name)) {
                if (page.rename(name)) {
                    nodeRenamed(page);
                    return true;
                } else {
                    Notification.show("Page " + name + " Already present");
                    return false;
                }
            }

            ObjectGroup<ORObjectInf> group = getSelectedObjectGroup();
            if (group != null && !group.getName().equals(name)) {
                if (group.rename(name)) {
                    nodeRenamed(group);
                    return true;
                } else {
                    Notification.show("Object " + name + " Already present");
                    return false;
                }
            }

            ORObjectInf obj = getSelectedObject();
            if (obj != null && !obj.getName().equals(name)) {
                if (obj.rename(name)) {
                    nodeRenamed(obj);
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
            int option = JOptionPane.showConfirmDialog(null,
                    "<html><body><p style='width: 200px;'>"
                    + "Are you sure want to delete the following Objects?<br>"
                    + objects
                    + "</p></body></html>",
                    "Delete Object",
                    JOptionPane.YES_NO_OPTION);
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
        ArrayList<String> records = new ArrayList<String>();
        try {
            String testPlanPath = getProject().getLocation() + "/TestPlan";
            String[] scenarioList = getFolderOrFileList(testPlanPath);
            for (int i = 0; i < scenarioList.length; i++) {
                String[] csvList = getFolderOrFileList(testPlanPath + "/" + scenarioList[i]);
                for (int j = 0; j < csvList.length; j++) {
                    String csvFilePath = testPlanPath + "/" + scenarioList[i] + "/" + csvList[j];
                    String[] values = null;
                    List objList = null;
                    try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            values = line.split(",");
                            if (!values[1].equals("Browser") && !values[1].equals("ObjectName") && (values.length == 7)) {//&& (values[1])!= "Browser" )
                                records.add(values[1]);
                                if (!(attributeMap.containsKey(values[6]))) {
                                    attributeMap.put(values[6], new ArrayList<String>());
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
            result.getOutputStream().close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void deleteObjectGroups() {
        List<ObjectGroup> objects = getSelectedObjectGroups();
        if (!objects.isEmpty()) {
            int option = JOptionPane.showConfirmDialog(null,
                    "<html><body><p style='width: 200px;'>"
                    + "Are you sure want to delete the following ObjectGroups?<br>"
                    + objects
                    + "</p></body></html>",
                    "Delete ObjectGroup",
                    JOptionPane.YES_NO_OPTION);
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
            int option = JOptionPane.showConfirmDialog(null,
                    "<html><body><p style='width: 200px;'>"
                    + "Are you sure want to delete the following Pages?<br>"
                    + pages
                    + "</p></body></html>",
                    "Delete Page",
                    JOptionPane.YES_NO_OPTION);
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
        showImpactedTestCases(
                getProject().getImpactedObjectTestCases(pageName, objectName),
                pageName,
                objectName);
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
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                ((DefaultTreeModel) tree.getModel()).nodeChanged(node);
            }
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

}
