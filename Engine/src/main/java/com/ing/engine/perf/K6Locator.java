package com.ing.engine.perf;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Locates and validates the k6 load-generator binary.
 *
 * <p>Resolution order:
 * <ol>
 *   <li>Global config key {@code perf.k6.path} in {@code ~/.ingenious/config.properties}</li>
 *   <li>{@code k6} on the system PATH</li>
 *   <li>A managed install at {@code ~/.ingenious/k6/k6[.exe]}</li>
 * </ol>
 *
 * <p>INGenious never bundles the binary; when it is missing the caller should
 * surface {@link #installHint()}.
 */
public final class K6Locator {
    /** Global config key holding an explicit path to the k6 binary. */
    public static final String CONFIG_KEY = "perf.k6.path";

    private K6Locator() {}

    /**
     * Resolve the k6 binary, or {@code null} when not found anywhere.
     */
    public static String resolve() {
        String configured = configuredPath();
        if (configured != null) {
            return configured;
        }
        String onPath = whichK6();
        if (onPath != null) {
            return onPath;
        }
        return managedInstall();
    }

    /** True when a usable k6 binary was found. */
    public static boolean available() {
        return resolve() != null;
    }

    /**
     * First line of {@code k6 version} for the given binary, or {@code null}
     * when it cannot be executed.
     */
    public static String version(String k6Path) {
        if (k6Path == null) {
            return null;
        }
        String out = runAndCapture(new String[] { k6Path, "version" }, 8);
        if (out == null || out.isEmpty()) {
            return null;
        }
        int nl = out.indexOf('\n');
        return nl > 0 ? out.substring(0, nl).trim() : out.trim();
    }

    /** Human-actionable installation hint for the current OS. */
    public static String installHint() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (os.contains("mac")) {
            return (
                "Install k6 with: brew install k6 (or set " +
                CONFIG_KEY +
                " in ~/.ingenious/config.properties)"
            );
        }
        if (os.contains("win")) {
            return "Install k6 with: winget install k6 --source winget (or choco install k6)";
        }
        return (
            "Install k6: https://grafana.com/docs/k6/latest/set-up/install-k6/ (or set " +
            CONFIG_KEY +
            ")"
        );
    }

    // ------------------------------------------------------------------
    // internals
    // ------------------------------------------------------------------

    private static String configuredPath() {
        File config = new File(System.getProperty("user.home"), ".ingenious/config.properties");
        if (!config.isFile()) {
            return null;
        }
        try (InputStream in = new FileInputStream(config)) {
            Properties props = new Properties();
            props.load(in);
            String value = props.getProperty(CONFIG_KEY);
            if (value == null || value.trim().isEmpty()) {
                return null;
            }
            File f = new File(value.trim());
            return f.isFile() && f.canExecute() ? f.getAbsolutePath() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String whichK6() {
        boolean windows = System
            .getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .contains("win");
        String probe = windows ? "where" : "which";
        String out = runAndCapture(new String[] { probe, "k6" }, 8);
        if (out == null || out.isEmpty()) {
            return null;
        }
        int nl = out.indexOf('\n');
        String first = (nl > 0 ? out.substring(0, nl) : out).trim();
        File f = new File(first);
        return f.isFile() ? f.getAbsolutePath() : null;
    }

    private static String managedInstall() {
        boolean windows = System
            .getProperty("os.name", "")
            .toLowerCase(Locale.ROOT)
            .contains("win");
        File f = new File(
            System.getProperty("user.home"),
            ".ingenious/k6/" + (windows ? "k6.exe" : "k6")
        );
        return f.isFile() && f.canExecute() ? f.getAbsolutePath() : null;
    }

    /**
     * Run a short-lived command and return its combined stdout/stderr, or
     * {@code null} on failure / non-zero exit.
     */
    private static String runAndCapture(String[] cmd, int timeoutSeconds) {
        try {
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            String out = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean done = p.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!done) {
                p.destroyForcibly();
                return null;
            }
            return p.exitValue() == 0 ? out : null;
        } catch (Exception e) {
            return null;
        }
    }
}
