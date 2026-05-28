package com.ing.datalib.settings;

import static org.assertj.core.api.Assertions.assertThat;
import com.ing.datalib.settings.testmgmt.Option;
import com.ing.datalib.settings.testmgmt.TestMgModule;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests for TestMgmtModule — JSON-based test management module management.
 */
public class TestMgmtModuleTest {

    private Path tempDir;

    @BeforeMethod
    public void setUp() throws IOException {
        tempDir = Files.createTempDirectory("testmgmt-test");
    }

    @AfterMethod
    public void tearDown() throws IOException {
        Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }

    @Test
    public void testConstructorLoadsFromClasspathFallback() {
        // No file at location — falls back to classpath TMModules.json
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        assertThat(tmm.getModules()).isNotNull();
        // Should have loaded at least some modules from classpath
        assertThat(tmm.getModules()).isNotEmpty();
    }

    @Test
    public void testAddModule() {
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        int initialSize = tmm.getModules().size();
        tmm.addModule("CustomModule");

        assertThat(tmm.getModules()).hasSize(initialSize + 1);
        assertThat(tmm.getModuleNames()).contains("CustomModule");
    }

    @Test
    public void testAddDuplicateModule() {
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        tmm.addModule("SameModule");
        int sizeAfterFirst = tmm.getModules().size();
        tmm.addModule("SameModule");

        assertThat(tmm.getModules()).hasSize(sizeAfterFirst);
    }

    @Test
    public void testRemoveModule() {
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        tmm.addModule("ToRemove");
        tmm.removeModule("ToRemove");

        assertThat(tmm.getModuleNames()).doesNotContain("ToRemove");
    }

    @Test
    public void testRemoveNonExistent() {
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        int sizeBefore = tmm.getModules().size();
        tmm.removeModule("NoSuch");
        assertThat(tmm.getModules()).hasSize(sizeBefore);
    }

    @Test
    public void testGetModule() {
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        tmm.addModule("FindMe");
        TestMgModule found = tmm.getModule("FindMe");

        assertThat(found).isNotNull();
        assertThat(found.getModule()).isEqualTo("FindMe");
    }

    @Test
    public void testGetModuleNonExistent() {
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        assertThat(tmm.getModule("NoSuch")).isNull();
    }

    @Test
    public void testPutValues() {
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        List<Option> options = new ArrayList<>();
        options.add(new Option("opt1", "val1"));
        options.add(new Option("opt2", "val2"));

        tmm.putValues("NewModule", options);

        TestMgModule module = tmm.getModule("NewModule");
        assertThat(module.getOptions()).hasSize(2);
        assertThat(module.getOptions().get(0).getName()).isEqualTo("opt1");
        assertThat(module.getOptions().get(0).getValue()).isEqualTo("val1");
    }

    @Test
    public void testPutValuesReplacesExisting() {
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        List<Option> options1 = new ArrayList<>();
        options1.add(new Option("a", "1"));
        tmm.putValues("Mod", options1);

        List<Option> options2 = new ArrayList<>();
        options2.add(new Option("b", "2"));
        options2.add(new Option("c", "3"));
        tmm.putValues("Mod", options2);

        TestMgModule module = tmm.getModule("Mod");
        assertThat(module.getOptions()).hasSize(2);
        assertThat(module.getOptions().get(0).getName()).isEqualTo("b");
    }

    @Test
    public void testAsMap() {
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        List<Option> options = new ArrayList<>();
        options.add(new Option("key1", "value1"));
        options.add(new Option("key2", "value2"));
        tmm.putValues("MapModule", options);

        Map<String, String> map = tmm.asMap();
        assertThat(map).containsEntry("key1", "value1");
        assertThat(map).containsEntry("key2", "value2");
    }

    @Test
    public void testSaveAndReload() {
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        tmm.addModule("Persisted");
        List<Option> opts = new ArrayList<>();
        opts.add(new Option("pk", "pv"));
        tmm.putValues("Persisted", opts);
        tmm.save();

        TestMgmtModule reloaded = new TestMgmtModule(tempDir.toString());
        assertThat(reloaded.getModuleNames()).contains("Persisted");
        TestMgModule mod = reloaded.getModule("Persisted");
        assertThat(mod.getOptions()).hasSize(1);
        assertThat(mod.getOptions().get(0).getName()).isEqualTo("pk");
    }

    @Test
    public void testSaveCreatesFile() {
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        tmm.save();
        assertThat(new File(tmm.getLocation())).exists();
    }

    @Test
    public void testGetLocation() {
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        assertThat(tmm.getLocation()).isEqualTo(tempDir.toString() + File.separator + "TMModules.json");
    }

    @Test
    public void testGetModuleNames() {
        TestMgmtModule tmm = new TestMgmtModule(tempDir.toString());
        tmm.addModule("A");
        tmm.addModule("B");

        List<String> names = tmm.getModuleNames();
        assertThat(names).contains("A", "B");
    }
}
