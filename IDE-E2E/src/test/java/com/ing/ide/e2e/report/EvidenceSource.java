package com.ing.ide.e2e.report;

import java.nio.file.Path;
import java.util.List;

/**
 * Implemented by test classes so the report listener can collect their evidence.
 */
public interface EvidenceSource {
    /**
     * Ends evidence collection for the current test case (e.g. finalises its recording).
     *
     * @return files captured for the current test case, in capture order
     */
    List<Path> completeEvidence();

    /**
     * Captures a screenshot of the failed state, while the IDE is still open.
     *
     * @param step short description used in the file name
     */
    void captureFailure(String step);
}
