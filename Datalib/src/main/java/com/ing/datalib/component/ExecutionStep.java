
package com.ing.datalib.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.csv.CSVRecord;

/**
 *
 * 
 */
public class ExecutionStep {

    public enum HEADERS {

        Execute(0), TestScenario(1), TestCase(2), Iteration(3), Status(4),
        Browser(5), BrowserVersion(6), Platform(7);

        private final int index;

        private HEADERS(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public static List<String> getValues() {
            List<String> list = new ArrayList<>();
            for (HEADERS header : HEADERS.values()) {
                list.add(header.name());
            }
            return list;
        }

        public static int size() {
            return HEADERS.values().length;
        }

    }

    private final TestSet testSet;

    List<String> exeStepDetails = new ArrayList<>(HEADERS.values().length);

    Map<String, Object> userData = new HashMap<>();

    public ExecutionStep(TestSet testSet, CSVRecord record) {
        this.testSet = testSet;
        loadStep(record);
    }

    public ExecutionStep(TestSet testSet) {
        this.testSet = testSet;
        loadEmptyStep();
    }

    public String getExecute() {
        return exeStepDetails.get(HEADERS.Execute.getIndex());
    }

    public void setExecute(String object) {
        exeStepDetails.set(HEADERS.Execute.getIndex(), object);
    }

    public String getTestScenarioName() {
        return exeStepDetails.get(HEADERS.TestScenario.getIndex());
    }

    public ExecutionStep setTestScenario(String testScenario) {
        exeStepDetails.set(HEADERS.TestScenario.getIndex(), testScenario);
        return this;
    }

    public String getTestCaseName() {
        return exeStepDetails.get(HEADERS.TestCase.getIndex());
    }

    public ExecutionStep setTestCase(String testCase) {
        exeStepDetails.set(HEADERS.TestCase.getIndex(), testCase);
        return this;
    }

    public String getIteration() {
        return exeStepDetails.get(HEADERS.Iteration.getIndex());
    }

    public ExecutionStep setIteration(String iteration) {
        exeStepDetails.set(HEADERS.Iteration.getIndex(), iteration);
        return this;
    }

    public String getStatus() {
        return exeStepDetails.get(HEADERS.Status.getIndex());
    }

    public ExecutionStep setStatus(String status) {
        exeStepDetails.set(HEADERS.Status.getIndex(), status);
        return this;
    }

    public String getPlatform() {
        return exeStepDetails.get(HEADERS.Platform.getIndex());
    }

    public ExecutionStep setPlatform(String platform) {
        exeStepDetails.set(HEADERS.Platform.getIndex(), platform);
        return this;
    }

    public String getBrowser() {
        return exeStepDetails.get(HEADERS.Browser.getIndex());
    }

    public ExecutionStep setBrowser(String browser) {
        exeStepDetails.set(HEADERS.Browser.getIndex(), browser);
        return this;
    }

    public String getBrowserVersion() {
        return exeStepDetails.get(HEADERS.BrowserVersion.getIndex());
    }

    public ExecutionStep setBrowserVersion(String browserVersion) {
        exeStepDetails.set(HEADERS.BrowserVersion.getIndex(), browserVersion);
        return this;
    }

    public Project getProject() {
        return testSet.getProject();
    }

    public TestSet getTestSet() {
        return testSet;
    }

    private void loadStep(CSVRecord record) {
        for (int i = 0; i < record.size(); i++) {
            exeStepDetails.add(record.get(i));
        }
    }

    private void loadEmptyStep() {
        for (HEADERS value : HEADERS.values()) {
            exeStepDetails.add("");
        }
        setExecute("true");
        setBrowserVersion("Default");
        setPlatform("Any");
        setStatus("NoRun");
        setIteration("Single");
    }

    public String getValueAt(int index) {
        return exeStepDetails.get(index);
    }

    public String getValueBy(String header) {
        return exeStepDetails.get(HEADERS.valueOf(header).getIndex());
    }

    public void putValueAt(int index, String value) {
        exeStepDetails.set(index, value);
    }

    public int size() {
        return exeStepDetails.size();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ExecutionStep - ")
                .append(this.getTestScenarioName())
                .append(" | ")
                .append(this.getTestCaseName())
                .append(" | ")
                .append(this.getBrowser());
        return builder.toString();
    }

    public void clearValues() {
        for (int i = 0; i < size(); i++) {
            putValueAt(i, "");
        }
    }

    public Boolean isEmpty() {
        for (String val : exeStepDetails) {
            if (!val.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public TestCase getTestCase() {
        Scenario scenario = testSet.getProject().getScenarioByName(getTestScenarioName());
        if (scenario != null) {
            return scenario.getTestCaseByName(getTestCaseName());
        }
        return null;
    }

    public void copyValuesTo(ExecutionStep step) {
        for (int i = 0; i < size(); i++) {
            step.putValueAt(i, getValueAt(i));
        }
    }

    public Boolean isDuplicate(ExecutionStep step) {
        return Objects.deepEquals(step.exeStepDetails, exeStepDetails);
    }

    public Map<String, Object> getUserData() {
        return userData;
    }

}
