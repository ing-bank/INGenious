package com.ing.engine.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.ing.engine.cli.INGeniousCLI;
import com.ing.engine.perf.HarReader;
import com.ing.engine.perf.K6BrowserScriptGenerator;
import com.ing.engine.perf.K6HttpScriptGenerator;
import com.ing.engine.perf.K6Locator;
import com.ing.engine.perf.K6Runner;
import com.ing.engine.perf.PerfProfile;
import com.ing.engine.perf.PerfRecorder;
import com.ing.engine.perf.PerfReportStore;
import com.ing.engine.perf.PerfWorkspace;
import com.ing.engine.perf.ScriptProvenance;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParentCommand;

/**
 * Performance-test authoring and execution (k6 integration).
 *
 * <p>Phase 0 surface: load-profile management, artifact listing and k6
 * binary detection. Script export / run / record arrive in later phases
 * (see Engine/docs/K6-PERFORMANCE-STUDIO-PLAN.md).
 */
@Command(
    name = "perf",
    aliases = { "performance" },
    description = "Performance test authoring & execution with k6",
    subcommands = {
        PerfCommand.ProfileCommand.class,
        PerfCommand.ListCommand.class,
        PerfCommand.ExportCommand.class,
        PerfCommand.RunCommand.class,
        PerfCommand.ValidateCommand.class,
        PerfCommand.ReportCommand.class,
        PerfCommand.RecordCommand.class,
        PerfCommand.StatusCommand.class,
        PerfCommand.LogsCommand.class,
        PerfCommand.CancelCommand.class,
        PerfCommand.ScaleCommand.class
    }
)
public class PerfCommand implements Callable<Integer> {
    @ParentCommand
    private INGeniousCLI parent;

    @Override
    public Integer call() {
        INGeniousCLI cli = INGeniousCLI.getInstance();
        cli.printHeader("INGenious Performance Studio (k6)");
        String k6 = K6Locator.resolve();
        if (k6 != null) {
            String version = K6Locator.version(k6);
            cli.printSuccess("k6 found: " + k6 + (version == null ? "" : " (" + version + ")"));
        } else {
            cli.printWarning("k6 not found. " + K6Locator.installHint());
        }
        System.out.println();
        System.out.println("Commands:");
        System.out.println(
            "  ingenious perf record <url>              Record browser traffic to a HAR"
        );
        System.out.println(
            "  ingenious perf export <Proj>/<Scen>/<TC> | <file.har>   Generate a k6 script"
        );
        System.out.println(
            "  ingenious perf run <script>              Execute a k6 script (load run)"
        );
        System.out.println(
            "  ingenious perf validate <script>         Debug run: 1 VU, 1 iteration, http trace"
        );
        System.out.println(
            "  ingenious perf report [latest]           Show the latest run summary"
        );
        System.out.println("  ingenious perf profile list              List load profiles");
        System.out.println("  ingenious perf profile show <name>       Show a profile as YAML");
        System.out.println("  ingenious perf profile create <name>     Create a project profile");
        System.out.println("  ingenious perf list [scripts|profiles|recordings|runs]");
        return 0;
    }

    /**
     * Resolve a project folder by name/path (same probing order as
     * {@code ingenious run}): absolute path, ./&lt;name&gt;, ./Projects/&lt;name&gt;.
     */
    static File resolveProjectDir(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        File abs = new File(name);
        if (abs.isAbsolute() && abs.isDirectory()) {
            return abs;
        }
        String cwd = System.getProperty("user.dir");
        File rel = new File(cwd, name);
        if (rel.isDirectory()) {
            return rel;
        }
        File underProjects = new File(cwd, "Projects/" + name);
        if (underProjects.isDirectory()) {
            return underProjects;
        }
        return null;
    }

    /** Project from an explicit option or the global -p flag; may be null. */
    static File projectFromOptions(String projectOption) {
        INGeniousCLI cli = INGeniousCLI.getInstance();
        String candidate = projectOption != null ? projectOption : cli.getProjectPath();
        return resolveProjectDir(candidate);
    }

    // ==================================================================
    // perf profile ...
    // ==================================================================

    @Command(
        name = "profile",
        aliases = { "profiles" },
        description = "Manage load profiles (smoke, average, stress, spike, soak + custom)",
        subcommands = {
            ProfileCommand.ProfileListCommand.class,
            ProfileCommand.ProfileShowCommand.class,
            ProfileCommand.ProfileCreateCommand.class
        }
    )
    public static class ProfileCommand implements Callable<Integer> {
        @ParentCommand
        private PerfCommand parent;

        @Override
        public Integer call() {
            System.out.println(
                "Use 'ingenious perf profile <list|show|create>' - see 'ingenious perf profile --help'"
            );
            return 0;
        }

        @Command(name = "list", description = "List built-in and project load profiles")
        public static class ProfileListCommand implements Callable<Integer> {
            @Option(names = { "-p", "--project" }, description = "Project name or path")
            private String projectOption;

