
package com.ing.ide.main.mainui.components.testdesign.or.clipboard;

import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORPageInf;

/**
 * Simple application-level clipboard manager
 * for OR objects.
 */
public final class ORClipboardManager {

    private static ORObjectClipboard clipboard;

    public static void copy(ORObjectInf object) {
        clipboard = new ORObjectClipboard(object, false);
    }

    public static void cut(ORObjectInf object) {
        clipboard = new ORObjectClipboard(object, true);
    }

    public static void copy(ORPageInf page) {
        clipboard = new ORObjectClipboard(page, false);
    }

    public static void cut(ORPageInf page) {
        clipboard = new ORObjectClipboard(page, true);
    }

    public static boolean hasData() {
        return clipboard != null;
    }

    public static ORObjectClipboard get() {
        return clipboard;
    }

    public static void clear() {
        clipboard = null;
    }
}