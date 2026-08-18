package com.ing.testdata.csv;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestData;
import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.testdata.DataProvider;
import com.ing.datalib.testdata.model.Record;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

@DataProvider(type = "csv")
public class CsvDataProvider extends TestData {
    private static final Logger LOGGER = Logger.getLogger(CsvDataProvider.class.getName());

    public CsvDataProvider(Project sProject, String enviroment) {
        super(sProject, enviroment);
    }

    @Override
    public void load() {
        // Skip all migrations and saves if in read-only mode (e.g., during validation)
        if (isReadOnlyMode()) {
            loadGlobalData();
            return;
        }

        File file = new File(getLocation());
        if (file.exists()) {
            for (File tData : file.listFiles(FileUtils.CSV_FILTER)) {
                if (!tData.getName().equals("GlobalData.csv")) {
                    CsvTestData csvData = new CsvTestData(tData.getAbsolutePath());
                    // ensure model is loaded so we can migrate records if the file lacks the Scope column
                    csvData.loadTableModel();
                    // A resolved (non-blank) Scope is persisted, static data - never recomputed. Only a
                    // file whose Scope column was just spliced in this load (legacy pre-Scope CSV) gets
                    // the full one-time population; a file that already had the column still gets a
                    // pass to retry any row left blank by a prior ambiguous resolution (or by a file
                    // touched before this retry existed), without touching rows that already resolved.
                    if (csvData.isScopeColumnMigrated()) {
                        migrateLegacyRecords(csvData);
                    } else {
                        retryUnresolvedScopes(csvData);
                    }
                    addTestData(csvData);
                }
            }
        }
        loadGlobalData();
    }

    /**
     * One-time, best-effort population of the Scope column for a file that predates it.
     * Only runs for files where {@link CsvTestData#isScopeColumnMigrated()} is true this load.
     */
    private void migrateLegacyRecords(CsvTestData csvData) {
        for (Record record : csvData.getRecords()) {
            realignOldFourColumnRow(record);

            String scenario = java.util.Objects.toString(record.get(0), "").trim();
            String testcase = java.util.Objects.toString(record.get(1), "").trim();
            String scope;
            String normalized = scenario;

            // First, check scenario for explicit scope markers carried over from legacy naming
            if (normalized.startsWith("[Project] ")) {
                scope = "[Project]";
                normalized = normalized.substring("[Project] ".length());
            } else if (normalized.startsWith("[Shared] ")) {
                scope = "[Shared]";
                normalized = normalized.substring("[Shared] ".length());
            } else if (normalized.startsWith("[TestPlan] ")) {
                scope = "";
                normalized = normalized.substring("[TestPlan] ".length());
            } else {
                scope = resolveScopeByUniqueMatch(normalized, testcase);
            }

            if (scope == null) {
                // Ambiguous: the same scenario+testcase combination exists in more than one scope
                // (test plan / project reusable / shared reusable). Surface it instead of guessing;
                // the structural column migration below is still persisted so this file isn't
                // reprocessed as "legacy" forever - the user can set the correct scope explicitly
                // by re-picking the scenario for this row.
                LOGGER.log(
                    Level.WARNING,
                    "Cannot uniquely resolve Scope for Scenario=[{0}] TestCase=[{1}] in {2} - " +
                    "found in multiple scopes. Leaving Scope unset; please set it explicitly.",
                    new Object[] { normalized, testcase, csvData.getName() }
                );
                record.set(0, normalized);
                continue;
            }

            record.set(2, scope);
            record.set(0, normalized);
        }
        // The Scope column itself was just spliced into this file's structure (isScopeColumnMigrated()),
        // so persist that structural change regardless of whether every row's value could be resolved.
        csvData.saveChanges();
    }

    /**
     * Retries Scope resolution for rows still blank - left that way by a prior ambiguous
     * migration (same scenario+testcase name found in more than one scope), or carried over from
     * a file touched before this retry pass existed. Rows that already carry a Scope value are
     * never touched here - once resolved, a Scope is trusted permanently. A blank row that still
     * doesn't uniquely resolve (or that genuinely belongs to the Test Plan, whose Scope is blank
     * by convention) is a no-op, so this is safe to run on every load.
     */
    private void retryUnresolvedScopes(CsvTestData csvData) {
        boolean changed = false;
        for (Record record : csvData.getRecords()) {
            String currentScope = java.util.Objects.toString(record.get(2), "").trim();
            if (!currentScope.isEmpty()) {
                continue;
            }
            String scenario = java.util.Objects.toString(record.get(0), "").trim();
            String testcase = java.util.Objects.toString(record.get(1), "").trim();
            String resolved = resolveScopeByUniqueMatch(scenario, testcase);
            if (resolved != null && !resolved.isEmpty()) {
                record.set(2, resolved);
                changed = true;
            }
        }
        if (changed) {
            csvData.saveChanges();
        }
    }

