package com.ing.datalib.plugin;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestData;
import com.ing.datalib.testdata.model.Record;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.datalib.testdata.view.TestDataView;
import com.ing.ingenious.api.contract.data.ProjectTestDataApi;
import com.ing.ingenious.api.contract.data.TestDataViewApi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * {@link ProjectTestDataApi} over the test data of whichever project is open.
 *
 * <p>The project is read through a supplier rather than held, because the application replaces
 * its project object when the user opens another one. A plugin therefore keeps one of these for
 * as long as it lives and always reaches the project in front of the user.
 *
 * <p>Only the default environment is offered. An environment is a deployment of the system
 * under test; which one a run uses is chosen at run time, so a design-time decision recorded
 * per environment would be a decision the user never made.
 */
public final class ProjectTestData implements ProjectTestDataApi {
    private final Supplier<Project> project;

    /**
     * @param project supplies the open project, or {@code null} when none is open
     */
    public ProjectTestData(Supplier<Project> project) {
        this.project = project;
    }

    @Override
    public List<String> sheets() {
        TestData testData = testData();
        if (testData == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(testData.getTestDataNames());
    }

    @Override
    public boolean addSheet(String sheet) {
        TestData testData = testData();
        if (testData == null || isBlank(sheet)) {
            return false;
        }
        if (testData.getByName(sheet) != null) {
            return true;
        }
        return testData.addTestData(testData.getNewTestData(sheet)) != null;
    }

    @Override
    public boolean addColumn(String sheet, String column) {
        TestDataModel model = model(sheet);
        if (model == null || isBlank(column)) {
            return false;
        }
        // Existing records grow a cell for the new column, so load them before adding it.
        model.loadTableModel();
        if (model.hasColumn(column)) {
            return true;
        }
        return Boolean.TRUE.equals(model.addColumn(column));
    }

    @Override
    public TestDataViewApi testCase(String sheet, String scenario, String testCase) {
        TestDataModel model = model(sheet);
        if (model == null || isBlank(scenario) || isBlank(testCase)) {
            return null;
        }
        model.loadTableModel();
        TestDataView view = find(model, scenario, testCase);
        if (view.get().isEmpty()) {
            addRecord(model, scenario, testCase);
            view = find(model, scenario, testCase);
        }
        return view;
    }

    @Override
    public boolean save(String sheet) {
        TestDataModel model = model(sheet);
        if (model == null) {
            return false;
        }
        // Records reached through a view are updated in place, which leaves the model looking
        // saved. Asking for a write here has to produce one.
        model.setSaved(false);
        model.save();
        return true;
    }

    /**
     * The records of one test case.
     *
     * <p>A view indexes the records it found and answers from that index next time. Clearing it
     * first is what makes a lookup after a write see the records as they now are.
     */
    private TestDataView find(TestDataModel model, String scenario, String testCase) {
        TestDataView view = model.view();
        view.clear();
        return view.withTestcase(quote(scenario), quote(testCase));
    }

    /**
     * Gives a test case its first record.
     *
     * <p>Written through the model rather than the view: the record is addressed by column
     * name, so a sheet whose leading columns have been migrated still gets the scenario and
     * test case in the right cells.
     */
    private void addRecord(TestDataModel model, String scenario, String testCase) {
        int row = model.getRowCount();
        model.addRecord();
        setCell(model, row, Record.HEADERS[0], scenario);
        setCell(model, row, Record.HEADERS[1], testCase);
        setCell(model, row, Record.HEADERS[3], "1");
        setCell(model, row, Record.HEADERS[4], "1");
    }

    private void setCell(TestDataModel model, int row, String column, String value) {
        int index = model.getColumnIndex(column);
        if (index >= 0) {
            model.setValueAt(value, row, index);
        }
    }

    private TestDataModel model(String sheet) {
        TestData testData = testData();
        if (testData == null || isBlank(sheet)) {
            return null;
        }
        return testData.getByName(sheet);
    }

    private TestData testData() {
        Project open = project.get();
        return open == null || open.getTestData() == null ? null : open.getTestData().defData();
    }

    /**
     * A view matches scenario and test case as regular expressions, which is what lets callers
     * pass a wildcard. A name is not a pattern, so it is quoted before it is used as one.
     */
    private String quote(String name) {
        return java.util.regex.Pattern.quote(name);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
