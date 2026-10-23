package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ing.datalib.component.EnvTestData;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.TestData;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.data.DataNotFoundException;
import com.ing.engine.execution.run.ProjectRunner;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.testdata.csv.CsvTestData;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Reproduces/verifies ING-REPORTED bug: "Using Shared Test Data in String Operations within
 * Loops ends the iteration prematurely" - a Test Plan test case (no reusable scope) reading an
 * {@code @Shared}-tagged sheet must see exactly the same Iteration/SubIteration set, and the same
 * end-of-data point, as reading a same-shaped sheet from the project's own Test Data - since both
 * a Project sheet ("ProjectSheet") and a same-structure Shared sheet ("SharedSheet@Shared") in
 * this test carry three matching sub-iterations (1,2,3 / a,b,c), a dynamic Param Loop driven by
 * either reference must run all three before hitting the EndOfDataSheet signal, never fewer.
 */
public class ProjectVsSharedSheetParamLoopIntegrationTest {
    @Mock
    private TestCaseRunner context;

    @Mock
    private ProjectRunner executor;

    @Mock
    private EnvTestData projectDataProvider;

    @Mock
    private TestData projectDefData;

    @Mock
    private Project project;

    @Mock
    private EnvTestData sharedDataProvider;

    @Mock
    private TestData sharedDefData;

    @Mock
    private CommandControl control;

    private AutoCloseable mocks;
    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);
        tempDir = Files.createTempDirectory("project-vs-shared-sheet-test");

        File projectCsv = tempDir.resolve("ProjectSheet.csv").toFile();
        try (FileWriter fw = new FileWriter(projectCsv)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration,Value\n");
            fw.write("Test1,Tc1,,1,1,a\n");
            fw.write("Test1,Tc1,,1,2,b\n");
            fw.write("Test1,Tc1,,1,3,c\n");
        }
        CsvTestData projectModel = new CsvTestData(projectCsv.getAbsolutePath());
        projectModel.loadRecords(projectCsv);

        File sharedCsv = tempDir.resolve("SharedSheet.csv").toFile();
        try (FileWriter fw = new FileWriter(sharedCsv)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration,Value\n");
            fw.write("Test1,Tc1,,1,1,a\n");
            fw.write("Test1,Tc1,,1,2,b\n");
            fw.write("Test1,Tc1,,1,3,c\n");
        }
        CsvTestData sharedModel = new CsvTestData(sharedCsv.getAbsolutePath());
        sharedModel.loadRecords(sharedCsv);

        // Test Plan (non-reusable) context: resolvedReusableScope == null, root == self.
        when(context.scenario()).thenReturn("Test1");
        when(context.testcase()).thenReturn("Tc1");
        when(context.iteration()).thenReturn("1");
        when(context.getResolvedReusableScope()).thenReturn(null);
        when(context.getRoot()).thenReturn(context);
        when(context.executor()).thenReturn(executor);
        when(context.project()).thenReturn(project);
        when(context.getControl()).thenReturn(control);
        when(control.getRunTimeVars()).thenReturn(new java.util.HashMap<>());

        // Project store: Default env == run env, so validEnv() is false -> def model path only.
        when(executor.runEnv()).thenReturn("Default");
        when(executor.sharedRunEnv()).thenReturn("Default");
        when(executor.dataProvider()).thenReturn(projectDataProvider);
        when(projectDataProvider.defEnv()).thenReturn("Default");
        when(projectDataProvider.defData()).thenReturn(projectDefData);
        when(projectDefData.getByName("ProjectSheet")).thenReturn(projectModel);

        // Shared store: same "Default" env, mirroring REQ-003 (Shared follows project's env).
        when(project.getSharedTestData()).thenReturn(sharedDataProvider);
        when(sharedDataProvider.defEnv()).thenReturn("Default");
        when(sharedDataProvider.getTestDataFor("Default")).thenReturn(sharedDefData);
        when(sharedDataProvider.defData()).thenReturn(sharedDefData);
        when(sharedDefData.getByName("SharedSheet")).thenReturn(sharedModel);
    }

    @AfterMethod
    public void tearDown() throws Exception {
        mocks.close();
        Files
            .walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    @Test
    public void projectAndSharedSheetsReportTheSameIterationSet() {
        assertThat(DataAccessInternal.getIterations(context, "ProjectSheet")).containsExactly("1");
        assertThat(DataAccessInternal.getIterations(context, "SharedSheet@Shared"))
            .as("An @Shared sheet with the same Iteration value must resolve identically")
            .containsExactly("1");
    }

    @Test
    public void projectAndSharedSheetsReportTheSameSubIterationSet() {
        Set<String> projectSubIters = DataAccessInternal.getSubIterations(context, "ProjectSheet");
        Set<String> sharedSubIters = DataAccessInternal.getSubIterations(
            context,
            "SharedSheet@Shared"
        );

        assertThat(projectSubIters).containsExactlyInAnyOrder("1", "2", "3");
        assertThat(sharedSubIters)
            .as(
                "A same-shaped @Shared sheet must report the same SubIteration set as the " +
                "equivalent Project sheet - a loop driven by either must run all 3 rows"
            )
            .containsExactlyInAnyOrder("1", "2", "3");
    }

    @Test
    public void projectAndSharedSheetsResolveAllThreeSubIterationsBeforeEndOfData() {
        // getData's success path reaches Control.getCurrentProject() global static state (not
        // wired here, same limitation noted in DataAccessTest) - exercise the resolve-or-throw
        // decision itself (DataAccessInternal.throwErrorWithCause) rather than the final
        // variable-substitution step, which is already covered by the Iteration/SubIteration
        // set assertions above.
        assertThat(DataAccessInternal.getSubIterations(context, "ProjectSheet"))
            .as("Row 4 doesn't exist in the Project sheet - subIteration 4 must be absent")
            .doesNotContain("4");
        assertThat(DataAccessInternal.getSubIterations(context, "SharedSheet@Shared"))
            .as("Row 4 doesn't exist in the Shared sheet either - subIteration 4 must be absent")
            .doesNotContain("4");

        // Row 4 doesn't exist in either sheet - both must signal EndOfDataSheet at the same point.
        DataNotFoundException projectEx = catchThrowableOfType(
            () -> DataAccess.getData(context, "ProjectSheet", "Value", "1", "4"),
            DataNotFoundException.class
        );
        DataNotFoundException sharedEx = catchThrowableOfType(
            () -> DataAccess.getData(context, "SharedSheet@Shared", "Value", "1", "4"),
            DataNotFoundException.class
        );

        assertThat(projectEx.cause.isEndData())
            .as("Project sheet row 4 must signal the plain EndOfDataSheet cause")
            .isTrue();
        assertThat(sharedEx.cause.isEndData())
            .as("Shared sheet row 4 must signal the plain EndOfDataSheet cause, just like Project")
            .isTrue();
    }
}
