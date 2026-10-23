package com.ing.engine.cli.lib;

import com.ing.engine.constants.SystemDefaults;

/**
 * Applies the headless capability override used by the CLI commands.
 *
 * <p>{@code -op_setHeadless} is not a legacy engine option, so headless mode is
 * requested through the working capability override that
 * {@code ProjectRunner.overrideWithEnv} reads.
 */
public final class HeadlessOverride {
    private static final String KEY_PREFIX = "capability.";
    private static final String KEY_SUFFIX = ".setHeadless";

    private HeadlessOverride() {}

    /**
     * Registers {@code capability.<browser>.setHeadless=true} for the given browser.
     *
     * @param browser the browser name; normalized via {@link BrowserNames#normalize(String)}
     */
    public static void apply(final String browser) {
        SystemDefaults.EnvVars.put(
            KEY_PREFIX + BrowserNames.normalize(browser) + KEY_SUFFIX,
            "true"
        );
    }

    /**
     * Registers the headless override only when {@code headless} is {@code true}.
     *
     * @param browser the browser name; normalized via {@link BrowserNames#normalize(String)}
     * @param headless whether headless mode was requested
     */
    public static void applyIf(final boolean headless, final String browser) {
        if (headless) {
            apply(browser);
        }
    }
}
