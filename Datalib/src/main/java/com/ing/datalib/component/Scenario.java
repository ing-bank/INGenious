package com.ing.datalib.component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.or.web.WebOR.ORScope;

/**
 * Represents a scenario within a project’s TestPlan and serves as a container for related test cases.
 * <p>
 * A {@code Scenario} is backed by a filesystem folder under {@code <project>/TestPlan/<scenario>},
 * automatically loads its {@link TestCase} CSV files on construction, and exposes methods for adding,
 * removing, loading, and saving test cases.
 * </p>
 *
 * <p>
 * The class also implements {@code DataModel} table-model behavior for UI consumption by presenting
 * a scenario-level view of non-reusable test cases, and provides refactoring and impact-analysis helpers
 * that delegate to contained test cases (e.g., object/page/test data reference updates and impacted test
 * case discovery).
 * </p>
 */
public class Scenario extends DataModel {

    public enum Source {
        TEST_PLAN,
        REUSABLE_COMPONENTS
    }

    private final Project project;

    private final List<TestCase> testCases = new ArrayList<>();

    private String name;

    private final Source source;

    /**
     * Constructs a scenario in the Test Plan.
     * @param project parent project
     * @param name scenario name
     */
    public Scenario(Project project, String name) {
        this(project, name, Source.TEST_PLAN);
    }

    /**
     * Constructs a scenario with specified source (Test Plan or Reusable Components).
     * @param project parent project
     * @param name scenario name
     * @param source scenario source (TEST_PLAN or REUSABLE_COMPONENTS)
     */
    public Scenario(Project project, String name, Source source) {
        this.project = project;
        this.name = name;
        this.source = source;
        loadTestcases();
    }

    /**
     * Returns the filesystem location of this scenario.
     * @return absolute path to the scenario directory
     */
    public String getLocation() {
        if (project == null) {
            return "";
        }
        String scenarioPath = project.getScenarioPath(source, name);
        if (scenarioPath != null && !scenarioPath.isEmpty()) {
            return scenarioPath;
        }
        String base = project.getLocation();
        if (base == null) {
            return "";
        }
        String dir = source == Source.REUSABLE_COMPONENTS
                ? Project.REUSABLE_COMPONENTS_DIR
                : Project.TEST_PLAN_DIR;
        return base + File.separator + dir + File.separator + name;
    }

    /**
     * Checks if this is a reusable scenario.
     * @return true if this scenario is in Reusable Components, false otherwise
     */
    public boolean isReusableScenario() {
        return source == Source.REUSABLE_COMPONENTS;
    }

    /**
     * Returns the source of this scenario.
     * @return TEST_PLAN or REUSABLE_COMPONENTS
     */
    public Source getSource() {
        return source;
    }

    /**
     * Returns the parent project.
     * @return parent project
     */
    public Project getProject() {
        return project;
    }

    /**
     * Returns all test cases in this scenario.
     * @return list of test cases
     */
    public List<TestCase> getTestCases() {
        return testCases;
    }

    /**
     * Finds a test case by name.
     * @param name test case name (case-insensitive)
     * @return the test case if found, null otherwise
     */
    public TestCase getTestCaseByName(String name) {
        for (TestCase testcase : testCases) {
            if (testcase.getName().equalsIgnoreCase(name)) {
                return testcase;
            }
        }
        return null;
    }
    /**
     * Finds a test case by name.
     * @param scenarioName scenario name (case-insensitive)
     * @param testCaseName test case name (case-insensitive)
     * @return the reusable test case if found, null otherwise
     */
    public TestCase getTestCaseByName(String scenarioName, String testCaseName) {
        Scenario sc = project.getScenarioByName(scenarioName);
        if (sc == null) {
            return null;
        }
        
        List<TestCase> tc = sc.getTestCases();
        String tc_name;
        for (TestCase testcase : tc) {
            tc_name = testcase.getName();
            if (tc_name.equalsIgnoreCase(testCaseName)) {
                return testcase;
            }
        }
        return null;
    }

