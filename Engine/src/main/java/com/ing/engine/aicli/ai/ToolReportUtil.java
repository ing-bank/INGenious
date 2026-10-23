package com.ing.engine.aicli.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Classifies and summarizes completed tool calls for reporting UIs (the AI
 * CLI's final table and the IDE assistant's turn summary). Shared so both
 * front-ends render self-agentic (Copilot SDK) tool activity identically.
 */
public final class ToolReportUtil {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolReportUtil() {}

    /** Best-effort JSON parse; returns null for plain text (e.g. "ok", error strings). */
    public static JsonNode parseJsonQuiet(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        String trimmed = s.trim();
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return null;
        }
        try {
            return MAPPER.readTree(trimmed);
        } catch (Exception e) {
            return null;
        }
    }

    /** Returns one of {@code OK}, {@code INFO}, {@code WARN}, {@code FAIL}. */
    public static String classify(
        String toolName,
        boolean success,
        String summary,
        JsonNode parsed
    ) {
        if (!success) {
            return "FAIL";
        }
        String n = toolName == null ? "" : toolName.toLowerCase(Locale.ROOT);
        if (parsed != null) {
            JsonNode valid = parsed.path("valid");
            if (valid.isBoolean() && !valid.asBoolean()) {
                return "WARN";
            }
            JsonNode errors = parsed.path("errors");
            if (errors.isArray() && errors.size() > 0) {
                return "WARN";
            }
            JsonNode warnings = parsed.path("warnings");
            if (warnings.isArray() && warnings.size() > 0) {
                return "WARN";
            }
        } else {
            String s = summary == null ? "" : summary.toLowerCase(Locale.ROOT);
            if (
                n.contains("validate") &&
                (
                    s.contains("error") ||
                    s.contains("invalid") ||
                    s.contains("fail") ||
                    s.contains("warn")
                )
            ) {
                return "WARN";
            }
        }
        if (
            n.contains("list") ||
            n.contains("show") ||
            n.contains("search") ||
            n.contains("info") ||
            n.contains("get") ||
            n.contains("categories") ||
            n.contains("latest") ||
            n.contains("history") ||
            n.contains("status")
        ) {
            return "INFO";
        }
        return "OK";
    }

    /** Short one-line preview for a compact report row (not the detail box). */
    public static String shortSummary(String summary, boolean success, JsonNode parsed) {
        if (!success) {
            return truncate(summary, 60);
        }
        if (parsed == null) {
            return truncate(summary, 46);
        }
        if (parsed.isArray()) {
            return parsed.size() + (parsed.size() == 1 ? " item" : " items");
        }
        List<String> preview = new ArrayList<>();
        Iterator<String> names = parsed.fieldNames();
        int shown = 0;
        while (names.hasNext() && shown < 4) {
            String key = names.next();
            JsonNode v = parsed.path(key);
            preview.add(v.isContainerNode() && v.size() > 0 ? key + " \u00d7" + v.size() : key);
            shown++;
        }
        int more = parsed.size() - shown;
        String joined = String.join(", ", preview);
        return more > 0 ? joined + " +" + more + " more" : joined;
    }

    /** Collapse to a single line and cap at {@code max} visible chars. */
    public static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        String one = s.replace('\n', ' ').replace('\r', ' ').trim();
        return one.length() <= max ? one : one.substring(0, Math.max(0, max - 1)) + "\u2026";
    }
}
