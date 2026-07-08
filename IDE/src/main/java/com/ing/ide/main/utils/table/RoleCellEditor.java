package com.ing.ide.main.utils.table;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractCellEditor;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellEditor;
import javax.swing.text.JTextComponent;

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
        "ALERT",
        "ALERTDIALOG",
        "APPLICATION",
        "ARTICLE",
        "BANNER",
        "BLOCKQUOTE",
        "BUTTON",
        "CAPTION",
        "CELL",
        "CHECKBOX",
        "CODE",
        "COLUMNHEADER",
        "COMBOBOX",
        "COMPLEMENTARY",
        "CONTENTINFO",
        "DEFINITION",
        "DELETION",
        "DIALOG",
        "DIRECTORY",
        "DOCUMENT",
        "EMPHASIS",
        "FEED",
        "FIGURE",
        "FORM",
        "GENERIC",
        "GRID",
        "GRIDCELL",
        "GROUP",
        "HEADING",
        "IMG",
        "INSERTION",
        "LINK",
        "LIST",
        "LISTBOX",
        "LISTITEM",
        "LOG",
        "MAIN",
        "MARQUEE",
        "MATH",
        "METER",
        "MENU",
        "MENUBAR",
        "MENUITEM",
        "MENUITEMCHECKBOX",
        "MENUITEMRADIO",
        "NAVIGATION",
        "NONE",
        "NOTE",
        "OPTION",
        "PARAGRAPH",
        "PRESENTATION",
        "PROGRESSBAR",
        "RADIO",
        "RADIOGROUP",
        "REGION",
        "ROW",
        "ROWGROUP",
        "ROWHEADER",
        "SCROLLBAR",
        "SEARCH",
        "SEARCHBOX",
        "SEPARATOR",
        "SLIDER",
        "SPINBUTTON",
        "STATUS",
        "STRONG",
        "SUBSCRIPT",
        "SUPERSCRIPT",
        "SWITCH",
        "TAB",
        "TABLE",
        "TABLIST",
        "TABPANEL",
        "TERM",
        "TEXTBOX",
        "TIME",
        "TIMER",
        "TOOLBAR",
        "TOOLTIP",
        "TREE",
        "TREEGRID",
        "TREEITEM"
    };

    private final JPanel editorPanel;
    private final JComboBox<String> roleComboBox;
    private final JTextField nameField;
    private final List<String> allRoles;
    private boolean suppressRoleFilter;

    /**
     * Creates and initializes the custom role cell editor.
     */
    public RoleCellEditor() {
        editorPanel = new JPanel(new BorderLayout(2, 0));

        // Role dropdown
        roleComboBox = new JComboBox<>(ARIA_ROLES);
        roleComboBox.setPreferredSize(new Dimension(80, 22));
        roleComboBox.setEditable(true);

        allRoles = new ArrayList<>();
        for (String role : ARIA_ROLES) {
            allRoles.add(role);
        }

        JTextComponent roleEditor = (JTextComponent) roleComboBox.getEditor().getEditorComponent();
        roleEditor
            .getDocument()
            .addDocumentListener(
                new DocumentListener() {

                    @Override
                    public void insertUpdate(DocumentEvent e) {
                        filterRoles(roleEditor.getText());
                    }

                    @Override
                    public void removeUpdate(DocumentEvent e) {
                        filterRoles(roleEditor.getText());
                    }

                    @Override
                    public void changedUpdate(DocumentEvent e) {
                        filterRoles(roleEditor.getText());
                    }
                }
            );

        roleEditor.addFocusListener(
            new FocusAdapter() {

                @Override
                public void focusGained(FocusEvent e) {
                    showAllRolesPopup();
                }
            }
        );

        roleEditor.addKeyListener(
            new KeyAdapter() {

                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                        moveRoleSelection(1);
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_UP) {
                        moveRoleSelection(-1);
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        SwingUtilities.invokeLater(() -> commitRoleSelectionFromKeyboard());
                        e.consume();
                    }
                }
            }
        );

        // Name text field with placeholder hint
        nameField = new JTextField();
        nameField.setToolTipText("Element name (optional)");

        editorPanel.add(roleComboBox, BorderLayout.WEST);
        editorPanel.add(nameField, BorderLayout.CENTER);

        // Stop editing on Enter key
        nameField.addKeyListener(
            new KeyAdapter() {

                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        stopCellEditing();
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        cancelCellEditing();
                    }
                }
            }
        );

        // Focus the name field after role selection
        roleComboBox.addActionListener(
            new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (suppressRoleFilter) {
                        return;
                    }
                    // Ignore edit events fired while typing in the combo editor.
                    if ("comboBoxEdited".equals(e.getActionCommand())) {
                        return;
                    }
                    nameField.requestFocusInWindow();
                }
            }
        );
    }

    /**
     * Returns the edited cell value in the formatted role/name representation.
     *
     * @return formatted cell value
     */
    @Override
    public Object getCellEditorValue() {
        String role = getNormalizedSelectedRole();
        String name = nameField.getText().trim();

        if (role == null || role.isEmpty()) {
            return name; // Just the name if no role
        } else if (name.isEmpty()) {
            return role; // Just the role if no name
        } else {
            return role + ";" + name; // Combined format
        }
    }

    /**
     * Prepares and returns the editor component for the specified table cell.
     *
     * @param table the target table
     * @param value the current cell value
     * @param isSelected whether the cell is selected
     * @param row the row index
     * @param column the column index
     * @return the editor component
     */
    @Override
    public Component getTableCellEditorComponent(
        JTable table,
        Object value,
        boolean isSelected,
        int row,
        int column
    ) {
        String strValue = value != null ? value.toString() : "";
        suppressRoleFilter = true;
        resetRoleFilter();

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
        suppressRoleFilter = false;

        // Apply theme colors for dark mode compatibility
        Color fgColor = UIManager.getColor("TextField.foreground");
        Color bgColor = UIManager.getColor("TextField.background");
        Color caretColor = UIManager.getColor("TextField.caretForeground");

        // Apply colors to nameField (JTextField)
        if (fgColor != null) {
            nameField.setForeground(fgColor);
        }
        if (bgColor != null) {
            nameField.setBackground(bgColor);
        }
        if (caretColor != null) {
            nameField.setCaretColor(caretColor);
        }

        // Apply colors to roleComboBox
        Color comboFg = UIManager.getColor("ComboBox.foreground");
        Color comboBg = UIManager.getColor("ComboBox.background");
        if (comboFg != null) {
            roleComboBox.setForeground(comboFg);
        }
        if (comboBg != null) {
            roleComboBox.setBackground(comboBg);
        }

        return editorPanel;
    }

    /**
     * Retrieves the currently selected role and normalizes its value.
     *
     * @return normalized role name, or an empty string if none is selected
     */
    private String getNormalizedSelectedRole() {
        Object selected = roleComboBox.getEditor().getItem();
        String roleText = selected != null ? selected.toString().trim() : "";
        if (roleText.isEmpty()) {
            Object comboSelected = roleComboBox.getSelectedItem();
            String selectedRole = comboSelected != null ? comboSelected.toString().trim() : "";
            for (String role : ARIA_ROLES) {
                if (role.equalsIgnoreCase(selectedRole)) {
                    return role;
                }
            }
            return "";
        }

        for (String role : ARIA_ROLES) {
            if (role.equalsIgnoreCase(roleText)) {
                return role;
            }
        }

        // If editor text is partial (e.g., "com"), use combo selection when it is valid.
        Object comboSelected = roleComboBox.getSelectedItem();
        String selectedRole = comboSelected != null ? comboSelected.toString().trim() : "";
        for (String role : ARIA_ROLES) {
            if (role.equalsIgnoreCase(selectedRole)) {
                return role;
            }
        }

        return "";
    }

    /**
     * Restores the role dropdown to display all available roles.
     */
    private void resetRoleFilter() {
        updateRoleModel(allRoles);
    }

    /**
     * Filters the roles based on the typed text.
     *
     * @param typedText the text typed by the user
     */
    private void filterRoles(String typedText) {
        if (suppressRoleFilter) {
            return;
        }

        String safeTypedText = typedText == null ? "" : typedText;
        String query = safeTypedText.trim().toUpperCase();
        List<String> filtered = new ArrayList<>();

        for (String role : allRoles) {
            if (query.isEmpty() || role.toUpperCase().startsWith(query)) {
                filtered.add(role);
            }
        }

        List<String> filteredRoles = new ArrayList<>(filtered);

        SwingUtilities.invokeLater(
            () -> {
                suppressRoleFilter = true;
                updateRoleModel(filteredRoles);
                roleComboBox.getEditor().setItem(safeTypedText);
                suppressRoleFilter = false;

                if (roleComboBox.isDisplayable() && !filteredRoles.isEmpty()) {
                    roleComboBox.showPopup();
                } else {
                    roleComboBox.hidePopup();
                }
            }
        );
    }

    /**
     * Updates the role model with the specified list of roles.
     *
     * @param roles the list of roles to display
     */
    private void updateRoleModel(List<String> roles) {
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (String role : roles) {
            model.addElement(role);
        }
        roleComboBox.setModel(model);
    }

    /**
     * Shows the popup with all available roles.
     */
    private void showAllRolesPopup() {
        SwingUtilities.invokeLater(
            () -> {
                String currentText = "";
                Object editorItem = roleComboBox.getEditor().getItem();
                if (editorItem != null) {
                    currentText = editorItem.toString();
                }

                suppressRoleFilter = true;
                resetRoleFilter();
                roleComboBox.getEditor().setItem(currentText);
                suppressRoleFilter = false;

                if (roleComboBox.isDisplayable()) {
                    roleComboBox.showPopup();
                }
            }
        );
    }

    /**
     * Moves combo-box selection up/down while editing from keyboard.
     *
     * @param delta +1 for next, -1 for previous item
     */
    private void moveRoleSelection(int delta) {
        int size = roleComboBox.getItemCount();
        if (size == 0) {
            return;
        }

        int selected = roleComboBox.getSelectedIndex();
        int nextIndex;
        if (selected < 0) {
            nextIndex = delta > 0 ? 0 : size - 1;
        } else {
            nextIndex = Math.max(0, Math.min(size - 1, selected + delta));
        }

        suppressRoleFilter = true;
        roleComboBox.setSelectedIndex(nextIndex);
        suppressRoleFilter = false;

        if (roleComboBox.isDisplayable()) {
            roleComboBox.showPopup();
        }
    }

    /**
     * Commits role selection from keyboard. If nothing is selected, picks first option.
     */
    private void commitRoleSelectionFromKeyboard() {
        if (roleComboBox.getItemCount() == 0) {
            return;
        }

        String typedText = "";
        Object editorItem = roleComboBox.getEditor().getItem();
        if (editorItem != null) {
            typedText = editorItem.toString().trim();
        }

        String roleToSelect = null;

        // Exact typed role wins.
        roleToSelect = findExactRole(typedText);

        // Otherwise choose the first prefix match (e.g., "com" -> "COMBOBOX").
        if (roleToSelect == null && !typedText.isEmpty()) {
            roleToSelect = findFirstRoleStartsWith(typedText);
        }

        // Fall back to current selection or first item.
        int selectedIndex = roleComboBox.getSelectedIndex();
        if (roleToSelect == null && selectedIndex >= 0) {
            Object selectedItem = roleComboBox.getItemAt(selectedIndex);
            roleToSelect = selectedItem != null ? selectedItem.toString() : null;
        }
        if (roleToSelect == null && roleComboBox.getItemCount() > 0) {
            Object firstItem = roleComboBox.getItemAt(0);
            roleToSelect = firstItem != null ? firstItem.toString() : "";
        }

        suppressRoleFilter = true;
        roleComboBox.setSelectedItem(roleToSelect != null ? roleToSelect : "");
        roleComboBox.getEditor().setItem(roleToSelect != null ? roleToSelect : "");
        suppressRoleFilter = false;
        roleComboBox.hidePopup();
        nameField.requestFocusInWindow();
    }

    /**
     * Finds a role by exact case-insensitive match.
     */
    private String findExactRole(String typedText) {
        if (typedText == null || typedText.isEmpty()) {
            return null;
        }

        for (String role : ARIA_ROLES) {
            if (role.equalsIgnoreCase(typedText)) {
                return role;
            }
        }

        return null;
    }

    /**
     * Finds the first role that starts with the provided text (case-insensitive).
     */
    private String findFirstRoleStartsWith(String typedText) {
        if (typedText == null || typedText.isEmpty()) {
            return null;
        }

        String query = typedText.toUpperCase();
        for (String role : ARIA_ROLES) {
            if (role.toUpperCase().startsWith(query)) {
                return role;
            }
        }

        return null;
    }
}
