package com.ing.engine.constants;

import static org.assertj.core.api.Assertions.assertThat;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for {@link ResultsPathsApiImpl} — verifies each method delegates to
 * the corresponding {@link AppResourcePath} static.
 */
public class ResultsPathsApiImplTest {
    private final ResultsPathsApiImpl api = ResultsPathsApiImpl.INSTANCE;

    @BeforeClass
    public void initDateTime() {
        // AppResourcePath.getCurrent* paths read the module-private date/time
        // fields populated by initDateTime(); without this, both the static
        // and the delegate would still agree (both read "null"), but calling
        // it mirrors the real run lifecycle where Control.initRun() sets it.
        AppResourcePath.initDateTime();
    }

    @Test
    public void instanceShouldBeNonNullAndStable() {
        assertThat(ResultsPathsApiImpl.INSTANCE).isNotNull();
        assertThat(ResultsPathsApiImpl.INSTANCE).isSameAs(ResultsPathsApiImpl.INSTANCE);
    }

    @Test
    public void resultsPathsShouldDelegateToAppResourcePath() {
        assertThat(api.getResultsPath()).isEqualTo(AppResourcePath.getResultsPath());
        assertThat(api.getCurrentResultsPath()).isEqualTo(AppResourcePath.getCurrentResultsPath());
        assertThat(api.getCurrentResultsLocation())
            .isEqualTo(AppResourcePath.getCurrentResultsLocation());
        assertThat(api.getLatestResultsLocation())
            .isEqualTo(AppResourcePath.getLatestResultsLocation());
    }

    @Test
    public void currentTestCaseSubFoldersShouldDelegateToAppResourcePath() {
        assertThat(api.getCurrentTestCaseLogsLocation())
            .isEqualTo(AppResourcePath.getCurrentTestCaseLogsLocation());
        assertThat(api.getCurrentTestCaseAccessibilityLocation())
            .isEqualTo(AppResourcePath.getCurrentTestCaseAccessibilityLocation());
        assertThat(api.getCurrentTestCaseVideosLocation())
            .isEqualTo(AppResourcePath.getCurrentTestCaseVideosLocation());
    }

    @Test
    public void reportDataPathsShouldDelegateToAppResourcePath() {
        assertThat(api.getCurrentReportFolderName())
            .isEqualTo(AppResourcePath.getCurrentReportFolderName());
        assertThat(api.getCurrentPerfReportHarPath())
            .isEqualTo(AppResourcePath.getCurrentPerfReportHarPath());
        assertThat(api.getCurrentReportDataPath())
            .isEqualTo(AppResourcePath.getCurrentReportDataPath());
        assertThat(api.getCurrentReportHistoryDataPath())
            .isEqualTo(AppResourcePath.getCurrentReportHistoryDataPath());
    }

    @Test
    public void dateAndTimeShouldDelegateToAppResourcePath() {
        assertThat(api.getDate()).isEqualTo(AppResourcePath.getDate());
        assertThat(api.getTime()).isEqualTo(AppResourcePath.getTime());
    }
}