            @Override
            public Integer call() {
                INGeniousCLI cli = INGeniousCLI.getInstance();
                List<String> headers = Arrays.asList("Name", "Source", "Load", "Description");
                List<List<String>> rows = new ArrayList<>();
                java.util.Set<String> projectNames = new java.util.LinkedHashSet<>();
                File projectDir = projectFromOptions(projectOption);
                if (projectDir != null) {
                    PerfWorkspace ws = new PerfWorkspace(projectDir);
                    java.util.Set<String> builtInNames = new java.util.LinkedHashSet<>();
                    for (PerfProfile p : PerfProfile.builtIns()) {
                        builtInNames.add(p.name);
                    }
                    for (File f : ws.listProfiles()) {
                        String source;
                        try {
                            PerfProfile p = PerfProfile.fromYaml(f);
                            projectNames.add(p.name);
                            source = builtInNames.contains(p.name) ? "built-in (yaml)" : "project";
                            rows.add(
                                Arrays.asList(
                                    p.name,
                                    source,
                                    p.summarize(),
                                    truncate(p.description, 60)
                                )
                            );
                        } catch (Exception e) {
                            rows.add(
                                Arrays.asList(
                                    f.getName(),
                                    "project",
                                    "-",
                                    "INVALID: " + e.getMessage()
                                )
                            );
                        }
                    }
                }
                // built-ins without a project YAML (or when no project given)
                for (PerfProfile p : PerfProfile.builtIns()) {
                    if (!projectNames.contains(p.name)) {
                        rows.add(
                            Arrays.asList(
                                p.name,
                                "built-in",
                                p.summarize(),
                                truncate(p.description, 60)
                            )
                        );
                    }
                }
                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                if (projectDir == null) {
                    cli.printInfo("Tip: pass --project <name> to include project-level profiles.");
                }
                return 0;
            }
        }

        @Command(name = "show", description = "Show a load profile as YAML")
        public static class ProfileShowCommand implements Callable<Integer> {
            @Parameters(index = "0", description = "Profile name")
            private String name;

            @Option(names = { "-p", "--project" }, description = "Project name or path")
            private String projectOption;

            @Override
            public Integer call() throws Exception {
                INGeniousCLI cli = INGeniousCLI.getInstance();
                File projectDir = projectFromOptions(projectOption);
                PerfProfile profile;
                try {
                    profile = PerfProfile.resolve(name, projectDir);
                } catch (IllegalArgumentException e) {
                    cli.printError(e.getMessage());
                    return 1;
                }
                if (profile == null) {
                    cli.printError("Unknown profile: " + name);
                    cli.printInfo("Built-ins: smoke, average, stress, spike, soak");
                    return 1;
                }
                YAMLMapper yaml = new YAMLMapper();
                System.out.print(
                    yaml
                        .writerWithDefaultPrettyPrinter()
                        .writeValueAsString(profile.toYamlNode(yaml))
                );
                return 0;
            }
        }

        @Command(
            name = "create",
            description = "Create a project profile (YAML) derived from a built-in template"
        )
        public static class ProfileCreateCommand implements Callable<Integer> {
            @Parameters(index = "0", description = "New profile name")
            private String name;

            @Option(
                names = { "--from" },
                description = "Built-in template to copy (default: smoke)",
                defaultValue = "smoke"
            )
            private String from;

            @Option(names = { "-p", "--project" }, description = "Project name or path")
            private String projectOption;

            @Option(names = { "--force" }, description = "Overwrite an existing profile")
            private boolean force;

            @Override
            public Integer call() throws Exception {
                INGeniousCLI cli = INGeniousCLI.getInstance();
                File projectDir = projectFromOptions(projectOption);
                if (projectDir == null) {
                    cli.printError("Project required. Use --project <name|path>.");
                    return 1;
                }
                PerfProfile template = PerfProfile.builtIn(from);
                if (template == null) {
                    cli.printError("Unknown template: " + from);
                    cli.printInfo("Built-ins: smoke, average, stress, spike, soak");
                    return 1;
                }
                PerfWorkspace ws = new PerfWorkspace(projectDir);
                File target = new File(ws.profilesDir(), name + ".yaml");
                if (target.exists() && !force) {
                    cli.printError("Profile already exists: " + target + " (use --force)");
                    return 1;
                }
                ws.ensure();
                PerfProfile copy = new PerfProfile(
                    name,
                    "Custom profile derived from '" + template.name + "'. Edit stages/thresholds.",
                    template.executor,
                    template.vus,
                    template.duration,
                    template.stages,
                    template.thresholds,
                    false
                );
                copy.saveTo(target);
                cli.printSuccess("Created " + target);
                cli.printInfo(
                    "Edit the YAML, then use it via: ingenious perf run ... --profile " + name
                );
                return 0;
            }
        }
    }

    // ==================================================================
    // perf list ...
    // ==================================================================

    @Command(
        name = "list",
        description = "List performance artifacts (scripts, profiles, recordings, runs)"
    )
    public static class ListCommand implements Callable<Integer> {
        @Parameters(
            index = "0",
            arity = "0..1",
            description = "What to list: scripts | profiles | recordings | runs (default: all)"
        )
        private String what;

        @Option(names = { "-p", "--project" }, description = "Project name or path")
        private String projectOption;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            File projectDir = projectFromOptions(projectOption);
            if (projectDir == null) {
                cli.printError("Project required. Use --project <name|path>.");
                return 1;
            }
            PerfWorkspace ws = new PerfWorkspace(projectDir);
            String filter = what == null ? "all" : what.toLowerCase();
            boolean all = "all".equals(filter);
            if (all || "scripts".equals(filter)) {
                printSection(cli, "Scripts (Performance/scripts)", ws.listScripts(), ws);
            }
            if (all || "profiles".equals(filter)) {
                printSection(cli, "Profiles (Performance/profiles)", ws.listProfiles(), ws);
            }
            if (all || "recordings".equals(filter)) {
                printSection(cli, "Recordings (Performance/recordings)", ws.listRecordings(), ws);
            }
            if (all || "runs".equals(filter)) {
                List<File> runs = ws.listRuns();
                cli.printHeader("Runs (Results/Performance)");
                if (runs.isEmpty()) {
                    cli.printInfo("  (none)");
                } else {
                    for (File run : runs) {
                        System.out.println(
                            "  " + run.getParentFile().getName() + "/" + run.getName()
                        );
                    }
                }
            }
            return 0;
        }

