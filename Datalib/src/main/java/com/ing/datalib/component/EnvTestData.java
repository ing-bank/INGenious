package com.ing.datalib.component;

import com.ing.datalib.testdata.TestDataFactory;
import com.ing.datalib.testdata.model.AbstractDataModel;
import com.ing.datalib.testdata.model.TestDataModel;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

    public void createNewEnvironment(
        String envName,
        String duplicateDataFromEnv,
        List<String> duplicateSheets,
        Boolean globalDataAsWell
    ) {
        loadForEnv(envName);
        TestData newEnvTestData = getTestDataFor(envName);
        TestData dupEnvTestData = getTestDataFor(duplicateDataFromEnv);
        if (dupEnvTestData != null) {
            if (globalDataAsWell) {
                dupEnvTestData.getGlobalData().cloneAs(newEnvTestData.getGlobalData());
            }
            for (String duplicateSheet : duplicateSheets) {
                TestDataModel dupTestDataModel = dupEnvTestData.getByName(duplicateSheet);
                TestDataModel newTestDataModel = newEnvTestData.addTestData(
                    newEnvTestData.getNewTestData(duplicateSheet)
                );
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
                if (sTestData.getByNameIgnoreCase(model.getName()) == null) {
                    TestDataModel newModel = sTestData.addTestData(
                        sTestData.getNewTestData(model.getName())
                    );
                    model.cloneAs(newModel);
                }
            }
        }
    }

    public void duplicateColumnInOtherEnv(
        String envName,
        TestDataModel model,
        List<String> columns
    ) {
        for (Map.Entry<String, TestData> entry : environmentTestData.entrySet()) {
            String key = entry.getKey();
            TestData sTestData = entry.getValue();
            if (!key.equals(envName)) {
                TestDataModel tdModel = sTestData.getByNameIgnoreCase(model.getName());
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
                TestDataModel tdModel = sTestData.getByNameIgnoreCase(model.getName());
                if (tdModel != null) {
                    for (int row : rows) {
                        tdModel.addRecord();
                        int tdRow = tdModel.getRowCount() - 1;
                        for (int i = 0; i < tdModel.getColumnCount(); i++) {
                            tdModel.setValueAt(model.getValueAt(row, i), tdRow, i);
                        }
                    }
                }
            }
        }
    }

    private void loadForEnv(String env) {
        TestData stestData = TestDataFactory.get(sProject.getTestdataType(), sProject, env);
        // Propagate read-only mode to prevent datasheet migrations during validation
        stestData.setReadOnlyMode(sProject.isReadOnlyMode());
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
        saveProperties(environmentProperties, getPropertiesLocation());

        // Track shared reusables used in test data
        try {
            updateSharedReusableProjectsItemsFromTestData(sProject, this);
        } catch (Exception ex) {
            Logger
                .getLogger(EnvTestData.class.getName())
                .log(
                    Level.WARNING,
                    "Failed to update shared reusable projects items from test data",
                    ex
                );
        }
    }

    private static void saveProperties(Properties prop, String filename) {
        File file = new File(filename);
        try (
            BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), "ISO_8859_1")
            )
        ) {
            synchronized (prop) {
                for (Map.Entry<Object, Object> e : prop.entrySet()) {
                    String key = (String) e.getKey();
                    String val = (String) e.getValue();
                    bw.write(key + "=" + val);
                    bw.newLine();
                }
            }
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
            if (sTestData.getByNameIgnoreCase(sheetName) != null) {
                return sTestData.getByNameIgnoreCase(sheetName);
            }
        }
        return null;
    }

    public void refactorScenario(String oldScenarioName, String newScenarioName) {
        for (TestData testData : getAllEnvironments()) {
            testData.refactorScenario(oldScenarioName, newScenarioName);
        }
    }

    public void refactorTestCase(
        String scenarioName,
        String oldTestCaseName,
        String newTestCaseName
    ) {
        for (TestData testData : getAllEnvironments()) {
            testData.refactorTestCase(scenarioName, oldTestCaseName, newTestCaseName);
        }
    }

    public void refactorTestCaseScenario(
        String testCaseName,
        String oldScenarioName,
        String newScenarioName
    ) {
        for (TestData testData : getAllEnvironments()) {
            testData.refactorTestCaseScenario(testCaseName, oldScenarioName, newScenarioName);
        }
    }

    /**
     * Updates the Scope of the test data entry for a scenario+testcase across all environments, as
     * part of an explicit test case conversion between Test Plan / Project Reusables / Shared Reusables.
     */
    public void updateScope(
        String scenarioName,
        String testCaseName,
        String oldScope,
        String newScope
    ) {
        for (TestData testData : getAllEnvironments()) {
            testData.updateScope(scenarioName, testCaseName, oldScope, newScope);
        }
    }

    /**
     * Find all environments that contain a datasheet with the given name.
     *
     * @param datasheetName the name of the datasheet to search for
     * @return list of environment names where the datasheet exists
     */
    public List<String> findEnvironmentsWithDatasheet(String datasheetName) {
        List<String> environments = new ArrayList<>();
        for (Map.Entry<String, TestData> entry : environmentTestData.entrySet()) {
            TestData testData = entry.getValue();
            if (testData.getByNameIgnoreCase(datasheetName) != null) {
                environments.add(entry.getKey());
            }
        }
        return environments;
    }

    /**
     * Find environments other than the current one that contain a datasheet with the given name.
     *
     * @param datasheetName the name of the datasheet to search for
     * @param currentEnv the current environment to exclude from search
     * @return list of environment names (excluding currentEnv) where the datasheet exists
     */
    public List<String> findOtherEnvironmentsWithDatasheet(
        String datasheetName,
        String currentEnv
    ) {
        List<String> environments = new ArrayList<>();
        for (Map.Entry<String, TestData> entry : environmentTestData.entrySet()) {
            String envName = entry.getKey();
            TestData testData = entry.getValue();
            if (
                !envName.equals(currentEnv) && testData.getByNameIgnoreCase(datasheetName) != null
            ) {
                environments.add(envName);
            }
        }
        return environments;
    }

    /**
     * Rename a datasheet in a single environment only.
     *
     * @param oldName current datasheet name
     * @param newName new datasheet name
     * @param envName environment where rename should occur
     * @return true if rename was successful, false if newName already exists
     */
    public Boolean renameTestData(String oldName, String newName, String envName) {
        TestData ntestData = sProject.getTestData().getTestDataFor(envName);
        TestDataModel existingByNewName = ntestData.getByNameIgnoreCase(newName);
        if (existingByNewName != null && !existingByNewName.getName().equals(oldName)) {
            return false;
        }

        for (TestData testData : getAllEnvironments()) {
            if (testData.getByName(oldName) != null && testData.getEnviroment().equals(envName)) {
                testData.getByName(oldName).rename(newName);
            }
        }
        sProject.refactorTestData(oldName, newName);
        return true;
    }

    /**
     * Rename a datasheet across multiple specified environments.
     *
     * @param oldName current datasheet name
     * @param newName new datasheet name
     * @param environments list of environment names where rename should occur
     * @return true if rename was successful across all environments, false if newName already exists in any
     */
    public Boolean renameTestDataAcrossEnvironments(
        String oldName,
        String newName,
        List<String> environments
    ) {
        // First check if newName already exists in any of the target environments
        for (String envName : environments) {
            TestData testData = getTestDataFor(envName);
            TestDataModel existingByNewName = testData == null
                ? null
                : testData.getByNameIgnoreCase(newName);
            if (existingByNewName != null && !existingByNewName.getName().equals(oldName)) {
                return false;
            }
        }

        // Perform the rename in all specified environments
        for (String envName : environments) {
            TestData testData = getTestDataFor(envName);
            if (testData != null && testData.getByName(oldName) != null) {
                testData.getByName(oldName).rename(newName);
            }
        }

        // Refactor references in test cases
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

    /**
     * Scans test data for shared reusable scenario references and updates projects.items file.
     * Collects scenario names from all test data records and checks if they are shared reusables.
     */
    private static void updateSharedReusableProjectsItemsFromTestData(
        Project project,
        EnvTestData envTestData
    ) {
        Set<String> sharedReusableNames = new java.util.HashSet<>();

        // Scan all test data environments and records for scenario names
        for (TestData testData : envTestData.getAllEnvironments()) {
            for (TestDataModel model : testData.getTestDataList()) {
                // Get all records from the model
                for (com.ing.datalib.testdata.model.Record record : model.getRecords()) {
                    String scenarioName = record.getScenario();
                    // Check if this scenario exists and is a shared reusable
                    if (scenarioName != null && !scenarioName.trim().isEmpty()) {
                        Scenario scenario = project.getScenarioByName(scenarioName);
                        if (scenario != null && scenario.isSharedReusableScenario()) {
                            sharedReusableNames.add(scenarioName);
                        }
                    }
                }
            }
        }

        // If shared reusables found in test data, update projects.items
        if (!sharedReusableNames.isEmpty()) {
            TestCase.updateSharedReusableProjectsItems(project, sharedReusableNames);
        }
    }
}
