package com.ing.datalib.component.io;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Format-agnostic persistence for a single test set.
 *
 * <p>Rows are returned in {@link com.ing.datalib.component.ExecutionStep.HEADERS}
 * order so the in-memory model is independent of disk format.
 */
public interface TestSetStore {
    TestCaseFormat format();

    List<List<String>> load(File file) throws IOException;

    void save(File file, String testSetName, String releaseName, List<List<String>> rows)
        throws IOException;
}
