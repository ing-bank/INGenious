
package com.ing.datalib.or.common;

import java.util.Collections;
import java.util.Comparator;
import javax.swing.tree.TreePath;

public class ORUtils {

    public static TreePath getPath(ORObjectInf object) {
        if (object.getParent().getChildCount() == 1) {
            return getPath(object.getPage()).pathByAddingChild(object);
        } else {
            return getPath(object.getParent()).pathByAddingChild(object);
        }
    }

    public static TreePath getPath(ObjectGroup objectGroup) {
        return getPath(objectGroup.getParent()).pathByAddingChild(objectGroup);
    }

    public static TreePath getPath(ORPageInf page) {
        return new TreePath(page.getRoot()).pathByAddingChild(page);
    }

    public static void sort(ORRootInf root) {
        Collections.sort(root.getPages(), new Comparator<ORPageInf>() {
            @Override
            public int compare(ORPageInf t, ORPageInf t1) {
                return t.getName().compareToIgnoreCase(t1.getName());
            }
        });
    }

    public static void sort(ORPageInf page) {
        Collections.sort(page.getObjectGroups(), new Comparator<ObjectGroup>() {
            @Override
            public int compare(ObjectGroup t, ObjectGroup t1) {
                return t.getName().compareToIgnoreCase(t1.getName());
            }
        });
    }

    public static void sort(ObjectGroup group) {
        Collections.sort(group.getObjects(), new Comparator<ORObjectInf>() {
            @Override
            public int compare(ORObjectInf t, ORObjectInf t1) {
                return t.getName().compareToIgnoreCase(t1.getName());
            }
        });
    }

}
