package com.ing.engine.cli.commands;

import com.ing.datalib.api.APICollection;
import com.ing.datalib.api.APIEnvironment;
import com.ing.datalib.api.APIRequest;
import com.ing.datalib.api.APIResponse;
import com.ing.datalib.api.CurlParser;
import com.ing.datalib.api.importer.ImportException;
import com.ing.datalib.api.importer.ImportUtils;
import com.ing.datalib.api.importer.ImportWarning;
import com.ing.datalib.api.importer.NormalizedCollection;
import com.ing.datalib.api.importer.bruno.BrunoImporter;
import com.ing.datalib.api.importer.postman.PostmanImporter;
import com.ing.datalib.api.importer.spi.CollectionImporter;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.engine.cli.INGeniousCLI;
import com.ing.engine.cli.lib.RequestToTestCaseBuilder;
import com.ing.engine.mcp.ApiCollectionStore;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Collection-first API workflow: ingest APIs as a collection, run them against a
 * live environment, then convert the observed run into an INGenious test case.
 * Thin CLI over {@link ApiCollectionStore} (shared with the MCP tools).
 */
@Command(
    name = "apicollection",
    description = "API collection-first workflow (import -> run -> to-testcase)",
    subcommands = {
        ApiCollectionCommand.ImportSub.class,
        ApiCollectionCommand.ListSub.class,
        ApiCollectionCommand.ShowSub.class,
        ApiCollectionCommand.EnvSetSub.class,
        ApiCollectionCommand.RunSub.class,
        ApiCollectionCommand.RequestRunSub.class,
        ApiCollectionCommand.ToTestCaseSub.class
    }
)
public class ApiCollectionCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("Use 'ingenious apicollection <subcommand>' - see --help");
        return 0;
    }

    private static File projectDir(String projectPath) {
        INGeniousCLI cli = INGeniousCLI.getInstance();
        String path = projectPath != null ? projectPath : cli.getProjectPath();
        if (path == null || path.isEmpty()) return null;
        return new File(path);
    }

    private static Map<String, String> resolveVars(File dir, String envName) {
        Map<String, String> vars = new LinkedHashMap<>();
        if (envName != null) {
            APIEnvironment env = ApiCollectionStore.loadEnvironment(dir, envName);
            if (env != null && env.getVariables() != null) vars.putAll(env.getVariables());
        }
        return vars;
    }

    // ===============================================================
    @Command(
        name = "import",
        description = "Ingest a Postman/Bruno file or a curl command as a collection"
    )
    public static class ImportSub implements Callable<Integer> {
        @ParentCommand
        private ApiCollectionCommand parent;

        @Parameters(index = "0", description = "Collection name")
        private String name;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(names = { "--format" }, description = "postman | bruno | curl (default: auto)")
        private String format;

        @Option(names = { "--file" }, description = "Postman/Bruno collection file")
        private File file;

        @Option(names = { "--curl" }, description = "A curl command string (format=curl)")
        private String curl;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            File dir = projectDir(projectPath);
            if (dir == null) {
                cli.printError("Project path required.");
                return 1;
            }
            try {
                APICollection collection;
                boolean curlMode =
                    ("curl".equalsIgnoreCase(format)) || (curl != null && file == null);
                if (curlMode) {
                    if (curl == null || !CurlParser.looksLikeCurl(curl)) {
                        cli.printError("Provide a valid --curl command for format=curl.");
                        return 1;
                    }
                    APIRequest req = CurlParser.parse(curl);
                    if (req.getName() == null || req.getName().isEmpty()) req.setName(name);
                    collection = new APICollection(name);
                    List<APIRequest> reqs = new ArrayList<>();
                    reqs.add(req);
                    collection.setRequests(reqs);
                } else {
                    if (file == null || !file.exists()) {
                        cli.printError("Provide --file (Postman/Bruno) or --curl.");
                        return 1;
                    }
                    CollectionImporter importer = pickImporter(format, file);
                    if (importer == null) {
                        cli.printError("Unrecognised collection format for: " + file.getName());
                        return 1;
                    }
                    List<ImportWarning> warnings = new ArrayList<>();
                    NormalizedCollection nc;
                    try {
                        nc = importer.parse(file, warnings);
                    } catch (ImportException ie) {
                        cli.printError("Parse failed: " + ie.getMessage());
                        return 1;
                    }
                    for (ImportWarning w : warnings) cli.printInfo(w.getMessage());
                    collection = ApiCollectionStore.fromNormalized(nc, name);
                }
                ApiCollectionStore.saveCollection(dir, collection);
                cli.printSuccess(
                    "Saved collection '" +
                    collection.getName() +
                    "' with " +
                    (collection.getRequests() == null ? 0 : collection.getRequests().size()) +
                    " request(s)."
                );
                return 0;
            } catch (Exception e) {
                cli.printError("Import failed: " + e.getMessage());
                return 1;
            }
        }

        private static CollectionImporter pickImporter(String format, File file) {
            if (format != null) {
                if ("postman".equalsIgnoreCase(format)) return new PostmanImporter();
                if ("bruno".equalsIgnoreCase(format)) return new BrunoImporter();
            }
            PostmanImporter pm = new PostmanImporter();
            if (pm.supports(file)) return pm;
            BrunoImporter br = new BrunoImporter();
            if (br.supports(file)) return br;
            return null;
        }
    }

    // ===============================================================
    @Command(name = "list", description = "List persisted API collections")
    public static class ListSub implements Callable<Integer> {
        @ParentCommand
        private ApiCollectionCommand parent;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            File dir = projectDir(projectPath);
            if (dir == null) {
                cli.printError("Project path required.");
                return 1;
            }
            for (APICollection c : ApiCollectionStore.listCollections(dir)) {
                int n = c.getRequests() == null ? 0 : c.getRequests().size();
                System.out.println(c.getName() + " (" + n + " request(s))");
            }
            return 0;
        }
    }

    // ===============================================================
    @Command(name = "show", description = "Show a collection's requests")
    public static class ShowSub implements Callable<Integer> {
        @ParentCommand
        private ApiCollectionCommand parent;

        @Parameters(index = "0", description = "Collection name")
        private String name;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            File dir = projectDir(projectPath);
            if (dir == null) {
                cli.printError("Project path required.");
                return 1;
            }
            APICollection c = ApiCollectionStore.loadCollection(dir, name);
            if (c == null) {
                cli.printError("Collection not found: " + name);
                return 1;
            }
            if (c.getRequests() != null) {
                for (APIRequest r : c.getRequests()) {
                    String m = r.getMethod() == null ? "GET" : r.getMethod().name();
                    System.out.println(m + "  " + r.getName() + "  " + r.getUrl());
                }
            }
            return 0;
        }
    }

    // ===============================================================
    @Command(name = "env-set", description = "Create/update an API environment")
    public static class EnvSetSub implements Callable<Integer> {
        @ParentCommand
        private ApiCollectionCommand parent;

        @Parameters(index = "0", description = "Environment name")
        private String env;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(names = { "--base-url" }, description = "Base URL stored as variable 'baseUrl'")
        private String baseUrl;

        @Option(names = { "-v", "--var" }, description = "Extra variable key=value (repeatable)")
        private Map<String, String> vars;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            File dir = projectDir(projectPath);
            if (dir == null) {
                cli.printError("Project path required.");
                return 1;
            }
            APIEnvironment e = ApiCollectionStore.loadEnvironment(dir, env);
            if (e == null) e = new APIEnvironment(env);
            if (baseUrl != null) e.setVariable("baseUrl", baseUrl);
            if (vars != null) {
                for (Map.Entry<String, String> en : vars.entrySet()) {
                    e.setVariable(en.getKey(), en.getValue());
                }
            }
            ApiCollectionStore.saveEnvironment(dir, e);
            cli.printSuccess("Saved environment '" + e.getName() + "'.");
            return 0;
        }
    }

    // ===============================================================
    @Command(name = "run", description = "Execute all requests against an environment")
    public static class RunSub implements Callable<Integer> {
        @ParentCommand
        private ApiCollectionCommand parent;

        @Parameters(index = "0", description = "Collection name")
        private String name;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(names = { "--env" }, description = "Environment name for {{vars}}")
        private String env;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            File dir = projectDir(projectPath);
            if (dir == null) {
                cli.printError("Project path required.");
                return 1;
            }
            APICollection c = ApiCollectionStore.loadCollection(dir, name);
            if (c == null) {
                cli.printError("Collection not found: " + name);
                return 1;
            }
            Map<String, String> vars = resolveVars(dir, env);
            int passed = 0, failed = 0;
            if (c.getRequests() != null) {
                for (APIRequest r : c.getRequests()) {
                    APIResponse resp = ApiCollectionStore.execute(r, vars);
                    boolean ok =
                        !resp.isError() &&
                        resp.getStatusCode() >= 200 &&
                        resp.getStatusCode() < 400;
                    if (ok) passed++; else failed++;
                    System.out.println(
                        (ok ? "PASS " : "FAIL ") +
                        r.getName() +
                        "  status=" +
                        resp.getStatusCode() +
                        "  " +
                        resp.getResponseTimeMs() +
                        "ms" +
                        (resp.isError() ? "  error=" + resp.getErrorMessage() : "")
                    );
                }
            }
            cli.printInfo("Passed: " + passed + "  Failed: " + failed);
            return failed == 0 ? 0 : 1;
        }
    }

    // ===============================================================
    @Command(name = "request-run", description = "Execute a single named request")
    public static class RequestRunSub implements Callable<Integer> {
        @ParentCommand
        private ApiCollectionCommand parent;

        @Parameters(index = "0", description = "Collection name")
        private String name;

        @Parameters(index = "1", description = "Request name")
        private String request;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(names = { "--env" }, description = "Environment name for {{vars}}")
        private String env;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            File dir = projectDir(projectPath);
            if (dir == null) {
                cli.printError("Project path required.");
                return 1;
            }
            APICollection c = ApiCollectionStore.loadCollection(dir, name);
            if (c == null) {
                cli.printError("Collection not found: " + name);
                return 1;
            }
            APIRequest req = null;
            if (c.getRequests() != null) {
                for (APIRequest r : c.getRequests()) {
                    if (r.getName() != null && r.getName().equalsIgnoreCase(request)) {
                        req = r;
                        break;
                    }
                }
            }
            if (req == null) {
                cli.printError("Request not found: " + request);
                return 1;
            }
            APIResponse resp = ApiCollectionStore.execute(req, resolveVars(dir, env));
            System.out.println(
                "Status: " + resp.getStatusCode() + "  " + resp.getResponseTimeMs() + "ms"
            );
            if (resp.isError()) System.out.println("Error: " + resp.getErrorMessage());
            if (resp.getBody() != null) System.out.println(resp.getBody());
            return resp.isError() ? 1 : 0;
        }
    }

    // ===============================================================
    @Command(name = "to-testcase", description = "Convert a collection into an INGenious test case")
    public static class ToTestCaseSub implements Callable<Integer> {
        @ParentCommand
        private ApiCollectionCommand parent;

        @Parameters(index = "0", description = "Collection name")
        private String name;

        @Option(names = { "-p", "--project" }, description = "Project path")
        private String projectPath;

        @Option(
            names = { "--scenario" },
            description = "Target scenario (default: collection name)"
        )
        private String scenario;

        @Option(names = { "--testcase" }, description = "Test case name (default: collection name)")
        private String testcase;

        @Option(names = { "--env" }, description = "Environment name to seed assertions from a run")
        private String env;

        @Option(names = { "--reusable" }, description = "Create under a reusable scenario")
        private boolean reusable;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            File dir = projectDir(projectPath);
            if (dir == null) {
                cli.printError("Project path required.");
                return 1;
            }
            APICollection c = ApiCollectionStore.loadCollection(dir, name);
            if (c == null) {
                cli.printError("Collection not found: " + name);
                return 1;
            }
            try {
                Project project = new Project(dir.getAbsolutePath());
                String scenName = scenario != null ? scenario : name;
                String tcName = ImportUtils.sanitizeFileName(testcase != null ? testcase : name);
                Scenario scn = reusable
                    ? project.getReusableScenarioByName(scenName)
                    : project.getScenarioByName(scenName);
                if (scn == null) {
                    scn =
                        reusable
                            ? project.addReusableScenario(scenName)
                            : project.addScenario(scenName);
                    new File(scn.getLocation()).mkdirs();
                }
                if (scn.getTestCaseByName(tcName) != null) {
                    cli.printError("Test case already exists: " + scn.getName() + "/" + tcName);
                    return 1;
                }
                Map<String, String> vars = resolveVars(dir, env);
                TestCase tc = scn.addTestCase(tcName);
                if (tc == null) {
                    cli.printError("Failed to create test case: " + tcName);
                    return 1;
                }
                int asserts = 0;
                if (c.getRequests() != null) {
                    for (APIRequest r : c.getRequests()) {
                        RequestToTestCaseBuilder.appendSteps(tc, r);
                        if (env != null) {
                            APIResponse resp = ApiCollectionStore.execute(r, vars);
                            if (!resp.isError() && resp.getStatusCode() > 0) {
                                TestStep st = tc.addNewStep();
                                st.setObject("Webservice");
                                st.setAction("assertResponseCode");
                                st.setDescription("Assert status for " + r.getName());
                                st.setInput("@" + resp.getStatusCode());
                                asserts++;
                            }
                        }
                    }
                }
                tc.save();
                project.save();
                cli.printSuccess(
                    "Created " +
                    scn.getName() +
                    "/" +
                    tcName +
                    " (" +
                    tc.getTestSteps().size() +
                    " steps, " +
                    asserts +
                    " seeded assertion(s))."
                );
                return 0;
            } catch (Exception e) {
                cli.printError("Conversion failed: " + e.getMessage());
                return 1;
            }
        }
    }
}
