
package com.ing.ide.main.mainui.components.testdesign.testcase;

import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.keys.Keystroke;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

/**
 *
 * 
 */
public class TestCasePopupMenu extends JPopupMenu {

    private final ActionListener actionListener;
    private JMenuItem saveMenuItem;

    public TestCasePopupMenu(ActionListener actionListener) {
        this.actionListener = actionListener;
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

        add(Utils.createMenuItem("Toggle BreakPoint", "Ctrl+B", Keystroke.BREAKPOINT, actionListener));
        add(Utils.createMenuItem("Toggle Comment", "Ctrl+/", Keystroke.COMMENT, actionListener));

        addSeparator();
        add(Utils.createMenuItem("Cut", "Ctrl+X", Keystroke.CUT, actionListener));
        add(Utils.createMenuItem("Copy", "Ctrl+C", Keystroke.COPY, actionListener));
        add(Utils.createMenuItem("Paste", "Ctrl+V", Keystroke.PASTE, actionListener));
        addSeparator();
        add(Utils.createMenuItem("Create Reusable", actionListener));

        JMenu goToMenu = new JMenu("Go To");
        goToMenu.add(Utils.createMenuItem("Go To Reusable", actionListener));
        goToMenu.add(Utils.createMenuItem("Go To Object", actionListener));
        goToMenu.add(Utils.createMenuItem("Go To TestData", actionListener));
        add(goToMenu);
        add(Utils.createMenuItem("Paramterize", actionListener));
        addSeparator();

        JRadioButtonMenuItem toggleValidation = new JRadioButtonMenuItem("Toggle Validation", true);
        toggleValidation.addActionListener(actionListener);
        add(toggleValidation);
        addSeparator();

        add(saveMenuItem = Utils.createMenuItem("Save", "Ctrl+S", Keystroke.SAVE, actionListener));
        add(Utils.createMenuItem("Reload", "F5", Keystroke.F5, actionListener));
    }

}
