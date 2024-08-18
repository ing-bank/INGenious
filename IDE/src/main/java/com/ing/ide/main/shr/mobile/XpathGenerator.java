
package com.ing.ide.main.shr.mobile;

import com.ing.ide.main.shr.mobile.android.AndroidTreeNode;
import com.ing.ide.main.shr.mobile.ios.IOSTreeNode;
import javax.swing.tree.TreeNode;

public class XpathGenerator {

    public static String getFullXpath(MobileTreeNode node) {
        if (node instanceof AndroidTreeNode) {
            if (!((AndroidTreeNode) node).getContentDescName().isEmpty()) {
                return "//*[@content-desc='" + ((AndroidTreeNode) node).getContentDescName() + "']";
            } else {
                AndroidTreeNode andNode = (AndroidTreeNode) node;
                String xpath = getPath(andNode);
                TreeNode parent = andNode.getParent();
                while (parent != null) {
                    if (((AndroidTreeNode) parent).isRoot()) {
                        break;
                    }
                    xpath = getPath((AndroidTreeNode) parent) + "/" + xpath;
                    parent = parent.getParent();
                }
                return "//" + xpath;
            }
        } else if (node instanceof IOSTreeNode) {
            IOSTreeNode iosNode = (IOSTreeNode) node;
            String xpath = getPath(iosNode);
            TreeNode parent = iosNode.getParent();
            while (parent != null) {
                if (((IOSTreeNode) parent).isRoot()) {
                    break;
                }
                xpath = getPath((IOSTreeNode) parent) + "/" + xpath;
                parent = parent.getParent();
            }
            return "//" + xpath;
        }
        return "";
    }

    private static String getPath(AndroidTreeNode node) {
        return getPosition(node);
    }

    private static String getPath(IOSTreeNode node) {
        return getPosition(node);
    }

    private static String getPosition(AndroidTreeNode node) {
        int count = 0;
        TreeNode parent = node.getParent();
        String nodeName = node.getClassName();
        for (int i = 0; i < parent.getChildCount(); i++) {
            AndroidTreeNode currNode = (AndroidTreeNode) parent.getChildAt(i);
            if (currNode.getClassName().equals(nodeName)) {
                count++;
                if (currNode.equals(node)) {
                    return nodeName + "[" + count + "]";
                }
            }
        }
        return nodeName;
    }

    private static String getPosition(IOSTreeNode node) {
        int count = 0;
        TreeNode parent = node.getParent();
        String nodeName = node.getAttribute("tag");
        for (int i = 0; i < parent.getChildCount(); i++) {
            IOSTreeNode currNode = (IOSTreeNode) parent.getChildAt(i);
            if (currNode.getAttribute("tag").equals(nodeName)) {
                count++;
                if (currNode.equals(node)) {
                    return nodeName + "[" + count + "]";
                }
            }
        }
        return nodeName;
    }
}
