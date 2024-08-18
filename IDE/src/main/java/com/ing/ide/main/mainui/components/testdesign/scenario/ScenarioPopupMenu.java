
package com.ing.ide.main.mainui.components.testdesign.scenario;

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
public class ScenarioPopupMenu extends JPopupMenu {

    private final ActionListener actionListener;
    private JMenuItem saveMenuItem;

    public ScenarioPopupMenu(ActionListener actionListener) {
        this.actionListener = actionListener;
        init();
    }

    public void setSave(Boolean flag) {
        saveMenuItem.setEnabled(flag);
    }

    private void init() {
        JMenuItem addRowButton = Utils.createMenuItem("Add Component", ""
                + "Ctrl+Plus to add a component at last"
                + "<br>"
                + "Ctrl+I to insert a component before the selected row", Keystroke.ADD_ROWP, actionListener);
        add(addRowButton);
        JMenuItem removeComp = Utils.createMenuItem("Remove Component",
                "Ctrl+Minus to Remove selected components",
                Keystroke.REMOVE_ROW, actionListener);
        add(removeComp);
        addSeparator();
        add(Utils.createMenuItem("Create Reusable", actionListener));
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
