package com.ing.datalib.settings;

import com.ing.datalib.util.data.LinkedProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 */
public class Capabilities {

    private final Map<String, LinkedProperties> browserCapabilties = new HashMap<>();

    private String location;

    public Capabilities(String location) {
        this.location = location;
        createCapsFolder();
        load();
    }

    public Map<String, LinkedProperties> getBrowserCapabilties() {
        return browserCapabilties;
    }

    public LinkedProperties getCapabiltiesFor(String browserName) {
        return browserCapabilties.get(browserName);
    }

    private void load() {
        File caps = new File(getLocation());
        if (caps.exists()) {
            for (File cap : caps.listFiles()) {
                if (cap.getName().endsWith(".properties")) {
                    browserCapabilties.put(cap.getName().replace(".properties", ""), PropUtils.load(cap));
                }
            }
        }
    }

    public void addCapability(String browserName) {
        addCapability(browserName, new LinkedProperties());
    }

    public void addCapability(String browserName, LinkedProperties props) {
        browserCapabilties.put(browserName, props);
        save();
    }

    public void updateCapabiltyFor(String browserName, String key, String newvalue) {
        browserCapabilties.get(browserName).update(key, newvalue);
    }

    public void addDefaultAppiumCapability(String browserName) {
        addDefaultAppiumCapability(browserName, "", "", "");
    }

    public void addDefaultAppiumCapability(String browserName, String udid, String appPackage, String appiumActivity) {
        LinkedProperties x = new LinkedProperties();
        if (appiumActivity.isEmpty()) {
            x.setProperty("browserName", "chrome");
        }
        x.setProperty("platformName", "Android");
        x.setProperty("deviceName", browserName);
        x.setProperty("platformVersion", "");
        x.setProperty("udid", udid);
        // Checkmarx findings
        x.put("appActivity", appiumActivity);
        x.put("appPackage", appPackage);
        addCapability(browserName, x);
    }

    public void save() {
        // createCapsFolder();
        for (Map.Entry<String, LinkedProperties> entry : browserCapabilties.entrySet()) {
            String capName = entry.getKey();
            Properties capProp = entry.getValue();
            PropUtils.save(capProp, getCapLocation(capName));
        }
    }

    public void save(String capsName) {
        //createCapsFolder();
        if (browserCapabilties.containsKey(capsName)) {
            PropUtils.save(browserCapabilties.get(capsName), getCapLocation(capsName));
        }
    }

    public void delete(String capsName) {
        if (browserCapabilties.containsKey(capsName)) {
            File caps = new File(getCapLocation(capsName));
            if (caps.exists()) {
                caps.delete();
            }
            browserCapabilties.remove(capsName);
        }
    }

    public Boolean rename(String oldCapsName, String newCapsName) {
        if (browserCapabilties.containsKey(oldCapsName) && !browserCapabilties.containsKey(newCapsName)) {
            File caps = new File(getCapLocation(oldCapsName));
            if (caps.exists()) {
                if (caps.renameTo(new File(getCapLocation(newCapsName)))) {
                    browserCapabilties.put(newCapsName, browserCapabilties.remove(oldCapsName));
                    return true;
                }
            } else {
                browserCapabilties.put(newCapsName, browserCapabilties.remove(oldCapsName));
            }
        }
        return false;
    }

    public String getCapLocation(String capsName) {
        return getLocation() + File.separator + capsName + ".properties";
    }

    public String getLocation() {
        return location + File.separator + "Capabilities";
    }

    public void setLocation(String location) {
        this.location = location;
    }

    private void createCapsFolder() {
        File caps = new File(getLocation());
        if (!caps.exists()) {
            caps.mkdirs();
            createDefaultFile(getLocation());
        }
    }

    private void createDefaultFile(String location) {
        String chromiumFile = location + File.separator + "Chromium.properties";
        String webkitFile = location + File.separator + "WebKit.properties";
        String firefoxFile = location + File.separator + "Firefox.properties";
        String sapFile = location + File.separator + "SAP.properties";
        createFile(chromiumFile);
        createFile(webkitFile);
        createFile(firefoxFile);
        createFile(sapFile);
    }

    private void createFile(String fileName) {
        File propertiesFile = new File(fileName);
        if (!propertiesFile.exists()) {
            try (FileOutputStream fos = new FileOutputStream(propertiesFile)) {
                Properties prop = new Properties();
                
                // Determine browser type from filename
                String baseName = propertiesFile.getName();
                
                if (baseName.equals("SAP.properties")) {
                    // SAP-specific properties
                    prop.setProperty("app", "C:\\Program Files\\SAP\\FrontEnd\\SAPGUI\\saplogon.exe");
                    prop.setProperty("libraryPath", "lib/jacob-1.21");
                    prop.setProperty("dllPath", "lib/jacob-1.21/jacob-1.21-x64.dll");
                    prop.setProperty("connectionName", "SAP_CONN_NAME");
                    prop.setProperty("platformName", "Windows");
                } else if (baseName.equals("Chromium.properties") || baseName.equals("WebKit.properties") || baseName.equals("Firefox.properties")) {
                    // Playwright browser properties (same for all)
                    prop.setProperty("setHeadless", "false");
                    prop.setProperty("setSlowMo", "");
                    prop.setProperty("setDevtools", "");
                    prop.setProperty("setDownloadsPath", "");
                    prop.setProperty("setExecutablePath", "");
                    prop.setProperty("setTimeout", "30000");
                    prop.setProperty("setProxy", "");
                    if (baseName.equals("Chromium.properties")) {
                        prop.setProperty("setChannel", "");
                        prop.setProperty("setChromiumSandbox", "");
                    }
                }
                // Write properties to the file
                prop.store(fos, null);
            } catch (IOException e) {
                System.err.println("Error writing to properties file: " + e.getMessage());
            }
        } else {
            System.out.println(fileName + " properties file already exists: " + location);
        }
    }
    
    /**
     * Ensures SAP.properties file exists. 
     * Creates it if missing for projects that have SAP emulator.
     */
    public void ensureSAPCapabilitiesExist() {
        String sapFile = location + File.separator + "SAP.properties";
        createFile(sapFile);
    }

}