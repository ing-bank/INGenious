package com.ing.ide.main.mainui.components.dbworkbench.util;

import java.awt.Color;
import javax.swing.UIManager;

/**
 * Theme-aware colors for the Database Workbench. Mirrors the API Workbench's
 * {@code APITesterColors} so the two workbenches feel identical.
 */
public final class DBWorkbenchColors {
    /** Indigo accent that distinguishes the DB Workbench from the API Workbench cyan. */
    public static final Color ACCENT = Color.decode("#3F51B5");
    public static final Color ACCENT_HOVER = Color.decode("#303F9F");
    public static final Color OK_GREEN = Color.decode("#2E7D32");
    public static final Color FAIL_RED = Color.decode("#C62828");
    public static final Color UNTESTED_GRAY = Color.decode("#9E9E9E");

    private DBWorkbenchColors() {}

    public static Color panelBackground() {
        Color c = UIManager.getColor("Panel.background");
        return c != null ? c : Color.WHITE;
    }

    public static Color textPrimary() {
        Color c = UIManager.getColor("Label.foreground");
        return c != null ? c : Color.BLACK;
    }

    public static Color textSecondary() {
        Color c = UIManager.getColor("Label.disabledForeground");
        if (c == null) c = UIManager.getColor("Label.foreground");
        return c != null ? c : Color.GRAY;
    }

    public static Color tableBackground() {
        Color c = UIManager.getColor("Table.background");
        return c != null ? c : Color.WHITE;
    }

    public static Color border() {
        Color c = UIManager.getColor("Component.borderColor");
        if (c == null) c = UIManager.getColor("Separator.foreground");
        return c != null ? c : Color.LIGHT_GRAY;
    }
}
