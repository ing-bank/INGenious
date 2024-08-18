package com.ing.datalib.component;

import com.ing.datalib.component.ExecutionStep.HEADERS;
import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.component.utils.SaveListener;
import com.ing.datalib.settings.ExecutionSettings;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.TableModelEvent;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 *
 *
 */
public class TestSet extends DataModel {

    private Release release;

    private final List<ExecutionStep> testSteps = Collections.synchronizedList(
            new ArrayList<ExecutionStep>());

    private String name;

    private Boolean saved = true;

    private SaveListener saveListener;

    private final ExecutionSettings execSettings;

    public TestSet(Release release, String name) {
        this.release = release;
        if (name.endsWith(".csv")) {
            this.name = name.substring(0, name.lastIndexOf(".csv"));
        } else {
            this.name = name;
        }
        this.execSettings = new ExecutionSettings(getProject().getLocation()
                + File.separator
                + "Settings"
                + File.separator
                + "TestExecution"
                + File.separator
                + getRelease().getName()
                + File.separator
                + getName()
        );
    }

    public final Project getProject() {
        return release.getProject();
    }

    public final Release getRelease() {
        return release;
    }

    public void setRelease(Release release) {
        this.release = release;
    }

    public List<ExecutionStep> getTestSteps() {
        return testSteps;
    }

