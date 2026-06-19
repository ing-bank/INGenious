package com.ing.datalib.component.io;

import java.io.File;
import java.io.FilenameFilter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the on-disk {@link TestCaseFormat} for a given test case / test set
 * file and supplies matching {@link TestCaseStore} / {@link TestSetStore}
 * implementations.
 *
 * <p>Detection rule per folder (per migration plan §4.1):
 * <ol>
 *   <li>If {@code &lt;name&gt;.yaml} (or {@code .yml}) exists → YAML.</li>
 *   <li>Else if {@code &lt;name&gt;.csv} exists → CSV.</li>
 *   <li>Else fall back to the supplied project-default format.</li>
 * </ol>
 *
 * <p>This class is stateless and thread-safe; instances of the underlying
 * stores are reused via {@link #yamlTestCaseStore()} etc.
 */
public final class TestCaseStoreFactory {
    private static final TestCaseStore CSV_TC = new CsvTestCaseStore();
    private static final TestCaseStore YAML_TC = new YamlTestCaseStore();
    private static final TestSetStore CSV_TS = new CsvTestSetStore();
    private static final TestSetStore YAML_TS = new YamlTestSetStore();

    /** Recognised test-case / reusable filename extensions, lower-cased. */
    public static final FilenameFilter TEST_CASE_FILE_FILTER = (dir, name) -> {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".csv") || lower.endsWith(".yaml") || lower.endsWith(".yml");
    };

    /** Recognised YAML-only filter (used by callers that already enumerate CSV separately). */
    public static final FilenameFilter YAML_FILTER = (dir, name) -> {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".yaml") || lower.endsWith(".yml");
    };

    private TestCaseStoreFactory() {}

    public static TestCaseStore csvTestCaseStore() {
        return CSV_TC;
    }

    public static TestCaseStore yamlTestCaseStore() {
        return YAML_TC;
    }

    public static TestSetStore csvTestSetStore() {
        return CSV_TS;
    }

    public static TestSetStore yamlTestSetStore() {
        return YAML_TS;
    }

    public static TestCaseStore testCaseStore(TestCaseFormat format) {
        return format == TestCaseFormat.YAML ? YAML_TC : CSV_TC;
    }

    public static TestSetStore testSetStore(TestCaseFormat format) {
        return format == TestCaseFormat.YAML ? YAML_TS : CSV_TS;
    }

    /**
     * Resolves the format for a logical test case / test set file given its
     * containing directory and base name (without extension).
     *
     * @param directory   folder containing the file
     * @param baseName    file name without extension
     * @param projectDefault format to assume when no file exists yet
     */
    public static TestCaseFormat resolveFormat(
        File directory,
        String baseName,
        TestCaseFormat projectDefault
    ) {
        if (directory != null && baseName != null) {
            if (existing(directory, baseName, TestCaseFormat.YAML_EXTENSIONS) != null) {
                return TestCaseFormat.YAML;
            }
            if (new File(directory, baseName + TestCaseFormat.CSV.extension()).isFile()) {
                return TestCaseFormat.CSV;
            }
        }
        return projectDefault != null ? projectDefault : TestCaseFormat.CSV;
    }

    /**
     * Returns the resolved file for the given base name within {@code directory}.
     * Existing files take precedence over the project-default format.
     */
    public static File resolveFile(File directory, String baseName, TestCaseFormat projectDefault) {
        File existing = existing(directory, baseName, TestCaseFormat.YAML_EXTENSIONS);
        if (existing != null) {
            return existing;
        }
        File csv = directory == null
            ? null
            : new File(directory, baseName + TestCaseFormat.CSV.extension());
        if (csv != null && csv.isFile()) {
            return csv;
        }
        TestCaseFormat fmt = projectDefault != null ? projectDefault : TestCaseFormat.CSV;
        return new File(directory, baseName + fmt.extension());
    }

    /**
     * Lists logical test case files in {@code directory}, deduplicating by
     * base name and preferring YAML when both extensions are present for the
     * same name. Returns a map of base name → chosen {@link File}, in directory
     * listing order.
     */
    public static Map<String, File> listLogicalFiles(File directory) {
        Map<String, File> result = new LinkedHashMap<>();
        if (directory == null || !directory.isDirectory()) {
            return result;
        }
        String[] names = directory.list(TEST_CASE_FILE_FILTER);
        if (names == null) {
            return result;
        }
        // First pass: collect YAML wins.
        Set<String> yamlBaseNames = new LinkedHashSet<>();
        for (String name : names) {
            TestCaseFormat fmt = TestCaseFormat.fromFileName(name);
            if (fmt == TestCaseFormat.YAML) {
                String base = TestCaseFormat.stripExtension(name);
                yamlBaseNames.add(base);
                result.put(base, new File(directory, name));
            }
        }
        // Second pass: add CSV-only files.
        for (String name : names) {
            TestCaseFormat fmt = TestCaseFormat.fromFileName(name);
            if (fmt == TestCaseFormat.CSV) {
                String base = TestCaseFormat.stripExtension(name);
                if (!yamlBaseNames.contains(base)) {
                    result.put(base, new File(directory, name));
                }
            }
        }
        return result;
    }

    private static File existing(File directory, String baseName, String[] extensions) {
        if (directory == null || baseName == null) {
            return null;
        }
        for (String ext : extensions) {
            File f = new File(directory, baseName + ext);
            if (f.isFile()) {
                return f;
            }
        }
        return null;
    }
}
