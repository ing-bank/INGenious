package com.ing.ide.main.mainui.components.aichat.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal, dependency-free Markdown to HTML converter for rendering chat
 * messages in the WebView transcript. Supports the common subset produced by
 * chat models: fenced code blocks, inline code, bold, italic, headings,
 * unordered/ordered lists, and paragraphs. All non-code text is HTML-escaped
 * first to avoid injecting markup from model output.
 */
public final class MarkdownRenderer {
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC = Pattern.compile(
        "(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)"
    );
    private static final Pattern INLINE_CODE = Pattern.compile("`([^`]+?)`");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]+)\\]\\(([^)\\s]+)\\)");
    private static final Pattern TABLE_SEP = Pattern.compile(
        "^\\s*\\|?\\s*:?-{1,}:?\\s*(\\|\\s*:?-{1,}:?\\s*)*\\|?\\s*$"
    );

    private MarkdownRenderer() {}

    /** Converts a Markdown string to an HTML fragment. */
    public static String toHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }
        String[] lines = markdown.split("\n", -1);
        StringBuilder out = new StringBuilder();
        boolean inCode = false;
        boolean inUl = false;
        boolean inOl = false;
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            if (trimmed.startsWith("```")) {
                if (inCode) {
                    out
                        .append("<pre><code>")
                        .append(escape(code.toString()))
                        .append("</code></pre>");
                    code.setLength(0);
                    inCode = false;
                } else {
                    inCode = true;
                    closeLists(out, inUl, inOl);
                    inUl = false;
                    inOl = false;
                }
                continue;
            }
            if (inCode) {
                code.append(line).append("\n");
                continue;
            }

            if (trimmed.isEmpty()) {
                closeLists(out, inUl, inOl);
                inUl = false;
                inOl = false;
                continue;
            }

            // GFM table: a header row immediately followed by a separator row.
            if (trimmed.contains("|") && i + 1 < lines.length && isTableSeparator(lines[i + 1])) {
                closeLists(out, inUl, inOl);
                inUl = false;
                inOl = false;
                i = renderTable(out, lines, i);
                continue;
            }

            Matcher heading = Pattern.compile("^(#{1,6})\\s+(.*)$").matcher(trimmed);
            if (heading.matches()) {
                closeLists(out, inUl, inOl);
                inUl = false;
                inOl = false;
                int level = heading.group(1).length();
                out
                    .append("<h")
                    .append(level)
                    .append('>')
                    .append(inline(heading.group(2)))
                    .append("</h")
                    .append(level)
                    .append('>');
                continue;
            }

            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                if (!inUl) {
                    closeLists(out, false, inOl);
                    inOl = false;
                    out.append("<ul>");
                    inUl = true;
                }
                out.append("<li>").append(inline(trimmed.substring(2))).append("</li>");
                continue;
            }

            Matcher ol = Pattern.compile("^\\d+\\.\\s+(.*)$").matcher(trimmed);
            if (ol.matches()) {
                if (!inOl) {
                    closeLists(out, inUl, false);
                    inUl = false;
                    out.append("<ol>");
                    inOl = true;
                }
                out.append("<li>").append(inline(ol.group(1))).append("</li>");
                continue;
            }

            closeLists(out, inUl, inOl);
            inUl = false;
            inOl = false;
            out.append("<p>").append(inline(trimmed)).append("</p>");
        }

        if (inCode) {
            out.append("<pre><code>").append(escape(code.toString())).append("</code></pre>");
        }
        closeLists(out, inUl, inOl);
        return out.toString();
    }

    private static void closeLists(StringBuilder out, boolean inUl, boolean inOl) {
        if (inUl) {
            out.append("</ul>");
        }
        if (inOl) {
            out.append("</ol>");
        }
    }

    private static String inline(String text) {
        // Escape first so that user/model text cannot inject HTML, then apply
        // inline markdown by replacing escaped delimiters.
        String escaped = escape(text);
        escaped = INLINE_CODE.matcher(escaped).replaceAll("<code>$1</code>");
        // Links become non-navigating styled spans (WebView must not load away).
        escaped = LINK.matcher(escaped).replaceAll("<span class='lnk' title='$2'>$1</span>");
        escaped = BOLD.matcher(escaped).replaceAll("<strong>$1</strong>");
        escaped = ITALIC.matcher(escaped).replaceAll("<em>$1</em>");
        return escaped;
    }

    private static boolean isTableSeparator(String line) {
        return line != null && line.contains("-") && TABLE_SEP.matcher(line.trim()).matches();
    }

    /**
     * Renders a GFM table whose header is at index {@code h} (the next line is
     * the separator). Returns the index of the last consumed line.
     */
    private static int renderTable(StringBuilder out, String[] lines, int h) {
        List<String> headers = splitCells(lines[h]);
        out.append("<table><thead><tr>");
        for (String cell : headers) {
            out.append("<th>").append(inline(cell)).append("</th>");
        }
        out.append("</tr></thead><tbody>");
        int i = h + 2; // skip header row + separator row
        for (; i < lines.length; i++) {
            String t = lines[i].trim();
            if (t.isEmpty() || !t.contains("|")) {
                break;
            }
            List<String> cells = splitCells(lines[i]);
            out.append("<tr>");
            for (int c = 0; c < headers.size(); c++) {
                String v = c < cells.size() ? cells.get(c) : "";
                out.append("<td>").append(inline(v)).append("</td>");
            }
            out.append("</tr>");
        }
        out.append("</tbody></table>");
        return i - 1;
    }

    /** Splits a table row into trimmed cell strings, ignoring the outer pipes. */
    private static List<String> splitCells(String row) {
        String t = row.trim();
        if (t.startsWith("|")) {
            t = t.substring(1);
        }
        if (t.endsWith("|")) {
            t = t.substring(0, t.length() - 1);
        }
        String[] parts = t.split("\\|", -1);
        List<String> cells = new ArrayList<>();
        for (String p : parts) {
            cells.add(p.trim());
        }
        return cells;
    }

    /** Escapes HTML-special characters. */
    public static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
