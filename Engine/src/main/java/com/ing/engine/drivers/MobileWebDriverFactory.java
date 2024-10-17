/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.ing.engine.drivers;

import com.galenframework.config.GalenConfig;
import com.galenframework.config.GalenProperty;
import com.ing.datalib.settings.ProjectSettings;
import com.ing.datalib.settings.emulators.Emulator;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.drivers.findObjectBy.support.ByObjectProp;
import static com.ing.engine.execution.data.DataProcessor.resolve;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

/**
 *
 * @author AP01BP
 */
public class MobileWebDriverFactory {
       public static WebDriver create(RunContext context, ProjectSettings settings) {
        return create(context, settings, false, null);
    }
       
       public static void initDriverLocation(ProjectSettings settings) {
		ByObjectProp.load();
		// System.setProperty("webdriver.chrome.driver",
		// resolve(settings.getDriverSettings().getChromeDriverPath()));
		System.setProperty("webdriver.edge.driver", resolve(settings.getDriverSettings().getEdgeDriverPath()));
		GalenConfig.getConfig().setProperty(GalenProperty.SCREENSHOT_FULLPAGE,
				String.valueOf(Control.exe.getExecSettings().getRunSettings().getTakeFullPageScreenShot()));
		GalenConfig.getConfig().setProperty(GalenProperty.SCREENSHOT_AUTORESIZE, "false");

		GalenConfig.getConfig().setProperty(GalenProperty.SCREENSHOT_FULLPAGE_SCROLLWAIT, "200");
	}

    private static WebDriver create(RunContext context, ProjectSettings settings, Boolean isGrid, String remoteUrl) {
        DesiredCapabilities caps = new DesiredCapabilities();
        UiAutomator2Options mobileOptions = new UiAutomator2Options();
        caps = getEmulatorCapabilities(context.BrowserName, settings);
        mobileOptions.setDeviceName(caps.getCapability("appium:deviceName").toString())
                .setPlatformVersion(caps.getCapability("appium:platformVersion").toString())
                .setAppPackage(caps.getCapability("appium:appPackage").toString())
                .setAppActivity(caps.getCapability("appium:appActivity").toString())
                .setPlatformName(caps.getCapability("platformName").toString());

        return create(context.BrowserName, caps, mobileOptions, settings, isGrid, remoteUrl);
    }

    private static DesiredCapabilities getEmulatorCapabilities(String browserName, ProjectSettings settings) {
        System.out.println("Browser Name : " + browserName);
        Properties prop = settings.getCapabilities().getCapabiltiesFor(browserName);
        DesiredCapabilities caps = new DesiredCapabilities();
        for (Object key : prop.keySet()) {
            caps.setCapability(key.toString(), prop.getProperty(key.toString()));
        }
        return caps;
    }

    private static WebDriver create(String browserName, DesiredCapabilities caps, UiAutomator2Options mobileOptions,
            ProjectSettings settings, Boolean isGrid, String remoteUrl) {
        return checkEmulators(browserName, caps, mobileOptions, settings, isGrid, remoteUrl);
    }

    private static WebDriver checkEmulators(String browserName, DesiredCapabilities caps, UiAutomator2Options mobileOptions, ProjectSettings settings,
            Boolean isGrid, String remoteUrl) {
        Emulator emulator = settings.getEmulators().getEmulator(browserName);
        if (emulator != null) {
            switch (emulator.getType()) {
                case "Remote URL": {
                    return createRemoteDriver(browserName, emulator.getRemoteUrl(), caps, mobileOptions, false,
                            settings.getDriverSettings());

                }
            }
        }
        return null;
    }

    private static WebDriver createRemoteDriver(String browserName, String url,
            DesiredCapabilities caps, UiAutomator2Options mobileOptions, Boolean checkForProxy, Properties props) {
        try {
            if (isAppiumNative(url, caps.asMap())) {
                if (isAndroidNative(caps.asMap())) {
                    return new AndroidDriver(new URL(url), mobileOptions);
                } else if (isIOSNative(caps.asMap())) {
                    return new AndroidDriver(new URL(url), mobileOptions);
                }
            }

        } catch (Exception ex) {
//            LOGGER.log(Level.SEVERE, ex.getMessage(), ex);
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