        private static void printSection(
            INGeniousCLI cli,
            String title,
            List<File> files,
            PerfWorkspace ws
        ) {
            cli.printHeader(title);
            if (files.isEmpty()) {
                cli.printInfo("  (none)");
                return;
            }
            for (File f : files) {
                System.out.println("  " + f.getName());
            }
        }
    }

    // ==================================================================
    // perf record
    // ==================================================================

    @Command(
        name = "record",
        description = "Record browser traffic to a HAR file (Performance/recordings)"
    )
    public static class RecordCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Start URL to open and record")
        private String url;

        @Option(
            names = { "--out" },
            description = "HAR file path (default: Performance/recordings/<host>_<ts>.har)"
        )
        private String out;

        @Option(names = { "-p", "--project" }, description = "Project name or path")
        private String projectOption;

        @Option(
            names = { "--duration" },
            description = "Stop automatically after this many seconds (default: interactive, Enter to stop)"
        )
        private Integer durationSeconds;

        @Option(names = { "--headless" }, description = "Record without a visible browser window")
        private boolean headless;

        @Override
        public Integer call() throws Exception {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            File harFile;
            File projectDir = projectFromOptions(projectOption);
            if (out != null) {
                harFile = new File(out);
            } else {
                if (projectDir == null) {
                    cli.printError("Pass --project (recording destination) or --out <file.har>.");
                    return 1;
                }
                PerfWorkspace ws = new PerfWorkspace(projectDir);
                ws.ensure();
                harFile = new File(ws.recordingsDir(), PerfRecorder.defaultName(url));
            }
            cli.printCallout("Recording", url + " -> " + harFile.getName());
            PerfRecorder.Session session;
            try {
                session = PerfRecorder.start(url, harFile, headless);
            } catch (Exception e) {
                cli.printError("Failed to start recording: " + e.getMessage());
                return 1;
            }
            try {
                if (durationSeconds != null && durationSeconds > 0) {
                    cli.printInfo("Recording for " + durationSeconds + "s...");
                    long deadline = System.currentTimeMillis() + durationSeconds * 1000L;
                    while (System.currentTimeMillis() < deadline && session.isAlive()) {
                        Thread.sleep(250);
                    }
                } else {
                    cli.printInfo("Interact with the browser. Press Enter here to stop recording.");
                    waitForEnterOrBrowserClose(session);
                }
            } finally {
                session.stop();
            }
            if (!harFile.isFile()) {
                cli.printError("No HAR was written (browser closed too early?).");
                return 1;
            }
            cli.printSuccess("Recorded " + harFile + " (" + harFile.length() + " bytes)");
            cli.printInfo(
                "Generate a script: ingenious perf export \"" +
                harFile.getPath() +
                "\"" +
                (projectDir == null ? "" : " -p " + projectDir.getName()) +
                " [--url-filter <host>]"
            );
            return 0;
        }

        /** Block until the user presses Enter or the browser window is closed. */
        private static void waitForEnterOrBrowserClose(PerfRecorder.Session session)
            throws Exception {
            while (session.isAlive()) {
                if (System.in.available() > 0) {
                    System.in.read();
                    return;
                }
                Thread.sleep(250);
            }
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    /**
     * Resolve a k6 script: an explicit .js path, or a name under
     * {@code <project>/Performance/scripts/} (with or without extension).
     */
    static File resolveScript(String target, File projectDir) {
        return PerfWorkspace.resolveScript(target, projectDir);
    }

    /** Derive the project dir from a script inside {@code <proj>/Performance/scripts}. */
    static File projectDirOfScript(File script) {
        return PerfWorkspace.projectDirOfScript(script);
    }

    // ==================================================================
    // perf export
    // ==================================================================

    @Command(
        name = "export",
        description = "Generate a k6 script from an API test case or a HAR recording"
    )
    public static class ExportCommand implements Callable<Integer> {
        @Parameters(
            index = "0",
            paramLabel = "<target>",
            description = "<Project>/<Scenario>/<TestCase> or a .har file path"
        )
        private String target;

        @Option(
            names = { "--type" },
            description = "Script flavor: http (protocol) or browser",
            defaultValue = "http"
        )
        private String type;

        @Option(
            names = { "--profile" },
            description = "Load profile baked into options (default: smoke)",
            defaultValue = "smoke"
        )
        private String profileName;

        @Option(
            names = { "--out" },
            description = "Output script path (default: Performance/scripts/<name>.js)"
        )
        private String out;

        @Option(names = { "--force" }, description = "Overwrite a hand-edited script")
        private boolean force;

        @Option(
            names = { "--url-filter" },
            description = "HAR only: keep requests whose URL contains this"
        )
        private String urlFilter;

        @Option(
            names = { "--include-static" },
            description = "HAR only: keep static assets (css/js/images)"
        )
        private boolean includeStatic;

        @Option(
            names = { "--auto-correlate" },
            description = "HAR only: propose correlation rules (tokens from responses that " +
            "reappear in later requests), save them to Performance/rules/, and apply them"
        )
        private boolean autoCorrelate;

        @Option(
            names = { "--rules" },
            description = "Rules file to apply (default: Performance/rules/<script>.rules.yaml)"
        )
        private String rules;

        @Option(names = { "-p", "--project" }, description = "Project (required for .har exports)")
        private String projectOption;

        @Override
        public Integer call() throws Exception {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            boolean browser = "browser".equalsIgnoreCase(type);
            if (!browser && !"http".equalsIgnoreCase(type)) {
                cli.printError("--type must be http or browser.");
                return 1;
            }
            boolean isHar = target.toLowerCase().endsWith(".har");
            if (isHar && browser) {
                cli.printError(
                    "--type browser applies to test cases only; HAR recordings export as --type http."
                );
                return 1;
            }
            return isHar ? exportHar(cli) : exportTestCase(cli, browser);
        }

        private Integer exportTestCase(INGeniousCLI cli, boolean browser) throws Exception {
            String[] parts = target.split("/");
            if (parts.length != 3) {
                cli.printError("Target must be <Project>/<Scenario>/<TestCase> or a .har file.");
                return 1;
            }
            File projectDir = resolveProjectDir(parts[0]);
            if (projectDir == null) {
                cli.printError("Project not found: " + parts[0]);
                return 1;
            }
            com.ing.datalib.component.Project project;
            try {
                project = new com.ing.datalib.component.Project(projectDir.getAbsolutePath());
            } catch (Exception e) {
                cli.printError("Failed to load project: " + e.getMessage());
                return 1;
            }
            com.ing.datalib.component.Scenario scenario = project.getScenarioByName(parts[1]);
            com.ing.datalib.component.TestCase tc = scenario == null
                ? null
                : scenario.getTestCaseByName(parts[2]);
            if (tc == null) {
                cli.printError("Test case not found: " + parts[1] + "/" + parts[2]);
                return 1;
            }
            PerfProfile profile = requireProfile(cli, profileName, projectDir);
            if (profile == null) {
                return 1;
            }
            String source = "TestPlan/" + parts[1] + "/" + parts[2];
            String regenerate =
                "ingenious perf export \"" +
                target +
                "\" --type " +
                (browser ? "browser" : "http") +
                " --profile " +
                profile.name;
            String script;
            java.util.List<String> warnings;
            int itemCount;
            if (browser) {
                K6BrowserScriptGenerator.Result gen = K6BrowserScriptGenerator.fromTestCase(
                    project,
                    tc
                );
                if (gen.actions == 0) {
                    cli.printError("Nothing to export: " + String.join("; ", gen.warnings));
                    return 1;
                }
                script =
                    K6BrowserScriptGenerator.generate(
                        source,
                        regenerate,
                        profile,
                        gen.lines,
                        gen.warnings
                    );
                warnings = gen.warnings;
                itemCount = gen.actions;
            } else {
                K6HttpScriptGenerator.Result gen = K6HttpScriptGenerator.fromTestCase(project, tc);
                if (gen.requests.isEmpty()) {
                    cli.printError("Nothing to export: " + String.join("; ", gen.warnings));
                    return 1;
                }
                script =
                    K6HttpScriptGenerator.generate(
                        source,
                        regenerate,
                        profile,
                        gen.requests,
                        gen.warnings
                    );
                warnings = gen.warnings;
                itemCount = gen.requests.size();
            }
            return write(
                cli,
                projectDir,
                parts[2],
                script,
                warnings,
                itemCount,
                browser ? "browser step" : "request"
            );
        }

        private Integer exportHar(INGeniousCLI cli) throws Exception {
            File har = new File(target);
            if (!har.isFile()) {
                cli.printError("HAR file not found: " + target);
                return 1;
            }
            File projectDir = projectFromOptions(projectOption);
            if (projectDir == null && out == null) {
                cli.printError("HAR export needs --project (script destination) or --out.");
                return 1;
            }
            PerfProfile profile = requireProfile(cli, profileName, projectDir);
            if (profile == null) {
                return 1;
            }
            HarReader.Result read = HarReader.read(har, urlFilter, includeStatic);
            if (read.requests.isEmpty()) {
                cli.printError("No usable requests in HAR: " + String.join("; ", read.warnings));
                return 1;
            }
            String baseName = har.getName().replaceAll("\\.har$", "");

            // ---- rules: load, optionally auto-propose, apply -------------
            com.ing.engine.perf.RuleEngine.Result appliedRules = null;
            File rulesFile = null;
            if (rules != null) {
                rulesFile = new File(rules);
            } else if (projectDir != null) {
                rulesFile =
                    com.ing.engine.perf.PerfRule.defaultRulesFile(
                        new PerfWorkspace(projectDir),
                        baseName
                    );
            }
            java.util.List<com.ing.engine.perf.PerfRule> ruleList = com.ing.engine.perf.PerfRule.load(
                rulesFile
            );
            if (autoCorrelate) {
                java.util.List<com.ing.engine.perf.PerfRule> proposals = com.ing.engine.perf.RuleEngine.proposeCorrelations(
                    read.requests
                );
                int added = 0;
                for (com.ing.engine.perf.PerfRule proposal : proposals) {
                    boolean duplicate = false;
                    for (com.ing.engine.perf.PerfRule existing : ruleList) {
                        if (
                            existing.type.equals(proposal.type) &&
                            existing.value.equals(proposal.value)
                        ) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) {
                        ruleList.add(proposal);
                        added++;
                        cli.printInfo(
                            "Proposed correlation '" +
                            proposal.name +
                            "' (json: " +
                            proposal.extractSelector +
                            " from " +
                            proposal.extractSource +
                            ")"
                        );
                    }
                }
                if (added > 0 && rulesFile != null) {
                    com.ing.engine.perf.PerfRule.save(ruleList, rulesFile);
                    cli.printSuccess("Saved " + added + " proposed rule(s) -> " + rulesFile);
                }
            }
            if (!ruleList.isEmpty()) {
                appliedRules = com.ing.engine.perf.RuleEngine.apply(read.requests, ruleList);
                read.warnings.addAll(appliedRules.warnings);
                cli.printInfo("Rules applied: " + appliedRules.applied + " of " + ruleList.size());
            }

            String script = K6HttpScriptGenerator.generate(
                har.getName(),
                "ingenious perf export \"" + target + "\" --type http --profile " + profile.name,
                profile,
                read.requests,
                read.warnings,
                appliedRules
            );
            return write(
                cli,
                projectDir,
                baseName,
                script,
                read.warnings,
                read.requests.size(),
                "request"
            );
        }

        private PerfProfile requireProfile(INGeniousCLI cli, String name, File projectDir) {
            PerfProfile profile;
            try {
                profile = PerfProfile.resolve(name, projectDir);
            } catch (IllegalArgumentException e) {
                cli.printError(e.getMessage());
                return null;
            }
            if (profile == null) {
                cli.printError(
                    "Unknown profile: " + name + " (built-ins: smoke, average, stress, spike, soak)"
                );
            }
            return profile;
        }

        private Integer write(
            INGeniousCLI cli,
            File projectDir,
            String baseName,
            String script,
            java.util.List<String> warnings,
            int itemCount,
            String noun
        )
            throws Exception {
            File targetFile;
            if (out != null) {
                targetFile = new File(out);
            } else {
                PerfWorkspace ws = new PerfWorkspace(projectDir);
                ws.ensure();
                targetFile = new File(ws.scriptsDir(), baseName + ".js");
            }
            if (targetFile.exists() && ScriptProvenance.isHandEdited(targetFile) && !force) {
                cli.printError(
                    "Refusing to overwrite (hand-edited or hand-written): " + targetFile
                );
                cli.printInfo("Use --force to overwrite, or --out for a different path.");
                return 1;
            }
            if (targetFile.getParentFile() != null) {
                targetFile.getParentFile().mkdirs();
            }
            java.nio.file.Files.write(
                targetFile.toPath(),
                script.getBytes(java.nio.charset.StandardCharsets.UTF_8)
            );
            cli.printSuccess("Exported " + itemCount + " " + noun + "(s) -> " + targetFile);
            for (String w : warnings) {
                cli.printWarning(w);
            }
            cli.printInfo(
                "Validate first: ingenious perf validate " +
                targetFile.getName() +
                (projectDir == null ? "" : " -p " + projectDir.getName())
            );
            return 0;
        }
    }

    // ==================================================================
    // perf run / validate
    // ==================================================================

    @Command(name = "run", description = "Execute a k6 script (load run)")
    public static class RunCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Script name (Performance/scripts) or .js path")
        private String target;

        @Option(names = { "--vus" }, description = "Override: number of VUs")
        private Integer vus;

        @Option(names = { "--duration" }, description = "Override: duration (e.g. 30s, 2m)")
        private String duration;

        @Option(names = { "-p", "--project" }, description = "Project name or path")
        private String projectOption;

        @Option(
            names = { "--detach" },
            description = "Start the run in the background and return immediately" +
            " (control it with perf status/logs/scale/cancel)"
        )
        private boolean detach;

        @Option(
            names = { "--dashboard" },
            description = "Enable the k6 web dashboard (live graphs in the browser +" +
            " report.html export). Implies --detach."
        )
        private boolean dashboard;

        @Override
        public Integer call() throws Exception {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            String k6 = K6Locator.resolve();
            if (k6 == null) {
                cli.printError("k6 not found. " + K6Locator.installHint());
                return 1;
            }
            File projectDir = projectFromOptions(projectOption);
            File script = resolveScript(target, projectDir);
            if (script == null) {
                cli.printError("Script not found: " + target);
                cli.printInfo("Export one first: ingenious perf export <Proj>/<Scen>/<TC>");
                return 1;
            }
            if (projectDir == null) {
                projectDir = projectDirOfScript(script);
            }
            if (projectDir == null) {
                cli.printError("Cannot determine the project for results. Pass --project.");
                return 1;
            }
            java.util.List<String> extra = new java.util.ArrayList<>();
            if (vus != null) {
                extra.add("--vus");
                extra.add(String.valueOf(vus));
            }
            if (duration != null) {
                extra.add("--duration");
                extra.add(duration);
            }
            PerfWorkspace ws = new PerfWorkspace(projectDir);
            if (detach || dashboard) {
                com.ing.engine.perf.PerfRunHandle handle = K6Runner.startAsync(
                    k6,
                    script,
                    ws,
                    "cli",
                    extra,
                    dashboard
                );
                cli.printSuccess("Started run " + handle.runId + " (pid " + handle.pid + ")");
                if (handle.dashboardUrl() != null) {
                    cli.printCallout("Live dashboard", handle.dashboardUrl());
                    openInBrowser(handle.dashboardUrl());
                }
                cli.printInfo(
                    "Track it: ingenious perf status \"" +
                    handle.runId +
                    "\" -p " +
                    projectDir.getName() +
                    "  |  logs / scale / cancel"
                );
                return 0;
            }
            cli.printCallout("k6 run", script.getName());
            K6Runner.RunResult run = K6Runner.run(k6, script, ws, "cli", extra);
            System.out.println();
            printRunOutcome(cli, run.runDir, run.exitCode, run.thresholdsFailed);
            return run.exitCode == 0 ? 0 : 1;
        }

        private static void openInBrowser(String url) {
            try {
                if (
                    java.awt.Desktop.isDesktopSupported() &&
                    java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)
                ) {
                    java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
                }
            } catch (Exception ignored) {
                // best effort — the URL was already printed
            }
        }
    }

    // ==================================================================
    // perf status / logs / cancel / scale (async runs)
    // ==================================================================

    /** Resolve a run handle from an optional id or fall back to the newest running one. */
    private static com.ing.engine.perf.PerfRunHandle resolveRun(
        INGeniousCLI cli,
        String runId,
        String projectOption
    ) {
        File projectDir = projectFromOptions(projectOption);
        if (projectDir == null) {
            cli.printError("Project required. Use --project <name|path>.");
            return null;
        }
        PerfWorkspace ws = new PerfWorkspace(projectDir);
        com.ing.engine.perf.PerfRunHandle handle = runId != null
            ? com.ing.engine.perf.PerfRunRegistry.find(ws, runId)
            : com.ing.engine.perf.PerfRunRegistry.latestRunning(ws);
        if (handle == null) {
            if (runId != null) {
                cli.printError("Run not found: " + runId);
            } else {
                cli.printWarning(
                    "No running k6 run found. Pass a run id (see perf report history)."
                );
            }
            return null;
        }
        return handle;
    }

    @Command(name = "status", description = "Live status + metrics of an async k6 run")
    public static class StatusCommand implements Callable<Integer> {
        @Parameters(
            index = "0",
            arity = "0..1",
            description = "Run id (<script>/<timestamp>); default: newest running"
        )
        private String runId;

        @Option(names = { "-p", "--project" }, description = "Project name or path")
        private String projectOption;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            com.ing.engine.perf.PerfRunHandle handle = resolveRun(cli, runId, projectOption);
            if (handle == null) {
                return 1;
            }
            String phase = handle.phase();
            cli.printCallout("Run", handle.runId + " — " + phase);
            if ("DRAINING".equals(phase)) {
                cli.printInfo(
                    "Test complete. k6 is waiting for dashboard viewers to disconnect — " +
                    "close the dashboard browser tab and the summary will be flushed."
                );
            }
            if (!"FINISHED".equals(phase)) {
                java.util.Map<String, String> live = com.ing.engine.perf.K6MetricsTap.snapshot(
                    handle.apiPort
                );
                if (live.isEmpty()) {
                    cli.printInfo("Metrics API not answering yet (starting up?).");
                } else {
                    for (java.util.Map.Entry<String, String> e : live.entrySet()) {
                        System.out.println(
                            "  " + String.format("%-12s", e.getKey()) + " " + e.getValue()
                        );
                    }
                }
                if (handle.dashboardUrl() != null) {
                    cli.printInfo("Dashboard: " + handle.dashboardUrl());
                }
            } else {
                K6Runner.reconcileRunMeta(handle);
                com.fasterxml.jackson.databind.JsonNode meta = PerfReportStore.runMeta(
                    handle.runDir
                );
                printRunOutcome(
                    cli,
                    handle.runDir,
                    meta == null ? 0 : meta.path("exitCode").asInt(0),
                    meta != null && meta.path("thresholdsFailed").asBoolean(false)
                );
            }
            return 0;
        }
    }

    @Command(name = "logs", description = "Output of an async k6 run")
    public static class LogsCommand implements Callable<Integer> {
        @Parameters(index = "0", arity = "0..1", description = "Run id; default: newest running")
        private String runId;

        @Option(
            names = { "--lines" },
            description = "Tail this many lines (default 40)",
            defaultValue = "40"
        )
        private int lines;

        @Option(names = { "-p", "--project" }, description = "Project name or path")
        private String projectOption;

        @Override
        public Integer call() throws Exception {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            com.ing.engine.perf.PerfRunHandle handle = resolveRun(cli, runId, projectOption);
            if (handle == null) {
                return 1;
            }
            File log = new File(handle.runDir, "output.log");
            if (!log.isFile()) {
                cli.printWarning("No output.log for " + handle.runId);
                return 0;
            }
            java.util.List<String> all = java.nio.file.Files.readAllLines(log.toPath());
            int from = Math.max(0, all.size() - Math.max(1, lines));
            for (int i = from; i < all.size(); i++) {
                System.out.println(all.get(i));
            }
            return 0;
        }
    }

    @Command(name = "cancel", description = "Stop an async k6 run (graceful, then kill)")
    public static class CancelCommand implements Callable<Integer> {
        @Parameters(index = "0", arity = "0..1", description = "Run id; default: newest running")
        private String runId;

        @Option(names = { "-p", "--project" }, description = "Project name or path")
        private String projectOption;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            com.ing.engine.perf.PerfRunHandle handle = resolveRun(cli, runId, projectOption);
            if (handle == null) {
                return 1;
            }
            if (!handle.isAlive()) {
                cli.printInfo("Run already finished: " + handle.runId);
                return 0;
            }
            boolean down = handle.cancel();
            K6Runner.reconcileRunMeta(handle);
            if (down) {
                cli.printSuccess("Cancelled " + handle.runId);
                return 0;
            }
            cli.printError("Could not stop " + handle.runId + " (pid " + handle.pid + ")");
            return 1;
        }
    }

    @Command(name = "scale", description = "Change the VU count of a running k6 test")
    public static class ScaleCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Target number of VUs")
        private int vus;

        @Parameters(index = "1", arity = "0..1", description = "Run id; default: newest running")
        private String runId;

        @Option(names = { "-p", "--project" }, description = "Project name or path")
        private String projectOption;

        @Override
        public Integer call() {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            com.ing.engine.perf.PerfRunHandle handle = resolveRun(cli, runId, projectOption);
            if (handle == null) {
                return 1;
            }
            if (!handle.isAlive()) {
                cli.printError("Run already finished: " + handle.runId);
                return 1;
            }
            if (com.ing.engine.perf.K6MetricsTap.scale(handle.apiPort, vus)) {
                cli.printSuccess("Scaled " + handle.runId + " to " + vus + " VUs");
                return 0;
            }
            cli.printError(
                "k6 rejected the scale request (executor may not support external VU control)."
            );
            return 1;
        }
    }

    @Command(
        name = "validate",
        description = "Debug run: 1 VU, 1 iteration, full HTTP trace (k6-studio Validator)"
    )
    public static class ValidateCommand implements Callable<Integer> {
        @Parameters(index = "0", description = "Script name (Performance/scripts) or .js path")
        private String target;

        @Option(names = { "-p", "--project" }, description = "Project name or path")
        private String projectOption;

        @Option(names = { "--full" }, description = "Print the entire trace (default: tail)")
        private boolean full;

        @Override
        public Integer call() throws Exception {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            String k6 = K6Locator.resolve();
            if (k6 == null) {
                cli.printError("k6 not found. " + K6Locator.installHint());
                return 1;
            }
            File projectDir = projectFromOptions(projectOption);
            File script = resolveScript(target, projectDir);
            if (script == null) {
                cli.printError("Script not found: " + target);
                return 1;
            }
            if (projectDir == null) {
                projectDir = projectDirOfScript(script);
            }
            if (projectDir == null) {
                cli.printError("Cannot determine the project for results. Pass --project.");
                return 1;
            }
            cli.printCallout("k6 validate", script.getName() + " (1 VU, 1 iteration)");
            K6Runner.RunResult run = K6Runner.validate(k6, script, new PerfWorkspace(projectDir));
            if (run.output != null) {
                String[] lines = run.output.split("\\R");
                int from = full ? 0 : Math.max(0, lines.length - 40);
                if (from > 0) {
                    cli.printInfo(
                        "(showing last 40 lines; --full for everything; full log: " +
                        new File(run.runDir, "output.log") +
                        ")"
                    );
                }
                for (int i = from; i < lines.length; i++) {
                    System.out.println(lines[i]);
                }
            }
            printRunOutcome(cli, run.runDir, run.exitCode, run.thresholdsFailed);
            return run.exitCode == 0 ? 0 : 1;
        }
    }

    private static void printRunOutcome(
        INGeniousCLI cli,
        File runDir,
        int exitCode,
        boolean thresholdsFailed
    ) {
        java.util.Map<String, String> headline = PerfReportStore.headline(runDir);
        if (!headline.isEmpty()) {
            cli.printHeader("Summary");
            for (java.util.Map.Entry<String, String> e : headline.entrySet()) {
                System.out.println("  " + String.format("%-15s", e.getKey()) + " " + e.getValue());
            }
        }
        java.util.Map<String, Boolean> thresholds = PerfReportStore.thresholds(runDir);
        if (!thresholds.isEmpty()) {
            cli.printHeader("Thresholds");
            for (java.util.Map.Entry<String, Boolean> e : thresholds.entrySet()) {
                if (e.getValue()) {
                    cli.printSuccess(e.getKey());
                } else {
                    cli.printError(e.getKey());
                }
            }
        }
        if (exitCode == 0) {
            cli.printSuccess("Run complete. Results: " + runDir);
        } else if (thresholdsFailed) {
            cli.printError("Thresholds failed (exit 99). Results: " + runDir);
        } else {
            cli.printError("k6 exited with code " + exitCode + ". Results: " + runDir);
        }
    }

    // ==================================================================
    // perf report
    // ==================================================================

    @Command(name = "report", description = "Show performance run results")
    public static class ReportCommand implements Callable<Integer> {
        @Parameters(
            index = "0",
            arity = "0..1",
            description = "latest (default) | history | compare | junit"
        )
        private String what;

        @Parameters(
            index = "1",
            arity = "0..1",
            description = "compare: baseline run id | junit: run id (default latest)"
        )
        private String argA;

        @Parameters(index = "2", arity = "0..1", description = "compare: candidate run id")
        private String argB;

        @Option(names = { "--out" }, description = "junit: write the XML here (default stdout)")
        private String out;

        @Option(names = { "-p", "--project" }, description = "Project name or path")
        private String projectOption;

        @Override
        public Integer call() throws Exception {
            INGeniousCLI cli = INGeniousCLI.getInstance();
            File projectDir = projectFromOptions(projectOption);
            if (projectDir == null) {
                cli.printError("Project required. Use --project <name|path>.");
                return 1;
            }
            PerfWorkspace ws = new PerfWorkspace(projectDir);
            String mode = what == null ? "latest" : what.toLowerCase();
            if ("compare".equals(mode)) {
                return compare(cli, ws);
            }
            if ("junit".equals(mode)) {
                return junit(cli, ws);
            }
            if ("history".equals(mode)) {
                List<File> runs = ws.listRuns();
                if (runs.isEmpty()) {
                    cli.printWarning("No performance runs yet.");
                    return 0;
                }
                List<String> headers = Arrays.asList("Script", "Timestamp", "Exit", "Profile");
                List<List<String>> rows = new ArrayList<>();
                for (File run : runs) {
                    com.fasterxml.jackson.databind.JsonNode meta = PerfReportStore.runMeta(run);
                    rows.add(
                        Arrays.asList(
                            run.getParentFile().getName(),
                            run.getName(),
                            meta == null ? "?" : String.valueOf(meta.path("exitCode").asInt()),
                            meta == null ? "?" : meta.path("profile").asText("?")
                        )
                    );
                }
                System.out.println(cli.getOutputFormatter().formatTable(headers, rows));
                return 0;
            }
            File latest = PerfReportStore.latestRunDir(ws);
            if (latest == null) {
                cli.printWarning("No performance runs yet. Run: ingenious perf run <script>");
                return 0;
            }
            cli.printCallout(
                "Latest run",
                latest.getParentFile().getName() + "/" + latest.getName()
            );
            com.fasterxml.jackson.databind.JsonNode meta = PerfReportStore.runMeta(latest);
            if (meta != null) {
                cli.printInfo(
                    "profile: " +
                    meta.path("profile").asText("?") +
                    ", started: " +
                    meta.path("startedAt").asText("?") +
                    ", " +
                    meta.path("k6Version").asText("")
                );
            }
            printRunOutcome(
                cli,
                latest,
                meta == null ? 0 : meta.path("exitCode").asInt(0),
                meta != null && meta.path("thresholdsFailed").asBoolean(false)
            );
            return 0;
        }

        /** Baseline-vs-candidate diff; nonzero exit on regressions (CI gate). */
        private Integer compare(INGeniousCLI cli, PerfWorkspace ws) {
            if (argA == null || argB == null) {
                cli.printError(
                    "Usage: ingenious perf report compare <baselineRunId> <candidateRunId>" +
                    " (ids from 'perf report history': <script>/<timestamp>)"
                );
                return 1;
            }
            File baseline = new File(ws.resultsDir(), argA);
            File candidate = new File(ws.resultsDir(), argB);
            if (!new File(baseline, "summary.json").isFile()) {
                cli.printError("No summary.json for baseline: " + argA);
                return 1;
            }
            if (!new File(candidate, "summary.json").isFile()) {
                cli.printError("No summary.json for candidate: " + argB);
                return 1;
            }
            List<String> thresholdRegressions = new ArrayList<>();
            List<PerfReportStore.CompareRow> rows = PerfReportStore.compare(
                baseline,
                candidate,
                thresholdRegressions
            );
            List<String> headers = Arrays.asList("Metric", "Baseline", "Candidate", "Delta");
            List<List<String>> table = new ArrayList<>();
            boolean anyRegression = !thresholdRegressions.isEmpty();
            for (PerfReportStore.CompareRow row : rows) {
                anyRegression |= row.regression;
                table.add(
                    Arrays.asList(
                        row.metric,
                        formatMetric(row.metric, row.baseline),
                        formatMetric(row.metric, row.candidate),
                        String.format(
                            java.util.Locale.ROOT,
                            "%+.1f%%%s",
                            row.deltaPercent,
                            row.regression ? "  << REGRESSION" : ""
                        )
                    )
                );
            }
            cli.printCallout("Compare", argA + "  ->  " + argB);
            System.out.println(cli.getOutputFormatter().formatTable(headers, table));
            for (String t : thresholdRegressions) {
                cli.printError("Threshold regressed: " + t);
            }
            if (anyRegression) {
                cli.printError("Performance regression detected.");
                return 1;
            }
            cli.printSuccess("No regressions.");
            return 0;
        }

        private static String formatMetric(String metric, double value) {
            if ("errorRate".equals(metric)) {
                return String.format(java.util.Locale.ROOT, "%.2f%%", value * 100);
            }
            if ("avg".equals(metric) || "p95".equals(metric) || "max".equals(metric)) {
                return String.format(java.util.Locale.ROOT, "%.1f ms", value);
            }
            return String.format(java.util.Locale.ROOT, "%.1f", value);
        }

        /** JUnit XML of the thresholds — CI-native performance gates. */
        private Integer junit(INGeniousCLI cli, PerfWorkspace ws) throws Exception {
            File run = argA != null
                ? new File(ws.resultsDir(), argA)
                : PerfReportStore.latestRunDir(ws);
            if (run == null || !new File(run, "summary.json").isFile()) {
                cli.printError("No run summary found" + (argA == null ? "." : ": " + argA));
                return 1;
            }
            String xml = PerfReportStore.toJUnitXml(
                run,
                "k6 " + run.getParentFile().getName() + "/" + run.getName()
            );
            if (out != null) {
                java.nio.file.Files.write(
                    new File(out).toPath(),
                    xml.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                );
                cli.printSuccess("Wrote " + out);
            } else {
                System.out.print(xml);
            }
            return 0;
        }
    }
}
