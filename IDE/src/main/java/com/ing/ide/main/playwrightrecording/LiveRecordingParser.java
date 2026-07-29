package com.ing.ide.main.playwrightrecording;

import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.web.WebORPage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Translates Playwright {@code codegen --output} Java script content into INGenious test steps.
 * <p>
 * Playwright rewrites the entire output file on every action (and may merge actions, e.g. a click
 * followed by typing becomes a single {@code fill}). Because of that the parser rebuilds the
 * recorded steps from the full file content on each change rather than appending line-by-line.
 * </p>
 * <p>
 * Web objects are extracted via {@link PlaywrightRecordingParser} and registered into a Web OR
 * page so each step references a named object in the repository.
 * </p>
 */
public class LiveRecordingParser {
    public static final String TEXT_INPUT_PLACEHOLDER = "text input detected";

    public static class DeferredTextInput {
        private final int rowIndex;
        private final String actualValue;

        public DeferredTextInput(int rowIndex, String actualValue) {
            this.rowIndex = rowIndex;
            this.actualValue = actualValue;
        }

        public int getRowIndex() {
            return rowIndex;
        }

        public String getActualValue() {
            return actualValue;
        }
    }

    private final PlaywrightRecordingParser parser;
    private final TestCase testCase;
    private final String referenceValue;
    private final int firstInsertIndex;
    private final WebORPage objectPage;
    private final boolean preserveExistingObjects;

    private int addedCount = 0;
    private String lastSignature = null;
    private final List<DeferredTextInput> deferredTextInputs = new ArrayList<>();
    private final List<String> lastLoggedActionLines = new ArrayList<>();

    public LiveRecordingParser(
        PlaywrightRecordingParser parser,
        TestCase testCase,
        int firstInsertIndex,
        String referenceValue,
        WebORPage objectPage
    ) {
        this(parser, testCase, firstInsertIndex, referenceValue, objectPage, false);
    }

    /**
     * Creates a live recording parser.
     *
     * @param parser                   the base Playwright recording parser
     * @param testCase                 the test case to populate with recorded steps
     * @param firstInsertIndex         the index where recorded steps should be inserted
     * @param referenceValue           the reference value for recorded steps
     * @param objectPage               the Web OR page to register recorded objects
     * @param preserveExistingObjects  if {@code true}, preserve any existing objects in the page (for RecordFromHere);
     *                                 if {@code false}, clear objects for a fresh rebuild
     */
    public LiveRecordingParser(
        PlaywrightRecordingParser parser,
        TestCase testCase,
        int firstInsertIndex,
        String referenceValue,
        WebORPage objectPage,
        boolean preserveExistingObjects
    ) {
        this.parser = parser;
        this.testCase = testCase;
        this.referenceValue = referenceValue;
        this.firstInsertIndex = Math.max(firstInsertIndex, 0);
        this.objectPage = objectPage;
        this.preserveExistingObjects = preserveExistingObjects;
    }

    /**
     * Rebuilds the recorded steps from the full recorder output. Returns {@code true} when the
     * recorded steps changed as a result of this call.
     *
     * @param fileLines the complete list of lines currently in the recorder output file
     * @param logger    optional logger for progress messages
     */
    public synchronized boolean syncFromLines(List<String> fileLines, Consumer<String> logger) {
        if (fileLines == null) {
            return false;
        }

        // Use the same stable parsing logic as the file-import path so frame details and
        // page-switch actions are detected identically. parseLinesToSteps maintains the cross-line
        // state (page-switch detection, page indices, current/previous page) and registers the web
        // objects into the recording page.
        List<PlaywrightRecordingParser.ParsedStep> parsedSteps = parser.parseLinesToSteps(
            fileLines,
            objectPage,
            preserveExistingObjects
        );

        String signature = buildSignature(parsedSteps);
        if (signature.equals(lastSignature)) {
            return false;
        }
        lastSignature = signature;

        removePreviouslyAddedSteps();
        deferredTextInputs.clear();

        int insertAt = Math.min(firstInsertIndex, testCase.getTestSteps().size());
        int stepNumber = 0;
        List<String> currentLoggedLines = new ArrayList<>();
        for (PlaywrightRecordingParser.ParsedStep parsedStep : parsedSteps) {
            TestStep step = insertAt < testCase.getTestSteps().size()
                ? testCase.addNewStepAt(insertAt)
                : testCase.addNewStep();

            populateStep(step, parsedStep, insertAt);
            stepNumber++;

            String stepSignature = stepSignature(parsedStep);
            currentLoggedLines.add(stepSignature);

            // Only log steps that are new or changed since the previous cycle to avoid
            // repeating already-reported steps on every rebuild.
            boolean isNewOrChanged =
                stepNumber > lastLoggedActionLines.size() ||
                !stepSignature.equals(lastLoggedActionLines.get(stepNumber - 1));
            if (logger != null && isNewOrChanged) {
                logger.accept(describeStep(stepNumber, step));
            }

            insertAt++;
            addedCount++;
        }

        lastLoggedActionLines.clear();
        lastLoggedActionLines.addAll(currentLoggedLines);

        parser.saveLiveRecordingPage(objectPage);
        return true;
    }

