package com.ing.ingenious.api.contract.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.ing.ingenious.api.contract.settings.ProjectSettingsApi;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Contract test for {@link ProjectApi}. Verifies the interface dispatches to
 * the expected sub-accessor instances.
 */
class ProjectApiTest {

    private final ProjectInfoApi info = proxy(ProjectInfoApi.class);
    private final ObjectRepositoryApi objectRepository = proxy(ObjectRepositoryApi.class);
    private final ProjectTestDataApi testData = proxy(ProjectTestDataApi.class);
    private final ProjectSettingsApi projectSettings = proxy(ProjectSettingsApi.class);
    private final Map<String, ReleaseApi> releases = new HashMap<>();

    private final ProjectApi project = new ProjectApi() {
        @Override
        public String getLocation() {
            return "/projects/MyProject";
        }

        @Override
        public ProjectInfoApi getInfo() {
            return info;
        }

        @Override
        public ObjectRepositoryApi getObjectRepository() {
            return objectRepository;
        }

        @Override
        public ProjectTestDataApi getTestData() {
            return testData;
        }

        @Override
        public ProjectSettingsApi getProjectSettings() {
            return projectSettings;
        }

        @Override
        public ReleaseApi getReleaseByName(String name) {
            return releases.get(name);
        }
    };

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[] { type },
            (proxyInstance, method, args) -> null
        );
    }

    @Test
    void shouldDispatchToTheExpectedSubAccessors() {
        assertEquals("/projects/MyProject", project.getLocation());
        assertSame(info, project.getInfo());
        assertSame(objectRepository, project.getObjectRepository());
        assertSame(testData, project.getTestData());
        assertSame(projectSettings, project.getProjectSettings());
    }

    @Test
    void getReleaseByNameShouldReturnNullWhenNotFound() {
        assertEquals(null, project.getReleaseByName("Unknown"));
    }

    @Test
    void getReleaseByNameShouldReturnTheRegisteredRelease() {
        ReleaseApi release = proxy(ReleaseApi.class);
        releases.put("Release1", release);
        assertSame(release, project.getReleaseByName("Release1"));
    }
}
