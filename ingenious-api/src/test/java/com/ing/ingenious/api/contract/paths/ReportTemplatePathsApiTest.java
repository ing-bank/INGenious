package com.ing.ingenious.api.contract.paths;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contract test for {@link ReportTemplatePathsApi}. Uses a minimal in-test
 * implementation to verify the interface can be implemented and dispatched
 * without pulling in any Engine dependency.
 */
class ReportTemplatePathsApiTest {

    private final ReportTemplatePathsApi api = new ReportTemplatePathsApi() {
        @Override
        public String getReportThemePreviewPath() {
            return "/app/Configuration/ReportTemplate/preview";
        }

        @Override
        public String getReportThemePath() {
            return "/app/Configuration/ReportTemplate/media/theme";
        }

        @Override
        public String getReportResourcePath() {
            return "/app/Configuration/ReportTemplate/media";
        }

        @Override
        public String getMailReportTemplatePath() {
            return "/app/Configuration/ReportTemplate/mailReport";
        }

        @Override
        public String getaXeReportTemplatePath() {
            return "/app/Configuration/ReportTemplate/aXe";
        }

        @Override
        public String getReportTemplatePath() {
            return "/app/Configuration/ReportTemplate/html";
        }

        @Override
        public String getReportMediaPath() {
            return "/app/Configuration/ReportTemplate/media";
        }
    };

    @Test
    void templatePathsShouldBeDispatched() {
        assertEquals("/app/Configuration/ReportTemplate/html", api.getReportTemplatePath());
        assertEquals("/app/Configuration/ReportTemplate/aXe", api.getaXeReportTemplatePath());
        assertEquals("/app/Configuration/ReportTemplate/mailReport", api.getMailReportTemplatePath());
    }
}
