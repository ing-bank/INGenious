package com.ing.engine.drivers;

import com.ing.datalib.settings.emulators.Emulator;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.drivers.PlaywrightDriverFactory.Browser;
import com.ing.engine.execution.exception.DriverClosedException;
import com.ing.engine.execution.exception.UnCaughtException;
import com.ing.engine.constants.FilePath;
import com.ing.engine.reporting.util.DateTimeUtils;
import com.ing.ingenious.api.contract.drivers.PlaywrightDriverCreationApi;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.microsoft.playwright.*;
import java.io.UnsupportedEncodingException;
import java.nio.file.Paths;

/**
 * Class to handle driver related operation
 *
 */
public class PlaywrightDriverCreation implements PlaywrightDriverCreationApi {

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
    public void launchDriver(RunContext context) throws UnCaughtException, UnsupportedEncodingException {
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

    public void launchDriver(String browser) throws UnCaughtException, UnsupportedEncodingException {
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

    public void RestartBrowser() throws UnCaughtException, UnsupportedEncodingException {
        StopBrowser();
        StartBrowser(runContext.BrowserName);
    }

    public void StopBrowser() {
        try {

            com.microsoft.playwright.Browser browser = browserContext.browser();
            page.close();
            closeBrowserContext();
            browser.close();

        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
        }
        this.page = null;
        browserContext = null;
    }

    public void StartBrowser(String b) throws UnCaughtException, UnsupportedEncodingException {
        StopBrowser();
        launchDriver(b);
    }

    public void closeBrowser() {
        if (this.page != null) {
            try {
                this.page.close();
                closeBrowserContext();
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

    public RunContext getRunContext() {
        return runContext;
    }


     /**
      * Sets the Playwright Page instance for this driver.
      * <p>
      * <b>API-Plugin Contract:</b> Required by {@link PlaywrightDriverCreationApi}. The argument is provided as Object for type erasure; cast to {@link Page} when calling framework methods.
      * </p>
      * @param page the Playwright Page instance (as Object, must be cast to {@link Page})
      */
     @Override
     public void setPage(Object page) {
         setPage((Page) page);
     }


    /**
     * Returns the current Playwright BrowserContext instance.
     * <p>
     * <b>API-Plugin Contract:</b> Required by {@link PlaywrightDriverCreationApi}. The returned Object should be cast to {@link BrowserContext} for Playwright operations.
     * </p>
     * @return the current BrowserContext as Object (cast to {@link BrowserContext})
     */
    @Override
    public Object getBrowserContext() {
        return browserContext;
    }


    /**
     * Returns the current Playwright instance.
     * <p>
     * <b>API-Plugin Contract:</b> Required by {@link PlaywrightDriverCreationApi}. The returned Object should be cast to {@link Playwright} for Playwright operations.
     * </p>
     * @return the current Playwright instance as Object (cast to {@link Playwright})
     */
    @Override
    public Object getPlaywright() {
        return playwright;
    }

}
