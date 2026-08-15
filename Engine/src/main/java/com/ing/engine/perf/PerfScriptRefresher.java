package com.ing.engine.perf;

import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Re-renders a generated k6 script from its provenance source with the
 * current load profile, so that profile edits (duration, VUs, thresholds)
 * reflect immediately in the emitted JS without a manual re-export.
 *
 * <p>Safety rules:
 * <ul>
 *   <li>only scripts carrying the provenance marker are touched</li>
 *   <li>hand-edited scripts are never overwritten</li>
 *   <li>HAR-sourced scripts re-apply their persisted rules file</li>
 * </ul>
 */
public final class PerfScriptRefresher {

    /** Outcome: refreshed or skipped (with the reason). */
    public static final class Result {
        public final boolean refreshed;
        public final String reason;

        private Result(boolean refreshed, String reason) {
            this.refreshed = refreshed;
            this.reason = reason;
        }
    }

    private PerfScriptRefresher() {}

    /**
     * Regenerate {@code script} in place using {@code profile}.
     *
     * @param project Datalib project (required for test-case sources; may be
     *                null for HAR sources)
     */
    public static Result refresh(
        Project project,
        File projectDir,
        File script,
        PerfProfile profile
    ) {
        if (!ScriptProvenance.isGenerated(script)) {
            return new Result(false, "not a generated script (hand-written)");
        }
        if (ScriptProvenance.isHandEdited(script)) {
            return new Result(false, "script was hand-edited; not overwriting");
        }
        String source = ScriptProvenance.sourceOf(script);
        if (source == null) {
            return new Result(false, "no source recorded in the script header");
        }
        try {
            String content;
            if (source.toLowerCase().endsWith(".har")) {
                content = regenerateFromHar(projectDir, script, source, profile);
            } else if (source.startsWith("TestPlan/")) {
                if (project == null) {
                    return new Result(false, "project model required for test-case sources");
                }
                content = regenerateFromTestCase(project, projectDir, script, source, profile);
            } else {
                return new Result(false, "unknown source kind: " + source);
            }
            if (content == null) {
                return new Result(false, "source no longer resolvable: " + source);
            }
            Files.write(script.toPath(), content.getBytes(StandardCharsets.UTF_8));
            return new Result(true, "regenerated from " + source + " with profile " + profile.name);
        } catch (Exception e) {
            return new Result(false, "refresh failed: " + e.getMessage());
        }
    }

    private static String regenerateFromTestCase(
        Project project,
        File projectDir,
        File script,
        String source,
        PerfProfile profile
    ) {
        String[] parts = source.split("/");
        if (parts.length != 3) {
            return null;
        }
        Scenario scenario = project.getScenarioByName(parts[1]);
        TestCase testCase = scenario == null ? null : scenario.getTestCaseByName(parts[2]);
        if (testCase == null) {
            return null;
        }
        boolean browser = "browser".equals(ScriptProvenance.typeOf(script));
        String regenerate =
            "ingenious perf export \"" +
            projectDir.getName() +
            "/" +
            parts[1] +
            "/" +
            parts[2] +
            "\" --type " +
            (browser ? "browser" : "http") +
            " --profile " +
            profile.name;
        if (browser) {
            K6BrowserScriptGenerator.Result gen = K6BrowserScriptGenerator.fromTestCase(
                project,
                testCase
            );
            if (gen.actions == 0) {
                return null;
            }
            return K6BrowserScriptGenerator.generate(
                source,
                regenerate,
                profile,
                gen.lines,
                gen.warnings
            );
        }
        K6HttpScriptGenerator.Result gen = K6HttpScriptGenerator.fromTestCase(project, testCase);
        if (gen.requests.isEmpty()) {
            return null;
        }
        return K6HttpScriptGenerator.generate(
            source,
            regenerate,
            profile,
            gen.requests,
            gen.warnings
        );
    }

    private static String regenerateFromHar(
        File projectDir,
        File script,
        String source,
        PerfProfile profile
    )
        throws Exception {
        PerfWorkspace workspace = new PerfWorkspace(projectDir);
        File har = new File(workspace.recordingsDir(), source);
        if (!har.isFile()) {
            har = new File(source); // header may carry an absolute path
        }
        if (!har.isFile()) {
            return null;
        }
        HarReader.Result read = HarReader.read(har, null, false);
        if (read.requests.isEmpty()) {
            return null;
        }
        String baseName = script.getName().replaceAll("\\.js$", "");
        RuleEngine.Result appliedRules = null;
        List<PerfRule> rules = PerfRule.load(PerfRule.defaultRulesFile(workspace, baseName));
        if (!rules.isEmpty()) {
            appliedRules = RuleEngine.apply(read.requests, rules);
            read.warnings.addAll(appliedRules.warnings);
        }
        String regenerate =
            "ingenious perf export \"" + har.getPath() + "\" --type http --profile " + profile.name;
        return K6HttpScriptGenerator.generate(
            har.getName(),
            regenerate,
            profile,
            read.requests,
            read.warnings,
            appliedRules
        );
    }
}
