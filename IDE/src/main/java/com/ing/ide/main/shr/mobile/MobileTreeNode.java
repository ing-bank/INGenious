
package com.ing.ide.main.shr.mobile;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * 
 */
public abstract class MobileTreeNode extends DefaultMutableTreeNode {

    private final Map<String, String> attributes = new LinkedHashMap<>();

    public MobileTreeNode() {
    }

    public MobileTreeNode(String text) {
        setUserObject(text);
    }

    public Map getAttributes() {
        return attributes;
    }

    public String getAttribute(String value) {
        return attributes.get(value);
    }

    public void setAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public abstract Rect getBounds();

    public boolean findLeafMostNodesAtPoint(int px, int py, MinAreaFindNodeListener listener) {
        boolean foundInChild = false;
        for (int i = 0; i < getChildCount(); i++) {
            MobileTreeNode node = (MobileTreeNode) getChildAt(i);
            foundInChild |= node.findLeafMostNodesAtPoint(px, py, listener);
        }

        // checked all children, if at least one child covers the point, return directly
        if (foundInChild) {
            return true;
        }
        // check self if the node has no children, or no child nodes covers the point
        if (getBounds() != null) {
            Rect rect = getBounds();
            if (rect.getX() <= px
                    && px <= rect.getX() + rect.getWidth()
                    && rect.getY() <= py
                    && py <= rect.getY() + rect.getHeight()) {
                listener.onFoundNode(this);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public abstract String getId();

    public abstract String getText();

    public abstract String getClassName();

    public abstract String getValidName();

    public abstract String getPageName();
}
