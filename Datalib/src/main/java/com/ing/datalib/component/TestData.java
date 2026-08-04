package com.ing.datalib.component;

import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.testdata.model.GlobalDataModel;
import com.ing.datalib.testdata.model.TestDataModel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 *
 */
public abstract class TestData {
    private List<TestDataModel> testDataList;

    private GlobalDataModel globalData;

    private final Project sProject;

    private String enviroment;

    /**
     * When true, skips datasheet migrations to ensure read-only operation (e.g., during validation).
     * Set by the parent Project when loaded in read-only mode.
     */
    private boolean readOnlyMode = false;

    public TestData(Project sProject, String enviroment) {
        this.sProject = sProject;
        this.enviroment = enviroment;
        testDataList = new ArrayList<>();
    }

    public abstract void load();

    /**
     * Sets the read-only mode for this TestData instance.
     * When true, prevents datasheet migrations during load.
     *
     * @param readOnly true to enable read-only mode
     */
    public void setReadOnlyMode(boolean readOnly) {
        this.readOnlyMode = readOnly;
    }

    /**
     * Returns whether this TestData is in read-only mode.
     *
     * @return true if in read-only mode
     */
    protected boolean isReadOnlyMode() {
        return readOnlyMode;
    }

    public void save() {
        for (TestDataModel tData : testDataList) {
            tData.save();
        }
        globalData.save();
        clearView();
    }

    public void clearView() {
        globalData.view().clear();
        for (TestDataModel tData : testDataList) {
            tData.view().clear();
        }
    }

    public String getLocation() {
        return (
            sProject.getLocation() +
            File.separator +
            "TestData" +
            (getEnviroment().equals("Default") ? "" : File.separator + getEnviroment())
        );
    }

    public List<TestDataModel> getTestDataList() {
        return testDataList;
    }

    public Boolean rename(String newName) {
        if (FileUtils.renameFile(getLocation(), newName)) {
            enviroment = newName;
            return true;
        }
        return false;
    }

    public void deleteAll() {
        globalData.delete();
        for (TestDataModel testDataModel : testDataList) {
            testDataModel.delete();
        }
        testDataList.clear();
        globalData = null;
    }

    public TestDataModel addTestData() {
        String name = "TestData";
        int i = 0;
        String tdName = name + i;
        while (hasTestDataNameIgnoreCase(tdName)) {
            tdName = name + ++i;
        }
        return addTestData(getNewTestData(tdName));
    }

    public Boolean deleteTestData(String name) {
        TestDataModel model = getByName(name);
        if (model != null && model.delete()) {
            testDataList.remove(model);
            return true;
        }
        return false;
    }

    public List<String> getTestDataNames() {
        List<String> names = new ArrayList<>();
        for (TestDataModel tData : testDataList) {
            names.add(tData.getName());
        }
        names.add(globalData.getName());
        return names;
    }

    public TestDataModel getByName(String name) {
        for (TestDataModel tData : testDataList) {
            if (tData.getName().equals(name)) {
                return tData;
            }
        }
        return null;
    }

    public TestDataModel getByNameIgnoreCase(String name) {
        if (name == null) {
            return null;
        }
        for (TestDataModel tData : testDataList) {
            if (tData.getName().equalsIgnoreCase(name)) {
                return tData;
            }
        }
        return null;
    }

    public boolean hasTestDataNameIgnoreCase(String name) {
        return getByNameIgnoreCase(name) != null;
    }

    public abstract TestDataModel getNewTestData(String name);

    public abstract TestDataModel importTestData(File file);

    public TestDataModel addTestData(TestDataModel testData) {
        testDataList.add(testData);
        return testData;
    }

    public void setTestDataList(List<TestDataModel> testDataList) {
        this.testDataList = testDataList;
    }

    public GlobalDataModel getGlobalData() {
        return globalData;
    }

    public void setGlobalData(GlobalDataModel globalData) {
        this.globalData = globalData;
    }

    public Project getsProject() {
        return sProject;
    }

    public String getEnviroment() {
        return enviroment;
    }

    public void refactorScenario(String oldScenarioName, String newScenarioName) {
        for (TestDataModel testDataList1 : testDataList) {
            testDataList1.refactorScenario(oldScenarioName, newScenarioName);
        }
    }

    public void refactorTestCase(
        String scenarioName,
        String oldTestCaseName,
        String newTestCaseName
    ) {
        for (TestDataModel testDataList1 : testDataList) {
            testDataList1.refactorTestCase(scenarioName, oldTestCaseName, newTestCaseName);
        }
    }

    public void refactorTestCaseScenario(
        String testCaseName,
        String oldScenarioName,
        String newScenarioName
    ) {
        for (TestDataModel testDataList1 : testDataList) {
            testDataList1.refactorTestCaseScenario(testCaseName, oldScenarioName, newScenarioName);
        }
    }

    public void updateScope(
        String scenarioName,
        String testCaseName,
        String oldScope,
        String newScope
    ) {
        for (TestDataModel testDataList1 : testDataList) {
            testDataList1.updateScope(scenarioName, testCaseName, oldScope, newScope);
        }
    }
}
