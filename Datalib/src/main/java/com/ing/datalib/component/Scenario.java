
package com.ing.datalib.component;

import com.ing.datalib.component.utils.FileUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 
 */
public class Scenario extends DataModel {

    private final Project project;

    private final List<TestCase> testCases = new ArrayList<>();

    private String name;

    public Scenario(Project project, String name) {
        this.project = project;
        this.name = name;
        loadTestcases();
    }

    public String getLocation() {
        return project.getLocation() + File.separator + "TestPlan" + File.separator + name;
    }

    public Project getProject() {
        return project;
    }

    public List<TestCase> getTestCases() {
        return testCases;
    }

    public TestCase getTestCaseByName(String name) {
        for (TestCase testcase : testCases) {
            if (testcase.getName().equalsIgnoreCase(name)) {
                return testcase;
            }
        }
        return null;
    }

    public int getIndexOfTestCaseByName(String name) {
        for (int i = 0; i < testCases.size(); i++) {
            if (testCases.get(i).getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    private void loadTestcases() {
        File scenDir = new File(getLocation());
        if (scenDir.exists()) {
            for (String testCase : scenDir.list(FileUtils.CSV_FILTER)) {
                testCases.add(new TestCase(this, testCase));
            }
        }
    }

    public void loadTestCasesTableModel() {
        for (TestCase testCase : testCases) {
            testCase.loadTestCaseTableModel();
        }
    }

    @Override
    public void loadTableModel() {
        loadTestCasesTableModel();
    }

    public TestCase addTestCase(String testCaseName) {
        if (getTestCaseByName(testCaseName) == null) {
            TestCase tc = new TestCase(this, testCaseName);
            testCases.add(tc);
            tc.setSaved(false);
            return tc;
        }
        return null;
    }

    public void removeTestCase(TestCase testCase) {
        if (testCases.remove(testCase)) {

        }
    }

    public void save() {
        for (TestCase testCase : testCases) {
            testCase.save();
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public int getRowCount() {
        return getTestcaseCount();
    }

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

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return "TestCase";
        }
        return "Component " + columnIndex;
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
        if (columnIndex == 0) {
            return getTestcasesAlone().get(rowIndex).getName();
        }
        return getTestcasesAlone().get(rowIndex).getValueAt(columnIndex - 1, 3);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
//            testCases.get(rowIndex).setName(aValue.toString());
//            project.reload(testCases.get(rowIndex));
        } else {
            getTestcasesAlone().get(rowIndex).setValueAt(aValue, columnIndex - 1, 3);
        }
    }

    @Override
    public Boolean addRow() {
        return false;
    }

    @Override
    public void removeRow(int row) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void insertRow(int row, Object[] values) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

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

    @Override
    public String toString() {
        return name;
    }

    /**
     *
     * @return all the TestCases without Reusable
     */
    public List<TestCase> getTestcasesAlone() {
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
        List<TestCase> testCasesR = new ArrayList<>();
        for (TestCase testCase : testCases) {
            if (testCase.isReusable()) {
                testCasesR.add(testCase);
            }
        }
        return testCasesR;
    }

    public TestCase getReusableAt(int i) {
        return getReusables().get(i);
    }

    public int getReusableCount() {
        return getReusables().size();
    }

    public void refactorScenario(String oldScenarioName, String newScenarioName) {
        for (TestCase testcase : testCases) {
            testcase.refactorScenario(oldScenarioName, newScenarioName);
        }
    }

    public void refactorTestCase(String scenarioName, String oldTestCaseName, String newTestCaseName) {
        for (TestCase testcase : testCases) {
            testcase.refactorTestCase(scenarioName, oldTestCaseName, newTestCaseName);
        }
    }

    public void refactorTestCaseScenario(String testCaseName, String oldScenarioName, String newScenarioName) {
        for (TestCase testcase : testCases) {
            testcase.refactorTestCaseScenario(testCaseName, oldScenarioName, newScenarioName);
        }
    }

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

    @Override
    public Boolean rename(String newName) {
        if (getProject().getScenarioByName(newName) == null) {
            if (FileUtils.renameFile(getLocation(), newName)) {
                getProject().refactorScenario(name, newName);
                name = newName;
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean delete() {
        if (FileUtils.deleteFile(getLocation())) {
            getProject().removeScenario(this);
            return true;
        }
        return false;
    }

}
