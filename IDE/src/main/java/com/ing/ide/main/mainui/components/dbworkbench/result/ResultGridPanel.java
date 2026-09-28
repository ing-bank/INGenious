package com.ing.ide.main.mainui.components.dbworkbench.result;

import com.ing.datalib.dbworkbench.DBValidation;
import com.ing.ide.main.mainui.components.dbworkbench.util.JdbcExecutor;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
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
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Bottom pane of the Database Workbench: the result grid, a messages area, and a
 * validations table. Right-clicking a cell or column header builds a
 * {@link DBValidation}, which later feeds the Automation step-builder.
 */
public class ResultGridPanel extends JPanel {
    private final JTabbedPane tabs = new JTabbedPane();
    private final JTable grid = new JTable();
    private final JTextArea messages = new JTextArea();
    private final JTable validationsTable = new JTable();
    private final DefaultTableModel validationsModel;
    private final JLabel statusLabel = new JLabel(" ");

    private final List<DBValidation> validations = new ArrayList<>();

    public ResultGridPanel() {
        setLayout(new BorderLayout());

        grid.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        grid.setCellSelectionEnabled(true);
        grid.setColumnSelectionAllowed(true);
        grid.setRowSelectionAllowed(true);
        grid.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        grid.setAutoCreateRowSorter(true);
        installGridContextMenu();

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
        bar.add(exportBtn);
        bar.add(copyBtn);
        return bar;
    }

    /** Renders a SELECT result set and shows the Grid tab. */
    public void showResult(JdbcExecutor.QueryResult result) {
        DefaultTableModel model = new DefaultTableModel();
        for (String col : result.columns) {
            model.addColumn(col);
        }
        for (List<Object> row : result.rows) {
            model.addRow(row.toArray());
        }
        grid.setModel(model);
        String status = result.rows.size() + " row(s) in " + result.elapsedMillis + " ms";
        if (result.truncated) {
            status += " (truncated to max rows)";
        }
        statusLabel.setText(status);
        messages.setForeground(Color.DARK_GRAY);
        messages.setText(status);
        tabs.setSelectedIndex(0);
    }

    /** Reports the row count of a DML statement and shows the Messages tab. */
    public void showDml(JdbcExecutor.DmlResult result) {
        grid.setModel(new DefaultTableModel());
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
                    grid.changeSelection(viewRow, viewCol, false, false);
                    showCellMenu(e, viewRow, viewCol);
                }
            }
        );
    }

    private void showCellMenu(MouseEvent e, int viewRow, int viewCol) {
        final String column = grid.getColumnName(viewCol);
        final int modelRow = grid.convertRowIndexToModel(viewRow);
        final int rowNumber = modelRow + 1; // engine rows are 1-based
        Object valueObj = grid.getValueAt(viewRow, viewCol);
        final String value = valueObj == null ? "" : valueObj.toString();

        JPopupMenu menu = new JPopupMenu();
        addItem(
            menu,
            "Copy value",
            () ->
                Toolkit
                    .getDefaultToolkit()
                    .getSystemClipboard()
                    .setContents(new StringSelection(value), null)
        );
        menu.addSeparator();
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
                    new DBValidation(column, rowNumber, DBValidation.Operator.CELL_EQUALS, value)
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
        JMenuItem remove = new JMenuItem("Remove selected validation");
        remove.addActionListener(
            e -> {
                int row = validationsTable.getSelectedRow();
                if (row >= 0 && row < validations.size()) {
                    validations.remove(row);
                    validationsModel.removeRow(row);
                }
            }
        );
        menu.add(remove);
        validationsTable.setComponentPopupMenu(menu);
    }

    private void addValidation(DBValidation v) {
        validations.add(v);
        validationsModel.addRow(
            new Object[] { v.getColumn(), v.getRow(), v.getOperator(), v.getExpectedValue() }
        );
        tabs.setSelectedIndex(2);
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
