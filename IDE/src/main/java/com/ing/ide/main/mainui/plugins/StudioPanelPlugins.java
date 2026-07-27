package com.ing.ide.main.mainui.plugins;

import com.ing.engine.plugin.loader.PluginLoader;
import com.ing.ingenious.api.contract.ui.StudioPanelApi;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;

/**
 * Discovers plugin-contributed Studio screens ({@link StudioPanelApi}) from the plugins
 * directory and exposes their manifest metadata to the UI.
 *
 * <p>Discovery reuses the engine's existing {@link PluginLoader}, so panel plugins are
 * packaged and loaded exactly like action plugins. Panel factories and components are
 * created lazily unless an older plugin requires an instance to supply missing metadata.
 */
public final class StudioPanelPlugins {
    /** Action-command prefix used by toolbar buttons to select a plugin screen. */
    public static final String ACTION_PREFIX = "Plugin Panel:";

    private static final Logger LOG = Logger.getLogger(StudioPanelPlugins.class.getName());

    private static List<Panel> cached;
    private static Map<String, Panel> cachedByIdentity;

    private StudioPanelPlugins() {}

    /**
     * Discovers panel declarations once and caches them.
     *
     * @return panel declarations sorted by order and title; empty when there are none
     */
    public static synchronized List<Panel> load() {
        if (cached != null) {
            return cached;
        }

        Map<String, Panel> found = new LinkedHashMap<>();
        List<Class<?>> entryClasses;
        try {
            entryClasses = PluginLoader.loadAllPluginsEntryClasses();
        } catch (RuntimeException ex) {
            // No plugins directory is the normal case for a stock install. Say so at INFO:
            // an empty toolbar with a silent log is indistinguishable from a broken load.
            LOG.log(Level.INFO, "No Studio panels loaded: {0}", ex.getMessage());
            cache(found);
            return cached;
        }

        for (Class<?> entryClass : entryClasses) {
            Panel panel = discover(entryClass);
            if (panel == null) {
                continue;
            }
            Panel existing = found.putIfAbsent(panel.getIdentity(), panel);
            if (existing != null) {
                LOG.log(
                    Level.WARNING,
                    "Skipping duplicate panel identity {0} from {1}",
                    new Object[] { panel.getIdentity(), entryClass.getName() }
                );
            }
        }

        cache(found);
        LOG.log(Level.INFO, "Studio panel discovery finished: {0} panel(s)", cached.size());
        return cached;
    }

    /**
     * Finds a discovered panel by its stable identity.
     *
     * @param identity the plugin ID or entry class name carried by the toolbar action
     * @return the panel declaration, or {@code null} when no declaration matches
     */
    public static synchronized Panel find(String identity) {
        load();
        return cachedByIdentity.get(identity);
    }

