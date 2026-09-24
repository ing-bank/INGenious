package com.ing.ide.main.mainui.components.dbworkbench.result;

import com.ing.datalib.dbworkbench.DBValidation;
import com.ing.ide.main.mainui.components.dbworkbench.DBWorkbenchUI;
import com.ing.ide.main.mainui.components.dbworkbench.util.JdbcExecutor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

/**
 * Bottom pane of the Database Workbench: the result grid, a messages area, and a
 * validations table. Right-clicking a cell or column header builds a
 * {@link DBValidation}, which later feeds the Automation step-builder.
 */
public class ResultGridPanel extends JPanel {
    /** How SQL NULL is shown so it is never confused with an empty string. */
    public static final String NULL_DISPLAY = "[NULL]";

    private final DBWorkbenchUI parent;
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTable grid = new JTable();
    private final JTextArea messages = new JTextArea();
    private final JTable validationsTable = new JTable();
    private final DefaultTableModel validationsModel;
    private final JLabel statusLabel = new JLabel(" ");
    private final JButton editToggle = new JButton("Enable Grid Editing");

    private final List<DBValidation> validations = new ArrayList<>();

    /** The result currently in the grid; needed to write edits back. */
    private JdbcExecutor.QueryResult currentResult;
    private boolean editingEnabled = false;

    public ResultGridPanel(DBWorkbenchUI parent) {
        this.parent = parent;
        setLayout(new BorderLayout());

        grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        grid.setCellSelectionEnabled(true);
        grid.setColumnSelectionAllowed(true);
        grid.setRowSelectionAllowed(true);
        // Multiple interval selection so arbitrary cell blocks, whole rows and
        // whole columns can be selected and copied.
        grid.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        grid
            .getColumnModel()
            .getSelectionModel()
            .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        grid.setAutoCreateRowSorter(true);
        grid.setDefaultRenderer(Object.class, new NullAwareRenderer());
        installGridContextMenu();
        installCopyShortcut();

        messages.setEditable(false);
        messages.setLineWrap(true);
        messages.setWrapStyleWord(true);
        messages.setBorder(new EmptyBorder(8, 8, 8, 8));

        validationsModel =
            new DefaultTableModel(
                new Object[] { "Column", "Row", "Operator", "Expected / Target" },
                0
            );
        validationsTable.setModel(validationsModel);
        validationsTable
            .getSelectionModel()
            .setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        installValidationsContextMenu();

        tabs.addTab("Grid", new JScrollPane(grid));
        tabs.addTab("Messages", new JScrollPane(messages));
        tabs.addTab("Validations", new JScrollPane(validationsTable));

        add(buildGridToolbar(), BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        statusLabel.setBorder(new EmptyBorder(4, 8, 4, 8));
        add(statusLabel, BorderLayout.SOUTH);
    }

    private JPanel buildGridToolbar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        JButton exportBtn = new JButton("Export CSV");
        exportBtn.addActionListener(e -> exportCsv());
        JButton copyBtn = new JButton("Copy Grid");
        copyBtn.addActionListener(e -> copyGridAsCsv());
        JButton copySelBtn = new JButton("Copy Selection");
        copySelBtn.setToolTipText("Copy the selected cells, rows or columns");
        copySelBtn.addActionListener(e -> copySelection());
        editToggle.setToolTipText(
            "Edit cells in the grid and write each change back to the source table"
        );
        editToggle.addActionListener(e -> toggleEditing());
        editToggle.setEnabled(false);
        bar.add(exportBtn);
        bar.add(copyBtn);
        bar.add(copySelBtn);
        bar.add(editToggle);
        return bar;
    }

