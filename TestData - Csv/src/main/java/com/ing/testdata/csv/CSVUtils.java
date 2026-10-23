package com.ing.testdata.csv;

import com.ing.datalib.component.utils.CSVHParser;
import com.ing.datalib.component.utils.FileUtils;
import com.ing.datalib.testdata.model.AbstractDataModel;
import com.ing.datalib.testdata.model.GlobalDataModel;
import com.ing.datalib.testdata.model.Record;
import com.ing.datalib.testdata.model.TestDataModel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

public class CSVUtils {
    private static final Logger LOGGER = Logger.getLogger(CSVUtils.class.getName());
    private static final String BACKUP_DIR = ".migration-backup";

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void load(File location, AbstractDataModel sAbstractData) {
        CSVHParser parser = FileUtils.getCSVHParser(location);
        if (parser != null) {
            // A file loaded under an older schema (e.g. test data predating the Scope
            // column) still has its rows laid out in the old column order - splice in the
            // missing value at the same index the column list was migrated at, or every
            // value from that point on would silently be read as the wrong column.
            boolean needsScopeSplice =
                (sAbstractData instanceof TestDataModel) &&
                ((TestDataModel) sAbstractData).isScopeColumnMigrated();

            for (CSVRecord crecord : parser.getRecords()) {
                List<String> record = (List<String>) sAbstractData.getNewRecord();
                for (int i = 0; i < crecord.size(); i++) {
                    String val = crecord.get(i);
                    if (i < record.size()) {
                        // set into existing slot (Record constructor initializes default slots)
                        record.set(i, val);
                    } else {
                        record.add(val);
                    }
                }
                if (needsScopeSplice) {
                    record.add(Math.min(2, record.size()), "");
                }
                sAbstractData.addRecord((List) record);
            }
        }
    }

    public static Set<String> loadColumns(File location) {
        CSVHParser parser = FileUtils.getCSVHParser(location);
        if (parser != null) {
            return parser.getHeaderMap().keySet();
        }
        return new HashSet<>();
    }

    private static void createIfNotExists(String fileLoc) {
        File file = new File(fileLoc);
        file.getParentFile().mkdirs();
    }

    public static void saveChanges(GlobalDataModel globalData) {
        createIfNotExists(globalData.getLocation());
        try (
            FileWriter out = new FileWriter(new File(globalData.getLocation()));
            CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withIgnoreEmptyLines());
        ) {
            for (String header : globalData.getColumns()) {
                printer.print(header);
            }
            printer.println();
            globalData.removeEmptyRecords();
            for (List<String> record : globalData.getRecords()) {
                for (String value : record) {
                    printer.print(value);
                }
                printer.println();
            }
        } catch (Exception ex) {
            Logger.getLogger(CSVUtils.class.getName()).log(Level.SEVERE, "Error while saving", ex);
        }
    }

    public static void saveChanges(TestDataModel testData) {
        createIfNotExists(testData.getLocation());
        try (
            FileWriter out = new FileWriter(new File(testData.getLocation()));
            CSVPrinter printer = new CSVPrinter(out, CSVFormat.EXCEL.withIgnoreEmptyLines());
        ) {
            // Write headers (including Scope column at index 2)
            List<String> columns = testData.getColumns();
            for (String header : columns) {
                printer.print(header);
            }
            printer.println();

            // Defensive check: warn if Scope column is missing
            if (columns.size() > 2 && !"Scope".equals(columns.get(2))) {
                Logger
                    .getLogger(CSVUtils.class.getName())
                    .log(
                        Level.WARNING,
                        "Scope column expected at index 2 but found: {0}",
                        columns.get(2)
                    );
            }

            testData.removeEmptyRecords();
            for (Record record : testData.getRecords()) {
                for (String value : record) {
                    printer.print(value);
                }
                printer.println();
            }
        } catch (Exception ex) {
            Logger.getLogger(CSVUtils.class.getName()).log(Level.SEVERE, "Error while saving", ex);
        }
    }

    /**
     * Creates a backup of the test data CSV file and its associated TestData directory.
     * Backs up to <project>/.migration-backup/TestData/ (or
     * <project>/.migration-backup/TestData/<Environment>/ for non-Default environments),
     * preserving the relative path so each environment's files are backed up separately.
     *
     * @param csvData the test data model to backup
     * @param environment the environment the test data belongs to (e.g. "Default", "Accp")
     */
    public static void backupTestDataFile(CsvTestData csvData, String environment) {
        try {
            File csvFile = new File(csvData.getLocation());
            File immediateParent = csvFile.getParentFile();
            File projectRoot;

            // Handle both Default (TestData/) and non-Default (TestData/Environment/) structures
            if ("Default".equals(environment)) {
                // Default environment: CSV is directly in TestData/
                // Path: <project>/TestData/SomeData.csv
                projectRoot = immediateParent.getParentFile(); // TestData/ -> Project/
            } else {
                // Non-default environment: CSV is in TestData/Environment/
                // Path: <project>/TestData/Accp/SomeData.csv or <project>/TestData/Test/SomeData.csv
                projectRoot = immediateParent.getParentFile().getParentFile(); // TestData/Env/ -> TestData/ -> Project/
            }

            // Create backup at project root under .migration-backup/, mirroring the
            // TestData/ or TestData/<Environment>/ structure so each environment's
            // files land in their own subfolder instead of colliding into one.
            String relativeTestDataPath = projectRoot
                .toPath()
                .relativize(immediateParent.toPath())
                .toString();
            File backupFolder = new File(
                projectRoot,
                BACKUP_DIR + File.separator + relativeTestDataPath
            );

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
    private static void backupDirectory(Path source, Path destination) throws IOException {
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
}
