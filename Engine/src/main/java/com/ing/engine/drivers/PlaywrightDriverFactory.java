package com.ing.engine.drivers;

import com.ing.datalib.settings.ProjectSettings;
import com.ing.engine.constants.FilePath;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.reporting.util.DateTimeUtils;
import java.io.File;
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
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.Collection;

public class PlaywrightDriverFactory {

    public enum Browser {

        Chromium("Chromium"), WebKit("WebKit"), Firefox("Firefox"), Empty("No Browser");

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
        newContextOptions = addContextOptions(newContextOptions, context, capabilities, settings);
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

    private static final Logger LOGGER = Logger.getLogger(PlaywrightDriverFactory.class.getName());

    private static LaunchOptions addLaunchOptions(LaunchOptions launchOptions, List<String> caps) {

        if (!caps.isEmpty()) {
            for (String cap : caps) {
                String key = cap.split("=", 2)[0];
                String value = cap.split("=", 2)[1];
                launchOptions.setArgs(List.of("--auth-server-allowlist='_'"));
                if (key.toLowerCase().contains("setheadless")) {
                    launchOptions.setHeadless((boolean) getPropertyValueAsDesiredType(value));
                }
                if (key.toLowerCase().contains("setslowmo")) {
                    launchOptions.setSlowMo((double) getPropertyValueAsDesiredType(value));
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
                    launchOptions.setDownloadsPath(Paths.get((String) getPropertyValueAsDesiredType(value)));
                }
                if (key.toLowerCase().contains("setexecutablepath")) {
                    launchOptions.setExecutablePath(Paths.get((String) getPropertyValueAsDesiredType(value)));
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


    private static NewContextOptions addContextOptions(NewContextOptions newContextOptions, RunContext context, List<String> options, ProjectSettings settings) {
        boolean isVideoEnabled = Control.exe.getExecSettings().getRunSettings().isVideoEnabled();
        boolean isHARrecordingEnabled = Control.exe.getExecSettings().getRunSettings().isHARrecordingEnabled();

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

        Properties contextDetails = getContextDetails("default");
        setHttpCredentialsIfAuthenticated(newContextOptions, contextDetails);
        setStorageStateIfEnabled(newContextOptions, contextDetails);

        List<String> contextOptions = getContextOptions("default", settings);
        if (!contextOptions.isEmpty()) {
            for (String prop : contextOptions) {
                String[] keyValue = prop.split("=", 2);
                String key = keyValue[0].toLowerCase();
                String value = keyValue[1];

                if (value != null && !value.isEmpty()) {
                    switch (key) {
                        case "setgeolocation":
                            setGeolocation(newContextOptions, value);
                            break;
                        case "setviewportsize":
                            setViewportSize(newContextOptions, value);
                            break;
                        case "setdevicescalefactor":
                            newContextOptions.setDeviceScaleFactor(Integer.parseInt(value));
                            break;
                        case "sethastouch":
                            newContextOptions.setHasTouch(Boolean.parseBoolean(value));
                            break;
                        case "setismobile":
                            newContextOptions.setIsMobile(Boolean.parseBoolean(value));
                            break;
                        case "setscreensize":
                            setScreenSize(newContextOptions, value);
                            break;
                        case "setrecordvideosize":
                            setRecordVideoSize(newContextOptions, value);
                            break;
                        case "setuseragent":
                            newContextOptions.setUserAgent(value);
                            break;
                        case "setrecordvideodir":
                            newContextOptions.setRecordVideoDir(Paths.get(value));
                            break;
                        case "setlocale":
                            newContextOptions.setLocale(value);
                            break;
                        case "settimezoneid":
                            newContextOptions.setTimezoneId(value);
                            break;
                        case "setoffline":
                            newContextOptions.setOffline(Boolean.parseBoolean(value));
                            break;
                    }
                }
            }
        }

        return newContextOptions;
    }

    private static void setHttpCredentialsIfAuthenticated(NewContextOptions newContextOptions, Properties contextDetails) {
        boolean isContextAuthenticated = Boolean.parseBoolean(contextDetails.getProperty("isAuthenticated"));
        if (isContextAuthenticated) {
            String userID = handleUserDefinedVariables(contextDetails.getProperty("userID"));
            String password = handleUserDefinedVariables(contextDetails.getProperty("password"));

            if (password.endsWith(" Enc")) {
                password = password.substring(0, password.lastIndexOf(" Enc"));
                byte[] valueDecoded = Encryption.getInstance().decrypt(password).getBytes();
                password = new String(valueDecoded);
            }
            newContextOptions.setHttpCredentials(userID, password);
        }
    }

    private static void setStorageStateIfEnabled(NewContextOptions newContextOptions, Properties contextDetails) {
        boolean useStorageState = Boolean.parseBoolean(contextDetails.getProperty("useStorageState"));
        if (useStorageState) {
            String storageStatePath = contextDetails.getProperty("storageStatePath");
            Path filePath = Paths.get(storageStatePath);
            if (filePath.toFile().exists()) {
                System.out.println("\n========================\nStorage State used : '" + storageStatePath + "'\n========================\n");
                newContextOptions.setStorageStatePath(filePath);
            } else {
                System.out.println("\n========================\nStorage State Path does not exist. Skipping setting Storage State\n========================\n");
            }
        }
    }

    private static void setGeolocation(NewContextOptions newContextOptions, String value) {
        String[] coordinates = value.split(",");
        double latitude = Double.parseDouble(coordinates[0]);
        double longitude = Double.parseDouble(coordinates[1]);
        newContextOptions.setGeolocation(latitude, longitude).setPermissions(Arrays.asList("geolocation"));
    }

    private static void setViewportSize(NewContextOptions newContextOptions, String value) {
        if (value.equals("maximized")) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            newContextOptions.setViewportSize((int) screenSize.getWidth(), (int) screenSize.getHeight());
        } else {
            String[] dimensions = value.split(",");
            int width = Integer.parseInt(dimensions[0]);
            int height = Integer.parseInt(dimensions[1]);
            newContextOptions.setViewportSize(width, height);
        }
    }

    private static void setScreenSize(NewContextOptions newContextOptions, String value) {
        String[] dimensions = value.split(",");
        int width = Integer.parseInt(dimensions[0]);
        int height = Integer.parseInt(dimensions[1]);
        newContextOptions.setScreenSize(width, height);
    }

    private static void setRecordVideoSize(NewContextOptions newContextOptions, String value) {
        String[] dimensions = value.split(",");
        int width = Integer.parseInt(dimensions[0]);
        int height = Integer.parseInt(dimensions[1]);
        newContextOptions.setRecordVideoSize(width, height);
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

    private static List<String> getContextOptions(String contextName, ProjectSettings settings) {
        Properties prop = settings.getContextSettings().getContextOptionsFor(contextName);
        List<String> options = new ArrayList<>();
        if (prop != null) {
            prop.keySet().stream().forEach((key) -> {
                if (prop.getProperty(key.toString()) == null) {
                } else {
                    options.add(key.toString() + "=" + prop.getProperty(key.toString()));
                }
            });
        }
        return options;
    }

    private static Properties getContextDetails(String contextAlias) {
        return Control.getCurrentProject().getProjectSettings().getContextSettings().getContextOptionsFor(contextAlias);

    }

    private static String handleUserDefinedVariables(String value) {
        Collection<Object> keys = Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().keySet();
        for (Object key : keys) {
            if (value.equals("%" + key + "%")) {
                return Control.getCurrentProject().getProjectSettings().getUserDefinedSettings().getProperty(key.toString());
            }
        }
        return value;
    }

}
