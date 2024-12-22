package com.ing.engine.drivers;

import com.ing.datalib.settings.emulators.Emulator;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.drivers.PlaywrightDriverFactory.Browser;
import com.ing.engine.execution.exception.DriverClosedException;
import com.ing.engine.execution.exception.UnCaughtException;
import com.ing.engine.constants.FilePath;
import com.ing.engine.reporting.util.DateTimeUtils;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.microsoft.playwright.*;
import java.nio.file.Paths;

/**
 * Class to handle driver related operation
 *
 */
public class PlaywrightDriverCreation {

    public Playwright playwright;
    public Page page;
    public BrowserContext browserContext;
    protected RunContext runContext;

    public Page getPage() {
        return page;
    }

    public void setPage(Page page) {
        this.page = page;
    }
    public void launchDriver(RunContext context) throws UnCaughtException {
        runContext = context;
        System.out.println("\n🚀 Launching " + runContext.BrowserName+"\n");
        try {
            playwright = PlaywrightDriverFactory.createPlaywright();
            
            if(Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().containsKey("testIdAttribute"))
            {
             playwright.selectors().setTestIdAttribute(Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().getProperty("testIdAttribute"));
            }
            
            BrowserType browserType = (BrowserType) PlaywrightDriverFactory.createBrowserType(playwright,runContext.BrowserName, context, Control.getCurrentProject().getProjectSettings());
            if (Control.exe.getExecSettings().getRunSettings().isGridExecution()) {
                System.out.println("🚀 Launching Remote Driver \n");
                browserContext = PlaywrightDriverFactory.createContext(true, browserType, runContext.BrowserName, Control.getCurrentProject().getProjectSettings(), runContext);
            } else {
                System.out.println("🚀 Launching Local Driver \n");
                browserContext = PlaywrightDriverFactory.createContext(false, browserType, runContext.BrowserName, Control.getCurrentProject().getProjectSettings(), runContext);

            }

            page = PlaywrightDriverFactory.createPage(browserContext);

        } catch (UnCaughtException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            throw new UnCaughtException("[Playwright Browser Exception] Cannot Initiate Browser " + ex.getMessage());
        }
    }

    public void launchDriver(String browser) throws UnCaughtException {
        RunContext context = new RunContext();
        context.BrowserName = browser;
        context.Browser = Browser.fromString(browser);
        context.PlatformValue = System.getProperty("os.name");
        context.BrowserVersion = "default";
        launchDriver(context);
    }

    public void closeBrowserContext() {

        Boolean isTracingEnabled = Control.exe.getExecSettings().getRunSettings().isTracingEnabled();
        if (isTracingEnabled) {
            System.out.println("Tracing Stopped");
            browserContext.tracing().stop(new Tracing.StopOptions().setPath(Paths.get(FilePath.getCurrentResultsPath() + File.separator
                    + "traces"
                    + File.separator
                    + runContext.Scenario
                    + "_"
                    + runContext.TestCase
                    + "_"
                    + DateTimeUtils.TimeNowForFolder()
                    + File.separator
                    + "traces.zip")));
        }
        browserContext.close();
    }

    public void RestartBrowser() throws UnCaughtException {
        StopBrowser();
        StartBrowser(runContext.BrowserName);
    }

    public void StopBrowser() {
        try {

            closeBrowserContext();
            page.close();

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
        }
        this.page = null;
        browserContext = null;
    }

    public void StartBrowser(String b) throws UnCaughtException {
        StopBrowser();
        launchDriver(b);
    }

    public void closeBrowser() {
        if (this.page != null) {
            try {
                closeBrowserContext();
                this.page.close();
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.OFF, "Couldn't Kill the Driver", ex);
            }
            page = null;
        }
    }

    public String getCurrentBrowser() {
        return runContext.BrowserName;
    }

    public String getDriverName(String browserName) {
        try {
            Emulator emulator = Control.getCurrentProject().getProjectSettings().getEmulators()
                    .getEmulator(browserName);
            if (emulator != null) {
                return emulator.getDriver();
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
        }
        return browserName;
    }

    public Boolean isAlive() {
        try {
            this.page.url();
            return true;
        } catch (Exception ex) {
            throw new DriverClosedException(runContext.BrowserName);
        }
    }

    public File createScreenShot() throws IOException {
        Boolean fullpageScreenshot = Control.exe.getExecSettings().getRunSettings().getTakeFullPageScreenShot();
        this.page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshot.png")).setFullPage(fullpageScreenshot));
        File file = new File("screenshot.png");
        return file;

    }

    public String getBrowserVersion() {
        return browserContext.browser().version();
    }

}
