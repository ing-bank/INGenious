package com.ing.datalib.component.io;

import com.ing.datalib.component.TestStep.HEADERS;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Round-trip parity tests for {@link YamlTestCaseStore} versus
 * {@link CsvTestCaseStore}.
 */
public class YamlTestCaseStoreTest {

    private Path tempDir;
    private final YamlTestCaseStore yaml = new YamlTestCaseStore();
    private final CsvTestCaseStore csv = new CsvTestCaseStore();

    @BeforeMethod
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("yaml-tc-test");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (tempDir != null) {
            Files.walk(tempDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    public void roundTripPreservesAllFields() throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(row("1", "Execute", "Given user is logged in", "StepDefinitions:User is logged in", "", "", ""));
        rows.add(row("2", "Username", "Enter the value [<Data>] in the Field [<Object>]", "Fill", "%user%", "", "Login"));
        rows.add(row("3", "signin-submit", "Click the [<Object>]", "Click", "", "", "Login"));

        File file = new File(tempDir.toFile(), "MyTest.yaml");
        yaml.save(file, "MyTest", "Login", false, null, rows);

        List<List<String>> loaded = yaml.load(file);
        assertThat(loaded).hasSize(3);
        assertThat(loaded.get(0).get(HEADERS.ObjectName.getIndex())).isEqualTo("Execute");
        assertThat(loaded.get(1).get(HEADERS.Input.getIndex())).isEqualTo("%user%");
        assertThat(loaded.get(2).get(HEADERS.Reference.getIndex())).isEqualTo("Login");
    }

    @Test
    public void roundTripPreservesBreakpointAndCommentMarkers() throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(row("*1", "Username", "desc", "Fill", "u", "", "Login"));   // breakpoint
        rows.add(row("//2", "Password", "desc", "Fill", "p", "", "Login"));  // commented
        rows.add(row("//*3", "Submit", "desc", "Click", "", "", "Login"));   // commented + breakpoint

        File file = new File(tempDir.toFile(), "Markers.yaml");
        yaml.save(file, "Markers", "Login", false, null, rows);

        // YAML output should contain explicit booleans, not `*` / `//` prefixes.
        String body = new String(Files.readAllBytes(file.toPath()));
        assertThat(body).contains("breakpoint: true");
        assertThat(body).contains("comment: true");
        assertThat(body).doesNotContain("\"*1\"");

        List<List<String>> loaded = yaml.load(file);
        assertThat(loaded.get(0).get(HEADERS.Step.getIndex())).isEqualTo("*1");
        assertThat(loaded.get(1).get(HEADERS.Step.getIndex())).isEqualTo("//2");
        assertThat(loaded.get(2).get(HEADERS.Step.getIndex())).isEqualTo("//*3");
    }

    @Test
    public void csvAndYamlYieldEquivalentRows() throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(row("1", "Execute", "given", "Scenario:Reusable", "", "", ""));
        rows.add(row("2", "Field", "fill", "Fill", "value", "if X", "Page"));

        File csvFile = new File(tempDir.toFile(), "x.csv");
        File yamlFile = new File(tempDir.toFile(), "x.yaml");
        csv.save(csvFile, "x", "Scn", false, null, rows);
        yaml.save(yamlFile, "x", "Scn", false, null, rows);

        List<List<String>> fromCsv = csv.load(csvFile);
        List<List<String>> fromYaml = yaml.load(yamlFile);

