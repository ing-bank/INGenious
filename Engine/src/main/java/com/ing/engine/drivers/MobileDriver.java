/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ing.engine.drivers;

import com.galenframework.config.GalenConfig;
import com.galenframework.config.GalenProperty;
import com.galenframework.utils.GalenUtils;
import com.ing.datalib.settings.emulators.Emulator;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.execution.exception.DriverClosedException;
import com.ing.engine.execution.exception.UnCaughtException;
import com.ing.engine.drivers.customWebDriver.EmptyDriver;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import javax.imageio.ImageIO;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;
import ru.yandex.qatools.ashot.shooting.ShootingStrategy;

public class MobileDriver {

    protected RunContext runContext;
    public WebDriver driver;

    public void launchDriver(RunContext context) throws UnCaughtException {
        runContext = context;
        try {
            System.out.println("Launching Local Driver");
            driver = MobileWebDriverFactory.create(context, Control.getCurrentProject().getProjectSettings());
        } catch (UnCaughtException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            throw new UnCaughtException("[Appium driver Exception] unable to launch APK file " + ex.getMessage());
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
            if (driver instanceof AndroidDriver || driver instanceof IOSDriver) //                driver.manage();
            {
                return true;
            }

        } catch (Exception ex) {
            throw new DriverClosedException(runContext.BrowserName);
        }
        return false;
    }

    public String getCurrentBrowser() {
        return runContext.BrowserName;
    }

    public File createScreenShot() {
        try {
            if (driver == null) {
                System.err.println("Report Driver[" + runContext.BrowserName + "]  is not initialized");
            } else if (isAlive()) {
                if (alertPresent()) {
                    System.err.println("Couldn't take ScreenShot Alert Present in the page");
                    return ((TakesScreenshot) (new EmptyDriver())).getScreenshotAs(OutputType.FILE);
                } else if (driver instanceof AndroidDriver || driver instanceof EmptyDriver || driver instanceof IOSDriver) {
                    return ((TakesScreenshot) (driver)).getScreenshotAs(OutputType.FILE);
                } else {
                    return createNewScreenshot();
                }
            }
        } catch (DriverClosedException ex) {
            System.err.println("Couldn't take ScreenShot Driver is closed or connection is lost with driver");
        }
        return null;
    }

    private File createNewScreenshot() {
        try {
            boolean fullPage = GalenConfig.getConfig().getBooleanProperty(GalenProperty.SCREENSHOT_FULLPAGE);
            return getScreenShotFromGalen(driver, fullPage);

        } catch (Exception ex) {
            throw new RuntimeException("Error making screenshot", ex);
        }
    }

    private File getScreenShotFromAShot(WebDriver driver, boolean fullPage) throws Exception {
        ShootingStrategy strategy = fullPage ? ShootingStrategies.viewportPasting(400) : ShootingStrategies.simple();
        return getScreenShotFromAShot(driver, strategy);
    }

    private File getScreenShotFromGalen(WebDriver driver, boolean fullPage) throws Exception {
        return fullPage ? GalenUtils.makeFullScreenshot(driver) : GalenUtils.takeScreenshot(driver);
    }

    private File getScreenShotFromAShot(WebDriver driver, ShootingStrategy strategy) throws IOException {
        File file = File.createTempFile("screenshot", ".png");
        Screenshot screenshot = new AShot().shootingStrategy(strategy).takeScreenshot(driver);
        ImageIO.write(screenshot.getImage(), "png", file);
        return file;
    }

    private boolean alertPresent() {
        try {
            driver.switchTo().alert();
            driver.switchTo().defaultContent();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
