package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ing.datalib.component.Project;
import com.ing.datalib.testdata.model.TestDataModel;
import com.ing.engine.execution.run.ProjectRunner;
import com.ing.engine.execution.run.TestCaseRunner;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Real-file, real-Project regression test for "[Shared] Sheet:Column" vs "[Project] Sheet:Column"
 * resolution, reproducing a reported bug where a [Shared]-tagged reference printed the
 * project-level value instead of the shared one. Uses actual CSV files on disk and a real
 * Project instance (not mocks) since the reported bug was not reproducible by manual code
 * tracing alone.
 */
public class SharedTestDataResolutionIntegrationTest {
    private Path appRoot;
    private String originalUserDir;
    private Project project;

    @BeforeMethod
    public void setUp() throws IOException {
        com.ing.datalib.testdata.TestDataFactory.load();
        appRoot = Files.createTempDirectory("shared-td-app-root-");
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", appRoot.toString());

        File projectDir = appRoot.resolve("Projects/Tutorial").toFile();
        File projectTestDataDir = new File(projectDir, "TestData");
        projectTestDataDir.mkdirs();
        writeCsv(
            new File(projectTestDataDir, "Basic.csv"),
            "Scenario,Flow,Scope,Iteration,SubIteration,URL",
            "MortgageCalculation-Browser,High Income,,1,1,PROJECT_VALUE"
        );

        File sharedDir = appRoot.resolve("Shared/SharedTestData").toFile();
        sharedDir.mkdirs();
        writeCsv(
            new File(sharedDir, "TestData0.csv"),
            "Scenario,Flow,Scope,Iteration,SubIteration,URL",
            "MortgageCalculation-Browser,High Income,,1,1,SHARED_VALUE"
        );
        // A named Shared environment with its own copy of the sheet, plus the environment
        // registration file so EnvTestData loads it as a real environment.
        File sharedSitDir = new File(sharedDir, "SIT");
        sharedSitDir.mkdirs();
        writeCsv(
            new File(sharedSitDir, "TestData0.csv"),
            "Scenario,Flow,Scope,Iteration,SubIteration,URL",
            "MortgageCalculation-Browser,High Income,,1,1,SHARED_SIT_VALUE"
        );
        writeCsv(new File(sharedDir, "environment.properties"), "Environment=SIT");

        project = new Project(projectDir.getAbsolutePath());
    }

