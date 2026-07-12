package com.ing.engine.mcp;

import com.ing.ingenious.api.annotation.Action;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Single source of truth for the catalog of {@link Action @Action}-annotated
 * methods that ship with INGenious (and any plugins that have already been
 * loaded into the current classloader).
 *
 * <p>Used by both {@code ingenious action ...} CLI commands and the MCP
 * server so the two stay in sync.
 */
public final class ActionCatalog {
    /**
     * ObjectType (from {@link com.ing.ingenious.api.types.ObjectType}) → friendly
     * category. Keys are normalised (lower-case, no whitespace) before lookup so
     * matching is case-insensitive.
     */
    private static final Map<String, String> CATEGORY = new LinkedHashMap<>();

    static {
        CATEGORY.put("playwright", "Browser");
        CATEGORY.put("browser", "Browser");
        CATEGORY.put("web", "Browser");
        CATEGORY.put("webservice", "API");
        CATEGORY.put("database", "Database");
        CATEGORY.put("kafka", "Kafka");
        CATEGORY.put("queue", "Kafka");
        CATEGORY.put("mobile", "Mobile");
        CATEGORY.put("app", "Mobile");
        CATEGORY.put("general", "General");
        CATEGORY.put("any", "General");
        CATEGORY.put("data", "General");
        CATEGORY.put("file", "General");
        CATEGORY.put("image", "General");
        CATEGORY.put("stringoperations", "General");
        CATEGORY.put("structureddata", "General");
        CATEGORY.put("protractorjs", "General");
        CATEGORY.put("sap", "General");
    }

    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replace(" ", "");
    }

    /** Explicit list of command classes shipped in {@code ingenious-engine}. */
    private static final String[] COMMAND_CLASSES = {
        // Browser
        "com.ing.engine.commands.browser.Basic",
        "com.ing.engine.commands.browser.Assertions",
        "com.ing.engine.commands.browser.CheckBox",
        "com.ing.engine.commands.browser.Command",
        "com.ing.engine.commands.browser.CommonMethods",
        "com.ing.engine.commands.browser.Cookies",
        "com.ing.engine.commands.browser.Dialogs",
        "com.ing.engine.commands.browser.DownloadFiles",
        "com.ing.engine.commands.browser.DragTo",
        "com.ing.engine.commands.browser.DynamicObject",
        "com.ing.engine.commands.browser.Focus",
        "com.ing.engine.commands.browser.General",
        "com.ing.engine.commands.browser.JSCommands",
        "com.ing.engine.commands.browser.Keys",
        "com.ing.engine.commands.browser.MouseClick",
        "com.ing.engine.commands.browser.Performance",
        "com.ing.engine.commands.browser.RequestFulfill",
        "com.ing.engine.commands.browser.Scroll",
        "com.ing.engine.commands.browser.SelectOptions",
        "com.ing.engine.commands.browser.StorageState",
        "com.ing.engine.commands.browser.Switch",
        "com.ing.engine.commands.browser.TextInput",
        "com.ing.engine.commands.browser.UploadFiles",
        "com.ing.engine.commands.browser.WaitFor",
        // Mobile
        "com.ing.engine.commands.mobile.AppiumDeviceCommands",
        "com.ing.engine.commands.mobile.AssertElement",
        "com.ing.engine.commands.mobile.Assertions",
        "com.ing.engine.commands.mobile.Basic",
        "com.ing.engine.commands.mobile.ByLabel",
        "com.ing.engine.commands.mobile.CheckBox",
        "com.ing.engine.commands.mobile.CommonMethods",
        "com.ing.engine.commands.mobile.DynamicObject",
        "com.ing.engine.commands.mobile.JSCommands",
        "com.ing.engine.commands.mobile.MobileGeneral",
        "com.ing.engine.commands.mobile.Performance",
        "com.ing.engine.commands.mobile.RelativeCommand",
        "com.ing.engine.commands.mobile.Scroll",
        "com.ing.engine.commands.mobile.SwitchTo",
        "com.ing.engine.commands.mobile.Table",
        "com.ing.engine.commands.mobile.WaitFor",
        "com.ing.engine.commands.mobile.WebButton",
        // Database
        "com.ing.engine.commands.database.Database",
        "com.ing.engine.commands.database.General",
        // Webservice
        "com.ing.engine.commands.webservice.Webservice",
        // Queue
        "com.ing.engine.commands.queue.QueueOperations",
        // General / Files / Strings
        "com.ing.engine.commands.general.GeneralOperations",
        "com.ing.engine.commands.file.FileOperations",
        "com.ing.engine.commands.stringOperations.StringOperations",
        // Accessibility
        "com.ing.engine.commands.aXe.Accessibility",
        // Synthetic data
        "com.ing.engine.commands.syntheticData.SyntheticDataGenerator",
        // Galen visual tests
        "com.ing.engine.commands.galenCommands.Align",
        "com.ing.engine.commands.galenCommands.Attribute",
        "com.ing.engine.commands.galenCommands.Centered",
        "com.ing.engine.commands.galenCommands.ColorScheme",
        "com.ing.engine.commands.galenCommands.Contains",
        "com.ing.engine.commands.galenCommands.CssProperties",
        "com.ing.engine.commands.galenCommands.Direction",
        "com.ing.engine.commands.galenCommands.General",
        "com.ing.engine.commands.galenCommands.Image",
        "com.ing.engine.commands.galenCommands.Inside",
        "com.ing.engine.commands.galenCommands.Near",
        "com.ing.engine.commands.galenCommands.On",
        "com.ing.engine.commands.galenCommands.PageDump",
        "com.ing.engine.commands.galenCommands.Report",
        "com.ing.engine.commands.galenCommands.Text",
        "com.ing.engine.commands.galenCommands.Title",
        "com.ing.engine.commands.galenCommands.Url",
        "com.ing.engine.commands.galenCommands.WidthAndHeight"
    };

    /** Immutable, de-duplicated catalog – built lazily on first access. */
    private static volatile List<ActionInfo> CACHED;

    private ActionCatalog() {}

    /** Returns every discovered action, sorted by category then name. */
    public static List<ActionInfo> all() {
        List<ActionInfo> cached = CACHED;
        if (cached != null) return cached;
        synchronized (ActionCatalog.class) {
            if (CACHED != null) return CACHED;
            CACHED = Collections.unmodifiableList(discover());
            return CACHED;
        }
    }

    /** Filter by friendly category ("Browser", "API", …). Case-insensitive. */
    public static List<ActionInfo> byCategory(String category) {
        if (category == null) return all();
        String norm = category.trim().toLowerCase(Locale.ROOT);
        List<ActionInfo> out = new ArrayList<>();
        for (ActionInfo a : all()) {
            if (a.category.toLowerCase(Locale.ROOT).equals(norm)) out.add(a);
        }
        return out;
    }

    /**
     * Synonym groups so a natural-language query matches INGenious's own
     * vocabulary. INGenious uses "Assert" for checks; users say
     * "validate/verify/check". Each word maps to the full set of equivalents.
     */
    private static final Map<String, String[]> SYNONYMS = new HashMap<>();

    static {
        String[] assertGroup = { "assert", "verify", "validate", "check", "ensure", "confirm" };
        String[] containGroup = { "contains", "contain", "include", "includes", "has" };
        String[] equalGroup = { "equals", "equal", "matches", "match", "same" };
        for (String w : assertGroup) SYNONYMS.put(w, assertGroup);
        for (String w : containGroup) SYNONYMS.put(w, containGroup);
        for (String w : equalGroup) SYNONYMS.put(w, equalGroup);
    }

    /** True when the query token (or one of its synonyms) occurs in the haystack. */
    private static boolean tokenMatches(String token, String hay) {
        String[] variants = SYNONYMS.get(token);
        if (variants == null) {
            return hay.contains(token);
        }
        for (String v : variants) {
            if (hay.contains(v)) return true;
        }
        return false;
    }

    /**
     * Free-text, synonym-aware, relevance-ranked search over name / description /
     * object type / category. Results are ordered best-match first so callers
     * that take the first hit (e.g. the CLI planner) get the most relevant
     * action. Matches even when only some query words are present.
     */
    public static List<ActionInfo> search(String query) {
        if (query == null || query.isEmpty()) return all();
        String q = query.toLowerCase(Locale.ROOT).trim();
        String[] tokens = q.split("\\s+");
        int contentTokens = 0;
        for (String tok : tokens) {
            if (!tok.isEmpty()) contentTokens++;
        }
        // Require a majority of query words to be present (min 1) so a single
        // common word doesn't drag in the whole catalog.
        int threshold = Math.max(1, (int) Math.ceil(contentTokens * 0.5));

        List<Scored> scored = new ArrayList<>();
        for (ActionInfo a : all()) {
            String name = a.name.toLowerCase(Locale.ROOT);
            String hay =
                (
                    a.name +
                    " " +
                    (a.description == null ? "" : a.description) +
                    " " +
                    a.objectType +
                    " " +
                    a.category
                ).toLowerCase(Locale.ROOT);

            int matched = 0;
            int nameMatched = 0;
            for (String tok : tokens) {
                if (tok.isEmpty()) continue;
                if (tokenMatches(tok, hay)) {
                    matched++;
                    if (tokenMatches(tok, name)) nameMatched++;
                }
            }
            if (matched < threshold) continue;

            int score = matched * 10 + nameMatched * 2;
            if (hay.contains(q)) score += 50; // exact phrase
            // Penalise negated variants when the query isn't negated, so
            // "assert contains" ranks above "assert NOT contains".
            if (hay.contains("not") && !q.contains("not")) score -= 7;
            scored.add(new Scored(a, score));
        }

        scored.sort(
            Comparator
                .comparingInt((Scored s) -> s.score)
                .reversed()
                .thenComparing(s -> s.action.name)
        );
        List<ActionInfo> out = new ArrayList<>(scored.size());
        for (Scored s : scored) out.add(s.action);
        return out;
    }

    private static final class Scored {
        final ActionInfo action;
        final int score;

        Scored(ActionInfo action, int score) {
            this.action = action;
            this.score = score;
        }
    }

    /** Lookup a single action by exact name. Returns {@code null} if not found. */
    public static ActionInfo find(String name) {
        if (name == null) return null;
        for (ActionInfo a : all()) {
            if (a.name.equals(name)) return a;
        }
        return null;
    }

    /** "Browser" → 42, "API" → 17, etc. */
    public static Map<String, Integer> categoryCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ActionInfo a : all()) {
            counts.merge(a.category, 1, Integer::sum);
        }
        return counts;
    }

    // ------------------------------------------------------------------
    // discovery
    // ------------------------------------------------------------------

    private static List<ActionInfo> discover() {
        Map<String, ActionInfo> unique = new LinkedHashMap<>();
        for (String className : COMMAND_CLASSES) {
            try {
                Class<?> clazz = Class.forName(className);
                String defaultObjType = defaultObjectTypeFor(className);
                for (Method method : clazz.getDeclaredMethods()) {
                    if (!method.isAnnotationPresent(Action.class)) continue;
                    Action a = method.getAnnotation(Action.class);
                    String objType = a.object();
                    // Most @Action methods on browser/mobile classes only set
                    // object() at the class level (or default to "Any"). Fall
                    // back to the per-class default so the catalog can still
                    // bucket them correctly.
                    String effective = (
                            objType == null || objType.isEmpty() || "Any".equalsIgnoreCase(objType)
                        )
                        ? defaultObjType
                        : objType;
                    String category = CATEGORY.getOrDefault(normalize(effective), "General");
                    unique.putIfAbsent(
                        method.getName(),
                        new ActionInfo(
                            method.getName(),
                            category,
                            effective,
                            a.desc(),
                            a.input().name(),
                            a.condition().name()
                        )
                    );
                }
            } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                // optional plugin class – skip
            } catch (Exception ignored) {
                // never let a bad class break discovery
            }
        }
        List<ActionInfo> out = new ArrayList<>(unique.values());
        out.sort(Comparator.comparing((ActionInfo a) -> a.category).thenComparing(a -> a.name));
        return out;
    }

    /**
     * Derive a default object type from the class's package so methods that
     * declare {@code @Action} without an explicit {@code object()} still end
     * up in the correct bucket. Matches how {@code com.ing.engine.commands.*}
     * is organised.
     */
    private static String defaultObjectTypeFor(String className) {
        String n = className == null ? "" : className.toLowerCase(Locale.ROOT);
        if (n.contains(".commands.browser.")) return "Browser";
        if (n.contains(".commands.mobile.")) return "Mobile";
        if (n.contains(".commands.database.")) return "Database";
        if (n.contains(".commands.webservice.")) return "Webservice";
        if (n.contains(".commands.queue.")) return "Kafka";
        if (n.contains(".commands.file.")) return "File";
        if (n.contains(".commands.stringoperations.")) return "String Operations";
        if (n.contains(".commands.syntheticdata.")) return "Data";
        if (n.contains(".commands.galencommands.")) return "Browser";
        if (n.contains(".commands.axe.")) return "Browser";
        if (n.contains(".commands.general.")) return "General";
        return "General";
    }

    /** Plain data holder describing a single test action. */
    public static final class ActionInfo {
        public final String name;
        public final String category;
        public final String objectType;
        public final String description;
        public final String inputRequired;
        public final String conditionSupported;

        public ActionInfo(
            String name,
            String category,
            String objectType,
            String description,
            String inputRequired,
            String conditionSupported
        ) {
            this.name = name;
            this.category = category;
            this.objectType = objectType;
            this.description = description;
            this.inputRequired = inputRequired;
            this.conditionSupported = conditionSupported;
        }
    }
}
