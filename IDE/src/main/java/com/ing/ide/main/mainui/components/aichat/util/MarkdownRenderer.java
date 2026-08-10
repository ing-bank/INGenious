package com.ing.ide.main.mainui.components.aichat.util;

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

        for (String line : lines) {
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
        escaped = BOLD.matcher(escaped).replaceAll("<strong>$1</strong>");
        escaped = ITALIC.matcher(escaped).replaceAll("<em>$1</em>");
        return escaped;
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
