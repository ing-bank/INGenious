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
 * rewritten to an {@code @Shared} reference.
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
        TestCase reusable = projectReusableCaseReferencing("R1", "RTC1", "Basic:URL@Project");

        Project.MakeSharedTestDataResult result = project.makeTestDataSheetShared(
            "Default",
            "Basic",
            new ArrayList<>()
        );

        assertThat(result.movedSheets).containsEntry("Basic", "Basic");
        assertThat(result.referenceUpdates).isEqualTo(2);
        assertThat(result.promoted).isEmpty();

        TestData sharedDefault = project.getSharedTestData().getTestDataFor("Default");
        assertThat(sharedDefault.getByNameIgnoreCase("Basic")).isNotNull();
        assertThat(new File(sharedDefault.getLocation(), "Basic.csv")).exists();

        TestData projectDefault = project.getTestData().getTestDataFor("Default");
        assertThat(projectDefault.getByNameIgnoreCase("Basic")).isNull();
        assertThat(new File(projectDefault.getLocation(), "Basic.csv")).doesNotExist();

        assertThat(firstInput(plan)).isEqualTo("Basic:URL@Shared");
        assertThat(firstInput(reusable)).isEqualTo("Basic:URL@Shared");
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
        assertThat(firstInput(plan)).isEqualTo("Basic_1:URL@Shared");
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
        assertThat(firstInput(movedTc)).isEqualTo("Basic:URL@Shared");
        // The now-empty Test Plan scenario is cleaned up; only the Shared Reusable S1 remains.
        assertThat(project.getScenarios()).noneMatch(s -> s.getName().equals("S1"));
    }

    @Test
    public void promotesProjectReusableTestCaseAndRetagsItsDataReference() throws Exception {
        importProjectSheet("Situation", "Default");
        TestCase reusable = projectReusableCaseReferencing(
            "MortgageFlow",
            "My income",
            "Situation:URL"
        );

        Project.MakeSharedTestDataResult result = project.makeTestDataSheetShared(
            "Default",
            "Situation",
            new ArrayList<>(Arrays.asList(reusable))
        );

        assertThat(result.promoted).hasSize(1);
        Scenario shared = project.getSharedReusableScenarioByName("MortgageFlow");
        assertThat(shared).isNotNull();
        TestCase movedTc = shared.getTestCaseByName("My income");
        assertThat(movedTc).isNotNull();
        assertThat(firstInput(movedTc)).isEqualTo("Situation:URL@Shared");
    }

    @Test
    public void promotesMultipleReusableTestCasesInSameScenarioAndRetagsAllOfThem()
        throws Exception {
        importProjectSheet("Situation", "Default");
        TestCase income = projectReusableCaseReferencing(
            "MortgageFlow",
            "My income",
            "Situation:Income"
        );
        TestCase expenses = projectReusableCaseReferencing(
            "MortgageFlow",
            "Mandatory expenses",
            "Situation:Debt"
        );
        TestCase plans = projectReusableCaseReferencing(
            "MortgageFlow",
            "Your plans",
            "Situation:BuyingSituation"
        );
        TestCase personal = projectReusableCaseReferencing(
            "MortgageFlow",
            "Personal situation",
            "Situation:Age"
        );

        Project.MakeSharedTestDataResult result = project.makeTestDataSheetShared(
            "Default",
            "Situation",
            new ArrayList<>(Arrays.asList(income, expenses, plans, personal))
        );

        assertThat(result.promoted).hasSize(4);
        Scenario shared = project.getSharedReusableScenarioByName("MortgageFlow");
        assertThat(shared).isNotNull();
        assertThat(firstInput(shared.getTestCaseByName("My income")))
            .isEqualTo("Situation:Income@Shared");
        assertThat(firstInput(shared.getTestCaseByName("Mandatory expenses")))
            .isEqualTo("Situation:Debt@Shared");
        assertThat(firstInput(shared.getTestCaseByName("Your plans")))
            .isEqualTo("Situation:BuyingSituation@Shared");
        assertThat(firstInput(shared.getTestCaseByName("Personal situation")))
            .isEqualTo("Situation:Age@Shared");

        // Verify persistence to disk, not just in-memory state: reload the project fresh.
        Project reloaded = new Project(project.getLocation());
        Scenario sharedReloaded = reloaded.getSharedReusableScenarioByName("MortgageFlow");
        assertThat(sharedReloaded).isNotNull();
        assertThat(firstInput(sharedReloaded.getTestCaseByName("My income")))
            .isEqualTo("Situation:Income@Shared");
        assertThat(firstInput(sharedReloaded.getTestCaseByName("Mandatory expenses")))
            .isEqualTo("Situation:Debt@Shared");
        assertThat(firstInput(sharedReloaded.getTestCaseByName("Your plans")))
            .isEqualTo("Situation:BuyingSituation@Shared");
        assertThat(firstInput(sharedReloaded.getTestCaseByName("Personal situation")))
            .isEqualTo("Situation:Age@Shared");
    }

    @Test
    public void retagsEmbeddedTokensInsideJsonPayloadsTooNotJustWholeInputs() throws Exception {
        importProjectSheet("Payment", "Default");
        TestCase apiCase = projectReusableCaseReferencing(
            "APIs",
            "POST_Payment",
            "{\"bank_name\": \"{Payment:Bank Name}\", \"account_number\": \"{Payment:Account Number}\"}"
        );

        Project.MakeSharedTestDataResult result = project.makeTestDataSheetShared(
            "Default",
            "Payment",
            new ArrayList<>(Arrays.asList(apiCase))
        );

        assertThat(result.referenceUpdates).isGreaterThanOrEqualTo(1);
        Scenario shared = project.getSharedReusableScenarioByName("APIs");
        assertThat(shared).isNotNull();
        assertThat(firstInput(shared.getTestCaseByName("POST_Payment")))
            .isEqualTo(
                "{\"bank_name\": \"{Payment:Bank Name@Shared}\", \"account_number\": \"{Payment:Account Number@Shared}\"}"
            );
    }

    @Test
    public void retagsEmbeddedTokenNestedDeepInsideJsonObjectsAndRetagsProjectTaggedOnesToo()
        throws Exception {
        importProjectSheet("Payment", "Default");
        TestCase apiCase = projectReusableCaseReferencing(
            "APIs",
            "POST_Payment",
            "{\"id\": 1, \"data\": {\"nested\": {\"field\": \"{Payment:Bank Name@Project}\"}}}"
        );

        project.makeTestDataSheetShared(
            "Default",
            "Payment",
            new ArrayList<>(Arrays.asList(apiCase))
        );

        Scenario shared = project.getSharedReusableScenarioByName("APIs");
        assertThat(firstInput(shared.getTestCaseByName("POST_Payment")))
            .isEqualTo(
                "{\"id\": 1, \"data\": {\"nested\": {\"field\": \"{Payment:Bank Name@Shared}\"}}}"
            );
    }

    @Test
    public void movingWholeEnvironmentRetagsAllReferencingReusableComponents() throws Exception {
        importProjectSheet("Basic", "Default");
        importProjectSheet("Situation", "Default");
        importProjectSheet("Payment", "Default");
        TestCase income = projectReusableCaseReferencing(
            "MortgageFlow",
            "My income",
            "Situation:Income"
        );
        TestCase expenses = projectReusableCaseReferencing(
            "MortgageFlow",
            "Mandatory expenses",
            "Situation:Debt"
        );
        TestCase plans = projectReusableCaseReferencing(
            "MortgageFlow",
            "Your plans",
            "Situation:BuyingSituation"
        );
        TestCase personal = projectReusableCaseReferencing(
            "MortgageFlow",
            "Personal situation",
            "Situation:Age"
        );
        TestCase mortgage = projectReusableCaseReferencing(
            "MortgageFlow",
            "My maximum mortgage",
            "Basic:URL"
        );

        Project.MakeSharedTestDataResult result = project.makeEnvironmentTestDataShared(
            "Default",
            new ArrayList<>(Arrays.asList(income, expenses, plans, personal, mortgage))
        );

        assertThat(result.promoted).hasSize(5);
        Scenario shared = project.getSharedReusableScenarioByName("MortgageFlow");
        assertThat(shared).isNotNull();
        assertThat(firstInput(shared.getTestCaseByName("My income")))
            .isEqualTo("Situation:Income@Shared");
        assertThat(firstInput(shared.getTestCaseByName("Mandatory expenses")))
            .isEqualTo("Situation:Debt@Shared");
        assertThat(firstInput(shared.getTestCaseByName("Your plans")))
            .isEqualTo("Situation:BuyingSituation@Shared");
        assertThat(firstInput(shared.getTestCaseByName("Personal situation")))
            .isEqualTo("Situation:Age@Shared");
        assertThat(firstInput(shared.getTestCaseByName("My maximum mortgage")))
            .isEqualTo("Basic:URL@Shared");
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
    public void movesEveryDatasheetOfAnEnvironmentButKeepsItEmptied() throws Exception {
        importProjectSheet("Sheet1", "QA");
        importProjectSheet("Sheet2", "QA");
        File qaFolder = new File(project.getTestData().getTestDataFor("QA").getLocation());
        assertThat(qaFolder).isDirectory();

        Project.MakeSharedTestDataResult result = project.makeEnvironmentTestDataShared(
            "QA",
            new ArrayList<>()
        );

        assertThat(result.movedSheets.keySet()).containsExactlyInAnyOrder("Sheet1", "Sheet2");
        // QA is kept - folder/tab stays, just emptied of datasheets
        TestData projectQa = project.getTestData().getTestDataFor("QA");
        assertThat(projectQa).isNotNull();
        assertThat(projectQa.getTestDataList()).isEmpty();
        assertThat(qaFolder).isDirectory();
        assertThat(project.getTestData().getEnvironments()).contains("QA");

        TestData sharedQa = project.getSharedTestData().getTestDataFor("QA");
        assertThat(sharedQa).isNotNull();
        assertThat(sharedQa.getByNameIgnoreCase("Sheet1")).isNotNull();
        assertThat(sharedQa.getByNameIgnoreCase("Sheet2")).isNotNull();
    }

    @Test
    public void movingANonDefaultEnvironmentLeavesDefaultsOwnSheetButRetagsReferencesAnyway()
        throws Exception {
        importProjectSheet("Common", "Default");
        importProjectSheet("Common", "QA");
        importProjectSheet("QaOnly", "QA");
        TestCase plan = testPlanCaseReferencing("S1", "TC1", "Common:URL");

        Project.MakeSharedTestDataResult result = project.makeEnvironmentTestDataShared(
            "QA",
            new ArrayList<>()
        );

        // Default keeps its own (now orphaned) Common sheet; QA is kept too, just emptied
        TestData projectDefault = project.getTestData().getTestDataFor("Default");
        assertThat(projectDefault.getByNameIgnoreCase("Common")).isNotNull();
        TestData projectQa = project.getTestData().getTestDataFor("QA");
        assertThat(projectQa).isNotNull();
        assertThat(projectQa.getTestDataList()).isEmpty();

        TestData sharedQa = project.getSharedTestData().getTestDataFor("QA");
        assertThat(sharedQa.getByNameIgnoreCase("Common")).isNotNull();
        assertThat(sharedQa.getByNameIgnoreCase("QaOnly")).isNotNull();

        // References are retagged regardless of Default still having its own Common sheet -
        // moving to Shared means every usage should redirect there.
        assertThat(firstInput(plan)).isEqualTo("Common:URL@Shared");
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

    @Test
    public void registersAndListsProjectsThatUseSharedTestData() throws Exception {
        importSharedSheet("Common");
        testPlanCaseReferencing("S1", "TC1", "Common:URL@Shared");

        project.registerSharedTestDataUsage();

        File items = new File(Project.getSharedTestDataPath(), "projects.items");
        assertThat(items).exists();
        assertThat(new String(java.nio.file.Files.readAllBytes(items.toPath())))
            .contains("SampleProject");

        // a second project rooted at the same shared store sees the first as a consumer,
        // and neither project lists itself
        Project other = new Project(tempDir.resolve("OtherProject").toString(), "csv")
        .createProject();
        assertThat(other.getOtherProjectsUsingSharedTestData())
            .anyMatch(s -> s.contains("SampleProject"));
        assertThat(project.getOtherProjectsUsingSharedTestData())
            .noneMatch(s -> s.contains("SampleProject"));
    }

    @Test
    public void savingATestCaseThatReferencesSharedDataRegistersTheProject() throws Exception {
        importSharedSheet("Common");
        // no explicit registerSharedTestDataUsage() call - TestCase.save() must do it
        testPlanCaseReferencing("S1", "TC1", "Common:URL@Shared");

        File items = new File(Project.getSharedTestDataPath(), "projects.items");
        assertThat(items).exists();
        assertThat(new String(java.nio.file.Files.readAllBytes(items.toPath())))
            .contains("SampleProject");
    }

    @Test
    public void detectsSharedTestDataReferencedByAnEmbeddedToken() throws Exception {
        importSharedSheet("Common");
        testPlanCaseReferencing("S1", "TC1", "{\"url\":\"{Common:URL@Shared}\"}");

        assertThat(project.usesSharedTestData()).isTrue();
        File items = new File(Project.getSharedTestDataPath(), "projects.items");
        assertThat(new String(java.nio.file.Files.readAllBytes(items.toPath())))
            .contains("SampleProject");
    }

    @Test
    public void unregistersWhenProjectNoLongerReferencesSharedTestData() throws Exception {
        importSharedSheet("Common");
        TestCase tc = testPlanCaseReferencing("S1", "TC1", "Common:URL@Shared");
        project.registerSharedTestDataUsage();
        File items = new File(Project.getSharedTestDataPath(), "projects.items");
        assertThat(new String(java.nio.file.Files.readAllBytes(items.toPath())))
            .contains("SampleProject");

        tc.getTestSteps().get(0).setInput("Local:URL");
        tc.save();
        project.registerSharedTestDataUsage();

        assertThat(new String(java.nio.file.Files.readAllBytes(items.toPath())))
            .doesNotContain("SampleProject");
    }
}
