
package com.ing.ide.main.mainui.components.testdesign.or.clipboard;

import com.ing.datalib.or.common.ORObjectInf;

/**
 * Simple application-level clipboard manager
 * for OR objects.
 */
public final class ORClipboardManager {

    private static ORObjectClipboard clipboard;

    private ORClipboardManager() {
        // Prevent instantiation
    }

    /**
     * Copy an OR object.
     */
    public static void copy(ORObjectInf object) {
        clipboard = new ORObjectClipboard(object, false);
    }

    /**
     * Cut an OR object.
     */
    public static void cut(ORObjectInf object) {
        clipboard = new ORObjectClipboard(object, true);
    }

    /**
     * Returns the current clipboard content.
     */
    public static ORObjectClipboard get() {
        return clipboard;
    }

    /**
     * Clears the clipboard.
     */
    public static void clear() {
        clipboard = null;
    }

    /**
     * Checks if clipboard has OR data.
     */
    public static boolean hasData() {
        return clipboard != null;
    }
}