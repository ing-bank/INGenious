
package com.ing.ide.main.mainui.components.testdesign.or.clipboard;

import com.ing.datalib.or.common.ORObjectInf;

/**
 * Holds a single OR object in clipboard along with
 * the action type (copy or cut).
 *
 * This is a UI-level helper class.
 */
public class ORObjectClipboard {

    private final ORObjectInf object;
    private final boolean cut;

    public ORObjectClipboard(ORObjectInf object, boolean cut) {
        this.object = object;
        this.cut = cut;
    }

    /**
     * Returns the OR object that was copied or cut.
     */
    public ORObjectInf getObject() {
        return object;
    }

    /**
     * Returns true if the action was CUT, false if COPY.
     */
    public boolean isCut() {
        return cut;
    }
}