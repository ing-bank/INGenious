
package com.ing.datalib.settings;

public class ContextSettings extends AbstractPropSettings {

    public ContextSettings(String location) {
        super(location, "contextsettings");
        if (isEmpty()) {
            loadDefault();
        }
    }

    private void loadDefault() {
        put("authenticateContext", "false");
        put("userID", "");
        put("password", "");
        put("useStorageState", "false");
        put("storageStatePath", "");
    }
    
}
