package com.ing.engine.execution.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 *   <li>{@code Sheet:Column@Project} / {@code {Sheet:Column@Project}} - the project's own Test
 *       Data, tag explicit (what the IDE writes for new entries).</li>
 *   <li>{@code Sheet:Column@Shared} / {@code {Sheet:Column@Shared}} - the app-root Shared Test
 *       Data store.</li>
 * </ul>
 *
 * <p>The {@code @Shared}/{@code @Project} tag is kept attached to the sheet name in the parse
 * result so the scope-aware pipeline ({@link DataAccess}/{@link DataAccessInternal}) can honour it -
 * {@code stripProjectSheetScopeTag} treats an untagged name and a {@code @Project}-tagged name
 * identically.</p>
 *
 * <p>Grammar deliberately mirrors {@code DataProcessor.SCOPED_DATASHEET_PATTERN} (engine, execution
 * side) and {@code TestStep.isScopedTestDataRef} (datalib, IDE side).</p>
 */
public final class TestDataToken {

    private TestDataToken() {}

    private static final Logger LOG = LoggerFactory.getLogger(TestDataToken.class);

    public static final String SHARED_TAG = "@Shared";
    public static final String PROJECT_TAG = "@Project";

    /**
     * Trailing scope tag, e.g. {@code "@Shared"} / {@code "@Project"}. Anchored to the end of the
     * reference (after the column), not the sheet - stays in step with
     * {@code DataProcessor.SCOPED_DATASHEET_PATTERN}.
     */
    private static final String TAG = "(?:@Shared|@Project)";

    /**
     * A whole string that is a (optionally braced, optionally scope-tagged) {@code Sheet:Column}
     * reference. Sheet and Column must start with a non-digit, non-space character and may not
     * contain {@code @} (reserved for the trailing scope tag) so a bare {@code host:8080} /
     * {@code 12:30} is not mistaken for a data reference.
     */
    private static final Pattern REFERENCE = Pattern.compile(
        "^\\{?\\s*[^\\{\\}:@\\d\\s][^\\{\\}:@]*:[^\\{\\}:@\\d\\s][^\\{\\}:@]*" + TAG + "?\\s*\\}?$"
    );

    /**
     * One embedded {@code {Sheet:Column}} token inside a larger string. Strict on purpose: the
     * sheet name must start with a letter / {@code _} / {@code $} and neither part may contain a
     * quote or {@code @}, so a JSON object literal such as {@code {"a":"b"}} is never matched.
     */
    private static final Pattern EMBEDDED_TOKEN = Pattern.compile(
        "\\{\\s*[A-Za-z_$][^\\{\\}:\"@]*:[^\\{\\}:\"@]+" + TAG + "?\\s*\\}"
    );

    /** {@code "@Shared"}, {@code "@Project"}, or {@code ""} - the scope tag {@code ref} carries. */
    public static String scopeTag(String ref) {
        String t = unwrapBraces(ref);
        if (t.endsWith(SHARED_TAG)) {
            return SHARED_TAG;
        }
        if (t.endsWith(PROJECT_TAG)) {
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
     * <b>optional</b> here - bare {@code Sheet:Column} / {@code Sheet:Column@Project} is the
     * normal form; a wrapping {@code {...}} is tolerated. The trailing {@code @Shared}/
     * {@code @Project} tag (if any) is stripped from the end first and re-attached to the sheet
     * in the result, the split is on the first {@code ':'}, both parts are trimmed. Returns
     * {@code null} when {@code ref} is not a usable {@code Sheet:Column} reference (no colon,
     * empty sheet-name, or empty column after the tag is removed).
     */
    public static String[] parse(String ref) {
        String s = unwrapBraces(ref);
        if (s.isEmpty()) {
            return null;
        }
        String tag = scopeTag(s);
        if (!tag.isEmpty()) {
            s = s.substring(0, s.length() - tag.length()).trim();
        }
        int colon = s.indexOf(':');
        if (colon < 1) {
            return null;
        }
        String sheet = s.substring(0, colon).trim();
        String column = s.substring(colon + 1).trim();
        if (sheet.isEmpty() || column.isEmpty()) {
            return null;
        }
        return new String[] { sheet + tag, column };
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
            String token = m.group();
            String[] sc = parse(token);
            String value = null;
            if (sc == null) {
                LOG.warn(
                    "Test Data token {} is not a valid Sheet:Column reference. Leaving it as literal text.",
                    token
                );
            } else {
                try {
                    value = userData.getData(sc[0], sc[1]);
                    if (value == null) {
                        LOG.warn(
                            "Test Data token {} (sheet '{}', column '{}') resolved to no value. Leaving it as literal text.",
                            token,
                            sc[0],
                            sc[1]
                        );
                    }
                } catch (RuntimeException e) {
                    LOG.warn(
                        "Test Data token {} (sheet '{}', column '{}') could not be resolved: {}. Leaving it as literal text.",
                        token,
                        sc[0],
                        sc[1],
                        e.getMessage()
                    );
                }
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(value != null ? value : token));
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
