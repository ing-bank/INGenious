
package com.ing.ide.main.explorer.settings;

import com.ing.ide.util.data.FileScanner;
import com.ing.ide.util.logging.UILogger;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

/**
 *
 * 
 */
public class ReportingModuleSettings {

    private static final String DBNAME = "XPLOR_SETTINGS";
    public String[] fieldHeaders = {"moduleId", "fieldId", "show", "fieldName", "fieldType", "fieldValue"},
            fieldDefValues = {"", "", "", "", "", ""},
            moduleHeaders = {"moduleId"},
            moduleDefValues = {""};
    private static ReportingModuleSettings settings;
    JSONObject data;
    private static final org.slf4j.Logger LOG = UILogger.getLogger(ReportingModuleSettings.class.getName());

    public ReportingModuleSettings() {
        init();
    }

    public static ReportingModuleSettings get() {
        if (settings == null) {
            settings = new ReportingModuleSettings();
        }
        return settings;
    }

    public void init() {
        try {
            String rawData = FileScanner.readFile(new File(getFile()));
            this.data = (JSONObject) JSONValue.parse(rawData);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    /**
     * Delete the particular Module record from json data
     *
     * @param moduleId
     */
    public void cleanModule(String moduleId) {
        ((JSONObject) data.get("fields")).remove(moduleId);
    }

    /**
     * clear all the modules
     */
    public void cleanModules() {
        ((JSONArray) data.get("modules")).clear();
    }

    /**
     * Delete the particular Field record from json data
     *
     * @param moduleID
     * @param fieldId
     */
    public void DeleteField(String moduleID, String fieldId) {
        ((JSONObject) ((JSONObject) data.get("fields")).get(moduleID)).remove(fieldId);
    }

    /**
     * returns the list of modules
     *
     * @param moduleId
     * @return
     */
    public List<HashMap<String, String>> getFields(String moduleId) {
        List<HashMap<String, String>> fields = new ArrayList<>();
        JSONObject moduleData = (JSONObject) ((JSONObject) data.get("fields")).get(moduleId);
        for (Object key : moduleData.keySet()) {
            HashMap<String, String> properties = (HashMap<String, String>) moduleData.get(key);
            fields.add(properties);
        }
        return fields;
    }

    /**
     * returns the list of defect modules
     *
     * @return
     */
    public List<HashMap<String, String>> getModules() {
        List<HashMap<String, String>> fields = new ArrayList<>();
        for (Object module : (JSONArray) data.get("modules")) {
            HashMap<String, String> properties = new HashMap<>();
            for (String val : this.moduleHeaders) {
                properties.put(val, (String) module);
            }
            fields.add(properties);
        }

        return fields;
    }

    /**
     * @return the _jsonFile
     */
    public String getFile() throws IOException {
       return new File(System.getProperty("user.dir") + File.separator
                + "Configuration" + File.separator + DBNAME + ".json").getCanonicalPath();
    }

    /**
     * update the fields table with new values
     *
     * @param fields
     * @param moduleId
     */
    public void updateFields(List<HashMap<String, String>> fields, String moduleId) throws IOException {

        for (HashMap<String, String> properties : fields) {
            ((JSONObject) ((JSONObject) data.get("fields")).get(moduleId))
                    .put(properties.get(fieldHeaders[1]), properties);
        }
        save();
    }

    /**
     * update the modules with the new values
     *
     * @param modules
     */
    public void updateModules(List<HashMap<String, String>> modules) throws IOException{
        this.cleanModules();
        for (HashMap<String, String> properties : modules) {
            ((JSONArray) data.get("modules")).add(properties.get(moduleHeaders[0]));
        }
        save();
    }

    public void save() throws IOException{
        FileScanner.writeFile(new File(getFile()), data.toJSONString());
    }
}
