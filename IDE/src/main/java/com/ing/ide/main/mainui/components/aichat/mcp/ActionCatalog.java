package com.ing.ide.main.mainui.components.aichat.mcp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Loads the set of valid INGenious action keywords from
 * {@code Configuration/StepMap.csv} (first column of each row). Used by the
 * tool server to validate that the model only emits real actions, rejecting
 * hallucinated keywords before any mutation.
 */
public final class ActionCatalog {
    private static final Logger LOG = Logger.getLogger(ActionCatalog.class.getName());

    private static final File STEP_MAP_FILE = new File(
        "Configuration" + File.separator + "StepMap.csv"
    );

    private final Set<String> actions = new LinkedHashSet<>();

    public ActionCatalog() {
        load();
    }

    private void load() {
        if (!STEP_MAP_FILE.exists()) {
            LOG.log(
                Level.WARNING,
                "StepMap.csv not found at {0}; action validation disabled",
                STEP_MAP_FILE.getAbsolutePath()
            );
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(STEP_MAP_FILE))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false; // skip "Step,Description,Expected Result"
                    continue;
                }
                int comma = line.indexOf(',');
                String action = (comma >= 0 ? line.substring(0, comma) : line).trim();
                if (!action.isEmpty()) {
                    actions.add(action.toLowerCase());
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.WARNING, "Failed to load StepMap.csv", ex);
        }
    }

    /**
     * Returns {@code true} if the given action keyword exists in the catalog.
     * When the catalog could not be loaded, validation is permissive (returns
     * {@code true}) so the feature degrades gracefully.
     */
    public boolean isValid(String action) {
        if (actions.isEmpty()) {
            return true;
        }
        return action != null && actions.contains(action.trim().toLowerCase());
    }

    public Set<String> all() {
        return Collections.unmodifiableSet(actions);
    }

    public boolean isLoaded() {
        return !actions.isEmpty();
    }
}
