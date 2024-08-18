package com.ing.engine.drivers;

import com.ing.datalib.settings.ProjectSettings;
import com.ing.engine.constants.FilePath;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.reporting.util.DateTimeUtils;
import java.io.File;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.ing.util.encryption.Encryption;
import com.microsoft.playwright.*;
import com.microsoft.playwright.Browser.NewContextOptions;
import com.microsoft.playwright.BrowserType.LaunchOptions;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class WebDriverFactory {

    public enum Browser {

        Chromium("Chromium"), WebKit("WebKit"), Firefox("Firefox");

        private final String browserValue;
        private String browserVersion;

        Browser(String value) {
            browserValue = value;
        }

        public String getBrowserValue() {
            return browserValue;
        }

        public String getBrowserVersion() {
            return browserVersion;
        }

        @Override
        public String toString() {
            return getBrowserValue();
        }

        public static Browser fromString(String browserName) {
            for (Browser browser : values()) {
                if (browser.browserValue.equalsIgnoreCase(browserName)) {
                    return browser;
                }
            }
            return null;

        }

        public static ArrayList<String> getValuesAsList() {
            ArrayList<String> browserList = new ArrayList<>();
            for (Browser browser : values()) {
                browserList.add(browser.getBrowserValue());
            }
            return browserList;
        }

    }

    public static Playwright createPlaywright() {
        return Playwright.create();
    }

    public static BrowserType createBrowserType(Playwright playwright, String browserName, RunContext context, ProjectSettings settings) {

        Browser browser = Browser.fromString(browserName);
        BrowserType browserType;

        switch (browser) {

            case Chromium:
                browserType = playwright.chromium();
                break;
            case WebKit:
                browserType = playwright.webkit();
                break;
            case Firefox:
                browserType = playwright.firefox();
                break;
//            case API:
//                browserType = (BrowserType) playwright.request();
//                break;

            default:
                throw new AssertionError(browser.name());
        }
        return browserType;

    }

    public static BrowserContext createContext(Boolean isGrid, BrowserType browserType, String browserName, ProjectSettings settings, RunContext context) {

        List<String> capabilities = getCapability(browserName, settings);
        LaunchOptions launchOptions = new LaunchOptions();
        launchOptions = addLaunchOptions(launchOptions, capabilities);
        NewContextOptions newContextOptions = new NewContextOptions();
        newContextOptions = addcontextOptions(newContextOptions, context, capabilities);
        BrowserContext browserContext = null;

        if (isGrid) {
            browserContext = browserType.connect("").newContext(newContextOptions);
        } else {
            browserContext = browserType.launch(launchOptions).newContext(newContextOptions);
        }
        return enhanceContext(browserContext);
    }

    public static Page createPage(BrowserContext browserContext) {
        Page page = browserContext.newPage();
        return page;
    }

    private static final Logger LOGGER = Logger.getLogger(WebDriverFactory.class.getName());

    private static LaunchOptions addLaunchOptions(LaunchOptions launchOptions, List<String> caps) {

        if (!caps.isEmpty()) {
            for (String cap : caps) {
                String key = cap.split("=")[0];
                String value = cap.split("=")[1];
                launchOptions.setArgs(List.of("--auth-server-allowlist='_'"));
                if (key.toLowerCase().contains("setheadless")) {
                    launchOptions.setHeadless((boolean) getPropertyValueAsDesiredType(value));
                }
                if (key.toLowerCase().contains("setslowmo")) {
                    launchOptions.setSlowMo((double) getPropertyValueAsDesiredType(value));
                }
                if (key.toLowerCase().contains("startmaximized")) {
                    launchOptions.setArgs(List.of("--start-maximized"));
                }
                if (key.toLowerCase().contains("setchannel")) {
                    launchOptions.setChannel((String) getPropertyValueAsDesiredType(value));
                }
                if (key.toLowerCase().contains("setchromiumsandbox")) {
                    launchOptions.setChromiumSandbox((boolean) getPropertyValueAsDesiredType(value));
                }
                if (key.toLowerCase().contains("setdevtools")) {
                    launchOptions.setDevtools((boolean) getPropertyValueAsDesiredType(value));
                }
                if (key.toLowerCase().contains("setdownloadspath")) {
                    launchOptions.setDownloadsPath((Path) getPropertyValueAsDesiredType(value));
                }
                if (key.toLowerCase().contains("setexecutablepath")) {
                    launchOptions.setExecutablePath((Path) getPropertyValueAsDesiredType(value));
                }
                if (key.toLowerCase().contains("settimeout")) {
                    launchOptions.setTimeout((double) getPropertyValueAsDesiredType(value));
                }
                if (key.toLowerCase().contains("setproxy")) {
                    launchOptions.setProxy((String) getPropertyValueAsDesiredType(value));
                }
            }
        }
        return launchOptions;
    }

    private static String getContextSetting(String property) {
        return Control.getCurrentProject().getProjectSettings().getContextSettings().getProperty(property);
    }

    private static NewContextOptions addcontextOptions(NewContextOptions newContextOptions, RunContext context, List<String> options) {


        Boolean isVideoEnabled = Control.exe.getExecSettings().getRunSettings().isVideoEnabled();
        Boolean isHARrecordingEnabled = Control.exe.getExecSettings().getRunSettings().isHARrecordingEnabled();
        Boolean isContextAuthenticated = Boolean.valueOf(getContextSetting("authenticateContext"));
        Boolean useStorageState = Boolean.valueOf(getContextSetting("useStorageState"));
        

        if (isContextAuthenticated)
        {
            String userID = getContextSetting("userID");
            String password = getContextSetting("password");
            if (password.endsWith(" Enc")) {
                password = password.substring(0, password.lastIndexOf(" Enc"));
                byte[] valueDecoded = Encryption.getInstance().decrypt(password).getBytes();
                password = new String(valueDecoded);
            }
            newContextOptions.setHttpCredentials(userID,password);

        }

        if (useStorageState)
        {
            String storageStatePath = getContextSetting("storageStatePath");
            Path filePath = Paths.get(storageStatePath);
            if(filePath.toFile().exists()) {
                System.out.println("\n" + "========================"+"\n" +
                        "Storage State used : +'"+storageStatePath+"'"
                        + "\n" + "========================"+"\n");
                newContextOptions.setStorageStatePath(filePath);
            }
            else
                System.out.println("\n" + "========================"+"\n" +
                        "Storage State Path does not exist. Skipping setting Storage State"
                        + "\n" + "========================"+"\n");
        }

        if (isVideoEnabled) {
            newContextOptions.setRecordVideoDir(Paths.get(FilePath.getCurrentResultsPath() + File.separator
                    + "videos"
                    + File.separator
                    + context.Scenario
                    + "_"
                    + context.TestCase));
        }

        if (isHARrecordingEnabled) {
            newContextOptions.setRecordHarPath(Paths.get(FilePath.getCurrentResultsPath() + File.separator
                    + "har"
                    + File.separator
                    + context.Scenario
                    + "_"
                    + context.TestCase
                    + "_"
                    + DateTimeUtils.TimeNowForFolder()
                    + ".har"));
        }

        if (!options.isEmpty()) {
            for (String option : options) {
                String key = option.split("=")[0];
                String value = option.split("=")[1];
                if (key.toLowerCase().contains("setgeolocation")) {
                    Double latitude = Double.valueOf(value.split(",")[0]);
                    Double longitude = Double.valueOf(value.split(",")[1]);
                    newContextOptions.setGeolocation(latitude, longitude).setPermissions(Arrays.asList("geolocation"));
                }

                if (key.toLowerCase().contains("setviewportsize")) {
                    int width = Integer.parseInt(value.split(",")[0]);
                    int height = Integer.parseInt(value.split(",")[1]);
                    newContextOptions.setViewportSize(width, height);
                }
                if (key.toLowerCase().contains("startmaximized")) {
                    newContextOptions.setViewportSize(null);
                }
                if (key.toLowerCase().contains("setdevicescalefactor")) {
                    int factor = Integer.parseInt(value);
                    newContextOptions.setDeviceScaleFactor(factor);
                }

                if (key.toLowerCase().contains("sethastouch")) {
                    newContextOptions.setHasTouch(Boolean.parseBoolean(value));
                }

                if (key.toLowerCase().contains("setismobile")) {
                    newContextOptions.setIsMobile(Boolean.parseBoolean(value));
                }

                if (key.toLowerCase().contains("setscreensize")) {
                    int width = Integer.parseInt(value.split(",")[0]);
                    int height = Integer.parseInt(value.split(",")[1]);
                    newContextOptions.setScreenSize(width, height);
                }

                if (key.toLowerCase().contains("setuseragent")) {
                    newContextOptions.setUserAgent(value);
                }

                if (key.toLowerCase().contains("setlocale")) {
                    newContextOptions.setLocale(value);
                }

                if (key.toLowerCase().contains("settimezoneid")) {
                    newContextOptions.setTimezoneId(value);
                }

                if (key.toLowerCase().contains("setoffline")) {
                    newContextOptions.setOffline(Boolean.parseBoolean(value));
                }
            }
        }

        return newContextOptions;
    }

    private static BrowserContext enhanceContext(BrowserContext browserContext) {

        Boolean isTracingEnabled = Control.exe.getExecSettings().getRunSettings().isTracingEnabled();

        if (isTracingEnabled) {
            System.out.println("Tracing Started");
            browserContext.tracing().start(new Tracing.StartOptions()
                    .setScreenshots(true)
                    .setSnapshots(true)
                    .setSources(true));

        }

        return browserContext;
    }

    private static Object getPropertyValueAsDesiredType(String value) {
        if (value != null && !value.isEmpty()) {
            if (value.toLowerCase().matches("(true|false)")) {
                return Boolean.valueOf(value);
            }
            if (value.matches("\\d+")) {
                return Double.valueOf(value);
            } else {
                return value;
            }
        }
        return value;
    }

    private static List<String> getCapability(String browserName, ProjectSettings settings) {

        Properties prop = settings.getCapabilities().getCapabiltiesFor(browserName);
        List<String> caps = new ArrayList<>();

        if (prop != null) {
            prop.keySet().stream().forEach((key) -> {
                if (prop.getProperty(key.toString()) == null || prop.getProperty(key.toString()).trim().isEmpty()) {
                } else {
                    caps.add(key.toString() + "=" + prop.getProperty(key.toString()));
                }
            });
        }
        return caps;
    }

    private static List<String> getOption(String browserName, ProjectSettings settings) {

        Properties prop = settings.getCapabilities().getCapabiltiesFor(browserName);
        List<String> options = new ArrayList<>();
        if (prop != null) {
            prop.keySet().stream().forEach((key) -> {
                if (prop.getProperty(key.toString()) == null || prop.getProperty(key.toString()).trim().isEmpty()) {
                    options.add(key.toString());
                }
            });
        }
        return options;
    }
}
