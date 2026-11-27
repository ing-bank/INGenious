package com.ing.engine.reporting;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ConsoleEmbedderTest {
    private static final String PLACEHOLDER = "__INGENIOUS_CONSOLE_DATA__";
    private static final String PAGE =
        "<script type=\"text/plain\" id=\"ing-console-data\">" + PLACEHOLDER + "</script>";

    private Path run;

    @BeforeMethod
    public void createRunFolder() throws IOException {
        run = Files.createTempDirectory("console-embedder-run");
        Files.writeString(run.resolve("console.txt"), "run output", StandardCharsets.UTF_8);
    }

    @Test
    public void embedsIntoOverviewPages() throws IOException {
        Path summary = page("summary-v2.html");
        Path detailed = page("detailed.html");

        ConsoleEmbedder.embedInto(run.toFile());

        assertThat(summary).content().contains("run output").doesNotContain(PLACEHOLDER);
        assertThat(detailed).content().contains("run output").doesNotContain(PLACEHOLDER);
    }

    @Test
    public void leavesTestCasePagesUntouched() throws IOException {
        Path testCase = page("Scenario_Case_Single_Chromium-v2.html");

        ConsoleEmbedder.embedInto(run.toFile());

        assertThat(testCase).content().isEqualTo(PAGE);
    }

    private Path page(String name) throws IOException {
        return Files.writeString(run.resolve(name), PAGE, StandardCharsets.UTF_8);
    }
}
