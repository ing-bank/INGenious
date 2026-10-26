package com.ing.ingenious.api.contract.project;

import com.ing.ingenious.api.contract.settings.ProjectSettingsApi;

/**
 * Contract for a loaded automation project, backed by the framework's
 * {@code Project}.
 */
public interface ProjectApi {

    /** @return the filesystem location of the project */
    String getLocation();

    /** @return the project's metadata (id, name, version, custom data items) */
    ProjectInfoApi getInfo();

    /** @return the project's object repository (existence checks only) */
    ObjectRepositoryApi getObjectRepository();

    /** @return the project's environment-scoped test data provider */
    ProjectTestDataApi getTestData();

    /** @return the project's settings tree */
    ProjectSettingsApi getProjectSettings();

    /**
     * @param name the release name
     * @return the release with the given name, or {@code null} if not found
     */
    ReleaseApi getReleaseByName(String name);
}
