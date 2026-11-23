package com.ing.ide.uitest.report;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Writes one report format for a test run. Register new formats in {@link ReportFormats}.
 */
public interface ReportWriter {
    /**
     * @return the format name used in {@code -Duitest.report}, e.g. {@code html}
     */
    String format();

    /**
     * Writes (or rewrites) the report from all results recorded so far.
     *
     * @param runDir the run folder; evidence paths are relative to it
     * @param run facts about the run
     * @param results all test case results, sorted by scenario and test case code
     * @throws IOException if the report cannot be written
     */
    void write(Path runDir, RunInfo run, List<TestCaseResult> results) throws IOException;
}
