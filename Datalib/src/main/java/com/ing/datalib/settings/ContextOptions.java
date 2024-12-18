/*
 * Copyright 2014 - 2017 Cognizant Technology Solutions
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ing.datalib.settings;

import com.ing.datalib.util.data.LinkedProperties;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

/**
 *
 */
public class ContextOptions {

    public static ArrayList<String> contextList = new ArrayList<>();

    private static String location;
    private final Map<String, LinkedProperties> contextOptions = new HashMap<>();


    public ContextOptions(String location) {
        this.location = location;
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
        if(!contextList.contains("default"))
            contextList.add("default");
        return contextList;
    }

    public LinkedProperties getContextOptionsFor(String contextName) {
        return contextOptions.get(contextName);
    }

    private void load() {
        File contextFile = new File(getLocation().toString());
        if (contextFile.exists()) {
            for (File contextfile : contextFile.listFiles()) {
                // if (contextfile.getName().equals("default")) {
                if (contextfile.getName().endsWith(".properties")) {
                    String contextAlias = contextfile.getName().replace(".properties", "");
                    if (!contextList.contains(contextAlias)) {
                        contextList.add(contextAlias);
                        contextOptions.put(contextfile.getName().replace(".properties", ""), PropUtils.load(contextfile));
                    }
                }
                // contextOptions.put("default", PropUtils.load(contextfile));
                // }
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
        addDefaultContextOptions(contextName, false, false, "<yourpath>", "<userID>", "<password>", "52.1326,5.2913", "1280, 1024",  "2", false, false, "1280, 1024", "<useragent>", "nl-NL", "Europe/Amsterdam", false);
    }

    public void addDefaultContextOptions(String contextName, Boolean authenticateContext,
                                         Boolean useStorageState, String storageStatePath,
                                         String userID, String password, String setGeolocation,
                                         String setViewportSize, String setDeviceScaleFactor,
                                         Boolean setHasTouch, Boolean setIsMobile, String setScreenSize, String setUserAgent,
                                         String setLocale, String setTimezoneId, Boolean setOffline
    ) {
        LinkedProperties x = new LinkedProperties();
        x.setProperty("authenticateContext", String.valueOf(authenticateContext));
        x.setProperty("userID", userID);
        x.setProperty("password", password);
        x.setProperty("useStorageState", String.valueOf(useStorageState));
        x.setProperty("storageStatePath", storageStatePath);
        x.setProperty("setGeolocation", setGeolocation);
        x.setProperty("setViewportSize", setViewportSize);
        x.setProperty("setDeviceScaleFactor", setDeviceScaleFactor);
        x.setProperty("setHasTouch", String.valueOf(setHasTouch));
        x.setProperty("setIsMobile", String.valueOf(setIsMobile));
        x.setProperty("setScreenSize", setScreenSize);
        x.setProperty("setUserAgent", setUserAgent);
        x.setProperty("setLocale", setLocale);
        x.setProperty("setTimezoneId", setTimezoneId);
        x.setProperty("setOffline", String.valueOf(setOffline));
        addContext(contextName, x);
    }

    public void save() {
        createContextFolder();
        for (Map.Entry<String, LinkedProperties> entry : contextOptions.entrySet()) {
            String contextName = entry.getKey();
            Properties contextProp = entry.getValue();
            if (!contextName.isBlank()) {
                PropUtils.save(contextProp, getContextLocation(contextName));
            }
        }
    }

    public void save(String contextName) {
        createContextFolder();
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
                prop.setProperty("useStorageState", "false");
                prop.setProperty("userID", "<userID>");
                prop.setProperty("password", "<password>");
                prop.setProperty("storageStatePath","<yourpath>");
                prop.setProperty("setGeolocation", "52.1326,5.2913");
                prop.setProperty("setViewportSize", "1280,1024");
                prop.setProperty("setDeviceScaleFactor", "2");
                prop.setProperty("setHasTouch", "false");
                prop.setProperty("setIsMobile", "false");
                prop.setProperty("setScreenSize", "1280,1024");
                prop.setProperty("setUserAgent", "<useragent>");
                prop.setProperty("setLocale", "nl-NL");
                prop.setProperty("setTimezoneId", "Europe/Amsterdam");
                prop.setProperty("setOffline", "false");

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
