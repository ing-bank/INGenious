package com.ing.engine.execution.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.ing.datalib.component.ReusableRef;
import com.ing.testdata.csv.CsvTestData;
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
 * End-to-end regression test for scope-aware test data resolution: the same Scenario/TestCase
 * name ("Login"/"Step1") exists in both the Project and Shared reusable scopes, each with its
 * own distinct iteration/sub-iteration/data. A standalone run of either reusable must only ever
 * see its own scope's rows, never the other scope's - exercises the real DataAccessInternal
 * methods (getIter/getSubIter) rather than mocking them away, unlike DataAccessScopeTest.
 */
public class DataAccessScopeIntegrationTest {
    private Path tempDir;
    private CsvTestData sharedNameData;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("data-access-scope-test");
        File csvFile = tempDir.resolve("LoginData.csv").toFile();
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration,Username\n");
            fw.write("Login,Step1,[Project],1,1,proj-user-1\n");
            fw.write("Login,Step1,[Project],1,2,proj-user-2\n");
            fw.write("Login,Step1,[Shared],1,1,shared-user-1\n");
        }
        sharedNameData = new CsvTestData(csvFile.getAbsolutePath());
        sharedNameData.loadRecords(csvFile);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files
            .walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    private com.ing.engine.execution.run.TestCaseRunner mockStandaloneReusable(
        ReusableRef.Scope scope
    ) {
        com.ing.engine.execution.run.TestCaseRunner context = mock(
            com.ing.engine.execution.run.TestCaseRunner.class
        );
        when(context.scenario()).thenReturn("Login");
        when(context.testcase()).thenReturn("Step1");
        when(context.iteration()).thenReturn("1");
        when(context.getResolvedReusableScope()).thenReturn(scope);
        // Standalone run: this reusable IS its own root.
        when(context.getRoot()).thenReturn(context);
        return context;
    }

    @Test
    public void testGetIter_StandaloneProjectReusable_OnlySeesProjectScope() {
        com.ing.engine.execution.run.TestCaseRunner context = mockStandaloneReusable(
            ReusableRef.Scope.PROJECT
        );

        Set<String> subIters = DataAccessInternal.getSubIter(context, sharedNameData);

        assertThat(subIters)
            .as("Standalone [Project] reusable must only see [Project]-scoped sub-iterations")
            .containsExactlyInAnyOrder("1", "2");
    }

    @Test
    public void testGetIter_StandaloneSharedReusable_OnlySeesSharedScope() {
        com.ing.engine.execution.run.TestCaseRunner context = mockStandaloneReusable(
            ReusableRef.Scope.SHARED
        );

        Set<String> subIters = DataAccessInternal.getSubIter(context, sharedNameData);

        assertThat(subIters)
            .as("Standalone [Shared] reusable must only see [Shared]-scoped sub-iterations")
            .containsExactly("1");
    }

    @Test
    public void testGetIter_NestedProjectReusableInsideRegularTestCase_OnlySeesProjectScope() {
        // Root: a normal Test Plan test case (empty scope) calling the [Project] "Login:Step1"
        // reusable via a nested Execute step - root and nested context are DIFFERENT instances.
        com.ing.engine.execution.run.TestCaseRunner rootContext = mock(
            com.ing.engine.execution.run.TestCaseRunner.class
        );
        when(rootContext.scenario()).thenReturn("MainFlow");
        when(rootContext.testcase()).thenReturn("RunLogin");
        when(rootContext.getResolvedReusableScope()).thenReturn(null);
        when(rootContext.getRoot()).thenReturn(rootContext);

        com.ing.engine.execution.run.TestCaseRunner nestedContext = mock(
            com.ing.engine.execution.run.TestCaseRunner.class
        );
        when(nestedContext.scenario()).thenReturn("Login");
        when(nestedContext.testcase()).thenReturn("Step1");
        when(nestedContext.iteration()).thenReturn("1");
        when(nestedContext.getResolvedReusableScope()).thenReturn(ReusableRef.Scope.PROJECT);
        when(nestedContext.getRoot()).thenReturn(rootContext);

        Set<String> subIters = DataAccessInternal.getSubIter(nestedContext, sharedNameData);

        assertThat(subIters)
            .as(
                "Nested [Project] reusable inside a regular test case must only see its own " +
                "[Project]-scoped sub-iterations, not [Shared]'s"
            )
            .containsExactlyInAnyOrder("1", "2");
    }

    @Test
    public void testGetDataFromModelWithScope_ResolvesCorrectRowPerScope() {
        String projectVal = DataAccessInternal.getDataFromModelWithScope(
            sharedNameData,
            "Username",
            "Login",
            "Step1",
            "1",
            "1",
            "[Project]"
        );
        String sharedVal = DataAccessInternal.getDataFromModelWithScope(
            sharedNameData,
            "Username",
            "Login",
            "Step1",
            "1",
            "1",
            "[Shared]"
        );

        assertThat(projectVal).isEqualTo("proj-user-1");
        assertThat(sharedVal).isEqualTo("shared-user-1");
    }

    /**
     * Regression test: writing to the [Project] row for Login/Step1/1/1 must not also update
     * (or instead update) the [Shared] row sharing the exact same scenario/testcase/iteration/
     * sub-iteration - the two scopes' rows are otherwise indistinguishable by those keys alone.
     */
    @Test
    public void testPutDataToModel_DoesNotCrossContaminateOtherScopeRow() {
        boolean updated = DataAccessInternal.putDataToModel(
            sharedNameData,
            "Username",
            "proj-user-1-UPDATED",
            "Login",
            "Step1",
            "1",
            "1",
            "[Project]"
        );
        assertThat(updated).isTrue();

        String projectVal = DataAccessInternal.getDataFromModelWithScope(
            sharedNameData,
            "Username",
            "Login",
            "Step1",
            "1",
            "1",
            "[Project]"
        );
        String sharedVal = DataAccessInternal.getDataFromModelWithScope(
            sharedNameData,
            "Username",
            "Login",
            "Step1",
            "1",
            "1",
            "[Shared]"
        );

        assertThat(projectVal)
            .as("The [Project] row itself should reflect the update")
            .isEqualTo("proj-user-1-UPDATED");
        assertThat(sharedVal)
            .as("The [Shared] row sharing the same scn/tc/iter/subIter must be untouched")
            .isEqualTo("shared-user-1");
    }
}
