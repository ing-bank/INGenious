
package com.ing.ide.main.shr.mobile.ios;

import com.ing.datalib.component.utils.XMLOperation;
import com.ing.ide.main.shr.mobile.MobileTree;
import com.ing.ide.main.utils.fileoperation.FileOptions;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultTreeModel;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * 
 */
public class IOSTree extends MobileTree {

    private static IOSTree andTree;
    private String xmlContent;

    public static IOSTree get() {
        if (andTree == null) {
            andTree = new IOSTree();
        }
        return andTree;
    }

    private static String sanitizePathTraversal(String filepath) throws IOException {
       // Path p = Paths.get(filepath);
        return new File(filepath).getCanonicalPath();
    }
    
    @Override
    public void loadTree (String xml)  {
        xmlContent = xml;
        Document doc = null;
        try {
            if (new File(sanitizePathTraversal(xml)).exists()) {
                doc = XMLOperation.initTreeOp(xml);
            } else {
                doc = XMLOperation.initTreeOpFromString(xml);
            }
        } catch (IOException ex) {
            Logger.getLogger(IOSTree.class.getName()).log(Level.SEVERE, null, ex);
        }
        Element rootElement = doc.getDocumentElement();
        IOSTreeNode rootNode = new IOSTreeNode(rootElement.getTagName());
        rootNode.setAttribute("tag", rootElement.getTagName());
        loadNodes(rootElement, rootNode);
        DefaultTreeModel newModel = new DefaultTreeModel(rootNode);
        getTree().setModel(newModel);
    }

    private IOSTreeNode loadNodes(Element parent, IOSTreeNode parentNode) {
        NodeList nodeList = parent.getChildNodes();
        int maxcount = 2000;
        int loopcount = nodeList.getLength();
        if(loopcount>maxcount)
            loopcount = maxcount;
        for (int i = 0; i < loopcount; i++) {
            Node node = nodeList.item(i);
            if (Node.ELEMENT_NODE == node.getNodeType()) {
                IOSTreeNode treeNode = new IOSTreeNode(getDisplayName(node));
                setAttributes(node, treeNode);
                treeNode.setAttribute("tag", node.getNodeName());
                loadNodes((Element) node, treeNode);
                parentNode.add(treeNode);
            }
        }
        return null;
    }

    @Override
    public String getDisplayName(Node node) {
        String type = node.getNodeName();
        String name = getAttribute(node, "name");
        name = name != null ? " " + name : "";
        return "[" + type + "]" + name;
    }

    @Override
    public IOSTreeNode getSelectedNode() {
        Object selected = getTree().getSelectionPath().getLastPathComponent();
        if (selected instanceof IOSTreeNode) {
            return (IOSTreeNode) selected;
        }
        return null;
    }

    @Override
    public void saveXML(String fileName) {
        if (xmlContent != null) {
            if (new File(xmlContent).exists()) {
                FileOptions.copyFileAs(xmlContent, fileName);
            } else {
                FileOptions.createFile(fileName, xmlContent);
            }
        }
    }

}
