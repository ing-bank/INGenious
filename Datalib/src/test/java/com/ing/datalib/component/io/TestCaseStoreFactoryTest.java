package com.ing.datalib.component.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestCaseStoreFactoryTest {
    private Path tempDir;

    @BeforeMethod
    public void setUp() throws Exception {
        tempDir = Files.createTempDirectory("store-factory-");
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
    public void yamlBeatsCsvOnSameBaseName() throws Exception {
        File dir = tempDir.toFile();
        Files.write(new File(dir, "Same.csv").toPath(), "x".getBytes());
        Files.write(new File(dir, "Same.yaml").toPath(), "x".getBytes());
        Files.write(new File(dir, "OnlyCsv.csv").toPath(), "x".getBytes());
        Files.write(new File(dir, "OnlyYaml.yaml").toPath(), "x".getBytes());

        Map<String, File> files = TestCaseStoreFactory.listLogicalFiles(dir);
        assertThat(files.keySet()).containsExactlyInAnyOrder("Same", "OnlyCsv", "OnlyYaml");
        assertThat(files.get("Same").getName()).isEqualTo("Same.yaml");
    }

    @Test
    public void resolveFormatPrefersExistingThenDefault() throws Exception {
        File dir = tempDir.toFile();
        assertThat(TestCaseStoreFactory.resolveFormat(dir, "Missing", TestCaseFormat.YAML))
            .isEqualTo(TestCaseFormat.YAML);
        assertThat(TestCaseStoreFactory.resolveFormat(dir, "Missing", null))
            .isEqualTo(TestCaseFormat.CSV);

        Files.write(new File(dir, "ExistingCsv.csv").toPath(), "x".getBytes());
        assertThat(TestCaseStoreFactory.resolveFormat(dir, "ExistingCsv", TestCaseFormat.YAML))
            .isEqualTo(TestCaseFormat.CSV);
    }

    @Test
    public void stripExtensionHandlesAllKnownSuffixes() {
        assertThat(TestCaseFormat.stripExtension("a.csv")).isEqualTo("a");
        assertThat(TestCaseFormat.stripExtension("a.yaml")).isEqualTo("a");
        assertThat(TestCaseFormat.stripExtension("a.yml")).isEqualTo("a");
        assertThat(TestCaseFormat.stripExtension("a.YAML")).isEqualTo("a");
        assertThat(TestCaseFormat.stripExtension("plain")).isEqualTo("plain");
    }
}
