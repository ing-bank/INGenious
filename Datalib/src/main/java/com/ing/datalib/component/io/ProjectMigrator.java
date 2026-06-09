package com.ing.datalib.component.io;

import com.ing.datalib.component.Project;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converts CSV test cases, reusable components, and test sets in a project to
 * YAML on disk. Idempotent — re-running on an already-migrated project is a
 * no-op.
 *
 * <p>This utility is intentionally decoupled from the {@link Project} runtime
 * model: it operates directly on the filesystem so it can run before or after
 * a project is loaded. After running, callers should reload the project to
 * pick up the new files.
 *
 * <p>Behaviour matches §5 of {@code YAML_TESTCASE_MIGRATION_PLAN.md}:
 * <ul>
 *   <li>Round-trip is verified by re-reading the freshly-written YAML and
 *       comparing row count + cell values against the original CSV. On
 *       mismatch the CSV is preserved and the YAML is removed.</li>
 *   <li>When {@code keepCsvBackup} is true, original CSVs are moved to
 *       {@code <project>/.migration-backup/} preserving the relative path.
 *       Otherwise CSV files are deleted after a successful round-trip.</li>
 *   <li>Existing YAML files take precedence: when both YAML and CSV exist
 *       for the same logical name, the CSV is moved to
 *       {@code .migration-backup/conflicts/}.</li>
 * </ul>
 */
public final class ProjectMigrator {
    private static final Logger LOGGER = Logger.getLogger(ProjectMigrator.class.getName());

    public static final String BACKUP_DIR = ".migration-backup";
    public static final String CONFLICTS_SUBDIR = "conflicts";

    /** Result of a single migration run. */
    public static final class Result {
        public final List<File> converted = new ArrayList<>();
        public final List<File> skippedYamlExisted = new ArrayList<>();
        public final List<File> conflicts = new ArrayList<>();
        public final List<String> errors = new ArrayList<>();

        public boolean hasChanges() {
            return !converted.isEmpty() || !conflicts.isEmpty();
        }
    }

    private ProjectMigrator() {}

    /**
     * Migrates all CSV test cases / test sets under the given project root to YAML.
     *
     * @param projectRoot  directory containing {@code TestPlan/}, {@code ReusableComponents/}, {@code TestLab/}
     * @param dryRun       when true, report what would change without writing
     * @param keepCsvBackup when true, move original CSVs to {@code .migration-backup/}
     */
    public static Result migrate(File projectRoot, boolean dryRun, boolean keepCsvBackup) {
        Result result = new Result();
        if (projectRoot == null || !projectRoot.isDirectory()) {
            result.errors.add("Project root not found: " + projectRoot);
            return result;
        }

        File backupRoot = new File(projectRoot, BACKUP_DIR);

        // Test plan and reusable components — use TestCaseStore.
        migrateFolderTree(
            new File(projectRoot, Project.TEST_PLAN_DIR),
            projectRoot,
            backupRoot,
            TestCaseStoreFactory.csvTestCaseStore(),
            TestCaseStoreFactory.yamlTestCaseStore(),
            /*testSet*/false,
            dryRun,
            keepCsvBackup,
            result
        );
        migrateFolderTree(
            new File(projectRoot, Project.REUSABLE_COMPONENTS_DIR),
            projectRoot,
            backupRoot,
            TestCaseStoreFactory.csvTestCaseStore(),
            TestCaseStoreFactory.yamlTestCaseStore(),
            /*testSet*/false,
            dryRun,
            keepCsvBackup,
            result
        );

        // Test lab — use TestSetStore.
        migrateFolderTree(
            new File(projectRoot, "TestLab"),
            projectRoot,
            backupRoot,
            null,
            null,
            /*testSet*/true,
            dryRun,
            keepCsvBackup,
            result
        );

        return result;
    }

