package com.ing.datalib.settings;

import com.ing.datalib.util.data.LinkedProperties;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Per-project store of named SAP connections, one {@code &lt;alias&gt;.properties} file
 * per connection under {@code Settings/SAP/}. Modelled on {@link Capabilities} /
 * {@link DBProperties}: a connection is opened at run time by an explicit
 * {@code SAP.initConnection} step (like {@code Database.initDBConnection}), not by a
 * per-iteration driver.
 *
 * <p>Files whose base name starts with {@code _} (e.g. {@code _config.properties},
 * managed by {@link SapDefaults}) are reserved metadata and never surface as a
 * connection alias.
 */
public class SapConnections {
    /** Connection-config keys written into a freshly created file. */
    public static final String KEY_CONNECTION_NAME = "connectionName";
    public static final String KEY_APP = "app";
    public static final String KEY_MULTI_LOGON = "multiLogon";
    public static final String KEY_SESSION_MODE = "sessionMode";
    public static final String KEY_CLIENT = "client";
    public static final String KEY_USER = "user";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_LANGUAGE = "language";

    private static final String DEFAULT_APP =
        "C:\\Program Files\\SAP\\FrontEnd\\SAPGUI\\saplogon.exe";

    private final Map<String, LinkedProperties> connections = new HashMap<>();

    private String location;

    private boolean readOnlyMode = false;

    public SapConnections(String location) {
        this(location, false);
    }

    public SapConnections(String location, boolean readOnlyMode) {
        this.location = location;
        this.readOnlyMode = readOnlyMode;
        createSapFolder();
        load();
    }

    public Map<String, LinkedProperties> getConnections() {
        return connections;
    }

    /** @return the connection aliases on disk, excluding reserved {@code _*} files. */
    public ArrayList<String> getSapList() {
        load();
        return new ArrayList<>(connections.keySet());
    }

    /** @return the raw properties for {@code alias}, or {@code null} when absent. */
    public LinkedProperties getSapPropertiesFor(String alias) {
        return connections.get(alias);
    }

    /**
     * Returns the property bag for {@code alias}, creating (and registering) an
     * empty one when missing. Used by the CLI {@code -setEnv "sap.&lt;alias&gt;.&lt;key&gt;=..."}
     * override so it works even when the connection file does not pre-exist.
     */
    public LinkedProperties getOrCreateSapPropertiesFor(String alias) {
        LinkedProperties props = connections.get(alias);
        if (props == null) {
            props = new LinkedProperties();
            connections.put(alias, props);
        }
        return props;
    }

    private void load() {
        connections.clear();
        File dir = new File(getLocation());
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            String fileName = file.getName();
            if (!fileName.toLowerCase().endsWith(".properties")) {
                continue;
            }
            if (fileName.startsWith("_")) {
                continue; // reserved metadata (see SapDefaults)
            }
            String alias = fileName.substring(0, fileName.length() - ".properties".length());
            connections.put(alias, PropUtils.load(file));
        }
    }

    /** Creates a connection file pre-filled with the standard key set. */
    public void addSap(String alias) {
        addSap(alias, defaultConnectionProperties());
    }

    public void addSap(String alias, LinkedProperties props) {
        connections.put(alias, props);
        save(alias);
    }

    public void updateSapProperty(String alias, String key, String value) {
        getOrCreateSapPropertiesFor(alias).update(key, value);
    }

    public LinkedProperties defaultConnectionProperties() {
        LinkedProperties props = new LinkedProperties();
        props.setProperty(KEY_CONNECTION_NAME, "");
        props.setProperty(KEY_APP, DEFAULT_APP);
        props.setProperty(KEY_MULTI_LOGON, "keepOthers");
        props.setProperty(KEY_SESSION_MODE, "newSession");
        props.setProperty(KEY_CLIENT, "");
        props.setProperty(KEY_USER, "");
        props.setProperty(KEY_PASSWORD, "");
        props.setProperty(KEY_LANGUAGE, "");
        return props;
    }

    public void save() {
        for (Map.Entry<String, LinkedProperties> entry : connections.entrySet()) {
            PropUtils.save(entry.getValue(), getSapLocation(entry.getKey()));
        }
    }

    public void save(String alias) {
        if (connections.containsKey(alias)) {
            PropUtils.save(connections.get(alias), getSapLocation(alias));
        }
    }

    public void delete(String alias) {
        if (connections.containsKey(alias)) {
            File file = new File(getSapLocation(alias));
            if (file.exists()) {
                file.delete();
            }
            connections.remove(alias);
        }
    }

    /** Renames a connection file and its in-memory entry. Fails on target collision. */
    public Boolean rename(String oldAlias, String newAlias) {
        if (connections.containsKey(oldAlias) && !connections.containsKey(newAlias)) {
            File file = new File(getSapLocation(oldAlias));
            if (file.exists()) {
                if (file.renameTo(new File(getSapLocation(newAlias)))) {
                    connections.put(newAlias, connections.remove(oldAlias));
                    return true;
                }
            } else {
                connections.put(newAlias, connections.remove(oldAlias));
                return true;
            }
        }
        return false;
    }

    public String getSapLocation(String alias) {
        return getLocation() + File.separator + alias + ".properties";
    }

    public String getLocation() {
        return location + File.separator + "SAP";
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setReadOnlyMode(boolean readOnly) {
        this.readOnlyMode = readOnly;
    }

    protected boolean isReadOnlyMode() {
        return readOnlyMode;
    }

    private void createSapFolder() {
        File dir = new File(getLocation());
        if (!dir.exists() && !readOnlyMode) {
            dir.mkdirs();
        }
    }
}
