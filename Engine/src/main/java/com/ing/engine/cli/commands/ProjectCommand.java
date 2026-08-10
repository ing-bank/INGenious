package com.ing.engine.cli.commands;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Release;
import com.ing.datalib.component.Release;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestSet;
import com.ing.datalib.component.TestStep;
import com.ing.engine.cli.INGeniousCLI;
import com.ing.engine.cli.output.OutputFormatter;
import com.ing.engine.cli.output.Silencer;
import com.ing.engine.cli.output.Style;
import java.io.File;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Project management commands.
 */
@Command(
    name = "project",
    mixinStandardHelpOptions = true,
    description = "Project management commands",
    subcommands = {
        ProjectCommand.ListCommand.class,
        ProjectCommand.InfoCommand.class,
        ProjectCommand.ValidateCommand.class,
        ProjectCommand.CreateCommand.class,
        UpgradeCommand.class
    }
)
public class ProjectCommand implements Callable<Integer> {
    @ParentCommand
    private INGeniousCLI parent;

    @Override
    public Integer call() {
        System.out.println("Use 'ingenious project <subcommand>' - see 'ingenious project --help'");
        return 0;
    }

    /**
     * List projects in a directory.
     */
    @Command(name = "list", description = "List all projects in a directory")
    public static class ListCommand implements Callable<Integer> {
        @ParentCommand
        private ProjectCommand parent;

        @Parameters(
            index = "0",
            description = "Directory to search for projects",
            defaultValue = "."
        )
        private File directory;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();

            if (!directory.exists() || !directory.isDirectory()) {
                cli.printError("Directory not found: " + directory.getAbsolutePath());
                return 1;
            }

            // Compact, scannable layout: name + counts. Path is shown
            // separately as a header line so the table doesn't blow up
            // horizontally and wrap on narrow terminals.
            List<String> headers = Arrays.asList(
                "Project",
                "Scenarios",
                "Test Cases",
                "Test Sets",
                "Location"
            );
            List<List<String>> rows = new ArrayList<>();

            File[] subdirs = directory.listFiles(File::isDirectory);
            if (subdirs != null) {
                Arrays.sort(
                    subdirs,
                    Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER)
                );
                // Silence the chatty bootstrap output from Datalib (default
                // properties materialisation, YamlORReader scans). We're only
                // reading counts here; the noise is irrelevant for `list`.
                try (Silencer ignored = Silencer.aroundProjectLoad()) {
                    for (File subdir : subdirs) {
                        if (isProjectDirectory(subdir)) {
                            try {
                                Project project = new Project(subdir.getAbsolutePath());
                                int scenarioCount = project.getScenarios().size();
                                int testCaseCount = project
                                    .getScenarios()
                                    .stream()
                                    .mapToInt(s -> s.getTestCases().size())
                                    .sum();
                                int testSetCount = project
                                    .getReleases()
                                    .stream()
                                    .mapToInt(r -> r.getTestSets().size())
                                    .sum();

                                rows.add(
                                    Arrays.asList(
                                        project.getName(),
                                        String.valueOf(scenarioCount),
                                        String.valueOf(testCaseCount),
                                        String.valueOf(testSetCount),
                                        shortLocation(subdir, directory)
                                    )
                                );
                            } catch (Exception e) {
                                rows.add(
                                    Arrays.asList(
                                        subdir.getName(),
                                        "?",
                                        "?",
                                        "?",
                                        shortLocation(subdir, directory)
                                    )
                                );
                            }
                        }
                    }
                }
            }

            if (rows.isEmpty()) {
                cli.printWarning("No projects found in: " + directory.getAbsolutePath());
                return 0;
            }

