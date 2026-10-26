package com.ing.ingenious.api.contract.paths;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contract test for {@link ReportOutputPathsApi}. Uses a minimal in-test
 * implementation to verify the interface can be implemented and dispatched
 * without pulling in any Engine dependency.
 */
class ReportOutputPathsApiTest {

    private static final String SUMMARY_V2_FILE = "summary-v2.html";

    private final ReportOutputPathsApi api = new ReportOutputPathsApi() {
        @Override
        public String getSummaryHTMLPath() {
            return "/app/Configuration/ReportTemplate/html/" + SUMMARY_V2_FILE;
        }

        @Override
        public String getDetailedHTMLPath() {
            return "/app/Configuration/ReportTemplate/html/detailed.html";
        }

        @Override
        public String getTCReportTemplate() {
            return "/app/Configuration/ReportTemplate/html/testCase.html";
        }

        @Override
        public String getReportHistoryHTMLPath() {
            return "/app/Configuration/ReportTemplate/html/ReportHistory.html";
        }

        @Override
        public String getPerfReportHTMLPath() {
            return "/app/Configuration/ReportTemplate/html/perfReport.html";
        }

        @Override
        public String getVideoReportHTMLPath() {
            return "/app/Configuration/ReportTemplate/html/videoReport.html";
        }

        @Override
        public String getSummaryHTMLPathV2() {
            return "/app/Configuration/ReportTemplate/html/" + SUMMARY_V2_FILE;
        }

        @Override
        public String getDetailedHTMLPathV2() {
            return "/app/Configuration/ReportTemplate/html/detailed-v2.html";
        }

        @Override
        public String getTCReportTemplateV2() {
            return "/app/Configuration/ReportTemplate/html/testCase-v2.html";
        }

        @Override
        public String getPerfReportHTMLPathV2() {
            return "/app/Configuration/ReportTemplate/html/perfReport-v2.html";
        }

        @Override
        public String getVideoReportHTMLPathV2() {
            return "/app/Configuration/ReportTemplate/html/videoReport-v2.html";
        }

        @Override
        public String getSummaryHtmlFileName() {
            return SUMMARY_V2_FILE;
        }

        @Override
        public String getCurrentDetailedHTMLPath() {
            return "/project/Results/.../15-Oct-2026 05-33-41/detailed.html";
        }

        @Override
        public String getCurrentExtentReportPath() {
            return "/project/Results/.../15-Oct-2026 05-33-41/extent.html";
        }

        @Override
        public String getCurrentAzureReportPath() {
            return "/project/Results/.../15-Oct-2026 05-33-41/azure.xml";
        }

        @Override
        public String getCurrentExtentPDFReportPath() {
            return "/project/Results/.../15-Oct-2026 05-33-41/extent.pdf";
        }

        @Override
        public String getCurrentSummaryHTMLPath() {
            return "/project/Results/.../15-Oct-2026 05-33-41/" + SUMMARY_V2_FILE;
        }

        @Override
        public String getCurrentSummaryHTMLPathRelative() {
            return "15-Oct-2026 05-33-41/" + SUMMARY_V2_FILE;
        }

        @Override
        public String getCurrentPerfReportHTMLPath() {
            return "/project/Results/.../15-Oct-2026 05-33-41/perfReport.html";
        }

        @Override
        public String getCurrentVideoReportHTMLPath() {
            return "/project/Results/.../15-Oct-2026 05-33-41/videoReport.html";
        }

        @Override
        public String getCurrentSummaryHTMLPathV2() {
            return "/project/Results/.../15-Oct-2026 05-33-41/" + SUMMARY_V2_FILE;
        }

        @Override
        public String getCurrentDetailedHTMLPathV2() {
            return "/project/Results/.../15-Oct-2026 05-33-41/detailed-v2.html";
        }

        @Override
        public String getCurrentPerfReportHTMLPathV2() {
            return "/project/Results/.../15-Oct-2026 05-33-41/perfReport-v2.html";
        }

        @Override
        public String getCurrentVideoReportHTMLPathV2() {
            return "/project/Results/.../15-Oct-2026 05-33-41/videoReport-v2.html";
        }
    };

    @Test
    void summaryV1AndV2ShouldBeIdenticalAsDocumented() {
        assertEquals(api.getSummaryHTMLPath(), api.getSummaryHTMLPathV2());
        assertEquals(api.getCurrentSummaryHTMLPath(), api.getCurrentSummaryHTMLPathV2());
    }

    @Test
    void otherReportTypesShouldHaveDistinctV1AndV2Paths() {
        assertEquals("/app/Configuration/ReportTemplate/html/detailed.html", api.getDetailedHTMLPath());
        assertEquals("/app/Configuration/ReportTemplate/html/detailed-v2.html", api.getDetailedHTMLPathV2());
    }

    @Test
    void summaryHtmlFileNameShouldBeDispatched() {
        assertEquals(SUMMARY_V2_FILE, api.getSummaryHtmlFileName());
    }
}
