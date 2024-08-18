
package com.ing.datalib.settings;

/**
 *
 * 
 */
public class ExecutionSettings {

    private final RunSettings runSettings;

    private final TestMgmtSettings testMgmgtSettings;

    public ExecutionSettings(String location) {
        runSettings = new RunSettings(location);
        testMgmgtSettings = new TestMgmtSettings(location);
    }

    public RunSettings getRunSettings() {
        return runSettings;
    }

    public TestMgmtSettings getTestMgmgtSettings() {
        return testMgmgtSettings;
    }

    public void setLocation(String location) {
        runSettings.setLocation(location);
        testMgmgtSettings.setLocation(location);
    }

    public void save() {
        runSettings.save();
        testMgmgtSettings.save();
    }
}
