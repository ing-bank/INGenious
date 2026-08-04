package com.ing.testdata.csv;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
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
 * Regression tests for Scope resolution on project (re)load, covering the reload bug where
 * duplicate Scenario+TestCase names across Test Plan / Project Reusables / Shared Reusables
 * caused the Scope column to be recomputed and overwritten on every load.
 */
public class CsvDataProviderTest {
    private Path tempDir;
    private Project project;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("csv-data-provider-test");
        project = mock(Project.class);
        when(project.getLocation()).thenReturn(tempDir.toString());
        when(project.getScenarioByName(anyString())).thenReturn(null);
        when(project.getReusableScenarioByName(anyString())).thenReturn(null);
        when(project.getSharedReusableScenarioByName(anyString())).thenReturn(null);
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files
            .walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    private Scenario scenarioWithTestCase(String testCaseName) {
        Scenario scenario = mock(Scenario.class);
        TestCase testCase = mock(TestCase.class);
        when(scenario.getTestCaseByName(testCaseName)).thenReturn(testCase);
        return scenario;
    }

    private File testDataDir() {
        File dir = new File(tempDir.toFile(), "TestData");
        dir.mkdirs();
        return dir;
    }

    private List<String> readLines(File csvFile) throws IOException {
        return Files.readAllLines(csvFile.toPath());
    }

    @Test
    public void reload_PreservesExistingScope_EvenWhenNameAlsoExistsInOtherScopes()
        throws IOException {
        // Same Scenario+TestCase name ("Login"/"Step1") exists in ALL THREE scopes - a genuine
        // collision. Each row already has its own explicitly-set, distinct Scope value.
        Scenario testPlanLogin = scenarioWithTestCase("Step1");
        Scenario projectLogin = scenarioWithTestCase("Step1");
        Scenario sharedLogin = scenarioWithTestCase("Step1");
        when(project.getScenarioByName("Login")).thenReturn(testPlanLogin);
        when(project.getReusableScenarioByName("Login")).thenReturn(projectLogin);
        when(project.getSharedReusableScenarioByName("Login")).thenReturn(sharedLogin);

        File csvFile = new File(testDataDir(), "LoginData.csv");
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration,User\n");
            fw.write("Login,Step1,,1,1,testplan-user\n");
            fw.write("Login,Step1,[Project],1,1,project-user\n");
            fw.write("Login,Step1,[Shared],1,1,shared-user\n");
        }

        CsvDataProvider provider = new CsvDataProvider(project, "Default");
        provider.load();
        provider.load(); // second reload - simulates closing and reopening the project again

        List<String> lines = readLines(csvFile);
        assertThat(lines.get(1)).startsWith("Login,Step1,,1,1,testplan-user");
        assertThat(lines.get(2)).startsWith("Login,Step1,[Project],1,1,project-user");
        assertThat(lines.get(3)).startsWith("Login,Step1,[Shared],1,1,shared-user");
    }

    @Test
    public void reload_UniqueNames_AreUnaffected() throws IOException {
        Scenario checkout = scenarioWithTestCase("Pay");
        when(project.getScenarioByName("Checkout")).thenReturn(checkout);

        File csvFile = new File(testDataDir(), "CheckoutData.csv");
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("Scenario,Flow,Scope,Iteration,SubIteration,Amount\n");
            fw.write("Checkout,Pay,,1,1,100\n");
        }

        CsvDataProvider provider = new CsvDataProvider(project, "Default");
        provider.load();

        List<String> lines = readLines(csvFile);
        assertThat(lines.get(1)).startsWith("Checkout,Pay,,1,1,100");
    }

    @Test
    public void legacyFile_UniqueMatch_AutoPopulatesScopeOnFirstLoad() throws IOException {
        // Old 4-column format (no Scope column at all) - genuinely unmigrated data.
        // "Login"/"Step1" exists ONLY in Shared Reusables, so the match is unambiguous.
        Scenario sharedLogin = scenarioWithTestCase("Step1");
        when(project.getSharedReusableScenarioByName("Login")).thenReturn(sharedLogin);

        File csvFile = new File(testDataDir(), "LegacyLoginData.csv");
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("Scenario,Flow,Iteration,SubIteration,User\n");
            fw.write("Login,Step1,1,1,shared-user\n");
        }

        CsvDataProvider provider = new CsvDataProvider(project, "Default");
        provider.load();

        List<String> lines = readLines(csvFile);
        assertThat(lines.get(0)).isEqualTo("Scenario,Flow,Scope,Iteration,SubIteration,User");
        assertThat(lines.get(1)).isEqualTo("Login,Step1,[Shared],1,1,shared-user");
    }

    @Test
    public void legacyFile_AmbiguousMatch_LeavesScopeUnsetInsteadOfDefaultingToProject()
        throws IOException {
        // Old 4-column format. "Login"/"Step1" collides across Project AND Shared Reusables -
        // genuinely ambiguous, must NOT silently default to "[Project]".
        Scenario projectLogin = scenarioWithTestCase("Step1");
        Scenario sharedLogin = scenarioWithTestCase("Step1");
        when(project.getReusableScenarioByName("Login")).thenReturn(projectLogin);
        when(project.getSharedReusableScenarioByName("Login")).thenReturn(sharedLogin);

        File csvFile = new File(testDataDir(), "AmbiguousLoginData.csv");
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write("Scenario,Flow,Iteration,SubIteration,User\n");
            fw.write("Login,Step1,1,1,some-user\n");
        }

        CsvDataProvider provider = new CsvDataProvider(project, "Default");
        provider.load();

        List<String> lines = readLines(csvFile);
        assertThat(lines.get(0)).isEqualTo("Scenario,Flow,Scope,Iteration,SubIteration,User");
        assertThat(lines.get(1)).isEqualTo("Login,Step1,,1,1,some-user");
    }
}
