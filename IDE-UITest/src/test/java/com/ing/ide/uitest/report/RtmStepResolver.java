package com.ing.ide.uitest.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Turns {@link StepRef} values into the values the RTM evidence report links to:
 * <ul>
 *   <li>steps: the printed step number, if the RTM test case has a step with that number. Steps
 *   are grouped like {@code group_steps()} in {@code generate_evidence_report.py}: a line starting
 *   with a number ({@code 4.}, {@code 4)}, any whitespace) starts a step, other lines continue it.</li>
 *   <li>asserts: the 1-based position among the non-empty {@code asserts} lines.</li>
 * </ul>
 */
final class RtmStepResolver {
    private static final YAMLMapper YAML = new YAMLMapper();
    private static final Pattern INVISIBLE = Pattern.compile(
        "[\\u200b\\u200c\\u200d\\u2060\\ufeff]"
    );
    // Leading/trailing whitespace incl. non-breaking spaces, like Python's str.strip()
    private static final Pattern EDGE_SPACE = Pattern.compile(
        "^[\\s\\p{Z}]+|[\\s\\p{Z}]+$",
        Pattern.UNICODE_CHARACTER_CLASS
    );
    private static final Pattern STEP_NUMBER = Pattern.compile(
        "^\\s*(\\d+)\\s*[.)](?:\\s|$)",
        Pattern.UNICODE_CHARACTER_CLASS
    );

    private final List<String> steps;
    private final List<String> asserts;

    private RtmStepResolver(final List<String> steps, final List<String> asserts) {
        this.steps = steps;
        this.asserts = asserts;
    }

    /**
     * Loads a test case from the RTM. If its code appears more than once, the first is used.
     *
     * @param rtmDir RTM repository root
     * @param testCaseCode e.g. {@code GEN-SC001-TC001}
     * @return the resolver, or {@code null} if the test case is not in the RTM
     * @throws IOException if a scenario file cannot be read
     */
    static RtmStepResolver load(final Path rtmDir, final String testCaseCode) throws IOException {
        final Path scenarios = rtmDir.resolve("scenarios");
        final String scenarioCode = testCaseCode.replaceFirst("-TC\\d+.*$", "");
        try (Stream<Path> files = Files.list(scenarios)) {
            for (final Path file : (Iterable<Path>) files
                .filter(
                    f ->
                        f.getFileName().toString().startsWith(scenarioCode) &&
                        f.toString().endsWith(".yaml")
                )
                .sorted()::iterator) {
                for (final JsonNode tc : YAML.readTree(file.toFile()).path("testCases")) {
                    if (testCaseCode.equals(tc.path("testCaseCode").asText())) {
                        return new RtmStepResolver(
                            lines(tc.path("steps")),
                            lines(tc.path("asserts"))
                        );
                    }
                }
            }
        }
        return null;
    }

    /**
     * @param printedNumber printed step number, e.g. "4"
     * @return the step number to link to, or {@code null} if the test case has no such step
     */
    String step(final String printedNumber) {
        for (final String line : steps) {
            final Matcher m = STEP_NUMBER.matcher(line);
            if (m.find() && m.group(1).equals(printedNumber)) {
                return printedNumber;
            }
        }
        return null;
    }

    /**
     * @param text assert text or part of it
     * @return 1-based line position, or {@code null} if no line contains it
     */
    String assertion(final String text) {
        final String needle = text.toLowerCase(Locale.ROOT);
        for (int i = 0; i < asserts.size(); i++) {
            if (asserts.get(i).toLowerCase(Locale.ROOT).contains(needle)) {
                return String.valueOf(i + 1);
            }
        }
        return null;
    }

    private static List<String> lines(final JsonNode value) {
        if (value.isMissingNode() || value.isNull()) {
            return List.of();
        }
        return Arrays
            .stream(value.asText().split("\\R"))
            .map(line -> EDGE_SPACE.matcher(INVISIBLE.matcher(line).replaceAll("")).replaceAll(""))
            .filter(line -> !line.isEmpty())
            .collect(Collectors.toList());
    }
}
