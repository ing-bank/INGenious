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
        TestCaseRunner context = mock(TestCaseRunner.class);
        ProjectRunner executor = mock(ProjectRunner.class);
        when(context.executor()).thenReturn(executor);
        when(context.project()).thenReturn(project);
        when(executor.getProject()).thenReturn(project);
        when(executor.dataProvider()).thenReturn(project.getTestData());
        when(executor.runEnv()).thenReturn("Default");
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
    public void testIsInputPatternDataSheetRecognizesExactUserInputStrings() {
        assertThat(DataProcessor.isInputPatternDataSheet("[Shared] TestData0:URL")).isTrue();
        assertThat(DataProcessor.isInputPatternDataSheet("[Project] Basic:URL")).isTrue();
    }
}
