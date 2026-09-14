package com.ing.datalib.component.migration;

import com.ing.datalib.component.Project;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Holds the registered {@link ProjectMigration} units and runs them, in order, against a project
 * on load. Empty by default - Phase 3 of the SAP connections redesign builds this seam so the
 * separate legacy-project-rewrite follow-up can register its unit here without touching
 * {@code Project.loadProject()} again.
 */
public final class ProjectMigrationRegistry {
    private static final Logger LOG = Logger.getLogger(ProjectMigrationRegistry.class.getName());
    private static final List<ProjectMigration> UNITS = new CopyOnWriteArrayList<>();

    private ProjectMigrationRegistry() {}

    /** Registers a migration unit. Registration order does not matter - units always run sorted by {@link ProjectMigration#order()} then {@link ProjectMigration#id()}. */
    public static void register(ProjectMigration unit) {
        if (unit == null) {
            return;
        }
        UNITS.removeIf(existing -> existing.id().equals(unit.id()));
        UNITS.add(unit);
    }

    /** Removes a previously registered unit by id - mainly for tests that don't want cross-test pollution of the shared registry. */
    public static void unregister(String id) {
        UNITS.removeIf(existing -> existing.id().equals(id));
    }

    public static List<ProjectMigration> units() {
        List<ProjectMigration> sorted = new ArrayList<>(UNITS);
        sorted.sort(
            Comparator.comparingInt(ProjectMigration::order).thenComparing(ProjectMigration::id)
        );
        return Collections.unmodifiableList(sorted);
    }

    /**
     * Runs every registered unit that {@link ProjectMigration#appliesTo(Project)} against the
     * project, in order. A unit that throws is logged and skipped - one bad migration must never
     * abort project load or block the units after it.
     */
    public static void runAll(Project project) {
        for (ProjectMigration unit : units()) {
            try {
                if (unit.appliesTo(project)) {
                    unit.migrate(project);
                }
            } catch (Exception ex) {
                LOG.log(Level.WARNING, "Project migration '" + unit.id() + "' failed", ex);
            }
        }
    }
}
