package com.ing.datalib.component.migration;

import com.ing.datalib.component.Project;

/**
 * A project-load migration unit: an ordered, idempotent step that brings an older project's
 * on-disk state in line with a newer model. Registered units run once per {@link Project#loadProject()}
 * (skipped in read-only mode, same as every other load-time migration), gated by whatever marker
 * the unit itself checks in {@link #appliesTo(Project)} - typically a settings flag flipped by
 * {@link #migrate(Project)} so a converted project is never re-migrated.
 *
 * <p>Introduced for the SAP connection-model rewrite (the {@code sap.model} marker in
 * {@code SapDefaults}): this interface and {@link ProjectMigrationRegistry} are the extension
 * point that follow-up feature registers its unit against - nothing here is SAP-specific, so any
 * future project-shape migration can reuse the same seam instead of hand-wiring another one-off
 * call into {@code Project.loadProject()}.
 */
public interface ProjectMigration {
    /** Stable id for logging - must never change once shipped, a migration's "have I run" marker may reference it. */
    String id();

    /** Lower runs first; ties broken by {@link #id()} for determinism. Default puts most units in the middle of the pack. */
    default int order() {
        return 100;
    }

    /** @return true if this project still needs {@link #migrate(Project)} - checked fresh on every load. */
    boolean appliesTo(Project project);

    /** Perform the migration. Must be safe to call only when {@link #appliesTo} is true, and should leave the project in a state where {@link #appliesTo} then returns false. */
    void migrate(Project project) throws Exception;
}
