package com.ing.datalib.component.io;

import com.ing.datalib.component.ExecutionStep;
import java.io.File;
import java.io.IOException;
import java.util.List;

/** CSV implementation of {@link TestSetStore}. */
public class CsvTestSetStore implements TestSetStore {

    @Override
    public TestCaseFormat format() {
        return TestCaseFormat.CSV;
    }

    @Override
    public List<List<String>> load(File file) {
        return CsvTestCaseStore.readRows(file, ExecutionStep.HEADERS.size());
    }

    @Override
    public void save(File file,
                     String testSetName,
                     String releaseName,
                     List<List<String>> rows) throws IOException {
        CsvTestCaseStore.writeRows(file, ExecutionStep.HEADERS.getValues(), rows);
    }
}
