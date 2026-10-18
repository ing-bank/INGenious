package com.ing.ingenious.api.contract.paths;

/**
 * Contract for the current run's results paths: the Results folder, the
 * timestamped folder for the current run, and the per-test-case logs/videos/
 * accessibility sub-folders underneath it.
 * <p>
 * The timestamp used by the {@code getCurrent*} methods is captured once per
 * run before any command executes, so these paths are stable for the
 * lifetime of a run.
 * </p>
 */
public interface ResultsPathsApi {

    /** @return the path to the project's Results folder */
    String getResultsPath();

    /** @return the path to the current run's timestamped results folder */
    String getCurrentResultsPath();

    /** @return the path to the current run's results location, excluding the timestamp */
    String getCurrentResultsLocation();

    /** @return the path to the current test case's logs folder */
    String getCurrentTestCaseLogsLocation();

    /** @return the path to the current test case's accessibility (aXe) folder */
    String getCurrentTestCaseAccessibilityLocation();

    /** @return the path to the current test case's videos folder */
    String getCurrentTestCaseVideosLocation();

    /** @return the path to the "Latest" results folder */
    String getLatestResultsLocation();

    /** @return the current run's report folder name, formatted as {@code date + " " + time} */
    String getCurrentReportFolderName();

    /** @return the path to the current run's performance HAR folder */
    String getCurrentPerfReportHarPath();

    /** @return the path to the current run's report data.js file */
    String getCurrentReportDataPath();

    /** @return the path to the current run's reportHistory.js file */
    String getCurrentReportHistoryDataPath();

    /** @return the date captured for the current run, formatted as {@code dd-MMM-yyyy} */
    String getDate();

    /** @return the time captured for the current run, formatted as {@code HH-mm-ss} */
    String getTime();
}
