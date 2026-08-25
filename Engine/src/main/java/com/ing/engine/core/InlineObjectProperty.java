package com.ing.engine.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parsing / serialization helpers for the <b>inline object-property override</b>
 * feature.
 *
 * <p>A locator-based step (e.g. {@code click}, {@code selectSingleByVisibleText})
 * may carry a runtime property override directly in its <b>Condition</b> column so
 * that a separate {@code setObjectProperty} step is no longer required. The cell
 * holds a self-identifying expression:</p>
 *
 * <pre>
 * setProp:       #token=&lt;value&gt;[; #token2=&lt;value2&gt; ...]   (object-scoped)
 * setGlobalProp: #token=&lt;value&gt;[; #token2=&lt;value2&gt; ...]   (global)
 * </pre>
 *
 * <p>The {@code &lt;value&gt;} may be a {@code Sheet:Column} reference, a
 * {@code %runtimeVar%}, a {@code #globalDataId}, or a hard-coded literal; the engine
 * resolves it through the normal data pipeline before applying it.</p>
 */
public final class InlineObjectProperty {
    /** Marker for an object-scoped inline override. */
    public static final String OBJECT_MARKER = "setprop:";

    /** Marker for a global (object-independent) inline override. */
    public static final String GLOBAL_MARKER = "setglobalprop:";

    /** Canonical find-type token that makes an object resolve against the global map. */
    public static final String GLOBAL_FIND_TYPE = "GlobalObject";

    private static final Pattern TOKEN_PATTERN = Pattern.compile("#[A-Za-z0-9_.\\-]+");

    /** Optional trailing sub-iteration selector on a value, e.g. {@code |subiter=3}. */
    private static final Pattern SUBITER_PATTERN = Pattern.compile(
        "\\|subiter=(\\d+)\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private InlineObjectProperty() {}

    /** @return {@code true} if the Condition value is an inline override expression. */
    public static boolean isInline(String condition) {
        if (condition == null) {
            return false;
        }
        String c = condition.trim().toLowerCase();
        return c.startsWith(OBJECT_MARKER) || c.startsWith(GLOBAL_MARKER);
    }

    /** @return {@code true} if the expression is the global variant. */
    public static boolean isGlobal(String condition) {
        return condition != null && condition.trim().toLowerCase().startsWith(GLOBAL_MARKER);
    }

    /**
     * Removes the {@code setProp:} / {@code setGlobalProp:} marker and returns the
     * remaining {@code #token=value; ...} payload (trimmed).
     */
    public static String stripMarker(String condition) {
        String c = condition.trim();
        int colon = c.indexOf(':');
        return colon < 0 ? "" : c.substring(colon + 1).trim();
    }

    /**
     * Parses a {@code #token=value; #token2=value2} payload into a list of
     * {@code [token, value, subIter]} triples. Pairs are separated by {@code ;} and
     * the token / value split occurs at the first {@code =} so values may themselves
     * contain {@code =}. An optional trailing {@code |subiter=N} on a value selects a
     * specific data-sheet sub-iteration; {@code subIter} is {@code ""} when absent.
     */
    public static List<String[]> parsePairs(String expr) {
        List<String[]> pairs = new ArrayList<>();
        if (expr == null) {
            return pairs;
        }
        for (String part : expr.split(";")) {
            if (part.trim().isEmpty()) {
                continue;
            }
            int eq = part.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String token = part.substring(0, eq).trim();
            String value = part.substring(eq + 1).trim();
            String subIter = "";
            Matcher sm = SUBITER_PATTERN.matcher(value);
            if (sm.find()) {
                subIter = sm.group(1);
                value = value.substring(0, sm.start()).trim();
            }
            if (!token.isEmpty()) {
                pairs.add(new String[] { token, value, subIter });
            }
        }
        return pairs;
    }

    /**
     * Serializes token/value pairs back into a Condition-cell expression using the
     * appropriate marker. Each pair may be {@code [token, value]} or
     * {@code [token, value, subIter]}; a non-empty {@code subIter} is appended as
     * {@code |subiter=N}. Used by the IDE builder panel for round-trip editing.
     */
    public static String serialize(boolean global, List<String[]> pairs) {
        StringBuilder sb = new StringBuilder(global ? "setGlobalProp: " : "setProp: ");
        for (int i = 0; i < pairs.size(); i++) {
            String[] pair = pairs.get(i);
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(pair[0]).append('=').append(pair[1]);
            if (pair.length > 2 && pair[2] != null && !pair[2].isEmpty()) {
                sb.append("|subiter=").append(pair[2]);
            }
        }
        return sb.toString();
    }

    /**
     * Extracts the distinct {@code #token} placeholders found in an object-repository
     * locator string, in first-seen order. Used to populate the IDE token dropdown.
     */
    public static Set<String> extractTokens(String locator) {
        Set<String> tokens = new LinkedHashSet<>();
        if (locator != null) {
            Matcher m = TOKEN_PATTERN.matcher(locator);
            while (m.find()) {
                tokens.add(m.group());
            }
        }
        return tokens;
    }

    /**
     * Puts an object-scoped property override into a driver's {@code dynamicValue}
     * map, creating the nested maps as needed. Shared by the inline path and the
     * explicit {@code setObjectProperty} actions so the mutation lives in one place.
     */
    public static void putObjectProperty(
        Map<String, Map<String, Map<String, String>>> dynamicValue,
        String reference,
        String objectName,
        String key,
        String value
    ) {
        dynamicValue
            .computeIfAbsent(reference, k -> new HashMap<>())
            .computeIfAbsent(objectName, k -> new HashMap<>())
            .put(key, value);
    }
}