    /**
     * Finds a reusable test case by name.
     * @param scenarioName scenario name (case-insensitive)
     * @param testCaseName test case name (case-insensitive)
     * @return the reusable test case if found, null otherwise
     */
    public TestCase getReusableTestCaseByName(String scenarioName, String testCaseName) {
        Scenario sc = project.getReusableScenarioByName(scenarioName);
        if (sc == null) {
            return null;
        }

        List<TestCase> reusables = sc.getTestCases();
        String tc_name;
        for (TestCase testcase : reusables) {
            tc_name = testcase.getName();
            if (tc_name.equalsIgnoreCase(testCaseName)) {
                return testcase;
            }
        }
        return null;
    }

    /**
     * Finds the index of a test case by name.
     * @param name test case name (case-insensitive)
     * @return the index if found, -1 otherwise
     */
    public int getIndexOfTestCaseByName(String name) {
        for (int i = 0; i < testCases.size(); i++) {
            if (testCases.get(i).getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Loads all test case CSV files from the scenario directory.
     */
    private void loadTestcases() {
        File scenDir = new File(getLocation());
        if (scenDir.exists()) {
            for (String testCase : scenDir.list(FileUtils.CSV_FILTER)) {
                testCases.add(new TestCase(this, testCase));
            }
        }
    }

    /**
     * Loads table models for all test cases.
     */
    public void loadTestCasesTableModel() {
        for (TestCase testCase : testCases) {
            testCase.loadTestCaseTableModel();
        }
    }

    /**
     * Loads the table model for this scenario (delegates to loadTestCasesTableModel).
     */
    @Override
    public void loadTableModel() {
        loadTestCasesTableModel();
    }

    /**
     * Adds a new test case to this scenario.
     * @param testCaseName name of the test case to add
     * @return the created test case, or null if it already exists
     */
    public TestCase addTestCase(String testCaseName) {
        if (getTestCaseByName(testCaseName) == null) {
            if (project.hasTestCaseInAnyScenario(getName(), testCaseName)) {
                return null;
            }
            TestCase tc = new TestCase(this, testCaseName);
            testCases.add(tc);
            tc.setSaved(false);
            // Auto-save to create directory and file immediately
            tc.save();
            return tc;
        }
        return null;
    }

    /**
     * Removes a test case from this scenario.
     * @param testCase test case to remove
     */
    public void removeTestCase(TestCase testCase) {
        if (testCases.remove(testCase)) {

        }
    }

    /**
     * Saves all test cases in this scenario.
     */
    public void save() {
        for (TestCase testCase : testCases) {
            testCase.save();
        }
    }

    /**
     * Returns the scenario name.
     * @return scenario name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the row count for table model (number of non-reusable test cases).
     * @return number of test cases
     */
    @Override
    public int getRowCount() {
        return getTestcaseCount();
    }

    /**
     * Returns the column count for table model (max number of test steps + 1).
     * @return column count
     */
    @Override
    public int getColumnCount() {
        int max = 0;
        if (getTestcaseCount() > 0) {
            for (TestCase testCase : getTestcasesAlone()) {
                max = Math.max(max, testCase.getTestSteps().size());
            }
        }
        return max + 1;
    }

    /**
     * Returns the column name for the specified column index.
     * @param columnIndex column index
     * @return column name
     */
    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return "TestCase";
        }
        return "Component " + columnIndex;
    }

    /**
     * Returns the column class for the specified column.
     * @param columnIndex column index
     * @return Object.class
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Object.class;
    }

    /**
     * Returns whether a cell is editable.
     * @param rowIndex row index
     * @param columnIndex column index
     * @return true if editable (all columns except the first), false otherwise
     */
    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex != 0;
    }

    /**
     * Returns the value at the specified cell.
     * @param rowIndex row index
     * @param columnIndex column index
     * @return cell value
     */
    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
            return getTestcasesAlone().get(rowIndex).getName();
        }
        return getTestcasesAlone().get(rowIndex).getValueAt(columnIndex - 1, 3);
    }

    /**
     * Sets the value at the specified cell.
     * @param aValue new value
     * @param rowIndex row index
     * @param columnIndex column index
     */
    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
