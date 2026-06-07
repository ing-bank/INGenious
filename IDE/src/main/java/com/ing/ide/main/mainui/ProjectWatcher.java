package com.ing.ide.main.mainui;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;

/**
 * Polling-based filesystem watcher for an INGenious project.
 * <p>
 * Detects external changes (e.g. files added/removed/edited by the CLI,
 * the MCP server, or another tool) and triggers a project reload on
 * the IDE.
 * <p>
 * A polling implementation is used in preference to
 * {@link java.nio.file.WatchService} because the JDK's WatchService
 * falls back to a polling implementation with a multi-second latency
 * on macOS, so the behaviour would not be consistent across platforms.
 * The polling here also makes it straightforward to coalesce write
 * bursts (e.g. a CLI command writing several files in sequence) into
 * a single reload.
 */
public class ProjectWatcher {

    private static final Logger LOG = Logger.getLogger(ProjectWatcher.class.getName());

    /** How often the watcher polls the project tree (ms). */
    private static final long POLL_INTERVAL_MS = 1500L;
    /** Required period of stability before reload fires (ms). */
    private static final long STABILITY_MS = 800L;
    /** Maximum directory depth scanned. */
    private static final int MAX_DEPTH = 16;
    /** Grace period after IDE-initiated writes before resuming detection (ms). */
    private static final long IDE_WRITE_GRACE_MS = 2500L;

    /** File-name suffixes ignored when computing the fingerprint. */
    private static final String[] IGNORED_SUFFIXES = {
            "~", ".tmp", ".swp", ".bak", ".lock", ".part", ".crswap"
    };

    /** Directory names ignored when walking the project tree. */
    private static final Set<String> IGNORED_DIR_NAMES = new HashSet<>(Arrays.asList(
            ".git", ".svn", ".hg", ".idea", ".vscode", "node_modules",
            "target", "build", "dist", "out", "__pycache__"
    ));

    private final Path projectRoot;
    private final Runnable onChange;
    private final Thread thread;
    private final AtomicBoolean stopRequested = new AtomicBoolean(false);
    /** Set while the IDE is itself writing to the project tree. */
    private final AtomicBoolean ideWriting = new AtomicBoolean(false);
    /** Quiet-time window after IDE writes (ms since epoch). */
    private volatile long quietUntilMs = 0L;

    private long lastFingerprint = 0L;
    /** Time the current pending change was first observed (0 = no pending). */
    private long pendingSince = 0L;

    public ProjectWatcher(Path projectRoot, Runnable onChange) {
        this.projectRoot = projectRoot;
        this.onChange = onChange;
        this.thread = new Thread(this::run, "ingenious-project-watcher");
        this.thread.setDaemon(true);
    }

    /**
     * Takes a baseline snapshot and starts the polling thread.
     */
    public void start() {
        lastFingerprint = fingerprint();
        thread.start();
    }

    /**
     * Stops the polling thread. Safe to call multiple times.
     */
    public void stop() {
        stopRequested.set(true);
        thread.interrupt();
    }

    /**
     * Marks the start of an IDE-initiated write to the project tree.
     * No reload will fire while writes are in progress.
     */
    public void beginIdeWrite() {
        ideWriting.set(true);
    }

    /**
     * Marks the end of an IDE-initiated write. Re-baselines the
     * snapshot so the writes that just happened are not seen as
     * external changes.
     */
    public void endIdeWrite() {
        ideWriting.set(false);
        quietUntilMs = System.currentTimeMillis() + IDE_WRITE_GRACE_MS;
        pendingSince = 0L;
        lastFingerprint = fingerprint();
    }

    private void run() {
        while (!stopRequested.get()) {
            try {
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException ie) {
                if (stopRequested.get()) {
                    return;
                }
                continue;
            }
            try {
                tick();
            } catch (RuntimeException ex) {
                LOG.log(Level.FINE, "Project watcher tick failed", ex);
            }
        }
    }

    private void tick() {
        if (ideWriting.get() || System.currentTimeMillis() < quietUntilMs) {
            // IDE is writing — re-baseline to its current state so we don't
            // mis-attribute the IDE's own writes to an external change.
            lastFingerprint = fingerprint();
            pendingSince = 0L;
            return;
        }
        long current = fingerprint();
        long now = System.currentTimeMillis();
        if (pendingSince == 0L) {
            // Baseline mode — looking for the first change.
            if (current != lastFingerprint) {
                lastFingerprint = current;
                pendingSince = now;
                LOG.log(Level.FINE,
                        "Project change detected — waiting for stability before reload");
            }
            return;
        }
        // Pending mode — waiting for the tree to settle.
        if (current != lastFingerprint) {
            // Still changing — restart the stability window.
            lastFingerprint = current;
            pendingSince = now;
            return;
        }
        // No change since last tick — fire once stability window elapses.
        if (now - pendingSince >= STABILITY_MS) {
            pendingSince = 0L;
            LOG.log(Level.INFO,
                    "External project change settled — triggering reload");
            try {
                SwingUtilities.invokeLater(onChange);
            } catch (RuntimeException re) {
                LOG.log(Level.FINE, "Watcher onChange dispatch failed", re);
            }
        }
    }

    /**
     * Computes a fingerprint of the project tree based on file paths,
     * last-modified times and sizes. Two snapshots taken when the tree
     * is unchanged will produce equal fingerprints.
     */
    private long fingerprint() {
        if (!Files.isDirectory(projectRoot)) {
            return 0L;
        }
        // [accumulated hash, file count]
        final long[] state = new long[]{1469598103934665603L, 0L}; // FNV-1a basis
        try {
            Files.walkFileTree(projectRoot, EnumSet.noneOf(FileVisitOption.class),
                    MAX_DEPTH, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(projectRoot)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String name = dir.getFileName().toString();
                    if (name.startsWith(".") || IGNORED_DIR_NAMES.contains(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String name = file.getFileName().toString();
                    if (name.startsWith(".")) {
                        return FileVisitResult.CONTINUE;
                    }
                    for (String suffix : IGNORED_SUFFIXES) {
                        if (name.endsWith(suffix)) {
                            return FileVisitResult.CONTINUE;
                        }
                    }
                    String rel = projectRoot.relativize(file).toString();
                    long h = rel.hashCode();
                    h = h * 1099511628211L ^ attrs.lastModifiedTime().toMillis();
                    h = h * 1099511628211L ^ attrs.size();
                    state[0] ^= h;
                    state[1]++;
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            LOG.log(Level.FINE, "Snapshot walk failed for " + projectRoot, ex);
        }
        return state[0] ^ (state[1] * 1099511628211L);
    }
}
