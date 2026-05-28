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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    public PlaywrightRecordingParser(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
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
            String testScenarioName = filePath.get("projectPath") + "/TestPlan/" + testCase.get("fileName");
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
            page.getRoot() .getObjectRepository() .saveWebPageNow(page);
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
            if (!line.contains("System.out.println(")
                    && !line.contains(pageMapping.get("currentPage") + ".onceDialog(dialog")
                    && !line.contains(".waitForPopup(() ->")) {
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
                            testCase.get("step") + "," +
                            testCase.get("ObjectName") + "," +
                            "" + "," +
                            testCase.get("action") + "," +
                            testCase.get("input") + "," +
                            testCase.get("Condition") + "," +
                            reference;
                    stepBuilder.append(stepAppender).append("\n");
                    stepNumber++;
                    testCase.put("input", "");
                }
            }
            if (line.trim().startsWith("page")) {
                pageMapping.put(
                        "previousPage",
                        line.trim().split("\\.")[0]
                );
            }
        }
        try {
            testCase.put("csvFileName", testCase.get("pageName"));
            filePath.put(
                    "csvFilePath",
                    testScenarioName + "/"
                            + testCase.get("csvFileName")
                            + ".csv"
            );
            File csvFile = new File(filePath.get("csvFilePath"));
            try (PrintWriter printWriter = new PrintWriter(csvFile)) {
                printWriter.write(stepBuilder.toString());
                printWriter.flush();
            }
        } catch (Exception e) {
            Logger.getLogger(PlaywrightRecordingParser.class.getName()).log(Level.WARNING, "Failed to write CSV", e);
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
            lines = Files.readAllLines(
                    Paths.get(fileName),
                    StandardCharsets.UTF_8);
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
        if (!line.contains(".navigate(") && !line.contains("dialog.dismiss()") && !line.contains("dialog.accept()")) {
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
                    input = "@" + ((line.split("\\)\\.")[length - 1])).split("\\(")[1].split("\"")[1];
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
                    stringLine = stringLine.replace("\\)\\)\\.containsText(", "\\)\\.containsText\\(");
                    line = stringLine.split("\\.containsText\\(")[0];
                }
                else if (stringLine.contains(")).hasValue(")) {
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
                    String frameLocator2 = stringLine.split("frameLocator\\(\"", 2)[1].split("\"\\)\\.", 2)[0];
                    stringLine = "." + stringLine.split("frameLocator\\(\"", 2)[1].split("\"\\)\\.")[1];
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
                            css = line.split("locator\\(\"")[1].split("\"\\)")[0].replace("\\", "").trim();
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
                if (testCase.get("ObjectName").equals("Refactor_Object") || testCase.get("ObjectName").equals("") && !testCase.get("ObjectName").equals("Browser")) {
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
            String[] locatorList = {".getByAltText", ".getByTitle", ".getByTestId", ".getByText", ".getByLabel", ".getByPlaceholder", ".getByRole", ".locator", ".first()", ".last()", ".filter", ".nth("};
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