    private static Panel discover(Class<?> entryClass) {
        if (!StudioPanelApi.class.isAssignableFrom(entryClass)) {
            LOG.log(Level.FINE, "Plugin entry is not a Studio panel: {0}", entryClass.getName());
            return null;
        }

        StudioPanelManifest.Metadata metadata = StudioPanelManifest.read(entryClass);
        if (metadata.surface != null && !"swing".equalsIgnoreCase(metadata.surface)) {
            LOG.log(
                Level.INFO,
                "Skipping Studio panel {0}: unsupported surface {1}",
                new Object[] {
                    metadata.pluginId != null ? metadata.pluginId : entryClass.getName(),
                    metadata.surface
                }
            );
            return null;
        }

        Constructor<? extends StudioPanelApi> constructor = publicConstructor(entryClass);
        if (constructor == null) {
            return null;
        }

        StudioPanelApi fallbackInstance = null;
        String title = metadata.title;
        String tooltip = metadata.tooltip;
        if (title == null || tooltip == null) {
            try {
                fallbackInstance = constructor.newInstance();
                if (title == null) {
                    title = fallbackInstance.getTitle();
                }
                if (tooltip == null) {
                    tooltip = fallbackInstance.getTooltip();
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                LOG.log(
                    Level.WARNING,
                    "Cannot instantiate panel plugin " + entryClass.getName(),
                    ex
                );
                return null;
            }
        }

        if (title == null || title.trim().isEmpty()) {
            LOG.log(
                Level.WARNING,
                "Skipping panel plugin with a blank title: {0}",
                entryClass.getName()
            );
            return null;
        }
        title = title.trim();
        if (tooltip == null || tooltip.trim().isEmpty()) {
            tooltip = title;
        } else {
            tooltip = tooltip.trim();
        }

        Integer order = parseOrder(metadata.order, entryClass);
        String identity = metadata.pluginId != null ? metadata.pluginId : entryClass.getName();
        Panel panel = new Panel(
            identity,
            title,
            tooltip,
            order,
            metadata.pluginVersion,
            constructor,
            fallbackInstance
        );
        LOG.log(
            Level.INFO,
            "Discovered Studio panel {0} ({1}), version {2}",
            new Object[] {
                panel.getTitle(),
                panel.getIdentity(),
                metadata.pluginVersion != null ? metadata.pluginVersion : "unspecified"
            }
        );
        return panel;
    }

    @SuppressWarnings("unchecked")
    private static Constructor<? extends StudioPanelApi> publicConstructor(Class<?> entryClass) {
        if (
            !Modifier.isPublic(entryClass.getModifiers()) ||
            Modifier.isAbstract(entryClass.getModifiers())
        ) {
            LOG.log(
                Level.WARNING,
                "Skipping non-instantiable panel plugin: {0}",
                entryClass.getName()
            );
            return null;
        }
        try {
            return (Constructor<? extends StudioPanelApi>) entryClass.getConstructor();
        } catch (NoSuchMethodException | SecurityException ex) {
            LOG.log(
                Level.WARNING,
                "Panel plugin requires a public no-argument constructor: {0}",
                entryClass.getName()
            );
            return null;
        }
    }

    private static Integer parseOrder(String value, Class<?> entryClass) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            LOG.log(
                Level.WARNING,
                "Ignoring non-integer studioPanelOrder ''{0}'' for {1}",
                new Object[] { value, entryClass.getName() }
            );
            return null;
        }
    }

    private static void cache(Map<String, Panel> found) {
        List<Panel> sorted = new ArrayList<>(found.values());
        sorted.sort(
            Comparator
                .comparing(Panel::getOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Panel::getTitle)
                .thenComparing(Panel::getIdentity)
        );
        cached = Collections.unmodifiableList(sorted);

        Map<String, Panel> byIdentity = new LinkedHashMap<>();
        for (Panel panel : sorted) {
            byIdentity.put(panel.getIdentity(), panel);
        }
        cachedByIdentity = Collections.unmodifiableMap(byIdentity);
    }

    /**
     * Slide identity for a panel. Namespaced so a plugin cannot collide with a built-in
     * screen such as {@code TestDesign}.
     *
     * @param identity the plugin ID, or entry class name when no ID is declared
     * @return the slide name
     */
    public static String slideName(String identity) {
        return "Plugin:" + identity;
    }

    /**
     * A panel declaration whose factory and Swing component are activated on demand.
     */
    public static final class Panel {
        private final String identity;
        private final String title;
        private final String tooltip;
        private final Integer order;
        private final String version;
        private final Constructor<? extends StudioPanelApi> constructor;

        private StudioPanelApi factory;
        private JComponent component;
        private boolean activationAttempted;

        private Panel(
            String identity,
            String title,
            String tooltip,
            Integer order,
            String version,
            Constructor<? extends StudioPanelApi> constructor,
            StudioPanelApi factory
        ) {
            this.identity = identity;
            this.title = title;
            this.tooltip = tooltip;
            this.order = order;
            this.version = version;
            this.constructor = constructor;
            this.factory = factory;
        }

        public String getIdentity() {
            return identity;
        }

        public String getTitle() {
            return title;
        }

        public String getTooltip() {
            return tooltip;
        }

        public Integer getOrder() {
            return order;
        }

        public String getVersion() {
            return version;
        }

        /**
         * Creates the panel on first activation and returns the cached component thereafter.
         * A failed activation is not retried.
         *
         * @return the component, or {@code null} when construction failed
         */
        public synchronized JComponent activate() {
            if (activationAttempted) {
                return component;
            }
            activationAttempted = true;

            LOG.log(Level.INFO, "Activating Studio panel {0}", identity);
            try {
                if (factory == null) {
                    factory = constructor.newInstance();
                }
                component = factory.createPanel();
                if (component == null) {
                    LOG.log(Level.WARNING, "Panel plugin returned no component: {0}", identity);
                }
            } catch (ReflectiveOperationException | RuntimeException ex) {
                LOG.log(Level.WARNING, "Panel plugin failed during activation: " + identity, ex);
                component = null;
            }
            return component;
        }
    }
}
