
package com.ing.datalib.component;

import com.ing.datalib.component.utils.FileUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * 
 */
public class Release extends DataModel {

    private final Project project;

    private final List<TestSet> testSets = new ArrayList<>();

    private String name;

    public Release(Project project, String name) {
        this.project = project;
        this.name = name;
        loadTestSets();
    }

    public String getLocation() {
        return project.getLocation() + File.separator + "TestLab" + File.separator + name;
    }

    public Project getProject() {
        return project;
    }

    public List<TestSet> getTestSets() {
        return testSets;
    }

    public TestSet getTestSetByName(String name) {
        for (TestSet testset : testSets) {
            if (testset.getName().equals(name)) {
                return testset;
            }
        }
        return null;
    }

    public int getIndexOfTestSetByName(String name) {
        for (int i = 0; i < testSets.size(); i++) {
            if (testSets.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    public TestSet addTestSet(String testSetName) {
        if (getTestSetByName(testSetName) == null) {
            TestSet tc = new TestSet(this, testSetName);
            testSets.add(tc);
            return tc;
        }
        return null;
    }

    public void removeTestSet(TestSet testSet) {
        if (testSets.remove(testSet)) {

        }
    }

    private void loadTestSets() {
        File relDir = new File(getLocation());
        if (relDir.exists()) {
            for (String testSet : relDir.list(FileUtils.CSV_FILTER)) {
                testSets.add(new TestSet(this, testSet));
            }
        }
    }

    public void loadTestSetsTableModel() {
        for (TestSet testSet : testSets) {
            testSet.loadTestSetTableModel();
        }
    }

    @Override
    public void loadTableModel() {
        loadTestSetsTableModel();
    }

    public void save() {
        for (TestSet testSet : testSets) {
            testSet.save();
        }
    }

    public String getName() {
        return name;
    }

    @Override
    public int getRowCount() {
        return testSets.size();
    }

    @Override
    public int getColumnCount() {
        int max = 0;
        if (!testSets.isEmpty()) {
            max = 1;
            for (TestSet testSet : testSets) {
                int group = testSet.getTestSteps().size();
                max = group > max ? group : max;
            }
        }
        return max + 1;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == 0) {
            return "TestSet";
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
            return testSets.get(rowIndex).getName();
        }
        return testSets.get(rowIndex).getValueAt(columnIndex - 1, 3);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0) {
//            testSets.get(rowIndex).setName(aValue.toString());
//            project.reload(testSets.get(rowIndex));
        } else {
            testSets.get(rowIndex).setValueAt(aValue, columnIndex - 1, 3);
        }
    }

    @Override
    public Boolean addRow() {
        return false;
    }

    @Override
    public void removeRow(int rows) {
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
                .append("Release - ")
                .append(name)
                .append("\n")
                .append("\t")
                .append("TestSet - ")
                .append(testSets.size())
                .append("\n");
        for (TestSet testSet : testSets) {
            builder.append(testSet.printString());
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return name;
    }

    public void refactorScenario(String oldScenarioName, String newScenarioName) {
        for (TestSet testSet : testSets) {
            testSet.refactorScenario(oldScenarioName, newScenarioName);
        }
    }

    public void refactorTestCase(String scenarioName, String oldTestCaseName, String newTestCaseName) {
        for (TestSet testSet : testSets) {
            testSet.refactorTestCase(scenarioName, oldTestCaseName, newTestCaseName);
        }
    }

    public void refactorTestCaseScenario(String testCaseName, String oldScenarioName, String newScenarioName) {
        for (TestSet testSet : testSets) {
            testSet.refactorTestCaseScenario(testCaseName, oldScenarioName, newScenarioName);
        }
    }

    @Override
    public Boolean rename(String newName) {
        if (getProject().getReleaseByName(newName) == null) {
            if (FileUtils.renameFile(getLocation(), newName)) {
                name = newName;
                for (TestSet testSet : testSets) {
                    testSet.resetExecSettingsLocation();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Boolean delete() {
        if (FileUtils.deleteFile(getLocation())) {
            getProject().removeRelease(this);
            return true;
        }
        return false;
    }

}
