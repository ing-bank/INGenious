
package com.ing.engine.core;

import com.ing.engine.drivers.PlaywrightDriverFactory.Browser;

public class RunContext {

    public String Scenario;
    public String TestCase;
    public String Description;
    public Browser Browser;
    public String BrowserName;
    public String BrowserVersion;
    public String Iteration;
    public String PlatformValue;
    public String BrowserVersionValue;
    public boolean useExistingDriver = false;

    public void print() {
        System.out.println("[Scenario:" + Scenario + "] [TestCase: " + TestCase + "]"
                + " [Description: " + Description + "] [Browser: " + BrowserName + "] "
                + "[BrowserVersion: " + BrowserVersion + "] [Platform: " + System.getProperty("os.name")
                + "][ExistingBrowser: " + useExistingDriver + "]"
        );
    }
    
    
    public String getName(){
        return String.format("%s_%s_%s_%s", Scenario,TestCase,Iteration,BrowserName);
    }

}
