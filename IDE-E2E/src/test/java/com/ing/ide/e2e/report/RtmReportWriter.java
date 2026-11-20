package com.ing.ide.e2e.report;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Publishes results and evidence to the RTM repository
 * ({@code p33148-ingenious-requirement-traceability-matrix}) through the RTM's own tooling, so the
 * results appear in its evidence report for a testing cycle:
 * <ol>
 *   <li>evidence files are copied with {@code .github/skills/evidence-recorder/scripts/save_evidence.py}
 *   into {@code cycles/<cycle>/<role>/<code>-<n>.<ext>};</li>
 *   <li>results are recorded with {@code scripts/evidence/record_test_result.py --batch-file}
 *   ({@code results.yaml} is never written directly, as the RTM requires);</li>
 *   <li>optionally the RTM evidence report is regenerated with
 *   {@code scripts/evidence/generate_evidence_report.py}.</li>
 * </ol>
 * Each evidence file is recorded with its caption and, when the test linked it with a
 * {@link StepRef}, the RTM step or assert it proves, so the RTM report shows "-> Step n" /
 * "-> Assert n" next to it.
 * <p>
 * Test case codes must be RTM codes (e.g. {@code UI-SC006-TC001}). Each test case is published
 * once per run; a marker in the run folder prevents duplicates when the report is rewritten.
 */
public final class RtmReportWriter implements ReportWriter {
    /** RTM repository root. */
    public static final String DIR_PROPERTY = "e2e.rtm.dir";
    /** Testing cycle name, i.e. the folder under {@code cycles/}. */
    public static final String CYCLE_PROPERTY = "e2e.rtm.cycle";
    /** {@code dev-test} (default) or {@code qa}: which result columns and evidence folder to fill. */
    public static final String ROLE_PROPERTY = "e2e.rtm.role";
    /** Tester name recorded with each result. */
    public static final String TESTER_PROPERTY = "e2e.rtm.tester";
    /** Python 3 executable with PyYAML. */
    public static final String PYTHON_PROPERTY = "e2e.rtm.python";
    /** Whether to regenerate the RTM evidence report after publishing. */
    public static final String GENERATE_PROPERTY = "e2e.rtm.generateReport";

    private static final String SAVE_EVIDENCE =
        ".github/skills/evidence-recorder/scripts/save_evidence.py";
    private static final String RECORD_RESULT = "scripts/evidence/record_test_result.py";
    private static final String GENERATE_REPORT = "scripts/evidence/generate_evidence_report.py";
    private static final String EXPORT_DIR = "rtm-export";
    private static final String SAVED_PREFIX = "saved -> ";
    private static final String LOG_FILE = "rtm-export.log";
    private static final long SCRIPT_TIMEOUT_SECONDS = 300;
    private static final int MAX_NOTES_LENGTH = 300;
    private static final ObjectMapper JSON = new ObjectMapper();

    private final Path rtmDir;
    private final String cycle;
    private final String role;
    private final String tester;
    private final String python;
    private final boolean generateReport;

    /**
     * Reads and validates the RTM settings, so a misconfigured run fails before any test starts.
     *
     * @throws IllegalArgumentException if a setting is missing or invalid
     */
    public RtmReportWriter() {
        this.rtmDir = Paths.get(required(DIR_PROPERTY)).toAbsolutePath().normalize();
        this.cycle = required(CYCLE_PROPERTY);
        this.role = System.getProperty(ROLE_PROPERTY, "dev-test").trim().toLowerCase(Locale.ROOT);
        this.tester = System.getProperty(TESTER_PROPERTY, "INGenious E2E (automated)").trim();
        this.python = System.getProperty(PYTHON_PROPERTY, "python3").trim();
        this.generateReport =
            !"false".equalsIgnoreCase(System.getProperty(GENERATE_PROPERTY, "true").trim());
        if (!"dev-test".equals(role) && !"qa".equals(role)) {
            throw new IllegalArgumentException(
                "-D" + ROLE_PROPERTY + " must be 'dev-test' or 'qa', got '" + role + "'"
            );
        }
        for (final String script : List.of(SAVE_EVIDENCE, RECORD_RESULT, GENERATE_REPORT)) {
            if (!Files.isRegularFile(rtmDir.resolve(script))) {
                throw new IllegalArgumentException(
                    "-D" +
                    DIR_PROPERTY +
                    "=" +
                    rtmDir +
                    " is not the RTM repository: missing " +
                    script
                );
            }
        }
    }

    @Override
    public String format() {
        return "rtm";
    }

    @Override
    public void write(final Path runDir, final RunInfo run, final List<TestCaseResult> results)
        throws IOException {
        final Path exported = runDir.resolve(EXPORT_DIR);
        Files.createDirectories(exported);
        final List<TestCaseResult> pending = results
            .stream()
            .filter(r -> !Files.exists(marker(exported, r)))
            .collect(Collectors.toList());
        if (pending.isEmpty()) {
            return;
        }
        final Path log = runDir.resolve(LOG_FILE);
        final List<Map<String, Object>> entries = new ArrayList<>();
        for (final TestCaseResult result : pending) {
            entries.add(batchEntry(result, run, saveEvidence(runDir, result, log)));
        }
        final Path batch = exported.resolve("batch-" + System.currentTimeMillis() + ".yaml");
        // JSON is valid YAML, and PyYAML reads it as the list of mappings the batch file expects
        JSON.writeValue(batch.toFile(), entries);
        runScript(
            log,
            RECORD_RESULT,
            "--root",
            rtmDir.toString(),
            "--cycle",
            cycle,
            "--batch-file",
            batch.toString()
        );
        for (final TestCaseResult result : pending) {
            Files.writeString(
                marker(exported, result),
                result.status().name(),
                StandardCharsets.UTF_8
            );
        }
        if (generateReport) {
            runScript(log, GENERATE_REPORT, "--root", rtmDir.toString(), "--cycle", cycle);
        }
    }

