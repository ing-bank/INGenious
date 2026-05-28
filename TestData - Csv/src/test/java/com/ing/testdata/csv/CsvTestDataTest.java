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
 * Tests for CsvTestData — CSV-backed test data model.
 */
public class CsvTestDataTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("csvtestdata-test");
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
        CsvTestData td = new CsvTestData(location);
        // No file means no columns loaded
        assertThat(td.getColumnCount()).isEqualTo(0);
    }

    @Test
    public void testConstructorWithExistingFile() throws IOException {
        File csvFile = tempDir.resolve("test.csv").toFile();
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("Scenario,Input1,Expected\n");
            fw.write("tc1,hello,world\n");
        }

        CsvTestData td = new CsvTestData(csvFile.getAbsolutePath());
        Set<String> cols = td.loadColumns(csvFile);
        assertThat(cols).contains("Scenario", "Input1", "Expected");
    }

    @Test
    public void testLoadRecordsFromFile() throws IOException {
        File csvFile = tempDir.resolve("records.csv").toFile();
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("Col1,Col2\n");
            fw.write("a,b\n");
            fw.write("c,d\n");
        }

        CsvTestData td = new CsvTestData(csvFile.getAbsolutePath());
        td.loadRecords(csvFile);

        assertThat(td.getRowCount()).isEqualTo(2);
    }

    @Test
    public void testGetNameFromLocation() throws IOException {
        File csvFile = tempDir.resolve("testcase.csv").toFile();
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("Col1\n");
        }

        CsvTestData td = new CsvTestData(csvFile.getAbsolutePath());
        assertThat(td.getName()).isEqualTo("testcase");
    }

    @Test
    public void testRenameWithExistingFile() throws IOException {
        File csvFile = tempDir.resolve("original.csv").toFile();
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("Col1\nval1\n");
        }

        CsvTestData td = new CsvTestData(csvFile.getAbsolutePath());
        Boolean result = td.rename("renamed");

        assertThat(result).isTrue();
        assertThat(td.getName()).isEqualTo("renamed");
        assertThat(new File(td.getLocation())).exists();
        assertThat(csvFile).doesNotExist();
    }

    @Test
    public void testRenameWithNoFile() {
        String location = tempDir.resolve("nofile.csv").toString();
        CsvTestData td = new CsvTestData(location);
        Boolean result = td.rename("newname");

        assertThat(result).isTrue();
        assertThat(td.getName()).isEqualTo("newname");
    }

    @Test
    public void testDeleteExistingFile() throws IOException {
        File csvFile = tempDir.resolve("todelete.csv").toFile();
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("Col1\nval1\n");
        }

        CsvTestData td = new CsvTestData(csvFile.getAbsolutePath());
        Boolean result = td.delete();

        assertThat(result).isTrue();
        assertThat(csvFile).doesNotExist();
    }

    @Test
    public void testDeleteNonExistentFile() {
        String location = tempDir.resolve("nofile.csv").toString();
        CsvTestData td = new CsvTestData(location);
        Boolean result = td.delete();

        assertThat(result).isTrue();
    }

    @Test
    public void testGetNewRecord() {
        String location = tempDir.resolve("data.csv").toString();
        CsvTestData td = new CsvTestData(location);
        assertThat(td.getNewRecord()).isNotNull().isEmpty();
    }

    @Test
    public void testSaveChanges() throws IOException {
        File csvFile = tempDir.resolve("save.csv").toFile();
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("Col1,Col2\n");
        }

        CsvTestData td = new CsvTestData(csvFile.getAbsolutePath());
        td.loadRecords(csvFile);
        td.addRecord();
        td.setValueAt("value1", 0, 0);
        td.setValueAt("value2", 0, 1);
        td.saveChanges();

        String content = new String(Files.readAllBytes(csvFile.toPath()));
        assertThat(content).contains("Col1");
    }
}
