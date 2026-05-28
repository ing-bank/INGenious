package com.ing.storywriter.bdd.data;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for BDDProject — JSON-based BDD project persistence.
 */
public class BDDProjectTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("bddproject-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testCreateProject() {
        File projectFile = tempDir.resolve("project.json").toFile();
        BDDProject.create(projectFile, "TestProject", "A test description");

        assertThat(projectFile).exists();
    }

    @Test
    public void testCreateProjectContent() throws IOException {
        File projectFile = tempDir.resolve("project.json").toFile();
        BDDProject.create(projectFile, "MyProject", "My desc");

        String content = new String(Files.readAllBytes(projectFile.toPath()));
        assertThat(content).contains("MyProject");
        assertThat(content).contains("My desc");
    }

    @Test
    public void testLoadProject() {
        File projectFile = tempDir.resolve("load.json").toFile();
        BDDProject.create(projectFile, "LoadTest", "Load desc");

        BDDProject loaded = BDDProject.load(projectFile);

        assertThat(loaded).isNotNull();
        assertThat(loaded.getStories()).isNotNull();
    }

    @Test
    public void testLoadProjectHasNoStories() {
        File projectFile = tempDir.resolve("empty.json").toFile();
        BDDProject.create(projectFile, "EmptyProject", "No stories");

        BDDProject loaded = BDDProject.load(projectFile);

        assertThat(loaded.hasStories()).isFalse();
    }

    @Test
    public void testWriteAndReload() {
        File projectFile = tempDir.resolve("write.json").toFile();
        BDDProject.create(projectFile, "WriteTest", "Write desc");

        BDDProject loaded = BDDProject.load(projectFile);
        loaded.getStories().add(new Story("TestStory"));
        loaded.write();

        BDDProject reloaded = BDDProject.load(projectFile);
        assertThat(reloaded.hasStories()).isTrue();
        assertThat(reloaded.getStories()).hasSize(1);
        assertThat(reloaded.getStories().get(0).name).isEqualTo("TestStory");
    }

    @Test
    public void testGetProject() {
        File projectFile = tempDir.resolve("proj.json").toFile();
        BDDProject.create(projectFile, "ProjTest", "desc");

        BDDProject loaded = BDDProject.load(projectFile);
        var json = loaded.getProject();

        assertThat(json.get("name")).isEqualTo("ProjTest");
        assertThat(json.get("desc")).isEqualTo("desc");
        assertThat(json.get("data")).isNotNull();
    }

    @Test
    public void testMultipleStories() {
        File projectFile = tempDir.resolve("multi.json").toFile();
        BDDProject.create(projectFile, "MultiStory", "Multiple stories");

        BDDProject loaded = BDDProject.load(projectFile);
        loaded.getStories().add(new Story("Story1"));
        loaded.getStories().add(new Story("Story2"));
        loaded.getStories().add(new Story("Story3"));
        loaded.write();

        BDDProject reloaded = BDDProject.load(projectFile);
        assertThat(reloaded.getStories()).hasSize(3);
    }
}
