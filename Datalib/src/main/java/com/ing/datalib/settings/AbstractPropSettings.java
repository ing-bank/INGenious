
package com.ing.datalib.settings;

import com.ing.datalib.util.data.LinkedProperties;
import java.io.File;
import java.util.Properties;

/**
 *
 * 
 */
public class AbstractPropSettings extends LinkedProperties {

    private String location;

    private String name;

    public AbstractPropSettings(String location, String name) {
        this.location = location;
        this.name = name;
        load();
    }

    private void load() {
        File driverProp = new File(getLocation());
        if (driverProp.exists()) {
            PropUtils.load(this, driverProp);
        }
    }

    public void save() {
        PropUtils.save(this, getLocation());
    }

    public String getLocation() {
        return location + File.separator + name + ".Properties";
    }

    public void set(Properties prop) {
        this.clear();
        for (String key : prop.stringPropertyNames()) {
            setProperty(key, prop.getProperty(key));
        }
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setName(String name) {
        this.name = name;
    }

}
