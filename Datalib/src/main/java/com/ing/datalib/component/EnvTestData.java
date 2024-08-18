
package com.ing.datalib.component;

import com.ing.datalib.testdata.TestDataFactory;
import com.ing.datalib.testdata.model.AbstractDataModel;
import com.ing.datalib.testdata.model.TestDataModel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class EnvTestData {

    private final Map<String, TestData> environmentTestData = new LinkedHashMap<>();
    private final Project sProject;
    private TestData defData;

    private final Properties environmentProperties;

    public EnvTestData(Project sProject) {
        this.sProject = sProject;
        environmentProperties = new Properties();
        load();
    }

    public Set<String> getEnvironments() {
        return environmentTestData.keySet();
    }

    private void load() {
        loadForEnv("Default");
        File file = new File(getPropertiesLocation());
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file);) {
                environmentProperties.load(fis);
                String env = environmentProperties.getProperty("Environment");
                if (env != null && !env.isEmpty()) {
                    String[] envs = env.split(",");
                    for (String env1 : envs) {
                        loadForEnv(env1);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(EnvTestData.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public String defEnv() {
        return "Default";
    }

    public TestData defData() {
        if (defData == null) {
            defData = environmentTestData.get(defEnv());
        }
        return defData;
    }

    public void createNewEnvironment(String envName) {
        loadForEnv(envName);
    }

    public void createNewEnvironment(String envName, String duplicateDataFromEnv,
            List<String> duplicateSheets, Boolean globalDataAsWell) {
        loadForEnv(envName);
        TestData newEnvTestData = getTestDataFor(envName);
        TestData dupEnvTestData = getTestDataFor(duplicateDataFromEnv);
        if (dupEnvTestData != null) {
            if (globalDataAsWell) {
                dupEnvTestData.getGlobalData().cloneAs(newEnvTestData.getGlobalData());
            }
            for (String duplicateSheet : duplicateSheets) {
                TestDataModel dupTestDataModel = dupEnvTestData.getByName(duplicateSheet);
                TestDataModel newTestDataModel = newEnvTestData.addTestData(newEnvTestData.getNewTestData(duplicateSheet));
                dupTestDataModel.cloneAs(newTestDataModel);
            }
        }
    }

    public Boolean renameEnvironment(String oldEnvName, String newEnvName) {
        TestData sTestData = getTestDataFor(oldEnvName);
        if (sTestData != null) {
            if (sTestData.rename(newEnvName)) {
                environmentTestData.put(newEnvName, environmentTestData.remove(oldEnvName));
                return true;
            }
        }
        return false;
    }

    public void deleteEnvironment(String envName) {
        TestData sTestData = getTestDataFor(envName);
        if (sTestData != null) {
            sTestData.deleteAll();
            environmentTestData.remove(envName);
        }
    }

    public void duplicateSheetsInOtherEnv(String envName, TestDataModel model) {
        for (Map.Entry<String, TestData> entry : environmentTestData.entrySet()) {
            String key = entry.getKey();
            TestData sTestData = entry.getValue();
            if (!key.equals(envName)) {
                if (sTestData.getByName(model.getName()) == null) {
                    TestDataModel newModel = sTestData.addTestData(sTestData.getNewTestData(model.getName()));
                    model.cloneAs(newModel);
                }
            }
        }
    }

    public void duplicateColumnInOtherEnv(String envName, TestDataModel model, List<String> columns) {
        for (Map.Entry<String, TestData> entry : environmentTestData.entrySet()) {
            String key = entry.getKey();
            TestData sTestData = entry.getValue();
            if (!key.equals(envName)) {
                TestDataModel tdModel = sTestData.getByName(model.getName());
                if (tdModel != null) {
                    for (String column : columns) {
                        tdModel.addColumn(column);
                    }
                }
            }
        }
    }

    public void duplicateRowsInOtherEnv(String envName, TestDataModel model, int[] rows) {
        for (Map.Entry<String, TestData> entry : environmentTestData.entrySet()) {
            String key = entry.getKey();
            TestData sTestData = entry.getValue();
            if (!key.equals(envName)) {
                TestDataModel tdModel = sTestData.getByName(model.getName());
                if (tdModel != null) {
                    for (int row : rows) {
                        tdModel.addRecord();
                        int tdRow = tdModel.getRowCount() - 1;
                        for (int i = 0; i < tdModel.getColumnCount(); i++) {
                            tdModel.setValueAt(
                                    model.getValueAt(row, i),
                                    tdRow, i);
                        }
                    }
                }
            }
        }
    }

    private void loadForEnv(String env) {
        TestData stestData = TestDataFactory.get(sProject.getTestdataType(), sProject, env);
        stestData.load();
        environmentTestData.put(env, stestData);
    }

    private String getPropertiesLocation() {
        return getLocation() + File.separator + "environment.properties";
    }

    public TestData getTestDataFor(String environment) {
        return environmentTestData.get(environment);
    }

    public void setTestDataFor(String environment, TestData testData) {
        environmentTestData.put(environment, testData);
    }

    public Collection<TestData> getAllEnvironments() {
        return environmentTestData.values();
    }

    public int getNoOfEnvironments() {
        return environmentTestData.size();
    }

    public Project getsProject() {
        return sProject;
    }

    public String getLocation() {
        return sProject.getLocation() + File.separator + "TestData";
    }

    public void save() {
        if (!new File(getLocation()).exists()) {
            new File(getLocation()).mkdirs();
        }
        for (TestData testData : getAllEnvironments()) {
            testData.save();
        }
        environmentProperties.put("Environment", getEnvironmentsAsString());
        try (FileOutputStream fout = new FileOutputStream(new File(getPropertiesLocation()));) {
            environmentProperties.store(fout, "List of Environments in comma seperated order");
        } catch (IOException ex) {
            Logger.getLogger(EnvTestData.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private String getEnvironmentsAsString() {
        List<String> eList = new ArrayList<>(environmentTestData.keySet());
        eList.remove("Default");
        String envs = "";
        for (String string : eList) {
            envs += string + ",";
        }
        return envs;
    }

    public TestDataModel getTestDataByName(String sheetName) {
        for (TestData sTestData : getAllEnvironments()) {
            if (sTestData.getByName(sheetName) != null) {
                return sTestData.getByName(sheetName);
            }
        }
        return null;
    }

    public void refactorScenario(String oldScenarioName, String newScenarioName) {
        for (TestData testData : getAllEnvironments()) {
            testData.refactorScenario(oldScenarioName, newScenarioName);
        }
    }

    public void refactorTestCase(String scenarioName, String oldTestCaseName, String newTestCaseName) {
        for (TestData testData : getAllEnvironments()) {
            testData.refactorTestCase(scenarioName, oldTestCaseName, newTestCaseName);
        }
    }

    public void refactorTestCaseScenario(String testCaseName, String oldScenarioName, String newScenarioName) {
        for (TestData testData : getAllEnvironments()) {
            testData.refactorTestCaseScenario(testCaseName, oldScenarioName, newScenarioName);
        }
    }

    public Boolean renameTestData(String oldName, String newName) {
        for (TestData testData : getAllEnvironments()) {
            if (testData.getByName(newName) != null) {
                return false;
            }
        }
        for (TestData testData : getAllEnvironments()) {
            if (testData.getByName(oldName) != null) {
                testData.getByName(oldName).rename(newName);
            }
        }
        sProject.refactorTestData(oldName, newName);
        return true;
    }

    public Boolean renameTestDataColumn(String tdName, String oldColName, String newColName) {
        for (TestData testData : getAllEnvironments()) {
            AbstractDataModel tData = testData.getByName(tdName);
            if (tData == null) {
                if (testData.getGlobalData().getName().equals(tdName)) {
                    tData = testData.getGlobalData();
                }
            }
            if (tData != null && tData.getColumnIndex(newColName) != -1) {
                return false;
            }
        }
        for (TestData testData : getAllEnvironments()) {
            AbstractDataModel tData = testData.getByName(tdName);
            if (tData == null) {
                if (testData.getGlobalData().getName().equals(tdName)) {
                    tData = testData.getGlobalData();
                }
            }
            if (tData != null) {
                tData.renameColumn(oldColName, newColName);
            }
        }
        sProject.refactorTestDataColumn(tdName, oldColName, newColName);
        return true;
    }

}
