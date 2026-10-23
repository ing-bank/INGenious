package com.ing.engine.perf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A load profile: how many virtual users, for how long, with which pass/fail
 * thresholds. Profiles translate 1:1 into the k6 {@code options} export of a
 * generated script.
 *
 * <p>Two executor styles are supported in v1:
 * <ul>
 *   <li>{@code constant-vus} — fixed {@link #vus} for {@link #duration}</li>
 *   <li>{@code ramping-vus}  — a sequence of {@link Stage} ramps</li>
 * </ul>
 *
 * <p>Custom profiles are persisted as YAML under
 * {@code <Project>/Performance/profiles/<name>.yaml}; five built-ins
 * (smoke / average / stress / spike / soak) always exist.
 */
public final class PerfProfile {

    /** One ramping step: run towards {@code target} VUs over {@code duration}. */
    public static final class Stage {
        public final String duration;
        public final int target;

        public Stage(String duration, int target) {
            this.duration = duration;
            this.target = target;
        }
    }

    public final String name;
    public final String description;
    /** "constant-vus" or "ramping-vus". */
    public final String executor;
    public final int vus;
    public final String duration;
    public final List<Stage> stages;
    /** metric name -> threshold expressions, e.g. http_req_duration -> ["p(95)<500"]. */
    public final Map<String, List<String>> thresholds;
    /** True for the profiles shipped with INGenious. */
    public final boolean builtIn;

    public PerfProfile(
        String name,
        String description,
        String executor,
        int vus,
        String duration,
        List<Stage> stages,
        Map<String, List<String>> thresholds,
        boolean builtIn
    ) {
        this.name = name;
        this.description = description;
        this.executor = executor;
        this.vus = vus;
        this.duration = duration;
        this.stages = stages == null ? new ArrayList<Stage>() : stages;
        this.thresholds =
            thresholds == null ? new LinkedHashMap<String, List<String>>() : thresholds;
        this.builtIn = builtIn;
    }

    // ------------------------------------------------------------------
    // built-ins
    // ------------------------------------------------------------------

    public static List<PerfProfile> builtIns() {
        List<PerfProfile> out = new ArrayList<>();
        out.add(
            new PerfProfile(
                "smoke",
                "Minimal load: 1 VU for 30s. Verifies the script works and the system responds.",
                "constant-vus",
                1,
                "30s",
                null,
                defaultThresholds(),
                true
            )
        );
        List<Stage> average = new ArrayList<>();
        average.add(new Stage("1m", 20));
        average.add(new Stage("3m", 20));
        average.add(new Stage("1m", 0));
        out.add(
            new PerfProfile(
                "average",
                "Typical day traffic: ramp to 20 VUs, hold 3m, ramp down.",
                "ramping-vus",
                0,
                null,
                average,
                defaultThresholds(),
                true
            )
        );
        List<Stage> stress = new ArrayList<>();
        stress.add(new Stage("2m", 50));
        stress.add(new Stage("5m", 50));
        stress.add(new Stage("2m", 100));
        stress.add(new Stage("5m", 100));
        stress.add(new Stage("2m", 0));
        out.add(
            new PerfProfile(
                "stress",
                "Beyond-normal load: step up to 50 then 100 VUs to find the breaking point.",
                "ramping-vus",
                0,
                null,
                stress,
                defaultThresholds(),
                true
            )
        );
        List<Stage> spike = new ArrayList<>();
        spike.add(new Stage("15s", 100));
        spike.add(new Stage("1m", 100));
        spike.add(new Stage("15s", 0));
        out.add(
            new PerfProfile(
                "spike",
                "Sudden surge: jump to 100 VUs almost instantly, hold 1m, drop.",
                "ramping-vus",
                0,
                null,
                spike,
                defaultThresholds(),
                true
            )
        );
        List<Stage> soak = new ArrayList<>();
        soak.add(new Stage("2m", 10));
        soak.add(new Stage("30m", 10));
        soak.add(new Stage("2m", 0));
        out.add(
            new PerfProfile(
                "soak",
                "Endurance: 10 VUs for 30m to expose leaks and slow degradation.",
                "ramping-vus",
                0,
                null,
                soak,
                defaultThresholds(),
                true
            )
        );
        return out;
    }

    public static PerfProfile builtIn(String name) {
        for (PerfProfile p : builtIns()) {
            if (p.name.equalsIgnoreCase(name)) {
                return p;
            }
        }
        return null;
    }

    /**
     * Write every built-in profile that does not yet exist as
     * {@code Performance/profiles/<name>.yaml} so users can edit the load
     * shape and thresholds directly. Existing files (i.e. user-customized
     * profiles) are left untouched. Resolution prefers these YAML files over
     * the in-code defaults, so edits take effect everywhere.
     */
    public static void materializeBuiltIns(PerfWorkspace workspace) {
        for (PerfProfile builtIn : builtIns()) {
            File target = new File(workspace.profilesDir(), builtIn.name + ".yaml");
            File targetYml = new File(workspace.profilesDir(), builtIn.name + ".yml");
            if (target.exists() || targetYml.exists()) {
                continue;
            }
            try {
                builtIn.saveTo(target);
            } catch (Exception e) {
                // best effort — the in-code built-in remains the fallback
            }
        }
    }

    private static Map<String, List<String>> defaultThresholds() {
        Map<String, List<String>> t = new LinkedHashMap<>();
        List<String> dur = new ArrayList<>();
        dur.add("p(95)<500");
        t.put("http_req_duration", dur);
        List<String> fail = new ArrayList<>();
        fail.add("rate<0.01");
        t.put("http_req_failed", fail);
        return t;
    }

    /**
     * Resolve a profile by name: project-level YAML first
     * ({@code <project>/Performance/profiles/<name>.yaml}), then built-ins.
     * Returns {@code null} when neither exists.
     */
    public static PerfProfile resolve(String name, File projectDir) {
        if (projectDir != null) {
            PerfWorkspace ws = new PerfWorkspace(projectDir);
            for (String ext : new String[] { ".yaml", ".yml" }) {
                File f = new File(ws.profilesDir(), name + ext);
                if (f.isFile()) {
                    try {
                        return fromYaml(f);
                    } catch (Exception e) {
                        throw new IllegalArgumentException(
                            "Invalid profile YAML: " + f + " (" + e.getMessage() + ")"
                        );
                    }
                }
            }
        }
        return builtIn(name);
    }

    // ------------------------------------------------------------------
    // YAML persistence
    // ------------------------------------------------------------------

    public static PerfProfile fromYaml(File file) throws Exception {
        JsonNode root = new YAMLMapper().readTree(file);
        if (root == null || !root.isObject()) {
            throw new IllegalArgumentException("Profile YAML must be a mapping");
        }
        String name = root.path("name").asText(stripExtension(file.getName()));
        String description = root.path("description").asText("");
        String executor = root
            .path("executor")
            .asText(root.has("stages") ? "ramping-vus" : "constant-vus");
        int vus = root.path("vus").asInt(1);
        String duration = root.hasNonNull("duration") ? root.get("duration").asText() : null;
        List<Stage> stages = new ArrayList<>();
        if (root.has("stages") && root.get("stages").isArray()) {
            for (JsonNode s : root.get("stages")) {
                stages.add(new Stage(s.path("duration").asText("1m"), s.path("target").asInt(0)));
            }
        }
        Map<String, List<String>> thresholds = new LinkedHashMap<>();
        JsonNode th = root.get("thresholds");
        if (th != null && th.isObject()) {
            Iterator<String> it = th.fieldNames();
            while (it.hasNext()) {
                String metric = it.next();
                List<String> exprs = new ArrayList<>();
                JsonNode v = th.get(metric);
                if (v.isArray()) {
                    for (JsonNode e : v) {
                        exprs.add(e.asText());
                    }
                } else {
                    exprs.add(v.asText());
                }
                thresholds.put(metric, exprs);
            }
        }
        return new PerfProfile(
            name,
            description,
            executor,
            vus,
            duration,
            stages,
            thresholds,
            false
        );
    }

    public void saveTo(File file) throws Exception {
        YAMLMapper yaml = new YAMLMapper();
        file.getParentFile().mkdirs();
        yaml.writerWithDefaultPrettyPrinter().writeValue(file, toYamlNode(yaml));
    }

    /** Full YAML representation (also used by "profile show"). */
    public JsonNode toYamlNode(ObjectMapper mapper) {
        ObjectNode root = mapper.createObjectNode();
        root.put("name", name);
        root.put("description", description);
        root.put("executor", executor);
        if ("constant-vus".equals(executor)) {
            root.put("vus", vus);
            if (duration != null) {
                root.put("duration", duration);
            }
        } else {
            ArrayNode arr = root.putArray("stages");
            for (Stage s : stages) {
                ObjectNode n = arr.addObject();
                n.put("duration", s.duration);
                n.put("target", s.target);
            }
        }
        ObjectNode th = root.putObject("thresholds");
        for (Map.Entry<String, List<String>> e : thresholds.entrySet()) {
            ArrayNode arr = th.putArray(e.getKey());
            for (String expr : e.getValue()) {
                arr.add(expr);
            }
        }
        return root;
    }

    /**
     * The k6 {@code options} object for this profile (shorthand form:
     * vus/duration or stages at top level, plus thresholds).
     */
    public JsonNode toOptionsNode(ObjectMapper mapper) {
        ObjectNode options = mapper.createObjectNode();
        if ("constant-vus".equals(executor)) {
            options.put("vus", vus);
            if (duration != null) {
                options.put("duration", duration);
            }
        } else {
            ArrayNode arr = options.putArray("stages");
            for (Stage s : stages) {
                ObjectNode n = arr.addObject();
                n.put("duration", s.duration);
                n.put("target", s.target);
            }
        }
        if (!thresholds.isEmpty()) {
            ObjectNode th = options.putObject("thresholds");
            for (Map.Entry<String, List<String>> e : thresholds.entrySet()) {
                ArrayNode arr = th.putArray(e.getKey());
                for (String expr : e.getValue()) {
                    arr.add(expr);
                }
            }
        }
        return options;
    }

    /** One-line human summary, e.g. "1 VU x 30s" or "ramp 1m->20, 3m->20, 1m->0". */
    public String summarize() {
        if ("constant-vus".equals(executor)) {
            return vus + " VU" + (vus == 1 ? "" : "s") + (duration == null ? "" : " x " + duration);
        }
        StringBuilder sb = new StringBuilder("ramp ");
        for (int i = 0; i < stages.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Stage s = stages.get(i);
            sb.append(s.duration).append("->").append(s.target);
        }
        return sb.toString();
    }

    private static String stripExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
