package com.ing.engine.aicli.execution;

/** One file mutation observed during plan execution (relative to project root). */
public final class FileChange {
    public final String relPath;
    /** Content before the plan ran; null when the file did not exist. */
    public final byte[] before;
    /** Content after the plan ran; null when the file was deleted. */
    public final byte[] after;

    public FileChange(String relPath, byte[] before, byte[] after) {
        this.relPath = relPath;
        this.before = before;
        this.after = after;
    }

    public String kind() {
        if (before == null) return "created";
        if (after == null) return "deleted";
        return "modified";
    }
}
