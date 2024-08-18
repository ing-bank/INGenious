
package com.ing.datalib.testdata.model;

import com.ing.datalib.component.utils.SaveListener;
import com.ing.datalib.testdata.view.TestDataView;
import com.ing.datalib.undoredo.UndoRedoModel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 *
 * 
 * @param <T>
 */
public abstract class AbstractDataModel<T extends List<String>> extends UndoRedoModel {

    private List<T> records = new ArrayList<>();

    private final List<String> columns = new ArrayList<>();

    private String location;

    private TestDataView v;

    private Boolean saved = true;

    private SaveListener saveListener;

    private final Boolean isFromFile;

    public AbstractDataModel(String location, Boolean isFromFile) {
        this.isFromFile = isFromFile;
        this.location = location;
        loadMColumns();
    }

    public TestDataView view() {
        if (v == null) {
            this.loadTableModel();
            v = new ModelView(this);
        }
        return v;
    }

    @Override
    public int getRowCount() {
        return records.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columns.get(columnIndex);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    abstract public boolean canEditOnExecution(int columnIndex);

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (records.size() > rowIndex) {
            if (records.get(rowIndex).size() > columnIndex) {
                return records.get(rowIndex).get(columnIndex);
            }
        }
        return "";
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (records.size() > rowIndex) {
            Object oldValue = getValueAt(rowIndex, columnIndex);
            if (!Objects.equals(oldValue, aValue)) {
                aValue = toObject(aValue);
                super.setValueAt(aValue, rowIndex, columnIndex);
                if (records.get(rowIndex).size() <= columnIndex) {
                    while (columns.size() > records.get(rowIndex).size()) {
                        records.get(rowIndex).add("");
                    }
                }
                records.get(rowIndex).set(columnIndex, aValue.toString());
                fireTableCellUpdated(rowIndex, columnIndex);
            }
        }
    }

    private Object toObject(Object aValue) {
        return Objects.isNull(aValue) ? "" : aValue;
    }

    public List<T> getRecords() {
        return records;
    }

    public T addRecord(T record) {
        this.records.add(record);
        super.rowAdded(records.size() - 1);
        fireTableRowsInserted(this.records.size() - 1, this.records.size() - 1);
        return record;
    }

    public T addRecord(T record, int index) {
        this.records.add(index, record);
        super.rowAdded(index);
        fireTableRowsInserted(index, index);
        return record;
    }

    /**
     * To add an empty row or record
     */
    public T addRecord() {
        T record = getNewRecord();
        for (String column : columns) {
            record.add("");
        }
        return addRecord(record);
    }

    public T addRecord(int index) {
        T record = getNewRecord();
        for (String column : columns) {
            record.add("");
        }
        return addRecord(record, index);
    }

    public Boolean moveRowsUp(int from, int to) {
        if (from - 1 < 0) {
            return false;
        }
        to = to + 1;
        Collections.rotate(records.subList(from - 1, to), -1);
        setSaved(false);
        return true;
    }
    

    public Boolean moveRowsDown(int from, int to) {
        if (to + 1 > records.size() - 1) {
            return false;
        }
        to += 1;
        Collections.rotate(records.subList(from, to + 1), 1);
        setSaved(false);
        return true;
    }

    public void replicateRecord(int index) {
        T record = records.get(index);
        T nrecord = getNewRecord();
        for (int i = 0; i < columns.size(); i++) {
            nrecord.add(i, record.get(i));
        }
        addRecord(nrecord, index);
    }

    public abstract T getNewRecord();

