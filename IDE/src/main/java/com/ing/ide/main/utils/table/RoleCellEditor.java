package com.ing.ide.main.utils.table;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

/**
 * Custom cell editor for the Role attribute in Web Object Repository.
 * Provides a dropdown for AriaRole selection and a text field for the name.
 * The value is stored as "ROLE;Name" format.
 */
public class RoleCellEditor extends AbstractCellEditor implements TableCellEditor {

    /**
     * Playwright AriaRole enum values
     */
    public static final String[] ARIA_ROLES = {
        "", // Empty option for no selection
        "ALERT", "ALERTDIALOG", "APPLICATION", "ARTICLE", "BANNER", "BLOCKQUOTE", 
        "BUTTON", "CAPTION", "CELL", "CHECKBOX", "CODE", "COLUMNHEADER", "COMBOBOX", 
        "COMPLEMENTARY", "CONTENTINFO", "DEFINITION", "DELETION", "DIALOG", "DIRECTORY", 
        "DOCUMENT", "EMPHASIS", "FEED", "FIGURE", "FORM", "GENERIC", "GRID", "GRIDCELL", 
        "GROUP", "HEADING", "IMG", "INSERTION", "LINK", "LIST", "LISTBOX", "LISTITEM", 
        "LOG", "MAIN", "MARQUEE", "MATH", "METER", "MENU", "MENUBAR", "MENUITEM", 
        "MENUITEMCHECKBOX", "MENUITEMRADIO", "NAVIGATION", "NONE", "NOTE", "OPTION", 
        "PARAGRAPH", "PRESENTATION", "PROGRESSBAR", "RADIO", "RADIOGROUP", "REGION", 
        "ROW", "ROWGROUP", "ROWHEADER", "SCROLLBAR", "SEARCH", "SEARCHBOX", "SEPARATOR", 
        "SLIDER", "SPINBUTTON", "STATUS", "STRONG", "SUBSCRIPT", "SUPERSCRIPT", "SWITCH", 
        "TAB", "TABLE", "TABLIST", "TABPANEL", "TERM", "TEXTBOX", "TIME", "TIMER", 
        "TOOLBAR", "TOOLTIP", "TREE", "TREEGRID", "TREEITEM"
    };

    private final JPanel editorPanel;
    private final JComboBox<String> roleComboBox;
    private final JTextField nameField;

    public RoleCellEditor() {
        editorPanel = new JPanel(new BorderLayout(2, 0));
        
        // Role dropdown
        roleComboBox = new JComboBox<>(ARIA_ROLES);
        roleComboBox.setPreferredSize(new Dimension(120, 22));
        roleComboBox.setEditable(false);
        
        // Name text field with placeholder hint
        nameField = new JTextField();
        nameField.setToolTipText("Element name (optional)");
        
        editorPanel.add(roleComboBox, BorderLayout.WEST);
        editorPanel.add(nameField, BorderLayout.CENTER);
        
        // Stop editing on Enter key
        nameField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    stopCellEditing();
                } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cancelCellEditing();
                }
            }
        });
        
        // Focus the name field after role selection
        roleComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                nameField.requestFocusInWindow();
            }
        });
    }

    @Override
    public Object getCellEditorValue() {
        String role = (String) roleComboBox.getSelectedItem();
        String name = nameField.getText().trim();
        
        if (role == null || role.isEmpty()) {
            return name; // Just the name if no role
        } else if (name.isEmpty()) {
            return role; // Just the role if no name
        } else {
            return role + ";" + name; // Combined format
        }
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value,
            boolean isSelected, int row, int column) {
        
        String strValue = value != null ? value.toString() : "";
        
        if (strValue.contains(";")) {
            // Parse existing "ROLE;Name" value
            String[] parts = strValue.split(";", 2);
            roleComboBox.setSelectedItem(parts[0].toUpperCase());
            nameField.setText(parts.length > 1 ? parts[1] : "");
        } else {
            // Check if it's a valid role
            boolean isRole = false;
            for (String role : ARIA_ROLES) {
                if (role.equalsIgnoreCase(strValue)) {
                    roleComboBox.setSelectedItem(role);
                    isRole = true;
                    break;
                }
            }
            if (!isRole) {
                roleComboBox.setSelectedItem("");
                nameField.setText(strValue);
            } else {
                nameField.setText("");
            }
        }
        
        return editorPanel;
    }
}
