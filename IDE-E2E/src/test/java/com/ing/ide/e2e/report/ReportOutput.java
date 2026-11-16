package com.ing.ide.e2e.report;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Resolves the folder of the current test run: {@code <e2e.outputDir>/<e2e.runId>}.
 * <p>
 * Maven passes the same {@code e2e.runId} to every forked test JVM, so all test classes of one
 * {@code mvn verify} write into one run folder.
 */
public final class ReportOutput {
    private static final DateTimeFormatter RUN_ID_FORMAT = DateTimeFormatter.ofPattern(
        "yyyyMMdd-HHmmss"
    );

    private static Path runDir;

    private ReportOutput() {}

    /**
     * @return the run folder, created if needed
     */
    public static synchronized Path runDir() {
        if (runDir == null) {
            final Path base = Paths.get(System.getProperty("e2e.outputDir", "ui-test-output"));
            String runId = System.getProperty("e2e.runId", "").trim();
            if (runId.isEmpty()) {
                runId = LocalDateTime.now().format(RUN_ID_FORMAT);
            }
            runDir = base.resolve(runId).toAbsolutePath().normalize();
            try {
                Files.createDirectories(runDir);
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot create run folder " + runDir, e);
            }
        }
        return runDir;
    }
}
