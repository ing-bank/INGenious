package com.ing.engine.cli.output;

/**
 * Centralised ANSI styling for CLI output.
 *
 * <p>All colour codes live here so every subcommand renders consistently.
 * Each helper respects the {@code colored} flag: when {@code false} every
 * style method returns the bare text, which keeps output legible on
 * terminals that don't support ANSI (CI logs, plain {@code less}, …) or
 * when the user passes {@code --no-color}.
 *
 * <p>Palette is deliberately small — we lean on bold/dim for hierarchy
 * rather than introducing many hues — so screen-reader and colour-blind
 * users still get a usable structure via the bullets/arrows alone.
 */
public final class Style {

    // 256-colour ANSI escapes — work in every modern terminal and over SSH.
    private static final String RESET   = "\u001B[0m";
    private static final String BOLD    = "\u001B[1m";
    private static final String DIM     = "\u001B[2m";
    private static final String ITALIC  = "\u001B[3m";

    private static final String FG_RED     = "\u001B[31m";
    private static final String FG_GREEN   = "\u001B[32m";
    private static final String FG_YELLOW  = "\u001B[33m";
    private static final String FG_BLUE    = "\u001B[34m";
    private static final String FG_MAGENTA = "\u001B[35m";
    private static final String FG_CYAN    = "\u001B[36m";
    private static final String FG_WHITE   = "\u001B[97m";
    private static final String FG_GREY    = "\u001B[90m";

    // Iconography — UTF-8 glyphs render on every modern terminal.
    public static final String ICON_OK     = "✓";
    public static final String ICON_ERR    = "✗";
    public static final String ICON_WARN   = "⚠";
    public static final String ICON_INFO   = "▸";
    public static final String ICON_BULLET = "•";
    public static final String ICON_ARROW  = "→";

    private final boolean colored;

    public Style(boolean colored) {
        this.colored = colored;
    }

    public boolean isColored() {
        return colored;
    }

    private String wrap(String code, Object value) {
        return colored ? code + value + RESET : String.valueOf(value);
    }

    public String bold(Object v)    { return wrap(BOLD, v); }
    public String dim(Object v)     { return wrap(DIM, v); }
    public String italic(Object v)  { return wrap(ITALIC, v); }

    public String red(Object v)     { return wrap(FG_RED, v); }
    public String green(Object v)   { return wrap(FG_GREEN, v); }
    public String yellow(Object v)  { return wrap(FG_YELLOW, v); }
    public String blue(Object v)    { return wrap(FG_BLUE, v); }
    public String magenta(Object v) { return wrap(FG_MAGENTA, v); }
    public String cyan(Object v)    { return wrap(FG_CYAN, v); }
    public String white(Object v)   { return wrap(FG_WHITE, v); }
    public String grey(Object v)    { return wrap(FG_GREY, v); }

    /** Bold + cyan — for section titles. */
    public String header(Object v) {
        return colored ? BOLD + FG_CYAN + v + RESET : String.valueOf(v);
    }

    /** Bold + green — for success summaries. */
    public String success(Object v) {
        return colored ? BOLD + FG_GREEN + v + RESET : String.valueOf(v);
    }

    /** Bold + red — for fatal errors. */
    public String failure(Object v) {
        return colored ? BOLD + FG_RED + v + RESET : String.valueOf(v);
    }

    /** Bold + yellow — for warnings / advisories. */
    public String warning(Object v) {
        return colored ? BOLD + FG_YELLOW + v + RESET : String.valueOf(v);
    }
}
