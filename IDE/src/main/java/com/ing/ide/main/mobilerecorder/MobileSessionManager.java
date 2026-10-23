package com.ing.ide.main.mobilerecorder;

import com.ing.datalib.component.Project;
import com.ing.datalib.or.mobile.MobilePlatform;
import com.ing.datalib.settings.ProjectSettings;
import com.ing.datalib.util.data.LinkedProperties;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 * Owns the lifecycle of a single live Appium session for the Mobile Recorder.
 * <p>
 * Capabilities and the Appium server URL are sourced from the project's own
 * emulator/device settings (the same source the Engine uses at execution time),
 * so a recording targets exactly the device the tests will later run against.
 */
public final class MobileSessionManager {
    private static final Logger LOG = Logger.getLogger(MobileSessionManager.class.getName());

    private AppiumDriver driver;
    private MobilePlatform platform = MobilePlatform.ANDROID;

    /**
     * Starts a session for the given emulator/device name using the project's
     * stored capabilities. Throws if a session is already running.
     */
    public synchronized AppiumDriver start(Project project, String deviceName)
        throws MalformedURLException {
        if (driver != null) {
            throw new IllegalStateException("A recording session is already running.");
        }
        ProjectSettings settings = project.getProjectSettings();
        LinkedProperties caps = settings.getCapabilities().getCapabiltiesFor(deviceName);
        String url = settings.resolveRemoteUrl(deviceName);
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "No Appium server URL configured for device '" + deviceName + "'."
            );
        }
        this.platform = resolvePlatform(caps);
        DesiredCapabilities desired = buildCapabilities(caps);
        URL serverUrl = new URL(url.trim());
        if (platform == MobilePlatform.IOS) {
            driver = new IOSDriver(serverUrl, desired);
        } else {
            driver = new AndroidDriver(serverUrl, desired);
        }
        LOG.log(
            Level.INFO,
            "Started {0} recording session on ''{1}''",
            new Object[] { platform, deviceName }
        );
        return driver;
    }

    private static MobilePlatform resolvePlatform(LinkedProperties caps) {
        if (caps != null) {
            String platformName = caps.getProperty("platformName");
            if (platformName == null) {
                platformName = caps.getProperty("appium:platformName");
            }
            if (platformName != null && platformName.trim().equalsIgnoreCase("iOS")) {
                return MobilePlatform.IOS;
            }
        }
        return MobilePlatform.ANDROID;
    }

    private static DesiredCapabilities buildCapabilities(LinkedProperties props) {
        DesiredCapabilities caps = new DesiredCapabilities();
        if (props == null) {
            return caps;
        }
        for (Object rawKey : props.keySet()) {
            String key = rawKey.toString().trim().replace("appium:", "");
            String value = props.getProperty(rawKey.toString());
            if (value == null || value.trim().isEmpty() || value.trim().equals("[]")) {
                continue;
            }
            value = value.trim();
            if (key.equalsIgnoreCase("platformName") || key.toLowerCase().contains("browsername")) {
                caps.setCapability(key, value);
            } else {
                caps.setCapability("appium:" + key, value);
            }
        }
        return caps;
    }

    public synchronized MobilePlatform getPlatform() {
        return platform;
    }

    public synchronized AppiumDriver getDriver() {
        return driver;
    }

    public synchronized boolean isRunning() {
        return driver != null;
    }

    public synchronized byte[] captureScreenshot() {
        return driver.getScreenshotAs(OutputType.BYTES);
    }

    public synchronized String pageSource() {
        return driver.getPageSource();
    }

    public synchronized Dimension windowSize() {
        return driver.manage().window().getSize();
    }

    /** Stops the current session, if any. Safe to call when nothing is running. */
    public synchronized void stop() {
        if (driver == null) {
            return;
        }
        try {
            driver.quit();
            LOG.info("Recording session stopped");
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Error stopping session", ex);
        } finally {
            driver = null;
        }
    }
}
