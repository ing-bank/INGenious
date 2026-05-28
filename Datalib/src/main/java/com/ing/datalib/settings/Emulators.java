
package com.ing.datalib.settings;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.datalib.settings.emulators.Emulator;
import com.ing.datalib.util.data.LinkedProperties;

/**
 *
 * 
 */
public class Emulators {

    private List<Emulator> emulators;

    private String location;

    private final ObjectMapper objMapper;

    public Emulators(String location) {
        this.location = location;
        this.objMapper = new ObjectMapper();
        emulators = new ArrayList<>();
        load();
    }

    private void load() {
        File emFile = new File(getLocation());
        if (emFile.exists()) {
            try {
                emulators = objMapper.readValue(emFile, objMapper.getTypeFactory().constructCollectionType(List.class, Emulator.class));
                // Ensure SAP emulator exists by default only for actual projects (Settings folder)
                if (isRealProjectSettings()) {
                    ensureDefaultEmulators();
                }
            } catch (IOException ex) {
                Logger.getLogger(Emulators.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
    
    /**
     * Check if this is a real project's Settings folder (not a test directory)
     */
    private boolean isRealProjectSettings() {
        // Real projects have "Settings" folder name in path
        return location.endsWith("Settings");
    }
    
    private void ensureDefaultEmulators() {
        // Add SAP emulator if it doesn't exist
        if (getEmulator("SAP") == null) {
            addEmulator("SAP");
            save();
        }
    }
    
    public void reload(){
        emulators.clear();
        load();
    }

    public void addAppiumEmulator(String emulatorName, String url) {
        addEmulator(emulatorName);
        getEmulator(emulatorName).setType("Remote URL");
        getEmulator(emulatorName).setRemoteUrl(url);
    }

    public void addEmulator(String emulatorName) {
        if (getEmulator(emulatorName) == null) {
            emulators.add(new Emulator(emulatorName));
        }
    }
  
    public LinkedProperties defaultEmulatorCap(){
        LinkedProperties props = new LinkedProperties();
        // Add default key-value pairs
        props.setProperty("deviceName", "");
        props.setProperty("platformName", "");
        props.setProperty("platformVersion", "");
        props.setProperty("automationName", "");
        
        return props;
    }

    /**
     * Returns default SAP-specific capabilities for SAP browser configuration
     */
    public LinkedProperties defaultSAPCapability() {
        LinkedProperties props = new LinkedProperties();
        // SAP-specific properties as defined in Capabilities.createFile()
        props.setProperty("app", "C:\\Program Files\\SAP\\FrontEnd\\SAPGUI\\saplogon.exe");
        props.setProperty("libraryPath", "lib/jacob-1.21");
        props.setProperty("dllPath", "lib/jacob-1.21/jacob-1.21-x64.dll");
        props.setProperty("connectionName", "SAP_CONN_NAME");
        props.setProperty("platformName", "Windows");
        
        return props;
    }

    public void deleteEmulator(String emulatorName) {
        Emulator emul = getEmulator(emulatorName);
        if (emul != null) {
            emulators.remove(emul);
        }
    }

    public Boolean renameEmulator(String oldName, String newName) {
        Emulator old = getEmulator(oldName);
        if (old != null) {
            Emulator emul = getEmulator(newName);
            if (emul == null) {
                old.setName(newName);
                return true;
            }
        }
        return false;
    }

    public List<Emulator> getEmulators() {
        return emulators;
    }

    public List<String> getAppiumEmulatorNames() {
        List<String> emulatorNames = new ArrayList<>();
        for (Emulator emulator : emulators) {
            if (Objects.equals(emulator.getType(), "Remote URL")) {
                emulatorNames.add(emulator.getName());
            }
        }
        return emulatorNames;
    }

    public List<String> getEmulatorNames() {
        List<String> emulatorNames = new ArrayList<>();
        for (Emulator emulator : emulators) {
            emulatorNames.add(emulator.getName());
        }
        return emulatorNames;
    }

    public Emulator getEmulator(String emulatorName) {
        for (Emulator emulator : emulators) {
            if (emulator.getName().equals(emulatorName)) {
                return emulator;
            }
        }
        return null;
    }

    public void save() {
        File emFile = new File(getLocation());
        if (!emFile.getParentFile().exists()) {
            emFile.getParentFile().mkdirs();
        }
        try {
            objMapper.writeValue(emFile, emulators);
        } catch (IOException ex) {
            Logger.getLogger(Emulators.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public String getLocation() {
        return location + File.separator + "Emulators.json";
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
