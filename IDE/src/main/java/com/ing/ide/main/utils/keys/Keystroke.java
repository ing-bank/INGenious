package com.ing.ide.main.utils.keys;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

/**
 * Central registry of application-wide {@link KeyStroke} constants.
 * <p>
 * The {@code SHORTCUT} modifier resolves to {@code Ctrl} on Windows/Linux
 * and the {@code Command (⌘)} key on Mac, following Swing's cross-platform convention.
 */
public class Keystroke {
    private static final int SHORTCUT = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    private static final boolean IS_MAC = System
        .getProperty("os.name")
        .toLowerCase()
        .contains("mac");

    // ── KeyStroke constants ────────────────────────────────────────────

    public static final KeyStroke DELETE = KeyStroke.getKeyStroke(
        KeyEvent.VK_DELETE,
        0
    ), RENAME = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), CUT = KeyStroke.getKeyStroke(
        KeyEvent.VK_X,
        SHORTCUT
    ), COPY = KeyStroke.getKeyStroke(KeyEvent.VK_C, SHORTCUT), PASTE = KeyStroke.getKeyStroke(
        KeyEvent.VK_V,
        SHORTCUT
    ), ENCRYPT = KeyStroke.getKeyStroke(
        KeyEvent.VK_E,
        SHORTCUT
    ), REPLICATE_ROW = KeyStroke.getKeyStroke(
        KeyEvent.VK_R,
        SHORTCUT
    ), INSERT_ROW = KeyStroke.getKeyStroke(
        KeyEvent.VK_I,
        SHORTCUT
    ), ADD_ROW = KeyStroke.getKeyStroke(
        KeyEvent.VK_ADD,
        SHORTCUT
    ), ADD_ROWP = KeyStroke.getKeyStroke(
        KeyEvent.VK_PLUS,
        SHORTCUT
    ), ADD_ROWX = KeyStroke.getKeyStroke(
        KeyEvent.VK_EQUALS,
        SHORTCUT
    ), REMOVE_ROW = KeyStroke.getKeyStroke(
        KeyEvent.VK_MINUS,
        SHORTCUT
    ), REMOVE_ROWX = KeyStroke.getKeyStroke(
        KeyEvent.VK_SUBTRACT,
        SHORTCUT
    ), ADD_COL = KeyStroke.getKeyStroke(
        KeyEvent.VK_ADD,
        KeyEvent.ALT_MASK
    ), ADD_COLP = KeyStroke.getKeyStroke(
        KeyEvent.VK_PLUS,
        KeyEvent.ALT_MASK
    ), ADD_COLX = KeyStroke.getKeyStroke(
        KeyEvent.VK_EQUALS,
        KeyEvent.ALT_MASK | KeyEvent.SHIFT_DOWN_MASK
    ), REMOVE_COL = KeyStroke.getKeyStroke(
        KeyEvent.VK_MINUS,
        KeyEvent.ALT_MASK
    ), REMOVE_COLX = KeyStroke.getKeyStroke(
        KeyEvent.VK_SUBTRACT,
        KeyEvent.ALT_MASK
    ), UP = KeyStroke.getKeyStroke(KeyEvent.VK_UP, SHORTCUT), DOWN = KeyStroke.getKeyStroke(
        KeyEvent.VK_DOWN,
        SHORTCUT
    ), LEFT = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, SHORTCUT), RIGHT = KeyStroke.getKeyStroke(
        KeyEvent.VK_RIGHT,
        SHORTCUT
    ), BACK = KeyStroke.getKeyStroke(
        KeyEvent.VK_DOWN,
        KeyEvent.ALT_MASK
    ), NEXT = KeyStroke.getKeyStroke(
        KeyEvent.VK_UP,
        KeyEvent.ALT_MASK
    ), NEW = KeyStroke.getKeyStroke(KeyEvent.VK_N, SHORTCUT), F5 = KeyStroke.getKeyStroke(
        KeyEvent.VK_F5,
        0
    ), CTRLF5 = KeyStroke.getKeyStroke(KeyEvent.VK_F5, SHORTCUT), F6 = KeyStroke.getKeyStroke(
        KeyEvent.VK_F6,
        0
    ), CTRLF6 = KeyStroke.getKeyStroke(KeyEvent.VK_F6, SHORTCUT), CLOSE = KeyStroke.getKeyStroke(
        KeyEvent.VK_C,
        SHORTCUT | KeyEvent.ALT_MASK
    ), SAVE = KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT), OPEN = KeyStroke.getKeyStroke(
        KeyEvent.VK_O,
        SHORTCUT | KeyEvent.ALT_MASK
    ), COPY_ABOVE = KeyStroke.getKeyStroke(KeyEvent.VK_D, SHORTCUT), FIND = KeyStroke.getKeyStroke(
        KeyEvent.VK_F,
        SHORTCUT
    ), BREAKPOINT = KeyStroke.getKeyStroke(
        KeyEvent.VK_B,
        SHORTCUT
    ), COMMENT = KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, SHORTCUT), UNDO = KeyStroke.getKeyStroke(
        KeyEvent.VK_Z,
        SHORTCUT
    ), REDO = KeyStroke.getKeyStroke(KeyEvent.VK_Y, SHORTCUT), ALTENTER = KeyStroke.getKeyStroke(
        KeyEvent.VK_ENTER,
        KeyEvent.ALT_MASK
    ), REMOVE_OBJECT = KeyStroke.getKeyStroke(KeyEvent.VK_O, SHORTCUT), /**
     * Ctrl+Alt+R / ⌘+⌥+R — Start Playwright recording. Registered globally via
     * {@link
     * com.ing.ide.main.mainui.components.testdesign.testcase.TestCaseComponent#registerGlobalShortcuts()}.
     */RECORD = KeyStroke.getKeyStroke(KeyEvent.VK_R, SHORTCUT | KeyEvent.ALT_MASK);

    // ── Formatting ────────────────────────────────────────────────────

    /**
     * Returns the user-facing label for the shortcut modifier key, matching the
     * platform convention: "⌘" on Mac, "Ctrl" on Windows/Linux.
     */
    public static String shortcutKeyLabel() {
        return IS_MAC ? "⌘" : "Ctrl";
    }

    /**
     * Returns the user-facing label for the Alt/Option modifier key.
     */
    public static String altKeyLabel() {
        return IS_MAC ? "⌥" : "Alt";
    }

    /**
     * Returns the user-facing label for the Shift modifier key.
     */
    public static String shiftKeyLabel() {
        return IS_MAC ? "⇧" : "Shift";
    }

    /**
     * Formats a {@link KeyStroke} into a human-readable shortcut string suitable for
     * tooltips or labels, using platform-appropriate modifier key names.
     * <p>
     * Examples: {@code ⌘S} (Mac), {@code Ctrl+S} (Windows), {@code ⌘⇧S} (Mac Shift),
     * {@code Ctrl+Alt+R} (Windows), {@code F5} (any platform).
     *
     * @param ks the keystroke to format, or {@code null}
     * @return formatted shortcut string, or an empty string if {@code ks} is null
     */
    public static String format(KeyStroke ks) {
        if (ks == null) {
            return "";
        }
        int mods = ks.getModifiers();
        int code = ks.getKeyCode();
        StringBuilder sb = new StringBuilder();

        // KeyStroke.getKeyStroke() internally converts legacy masks to extended
        // modifiers, so BOTH ALT_MASK (0x8) and ALT_DOWN_MASK (0x200) are often set
        // simultaneously. Use group-level checks to avoid double-counting.
        boolean hasCtrl =
            (mods & InputEvent.CTRL_DOWN_MASK) != 0 || (mods & InputEvent.CTRL_MASK) != 0;
        boolean hasAlt =
            (mods & InputEvent.ALT_DOWN_MASK) != 0 || (mods & InputEvent.ALT_MASK) != 0;
        boolean hasShift =
            (mods & InputEvent.SHIFT_DOWN_MASK) != 0 || (mods & InputEvent.SHIFT_MASK) != 0;
        boolean hasMeta =
            (mods & InputEvent.META_DOWN_MASK) != 0 || (mods & InputEvent.META_MASK) != 0;

        // Platform shortcut key (Ctrl or ⌘) — consume Ctrl/Meta group
        if ((mods & SHORTCUT) != 0) {
            sb.append(shortcutKeyLabel()).append("+");
            hasCtrl = false;
            hasMeta = false;
        } else if (hasCtrl) {
            sb.append("Ctrl+");
            hasCtrl = false;
        } else if (hasMeta) {
            sb.append("⌘+");
            hasMeta = false;
        }

        // Alt / Option
        if (hasAlt) {
            sb.append(altKeyLabel()).append("+");
            hasAlt = false;
        }

        // Shift
        if (hasShift) {
            sb.append(shiftKeyLabel()).append("+");
            hasShift = false;
        }

        // Key name
        String keyName;
        if (code == KeyEvent.VK_ADD || code == KeyEvent.VK_PLUS) {
            keyName = "Plus";
        } else if (code == KeyEvent.VK_SUBTRACT) {
            keyName = "Minus";
        } else if (code == KeyEvent.VK_EQUALS) {
            keyName = "=";
        } else if (code == KeyEvent.VK_SLASH) {
            keyName = "/";
        } else if (code == KeyEvent.VK_MINUS) {
            keyName = "-";
        } else {
            keyName = KeyEvent.getKeyText(code);
        }
        sb.append(keyName);
        return sb.toString();
    }
}
