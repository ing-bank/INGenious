
package com.ing.datalib.settings;

public class ExtentReportSettings extends AbstractPropSettings {
    
    public ExtentReportSettings(String location) {
        super(location, "extentreport");
        if (isEmpty()) {
            loadDefault();
        }
    }

    private void loadDefault() {
        put("HTML-Theme", "Dark");
    }
    
}
