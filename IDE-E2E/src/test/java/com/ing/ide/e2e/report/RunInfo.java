package com.ing.ide.e2e.report;

/**
 * Facts about a test run shown in report headers.
 */
public final class RunInfo {
    private final String runId;
    private final String ideVersion;
    private final String inputMode;
    private final String os;
    private final String java;
    private final String generatedAt;

    /**
     * @param runId name of the run folder
     * @param ideVersion INGenious version under test
     * @param inputMode how input was delivered ({@code direct} or {@code robot})
     * @param os operating system
     * @param java Java runtime that ran the IDE
     * @param generatedAt ISO-8601 time the report was written
     */
    public RunInfo(
        final String runId,
        final String ideVersion,
        final String inputMode,
        final String os,
        final String java,
        final String generatedAt
    ) {
        this.runId = runId;
        this.ideVersion = ideVersion;
        this.inputMode = inputMode;
        this.os = os;
        this.java = java;
        this.generatedAt = generatedAt;
    }

    /**
     * @return name of the run folder
     */
    public String runId() {
        return runId;
    }

    /**
     * @return INGenious version under test
     */
    public String ideVersion() {
        return ideVersion;
    }

    /**
     * @return how input was delivered
     */
    public String inputMode() {
        return inputMode;
    }

    /**
     * @return operating system
     */
    public String os() {
        return os;
    }

    /**
     * @return Java runtime that ran the IDE
     */
    public String java() {
        return java;
    }

    /**
     * @return ISO-8601 time the report was written
     */
    public String generatedAt() {
        return generatedAt;
    }
}
