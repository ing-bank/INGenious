package com.ing.engine.drivers;

import static com.ing.engine.execution.data.DataProcessor.resolve;

import com.galenframework.config.GalenConfig;
import com.galenframework.config.GalenProperty;
import com.ing.datalib.settings.ProjectSettings;
import com.ing.engine.core.Control;
import com.ing.engine.core.RunContext;
import com.ing.engine.drivers.customWebDriver.EmptyDriver;
import com.ing.engine.drivers.findObjectBy.support.ByObjectProp;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

public class WebDriverFactory {
    static HashMap<String, Boolean> driverInformation;

    public static WebDriver create(RunContext context, ProjectSettings settings)
        throws MalformedURLException {
        return create(context, settings, false, null);
    }

    public static void initDriverLocation(ProjectSettings settings) {
        ByObjectProp.load();
        System.setProperty(
            "webdriver.edge.driver",
            resolve(settings.getDriverSettings().getEdgeDriverPath())
        );
        GalenConfig
            .getConfig()
            .setProperty(
                GalenProperty.SCREENSHOT_FULLPAGE,
                String.valueOf(
                    Control.exe.getExecSettings().getRunSettings().getTakeFullPageScreenShot()
                )
            );
        GalenConfig.getConfig().setProperty(GalenProperty.SCREENSHOT_AUTORESIZE, "false");

        GalenConfig.getConfig().setProperty(GalenProperty.SCREENSHOT_FULLPAGE_SCROLLWAIT, "200");
    }

    private static WebDriver create(
        RunContext context,
        ProjectSettings settings,
        Boolean isGrid,
        String remoteUrl
    )
        throws MalformedURLException {
        if (context.BrowserName.equals("No Browser")) {
            return new EmptyDriver();
        } else {
            DesiredCapabilities caps = new DesiredCapabilities();
            caps = getEmulatorCapabilities(context, settings);
            return create(context.BrowserName, caps, settings);
        }
    }

    private static DesiredCapabilities getEmulatorCapabilities(
        RunContext context,
        ProjectSettings settings
    ) {
        Properties prop = settings.getCapabilities().getCapabiltiesFor(context.BrowserName);
        String url = settings.resolveRemoteUrl(context.BrowserName);
        if (url == null) {
            url = "";
        }
        DesiredCapabilities caps = new DesiredCapabilities();
        HashMap<String, Object> ltOptions = new HashMap<String, Object>();
        driverInformation = new HashMap<String, Boolean>();
        driverInformation.put("isLambdaExecution", false);
        driverInformation.put("isIOSNative", false);
        driverInformation.put("isAndroidNative", false);

        // Collect applied capabilities (keyed by the final capability name actually
        // sent to the driver) so we can print a single pretty boxed log later.
        java.util.LinkedHashMap<String, String> appliedCaps = new java.util.LinkedHashMap<>();
        boolean isLambda = url.endsWith("hub.lambdatest.com/wd/hub");

        if (prop != null) {
            for (Object key : prop.keySet()) {
                String capability = key.toString().trim().replace("appium:", "");
                String value = prop.getProperty(key.toString());
                if (value == null) {
                    continue;
                }
                value = value.trim();
                // Skip empty values and empty collections – LambdaTest (and Appium)
                // rejects them (e.g. "The capability X has value [] is not supported").
                if (isBlankCapabilityValue(value)) {
                    continue;
                }
                if (capability.equalsIgnoreCase("platformName")) {
                    checkPlatformName(prop, key, capability);
                }
                // Coerce the raw string into a proper JSON type (Boolean / Integer /
                // List / Map / String) so remote ends like LambdaTest don't reject
                // e.g. `"autoLaunch" must be of type boolean`.
                Object typedValue = coerceCapabilityValue(capability, value);
                if (isLambda) {
                    ltOptions.put(capability, typedValue);
                    appliedCaps.put("lt:options." + capability, String.valueOf(typedValue));
                } else {
                    if (
                        capability.contains("platformName") ||
                        capability.toLowerCase().contains("browsername")
                    ) {
                        caps.setCapability(capability, typedValue);
                        appliedCaps.put(capability, String.valueOf(typedValue));
                    } else {
                        caps.setCapability("appium:" + capability, typedValue);
                        appliedCaps.put("appium:" + capability, String.valueOf(typedValue));
                    }
                }
            }
        }
        if (isLambda) {
            driverInformation.put("isLambdaExecution", true);
            ltOptions.put("name", context.TestCase);
            appliedCaps.put("lt:options.name", context.TestCase);
            // Mirror Browser Testing (PlaywrightDriverFactory) behaviour: if the user
            // hasn't supplied a "build" capability, auto-generate one as
            // "<Scenario> - <executionStartTime>".
            Object existingBuild = ltOptions.get("build");
            if (existingBuild == null || String.valueOf(existingBuild).trim().isEmpty()) {
                String defaultBuild = context.Scenario + " - " + Control.executionStartTime;
                ltOptions.put("build", defaultBuild);
                appliedCaps.put("lt:options.build", defaultBuild);
            }
            caps.setCapability("lt:options", ltOptions);
        }

        logConnectionDetails(
            context.BrowserName,
            resolveCapabilitySource(context.BrowserName, settings),
            url,
            appliedCaps
        );
        return caps;
    }

