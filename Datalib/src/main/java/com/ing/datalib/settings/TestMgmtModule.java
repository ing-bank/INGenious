
package com.ing.datalib.settings;

import com.ing.datalib.settings.testmgmt.Option;
import com.ing.datalib.settings.testmgmt.TestMgModule;
import com.ing.datalib.util.data.FileScanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * 
 */
public class TestMgmtModule {

    private List<TestMgModule> modules;

    private String location;

    private final ObjectMapper objMapper;

    public TestMgmtModule(String location) {
        this.location = location;
        this.objMapper = new ObjectMapper();
        modules = new ArrayList<>();
        load();
    }

    private void load() {
        File modulesFile = new File(getLocation());
        try {
            if (modulesFile.exists()) {
                modules = objMapper.readValue(modulesFile,
                        objMapper.getTypeFactory().constructCollectionType(List.class, TestMgModule.class));
            } else {
                modules = objMapper.readValue(FileScanner.getResourceString("TMModules.json"),
                        objMapper.getTypeFactory().constructCollectionType(List.class, TestMgModule.class));
            }
        } catch (IOException ex) {
            Logger.getLogger(TestMgmtModule.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public List<TestMgModule> getModules() {
        return modules;
    }

    public Map<String, String> asMap() {
        Map<String, String> map = new HashMap();
        for (TestMgModule module : modules) {
            for (Option option : module.getOptions()) {
                map.put(option.getName(), option.getValue());
            }
        }
        return map;
    }

    public void addModule(String newModuleName) {
        if (getModule(newModuleName) == null) {
            modules.add(new TestMgModule(newModuleName));
        }
    }

    public void removeModule(String moduleName) {
        if (getModule(moduleName) != null) {
            modules.remove(getModule(moduleName));
        }
    }

    public List<String> getModuleNames() {
        List<String> moduleNames = new ArrayList<>();
        for (TestMgModule module : modules) {
            moduleNames.add(module.getModule());
        }
        return moduleNames;
    }

    public TestMgModule getModule(String moduleName) {
        for (TestMgModule module : modules) {
            if (module.getModule().equals(moduleName)) {
                return module;
            }
        }
        return null;
    }

    public void putValues(String moduleName, List<Option> prop) {
        addModule(moduleName);
        TestMgModule module = getModule(moduleName);
        module.getOptions().clear();
        for (Option key : prop) {
            module.getOptions().add(new Option(key.getName(), key.getValue()));
        }
    }

    public String getLocation() {
        return location + File.separator + "TMModules.json";
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public final void save() {
        File emFile = new File(getLocation());
        if (!emFile.getParentFile().exists()) {
            emFile.getParentFile().mkdirs();
        }
        try {
            objMapper.writeValue(emFile, modules);
        } catch (IOException ex) {
            Logger.getLogger(TestMgmtModule.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
