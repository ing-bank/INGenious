package com.ing.testdata.csv;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestData;
import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.testdata.DataProvider;
import com.ing.datalib.testdata.model.Record;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

@DataProvider(type = "csv")
public class CsvDataProvider extends TestData {
    private static final Logger LOGGER = Logger.getLogger(CsvDataProvider.class.getName());
    private static final String BACKUP_DIR = ".migration-backup";

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
                    // Scope is persisted, static data once written. Only a file whose Scope column was
                    // just spliced in this load (legacy pre-Scope CSV) needs one-time population; a file
                    // that already had the column keeps whatever values it already has, untouched.
                    if (csvData.isScopeColumnMigrated()) {
                        migrateLegacyRecords(csvData);
                    } else if (hasEmptyScopeValues(csvData)) {
                        // Also populate Scope for files where column exists but values are empty
                        // This can happen if previous migration was interrupted or file was manually edited
                        LOGGER.log(
                            Level.INFO,
                            "Populating empty Scope values in {0}",
                            csvData.getName()
                        );
                        populateEmptyScopeValues(csvData);
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
        // Create backup before saving changes
        backupTestDataFile(csvData);
        csvData.saveChanges();
    }

    /**
     * Checks if any records have empty Scope values (column exists but value is null/empty).
     * @param csvData the test data to check
     * @return true if any record has an empty Scope value
     */
    private boolean hasEmptyScopeValues(CsvTestData csvData) {
        if (csvData.getColumns().size() <= 2) {
            return false; // Scope column doesn't exist
        }
        String scopeColumn = csvData.getColumns().get(2);
        if (!"Scope".equals(scopeColumn)) {
            return false; // Not the Scope column at index 2
        }
        // Check if any record has empty Scope
        for (Record record : csvData.getRecords()) {
            if (record.size() > 2) {
                String scopeValue = java.util.Objects.toString(record.get(2), "").trim();
                if (scopeValue.isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Populates empty Scope values in records where Scope column exists but values are missing.
     * Similar to migrateLegacyRecords but only updates records with empty Scope values.
     * @param csvData the test data to populate
     */
    private void populateEmptyScopeValues(CsvTestData csvData) {
        boolean hasChanges = false;
        for (Record record : csvData.getRecords()) {
            if (record.size() <= 2) {
                continue; // Skip malformed records
            }

            String currentScope = java.util.Objects.toString(record.get(2), "").trim();
            if (!currentScope.isEmpty()) {
                continue; // Skip records that already have a Scope value
            }

            String scenario = java.util.Objects.toString(record.get(0), "").trim();
            String testcase = java.util.Objects.toString(record.get(1), "").trim();
            String scope;
            String normalized = scenario;

            // Check scenario for explicit scope markers
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
                LOGGER.log(
                    Level.WARNING,
                    "Cannot uniquely resolve Scope for Scenario=[{0}] TestCase=[{1}] in {2} - " +
                    "found in multiple scopes. Leaving Scope empty; please set it explicitly.",
                    new Object[] { normalized, testcase, csvData.getName() }
                );
                record.set(0, normalized);
                continue;
            }

            record.set(2, scope);
            record.set(0, normalized);
            hasChanges = true;
        }

        if (hasChanges) {
            // Create backup before saving changes
            backupTestDataFile(csvData);
            csvData.saveChanges();
            LOGGER.log(Level.INFO, "Saved populated Scope values to {0}", csvData.getName());
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

    /**
     * Creates a backup of the test data CSV file and its associated TestData directory.
     * Backs up to <project>/.migration-backup/TestData/ preserving the relative path.
     *
     * @param csvData the test data model to backup
     */
    private void backupTestDataFile(CsvTestData csvData) {
        try {
            File csvFile = new File(csvData.getLocation());
            File immediateParent = csvFile.getParentFile();
            File projectRoot;

            // Handle both Default (TestData/) and non-Default (TestData/Environment/) structures
            if ("Default".equals(getEnviroment())) {
                // Default environment: CSV is directly in TestData/
                // Path: <project>/TestData/SomeData.csv
                projectRoot = immediateParent.getParentFile(); // TestData/ -> Project/
            } else {
                // Non-default environment: CSV is in TestData/Environment/
                // Path: <project>/TestData/Accp/SomeData.csv or <project>/TestData/Test/SomeData.csv
                projectRoot = immediateParent.getParentFile().getParentFile(); // TestData/Env/ -> TestData/ -> Project/
            }
            
            // Create backup at project root under .migration-backup/TestData/
            File backupFolder = new File(projectRoot, BACKUP_DIR + File.separator + "TestData");

            if (!backupFolder.exists()) {
                backupFolder.mkdirs();
            }

            // Backup the CSV file
            File csvBackup = new File(backupFolder, csvFile.getName());
            if (!csvBackup.exists()) {
                Files.copy(
                    csvFile.toPath(),
                    csvBackup.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
                );
                LOGGER.log(Level.INFO, "Created backup: {0}", csvBackup.getAbsolutePath());
            }

            // Backup the associated TestData directory if it exists
            // Convention: TestData CSV "MyData.csv" may have a folder "MyData/" with properties
            String csvBaseName = csvFile.getName().replace(".csv", "");
            File testDataDir = new File(immediateParent, csvBaseName);

            if (testDataDir.exists() && testDataDir.isDirectory()) {
                File testDataBackup = new File(backupFolder, csvBaseName);
                if (!testDataBackup.exists()) {
                    backupDirectory(testDataDir.toPath(), testDataBackup.toPath());
                    LOGGER.log(
                        Level.INFO,
                        "Created TestData directory backup: {0}",
                        testDataBackup.getAbsolutePath()
                    );
                }
            }
        } catch (IOException ex) {
            LOGGER.log(
                Level.WARNING,
                "Failed to create backup for {0}: {1}",
                new Object[] { csvData.getName(), ex.getMessage() }
            );
        }
    }

    /**
     * Recursively copies a directory and all its contents.
     *
     * @param source the source directory path
     * @param destination the destination directory path
     * @throws IOException if copy operation fails
     */
    private void backupDirectory(Path source, Path destination) throws IOException {
        Files
            .walk(source)
            .forEach(
                sourcePath -> {
                    try {
                        Path targetPath = destination.resolve(source.relativize(sourcePath));
                        if (Files.isDirectory(sourcePath)) {
                            if (!Files.exists(targetPath)) {
                                Files.createDirectories(targetPath);
                            }
                        } else {
                            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        LOGGER.log(
                            Level.WARNING,
                            "Failed to copy {0}: {1}",
                            new Object[] { sourcePath, e.getMessage() }
                        );
                    }
                }
            );
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