    public void removeRecord(int index) {
        super.rowDeleted(index);
        records.remove(index);
        fireTableRowsDeleted(index, index);
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public T getRecordByColumn(String columnName, String value) {
        int index = getColumnIndex(columnName);
        if (index != -1) {
            for (T record : records) {
                if (record.get(index).equals(value)) {
                    return record;
                }
            }
        }
        return null;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(Set<String> columns) {
        this.columns.clear();
        this.columns.addAll(columns);
    }

    public void setColumns(String[] columns) {
        this.columns.clear();
        if (columns != null) {
            this.columns.addAll(Arrays.asList(columns));
        }
    }

    public void setColumns(List<String> columns) {
        setColumns(new LinkedHashSet<>(columns));
    }

    public Boolean addColumn() {
        String val = "NewColumn";
        int i = 0;
        String colVal = val + i;
        while (columns.contains(colVal)) {
            colVal = val + i++;
        }
        return addColumn(colVal);
    }

    public Boolean addColumn(String column) {
        Boolean flag = !columns.contains(column) && columns.add(column);
        if (flag) {
            for (T record : records) {
                record.add("");
            }
            super.columnAdded(column);
            fireTableStructureChanged();
        }
        return flag;
    }

    public Boolean removeColumn(String column) {
        int index = columns.indexOf(column);
        super.columnRemoved(index);
        Boolean flag = columns.remove(column);
        if (flag) {
            for (T record : records) {
                record.remove(index);
            }
            fireTableStructureChanged();
        }
        return flag;
    }

    public void removeColumn(List<Integer> columnList) {
        super.startGroupEdit();
        for (int column : columnList) {
            super.columnRemoved(column);
            columns.remove(column);
            for (T record : records) {
                record.remove(column);
            }
        }
        super.stopGroupEdit();
        fireTableStructureChanged();
    }

    public Boolean renameColumn(String oldColumn, String newColumn) {
        int index = columns.indexOf(oldColumn);
        if (index != -1) {
            columns.set(index, newColumn);
            fireTableStructureChanged();
            return true;
        }
        return false;
    }

    public void clearSteps() {
        records.clear();
    }

    @Override
    public void removeRow(int row) {
        removeRecord(row);
    }

    @Override
    public void insertRow(int row, Object[] values) {
        addRecord(row);
        for (int column = 0; column < values.length; column++) {
            setValueAt(values[column], row, column);
        }
    }

    @Override
    public void insertColumnAt(int colIndex, String name, Object[] values) {
        columns.add(colIndex, name);
        for (int i = 0; i < records.size(); i++) {
            records.get(i).add(colIndex, Objects.toString(values[i]));
        }
        fireTableStructureChanged();
    }

    @Override
    public void removeColumn(int colIndex) {
        removeColumn(columns.get(colIndex));
    }

    public final void loadTableModel() {
        if (records.isEmpty()) {
            loadMRecords();
            setSaved(true);
        }
    }

    public final void load() {
        loadMColumns();
        loadMRecords();
        fireTableStructureChanged();
        setSaved(true);
        super.clearUndoRedo();
    }

    private void loadMRecords() {
        records.clear();
        if (isFromFile) {
            if (new File(getLocation()).exists()) {
                loadRecords(new File(getLocation()));
            }
        } else {
            loadRecords();
        }
        super.clearUndoRedo();
    }

    public void removeEmptyRecords() {
        int previousSize = getRecords().size();
        for (int i = previousSize - 1; i >= 0; i--) {
            T record = getRecords().get(i);
            Boolean empty = true;
            for (String value : record) {
                if (!value.trim().isEmpty()) {
                    empty = false;
                    break;
                }
            }
            if (empty) {
                getRecords().remove(record);
            }
        }
        if (previousSize != getRecords().size()) {
            fireTableDataChanged();
        }
    }

    private void loadMColumns() {
        if (isFromFile) {
            if (new File(getLocation()).exists()) {
                setColumns(loadColumns(new File(getLocation())));
            }
        } else {
            setColumns(loadColumns());
        }
        setSaved(true);
    }
    
    
        
    


    public abstract Set<String> loadColumns(File location);

    public abstract AbstractDataModel loadRecords(File location);

    public abstract Set<String> loadColumns();

    public abstract AbstractDataModel loadRecords();

    public abstract Boolean rename(String newName);

    public abstract Boolean delete();

    public final void save() {
        if (!isSaved()) {
            saveChanges();
            setSaved(true);
        }
    }

    public abstract void saveChanges();

    public abstract String getName();

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getColumnIndex(String col) {
        return columns.indexOf(col);
    }

    public boolean hasColumn(String field) {
        return columns.contains(field);
    }

    public Boolean isSaved() {
        return saved;
    }

    public void setSaved(Boolean saved) {
        this.saved = saved;
        if (saveListener != null) {
            saveListener.onSave(saved);
        }
    }

    public void setSaveListener(SaveListener saveListener) {
        this.saveListener = saveListener;
    }

    @Override
    public void fireTableChanged(TableModelEvent tme) {
        setSaved(false);
        super.fireTableChanged(tme);
    }

    @Override
    public void fireTableCellUpdated(int i, int i1) {
        setSaved(false);
        super.fireTableCellUpdated(i, i1);
    }

    @Override
    public void fireTableRowsDeleted(int i, int i1) {
        setSaved(false);
        super.fireTableRowsDeleted(i, i1);
    }

    @Override
    public void fireTableRowsUpdated(int i, int i1) {
        setSaved(false);
        super.fireTableRowsUpdated(i, i1);
    }

    @Override
    public void fireTableRowsInserted(int i, int i1) {
        setSaved(false);
        super.fireTableRowsInserted(i, i1);
    }

    @Override
    public void fireTableStructureChanged() {
        setSaved(false);
        super.fireTableStructureChanged();
    }

    @Override
    public void fireTableDataChanged() {
        setSaved(false);
        super.fireTableDataChanged();
    }

    public void cloneAs(AbstractDataModel newTestDataModel) {
        newTestDataModel.setColumns(getColumns());
        loadTableModel();
        for (int row = 0; row < getRowCount(); row++) {
            newTestDataModel.addRecord();
            for (int column = 0; column < getColumnCount(); column++) {
                String value = Objects.toString(getValueAt(row, column), "");
                newTestDataModel.setValueAt(value, row, column);
            }
        }
        newTestDataModel.setSaved(false);
    }

    class ModelView extends TestDataView {

        AbstractDataModel model;

        public ModelView(AbstractDataModel model) {
            this.model = model;
        }

        @Override
        public List records() {
            return model.getRecords();
        }

        @Override
        public int index(String field) {
            return model.getColumnIndex(field);
        }

        @Override
        public boolean canUpdate(String field) {
            return model.canEditOnExecution(index(field));
        }

        @Override
        public List columns() {
            return model.getColumns();
        }

        @Override
        public List<String> addRecord(String scenario, String testcase, String iteration, String subIteration) {
            List<String> record = model.addRecord();
            record.set(0, scenario);
            record.set(1, testcase);
            record.set(2, iteration);
            record.set(3, subIteration);
            return record;
        }

    }
}
