package com.ing.datalib.api.importer.playwright;

import com.ing.datalib.api.importer.ImportUtils;
import com.ing.datalib.component.Project;
import com.ing.datalib.component.Scenario;
import com.ing.datalib.component.TestCase;
import com.ing.datalib.component.TestStep;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * UI-free port of the IDE's {@code PlaywrightRecordingParser}. Parses a
 * Playwright Java recording (the text emitted by Playwright's codegen) into
 * an INGenious test case + object repository entries.
 *
 * <p>This class is the <em>single source of truth</em> for the
 * Playwright-recording → INGenious conversion. Both the Swing IDE
 * (Tools → Import Playwright Recording) and the CLI/MCP
 * ({@code ingenious import playwright}, {@code ingenious_import_playwright})
 * delegate here so behaviour stays identical across surfaces.
 *
 * <p>It does <em>not</em> mutate the in-memory {@link Project} state beyond
 * adding the {@link WebORPage} + objects to the project's object repository
 * and creating the {@link TestCase} via the standard datalib API. The test
 * case is persisted via {@link TestCase#save()}, which respects the project's
 * configured format (YAML by default, CSV when the scenario already uses CSV).
 * Callers may optionally invoke {@code project.reload()} afterwards if they
 * need a fresh in-memory view.
 */
public final class PlaywrightRecordingImporter {

    /** Result of an import call. */
    public static final class Result {
        public final String scenarioName;
        public final String testCaseName;
        public final String pageName;
        public final int stepCount;
        public final List<String> warnings;

        Result(
            String scenarioName,
            String testCaseName,
            String pageName,
            int stepCount,
            List<String> warnings
        ) {
            this.scenarioName = scenarioName;
            this.testCaseName = testCaseName;
            this.pageName = pageName;
            this.stepCount = stepCount;
            this.warnings = warnings;
        }
    }

    // --- per-invocation state (mirrors the IDE parser) -----------------
    private final Map<String, String> attribute = new LinkedHashMap<>();
    private final Map<String, String> filePath = new HashMap<>();
    private final Map<String, String> testCase = new HashMap<>();
    private final Map<String, String> pageMapping = new HashMap<>();
    private boolean pageSwitchOnClick = false;
    private final List<String> warnings = new ArrayList<>();

    private PlaywrightRecordingImporter() {}

    /**
     * Imports a Playwright recording.
     *
     * @param project       loaded INGenious project (must have a location on disk)
     * @param recording     recording file produced by Playwright codegen
     * @param scenarioName  scenario folder under {@code TestPlan/}; if
     *                      {@code null}/empty defaults to the capitalised file
     *                      base name (matching the IDE behaviour)
     * @param testCaseName  desired test-case name; if {@code null}/empty
     *                      defaults to the capitalised file base name; a numeric
     *                      suffix ({@code _1}, {@code _2}, ...) is appended if a
     *                      {@code WebORPage} with that name already exists
     */
    public static Result importInto(
        Project project,
        File recording,
        String scenarioName,
        String testCaseName
    ) {
        if (project == null) throw new IllegalArgumentException("project is required");
        if (recording == null || !recording.exists()) {
            throw new IllegalArgumentException("Recording file not found: " + recording);
        }
        return new PlaywrightRecordingImporter()
        .run(project, recording, scenarioName, testCaseName);
    }

    private Result run(Project project, File recording, String scenarioName, String testCaseName) {
        filePath.put("projectPath", project.getLocation());
        filePath.put("importPlaywrightRecordingFilePath", recording.getAbsolutePath());

        String fileBase = capitalize(baseName(recording.getAbsolutePath()));

        String scenarioFolder = (scenarioName != null && !scenarioName.isEmpty())
            ? ImportUtils.sanitizeFileName(scenarioName)
            : fileBase;
        String desiredTcName = (testCaseName != null && !testCaseName.isEmpty())
            ? ImportUtils.sanitizeFileName(testCaseName)
            : fileBase;

        testCase.put("fileName", scenarioFolder);
        testCase.put("pageName", desiredTcName);

        // Use the standard Project/Scenario API so the test case is registered
        // in-memory and persisted in the project's configured format (YAML by
        // default). This replaces the older flow that wrote a CSV file directly
        // and required a project.reload() to pick it up.
        Scenario scn = project.getScenarioByName(scenarioFolder);
        if (scn == null) {
            scn = project.addScenario(scenarioFolder);
            File scnDir = new File(scn.getLocation());
            if (!scnDir.exists()) scnDir.mkdirs();
        }
        String tcName = desiredTcName;
        if (scn.getTestCaseByName(tcName) != null) {
            int n = 1;
            while (scn.getTestCaseByName(desiredTcName + "_" + n) != null) n++;
            tcName = desiredTcName + "_" + n;
        }
        TestCase tc = scn.addTestCase(tcName);
        if (tc == null) {
            throw new IllegalStateException(
                "Failed to create test case: " + scenarioFolder + "/" + tcName
            );
        }

        WebOR webOR = project.getObjectRepository().getWebOR();
        String pageName = tcName;
        if (webOR.getPageByName(pageName) != null) {
            int counter = 1;
            while (webOR.getPageByName(tcName + "_" + counter) != null) counter++;
            pageName = tcName + "_" + counter;
        }
        testCase.put("pageName", pageName);
        WebORPage page = webOR.addPage(pageName);

        List<String> lines = readFileInList(filePath.get("importPlaywrightRecordingFilePath"));
        Iterator<String> iterator = lines.iterator();
        int stepCount = executeParse(iterator, page, tc);
        page.getRoot().getObjectRepository().saveWebPageNow(page);
        tc.save();

        return new Result(scenarioFolder, tcName, pageName, stepCount, new ArrayList<>(warnings));
    }

    private int executeParse(Iterator<String> iterator, WebORPage page, TestCase tc) {
        testCaseParameter();
        attributeDeclaration();
        int stepNumber = 1;
        int playwrightSteps = 0;
        while (iterator.hasNext()) {
            attributeDeclaration();
            testCaseParameter();
            String line = iterator.next();
            checkPageSwitch(line);
            storePageIndex(line);
            if (line.trim().startsWith("page")) {
                pageMapping.put("currentPage", line.trim().split("\\.")[0]);
            }
            String currentPage = pageMapping.getOrDefault("currentPage", "page");
            if (
                !line.contains("System.out.println(") &&
                !line.contains(currentPage + ".onceDialog(dialog") &&
                !line.contains(".waitForPopup(() ->")
            ) {
                if (line.trim().startsWith("page")) {
                    playwrightSteps++;
                }
                if (playwrightSteps >= 1 && !line.contains("}")) {
                    testCaseMap(getAction(line), getInput(line));
                    attributeInitialization(line);
                    if (!"Browser".equals(testCase.get("ObjectName"))) {
                        String objectName = testCase.get("ObjectName");
                        ObjectGroup<WebORObject> group = page.getObjectGroupByName(objectName);
                        if (group == null) {
                            group = new ObjectGroup<>(objectName, page);
                            page.getObjectGroups().add(group);
                        }
                        WebORObject obj = new WebORObject(objectName, group);
                        for (Map.Entry<String, String> entry : attribute.entrySet()) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            if (value != null && !value.isEmpty()) {
                                obj.setAttributeByName(key, value);
                            }
                        }
                        if (!testCase.get("frame").isEmpty()) {
                            obj.setFrame(testCase.get("frame"));
                        }
                        group.getObjects().clear();
                        group.getObjects().add(obj);
                    }
                    String objectName = testCase.get("ObjectName");
                    String reference = (objectName != null && objectName.trim().equals("Browser"))
                        ? ""
                        : "[Project] " + testCase.get("pageName");
                    TestStep step = tc.addNewStep();
                    // The first auto-step added by Scenario.addTestCase has tag
                    // "1" already — we just want to set the column values.
                    step.setTag(String.valueOf(stepNumber));
                    step.setObject(nullToEmpty(objectName));
                    step.setAction(nullToEmpty(testCase.get("action")));
                    step.setInput(nullToEmpty(testCase.get("input")));
                    step.setCondition(nullToEmpty(testCase.get("Condition")));
                    step.setReference(reference);
                    stepNumber++;
                    testCase.put("input", "");
                }
            }
            if (line.trim().startsWith("page")) {
                pageMapping.put("previousPage", line.trim().split("\\.")[0]);
            }
        }
        // Scenario.addTestCase pre-populates a blank step at index 0. If the
        // recording produced at least one real step, drop the blank.
        int produced = stepNumber - 1;
        if (produced > 0 && tc.getTestSteps().size() > produced) {
            tc.getTestSteps().remove(0);
        }
        return produced;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void attributeDeclaration() {
        attribute.put("Role", "");
        attribute.put("xpath", "");
        attribute.put("Text", "");
        attribute.put("css", "");
        attribute.put("Placeholder", "");
        attribute.put("Label", "");
        attribute.put("AltText", "");
        attribute.put("Title", "");
        attribute.put("TestId", "");
        attribute.put("ChainedLocator", "");
    }

    private void testCaseParameter() {
        testCase.put("action", "");
        testCase.put("actionName", "");
        testCase.put("input", "");
        testCase.put("Condition", "");
        testCase.put("step", "");
        testCase.put("Object", "");
        testCase.put("stepAppender", "");
        testCase.put("testScenarioName", "");
        testCase.put("ObjectName", "");
        testCase.put("frame", "");
    }

    private static List<String> readFileInList(String fileName) {
        try {
            return Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    private void testCaseMap(String action, String input) {
        testCase.put("action", action);
        testCase.put("input", input);
    }

    private String getAction(String line) {
        String actionType = "";
        if (
            !line.contains(".navigate(") &&
            !line.contains("dialog.dismiss()") &&
            !line.contains("dialog.accept()")
        ) {
            int length = line.split("\\)\\.").length;
            String action = ((line.split("\\)\\.")[length - 1])).split("\\(")[0];
            switch (action) {
                case "click":
                    actionType = "Click";
                    break;
                case "fill":
                    actionType = "Fill";
                    break;
                case "selectOption":
                    actionType = "SelectSingleByText";
                    break;
                case "check":
                    actionType = "Check";
                    break;
                case "press":
                    actionType = "KeyPress";
                    break;
                case "isEmpty":
                    actionType = "assertElementIsEmpty";
                    break;
                case "isVisible":
                    actionType = "assertElementIsVisible";
                    break;
                case "containsText":
                    actionType = "assertElementContainsText";
                    break;
                case "hasValue":
                    actionType = "assertElementValueMatches";
                    break;
                default:
                    break;
            }
        } else {
            if (line.contains(".navigate(")) actionType = "Open";
            if (line.contains("dialog.accept()")) actionType = "acceptNextAlert";
            if (line.contains("dialog.dismiss()")) actionType = "dismissNextAlert";
        }
        if (pageSwitchOnClick) {
            actionType = "clickAndSwitchToNewPage";
            pageSwitchOnClick = false;
        }
        return actionType;
    }

    private String getInput(String line) {
        String input = "";
        if (!line.contains(".navigate(")) {
            int length = line.split("\\)\\.").length;
            String action = ((line.split("\\)\\.")[length - 1])).split("\\(")[0];
            switch (action) {
                case "click":
                case "check":
                case "isEmpty":
                case "isVisible":
                    input = "";
                    break;
                case "press":
                case "selectOption":
                case "fill":
                case "hasValue":
                case "containsText":
                    input =
                        "@" + ((line.split("\\)\\.")[length - 1])).split("\\(")[1].split("\"")[1];
                    break;
                default:
                    break;
            }
        }
        if (line.contains(".navigate(")) {
            input = "@" + line.split("\\.navigate\\(\"")[1].split("\"")[0];
        }
        if (input.contains(",")) {
            input = "\"" + input + "\"";
        }
        return input;
    }

    private void attributeInitialization(String stringLine) {
        try {
            String line = "";
            if (stringLine.contains(").click(")) {
                line = stringLine.split("\\.click\\(")[0];
            } else if (stringLine.contains(").fill(")) {
                line = stringLine.split("\\.fill\\(")[0];
            } else if (stringLine.contains(").selectOption(")) {
                line = stringLine.split("\\.selectOption\\(")[0];
            } else if (stringLine.contains(").check(")) {
                line = stringLine.split("\\.check\\(")[0];
            } else if (stringLine.contains("assertThat(page")) {
                stringLine = stringLine.split("assertThat\\(")[1];
                if (stringLine.contains(")).isVisible(")) {
                    stringLine = stringLine.replace("\\)\\)\\.isVisible(", "\\)\\.isVisible\\(");
                    line = stringLine.split("\\.isVisible\\(")[0];
                } else if (stringLine.contains(")).isEmpty(")) {
                    stringLine = stringLine.replace("\\)\\)\\.isEmpty(", "\\)\\.isEmpty\\(");
                    line = stringLine.split("\\.isEmpty\\(")[0];
                } else if (stringLine.contains(")).containsText(")) {
                    stringLine =
                        stringLine.replace("\\)\\)\\.containsText(", "\\)\\.containsText\\(");
                    line = stringLine.split("\\.containsText\\(")[0];
                } else if (stringLine.contains(")).hasValue(")) {
                    stringLine = stringLine.replace("\\)\\)\\.hasValue(", "\\)\\.hasValue\\(");
                    line = stringLine.split("\\.hasValue\\(")[0];
                }
            }
            if (line.contains("frameLocator(")) {
                String frame = line.split("\"\\)\\.")[0].split("frameLocator\\(\"")[1];
                testCase.put("frame", frame.replace("\\", ""));
                testCase.put("ObjectName", "Refactor_Object");
                stringLine = line.split("]\"\\)")[1];
                if (stringLine.contains("frameLocator(\"")) {
                    String frameLocator2 = stringLine
                        .split("frameLocator\\(\"", 2)[1].split("\"\\)\\.", 2)[0];
                    stringLine =
                        "." + stringLine.split("frameLocator\\(\"", 2)[1].split("\"\\)\\.")[1];
                    String chainedFrameLocator = testCase.get("frame") + ";" + frameLocator2;
                    testCase.put("frame", chainedFrameLocator);
                }
            }
            if (!chainAttributeExist(stringLine) && !stringLine.contains(".press(\"")) {
                switch (stringLine.split("\\(")[0].split("\\.")[1]) {
                    case "navigate":
                    case "dismiss":
                    case "accept":
                        testCase.put("ObjectName", "Browser");
                        break;
                    case "locator":
                        {
                            String css;
                            String objectName = "";
                            if (!line.contains(").filter(")) {
                                css =
                                    line
                                        .split("locator\\(\"")[1].split("\"\\)")[0].replace(
                                            "\\",
                                            ""
                                        )
                                        .trim();
                                if (css.contains("[")) {
                                    objectName = css.split("\"")[1].replace("\\", "");
                                } else if (css.contains("#")) {
                                    objectName = css.replace("#", "");
                                } else if (css.contains("$")) {
                                    objectName = css.replace("$", "");
                                } else if (css.contains("^")) {
                                    objectName = css.replace("^", "");
                                }
                                attribute.put("css", css);
                                testCase.put("ObjectName", objectName);
                                if (testCase.get("ObjectName").equals("")) {
                                    testCase.put("ObjectName", "Refactor_Object");
                                }
                            } else {
                                testCase.put("ObjectName", "Refactor_Object");
                            }
                            break;
                        }
                    case "getByRole":
                        {
                            String roleSetExact = line.contains(".setExact(true))") ? ";exact" : "";
                            String role = line
                                .split("getByRole\\(AriaRole.")[1].split(",")[0].trim();
                            String value = line.split(".setName\\(\"")[1].split("\"")[0].trim();
                            String roleValue = role + ";" + value + roleSetExact;
                            attribute.put("Role", roleValue);
                            testCase.put("ObjectName", value);
                            break;
                        }
                    case "getByPlaceholder":
                        {
                            String placeholderSetExact = line.contains(".setExact(true))")
                                ? ";exact"
                                : "";
                            String placeholder = line
                                .split("getByPlaceholder\\(\"")[1].split("\"")[0];
                            testCase.put("ObjectName", placeholder);
                            attribute.put("Placeholder", placeholder + placeholderSetExact);
                            break;
                        }
                    case "getByLabel":
                        {
                            String lableSetExact = line.contains(".setExact(true))")
                                ? ";exact"
                                : "";
                            String label = line.split("getByLabel\\(\"")[1].split("\"")[0];
                            attribute.put("Label", label + lableSetExact);
                            testCase.put("ObjectName", label);
                            break;
                        }
                    case "getByText":
                        {
                            String textSetExact = line.contains(".setExact(true))") ? ";exact" : "";
                            String text = line.split("getByText\\(\"")[1].split("\"")[0];
                            attribute.put("Text", text + textSetExact);
                            testCase.put("ObjectName", text);
                            break;
                        }
                    case "getByTestId":
                        {
                            String testId = line.split("getByTestId\\(\"")[1].split("\"")[0];
                            attribute.put("TestId", testId);
                            testCase.put("ObjectName", testId);
                            break;
                        }
                    case "getByTitle":
                        {
                            String title = line.split("getByTitle\\(\"")[1].split("\"")[0];
                            attribute.put("Title", title);
                            testCase.put("ObjectName", title);
                            break;
                        }
                    case "getByAltText":
                        {
                            String altText = line.split("getByAltText\\(\"")[1].split("\"")[0];
                            attribute.put("AltText", altText);
                            testCase.put("ObjectName", altText);
                            break;
                        }
                    default:
                        break;
                }
            }
            if (!line.contains("frameLocator")) {
                if (
                    testCase.get("ObjectName").equals("Refactor_Object") ||
                    (
                        testCase.get("ObjectName").equals("") &&
                        !testCase.get("ObjectName").equals("Browser")
                    )
                ) {
                    chainAttributeInitialization(line);
                }
            }
            if (stringLine.contains(".press(\"")) {
                testCase.put("ObjectName", "Browser");
            }
            testCase.put("ObjectName", testCase.get("ObjectName").replace(",", ""));
        } catch (Exception e) {
            testCase.put("ObjectName", "Refactor_Object");
        }
    }

    private boolean chainAttributeExist(String line) {
        boolean chainAttribute = false;
        if (!line.contains("frameLocator") || !line.contains("dialog.")) {
            String[] split = line.split("[.]", 2);
            if (split.length < 2) return false;
            line = split[1];
            String[] locatorList = {
                ".getByAltText",
                ".getByTitle",
                ".getByTestId",
                ".getByText",
                ".getByLabel",
                ".getByPlaceholder",
                ".getByRole",
                ".locator",
                ".first()",
                ".last()",
                ".filter",
                ".nth("
            };
            for (String locator : locatorList) {
                if (line.contains(locator)) {
                    chainAttribute = true;
                    break;
                }
            }
        }
        return chainAttribute;
    }

    private void chainAttributeInitialization(String line) {
        testCase.put("ObjectName", "Refactor_Object");
        List<String> p = new ArrayList<>();
        String[] b = line.split("\\)\\.");
        List<String> usedObject = new ArrayList<>();
        String chainLocator = "";

        for (int i = 0; i < b.length; i++) {
            String c = (i == b.length - 1) ? b[i] : b[i] + ")";
            p.add(c);
        }
        for (int j = 0; j < p.size(); j++) {
            if (p.get(j).contains("()") && j != p.size() - 1) {
                String d = p.get(j);
                String e = p.get(j + 1);
                usedObject.add(d + "." + e);
                j = j + 1;
            } else {
                usedObject.add(p.get(j));
            }
        }
        for (int k = 0; k < usedObject.size(); k++) {
            chainLocator +=
                (k == usedObject.size() - 1) ? usedObject.get(k) : usedObject.get(k) + ";";
        }
        String currentPage = pageMapping.getOrDefault("currentPage", "page");
        chainLocator = chainLocator.replace(currentPage + ".", "");
        attribute.put("ChainedLocator", chainLocator.trim());
    }

    private void checkPageSwitch(String line) {
        if (line.contains("Page page") && line.contains(".waitForPopup(() ->")) {
            pageSwitchOnClick = true;
        }
    }

    /** Mirror of {@code FilenameUtils.getBaseName} — strip directory + extension. */
    private static String baseName(String path) {
        if (path == null) return "";
        String n = path;
        int sep = Math.max(n.lastIndexOf('/'), n.lastIndexOf('\\'));
        if (sep >= 0) n = n.substring(sep + 1);
        int dot = n.lastIndexOf('.');
        return (dot > 0) ? n.substring(0, dot) : n;
    }

    /** Mirror of {@code StringUtils.capitalize}. */
    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        char first = s.charAt(0);
        if (Character.isTitleCase(first)) return s;
        return Character.toTitleCase(first) + s.substring(1);
    }

    private void storePageIndex(String line) {
        if (line.trim().startsWith("Page page")) {
            int pageSideLength = line.split("=", 2)[0].trim().length();
            if (pageSideLength > 9) {
                String index = line.split("=", 2)[0].trim().substring(9).trim();
                String pg = line.split("=", 2)[0].trim().substring(5).trim();
                pageMapping.put(pg, index);
                pageMapping.put("switchedPageName", pg);
            }
            if (pageSideLength == 9) {
                pageMapping.put("page", "0");
            }
        }
    }
}