    /**
     * Detects and repairs the older 4-column layout (Scenario, Flow, Iteration, SubIteration) where the
     * Scope column didn't exist, so Iteration/SubIteration/Data values are still at the pre-migration
     * positions. Normally a no-op: {@code CSVUtils.load} already splices the Scope slot in for any
     * file where {@code isScopeColumnMigrated()} is true, so this only matters as a defensive fallback.
     */
    private void realignOldFourColumnRow(Record record) {
        try {
            String idx2 = java.util.Objects.toString(record.get(2), "").trim();
            String idx3 = java.util.Objects.toString(record.get(3), "").trim();

            boolean idx2IsNumeric = idx2.matches("\\d+");
            boolean idx3IsNumeric = idx3.matches("\\d+");
            boolean idx2IsScopeMarker = idx2.startsWith("[") && idx2.endsWith("]");

            if (idx2IsNumeric && idx3IsNumeric && !idx2IsScopeMarker) {
                try {
                    record.add(2, "");
                    return;
                } catch (Exception e) {
                    try {
                        record.set(4, idx3);
                        record.set(3, idx2);
                        record.set(2, "");
                        return;
                    } catch (Exception ex) {
                        // ignore - will be handled later
                    }
                }
            }
        } catch (Exception ex) {
            // ignore index errors - record may not have enough elements
        }
    }

    /**
     * Resolves scope by requiring an exact (scenario name + test case name) match in exactly one of
     * Test Plan / Project Reusables / Shared Reusables. Returns "" for a unique Test Plan match,
     * "[Project]"/"[Shared]" for a unique reusable match, or null if the match is ambiguous or absent.
     */
    private String resolveScopeByUniqueMatch(String scenarioName, String testCaseName) {
        Project proj = getsProject();
        if (proj == null || testCaseName.isEmpty()) {
            return "";
        }

        boolean inTestPlan = hasTestCase(proj.getScenarioByName(scenarioName), testCaseName);
        boolean inProjectReusable = hasTestCase(
            proj.getReusableScenarioByName(scenarioName),
            testCaseName
        );
        boolean inSharedReusable = hasTestCase(
            proj.getSharedReusableScenarioByName(scenarioName),
            testCaseName
        );

        int matchCount =
            (inTestPlan ? 1 : 0) + (inProjectReusable ? 1 : 0) + (inSharedReusable ? 1 : 0);
        if (matchCount != 1) {
            // 0 matches: nothing found anywhere, default to Test Plan (its historical meaning).
            // >1 match: same scenario+testcase name exists in multiple scopes - ambiguous.
            return matchCount == 0 ? "" : null;
        }
        if (inProjectReusable) {
            return "[Project]";
        }
        if (inSharedReusable) {
            return "[Shared]";
        }
        return "";
    }

    private boolean hasTestCase(Scenario scenario, String testCaseName) {
        return scenario != null && scenario.getTestCaseByName(testCaseName) != null;
    }

    private void loadGlobalData() {
        File file = new File(getLocation() + File.separator + "GlobalData.csv");
        setGlobalData(new CsvGlobalData(file.getAbsolutePath()));
    }

    @Override
    public CsvTestData getNewTestData(String name) {
        CsvTestData csvData = new CsvTestData(getLocation() + File.separator + name + ".csv");
        csvData.setColumns(Record.HEADERS);
        csvData.addColumn("Data1");
        csvData.addColumn("Data2");
        return csvData;
    }

    @Override
    public CsvTestData importTestData(File file) {
        CsvTestData csvData = new CsvTestData(file.getAbsolutePath());
        csvData.loadTableModel();
        csvData.setLocation(getLocation() + File.separator + file.getName());
        csvData.saveChanges();
        return csvData;
    }
}
