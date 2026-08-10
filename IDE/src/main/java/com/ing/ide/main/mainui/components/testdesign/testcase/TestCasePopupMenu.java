package com.ing.ide.main.mainui.components.testdesign.testcase;

import com.ing.datalib.component.TestStep;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.keys.Keystroke;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 *
 *
 */
public class TestCasePopupMenu extends JPopupMenu {
    private final ActionListener actionListener;
    private JMenuItem saveMenuItem;
    private JPopupMenu.Separator assertionSeparator;
    private JRadioButtonMenuItem softAssertionItem;
    private JRadioButtonMenuItem hardAssertionItem;

    public TestCasePopupMenu(ActionListener actionListener) {
        this.actionListener = actionListener;
        init();
    }

    public void setSave(Boolean flag) {
        saveMenuItem.setEnabled(flag);
    }

    private void init() {
        JMenuItem addRowButton = Utils.createMenuItem(
            "Add Row",
            "" +
            "Ctrl+Plus to add a row at last" +
            "<br>" +
            "Ctrl+I to insert a row before the selected row" +
            "<br>" +
            "Ctrl+R to replicate the row",
            Keystroke.ADD_ROWP,
            actionListener
        );
        add(addRowButton);
        add(
            Utils.createMenuItem("Delete Rows", "Ctrl+Minus", Keystroke.REMOVE_ROW, actionListener)
        );
        addSeparator();

        add(
            Utils.createMenuItem(
                "Toggle BreakPoint",
                "Ctrl+B",
                Keystroke.BREAKPOINT,
                actionListener
            )
        );
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
        add(Utils.createMenuItem("Parameterize", actionListener));
        addSeparator();

        JRadioButtonMenuItem toggleValidation = new JRadioButtonMenuItem("Toggle Validation", true);
        toggleValidation.addActionListener(actionListener);
        add(toggleValidation);
        addSeparator();

        add(saveMenuItem = Utils.createMenuItem("Save", "Ctrl+S", Keystroke.SAVE, actionListener));
        add(Utils.createMenuItem("Reload", "F5", Keystroke.F5, actionListener));

        initAssertionItems();
    }

    /**
     * Builds the per-step assertion type options ("Soft Assertion" /
     * "Hard Assertion"). These are only shown for assertion steps (action
     * starting with {@code assert}) and reflect the selected step's current
     * setting. All assertions are soft by default.
     */
    private void initAssertionItems() {
        assertionSeparator = new JPopupMenu.Separator();
        add(assertionSeparator);

        ButtonGroup assertionGroup = new ButtonGroup();

        softAssertionItem = new JRadioButtonMenuItem("Soft Assertion", true);
        softAssertionItem.setActionCommand("Soft Assertion");
        softAssertionItem.setToolTipText(
            "On failure, continue executing the remaining steps in this iteration (default)."
        );
        softAssertionItem.addActionListener(actionListener);

        hardAssertionItem = new JRadioButtonMenuItem("Hard Assertion", false);
        hardAssertionItem.setActionCommand("Hard Assertion");
        hardAssertionItem.setToolTipText(
            "On failure, fail and stop the current iteration, then continue with the next iteration."
        );
        hardAssertionItem.addActionListener(actionListener);

        assertionGroup.add(softAssertionItem);
        assertionGroup.add(hardAssertionItem);
        add(softAssertionItem);
        add(hardAssertionItem);

        addPopupMenuListener(
            new PopupMenuListener() {

                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                    updateAssertionItems();
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {}
            }
        );
    }

    private void updateAssertionItems() {
        TestStep step = null;
        if (actionListener instanceof TestCaseComponent) {
            step = ((TestCaseComponent) actionListener).getSelectedStep();
        }
        boolean isAssert = step != null && step.isAssertStep();
        assertionSeparator.setVisible(isAssert);
        softAssertionItem.setVisible(isAssert);
        hardAssertionItem.setVisible(isAssert);
        if (isAssert) {
            if (step.isHardAssertion()) {
                hardAssertionItem.setSelected(true);
            } else {
                softAssertionItem.setSelected(true);
            }
        }
    }
}
