package com.ing.engine.cli.commands;

import com.ing.datalib.api.APIRequest;
import com.ing.datalib.api.CurlParser;
import com.ing.datalib.api.importer.ImportException;
import com.ing.datalib.api.importer.ImportUtils;
import com.ing.datalib.api.importer.ImportWarning;
import com.ing.datalib.api.importer.NormalizedCollection;
import com.ing.datalib.api.importer.NormalizedRequest;
import com.ing.datalib.api.importer.bruno.BrunoImporter;
import com.ing.datalib.api.importer.postman.PostmanImporter;
import com.ing.datalib.api.importer.playwright.PlaywrightRecordingImporter;
import com.ing.datalib.api.importer.spi.CollectionImporter;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.engine.cli.INGeniousCLI;
import com.ing.engine.cli.lib.RequestToTestCaseBuilder;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Import external artefacts (curl, Postman, Bruno, Playwright) into an
 * INGenious project as test cases or reusable components.
 */
@Command(
    name = "import",
    description = "Import external sources (curl/postman/bruno/playwright) into a project",
    subcommands = {
        ImportCommand.CurlSubCommand.class,
        ImportCommand.PostmanSubCommand.class,
        ImportCommand.BrunoSubCommand.class,
        ImportCommand.PlaywrightSubCommand.class
    }
)
public class ImportCommand implements Callable<Integer> {

    @ParentCommand
    private INGeniousCLI parent;

    @Override
    public Integer call() {
        System.out.println("Use 'ingenious import <subcommand>' - see 'ingenious import --help'");
        return 0;
    }

    // ===============================================================
    // curl
    // ===============================================================
    @Command(name = "curl", description = "Import a single curl command as an API test case")
    public static class CurlSubCommand implements Callable<Integer> {
        @ParentCommand private ImportCommand parent;

        @Parameters(index = "0", description = "The curl command string (quote it)")
        private String curl;

