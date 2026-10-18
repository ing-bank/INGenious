package com.ing.engine.constants;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Tests for {@link PathsApiImpl} — verifies the root delegate returns the
 * expected per-concern singleton instances.
 */
public class PathsApiImplTest {
    private final PathsApiImpl api = PathsApiImpl.INSTANCE;

    @Test
    public void instanceShouldBeNonNullAndStable() {
        assertThat(PathsApiImpl.INSTANCE).isNotNull();
        assertThat(PathsApiImpl.INSTANCE).isSameAs(PathsApiImpl.INSTANCE);
    }

    @Test
    public void subAccessorsShouldReturnTheExpectedSingletons() {
        assertThat(api.project()).isSameAs(ProjectPathsApiImpl.INSTANCE);
        assertThat(api.app()).isSameAs(AppPathsApiImpl.INSTANCE);
        assertThat(api.results()).isSameAs(ResultsPathsApiImpl.INSTANCE);
        assertThat(api.reportTemplates()).isSameAs(ReportTemplatePathsApiImpl.INSTANCE);
        assertThat(api.reportOutputs()).isSameAs(ReportOutputPathsApiImpl.INSTANCE);
    }
}