    public List<ExecutionStep> getExecutableSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        for (ExecutionStep step : getTestSteps()) {
            if (Boolean.valueOf(step.getExecute())) {
                steps.add(step);
            }
        }
        return steps;
    }

    @Override
    public Boolean addRow() {
        addNewStep();
        return true;
    }

    @Override
    public void removeRow(int row) {
        if (row < testSteps.size()) {
            rowDeleted(row);
            testSteps.remove(row);
            fireTableRowsDeleted(row, row);
        }
    }

    @Override
    public void insertRow(int row, Object[] values) {
        addNewStepAt(row);
        for (int i = 0; i < values.length; i++) {
            setValueAt(values[i], row, i);
        }
    }

    public ExecutionStep addNewStep() {
        ExecutionStep step = new ExecutionStep(this);
        testSteps.add(step);
        rowAdded(testSteps.size() - 1);
        fireTableRowsInserted(testSteps.size() - 1, testSteps.size() - 1);
        return step;
    }

    public ExecutionStep addNewStepAt(int index) {
        ExecutionStep step = new ExecutionStep(this);
        testSteps.add(index, step);
        rowAdded(index);
        fireTableRowsInserted(index, index);
        return step;
    }

    public void replicateStepAt(int index) {
        ExecutionStep step = testSteps.get(index);
        ExecutionStep newStep = new ExecutionStep(this);
        step.copyValuesTo(newStep);
        testSteps.add(index, newStep);
        rowAdded(index);
        fireTableRowsInserted(index, index);
    }

    public Boolean moveRowsUp(int from, int to) {
        if (from - 1 < 0) {
            return false;
        }
        to = to + 1;
        Collections.rotate(testSteps.subList(from - 1, to), -1);
        setSaved(false);
        return true;
    }

    public Boolean moveRowsDown(int from, int to) {
        if (to + 1 > testSteps.size() - 1) {
            return false;
        }
        to += 1;
        Collections.rotate(testSteps.subList(from, to + 1), 1);
        setSaved(false);
        return true;
    }

    public void removeSteps(List<Integer> indices) {
        if (!indices.isEmpty()) {
            startGroupEdit();
            for (int index : indices) {
                rowDeleted(index);
                testSteps.remove(index);
            }
            stopGroupEdit();
            fireTableRowsDeleted(indices.get(indices.size() - 1), indices.get(0));
        }
    }

    public void removeSteps(int[] indices) {
        if (indices != null && indices.length > 0) {
            startGroupEdit();
            for (int index : indices) {
                if (index < testSteps.size()) {
                    rowDeleted(index);
                    testSteps.remove(index);
                    fireTableRowsDeleted(index, index);
                }
            }
            stopGroupEdit();
        }
    }

    public final String getName() {
        return name;
    }

    public final String getLocation() {
        return release.getLocation() + File.separator + name + ".csv";
    }

    public synchronized void loadTestSetTableModel() {
        if (testSteps.isEmpty()) {
            loadSteps();
        }
    }

    @Override
    public void loadTableModel() {
        loadTestSetTableModel();
    }

    public void reload() {
        testSteps.clear();
        loadSteps();
        fireTableStructureChanged();
        setSaved(true);
        for (int i = 0; i < testSteps.size(); i++) {
            testSteps.get(i).putValueAt(HEADERS.Status.getIndex(), "No Run");
            fireTableCellUpdated(i, HEADERS.Status.getIndex());
            setSaved(true);
        }
    }

    private void loadSteps() {
        List<CSVRecord> records = FileUtils.getRecords(new File(getLocation()));
        if (!records.isEmpty()) {
            for (CSVRecord record : records) {
                testSteps.add(new ExecutionStep(this, record));
            }
            setSaved(true);
        } else {
            testSteps.add(new ExecutionStep(this));
        }
    }

    public void save() {
        if (!isSaved()) {
            createIfNotExists();
            try (FileWriter out = new FileWriter(new File(getLocation())); CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withIgnoreEmptyLines());) {
                printer.printRecord(HEADERS.getValues());
                removeEmptySteps();
                for (ExecutionStep testStep : testSteps) {
                    printer.printRecord(testStep.exeStepDetails);
                }
                setSaved(true);
            } catch (Exception ex) {
                Logger.getLogger(TestSet.class.getName()).log(Level.SEVERE, "Error while saving", ex);
            }
        }
        execSettings.save();
    }

    private void createIfNotExists() {
        File file = new File(getLocation()).getParentFile();
        file.mkdirs();
    }

    private void removeEmptySteps() {
        for (int i = testSteps.size() - 1; i >= 0; i--) {
            if (testSteps.get(i).isEmpty()) {
                testSteps.remove(i);
                fireTableRowsDeleted(i, i);
            }
        }

    }

    @Override
    public int getRowCount() {
        return testSteps.size();
    }

    @Override
    public int getColumnCount() {
        return ExecutionStep.HEADERS.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return ExecutionStep.HEADERS.values()[columnIndex].name();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) {
            return Boolean.class;
        }
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != ExecutionStep.HEADERS.Status.getIndex();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (testSteps.size() > rowIndex) {
            String value = testSteps.get(rowIndex).getValueAt(columnIndex);
            if (columnIndex == 0) {
                return Boolean.valueOf(value);
            }
            return value;
        }
        return "";
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (!getValueAt(rowIndex, columnIndex).equals(aValue)) {
            if (testSteps.size() <= rowIndex) {
                testSteps.add(new ExecutionStep(this));
                setValueAt(aValue, rowIndex, columnIndex);
            }
            super.setValueAt(aValue, rowIndex, columnIndex);
            testSteps.get(rowIndex).putValueAt(columnIndex, Objects.toString(aValue, ""));
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    public String printString() {
        StringBuilder builder = new StringBuilder();
        builder
                .append("\t\t")
                .append("TestCase - ")
                .append(name)
                .append("\n")
                .append("\t\t")
                .append("TestSteps - ")
                .append(testSteps.size())
                .append("\n");
        return builder.toString();
    }

    @Override
    public String toString() {
        return name;

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

    public void refactorScenario(String oldScenarioName, String newScenarioName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        Boolean changesDone = false;
        loadTableModel();
        for (ExecutionStep testStep : testSteps) {
            if (testStep.getTestScenarioName().equals(oldScenarioName)) {
                testStep.setTestScenario(newScenarioName);
                changesDone = true;
            }
        }
        if (clearOnExit) {
            if (changesDone) {
                save();
            }
            getTestSteps().clear();
        }
    }

    public void refactorTestCase(String scenarioName, String oldTestCaseName, String newTestCaseName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        Boolean changesDone = false;
        loadTableModel();
        for (ExecutionStep testStep : testSteps) {
            if (testStep.getTestScenarioName().equals(scenarioName) && testStep.getTestCaseName().equals(oldTestCaseName)) {
                testStep.setTestCase(newTestCaseName);
                changesDone = true;
            }
        }
        if (clearOnExit) {
            if (changesDone) {
                save();
            }
            getTestSteps().clear();
        }
    }

    public void refactorTestCaseScenario(String testCaseName, String oldScenarioName, String newScenarioName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        Boolean changesDone = false;
        loadTableModel();
        for (ExecutionStep testStep : testSteps) {
            if (testStep.getTestScenarioName().equals(oldScenarioName) && testStep.getTestCaseName().equals(testCaseName)) {
                testStep.setTestScenario(newScenarioName);
                changesDone = true;
            }
        }
        if (clearOnExit) {
            if (changesDone) {
                save();
            }
            getTestSteps().clear();
        }
    }

    @Override
    public Boolean rename(String newName) {
        if (getRelease().getTestSetByName(newName) == null) {
            if (FileUtils.renameFile(getLocation(), newName + ".csv")) {
                name = newName;
                resetExecSettingsLocation();
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean delete() {
        if (FileUtils.deleteFile(getLocation())) {
            getRelease().removeTestSet(this);
            return true;
        }
        return false;
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

    public ExecutionSettings getExecSettings() {
        return execSettings;
    }

    public void resetExecSettingsLocation() {
        execSettings.setLocation(getProject().getLocation()
                + File.separator
                + "Settings"
                + File.separator
                + "TestExecution"
                + File.separator
                + getRelease().getName()
                + File.separator
                + getName()
        );
    }
}