    private static void migrateFolderTree(
        File root,
        File projectRoot,
        File backupRoot,
        TestCaseStore csvTc,
        TestCaseStore yamlTc,
        boolean testSet,
        boolean dryRun,
        boolean keepCsvBackup,
        Result result
    ) {
        if (root == null || !root.isDirectory()) {
            return;
        }
        File[] children = root.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                migrateFolderTree(
                    child,
                    projectRoot,
                    backupRoot,
                    csvTc,
                    yamlTc,
                    testSet,
                    dryRun,
                    keepCsvBackup,
                    result
                );
            } else if (TestCaseFormat.fromFile(child) == TestCaseFormat.CSV) {
                migrateOneFile(
                    child,
                    projectRoot,
                    backupRoot,
                    csvTc,
                    yamlTc,
                    testSet,
                    dryRun,
                    keepCsvBackup,
                    result
                );
            }
        }
    }

    private static void migrateOneFile(
        File csvFile,
        File projectRoot,
        File backupRoot,
        TestCaseStore csvTc,
        TestCaseStore yamlTc,
        boolean testSet,
        boolean dryRun,
        boolean keepCsvBackup,
        Result result
    ) {
        String baseName = TestCaseFormat.stripExtension(csvFile.getName());
        File parent = csvFile.getParentFile();
        File yamlFile = new File(parent, baseName + TestCaseFormat.YAML.extension());

        if (yamlFile.isFile()) {
            // YAML already wins; archive the CSV under conflicts/ when keeping backups.
            result.conflicts.add(csvFile);
            if (!dryRun) {
                if (keepCsvBackup) {
                    File dest = new File(
                        new File(backupRoot, CONFLICTS_SUBDIR),
                        relativize(projectRoot, csvFile)
                    );
                    moveQuietly(csvFile, dest, result);
                } else {
                    deleteQuietly(csvFile, result);
                }
            }
            return;
        }

        try {
            List<List<String>> rows;
            if (testSet) {
                rows = TestCaseStoreFactory.csvTestSetStore().load(csvFile);
            } else {
                rows = csvTc.load(csvFile);
            }

            if (dryRun) {
                result.converted.add(csvFile);
                return;
            }

            String parentName = parent == null ? null : parent.getName();
            if (testSet) {
                TestCaseStoreFactory.yamlTestSetStore().save(yamlFile, baseName, parentName, rows);
            } else {
                yamlTc.save(yamlFile, baseName, parentName, /*reusable*/false, /*tags*/null, rows);
            }

            // Round-trip verification.
            List<List<String>> roundTripped;
            if (testSet) {
                roundTripped = TestCaseStoreFactory.yamlTestSetStore().load(yamlFile);
            } else {
                roundTripped = yamlTc.load(yamlFile);
            }
            if (!rowsEqual(rows, roundTripped, testSet)) {
                yamlFile.delete();
                result.errors.add("Round-trip mismatch, kept CSV: " + csvFile.getPath());
                return;
            }

            if (keepCsvBackup) {
                File dest = new File(backupRoot, relativize(projectRoot, csvFile));
                moveQuietly(csvFile, dest, result);
            } else {
                deleteQuietly(csvFile, result);
            }
            result.converted.add(csvFile);
        } catch (Exception ex) {
            // Roll back partial YAML if any.
            if (yamlFile.exists()) {
                yamlFile.delete();
            }
            result.errors.add("Failed to migrate " + csvFile.getPath() + ": " + ex.getMessage());
            LOGGER.log(Level.WARNING, "Migration failed for " + csvFile.getPath(), ex);
        }
    }

    private static boolean rowsEqual(List<List<String>> a, List<List<String>> b, boolean testSet) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            List<String> ra = a.get(i);
            List<String> rb = b.get(i);
            int width = Math.max(ra.size(), rb.size());
            for (int c = 0; c < width; c++) {
                String va = c < ra.size() ? nullToEmpty(ra.get(c)) : "";
                String vb = c < rb.size() ? nullToEmpty(rb.get(c)) : "";
                if (!va.equals(vb)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String relativize(File root, File file) {
        Path rootPath = root.toPath().toAbsolutePath();
        Path filePath = file.toPath().toAbsolutePath();
        try {
            return rootPath.relativize(filePath).toString();
        } catch (IllegalArgumentException ex) {
            return file.getName();
        }
    }

    private static void moveQuietly(File src, File dest, Result result) {
        try {
            File destParent = dest.getParentFile();
            if (destParent != null) {
                destParent.mkdirs();
            }
            Files.move(src.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            result.errors.add(
                "Failed to move " + src.getPath() + " -> " + dest.getPath() + ": " + ex.getMessage()
            );
            LOGGER.log(Level.WARNING, "Move failed", ex);
        }
    }

    private static void deleteQuietly(File file, Result result) {
        if (!file.delete()) {
            result.errors.add("Failed to delete " + file.getPath());
        }
    }
}
