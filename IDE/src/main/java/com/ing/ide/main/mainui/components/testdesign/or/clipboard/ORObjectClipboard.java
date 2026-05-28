
package com.ing.ide.main.mainui.components.testdesign.or.clipboard;

import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORPageInf;

/**
 * Holds a single OR object in clipboard along with
 * the action type (copy or cut).
 *
 * This is a UI-level helper class.
 */
public class ORObjectClipboard {

    public enum Type { OBJECT, PAGE }

    private final Object data;     // ORObjectInf or ORPageInf
    private final boolean cut;
    private final Type type;

    public ORObjectClipboard(ORObjectInf object, boolean cut) {
        this.data = object;
        this.cut = cut;
        this.type = Type.OBJECT;
    }

    public ORObjectClipboard(ORPageInf page, boolean cut) {
        this.data = page;
        this.cut = cut;
        this.type = Type.PAGE;
    }

    public boolean isCut() {
        return cut;
    }

    public Type getType() {
        return type;
    }

    public ORObjectInf getObject() {
        return (ORObjectInf) data;
    }

    public ORPageInf getPage() {
        return (ORPageInf) data;
    }
}