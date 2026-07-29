package com.ing.datalib.api.importer.bruno;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tiny line-oriented parser for the Bruno {@code .bru} block DSL.
 * <p>
 * A {@code .bru} file is a sequence of named blocks:
 * <pre>
 * blockname {
 *   key: value
 *   ...
 * }
 *
 * body:json {
 *   { "a": 1 }
 * }
 * </pre>
 * Block names may contain a colon (e.g. {@code body:json}, {@code auth:bearer}).
 * Body blocks contain free-form multi-line content.
 */
public final class BrunoParser {

    /** A parsed block. {@link #raw} holds the inner text verbatim (for body/script blocks). */
    public static final class Block {
        public final String name;
        public final Map<String, String> entries;
        public final String raw;

        Block(String name, Map<String, String> entries, String raw) {
            this.name = name;
            this.entries = entries;
            this.raw = raw;
        }
    }

    private BrunoParser() {}

    public static List<Block> parseFile(Path file) throws IOException {
        return parseLines(Files.readAllLines(file));
    }

    public static List<Block> parseString(String content) {
        List<String> lines = new ArrayList<>();
        for (String line : content.split("\r?\n", -1)) {
            lines.add(line);
        }
        return parseLines(lines);
    }

    public static List<Block> parseLines(List<String> lines) {
        List<Block> blocks = new ArrayList<>();
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#")) {
                i++;
                continue;
            }
            // Block header:  name {
            int braceIdx = trimmed.indexOf('{');
            if (braceIdx > 0) {
                String name = trimmed.substring(0, braceIdx).trim();
                StringBuilder inner = new StringBuilder();
                int depth = 1;
                // tail after the opening brace on the same line (rare but tolerate it)
                String tail = trimmed.substring(braceIdx + 1);
                if (!tail.isEmpty()) {
                    depth += countUnescaped(tail, '{');
                    depth -= countUnescaped(tail, '}');
                    if (depth > 0) inner.append(tail).append('\n');
                }
                i++;
                while (i < lines.size() && depth > 0) {
                    String l = lines.get(i);
                    int opens = countUnescaped(l, '{');
                    int closes = countUnescaped(l, '}');
                    if (depth + opens - closes <= 0) {
                        // last line — strip the final '}' only
                        int lastClose = l.lastIndexOf('}');
                        if (lastClose >= 0) {
                            String before = l.substring(0, lastClose);
                            if (!before.trim().isEmpty()) inner.append(before).append('\n');
                        }
                        depth = 0;
                        i++;
                        break;
                    } else {
                        inner.append(l).append('\n');
                        depth += opens - closes;
                        i++;
                    }
                }
                String raw = inner.toString();
                Map<String, String> entries = parseEntries(raw);
                blocks.add(new Block(name, entries, raw));
            } else {
                i++;
            }
        }
        return blocks;
    }

    private static int countUnescaped(String s, char c) {
        int n = 0;
        boolean inString = false;
        char quote = 0;
        for (int k = 0; k < s.length(); k++) {
            char ch = s.charAt(k);
            if (inString) {
                if (ch == '\\' && k + 1 < s.length()) {
                    k++;
                    continue;
                }
                if (ch == quote) inString = false;
            } else {
                if (ch == '"' || ch == '\'') {
                    inString = true;
                    quote = ch;
                    continue;
                }
                if (ch == c) n++;
            }
        }
        return n;
    }

    private static Map<String, String> parseEntries(String inner) {
        // Best-effort: extract `key: value` pairs at top-level (one per line).
        Map<String, String> map = new LinkedHashMap<>();
        for (String line : inner.split("\r?\n")) {
            String t = line.trim();
            if (t.isEmpty() || t.startsWith("//") || t.startsWith("#")) continue;
            int colon = t.indexOf(':');
            if (colon <= 0) continue;
            String key = t.substring(0, colon).trim();
            String val = t.substring(colon + 1).trim();
            // Strip optional trailing comment
            int commentIdx = -1;
            boolean inStr = false;
            char q = 0;
            for (int k = 0; k < val.length(); k++) {
                char ch = val.charAt(k);
                if (inStr) {
                    if (ch == q) inStr = false;
                } else if (ch == '"' || ch == '\'') {
                    inStr = true;
                    q = ch;
                } else if (
                    ch == '/' &&
                    k + 1 < val.length() &&
                    val.charAt(k + 1) == '/' &&
                    (k == 0 || Character.isWhitespace(val.charAt(k - 1)))
                ) {
                    commentIdx = k;
                    break;
                }
            }
            if (commentIdx >= 0) val = val.substring(0, commentIdx).trim();
            // Strip surrounding quotes
            if (val.length() >= 2) {
                char c0 = val.charAt(0), c1 = val.charAt(val.length() - 1);
                if ((c0 == '"' || c0 == '\'') && c0 == c1) {
                    val = val.substring(1, val.length() - 1);
                }
            }
            map.put(key, val);
        }
        return map;
    }
}
