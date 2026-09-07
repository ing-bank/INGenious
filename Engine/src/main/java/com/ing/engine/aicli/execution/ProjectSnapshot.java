package com.ing.engine.aicli.execution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Cheap whole-project content snapshot used to derive mutation manifests
 * (created/modified/deleted files) without requiring every tool to report its
 * writes. INGenious projects are small (YAML/CSV/properties), so an in-memory
 * snapshot is safe; large/binary artifacts are skipped.
 */
public final class ProjectSnapshot {
    private static final long MAX_FILE_BYTES = 1024 * 1024; // 1 MB
    private static final Set<String> SKIP_DIRS = Set.of(
        ".ingenious",
        ".git",
        "target",
        "node_modules",
        "Results"
    );

    private ProjectSnapshot() {}

    public static Map<String, byte[]> take(Path root) throws IOException {
        Map<String, byte[]> out = new HashMap<>();
        if (root == null || !Files.isDirectory(root)) return out;
        try (Stream<Path> walk = Files.walk(root)) {
            walk
                .filter(Files::isRegularFile)
                .filter(p -> !skipped(root, p))
                .forEach(
                    p -> {
                        try {
                            if (Files.size(p) <= MAX_FILE_BYTES) {
                                out.put(root.relativize(p).toString(), Files.readAllBytes(p));
                            }
                        } catch (IOException ignored) {
                            // unreadable file: leave out of the snapshot
                        }
                    }
                );
        }
        return out;
    }

    public static List<FileChange> diff(Map<String, byte[]> before, Path root) throws IOException {
        Map<String, byte[]> after = take(root);
        List<FileChange> changes = new ArrayList<>();
        for (Map.Entry<String, byte[]> e : after.entrySet()) {
            byte[] old = before.get(e.getKey());
            if (old == null) {
                changes.add(new FileChange(e.getKey(), null, e.getValue()));
            } else if (!java.util.Arrays.equals(old, e.getValue())) {
                changes.add(new FileChange(e.getKey(), old, e.getValue()));
            }
        }
        for (Map.Entry<String, byte[]> e : before.entrySet()) {
            if (!after.containsKey(e.getKey())) {
                changes.add(new FileChange(e.getKey(), e.getValue(), null));
            }
        }
        changes.sort(java.util.Comparator.comparing(c -> c.relPath));
        return changes;
    }

    private static boolean skipped(Path root, Path p) {
        Path rel = root.relativize(p);
        for (Path part : rel) {
            if (SKIP_DIRS.contains(part.toString())) return true;
        }
        return false;
    }
}
