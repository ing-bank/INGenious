package com.ing.engine.aicli.ui;

import java.util.ArrayList;
import java.util.List;

/** Boxed panels with rounded corners (Claude-Code-style) for the AI CLI. */
public final class Panels {
    private final Theme t;

    public Panels(Theme theme) {
        this.t = theme;
    }

    /**
     * Word-wraps {@code line} to at most {@code maxWidth} visible (ANSI-stripped)
     * characters, splitting on spaces so embedded colour escapes never get cut.
     * Lines already within the limit are returned unchanged.
     */
    public static List<String> wrap(String line, int maxWidth) {
        if (Theme.visibleLength(line) <= maxWidth) {
            return List.of(line);
        }
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : line.split(" ", -1)) {
            String candidate = cur.length() == 0 ? word : cur + " " + word;
            if (Theme.visibleLength(candidate) > maxWidth && cur.length() > 0) {
                out.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(candidate);
            }
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }

    /** Render a rounded box; title may be null. */
    public String box(String title, List<String> lines) {
        int width = title == null ? 0 : Theme.visibleLength(title) + 2;
        for (String l : lines) {
            width = Math.max(width, Theme.visibleLength(l));
        }
        width = Math.min(Math.max(width + 4, 30), 96);

        StringBuilder sb = new StringBuilder();
        String horiz = "\u2500".repeat(width - 2);
        if (title != null && !title.isEmpty()) {
            String inner = " " + title + " ";
            int pad = width - 2 - Theme.visibleLength(inner);
            int left = Math.max(0, pad / 2);
            int right = Math.max(0, pad - left);
            sb
                .append(t.purple("\u256d" + "\u2500".repeat(left)))
                .append(t.bold(inner))
                .append(t.purple("\u2500".repeat(right) + "\u256e"))
                .append('\n');
        } else {
            sb.append(t.purple("\u256d" + horiz + "\u256e")).append('\n');
        }
        for (String l : lines) {
            int pad = width - 4 - Theme.visibleLength(l);
            sb
                .append(t.purple("\u2502"))
                .append(' ')
                .append(l)
                .append(" ".repeat(Math.max(0, pad)))
                .append(' ')
                .append(t.purple("\u2502"))
                .append('\n');
        }
        sb.append(t.purple("\u2570" + horiz + "\u256f"));
        return sb.toString();
    }

    public void print(String title, List<String> lines) {
        System.out.println(box(title, lines));
    }
}
