
package com.ing.datalib.component;

import com.ing.datalib.component.utils.XMLOperation;
import java.io.File;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *
 * 
 */
public class Reusable {

    private String executableType = "Executable";

    private String group;

    public String getExecutableType() {
        return executableType;
    }

    public void setExecutableType(String executableType) {
        this.executableType = executableType;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public static void parseAndSetReusable(Project sProject) {
        String xml = sProject.getLocation() + File.separator + "ReusableComponent.xml";
        if (new File(xml).exists()) {
            Document doc = XMLOperation.initTreeOp(xml);
            Element rootElement = doc.getDocumentElement();
            loadFolders(rootElement, sProject);
        }
    }

    private static void loadFolders(Element root, Project sProject) {
        NodeList folderList = root.getElementsByTagName("Folder");
        if (folderList.getLength() > 0) {
            for (int j = 0; j < folderList.getLength(); j++) {
                Node folderNode = folderList.item(j);
                if (Node.ELEMENT_NODE == folderNode.getNodeType()) {
                    loadScenarios((Element) folderNode, sProject);
                }
            }
        } else {
            loadScenarios(root, sProject);
        }
    }

    private static void loadScenarios(Element folder, Project sProject) {
        NodeList scenarioList = folder.getElementsByTagName("Scenario");
        if (scenarioList.getLength() > 0) {
            String folderName = XMLOperation.getAttribute(folder, "ref");
            for (int j = 0; j < scenarioList.getLength(); j++) {
                Node scenarioNode = scenarioList.item(j);
                if (Node.ELEMENT_NODE == scenarioNode.getNodeType()) {
                    String scenarioName = XMLOperation.getAttribute(scenarioNode, "ref");
                    Scenario sScenario = sProject.getScenarioByName(scenarioName);
                    if (sScenario == null) {
                        sScenario = sProject.addScenario(scenarioName);
                    }
                    loadTestCases(folderName, (Element) scenarioNode, sScenario);
                }
            }
        }
    }

    private static void loadTestCases(String folderName, Element scenario, Scenario sScenario) {
        NodeList testCaseList = scenario.getElementsByTagName("TestCase");
        for (int j = 0; j < testCaseList.getLength(); j++) {
            Node testCaseNode = testCaseList.item(j);
            if (Node.ELEMENT_NODE == testCaseNode.getNodeType()) {
                String reusableName = XMLOperation.getAttribute(testCaseNode, "ref");
                String exeType = XMLOperation.getAttribute(testCaseNode, "exeType");
                Reusable reusable = new Reusable();
                reusable.setGroup(folderName);
                reusable.setExecutableType(exeType);
                TestCase sTestCase = sScenario.getTestCaseByName(reusableName);
                if (sTestCase == null) {
                    sTestCase = sScenario.addTestCase(reusableName);
                }
                sTestCase.setReusable(reusable);
            }
        }
    }

}
