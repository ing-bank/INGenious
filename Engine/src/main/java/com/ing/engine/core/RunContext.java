package com.ing.engine.core;

import com.ing.engine.drivers.PlaywrightDriverFactory.Browser;

public class RunContext {
    public String Scenario;
    public String TestCase;
    /**
     * Which scope the Scenario/TestCase names were resolved from when this run was
     * requested: "PROJECT", "SHARED", or "" if unknown (e.g. a CLI run by name only).
     * Lets {@code Task#getTestCase()} look up the exact intended test case directly,
     * instead of guessing via a Test Plan -> Project -> Shared priority search that
     * silently picks the wrong one when names collide across scopes.
     */
    public String ReusableScope = "";
    public String Description;
    public Browser Browser;
    public String BrowserName;
    public String BrowserVersion;
    public String Iteration;
    public String PlatformValue;
    public String BrowserVersionValue;
    public boolean useExistingDriver = false;

    public void print() {
        System.out.println(
            "[Scenario:" +
            Scenario +
            "] [TestCase: " +
            TestCase +
            "]" +
            " [Description: " +
            Description +
            "] [Browser: " +
            BrowserName +
            "] " +
            "[BrowserVersion: " +
            BrowserVersion +
            "] [Platform: " +
            System.getProperty("os.name") +
            "][ExistingBrowser: " +
            useExistingDriver +
            "]"
        );
    }

    public String getName() {
        return String.format("%s_%s_%s_%s", Scenario, TestCase, Iteration, BrowserName);
    }
}
