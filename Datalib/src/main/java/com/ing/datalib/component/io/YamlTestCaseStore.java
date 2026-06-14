package com.ing.datalib.component.io;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.ing.datalib.component.TestStep.HEADERS;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * YAML implementation of {@link TestCaseStore}.
 *
 * <p>Round-trip mapping rules between the on-disk {@link TestCaseYaml.StepYaml}
 * shape and the in-memory {@link com.ing.datalib.component.TestStep} row are:
 *
 * <ul>
 *   <li>The numeric YAML {@code step} index becomes the digit portion of the
 *       in-memory {@code Step} tag.</li>
 *   <li>{@code breakpoint: true} adds a leading {@code *} marker so
 *       {@link com.ing.datalib.component.TestStep#hasBreakPoint()} keeps
 *       returning {@code true}.</li>
 *   <li>{@code comment: true} adds a leading {@code //} marker so
 *       {@link com.ing.datalib.component.TestStep#isCommented()} keeps
 *       returning {@code true}.</li>
 *   <li>{@code hardAssertion: true} adds a {@code ~} marker so
 *       {@link com.ing.datalib.component.TestStep#isHardAssertion()} keeps
 *       returning {@code true}.</li>
 *   <li>Missing fields ({@code input}, {@code condition}, {@code reference})
 *       round-trip as empty strings, matching the CSV behaviour.</li>
 * </ul>
 */
public class YamlTestCaseStore implements TestCaseStore {
    private static final String BREAKPOINT = "*";
    private static final String COMMENT = "//";
    private static final String HARD_ASSERTION = "~";
    private static final Pattern STEP_DIGITS = Pattern.compile("(\\d+)");

    private final ObjectMapper mapper;

    public YamlTestCaseStore() {
        this.mapper = buildMapper();
    }

    static ObjectMapper buildMapper() {
        YAMLFactory factory = new YAMLFactory();
        factory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        factory.enable(YAMLGenerator.Feature.MINIMIZE_QUOTES);
        factory.enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);
        ObjectMapper m = new ObjectMapper(factory);
        m.enable(SerializationFeature.INDENT_OUTPUT);
        m.findAndRegisterModules();
        return m;
    }

    @Override
    public TestCaseFormat format() {
        return TestCaseFormat.YAML;
    }

    @Override
    public List<List<String>> load(File file) throws IOException {
        if (file == null || !file.exists()) {
            return new ArrayList<>();
        }
        TestCaseYaml yaml = mapper.readValue(file, TestCaseYaml.class);
        if (yaml == null || yaml.getSteps() == null) {
            return new ArrayList<>();
        }
        return toRows(yaml);
    }

    /**
     * Reads only the {@code tags:} section of a YAML test case, returning an
     * empty list when the file is absent or carries no tags. Used by the
     * loader to sync on-disk tags back into the project's {@code DataItem}.
     */
    public List<String> loadTags(File file) throws IOException {
        if (file == null || !file.exists()) {
            return new ArrayList<>();
        }
        TestCaseYaml yaml = mapper.readValue(file, TestCaseYaml.class);
        if (yaml == null || yaml.getTags() == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(yaml.getTags());
    }

    private List<List<String>> toRows(TestCaseYaml yaml) {
        List<List<String>> rows = new ArrayList<>(yaml.getSteps().size());
        int idx = 0;
        for (TestCaseYaml.StepYaml step : yaml.getSteps()) {
            idx++;
            rows.add(toRow(step, idx));
        }
        return rows;
    }

    @Override
    public void save(
        File file,
        String testCaseName,
        String scenarioName,
        boolean reusable,
        List<String> tags,
        List<List<String>> rows
    )
        throws IOException {
        CsvTestCaseStore.ensureParent(file);

        TestCaseYaml yaml = new TestCaseYaml();
        // The discriminator key (testCase vs reusable) carries both the kind
        // identifier and the artifact name. Only one is populated.
        if (reusable) {
            yaml.setReusable(testCaseName);
        } else {
            yaml.setTestCase(testCaseName);
        }
        yaml.setScenario(scenarioName);
        if (tags != null && !tags.isEmpty()) {
            yaml.setTags(new ArrayList<>(tags));
        } else {
            yaml.setTags(null);
        }

        List<TestCaseYaml.StepYaml> steps = new ArrayList<>(rows.size());
        int idx = 0;
        for (List<String> row : rows) {
            idx++;
            steps.add(toStepYaml(row, idx));
        }
        yaml.setSteps(steps);

        String body = mapper.writeValueAsString(yaml);
        Files.write(file.toPath(), addBlankLineBetweenSteps(body).getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Inserts a single blank line between each top-level {@code - step:} entry
     * for readability. Jackson YAML does not emit blank lines between sequence
     * items, so we post-process the rendered string.
     */
    static String addBlankLineBetweenSteps(String body) {
        if (body == null || body.isEmpty()) {
            return body;
        }
        // Prefix every "- step:" sequence entry (indent 2) with a blank line.
        String result = body.replace("\n  - step:", "\n\n  - step:");
        // Undo the doubling immediately after the steps: marker (first entry).
        result = result.replaceFirst("steps:\\s*\n\n  - step:", "steps:\n  - step:");
        return result;
    }

    private static List<String> toRow(TestCaseYaml.StepYaml step, int fallbackIndex) {
        int size = HEADERS.size();
        List<String> row = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            row.add("");
        }
        String tag = buildTag(step, fallbackIndex);
        row.set(HEADERS.Step.getIndex(), tag);
        row.set(HEADERS.ObjectName.getIndex(), nullToEmpty(step.getObject()));
        row.set(HEADERS.Description.getIndex(), nullToEmpty(step.getDescription()));
        row.set(HEADERS.Action.getIndex(), nullToEmpty(step.getAction()));
        row.set(HEADERS.Input.getIndex(), nullToEmpty(step.getInput()));
        row.set(HEADERS.Condition.getIndex(), nullToEmpty(step.getCondition()));
        row.set(HEADERS.Reference.getIndex(), nullToEmpty(step.getReference()));
        return row;
    }

    private static String buildTag(TestCaseYaml.StepYaml step, int fallbackIndex) {
        int number = step.getStep() != null ? step.getStep() : fallbackIndex;
        StringBuilder sb = new StringBuilder();
        if (Boolean.TRUE.equals(step.getComment())) {
            sb.append(COMMENT);
        }
        if (Boolean.TRUE.equals(step.getBreakpoint())) {
            sb.append(BREAKPOINT);
        }
        if (Boolean.TRUE.equals(step.getHardAssertion())) {
            sb.append(HARD_ASSERTION);
        }
        sb.append(number);
        return sb.toString();
    }

    private static TestCaseYaml.StepYaml toStepYaml(List<String> row, int fallbackIndex) {
        TestCaseYaml.StepYaml step = new TestCaseYaml.StepYaml();
        String tag = safeGet(row, HEADERS.Step.getIndex());
        step.setStep(parseStepNumber(tag, fallbackIndex));
        if (tag.startsWith(COMMENT)) {
            step.setComment(Boolean.TRUE);
        }
        // Breakpoint marker may sit before or after the optional comment marker.
        String afterComment = tag.startsWith(COMMENT) ? tag.substring(COMMENT.length()) : tag;
        if (afterComment.startsWith(BREAKPOINT)) {
            step.setBreakpoint(Boolean.TRUE);
        }
        if (tag.contains(HARD_ASSERTION)) {
            step.setHardAssertion(Boolean.TRUE);
        }
        step.setObject(emptyToNull(safeGet(row, HEADERS.ObjectName.getIndex())));
        step.setDescription(emptyToNull(safeGet(row, HEADERS.Description.getIndex())));
        step.setAction(emptyToNull(safeGet(row, HEADERS.Action.getIndex())));
        step.setInput(emptyToNull(safeGet(row, HEADERS.Input.getIndex())));
        step.setCondition(emptyToNull(safeGet(row, HEADERS.Condition.getIndex())));
        step.setReference(emptyToNull(safeGet(row, HEADERS.Reference.getIndex())));
        return step;
    }

    private static int parseStepNumber(String tag, int fallback) {
        if (tag == null || tag.isEmpty()) {
            return fallback;
        }
        Matcher m = STEP_DIGITS.matcher(tag);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return fallback;
    }

    private static String safeGet(List<String> row, int idx) {
        return idx < row.size() && row.get(idx) != null ? row.get(idx) : "";
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
