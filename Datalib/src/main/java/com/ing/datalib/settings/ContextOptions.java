package com.ing.datalib.settings;

import com.ing.datalib.util.data.LinkedProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 *
 */
public class ContextOptions {

    private static ArrayList<String> contextList = new ArrayList<>();
    private static String location;
    private final Map<String, LinkedProperties> contextOptions = new HashMap<>();


    public ContextOptions(String location) {
        this.location = location;
        createContextFolder();
        load();
    }

    public static String getLocation() {
        return location + File.separator + "Contexts";
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Map<String, LinkedProperties> getContextOptions() {
        return contextOptions;
    }

    public ArrayList<String> getContextList() {
        load();
        return contextList;
    }

    public LinkedProperties getContextOptionsFor(String contextName) {
        return contextOptions.get(contextName);
    }

    private void load() {
        File contextFile = new File(getLocation());
        if (contextFile.exists()) {
            for (File contextfile : contextFile.listFiles()) {
                if (contextfile.getName().endsWith(".properties")) {
                    String contextAlias = contextfile.getName().replace(".properties", "");
                    if (!contextList.contains(contextAlias)) {
                        contextList.add(contextAlias);
                        contextOptions.put(contextfile.getName().replace(".properties", ""), PropUtils.load(contextfile));
                    }
                }
            }
        }
    }

    public void addContextName(String contextName) {
        contextList.add(contextName);
    }

    public void addContext(String contextName, LinkedProperties props) {
        contextOptions.put(contextName, props);
    }

    public void addContextOptions(String contextName) {
        addDefaultContextOptions(contextName, false);

    }

    public void addDefaultContextOptions(String contextName, Boolean authenticateContext){
    LinkedProperties x = new LinkedProperties();
        x.setProperty("authenticateContext", String.valueOf(authenticateContext));
        x.setProperty("userID", "");
        x.setProperty("password", "");
        x.setProperty("useStorageState", "");
        x.setProperty("storageStatePath", "");
        x.setProperty("setGeolocation", "");
        x.setProperty("setViewportSize", "");
        x.setProperty("setDeviceScaleFactor", "");
        x.setProperty("setHasTouch", "");
        x.setProperty("setIsMobile", "");
        x.setProperty("setScreenSize", "");
        x.setProperty("setUserAgent", "");
        x.setProperty("setLocale", "");
        x.setProperty("setTimezoneId", "");
        x.setProperty("setOffline", "");
        addContext(contextName, x);
    }

    public void save() {
        for (Map.Entry<String, LinkedProperties> entry : contextOptions.entrySet()) {
            String contextName = entry.getKey();
            Properties contextProp = entry.getValue();
            if (!contextName.isBlank()) {
                PropUtils.save(contextProp, getContextLocation(contextName));
            }
        }
    }

    public void save(String contextName) {
        if (contextOptions.containsKey(contextName)) {
            PropUtils.save(contextOptions.get(contextName), getContextLocation(contextName));
        }
    }

    public void delete(String contextName) {
        if (contextOptions.containsKey(contextName)) {
            File context = new File(getContextLocation(contextName));
            if (context.exists()) {
                context.delete();
            }
            contextOptions.remove(contextName);
            contextList.remove(contextName);
        }
    }

    public String getContextLocation(String contextName) {
        return getLocation() + File.separator + contextName + ".properties";
    }

    private void createContextFolder() {
        File contexts = new File(getLocation());
        if (!contexts.exists()) {
            contexts.mkdirs();
            createDefaultFile(getLocation());
            }
        }


    private void createDefaultFile(String location) {
        String fileName = location + File.separator + "default.properties";
        File propertiesFile = new File(fileName);
        if (!propertiesFile.exists()) {
            try (FileOutputStream fos = new FileOutputStream(propertiesFile)) {
                Properties prop = new Properties();
                // Add default key-value pairs
                prop.setProperty("authenticateContext", "false");
                prop.setProperty("userID", "");
                prop.setProperty("password", "");
                prop.setProperty("useStorageState", "");
                prop.setProperty("storageStatePath", "");
                prop.setProperty("setGeolocation", "");
                prop.setProperty("setViewportSize", "");
                prop.setProperty("setDeviceScaleFactor", "");
                prop.setProperty("setHasTouch", "");
                prop.setProperty("setIsMobile", "");
                prop.setProperty("setScreenSize", "");
                prop.setProperty("setUserAgent", "");
                prop.setProperty("setLocale", "");
                prop.setProperty("setTimezoneId", "");
                prop.setProperty("setOffline", "");

                // Write properties to the file
                prop.store(fos, "Default Properties");
                System.out.println("default.properties file created: " + location);
            } catch (IOException e) {
                System.err.println("Error writing to default.properties file: " + e.getMessage());
            }
        } else {
            System.out.println("default.properties file already exists: " + location);
        }
    }


}