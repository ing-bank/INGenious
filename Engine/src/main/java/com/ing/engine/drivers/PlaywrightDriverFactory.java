package com.ing.engine.drivers;

import com.google.gson.JsonObject;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PlaywrightDriverFactory {
    
    public static boolean isViewPortSizeMaximized;
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
        Map<String, String> env = new HashMap<>();
 
        //if(Control.exe.getExecSettings().getRunSettings().isGridExecution())
        //    env.put("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1");
 
        return Playwright.create(new Playwright.CreateOptions().setEnv(env));
        
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

    public static BrowserContext createContext(Boolean isGrid, BrowserType browserType, String browserName, ProjectSettings settings, RunContext context) throws UnsupportedEncodingException {
        List<String> capabilities = getCapability(browserName, settings);
        NewContextOptions newContextOptions = new NewContextOptions();
        newContextOptions = addContextOptions(newContextOptions, context, capabilities, settings);
        LaunchOptions launchOptions = new LaunchOptions();
        launchOptions = addLaunchOptions(launchOptions, capabilities);
        BrowserContext browserContext = null;
        if (isGrid) {
            String cdpURL = Control.exe.getExecSettings().getRunSettings().getRemoteGridURL();
            if(!cdpURL.endsWith("/"))
                cdpURL = cdpURL + "/";
            cdpURL = cdpURL + "playwright?capabilities=" + lambdaTestCapabilities(context, capabilities);
            browserContext = browserType.connect(cdpURL).newContext(newContextOptions);
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
        List<String> customArgs = new ArrayList<>();
        customArgs.add("--auth-server-allowlist='_'");
                
        if(isViewPortSizeMaximized){
            customArgs.add("--start-maximized=true");    
        } 
        
        if (!caps.isEmpty()) {
            for (String cap : caps) {
                String key = cap.split("=", 2)[0];
                String value = cap.split("=", 2)[1];
                
                if (key.toLowerCase().contains("setheadless")) {
                    if (!value.trim().equals(""))
                        launchOptions.setHeadless((boolean) getPropertyValueAsDesiredType(value));
                } else if (key.toLowerCase().contains("setslowmo")) {
                    if (!value.trim().equals(""))
                        launchOptions.setSlowMo((double) getPropertyValueAsDesiredType(value));
                } else if (key.toLowerCase().contains("setchannel")) {
                    if (!value.trim().equals(""))
                        launchOptions.setChannel((String) getPropertyValueAsDesiredType(value));
                } else if (key.toLowerCase().contains("setchromiumsandbox")) {
                    if (!value.trim().equals(""))
                        launchOptions.setChromiumSandbox((boolean) getPropertyValueAsDesiredType(value));
                } else if (key.toLowerCase().contains("setdevtools")) {
                    if (!value.trim().equals(""))
                        customArgs.add("--auto-open-devtools-for-tabs");
                } else if (key.toLowerCase().contains("setdownloadspath")) {
                    if (!value.trim().equals(""))
                        launchOptions.setDownloadsPath(Paths.get((String) getPropertyValueAsDesiredType(value)));
                } else if (key.toLowerCase().contains("setexecutablepath")) {
                    if (!value.trim().equals(""))
                        launchOptions.setExecutablePath(Paths.get((String) getPropertyValueAsDesiredType(value)));
                } else if (key.toLowerCase().contains("settimeout")) {
                    if (!value.trim().equals(""))
                        launchOptions.setTimeout((double) getPropertyValueAsDesiredType(value));
                } else if (key.toLowerCase().contains("setproxy")) {
                    if (!value.trim().equals(""))
                        launchOptions.setProxy((String) getPropertyValueAsDesiredType(value));
                } else {
                    customArgs.add(!value.trim().equals("") ? cap : key);
                }
                
            }
        }
        launchOptions.setArgs(customArgs);
        
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
                
                switch (key) {
                    case "setgeolocation":
                        if (value != null && !value.isEmpty()) {
                            setGeolocation(newContextOptions, value);
                        }
                        break;
                    case "setviewportsize":
                        if (value != null && !value.isEmpty()) {
                            setViewportSize(newContextOptions, value);
                        } else {
                            isViewPortSizeMaximized=false;
                        }
                        break;
                    case "setdevicescalefactor":
                        if (value != null && !value.isEmpty()) {
                            newContextOptions.setDeviceScaleFactor(Integer.parseInt(value));
                        }
                        break;
                    case "sethastouch":
                        if (value != null && !value.isEmpty()) {
                            newContextOptions.setHasTouch(Boolean.parseBoolean(value));
                        }
                        break;
                    case "setismobile":
                        if (value != null && !value.isEmpty()) {
                            newContextOptions.setIsMobile(Boolean.parseBoolean(value));
                        }
                        break;
                    case "setscreensize":
                        if (value != null && !value.isEmpty()) {
                            setScreenSize(newContextOptions, value);
                        }
                        break;
                    case "setrecordvideosize":
                        if (value != null && !value.isEmpty()) {
                            setRecordVideoSize(newContextOptions, value);
                        }
                        break;
                    case "setuseragent":
                        if (value != null && !value.isEmpty()) {
                            newContextOptions.setUserAgent(value);
                        }
                        break;
                    case "setrecordvideodir":
                        if (value != null && !value.isEmpty()) {
                            newContextOptions.setRecordVideoDir(Paths.get(value));
                        }
                        break;
                    case "setlocale":
                        if (value != null && !value.isEmpty()) {
                            newContextOptions.setLocale(value);
                        }
                        break;
                    case "settimezoneid":
                        if (value != null && !value.isEmpty()) {
                            newContextOptions.setTimezoneId(value);
                        }
                        break;
                    case "setoffline":
                        if (value != null && !value.isEmpty()) {
                            newContextOptions.setOffline(Boolean.parseBoolean(value));
                        }
                        break;
                }
                
            }
        } else {
            isViewPortSizeMaximized=false;
        }

        return newContextOptions;
    }
    
    private static String lambdaTestCapabilities(RunContext context, List<String> caps) throws UnsupportedEncodingException {

        JsonObject ltcapabilities = new JsonObject();
        JsonObject ltOptions = new JsonObject();

        String browserName = "pw-"+context.BrowserName.toLowerCase();
        
        if (!caps.isEmpty()) {
            for (String cap : caps) {
                String key = cap.split("=",2)[0];
                String value = cap.split("=",2)[1];
                
                if (key.toLowerCase().contains("setchannel")) {
                    if(value.toLowerCase().contains("edge"))
                        browserName = "Microsoft Edge";
                    else
                        browserName = "Chrome";
                    break;
                }
            }
        }      
        
        String platform = "";
        if(context.PlatformValue.contains("Mac"))
            platform = "ubuntu-20";
        else if(context.PlatformValue.contains("Any"))
            platform = "Windows 11";
        else
            platform = context.PlatformValue;
        
        String browserVersion = "latest";
        
        if (context.BrowserVersionValue == null) {
            browserVersion = "latest";
        } else if (context.BrowserVersionValue.contains("Default")) {
            browserVersion = "latest";
        } else {
            browserVersion = context.BrowserVersionValue;
        }
        
        
        ltcapabilities.addProperty("browserName", browserName);
        ltcapabilities.addProperty("browserVersion", browserVersion);
        ltOptions.addProperty("platform", platform);
        ltOptions.addProperty("name", context.TestCase);
        if(getLambdaTestCap("build").isEmpty())
            ltOptions.addProperty("build", context.Scenario + " - " + Control.executionStartTime);
        else
            ltOptions.addProperty("build", getLambdaTestCap("build"));
        ltOptions.addProperty("user", getLambdaTestCap("user"));
        ltOptions.addProperty("accessKey", getLambdaTestCap("accessKey"));
        ltOptions.addProperty("video", Boolean.valueOf(getLambdaTestCap("video")));
        ltOptions.addProperty("console", Boolean.valueOf(getLambdaTestCap("console")));
        ltOptions.addProperty("network", Boolean.valueOf(getLambdaTestCap("network")));
        ltOptions.addProperty("resolution", getLambdaTestCap("resolution"));
        ltOptions.addProperty("visual", Boolean.valueOf(getLambdaTestCap("visual")));
        ltOptions.addProperty("tunnel", Boolean.valueOf(getLambdaTestCap("tunnel")));
        if(!getLambdaTestCap("tunnelName").isEmpty())
             ltOptions.addProperty("tunnel", getLambdaTestCap("tunnelName"));
        if(!getLambdaTestCap("geoLocation").isEmpty())
             ltOptions.addProperty("tunnel", getLambdaTestCap("geoLocation"));
        ltOptions.addProperty("idleTimeout", Integer.valueOf(getLambdaTestCap("idleTimeout")));
        ltOptions.addProperty("useSpecificBundleVersion", Boolean.valueOf(getLambdaTestCap("useSpecificBundleVersion")));
        
        ltcapabilities.add("LT:Options", ltOptions);


        return URLEncoder.encode(ltcapabilities.toString(), "utf-8");
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
        if (value.matches("^\\d+,\\d+")){
            isViewPortSizeMaximized=false;
            String[] dimensions = value.split(",");
            int width = Integer.parseInt(dimensions[0]);
            int height = Integer.parseInt(dimensions[1]);
            newContextOptions.setViewportSize(width, height);
        } else if (value.equals("maximized")) {
            newContextOptions.setViewportSize(null);
            isViewPortSizeMaximized=true;
        } else {
            isViewPortSizeMaximized=false;
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
                caps.add(key.toString() + "=" + prop.getProperty(key.toString()));
//                if (prop.getProperty(key.toString()) == null || prop.getProperty(key.toString()).trim().isEmpty()) {
//                } else {
//                    caps.add(key.toString() + "=" + prop.getProperty(key.toString()));
//                }
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
    
    private static String getLambdaTestCap(String property) {
        return Control.getCurrentProject().getProjectSettings().getLambdaTestCaps().getProperty(property);
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
