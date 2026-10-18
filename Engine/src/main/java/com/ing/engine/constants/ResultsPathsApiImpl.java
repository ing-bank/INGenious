package com.ing.engine.constants;

import com.ing.ingenious.api.contract.paths.ResultsPathsApi;

/**
 * Delegates the {@link ResultsPathsApi} plugin contract to the existing
 * {@link AppResourcePath} static methods.
 */
public final class ResultsPathsApiImpl implements ResultsPathsApi {
    public static final ResultsPathsApiImpl INSTANCE = new ResultsPathsApiImpl();

    private ResultsPathsApiImpl() {}

    @Override
    public String getResultsPath() {
        return AppResourcePath.getResultsPath();
    }

    @Override
    public String getCurrentResultsPath() {
        return AppResourcePath.getCurrentResultsPath();
    }

    @Override
    public String getCurrentResultsLocation() {
        return AppResourcePath.getCurrentResultsLocation();
    }

    @Override
    public String getCurrentTestCaseLogsLocation() {
        return AppResourcePath.getCurrentTestCaseLogsLocation();
    }

    @Override
    public String getCurrentTestCaseAccessibilityLocation() {
        return AppResourcePath.getCurrentTestCaseAccessibilityLocation();
    }

    @Override
    public String getCurrentTestCaseVideosLocation() {
        return AppResourcePath.getCurrentTestCaseVideosLocation();
    }

    @Override
    public String getLatestResultsLocation() {
        return AppResourcePath.getLatestResultsLocation();
    }

    @Override
    public String getCurrentReportFolderName() {
        return AppResourcePath.getCurrentReportFolderName();
    }

    @Override
    public String getCurrentPerfReportHarPath() {
        return AppResourcePath.getCurrentPerfReportHarPath();
    }

    @Override
    public String getCurrentReportDataPath() {
        return AppResourcePath.getCurrentReportDataPath();
    }

    @Override
    public String getCurrentReportHistoryDataPath() {
        return AppResourcePath.getCurrentReportHistoryDataPath();
    }

    @Override
    public String getDate() {
        return AppResourcePath.getDate();
    }

    @Override
    public String getTime() {
        return AppResourcePath.getTime();
    }
}
