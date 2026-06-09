package com.ing.engine.cli.output;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Temporarily silences noisy stdout / stderr / JUL output produced by the
 * deeper Datalib bootstrap path (object-repository scans, default settings
 * file creation, etc.).
 *
 * <p>CLI commands like {@code project list} construct {@code Project}
 * instances purely to read metadata; the side-effects those constructors
 * print are useful when running a test but pure noise when listing or
 * introspecting projects.
 *
 * <p>Use with try-with-resources so the original streams and log levels
 * are always restored, even on exceptions:
 *
 * <pre>{@code
 * try (Silencer s = Silencer.aroundProjectLoad()) {
 *     Project p = new Project(path);
 *     // ...
 * }
 * }</pre>
 */
public final class Silencer implements AutoCloseable {
    /** Loggers known to be chatty during project metadata loading. */
    private static final String[] DATALIB_LOGGERS = {
        "com.ing.datalib",
        "com.ing.datalib.or.yaml.YamlORReader",
        "com.ing.datalib.settings"
    };

    private final PrintStream origOut;
    private final PrintStream origErr;
    private final Map<String, Level> savedLevels = new LinkedHashMap<>();

    private Silencer(String[] loggers) {
        this.origOut = System.out;
        this.origErr = System.err;
        PrintStream nul = new PrintStream(OutputStream.nullOutputStream(), true);
        System.setOut(nul);
        System.setErr(nul);
        for (String name : loggers) {
            Logger l = Logger.getLogger(name);
            savedLevels.put(name, l.getLevel());
            l.setLevel(Level.OFF);
        }
    }

    /**
     * Standard silencer for any code path that constructs a {@code Project}
     * just to inspect metadata.
     */
    public static Silencer aroundProjectLoad() {
        return new Silencer(DATALIB_LOGGERS);
    }

    @Override
    public void close() {
        System.setOut(origOut);
        System.setErr(origErr);
        savedLevels.forEach((name, level) -> Logger.getLogger(name).setLevel(level));
    }
}
