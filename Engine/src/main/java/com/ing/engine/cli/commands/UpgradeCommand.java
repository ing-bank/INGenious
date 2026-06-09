package com.ing.engine.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.io.ProjectMigrator;
import com.ing.datalib.component.io.TestCaseYaml;
import com.ing.engine.cli.INGeniousCLI;
import com.ing.engine.cli.output.Silencer;
import com.ing.engine.cli.output.Style;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.stream.Stream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

/**
 * Interactive project upgrade wizard. Walks the user through four
 * independent modernisation steps:
 *
 * <ol>
 *   <li><b>Object Repositories</b> — convert {@code IOR.object}, {@code MOR.object},
 *       {@code StructuredDataOR.object}, {@code SapOR.object} (and their Shared
 *       siblings) to per-page YAML under {@code ObjectRepository/}.
 *       Implemented by loading the project — {@code ObjectRepository.init()}
 *       auto-migrates and archives the originals to {@code ProjectXMLOR/}.</li>
 *   <li><b>Test cases</b> — convert all {@code TestPlan/} and
 *       {@code ReusableComponents/} CSVs to YAML via
 *       {@link ProjectMigrator#migrate(File, boolean, boolean)}.</li>
 *   <li><b>Deprecated files</b> — delete the legacy XML stubs / archive
 *       folders that {@code ObjectRepository.init()} leaves behind after
 *       its first run, plus the {@code ReusableComponent.xml.bak} stub.</li>
 *   <li><b>Mislocated reusables</b> — any YAML file under
 *       {@code TestPlan/<scenario>/<x>.yaml} whose top-level key is
 *       {@code reusable: <name>} belongs in {@code ReusableComponents/};
 *       relocate while preserving the scenario sub-folder.</li>
 * </ol>
 *
 * <p>Each step prompts before executing. Use {@code --yes} to accept all
 * defaults, or {@code --dry-run} to preview without writing.
 */
@Command(
    name = "upgrade",
    description = "Interactive upgrade wizard: modernise ORs, test cases, and clean up legacy files"
)
public class UpgradeCommand implements Callable<Integer> {
    @Parameters(index = "0", description = "Project name or path", defaultValue = "")
    private String projectPath;

    @Option(
        names = { "-y", "--yes" },
        description = "Accept the default for every prompt (run unattended)"
    )
    private boolean assumeYes;

    @Option(
        names = { "--dry-run" },
        description = "Report what would change without writing any files"
    )
    private boolean dryRun;

    @Option(
        names = { "--keep-backup" },
        description = "When migrating CSV → YAML, keep originals under .migration-backup/"
    )
    private boolean keepBackup;

    /** Cached interactive reader. Falls back to {@code System.in} when no console. */
    private BufferedReader stdin;

    @Override
    public Integer call() {
        INGeniousCLI cli = INGeniousCLI.getInstance();
        Style s = cli.style();

        String path = projectPath.isEmpty() ? cli.getProjectPath() : projectPath;
        if (path == null || path.isEmpty()) {
            cli.printError("Project path required. Pass it as an argument or use --project.");
            return 1;
        }
        File projectDir = resolveProjectDir(path);
        if (projectDir == null) {
            cli.printError("Project not found: " + path);
            return 1;
        }

        cli.printHeader("Project Upgrade Wizard  ·  " + projectDir.getName());
        System.out.println("  " + s.dim("Location: ") + projectDir.getAbsolutePath());
        if (dryRun) {
            cli.printWarning("Dry-run mode: no files will be modified.");
        }

        // --- Pre-flight scan ----------------------------------------------
        UpgradePlan plan = scan(projectDir);
        renderPlan(cli, plan);

        if (!plan.hasAnything()) {
            cli.printSuccess("Nothing to upgrade — project is already modern.");
            return 0;
        }

        int totalChanges = 0;

        // --- Step 1: Object Repository XML → YAML -------------------------
        if (plan.hasXmlOR && askYes(cli, "Convert Object Repositories from XML to YAML?", true)) {
            totalChanges += upgradeObjectRepository(cli, projectDir);
            // Conversion archives the originals to ProjectXMLOR/ and leaves
            // a small stub at the original location. Re-scan so step 3
            // picks them up automatically.
            rescanDeprecated(projectDir, plan);
        }

        // --- Step 2: Test cases CSV → YAML --------------------------------
        if (
            plan.csvTestCases > 0 &&
            askYes(
                cli,
                "Convert " + plan.csvTestCases + " CSV test case(s) / reusable(s) to YAML?",
                true
            )
        ) {
            totalChanges += upgradeTestCases(cli, projectDir);
        }

        // --- Step 3: Deprecated files cleanup -----------------------------
        if (
            !plan.deprecated.isEmpty() &&
            askYes(
                cli,
                "Delete " + plan.deprecated.size() + " deprecated file(s) / folder(s)?",
                true
            )
        ) {
            totalChanges += cleanupDeprecated(cli, plan.deprecated);
        }

        // --- Step 4: Relocate mislocated reusables ------------------------
        if (
            !plan.mislocatedReusables.isEmpty() &&
            askYes(
                cli,
                "Move " +
                plan.mislocatedReusables.size() +
                " reusable component(s) from TestPlan/ to ReusableComponents/?",
                true
            )
        ) {
            totalChanges += relocateReusables(cli, projectDir, plan.mislocatedReusables);
        }

        cli.printHeader("Summary");
        if (totalChanges == 0) {
            cli.printInfo("No changes applied.");
        } else if (dryRun) {
            cli.printInfo(
                totalChanges + " change(s) would be applied. Re-run without --dry-run to commit."
            );
        } else {
            cli.printSuccess(totalChanges + " change(s) applied.");
            cli.printInfo(
                "Re-run 'ingenious project validate " +
                projectDir.getName() +
                "' to see the updated health score."
            );
        }
        return 0;
    }

