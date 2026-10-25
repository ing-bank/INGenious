package com.ing.engine.constants;

import com.ing.ingenious.api.contract.paths.ReportTemplatePathsApi;

/**
 * Delegates the {@link ReportTemplatePathsApi} plugin contract to the existing
 * {@link AppResourcePath} static methods.
 */
public final class ReportTemplatePathsApiImpl implements ReportTemplatePathsApi {
    public static final ReportTemplatePathsApiImpl INSTANCE = new ReportTemplatePathsApiImpl();

    private ReportTemplatePathsApiImpl() {}

    @Override
    public String getReportThemePreviewPath() {
        return AppResourcePath.getReportThemePreviewPath();
    }

    @Override
    public String getReportThemePath() {
        return AppResourcePath.getReportThemePath();
    }

    @Override
    public String getReportResourcePath() {
        return AppResourcePath.getReportResourcePath();
    }

    @Override
    public String getMailReportTemplatePath() {
        return AppResourcePath.getMailReportTemplatePath();
    }

    @Override
    public String getaXeReportTemplatePath() {
        return AppResourcePath.getaXeReportTemplatePath();
    }

    @Override
    public String getReportTemplatePath() {
        return AppResourcePath.getReportTemplatePath();
    }

    @Override
    public String getReportMediaPath() {
        return AppResourcePath.getReportMediaPath();
    }
}
