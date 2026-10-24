package com.ing.engine.constants;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.Test;

/**
 * Tests for {@link ReportTemplatePathsApiImpl} — verifies each method
 * delegates to the corresponding {@link AppResourcePath} static.
 */
public class ReportTemplatePathsApiImplTest {
    private final ReportTemplatePathsApiImpl api = ReportTemplatePathsApiImpl.INSTANCE;

    @Test
    public void instanceShouldBeNonNullAndStable() {
        assertThat(ReportTemplatePathsApiImpl.INSTANCE).isNotNull();
        assertThat(ReportTemplatePathsApiImpl.INSTANCE)
            .isSameAs(ReportTemplatePathsApiImpl.INSTANCE);
    }

    @Test
    public void templatePathsShouldDelegateToAppResourcePath() {
        assertThat(api.getReportThemePreviewPath())
            .isEqualTo(AppResourcePath.getReportThemePreviewPath());
        assertThat(api.getReportThemePath()).isEqualTo(AppResourcePath.getReportThemePath());
        assertThat(api.getReportResourcePath()).isEqualTo(AppResourcePath.getReportResourcePath());
        assertThat(api.getMailReportTemplatePath())
            .isEqualTo(AppResourcePath.getMailReportTemplatePath());
        assertThat(api.getaXeReportTemplatePath())
            .isEqualTo(AppResourcePath.getaXeReportTemplatePath());
        assertThat(api.getReportTemplatePath()).isEqualTo(AppResourcePath.getReportTemplatePath());
        assertThat(api.getReportMediaPath()).isEqualTo(AppResourcePath.getReportMediaPath());
    }
}
