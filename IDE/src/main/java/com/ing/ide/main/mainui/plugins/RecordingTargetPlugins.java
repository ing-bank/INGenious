package com.ing.ide.main.mainui.plugins;

import com.ing.engine.plugin.loader.PluginLoader;
import com.ing.ingenious.api.contract.ui.RecordingTarget;
import com.ing.ingenious.api.contract.ui.RecordingTargetApi;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Asks plugins where a recording belongs before Studio asks the user.
 *
 * <p>Discovery reuses the engine's existing {@link PluginLoader}, so a plugin that answers this
 * question is packaged and loaded exactly like an action plugin. Entry classes are discovered
 * and instantiated once, lazily, on the first recording of the session — a stock install with
 * no plugins pays nothing.
 *
 * <p>Nothing here can stop a recording: a missing plugins directory, a class that will not
 * instantiate, and a plugin that throws all end the same way — no answer, and the recorder's own
 * target chooser opens as it always has.
 */
public final class RecordingTargetPlugins {
    private static final Logger LOG = Logger.getLogger(RecordingTargetPlugins.class.getName());

    private static List<RecordingTargetApi> cached;

    private RecordingTargetPlugins() {}

    /**
     * The target the first plugin willing to answer proposes for the next recording.
     *
     * @return a target, or {@code null} when no plugin has an answer and the user should choose
     */
    public static RecordingTarget currentTarget() {
        for (RecordingTargetApi provider : load()) {
            RecordingTarget target;
            try {
                target = provider.getRecordingTarget();
            } catch (RuntimeException ex) {
                // A plugin's opinion is never worth failing a recording over.
                LOG.log(
                    Level.WARNING,
                    "Recording target plugin failed, asking the user instead: " +
                    provider.getClass().getName(),
                    ex
                );
                continue;
            }
            if (target != null) {
                LOG.log(
                    Level.INFO,
                    "Recording target {0} supplied by {1}",
                    new Object[] { target, provider.getClass().getName() }
                );
                return target;
            }
        }
        return null;
    }

    /**
     * Discovers and instantiates the providers once, then caches them.
     *
     * @return the providers, empty when there are none
     */
    private static synchronized List<RecordingTargetApi> load() {
        if (cached != null) {
            return cached;
        }

        List<RecordingTargetApi> found = new ArrayList<>();
        List<Class<?>> entryClasses;
        try {
            entryClasses = PluginLoader.loadAllPluginsEntryClasses();
        } catch (RuntimeException ex) {
            // No plugins directory is the normal case for a stock install.
            LOG.log(Level.INFO, "No recording target plugins loaded: {0}", ex.getMessage());
            cached = Collections.emptyList();
            return cached;
        }

        for (Class<?> entryClass : entryClasses) {
            if (!RecordingTargetApi.class.isAssignableFrom(entryClass)) {
                continue;
            }
            try {
                found.add((RecordingTargetApi) entryClass.getConstructor().newInstance());
                LOG.log(Level.INFO, "Discovered recording target plugin {0}", entryClass.getName());
            } catch (ReflectiveOperationException | RuntimeException ex) {
                LOG.log(
                    Level.WARNING,
                    "Cannot instantiate recording target plugin " + entryClass.getName(),
                    ex
                );
            }
        }

        cached = Collections.unmodifiableList(found);
        return cached;
    }
}
