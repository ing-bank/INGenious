package com.ing.datalib.component.io;

import com.ing.datalib.component.ExecutionStep;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.component.utils.FileUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

/**
 * CSV implementation of {@link TestCaseStore}. Preserves the legacy
 * {@code CSVFormat.EXCEL} behaviour previously embedded inside
 * {@link com.ing.datalib.component.TestCase}.
 */
public class CsvTestCaseStore implements TestCaseStore {

    @Override
    public TestCaseFormat format() {
        return TestCaseFormat.CSV;
    }

    @Override
    public List<List<String>> load(File file) {
        List<CSVRecord> records = FileUtils.getRecords(file);
        List<List<String>> rows = new ArrayList<>(records.size());
        int headerCount = TestStep.HEADERS.size();
        for (CSVRecord record : records) {
            List<String> row = new ArrayList<>(headerCount);
            for (int i = 0; i < record.size(); i++) {
                row.add(record.get(i));
            }
            while (row.size() < headerCount) {
                row.add("");
            }
            rows.add(row);
        }
        return rows;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void save(
        File file,
        String testCaseName,
        String scenarioName,
        boolean reusable,
        List<String> tags,
        List<List<String>> rows
    )
        throws IOException {
        ensureParent(file);
        try (
            FileWriter out = new FileWriter(file);
            CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withIgnoreEmptyLines())
        ) {
            printer.printRecord(TestStep.HEADERS.getValues());
            for (List<String> row : rows) {
                printer.printRecord(row);
            }
        }
    }

    static void ensureParent(File file) {
        File parent = file.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
    }

    /** Shared helper for {@link CsvTestSetStore} to avoid duplicating CSV plumbing. */
    static List<List<String>> readRows(File file, int minColumns) {
        List<CSVRecord> records = FileUtils.getRecords(file);
        List<List<String>> rows = new ArrayList<>(records.size());
        for (CSVRecord record : records) {
            List<String> row = new ArrayList<>(Math.max(minColumns, record.size()));
            for (int i = 0; i < record.size(); i++) {
                row.add(record.get(i));
            }
            while (row.size() < minColumns) {
                row.add("");
            }
            rows.add(row);
        }
        return rows;
    }

    @SuppressWarnings("deprecation")
    static void writeRows(File file, List<String> headers, List<List<String>> rows)
        throws IOException {
        ensureParent(file);
        try (
            FileWriter out = new FileWriter(file);
            CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withIgnoreEmptyLines())
        ) {
            printer.printRecord(headers);
            for (List<String> row : rows) {
                printer.printRecord(row);
            }
        }
    }

    // Reference to ExecutionStep just to ensure class is loaded; harmless.
    @SuppressWarnings("unused")
    private static final Class<?> KEEP = ExecutionStep.class;
}
