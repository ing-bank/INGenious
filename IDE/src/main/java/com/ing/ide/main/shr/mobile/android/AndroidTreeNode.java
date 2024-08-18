
package com.ing.ide.main.shr.mobile.android;

import com.ing.ide.main.shr.mobile.MobileTreeNode;
import com.ing.ide.main.shr.mobile.Rect;

/**
 *
 * 
 */
public class AndroidTreeNode extends MobileTreeNode {

    public AndroidTreeNode() {
    }

    public AndroidTreeNode(String text) {
        setUserObject(text);
    }

    @Override
    public Rect getBounds() {
        return Rect.fromString(getAttribute("bounds"));
    }

    @Override
    public String getClassName() {
        return getAttribute("class");
    }

    public String getIndex() {
        return getAttribute("index");
    }

    @Override
    public String getId() {
        return getAttribute("resource-id");
    }

    @Override
    public String getText() {
        return getAttribute("text");
    }

    @Override
    public String getPageName() {
        return getPackageName();
    }

    public String getPackageName() {
        return getAttribute("package");
    }

    public String getContentDescName() {
        return getAttribute("content-desc");
    }

    @Override
    public String getValidName() {
        String name = getText();
        if (name != null && !name.isEmpty()) {
            return name;
        }
        name = getAttribute("content-desc");
        if (name != null && !name.isEmpty()) {
            return name;
        }
        name = getId();
        if (name != null && !name.isEmpty()) {
            return name.replaceFirst("(\\S)+:id/", "");
        }
        name = getClassName();
        return name.replaceFirst("(android.(widget|view).)?", "") + " " + getAttribute("index");
    }

}