    /**
     * Returns true when a capability value should NOT be sent to the driver.
     * Skips null/empty strings and the textual forms of empty collections ("[]", "{}").
     */
    private static boolean isBlankCapabilityValue(String value) {
        if (value == null) {
            return true;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return true;
        }
        return v.equals("[]") || v.equals("{}");
    }

    /**
     * Capability keys that must always be serialised as {@link Long} when the
     * user supplies a numeric value. LambdaTest's schema rejects e.g. an
     * {@code Integer} or {@code Boolean} here, so we force the type even when
     * the value would otherwise fit in an {@code int}.
     */
    private static final java.util.Set<String> LONG_ONLY_CAPABILITIES = new java.util.HashSet<>(
        java.util.Arrays.asList("waitforidletimeout")
    );

    /**
     * Convert a raw capability string into the most appropriate JSON type.
     * Remote drivers (Appium, LambdaTest) validate types strictly, so
     * {@code "TRUE"} becomes {@link Boolean#TRUE}, {@code "60"} becomes
     * {@link Integer}, JSON-shaped strings are parsed via Jackson, and
     * everything else remains a String.
     *
     * <p>If {@code capability} is in {@link #LONG_ONLY_CAPABILITIES}, numeric
     * values are returned as {@link Long} and boolean-looking strings are
     * passed through untouched.
     */
    private static Object coerceCapabilityValue(String capability, String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return v;
        }
        boolean longOnly =
            capability != null && LONG_ONLY_CAPABILITIES.contains(capability.toLowerCase());
        if (longOnly) {
            if (v.matches("-?\\d+")) {
                try {
                    return Long.parseLong(v);
                } catch (NumberFormatException ignore) {
                    // fall through to default string
                }
            }
            return v;
        }
        return coerceCapabilityValue(v);
    }

    /**
     * Convert a raw capability string into the most appropriate JSON type.
     * Remote drivers (Appium, LambdaTest) validate types strictly, so
     * {@code "TRUE"} becomes {@link Boolean#TRUE}, {@code "60"} becomes
     * {@link Integer}, JSON-shaped strings are parsed via Jackson, and
     * everything else remains a String.
     */
    private static Object coerceCapabilityValue(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return v;
        }
        // Boolean
        if (v.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (v.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        // Integer / Long
        if (v.matches("-?\\d+")) {
            try {
                long l = Long.parseLong(v);
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                    return (int) l;
                }
                return l;
            } catch (NumberFormatException ignore) {
                // fall through
            }
        }
        // Double
        if (v.matches("-?\\d+\\.\\d+")) {
            try {
                return Double.parseDouble(v);
            } catch (NumberFormatException ignore) {
                // fall through
            }
        }
        // JSON array or object
        if ((v.startsWith("[") && v.endsWith("]")) || (v.startsWith("{") && v.endsWith("}"))) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(v, Object.class);
            } catch (Exception ignore) {
                // fall back to raw string if it isn't valid JSON
            }
        }
        return v;
    }

    private static String resolveCapabilitySource(String name, ProjectSettings settings) {
        if (settings.getDevices() != null && settings.getDevices().getDevice(name) != null) {
            return "Manage Devices (Devices.json)";
        }
        // SAP and any legacy unmigrated entries still live in Emulators.json.
        if (settings.getEmulators() != null && settings.getEmulators().getEmulator(name) != null) {
            return "Manage Browsers (Emulators.json - legacy)";
        }
        return "Unknown";
    }

    private static void logConnectionDetails(
        String name,
        String source,
        String url,
        java.util.LinkedHashMap<String, String> caps
    ) {
        final int width = 78;
        String top = "┌" + repeat("─", width) + "┐";
        String mid = "├" + repeat("─", width) + "┤";
        String bottom = "└" + repeat("─", width) + "┘";

        StringBuilder sb = new StringBuilder();
        sb.append('\n').append(top).append('\n');
        sb.append(padLine("  \uD83D\uDE80  Establishing driver connection", width)).append('\n');
        sb.append(mid).append('\n');
        sb.append(padLine("  Device / Browser : " + nullSafe(name), width)).append('\n');
        sb.append(padLine("  Source           : " + nullSafe(source), width)).append('\n');
        sb
            .append(padLine("  Remote URL       : " + nullSafe(maskUrlCredentials(url)), width))
            .append('\n');
        sb.append(mid).append('\n');
        sb.append(padLine("  Capabilities (" + caps.size() + "):", width)).append('\n');
        for (java.util.Map.Entry<String, String> e : caps.entrySet()) {
            sb
                .append(
                    padLine(
                        "   \u2022 " + padRight(e.getKey(), 28) + " : " + nullSafe(e.getValue()),
                        width
                    )
                )
                .append('\n');
        }
        sb.append(bottom);
        System.out.println(sb.toString());
    }

    private static String repeat(String s, int n) {
        StringBuilder b = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) {
            b.append(s);
        }
        return b.toString();
    }

    private static String padRight(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= width) {
            return s;
        }
        StringBuilder b = new StringBuilder(s);
        for (int i = s.length(); i < width; i++) {
            b.append(' ');
        }
        return b.toString();
    }

    private static String padLine(String content, int width) {
        if (content == null) {
            content = "";
        }
        String truncated = content.length() > width ? content.substring(0, width) : content;
        return "│" + padRight(truncated, width) + "│";
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Replaces inline userinfo credentials in a URL with placeholders so they
     * never appear in console / log output. For example
     * {@code https://user:key@mobile-hub.lambdatest.com/wd/hub} becomes
     * {@code https://<username>:<accessKey>@mobile-hub.lambdatest.com/wd/hub}.
     */
    private static String maskUrlCredentials(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.replaceFirst("://[^/@]+@", "://<username>:<accessKey>@");
    }

    /**
     * Returns true when a capability value should NOT be sent to the driver.
     * Skips null/empty strings and the textual forms of empty collections ("[]", "{}").
     */
    private static boolean isBlankCapabilityValue(String value) {
        if (value == null) {
            return true;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return true;
        }
        return v.equals("[]") || v.equals("{}");
    }

    /**
     * Capability keys that must always be serialised as {@link Long} when the
     * user supplies a numeric value. LambdaTest's schema rejects e.g. an
     * {@code Integer} or {@code Boolean} here, so we force the type even when
     * the value would otherwise fit in an {@code int}.
     */
    private static final java.util.Set<String> LONG_ONLY_CAPABILITIES = new java.util.HashSet<>(
        java.util.Arrays.asList("waitforidletimeout")
    );

    /**
     * Convert a raw capability string into the most appropriate JSON type.
     * Remote drivers (Appium, LambdaTest) validate types strictly, so
     * {@code "TRUE"} becomes {@link Boolean#TRUE}, {@code "60"} becomes
     * {@link Integer}, JSON-shaped strings are parsed via Jackson, and
     * everything else remains a String.
     *
     * <p>If {@code capability} is in {@link #LONG_ONLY_CAPABILITIES}, numeric
     * values are returned as {@link Long} and boolean-looking strings are
     * passed through untouched.
     */
    private static Object coerceCapabilityValue(String capability, String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return v;
        }
        boolean longOnly =
            capability != null && LONG_ONLY_CAPABILITIES.contains(capability.toLowerCase());
        if (longOnly) {
            if (v.matches("-?\\d+")) {
                try {
                    return Long.parseLong(v);
                } catch (NumberFormatException ignore) {
                    // fall through to default string
                }
            }
            return v;
        }
        return coerceCapabilityValue(v);
    }

    /**
     * Convert a raw capability string into the most appropriate JSON type.
     * Remote drivers (Appium, LambdaTest) validate types strictly, so
     * {@code "TRUE"} becomes {@link Boolean#TRUE}, {@code "60"} becomes
     * {@link Integer}, JSON-shaped strings are parsed via Jackson, and
     * everything else remains a String.
     */
    private static Object coerceCapabilityValue(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.isEmpty()) {
            return v;
        }
        // Boolean
        if (v.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (v.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        // Integer / Long
        if (v.matches("-?\\d+")) {
            try {
                long l = Long.parseLong(v);
                if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                    return (int) l;
                }
                return l;
            } catch (NumberFormatException ignore) {
                // fall through
            }
        }
        // Double
        if (v.matches("-?\\d+\\.\\d+")) {
            try {
                return Double.parseDouble(v);
            } catch (NumberFormatException ignore) {
                // fall through
            }
        }
        // JSON array or object
        if ((v.startsWith("[") && v.endsWith("]")) || (v.startsWith("{") && v.endsWith("}"))) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().readValue(v, Object.class);
            } catch (Exception ignore) {
                // fall back to raw string if it isn't valid JSON
            }
        }
        return v;
    }

    private static String resolveCapabilitySource(String name, ProjectSettings settings) {
        if (settings.getDevices() != null && settings.getDevices().getDevice(name) != null) {
            return "Manage Devices (Devices.json)";
        }
        // SAP and any legacy unmigrated entries still live in Emulators.json.
        if (settings.getEmulators() != null && settings.getEmulators().getEmulator(name) != null) {
            return "Manage Browsers (Emulators.json - legacy)";
        }
        return "Unknown";
    }

    private static void logConnectionDetails(
        String name,
        String source,
        String url,
        java.util.LinkedHashMap<String, String> caps
    ) {
        final int width = 78;
        String top = "┌" + repeat("─", width) + "┐";
        String mid = "├" + repeat("─", width) + "┤";
        String bottom = "└" + repeat("─", width) + "┘";

        StringBuilder sb = new StringBuilder();
        sb.append('\n').append(top).append('\n');
        sb.append(padLine("  \uD83D\uDE80  Establishing driver connection", width)).append('\n');
        sb.append(mid).append('\n');
        sb.append(padLine("  Device / Browser : " + nullSafe(name), width)).append('\n');
        sb.append(padLine("  Source           : " + nullSafe(source), width)).append('\n');
        sb
            .append(padLine("  Remote URL       : " + nullSafe(maskUrlCredentials(url)), width))
            .append('\n');
        sb.append(mid).append('\n');
        sb.append(padLine("  Capabilities (" + caps.size() + "):", width)).append('\n');
        for (java.util.Map.Entry<String, String> e : caps.entrySet()) {
            sb
                .append(
                    padLine(
                        "   \u2022 " + padRight(e.getKey(), 28) + " : " + nullSafe(e.getValue()),
                        width
                    )
                )
                .append('\n');
        }
        sb.append(bottom);
        System.out.println(sb.toString());
    }

    private static String repeat(String s, int n) {
        StringBuilder b = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) {
            b.append(s);
        }
        return b.toString();
    }

    private static String padRight(String s, int width) {
        if (s == null) {
            s = "";
        }
        if (s.length() >= width) {
            return s;
        }
        StringBuilder b = new StringBuilder(s);
        for (int i = s.length(); i < width; i++) {
            b.append(' ');
        }
        return b.toString();
    }

    private static String padLine(String content, int width) {
        if (content == null) {
            content = "";
        }
        String truncated = content.length() > width ? content.substring(0, width) : content;
        return "│" + padRight(truncated, width) + "│";
    }

    private static String nullSafe(String s) {
        return s == null ? "" : s;
    }

    /**
     * Replaces inline userinfo credentials in a URL with placeholders so they
     * never appear in console / log output. For example
     * {@code https://user:key@mobile-hub.lambdatest.com/wd/hub} becomes
     * {@code https://<username>:<accessKey>@mobile-hub.lambdatest.com/wd/hub}.
     */
    private static String maskUrlCredentials(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        return url.replaceFirst("://[^/@]+@", "://<username>:<accessKey>@");
    }

    public static void checkPlatformName(Properties prop, Object key, String capability) {
        String platformName = prop.getProperty(key.toString()).trim().toString().toLowerCase();
        switch (platformName) {
            case "android":
                driverInformation.put("isAndroidNative", true);
                break;
            case "ios":
                driverInformation.put("isIOSNative", true);
                break;
        }
    }

    private static WebDriver create(
        String browserName,
        DesiredCapabilities caps,
        ProjectSettings settings
    )
        throws MalformedURLException {
        return checkEmulators(browserName, caps, settings);
    }

    private static WebDriver checkEmulators(
        String browserName,
        DesiredCapabilities caps,
        ProjectSettings settings
    )
        throws MalformedURLException {
        // "Manage Devices" is now the single source of truth for Appium endpoints.
        // resolveRemoteUrl() still falls back to any unmigrated legacy emulator
        // entries in Emulators.json, so this remains backwards-compatible.
        String url = settings.resolveRemoteUrl(browserName);
        if (url != null && !url.isEmpty()) {
            return createRemoteDriver(url, caps);
        }
        return null;
    }

    private static WebDriver createRemoteDriver(String url, DesiredCapabilities caps)
        throws MalformedURLException {
        System.out.println("\u27A4 Connecting to remote driver at: " + maskUrlCredentials(url));
        if (isAppiumNative(url, caps.asMap())) {
            if (driverInformation.get("isAndroidNative")) {
                System.out.println("\u27A4 Driver type: AndroidDriver (Appium native)\n");
                return new io.appium.java_client.android.AndroidDriver(new URL(url), caps);
            } else if (driverInformation.get("isIOSNative")) {
                System.out.println("\u27A4 Driver type: IOSDriver (Appium native)\n");
                return new io.appium.java_client.ios.IOSDriver(new URL(url), caps);
            }
        }

        return null;
    }

    private static boolean isAppiumNative(String remoteUrl, Map props) {
        if (driverInformation.get("isLambdaExecution")) {
            return true;
        } else {
            return (
                props != null &&
                props.containsKey("platformName") &&
                toLString(props.get("platformName")).matches("android|ios") &&
                (!props.containsKey("browserName") || isNullOrEmpty(props.get("browserName")))
            );
        }
    }

    private static boolean isNullOrEmpty(Object o) {
        return Objects.isNull(o) || o.toString().isEmpty();
    }

    private static String toLString(Object o) {
        return Objects.toString(o, "").toLowerCase();
    }
}
