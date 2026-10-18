package com.ing.ingenious.api.contract.paths;

/**
 * Root contract for all file/folder path utilities available to plugins,
 * grouped by concern. Implementations back these methods with INGenious's
 * own path-resolution logic, so paths used by a plugin stay consistent with
 * those used internally by the framework's reporting and execution code.
 * <p>
 * Most paths under {@link #project()}, {@link #results()}, and
 * {@link #reportOutputs()} depend on a currently loaded project and/or an
 * active run; calling them before that context is established may produce
 * paths built from empty or default values. {@link #app()} and
 * {@link #reportTemplates()} are independent of any project or run.
 * </p>
 */
public interface PathsApi {

    /** @return paths within the currently loaded project (object repositories, TestPlan/TestLab/TestCase tree) */
    ProjectPathsApi project();

    /** @return installation-level paths (app root, lib, configuration, addons, engine jar) */
    AppPathsApi app();

    /** @return the current run's results paths (results folder, logs, videos, accessibility) */
    ResultsPathsApi results();

    /** @return static report template and resource paths */
    ReportTemplatePathsApi reportTemplates();

    /** @return generated report output file paths, for both the static templates and the current run */
    ReportOutputPathsApi reportOutputs();
}
