
package com.ing.ide.main.mainui.components.testexecution.testset;

import com.ing.engine.drivers.WebDriverFactory;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.keys.Keystroke;
import com.ing.ide.util.Canvas;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 *
 * 
 */
public class TestSetPopupMenu extends JPopupMenu {

    private final ActionListener actionListener;
    private JMenuItem saveMenuItem;
    private JMenu changeBrowser;

    public TestSetPopupMenu(ActionListener actionListener) {
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

        JMenu resetStatus = new JMenu("Change Status");

        JMenuItem noRun = Utils.createMenuItem("NoRun", actionListener);
        noRun.setActionCommand("Change Status");
        noRun.setIcon(Canvas.EmptyIcon);
        resetStatus.add(noRun);

        JMenuItem passed = Utils.createMenuItem("Passed", actionListener);
        passed.setActionCommand("Change Status");
        resetStatus.add(passed);

        JMenuItem failed = Utils.createMenuItem("Failed", actionListener);
        failed.setActionCommand("Change Status");
        resetStatus.add(failed);

        add(resetStatus);

        changeBrowser = new JMenu("Change Browser");
        add(changeBrowser);
        addSeparator();

        add(Utils.createMenuItem("Cut", "Ctrl+X", Keystroke.CUT, actionListener));
        add(Utils.createMenuItem("Copy", "Ctrl+C", Keystroke.COPY, actionListener));
        add(Utils.createMenuItem("Paste", "Ctrl+V", Keystroke.PASTE, actionListener));

        addSeparator();
        add(Utils.createMenuItem("Go To TestCase", "Alt+Click", null, actionListener));
        addSeparator();
        add(saveMenuItem = Utils.createMenuItem("Save", "Ctrl+S", Keystroke.SAVE, actionListener));
        add(Utils.createMenuItem("Reload", "F5", Keystroke.F5, actionListener));
        saveMenuItem.setIcon(Canvas.EmptyIcon);
    }

    void loadBrowsers(List<String> emulators) {
        changeBrowser.removeAll();
        loadBrowsersToMenu(WebDriverFactory.Browser.getValuesAsList());
        if (!emulators.isEmpty()) {
            changeBrowser.addSeparator();
            loadBrowsersToMenu(emulators);
        }
    }

    private void loadBrowsersToMenu(List<String> browsers) {
        for (String browser : browsers) {
            JMenuItem menuItem = new JMenuItem(browser);
            menuItem.setActionCommand("ChangeBrowser");
            menuItem.addActionListener(actionListener);
            changeBrowser.add(menuItem);
            menuItem.setIcon(Canvas.EmptyIcon);
        }
    }

}
