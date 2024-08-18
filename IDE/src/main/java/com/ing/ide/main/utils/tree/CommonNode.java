
package com.ing.ide.main.utils.tree;

import java.util.Collections;
import java.util.Comparator;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * 
 */
public class CommonNode extends DefaultMutableTreeNode {

    public void sort() {
        Collections.sort(children, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                return o1.toString().compareToIgnoreCase(o2.toString());
            }

        });
    }
}
