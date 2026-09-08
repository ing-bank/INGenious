package com.ing.engine.execution.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One canonical parser for a Test Data reference.
 *
 * <p><b>Two usage modes - braces are a delimiter, not a mode switch:</b></p>
 * <ul>
 *   <li><b>Whole-input</b> - the entire value <em>is</em> the reference (a step's Input, a write
 *       action's destination, a param-loop's data column). Braces are <b>optional</b>: bare
 *       {@code Sheet:Column} is the canonical form. Use {@link #parse(String)} /
 *       {@link #isReference(String)}. Mirrors {@code DataProcessor.isInputPatternDataSheet} on the
 *       step-execution side and {@code TestStep.isTestDataStep} on the IDE side.</li>
 *   <li><b>Embedded</b> - the reference sits inside a larger string (webservice / MQ payloads,
 *       endpoints, headers, SQL text, file templates, browser-context values, Database connection
 *       config, String Operations fragments). Braces are <b>required</b> - {@code {...}} is what
 *       marks a fragment as a reference versus surrounding literal text. Use
 *       {@link #resolveEmbeddedTokens(String, UserDataAccess)}.</li>
 * </ul>
 *
 * <p>Every accepted form resolves against Test Data:</p>
 * <ul>
 *   <li>{@code Sheet:Column} / {@code {Sheet:Column}} - the project's own Test Data (canonical,
 *       unchanged; no migration).</li>
 *   <li>{@code [Project] Sheet:Column} / {@code {[Project] Sheet:Column}} - the project's own Test
 *       Data, tag explicit (what the IDE writes for new entries).</li>
 *   <li>{@code [Shared] Sheet:Column} / {@code {[Shared] Sheet:Column}} - the app-root Shared Test
 *       Data store.</li>
 * </ul>
 *
 * <p>The {@code [Shared]}/{@code [Project]} tag is kept attached to the sheet name in the parse
 * result so the scope-aware pipeline ({@link DataAccess}/{@link DataAccessInternal}) can honour it -
 * {@code stripProjectScopeTag} treats an untagged name and a {@code [Project]}-tagged name
 * identically.</p>
 *
 * <p>Grammar deliberately mirrors {@code DataProcessor.SCOPED_DATASHEET_PATTERN} (engine, execution
 * side) and {@code TestStep.isScopedTestDataRef} (datalib, IDE side).</p>
 */
public final class TestDataToken {

    private TestDataToken() {}

    public static final String SHARED_TAG = "[Shared]";
    public static final String PROJECT_TAG = "[Project]";

    /**
     * Leading scope tag, e.g. {@code "[Shared] "} / {@code "[Project] "}. {@code \s*} (not
     * {@code \s+}) so it stays in step with {@code DataProcessor.SCOPED_DATASHEET_PATTERN}.
     */
    private static final String TAG = "(?:\\[Shared\\]|\\[Project\\])\\s*";

    /**
     * A whole string that is a (optionally braced, optionally scope-tagged) {@code Sheet:Column}
     * reference. Sheet and Column must start with a non-digit, non-space character so a bare
     * {@code host:8080} / {@code 12:30} is not mistaken for a data reference.
     */
    private static final Pattern REFERENCE = Pattern.compile(
        "^\\{?\\s*(?:" + TAG + ")?[^\\{\\}:\\d\\s][^\\{\\}:]*:[^\\{\\}:\\d\\s][^\\{\\}:]*\\s*\\}?$"
    );

    /**
     * One embedded {@code {Sheet:Column}} token inside a larger string. Strict on purpose: the
     * sheet name must start with a letter / {@code _} / {@code $} and neither part may contain a
     * quote, so a JSON object literal such as {@code {"a":"b"}} is never matched.
     */
    private static final Pattern EMBEDDED_TOKEN = Pattern.compile(
        "\\{\\s*(?:" + TAG + ")?[A-Za-z_$][^\\{\\}:\"]*:[^\\{\\}:\"]+\\s*\\}"
    );

    /** {@code "[Shared]"}, {@code "[Project]"}, or {@code ""} - the scope tag {@code ref} carries. */
    public static String scopeTag(String ref) {
        String t = unwrapBraces(ref);
        if (t.startsWith(SHARED_TAG)) {
            return SHARED_TAG;
        }
        if (t.startsWith(PROJECT_TAG)) {
            return PROJECT_TAG;
        }
        return "";
    }

    public static boolean hasScopeTag(String ref) {
        return !scopeTag(ref).isEmpty();
    }

    /** True when {@code ref} as a whole is an (optionally braced/tagged) {@code Sheet:Column}. */
    public static boolean isReference(String ref) {
        return ref != null && REFERENCE.matcher(ref.trim()).matches();
    }

    /** True when {@code text} contains at least one embedded {@code {Sheet:Column}} token. */
    public static boolean containsEmbeddedToken(String text) {
        return text != null && EMBEDDED_TOKEN.matcher(text).find();
    }

    /**
     * Whole-input parse: splits a reference into {@code { taggedSheet, column }}. Braces are
     * <b>optional</b> here - bare {@code Sheet:Column} / {@code [Project] Sheet:Column} is the
     * normal form; a wrapping {@code {...}} is tolerated. The {@code [Shared]}/{@code [Project]}
     * tag (if any) stays on the sheet, the split is on the first {@code ':'}, both parts are
     * trimmed. Returns {@code null} when {@code ref} is not a usable {@code Sheet:Column}
     * reference (no colon, empty sheet-name after the tag, or empty column).
     */
    public static String[] parse(String ref) {
        String s = unwrapBraces(ref);
        if (s.isEmpty()) {
            return null;
        }
        int colon = s.indexOf(':');
        if (colon < 1) {
            return null;
        }
        String sheet = s.substring(0, colon).trim();
        String column = s.substring(colon + 1).trim();
        if (column.isEmpty()) {
            return null;
        }
        String tag = scopeTag(sheet);
        if (!tag.isEmpty() && sheet.substring(tag.length()).trim().isEmpty()) {
            return null; // "[Project]:Col" - tag but no sheet name
        }
        if (tag.isEmpty() && sheet.isEmpty()) {
            return null;
        }
        return new String[] { sheet, column };
    }

    /**
     * Embedded substitution: replaces every {@code {Sheet:Column}} token (any accepted scope form)
     * in {@code text} with its Test Data value, leaving all other text alone. Braces are
     * <b>required</b> here - they are the delimiter that tells a fragment apart from surrounding
     * literal text (SQL, JSON/XML payloads, URLs, connection strings, file templates). A token
     * whose sheet/column does not resolve is left untouched - matching the historical "unknown
     * token stays literal" behaviour and keeping JSON/YAML braces safe.
     */
    public static String resolveEmbeddedTokens(String text, UserDataAccess userData) {
        if (text == null || text.indexOf('{') < 0) {
            return text;
        }
        Matcher m = EMBEDDED_TOKEN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String[] sc = parse(m.group());
            String value = null;
            if (sc != null) {
                try {
                    value = userData.getData(sc[0], sc[1]);
                } catch (RuntimeException ignore) {
                    value = null; // not a resolvable data reference - leave the token as-is
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : m.group()));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    /** Strips one layer of surrounding {@code { }} (tolerating inner whitespace) and trims. */
    public static String unwrapBraces(String ref) {
        String t = ref == null ? "" : ref.trim();
        if (t.length() >= 2 && t.charAt(0) == '{' && t.charAt(t.length() - 1) == '}') {
            t = t.substring(1, t.length() - 1).trim();
        }
        return t;
    }
}
