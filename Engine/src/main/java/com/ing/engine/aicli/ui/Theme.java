package com.ing.engine.aicli.ui;

import java.util.regex.Pattern;

/**
 * Terminal styling for the interactive AI CLI. Honours {@code NO_COLOR} and
 * degrades to plain text when stdout is not a terminal.
 */
public final class Theme {
    private static final Pattern ANSI = Pattern.compile("\u001b\\[[0-9;]*m");

    public static final String CHECK = "\u2713"; // ✓
    public static final String CROSS = "\u2717"; // ✗
    public static final String WARN = "\u26a0"; // ⚠
    public static final String ARROW = "\u276f"; // ❯
    public static final String DOT = "\u25cf"; // ●

    private final boolean ansi;

    public Theme(boolean ansi) {
        this.ansi = ansi;
    }

    public static Theme auto() {
        boolean enabled = System.getenv("NO_COLOR") == null && System.console() != null;
        return new Theme(enabled);
    }

    public boolean ansiEnabled() {
        return ansi;
    }

    private String wrap(String code, String s) {
        return ansi ? code + s + "\u001b[0m" : s;
    }

    public String bold(String s) {
        return wrap("\u001b[1m", s);
    }

    public String dim(String s) {
        return wrap("\u001b[2m", s);
    }

    public String purple(String s) {
        return wrap("\u001b[38;2;119;36;255m", s);
    }

    public String brightPurple(String s) {
        return wrap("\u001b[38;2;147;92;255m", s);
    }

    public String green(String s) {
        return wrap("\u001b[32m", s);
    }

    public String red(String s) {
        return wrap("\u001b[31m", s);
    }

    public String yellow(String s) {
        return wrap("\u001b[33m", s);
    }

    public String cyan(String s) {
        return wrap("\u001b[36m", s);
    }

    public String gray(String s) {
        return wrap("\u001b[90m", s);
    }

    public String ok(String s) {
        return green(CHECK) + " " + s;
    }

    public String fail(String s) {
        return red(CROSS) + " " + s;
    }

    public static String stripAnsi(String s) {
        return s == null ? "" : ANSI.matcher(s).replaceAll("");
    }

    public static int visibleLength(String s) {
        return stripAnsi(s).length();
    }
}
