package com.ing.testdata.csv;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for CsvGlobalData — CSV-backed global data model.
 */
public class CsvGlobalDataTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("csvglobal-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testConstructorWithNoFile() {
        String location = tempDir.resolve("nonexistent.csv").toString();
        CsvGlobalData gd = new CsvGlobalData(location);

        assertThat(gd.getColumnCount()).isGreaterThan(0);
        assertThat(gd.getColumnName(0)).isEqualTo("GlobalDataID");
    }

    @Test
    public void testConstructorWithExistingFile() throws IOException {
        File csvFile = tempDir.resolve("data.csv").toFile();
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("GlobalDataID,Name,Value\n");
            fw.write("#key1,TestName,TestVal\n");
        }

        CsvGlobalData gd = new CsvGlobalData(csvFile.getAbsolutePath());
        Set<String> cols = gd.loadColumns(csvFile);

        assertThat(cols).contains("GlobalDataID", "Name", "Value");
    }

    @Test
    public void testLoadRecordsFromFile() throws IOException {
        File csvFile = tempDir.resolve("records.csv").toFile();
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("GlobalDataID,Col1,Col2\n");
            fw.write("#row1,a,b\n");
            fw.write("#row2,c,d\n");
        }

        CsvGlobalData gd = new CsvGlobalData(csvFile.getAbsolutePath());
        gd.loadRecords(csvFile);

        assertThat(gd.getRowCount()).isEqualTo(2);
    }

    @Test
    public void testGetNameFromLocation() throws IOException {
        File csvFile = tempDir.resolve("mydata.csv").toFile();
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("GlobalDataID\n");
        }

        CsvGlobalData gd = new CsvGlobalData(csvFile.getAbsolutePath());
        assertThat(gd.getName()).isEqualTo("mydata");
    }

    @Test
    public void testRenameNotSupported() {
        String location = tempDir.resolve("data.csv").toString();
        CsvGlobalData gd = new CsvGlobalData(location);
        assertThat(gd.rename("newName")).isFalse();
    }

    @Test
    public void testGetNewRecord() {
        String location = tempDir.resolve("data.csv").toString();
        CsvGlobalData gd = new CsvGlobalData(location);
        assertThat(gd.getNewRecord()).isNotNull().isEmpty();
    }

    @Test
    public void testSaveChanges() throws IOException {
        File csvFile = tempDir.resolve("save.csv").toFile();
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("GlobalDataID,Col1\n");
        }

        CsvGlobalData gd = new CsvGlobalData(csvFile.getAbsolutePath());
        gd.loadRecords(csvFile);
        gd.addRecord();
        gd.setValueAt("#key1", 0, 0);
        gd.setValueAt("val1", 0, 1);
        gd.saveChanges();

        String content = new String(Files.readAllBytes(csvFile.toPath()));
        assertThat(content).contains("GlobalDataID");
    }
}
