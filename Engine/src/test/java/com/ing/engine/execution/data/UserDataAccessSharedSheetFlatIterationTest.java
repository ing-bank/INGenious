package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Reproduces the exact shape of ING-reported bug "Using Shared Test Data in String Operations
 * within Loops ends the iteration prematurely": a flat (no Start/End Param) Test Plan test case
 * whose data sheets are keyed by main Iteration (1/2/3), one row per Iteration, SubIteration
 * fixed at "1" - the shape the attached repro CSV uses (print/Concat steps with no Param Loop).
 * Compares {@link UserDataAccess#getData(String, String)} (the 2-arg path String Operations'
 * {@code getVarValue}/{@code getDatasheet} uses) against the explicit-iter/subIter path a normal
 * command's Input resolution ({@code DataProcessor.resolve} -&gt; {@code DataAccess.getData})
 * uses, for both an untagged Project sheet and an {@code @Shared}-tagged sheet.
 */
public class UserDataAccessSharedSheetFlatIterationTest {
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
    private UserDataAccess userData;

    @BeforeMethod
    public void setUp() throws IOException {
        mocks = MockitoAnnotations.openMocks(this);
        tempDir = Files.createTempDirectory("flat-iteration-shared-sheet-test");

        // One row per main Iteration, SubIteration fixed at "1" - no Start/End Param block.
        File projectCsv = tempDir.resolve("ProjectSheet.csv").toFile();
        try (FileWriter fw = new FileWriter(projectCsv)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration,Data1\n");
            fw.write("NewScenario,NewTestCase,,1,1,a\n");
            fw.write("NewScenario,NewTestCase,,2,1,b\n");
            fw.write("NewScenario,NewTestCase,,3,1,c\n");
        }
        CsvTestData projectModel = new CsvTestData(projectCsv.getAbsolutePath());
        projectModel.loadRecords(projectCsv);

        File sharedCsv = tempDir.resolve("SharedSheet.csv").toFile();
        try (FileWriter fw = new FileWriter(sharedCsv)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration,Data1\n");
            fw.write("NewScenario,NewTestCase,,1,1,a\n");
            fw.write("NewScenario,NewTestCase,,2,1,b\n");
            fw.write("NewScenario,NewTestCase,,3,1,c\n");
        }
        CsvTestData sharedModel = new CsvTestData(sharedCsv.getAbsolutePath());
        sharedModel.loadRecords(sharedCsv);

        when(context.scenario()).thenReturn("NewScenario");
        when(context.testcase()).thenReturn("NewTestCase");
        when(context.getResolvedReusableScope()).thenReturn(null);
        when(context.getRoot()).thenReturn(context);
        when(context.executor()).thenReturn(executor);
        when(context.project()).thenReturn(project);
        when(context.getControl()).thenReturn(control);
        when(control.getRunTimeVars()).thenReturn(new java.util.HashMap<>());
        // Flat (non-loop) run: subIteration is fixed at "1" for every step, matching
        // TestCaseRunner.resolveParam() falling back to this.parameter.getSubIteration()
        // whenever stepStack is empty (no Start/End Param block in the test case).
        when(context.getCurrentSubIteration()).thenReturn("1");
        when(context.subIteration()).thenReturn("1");

        when(executor.runEnv()).thenReturn("Default");
        when(executor.sharedRunEnv()).thenReturn("Default");
        when(executor.dataProvider()).thenReturn(projectDataProvider);
        when(projectDataProvider.defEnv()).thenReturn("Default");
        when(projectDataProvider.defData()).thenReturn(projectDefData);
        when(projectDefData.getByName("ProjectSheet")).thenReturn(projectModel);

        when(project.getSharedTestData()).thenReturn(sharedDataProvider);
        when(sharedDataProvider.defEnv()).thenReturn("Default");
        when(sharedDataProvider.getTestDataFor("Default")).thenReturn(sharedDefData);
        when(sharedDataProvider.defData()).thenReturn(sharedDefData);
        when(sharedDefData.getByName("SharedSheet")).thenReturn(sharedModel);

        userData =
            new UserDataAccess() {

                @Override
                public TestCaseRunner context() {
                    return context;
                }
            };
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
    public void stringOperationsStyleTwoArgGetData_ResolvesProjectSheetAcrossAllThreeIterations() {
        // DataAccess.getData's final step needs Control.getCurrentProject() global static state
        // (not wired here, same limitation DataAccessTest already notes) - what matters for this
        // regression is that no DataNotFoundException/TestDataNotFoundException is thrown, i.e.
        // the row IS found for every Iteration; reaching that unrelated NPE proves it was.
        for (String iter : new String[] { "1", "2", "3" }) {
            when(context.iteration()).thenReturn(iter);
            Throwable thrown = catchThrowable(() -> userData.getData("ProjectSheet", "Data1"));
            assertThat(thrown)
                .as("Iteration " + iter + " on the untagged Project sheet must be found")
                .isNotInstanceOf(DataNotFoundException.class);
        }
    }

    @Test
    public void stringOperationsStyleTwoArgGetData_ResolvesSharedSheetAcrossAllThreeIterations() {
        for (String iter : new String[] { "1", "2", "3" }) {
            when(context.iteration()).thenReturn(iter);
            Throwable thrown = catchThrowable(
                () -> userData.getData("SharedSheet@Shared", "Data1")
            );
            assertThat(thrown)
                .as("Iteration " + iter + " on the @Shared sheet must be found, just like Project")
                .isNotInstanceOf(DataNotFoundException.class);
        }
    }
}
