package com.ing.datalib.component.io;

import com.ing.datalib.component.Project;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ProjectMigratorTest {

    private Path projectRoot;

    @BeforeMethod
    public void setUp() throws Exception {
        projectRoot = Files.createTempDirectory("ingenious-project-");
    }

    @AfterMethod
    public void tearDown() throws Exception {
        if (projectRoot != null) {
            Files.walk(projectRoot).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        }
    }

    @Test
    public void convertsCsvTestCaseAndKeepsBackup() throws Exception {
        File scenarioDir = makeDirs(projectRoot.toFile(), Project.TEST_PLAN_DIR, "Login");
        File csv = new File(scenarioDir, "ValidUser.csv");
        writeCsv(csv,
                "Step,ObjectName,Description,Action,Input,Condition,Reference",
                "1,Execute,given,Step:Reusable,,,",
                "2,Username,fill,Fill,bob,,Login");

        ProjectMigrator.Result result = ProjectMigrator.migrate(projectRoot.toFile(), false, true);

        assertThat(result.errors).isEmpty();
        assertThat(result.converted).extracting(File::getName).containsExactly("ValidUser.csv");
        assertThat(new File(scenarioDir, "ValidUser.yaml")).isFile();
        assertThat(csv).doesNotExist();
        File backup = new File(projectRoot.toFile(),
                ProjectMigrator.BACKUP_DIR + "/" + Project.TEST_PLAN_DIR + "/Login/ValidUser.csv");
        assertThat(backup).isFile();
    }

    @Test
    public void isIdempotentOnYamlOnlyProject() throws Exception {
        File scenarioDir = makeDirs(projectRoot.toFile(), Project.TEST_PLAN_DIR, "Login");
        File yamlFile = new File(scenarioDir, "ValidUser.yaml");
        new YamlTestCaseStore().save(yamlFile, "ValidUser", "Login", false, null,
                Arrays.asList(Arrays.asList("1", "Execute", "given", "Step:R", "", "", "")));

        ProjectMigrator.Result first = ProjectMigrator.migrate(projectRoot.toFile(), false, true);
        assertThat(first.hasChanges()).isFalse();
        assertThat(first.errors).isEmpty();
    }

    @Test
    public void dryRunDoesNotModifyDisk() throws Exception {
        File scenarioDir = makeDirs(projectRoot.toFile(), Project.TEST_PLAN_DIR, "Login");
        File csv = new File(scenarioDir, "ValidUser.csv");
        writeCsv(csv, "Step,ObjectName,Description,Action,Input,Condition,Reference",
                "1,Execute,given,Step:R,,,");

        ProjectMigrator.Result result = ProjectMigrator.migrate(projectRoot.toFile(), true, true);

        assertThat(result.converted).hasSize(1);
        assertThat(csv).isFile();
        assertThat(new File(scenarioDir, "ValidUser.yaml")).doesNotExist();
    }

    @Test
    public void mixedFormatTreatsYamlAsWinnerAndArchivesCsv() throws Exception {
        File scenarioDir = makeDirs(projectRoot.toFile(), Project.TEST_PLAN_DIR, "Login");
        File csv = new File(scenarioDir, "Same.csv");
        writeCsv(csv, "Step,ObjectName,Description,Action,Input,Condition,Reference",
                "1,Execute,given,Step:R,,,");
        File yamlFile = new File(scenarioDir, "Same.yaml");
        new YamlTestCaseStore().save(yamlFile, "Same", "Login", false, null,
                Arrays.asList(Arrays.asList("1", "Execute", "given", "Step:R", "", "", "")));

        ProjectMigrator.Result result = ProjectMigrator.migrate(projectRoot.toFile(), false, true);

        assertThat(result.conflicts).hasSize(1);
        assertThat(csv).doesNotExist();
        File conflictBackup = new File(projectRoot.toFile(),
                ProjectMigrator.BACKUP_DIR + "/" + ProjectMigrator.CONFLICTS_SUBDIR
                        + "/" + Project.TEST_PLAN_DIR + "/Login/Same.csv");
        assertThat(conflictBackup).isFile();
    }

    private static File makeDirs(File root, String... segments) {
        File dir = root;
        for (String segment : segments) {
            dir = new File(dir, segment);
        }
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IllegalStateException("Cannot create " + dir);
        }
        return dir;
    }

    private static void writeCsv(File file, String... lines) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            sb.append(line).append('\n');
        }
        Files.write(file.toPath(), sb.toString().getBytes());
    }
}
