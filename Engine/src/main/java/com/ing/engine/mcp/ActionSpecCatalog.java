package com.ing.engine.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ing.engine.constants.FilePath;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Single lookup point for per-action {@link ArgSpec} format specifications,
 * shared by the IDE IntelliSense and the AI step-authoring tools so both behave
 * identically.
 *
 * <p>A spec is resolved in priority order:
 * <ol>
 *   <li>a sidecar override ({@code Configuration/action-argspec.json}) - lets
 *       plugin/third-party actions be specced without touching their source;</li>
 *   <li>the action's {@link com.ing.ingenious.api.annotation.Args @Args}
 *       annotation (via {@link ActionCatalog});</li>
 *   <li>an inferred free-text fallback so an un-specced action is never rejected.</li>
 * </ol>
 */
public final class ActionSpecCatalog {

    private ActionSpecCatalog() {}

    private static volatile Map<String, ArgSpec> SIDECAR;

    // ------------------------------------------------------------------
    // public API
    // ------------------------------------------------------------------

    /** Resolve the spec for {@code action}; never {@code null}. */
    public static ArgSpec forAction(String action) {
        if (action == null || action.isEmpty()) {
            return ArgSpec.inferred("");
        }
        ArgSpec sc = sidecar().get(action);
        if (sc != null) {
            return sc;
        }
        ActionCatalog.ActionInfo info = ActionCatalog.find(action);
        if (info != null && info.argSpec != null) {
            return info.argSpec;
        }
        return ArgSpec.inferred(action);
    }

    /** True when {@code action} has an explicit spec (annotation or sidecar). */
    public static boolean hasSpec(String action) {
        if (action == null || action.isEmpty()) {
            return false;
        }
        if (sidecar().containsKey(action)) {
            return true;
        }
        ActionCatalog.ActionInfo info = ActionCatalog.find(action);
        return info != null && info.argSpec != null;
    }

    /** Every discovered action's resolved spec. */
    public static List<ArgSpec> all() {
        List<ArgSpec> out = new ArrayList<>();
        for (ActionCatalog.ActionInfo a : ActionCatalog.all()) {
            out.add(forAction(a.name));
        }
        return out;
    }

    /** Names of discovered actions that lack an explicit spec (coverage report). */
    public static List<String> unspecifiedActions() {
        List<String> out = new ArrayList<>();
        for (ActionCatalog.ActionInfo a : ActionCatalog.all()) {
            if (!hasSpec(a.name)) {
                out.add(a.name);
            }
        }
        Collections.sort(out);
        return out;
    }

    // ------------------------------------------------------------------
    // sidecar loading (plugin / third-party overrides)
    // ------------------------------------------------------------------

    private static Map<String, ArgSpec> sidecar() {
        Map<String, ArgSpec> s = SIDECAR;
        if (s != null) {
            return s;
        }
        synchronized (ActionSpecCatalog.class) {
            if (SIDECAR != null) {
                return SIDECAR;
            }
            SIDECAR = Collections.unmodifiableMap(loadSidecar());
            return SIDECAR;
        }
    }

    private static Map<String, ArgSpec> loadSidecar() {
        Map<String, ArgSpec> map = new LinkedHashMap<>();
        for (File f : sidecarFiles()) {
            try {
                if (f == null || !f.isFile()) {
                    continue;
                }
                JsonNode root = new ObjectMapper().readTree(f);
                JsonNode actions = root.has("actions") ? root.get("actions") : root;
                if (actions == null || !actions.isObject()) {
                    continue;
                }
                Iterator<String> names = actions.fieldNames();
                while (names.hasNext()) {
                    String name = names.next();
                    ArgSpec spec = parseSpec(name, actions.get(name));
                    if (spec != null) {
                        map.put(name, spec);
                    }
                }
            } catch (Exception ignored) {
                // A malformed sidecar must never break the catalog.
            }
        }
        return map;
    }

    private static List<File> sidecarFiles() {
        List<File> files = new ArrayList<>();
        try {
            String root = FilePath.getAppRoot();
            if (root != null) {
                files.add(new File(root, "Configuration/action-argspec.json"));
                File plugins = new File(root, "plugins");
                File[] jars = plugins.listFiles();
                if (jars != null) {
                    for (File p : jars) {
                        if (p.isDirectory()) {
                            files.add(new File(p, "action-argspec.json"));
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
            // FilePath may be unavailable outside a running engine; ignore.
        }
        return files;
    }

    private static ArgSpec parseSpec(String name, JsonNode n) {
        if (n == null || !n.isObject()) {
            return null;
        }
        ArgType input = parseArgType(text(n, "input"), ArgType.TEXT);
        String inputExample = text(n, "inputExample");
        boolean allowsData = !n.has("inputAllowsData") || n.get("inputAllowsData").asBoolean(true);
        ArgType secondObject = parseArgType(text(n, "secondObject"), ArgType.TEXT);
        ConditionKind cond = parseConditionKind(text(n, "condition"), ConditionKind.NONE);
        List<String> values = new ArrayList<>();
        if (n.has("conditionValues") && n.get("conditionValues").isArray()) {
            for (JsonNode v : n.get("conditionValues")) {
                values.add(v.asText());
            }
        }
        String condExample = text(n, "conditionExample");
        String help = text(n, "help");
        String inputHelp = text(n, "inputHelp");
        String conditionHelp = text(n, "conditionHelp");
        return new ArgSpec(
            name,
            input,
            inputExample,
            allowsData,
            secondObject,
            cond,
            values,
            condExample,
            help,
            inputHelp,
            conditionHelp,
            true
        );
    }

    private static String text(JsonNode n, String field) {
        return n.has(field) ? n.get(field).asText("") : "";
    }

    private static ArgType parseArgType(String v, ArgType fallback) {
        if (v == null || v.isEmpty()) {
            return fallback;
        }
        try {
            return ArgType.valueOf(v.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }

    private static ConditionKind parseConditionKind(String v, ConditionKind fallback) {
        if (v == null || v.isEmpty()) {
            return fallback;
        }
        try {
            return ConditionKind.valueOf(v.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return fallback;
        }
    }
}
