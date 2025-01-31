package com.ing.engine.drivers;

import com.galenframework.config.GalenConfig;
import com.galenframework.config.GalenProperty;
import com.ing.datalib.settings.ProjectSettings;
import com.ing.datalib.settings.emulators.Emulator;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.drivers.customWebDriver.EmptyDriver;
import com.ing.engine.drivers.findObjectBy.support.ByObjectProp;
import static com.ing.engine.execution.data.DataProcessor.resolve;
import com.ing.engine.execution.exception.AppiumDriverException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

public class WebDriverFactory {

    private static final Logger LOGGER = Logger.getLogger(WebDriverFactory.class.getName());

    public static WebDriver create(RunContext context, ProjectSettings settings) throws MalformedURLException {
        return create(context, settings, false, null);
    }

    public static void initDriverLocation(ProjectSettings settings) {
        ByObjectProp.load();
        System.setProperty("webdriver.edge.driver", resolve(settings.getDriverSettings().getEdgeDriverPath()));
        GalenConfig.getConfig().setProperty(GalenProperty.SCREENSHOT_FULLPAGE,
                String.valueOf(Control.exe.getExecSettings().getRunSettings().getTakeFullPageScreenShot()));
        GalenConfig.getConfig().setProperty(GalenProperty.SCREENSHOT_AUTORESIZE, "false");

        GalenConfig.getConfig().setProperty(GalenProperty.SCREENSHOT_FULLPAGE_SCROLLWAIT, "200");
    }

    private static WebDriver create(RunContext context, ProjectSettings settings, Boolean isGrid, String remoteUrl) throws MalformedURLException {
        if (context.BrowserName.equals("No Browser")) {
            return new EmptyDriver();
        } else {

            DesiredCapabilities caps = new DesiredCapabilities();
            caps = getEmulatorCapabilities(context.BrowserName, settings);
            return create(context.BrowserName, caps, settings);
        }
    }

    private static DesiredCapabilities getEmulatorCapabilities(String browserName, ProjectSettings settings) {

        Properties prop = settings.getCapabilities().getCapabiltiesFor(browserName);
        DesiredCapabilities caps = new DesiredCapabilities();
        System.out.println("\n🚀 Capabilities used : \n");
        for (Object key : prop.keySet()) {
            String capability = key.toString().trim().replace("appium:", "");
            String value = prop.getProperty(key.toString()).trim();
            if (capability.contains("platformName") || capability.toLowerCase().contains("browserName")) {
                System.out.println("\"" + capability + "\"" + " : " + "\"" + value + "\"");
                caps.setCapability(capability, value);
            } else {
                System.out.println("\"" + "appium:" + capability + "\"" + " : " + "\"" + value + "\"");
                caps.setCapability("appium:" + capability, value);
            }
        }
        return caps;

    }

    private static WebDriver create(String browserName, DesiredCapabilities caps,
            ProjectSettings settings) throws MalformedURLException {
        return checkEmulators(browserName, caps, settings);
    }

    private static WebDriver checkEmulators(String browserName, DesiredCapabilities caps, ProjectSettings settings) throws MalformedURLException {
        Emulator emulator = settings.getEmulators().getEmulator(browserName);
        if (emulator != null) {
            switch (emulator.getType()) {
                case "Remote URL": {
                    return createRemoteDriver(emulator.getRemoteUrl(), caps);

                }
            }
        }

        return null;
    }

    private static WebDriver createRemoteDriver(String url, DesiredCapabilities caps) throws MalformedURLException {

        if (isAppiumNative(url, caps.asMap())) {
            if (isAndroidNative(caps.asMap())) {
                return new io.appium.java_client.android.AndroidDriver(new URL(url), caps);
            } else if (isIOSNative(caps.asMap())) {
                return new io.appium.java_client.ios.IOSDriver(new URL(url), caps);
            }
        } 
        
        return null;
    }

    private static boolean isAppiumNative(String remoteUrl, Map props) {
        return props != null && props.containsKey("platformName")
                && toLString(props.get("platformName")).matches("android|ios")
                && (!props.containsKey("browserName") || isNullOrEmpty(props.get("browserName")));
    }

    private static boolean isNullOrEmpty(Object o) {
        return Objects.isNull(o) || o.toString().isEmpty();
    }

    private static boolean isAndroidNative(Map props) {
        return toLString(props.get("platformName")).matches("android");
    }

    private static boolean isIOSNative(Map props) {
        return toLString(props.get("platformName")).matches("ios");
    }

    private static String toLString(Object o) {
        return Objects.toString(o, "").toLowerCase();
    }

}
