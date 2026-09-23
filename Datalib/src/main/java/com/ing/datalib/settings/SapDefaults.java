package com.ing.datalib.settings;

/**
 * Project-level SAP metadata, stored in {@code Settings/SAP/_config.properties}
 * (the {@code _} prefix keeps it out of {@link SapConnections#getSapList()}).
 *
 * <ul>
 *   <li>{@code defaultSap} &mdash; the connection alias used when a SAP action's
 *       input is blank.</li>
 *   <li>{@code sap.model} &mdash; {@code legacy} until the project is migrated off
 *       the old {@code Browser = "SAP"} model, then {@code connection}.</li>
 * </ul>
 */
public class SapDefaults extends AbstractPropSettings {
    public static final String KEY_DEFAULT = "defaultSap";
    public static final String KEY_MODEL = "sap.model";

    public static final String MODEL_LEGACY = "legacy";
    public static final String MODEL_CONNECTION = "connection";

    public SapDefaults(String location) {
        super(location, "SAP" + java.io.File.separator + "_config");
    }

    /** @return the default connection alias, or {@code null}/empty when unset. */
    public String getDefaultConnection() {
        String v = getProperty(KEY_DEFAULT);
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }

    public void setDefaultConnection(String alias) {
        setProperty(KEY_DEFAULT, alias == null ? "" : alias);
    }

    public String getModel() {
        String v = getProperty(KEY_MODEL);
        return (v == null || v.trim().isEmpty()) ? MODEL_LEGACY : v.trim();
    }

    public void setModel(String model) {
        setProperty(KEY_MODEL, model);
    }

    public boolean isLegacyModel() {
        return MODEL_LEGACY.equalsIgnoreCase(getModel());
    }
}
