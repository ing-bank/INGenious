package com.ing.datalib.settings;

import com.ing.datalib.util.data.LinkedProperties;
import java.util.List;

/**
 * Resolution / policy layer over {@link SapConnections} + {@link SapDefaults}.
 * Shared by the Engine (runtime {@code SapSessionManager}) and the IDE
 * "SAP Connections" settings tab.
 */
public class SapConfigRegistry {
    private final SapConnections store;
    private final SapDefaults defaults;

    public SapConfigRegistry(SapConnections store, SapDefaults defaults) {
        this.store = store;
        this.defaults = defaults;
    }

    /** @return every configured connection alias. */
    public List<String> listNames() {
        return store.getSapList();
    }

    /** @return {@code true} when {@code alias} names a configured SAP connection. */
    public boolean isSapTarget(String alias) {
        return alias != null && listNames().contains(alias.trim());
    }

    /** @return raw properties for {@code alias}, or {@code null} when absent. */
    public LinkedProperties get(String alias) {
        return store.getSapPropertiesFor(alias);
    }

    public boolean hasDefault() {
        try {
            return resolveDefault() != null;
        } catch (SapConfigException ex) {
            return false;
        }
    }

    /**
     * Resolves a SAP-action input to a concrete connection alias.
     * <ul>
     *   <li>blank &rarr; the project default (see {@link #resolveDefault()})</li>
     *   <li>{@code #alias} or {@code alias} &rarr; that alias</li>
     * </ul>
     *
     * @throws SapConfigException when blank and no default can be determined
     */
    public String resolveAlias(String rawInput) {
        String in = rawInput == null ? "" : rawInput.trim();
        if (in.isEmpty()) {
            return resolveDefault();
        }
        if (in.startsWith("#")) {
            in = in.substring(1).trim();
        }
        return in;
    }

    /**
     * @return the project default connection alias
     * @throws SapConfigException when no default is set and the connection set is
     *         empty or ambiguous
     */
    public String resolveDefault() {
        String explicit = defaults.getDefaultConnection();
        if (explicit != null && store.getSapPropertiesFor(explicit) != null) {
            return explicit;
        }
        List<String> names = listNames();
        if (names.size() == 1) {
            return names.get(0);
        }
        if (names.isEmpty()) {
            throw new SapConfigException(
                "No SAP connections are configured. Add one under Settings/SAP/ " +
                "(or the SAP Connections settings tab)."
            );
        }
        throw new SapConfigException(
            "No default SAP connection set and more than one is configured " +
            names +
            ". Set one as default, or pass an explicit #alias."
        );
    }

    public SapConnections getStore() {
        return store;
    }

    public SapDefaults getDefaults() {
        return defaults;
    }
}
