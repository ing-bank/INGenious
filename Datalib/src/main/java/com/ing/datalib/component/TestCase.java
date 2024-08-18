
package com.ing.datalib.component;

import com.ing.datalib.component.TestStep.HEADERS;
import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.component.utils.SaveListener;
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
public class TestCase extends DataModel {

    private Scenario scenario;

    private final List<TestStep> testSteps = Collections.synchronizedList(new ArrayList<TestStep>());

    private String name;

    private Boolean saved = true;

    private SaveListener saveListener;

    private Reusable reusable = null;

    public TestCase(Scenario scenario, String name) {
        this.scenario = scenario;
        if (name.endsWith(".csv")) {
            this.name = name.substring(0, name.lastIndexOf(".csv"));
        } else {
            this.name = name;
        }
    }

    public Project getProject() {
        return scenario.getProject();
    }

    public Scenario getScenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public List<TestStep> getTestSteps() {
        return testSteps;
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

    @Override
    public Boolean addRow() {
        addNewStep();
        return true;
    }

    public TestStep addNewStep() {
        TestStep step = new TestStep(this);
        testSteps.add(step);
        rowAdded(testSteps.size() - 1);
        fireTableRowsInserted(testSteps.size() - 1, testSteps.size() - 1);
        return step;
    }

    public TestStep addNewStepAt(int index) {
        TestStep step = new TestStep(this);
        testSteps.add(index, step);
        rowAdded(index);
        fireTableRowsInserted(index, index);
        return step;
    }

    public void replicateStepAt(int index) {
        TestStep step = testSteps.get(index);
        TestStep newStep = new TestStep(this);
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

    public void clearSteps() {
        startGroupEdit();
        testSteps.clear();
        fireTableDataChanged();
        stopGroupEdit();
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

    public void toggleComment(int[] indices) {
        startGroupEdit();
        for (int index : indices) {
            testSteps.get(index).toggleComment();
        }
        stopGroupEdit();
    }

    public void toggleBreakPoint(int[] indices) {
        startGroupEdit();
        for (int index : indices) {
            testSteps.get(index).toggleBreakPoint();
        }
        stopGroupEdit();
    }

    public void addReusableStep(String reusable) {
        TestStep step = new TestStep(this);
        step.setObject("Execute");
        step.setAction(reusable);
        testSteps.add(step);
        rowAdded(testSteps.size() - 1);
        fireTableRowsInserted(testSteps.size() - 1, testSteps.size() - 1);
    }

    public void addReusableStep(int index, String reusable) {
        TestStep step = new TestStep(this);
        step.setObject("Execute");
        step.setAction(reusable);
        addStep(index, step);
    }

    public void addObjectStep(int index, String objectName, String pageName) {
        TestStep step = new TestStep(this).asObjectStep(objectName, pageName);
        addStep(index, step);
    }

    private void addStep(int index, TestStep step) {
        if (testSteps.size() > index) {
            testSteps.add(index, step);
        } else {
            testSteps.add(step);
        }
        rowAdded(index);
        fireTableRowsInserted(index, index);
    }

    public TestCase createAsReusable(String reusableName, int fromStep, int toStep) {
        TestCase newTestcase = getScenario().addTestCase(reusableName);
        if (newTestcase != null) {
            for (int i = fromStep; i <= toStep; i++) {
                testSteps.get(i).copyValuesTo(newTestcase.addNewStep());
            }
            startGroupEdit();
            addReusableStep(fromStep,
                    getScenario().getName() + ":" + reusableName);
            for (int i = toStep + 1; i >= fromStep + 1; i--) {
                rowDeleted(i);
                testSteps.remove(i);
            }
            stopGroupEdit();
            fireTableRowsDeleted(fromStep + 1, toStep);
            return newTestcase;
        }
        return null;
    }

    public void toggleAsReusable() {
        if (reusable == null) {
            reusable = new Reusable();
        } else {
            reusable = null;
        }
    }

    public void copyValuesTo(TestCase testCase) {
        for (TestStep testStep : testSteps) {
            testStep.copyValuesTo(testCase.addNewStep());
        }
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return scenario.getLocation() + File.separator + name + ".csv";
    }

    public void loadTestCaseTableModel() {
        if (testSteps.isEmpty()) {
            loadSteps();
        }
    }

    @Override
    public synchronized void loadTableModel() {
        loadTestCaseTableModel();
    }

    public void reload() {
        testSteps.clear();
        loadSteps();
        fireTableStructureChanged();
        setSaved(true);
    }

    private void loadSteps() {
        List<CSVRecord> records = FileUtils.getRecords(new File(getLocation()));
        if (!records.isEmpty()) {
            for (CSVRecord record : records) {
                testSteps.add(new TestStep(this, record));
            }
            setSaved(true);
        } else {
            testSteps.add(new TestStep(this));
        }
        super.clearUndoRedo();
    }

    public void save() {
        if (!isSaved()) {
            createIfNotExists();
            try (FileWriter out = new FileWriter(new File(getLocation())); CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withIgnoreEmptyLines());) {
                printer.printRecord(HEADERS.getValues());
                removeEmptySteps();
                autoNumber();
                for (TestStep testStep : testSteps) {
                    printer.printRecord(testStep.stepDetails);
                }
                setSaved(true);
            } catch (Exception ex) {
                Logger.getLogger(TestCase.class.getName()).log(Level.SEVERE, "Error while saving", ex);
            }
        }
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

    private void autoNumber() {
        for (int i = 0; i < testSteps.size(); i++) {
            String val = testSteps.get(i).getTag().replaceAll("[0-9]+", "").trim();
            testSteps.get(i).setTag(val + (i + 1));
        }
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return testSteps.size();
    }

    @Override
    public int getColumnCount() {
        return TestStep.HEADERS.size();
    }

    @Override
    public String getColumnName(int columnIndex) {
        return TestStep.HEADERS.values()[columnIndex].name();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (testSteps.size() > rowIndex) {
            return testSteps.get(rowIndex).getValueAt(columnIndex);
        }
        return "";
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (!getValueAt(rowIndex, columnIndex).equals(aValue)) {
            if (testSteps.size() <= rowIndex) {
                testSteps.add(new TestStep(this));
                setValueAt(aValue, rowIndex, columnIndex);
            }
            super.setValueAt(aValue, rowIndex, columnIndex);
            testSteps.get(rowIndex).putValueAt(columnIndex, Objects.toString(aValue, ""));
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

    public boolean isReusable() {
        return getReusable() != null;
    }

    public Reusable getReusable() {
        return reusable;
    }

    public void setReusable(Reusable reusable) {
        this.reusable = reusable;
    }

    public String getKey() {
        return getScenario().getName() + "#" + getName();
    }

    @Override
    public Boolean delete() {
        if (FileUtils.deleteFile(getLocation())) {
            getScenario().removeTestCase(this);
            return true;
        }
        return false;
    }

    public void refactorScenario(String oldScenarioName, String newScenarioName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getReusableData();
            if (values != null) {
                if (values[0].equals(oldScenarioName)) {
                    testStep.asReusableStep(newScenarioName, values[1]);
                }
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public void refactorTestCase(String scenarioName, String oldTestCaseName, String newTestCaseName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getReusableData();
            if (values != null) {
                if (values[0].equals(scenarioName) && values[1].equals(oldTestCaseName)) {
                    testStep.asReusableStep(scenarioName, newTestCaseName);
                }
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public void refactorTestCaseScenario(String testCaseName, String oldScenarioName, String newScenarioName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getReusableData();
            if (values != null) {
                if (values[0].equals(oldScenarioName) && values[1].equals(testCaseName)) {
                    testStep.asReusableStep(newScenarioName, testCaseName);
                }
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public void refactorTestData(String oldTDName, String newTDName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getTestDataFromInput();
            if (values != null) {
                if (values[0].equals(oldTDName)) {
                    testStep.setInput(newTDName + ":" + values[1]);
                }
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public void refactorTestDataColumn(String testDataName, String oldColumnName, String newColumnName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getTestDataFromInput();
            if (values != null) {
                if (values[0].equals(testDataName) && values[1].equals(oldColumnName)) {
                    testStep.setInput(testDataName + ":" + newColumnName);
                }
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public void refactorObjectName(String pageName, String oldName, String newName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            if (testStep.getReference().equals(pageName) && testStep.getObject().equals(oldName)) {
                testStep.setObject(newName);
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public void refactorObjectName(String oldpageName, String oldObjName, String newPageName, String newObjName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            if (testStep.getReference().equals(oldpageName) && testStep.getObject().equals(oldObjName)) {
                testStep.setObject(newObjName);
                testStep.setReference(newPageName);
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public void refactorPageName(String oldPageName, String newPageName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        for (TestStep testStep : testSteps) {
            if (testStep.getReference().equals(oldPageName)) {
                testStep.setReference(newPageName);
            }
        }
        if (clearOnExit) {
            save();
            getTestSteps().clear();
        }
    }

    public TestCase getImpactedObjectTestCases(String pageName, String objectName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        Boolean impacted = false;
        for (TestStep testStep : testSteps) {
            if (testStep.getReference().equals(pageName)
                    && testStep.getObject().equals(objectName)) {
                impacted = true;
                break;
            }
        }
        if (clearOnExit) {
            getTestSteps().clear();
        }
        return impacted ? this : null;
    }

    public TestCase getImpactedTestCaseTestCases(String scenarioName, String testCaseName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        Boolean impacted = false;
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getReusableData();
            if (values != null) {
                if (values[0].equals(scenarioName) && values[1].equals(testCaseName)) {
                    impacted = true;
                    break;
                }
            }
        }
        if (clearOnExit) {
            getTestSteps().clear();
        }
        return impacted ? this : null;
    }

    public TestCase getImpactedTestDataTestCases(String testDataName) {
        Boolean clearOnExit = getTestSteps().isEmpty();
        loadTableModel();
        Boolean impacted = false;
        for (TestStep testStep : testSteps) {
            String[] values = testStep.getTestDataFromInput();
            if (values != null) {
                if (values[0].equals(testDataName)) {
                    impacted = true;
                    break;
                }
            }
        }
        if (clearOnExit) {
            getTestSteps().clear();
        }
        return impacted ? this : null;
    }

    @Override
    public Boolean rename(String newName) {
        if (getScenario().getTestCaseByName(newName) == null) {
            if (FileUtils.renameFile(getLocation(), newName + ".csv")) {
                getProject().refactorTestCase(getScenario().getName(), name, newName);
                name = newName;
                return true;
            }
        }
        return false;
    }

}
