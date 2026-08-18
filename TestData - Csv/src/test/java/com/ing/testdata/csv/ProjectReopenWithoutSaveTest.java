package com.ing.testdata.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.component.Project;
import com.ing.datalib.testdata.TestDataFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Regression test for the reported "close without saving, reopen, migrated Scope values are
 * gone" scenario, exercised through the real {@link Project} constructor (the same one
 * AppMainFrame.loadProject uses) rather than a mocked Project - this is the closest
 * source-level simulation of "open project" -> "close without saving" -> "reopen" available
 * without driving the actual Swing UI.
 */
public class ProjectReopenWithoutSaveTest {
    private Path tempDir;
    private File csvFile;

    @BeforeMethod
    public void setUp() throws IOException {
        TestDataFactory.load();
        tempDir = Files.createTempDirectory("project-reopen-test");

        File testPlanScenario = new File(tempDir.toFile(), "TestPlan/Checkout");
        testPlanScenario.mkdirs();
        new File(testPlanScenario, "Pay.yaml").createNewFile();

        File testDataDir = new File(tempDir.toFile(), "TestData");
        testDataDir.mkdirs();
        csvFile = new File(testDataDir, "Legacy.csv");
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("Scenario,Flow,Iteration,SubIteration,User\n");
            fw.write("Checkout,Pay,1,1,legacy-user\n");
        }
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files
            .walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    @Test
    public void firstOpen_MigratesAndPersistsScope_WithNoExplicitSaveCall() throws IOException {
        // "Open project" - exactly what AppMainFrame.loadProject does: sProject = new Project(location)
        new Project(tempDir.toString());

        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertThat(lines.get(0)).isEqualTo("Scenario,Flow,Scope,Iteration,SubIteration,User");
        assertThat(lines.get(1)).isEqualTo("Checkout,Pay,,1,1,legacy-user");
    }

    @Test
    public void reopenAfterFirstOpen_WithNoSaveCallEver_StillHasMigratedScope() throws IOException {
        // "Open project" (first time) - migration runs and, per CsvDataProvider, persists
        // immediately.
        new Project(tempDir.toString());

        // "Close without saving": no .save()/.saveChanges() call of any kind happens here -
        // this is the entire point of the scenario. Then "reopen": construct a brand new
        // Project against the same directory, exactly as AppMainFrame.loadProject would on the
        // next launch.
        new Project(tempDir.toString());

        List<String> lines = Files.readAllLines(csvFile.toPath());
        assertThat(lines.get(0)).isEqualTo("Scenario,Flow,Scope,Iteration,SubIteration,User");
        assertThat(lines.get(1)).isEqualTo("Checkout,Pay,,1,1,legacy-user");
    }
}
