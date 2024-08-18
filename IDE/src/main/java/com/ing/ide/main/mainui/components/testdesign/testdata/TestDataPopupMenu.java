
package com.ing.ide.main.mainui.components.testdesign.testdata;


import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.util.Canvas;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 *
 * 
 */
public class TestDataPopupMenu extends JPopupMenu {

    private final ActionListener actionListener;
    private JMenuItem saveMenuItem;

    public TestDataPopupMenu(ActionListener tdProxy) {
        this.actionListener = tdProxy;
        init();
    }

    public void setSave(Boolean flag) {
        saveMenuItem.setEnabled(flag);
    }

    private void init() {
        JMenuItem addRowButton = Utils.createMenuItem("Add Row", ""
                + "Ctrl+Plus to add a row at last"
                + "<br>"
                + "Ctrl+I to insert a row before the selected row"
                + "<br>"
                + "Ctrl+R to replicate the row", Keystroke.ADD_ROWP, actionListener);
        add(addRowButton);
        add(Utils.createMenuItem("Delete Rows", "Ctrl+Minus", Keystroke.REMOVE_ROW, actionListener));
        addSeparator();

        add(Utils.createMenuItem("Add Column", "Alt+Plus", Keystroke.ADD_COLP, actionListener));
        add(Utils.createMenuItem("Delete Columns", "Alt+Minus", Keystroke.REMOVE_COL, actionListener));
        addSeparator();

        add(Utils.createMenuItem("Create Selected Column's In All Env", "", null, actionListener));

        add(Utils.createMenuItem("Create Selected Rows's In All Env", "", null, actionListener));

        add(Utils.createMenuItem("Encrypt", "", null, actionListener));
        addSeparator();
        add(Utils.createMenuItem("Go To TestCase", "", null, actionListener));
        addSeparator();

        add(Utils.createMenuItem("Cut", "Ctrl+X", Keystroke.CUT, actionListener));
        add(Utils.createMenuItem("Copy", "Ctrl+C", Keystroke.COPY, actionListener));
        add(Utils.createMenuItem("Paste", "Ctrl+V", Keystroke.PASTE, actionListener));
        addSeparator();

        add(saveMenuItem = Utils.createMenuItem("Save", "Ctrl+S", Keystroke.SAVE, actionListener));
        add(Utils.createMenuItem("Reload", "F5", Keystroke.F5, actionListener));

        saveMenuItem.setIcon(Canvas.EmptyIcon);
    }
}
