
package com.ing.datalib.settings;

/**
 *
 * 
 */
public class DatabaseSettings extends AbstractPropSettings {

    public DatabaseSettings(String location) {
        super(location, "DBSettings");
        if (isEmpty()) {
            loadDefault();
        }
    }

    private void loadDefault() {
        put("db.connection.string", "jdbc:<Database>://<Host>:<Port>/<Database name>");
        put("db.user", "");
        put("db.secret", "");
        put("db.driver", "");
        put("db.commit", "True");
        put("db.timeout", "30");
    }
}
