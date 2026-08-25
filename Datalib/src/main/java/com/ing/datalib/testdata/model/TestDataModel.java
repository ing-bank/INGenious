package com.ing.datalib.testdata.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;

/**
 *
 *
 */
public abstract class TestDataModel extends AbstractDataModel<Record> {
    // Deliberately no explicit initializer: AbstractDataModel's constructor calls
    // migrateColumnsIfNeeded() (via loadMColumns()) before this subclass's own field
    // initializers would run, so an explicit "= false" here would silently clobber the
    // value just set during that super-constructor call.
    private boolean scopeColumnMigrated;

    public TestDataModel(String location, Boolean isFromFile) {
        super(location, isFromFile);
    }

    @Override
    public Record getNewRecord() {
        return new Record();
    }

    @Override
    public List<Record> getRecords() {
        return super.getRecords();
    }

    /**
     * Migrates test data columns by inserting the 'Scope' column at index 2 if missing.
     * This ensures older CSV files without the Scope column are automatically updated.
     *
     * @param columns the columns loaded from file
     * @return the migrated column list with Scope at index 2
     */
    @Override
    protected java.util.List<String> migrateColumnsIfNeeded(java.util.List<String> columns) {
        // Check if Scope column is missing (older 4-column format)
        if (!columns.contains(Record.HEADERS[2])) {
            // Insert Scope at index 2 if we have at least two columns, otherwise append
            if (columns.size() >= 2) {
                columns.add(2, Record.HEADERS[2]);
            } else {
                columns.add(Record.HEADERS[2]);
            }
            // Row values are still loaded positionally from the (unmigrated) file, so the
            // loader needs to know a Scope slot must be spliced into each row too - otherwise
            // every value from Iteration onward silently shifts left by one column.
            scopeColumnMigrated = true;
        }
        return columns;
    }

    /**
     * Whether {@link #migrateColumnsIfNeeded} just inserted the 'Scope' column because the
     * underlying file predates it. The row loader (e.g. CSV) uses this to splice an empty
     * Scope value into each loaded row at the same index, keeping row values aligned with
     * the migrated column list.
     *
     * @return true if this file's rows still need a Scope value spliced in at load time
     */
    public boolean isScopeColumnMigrated() {
        return scopeColumnMigrated;
    }

    @Override
    public boolean canEditOnExecution(int columnIndex) {
        // With new Scope column inserted at index 2, editable columns during execution are after SubIteration (now index 4)
        return columnIndex > 4;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Scope column (index 2) is read-only and auto-populated from Scenario/TestCase selection
        // It cannot be edited by users via UI or API
        if (columnIndex == 2) {
            return false;
        }
        return super.isCellEditable(rowIndex, columnIndex);
    }

    public Queue<Integer> getDataIteration(
        String scenario,
        String testcase,
        int startIteration,
        int endIteration
    ) {
        List<Integer> iterations = new LinkedList<>();
        iterations.addAll(
            getIterations(
                view().withTestcase(scenario, testcase).get(),
                getColumnIndex("Iteration"),
                startIteration,
                endIteration
            )
        );
        return (Queue<Integer>) iterations;
    }

    private List getIterations(List<List<String>> rows, int iter, int start, int end) {
        List<Integer> iterations = new LinkedList<>();
        if (rows != null && !rows.isEmpty()) {
            for (List<String> row : rows) {
                int i = parseInt(row.get(iter), 0);
                if (i >= start && (i <= end || end == -1)) {
                    if (!iterations.contains(i)) {
                        iterations.add(i);
                    }
                }
            }
        }
        return iterations;
    }

    private int parseInt(String val, int def) {
        if (Objects.toString(val, "").matches("[0-9]+")) {
            return Integer.valueOf(val);
        }
        return def;
    }

    public void refactorScenario(String oldScenarioName, String newScenarioName) {
        Boolean clearOnExit = getRecords().isEmpty();
        loadTableModel();
        for (Record record : getRecords()) {
            if (record.getScenario().equals(oldScenarioName)) {
                record.setScenario(newScenarioName);
                fireTableCellUpdated(getRecords().indexOf(record), 0);
            }
        }
        if (clearOnExit) {
            save();
            getRecords().clear();
        }
    }

    public void refactorTestCase(
        String scenarioName,
        String oldTestCaseName,
        String newTestCaseName
    ) {
        Boolean clearOnExit = getRecords().isEmpty();
        loadTableModel();
        for (Record record : getRecords()) {
            if (
                record.getScenario().equals(scenarioName) &&
                record.getTestcase().equals(oldTestCaseName)
            ) {
                record.setTestcase(newTestCaseName);
                fireTableCellUpdated(getRecords().indexOf(record), 1);
            }
        }
        if (clearOnExit) {
            save();
            getRecords().clear();
        }
    }

    public void refactorTestCaseScenario(
        String testCaseName,
        String oldScenarioName,
        String newScenarioName
    ) {
        Boolean clearOnExit = getRecords().isEmpty();
        loadTableModel();
        for (Record record : getRecords()) {
            if (
                record.getScenario().equals(oldScenarioName) &&
                record.getTestcase().equals(testCaseName)
            ) {
                record.setScenario(newScenarioName);
                fireTableCellUpdated(getRecords().indexOf(record), 1);
            }
        }
        if (clearOnExit) {
            save();
            getRecords().clear();
        }
    }

    /**
     * Updates the Scope of the test data entry belonging to the given scenario+testcase, as part of
     * an explicit test case conversion (Test Plan / Project Reusables / Shared Reusables move).
     * Matches on the entry's current scope too, so that when the same scenario+testcase name exists
     * in more than one scope, only the entry actually being converted (the one still carrying
     * {@code oldScope}) is updated - the colliding entry elsewhere is left untouched.
     */
    public void updateScope(
        String scenarioName,
        String testCaseName,
        String oldScope,
        String newScope
    ) {
        Boolean clearOnExit = getRecords().isEmpty();
        loadTableModel();
        boolean scopeUpdated = false;
        for (Record record : getRecords()) {
            if (
                record.getScenario().equals(scenarioName) &&
                record.getTestcase().equals(testCaseName) &&
                Objects.toString(record.getScope(), "").equals(oldScope)
            ) {
                record.setScope(newScope);
                fireTableCellUpdated(getRecords().indexOf(record), 2);
                scopeUpdated = true;
            }
        }
        // Always save if scope was updated to persist changes to CSV
        if (scopeUpdated) {
            save();
        }
        if (clearOnExit) {
            getRecords().clear();
        }
    }
}
