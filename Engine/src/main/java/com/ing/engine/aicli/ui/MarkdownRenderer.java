package com.ing.engine.aicli.ui;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal terminal Markdown renderer for AI answers: headings, bullets,
 * fenced code blocks, inline {@code `code`} and {@code **bold**}.
 */
public final class MarkdownRenderer {
    private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern CODE = Pattern.compile("`([^`]+)`");

    private final Theme t;

    public MarkdownRenderer(Theme theme) {
        this.t = theme;
    }

    public String render(String markdown) {
        if (markdown == null) return "";
        StringBuilder out = new StringBuilder();
        boolean inCode = false;
        for (String line : markdown.split("\n", -1)) {
            String trimmed = line.strip();
            if (trimmed.startsWith("```")) {
                inCode = !inCode;
                continue;
            }
            if (inCode) {
                out.append("  ").append(t.gray(line)).append('\n');
                continue;
            }
            if (trimmed.startsWith("#")) {
                String text = trimmed.replaceFirst("^#+\\s*", "");
                out.append('\n').append(t.bold(t.brightPurple(text))).append('\n');
                continue;
            }
            String rendered = line;
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                int idx = line.indexOf(trimmed);
                rendered = line.substring(0, idx) + t.purple("\u2022") + " " + trimmed.substring(2);
            }
            rendered = replace(BOLD, rendered, t::bold);
            rendered = replace(CODE, rendered, t::cyan);
            out.append(rendered).append('\n');
        }
        return out.toString().stripTrailing();
    }

    public void print(String markdown) {
        System.out.println(render(markdown));
    }

    private static String replace(
        Pattern p,
        String input,
        java.util.function.UnaryOperator<String> fn
    ) {
        Matcher m = p.matcher(input);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(fn.apply(m.group(1))));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
