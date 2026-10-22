package com.ing.datalib.component;

import com.ing.datalib.model.ProjectInfo;
import com.ing.datalib.model.ProjectInfoApiImpl;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.or.ObjectRepositoryApiImpl;
import com.ing.datalib.settings.ProjectSettingsApiImpl;
import com.ing.ingenious.api.contract.project.ObjectRepositoryApi;
import com.ing.ingenious.api.contract.project.ProjectApi;
import com.ing.ingenious.api.contract.project.ProjectInfoApi;
import com.ing.ingenious.api.contract.project.ProjectTestDataApi;
import com.ing.ingenious.api.contract.project.ReleaseApi;
import com.ing.ingenious.api.contract.settings.ProjectSettingsApi;

/**
 * Delegates the {@link ProjectApi} plugin contract to a specific {@link Project}
 * instance.
 */
public final class ProjectApiImpl implements ProjectApi {
    private final Project project;

    public ProjectApiImpl(Project project) {
        this.project = project;
    }

    @Override
    public String getLocation() {
        return project.getLocation();
    }

    @Override
    public ProjectInfoApi getInfo() {
        ProjectInfo info = project.getInfo();
        return info == null ? null : new ProjectInfoApiImpl(info);
    }

    @Override
    public ObjectRepositoryApi getObjectRepository() {
        ObjectRepository or = project.getObjectRepository();
        return or == null ? null : new ObjectRepositoryApiImpl(or);
    }

    @Override
    public ProjectTestDataApi getTestData() {
        EnvTestData testData = project.getTestData();
        return testData == null ? null : new ProjectTestDataApiImpl(testData);
    }

    @Override
    public ProjectSettingsApi getProjectSettings() {
        return new ProjectSettingsApiImpl(project.getProjectSettings());
    }

    @Override
    public ReleaseApi getReleaseByName(String name) {
        Release release = project.getReleaseByName(name);
        return release == null ? null : new ReleaseApiImpl(release);
    }
}
