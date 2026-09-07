package com.ing.engine.perf;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.testng.annotations.Test;

/**
 * Phase 0 conformance for the Performance Studio foundations: built-in
 * profiles, YAML round-trip, k6 options emission and workspace layout.
 */
public class PerfProfileTest {

    @Test
    public void builtInsArePresentAndWellFormed() {
        List<PerfProfile> all = PerfProfile.builtIns();
        assertThat(all)
            .extracting(p -> p.name)
            .containsExactly("smoke", "average", "stress", "spike", "soak");
        for (PerfProfile p : all) {
            assertThat(p.builtIn).isTrue();
            assertThat(p.description).isNotEmpty();
            assertThat(p.thresholds).containsKeys("http_req_duration", "http_req_failed");
            if ("constant-vus".equals(p.executor)) {
                assertThat(p.vus).isPositive();
                assertThat(p.duration).isNotNull();
            } else {
                assertThat(p.executor).isEqualTo("ramping-vus");
                assertThat(p.stages).isNotEmpty();
            }
        }
    }

    @Test
    public void builtInLookupIsCaseInsensitive() {
        assertThat(PerfProfile.builtIn("SMOKE")).isNotNull();
        assertThat(PerfProfile.builtIn("nope")).isNull();
    }

    @Test
    public void smokeOptionsNodeUsesShorthand() {
        PerfProfile smoke = PerfProfile.builtIn("smoke");
        JsonNode options = smoke.toOptionsNode(new ObjectMapper());
        assertThat(options.get("vus").asInt()).isEqualTo(1);
        assertThat(options.get("duration").asText()).isEqualTo("30s");
        assertThat(options.get("thresholds").get("http_req_duration").get(0).asText())
            .isEqualTo("p(95)<500");
        assertThat(options.has("stages")).isFalse();
    }

    @Test
    public void rampingOptionsNodeEmitsStages() {
        PerfProfile average = PerfProfile.builtIn("average");
        JsonNode options = average.toOptionsNode(new ObjectMapper());
        assertThat(options.has("vus")).isFalse();
        assertThat(options.get("stages")).hasSize(3);
        assertThat(options.get("stages").get(0).get("target").asInt()).isEqualTo(20);
    }

    @Test
    public void yamlRoundTripPreservesEverything() throws Exception {
        File dir = Files.createTempDirectory("perf-profile-test").toFile();
        try {
            PerfProfile stress = PerfProfile.builtIn("stress");
            File file = new File(dir, "custom.yaml");
            PerfProfile custom = new PerfProfile(
                "custom",
                "my profile",
                stress.executor,
                stress.vus,
                stress.duration,
                stress.stages,
                stress.thresholds,
                false
            );
            custom.saveTo(file);
            PerfProfile loaded = PerfProfile.fromYaml(file);
            assertThat(loaded.name).isEqualTo("custom");
            assertThat(loaded.description).isEqualTo("my profile");
            assertThat(loaded.executor).isEqualTo("ramping-vus");
            assertThat(loaded.stages).hasSize(stress.stages.size());
            assertThat(loaded.stages.get(2).target).isEqualTo(100);
            assertThat(loaded.thresholds).isEqualTo(stress.thresholds);
            assertThat(loaded.builtIn).isFalse();
        } finally {
            deleteRecursively(dir);
        }
    }

    @Test
    public void resolvePrefersProjectProfileOverBuiltIn() throws Exception {
        File projectDir = Files.createTempDirectory("perf-project-test").toFile();
        try {
            PerfWorkspace ws = new PerfWorkspace(projectDir);
            ws.ensure();
            assertThat(ws.scriptsDir()).isDirectory();
            assertThat(ws.recordingsDir()).isDirectory();
            // shadow the built-in "smoke" with a project-level one
            PerfProfile shadow = new PerfProfile(
                "smoke",
                "project override",
                "constant-vus",
                7,
                "10s",
                null,
                null,
                false
            );
            shadow.saveTo(new File(ws.profilesDir(), "smoke.yaml"));
            PerfProfile resolved = PerfProfile.resolve("smoke", projectDir);
            assertThat(resolved.vus).isEqualTo(7);
            assertThat(resolved.builtIn).isFalse();
            // unknown falls through to null
            assertThat(PerfProfile.resolve("does-not-exist", projectDir)).isNull();
            // without a project dir the built-in wins
            assertThat(PerfProfile.resolve("smoke", null).vus).isEqualTo(1);
        } finally {
            deleteRecursively(projectDir);
        }
    }

    @Test
    public void summariesAreHumanReadable() {
        assertThat(PerfProfile.builtIn("smoke").summarize()).isEqualTo("1 VU x 30s");
        assertThat(PerfProfile.builtIn("average").summarize())
            .isEqualTo("ramp 1m->20, 3m->20, 1m->0");
    }

    @Test
    public void installHintMentionsAnInstaller() {
        assertThat(K6Locator.installHint()).containsAnyOf("brew", "winget", "grafana.com");
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
}