        assertThat(fromYaml).hasSameSizeAs(fromCsv);
        for (int i = 0; i < fromCsv.size(); i++) {
            assertThat(fromYaml.get(i)).isEqualTo(fromCsv.get(i));
        }
    }

    @Test
    public void emptyOptionalFieldsAreOmittedFromYaml() throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(row("1", "Execute", "", "Scenario:Reusable", "", "", ""));

        File file = new File(tempDir.toFile(), "Compact.yaml");
        yaml.save(file, "Compact", "Scn", false, null, rows);

        String body = new String(Files.readAllBytes(file.toPath()));
        assertThat(body).doesNotContain("input:");
        assertThat(body).doesNotContain("condition:");
        assertThat(body).doesNotContain("reference:");
        assertThat(body).doesNotContain("description:");
    }

    @Test
    public void schemaVersionIsEmittedAndPreserved() throws Exception {
        File file = new File(tempDir.toFile(), "Schema.yaml");
        yaml.save(file, "Schema", "Scn", false, null, new ArrayList<>());
        String body = new String(Files.readAllBytes(file.toPath()));
        assertThat(body).contains("schemaVersion: 1");
    }

    @Test
    public void testCaseKeyIsUsedForRegularTestCase() throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(row("1", "Browser", "open", "Open", "url", "", ""));
        File file = new File(tempDir.toFile(), "TC1.yaml");
        yaml.save(file, "TC1", "SC1", false, null, rows);
        String body = new String(Files.readAllBytes(file.toPath()));
        assertThat(body).contains("testCase: TC1");
        assertThat(body).doesNotContain("reusable:");
        assertThat(body).doesNotContain("name: TC1");
    }

    @Test
    public void reusableKeyIsUsedForReusableComponent() throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(row("1", "Browser", "open", "Open", "url", "", ""));
        File file = new File(tempDir.toFile(), "Reusableflow1.yaml");
        yaml.save(file, "Reusableflow1", "Reusable1", true, null, rows);
        String body = new String(Files.readAllBytes(file.toPath()));
        assertThat(body).contains("reusable: Reusableflow1");
        assertThat(body).doesNotContain("testCase:");
    }

    @Test
    public void tagsArePersistedWhenProvided() throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(row("1", "Browser", "open", "Open", "url", "", ""));
        File file = new File(tempDir.toFile(), "Tagged.yaml");
        yaml.save(file, "Tagged", "SC1", false, Arrays.asList("@smoke", "@regression"), rows);
        String body = new String(Files.readAllBytes(file.toPath()));
        assertThat(body).contains("tags:");
        assertThat(body).contains("@smoke");
        assertThat(body).contains("@regression");
    }

    @Test
    public void blankLineSeparatesEachStep() throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(row("1", "A", "", "Open", "", "", ""));
        rows.add(row("2", "B", "", "Click", "", "", ""));
        rows.add(row("3", "C", "", "Type", "", "", ""));
        File file = new File(tempDir.toFile(), "Spaced.yaml");
        yaml.save(file, "Spaced", "SC1", false, null, rows);
        String body = new String(Files.readAllBytes(file.toPath()));
        // Exactly two blank-line separators (between 1-2 and 2-3), none before the first step.
        long blankBeforeStep = body.lines()
                .reduce("", (prev, line) -> {
                    return prev + (line.equals("  - step: 1") || line.equals("  - step: 2") || line.equals("  - step: 3")
                            ? "|" + line : line);
                }).chars().filter(c -> c == '|').count();
        assertThat(body).contains("\n\n  - step: 2");
        assertThat(body).contains("\n\n  - step: 3");
        assertThat(body).doesNotContain("\n\n  - step: 1");
        assertThat(blankBeforeStep).isEqualTo(3);
    }

    @Test
    public void legacyReusableBooleanIsAcceptedOnLoad() throws Exception {
        File file = new File(tempDir.toFile(), "Legacy.yaml");
        String legacy = "schemaVersion: 1\n"
                + "name: Old\n"
                + "scenario: Scn\n"
                + "reusable: true\n"
                + "steps:\n"
                + "  - step: 1\n"
                + "    object: A\n"
                + "    action: Open\n";
        Files.write(file.toPath(), legacy.getBytes());
        List<List<String>> loaded = yaml.load(file);
        assertThat(loaded).hasSize(1);
        assertThat(loaded.get(0).get(HEADERS.ObjectName.getIndex())).isEqualTo("A");
    }

    private static List<String> row(String step, String object, String description,
                                    String action, String input, String condition, String reference) {
        return new ArrayList<>(Arrays.asList(step, object, description, action, input, condition, reference));
    }
}
