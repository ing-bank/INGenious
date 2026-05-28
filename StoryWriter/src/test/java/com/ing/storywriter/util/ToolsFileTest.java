package com.ing.storywriter.util;

import static org.assertj.core.api.Assertions.assertThat;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for Tools — file read/write operations.
 */
public class ToolsFileTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("toolsfile-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testWriteFile() {
        File f = tempDir.resolve("output.txt").toFile();
        Tools.writeFile(f, "Hello World");
        assertThat(f).exists();
    }

    @Test
    public void testWriteAndReadFile() throws Exception {
        File f = tempDir.resolve("readwrite.txt").toFile();
        String content = "Test content with special chars: <>&\"'";
        Tools.writeFile(f, content);

        String read = Tools.readFile(f);
        assertThat(read).isEqualTo(content);
    }

    @Test
    public void testWriteOverwritesExisting() throws Exception {
        File f = tempDir.resolve("overwrite.txt").toFile();
        Tools.writeFile(f, "First");
        Tools.writeFile(f, "Second");

        String read = Tools.readFile(f);
        assertThat(read).isEqualTo("Second");
    }

    @Test
    public void testWriteMultiLineContent() throws Exception {
        File f = tempDir.resolve("multiline.txt").toFile();
        String content = "Line 1\nLine 2\nLine 3";
        Tools.writeFile(f, content);

        String read = Tools.readFile(f);
        assertThat(read).contains("Line 1");
        assertThat(read).contains("Line 3");
    }

    @Test
    public void testWriteEmptyContent() throws Exception {
        File f = tempDir.resolve("empty.txt").toFile();
        Tools.writeFile(f, "");
        assertThat(f).exists();
        assertThat(f.length()).isEqualTo(0);
    }

    @Test
    public void testWriteJsonContent() throws Exception {
        File f = tempDir.resolve("data.json").toFile();
        String json = "{\"name\":\"test\",\"value\":42}";
        Tools.writeFile(f, json);

        String read = Tools.readFile(f);
        assertThat(read).isEqualTo(json);
    }
}