    /** Renders SQL NULL distinctly from an empty string. */
    private static class NullAwareRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        ) {
            Component comp = super.getTableCellRendererComponent(
                table,
                value,
                isSelected,
                hasFocus,
                row,
                column
            );
            if (value == null) {
                setText(NULL_DISPLAY);
                setFont(getFont().deriveFont(Font.ITALIC));
                if (!isSelected) {
                    setForeground(Color.GRAY);
                }
            } else {
                setFont(getFont().deriveFont(Font.PLAIN));
            }
            return comp;
        }
    }

    /**
     * Shows the outcome of a whole script: the last result set goes to the grid
     * and every statement is reported in the Messages tab.
     *
     * @param alias the connection the script ran on
     * @param outcomes one entry per executed statement
     */
    public void showOutcomes(String alias, List<JdbcExecutor.StatementOutcome> outcomes) {
        StringBuilder log = new StringBuilder();
        JdbcExecutor.QueryResult lastQuery = null;
        int statement = 0;
        for (JdbcExecutor.StatementOutcome outcome : outcomes) {
            statement++;
            log.append(statement).append(". ").append(firstLine(outcome.sql)).append('\n');
            if (outcome.queryResult != null) {
                lastQuery = outcome.queryResult;
                log
                    .append("   ")
                    .append(outcome.queryResult.rows.size())
                    .append(" row(s) in ")
                    .append(outcome.queryResult.elapsedMillis)
                    .append(" ms")
                    .append(outcome.queryResult.truncated ? " (truncated to max rows)" : "")
                    .append("\n\n");
            } else if (outcome.dmlResult != null) {
                int affected = outcome.dmlResult.affectedRows;
                log
                    .append("   ")
                    .append(affected < 0 ? "statement executed" : affected + " row(s) affected")
                    .append(" in ")
                    .append(outcome.dmlResult.elapsedMillis)
                    .append(" ms\n\n");
            }
        }
        messages.setForeground(Color.DARK_GRAY);
        messages.setText(log.toString().trim());

        if (lastQuery != null) {
            showResult(lastQuery);
        } else {
            setGridResult(null);
            tabs.setSelectedIndex(1);
        }
        statusLabel.setText(outcomes.size() + " statement(s) executed on '" + alias + "'.");
    }

    private static String firstLine(String sql) {
        String oneLine = sql.replaceAll("\\s+", " ").trim();
        return oneLine.length() <= 120 ? oneLine : oneLine.substring(0, 117) + "...";
    }

    /** Renders a SELECT result set and shows the Grid tab. */
    public void showResult(JdbcExecutor.QueryResult result) {
        setGridResult(result);
        String status = result.rows.size() + " row(s) in " + result.elapsedMillis + " ms";
        if (result.truncated) {
            status += " (truncated to max rows)";
        }
        statusLabel.setText(status);
        messages.setForeground(Color.DARK_GRAY);
        if (messages.getText().isEmpty()) {
            messages.setText(status);
        }
        tabs.setSelectedIndex(0);
    }

    private void setGridResult(JdbcExecutor.QueryResult result) {
        currentResult = result;
        editingEnabled = false;
        editToggle.setText("Enable Grid Editing");
        editToggle.setEnabled(result != null && result.isUpdatable());
        if (result == null) {
            grid.setModel(new DefaultTableModel());
            return;
        }
        DefaultTableModel model = new DefaultTableModel() {

            @Override
            public boolean isCellEditable(int row, int column) {
                return editingEnabled;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return Object.class;
            }
        };
        for (String col : result.columns) {
            model.addColumn(col);
        }
        for (List<Object> row : result.rows) {
            model.addRow(row.toArray());
        }
        grid.setModel(model);
        model.addTableModelListener(
            e -> {
                if (e.getType() == javax.swing.event.TableModelEvent.UPDATE && editingEnabled) {
                    pushCellEdit(e.getFirstRow(), e.getColumn());
                }
            }
        );
    }

    private void toggleEditing() {
        if (currentResult == null || !currentResult.isUpdatable()) {
            showError(
                "Grid editing needs a simple SELECT from a single table that includes its " +
                "primary key columns."
            );
            return;
        }
        editingEnabled = !editingEnabled;
        editToggle.setText(editingEnabled ? "Disable Grid Editing" : "Enable Grid Editing");
        showMessage(
            editingEnabled
                ? "Grid editing enabled on " +
                currentResult.qualifiedTable() +
                ". Each change is sent as an UPDATE; use Commit to make it permanent."
                : "Grid editing disabled."
        );
    }

    /** Writes one edited cell back to the source table. */
    private void pushCellEdit(int modelRow, int modelColumn) {
        if (currentResult == null || modelColumn < 0 || modelRow < 0) return;
        String column = currentResult.columnNames.get(modelColumn);
        Object newValue = grid.getModel().getValueAt(modelRow, modelColumn);
        if (NULL_DISPLAY.equals(newValue)) {
            newValue = null;
        }
        Map<String, Object> keyValues = new LinkedHashMap<>();
        for (String key : currentResult.primaryKey) {
            int index = currentResult.columnNames.indexOf(key);
            if (index < 0) {
                showError("Primary key column '" + key + "' is not in the result set.");
                return;
            }
            keyValues.put(key, currentResult.rows.get(modelRow).get(index));
        }
        try {
            int affected = parent.applyGridEdit(currentResult, column, newValue, keyValues);
            currentResult.rows.get(modelRow).set(modelColumn, newValue);
            showMessage(
                affected +
                " row(s) updated in " +
                currentResult.qualifiedTable() +
                (
                    parent.isCurrentConnectionTransactional()
                        ? ". Click Commit to make it permanent."
                        : "."
                )
            );
        } catch (Exception ex) {
            // Put the old value back so the grid keeps matching the database.
            Object previous = currentResult.rows.get(modelRow).get(modelColumn);
            grid.getModel().setValueAt(previous, modelRow, modelColumn);
            showError(
                "Update failed: " + (ex.getMessage() == null ? ex.toString() : ex.getMessage())
            );
        }
    }

    /** Reports the row count of a DML statement and shows the Messages tab. */
    public void showDml(JdbcExecutor.DmlResult result) {
        setGridResult(null);
        String msg = result.affectedRows + " row(s) affected in " + result.elapsedMillis + " ms";
        statusLabel.setText(msg);
        messages.setForeground(Color.DARK_GRAY);
        messages.setText(msg);
        tabs.setSelectedIndex(1);
    }

    /** Shows an error in the Messages tab without a modal. */
    public void showError(String message) {
        statusLabel.setText("Error");
        messages.setForeground(new Color(0xC6, 0x28, 0x28));
        messages.setText(message);
        tabs.setSelectedIndex(1);
    }

    /** Shows an informational message (e.g. commit/rollback) in the Messages tab. */
    public void showMessage(String message) {
        statusLabel.setText(message);
        messages.setForeground(Color.DARK_GRAY);
        messages.setText(message);
        tabs.setSelectedIndex(1);
    }

    public List<DBValidation> getValidations() {
        return validations;
    }

    public void clearValidations() {
        validations.clear();
        validationsModel.setRowCount(0);
    }

    private String gridToCsv() {
        StringBuilder sb = new StringBuilder();
        int cols = grid.getColumnCount();
        for (int c = 0; c < cols; c++) {
            if (c > 0) sb.append(',');
            sb.append(csvEscape(grid.getColumnName(c)));
        }
        sb.append('\n');
        for (int r = 0; r < grid.getRowCount(); r++) {
            for (int c = 0; c < cols; c++) {
                if (c > 0) sb.append(',');
                Object v = grid.getValueAt(r, c);
                sb.append(csvEscape(v == null ? "" : v.toString()));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String csvEscape(String s) {
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    private void copyGridAsCsv() {
        if (grid.getColumnCount() == 0) return;
        Toolkit
            .getDefaultToolkit()
            .getSystemClipboard()
            .setContents(new StringSelection(gridToCsv()), null);
        showMessage("Grid copied to clipboard as CSV.");
    }

    private void installCopyShortcut() {
        KeyStroke copy = KeyStroke.getKeyStroke(
            KeyEvent.VK_C,
            Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
        );
        grid.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(copy, "copy-selection");
        grid
            .getActionMap()
            .put(
                "copy-selection",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        copySelection();
                    }
                }
            );
    }

    /** Copies only the selected cells, rows or columns, preserving their layout. */
    private void copySelection() {
        int[] rows = grid.getSelectedRows();
        int[] cols = grid.getSelectedColumns();
        if (rows.length == 0 || cols.length == 0) {
            showMessage("Select one or more cells, rows or columns to copy.");
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (int c = 0; c < cols.length; c++) {
            if (c > 0) sb.append('\t');
            sb.append(grid.getColumnName(cols[c]));
        }
        sb.append('\n');
        for (int r : rows) {
            for (int c = 0; c < cols.length; c++) {
                if (c > 0) sb.append('\t');
                Object v = grid.getValueAt(r, cols[c]);
                sb.append(v == null ? NULL_DISPLAY : v.toString());
            }
            sb.append('\n');
        }
        Toolkit
            .getDefaultToolkit()
            .getSystemClipboard()
            .setContents(new StringSelection(sb.toString()), null);
        showMessage(
            "Copied " + rows.length + " row(s) × " + cols.length + " column(s) to clipboard."
        );
        tabs.setSelectedIndex(0);
    }

    private void exportCsv() {
        if (grid.getColumnCount() == 0) {
            JOptionPane.showMessageDialog(
                this,
                "No result to export.",
                "Export CSV",
                JOptionPane.INFORMATION_MESSAGE
            );
            return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("result.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (FileWriter w = new FileWriter(chooser.getSelectedFile())) {
            w.write(gridToCsv());
            showMessage("Exported to " + chooser.getSelectedFile().getAbsolutePath());
        } catch (IOException ex) {
            showError("Export failed: " + ex.getMessage());
        }
    }

    private void installGridContextMenu() {
        grid.addMouseListener(
            new MouseAdapter() {

                @Override
                public void mousePressed(MouseEvent e) {
                    maybeShow(e);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    maybeShow(e);
                }

                private void maybeShow(MouseEvent e) {
                    if (!e.isPopupTrigger()) return;
                    int viewRow = grid.rowAtPoint(e.getPoint());
                    int viewCol = grid.columnAtPoint(e.getPoint());
                    if (viewRow < 0 || viewCol < 0) return;
                    // Keep an existing multi-cell selection intact for "Copy selection".
                    if (!grid.isCellSelected(viewRow, viewCol)) {
                        grid.changeSelection(viewRow, viewCol, false, false);
                    }
                    showCellMenu(e, viewRow, viewCol);
                }
            }
        );
    }

    private void showCellMenu(MouseEvent e, int viewRow, int viewCol) {
        final String column = grid.getColumnName(viewCol);
        final int modelRow = grid.convertRowIndexToModel(viewRow);
        final int rowNumber = modelRow + 1; // engine rows are 1-based
        final Object valueObj = grid.getValueAt(viewRow, viewCol);
        final boolean isNull = valueObj == null;
        final String value = isNull ? "" : valueObj.toString();

        JPopupMenu menu = new JPopupMenu();
        addItem(
            menu,
            isNull ? "Copy value (NULL)" : "Copy value",
            () ->
                Toolkit
                    .getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(isNull ? NULL_DISPLAY : value), null)
        );
        addItem(menu, "Copy selection", this::copySelection);
        menu.addSeparator();
        if (isNull) {
            // A NULL cell has no value to compare, so only the NULL assertions apply.
            addItem(
                menu,
                "Assert value is NULL",
                () ->
                    addValidation(
                        new DBValidation(column, rowNumber, DBValidation.Operator.IS_NULL, "")
                    )
            );
            addItem(
                menu,
                "Assert value is NOT NULL",
                () ->
                    addValidation(
                        new DBValidation(column, rowNumber, DBValidation.Operator.IS_NOT_NULL, "")
                    )
            );
        } else {
            addItem(
                menu,
                "Assert value equals",
                () ->
                    addValidation(
                        new DBValidation(column, rowNumber, DBValidation.Operator.EQUALS, value)
                    )
            );
            addItem(
                menu,
                "Assert value contains",
                () ->
                    addValidation(
                        new DBValidation(column, rowNumber, DBValidation.Operator.CONTAINS, value)
                    )
            );
            addItem(
                menu,
                "Assert column contains value",
                () ->
                    addValidation(
                        new DBValidation(column, rowNumber, DBValidation.Operator.EXISTS, value)
                    )
            );
            addItem(
                menu,
                "Assert cell equals (exact, this row)",
                () ->
                    addValidation(
                        new DBValidation(
                            column,
                            rowNumber,
                            DBValidation.Operator.CELL_EQUALS,
                            value
                        )
                    )
            );
            addItem(
                menu,
                "Assert value is NULL",
                () ->
                    addValidation(
                        new DBValidation(column, rowNumber, DBValidation.Operator.IS_NULL, "")
                    )
            );
            addItem(
                menu,
                "Assert value is NOT NULL",
                () ->
                    addValidation(
                        new DBValidation(column, rowNumber, DBValidation.Operator.IS_NOT_NULL, "")
                    )
            );
        }
        addItem(
            menu,
            "Assert row count…",
            () -> {
                String n = prompt("Expected row count:", String.valueOf(grid.getRowCount()));
                if (n != null) addValidation(
                    new DBValidation(column, rowNumber, DBValidation.Operator.ROW_COUNT, n)
                );
            }
        );
        menu.addSeparator();
        addItem(
            menu,
            "Store value → variable",
            () -> {
                String var = prompt("Runtime variable name (e.g. %dbValue%):", "%dbValue%");
                if (var != null) addValidation(
                    new DBValidation(column, rowNumber, DBValidation.Operator.STORE_VAR, var)
                );
            }
        );
        addItem(
            menu,
            "Store value → global variable",
            () -> {
                String var = prompt("Global variable name (e.g. %dbValue%):", "%dbValue%");
                if (var != null) addValidation(
                    new DBValidation(column, rowNumber, DBValidation.Operator.STORE_GLOBAL, var)
                );
            }
        );
        addItem(
            menu,
            "Store value → data sheet",
            () -> {
                String ref = prompt("Data-sheet reference (Sheet:Column):", "DbData:" + column);
                if (ref != null) addValidation(
                    new DBValidation(column, rowNumber, DBValidation.Operator.STORE_SHEET, ref)
                );
            }
        );
        menu.show(grid, e.getX(), e.getY());
    }

    private void installValidationsContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem remove = new JMenuItem("Remove selected validation(s)");
        remove.addActionListener(e -> removeSelectedValidations());
        JMenuItem removeAll = new JMenuItem("Remove all validations");
        removeAll.addActionListener(e -> confirmRemoveAllValidations());
        menu.add(remove);
        menu.add(removeAll);
        validationsTable.setComponentPopupMenu(menu);

        validationsTable
            .getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "remove-validations");
        validationsTable
            .getActionMap()
            .put(
                "remove-validations",
                new AbstractAction() {

                    @Override
                    public void actionPerformed(java.awt.event.ActionEvent e) {
                        removeSelectedValidations();
                    }
                }
            );
    }

    /** Removes every selected validation in one go, highest index first. */
    private void removeSelectedValidations() {
        int[] rows = validationsTable.getSelectedRows();
        if (rows.length == 0) return;
        int[] ordered = Arrays.copyOf(rows, rows.length);
        Arrays.sort(ordered);
        for (int i = ordered.length - 1; i >= 0; i--) {
            int row = ordered[i];
            if (row >= 0 && row < validations.size()) {
                validations.remove(row);
                validationsModel.removeRow(row);
            }
        }
    }

    private void confirmRemoveAllValidations() {
        if (validations.isEmpty()) return;
        int confirm = JOptionPane.showConfirmDialog(
            this,
            "Remove all " + validations.size() + " validations?",
            "Remove Validations",
            JOptionPane.YES_NO_OPTION
        );
        if (confirm == JOptionPane.YES_OPTION) {
            clearValidations();
        }
    }

    private void addValidation(DBValidation v) {
        validations.add(v);
        validationsModel.addRow(
            new Object[] { v.getColumn(), v.getRow(), v.getOperator(), displayExpected(v) }
        );
        tabs.setSelectedIndex(2);
    }

    /** NULL assertions carry no expected value; show that instead of a blank cell. */
    private static String displayExpected(DBValidation v) {
        if (
            v.getOperator() == DBValidation.Operator.IS_NULL ||
            v.getOperator() == DBValidation.Operator.IS_NOT_NULL
        ) {
            return NULL_DISPLAY;
        }
        return v.getExpectedValue();
    }

    private void addItem(JPopupMenu menu, String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    private String prompt(String message, String initial) {
        String result = JOptionPane.showInputDialog(this, message, initial);
        return (result == null || result.trim().isEmpty()) ? null : result.trim();
    }
}
