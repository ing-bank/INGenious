package com.ing.engine.perf;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import org.testng.annotations.Test;

/**
 * Conformance for profile materialization (built-ins as editable YAML) and
 * the profile-driven script refresher (edits reflect in the rendered JS).
 */
public class PerfScriptRefresherTest {

    private static File tempProject() throws Exception {
        return Files.createTempDirectory("perf-refresh-test").toFile();
    }

    private static void deleteRecursively(File f) {
        File[] children = f.listFiles();
        if (children != null) {
            for (File c : children) {
                deleteRecursively(c);
            }
        }
        f.delete();
    }

    private static final String HAR =
        "{ \"log\": { \"entries\": [" +
        "{ \"request\": { \"method\": \"GET\", \"url\": \"https://api.example.com/users\", \"headers\": [] }," +
        "  \"response\": {\"status\": 200, \"content\": {\"mimeType\": \"application/json\"}} }" +
        "] } }";

    @Test
    public void ensureMaterializesBuiltInsAsYamlWithoutClobberingEdits() throws Exception {
        File projectDir = tempProject();
        try {
            PerfWorkspace ws = new PerfWorkspace(projectDir);
            ws.ensure();
            for (String name : new String[] { "smoke", "average", "stress", "spike", "soak" }) {
                assertThat(new File(ws.profilesDir(), name + ".yaml"))
                    .as("materialized %s", name)
                    .isFile();
            }
            // resolution now reads the YAML (project scope)
            assertThat(PerfProfile.resolve("smoke", projectDir).builtIn).isFalse();
            // edit smoke: 1m instead of 30s, p95<1000 instead of 500
            File smoke = new File(ws.profilesDir(), "smoke.yaml");
            String edited = new String(Files.readAllBytes(smoke.toPath()), StandardCharsets.UTF_8)
                .replace("\"30s\"", "\"1m\"")
                .replace("p(95)<500", "p(95)<1000");
            Files.write(smoke.toPath(), edited.getBytes(StandardCharsets.UTF_8));
            // a second ensure() must NOT overwrite the edit
            ws.ensure();
            PerfProfile resolved = PerfProfile.resolve("smoke", projectDir);
            assertThat(resolved.duration).isEqualTo("1m");
            assertThat(resolved.thresholds.get("http_req_duration")).containsExactly("p(95)<1000");
        } finally {
            deleteRecursively(projectDir);
        }
    }

    @Test
    public void provenanceExposesSourceAndType() throws Exception {
        File dir = tempProject();
        try {
            File script = new File(dir, "s.js");
            String content = ScriptProvenance.wrap(
                "TestPlan/APIBasics/CreatePost",
                "ingenious perf export \"P/APIBasics/CreatePost\" --type browser --profile smoke",
                "smoke",
                "body();\n"
            );
            Files.write(script.toPath(), content.getBytes(StandardCharsets.UTF_8));
            assertThat(ScriptProvenance.sourceOf(script))
                .isEqualTo("TestPlan/APIBasics/CreatePost");
            assertThat(ScriptProvenance.typeOf(script)).isEqualTo("browser");
            // non-generated file
            File plain = new File(dir, "plain.js");
            Files.write(plain.toPath(), "x();".getBytes(StandardCharsets.UTF_8));
            assertThat(ScriptProvenance.sourceOf(plain)).isNull();
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    public void refreshRegeneratesHarScriptWithNewProfileOptions() throws Exception {
        File projectDir = tempProject();
        try {
            PerfWorkspace ws = new PerfWorkspace(projectDir);
            ws.ensure();
            File har = new File(ws.recordingsDir(), "rec.har");
            Files.write(har.toPath(), HAR.getBytes(StandardCharsets.UTF_8));
            // initial export with smoke (30s by default from materialized yaml)
            HarReader.Result read = HarReader.read(har, null, false);
            PerfProfile smoke = PerfProfile.resolve("smoke", projectDir);
            String script = K6HttpScriptGenerator.generate(
                "rec.har",
                "ingenious perf export \"" + har.getPath() + "\" --type http --profile smoke",
                smoke,
                read.requests,
                read.warnings
            );
            File scriptFile = new File(ws.scriptsDir(), "rec.js");
            Files.write(scriptFile.toPath(), script.getBytes(StandardCharsets.UTF_8));
            assertThat(script).contains("\"duration\" : \"30s\"");

            // edit the smoke profile: duration 2m, looser threshold
            File smokeYaml = new File(ws.profilesDir(), "smoke.yaml");
            String edited = new String(
                Files.readAllBytes(smokeYaml.toPath()),
                StandardCharsets.UTF_8
            )
                .replace("\"30s\"", "\"2m\"")
                .replace("p(95)<500", "p(95)<1000");
            Files.write(smokeYaml.toPath(), edited.getBytes(StandardCharsets.UTF_8));

            PerfScriptRefresher.Result result = PerfScriptRefresher.refresh(
                null,
                projectDir,
                scriptFile,
                PerfProfile.resolve("smoke", projectDir)
            );
            assertThat(result.refreshed).as(result.reason).isTrue();
            String refreshed = new String(
                Files.readAllBytes(scriptFile.toPath()),
                StandardCharsets.UTF_8
            );
            assertThat(refreshed).contains("\"duration\" : \"2m\"");
            assertThat(refreshed).contains("p(95)<1000");
            // still a valid generated (non-hand-edited) script
            assertThat(ScriptProvenance.isHandEdited(scriptFile)).isFalse();
        } finally {
            deleteRecursively(projectDir);
        }
    }

    @Test
    public void refreshRefusesHandEditedAndHandWrittenScripts() throws Exception {
        File projectDir = tempProject();
        try {
            PerfWorkspace ws = new PerfWorkspace(projectDir);
            ws.ensure();
            PerfProfile smoke = PerfProfile.resolve("smoke", projectDir);
            // hand-written
            File plain = new File(ws.scriptsDir(), "hand.js");
            Files.write(
                plain.toPath(),
                "export default function(){}".getBytes(StandardCharsets.UTF_8)
            );
            assertThat(PerfScriptRefresher.refresh(null, projectDir, plain, smoke).refreshed)
                .isFalse();
            // generated but hand-edited
            HttpRequestSpec spec = new HttpRequestSpec();
            spec.method = "get";
            spec.url = "https://x.test/a";
            spec.name = "GET /a";
            java.util.List<HttpRequestSpec> reqs = new ArrayList<>();
            reqs.add(spec);
            String script = K6HttpScriptGenerator.generate(
                "rec.har",
                null,
                smoke,
                reqs,
                new ArrayList<String>()
            );
            File edited = new File(ws.scriptsDir(), "edited.js");
            Files.write(
                edited.toPath(),
                (script + "\n// my manual tweak\n").getBytes(StandardCharsets.UTF_8)
            );
            PerfScriptRefresher.Result result = PerfScriptRefresher.refresh(
                null,
                projectDir,
                edited,
                smoke
            );
            assertThat(result.refreshed).isFalse();
            assertThat(result.reason).contains("hand-edited");
        } finally {
            deleteRecursively(projectDir);
        }
    }
}