        @Option(names = {"-p", "--project"}, description = "Project path") private String projectPath;
        @Option(names = {"--scenario"}, defaultValue = "Imported", description = "Target scenario name")
        private String scenarioName;
        @Option(names = {"--testcase"}, description = "Target test case name (default: derived)")
        private String testCaseName;
        @Option(names = {"--reusable"}, description = "Create as a reusable component instead")
        private boolean reusable;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) { cli.printError("Project path required."); return 1; }
            if (!CurlParser.looksLikeCurl(curl)) {
                cli.printError("Input does not look like a curl command.");
                return 1;
            }
            try {
                APIRequest req = CurlParser.parse(curl);
                Project project = new Project(path);
                Scenario scn = ensureScenario(project, scenarioName, reusable);
                String name = testCaseName != null && !testCaseName.isEmpty()
                        ? ImportUtils.sanitizeFileName(testCaseName)
                        : deriveName(req);
                if (scn.getTestCaseByName(name) != null) {
                    cli.printError("Test case already exists: " + scn.getName() + "/" + name);
                    return 1;
                }
                TestCase tc = RequestToTestCaseBuilder.build(req, scn, name);
                project.save();
                if (tc == null) {
                    cli.printError("Failed to create test case.");
                    return 1;
                }
                cli.printSuccess("Imported curl as "
                        + (reusable ? "reusable " : "") + "test case: "
                        + scn.getName() + "/" + name);
                return 0;
            } catch (Exception e) {
                cli.printError("Curl import failed: " + e.getMessage());
                return 1;
            }
        }
    }

    // ===============================================================
    // postman
    // ===============================================================
    @Command(name = "postman", description = "Import a Postman collection as test cases or reusables")
    public static class PostmanSubCommand implements Callable<Integer> {
        @ParentCommand private ImportCommand parent;

        @Parameters(index = "0", description = "Postman collection JSON file")
        private File file;

        @Option(names = {"-p", "--project"}, description = "Project path") private String projectPath;
        @Option(names = {"--scenario"}, defaultValue = "Postman", description = "Target scenario name")
        private String scenarioName;
        @Option(names = {"--reusable"}, description = "Import as reusable components")
        private boolean reusable;
        @Option(names = {"--conflict"}, defaultValue = "rename",
                description = "On name conflict: skip | overwrite | rename (default: rename)")
        private ConflictMode conflict;

        @Override
        public Integer call() {
            return importCollection(new PostmanImporter(), file, projectPath,
                    scenarioName, reusable, conflict, "Postman");
        }
    }

    // ===============================================================
    // bruno
    // ===============================================================
    @Command(name = "bruno", description = "Import a Bruno collection (file or directory) as test cases or reusables")
    public static class BrunoSubCommand implements Callable<Integer> {
        @ParentCommand private ImportCommand parent;

        @Parameters(index = "0", description = "Bruno collection file OR root directory")
        private File file;

        @Option(names = {"-p", "--project"}, description = "Project path") private String projectPath;
        @Option(names = {"--scenario"}, defaultValue = "Bruno", description = "Target scenario name")
        private String scenarioName;
        @Option(names = {"--reusable"}, description = "Import as reusable components")
        private boolean reusable;
        @Option(names = {"--conflict"}, defaultValue = "rename",
                description = "On name conflict: skip | overwrite | rename (default: rename)")
        private ConflictMode conflict;

        @Override
        public Integer call() {
            return importCollection(new BrunoImporter(), file, projectPath,
                    scenarioName, reusable, conflict, "Bruno");
        }
    }

    // ===============================================================
    // playwright
    // ===============================================================
    @Command(name = "playwright", description = "Import a Playwright recording as a test case")
    public static class PlaywrightSubCommand implements Callable<Integer> {
        @ParentCommand private ImportCommand parent;

        @Parameters(index = "0", description = "Playwright recording (Java source emitted by codegen, .txt or .java)")
        private File file;

        @Option(names = {"-p", "--project"}, description = "Project path") private String projectPath;
        @Option(names = {"--scenario"}, description = "Target scenario name (default: derived from file name)")
        private String scenarioName;
        @Option(names = {"--testcase"}, description = "Test case name (default: derived from file name)")
        private String testCaseName;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            String path = projectPath != null ? projectPath : cli.getProjectPath();
            if (path == null || path.isEmpty()) { cli.printError("Project path required."); return 1; }
            if (file == null || !file.exists()) { cli.printError("Recording file not found."); return 1; }
            try {
                Project project = new Project(path);
                PlaywrightRecordingImporter.Result r = PlaywrightRecordingImporter.importInto(
                        project, file, scenarioName, testCaseName);
                project.save();
                project.reload();
                if (r.stepCount == 0) {
                    cli.printError("No recognised Playwright steps in: " + file.getName());
                    return 1;
                }
                cli.printSuccess("Imported Playwright recording as test case: "
                        + r.scenarioName + "/" + r.testCaseName
                        + " (" + r.stepCount + " step(s))");
                for (String w : r.warnings) cli.printError("Warning: " + w);
                return 0;
            } catch (Exception e) {
                cli.printError("Playwright import failed: " + e.getMessage());
                return 1;
            }
        }
    }

    // ===============================================================
    // shared helpers
    // ===============================================================

    public enum ConflictMode { skip, overwrite, rename }

    private static Scenario ensureScenario(Project project, String name, boolean reusable) {
        Scenario s = reusable
                ? project.getReusableScenarioByName(name)
                : project.getScenarioByName(name);
        if (s == null) {
            s = reusable ? project.addReusableScenario(name) : project.addScenario(name);
            new File(s.getLocation()).mkdirs();
        }
        return s;
    }

    private static String deriveName(APIRequest req) {
        if (req.getName() != null && !req.getName().isEmpty()) {
            return ImportUtils.sanitizeFileName(req.getName());
        }
        String url = req.getUrl() == null ? "request" : req.getUrl();
        String path = url.replaceAll("https?://[^/]+", "");
        if (path.isEmpty() || "/".equals(path)) path = url;
        path = path.replaceAll("[?#].*$", "").replaceAll("/+$", "");
        if (path.startsWith("/")) path = path.substring(1);
        if (path.isEmpty()) path = "request";
        String method = req.getMethod() == null ? "GET" : req.getMethod().name();
        return ImportUtils.sanitizeFileName(method + "_" + path.replace('/', '_'));
    }

    /**
     * Run a {@link CollectionImporter}, map every {@link NormalizedRequest}
     * into a test case (or reusable component) under {@code scenarioName},
     * and persist. Prints per-warning info as well as a final summary.
     */
    private static int importCollection(CollectionImporter importer, File source,
                                        String projectPath, String scenarioName,
                                        boolean reusable, ConflictMode conflict,
                                        String label) {
        INGeniousCLI cli = INGeniousCLI.getInstance();
        String path = projectPath != null ? projectPath : cli.getProjectPath();
        if (path == null || path.isEmpty()) { cli.printError("Project path required."); return 1; }
        if (source == null || !source.exists()) {
            cli.printError(label + " source not found.");
            return 1;
        }
        if (!importer.supports(source)) {
            cli.printError(source.getName() + " is not recognised as a " + label + " source.");
            return 1;
        }
        try {
            List<ImportWarning> warnings = new ArrayList<>();
            NormalizedCollection collection;
            try {
                collection = importer.parse(source, warnings);
            } catch (ImportException ie) {
                cli.printError(label + " parse failed: " + ie.getMessage());
                return 1;
            }
            for (ImportWarning w : warnings) cli.printInfo(w.getMessage());

            Project project = new Project(path);
            Scenario scn = ensureScenario(project, scenarioName, reusable);

            int created = 0, skipped = 0, renamed = 0;
            for (NormalizedRequest nreq : collection.getRequests()) {
                if (nreq == null || nreq.getRequest() == null) continue;
                APIRequest req = nreq.getRequest();
                String base = (req.getName() != null && !req.getName().isEmpty())
                        ? req.getName() : deriveName(req);
                String name = ImportUtils.sanitizeFileName(base);

                if (scn.getTestCaseByName(name) != null) {
                    switch (conflict) {
                        case skip:
                            skipped++;
                            continue;
                        case overwrite: {
                            TestCase old = scn.getTestCaseByName(name);
                            File f = new File(old.getLocation());
                            if (f.exists()) f.delete();
                            scn.getTestCases().remove(old);
                            break;
                        }
                        case rename:
                        default: {
                            String candidate = name;
                            int n = 2;
                            while (scn.getTestCaseByName(candidate) != null) {
                                candidate = name + "_" + (n++);
                            }
                            name = candidate;
                            renamed++;
                            break;
                        }
                    }
                }
                TestCase tc = RequestToTestCaseBuilder.build(req, scn, name);
                if (tc != null) created++;
            }
            project.save();
            cli.printSuccess(label + " import complete: " + created + " created, "
                    + renamed + " renamed, " + skipped + " skipped");
            return 0;
        } catch (Exception e) {
            cli.printError(label + " import failed: " + e.getMessage());
            return 1;
        }
    }
}
