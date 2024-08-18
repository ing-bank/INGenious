
package com.ing.ide.main.mainui.components.testexecution.testset;

import com.ing.ide.main.utils.SearchBox;
import com.ing.ide.main.utils.Utils;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

/**
 *
 * 
 */
public class TestSetToolBar extends JToolBar {

    private final ActionListener actionListener;

    private JButton runButton;
    private JButton saveButton;
    private SearchBox searchField;

    public TestSetToolBar(ActionListener tdProxy) {
        this.actionListener = tdProxy;
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
        searchField.setText("");
    }

    private void init() {
        searchField = new SearchBox(actionListener);
        add(searchField);
        addSeparator();
        add(createToggleButton());
        add(runButton = Utils.createButton("Run", "run", "F6", actionListener));
        addSeparator();
        JButton addRowButton = Utils.createButton("Add Row", "add", ""
                + "Ctrl+Plus to add a row at last"
                + "<br>"
                + "Ctrl+I to insert a row before the selected row"
                + "<br>"
                + "Ctrl+R to replicate the row", actionListener);
        add(addRowButton);
        JButton removeRow = Utils.createButton("Delete Rows", "remove", "Ctrl+Minus", actionListener);
        add(removeRow);
        addSeparator();
        add(Utils.createButton("Move Rows Up", "up", "Ctrl+Up", actionListener));
        add(Utils.createButton("Move Rows Down", "down", "Ctrl+Down", actionListener));
        addSeparator();
        add(saveButton = Utils.createButton("Save", "save", "Ctrl+S", actionListener));
        add(Utils.createButton("Reload", "reload", "F5", actionListener));
        add(Utils.createButton("Open with System Editor", "openwithsystemeditor", "Ctrl+Alt+O", actionListener));
        saveButton.setEnabled(false);
    }

    private JToggleButton createToggleButton() {
        JToggleButton console = new JToggleButton(Utils.getIconByResourceName("/ui/resources/toolbar/console"));
        console.setActionCommand("Console");
        console.setToolTipText("Show/Hide Console Box");
        console.addActionListener(actionListener);
        return console;
    }

    void startMode() {
        runButton.setActionCommand("Run");
        runButton.setIcon(Utils.getIconByResourceName("/ui/resources/run"));
    }

    void stopMode() {
        runButton.setActionCommand("StopRun");
        runButton.setIcon(Utils.getIconByResourceName("/ui/resources/stop"));
    }

    void setPlaceHolderText(String text) {
        searchField.setPlaceHolder(text, null);
    }
}
