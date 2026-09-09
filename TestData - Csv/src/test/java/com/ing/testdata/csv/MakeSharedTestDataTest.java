package com.ing.testdata.csv;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestData;
import com.ing.datalib.testdata.TestDataFactory;
import com.ing.datalib.testdata.model.TestDataModel;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for {@link Project#makeTestDataSheetShared(String, java.util.List)} and
 * {@link Project#makeEnvironmentTestDataShared(String, java.util.List)} - the "Make As Shared
 * TestData" flow: a project datasheet is moved into the app-root Shared Test Data store and
 * every whole-input reference to it (Test Plan, Project Reusables, Shared Reusables) is
 * rewritten to a {@code [Shared]} reference.
 */
public class MakeSharedTestDataTest {
    private Path tempDir;
    private String originalUserDir;
    private Project project;

    @BeforeMethod
    public void setUp() throws Exception {
        TestDataFactory.load();

        tempDir = Files.createTempDirectory("make-shared-testdata-test");
        originalUserDir = System.getProperty("user.dir");
        // Shared Test Data resolves from user.dir - isolate it under the temp dir.
        System.setProperty("user.dir", tempDir.toString());

        project = new Project(tempDir.resolve("SampleProject").toString(), "csv").createProject();
    }

    @AfterMethod
    public void tearDown() throws Exception {
        System.setProperty("user.dir", originalUserDir);
        Files
            .walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void importProjectSheet(String sheetName, String env) throws IOException {
        File source = tempDir.resolve(sheetName + ".csv").toFile();
        try (FileWriter fw = new FileWriter(source)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration,URL\n");
            fw.write("Buy,Step1,,1,1,https://project\n");
        }
        if (project.getTestData().getTestDataFor(env) == null) {
            project.getTestData().createNewEnvironment(env);
        }
        project.getTestData().importTestData(source, Arrays.asList(env));
    }

    private void importSharedSheet(String sheetName) throws IOException {
        File source = tempDir.resolve("shared-" + sheetName + ".csv").toFile();
        try (FileWriter fw = new FileWriter(source)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration,URL\n");
            fw.write("Buy,Step1,,1,1,https://shared\n");
        }
        // importTestData keys the new sheet off the source file name.
        File renamed = tempDir.resolve(sheetName + ".csv").toFile();
        renamed.delete();
        source.renameTo(renamed);
        project.getSharedTestData().importTestData(renamed, Arrays.asList("Default"));
    }

    private TestCase testPlanCaseReferencing(String scen, String tc, String input) {
        Scenario scenario = project.getScenarioByName(scen);
        if (scenario == null) {
            scenario = project.addScenario(scen);
        }
        TestCase testCase = scenario.addTestCase(tc);
        testCase.addNewStep().setInput(input);
        testCase.save();
        return testCase;
    }

    private TestCase projectReusableCaseReferencing(String scen, String tc, String input) {
        Scenario scenario = project.getReusableScenarioByName(scen);
        if (scenario == null) {
            scenario = project.addReusableScenario(scen);
        }
        TestCase testCase = scenario.addTestCase(tc);
        testCase.addNewStep().setInput(input);
        testCase.save();
        return testCase;
    }

    private String firstInput(TestCase tc) {
        tc.loadTableModel();
        return tc.getTestSteps().get(0).getInput();
    }

    // ─── tests ───────────────────────────────────────────────────────────────

    @Test
    public void movesSheetAndRetagsReferencesAcrossScopes() throws Exception {
        importProjectSheet("Basic", "Default");
        TestCase plan = testPlanCaseReferencing("S1", "TC1", "Basic:URL");
        TestCase reusable = projectReusableCaseReferencing("R1", "RTC1", "[Project] Basic:URL");

        Project.MakeSharedTestDataResult result = project.makeTestDataSheetShared(
            "Default",
            "Basic",
            new ArrayList<>()
        );

        assertThat(result.movedSheets).containsEntry("Basic", "Basic");
        assertThat(result.referenceUpdates).isEqualTo(2);
        assertThat(result.promoted).isEmpty();
        assertThat(result.partiallyMovedSheets).isEmpty();

        TestData sharedDefault = project.getSharedTestData().getTestDataFor("Default");
        assertThat(sharedDefault.getByNameIgnoreCase("Basic")).isNotNull();
        assertThat(new File(sharedDefault.getLocation(), "Basic.csv")).exists();

        TestData projectDefault = project.getTestData().getTestDataFor("Default");
        assertThat(projectDefault.getByNameIgnoreCase("Basic")).isNull();
        assertThat(new File(projectDefault.getLocation(), "Basic.csv")).doesNotExist();

        assertThat(firstInput(plan)).isEqualTo("[Shared] Basic:URL");
        assertThat(firstInput(reusable)).isEqualTo("[Shared] Basic:URL");
    }

    @Test
    public void suffixesTheSheetNameOnCollisionInSharedStore() throws Exception {
        importSharedSheet("Basic");
        importProjectSheet("Basic", "Default");
        TestCase plan = testPlanCaseReferencing("S1", "TC1", "Basic:URL");

        Project.MakeSharedTestDataResult result = project.makeTestDataSheetShared(
            "Default",
            "Basic",
            new ArrayList<>()
        );

        assertThat(result.movedSheets).containsEntry("Basic", "Basic_1");
        TestData sharedDefault = project.getSharedTestData().getTestDataFor("Default");
        assertThat(sharedDefault.getByNameIgnoreCase("Basic")).isNotNull(); // pre-existing
        assertThat(sharedDefault.getByNameIgnoreCase("Basic_1")).isNotNull(); // moved copy
        assertThat(firstInput(plan)).isEqualTo("[Shared] Basic_1:URL");
    }

    @Test
    public void promotesReferencingTestCasesToSharedReusableWhenRequested() throws Exception {
        importProjectSheet("Basic", "Default");
        TestCase plan = testPlanCaseReferencing("S1", "TC1", "Basic:URL");

        Project.MakeSharedTestDataResult result = project.makeTestDataSheetShared(
            "Default",
            "Basic",
            new ArrayList<>(Arrays.asList(plan))
        );

        assertThat(result.promoted).hasSize(1);
        Scenario shared = project.getSharedReusableScenarioByName("S1");
        assertThat(shared).isNotNull();
        TestCase movedTc = shared.getTestCaseByName("TC1");
        assertThat(movedTc).isNotNull();
        assertThat(firstInput(movedTc)).isEqualTo("[Shared] Basic:URL");
        // The now-empty Test Plan scenario is cleaned up; only the Shared Reusable S1 remains.
        assertThat(project.getScenarios()).noneMatch(s -> s.getName().equals("S1"));
    }

    @Test
    public void detectsTestCasesFromDatasheetRowsNotJustStepReferences() throws Exception {
        File source = tempDir.resolve("Rows.csv").toFile();
        try (FileWriter fw = new FileWriter(source)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration,URL\n");
            fw.write("Buy,Step1,[Project],1,1,x\n");
        }
        project.getTestData().importTestData(source, Arrays.asList("Default"));

        Scenario reusable = project.addReusableScenario("Buy");
        reusable.addTestCase("Step1"); // no step references "Rows" at all

        List<TestCase> promotable = project.getPromotableTestCasesForSheet("Rows");

        assertThat(promotable).extracting(TestCase::getName).containsExactly("Step1");
    }

    @Test
    public void movesEveryDatasheetOfAnEnvironmentAndDeletesItsFolder() throws Exception {
        importProjectSheet("Sheet1", "QA");
        importProjectSheet("Sheet2", "QA");
        File qaFolder = new File(project.getTestData().getTestDataFor("QA").getLocation());
        assertThat(qaFolder).isDirectory();

        Project.MakeSharedTestDataResult result = project.makeEnvironmentTestDataShared(
            "QA",
            new ArrayList<>()
        );

        assertThat(result.movedSheets.keySet()).containsExactlyInAnyOrder("Sheet1", "Sheet2");
        assertThat(project.getTestData().getTestDataFor("QA")).isNull();
        // the whole environment folder (with its GlobalData.csv) is gone from disk
        assertThat(qaFolder).doesNotExist();
        // and the project's environment.properties no longer lists QA
        assertThat(project.getTestData().getEnvironments()).doesNotContain("QA");

        TestData sharedQa = project.getSharedTestData().getTestDataFor("QA");
        assertThat(sharedQa).isNotNull();
        assertThat(sharedQa.getByNameIgnoreCase("Sheet1")).isNotNull();
        assertThat(sharedQa.getByNameIgnoreCase("Sheet2")).isNotNull();
    }

    @Test
    public void movingANonDefaultEnvironmentLeavesDefaultUntouched() throws Exception {
        importProjectSheet("Common", "Default");
        importProjectSheet("Common", "QA");
        importProjectSheet("QaOnly", "QA");
        TestCase plan = testPlanCaseReferencing("S1", "TC1", "Common:URL");

        Project.MakeSharedTestDataResult result = project.makeEnvironmentTestDataShared(
            "QA",
            new ArrayList<>()
        );

        // Default keeps its Common sheet; only QA's copies moved
        TestData projectDefault = project.getTestData().getTestDataFor("Default");
        assertThat(projectDefault.getByNameIgnoreCase("Common")).isNotNull();
        assertThat(project.getTestData().getTestDataFor("QA")).isNull();

        TestData sharedQa = project.getSharedTestData().getTestDataFor("QA");
        assertThat(sharedQa.getByNameIgnoreCase("Common")).isNotNull();
        assertThat(sharedQa.getByNameIgnoreCase("QaOnly")).isNotNull();

        // Common still lives in project Default -> its references are left as-is and reported
        assertThat(result.partiallyMovedSheets).containsKey("Common");
        assertThat(result.partiallyMovedSheets.get("Common")).containsExactly("Default");
        assertThat(firstInput(plan)).isEqualTo("Common:URL");
        // QaOnly was unique to QA -> no partial-move entry for it
        assertThat(result.partiallyMovedSheets).doesNotContainKey("QaOnly");
    }

    @Test
    public void keepsDefaultEnvironmentButEmptiesItAndDropsGlobalData() throws Exception {
        importProjectSheet("Basic", "Default");
        File globalDataCsv = new File(
            project.getTestData().getTestDataFor("Default").getGlobalData().getLocation()
        );

        project.makeEnvironmentTestDataShared("Default", new ArrayList<>());

        TestData projectDefault = project.getTestData().getTestDataFor("Default");
        assertThat(projectDefault).isNotNull();
        assertThat(projectDefault.getByNameIgnoreCase("Basic")).isNull();
        assertThat(globalDataCsv).doesNotExist();
        assertThat(
                project.getSharedTestData().getTestDataFor("Default").getByNameIgnoreCase("Basic")
            )
            .isNotNull();
    }
}
