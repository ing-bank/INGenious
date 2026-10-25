package com.ing.engine.constants;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for {@link ReportOutputPathsApiImpl} — verifies each method
 * delegates to the corresponding {@link AppResourcePath} static or constant.
 */
public class ReportOutputPathsApiImplTest {
    private final ReportOutputPathsApiImpl api = ReportOutputPathsApiImpl.INSTANCE;

    @BeforeClass
    public void initDateTime() {
        AppResourcePath.initDateTime();
    }

    @Test
    public void instanceShouldBeNonNullAndStable() {
        assertThat(ReportOutputPathsApiImpl.INSTANCE).isNotNull();
        assertThat(ReportOutputPathsApiImpl.INSTANCE).isSameAs(ReportOutputPathsApiImpl.INSTANCE);
    }

    @Test
    public void templateReportPathsShouldDelegateToAppResourcePath() {
        assertThat(api.getSummaryHTMLPath()).isEqualTo(AppResourcePath.getSummaryHTMLPath());
        assertThat(api.getDetailedHTMLPath()).isEqualTo(AppResourcePath.getDetailedHTMLPath());
        assertThat(api.getTCReportTemplate()).isEqualTo(AppResourcePath.getTCReportTemplate());
        assertThat(api.getReportHistoryHTMLPath())
            .isEqualTo(AppResourcePath.getReportHistoryHTMLPath());
        assertThat(api.getPerfReportHTMLPath()).isEqualTo(AppResourcePath.getPerfReportHTMLPath());
        assertThat(api.getVideoReportHTMLPath())
            .isEqualTo(AppResourcePath.getVideoReportHTMLPath());
    }

    @Test
    public void v2TemplateReportPathsShouldDelegateToAppResourcePath() {
        assertThat(api.getSummaryHTMLPathV2()).isEqualTo(AppResourcePath.getSummaryHTMLPathV2());
        assertThat(api.getDetailedHTMLPathV2()).isEqualTo(AppResourcePath.getDetailedHTMLPathV2());
        assertThat(api.getTCReportTemplateV2()).isEqualTo(AppResourcePath.getTCReportTemplateV2());
        assertThat(api.getPerfReportHTMLPathV2())
            .isEqualTo(AppResourcePath.getPerfReportHTMLPathV2());
        assertThat(api.getVideoReportHTMLPathV2())
            .isEqualTo(AppResourcePath.getVideoReportHTMLPathV2());
    }

    @Test
    public void summaryV1AndV2ShouldBeIdenticalAsDocumented() {
        // Documents the current AppResourcePath behavior: the Summary report
        // has fully migrated to V2, so both names return the same value.
        assertThat(api.getSummaryHTMLPath()).isEqualTo(api.getSummaryHTMLPathV2());
        assertThat(api.getCurrentSummaryHTMLPath()).isEqualTo(api.getCurrentSummaryHTMLPathV2());
    }

    @Test
    public void summaryHtmlFileNameShouldDelegateToTheConstant() {
        assertThat(api.getSummaryHtmlFileName()).isEqualTo(AppResourcePath.SUMMARY_HTML_V2);
    }

    @Test
    public void currentRunReportPathsShouldDelegateToAppResourcePath() {
        assertThat(api.getCurrentDetailedHTMLPath())
            .isEqualTo(AppResourcePath.getCurrentDetailedHTMLPath());
        assertThat(api.getCurrentExtentReportPath())
            .isEqualTo(AppResourcePath.getCurrentExtentReportPath());
        assertThat(api.getCurrentAzureReportPath())
            .isEqualTo(AppResourcePath.getCurrentAzureReportPath());
        assertThat(api.getCurrentExtentPDFReportPath())
            .isEqualTo(AppResourcePath.getCurrentExtentPDFReportPath());
        assertThat(api.getCurrentSummaryHTMLPath())
            .isEqualTo(AppResourcePath.getCurrentSummaryHTMLPath());
        assertThat(api.getCurrentSummaryHTMLPathRelative())
            .isEqualTo(AppResourcePath.getCurrentSummaryHTMLPathRelative());
        assertThat(api.getCurrentPerfReportHTMLPath())
            .isEqualTo(AppResourcePath.getCurrentPerfReportHTMLPath());
        assertThat(api.getCurrentVideoReportHTMLPath())
            .isEqualTo(AppResourcePath.getCurrentVideoReportHTMLPath());
    }

    @Test
    public void currentRunV2ReportPathsShouldDelegateToAppResourcePath() {
        assertThat(api.getCurrentSummaryHTMLPathV2())
            .isEqualTo(AppResourcePath.getCurrentSummaryHTMLPathV2());
        assertThat(api.getCurrentDetailedHTMLPathV2())
            .isEqualTo(AppResourcePath.getCurrentDetailedHTMLPathV2());
        assertThat(api.getCurrentPerfReportHTMLPathV2())
            .isEqualTo(AppResourcePath.getCurrentPerfReportHTMLPathV2());
        assertThat(api.getCurrentVideoReportHTMLPathV2())
            .isEqualTo(AppResourcePath.getCurrentVideoReportHTMLPathV2());
    }
}
