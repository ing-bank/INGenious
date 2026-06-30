package com.ing.ide.main.mainui.components.testdesign.or.clipboard;

import com.ing.datalib.or.common.ORObjectInf;
import com.ing.datalib.or.common.ORPageInf;
import java.util.List;

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

    public static void copy(List<ORObjectInf> objects) {
        clipboard = new ORObjectClipboard(objects, false);
    }

    public static void cut(List<ORObjectInf> objects) {
        clipboard = new ORObjectClipboard(objects, true);
    }

    public static void copy(ORPageInf page) {
        clipboard = new ORObjectClipboard(page, false);
    }

    public static void cut(ORPageInf page) {
        clipboard = new ORObjectClipboard(page, true);
    }

    public static void copyPages(List<ORPageInf> pages) {
        clipboard = ORObjectClipboard.forPages(pages, false);
    }

    public static void cutPages(List<ORPageInf> pages) {
        clipboard = ORObjectClipboard.forPages(pages, true);
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