            cli.printHeader("Projects");
            cli.printInfo(cli.style().dim("in " + shortLocation(directory, null)));
            System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
            System.out.println();
            cli.printInfo(rows.size() + " project" + (rows.size() == 1 ? "" : "s") + " found");
            return 0;
        }

        /**
         * Return a path short enough to fit a table column without wrapping.
         * If the project sits inside the search directory we just show
         * {@code ./<name>} (or {@code ./Projects/<name>}); otherwise we fall
         * back to a {@code ~/…} home-shortened path.
         */
        private String shortLocation(File target, File base) {
            try {
                String home = System.getProperty("user.home", "");
                String abs = target.getCanonicalPath();
                if (base != null) {
                    String baseAbs = base.getCanonicalPath();
                    if (abs.startsWith(baseAbs + File.separator)) {
                        return "./" + abs.substring(baseAbs.length() + 1);
                    }
                    if (abs.equals(baseAbs)) {
                        return ".";
                    }
                }
                if (!home.isEmpty() && abs.startsWith(home)) {
                    return "~" + abs.substring(home.length());
                }
                return abs;
            } catch (Exception e) {
                return target.getPath();
            }
        }

        private boolean isProjectDirectory(File dir) {
            return (
                new File(dir, "TestPlan").exists() ||
                new File(dir, ".project").exists() ||
                new File(dir, "ObjectRepository").exists()
            );
        }
    }

    /**
     * Show project information.
     */
    @Command(name = "info", description = "Show project information")
    public static class InfoCommand implements Callable<Integer> {
        @ParentCommand
        private ProjectCommand parent;

        @Parameters(index = "0", description = "Project path", defaultValue = "")
        private String projectPath;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();

            String path = projectPath.isEmpty() ? cli.getProjectPath() : projectPath;
            if (path == null || path.isEmpty()) {
                cli.printError("Project path required. Use --project or specify as argument.");
                return 1;
            }

            File projectDir = new File(path);
            if (!projectDir.exists()) {
                cli.printError("Project not found: " + path);
                return 1;
            }

            try {
                Project project;
                try (Silencer ignored = Silencer.aroundProjectLoad()) {
                    project = new Project(projectDir.getAbsolutePath());
                }

                Map<String, Object> info = new LinkedHashMap<>();
                info.put("name", project.getName());
                info.put("location", project.getLocation());
                info.put("scenarios", project.getScenarios().size());

                int testCaseCount = project
                    .getScenarios()
                    .stream()
                    .mapToInt(s -> s.getTestCases().size())
                    .sum();
                info.put("testCases", testCaseCount);

                info.put("releases", project.getReleases().size());

                int testSetCount = project
                    .getReleases()
                    .stream()
                    .mapToInt(r -> r.getTestSets().size())
                    .sum();
                info.put("testSets", testSetCount);

                // Object Repository info
                if (
                    project.getObjectRepository() != null &&
                    project.getObjectRepository().getWebOR() != null
                ) {
                    int pageCount = project.getObjectRepository().getWebOR().getPages().size();
                    int objectCount = project
                        .getObjectRepository()
                        .getWebOR()
                        .getPages()
                        .stream()
                        .mapToInt(p -> p.getChildCount())
                        .sum();
                    info.put("pages", pageCount);
                    info.put("objects", objectCount);
                }

                System.out.println(cli.getOutputFormatter().formatKeyValue(info));
                return 0;
            } catch (Exception e) {
                cli.printError("Failed to load project: " + e.getMessage());
                return 1;
            }
        }
    }

    /**
     * Validate project structure & report a health dashboard.
     *
     * <p>Beyond the original "do the required directories exist?" check, this
     * command now scores six independent dimensions of project hygiene and
     * surfaces them as a per-test-case quality breakdown plus a single
     * overall grade:
     *
     * <ol>
     *   <li><b>OR format</b> — XML (legacy {@code IOR.object} etc.) vs YAML
     *       ({@code ObjectRepository/Web|Mobile|StructuredData|SAP/})</li>
     *   <li><b>Test-case format</b> — % of test cases stored as YAML rather
     *       than the legacy CSV under {@code TestPlan/}</li>
     *   <li><b>Modularity</b> — % of steps per test case that are reusable
     *       calls vs inline ("loose") steps; modest reuse is healthy, so the
     *       score saturates at 30% reusable steps</li>
     *   <li><b>Data parameterisation</b> — % of step inputs that are NOT
     *       hard-coded literals (i.e. they reference a datasheet column, a
     *       variable, or an expression rather than starting with {@code @})</li>
     *   <li><b>Test-set coverage</b> — has at least one test set, with a
     *       bonus that scales with the fraction of test cases (not just
     *       scenarios) referenced by at least one test set's executions</li>
     *   <li><b>Tagging</b> — % of test cases that carry at least one tag</li>
     * </ol>
     *
     * The six sub-scores are averaged into an overall percentage and mapped
     * to A/B/C/D/F.
     */
    @Command(
        name = "validate",
        description = "Show a health dashboard for the project (formats, quality, tagging, score)"
    )
    public static class ValidateCommand implements Callable<Integer> {
        @ParentCommand
        private ProjectCommand parent;

        @Parameters(index = "0", description = "Project name or path", defaultValue = "")
        private String projectPath;

        @Option(
            names = "--strict",
            description = "Treat warnings as errors (non-zero exit) and add stricter checks"
        )
        private boolean strict;

        @Option(
            names = "--no-detail",
            description = "Skip the per-test-case quality table; show only the summary scores"
        )
        private boolean noDetail;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();

            String path = projectPath.isEmpty() ? cli.getProjectPath() : projectPath;
            if (path == null || path.isEmpty()) {
                cli.printError("Project name or path required.");
                cli.printInfo("Usage: ingenious project validate <Project>");
                return 1;
            }

            File projectDir = resolveProjectDir(path);
            if (projectDir == null) {
                cli.printError("Project not found: " + path);
                return 1;
            }

            // ----------------------------------------------------------------
            // 1. Structural sanity (no point scoring a broken layout)
            // ----------------------------------------------------------------
            List<String> errors = new ArrayList<>();
            List<String> warnings = new ArrayList<>();
            checkDirectory(projectDir, "TestPlan", errors);
            checkDirectory(projectDir, "Settings", warnings);
            checkDirectory(projectDir, "TestData", warnings);
            // ObjectRepository folder is only created when YAML ORs exist;
            // legacy XML projects keep IOR.object at the root. Don't treat
            // its absence as an error.

            // ----------------------------------------------------------------
            // 2. Format detection (XML vs YAML for OR, CSV vs YAML for TCs)
            // ----------------------------------------------------------------
            OrFormat orFormat = detectOrFormat(projectDir);
            TcFormatStats tcFormat = scanTestCaseFormats(new File(projectDir, "TestPlan"));

            // ----------------------------------------------------------------
            // 2b. Datasheet quality analysis (Scope column adoption)
            // ----------------------------------------------------------------
            DatasheetQualityStats datasheetStats = scanDatasheetQuality(
                new File(projectDir, "TestData")
            );

            // ----------------------------------------------------------------
            // 3. Per-test-case quality analysis (uses the live project model)
            // ----------------------------------------------------------------
            Project project = null;
            List<TestCaseQuality> tcQuality = new ArrayList<>();
            List<TestCaseQuality> reusableQuality = new ArrayList<>();
            int scenarioCount = 0;
            int totalTestCases = 0;
            int totalReleases = 0;
            int totalTestSets = 0;
            int reusableScenarioCount = 0;
            int reusableComponentCount = 0;
            int taggedTcCount = 0;
            Set<String> scenariosInTestSets = new HashSet<>();
            // Track per-test-case coverage as "Scenario/TestCase" keys so
            // a test set that includes only one of several sibling test
            // cases doesn't falsely score 100% coverage.
            Set<String> testCasesInTestSets = new HashSet<>();

            try {
                try (Silencer ignored = Silencer.aroundProjectLoad()) {
                    // Load project in read-only mode to prevent auto-migrations during validation
                    project = new Project(projectDir.getAbsolutePath(), true);
                }
                scenarioCount = project.getScenarios().size();
                reusableScenarioCount = project.getReusableScenarios().size();

                for (Scenario scenario : project.getScenarios()) {
                    for (TestCase tc : scenario.getTestCases()) {
                        totalTestCases++;
                        // Test cases are constructed lazily — the steps list
                        // is empty until we explicitly load it from disk.
                        try (Silencer silenced = Silencer.aroundProjectLoad()) {
                            tc.loadTestCaseTableModel();
                        } catch (Exception loadEx) {
                            /* best effort */
                        }
                        TestCaseQuality q = analyseTestCase(project, scenario, tc, new HashSet<>());
                        tcQuality.add(q);
                        if (q.hasTag) taggedTcCount++;
                        if (tc.getTestSteps().isEmpty()) {
                            warnings.add(
                                "Empty test case: " + scenario.getName() + "/" + tc.getName()
                            );
                        }
                    }
                    if (scenario.getTestCases().isEmpty()) {
                        warnings.add("Scenario '" + scenario.getName() + "' has no test cases");
                    }
                }

                // Enhancement #2: report quality for every reusable
                // component (each test case in each reusable scenario).
                for (Scenario rs : project.getReusableScenarios()) {
                    for (TestCase rtc : rs.getTestCases()) {
                        reusableComponentCount++;
                        try (Silencer silenced = Silencer.aroundProjectLoad()) {
                            rtc.loadTestCaseTableModel();
                        } catch (Exception loadEx) {
                            /* best effort */
                        }
                        reusableQuality.add(analyseTestCase(project, rs, rtc, new HashSet<>()));
                    }
                }

                for (Release release : project.getReleases()) {
                    totalReleases++;
                    for (TestSet ts : release.getTestSets()) {
                        totalTestSets++;
                        // Steps are populated lazily — load before reading.
                        try (Silencer silenced = Silencer.aroundProjectLoad()) {
                            ts.loadTestSetTableModel();
                        } catch (Exception loadEx) {
                            /* best effort */
                        }
                        for (com.ing.datalib.component.ExecutionStep es : ts.getTestSteps()) {
                            String scn = es.getTestScenarioName();
                            String tcName = es.getTestCaseName();
                            if (scn != null && !scn.isEmpty()) {
                                scenariosInTestSets.add(scn);
                                if (tcName != null && !tcName.isEmpty()) {
                                    testCasesInTestSets.add(scn + "/" + tcName);
                                }
                            }
                        }
                    }
                    if (release.getTestSets().isEmpty()) {
                        warnings.add("Release '" + release.getName() + "' has no test sets");
                    }
                }

                // Surface test cases that no test set references — these
                // are why test-set coverage is below 100.
                if (totalTestSets > 0) {
                    for (Scenario scenario : project.getScenarios()) {
                        for (TestCase tc : scenario.getTestCases()) {
                            String key = scenario.getName() + "/" + tc.getName();
                            if (!testCasesInTestSets.contains(key)) {
                                warnings.add("Test case not in any test set: " + key);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                errors.add("Failed to load project: " + e.getMessage());
            }

            // ----------------------------------------------------------------
            // 4. Score each dimension (0-100)
            // ----------------------------------------------------------------
            int orScore = orFormat.score();
            int tcFormatScore = tcFormat.scoreYamlPct();
            int modularityScore = averageReusableScore(tcQuality);
            int dataScore = averageDataScore(tcQuality);
            int testSetScore = scoreTestSets(totalTestSets, totalTestCases, testCasesInTestSets);
            int tagScore = scoreTagging(taggedTcCount, totalTestCases);
            int datasheetScore = datasheetStats.scoreScopeMigration();

            int overall = (int) Math.round(
                (
                    orScore +
                    tcFormatScore +
                    modularityScore +
                    dataScore +
                    testSetScore +
                    tagScore +
                    datasheetScore
                ) /
                7.0
            );

            // ----------------------------------------------------------------
            // 5. Render the dashboard
            // ----------------------------------------------------------------
            renderDashboard(
                cli,
                projectDir,
                project,
                orFormat,
                tcFormat,
                scenarioCount,
                reusableScenarioCount,
                reusableComponentCount,
                totalTestCases,
                totalReleases,
                totalTestSets,
                taggedTcCount,
                orScore,
                tcFormatScore,
                modularityScore,
                dataScore,
                testSetScore,
                tagScore,
                datasheetScore,
                datasheetStats,
                overall
            );

            if (!noDetail && datasheetStats.total > 0) {
                renderDatasheetQualityTable(cli, datasheetStats);
            }

            if (!noDetail && !tcQuality.isEmpty()) {
                renderTestCaseTable(cli, tcQuality);
            }
            if (!noDetail && !reusableQuality.isEmpty()) {
                renderReusableComponentTable(cli, reusableQuality);
            }

            if (!errors.isEmpty()) {
                cli.printHeader("Errors");
                for (String error : errors) {
                    cli.printError(error);
                }
            }
            if (!warnings.isEmpty()) {
                cli.printHeader("Warnings");
                for (String warning : warnings) {
                    cli.printWarning(warning);
                }
            }

            System.out.println();
            if (!errors.isEmpty()) return 1;
            if (strict && !warnings.isEmpty()) return 1;
            return 0;
        }

        // -------- helpers -----------------------------------------------------

        private void checkDirectory(File base, String name, List<String> errors) {
            File dir = new File(base, name);
            if (!dir.exists()) {
                errors.add("Missing directory: " + name);
            }
        }

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

        /** Marker of which OR format(s) live on disk. */
        private enum OrFormat {
            YAML_ONLY,
            XML_ONLY,
            MIXED,
            NONE;

            String label() {
                switch (this) {
                    case YAML_ONLY:
                        return "YAML (modern)";
                    case XML_ONLY:
                        return "XML (legacy)";
                    case MIXED:
                        return "Mixed XML + YAML";
                    default:
                        return "None";
                }
            }

            /** YAML=100, mixed=60, XML=30, none=50 (no signal). */
            int score() {
                switch (this) {
                    case YAML_ONLY:
                        return 100;
                    case MIXED:
                        return 60;
                    case XML_ONLY:
                        return 30;
                    default:
                        return 50;
                }
            }
        }

        /**
         * Detects which Object Repository format(s) are present. Mirrors the
         * detection logic used by {@code ObjectRepository.init()}:
         *   XML lives in {@code IOR.object / MOR.object / StructuredDataOR.object / SapOR.object};
         *   YAML lives under {@code ObjectRepository/<Web|Mobile|StructuredData|SAP>/}.
         */
        private static OrFormat detectOrFormat(File projectDir) {
            boolean xml = new File(projectDir, "IOR.object").isFile() ||
            new File(projectDir, "MOR.object").isFile() ||
            new File(projectDir, "StructuredDataOR.object").isFile() ||
            new File(projectDir, "SapOR.object").isFile();
            File orDir = new File(projectDir, "ObjectRepository");
            boolean yaml = false;
            if (orDir.isDirectory()) {
                for (String sub : new String[] { "Web", "Mobile", "StructuredData", "SAP" }) {
                    File subDir = new File(orDir, sub);
                    if (subDir.isDirectory() && hasFiles(subDir)) {
                        yaml = true;
                        break;
                    }
                }
            }
            // The OR.init() auto-converts XML to YAML on first load and the
            // root XML file may be a 1-line stub (`<Root type="OR" .../>`)
            // pointing at converted YAML. Treat an XML stub of <500 bytes
            // alongside YAML content as YAML_ONLY for scoring purposes.
            if (xml && yaml) {
                File ior = new File(projectDir, "IOR.object");
                if (ior.isFile() && ior.length() < 500) return OrFormat.YAML_ONLY;
                return OrFormat.MIXED;
            }
            if (yaml) return OrFormat.YAML_ONLY;
            if (xml) return OrFormat.XML_ONLY;
            return OrFormat.NONE;
        }

        private static boolean hasFiles(File dir) {
            File[] kids = dir.listFiles();
            if (kids == null) return false;
            for (File k : kids) {
                if (k.isFile()) return true;
                if (k.isDirectory() && hasFiles(k)) return true;
            }
            return false;
        }

        /** Counts CSV vs YAML test-case files under {@code TestPlan/}. */
        private static class TcFormatStats {
            int csv = 0;
            int yaml = 0;

            int total() {
                return csv + yaml;
            }

            String label() {
                if (total() == 0) return "—";
                if (csv == 0) return "YAML (modern)";
                if (yaml == 0) return "CSV (legacy)";
                int pct = (int) Math.round(yaml * 100.0 / total());
                return "Mixed (" + pct + "% YAML)";
            }

            int scoreYamlPct() {
                if (total() == 0) return 50;
                return (int) Math.round(yaml * 100.0 / total());
            }
        }

        private static TcFormatStats scanTestCaseFormats(File testPlanDir) {
            TcFormatStats s = new TcFormatStats();
            if (!testPlanDir.isDirectory()) return s;
            try {
                Files
                    .walk(testPlanDir.toPath())
                    .forEach(
                        p -> {
                            String name = p.getFileName().toString().toLowerCase(Locale.ROOT);
                            if (name.endsWith(".csv")) s.csv++; else if (
                                name.endsWith(".yaml") || name.endsWith(".yml")
                            ) s.yaml++;
                        }
                    );
            } catch (Exception ignored) {
                /* best-effort scan */
            }
            return s;
        }

        /** Statistics about test datasheets (CSV files in TestData directory). */
        private static class DatasheetQualityStats {
            int total = 0;
            int withScope = 0;
            int withoutScope = 0;
            int scopeShared = 0;
            int scopeProject = 0;
            int scopeEmpty = 0;
            int scopeUnexpected = 0;

            int scoreScopeMigration() {
                if (total == 0) return 0;
                return (int) Math.round(withScope * 100.0 / total);
            }
        }

        /**
         * Scans the TestData directory for CSV files and checks which ones
         * have a "Scope" column. Counts records by Scope value (Shared,
         * Project, Empty/Blank). Excludes Global Data CSV files that contain
         * data shared across all environments.
         */
        private static DatasheetQualityStats scanDatasheetQuality(File testDataDir) {
            DatasheetQualityStats stats = new DatasheetQualityStats();
            if (!testDataDir.isDirectory()) return stats;
            try {
                Files
                    .walk(testDataDir.toPath())
                    .filter(
                        p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv")
                    )
                    .filter(p -> !isGlobalDataFile(p.toFile()))
                    .forEach(
                        p -> {
                            try {
                                stats.total++;
                                analyzeDatasheet(p.toFile(), stats);
                            } catch (Exception ignored) {
                                /* best-effort analysis */
                            }
                        }
                    );
            } catch (Exception ignored) {
                /* best-effort scan */
            }
            return stats;
        }

        /**
         * Analyzes a single CSV datasheet file to check for Scope column
         * and count its values. Valid Scope values are:
         * - "[Shared]" (case-insensitive)
         * - "[Project]" (case-insensitive)
         * - null, empty, or missing values
         * Any other non-empty value is tracked as unexpected.
         */
        private static void analyzeDatasheet(File csvFile, DatasheetQualityStats stats) {
            try (
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.FileReader(csvFile)
                )
            ) {
                String headerLine = reader.readLine();
                if (headerLine == null || headerLine.trim().isEmpty()) return;

                String[] headers = headerLine.split(",");
                int scopeColIndex = -1;

                // Find the Scope column index
                for (int i = 0; i < headers.length; i++) {
                    if (headers[i].trim().equalsIgnoreCase("Scope")) {
                        scopeColIndex = i;
                        break;
                    }
                }

                if (scopeColIndex == -1) {
                    stats.withoutScope++;
                    return;
                }

                // Scope column found
                stats.withScope++;

                // Count scope values in data rows
                String dataLine;
                while ((dataLine = reader.readLine()) != null) {
                    if (dataLine.trim().isEmpty()) continue;
                    String[] values = dataLine.split(",");
                    if (scopeColIndex < values.length) {
                        String scopeVal = values[scopeColIndex].trim();
                        if (scopeVal.isEmpty()) {
                            // Empty/blank scope value
                            stats.scopeEmpty++;
                        } else if (scopeVal.equalsIgnoreCase("[Shared]")) {
                            // Shared reusable component
                            stats.scopeShared++;
                        } else if (scopeVal.equalsIgnoreCase("[Project]")) {
                            // Project reusable component
                            stats.scopeProject++;
                        } else {
                            // Unexpected/invalid scope value
                            stats.scopeUnexpected++;
                        }
                    } else {
                        // Missing scope value (column index out of range)
                        stats.scopeEmpty++;
                    }
                }
            } catch (Exception ignored) {
                /* best-effort analysis */
            }
        }

        /**
         * Checks if a CSV file is a Global Data file. Global Data files
         * (GlobalData.csv) contain data shared across all environments and
         * should be excluded from Test Datasheet validation metrics.
         *
         * @param csvFile the CSV file to check
         * @return true if the file is a Global Data file, false otherwise
         */
        private static boolean isGlobalDataFile(File csvFile) {
            String fileName = csvFile.getName();
            return fileName.equalsIgnoreCase("GlobalData.csv");
        }

        /** Quality snapshot for a single test case (or reusable component). */
        private static class TestCaseQuality {
            String scenario;
            String name;
            int totalSteps;
            int reusableSteps;
            int hardcodedInputs;
            int parameterisedInputs;
            boolean hasTag;
            // Per-archetype step tallies. A step is counted in exactly one
            // bucket (or none, if it cannot be classified). These are used
            // to derive the {@link #kindLabel()} shown in the table.
            int webSteps;
            int apiSteps;
            int mobileSteps;
            int dbSteps;
            int kafkaSteps;

            int reusablePct() {
                return totalSteps == 0 ? 0 : (int) Math.round(reusableSteps * 100.0 / totalSteps);
            }

            int dataPct() {
                int inputs = hardcodedInputs + parameterisedInputs;
                return inputs == 0
                    ? 100 // nothing to parameterise = perfect
                    : (int) Math.round(parameterisedInputs * 100.0 / inputs);
            }

            /** True when at least one Webservice/StructuredOR step is present. */
            boolean isApi() {
                return apiSteps > 0;
            }

            /**
             * Renders the {@code Kind} column. Joins every non-zero archetype
             * with {@code " + "} (e.g. {@code "Web + API"}). Returns
             * {@code "Unknown"} when no step could be classified.
             */
            String kindLabel() {
                List<String> parts = new ArrayList<>(5);
                if (webSteps > 0) parts.add("UI");
                if (apiSteps > 0) parts.add("API");
                if (mobileSteps > 0) parts.add("Mobile");
                if (dbSteps > 0) parts.add("DB");
                if (kafkaSteps > 0) parts.add("Kafka");
                return parts.isEmpty() ? "Unknown" : String.join(" + ", parts);
            }
        }

        /**
         * Walks every step of a test case and classifies its inputs.
         *
         * <p>Conventions (mirror {@code TestStep.isTestDataStep()}):
         * <ul>
         *   <li>{@code @value} — hard-coded literal</li>
         *   <li>{@code %var%}  — variable reference  (parameterised)</li>
         *   <li>{@code =expr}  — expression          (parameterised)</li>
         *   <li>{@code Sheet:Column} — datasheet ref (parameterised)</li>
         * </ul>
         *
         * <p>Two domain-specific tweaks applied here:
         * <ol>
         *   <li><b>API tests</b> (mostly {@code Webservice} object steps): we
         *       only score the request payload(s). For each payload we count
         *       the leaf scalar values and the number of those that contain
         *       a {@code %var%} reference. URLs, headers, status codes and
         *       JSONPath assertions are ignored — they would otherwise drown
         *       out the actual data-driven content.</li>
         *   <li><b>Reusable calls</b>: when a step is {@code Execute
         *       Scenario:Reusable}, the called reusable's own input counts
         *       are merged into the caller (one level deep, with a visited
         *       set to prevent cycles). This rewards modular tests for the
         *       parameterisation buried inside their reusables.</li>
         * </ol>
         */
        private static TestCaseQuality analyseTestCase(
            Project project,
            Scenario scenario,
            TestCase tc,
            Set<String> visiting
        ) {
            TestCaseQuality q = new TestCaseQuality();
            q.scenario = scenario.getName();
            q.name = tc.getName();

            for (TestStep step : tc.getTestSteps()) {
                if (step.isEmpty() || step.isCommented()) continue;
                q.totalSteps++;
                boolean reusable = Boolean.TRUE.equals(step.isReusableStep());
                if (reusable) {
                    q.reusableSteps++;
                    // Enhancement #1: bubble the reusable's own input scoring
                    // (and its archetype tallies) into the parent. Look up
                    // Scenario:Reusable, expand it, and merge its counts.
                    mergeReusableInto(q, project, step, visiting);
                    continue;
                }

                // Per-step archetype classification. We look at the step's
                // Object cell — first against the built-in archetype names
                // (Browser / Webservice / Mobile / Database / Kafka), then
                // fall back to OR membership (a Page name living in the
                // Web, Mobile or StructuredData OR).
                String archetype = classifyObjectType(project, step.getObject());
                boolean stepIsApi = "API".equals(archetype);
                if (archetype != null) {
                    switch (archetype) {
                        case "Web":
                            q.webSteps++;
                            break;
                        case "API":
                            q.apiSteps++;
                            break;
                        case "Mobile":
                            q.mobileSteps++;
                            break;
                        case "DB":
                            q.dbSteps++;
                            break;
                        case "Kafka":
                            q.kafkaSteps++;
                            break;
                        default:/* leave unclassified */
                    }
                }

                String input = step.getInput();
                if (input == null || input.trim().isEmpty()) continue;
                String trimmed = input.trim();

                if (stepIsApi) {
                    // Enhancement #3: API steps are scored on payload tags
                    // only. URLs / headers / status codes / JSONPath asserts
                    // are not "test data" in the user-content sense.
                    if (looksLikePayload(trimmed)) {
                        int[] counts = scorePayload(trimmed);
                        q.parameterisedInputs += counts[0];
                        q.hardcodedInputs += counts[1];
                    }
                    // else: skip — not a payload field
                } else {
                    if (trimmed.startsWith("@")) {
                        q.hardcodedInputs++;
                    } else if (
                        trimmed.startsWith("%") ||
                        trimmed.startsWith("=") ||
                        Boolean.TRUE.equals(step.isTestDataStep())
                    ) {
                        q.parameterisedInputs++;
                    } else {
                        // Inline body / free text — count as hard-coded.
                        q.hardcodedInputs++;
                    }
                }
            }

            // Tag detection: drill into the data items the same way
            // TestCase.collectTags() does, but tolerate missing project info.
            try {
                if (
                    tc.getProject() != null &&
                    tc.getProject().getInfo() != null &&
                    tc.getProject().getInfo().getData() != null
                ) {
                    q.hasTag =
                        tc
                            .getProject()
                            .getInfo()
                            .getData()
                            .find(tc.getName(), scenario.getName())
                            .map(di -> di.getTags() != null && !di.getTags().isEmpty())
                            .orElse(false);
                }
            } catch (Exception ignored) {
                /* tags optional */
            }
            return q;
        }

        /**
         * Resolves a {@code Scenario:Reusable} step against the project's
         * reusable catalogue and folds the reusable's hardcoded/parameterised
         * input counts into the caller's {@code q}. Uses a visited set keyed
         * by {@code "<scenario>/<reusable>"} to prevent infinite recursion
         * if a reusable calls back into the call chain.
         */
        private static void mergeReusableInto(
            TestCaseQuality q,
            Project project,
            TestStep step,
            Set<String> visiting
        ) {
            if (project == null) return;
            String[] parts = step.getReusableData();
            if (parts == null || parts.length < 2) return;
            String key = parts[0] + "/" + parts[1];
            if (!visiting.add(key)) return; // cycle guard
            try {
                Scenario rs = project.getReusableScenarioByName(parts[0]);
                if (rs == null) return;
                TestCase rtc = rs.getTestCaseByName(parts[1]);
                if (rtc == null) return;
                try (Silencer silenced = Silencer.aroundProjectLoad()) {
                    rtc.loadTestCaseTableModel();
                } catch (Exception loadEx) {
                    /* best effort */
                }
                TestCaseQuality inner = analyseTestCase(project, rs, rtc, visiting);
                q.hardcodedInputs += inner.hardcodedInputs;
                q.parameterisedInputs += inner.parameterisedInputs;
                // Propagate archetype tallies so the parent's Kind column
                // reflects what the reusables actually do.
                q.webSteps += inner.webSteps;
                q.apiSteps += inner.apiSteps;
                q.mobileSteps += inner.mobileSteps;
                q.dbSteps += inner.dbSteps;
                q.kafkaSteps += inner.kafkaSteps;
            } finally {
                visiting.remove(key);
            }
        }

        /**
         * Classifies a step's {@code Object} cell into one of the framework
         * archetypes: {@code "Web"}, {@code "API"}, {@code "Mobile"},
         * {@code "DB"}, {@code "Kafka"} — or {@code null} when nothing
         * matches.
         *
         * <p>Resolution order:
         * <ol>
         *   <li>Built-in archetype names ({@code Browser}, {@code Webservice},
         *       {@code Mobile}, {@code Database}/{@code DB},
         *       {@code Kafka}/{@code Queue}).</li>
         *   <li>OR membership — the cell matches either a page name
         *       <i>or</i> the name of any object inside any page of
         *       {@code WebOR}/{@code SapOR} → Web,
         *       {@code MobileOR} → Mobile,
         *       {@code StructuredDataOR} → API.</li>
         * </ol>
         *
         * <p>The OR is traversed once per project and the result memoised
         * in {@link #OR_NAME_INDEX} (keyed by {@code Project} identity) so
         * repeated lookups during a single validate run stay O(1).
         */
        private static String classifyObjectType(Project project, String obj) {
            if (obj == null || obj.isEmpty()) return null;
            switch (obj.toLowerCase()) {
                case "browser":
                    return "Web";
                case "webservice":
                    return "API";
                case "mobile":
                    return "Mobile";
                case "database":
                case "db":
                    return "DB";
                case "kafka":
                case "queue":
                    return "Kafka";
                default:
                    break;
            }
            if (project == null) return null;
            Map<String, String> index = orNameIndex(project);
            return index.get(obj.toLowerCase());
        }

        /**
         * Per-{@code Project} cache of {@code lowercase(name) → archetype}.
         * Holds page names AND object names from every loaded OR (Web,
         * SAP, Mobile, StructuredData) across both the project and shared
         * scopes. Lazily built on first classification request.
         *
         * <p>Uses {@link java.util.IdentityHashMap} so two distinct
         * {@code Project} instances pointing at the same disk location
         * never collide.
         */
        private static final java.util.Map<Project, Map<String, String>> OR_NAME_INDEX = java.util.Collections.synchronizedMap(
            new java.util.IdentityHashMap<>()
        );

        private static Map<String, String> orNameIndex(Project project) {
            Map<String, String> cached = OR_NAME_INDEX.get(project);
            if (cached != null) return cached;
            Map<String, String> index = new java.util.HashMap<>();
            try {
                com.ing.datalib.or.ObjectRepository or = project.getObjectRepository();
                if (or != null) {
                    // Project ORs
                    try {
                        indexWeb(or.getWebOR(), index, "Web");
                    } catch (Exception ignored) {}
                    try {
                        indexWeb(or.getSapOR(), index, "Web");
                    } catch (Exception ignored) {}
                    try {
                        indexMobile(or.getMobileOR(), index, "Mobile");
                    } catch (Exception ignored) {}
                    try {
                        indexStructured(or.getStructuredDataOR(), index, "API");
                    } catch (Exception ignored) {}
                    // Shared ORs (each has its own getXxxSharedOR accessor)
                    try {
                        indexWeb(or.getWebSharedOR(), index, "Web");
                    } catch (Exception ignored) {}
                    try {
                        indexWeb(or.getSapSharedOR(), index, "Web");
                    } catch (Exception ignored) {}
                    try {
                        indexMobile(or.getMobileSharedOR(), index, "Mobile");
                    } catch (Exception ignored) {}
                    try {
                        indexStructured(or.getStructuredDataSharedOR(), index, "API");
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {
                /* OR may be partially loaded */
            }
            OR_NAME_INDEX.put(project, index);
            return index;
        }

        // -------- OR walkers (one shape, used for Web / Mobile / Structured) -------
        // Every OR family exposes getPages() → page.getObjectGroups() →
        // group.getObjects() with a consistent {@code getName()} accessor.
        // Reflection avoids a chain of type-specific overloads that would
        // need to be kept in sync as the OR model evolves.

        private static void indexWeb(Object orObj, Map<String, String> index, String archetype) {
            if (orObj == null) return;
            try {
                java.util.List<?> pages = (java.util.List<?>) orObj
                    .getClass()
                    .getMethod("getPages")
                    .invoke(orObj);
                if (pages == null) return;
                for (Object page : pages) {
                    addLower(
                        index,
                        (String) page.getClass().getMethod("getName").invoke(page),
                        archetype
                    );
                    java.util.List<?> groups = (java.util.List<?>) page
                        .getClass()
                        .getMethod("getObjectGroups")
                        .invoke(page);
                    if (groups == null) continue;
                    for (Object group : groups) {
                        addLower(
                            index,
                            (String) group.getClass().getMethod("getName").invoke(group),
                            archetype
                        );
                        java.util.List<?> objs = (java.util.List<?>) group
                            .getClass()
                            .getMethod("getObjects")
                            .invoke(group);
                        if (objs == null) continue;
                        for (Object o : objs) {
                            addLower(
                                index,
                                (String) o.getClass().getMethod("getName").invoke(o),
                                archetype
                            );
                        }
                    }
                }
            } catch (Exception ignored) {
                /* shape mismatch — skip */
            }
        }

        private static void indexMobile(Object orObj, Map<String, String> index, String archetype) {
            indexWeb(orObj, index, archetype); // identical OR shape
        }

        private static void indexStructured(
            Object orObj,
            Map<String, String> index,
            String archetype
        ) {
            indexWeb(orObj, index, archetype); // identical OR shape
        }

        private static void addLower(Map<String, String> index, String name, String archetype) {
            if (name == null || name.isEmpty()) return;
            index.putIfAbsent(name.toLowerCase(), archetype);
        }

        /** True when the input looks like an inline JSON/XML/array body. */
        private static boolean looksLikePayload(String trimmed) {
            return trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("<");
        }

        /**
         * Scores an inline payload (JSON, XML, or array body) by counting
         * leaf scalar values and how many of them embed a {@code %var%}
         * reference.
         *
         * <p>For JSON/array bodies we use Jackson to walk the tree
         * accurately. For XML we fall back to a regex that pulls out text
         * nodes ({@code >value<}). Both fall back to a final regex scan if
         * parsing fails — better a rough number than no signal at all.
         *
         * @return {@code [parameterised, hardcoded]} counts.
         */
        private static int[] scorePayload(String payload) {
            int parameterised = 0;
            int hardcoded = 0;
            boolean parsed = false;

            if (payload.startsWith("{") || payload.startsWith("[")) {
                try {
                    com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(payload);
                    int[] counts = { 0, 0 };
                    walkJsonLeaves(root, counts);
                    parameterised = counts[0];
                    hardcoded = counts[1];
                    parsed = true;
                } catch (Exception ignored) {
                    /* fall through to regex */
                }
            } else if (payload.startsWith("<")) {
                java.util.regex.Matcher m = java
                    .util.regex.Pattern.compile(">([^<>]+)<")
                    .matcher(payload);
                while (m.find()) {
                    String value = m.group(1).trim();
                    if (value.isEmpty()) continue;
                    if (VAR_REF.matcher(value).find()) parameterised++; else hardcoded++;
                }
                parsed = (parameterised + hardcoded) > 0;
            }

            if (!parsed) {
                // Fallback: at least surface the variable density of the raw
                // payload so a fully-templated body still scores well.
                long varHits = VAR_REF.matcher(payload).results().count();
                if (varHits > 0) {
                    parameterised = (int) varHits;
                    hardcoded = 1; // treat as "mostly templated"
                } else {
                    hardcoded = 1; // one opaque literal payload
                }
            }
            return new int[] { parameterised, hardcoded };
        }

        private static final java.util.regex.Pattern VAR_REF = java.util.regex.Pattern.compile(
            "%[A-Za-z0-9_.]+%"
        );

        /** Recursive walker: every non-null scalar leaf is one "tag". */
        private static void walkJsonLeaves(
            com.fasterxml.jackson.databind.JsonNode node,
            int[] counts
        ) {
            if (node == null || node.isNull()) return;
            if (node.isObject()) {
                node.fields().forEachRemaining(e -> walkJsonLeaves(e.getValue(), counts));
            } else if (node.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode child : node) {
                    walkJsonLeaves(child, counts);
                }
            } else {
                String text = node.asText("");
                if (VAR_REF.matcher(text).find()) counts[0]++; else counts[1]++;
            }
        }

        /** Score for modularity: reusable% saturates at 30% → 100. */
        private static int averageReusableScore(List<TestCaseQuality> list) {
            if (list.isEmpty()) return 50;
            double sum = 0;
            int counted = 0;
            for (TestCaseQuality q : list) {
                if (q.totalSteps == 0) continue;
                double pct = q.reusablePct();
                double score = Math.min(100.0, pct * (100.0 / 30.0));
                sum += score;
                counted++;
            }
            return counted == 0 ? 50 : (int) Math.round(sum / counted);
        }

        private static int averageDataScore(List<TestCaseQuality> list) {
            if (list.isEmpty()) return 50;
            double sum = 0;
            int counted = 0;
            for (TestCaseQuality q : list) {
                int inputs = q.hardcodedInputs + q.parameterisedInputs;
                if (inputs == 0) continue;
                sum += q.dataPct();
                counted++;
            }
            return counted == 0 ? 100 : (int) Math.round(sum / counted);
        }

        /**
         * Score test-set coverage.
         * <ul>
         *   <li>0 if no test sets exist at all.</li>
         *   <li>50 baseline for having any test set, plus up to 50 more
         *       based on the fraction of test cases referenced by at
         *       least one test set's executions.</li>
         * </ul>
         * Using test-case granularity (not scenario granularity) ensures
         * a test set that includes only some of a scenario's test cases
         * doesn't falsely score 100%.
         */
        private static int scoreTestSets(
            int totalTestSets,
            int totalTestCases,
            Set<String> testCasesInTestSets
        ) {
            if (totalTestSets == 0) return 0;
            if (totalTestCases == 0) return 50;
            double coverage = Math.min(1.0, testCasesInTestSets.size() / (double) totalTestCases);
            return (int) Math.round(50 + coverage * 50);
        }

        /** % of test cases tagged, scaled so 50% coverage = 100. */
        private static int scoreTagging(int taggedCount, int total) {
            if (total == 0) return 50;
            double pct = taggedCount * 100.0 / total;
            return (int) Math.round(Math.min(100.0, pct * 2.0));
        }

        private static String grade(int score) {
            if (score >= 90) return "A";
            if (score >= 80) return "B";
            if (score >= 70) return "C";
            if (score >= 60) return "D";
            return "F";
        }

        private static String scoreColor(Style s, int score) {
            if (score >= 80) return s.green(score + "/100");
            if (score >= 60) return s.yellow(score + "/100");
            return s.red(score + "/100");
        }

        private static String scoreBar(Style s, int score) {
            int filled = (int) Math.round(score / 5.0); // 0-20 cells
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < 20; i++) {
                bar.append(i < filled ? "█" : "░");
            }
            String coloured;
            if (score >= 80) coloured = s.green(bar.toString()); else if (score >= 60) coloured =
                s.yellow(bar.toString()); else coloured = s.red(bar.toString());
            return coloured;
        }

        private static void renderDashboard(
            INGeniousCLI cli,
            File projectDir,
            Project project,
            OrFormat orFormat,
            TcFormatStats tcFormat,
            int scenarios,
            int reusableScenarios,
            int reusableComponents,
            int testCases,
            int releases,
            int testSets,
            int taggedTcs,
            int orScore,
            int tcScore,
            int modScore,
            int dataScore,
            int tsScore,
            int tagScore,
            int datasheetScore,
            DatasheetQualityStats datasheetStats,
            int overall
        ) {
            Style s = cli.style();
            String projectName = project != null ? project.getName() : projectDir.getName();

            cli.printHeader("Project Health  ·  " + projectName);
            System.out.println("  " + s.dim("Location: ") + projectDir.getAbsolutePath());

            cli.printHeader("Inventory");
            printRow(s, "Scenarios", String.valueOf(scenarios));
            printRow(s, "Test cases", String.valueOf(testCases));
            printRow(s, "Reusable scenarios", String.valueOf(reusableScenarios));
            printRow(s, "Reusable components", String.valueOf(reusableComponents));
            printRow(s, "Releases", String.valueOf(releases));
            printRow(s, "Test sets", String.valueOf(testSets));
            printRow(s, "Tagged test cases", taggedTcs + " / " + testCases);

            cli.printHeader("Formats");
            printRow(s, "Object Repository", orFormat.label());
            printRow(
                s,
                "Test cases",
                tcFormat.label() + s.dim("  (" + tcFormat.csv + " CSV, " + tcFormat.yaml + " YAML)")
            );

            cli.printHeader("Scores");
            printScoreRow(s, "OR modernisation", orScore);
            printScoreRow(s, "Test-case modernisation", tcScore);
            printScoreRow(s, "Modularity (reusables)", modScore);
            printScoreRow(s, "Data parameterisation", dataScore);
            printScoreRow(s, "Test-set coverage", tsScore);
            printScoreRow(s, "Tag adoption", tagScore);
            printScoreRow(s, "Test Datasheet Scope", datasheetScore);

            cli.printHeader("Overall");
            String g = grade(overall);
            String coloured;
            if (overall >= 80) coloured =
                s.bold(s.green(overall + " / 100  (Grade " + g + ")")); else if (
                overall >= 60
            ) coloured = s.bold(s.yellow(overall + " / 100  (Grade " + g + ")")); else coloured =
                s.bold(s.red(overall + " / 100  (Grade " + g + ")"));
            System.out.println("  " + scoreBar(s, overall) + "  " + coloured);
        }

        private static void printRow(Style s, String label, String value) {
            // Pad to 24 chars so the values line up.
            String padded = label + "                        ";
            padded = padded.substring(0, 24);
            System.out.println(
                "  " + s.cyan(Style.ICON_BULLET) + " " + s.bold(padded) + s.dim(": ") + value
            );
        }

        private static void printScoreRow(Style s, String label, int score) {
            String padded = label + "                        ";
            padded = padded.substring(0, 24);
            System.out.println(
                "  " +
                s.cyan(Style.ICON_BULLET) +
                " " +
                s.bold(padded) +
                " " +
                scoreBar(s, score) +
                "  " +
                scoreColor(s, score)
            );
        }

        /**
         * Renders the Test Datasheet Quality analysis section, showing
         * adoption of the Scope column migration across all test datasheets.
         */
        private static void renderDatasheetQualityTable(
            INGeniousCLI cli,
            DatasheetQualityStats stats
        ) {
            Style s = cli.style();
            cli.printHeader("Test Datasheet Quality");

            System.out.println("  " + s.bold("Migration/Adoption Analysis"));
            System.out.println("  " + s.dim("─".repeat(80)));

            String total = String.valueOf(stats.total);
            String migrated = String.valueOf(stats.withScope);
            String notMigrated = String.valueOf(stats.withoutScope);

            System.out.println(
                "  " +
                s.cyan(Style.ICON_BULLET) +
                " " +
                s.bold("Total test datasheets") +
                s.dim(": ") +
                total
            );
            System.out.println(
                "  " +
                s.cyan(Style.ICON_BULLET) +
                " " +
                s.bold("Datasheets with Scope column") +
                s.dim(": ") +
                s.green(migrated)
            );
            System.out.println(
                "  " +
                s.cyan(Style.ICON_BULLET) +
                " " +
                s.bold("Datasheets without Scope column") +
                s.dim(": ") +
                (stats.withoutScope > 0 ? s.red(notMigrated) : s.dim(notMigrated))
            );

            if (stats.withScope > 0) {
                System.out.println();
                System.out.println("  " + s.bold("Scope Column Values"));
                System.out.println("  " + s.dim("─".repeat(80)));

                int sharedCount = stats.scopeShared;
                int projectCount = stats.scopeProject;
                int emptyCount = stats.scopeEmpty;
                int unexpectedCount = stats.scopeUnexpected;

                System.out.println(
                    "  " +
                    s.cyan(Style.ICON_BULLET) +
                    " " +
                    s.bold("Shared Reusable Components") +
                    s.dim(": ") +
                    String.valueOf(sharedCount)
                );
                System.out.println(
                    "  " +
                    s.cyan(Style.ICON_BULLET) +
                    " " +
                    s.bold("Project Reusable Components") +
                    s.dim(": ") +
                    String.valueOf(projectCount)
                );
                System.out.println(
                    "  " +
                    s.cyan(Style.ICON_BULLET) +
                    " " +
                    s.bold("Empty/Blank (test cases in test plan)") +
                    s.dim(": ") +
                    String.valueOf(emptyCount)
                );

                // Display unexpected scope values if any exist
                if (unexpectedCount > 0) {
                    System.out.println(
                        "  " +
                        s.cyan(Style.ICON_BULLET) +
                        " " +
                        s.bold("Unexpected Scope Values") +
                        s.dim(": ") +
                        s.red(String.valueOf(unexpectedCount))
                    );
                }
            }

            System.out.println();
            int score = stats.scoreScopeMigration();
            String scoreStr = score + "%";
            String coloured;
            if (score >= 90) coloured = s.green(scoreStr); else if (score >= 70) coloured =
                s.yellow(scoreStr); else coloured = s.red(scoreStr);
            System.out.println("  " + s.bold("Migration/Adoption Score") + s.dim(": ") + coloured);
        }

        private static void renderTestCaseTable(INGeniousCLI cli, List<TestCaseQuality> list) {
            Style s = cli.style();
            cli.printHeader("Per-Test-Case Quality");
            // Header
            System.out.println(
                "  " +
                s.bold(pad("Scenario / Test Case", 38)) +
                " " +
                s.bold(pad("Kind", 16)) +
                " " +
                s.bold(pad("Steps", 6)) +
                " " +
                s.bold(pad("Reusable", 10)) +
                " " +
                s.bold(pad("Param", 7)) +
                " " +
                s.bold(pad("Tagged", 7))
            );
            System.out.println("  " + s.dim("─".repeat(87)));
            for (TestCaseQuality q : list) {
                String tcName = q.scenario + "/" + q.name;
                if (tcName.length() > 38) tcName = tcName.substring(0, 35) + "...";
                String kindRaw = q.kindLabel();
                String kindColoured = colourKind(s, kindRaw);
                int reusePctVisLen;
                String reuse;
                if (q.totalSteps == 0) {
                    reuse = s.dim("—");
                    reusePctVisLen = 1;
                } else {
                    String pctTxt = q.reusablePct() + "%";
                    reuse = colourPct(s, q.reusablePct(), 20);
                    reusePctVisLen = pctTxt.length();
                }
                int paramPctVisLen;
                String param;
                int inputs = q.hardcodedInputs + q.parameterisedInputs;
                if (inputs == 0) {
                    param = s.dim("—");
                    paramPctVisLen = 1;
                } else {
                    String pctTxt = q.dataPct() + "%";
                    param = colourPct(s, q.dataPct(), 70);
                    paramPctVisLen = pctTxt.length();
                }
                String tagged = q.hasTag ? s.green("✓") : s.dim("·");
                System.out.println(
                    "  " +
                    pad(tcName, 38) +
                    " " +
                    padRaw(kindColoured, 16, kindRaw.length()) +
                    " " +
                    pad(String.valueOf(q.totalSteps), 6) +
                    " " +
                    padRaw(reuse, 10, reusePctVisLen) +
                    " " +
                    padRaw(param, 7, paramPctVisLen) +
                    " " +
                    padRaw(tagged, 7, 1)
                );
            }
        }

        /**
         * Quality table for the reusable components themselves. We treat
         * each {@code ReusableComponents/<Scenario>/<Reusable>.csv} as a
         * mini test case and surface its step count plus how much of its
         * input is parameterised — which tells maintainers how reusable
         * each "user intent" really is (a reusable full of @hardcoded
         * literals is barely reusable).
         */
        private static void renderReusableComponentTable(
            INGeniousCLI cli,
            List<TestCaseQuality> list
        ) {
            Style s = cli.style();
            cli.printHeader("Per-Reusable-Component Quality");
            System.out.println(
                "  " +
                s.bold(pad("Scenario / Reusable", 42)) +
                " " +
                s.bold(pad("Kind", 16)) +
                " " +
                s.bold(pad("Steps", 6)) +
                " " +
                s.bold(pad("Param", 7)) +
                " " +
                s.bold(pad("Calls reuse", 12))
            );
            System.out.println("  " + s.dim("─".repeat(88)));
            for (TestCaseQuality q : list) {
                String name = q.scenario + "/" + q.name;
                if (name.length() > 42) name = name.substring(0, 39) + "...";
                String kindRaw = q.kindLabel();
                String kindColoured = colourKind(s, kindRaw);
                int paramPctVisLen;
                String param;
                int inputs = q.hardcodedInputs + q.parameterisedInputs;
                if (inputs == 0) {
                    param = s.dim("—");
                    paramPctVisLen = 1;
                } else {
                    String pctTxt = q.dataPct() + "%";
                    param = colourPct(s, q.dataPct(), 70);
                    paramPctVisLen = pctTxt.length();
                }
                String calls = q.reusableSteps > 0
                    ? s.cyan(String.valueOf(q.reusableSteps))
                    : s.dim("·");
                int callsVisLen = q.reusableSteps > 0
                    ? String.valueOf(q.reusableSteps).length()
                    : 1;
                System.out.println(
                    "  " +
                    pad(name, 42) +
                    " " +
                    padRaw(kindColoured, 16, kindRaw.length()) +
                    " " +
                    pad(String.valueOf(q.totalSteps), 6) +
                    " " +
                    padRaw(param, 7, paramPctVisLen) +
                    " " +
                    padRaw(calls, 12, callsVisLen)
                );
            }
        }

        /**
         * Picks an ANSI colour for the Kind label so the column reads at a
         * glance. Pure archetypes get a distinctive colour; mixed labels
         * (containing " + ") are rendered bold; {@code Unknown} is dimmed.
         */
        private static String colourKind(Style s, String label) {
            if ("Unknown".equals(label)) return s.dim(label);
            if (label.contains(" + ")) return s.bold(label);
            switch (label) {
                case "UI":
                    return s.cyan(label);
                case "API":
                    return s.magenta(label);
                case "Mobile":
                    return s.blue(label);
                case "DB":
                    return s.yellow(label);
                case "Kafka":
                    return s.green(label);
                default:
                    return label;
            }
        }

        private static String pad(String v, int w) {
            if (v.length() >= w) return v;
            StringBuilder b = new StringBuilder(v);
            while (b.length() < w) b.append(' ');
            return b.toString();
        }

        /**
         * Pads a string that already contains ANSI escape sequences. We can't
         * just measure {@code .length()} because the escapes are invisible
         * but counted by Java. {@code visibleLen} tells us the visible width
         * so we know how many spaces to append.
         */
        private static String padRaw(String coloured, int w, int visibleLen) {
            if (visibleLen >= w) return coloured;
            StringBuilder b = new StringBuilder(coloured);
            for (int i = visibleLen; i < w; i++) b.append(' ');
            return b.toString();
        }

        /** Colours a "<pct>%" string by threshold and pads-safe for tables. */
        private static String colourPct(Style s, int pct, int goodAt) {
            String txt = pct + "%";
            if (pct >= goodAt) return s.green(txt);
            if (pct >= goodAt / 2) return s.yellow(txt);
            return s.red(txt);
        }
    }

    /**
     * Create a new project.
     */
    @Command(name = "create", description = "Create a new project")
    public static class CreateCommand implements Callable<Integer> {
        @ParentCommand
        private ProjectCommand parent;

        @Parameters(index = "0", description = "Project name")
        private String projectName;

        @Option(
            names = { "-d", "--directory" },
            description = "Parent directory",
            defaultValue = "."
        )
        private File directory;

        @Option(names = { "--template" }, description = "Project template (web, mobile, api)")
        private String template;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();

            File projectDir = new File(directory, projectName);

            if (projectDir.exists()) {
                cli.printError("Project already exists: " + projectDir.getAbsolutePath());
                return 1;
            }

            try {
                // The scaffolding itself is quiet today, but constructing
                // a Project (which the IDE / future enhancements may do to
                // materialise default settings files) is chatty. Wrap the
                // whole bootstrap so any such side-effects stay silent.
                try (Silencer ignored = Silencer.aroundProjectLoad()) {
                    // Create project structure
                    projectDir.mkdirs();
                    new File(projectDir, "TestPlan").mkdirs();
                    new File(projectDir, "ObjectRepository").mkdirs();
                    new File(projectDir, "TestData").mkdirs();
                    new File(projectDir, "Settings").mkdirs();
                    new File(projectDir, "Results").mkdirs();

                    // Create default scenario
                    File defaultScenario = new File(projectDir, "TestPlan/NewScenario");
                    defaultScenario.mkdirs();
                    new File(defaultScenario, "NewTestCase.csv").createNewFile();

                    // Create default release and testset
                    File defaultRelease = new File(projectDir, "TestPlan/NewRelease");
                    defaultRelease.mkdirs();
                    new File(defaultRelease, "NewTestSet.csv").createNewFile();
                }

                cli.printSuccess("Project created: " + projectDir.getAbsolutePath());

                Map<String, Object> result = new LinkedHashMap<>();
                result.put("name", projectName);
                result.put("location", projectDir.getAbsolutePath());
                result.put("template", template != null ? template : "default");

                System.out.println(cli.getOutputFormatter().formatKeyValue(result));
                return 0;
            } catch (Exception e) {
                cli.printError("Failed to create project: " + e.getMessage());
                return 1;
            }
        }
    }
}
