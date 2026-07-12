package com.ing.engine.mcp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The single source of truth for INGenious authoring conventions.
 *
 * <p>Every guidance surface is rendered from this class so the rules can
 * never drift apart:
 * <ul>
 *   <li>{@code initialize.instructions} handshake text ({@link #condensedInstructions()})</li>
 *   <li>the {@code ingenious://docs/conventions} resource ({@link #conventionsDoc()})</li>
 *   <li>write-time normalization ({@link StepNormalizer})</li>
 *   <li>lint rules applied by {@code ingenious_testcase_validate}</li>
 * </ul>
 *
 * <p>Grammar summary (the "input grammar"):
 * <ul>
 *   <li>{@code @literal}       – hard-coded value (always {@code @}-prefixed)</li>
 *   <li>{@code Sheet:Column}   – whole-input data-sheet reference</li>
 *   <li>{@code {Sheet:Column}} – data-sheet reference embedded in an API payload</li>
 *   <li>{@code #id}            – GlobalData environment id; only valid inside
 *                                data-sheet cells, never in a step input</li>
 *   <li>{@code %var%}          – runtime variable; passed through untouched</li>
 * </ul>
 */
public final class ConventionCatalog {

    private ConventionCatalog() {}

    // ==================================================================
    // rules registry
    // ==================================================================

    /** Severity of a convention rule. */
    public static final String ERROR = "error";

    public static final String WARN = "warn";
    public static final String INFO = "info";

    /** One machine-checkable convention rule. */
    public static final class Rule {
        public final String id;
        public final String severity;
        public final String summary;

        Rule(String id, String severity, String summary) {
            this.id = id;
            this.severity = severity;
            this.summary = summary;
        }
    }

    private static final Map<String, Rule> RULES = new LinkedHashMap<>();

    private static void rule(String id, String severity, String summary) {
        RULES.put(id, new Rule(id, severity, summary));
    }

    static {
        // ---- errors --------------------------------------------------
        rule("E1", ERROR, "Action name is not in the action catalog and is not a reusable call.");
        rule(
            "E2",
            ERROR,
            "Step 'reference' points to an Object Repository page that does not exist."
        );
        rule("E3", ERROR, "Execute step must use action '<ReusableScenario>:<ReusableName>'.");
        rule(
            "E4",
            ERROR,
            "Data reference (Sheet:Column or {Sheet:Column}) points to a missing sheet or column."
        );
        rule(
            "E5",
            ERROR,
            "The same scenario name is used in both ReusableComponents/ and TestPlan/."
        );
        rule(
            "E6",
            ERROR,
            "GlobalData environment id (#id) placed directly in a step input; it belongs in data-sheet cells only."
        );
        rule(
            "E7",
            ERROR,
            "Object reference must not be @-prefixed (except engine specials such as @Browser)."
        );
        rule("E8", ERROR, "Step is missing an action.");
        // ---- warnings ------------------------------------------------
        rule(
            "W1",
            WARN,
            "Hard-coded @literal input; externalise it with ingenious_testcase_parameterize."
        );
        rule("W2", WARN, "Fixed sleep (pause with a literal duration); prefer a waitFor* action.");
        rule("W3", WARN, "Interaction step without a preceding waitFor* on the element.");
        rule("W4", WARN, "Test case contains no assertion step.");
        rule("W7", WARN, "Possible secret (password/token) stored as a literal value.");
        rule(
            "W8",
            WARN,
            "MANUAL marker step must carry the note in 'action' only (object/input empty)."
        );
        rule(
            "W9",
            WARN,
            "Long test case with no Execute steps; extract reusable components (user intents)."
        );
        rule(
            "W10",
            WARN,
            "Identical step sequence shared by several test cases; extract a reusable component."
        );
        // ---- info ----------------------------------------------------
        rule(
            "I1",
            INFO,
            "Scenario / test case names should describe a business flow and a user journey."
        );
        rule("I3", INFO, "Step has no description.");
        rule("I4", INFO, "Tags are missing or lack the @ prefix.");
    }

    public static Collection<Rule> all() {
        return RULES.values();
    }

    public static Rule find(String id) {
        return RULES.get(id);
    }

    // ==================================================================
    // input grammar helpers
    // ==================================================================

    /** Whole-input data reference: {@code Sheet:Column} (no spaces, no scheme URLs). */
    private static final Pattern DATA_REF = Pattern.compile(
        "^[A-Za-z0-9_.\\-]+:[A-Za-z0-9_.\\-]+$"
    );

    /** Data reference embedded in a payload: {@code {Sheet:Column}}. */
    public static final Pattern PAYLOAD_TOKEN = Pattern.compile(
        "\\{([A-Za-z0-9_.\\-]+):([A-Za-z0-9_.\\-]+)\\}"
    );

    /** Engine directives that look like literals but must never be parameterized. */
    private static final Set<String> ENGINE_DIRECTIVES = new HashSet<>(
        Arrays.asList("@browser", "@enter", "@tab", "@escape", "@space", "@0")
    );

    /** Actions whose input is a raw request payload (never {@code @}-prefixed). */
    private static final Set<String> PAYLOAD_ACTIONS = new HashSet<>(
        Arrays.asList(
            "postrestrequest",
            "putrestrequest",
            "patchrestrequest",
            "deletewithpayload",
            "postsoaprequest"
        )
    );

    /** True when {@code input} is a whole-input data-sheet reference. */
    public static boolean isDataRef(String input) {
        return input != null && DATA_REF.matcher(input).matches();
    }

    /** True when {@code input} contains at least one embedded {@code {Sheet:Column}} token. */
    public static boolean containsPayloadTokens(String input) {
        return input != null && PAYLOAD_TOKEN.matcher(input).find();
    }

    /** True when {@code input} is a GlobalData environment id such as {@code #test}. */
    public static boolean isGlobalDataId(String input) {
        return input != null && input.startsWith("#") && input.length() > 1;
    }

    /** True when {@code input} is an engine directive such as {@code @Browser}. */
    public static boolean isEngineDirective(String input) {
        return input != null && ENGINE_DIRECTIVES.contains(input.toLowerCase(Locale.ROOT));
    }

    /** True when {@code action} takes a raw payload body as its input. */
    public static boolean isPayloadAction(String action) {
        return action != null && PAYLOAD_ACTIONS.contains(action.trim().toLowerCase(Locale.ROOT));
    }

    /** True when {@code input} is a hard-coded {@code @literal} eligible for parameterization. */
    public static boolean isParameterizableLiteral(String input) {
        return (
            input != null &&
            input.startsWith("@") &&
            input.length() > 1 &&
            !isEngineDirective(input) &&
            !isDataRef(input.substring(1))
        );
    }

    // ==================================================================
    // rendered guidance
    // ==================================================================

    /**
     * Condensed conventions returned in the MCP {@code initialize} response.
     * Every MCP client injects this into the model context automatically.
     */
    public static String condensedInstructions() {
        return (
            "INGenious test-automation conventions (authoritative - always follow):\n" +
            "\n" +
            "STEP INPUT GRAMMAR\n" +
            "* Hard-coded values are @-prefixed: input=\"@200\", input=\"@https://site\".\n" +
            "* Data-driven values reference a data sheet: input=\"Sheet:Column\".\n" +
            "* Inside API payload bodies use embedded tokens: {Sheet:Column}. Payload bodies\n" +
            "  (postRestRequest/putRestRequest/patchRestRequest) are NOT @-prefixed.\n" +
            "* GlobalData environment ids (#dev, #test, ...) are data-sheet CELL VALUES only -\n" +
            "  never place them in a step input. Environment names come from the project's\n" +
            "  GlobalData sheet; never assume them.\n" +
            "* Object references are never @-prefixed (engine specials like @Browser excepted).\n" +
            "\n" +
            "AUTHORING WORKFLOW\n" +
            "1. Discover real action names first (ingenious_action_search/list) - never invent them.\n" +
            "2. Create the test case with @literal inputs (ingenious_testcase_create).\n" +
            "3. Externalise data with ingenious_testcase_parameterize (mode=scan, then apply).\n" +
            "4. Validate (ingenious_testcase_validate), run (ingenious_run), triage\n" +
            "   (ingenious_report_failures).\n" +
            "\n" +
            "STRUCTURE & NAMING\n" +
            "* TestPlan scenarios are business flows (e.g. 'Mortgage Calculation'); test cases\n" +
            "  are user journeys (e.g. 'Young Single buying a High Energy Label home').\n" +
            "* ReusableComponents scenarios group user intents (e.g. 'Common', 'Flow'); each\n" +
            "  reusable is one user intent (e.g. 'Fill Income', 'Launch the App').\n" +
            "* A scenario name must NEVER be used by both TestPlan/ and ReusableComponents/.\n" +
            "* Test cases compose reusables: object=\"Execute\",\n" +
            "  action=\"<ReusableScenario>:<ReusableName>\", reference=\"[Project]\".\n" +
            "* Reuse existing reusables and data-sheet rows before creating new ones.\n" +
            "\n" +
            "QUALITY\n" +
            "* Never use fixed sleeps; use waitFor* actions.\n" +
            "* Every test case ends with at least one assertion.\n" +
            "* Never copy plaintext passwords into steps or data sheets.\n" +
            "Full reference: read the ingenious://docs/conventions resource."
        );
    }

    /** Full conventions document served as {@code ingenious://docs/conventions}. */
    public static String conventionsDoc() {
        StringBuilder sb = new StringBuilder();
        sb
            .append("# INGenious authoring conventions\n\n")
            .append("This document is generated from the engine's ConventionCatalog - the same\n")
            .append("rules are enforced by the write tools and `ingenious_testcase_validate`.\n\n")
            .append("## Step input grammar\n\n")
            .append("| Form | Meaning | Example |\n")
            .append("|------|---------|---------|\n")
            .append("| `@literal` | Hard-coded value | `@200`, `@https://example.com` |\n")
            .append(
                "| `Sheet:Column` | Whole-input data-sheet reference | `LoginData:Username` |\n"
            )
            .append(
                "| `{Sheet:Column}` | Data reference embedded in an API payload | `{Payment:AccountNumber}` |\n"
            )
            .append("| `#id` | GlobalData environment id - data-sheet cells ONLY | `#test` |\n")
            .append("| `%var%` | Runtime variable | `%orderId%` |\n\n")
            .append("Payload-bearing actions (`postRestRequest`, `putRestRequest`,\n")
            .append("`patchRestRequest`, `deleteWithPayload`) take the raw body as input - the\n")
            .append(
                "body itself is **not** `@`-prefixed; parameterize individual JSON/XML values\n"
            )
            .append("with `{Sheet:Column}` tokens instead:\n\n")
            .append("```json\n")
            .append(
                "{ \"payment_details\": { \"account_number\": \"{Payment:AccountNumber}\" } }\n"
            )
            .append("```\n\n")
            .append("## Structure and naming\n\n")
            .append(
                "* **TestPlan** scenario = business flow (`Mortgage Calculation`); test case =\n"
            )
            .append("  user journey (`Young Single buying a High Energy Label home`).\n")
            .append("* **ReusableComponents** scenario = user-intent group (`Common`, `Flow`);\n")
            .append("  reusable = one user intent (`Launch the App`, `Fill Income`).\n")
            .append("* Scenario names must be unique across TestPlan/ and ReusableComponents/.\n")
            .append("* Compose test cases from reusables:\n\n")
            .append("```yaml\n")
            .append("  - step: 1\n")
            .append("    object: Execute\n")
            .append("    action: Common:Launch the App\n")
            .append("    reference: \"[Project]\"\n")
            .append("```\n\n")
            .append("## Recommended workflow\n\n")
            .append("1. `ingenious_action_search` - discover real action names (never invent).\n")
            .append("2. `ingenious_testcase_create` - author with `@literal` inputs first.\n")
            .append("3. `ingenious_testcase_parameterize` - `mode=scan` lists every hard-coded\n")
            .append("   value (including individual JSON payload fields); apply `mode=all` or a\n")
            .append(
                "   `selections` subset. This moves values into a data-sheet row keyed to the\n"
            )
            .append("   test case and rewrites inputs as `Sheet:Column` / `{Sheet:Column}`.\n")
            .append("4. `ingenious_testcase_validate` - lint against the rules below.\n")
            .append("5. `ingenious_run` / `ingenious_report_failures` - execute and triage.\n\n")
            .append("## Rules\n\n")
            .append("| Id | Severity | Rule |\n")
            .append("|----|----------|------|\n");
        for (Rule r : RULES.values()) {
            sb
                .append("| ")
                .append(r.id)
                .append(" | ")
                .append(r.severity)
                .append(" | ")
                .append(r.summary)
                .append(" |\n");
        }
        sb
            .append("\n## Environments\n\n")
            .append("Environment ids are project-specific and live in the GlobalData sheet\n")
            .append("(`GlobalDataID` column). Discover them with `ingenious_data_show` - never\n")
            .append("assume names like `#dev`/`#prod`. Data-sheet cells reference an environment\n")
            .append("id; the engine substitutes the matching GlobalData column value at runtime.\n")
            .append("Column names in data sheets must match GlobalData column names exactly.\n\n")
            .append("## Secrets\n\n")
            .append("Never store plaintext passwords in steps or data sheets. Use placeholders\n")
            .append("(e.g. `PLACEHOLDER_TEST_DO_NOT_COMMIT`) and inject real values at runtime.\n");
        return sb.toString();
    }
}