    // Copies the evidence and returns one RTM evidence item per file, in the same order.
    private List<Map<String, String>> saveEvidence(
        final Path runDir,
        final TestCaseResult result,
        final Path log
    )
        throws IOException {
        if (result.evidence().isEmpty()) {
            return List.of();
        }
        final List<String> args = new ArrayList<>(
            List.of(
                "--root",
                rtmDir.toString(),
                "--cycle",
                cycle,
                "--code",
                result.identity().testCaseCode(),
                "--role",
                role
            )
        );
        result.evidence().forEach(e -> args.add(runDir.resolve(e.path()).toString()));
        final List<Path> saved = runScript(log, SAVE_EVIDENCE, args.toArray(new String[0]))
            .lines()
            .filter(line -> line.startsWith(SAVED_PREFIX))
            .map(line -> Paths.get(line.substring(SAVED_PREFIX.length()).trim()))
            .collect(Collectors.toList());
        if (saved.size() != result.evidence().size()) {
            throw new IOException(
                "save_evidence.py reported " +
                saved.size() +
                " files for " +
                result.evidence().size() +
                " evidence items"
            );
        }
        final String code = result.identity().testCaseCode();
        final RtmStepResolver steps = RtmStepResolver.load(rtmDir, code);
        final List<Map<String, String>> items = new ArrayList<>();
        for (int i = 0; i < saved.size(); i++) {
            final Evidence evidence = result.evidence().get(i);
            final Map<String, String> item = new LinkedHashMap<>();
            item.put(
                "file",
                rtmDir
                    .relativize(saved.get(i).toAbsolutePath().normalize())
                    .toString()
                    .replace('\\', '/')
            );
            putIfPresent(item, "caption", evidence.caption());
            if (evidence.step() != null || evidence.assertion() != null) {
                final String position = steps == null
                    ? null
                    : evidence.step() != null
                        ? steps.step(evidence.step())
                        : steps.assertion(evidence.assertion());
                if (position == null) {
                    warn(
                        log,
                        code +
                        ": evidence " +
                        evidence.title() +
                        " published without a link - " +
                        (
                            steps == null
                                ? "test case not found in the RTM"
                                : "no RTM line matches " + describe(evidence)
                        )
                    );
                } else {
                    item.put(evidence.step() != null ? "step" : "assert", position);
                }
            }
            items.add(item);
        }
        return items;
    }

    private Map<String, Object> batchEntry(
        final TestCaseResult result,
        final RunInfo run,
        final List<Map<String, String>> evidence
    ) {
        final String prefix = "qa".equals(role) ? "qa" : "dev";
        final Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("code", result.identity().testCaseCode());
        entry.put(prefix + "Result", rtmResult(result.status()));
        entry.put(prefix + "Tester", tester);
        entry.put(prefix + "Date", LocalDate.now().toString());
        entry.put("notes", notes(result, run));
        if (!evidence.isEmpty()) {
            entry.put(prefix + "Evidence", evidence);
        }
        return entry;
    }

    private static String describe(final Evidence evidence) {
        return evidence.step() != null
            ? "step " + evidence.step() + "."
            : "assert \"" + evidence.assertion() + "\"";
    }

    private static void warn(final Path log, final String message) throws IOException {
        System.err.println("[E2E report] WARNING " + message);
        Files.writeString(
            log,
            "WARNING " + message + System.lineSeparator(),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
    }

    private static void putIfPresent(
        final Map<String, String> item,
        final String key,
        final String value
    ) {
        if (value != null && !value.isBlank()) {
            item.put(key, value);
        }
    }

    private static String rtmResult(final TestStatus status) {
        if (status == TestStatus.PASSED) {
            return "Pass";
        }
        return status == TestStatus.FAILED ? "Fail" : "Blocked";
    }

    private static String notes(final TestCaseResult result, final RunInfo run) {
        final StringBuilder notes = new StringBuilder("Automated E2E run ")
            .append(run.runId())
            .append(" (INGenious ")
            .append(run.ideVersion())
            .append(", ")
            .append(run.inputMode())
            .append(" input, ")
            .append(run.os())
            .append(")");
        if (result.message() != null) {
            final String firstLine = result.message().lines().findFirst().orElse("").trim();
            notes.append(": ").append(firstLine);
        }
        return notes.length() > MAX_NOTES_LENGTH
            ? notes.substring(0, MAX_NOTES_LENGTH - 3) + "..."
            : notes.toString();
    }

    private String runScript(final Path log, final String script, final String... args)
        throws IOException {
        final List<String> command = new ArrayList<>(
            List.of(python, rtmDir.resolve(script).toString())
        );
        command.addAll(List.of(args));
        final Process process = new ProcessBuilder(command)
            .directory(rtmDir.toFile())
            .redirectErrorStream(true)
            .start();
        final String output;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(SCRIPT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new IOException("Timed out: " + String.join(" ", command));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new IOException("Interrupted: " + String.join(" ", command), e);
        }
        Files.writeString(
            log,
            "$ " +
            String.join(" ", command) +
            System.lineSeparator() +
            output +
            System.lineSeparator(),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        );
        if (process.exitValue() != 0) {
            throw new IOException(
                script + " failed (exit " + process.exitValue() + "): " + output.trim()
            );
        }
        return output;
    }

    private static Path marker(final Path exported, final TestCaseResult result) {
        return exported.resolve(result.identity().fileKey() + ".done");
    }

    private static String required(final String property) {
        final String value = System.getProperty(property, "").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Report format 'rtm' needs -D" + property);
        }
        return value;
    }
}
