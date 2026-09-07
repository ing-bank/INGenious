package com.ing.engine.perf;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Layout helper for the performance-studio folders inside a project:
 *
 * <pre>
 * &lt;Project&gt;/
 * ├── Performance/
 * │   ├── scripts/       generated + hand-edited k6 .js
 * │   ├── profiles/      custom load profiles (YAML)
 * │   ├── rules/         correlation / parameterization rules
 * │   ├── recordings/    HAR recordings
 * │   └── data/          CSV/JSON data files for SharedArray
 * └── Results/Performance/&lt;name&gt;/&lt;timestamp&gt;/
 * </pre>
 *
 * <p>Nothing is created eagerly; call {@link #ensure()} before writing.
 */
public final class PerfWorkspace {
    private final File projectDir;

    public PerfWorkspace(File projectDir) {
        this.projectDir = projectDir;
    }

    public File projectDir() {
        return projectDir;
    }

    public File perfDir() {
        return new File(projectDir, "Performance");
    }

    public File scriptsDir() {
        return new File(perfDir(), "scripts");
    }

    public File profilesDir() {
        return new File(perfDir(), "profiles");
    }

    public File rulesDir() {
        return new File(perfDir(), "rules");
    }

    public File recordingsDir() {
        return new File(perfDir(), "recordings");
    }

    public File dataDir() {
        return new File(perfDir(), "data");
    }

    public File resultsDir() {
        return new File(projectDir, "Results/Performance");
    }

    /**
     * Create the whole Performance/ subtree (idempotent) and materialize the
     * built-in load profiles as editable YAML files (missing ones only —
     * user edits are never overwritten).
     */
    public void ensure() {
        scriptsDir().mkdirs();
        profilesDir().mkdirs();
        rulesDir().mkdirs();
        recordingsDir().mkdirs();
        dataDir().mkdirs();
        PerfProfile.materializeBuiltIns(this);
    }

    /**
     * Resolve a k6 script: an explicit .js path, or a name under
     * {@code <project>/Performance/scripts/} (with or without extension).
     * Returns null when neither exists.
     */
    public static File resolveScript(String target, File projectDir) {
        File direct = new File(target);
        if (direct.isFile() && target.endsWith(".js")) {
            return direct;
        }
        if (projectDir != null) {
            PerfWorkspace ws = new PerfWorkspace(projectDir);
            File named = new File(
                ws.scriptsDir(),
                target.endsWith(".js") ? target : target + ".js"
            );
            if (named.isFile()) {
                return named;
            }
        }
        return null;
    }

    /**
     * Derive the project dir from a script located inside
     * {@code <proj>/Performance/scripts}; null when it lives elsewhere.
     */
    public static File projectDirOfScript(File script) {
        File scripts = script.getParentFile();
        if (scripts == null || !"scripts".equals(scripts.getName())) {
            return null;
        }
        File perf = scripts.getParentFile();
        if (perf == null || !"Performance".equals(perf.getName())) {
            return null;
        }
        return perf.getParentFile();
    }

    public List<File> listScripts() {
        return listFiles(scriptsDir(), ".js");
    }

    public List<File> listProfiles() {
        List<File> out = listFiles(profilesDir(), ".yaml");
        out.addAll(listFiles(profilesDir(), ".yml"));
        out.sort(Comparator.comparing(File::getName));
        return out;
    }

    public List<File> listRecordings() {
        return listFiles(recordingsDir(), ".har");
    }

    /** Run result folders, newest first: Results/Performance/&lt;name&gt;/&lt;timestamp&gt;. */
    public List<File> listRuns() {
        List<File> out = new ArrayList<>();
        File root = resultsDir();
        File[] names = root.listFiles(File::isDirectory);
        if (names == null) {
            return out;
        }
        for (File name : names) {
            File[] stamps = name.listFiles(File::isDirectory);
            if (stamps == null) {
                continue;
            }
            out.addAll(Arrays.asList(stamps));
        }
        out.sort(Comparator.comparing(File::getName).reversed());
        return out;
    }

    private static List<File> listFiles(File dir, String extension) {
        List<File> out = new ArrayList<>();
        File[] files = dir.listFiles(
            f -> f.isFile() && f.getName().toLowerCase().endsWith(extension)
        );
        if (files != null) {
            out.addAll(Arrays.asList(files));
            out.sort(Comparator.comparing(File::getName));
        }
        return out;
    }
}
