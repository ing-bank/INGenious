package com.ing.datalib.settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.datalib.settings.emulators.Device;
import com.ing.datalib.util.data.LinkedProperties;

/**
 * Manages the list of mobile devices configured under the
 * "Manage Devices" tab in the Configurations window.
 *
 * Persistence: Settings/Devices.json
 *
 * Capability properties for each device are stored alongside browser
 * capabilities (via {@link Capabilities}) using the device name as the key.
 */
public class Devices {

    /** Marker prefix used to render section header rows in the capability table. */
    public static final String SECTION_PREFIX = "-- ";
    public static final String SECTION_SUFFIX = " --";

    private List<Device> devices;
    private String location;
    private final ObjectMapper objMapper;

    public Devices(String location) {
        this.location = location;
        this.objMapper = new ObjectMapper();
        this.devices = new ArrayList<>();
        load();
    }

    private void load() {
        File f = new File(getLocation());
        if (f.exists()) {
            try {
                devices = objMapper.readValue(
                        f,
                        objMapper.getTypeFactory().constructCollectionType(List.class, Device.class));
            } catch (IOException ex) {
                Logger.getLogger(Devices.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void reload() {
        devices.clear();
        load();
    }

    public List<Device> getDevices() {
        return devices;
    }

    public List<String> getDeviceNames() {
        List<String> names = new ArrayList<>();
        for (Device d : devices) {
            names.add(d.getName());
        }
        return names;
    }

    public Device getDevice(String name) {
        for (Device d : devices) {
            if (d.getName().equals(name)) {
                return d;
            }
        }
        return null;
    }

    public void addDevice(String name) {
        if (getDevice(name) == null) {
            devices.add(new Device(name));
        }
    }

    /**
     * Returns the device named {@code name}, creating a fresh entry with
     * default values if it does not already exist. Used by the CLI override
     * dispatcher so {@code -setEnv "device.<name>.<key>=<value>"} can target
     * a device that hasn't been added yet via the IDE.
     */
    public Device getOrCreateDevice(String name) {
        Device d = getDevice(name);
        if (d == null) {
            d = new Device(name);
            devices.add(d);
        }
        return d;
    }

    public void deleteDevice(String name) {
        Device d = getDevice(name);
        if (d != null) {
            devices.remove(d);
        }
    }

    public Boolean renameDevice(String oldName, String newName) {
        Device existing = getDevice(oldName);
        if (existing != null && getDevice(newName) == null) {
            existing.setName(newName);
            return true;
        }
        return false;
    }

    public void save() {
        File f = new File(getLocation());
        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
        try {
            objMapper.writeValue(f, devices);
        } catch (IOException ex) {
            Logger.getLogger(Devices.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getLocation() {
        return location + File.separator + "Devices.json";
    }

    public void setLocation(String location) {
        this.location = location;
    }

    // ---------------------------------------------------------------------
    // Default capability sets
    // ---------------------------------------------------------------------

    /**
     * Default capabilities for a "regular" (non-LambdaTest) device.
     */
    public LinkedProperties defaultDeviceCap() {
        LinkedProperties props = new LinkedProperties();
        props.setProperty("deviceName", "");
        props.setProperty("platformName", "");
        props.setProperty("platformVersion", "");
        props.setProperty("automationName", "");
        return props;
    }

    /**
     * Default capabilities for a LambdaTest device, grouped by category.
     * The map preserves insertion order so callers can render headers in
     * the same order they're returned.
     */
    public Map<String, LinkedProperties> defaultLambdaTestCaps() {
        Map<String, LinkedProperties> grouped = new LinkedHashMap<>();

        LinkedProperties mandatory = new LinkedProperties();
        mandatory.setProperty("isRealMobile", "FALSE");
        mandatory.setProperty("deviceName", "iPhone 13");
        mandatory.setProperty("platformName", "ios");
        mandatory.setProperty("platformVersion", "14");
        grouped.put("Mandatory capabilities", mandatory);

        LinkedProperties debugging = new LinkedProperties();
        debugging.setProperty("video", "TRUE");
        debugging.setProperty("devicelog", "FALSE");
        debugging.setProperty("network", "FALSE");
        debugging.setProperty("console", "FALSE");
        grouped.put("Debugging Options", debugging);

        LinkedProperties testConfig = new LinkedProperties();
        testConfig.setProperty("build", "Untitled");
        testConfig.setProperty("name", "");
        testConfig.setProperty("project", "");
        testConfig.setProperty("app", "");
        testConfig.setProperty("queueTimeout", "600");
        testConfig.setProperty("idleTimeout", "120");
        testConfig.setProperty("deviceOrientation", "PORTRAIT");
        testConfig.setProperty("orientation", "auto");
        testConfig.setProperty("newCommandTimeout", "60");
        testConfig.setProperty("automationName", "UiAutomator2");
        testConfig.setProperty("eventTimings", "FALSE");
        testConfig.setProperty("otherApps", "[]");
        testConfig.setProperty("globalHttpProxy", "FALSE");
        testConfig.setProperty("region", "");
        testConfig.setProperty("waitForIdleTimeout", "");
        testConfig.setProperty("privateCloud", "FALSE");
        testConfig.setProperty("w3c", "FALSE");
        testConfig.setProperty("autoLaunch", "TRUE");
        grouped.put("Test configuration", testConfig);

        LinkedProperties realUser = new LinkedProperties();
        realUser.setProperty("language", "");
        realUser.setProperty("locale", "");
        realUser.setProperty("disableAnimation", "FALSE");
        realUser.setProperty("lambdaMaskCommands", "[]");
        realUser.setProperty("timezone", "");
        realUser.setProperty("geoLocation", "");
        realUser.setProperty("enableImageInjection", "FALSE");
        realUser.setProperty("media", "");
        realUser.setProperty("enableBluetooth", "FALSE");
        grouped.put("Real User conditions", realUser);

        LinkedProperties deviceConfig = new LinkedProperties();
        deviceConfig.setProperty("autoGrantPermissions", "FALSE");
        deviceConfig.setProperty("proxyUrl", "");
        grouped.put("Device configuration", deviceConfig);

        LinkedProperties networkConfig = new LinkedProperties();
        networkConfig.setProperty("tunnel", "FALSE");
        networkConfig.setProperty("tunnelName", "");
        networkConfig.setProperty("dedicatedProxy", "FALSE");
        networkConfig.setProperty("blockDomains", "[]");
        grouped.put("Network configuration", networkConfig);

        return grouped;
    }

    /**
     * Returns true if the given table row "property" cell value represents a
     * category header (rather than an actual capability key).
     */
    public static boolean isSectionHeader(String propertyCell) {
        return propertyCell != null
                && propertyCell.startsWith(SECTION_PREFIX)
                && propertyCell.endsWith(SECTION_SUFFIX);
    }

    public static String sectionHeader(String name) {
        return SECTION_PREFIX + name + SECTION_SUFFIX;
    }
}
