package com.ing.engine.constants;

import com.ing.ingenious.api.contract.paths.ReportOutputPathsApi;

/**
 * Delegates the {@link ReportOutputPathsApi} plugin contract to the existing
 * {@link AppResourcePath} static methods and constants.
 */
public final class ReportOutputPathsApiImpl implements ReportOutputPathsApi {
    public static final ReportOutputPathsApiImpl INSTANCE = new ReportOutputPathsApiImpl();

    private ReportOutputPathsApiImpl() {}

    @Override
    public String getSummaryHTMLPath() {
        return AppResourcePath.getSummaryHTMLPath();
    }

    @Override
    public String getDetailedHTMLPath() {
        return AppResourcePath.getDetailedHTMLPath();
    }

    @Override
    public String getTCReportTemplate() {
        return AppResourcePath.getTCReportTemplate();
    }

    @Override
    public String getReportHistoryHTMLPath() {
        return AppResourcePath.getReportHistoryHTMLPath();
    }

    @Override
    public String getPerfReportHTMLPath() {
        return AppResourcePath.getPerfReportHTMLPath();
    }

    @Override
    public String getVideoReportHTMLPath() {
        return AppResourcePath.getVideoReportHTMLPath();
    }

    @Override
    public String getSummaryHTMLPathV2() {
        return AppResourcePath.getSummaryHTMLPathV2();
    }

    @Override
    public String getDetailedHTMLPathV2() {
        return AppResourcePath.getDetailedHTMLPathV2();
    }

    @Override
    public String getTCReportTemplateV2() {
        return AppResourcePath.getTCReportTemplateV2();
    }

    @Override
    public String getPerfReportHTMLPathV2() {
        return AppResourcePath.getPerfReportHTMLPathV2();
    }

    @Override
    public String getVideoReportHTMLPathV2() {
        return AppResourcePath.getVideoReportHTMLPathV2();
    }

    @Override
    public String getSummaryHtmlFileName() {
        return AppResourcePath.SUMMARY_HTML_V2;
    }

    @Override
    public String getCurrentDetailedHTMLPath() {
        return AppResourcePath.getCurrentDetailedHTMLPath();
    }

    @Override
    public String getCurrentExtentReportPath() {
        return AppResourcePath.getCurrentExtentReportPath();
    }

    @Override
    public String getCurrentAzureReportPath() {
        return AppResourcePath.getCurrentAzureReportPath();
    }

    @Override
    public String getCurrentExtentPDFReportPath() {
        return AppResourcePath.getCurrentExtentPDFReportPath();
    }

    @Override
    public String getCurrentSummaryHTMLPath() {
        return AppResourcePath.getCurrentSummaryHTMLPath();
    }

    @Override
    public String getCurrentSummaryHTMLPathRelative() {
        return AppResourcePath.getCurrentSummaryHTMLPathRelative();
    }

    @Override
    public String getCurrentPerfReportHTMLPath() {
        return AppResourcePath.getCurrentPerfReportHTMLPath();
    }

    @Override
    public String getCurrentVideoReportHTMLPath() {
        return AppResourcePath.getCurrentVideoReportHTMLPath();
    }

    @Override
    public String getCurrentSummaryHTMLPathV2() {
        return AppResourcePath.getCurrentSummaryHTMLPathV2();
    }

    @Override
    public String getCurrentDetailedHTMLPathV2() {
        return AppResourcePath.getCurrentDetailedHTMLPathV2();
    }

    @Override
    public String getCurrentPerfReportHTMLPathV2() {
        return AppResourcePath.getCurrentPerfReportHTMLPathV2();
    }

    @Override
    public String getCurrentVideoReportHTMLPathV2() {
        return AppResourcePath.getCurrentVideoReportHTMLPathV2();
    }
}
