package com.ing.ide.main.utils.table;

import com.ing.datalib.undoredo.UndoRedoModel;
import com.ing.ide.main.utils.keys.ClipboardKeyAdapter;
import com.ing.ide.main.utils.keys.Keystroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JComponent;
import javax.swing.JTable;
import static javax.swing.JTable.AUTO_RESIZE_OFF;
import static javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

public class XTable extends JTable {

    private boolean inLayout;

    private SearchRenderer searchRenderer;

    private EditHeader editHeader;

    public XTable() {
        init();
    }

    public XTable(TableModel tm) {
        super(tm);
        init();
    }

    private void init() {
        try {
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, new File("resources/ui/resources/fonts/ingme_regular.ttf"));//.deriveFont(12f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(customFont);
        } catch (IOException | FontFormatException e) {

            //  e.printStackTrace();
        }
        setFont(new Font("ING Me", Font.BOLD, 11));
        searchRenderer = new SearchRenderer();
        setFillsViewportHeight(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        getTableHeader().setFont(new Font("ING Me", Font.BOLD, 11));
        Color headerBg = UIManager.getColor("TableHeader.background");
        Color headerFg = UIManager.getColor("TableHeader.foreground");
        Color gridClr = UIManager.getColor("Table.gridColor");
        getTableHeader().setBackground(headerBg != null ? headerBg : Color.decode("#F6F8FA"));
        getTableHeader().setForeground(headerFg != null ? headerFg : Color.decode("#24292F"));
        Color borderClr = UIManager.getColor("Component.borderColor");
        getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderClr != null ? borderClr : Color.decode("#D0D7DE")));
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setDefaultRenderer(new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, borderClr != null ? borderClr : Color.decode("#D0D7DE")),
                    BorderFactory.createEmptyBorder(4, 8, 4, 4)));
                Color hBg = UIManager.getColor("TableHeader.background");
                setBackground(hBg != null ? hBg : Color.decode("#F6F8FA"));
                Color hFg = UIManager.getColor("TableHeader.foreground");
                setForeground(hFg != null ? hFg : Color.decode("#24292F"));
                return c;
            }
        });
        setCellSelectionEnabled(true);
        setColumnSelectionAllowed(true);
        setGridColor(gridClr != null ? gridClr : new Color(246, 227, 221));
        setIntercellSpacing(new java.awt.Dimension(0, 1));
        
        // Apply theme-aware background
        Color tableBg = UIManager.getColor("Table.background");
        setBackground(tableBg != null ? tableBg : Color.WHITE);
        Color tableFg = UIManager.getColor("Table.foreground");
        setForeground(tableFg != null ? tableFg : Color.BLACK);
        
        setDefaultEditor(Object.class, new CustomTableCellEditor());
        addKeyListeners();
        


        putClientProperty("terminateEditOnFocusLost", true);
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent fe) {
                searchRenderer.focused = true;
                repaint();
            }

            @Override
            public void focusLost(FocusEvent fe) {
                searchRenderer.focused = false;
                repaint();
            }
        });
        TableCellDrag.install(this);
    }
    
    /**
     * Called when the L&F changes. Refresh theme-aware colors.
     */
    @Override
    public void updateUI() {
        super.updateUI();
        // Refresh grid color
        Color gridClr = UIManager.getColor("Table.gridColor");
        setGridColor(gridClr != null ? gridClr : new Color(246, 227, 221));
        // Refresh background/foreground
        Color tableBg = UIManager.getColor("Table.background");
        setBackground(tableBg != null ? tableBg : Color.WHITE);
        Color tableFg = UIManager.getColor("Table.foreground");
        setForeground(tableFg != null ? tableFg : Color.BLACK);
        // Refresh header
        if (getTableHeader() != null) {
            Color headerBg = UIManager.getColor("TableHeader.background");
            getTableHeader().setBackground(headerBg != null ? headerBg : Color.decode("#F6F8FA"));
            Color headerFg = UIManager.getColor("TableHeader.foreground");
            getTableHeader().setForeground(headerFg != null ? headerFg : Color.decode("#24292F"));
            Color borderClr = UIManager.getColor("Component.borderColor");
            getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, borderClr != null ? borderClr : Color.decode("#D0D7DE")));
        }
    }

    private void addKeyListeners() {
        addKeyListener(new ClipboardKeyAdapter(this));
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.DELETE, "Clear");
        getActionMap().put("Clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                deleteSelectedCells();
            }
        });
        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "none");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "none");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "none");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "none");

        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "cut");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "copy");
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "paste");

        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.INSERT_ROW, "Insert");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.ADD_ROW, "Add");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.ADD_ROWX, "Add");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.REMOVE_ROW, "Delete");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.REMOVE_ROWX, "Delete");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.ADD_COL, "Add Column");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.ADD_COLX, "Add Column");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.REMOVE_COL, "Delete Column");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.REMOVE_COLX, "Delete Column");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.RIGHT, "Move Column Right");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.LEFT, "Move Column Left");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.COPY_ABOVE, "Copy Above");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.REPLICATE_ROW, "Replicate");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.SAVE, "Save");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.F5, "Reload");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.OPEN, "Open");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.FIND, "Search");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.COMMENT, "Comment");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.BREAKPOINT, "BreakPoint");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.UP, "MoveUp");
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.DOWN, "MoveDown");

        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.UNDO, "Undo");
        getActionMap().put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (getModel() instanceof UndoRedoModel) {
                    ((UndoRedoModel) getModel()).getUndoManager().undo();
                }
            }
        });
        getInputMap(JComponent.WHEN_FOCUSED).put(Keystroke.REDO, "Redo");
        getActionMap().put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (getModel() instanceof UndoRedoModel) {
                    ((UndoRedoModel) getModel()).getUndoManager().redo();
                }
            }
        });
        getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "selectAll");
        getActionMap().put("selectAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectAll();
            }
        });
    }

    private void deleteSelectedCells() {
        if (getModel() instanceof UndoRedoModel) {
            ((UndoRedoModel) getModel()).startGroupEdit();
        }
        int[] srow = getSelectedRows();
        int[] scol = getSelectedColumns();
        int lastSrow = srow.length;
        int lastScol = scol.length;
        for (int i = 0; i < lastSrow; i++) {
            for (int j = 0; j < lastScol; j++) {
                if (isCellEditable(srow[i], scol[j])) {
                    setValueAt("", srow[i], scol[j]);
                }
            }
        }
        if (getModel() instanceof UndoRedoModel) {
            ((UndoRedoModel) getModel()).stopGroupEdit();
        }
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        return searchRenderer.setDefRenderer(super.getCellRenderer(row, column));
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return hasExcessWidth();

    }

    @Override
    public int getRowHeight() {
        return 25;
    }

    @Override
    public void doLayout() {
        if (hasExcessWidth()) {
            // fool super
            autoResizeMode = AUTO_RESIZE_SUBSEQUENT_COLUMNS;
        }
        inLayout = true;
        super.doLayout();
        inLayout = false;
        autoResizeMode = AUTO_RESIZE_OFF;
    }

    protected boolean hasExcessWidth() {
        return getPreferredSize().width < getParent().getWidth();
    }

    @Override
    public void columnMarginChanged(ChangeEvent e) {
        if (isEditing()) {
            // JW: darn - cleanup to terminate editing ...
            removeEditor();
        }
        TableColumn resizingColumn = getTableHeader().getResizingColumn();
        // Need to do this here, before the parent's
        // layout manager calls getPreferredSize().
        if (resizingColumn != null && autoResizeMode == AUTO_RESIZE_OFF
                && !inLayout) {
            resizingColumn.setPreferredWidth(resizingColumn.getWidth());
        }
        resizeAndRepaint();
    }

    @Override
    public boolean editCellAt(int i, int i1, EventObject e) {
        Boolean flag = super.editCellAt(i, i1, e);
        if (flag) {
            if (e instanceof KeyEvent) {
                int code = ((KeyEvent) e).getKeyCode();
                if (code < KeyEvent.VK_DELETE
                        && code >= KeyEvent.VK_COMMA) {
                    getEditorComponent().requestFocusInWindow();
                }
            }
        }
        return flag;
    }

    public void setActionFor(String value, Action action) {
        getActionMap().put(value, action);
    }

    public void setKeyStrokeFor(String value, KeyStroke keyStroke) {
        getInputMap(JComponent.WHEN_FOCUSED).put(keyStroke, value);
    }

    public void setColumnRename(Action onColumnRenameAction, Integer... dontEditTheseColumns) {
        editHeader = EditHeader.setEditableHeader(this, onColumnRenameAction, dontEditTheseColumns);
    }

    public void disableColumnRename() {
        if (editHeader != null) {
            editHeader.disableEdit();
        }
    }

    public void searchFor(String text) {
        /**
         * *************Header Search **************
         */
        if (text.startsWith("@")) {
            for (int i = 0; i < getColumnCount(); i++) {
                if (text.length() > 1) {
                    String searchText = text.split("@")[1];
                    if ((getColumnName(i).toUpperCase()).contains(searchText.toUpperCase())) {
                        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
                        setColumnSelectionAllowed(true);
                        headerRenderer.setForeground(Color.BLACK);
                        Color matchBg = UIManager.getColor("ing.searchHighlight");
                        headerRenderer.setBackground(matchBg != null ? matchBg : Color.ORANGE);
                        getTableHeader().getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);

                    } else {
                        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
                        Color hdrBg = UIManager.getColor("TableHeader.background");
                        Color hdrFg = UIManager.getColor("TableHeader.foreground");
                        headerRenderer.setBackground(hdrBg != null ? hdrBg : Color.decode("#f0edf6"));
                        getTableHeader().setForeground(hdrFg != null ? hdrFg : Color.decode("#342245"));
                        getTableHeader().getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);

                    }

                } else {
                    DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
                    Color hdrBg = UIManager.getColor("TableHeader.background");
                    Color hdrFg = UIManager.getColor("TableHeader.foreground");
                    headerRenderer.setBackground(hdrBg != null ? hdrBg : Color.decode("#f0edf6"));
                    getTableHeader().setForeground(hdrFg != null ? hdrFg : Color.decode("#342245"));
                    getTableHeader().getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);

                }
            }
        }

        /**
         * *****************************************
         */
        searchRenderer.searchRowMap.clear();
        Boolean isRegex = false;
        if (text != null && !text.isEmpty()) {
            if (text.startsWith("$")) {
                if (text.length() < 2) {
                    return;
                }
                isRegex = true;
                text = text.substring(1);
                if (!isRegexValid(text)) {
                    return;
                }
            }
            for (int row = 0; row < getRowCount(); row++) {
                for (int column = 0; column < getColumnCount(); column++) {
                    String value = Objects.toString(getValueAt(row, column), "");
                    if (isRegex ? value.matches(text) : value.toLowerCase().contains(text.toLowerCase())) {
                        //if (value.contains(text)){
                        if (!searchRenderer.searchRowMap.containsKey(row)) {
                            searchRenderer.searchRowMap.put(row, new ArrayList<Integer>());
                        }
                        searchRenderer.searchRowMap.get(row).add(column);
                    }
                }
            }
        }
        repaint();
    }

    private Boolean isRegexValid(String regex) {
        try {
            Pattern.compile(regex);
            return true;
        } catch (PatternSyntaxException e) {
            return false;
        }
    }

    public void goToNextSearch() {
        int selectedRow = getSelectedRow();
        int selectedColumn = getSelectedColumn();

        int nextRow = -1;
        int nextColumn = -1;

        if (!searchRenderer.searchRowMap.isEmpty()) {
            List<Integer> searchedRowList = new ArrayList<>(searchRenderer.searchRowMap.keySet());
            Collections.sort(searchedRowList);
            if (searchedRowList.contains(selectedRow)) {
                List<Integer> searchColumns = searchRenderer.searchRowMap.get(selectedRow);
                int index = searchColumns.indexOf(new Integer(selectedColumn));
                if (index + 1 < searchColumns.size()) {
                    nextRow = selectedRow;
                    nextColumn = searchColumns.get(index + 1);
                }
            }
            if (nextColumn == -1) {
                for (Integer searchedRow : searchedRowList) {
                    if (searchedRow > selectedRow) {
                        nextRow = searchedRow;
                        nextColumn = searchRenderer.searchRowMap.get(searchedRow).get(0);
                        break;
                    }
                }
            }
            if (nextColumn != -1) {
                changeSelection(nextRow, nextColumn, false, false);
            }
        }

    }

    public void goToPrevoiusSearch() {
        int selectedRow = getSelectedRow();
        int selectedColumn = getSelectedColumn();

        int nextRow = -1;
        int nextColumn = -1;

        if (!searchRenderer.searchRowMap.isEmpty() && selectedColumn != -1) {
            List<Integer> searchedRowList = new ArrayList<>(searchRenderer.searchRowMap.keySet());
            Collections.sort(searchedRowList, Collections.reverseOrder());
            if (searchedRowList.contains(selectedRow)) {
                List<Integer> searchColumns = searchRenderer.searchRowMap.get(selectedRow);
                int index = searchColumns.indexOf(new Integer(selectedColumn));
                if (index - 1 != -1) {
                    nextRow = selectedRow;
                    nextColumn = searchColumns.get(index - 1);
                }
            }
            if (nextColumn == -1) {
                for (Integer searchedRow : searchedRowList) {
                    if (searchedRow < selectedRow) {
                        nextRow = searchedRow;
                        nextColumn = searchRenderer.searchRowMap.get(searchedRow)
                                .get(searchRenderer.searchRowMap.get(searchedRow).size() - 1);
                        break;
                    }
                }
            }
            if (nextColumn != -1) {
                changeSelection(nextRow, nextColumn, false, false);
            }
        }
    }

    public void cut() {
        XTableUtils.copyToClipboard(this, true);
    }

    public void copy() {
        XTableUtils.copyToClipboard(this, false);
    }

    public void paste() {
        XTableUtils.pasteFromClipboard(this);
    }

    public void selectColumn(int colIndex) {
        if (getRowCount() > 0) {
            changeSelection(0, colIndex, false, false);
            for (int i = 1; i < getRowCount(); i++) {
                changeSelection(i, colIndex, false, true);
            }
        }
    }
    
    public class CustomTableCellEditor extends DefaultCellEditor {

    public CustomTableCellEditor() {
        super(new JTextField());
        JTextField editor = (JTextField) getComponent();
        int menuShortcutKeyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();

        // Remove default Ctrl key bindings
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "none");
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "none");
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "none");
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "none");

        // Add Cmd key bindings
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuShortcutKeyMask), "cut");
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuShortcutKeyMask), "copy");
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuShortcutKeyMask), "paste");
        editor.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuShortcutKeyMask), "selectAll");
        editor.getActionMap().put("selectAll", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                editor.selectAll();
            }
        });
    }
}    

}

