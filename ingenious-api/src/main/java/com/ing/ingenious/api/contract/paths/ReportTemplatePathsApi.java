package com.ing.ingenious.api.contract.paths;

/**
 * Contract for static report template and resource locations: the HTML/theme
 * templates and media used to render summary, detailed, and mail reports.
 * These paths are independent of any loaded project or run.
 */
public interface ReportTemplatePathsApi {

    /** @return the path to the report theme preview folder */
    String getReportThemePreviewPath();

    /** @return the path to the report theme folder */
    String getReportThemePath();

    /** @return the path to the report's media/resource folder */
    String getReportResourcePath();

    /** @return the path to the mail report template folder */
    String getMailReportTemplatePath();

    /** @return the path to the aXe accessibility report template folder */
    String getaXeReportTemplatePath();

    /** @return the path to the HTML report template folder */
    String getReportTemplatePath();

    /** @return the path to the report media folder */
    String getReportMediaPath();
}
