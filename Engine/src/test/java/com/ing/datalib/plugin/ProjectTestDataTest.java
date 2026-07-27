package com.ing.datalib.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.component.Project;
import com.ing.datalib.testdata.TestDataFactory;
import com.ing.ingenious.api.contract.data.TestDataViewApi;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Exercises {@link ProjectTestData} against a real project on disk.
 *
 * <p>The class under test lives in Datalib, but the test lives here: reading and writing test
 * data needs a test data provider, and the CSV provider is first on the class path in this
 * module. A test in Datalib could only use a stand-in, which would prove nothing about whether
 * a decision survives being written to a file and read back.
 */
public class ProjectTestDataTest {
    private static final String SHEET = "Customers";
    private static final String SCENARIO = "NewScenario";
    private static final String TEST_CASE = "NewTestCase";

    private Path temporaryDirectory;

    @BeforeMethod
    public void setUp() throws IOException {
        // Without this the CSV provider is not discovered and the Project constructor throws.
        TestDataFactory.load();
        temporaryDirectory = Files.createTempDirectory("project-test-data-");
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws IOException {
        if (temporaryDirectory == null) {
            return;
        }
        try (var paths = Files.walk(temporaryDirectory)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ex) {
                    path.toFile().deleteOnExit();
                }
            }
        }
    }

    @Test
    public void writtenTestCaseDataSurvivesReopeningTheProject() throws Exception {
        Project project = newProject();
        ProjectTestData testData = new ProjectTestData(() -> project);

        assertThat(testData.addSheet(SHEET)).isTrue();
        assertThat(testData.sheets()).contains(SHEET);
        assertThat(testData.addColumn(SHEET, "Segment")).isTrue();
        assertThat(testData.addColumn(SHEET, "Region")).isTrue();

        TestDataViewApi view = testData.testCase(SHEET, SCENARIO, TEST_CASE);
        assertThat(view).isNotNull();
        assertThat(view.update("Segment", "business")).isTrue();
        assertThat(view.update("Region", "north")).isTrue();
        assertThat(testData.save(SHEET)).isTrue();
        project.save();

        File sheetFile = new File(
            project.getLocation(),
            "TestData" + File.separator + SHEET + ".csv"
        );
        assertThat(sheetFile).exists();
        String written = Files.readString(sheetFile.toPath());

        // Reopen the project the way the application does, from the directory alone.
        Project reopened = new Project(project.getLocation());
        ProjectTestData reread = new ProjectTestData(() -> reopened);

        assertThat(reread.sheets()).contains(SHEET);
        TestDataViewApi back = reread.testCase(SHEET, SCENARIO, TEST_CASE);
        assertThat(back).isNotNull();
        assertThat(back.getField("Segment")).isEqualTo("business");
        assertThat(back.getField("Region")).isEqualTo("north");

        Reporter.log(
            "writtenTestCaseDataSurvivesReopeningTheProject EVIDENCE persistence: " +
            sheetFile.getAbsolutePath() +
            " contains\n" +
            written.trim() +
            "\nand a reopened project reads Segment=" +
            back.getField("Segment") +
            ", Region=" +
            back.getField("Region"),
            true
        );
    }

    @Test
    public void aTestCaseWithoutDataIsGivenItsFirstRecord() throws Exception {
        Project project = newProject();
        ProjectTestData testData = new ProjectTestData(() -> project);
        testData.addSheet(SHEET);

        TestDataViewApi view = testData.testCase(SHEET, SCENARIO, TEST_CASE);

        assertThat(view).isNotNull();
        assertThat(view.get()).hasSize(1);
        List<String> record = (List<String>) view.get().get(0);
        assertThat(record.get(0)).isEqualTo(SCENARIO);
        assertThat(record.get(1)).isEqualTo(TEST_CASE);
        // A record is exactly as wide as the sheet, or the CSV it is written to is ragged.
        assertThat(record).hasSameSizeAs(view.columns());

        // Asking again must not add a second record.
        assertThat(testData.testCase(SHEET, SCENARIO, TEST_CASE).get()).hasSize(1);

        Reporter.log(
            "aTestCaseWithoutDataIsGivenItsFirstRecord EVIDENCE first record: " +
            record +
            " for columns " +
            view.columns(),
            true
        );
    }

    @Test
    public void oneTestCaseIsNotConfusedWithAnother() throws Exception {
        Project project = newProject();
        ProjectTestData testData = new ProjectTestData(() -> project);
        testData.addSheet(SHEET);
        testData.addColumn(SHEET, "Segment");

        testData.testCase(SHEET, SCENARIO, TEST_CASE).update("Segment", "business");
        testData.testCase(SHEET, SCENARIO, "Another test case").update("Segment", "private");

        assertThat(testData.testCase(SHEET, SCENARIO, TEST_CASE).getField("Segment"))
            .isEqualTo("business");
        assertThat(testData.testCase(SHEET, SCENARIO, "Another test case").getField("Segment"))
            .isEqualTo("private");

        Reporter.log(
            "oneTestCaseIsNotConfusedWithAnother EVIDENCE separation: two test cases in one sheet kept their own values",
            true
        );
    }

    @Test
    public void noOpenProjectIsAnsweredWithNothingRatherThanAFailure() {
        ProjectTestData testData = new ProjectTestData(() -> null);

        assertThat(testData.sheets()).isEmpty();
        assertThat(testData.addSheet(SHEET)).isFalse();
        assertThat(testData.addColumn(SHEET, "Segment")).isFalse();
        assertThat(testData.testCase(SHEET, SCENARIO, TEST_CASE)).isNull();
        assertThat(testData.save(SHEET)).isFalse();

        Reporter.log(
            "noOpenProjectIsAnsweredWithNothingRatherThanAFailure EVIDENCE closed project: every call answered without throwing",
            true
        );
    }

    private Project newProject() {
        return new Project("Sample", temporaryDirectory.toAbsolutePath().toString(), "csv")
        .createProject();
    }
}