    private String buildSignature(List<PlaywrightRecordingParser.ParsedStep> parsedSteps) {
        StringBuilder sb = new StringBuilder();
        for (PlaywrightRecordingParser.ParsedStep parsedStep : parsedSteps) {
            sb.append(stepSignature(parsedStep)).append('\n');
        }
        return sb.toString();
    }

    private String stepSignature(PlaywrightRecordingParser.ParsedStep parsedStep) {
        return parsedStep.objectName + "|" + parsedStep.action + "|" + parsedStep.input;
    }

    /**
     * Builds a human-readable description for a captured step, e.g.
     * {@code "Step 3 captured: Click on 'Login' [Login_button]"}.
     */
    private String describeStep(int stepNumber, TestStep step) {
        StringBuilder sb = new StringBuilder();
        sb.append("✅ Step ").append(stepNumber).append(" captured: ");
        String action = step.getAction();
        sb.append((action == null || action.isEmpty()) ? "Action" : action);

        String objectName = step.getObject();
        if (objectName != null && !objectName.isEmpty()) {
            sb.append(" on [").append(objectName).append("]");
        }

        String input = step.getInput();
        if (input != null && !input.isEmpty()) {
            String shown = TEXT_INPUT_PLACEHOLDER.equals(input)
                ? "text input"
                : input.startsWith("@") ? input.substring(1) : input;
            sb.append(" = '").append(shown).append("'");
        }
        return sb.toString();
    }

    private void removePreviouslyAddedSteps() {
        for (int k = 0; k < addedCount; k++) {
            if (firstInsertIndex < testCase.getTestSteps().size()) {
                testCase.removeRow(firstInsertIndex);
            }
        }
        addedCount = 0;
    }

    private void populateStep(
        TestStep step,
        PlaywrightRecordingParser.ParsedStep parsedStep,
        int insertAt
    ) {
        String objectName = parsedStep.objectName;
        String action = parsedStep.action;
        String input = parsedStep.input;

        step.setObject(objectName);
        step.setAction(action);
        step.setDescription("");
        step.setCondition(parsedStep.condition == null ? "" : parsedStep.condition);
        step.setReference("Browser".equals(objectName) ? "" : referenceValue);
        step.setNewlyRecorded(true);

        if ("Fill".equals(action) && input != null && !input.isEmpty()) {
            step.setInput(TEXT_INPUT_PLACEHOLDER);
            deferredTextInputs.add(new DeferredTextInput(insertAt, input));
        } else {
            step.setInput(input == null ? "" : input);
        }
    }

    public synchronized int finalizeDeferredInputs() {
        int updates = 0;
        for (DeferredTextInput deferred : deferredTextInputs) {
            if (
                deferred.getRowIndex() >= 0 &&
                deferred.getRowIndex() < testCase.getTestSteps().size()
            ) {
                testCase
                    .getTestSteps()
                    .get(deferred.getRowIndex())
                    .setInput(deferred.getActualValue());
                updates++;
            }
        }
        return updates;
    }
}