    // -----------------------------------------------------------------------
    // Pre-flight scan
    // -----------------------------------------------------------------------

    /** Snapshot of every modernisation opportunity discovered on disk. */
    private static final class UpgradePlan {
        boolean hasXmlOR;
        final List<File> xmlORFiles = new ArrayList<>();
        int csvTestCases;
        final List<File> deprecated = new ArrayList<>();
        final List<File> mislocatedReusables = new ArrayList<>();

        boolean hasAnything() {
            return (
                hasXmlOR ||
                csvTestCases > 0 ||
                !deprecated.isEmpty() ||
                !mislocatedReusables.isEmpty()
            );
        }
    }

    /** Walks the project root and populates an {@link UpgradePlan}. */
    private UpgradePlan scan(File projectDir) {
        UpgradePlan plan = new UpgradePlan();

        // OR XML files — both project root and Shared/* siblings.
        String[] orXmlNames = {
            "IOR.object",
            "MOR.object",
            "StructuredDataOR.object",
            "SapOR.object"
        };
        for (String name : orXmlNames) {
            File f = new File(projectDir, name);
            if (f.isFile() && f.length() > 0) {
                plan.hasXmlOR = true;
                plan.xmlORFiles.add(f);
            }
        }
        File sharedDir = new File(projectDir.getParentFile(), "Shared");
        if (sharedDir.isDirectory()) {
            for (String[] pair : new String[][] {
                { "SharedWebObjects", "SharedOR.object" },
                { "SharedMobileObjects", "SharedMOR.object" },
                { "SharedSapObjects", "SharedSapOR.object" }
            }) {
                File f = new File(new File(sharedDir, pair[0]), pair[1]);
                if (f.isFile() && f.length() > 0) {
                    plan.hasXmlOR = true;
                    plan.xmlORFiles.add(f);
                }
            }
        }

        // CSV test cases under TestPlan/, ReusableComponents/, TestLab/.
        plan.csvTestCases =
            countCsv(new File(projectDir, "TestPlan")) +
            countCsv(new File(projectDir, "ReusableComponents")) +
            countCsv(new File(projectDir, "TestLab"));

        // Deprecated files / archive folders.
        for (String name : new String[] {
            "ReusableComponent.xml.bak",
            "ReusableComponent.xml",
            "ProjectXMLOR",
            "SharedXMLOR"
        }) {
            File f = new File(projectDir, name);
            if (f.exists()) plan.deprecated.add(f);
        }
        // Stub OR XML files that ObjectRepository.init() leaves as breadcrumbs
        // after an earlier auto-conversion. We only flag them as deprecated
        // when no live XML payload is present — otherwise step 1 handles
        // them via the archive flow.
        if (!plan.hasXmlOR) {
            for (String name : orXmlNames) {
                File f = new File(projectDir, name);
                if (f.isFile() && f.length() < 500) plan.deprecated.add(f);
            }
        }

        // Mislocated reusables — YAMLs under TestPlan/ that declare 'reusable:'.
        File testPlan = new File(projectDir, "TestPlan");
        if (testPlan.isDirectory()) {
            collectMislocatedReusables(testPlan, plan.mislocatedReusables);
        }

        return plan;
    }