//            testCases.get(rowIndex).setName(aValue.toString());
//            project.reload(testCases.get(rowIndex));
        } else {
            getTestcasesAlone().get(rowIndex).setValueAt(aValue, columnIndex - 1, 3);
        }
    }

    /**
     * Adds a new row (not supported).
     * @return false
     */
    @Override
    public Boolean addRow() {
        return false;
    }

    /**
     * Removes a row (not supported).
     * @param row row index
     */
    @Override
    public void removeRow(int row) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Inserts a row (not supported).
     * @param row row index
     * @param values row values
     */
    @Override
    public void insertRow(int row, Object[] values) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    /**
     * Returns a detailed string representation of the scenario.
     * @return detailed scenario information
     */
    public String printString() {
        StringBuilder builder = new StringBuilder();
        builder
                .append("\t")
                .append("Scenario - ")
                .append(name)
                .append("\n")
                .append("\t")
                .append("TestCases - ")
                .append(testCases.size())
                .append("\n");
        for (TestCase testCase : testCases) {
            builder.append(testCase.toString());
        }
        return builder.toString();
    }

    /**
     * Returns string representation of the scenario (scenario name).
     * @return scenario name
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     *
     * @return all the TestCases without Reusable
     */
    public List<TestCase> getTestcasesAlone() {
        if (isReusableScenario()) {
            return new ArrayList<>();
        }
        List<TestCase> testCasesAl = new ArrayList<>();
        for (TestCase testCase : testCases) {
            if (!testCase.isReusable()) {
                testCasesAl.add(testCase);
            }
        }
        return testCasesAl;
    }

    /**
     *
     * @return no of TestCases excluding Reusable
     */
    public int getTestcaseCount() {
        return getTestcasesAlone().size();
    }

    public int getTestCaseIndex(TestCase tn) {
        int count = -1;
        for (TestCase testCase : testCases) {
            if (!testCase.isReusable()) {
                count++;
            }
            if (tn.equals(testCase)) {
                break;
            }
        }
        return count;
    }

    public TestCase getTestCaseAt(int i) {
        return getTestcasesAlone().get(i);
    }

    /**
     * @return all the Reusables
     */
    public List<TestCase> getReusables() {
        if (isReusableScenario()) {
            return new ArrayList<>(testCases);
        }
        List<TestCase> testCasesR = new ArrayList<>();
        for (TestCase testCase : testCases) {
            if (testCase.isReusable()) {
                testCasesR.add(testCase);
            }
        }
        return testCasesR;
    }

    /**
     * Returns a reusable test case at the specified index.
     * @param i index
     * @return reusable test case
     */
    public TestCase getReusableAt(int i) {
        return getReusables().get(i);
    }

    /**
     * Returns the number of reusable test cases.
     * @return count of reusable test cases
     */
    public int getReusableCount() {
        return getReusables().size();
    }

    /**
     * Refactors (renames) a scenario reference across all test cases.
     * @param oldScenarioName old scenario name
     * @param newScenarioName new scenario name
     */
    public void refactorScenario(String oldScenarioName, String newScenarioName) {
        for (TestCase testcase : testCases) {
            testcase.refactorScenario(oldScenarioName, newScenarioName);
        }
    }

    /**
     * Refactors (renames) a test case reference across all test cases.
     * @param scenarioName scenario containing the test case
     * @param oldTestCaseName old test case name
     * @param newTestCaseName new test case name
     */
    public void refactorTestCase(String scenarioName, String oldTestCaseName, String newTestCaseName) {
        for (TestCase testcase : testCases) {
            testcase.refactorTestCase(scenarioName, oldTestCaseName, newTestCaseName);
        }
    }

    /**
     * Refactors (moves) a test case from one scenario to another.
     * @param testCaseName test case name
     * @param oldScenarioName old scenario name
     * @param newScenarioName new scenario name
     */
    public void refactorTestCaseScenario(String testCaseName, String oldScenarioName, String newScenarioName) {
        for (TestCase testcase : testCases) {
            testcase.refactorTestCaseScenario(testCaseName, oldScenarioName, newScenarioName);
        }
    }

    /**
     * Refactors (renames) an object reference across all test cases.
     * @param pageName page name containing the object
     * @param oldName old object name
     * @param newName new object name
     */
    public void refactorObjectName(String pageName, String oldName, String newName) {
        for (TestCase testCase : testCases) {
            testCase.refactorObjectName(pageName, oldName, newName);
        }
    }

    public void refactorObjectName(String oldpageName, String oldObjName, String newPageName, String newObjName) {
        for (TestCase testCase : testCases) {
            testCase.refactorObjectName(oldpageName, oldObjName, newPageName, newObjName);
        }
    }

    /**
     * Renames an object reference on the given page for the specified OR scope within this scenario,
     * by delegating to all test cases.
     *
     * @param scope    OR scope to match (e.g., shared vs project)
     * @param pageName page (screen) name containing the object reference
     * @param oldName  existing object name to replace
     * @param newName  new object name to apply
     */
    public void refactorObjectName(ORScope scope, String pageName, String oldName, String newName) {
        for (TestCase testCase : testCases) {
            testCase.refactorObjectName(scope, pageName, oldName, newName);
        }
    }

    public void refactorPageName(String oldPageName, String newPageName) {
        for (TestCase testCase : testCases) {
            testCase.refactorPageName(oldPageName, newPageName);
        }
    }

    public void refactorTestData(String oldTDName, String newTDName) {
        for (TestCase testCase : testCases) {
            testCase.refactorTestData(oldTDName, newTDName);
        }
    }

    public void refactorTestDataColumn(String testDataName, String oldColumnName, String newColumnName) {
        for (TestCase testCase : testCases) {
            testCase.refactorTestDataColumn(testDataName, oldColumnName, newColumnName);
        }
    }

    public List<TestCase> getImpactedObjectTestCases(String pageName, String objectName) {
        List<TestCase> impactedTestCases = new ArrayList<>();
        for (TestCase testCase : testCases) {
            TestCase impact = testCase.getImpactedObjectTestCases(pageName, objectName);
            if (impact != null) {
                impactedTestCases.add(impact);
            }
        }
        return impactedTestCases;
    }

    /**
     * Returns test cases that reference the specified test case.
     * @param scenarioName scenario name
     * @param testCaseName test case name
     * @return list of impacted test cases
     */
    public List<TestCase> getImpactedTestCaseTestCases(String scenarioName, String testCaseName) {
        List<TestCase> impactedTestCases = new ArrayList<>();
        for (TestCase testCase : testCases) {
            TestCase impact = testCase.getImpactedTestCaseTestCases(scenarioName, testCaseName);
            if (impact != null) {
                impactedTestCases.add(impact);
            }
        }
        return impactedTestCases;
    }

    /**
     * Returns test cases that reference the specified test data.
     * @param testDataName test data name
     * @return list of impacted test cases
     */
    public List<TestCase> getImpactedTestDataTestCases(String testDataName) {
        List<TestCase> impactedTestCases = new ArrayList<>();
        for (TestCase testCase : testCases) {
            TestCase impact = testCase.getImpactedTestDataTestCases(testDataName);
            if (impact != null) {
                impactedTestCases.add(impact);
            }
        }
        return impactedTestCases;
    }

    /**
     * Renames this scenario.
     * @param newName new scenario name
     * @return true if successful, false if a scenario with the new name already exists
     */
    @Override
    public Boolean rename(String newName) {
        if (getProject().getTestPlanScenarioByName(newName) == null) {
            if (FileUtils.renameFile(getLocation(), newName)) {
                getProject().refactorScenario(name, newName);
                name = newName;
                return true;
            }
        }
        return false;
    }
    /**
     * Renames this scenario.
     * @param newName new scenario name
     * @return true if successful, false if a scenario with the new name already exists
     */
    
    public Boolean renameReusable(String newName) {
        if (getProject().getReusableScenarioByName(newName) == null) {
            if (FileUtils.renameFile(getLocation(), newName)) {
                getProject().refactorScenario(name, newName);
                name = newName;
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Deletes this scenario from disk and removes it from the project.
     * @return true if successful, false otherwise
     */
    @Override
    public Boolean delete() {
        if (FileUtils.deleteFile(getLocation())) {
            getProject().removeScenario(this);
            return true;
        }
        return false;
    }
}