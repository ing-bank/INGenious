/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ing.engine.drivers;
import com.ing.datalib.settings.emulators.Emulator;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.drivers.WebDriverFactory.Browser;
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
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebDriver;


public class MobileDriver {
//    public Playwright playwright;
    protected RunContext runContext;
    public WebDriver driver;

    public void launchDriver(RunContext context) throws UnCaughtException {
        runContext = context;
        if (isMobileExecution()) {
            try{
            System.out.println("Launching Local Driver");
            driver = MobileWebDriverFactory.create(context, Control.getCurrentProject().getProjectSettings());
            }
           catch (UnCaughtException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
                throw new UnCaughtException("[Appium driver Exception] unable to launch APK file " + ex.getMessage());
            }            
        }
        else{
            driver=null;
        }
    }

    public boolean isBrowserExecution() {
        boolean isBrowserExecution = false;
        try {
            String browserName = runContext.BrowserName;
            if (browserName.equals("Chromium") || browserName.equals("Webkit") || browserName.equals("Firefox")) {
                isBrowserExecution = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isBrowserExecution;
    }

    public boolean isNoBrowserExecution() {
        boolean isNoBrowserExecution = false;
        try {
            String browserName = runContext.BrowserName;
            if (browserName.equals("NoBrowser")) {
                isNoBrowserExecution = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isNoBrowserExecution;
    }

    public boolean isMobileExecution() {
        boolean isMobileExecution = false;
        try {
            if (!isBrowserExecution() && !isNoBrowserExecution()) {
                isMobileExecution = true;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return isMobileExecution;
    }

    public void launchDriver(String browser) throws UnCaughtException {
        RunContext context = new RunContext();
        context.BrowserName = browser;
        context.Browser = Browser.fromString(browser);
        context.PlatformValue = System.getProperty("os.name");
        context.BrowserVersion = "default";
        launchDriver(context);
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
            
             if (driver instanceof AndroidDriver) 
//                driver.manage();
                return true;
            
        } catch (Exception ex) {
            throw new DriverClosedException(runContext.BrowserName);
        }
        return false;
    }

//    public File createScreenShot() throws IOException {
//        Boolean fullpageScreenshot = Control.exe.getExecSettings().getRunSettings().getTakeFullPageScreenShot();
////        this.page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshot.png")).setFullPage(fullpageScreenshot));
//        File file = new File("screenshot.png");
//        return file;
//
//    }

}