    private int countCsv(File root) {
        if (root == null || !root.isDirectory()) return 0;
        try (Stream<Path> stream = Files.walk(root.toPath())) {
            return (int) stream
                .filter(Files::isRegularFile)
                .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".csv"))
                .count();
        } catch (IOException ignored) {
            return 0;
        }
    }

    /**
     * Refreshes the {@link UpgradePlan#deprecated} list after a destructive
     * step has run. Used after the OR conversion to surface the stubs and
     * archive folders that {@code ObjectRepository.init()} leaves behind.
     */
    private void rescanDeprecated(File projectDir, UpgradePlan plan) {
        plan.deprecated.clear();
        for (String name : new String[] {
            "ReusableComponent.xml.bak",
            "ReusableComponent.xml",
            "ProjectXMLOR",
            "SharedXMLOR"
        }) {
            File f = new File(projectDir, name);
            if (f.exists()) plan.deprecated.add(f);
        }
        for (String name : new String[] {
            "IOR.object",
            "MOR.object",
            "StructuredDataOR.object",
            "SapOR.object"
        }) {
            File f = new File(projectDir, name);
            // After conversion, any file still here is a stub by definition.
            if (f.isFile()) plan.deprecated.add(f);
        }
    }

    private void collectMislocatedReusables(File testPlanRoot, List<File> out) {
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        try (Stream<Path> stream = Files.walk(testPlanRoot.toPath())) {
            stream
                .filter(Files::isRegularFile)
                .filter(
                    p -> {
                        String n = p.getFileName().toString().toLowerCase();
                        return n.endsWith(".yaml") || n.endsWith(".yml");
                    }
                )
                .forEach(
                    p -> {
                        try {
                            TestCaseYaml tcy = yaml.readValue(p.toFile(), TestCaseYaml.class);
                            if (tcy != null && tcy.isReusable()) {
                                out.add(p.toFile());
                            }
                        } catch (Exception ignored) {
                            // Malformed YAML — leave for the validate command.
                        }
                    }
                );
        } catch (IOException ignored) {
            // Walk failure — nothing to relocate.
        }
    }

    // -----------------------------------------------------------------------
    // Rendering
    // -----------------------------------------------------------------------

    private void renderPlan(INGeniousCLI cli, UpgradePlan plan) {
        Style s = cli.style();
        cli.printHeader("Findings");
        bullet(s, "Legacy OR XML", plan.hasXmlOR ? plan.xmlORFiles.size() + " file(s)" : "none");
        bullet(
            s,
            "CSV test cases",
            plan.csvTestCases == 0 ? "none" : plan.csvTestCases + " file(s)"
        );
        bullet(
            s,
            "Deprecated files",
            plan.deprecated.isEmpty() ? "none" : plan.deprecated.size() + " item(s)"
        );
        bullet(
            s,
            "Mislocated reusables",
            plan.mislocatedReusables.isEmpty()
                ? "none"
                : plan.mislocatedReusables.size() + " file(s)"
        );
    }

    private void bullet(Style s, String label, String value) {
        String padded = label + "                        ";
        padded = padded.substring(0, 24);
        boolean isNone = "none".equals(value);
        String coloured = isNone ? s.green(value) : s.yellow(value);
        System.out.println(
            "  " + s.cyan(Style.ICON_BULLET) + " " + s.bold(padded) + s.dim(": ") + coloured
        );
    }

    // -----------------------------------------------------------------------
    // Step implementations
    // -----------------------------------------------------------------------

    /**
     * Loading the project triggers {@code ObjectRepository.init()} which
     * detects XML, converts every page to YAML, and archives the originals
     * to {@code ProjectXMLOR/} and {@code SharedXMLOR/}. We just need to
     * load the project once and report.
     */
    private int upgradeObjectRepository(INGeniousCLI cli, File projectDir) {
        if (dryRun) {
            cli.printInfo("  → would convert XML ORs to YAML and archive originals");
            return 1;
        }
        try (Silencer silenced = Silencer.aroundProjectLoad()) {
            Project project = new Project(projectDir.getAbsolutePath());
            // saveAsYaml() is a no-op when nothing changed, but it forces the
            // page-per-file layout to be flushed in case init() created an
            // in-memory representation only.
            project.getObjectRepository().saveAsYaml();
        } catch (Exception ex) {
            cli.printError("OR conversion failed: " + ex.getMessage());
            return 0;
        }
        cli.printSuccess("Object Repositories converted to YAML.");
        return 1;
    }

    /**
     * Runs {@link ProjectMigrator#migrate(File, boolean, boolean)} on the
     * project root. Conflicts (CSV + YAML for the same name) are reported
     * via the {@code .migration-backup/conflicts/} folder.
     */
    private int upgradeTestCases(INGeniousCLI cli, File projectDir) {
        ProjectMigrator.Result r;
        try {
            r = ProjectMigrator.migrate(projectDir, dryRun, keepBackup);
        } catch (Exception ex) {
            cli.printError("Test-case migration failed: " + ex.getMessage());
            return 0;
        }
        if (!r.converted.isEmpty()) {
            String verb = dryRun ? "would be converted" : "converted";
            cli.printSuccess(r.converted.size() + " CSV file(s) " + verb + " to YAML.");
        }
        if (!r.skippedYamlExisted.isEmpty()) {
            cli.printInfo(r.skippedYamlExisted.size() + " already-YAML file(s) skipped.");
        }
        if (!r.conflicts.isEmpty()) {
            cli.printWarning(
                r.conflicts.size() +
                " CSV/YAML conflict(s) parked under " +
                ProjectMigrator.BACKUP_DIR +
                "/" +
                ProjectMigrator.CONFLICTS_SUBDIR +
                "/"
            );
        }
        for (String err : r.errors) {
            cli.printError(err);
        }
        return r.converted.size() + r.conflicts.size();
    }

    /** Deletes the deprecated file/folder list collected during scan. */
    private int cleanupDeprecated(INGeniousCLI cli, List<File> deprecated) {
        int removed = 0;
        for (File f : deprecated) {
            if (dryRun) {
                cli.printInfo("  → would delete " + relPath(f));
                removed++;
                continue;
            }
            if (deleteRecursive(f)) {
                cli.printSuccess("Deleted " + relPath(f));
                removed++;
            } else {
                cli.printWarning("Could not delete " + relPath(f));
            }
        }
        return removed;
    }

    /**
     * Moves each YAML file under {@code TestPlan/<scenario>/<x>.yaml} that
     * declares {@code reusable:} into {@code ReusableComponents/<scenario>/<x>.yaml},
     * creating parent directories as needed. The relative path under
     * {@code TestPlan/} is preserved verbatim so nested scenario folders
     * survive intact.
     */
    private int relocateReusables(INGeniousCLI cli, File projectDir, List<File> files) {
        Path testPlanRoot = new File(projectDir, "TestPlan").toPath();
        Path reusableRoot = new File(projectDir, "ReusableComponents").toPath();
        int moved = 0;
        for (File f : files) {
            Path rel = testPlanRoot.relativize(f.toPath());
            Path target = reusableRoot.resolve(rel);
            if (dryRun) {
                cli.printInfo(
                    "  → would move " + relPath(f) + " → " + projectDir.toPath().relativize(target)
                );
                moved++;
                continue;
            }
            try {
                Files.createDirectories(target.getParent());
                if (Files.exists(target)) {
                    cli.printWarning(
                        "Target already exists, skipping: " + projectDir.toPath().relativize(target)
                    );
                    continue;
                }
                Files.move(f.toPath(), target, StandardCopyOption.ATOMIC_MOVE);
                cli.printSuccess(
                    "Moved " + relPath(f) + " → " + projectDir.toPath().relativize(target)
                );
                moved++;
            } catch (IOException ex) {
                cli.printError("Move failed for " + relPath(f) + ": " + ex.getMessage());
            }
        }
        return moved;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Resolves a project argument the same way {@code ValidateCommand} does:
     * absolute path → relative to cwd → relative to {@code ./Projects/}.
     */
    private static File resolveProjectDir(String name) {
        File abs = new File(name);
        if (abs.isAbsolute() && abs.isDirectory()) return abs;
        String cwd = System.getProperty("user.dir");
        File rel = new File(cwd, name);
        if (rel.isDirectory()) return rel;
        File underProjects = new File(cwd, "Projects/" + name);
        if (underProjects.isDirectory()) return underProjects;
        return null;
    }

    private boolean askYes(INGeniousCLI cli, String question, boolean def) {
        if (assumeYes) {
            System.out.println(
                "  " + cli.style().dim("? ") + question + " " + cli.style().cyan("[y/N → auto-yes]")
            );
            return true;
        }
        String hint = def ? "[Y/n]" : "[y/N]";
        System.out.print(
            "  " + cli.style().cyan("? ") + question + " " + cli.style().dim(hint) + " "
        );
        System.out.flush();
        try {
            if (stdin == null) {
                stdin =
                    new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            }
            String line = stdin.readLine();
            if (line == null) return def; // EOF — accept default
            String trimmed = line.trim().toLowerCase();
            if (trimmed.isEmpty()) return def;
            return trimmed.startsWith("y");
        } catch (IOException ex) {
            return def;
        }
    }

    private boolean deleteRecursive(File f) {
        if (f == null || !f.exists()) return true;
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) {
                for (File c : children) deleteRecursive(c);
            }
        }
        return f.delete();
    }

    /** Project-relative path for prettier console output. */
    private String relPath(File f) {
        try {
            String cwd = System.getProperty("user.dir");
            Path base = new File(cwd).toPath().toAbsolutePath();
            return base.relativize(f.toPath().toAbsolutePath()).toString();
        } catch (Exception ex) {
            return f.getAbsolutePath();
        }
    }

    // Suppress IDE "unused" warning on field present for future extension.
    @SuppressWarnings("unused")
    private static final List<String> RESERVED = Arrays.asList();
}
