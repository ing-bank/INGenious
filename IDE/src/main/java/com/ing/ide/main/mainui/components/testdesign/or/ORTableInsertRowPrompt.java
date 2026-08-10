package com.ing.ide.main.mainui.components.testdesign.or;

import com.ing.ide.main.utils.table.XTable;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Utility for wiring XTable's insert-row prompt to OR property tables.
 */
public final class ORTableInsertRowPrompt {

    private ORTableInsertRowPrompt() {
        // Utility class
    }

    /**
     * Inserts a row into an OR object at the given model index.
     *
     * @param <T> OR object type
     */
    @FunctionalInterface
    public interface RowInserter<T> {
        int insert(T object, int modelInsertIndex);
    }

    /**
     * Registers an insert-row prompt handler for the given table.
     *
     * @param table target table
     * @param stopCellEditing callback to stop active cell editing
     * @param objectSupplier supplies the table's current OR object
     * @param rowCountProvider returns the OR object's model row count
     * @param rowInserter inserts a row and returns the inserted model index
     * @param <T> OR object type
     */
    public static <T> void install(
        XTable table,
        Runnable stopCellEditing,
        Supplier<T> objectSupplier,
        ToIntFunction<T> rowCountProvider,
        RowInserter<T> rowInserter
    ) {
        table.setInsertRowHandler(
            viewInsertIndex -> {
                stopCellEditing.run();

                T object = objectSupplier.get();
                if (object == null) {
                    return;
                }

                int modelInsertIndex = toModelInsertIndex(
                    table,
                    viewInsertIndex,
                    rowCountProvider.applyAsInt(object)
                );

                int insertedRow = rowInserter.insert(object, modelInsertIndex);

                if (insertedRow >= 0) {
                    selectInsertedRow(table, insertedRow);
                }
            }
        );
    }

    /**
     * Converts a table view insertion index to a model insertion index.
     */
    private static int toModelInsertIndex(XTable table, int viewInsertIndex, int modelRowCount) {
        if (viewInsertIndex <= 0) {
            return 0;
        }

        if (viewInsertIndex >= table.getRowCount()) {
            return modelRowCount;
        }

        return table.convertRowIndexToModel(viewInsertIndex);
    }

    /**
     * Selects and scrolls to the inserted row.
     */
    private static void selectInsertedRow(XTable table, int insertedModelRow) {
        int viewRow = table.convertRowIndexToView(insertedModelRow);

        if (viewRow < 0) {
            table.repaint();
            return;
        }

        table.getSelectionModel().setSelectionInterval(viewRow, viewRow);
        table.scrollRectToVisible(table.getCellRect(viewRow, 0, true));
    }
}
