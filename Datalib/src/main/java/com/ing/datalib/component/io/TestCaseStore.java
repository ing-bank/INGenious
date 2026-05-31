package com.ing.datalib.component.io;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Format-agnostic persistence for a single test case (or reusable component).
 *
 * <p>Implementations read/write step rows in the order defined by
 * {@link com.ing.datalib.component.TestStep.HEADERS}, returning each row as a
 * {@code List<String>} whose length equals the header count. This keeps the
 * in-memory {@link com.ing.datalib.component.TestStep} model unchanged
 * regardless of on-disk format.
 */
public interface TestCaseStore {

    /** Format handled by this store. */
    TestCaseFormat format();

    /**
     * Reads all step rows from {@code file}. Returns an empty list when the
     * file does not exist or contains no records.
     */
    List<List<String>> load(File file) throws IOException;

    /**
     * Writes the given step rows to {@code file}, creating parent
     * directories if necessary.
     *
     * @param file destination file
     * @param testCaseName logical name of the test case (used for YAML metadata)
     * @param scenarioName scenario / folder name (used for YAML metadata; may be null)
     * @param reusable     whether this test case is a reusable component
     * @param tags         optional metadata tags (may be null/empty)
     * @param rows         step rows in {@link com.ing.datalib.component.TestStep.HEADERS} order
     */
    void save(File file,
              String testCaseName,
              String scenarioName,
              boolean reusable,
              List<String> tags,
              List<List<String>> rows) throws IOException;
}
