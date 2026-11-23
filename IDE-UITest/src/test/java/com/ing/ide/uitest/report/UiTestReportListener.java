package com.ing.ide.uitest.report;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Records every test case result in the run folder and (re)writes the selected reports, so the
 * reports are complete after the last test JVM of a run finishes.
 */
public final class UiTestReportListener implements ITestListener {
    private List<ReportWriter> writers;

    @Override
    public void onStart(final ITestContext context) {
        writers = ReportFormats.fromSystemProperty();
    }

    @Override
    public void onTestSuccess(final ITestResult result) {
        record(result, TestStatus.PASSED);
    }

    @Override
    public void onTestFailure(final ITestResult result) {
        if (result.getInstance() instanceof EvidenceSource) {
            try {
                ((EvidenceSource) result.getInstance()).captureFailure(
                        "FAILED-" + result.getMethod().getMethodName()
                    );
            } catch (RuntimeException e) {
                System.err.println("[UI test report] Failure screenshot not captured: " + e);
            }
        }
        record(result, TestStatus.FAILED);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(final ITestResult result) {
        record(result, TestStatus.FAILED);
    }

    @Override
    public void onTestSkipped(final ITestResult result) {
        record(result, TestStatus.SKIPPED);
    }

    private void record(final ITestResult result, final TestStatus status) {
        final Path runDir = ReportOutput.runDir();
        final TestIdentity identity = TestIdentity.of(
            result.getTestClass().getRealClass(),
            result.getMethod().getConstructorOrMethod().getMethod()
        );
        final TestCaseResult testCase = new TestCaseResult(
            identity,
            status,
            Instant.ofEpochMilli(result.getStartMillis()).toString(),
            Math.max(0, result.getEndMillis() - result.getStartMillis()),
            message(result.getThrowable()),
            evidence(result.getInstance(), runDir, status)
        );
        try {
            final ResultStore store = new ResultStore(runDir);
            store.save(testCase);
            final List<TestCaseResult> all = store.loadAll();
            for (final ReportWriter writer : writers) {
                writer.write(runDir, runInfo(runDir), all);
            }
        } catch (IOException e) {
            System.err.println("[UI test report] Report not written to " + runDir + ": " + e);
        }
    }

    // A skipped test case did not run, so it has no evidence (its recording is still stopped)
    private static List<Evidence> evidence(
        final Object instance,
        final Path runDir,
        final TestStatus status
    ) {
        if (!(instance instanceof EvidenceSource)) {
            return List.of();
        }
        final List<Evidence> captured = ((EvidenceSource) instance).completeEvidence();
        if (status == TestStatus.SKIPPED) {
            return List.of();
        }
        return captured
            .stream()
            .map(
                e ->
                    e.withPath(
                        runDir
                            .relativize(Path.of(e.path()).toAbsolutePath())
                            .toString()
                            .replace('\\', '/')
                    )
            )
            .collect(Collectors.toList());
    }

    private static String message(final Throwable error) {
        if (error == null) {
            return null;
        }
        final StringWriter trace = new StringWriter();
        error.printStackTrace(new PrintWriter(trace));
        return trace.toString();
    }

    private static RunInfo runInfo(final Path runDir) {
        return new RunInfo(
            runDir.getFileName().toString(),
            System.getProperty("ingenious.expectedVersion", "unknown"),
            System.getProperty("uitest.input", "direct"),
            System.getProperty("os.name") +
            " " +
            System.getProperty("os.version") +
            " (" +
            System.getProperty("os.arch") +
            ")",
            System.getProperty("java.vendor") + " " + System.getProperty("java.version"),
            OffsetDateTime.now(ZoneId.systemDefault()).withNano(0).toString()
        );
    }
}
