package com.ing.engine.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ing.datalib.component.Project;
import com.ing.datalib.testdata.TestDataFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Regression coverage for AI-generated data-sheet rows (INGenie / AI Tools):
 * reusable scope must live in the Scope column rather than a "(R) "
 * scenario-name prefix, and repeated row-add calls for the same
 * scenario/testcase/iteration/scope must not create duplicate rows.
 */
public class DataSheetGenerationTest {
    private final ObjectMapper json = new ObjectMapper();
    private MCPTools tools;
    private Path tempDir;
    private File project;
    private String originalUserDir;

    @BeforeMethod
    public void setUp() throws Exception {
        // Must run before the first Project is ever constructed in this JVM - see
        // ProjectTestDataScopeConversionTest for the same pattern.
        TestDataFactory.load();
        tools = new MCPTools(null);
        tempDir = Files.createTempDirectory("ingenious-datasheet-test");
        // Shared Test Data resolves from user.dir - isolate it under the temp dir so
        // project.save() (triggered via the data_row_add tool) can't write a stray
        // Shared/SharedTestData folder into this module's basedir.
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
        project = tempDir.resolve("Proj").toFile();
        new Project(project.getAbsolutePath(), "csv").createProject();
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

    private JsonNode call(String name, ObjectNode args) {
        args.put("project", project.getAbsolutePath());
        ObjectNode params = json.createObjectNode();
        params.put("name", name);
        params.set("arguments", args);
        return tools.call(json, params).path("structuredContent");
    }

    private ObjectNode rowAddArgs(boolean reusable) {
        ObjectNode args = json.createObjectNode();
        args.put("sheet", "LoginData");
        args.put("scenario", "Login");
        args.put("testcase", "LoginTestCase");
        args.put("reusable", reusable);
        ObjectNode columns = args.putObject("columns");
        columns.put("Username", "alice");
        return args;
    }

    @Test
    public void reusableRowDoesNotGetRPrefixAndSetsScopeColumn() {
        JsonNode result = call("ingenious_data_row_add", rowAddArgs(true));
        assertThat(result.path("scenario").asText()).isEqualTo("Login");

        String csv = readSheet();
        assertThat(csv).doesNotContain("(R)");
        assertThat(csv).contains("[Project]");
    }

    @Test
    public void repeatedRowAddForSameKeyIsIdempotent() {
        call("ingenious_data_row_add", rowAddArgs(true));
        JsonNode second = call("ingenious_data_row_add", rowAddArgs(true));
        assertThat(second.path("duplicate").asBoolean()).isTrue();
        assertThat(countDataRows(readSheet())).isEqualTo(1);
    }

    @Test
    public void validateFlagsLegacyRPrefixAndDuplicateRows() throws Exception {
        // Simulate rows produced by the pre-fix behavior to prove the new lint rules catch them.
        File sheet = new File(project, "TestData/LoginData.csv");
        sheet.getParentFile().mkdirs();
        Files.write(
            sheet.toPath(),
            (
                "Scenario,Flow,Scope,Iteration,SubIteration,Username\n" +
                "(R) Login,LoginTestCase,,1,1,alice\n" +
                "Checkout,CheckoutTestCase,,1,1,bob\n" +
                "Checkout,CheckoutTestCase,,1,1,bob\n"
            ).getBytes()
        );
        JsonNode result = call("ingenious_testcase_validate", json.createObjectNode());
        String errors = result.path("errors").toString();
        String warnings = result.path("warnings").toString();
        assertThat(errors).contains("[E13]").contains("(R) Login");
        assertThat(warnings).contains("[W12]");
    }

    private String readSheet() {
        try {
            return new String(
                Files.readAllBytes(new File(project, "TestData/LoginData.csv").toPath())
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static int countDataRows(String csv) {
        String[] lines = csv.split("\n");
        int count = 0;
        for (int i = 1; i < lines.length; i++) {
            if (!lines[i].trim().isEmpty()) count++;
        }
        return count;
    }
}
