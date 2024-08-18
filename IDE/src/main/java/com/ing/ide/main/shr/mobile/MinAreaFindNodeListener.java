
package com.ing.ide.main.shr.mobile;

/**
 *
 * 
 */
public class MinAreaFindNodeListener {

    public MobileTreeNode mNode = null;

    public void onFoundNode(MobileTreeNode node) {
        if (mNode == null) {
            mNode = node;
        } else {
            if ((node.getBounds().getHeight() * node.getBounds().getWidth()) < (mNode.getBounds().getHeight() * mNode.getBounds().getWidth())) {
                mNode = node;
            }
        }
    }
}
