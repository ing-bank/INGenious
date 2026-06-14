package com.ing.ide.main.playwrightrecording;

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
     * Resets the live-recording object registry and clears the given page's object groups so a
     * fresh rebuild from the full recorder output produces deterministic, incrementally numbered
     * object names (e.g. {@code Refactor_Object}, {@code Refactor_Object_1}, ...).
     *
     * @param page the Web OR page being rebuilt
     */
    public void resetLiveObjectRegistry(WebORPage page) {
        usedLiveObjectNames.clear();
        if (page != null) {
            page.getObjectGroups().clear();
        }
    }

    /**
     * Extracts the web object for a single recorder line and registers it into the given Web OR
     * page. Distinct objects that resolve to the same name are uniquely numbered so each detected
     * element gets its own OR object. Navigation/dialog lines resolve to {@code "Browser"} and do
     * not create an OR object.
     *
     * @param line the recorder output line
     * @param page the Web OR page to populate
     * @return the resolved object name to be used in the test step
     */
    public String registerLiveObject(String line, WebORPage page) {
        attributeDeclaration();
        testCaseParameter();
        if (line.trim().startsWith("page")) {
            pageMapping.put("currentPage", line.trim().split("\\.")[0]);
        }
        attributeInitialization(line);

        String objectName = testCase.get("ObjectName");
        if (objectName == null || objectName.isEmpty()) {
            objectName = "Refactor_Object";
        }

        if (!"Browser".equals(objectName) && page != null) {
            objectName = resolveUniqueObjectName(objectName);
            ObjectGroup group = page.getObjectGroupByName(objectName);
            if (group == null) {
                group = new ObjectGroup(objectName, page);
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
            String frame = testCase.get("frame");
            if (frame != null && !frame.isEmpty()) {
                obj.setFrame(frame);
            }
            group.getObjects().clear();
            group.getObjects().add(obj);
        }
        return objectName;
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
        if (!usedLiveObjectNames.contains(objectName)) {
            usedLiveObjectNames.add(objectName);
            return objectName;
        }
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

    private void executeParse(Iterator<String> iterator, WebORPage page, String testScenarioName) {
        StringBuilder stepBuilder = new StringBuilder();
        testCaseParameter();
        attributeDeclaration();
        int stepNumber = 1;
        int playwrightSteps = 0;
        stepBuilder.append("Step,ObjectName,Description,Action,Input,Condition,Reference\n");
        while (iterator.hasNext()) {
            attributeDeclaration();
            testCaseParameter();
            String line = iterator.next();
            checkPageSwitch(line);
            storePageIndex(line);
            if (line.trim().startsWith("page")) {
                pageMapping.put("currentPage", line.trim().split("\\.")[0]);
            }
            if (
                !line.contains("System.out.println(") &&
                !line.contains(pageMapping.get("currentPage") + ".onceDialog(dialog") &&
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
                        ObjectGroup group = page.getObjectGroupByName(objectName);
                        if (group == null) {
                            group = new ObjectGroup(objectName, page);
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
                    testCase.put("step", String.valueOf(stepNumber));
                    // Only add [Project] reference for object-based actions, not for Browser actions
                    String objectName = testCase.get("ObjectName");
                    String reference = (objectName != null && objectName.trim().equals("Browser"))
                        ? ""
                        : "[Project] " + testCase.get("pageName");
                    String stepAppender =
                        testCase.get("step") +
                        "," +
                        testCase.get("ObjectName") +
                        "," +
                        "" +
                        "," +
                        testCase.get("action") +
                        "," +
                        testCase.get("input") +
                        "," +
                        testCase.get("Condition") +
                        "," +
                        reference;
                    stepBuilder.append(stepAppender).append("\n");
                    stepNumber++;
                    testCase.put("input", "");
                }
            }
            if (line.trim().startsWith("page")) {
                pageMapping.put("previousPage", line.trim().split("\\.")[0]);
            }
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
        if (input.contains(",")) {
            input = "\"" + input + "\"";
        }
        return input;
    }

    public void attributeInitialization(String stringLine) {
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