class SearchRenderer extends DefaultTableCellRenderer {

    public Map<Integer, List<Integer>> searchRowMap = new LinkedHashMap<>();

    TableCellRenderer defCellRenderer;

    Boolean focused = true;

    public SearchRenderer setDefRenderer(TableCellRenderer defCellRenderer) {
        this.defCellRenderer = defCellRenderer;
        return this;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JComponent comp = (JComponent) defCellRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

        // Add left padding for modern web-table look
        comp.setBorder(javax.swing.BorderFactory.createEmptyBorder(2, 8, 2, 4));

        Boolean rowSelected = false;
        for (int srow : table.getSelectedRows()) {
            if (srow == row) {
                rowSelected = true;
                break;
            }
        }

        setSelectionColor(rowSelected, isSelected, comp, table.getBackground());
        setSearchColor(comp, row, column, isSelected);
        return comp;
    }

    private void setSearchColor(JComponent comp, int row, int column, Boolean cellSelected) {
        if (!cellSelected) {
            if (searchRowMap.get(row) != null && searchRowMap.get(row).indexOf(column) != -1) {
                Color searchBg = UIManager.getColor("ing.searchHighlight");
                comp.setBackground(searchBg != null ? searchBg : Color.decode("#E5D6FF"));
            }
        }
    }

    private void setSelectionColor(Boolean rowSelected, Boolean cellSelected,
            JComponent comp, Color defalutRowBgColor) {
        if (rowSelected) {
            Color selBg = UIManager.getColor("ing.selectionBackground");
            comp.setBackground(selBg != null ? selBg : Color.decode("#D4EDFD"));
        } else {
            comp.setBackground(defalutRowBgColor);
        }
        if (cellSelected) {
            if (focused) {
                Color focusBg = UIManager.getColor("ing.focusedSelectionBackground");
                comp.setBackground(focusBg != null ? focusBg : Color.decode("#89D6FD"));
            } else {
                Color inactiveBg = UIManager.getColor("ing.selectionInactiveBackground");
                comp.setBackground(inactiveBg != null ? inactiveBg : Color.decode("#D4EDFD"));
                Color selFg = UIManager.getColor("ing.selectionForeground");
                comp.setForeground(selFg != null ? selFg : Color.decode("#4D0020"));
            }
        }
    }
    
}



