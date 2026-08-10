package com.ing.datalib.component.io;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.component.ExecutionStep.HEADERS;
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

public class YamlTestSetStoreTest {
    private Path tempDir;
    private final YamlTestSetStore yaml = new YamlTestSetStore();
    private final CsvTestSetStore csv = new CsvTestSetStore();

    @BeforeMethod
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("yaml-ts-test");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (tempDir != null) {
            Files
                .walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    @Test
    public void roundTripPreservesAllRows() throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(
            execRow("true", "Payments", "Pay", "Single", "NoRun", "Chromium", "Default", "Any")
        );
        rows.add(
            execRow(
                "false",
                "Login",
                "ValidUser",
                "Single",
                "NoRun",
                "Firefox",
                "Default",
                "Windows"
            )
        );

        File file = new File(tempDir.toFile(), "Set1.yaml");
        yaml.save(file, "Set1", "Release1", rows);

        List<List<String>> loaded = yaml.load(file);
        assertThat(loaded).hasSize(2);
        assertThat(loaded.get(0).get(HEADERS.TestScenario.getIndex())).isEqualTo("Payments");
        assertThat(loaded.get(1).get(HEADERS.Execute.getIndex())).isEqualTo("false");
        assertThat(loaded.get(1).get(HEADERS.Platform.getIndex())).isEqualTo("Windows");
    }

    @Test
    public void csvAndYamlYieldEquivalentRows() throws Exception {
        List<List<String>> rows = new ArrayList<>();
        rows.add(execRow("true", "Scn", "TC", "Single", "NoRun", "Chromium", "Default", "Any"));

        File csvFile = new File(tempDir.toFile(), "x.csv");
        File yamlFile = new File(tempDir.toFile(), "x.yaml");
        csv.save(csvFile, "x", "Rel", rows);
        yaml.save(yamlFile, "x", "Rel", rows);

        List<List<String>> fromCsv = csv.load(csvFile);
        List<List<String>> fromYaml = yaml.load(yamlFile);
        assertThat(fromYaml).isEqualTo(fromCsv);
    }

    private static List<String> execRow(
        String execute,
        String scenario,
        String testCase,
        String iteration,
        String status,
        String browser,
        String browserVersion,
        String platform
    ) {
        return new ArrayList<>(
            Arrays.asList(
                execute,
                scenario,
                testCase,
                iteration,
                status,
                browser,
                browserVersion,
                platform
            )
        );
    }
}
