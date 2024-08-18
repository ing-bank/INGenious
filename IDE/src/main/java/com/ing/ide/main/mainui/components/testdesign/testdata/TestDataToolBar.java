
package com.ing.ide.main.mainui.components.testdesign.testdata;

import com.ing.ide.main.utils.SearchBox;
import com.ing.ide.main.utils.Utils;
import com.ing.ide.main.utils.keys.Keystroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

/**
 *
 * 
 */
public class TestDataToolBar extends JToolBar {

    private final ActionListener actionListener;

    private JMenuItem addColumn;
    private JMenuItem removeColumn;
    private JMenuItem removeRow;
    private JButton makeAsGlobalData;
    private JButton saveButton;
    private SearchBox searchField;

    public TestDataToolBar(ActionListener tdProxy) {
        this.actionListener = tdProxy;
        setFloatable(false);
        setBorder(javax.swing.BorderFactory.createEtchedBorder());
        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS));
        init();
    }

    public void switchOptionsForGlobalData(Boolean flag) {
        makeAsGlobalData.setEnabled(flag);
    }

    public void setSave(Boolean flag) {
        saveButton.setEnabled(flag);
    }

    public void focusSearch() {
        searchField.focus();
        searchField.setText("");
    }

    public void setSearchText(String tdName, String envName) {
        String scText = tdName;
        if (scText.length() > 20) {
            scText = scText.substring(0, 20) + "...";
        }
        String tcText = envName;
        if (tcText.length() > 20) {
            tcText = tcText.substring(0, 20) + "...";
        }
        String text
                = scText
                + " ["
                + tcText
                + " ]";
//        String toolTip
//                = tdName
//                + " ["
//                + envName
//                + " ]";
        searchField.setPlaceHolder(text, null);
    }

    private void init() {
        searchField = new SearchBox(actionListener);
        add(searchField);
        addSeparator();
        JMenuItem addRowButton = Utils.createMenuItem("Add Row", ""
                + "Ctrl+Plus to add a row at last"
                + "<br>"
                + "Ctrl+I to insert a row before the selected row"
                + "<br>"
                + "Ctrl+R to replicate the row", Keystroke.ADD_ROWP, actionListener);
        addColumn = Utils.createMenuItem("Add Column", "Alt+Plus", Keystroke.ADD_COLP, actionListener);

        JDropDownButton addSplitButton = new JDropDownButton("Add Row");
        addSplitButton.setToolTipText("Add Rows/Columns");
        addSplitButton.setIcon(Utils.getIconByResourceName("/ui/resources/toolbar/add"));
        addSplitButton.addMenu(addRowButton);
        addSplitButton.addMenu(addColumn);

        add(addSplitButton);

        removeRow = Utils.createMenuItem("Delete Rows", "Ctrl+Minus", Keystroke.REMOVE_ROW, actionListener);

        removeColumn = Utils.createMenuItem("Delete Columns", "Alt+Minus", Keystroke.REMOVE_COL, actionListener);

        JDropDownButton removeSplitButton = new JDropDownButton("Delete Rows");
        removeSplitButton.setToolTipText("Remove Rows/Columns");
        removeSplitButton.setIcon(Utils.getIconByResourceName("/ui/resources/toolbar/remove"));
        removeSplitButton.addMenu(removeRow);
        removeSplitButton.addMenu(removeColumn);

        add(removeSplitButton);

        addSeparator();
        makeAsGlobalData = Utils.createButton("Global Data", "global data", null, actionListener);
        makeAsGlobalData.setToolTipText("Make selected data to refer value from Global Data");
        add(makeAsGlobalData);
        add(Utils.createButton("Move Rows Up", "up", "Ctrl+Up", actionListener));
        add(Utils.createButton("Move Rows Down", "down", "Ctrl+Down", actionListener));
        add(Utils.createLRButton("Move Column Left", "moveleft", actionListener));
        add(Utils.createLRButton("Move Column Right", "moveright",  actionListener));
        addSeparator();
        add(saveButton = Utils.createButton("Save", "save", "Ctrl+S", actionListener));
        add(Utils.createButton("Reload", "reload", "F5", actionListener));
        add(Utils.createButton("Open with System Editor", "openwithsystemeditor", "Ctrl+Alt+O", actionListener));
        saveButton.setEnabled(false);
    }

}

class JDropDownButton extends JButton implements ActionListener {

    private final JPopupMenu dropDownMenu = new JPopupMenu();

    public JDropDownButton(String text) {
        addActionListener(this);
    }

    public void addMenu(JMenuItem menuItem) {
        dropDownMenu.add(menuItem);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        dropDownMenu.show(this, 0, this.getHeight());
    }

}
