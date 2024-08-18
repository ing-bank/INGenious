
package com.ing.ide.main.shr.mobile;

import com.ing.datalib.or.mobile.MobileORPage;
import com.ing.ide.util.Notification;
import java.io.File;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 *
 * 
 */
public abstract class MobileTree {

    public void setTree(JTree tree) {
        this.tree = tree;
    }

    public JTree getTree() {
        return tree;
    }
    private JTree tree;

    public abstract void loadTree(String xml);

    protected String getAttribute(Node node, String attribute) {
        Node attr = node.getAttributes().getNamedItem(attribute);
        return attr == null ? null : attr.getTextContent();
    }

    protected void setAttributes(Node node, MobileTreeNode treeNode) {
        NamedNodeMap nodemap = node.getAttributes();
        int maxcount = 2000;
        int loopcount = nodemap.getLength();
        if(loopcount>maxcount)
            loopcount = maxcount;
        for (int i = 0; i < loopcount; i++) {
            Node attrnode = nodemap.item(i);
            treeNode.setAttribute(attrnode.getNodeName(), attrnode.getTextContent());
        }
    }

    public Boolean saveAsXML(MobileORPage node) {
        if (node != null) {
            String loc = node.getRepLocation();
            File file = new File(loc);
            if (!file.exists()) {
                file.mkdirs();
            }
            loc = loc + File.separator + "dump.xml";
            if (new File(loc).exists()) {
                int option = JOptionPane.showConfirmDialog(null, "Mapping already present.Do you want to overwrite?", "Overwrite", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    saveXML(loc);
                    return true;
                }
            } else {
                saveXML(loc);
                return true;
            }
        } else {
            Notification.show("Please Select a page from MOR to Map");
        }
        return false;
    }

    public abstract void saveXML(String fileName);

    public abstract String getDisplayName(Node node);

    public abstract MobileTreeNode getSelectedNode();

}
