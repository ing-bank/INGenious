
package com.ing.ide.main.shr.mobile.ios;

import com.ing.ide.main.shr.mobile.MobileTreeNode;
import com.ing.ide.main.shr.mobile.Rect;

/**
 *
 * 
 */
public class IOSTreeNode extends MobileTreeNode {

    public IOSTreeNode() {
    }

    public IOSTreeNode(String text) {
        setUserObject(text);
    }

    @Override
    public Rect getBounds() {
        Rect rect = new Rect();
        rect.setX(getValue(getAttribute("x")) * IOSUtil.get().getScaleFactor());
        rect.setY(getValue(getAttribute("y")) * IOSUtil.get().getScaleFactor());
        rect.setWidth(getValue(getAttribute("width")) * IOSUtil.get().getScaleFactor());
        rect.setHeight(getValue(getAttribute("height")) * IOSUtil.get().getScaleFactor());
        return rect;
    }

    private double getValue(String val) {
        if (val != null && !val.trim().isEmpty()) {
            return Double.parseDouble(val);
        }
        return 0;
    }

    @Override
    public String getId() {
        return getAttribute("id");
    }

    @Override
    public String getText() {
        return getAttribute("label");
    }

    @Override
    public String getClassName() {
        return "";
    }

    @Override
    public String getValidName() {
        String name = getAttribute("name");
        if (!name.trim().isEmpty()) {
            return name;
        }
        name = getAttribute("label");
        if (!name.trim().isEmpty()) {
            return name;
        }
        name = getAttribute("value");
        if (!name.trim().isEmpty()) {
            return name;
        }
        name = getAttribute("tag");
        if (!name.trim().isEmpty()) {
            return name;
        }
        return "Object";
    }

    @Override
    public String getPageName() {
        return ((IOSTreeNode) getRoot().getChildAt(0)).getAttribute("name");
    }
}
