package com.ing.engine.plugin.loader;

import com.ing.engine.constants.FilePath;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

final class PluginSearchPath {
    static final String PLUGIN_PATH_ENV = "INGENIOUS_PLUGIN_PATH";
    static final String DISABLE_USER_PLUGINS_ENV = "INGENIOUS_DISABLE_USER_PLUGINS";

    private PluginSearchPath() {}

    static List<Location> resolve() {
        return resolve(
            System::getenv,
            new File(FilePath.getAppRoot()),
            System.getProperty("os.name", ""),
            System.getProperty("user.home", "")
        );
    }

    static List<Location> resolve(
        Function<String, String> environment,
        File appRoot,
        String osName,
        String userHome
    ) {
        List<Location> locations = new ArrayList<>();
        String configuredPath = environment.apply(PLUGIN_PATH_ENV);
        if (configuredPath != null) {
            for (String entry : configuredPath.split(Pattern.quote(File.pathSeparator), -1)) {
                if (!entry.isBlank()) {
                    locations.add(
                        new Location(
                            new File(expandHome(entry.trim(), userHome)).getAbsoluteFile(),
                            "env"
                        )
                    );
                }
            }
            return locations;
        }

        if (!userPluginsDisabled(environment.apply(DISABLE_USER_PLUGINS_ENV))) {
            locations.add(
                new Location(resolveUserDirectory(environment, osName, userHome), "user")
            );
        }
        locations.add(
            new Location(new File(safeAppRoot(appRoot), "plugins").getAbsoluteFile(), "install")
        );
        return locations;
    }

    private static File resolveUserDirectory(
        Function<String, String> environment,
        String osName,
        String userHome
    ) {
        String normalizedOsName = osName.toLowerCase(Locale.ROOT);
        if (normalizedOsName.startsWith("windows")) {
            String localAppData = nonBlank(environment.apply("LOCALAPPDATA"));
            if (localAppData == null) {
                String userProfile = firstNonBlank(environment.apply("USERPROFILE"), userHome, ".");
                localAppData =
                    new File(userProfile, "AppData" + File.separator + "Local").getPath();
            }
            return new File(localAppData, "INGenious" + File.separator + "plugins")
            .getAbsoluteFile();
        }

        String home = firstNonBlank(environment.apply("HOME"), userHome, ".");
        if (normalizedOsName.contains("mac")) {
            return new File(
                home,
                "Library" +
                File.separator +
                "Application Support" +
                File.separator +
                "INGenious" +
                File.separator +
                "plugins"
            )
            .getAbsoluteFile();
        }

        String dataHome = nonBlank(environment.apply("XDG_DATA_HOME"));
        if (dataHome == null) {
            dataHome = new File(home, ".local" + File.separator + "share").getPath();
        }
        return new File(dataHome, "ingenious" + File.separator + "plugins").getAbsoluteFile();
    }

    private static boolean userPluginsDisabled(String value) {
        return value != null && ("true".equalsIgnoreCase(value.trim()) || "1".equals(value.trim()));
    }

    private static String expandHome(String entry, String userHome) {
        String home = nonBlank(userHome);
        if (home == null) {
            return entry;
        }
        if (
            entry.equals("~") ||
            entry.startsWith("~" + File.separator) ||
            entry.startsWith("~/") ||
            entry.startsWith("~\\")
        ) {
            return home + entry.substring(1);
        }
        if (entry.startsWith("${user.home}")) {
            return home + entry.substring("${user.home}".length());
        }
        return entry;
    }

    private static File safeAppRoot(File appRoot) {
        return appRoot == null ? new File(".").getAbsoluteFile() : appRoot;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            String candidate = nonBlank(value);
            if (candidate != null) {
                return candidate;
            }
        }
        return ".";
    }

    private static String nonBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    record Location(File directory, String source) {}
}
