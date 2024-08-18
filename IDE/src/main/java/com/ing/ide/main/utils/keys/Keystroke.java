
package com.ing.ide.main.utils.keys;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.KeyStroke;

public class Keystroke {

    private static final int SHORTCUT = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    public static final KeyStroke DELETE = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0),
            RENAME = KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0),
            CUT = KeyStroke.getKeyStroke(KeyEvent.VK_X, SHORTCUT),
            COPY = KeyStroke.getKeyStroke(KeyEvent.VK_C, SHORTCUT),
            PASTE = KeyStroke.getKeyStroke(KeyEvent.VK_V, SHORTCUT),
            ENCRYPT = KeyStroke.getKeyStroke(KeyEvent.VK_E, SHORTCUT),
            REPLICATE_ROW = KeyStroke.getKeyStroke(KeyEvent.VK_R, SHORTCUT),
            INSERT_ROW = KeyStroke.getKeyStroke(KeyEvent.VK_I, SHORTCUT),
            ADD_ROW = KeyStroke.getKeyStroke(KeyEvent.VK_ADD, SHORTCUT),
            ADD_ROWP = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, SHORTCUT),
            ADD_ROWX = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, SHORTCUT),
            REMOVE_ROW = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, SHORTCUT),
            REMOVE_ROWX = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, SHORTCUT),
            ADD_COL = KeyStroke.getKeyStroke(KeyEvent.VK_ADD, KeyEvent.ALT_MASK),
            ADD_COLP = KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, KeyEvent.ALT_MASK),
            ADD_COLX = KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.ALT_MASK | KeyEvent.SHIFT_DOWN_MASK),
            REMOVE_COL = KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.ALT_MASK),
            REMOVE_COLX = KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, KeyEvent.ALT_MASK),
            UP = KeyStroke.getKeyStroke(KeyEvent.VK_UP, SHORTCUT),
            DOWN = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, SHORTCUT),
            LEFT = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, SHORTCUT),
            RIGHT = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, SHORTCUT),
            BACK = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, KeyEvent.ALT_MASK),
            NEXT = KeyStroke.getKeyStroke(KeyEvent.VK_UP, KeyEvent.ALT_MASK),
            NEW = KeyStroke.getKeyStroke(KeyEvent.VK_N, SHORTCUT),
            F5 = KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0),
            CTRLF5 = KeyStroke.getKeyStroke(KeyEvent.VK_F5, SHORTCUT),
            F6 = KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0),
            CTRLF6 = KeyStroke.getKeyStroke(KeyEvent.VK_F6, SHORTCUT),
            CLOSE = KeyStroke.getKeyStroke(KeyEvent.VK_C, SHORTCUT | KeyEvent.ALT_MASK),
            SAVE = KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT),
            OPEN = KeyStroke.getKeyStroke(KeyEvent.VK_O, SHORTCUT | KeyEvent.ALT_MASK),
            COPY_ABOVE = KeyStroke.getKeyStroke(KeyEvent.VK_D, SHORTCUT),
            FIND = KeyStroke.getKeyStroke(KeyEvent.VK_F, SHORTCUT),
            BREAKPOINT = KeyStroke.getKeyStroke(KeyEvent.VK_B, SHORTCUT),
            COMMENT = KeyStroke.getKeyStroke(KeyEvent.VK_SLASH, SHORTCUT),
            UNDO = KeyStroke.getKeyStroke(KeyEvent.VK_Z, SHORTCUT),
            REDO = KeyStroke.getKeyStroke(KeyEvent.VK_Y, SHORTCUT),
            ALTENTER = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.ALT_MASK),
            REMOVE_OBJECT = KeyStroke.getKeyStroke(KeyEvent.VK_O, SHORTCUT);

}
