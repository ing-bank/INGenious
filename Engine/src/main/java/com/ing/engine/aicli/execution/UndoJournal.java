package com.ing.engine.aicli.execution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Persistent undo/redo journal for plan executions. Each executed plan that
 * mutated files gets one journal entry containing the before and after
 * contents of every affected file, stored under
 * {@code <projectRoot>/.ingenious/undo/}. Bounded to the last 10 entries.
 */
public final class UndoJournal {
    private static final int MAX_ENTRIES = 10;

    private final ObjectMapper mapper = new ObjectMapper();
    private final Path projectRoot;
    private final Path dir;
    private final Path stackFile;

    public UndoJournal(Path projectRoot) {
        this.projectRoot = projectRoot;
        this.dir = projectRoot.resolve(".ingenious").resolve("undo");
        this.stackFile = dir.resolve("stack.json");
    }

    /** Record an executed plan's file changes as a new undoable entry. */
    public synchronized void record(String planId, String goal, List<FileChange> changes)
        throws IOException {
        if (changes.isEmpty()) return;
        Files.createDirectories(dir);
        String entryId = System.currentTimeMillis() + "-" + planId;
        Path entryDir = dir.resolve(entryId);
        Files.createDirectories(entryDir.resolve("before"));
        Files.createDirectories(entryDir.resolve("after"));

        ObjectNode manifest = mapper.createObjectNode();
        manifest.put("planId", planId);
        manifest.put("goal", goal);
        ArrayNode arr = manifest.putArray("changes");
        int i = 0;
        for (FileChange c : changes) {
            ObjectNode cn = arr.addObject();
            cn.put("path", c.relPath);
            cn.put("index", i);
            if (c.before != null) {
                Files.write(entryDir.resolve("before").resolve(String.valueOf(i)), c.before);
                cn.put("existedBefore", true);
            } else {
                cn.put("existedBefore", false);
            }
            if (c.after != null) {
                Files.write(entryDir.resolve("after").resolve(String.valueOf(i)), c.after);
                cn.put("existsAfter", true);
            } else {
                cn.put("existsAfter", false);
            }
            i++;
        }
        Files.writeString(entryDir.resolve("manifest.json"), manifest.toPrettyString());

        ObjectNode stack = loadStack();
        ((ArrayNode) stack.withArray("undo")).add(entryId);
        stack.putArray("redo"); // new action clears the redo stack
        trim((ArrayNode) stack.withArray("undo"));
        saveStack(stack);
    }

    /** Revert the most recent entry. Returns a description of what was undone. */
    public synchronized String undo() throws IOException {
        ObjectNode stack = loadStack();
        ArrayNode undo = (ArrayNode) stack.withArray("undo");
        if (undo.isEmpty()) throw new IllegalStateException("Nothing to undo.");
        String entryId = undo.remove(undo.size() - 1).asText();
        String goal = restore(entryId, true);
        ((ArrayNode) stack.withArray("redo")).add(entryId);
        saveStack(stack);
        return goal;
    }

    /** Re-apply the most recently undone entry. */
    public synchronized String redo() throws IOException {
        ObjectNode stack = loadStack();
        ArrayNode redo = (ArrayNode) stack.withArray("redo");
        if (redo.isEmpty()) throw new IllegalStateException("Nothing to redo.");
        String entryId = redo.remove(redo.size() - 1).asText();
        String goal = restore(entryId, false);
        ((ArrayNode) stack.withArray("undo")).add(entryId);
        saveStack(stack);
        return goal;
    }

    public synchronized int undoCount() {
        return loadStack().withArray("undo").size();
    }

    public synchronized int redoCount() {
        return loadStack().withArray("redo").size();
    }

    // ------------------------------------------------------------------

    /** Restore the "before" (undo) or "after" (redo) side of an entry. */
    private String restore(String entryId, boolean toBefore) throws IOException {
        Path entryDir = dir.resolve(entryId);
        JsonNode manifest = mapper.readTree(entryDir.resolve("manifest.json").toFile());
        for (JsonNode c : manifest.path("changes")) {
            String rel = c.path("path").asText();
            int index = c.path("index").asInt();
            boolean exists = toBefore
                ? c.path("existedBefore").asBoolean()
                : c.path("existsAfter").asBoolean();
            Path target = projectRoot.resolve(rel);
            if (exists) {
                Path src = entryDir
                    .resolve(toBefore ? "before" : "after")
                    .resolve(String.valueOf(index));
                Files.createDirectories(target.getParent());
                Files.write(target, Files.readAllBytes(src));
            } else {
                Files.deleteIfExists(target);
            }
        }
        return manifest.path("goal").asText(entryId);
    }

    private ObjectNode loadStack() {
        try {
            if (Files.exists(stackFile)) {
                return (ObjectNode) mapper.readTree(stackFile.toFile());
            }
        } catch (IOException | ClassCastException ignored) {
            // fall through to a fresh stack
        }
        ObjectNode fresh = mapper.createObjectNode();
        fresh.putArray("undo");
        fresh.putArray("redo");
        return fresh;
    }

    private void saveStack(ObjectNode stack) throws IOException {
        Files.createDirectories(dir);
        Files.writeString(stackFile, stack.toPrettyString());
    }

    /** Drop journal directories older than the retained window. */
    private void trim(ArrayNode undo) throws IOException {
        List<String> keep = new ArrayList<>();
        undo.forEach(n -> keep.add(n.asText()));
        while (undo.size() > MAX_ENTRIES) {
            undo.remove(0);
        }
        if (keep.size() <= MAX_ENTRIES || !Files.isDirectory(dir)) return;
        List<String> retained = new ArrayList<>();
        undo.forEach(n -> retained.add(n.asText()));
        try (java.util.stream.Stream<Path> stream = Files.list(dir)) {
            stream
                .filter(Files::isDirectory)
                .filter(p -> keep.contains(p.getFileName().toString()))
                .filter(p -> !retained.contains(p.getFileName().toString()))
                .forEach(UndoJournal::deleteRecursively);
        }
    }

    private static void deleteRecursively(Path p) {
        try (java.util.stream.Stream<Path> stream = Files.walk(p)) {
            stream
                .sorted(Comparator.reverseOrder())
                .forEach(
                    f -> {
                        try {
                            Files.deleteIfExists(f);
                        } catch (IOException ignored) {
                            // best effort
                        }
                    }
                );
        } catch (IOException ignored) {
            // best effort
        }
    }
}
