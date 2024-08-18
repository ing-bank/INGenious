
package com.ing.datalib.settings;

public class ReportPortalSettings extends AbstractPropSettings {
    
    public ReportPortalSettings(String location) {
        super(location, "reportportal");
        if (isEmpty()) {
            loadDefault();
        }
    }

    private void loadDefault() {
        put("rp.endpoint", "http://localhost:8080");
        put("rp.uuid", "");
        put("rp.project", "");
    }
    
}
