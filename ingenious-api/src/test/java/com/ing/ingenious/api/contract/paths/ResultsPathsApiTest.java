package com.ing.ingenious.api.contract.paths;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contract test for {@link ResultsPathsApi}. Uses a minimal in-test
 * implementation to verify the interface can be implemented and dispatched
 * without pulling in any Engine dependency.
 */
class ResultsPathsApiTest {

    private final ResultsPathsApi api = new ResultsPathsApi() {
        @Override
        public String getResultsPath() {
            return "/project/Results";
        }

        @Override
        public String getCurrentResultsPath() {
            return "/project/Results/TestExecution/Release1/TS1/15-Oct-2026 05-33-41";
        }

        @Override
        public String getCurrentResultsLocation() {
            return "/project/Results/TestExecution/Release1/TS1";
        }

        @Override
        public String getCurrentTestCaseLogsLocation() {
            return getCurrentResultsLocation() + "/15-Oct-2026 05-33-41/logs";
        }

        @Override
        public String getCurrentTestCaseAccessibilityLocation() {
            return getCurrentResultsLocation() + "/15-Oct-2026 05-33-41/aXe";
        }

        @Override
        public String getCurrentTestCaseVideosLocation() {
            return getCurrentResultsLocation() + "/15-Oct-2026 05-33-41/videos";
        }

        @Override
        public String getLatestResultsLocation() {
            return getCurrentResultsLocation() + "/Latest";
        }

        @Override
        public String getCurrentReportFolderName() {
            return "15-Oct-2026 05-33-41";
        }

        @Override
        public String getCurrentPerfReportHarPath() {
            return getResultsPath() + "/perf/har";
        }

        @Override
        public String getCurrentReportDataPath() {
            return getCurrentResultsPath() + "/data.js";
        }

        @Override
        public String getCurrentReportHistoryDataPath() {
            return getCurrentResultsLocation() + "/reportHistory.js";
        }

        @Override
        public String getDate() {
            return "15-Oct-2026";
        }

        @Override
        public String getTime() {
            return "05-33-41";
        }
    };

    @Test
    void resultsPathsShouldBeDispatched() {
        assertEquals("/project/Results", api.getResultsPath());
        assertEquals("15-Oct-2026", api.getDate());
        assertEquals("05-33-41", api.getTime());
    }

    @Test
    void currentRunSubFoldersShouldBeDispatched() {
        assertEquals(
            api.getCurrentResultsLocation() + "/15-Oct-2026 05-33-41/logs",
            api.getCurrentTestCaseLogsLocation()
        );
        assertEquals(
            api.getCurrentResultsLocation() + "/15-Oct-2026 05-33-41/videos",
            api.getCurrentTestCaseVideosLocation()
        );
    }
}
