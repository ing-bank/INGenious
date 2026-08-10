package com.ing.engine.settings;

import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestSet;
import com.ing.datalib.settings.AbstractPropSettings;

/**
 *
 *
 */
public class GlobalSettings extends AbstractPropSettings {
    private static final long serialVersionUID = -431006539190776440L;

    public GlobalSettings(String location) {
        super(location, "Global Settings");
    }

    public void setFor(TestCase testCase, String browser) {
        setTestRun(true);
        setProjectPath(testCase.getProject().getLocation());
        setProjectName(testCase.getProject().getName());
        setScenario(testCase.getScenario().getName());
        setTestCase(testCase.getName());
        // Capture WHICH scope this exact TestCase was opened from (Test Plan, Project
        // Reusable, or Shared Reusable) so a standalone run can look it up unambiguously
        // later, even when a same-named scenario/testcase exists in another scope.
        setReusableScope(reusableScopeOf(testCase));
        setBrowser(browser);
        save();
    }

    private String reusableScopeOf(TestCase testCase) {
        Scenario scn = testCase.getScenario();
        if (scn == null || scn.getSource() == null) {
            return "";
        }
        switch (scn.getSource()) {
            case REUSABLE_COMPONENTS:
                return "PROJECT";
            case SHARED_REUSABLE_COMPONENTS:
                return "SHARED";
            case TEST_PLAN:
            default:
                return "";
        }
    }

    public void setFor(TestSet testSet) {
        setTestRun(false);
        setProjectPath(testSet.getProject().getLocation());
        setProjectName(testSet.getProject().getName());
        setRelease(testSet.getRelease().getName());
        setTestSet(testSet.getName());
        //setRPUpdate(true);
        save();
    }

    public String getProjectPath() {
        return getProperty("ProjectPath");
    }

    public void setProjectPath(String value) {
        setProperty("ProjectPath", value);
    }

    public String getProjectName() {
        return getProperty("ProjectName");
    }

    public void setProjectName(String value) {
        setProperty("ProjectName", value);
    }

    public Boolean isTestRun() {
        return Boolean.valueOf(getProperty("TestRun", "true"));
    }

    public void setTestRun(Boolean value) {
        setProperty("TestRun", String.valueOf(value));
    }

    public String getScenario() {
        return getProperty("Scenario");
    }

    public void setScenario(String value) {
        setProperty("Scenario", value);
        // Reset any stale scope hint from a previous run (e.g. a CLI run that sets the
        // scenario/testcase by name only, with no TestCase object to derive scope from).
        // setFor(TestCase, ...) re-applies the correct hint right after calling this.
        setReusableScope("");
    }

    public String getTestCase() {
        return getProperty("TestCase");
    }

    public void setTestCase(String value) {
        setProperty("TestCase", value);
    }

    /**
     * Scope of the TestCase last set via {@link #setFor(TestCase, String)}: "PROJECT",
     * "SHARED", or "" for a Test Plan test case / when unknown (e.g. CLI runs by name).
     */
    public String getReusableScope() {
        return getProperty("ReusableScope", "");
    }

    public void setReusableScope(String value) {
        setProperty("ReusableScope", value);
    }

    public String getBrowser() {
        return getProperty("Browser");
    }

    public void setBrowser(String value) {
        setProperty("Browser", value);
    }

    public String getRelease() {
        return getProperty("Release");
    }

    public void setRelease(String value) {
        setProperty("Release", value);
    }

    public String getTestSet() {
        return getProperty("TestSet");
    }

    public String getTags() {
        return getProperty("Tags");
    }

    public void setTestSet(String value) {
        setProperty("TestSet", value);
    }

    public Boolean isRPUpdate() {
        return Boolean.valueOf(getProperty("ReportPortal", "false"));
    }

    public void setRPUpdate(Boolean value) {
        setProperty("ReportPortal", String.valueOf(value));
    }

    public void setTags(String value) {
        setProperty("Tags", value);
    }
}
