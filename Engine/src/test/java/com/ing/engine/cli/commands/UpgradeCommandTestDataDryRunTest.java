package com.ing.engine.cli.commands;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.datalib.testdata.TestDataFactory;
import com.ing.engine.cli.INGeniousCLI;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import picocli.CommandLine;

/**
 * Regression test for the {@code ingenious project upgrade --dry-run} bug where the Test Data
 * Scope migration step ignored {@code --dry-run} entirely: it always constructed a
 * {@code Project}, and since project construction is what performs and persists that
 * migration (not a separate, explicitly-triggered save), the CSV file was rewritten on disk
 * even when the user only asked for a preview.
 */
public class UpgradeCommandTestDataDryRunTest {
    private Path tempDir;
    private File csvFile;

    @BeforeMethod
    public void setUp() throws IOException {
        // The real CLI entry point (com.ing.engine.core.Control, invoked by the ingenious/
        // ingenious.bat launcher) calls this once at startup to register CsvDataProvider.
        // Invoking UpgradeCommand directly, as this test does, bypasses that bootstrap, so it
        // must be replicated here - otherwise TestDataFactory.get() finds no "csv" provider.
        TestDataFactory.load();
        tempDir = Files.createTempDirectory("upgrade-command-dryrun-test");
        File testDataDir = new File(tempDir.toFile(), "TestData");
        testDataDir.mkdirs();
        csvFile = new File(testDataDir, "Legacy.csv");
        try (java.io.FileWriter fw = new java.io.FileWriter(csvFile)) {
            fw.write("Scenario,Flow,Iteration,SubIteration,User\n");
            fw.write("Checkout,Pay,1,1,legacy-user\n");
        }
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files
            .walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
    }

    @Test
    public void dryRun_DoesNotWriteScopeMigrationToDisk() throws IOException {
        String before = Files.readString(csvFile.toPath());

        int exitCode = new CommandLine(new INGeniousCLI())
        .execute("upgrade", tempDir.toAbsolutePath().toString(), "--dry-run", "--yes");

        assertThat(exitCode).isEqualTo(0);
        assertThat(Files.readString(csvFile.toPath())).isEqualTo(before);
        assertThat(before).doesNotContain("Scope");
    }

    @Test
    public void realRun_DoesWriteScopeMigrationToDisk() throws IOException {
        int exitCode = new CommandLine(new INGeniousCLI())
        .execute("upgrade", tempDir.toAbsolutePath().toString(), "--yes");

        assertThat(exitCode).isEqualTo(0);
        String after = Files.readString(csvFile.toPath());
        assertThat(after).contains("Scope");
        assertThat(after).contains("Checkout,Pay,,1,1,legacy-user");
    }
}
