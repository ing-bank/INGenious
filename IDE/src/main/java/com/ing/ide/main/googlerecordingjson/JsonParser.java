package com.ing.ide.main.googlerecordingjson;

import com.ing.ide.main.mainui.AppMainFrame;
import java.io.File;
import java.io.FileReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.simple.parser.*;
import java.io.*;
import java.util.*;
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
import org.json.simple.*;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class JsonParser {

    private final AppMainFrame sMainFrame;
    Map<String, String> testCase = new HashMap<>();
    Map<String, String> locator = new LinkedHashMap<>();
    Map<String, String> attributeMap = new HashMap<>();
    Map<String, String> filePath = new HashMap<>();
    List<String> LocatorNameList = new ArrayList<>();
    JSONParser parser = new JSONParser();
    Iterator<Map.Entry> stepIterator;

    public JsonParser(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
    }

    public void parse(File file) {
        if (file != null && file.exists()) {
            try {
                filePath.put("projectPath", sMainFrame.getProject().getLocation());
                filePath.put("importJsonFilePath", file.getAbsolutePath());
                File JSONfile = new File(filePath.get("importJsonFilePath"));
                String baseName = FilenameUtils.getBaseName(filePath.get("importJsonFilePath"));
                testCase.put("fileName", StringUtils.capitalize(baseName));
                Object obj = parser.parse(new FileReader(JSONfile));
                JSONObject jsonObject = (JSONObject) obj;
                JSONArray steps = (JSONArray) jsonObject.get("steps");
                Iterator iterator = steps.iterator();
                Element objectFrame = null;
                testCase.put("pageName", testCase.get("fileName"));
                filePath.put("orFilePath", (filePath.get("projectPath") + "/OR.object"));
                File OrFile = new File(filePath.get("orFilePath"));
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
                    executeParse(iterator, objectFrame, testCase.get("testScenarioName"), filePath.get("orFilePath"), page, doc, orExistFlag, rootElement);
                } else {
                    boolean orExistFlag = true;
                    DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
                    Document doc = documentBuilder.parse(new File(filePath.get("orFilePath")));
                    doc.getDocumentElement().normalize();
                    Element root = doc.getDocumentElement();
                    Element page = doc.createElement("Page");
                    executeParse(iterator, objectFrame, testCase.get("testScenarioName"), filePath.get("orFilePath"), page, doc, orExistFlag, root);
                }
                testCase.clear();
                locator.clear();
                attributeMap.clear();
                filePath.clear();
                LocatorNameList.clear();
            } catch (IOException | ParserConfigurationException | ParseException | DOMException | SAXException ex) {
                Logger.getLogger(JsonParser.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private void executeParse(Iterator iterator, Element objectFrame, String testScenarioName, String orFilePath, Element page, Document doc, boolean orExistFlag, Element root) {

        StringBuilder stepBuilder = new StringBuilder();
        testCaseParameter();
        locatorDeclaration();
        int stepNumber = 1;
        stepBuilder.append("Step" + "," + "ObjectName" + "," + "Description" + "," + "Action" + "," + "Input" + ","
                + "Condition" + "," + "Reference");
        stepBuilder.append("\n");
        try {
            page.setAttribute("ref", testCase.get("pageName"));
            page.setAttribute("title", "");
            while (iterator.hasNext()) {
                stepIterator = ((Map) iterator.next()).entrySet().iterator();
                attributeCreation();
                getActionName();
                if (testCase.get("actionName").equalsIgnoreCase("click") || testCase.get("actionName").equalsIgnoreCase("change") || testCase.get("actionName").equalsIgnoreCase("select")) {
                    String selector = attributeMap.get("selectors");
                    if (selector.contains(",")) {
                        String[] selectorPart = selector.split(",");
                        for (int i = 0; i < selectorPart.length; i++) {
                            if (selectorPart[i].contains("xpath") && selectorPart[i].contains("@id")) {
                                locator.put("id", (selectorPart[i]).split("\"")[2].replace("\\", "").trim());
                                testCase.put("LocatorName", StringUtils.capitalize(locator.get("id")));
                            }
                            if (selectorPart[i].contains("xpath") && selectorPart[i].contains("@") && !selectorPart[i].contains("]\\")) {
                                locator.put("relative_xpath", selectorPart[i].replace("xpath", "").replace("\\", "").replace("/", "")
                                        .replace("[\"", "//").replace("]", "").replace("\"", "'").replace("''", "']")
                                        .trim());
                                if (testCase.get("action").equals("selectByValue")) {
                                    locator.put("relative_xpath", locator.get("relative_xpath").replace("*", "select"));
                                }
                                locator.put("xpath", locator.get("xpath"));
                            } else if (selectorPart[i].contains("xpath") && selectorPart[i].contains("@") && selectorPart[i].contains("]\\")) {
                                String xpathValue1 = selectorPart[i].split("]", 2)[0].replace("\\", "").replace("xpath", "").replace("/", "").replace("[\"", "//").replace("=\"", "='").replace("\"", "']");
                                String xpathValue2 = selectorPart[i].split("]", 2)[1].replace("\\", "").replace("\"]", "");
                                locator.put("xpath", xpathValue1 + xpathValue2);
                                locator.put("id", "");
                                if (testCase.get("action").equals("selectByValue")) {
                                    locator.put("xpath", locator.get("xpath").replace("*", "select"));
                                }
                                if(locator.get("xpath").endsWith("/a]"))
                                    locator.put("xpath", locator.get("xpath").substring(0,locator.get("xpath").length()-1));
                                locator.put("xpath", locator.get("xpath"));
                            } else if (selectorPart[i].contains("data-test")) {
                                if (selectorPart[i].contains("=")) {
                                    testCase.put("Object", selectorPart[i].split("=")[1].replace("]", "").replace("'", "")
                                            .replace("\"", "").trim());
                                    locator.put("css", selectorPart[i].replace("[\"", "").replace("]\"", "")
                                            .replace("[[", "[").trim());
                                    testCase.put("LocatorName", StringUtils.capitalize(testCase.get("Object")));
                                    locator.put("css", locator.get("css"));
                                }
                            } else if (selectorPart[i].contains("text/")) {
                                String text1 = selectorPart[i].replace("\\/", "=").replace("[", "").replace("]", "")
                                        .replace("\"", "");
                                locator.put("link_text", text1.split("=")[1]);
                                testCase.put("LocatorName", StringUtils.capitalize(locator.get("link_text")));
                                locator.put("link_text", locator.get("link_text"));
                            }
                            int j = 0;
                            if (i == (selectorPart.length - 1)) {
                                int k = 0;
                                LocatorNameList.add(testCase.get("LocatorName"));

                                for (String Locator : LocatorNameList) {
                                    if ((Locator.trim()).equals(testCase.get("LocatorName").trim())) {
                                        j++;
                                    }

                                    if (j > 1) {
                                        k = (j - 1);
                                    }

                                }
                                String locatorNumber = Integer.toString(k);
                                if (!locatorNumber.equals("0")) {
                                    testCase.put("LocatorName", testCase.get("LocatorName") + "_" + locatorNumber);
                                }
                                Element objectGroup = doc.createElement("ObjectGroup");
                                page.appendChild(objectGroup);
                                objectGroup.setAttribute("ref", testCase.get("LocatorName"));
                                objectFrame = doc.createElement("Object");
                                objectGroup.appendChild(objectFrame);
                                objectFrame.setAttribute("frame", "");
                                objectFrame.setAttribute("ref", testCase.get("LocatorName"));
                            }
                        }
                        for (int k = 0; k < locator.size(); k++) {
                            Element Property = doc.createElement("Property");
                            objectFrame.appendChild(Property);
                            String key = (String) locator.keySet().toArray()[k];
                            Property.setAttribute("ref", key);
                            Property.setAttribute("value", locator.get(key));
                            String prefNumber = Integer.toString(k + 1);
                            Property.setAttribute("pref", prefNumber);

                        }
                        locatorDeclaration();
                    }
                    testCase.put("step", String.valueOf(stepNumber));
                    String stepAppenderString = testCase.get("step") + "," + testCase.get("LocatorName") + "," + "" + "," + testCase.get("action") + "," + testCase.get("input") + ","
                            + testCase.get("Condition") + "," + testCase.get("pageName");
                    testCase.put("stepAppender", stepAppenderString);
                    stepBuilder.append(testCase.get("stepAppender"));
                    stepBuilder.append("\n");
                    testCase.put("input", "");
                    stepNumber++;
                }
                if (!attributeMap.get("type").equals("setViewport")) {
                    if (testCase.get("action").equals("Open") || testCase.get("action").equals("createAndSwitchToWindow")) {
                        testCase.put("step", String.valueOf(stepNumber));
                        String stepAppenderValue = testCase.get("step") + "," + testCase.get("LocatorName") + "," + "" + "," + testCase.get("action") + "," + testCase.get("input") + ","
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
                        Logger.getLogger(JsonParser.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                attributeMap.clear();
            }
            if (orExistFlag) {
                root.appendChild(page);
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(new File(orFilePath));
            transformer.transform(source, result);

        } catch (TransformerException | DOMException ex) {
            Logger.getLogger(JsonParser.class.getName()).log(Level.SEVERE, null, ex);
        }
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
        testCase.put("LocatorName", "");
    }

    public void locatorDeclaration() {
        locator.put("relative_xpath", "");
        locator.put("xpath", "");
        locator.put("id", "");
        locator.put("link_text", "");
        locator.put("css", "");
        locator.put("NLP_locator", "");
        locator.put("name", "");
        locator.put("class", "");
        locator.put("type", "");
        locator.put("outerHTML", "");
        locator.put("tagName", "");

    }

    public void attributeCreation() {
        while (stepIterator.hasNext()) {
            Map.Entry pair = stepIterator.next();
            String key = pair.getKey().toString();
            String value = pair.getValue().toString();
            attributeMap.put(key, value);
        }
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

    public void getActionName() {
        if (!attributeMap.get("type").equals("navigate") && !attributeMap.get("type").equals("keyUp") && attributeMap.containsKey("selectors") && attributeMap.containsKey("value") && attributeMap.get("selectors").toUpperCase().contains("SELECT")) {
            testCase.put("actionName", "select");
        } else {
            testCase.put("actionName", attributeMap.get("type"));
        }
        switch (testCase.get("actionName")) {
            case "navigate":
                if (attributeMap.get("url").contains("chrome://new-tab-page/")) {
                    testCase.put("action", "createAndSwitchToWindow");
                } else {
                    testCase.put("action", "Open");
                    testCase.put("input", "@" + attributeMap.get("url"));
                }
                testCase.put("LocatorName", "Browser");
                break;
            case "click":
                testCase.put("action", "Click");
                break;
            case "change":
                testCase.put("action", "Set");
                testCase.put("input", "@" + attributeMap.get("value"));
                break;
            case "select":
                testCase.put("action", "selectByValue");
                testCase.put("input", "@" + attributeMap.get("value"));
                break;
        }
    }
}
