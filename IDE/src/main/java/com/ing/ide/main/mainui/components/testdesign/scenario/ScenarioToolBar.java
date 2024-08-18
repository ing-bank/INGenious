
package com.ing.ide.main.mainui.components.testdesign.scenario;

import com.ing.ide.main.utils.SearchBox;
import com.ing.ide.main.utils.Utils;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JToolBar;

/**
 *
 * 
 */
public class ScenarioToolBar extends JToolBar {

    private final ActionListener actionListener;

    private JButton saveButton;
    private SearchBox searchField;

    public ScenarioToolBar(ActionListener scProxy) {
        this.actionListener = scProxy;
        setFloatable(false);
        setBorder(javax.swing.BorderFactory.createEtchedBorder());
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));
        init();
    }

    public void setSave(Boolean flag) {
        saveButton.setEnabled(flag);
    }

    public void focusSearch() {
        searchField.focus();
    }

    private void init() {
        searchField = new SearchBox(actionListener);
        add(searchField);
        addSeparator();
        add(Utils.createButton("Create Reusable", "reusable", null, actionListener));

        JButton addCompButton = Utils.createButton("Add Component", "add", ""
                + "Ctrl+Plus to add a Component at last"
                + "<br>"
                + "Ctrl+I to insert a Component before the selected column", actionListener);
        add(addCompButton);

        JButton removeCompButton = Utils.createButton("Remove Component", "remove", ""
                + "Ctrl+Minus to remove Components", actionListener);
        add(removeCompButton);
        addSeparator();
        add(saveButton = Utils.createButton("Save", "save", "Ctrl+S", actionListener));
        add(Utils.createButton("Reload", "reload", "F5", actionListener));
        saveButton.setEnabled(false);
    }

    void setPlaceHolderText(String text) {
        searchField.setPlaceHolder(text, null);
    }

    void changeSave(Boolean flag) {
        saveButton.setEnabled(flag);
    }
}
