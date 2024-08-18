
package com.ing.datalib.settings;

import com.ing.datalib.util.data.LinkedProperties;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 *
 * 
 */
public class Capabilities {

    private final Map<String, LinkedProperties> browserCapabilties = new HashMap<>();

    private String location;

    public Capabilities(String location) {
        this.location = location;
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
    }

    public void updateCapabiltyFor(String browserName, String key, String newvalue) {
        browserCapabilties.get(browserName).update(key,newvalue);
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
        createCapsFolder();
        for (Map.Entry<String, LinkedProperties> entry : browserCapabilties.entrySet()) {
            String capName = entry.getKey();
            Properties capProp = entry.getValue();
            PropUtils.save(capProp, getCapLocation(capName));
        }
    }

    public void save(String capsName) {
        createCapsFolder();
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
        }
    }

}
