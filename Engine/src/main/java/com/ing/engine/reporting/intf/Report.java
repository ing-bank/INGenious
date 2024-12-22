
package com.ing.engine.reporting.intf;

import com.ing.engine.core.RunContext;
import com.ing.engine.drivers.WebDriverCreation;
import com.ing.engine.drivers.PlaywrightDriverCreation;
import com.ing.engine.support.Status;
import com.ing.engine.support.Step;
import java.io.File;
import java.util.List;

public interface Report {

    public void createReport(RunContext runContext, String runTime);

    public void updateTestLog(String stepName, String stepDescription, Status state, String link, List<String> links);

    public Status finalizeReport();

    public void startComponent(String component,String desc);

    public void startIteration(int iteration);

    public void endComponent(String string);

    public void endIteration(int iteration);

    public PlaywrightDriverCreation getPlaywrightDriver();
    
    public WebDriverCreation getWebDriver();

    public String getScreenShotName();
    
    public String getNewScreenShotName();

    public File getReportLoc();

    public Step getStep();

    public int getStepCount();

}
