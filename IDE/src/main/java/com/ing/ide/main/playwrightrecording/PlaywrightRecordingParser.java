package com.ing.ide.main.playwrightrecording;

import com.ing.datalib.or.common.ORAttribute;
import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.web.WebOR;
import com.ing.datalib.or.web.WebORObject;
import com.ing.datalib.or.web.WebORPage;
import com.ing.ide.main.mainui.AppMainFrame;
import java.io.File;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

public class PlaywrightRecordingParser {
    private final AppMainFrame sMainFrame;
    Map<String, String> attribute = new LinkedHashMap<>();
    Map<String, String> filePath = new HashMap<>();
    Map<String, String> testCase = new HashMap<>();
    List<String> ObjectNameList = new ArrayList<>();
    Map<String, HashMap> allObjectMaping = new HashMap<>();
    Map<String, String> objectFrameMap = new HashMap<>();
    Map<String, String> pageMapping = new HashMap<>();
    boolean pageSwitchOnClick = false;

    /** Tracks object names already used during the current live-recording rebuild cycle. */
    private final Set<String> usedLiveObjectNames = new HashSet<>();

    public PlaywrightRecordingParser(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
    }

    /**
     * Creates a new, uniquely named Web OR page for a live recording session.
     *
     * @param basePageName the desired page name (typically the test case name)
     * @return the newly created Web OR page
     */
    public WebORPage createLiveRecordingPage(String basePageName) {
        WebOR webOR = sMainFrame.getProject().getObjectRepository().getWebOR();
        String pageName = basePageName;
        if (webOR.getPageByName(pageName) != null) {
            int counter = 1;
            while (webOR.getPageByName(basePageName + "_" + counter) != null) {
                counter++;
            }
            pageName = basePageName + "_" + counter;
        }
        testCase.put("pageName", pageName);
        return webOR.addPage(pageName);
    }

    /**
     * Returns the resolved page name for the current live recording session.
     */
    public String getLiveRecordingPageName() {
        return testCase.get("pageName");
    }

    /**
     * Returns the object name to use for the current rebuild cycle.
     * <p>
     * Named objects (e.g. {@code username}) are returned as-is so that repeated interactions with
     * the same element (a click then a fill) reuse the same OR object. Only the generic
     * {@code Refactor_Object} fallback is incrementally numbered ({@code Refactor_Object_1},
     * {@code Refactor_Object_2}, ...) since each unnamed element is a distinct object.
     * </p>
     */
    private String resolveUniqueObjectName(String objectName) {
        if (!"Refactor_Object".equals(objectName)) {
            return objectName;
        }
        // Always append suffix starting from _1 for Refactor_Object
        int counter = 1;
        while (usedLiveObjectNames.contains(objectName + "_" + counter)) {
            counter++;
        }
        String uniqueName = objectName + "_" + counter;
        usedLiveObjectNames.add(uniqueName);
        return uniqueName;
    }

    /**
     * Persists the given Web OR page to disk.
     */
    public void saveLiveRecordingPage(WebORPage page) {
        if (page != null) {
            page.getRoot().getObjectRepository().saveWebPageNow(page);
        }
    }

