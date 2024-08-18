package com.ing.ide.main.playwrightrecording;

import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.util.logging.UILogger;
import java.io.File;
import java.io.IOException;
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
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class PlaywrightRecordingParser {

    private final AppMainFrame sMainFrame;
    Map<String, String> attribute = new LinkedHashMap<>();
    Map<String, String> filePath = new HashMap<>();
    Map<String, String> testCase = new HashMap<>();
    List<String> ObjectNameList = new ArrayList<>();
    Map<String, HashMap> allObjectMaping = new HashMap<>();

    public PlaywrightRecordingParser(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
    }

    public void playwrightParser(File file) throws ParserConfigurationException, TransformerException, SAXException, IOException {
        if (file != null && file.exists()) {
            try {
                filePath.put("projectPath", sMainFrame.getProject().getLocation());
                filePath.put("importPlaywrightRecordingFilePath", file.getAbsolutePath());
                File PlaywrightRecordingfile = new File(filePath.get("importPlaywrightRecordingFilePath"));
                filePath.put("orFilePath", (filePath.get("projectPath") + "/OR.object"));
                String baseName = FilenameUtils.getBaseName(filePath.get("importPlaywrightRecordingFilePath"));
                testCase.put("fileName", StringUtils.capitalize(baseName));
                File OrFile = new File(filePath.get("orFilePath"));
                Element objectFrame = null;
                testCase.put("pageName", testCase.get("fileName"));
                testCase.put("testScenarioName", (filePath.get("projectPath") + "/TestPlan/" + testCase.get("fileName")).replace("\\", "/"));
                File testScenario = new File(testCase.get("testScenarioName"));
                if (!testScenario.exists()) {
                    testScenario.mkdirs();
                }
                testCase.put("pageName", getPageName(testScenario, testCase.get("pageName")));

                if (!OrFile.exists()) {
                    boolean orExistFlag = false;
                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    Document doc = dBuilder.newDocument();
                    Element rootElement = doc.createElement("Root");
                    doc.appendChild(rootElement);
                    rootElement.setAttribute("ref", testCase.get("fileName"));
                    rootElement.setAttribute("type", "OR");
                    Element page = doc.createElement("Page");
                    rootElement.appendChild(page);

                    List l = readFileInList(
                            filePath.get("importPlaywrightRecordingFilePath"));

                    Iterator<String> iterator = l.iterator();
                    executeParse(iterator, objectFrame, testCase.get("testScenarioName"), filePath.get("orFilePath"), page, doc, orExistFlag, rootElement);
                    allObjectMaping.clear();
                } else {
                    boolean orExistFlag = true;
                    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                    Document doc = documentBuilder.parse(new File(filePath.get("orFilePath")));
                    doc.getDocumentElement().normalize();
                    Element root = doc.getDocumentElement();
                    Element page = doc.createElement("Page");
                    List l = readFileInList(
                            filePath.get("importPlaywrightRecordingFilePath"));

                    Iterator<String> iterator = l.iterator();
                    executeParse(iterator, objectFrame, testCase.get("testScenarioName"), filePath.get("orFilePath"), page, doc, orExistFlag, root);
                    allObjectMaping.clear();
                }
                testCase.clear();
                attribute.clear();
                filePath.clear();
                ObjectNameList.clear();
            } catch (Exception ex) {
                Logger.getLogger(PlaywrightRecordingParser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void executeParse(Iterator iterator, Element objectFrame, String testScenarioName, String orFilePath, Element page, Document doc, boolean orExistFlag, Element root) throws TransformerException {

        StringBuilder stepBuilder = new StringBuilder();
        testCaseParameter();
        attributeDeclaration();
        int stepNumber = 1;
        stepBuilder.append("Step" + "," + "ObjectName" + "," + "Description" + "," + "Action" + "," + "Input" + ","
                + "Condition" + "," + "Reference");
        stepBuilder.append("\n");
        page.setAttribute("ref", testCase.get("pageName"));
        page.setAttribute("title", "");
        int playwrightSteps = 0;
        while (iterator.hasNext()) {
            attributeDeclaration();
            testCaseParameter();
            String line = (String) iterator.next();

            if(!line.contains("System.out.println(")&&!line.contains("page.onceDialog(dialog"))
            {
            
            if (line.contains("page.")) {
                playwrightSteps = playwrightSteps + 1;
            }
            if (playwrightSteps >= 1) {
                if (!line.contains("}")) {
                    int matchingAttribute = 0;
                    testCaseMap(getAction(line), getInput(line));
                    attributeInitialization(line);
                    if (!testCase.get("ObjectName").equals("Browser")) {
                        if (!allObjectMaping.isEmpty() && allObjectMaping.containsKey(testCase.get("ObjectName"))) {
                            HashMap<String, String> objectAttributeMap = allObjectMaping.get(testCase.get("ObjectName"));

                            Set<String> attributesKey = objectAttributeMap.keySet();
                            for (String allObjectAttributeKey : attributesKey) {
                                if (objectAttributeMap.get(allObjectAttributeKey).equals(attribute.get(allObjectAttributeKey))) {
                                    matchingAttribute = matchingAttribute + 1;
                                }
                            }
                        }

                        if (!testCase.get("ObjectName").equals("Browser")) {
                            ObjectNameList.add(testCase.get("ObjectName"));
                            int j = 0;
                            int k = 0;
                            for (String Locator : ObjectNameList) {
                                if ((Locator.trim()).equals(testCase.get("ObjectName").trim())) {
                                    j++;
                                }
                                if (j > 1) {
                                    k = (j - 1);
                                }
                            }
                            String locatorNumber = Integer.toString(k);
                            if (attribute.size() != matchingAttribute || testCase.get("ObjectName").contains("Refactor_Object")) {
                                if (!locatorNumber.equals("0")) {
                                    testCase.put("ObjectName", testCase.get("ObjectName") + "_" + locatorNumber);
                                }
                            }

                        }
                        Map<String, String> objectAttribute = new HashMap<>();
                        Set a = attribute.keySet();
                        for (Object key : a) {
                            String newKey = key.toString();
                            objectAttribute.put(newKey, attribute.get(newKey));
                        }
                        allObjectMaping.put(testCase.get("ObjectName"), (HashMap) objectAttribute);
                        if (attribute.size() != matchingAttribute || testCase.get("ObjectName").contains("Refactor_Object")) {
                            if (!testCase.get("ObjectName").equals("Browser")) {
                                Element objectGroup = doc.createElement("ObjectGroup");
                                page.appendChild(objectGroup);
                                objectGroup.setAttribute("ref", testCase.get("ObjectName"));
                                objectFrame = doc.createElement("Object");
                                objectGroup.appendChild(objectFrame);
                                objectFrame.setAttribute("frame", testCase.get("frame"));
                                objectFrame.setAttribute("ref", testCase.get("ObjectName"));

                                for (int p = 0; p < attribute.size(); p++) {
                                    Element Property = doc.createElement("Property");
                                    objectFrame.appendChild(Property);
                                    String key = (String) attribute.keySet().toArray()[p];
                                    Property.setAttribute("ref", key);
                                    Property.setAttribute("value", attribute.get(key));
                                    String prefNumber = Integer.toString(p + 1);
                                    Property.setAttribute("pref", prefNumber);
                                }
                            }

                        }
                    }
                    attributeDeclaration();
                    if (!testCase.get("action").equals("Open")) {
                        testCase.put("step", String.valueOf(stepNumber));

                        String stepAppenderString = testCase.get("step") + "," + testCase.get("ObjectName") + "," + "" + "," + testCase.get("action") + "," + testCase.get("input") + ","
                                + testCase.get("Condition") + "," + testCase.get("pageName");
                        testCase.put("stepAppender", stepAppenderString);
                        stepBuilder.append(testCase.get("stepAppender"));
                        stepBuilder.append("\n");
                        testCase.put("input", "");
                        stepNumber++;
                    }

                    if (testCase.get("action").equals("Open")) {
                        testCase.put("step", String.valueOf(stepNumber));
                        String stepAppenderValue = testCase.get("step") + "," + testCase.get("ObjectName") + "," + "" + "," + testCase.get("action") + "," + testCase.get("input") + ","
                                + testCase.get("Condition") + "," + "";
                        testCase.put("stepAppender", stepAppenderValue);
                        stepBuilder.append(testCase.get("stepAppender"));
                        stepBuilder.append("\n");
                        testCase.put("input", "");
                        stepNumber++;
                    }
                    testCase.put("csvFileName", testCase.get("pageName"));
                    filePath.put("csvFilePath", (testScenarioName + "/" + testCase.get("csvFileName") + ".csv"));
                    File file = new File(filePath.get("csvFilePath"));
                    try (PrintWriter printWriter = new PrintWriter(file)) {
                        printWriter.write(stepBuilder.toString());
                        printWriter.flush();
                    } catch (Exception ex) {

                    }
                }
            }
        }
        }
        if (orExistFlag) {
            root.appendChild(page);
        }
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(orFilePath));
        transformer.transform(source, result);
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
        testCase.put("frame","");
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
        if (!line.contains(".navigate(")&&!line.contains("dialog.dismiss()")&&!line.contains("dialog.accept()")) {
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

            }
        }
        else
        {
        if (line.contains(".navigate(")) {
            actionType = "Open";
        }
        
        if(line.contains("dialog.accept()"))
        {
            actionType= "acceptNextAlert";
        }
         if(line.contains("dialog.dismiss()"))
        {
            actionType= "dismissNextAlert";
        }
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
                    input = "";
                    break;
                case "fill":
                    input = "@" + ((line.split("\\)\\.")[length - 1])).split("\\(")[1].split("\"")[1];
                    break;
                case "selectOption":
                    input = "@" + ((line.split("\\)\\.")[length - 1])).split("\\(")[1].split("\"")[1];
                    break;
                case "check":
                    input = "";
                    break;
            }
        }
        if (line.contains(".navigate(")) {
            input = "@" + line.split("\\.navigate\\(\"")[1].split("\"")[0];
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
            }           
            if(line.contains("frameLocator("))
            {
                        String frame= line.split("\"\\)\\.")[0].split("frameLocator\\(\"")[1];
                        testCase.put("frame", frame.replace("\\", ""));
                        testCase.put("ObjectName", "Refactor_Object");
                        stringLine=line.split("]\"\\)")[1];
            }
            if (!chainAttributeExist(stringLine)) {
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
                        testCase.put("ObjectName",placeholder);
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
                    if(!line.contains("frameLocator"))
                    {
                                if (testCase.get("ObjectName").equals("Refactor_Object") || testCase.get("ObjectName").equals("") && !testCase.get("ObjectName").equals("Browser")) {
                                    chainAttributeInitialization(line);
                                }
                    }
        } catch (Exception e) {
            testCase.put("ObjectName", "Refactor_Object");
        }
    }

    public boolean chainAttributeExist(String line) {
        boolean chainAttribute = false;
        if(!line.contains("frameLocator")||!line.contains("dialog."))
        {
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
        chainLocator = chainLocator.replace("page.", "");
        attribute.put("ChainedLocator", chainLocator.trim());
    }

}
