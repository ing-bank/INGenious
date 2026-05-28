package com.ing.storywriter.bdd.data;

import static org.assertj.core.api.Assertions.assertThat;
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
 * Tests for StoryParser — reads feature files into Story objects.
 */
public class StoryParserTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("storyparser-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testParseFeatureFile() throws Exception {
        File feature = tempDir.resolve("test.feature").toFile();
        try (FileWriter fw = new FileWriter(feature)) {
            fw.write("Feature: Login\n");
            fw.write("  Scenario: Valid login\n");
            fw.write("    Given user is on login page\n");
        }

        StoryParser parser = new StoryParser(feature);
        List<Story> stories = parser.stories();

        assertThat(stories).hasSize(1);
        // Story name gets updated from feature data via update()
        assertThat(stories.get(0).name).isEqualTo("Login");
    }

    @Test
    public void testParsedStoryContainsData() throws Exception {
        File feature = tempDir.resolve("test.feature").toFile();
        String content = "Feature: Search\nScenario: Basic search\nGiven search page is open";
        try (FileWriter fw = new FileWriter(feature)) {
            fw.write(content);
        }

        StoryParser parser = new StoryParser(feature);
        Story story = parser.stories().get(0);

        assertThat(story.getData()).contains("Feature: Search");
    }

    @Test
    public void testParseEmptyFeatureFile() throws Exception {
        File feature = tempDir.resolve("empty.feature").toFile();
        try (FileWriter fw = new FileWriter(feature)) {
            fw.write("Feature: Empty\n");
        }

        StoryParser parser = new StoryParser(feature);
        assertThat(parser.stories()).hasSize(1);
    }

    @Test
    public void testParseMultiLineFeature() throws Exception {
        File feature = tempDir.resolve("multi.feature").toFile();
        StringBuilder sb = new StringBuilder();
        sb.append("Feature: Multi Scenario\n");
        sb.append("  Scenario: First\n");
        sb.append("    Given step 1\n");
        sb.append("    When step 2\n");
        sb.append("    Then step 3\n");
        sb.append("\n");
        sb.append("  Scenario: Second\n");
        sb.append("    Given step A\n");
        sb.append("    Then step B\n");

        try (FileWriter fw = new FileWriter(feature)) {
            fw.write(sb.toString());
        }

        StoryParser parser = new StoryParser(feature);
        Story story = parser.stories().get(0);

        assertThat(story.getData()).contains("Scenario: First");
        assertThat(story.getData()).contains("Scenario: Second");
    }
}
