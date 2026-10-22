package com.ing.datalib.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ing.datalib.model.ProjectInfo;
import com.ing.datalib.or.ObjectRepository;
import com.ing.datalib.settings.ProjectSettings;
import org.testng.annotations.Test;

/**
 * Tests for {@link ProjectApiImpl} — verifies each accessor delegates to the
 * wrapped {@link Project} instance, building a fresh child wrapper each call.
 */
public class ProjectApiImplTest {

    @Test
    public void getLocationShouldDelegate() {
        Project project = mock(Project.class);
        when(project.getLocation()).thenReturn("/projects/MyProject");

        assertThat(new ProjectApiImpl(project).getLocation()).isEqualTo("/projects/MyProject");
    }

    @Test
    public void getInfoShouldWrapTheUnderlyingProjectInfo() {
        Project project = mock(Project.class);
        ProjectInfo info = mock(ProjectInfo.class);
        when(project.getInfo()).thenReturn(info);

        assertThat(new ProjectApiImpl(project).getInfo()).isNotNull();
    }

    @Test
    public void getInfoShouldReturnNullWhenAbsent() {
        Project project = mock(Project.class);
        when(project.getInfo()).thenReturn(null);

        assertThat(new ProjectApiImpl(project).getInfo()).isNull();
    }

    @Test
    public void getObjectRepositoryShouldWrapTheUnderlyingRepository() {
        Project project = mock(Project.class);
        ObjectRepository or = mock(ObjectRepository.class);
        when(project.getObjectRepository()).thenReturn(or);

        assertThat(new ProjectApiImpl(project).getObjectRepository()).isNotNull();
    }

    @Test
    public void getTestDataShouldWrapTheUnderlyingEnvTestData() {
        Project project = mock(Project.class);
        EnvTestData testData = mock(EnvTestData.class);
        when(project.getTestData()).thenReturn(testData);

        assertThat(new ProjectApiImpl(project).getTestData()).isNotNull();
    }

    @Test
    public void getProjectSettingsShouldWrapTheUnderlyingSettings() {
        Project project = mock(Project.class);
        ProjectSettings settings = mock(ProjectSettings.class);
        when(project.getProjectSettings()).thenReturn(settings);

        assertThat(new ProjectApiImpl(project).getProjectSettings()).isNotNull();
    }

    @Test
    public void getReleaseByNameShouldWrapTheUnderlyingRelease() {
        Project project = mock(Project.class);
        Release release = mock(Release.class);
        when(project.getReleaseByName("Release1")).thenReturn(release);

        assertThat(new ProjectApiImpl(project).getReleaseByName("Release1")).isNotNull();
    }

    @Test
    public void getReleaseByNameShouldReturnNullWhenNotFound() {
        Project project = mock(Project.class);
        when(project.getReleaseByName("Unknown")).thenReturn(null);

        assertThat(new ProjectApiImpl(project).getReleaseByName("Unknown")).isNull();
    }
}
