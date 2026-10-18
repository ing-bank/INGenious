package com.ing.ingenious.api.contract.paths;

/**
 * Contract for generated report output file paths: the static (non-current-run)
 * template-relative outputs and the current run's timestamped report files, in
 * both the legacy and "V2" (modern) report formats.
 * <p>
 * <b>Note:</b> as of the current implementation the Summary report has fully
 * migrated to the V2 format, so {@link #getSummaryHTMLPath()} and
 * {@link #getSummaryHTMLPathV2()} (and their {@code current*} counterparts)
 * return identical values. The other report types (Detailed, TestCase, Perf,
 * Video) still have distinct V1 and V2 outputs.
 * </p>
 */
public interface ReportOutputPathsApi {

    /** @return the path to the summary report HTML template (currently identical to the V2 path) */
    String getSummaryHTMLPath();

    /** @return the path to the detailed report HTML template */
    String getDetailedHTMLPath();

    /** @return the path to the test case report HTML template */
    String getTCReportTemplate();

    /** @return the path to the report history HTML template */
    String getReportHistoryHTMLPath();

    /** @return the path to the performance report HTML template */
    String getPerfReportHTMLPath();

    /** @return the path to the video report HTML template */
    String getVideoReportHTMLPath();

    /** @return the path to the modern (V2) summary report HTML template */
    String getSummaryHTMLPathV2();

    /** @return the path to the modern (V2) detailed report HTML template */
    String getDetailedHTMLPathV2();

    /** @return the path to the modern (V2) test case report HTML template */
    String getTCReportTemplateV2();

    /** @return the path to the modern (V2) performance report HTML template */
    String getPerfReportHTMLPathV2();

    /** @return the path to the modern (V2) video report HTML template */
    String getVideoReportHTMLPathV2();

    /** @return the file name used for the modern (V2) summary report ({@code "summary-v2.html"}) */
    String getSummaryHtmlFileName();

    /** @return the path to the current run's detailed report HTML file */
    String getCurrentDetailedHTMLPath();

    /** @return the path to the current run's Extent HTML report file */
    String getCurrentExtentReportPath();

    /** @return the path to the current run's Azure XML report file */
    String getCurrentAzureReportPath();

    /** @return the path to the current run's Extent PDF report file */
    String getCurrentExtentPDFReportPath();

    /** @return the path to the current run's summary report HTML file (currently identical to the V2 path) */
    String getCurrentSummaryHTMLPath();

    /** @return the current run's summary report HTML path, relative to the results root */
    String getCurrentSummaryHTMLPathRelative();

    /** @return the path to the current run's performance report HTML file */
    String getCurrentPerfReportHTMLPath();

    /** @return the path to the current run's video report HTML file */
    String getCurrentVideoReportHTMLPath();

    /** @return the path to the current run's modern (V2) summary report HTML file */
    String getCurrentSummaryHTMLPathV2();

    /** @return the path to the current run's modern (V2) detailed report HTML file */
    String getCurrentDetailedHTMLPathV2();

    /** @return the path to the current run's modern (V2) performance report HTML file */
    String getCurrentPerfReportHTMLPathV2();

    /** @return the path to the current run's modern (V2) video report HTML file */
    String getCurrentVideoReportHTMLPathV2();
}
