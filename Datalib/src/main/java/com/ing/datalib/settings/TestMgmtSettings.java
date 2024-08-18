
package com.ing.datalib.settings;

/**
 *
 * 
 */
public class TestMgmtSettings extends AbstractPropSettings {

    public TestMgmtSettings(String location) {
        super(location, "TestMgmtSettings");
    }

    public String getUpdateResultsToTM() {
        return getProperty("UpdateResultsToTM", "None");
    }

    public void setUpdateResultsToTM(String value) {
        setProperty("UpdateResultsToTM", value);
    }
}