    @AfterMethod
    public void tearDown() throws IOException {
        if (originalUserDir != null) {
            System.setProperty("user.dir", originalUserDir);
        }
        Files
            .walk(appRoot)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    private void writeCsv(File file, String... lines) throws IOException {
        try (FileWriter fw = new FileWriter(file)) {
            for (String line : lines) {
                fw.write(line);
                fw.write("\n");
            }
        }
    }

    private TestCaseRunner mockContext() {
        return mockContext("Default", "Default");
    }

    private TestCaseRunner mockContext(String projectEnv, String sharedEnv) {
        TestCaseRunner context = mock(TestCaseRunner.class);
        ProjectRunner executor = mock(ProjectRunner.class);
        when(context.executor()).thenReturn(executor);
        when(context.project()).thenReturn(project);
        when(executor.getProject()).thenReturn(project);
        when(executor.dataProvider()).thenReturn(project.getTestData());
        when(executor.runEnv()).thenReturn(projectEnv);
        when(executor.sharedRunEnv()).thenReturn(sharedEnv);
        // Enough wiring for getIterations()/getIter() on a Test Plan (unscoped) test case.
        when(context.getRoot()).thenReturn(context);
        when(context.scenario()).thenReturn("MortgageCalculation-Browser");
        when(context.testcase()).thenReturn("High Income");
        when(context.isReusable()).thenReturn(false);
        return context;
    }

    @Test
    public void testSharedTaggedReferenceResolvesTheSharedSheet() {
        TestCaseRunner context = mockContext();

        TestDataModel shared = DataAccessInternal.getModel(context, "[Shared] TestData0");

        assertThat(shared).as("[Shared] TestData0 should resolve to the shared sheet").isNotNull();
        assertThat(shared.getName()).isEqualTo("TestData0");
    }

    @Test
    public void testProjectTaggedReferenceResolvesTheProjectSheet() {
        TestCaseRunner context = mockContext();

        TestDataModel proj = DataAccessInternal.getModel(context, "[Project] Basic");

        assertThat(proj).as("[Project] Basic should resolve to the project sheet").isNotNull();
        assertThat(proj.getName()).isEqualTo("Basic");
    }

    @Test
    public void testSharedAndProjectTaggedReferencesResolveDistinctValues() {
        TestCaseRunner context = mockContext();

        TestDataModel shared = DataAccessInternal.getModel(context, "[Shared] TestData0");
        TestDataModel proj = DataAccessInternal.getModel(context, "[Project] Basic");

        String sharedVal = DataAccessInternal.getDataFromModelWithScope(
            shared,
            "URL",
            "MortgageCalculation-Browser",
            "High Income",
            "1",
            "1",
            ""
        );
        String projVal = DataAccessInternal.getDataFromModelWithScope(
            proj,
            "URL",
            "MortgageCalculation-Browser",
            "High Income",
            "1",
            "1",
            ""
        );

        assertThat(sharedVal).isEqualTo("SHARED_VALUE");
        assertThat(projVal).isEqualTo("PROJECT_VALUE");
    }

    @Test
    public void testSharedRunEnvSelectsSharedEnvironmentIndependentlyOfProjectEnv() {
        // Project env stays Default; Shared env is SIT. The [Shared] reference must resolve the
        // SIT copy, while [Project] is unaffected and still resolves the project's Default value.
        TestCaseRunner context = mockContext("Default", "SIT");

        TestDataModel shared = DataAccessInternal.getModel(context, "[Shared] TestData0");
        TestDataModel proj = DataAccessInternal.getModel(context, "[Project] Basic");

        String sharedVal = DataAccessInternal.getDataFromModelWithScope(
            shared,
            "URL",
            "MortgageCalculation-Browser",
            "High Income",
            "1",
            "1",
            ""
        );
        String projVal = DataAccessInternal.getDataFromModelWithScope(
            proj,
            "URL",
            "MortgageCalculation-Browser",
            "High Income",
            "1",
            "1",
            ""
        );

        assertThat(sharedVal).isEqualTo("SHARED_SIT_VALUE");
        assertThat(projVal).isEqualTo("PROJECT_VALUE");
    }

    @Test
    public void testGetIterationsForSharedSheetHonoursSharedRunEnvNotProjectEnv() {
        // Regression: getIterations() gated the env lookup on validEnv() (project runEnv), so a
        // [Shared] sheet with a Shared env selected but the Project env left on Default fell
        // back to Shared Default and reported "Iteration 1 missing".
        TestCaseRunner context = mockContext("Default", "SIT");

        java.util.Set<String> iters = DataAccessInternal.getIterations(
            context,
            "[Shared] TestData0"
        );

        assertThat(iters).contains("1");
    }

    @Test
    public void testUnknownSharedRunEnvFallsBackToSharedDefault() {
        TestCaseRunner context = mockContext("Default", "NoSuchEnv");

        TestDataModel shared = DataAccessInternal.getModel(context, "[Shared] TestData0");
        String sharedVal = DataAccessInternal.getDataFromModelWithScope(
            shared,
            "URL",
            "MortgageCalculation-Browser",
            "High Income",
            "1",
            "1",
            ""
        );

        assertThat(sharedVal).isEqualTo("SHARED_VALUE");
    }

    @Test
    public void testIsInputPatternDataSheetRecognizesExactUserInputStrings() {
        assertThat(DataProcessor.isInputPatternDataSheet("[Shared] TestData0:URL")).isTrue();
        assertThat(DataProcessor.isInputPatternDataSheet("[Project] Basic:URL")).isTrue();
    }

    @Test
    public void testTestDataTokenParseFeedsScopeAwareModelLookup() {
        TestCaseRunner context = mockContext();

        // Untagged, {braced}, and [Project]-tagged all resolve to the same project sheet+value.
        for (String ref : new String[] {
            "Basic:URL",
            "{Basic:URL}",
            "[Project] Basic:URL",
            "{[Project] Basic:URL}"
        }) {
            String[] sc = TestDataToken.parse(ref);
            assertThat(sc).as(ref).isNotNull();
            TestDataModel model = DataAccessInternal.getModel(context, sc[0]);
            assertThat(model).as(ref).isNotNull();
            assertThat(model.getName()).as(ref).isEqualTo("Basic");
            assertThat(
                    DataAccessInternal.getDataFromModelWithScope(
                        model,
                        sc[1],
                        "MortgageCalculation-Browser",
                        "High Income",
                        "1",
                        "1",
                        ""
                    )
                )
                .as(ref)
                .isEqualTo("PROJECT_VALUE");
        }

        // The [Shared] tag routes to the app-root Shared store instead.
        String[] shared = TestDataToken.parse("{[Shared] TestData0:URL}");
        assertThat(DataAccessInternal.getModel(context, shared[0]).getName())
            .isEqualTo("TestData0");
    }
}
