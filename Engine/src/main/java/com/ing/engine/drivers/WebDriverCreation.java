package com.ing.engine.drivers;

import com.galenframework.config.GalenConfig;
import com.galenframework.config.GalenProperty;
import com.galenframework.utils.GalenUtils;
import com.ing.datalib.settings.ProjectSettings;
import com.ing.datalib.settings.emulators.Emulator;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.execution.exception.DriverClosedException;
import com.ing.engine.execution.exception.UnCaughtException;
import com.ing.engine.drivers.customWebDriver.EmptyDriver;
import com.ing.engine.execution.exception.AppiumDriverException;
import com.ing.ingenious.api.contract.drivers.MobileDriverControlApi;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import java.util.Properties;
import javax.imageio.ImageIO;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import ru.yandex.qatools.ashot.AShot;
import ru.yandex.qatools.ashot.Screenshot;
import ru.yandex.qatools.ashot.shooting.ShootingStrategies;
import ru.yandex.qatools.ashot.shooting.ShootingStrategy;

public class WebDriverCreation implements MobileDriverControlApi {

    protected RunContext runContext;
    public WebDriver driver;

    public void launchDriver(RunContext context) throws UnCaughtException {
        runContext = context;
        try {
            System.out.println("\n🚀 Launching Driver \n");
            driver = WebDriverFactory.create(context, Control.getCurrentProject().getProjectSettings());
        } catch (Exception ex) {
            throw new AppiumDriverException("[Appium Driver Exception]. Please verify if the capabilities are passed correctly. Please visit  'https://appium.io/docs/en/2.0/guides/caps/' for more details. \n" + ex.getMessage());

        }
    }

    public boolean isBrowserExecution() {
        boolean isBrowserExecution = false;
        try {
            String browserName = runContext.BrowserName;
            if (browserName.equals("Chromium") || browserName.equals("WebKit") || browserName.equals("Firefox")) {
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
            if (browserName.equals("No Browser")) {
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
            if (driver instanceof AndroidDriver || driver instanceof IOSDriver) {
                return true;
            }

        } catch (Exception ex) {
            throw new DriverClosedException(runContext.BrowserName);
        }
        return false;
    }

    public String getCurrentBrowser() {
        if (isNoBrowserExecution()) {
            return runContext.BrowserName;
        }
        return getEmulatorCapabilityValue(runContext.BrowserName, Control.getCurrentProject().getProjectSettings(), "deviceName");
    }

    public String getCurrentBrowserVersion() {
        if (isNoBrowserExecution()) {
            return "NA";
        }
        return getEmulatorCapabilityValue(runContext.BrowserName, Control.getCurrentProject().getProjectSettings(), "platformVersion");
    }

    public String getPlatform() {
        if (isNoBrowserExecution()) {
            return System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch");
        } else {
            return getEmulatorCapabilityValue(runContext.BrowserName, Control.getCurrentProject().getProjectSettings(), "platformName");
        }
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

    private String getEmulatorCapabilityValue(String browserName, ProjectSettings settings, String cap) {

        Properties prop = settings.getCapabilities().getCapabiltiesFor(browserName);
        for (Object key : prop.keySet()) {
            String capability = key.toString().trim().replace("appium:", "");
            String value = prop.getProperty(key.toString()).trim();
            if (capability.contains(cap)) {
                return value;
            }

        }
        return null;
    }

    public void StopBrowser() {
        try {
            driver.quit();
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
        }
        driver = null;
    }

    public boolean isLambdaTestExecutionPlatform() {
        if (!runContext.BrowserName.equalsIgnoreCase("No Browser")) {
            String url = Control.getCurrentProject().getProjectSettings().getEmulators().getEmulator(runContext.BrowserName).getRemoteUrl();
            return url.endsWith("hub.lambdatest.com/wd/hub");
        } else {
            return false;
        }
    }
    
    public RunContext getRunContext() {
        return runContext;
    }
    
    // ===== API Interface Implementations (Object type wrappers) =====
    
    /**
     * Gets the WebDriver instance exposed as Object.
     * <p>
     * <b>API-Plugin Contract:</b> Required by {@link MobileDriverControlApi}. 
     * The returned Object should be cast to {@link WebDriver}, {@link AndroidDriver}, or {@link IOSDriver}.
     * </p>
     * @return the current WebDriver instance as Object
     */
    @Override
    public Object getDriver() {
        return driver;
    }
    
    /**
     * Launches the driver with the specified context (API wrapper).
     * <p>
     * <b>API-Plugin Contract:</b> Required by {@link MobileDriverControlApi}. 
     * The argument is provided as Object for type erasure; cast to {@link RunContext} when calling framework methods.
     * </p>
     * @param context RunContext instance as Object, must be cast to {@link RunContext}
     * @throws Exception if driver initialization fails
     */
    @Override
    public void launchDriver(Object context) throws Exception {
        launchDriver((RunContext) context);
    }
}
