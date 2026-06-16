package com.ing.engine.cli;

import com.ing.engine.constants.SystemDefaults;
import picocli.CommandLine.IVersionProvider;

/**
 * Supplies the version string for picocli's {@code --version} flag.
 *
 * <p>The version itself comes from the single source of truth: the Maven
 * {@code project.version} that resource-filtering writes into
 * {@code engine/build.properties} at build time
 * (see {@link SystemDefaults#getBuildVersion()}).
 *
 * <p>Output is multi-line and styled with the brand purple ({@code #7724FF})
 * to match the rest of the CLI. Colours are suppressed automatically when
 * stdout is not a terminal (e.g. piped to a file or CI logs) or when
 * {@code NO_COLOR} is set.
 */
public class INGeniousVersionProvider implements IVersionProvider {

    @Override
    public String[] getVersion() {
        boolean color = useColor();

        String purple = color ? "\u001b[38;2;119;36;255m" : "";
        String light = color ? "\u001b[38;2;180;140;255m" : "";
        String bold = color ? "\u001b[1m" : "";
        String dim = color ? "\u001b[2m" : "";
        String cyan = color ? "\u001B[36m" : "";
        String reset = color ? "\u001b[0m" : "";

        String version = SystemDefaults.getBuildVersion();
        String javaVer = System.getProperty("java.version", "?");
        String javaVend = System.getProperty("java.vendor", "?");
        String osName = System.getProperty("os.name", "?");
        String osArch = System.getProperty("os.arch", "?");

        return new String[] {
            "",
            "  " +
            bold +
            purple +
            "INGenious" +
            reset +
            " " +
            bold +
            light +
            "CLI" +
            reset +
            "  " +
            dim +
            "v" +
            reset +
            bold +
            version +
            reset,
            "",
            "  " + dim + "Build  :" + reset + " " + version,
            "  " +
            dim +
            "Java   :" +
            reset +
            " " +
            javaVer +
            " " +
            dim +
            "(" +
            javaVend +
            ")" +
            reset,
            "  " + dim + "OS     :" + reset + " " + osName + " " + dim + osArch + reset,
            "",
            "  " +
            cyan +
            "→" +
            reset +
            " " +
            dim +
            "https://github.com/INGenious-Test-Automation" +
            reset,
            ""
        };
    }

    /**
     * Decide whether to emit ANSI colour. Honours the de-facto
     * {@code NO_COLOR} environment variable and skips colour when stdout
     * isn't attached to a terminal (so piped output stays clean).
     */
    private boolean useColor() {
        if (System.getenv("NO_COLOR") != null) {
            return false;
        }
        if ("true".equalsIgnoreCase(System.getProperty("ingenious.no-color"))) {
            return false;
        }
        // System.console() returns null when stdout is redirected
        return System.console() != null;
    }
}