    public void playwrightParser(File file) {
        if (file == null || !file.exists()) {
            return;
        }
        try {
            filePath.put("projectPath", sMainFrame.getProject().getLocation());
            filePath.put("importPlaywrightRecordingFilePath", file.getAbsolutePath());
            String baseName = FilenameUtils.getBaseName(file.getAbsolutePath());
            // Use the basename as-is since sanitization is handled in the UI dialog
            testCase.put("fileName", StringUtils.capitalize(baseName));
            testCase.put("pageName", testCase.get("fileName"));
            String testScenarioName =
                filePath.get("projectPath") + "/TestPlan/" + testCase.get("fileName");
            testScenarioName = testScenarioName.replace("\\", "/");
            testCase.put("testScenarioName", testScenarioName);
            File testScenario = new File(testScenarioName);
            if (!testScenario.exists()) {
                testScenario.mkdirs();
            }
            WebOR webOR = sMainFrame.getProject().getObjectRepository().getWebOR();
            String basePageName = testCase.get("pageName");
            String pageName = basePageName;
            if (webOR.getPageByName(pageName) != null) {
                int counter = 1;
                while (webOR.getPageByName(basePageName + "_" + counter) != null) {
                    counter++;
                }
                pageName = basePageName + "_" + counter;
            }
            testCase.put("pageName", pageName);
            WebORPage page = webOR.addPage(pageName);
            List<String> lines = readFileInList(filePath.get("importPlaywrightRecordingFilePath"));
            Iterator<String> iterator = lines.iterator();
            executeParse(iterator, page, testScenarioName);
            page.getRoot().getObjectRepository().saveWebPageNow(page);
        } catch (Exception ex) {
            Logger.getLogger(PlaywrightRecordingParser.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * A single parsed recording step extracted from the recorder output: the resolved web object
     * name, the INGenious action, its input and condition.
     */
    public static class ParsedStep {
        public final String objectName;
        public final String action;
        public final String input;
        public final String condition;

        public ParsedStep(String objectName, String action, String input, String condition) {
            this.objectName = objectName;
            this.action = action;
            this.input = input;
            this.condition = condition;
        }
    }

    /**
     * Parses the full recorder output into ordered steps using the single, stable parsing logic
     * shared by both file import and live recording.
     * <p>
     * The line-by-line state required for correct results — page-switch detection
     * ({@link #checkPageSwitch}), page index tracking ({@link #storePageIndex}) and current/previous
     * page resolution — is maintained across the whole list. Each detected web object is registered
     * into {@code page} (whose object groups are cleared first so a fresh rebuild is deterministic).
     * </p>
     *
     * @param lines the complete list of recorder output lines
     * @param page  the Web OR page to populate (may be {@code null} to skip OR registration)
     * @return the ordered list of parsed steps
     */
    public List<ParsedStep> parseLinesToSteps(List<String> lines, WebORPage page) {
        return parseLinesToSteps(lines, page, false);
    }

    /**
     * Parses Playwright recorder output into a list of INGenious test steps. This parsing logic is
     * shared by both file import and live recording.
     * <p>
     * The line-by-line state required for correct results — page-switch detection
     * ({@link #checkPageSwitch}), page index tracking ({@link #storePageIndex}) and current/previous
     * page resolution — is maintained across the whole list. Each detected web object is registered
     * into {@code page}.
     * </p>
     *
     * @param lines                   the complete list of recorder output lines
     * @param page                    the Web OR page to populate (may be {@code null} to skip OR registration)
     * @param preserveExistingObjects if {@code true}, preserve existing object groups in the page; if {@code false}, clear them first for a fresh rebuild
     * @return the ordered list of parsed steps
     */
    public List<ParsedStep> parseLinesToSteps(
        List<String> lines,
        WebORPage page,
        boolean preserveExistingObjects
    ) {
        List<ParsedStep> steps = new ArrayList<>();
        if (lines == null) {
            return steps;
        }
        testCaseParameter();
        attributeDeclaration();
        usedLiveObjectNames.clear();
        if (page != null && !preserveExistingObjects) {
            page.getObjectGroups().clear();
        }
        int playwrightSteps = 0;
        // Tracks the page/tab that subsequent actions run against. When an action targets a
        // different already-open page, a switchToPageByIndex step is emitted to switch back to it.
        String activePageIndex = "0";
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            try {
                attributeDeclaration();
                testCaseParameter();
                checkPageSwitch(line);
                storePageIndex(line);
                if (line.trim().startsWith("page")) {
                    pageMapping.put("currentPage", line.trim().split("\\.")[0]);
                }
                if (
                    !line.contains("System.out.println(") &&
                    !line.contains(pageMapping.get("currentPage") + ".onceDialog(dialog") &&
                    !line.contains(".waitForPopup(() ->") &&
                    // Closing the browser externally lets Playwright codegen flush teardown calls
                    // (page/context/browser .close()). These are not user actions and would
                    // otherwise be parsed into a trailing empty step, so skip them.
                    !line.contains(".close()")
                ) {
                    if (line.trim().startsWith("page")) {
                        playwrightSteps++;
                    }
                    if (playwrightSteps >= 1 && !line.contains("}")) {
                        // Resolve the action up front. getAction() consumes the pending
                        // page-switch flag, so it must be called exactly once per line.
                        String resolvedAction = getAction(line);

                        // Determine the page/tab this action runs on (e.g. "page", "page1", ...).
                        // If it is an already-open page different from the active one, switch back
                        // to it first by emitting a switchToPageByIndex step.
                        String linePageVar = line.trim().split("\\.")[0];
                        String linePageIndex = null;
                        if (linePageVar.equals("page")) {
                            linePageIndex = "0";
                        } else if (
                            linePageVar.startsWith("page") && pageMapping.containsKey(linePageVar)
                        ) {
                            linePageIndex = pageMapping.get(linePageVar);
                        }
                        if (linePageIndex != null && !linePageIndex.equals(activePageIndex)) {
                            steps.add(
                                new ParsedStep(
                                    "Browser",
                                    "switchToPageByIndex",
                                    "@" + linePageIndex,
                                    ""
                                )
                            );
                            activePageIndex = linePageIndex;
                        }

                        testCaseMap(resolvedAction, getInput(line));
                        attributeInitialization(line);
                        String resolvedObjectName = testCase.get("ObjectName");
                        if (!"Browser".equals(testCase.get("ObjectName"))) {
                            resolvedObjectName =
                                resolveUniqueObjectName(testCase.get("ObjectName"));
                            if (page != null) {
                                ObjectGroup group = page.getObjectGroupByName(resolvedObjectName);
                                if (group == null) {
                                    group = new ObjectGroup(resolvedObjectName, page);
                                    page.getObjectGroups().add(group);
                                }
                                WebORObject obj = new WebORObject(resolvedObjectName, group);
                                for (Map.Entry<String, String> entry : attribute.entrySet()) {
                                    String key = entry.getKey();
                                    String value = entry.getValue();
                                    if (value != null && !value.isEmpty()) {
                                        // The recorder encodes Playwright's setExact(true) as a
                                        // ";exact" suffix on the attribute value. Normalize it into
                                        // the structured exact flag so the in-memory OR object matches
                                        // the YAML round-tripped form (clean value + checked Exact box).
                                        boolean exactFlag = false;
                                        if (value.endsWith(";exact")) {
                                            exactFlag = true;
                                            value =
                                                value.substring(
                                                    0,
                                                    value.length() - ";exact".length()
                                                );
                                        }
                                        obj.setAttributeByName(key, value);
                                        if (exactFlag) {
                                            ORAttribute orAttr = obj.getAttribute(key);
                                            if (orAttr != null) {
                                                orAttr.setExact(true);
                                            }
                                        }
                                    }
                                }
                                if (!testCase.get("frame").isEmpty()) {
                                    obj.setFrame(testCase.get("frame"));
                                }
                                group.getObjects().clear();
                                group.getObjects().add(obj);
                            }
                        }
                        steps.add(
                            new ParsedStep(
                                resolvedObjectName,
                                testCase.get("action"),
                                testCase.get("input"),
                                testCase.get("Condition")
                            )
                        );
                        testCase.put("input", "");

                        // A click that opens a new page/tab makes that new page the active one
                        // for subsequent actions (the popup page recorded on the preceding
                        // "Page pageN = page.waitForPopup(...)" line).
                        if ("clickAndSwitchToNewPage".equals(resolvedAction)) {
                            String newPageVar = pageMapping.get("switchedPageName");
                            if (newPageVar != null && pageMapping.containsKey(newPageVar)) {
                                activePageIndex = pageMapping.get(newPageVar);
                            }
                        }
                    }
                }
                if (line.trim().startsWith("page")) {
                    pageMapping.put("previousPage", line.trim().split("\\.")[0]);
                }
            } catch (Exception ex) {
                // A partial line (e.g. read while Playwright is mid-rewrite during live recording)
                // must not abort the whole parse; skip it and continue with the rest.
                Logger
                    .getLogger(PlaywrightRecordingParser.class.getName())
                    .log(Level.FINE, "Skipping unparsable recorder line: " + line, ex);
            }
        }
        return steps;
    }

    private void executeParse(Iterator<String> iterator, WebORPage page, String testScenarioName) {
        List<String> lines = new ArrayList<>();
        while (iterator.hasNext()) {
            lines.add(iterator.next());
        }
        List<ParsedStep> steps = parseLinesToSteps(lines, page);

        StringBuilder stepBuilder = new StringBuilder();
        stepBuilder.append("Step,ObjectName,Description,Action,Input,Condition,Reference\n");
        int stepNumber = 1;
        for (ParsedStep ps : steps) {
            // Only add [Project] reference for object-based actions, not for Browser actions
            String reference = (ps.objectName != null && ps.objectName.trim().equals("Browser"))
                ? ""
                : "[Project] " + testCase.get("pageName");
            String stepAppender =
                stepNumber +
                "," +
                csvField(ps.objectName) +
                "," +
                "" +
                "," +
                csvField(ps.action) +
                "," +
                csvField(ps.input) +
                "," +
                csvField(ps.condition) +
                "," +
                csvField(reference);
            stepBuilder.append(stepAppender).append("\n");
            stepNumber++;
        }
        try {
            testCase.put("csvFileName", testCase.get("pageName"));
            filePath.put(
                "csvFilePath",
                testScenarioName + "/" + testCase.get("csvFileName") + ".csv"
            );
            File csvFile = new File(filePath.get("csvFilePath"));
            try (PrintWriter printWriter = new PrintWriter(csvFile)) {
                printWriter.write(stepBuilder.toString());
                printWriter.flush();
            }
        } catch (Exception e) {
            Logger
                .getLogger(PlaywrightRecordingParser.class.getName())
                .log(Level.WARNING, "Failed to write CSV", e);
        }
    }

    public void attributeDeclaration() {
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

    public void testCaseParameter() {
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

    private String getPageName(File testScenario, String pageName) {
        int fileNumber = 0;
        for (File fileNameValidate : testScenario.listFiles()) {
            if (fileNameValidate.isFile()) {
                if (fileNameValidate.getName().contains(pageName)) {
                    fileNumber++;
                }
            }
        }
        String filecount = Integer.toString(fileNumber);
        if (!filecount.equals("0")) {
            pageName = pageName + "_" + filecount;
        }
        return pageName;
    }

    public static List<String> readFileInList(String fileName) {
        List<String> lines = Collections.emptyList();
        try {
            lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return lines;
    }

    public void testCaseMap(String action, String input) {
        testCase.put("action", action);
        testCase.put("input", input);
    }

    public String getAction(String line) {
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
            }
        } else {
            if (line.contains(".navigate(")) {
                actionType = "Open";
            }

            if (line.contains("dialog.accept()")) {
                actionType = "acceptNextAlert";
            }
            if (line.contains("dialog.dismiss()")) {
                actionType = "dismissNextAlert";
            }
        }
        if (pageSwitchOnClick) {
            actionType = "clickAndSwitchToNewPage";
            pageSwitchOnClick = false;
        }
        return actionType;
    }

    public String getInput(String line) {
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
            }
        }
        if (line.contains(".navigate(")) {
            input = "@" + line.split("\\.navigate\\(\"")[1].split("\"")[0];
        }
        // Return the pure semantic value. CSV-specific escaping (for values containing commas,
        // quotes or newlines) is applied only where the CSV file is written, so that callers which
        // consume the value directly (e.g. the live recorder writing to a TestStep) are not given a
        // quote-wrapped value that never gets stripped back out.
        return input;
    }

    /**
     * Escapes a single value for inclusion in a CSV field following RFC 4180 / Excel rules used by
     * the reader ({@code CSVFormat.EXCEL}): if the value contains a comma, double quote, carriage
     * return or line feed, it is wrapped in double quotes and any embedded double quotes are
     * doubled. Other values are returned unchanged.
     */
    private static String csvField(String value) {
        if (value == null) {
            return "";
        }
        if (
            value.contains(",") ||
            value.contains("\"") ||
            value.contains("\r") ||
            value.contains("\n")
        ) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Normalizes the newer Playwright "content frame" locator syntax into the classic
     * {@code frameLocator} form so a single, stable frame-parsing path handles both.
     * <p>
     * Recent Playwright codegen (used by the live recorder) emits frame interactions as
     * {@code page.locator("SEL").contentFrame().getByX(...)} whereas the imported scripts use the
     * older {@code page.frameLocator("SEL").getByX(...)}. Rewriting the former into the latter lets
     * {@link #attributeInitialization} resolve framed elements to properly named objects (with a
     * {@code frame} attribute) instead of falling back to {@code Refactor_Object} chained locators.
     * Nested frames ({@code .locator(a).contentFrame().locator(b).contentFrame()...}) are converted
     * too, since the replacement is applied to every occurrence.
     * </p>
     */
    static String normalizeContentFrame(String line) {
        if (line == null || !line.contains(".contentFrame()")) {
            return line;
        }
        return line.replaceAll("\\.locator\\((.*?)\\)\\.contentFrame\\(\\)", ".frameLocator($1)");
    }

    public void attributeInitialization(String stringLine) {
        try {
            stringLine = normalizeContentFrame(stringLine);
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
                //code to handle chain locator
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
                        testCase.put("ObjectName", "Browser");
                        break;
                    case "dismiss":
                        testCase.put("ObjectName", "Browser");
                        break;
                    case "accept":
                        testCase.put("ObjectName", "Browser");
                        break;
                    case "locator":
                        String css = "";
                        String objectName = "";
                        if (!line.contains(").filter(")) {
                            css =
                                line
                                    .split("locator\\(\"")[1].split("\"\\)")[0].replace("\\", "")
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
                    case "getByRole":
                        String role = "";
                        String roleValue = "";
                        String value = "";
                        String roleSetExact = "";
                        if (line.contains(".setExact(true))")) {
                            roleSetExact = ";exact";
                        } else {
                            roleSetExact = "";
                        }
                        role = line.split("getByRole\\(AriaRole.")[1].split(",")[0].trim();
                        value = line.split(".setName\\(\"")[1].split("\"")[0].trim();
                        roleValue = role + ";" + value + roleSetExact;
                        attribute.put("Role", roleValue);
                        testCase.put("ObjectName", value);
                        break;
                    case "getByPlaceholder":
                        String placeholderSetExact = "";
                        if (line.contains(".setExact(true))")) {
                            placeholderSetExact = ";exact";
                        } else {
                            placeholderSetExact = "";
                        }
                        String placeholder = line.split("getByPlaceholder\\(\"")[1].split("\"")[0];
                        testCase.put("ObjectName", placeholder);
                        attribute.put("Placeholder", placeholder + placeholderSetExact);
                        break;
                    case "getByLabel":
                        String lableSetExact = "";
                        if (line.contains(".setExact(true))")) {
                            lableSetExact = ";exact";
                        } else {
                            lableSetExact = "";
                        }
                        String Label = line.split("getByLabel\\(\"")[1].split("\"")[0];

                        attribute.put("Label", Label + lableSetExact);
                        testCase.put("ObjectName", Label);
                        break;
                    case "getByText":
                        String textSetExact = "";
                        if (line.contains(".setExact(true))")) {
                            textSetExact = ";exact";
                        } else {
                            textSetExact = "";
                        }
                        String text = line.split("getByText\\(\"")[1].split("\"")[0];
                        attribute.put("Text", text + textSetExact);
                        testCase.put("ObjectName", text);
                        break;
                    case "getByTestId":
                        String testId = line.split("getByTestId\\(\"")[1].split("\"")[0];
                        attribute.put("TestId", testId);
                        testCase.put("ObjectName", testId);
                        break;
                    case "getByTitle":
                        String title = line.split("getByTitle\\(\"")[1].split("\"")[0];
                        attribute.put("Title", title);
                        testCase.put("ObjectName", title);
                        break;
                    case "getByAltText":
                        String altText = line.split("getByAltText\\(\"")[1].split("\"")[0];
                        attribute.put("AltText", altText);
                        testCase.put("ObjectName", altText);
                        break;
                }
            }
            if (!line.contains("frameLocator")) {
                // A simple single locator (e.g. page.locator(".a.b.c")) already captured the
                // selector into the css attribute; in that case we must NOT also build a
                // ChainedLocator. Chained locators (.first()/.nth()/multiple parts) skip the
                // switch above via chainAttributeExist(), leaving css empty, so they still chain.
                boolean cssCaptured =
                    attribute.get("css") != null && !attribute.get("css").isEmpty();
                if (
                    !cssCaptured &&
                    (
                        testCase.get("ObjectName").equals("Refactor_Object") ||
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

    public boolean chainAttributeExist(String line) {
        boolean chainAttribute = false;
        if (!line.contains("frameLocator") || !line.contains("dialog.")) {
            line = line.split("[.]", 2)[1];
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

    public void chainAttributeInitialization(String line) {
        testCase.put("ObjectName", "Refactor_Object");
        List<String> p = new ArrayList<>();
        String[] b = line.split("\\)\\.");
        List<Integer> removeObjects = new ArrayList<>();
        List<String> usedObject = new ArrayList<>();
        String chainLocator = "";
        String c = "";

        for (int i = 0; i < b.length; i++) {
            if (i == b.length - 1) {
                c = b[i];
            } else {
                c = b[i] + ")";
            }
            p.add(c);
        }
        for (int j = 0; j < p.size(); j++) {
            if (p.get(j).contains("()") && j != p.size() - 1) {
                String d = p.get(j);
                String e = p.get(j + 1);
                String f = d + "." + e;
                usedObject.add(f);
                j = j + 1;
            } else {
                usedObject.add(p.get(j));
            }
        }

        for (int k = 0; k < usedObject.size(); k++) {
            if (k == usedObject.size() - 1) {
                chainLocator = chainLocator + usedObject.get(k);
            } else {
                chainLocator = chainLocator + usedObject.get(k) + ";";
            }
        }
        chainLocator = chainLocator.replace(pageMapping.get("currentPage") + ".", "");
        attribute.put("ChainedLocator", chainLocator.trim());
    }

    public void checkPageSwitch(String line) {
        if (line.contains("Page page") && line.contains(".waitForPopup(() ->")) {
            pageSwitchOnClick = true;
        }
    }

    public void storePageIndex(String line) {
        if (line.trim().startsWith("Page page")) {
            int pageSideLength = line.split("=", 2)[0].trim().length();
            if (pageSideLength > 9) {
                String index = line.split("=", 2)[0].trim().substring(9).trim();
                String page = line.split("=", 2)[0].trim().substring(5).trim();
                pageMapping.put(page, index);
                pageMapping.put("switchedPageName", page);
            }
            if (pageSideLength == 9) {
                pageMapping.put("page", "0");
            }
        }
    }
}
