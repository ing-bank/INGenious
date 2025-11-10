package com.ing.engine.commands.xml;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import org.w3c.dom.Node;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.*;

import org.w3c.dom.*;
import org.xmlunit.util.Predicate;

import javax.xml.parsers.*;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class XmlOperations extends General {

    public XmlOperations(CommandControl cc) {
        super(cc);
    }

    public static Diff compareXML(String xml1, String xml2, List<String> ignoreNodes, List<String> ignoreTextNodes, String parentElementName, String sortByChildElement, boolean isNumeric) throws Exception {
        System.out.println("Ignoring nodes: " + (ignoreNodes != null ? ignoreNodes : "None"));
        System.out.println("Ignoring text content in nodes: " + (ignoreTextNodes != null ? ignoreTextNodes : "None"));

        // Convert the string to bytes in UTF-8
        byte[] utf8Bytes = xml1.getBytes(StandardCharsets.UTF_8);
        byte[] utf8Bytes2 = xml2.getBytes(StandardCharsets.UTF_8);

        // Create a new string with UTF-8 charset
        xml1 = new String(utf8Bytes, StandardCharsets.UTF_8);
        xml2 = new String(utf8Bytes2, StandardCharsets.UTF_8);

        System.out.println("\n\nPROVIDED\n\n");
        System.out.println("provided xml1: \n" + xml1 + "\n\n");
        System.out.println("provided xml2: \n" + xml2 + "\n\n");

        // Parse and sort XML documents
//        Document doc1 = parentElementName != null ? parseAndSortXML(xml1, parentElementName, sortByChildElement, isNumeric) : null;
//        Document doc2 = parentElementName != null ? parseAndSortXML(xml2, parentElementName, sortByChildElement, isNumeric) : null;

        Document doc1;
        Document doc2;

        if (parentElementName != null && sortByChildElement != null) {
            doc1 = parseAndSortXML(xml1, parentElementName, sortByChildElement, isNumeric);
            doc2 = parseAndSortXML(xml2, parentElementName, sortByChildElement, isNumeric);
            xml1 = convertDocumentToString(doc1);
            xml2 = convertDocumentToString(doc2);
            System.out.println("\n\nSORTED BY CONDITION\n\n");
            System.out.println("sorted xml1: \n" + xml1 + "\n\n");
            System.out.println("sorted xml2: \n" + xml2 + "\n\n");
        } else {
            doc1 = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml1.getBytes(StandardCharsets.UTF_8)));
            doc2 = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(xml2.getBytes(StandardCharsets.UTF_8)));
        }


        xml1 = convertDocumentToString(doc1);
        xml2 = convertDocumentToString(doc2);

        System.out.println("\n\nSORTED BY CONDITION\n\n");
        System.out.println("sorted xml1: \n" + xml1 + "\n\n");
        System.out.println("sorted xml2: \n" + xml2 + "\n\n");

        return DiffBuilder.compare(org.xmlunit.builder.Input.fromString(xml1))
                .withTest(org.xmlunit.builder.Input.fromString(xml2))
                .withNodeFilter(getCustomNodeFilter(ignoreNodes))
                .withDifferenceEvaluator(getCustomDifferenceEvaluator(ignoreNodes, ignoreTextNodes))
                .ignoreWhitespace()
                .checkForSimilar()
                .build();
    }

    public static Document parseAndSortXML(String xml, String parentElementName, String sortByChildElement, boolean isNumeric) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        NodeList parentNodes = doc.getElementsByTagName(parentElementName);
        List<Element> elements = new ArrayList<>();

        for (int i = 0; i < parentNodes.getLength(); i++) {
            if (parentNodes.item(i) instanceof Element) {
                elements.add((Element) parentNodes.item(i));
            }
        }

        if (isNumeric) {
            elements.sort((e1, e2) -> {
                double amount1 = Double.parseDouble(e1.getElementsByTagName(sortByChildElement).item(0).getTextContent());
                double amount2 = Double.parseDouble(e2.getElementsByTagName(sortByChildElement).item(0).getTextContent());
                return Double.compare(amount1, amount2); // Sort in ascending order
            });
        } else {
            elements.sort(Comparator.comparing(e -> e.getElementsByTagName(sortByChildElement).item(0).getTextContent()));
        }

        Node parentNode = parentNodes.item(0).getParentNode();
        for (Element element : elements) {
            parentNode.appendChild(element);
        }

        return doc;
    }

    private static String convertDocumentToString(Document doc) throws Exception {
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.getBuffer().toString();
    }

    private static Predicate<Node> getCustomNodeFilter(List<String> ignoreNodes) {
        List<String> normalizedIgnoreNodes = normalizeXPaths(ignoreNodes);

        return node -> {
            String xpath = normalizeXPath(getXPath(node));
            if (xpath == null) {
                return true;
            }

            return normalizedIgnoreNodes == null || normalizedIgnoreNodes.stream().noneMatch(xpath::startsWith);
        };
    }

    private static String getXPath(Node node) {
        if (node == null) {
            return null;
        }

        StringBuilder path = new StringBuilder();

        while (node != null && node.getNodeType() != Node.DOCUMENT_NODE) {
            int index = getNodeIndex(node);
            String name = node.getNodeName();

            path.insert(0, "/" + name + "[" + index + "]");
            node = node.getParentNode();
        }

        return path.toString();
    }

    private static int getNodeIndex(Node node) {
        Node parent = node.getParentNode();
        if (parent == null) {
            return 1;
        }

        NodeList siblings = parent.getChildNodes();
        int count = 0;
        int index = 1;

        for (int i = 0; i < siblings.getLength(); i++) {
            Node sibling = siblings.item(i);
            if (sibling.getNodeType() == node.getNodeType() &&
                    sibling.getNodeName().equals(node.getNodeName())) {
                count++;
                if (sibling == node) {
                    index = count;
                }
            }
        }

        return index;
    }

    private static DifferenceEvaluator getCustomDifferenceEvaluator(List<String> ignoreNodes, List<String> ignoreTextNodes) {
        List<String> normalizedIgnoreNodes = normalizeXPaths(ignoreNodes);
        List<String> normalizedIgnoreTextNodes = normalizeXPaths(ignoreTextNodes);

        return (comparison, outcome) -> {
            if (outcome == ComparisonResult.EQUAL) {
                return outcome;
            }

            String xpath = normalizeXPath(comparison.getControlDetails().getXPath());
            if (xpath == null) {
                return outcome;
            }

            // Ignore entire subtree for specified nodes
            if (normalizedIgnoreNodes != null && normalizedIgnoreNodes.stream().anyMatch(xpath::startsWith)) {
                return ComparisonResult.EQUAL;
            }

            // Ignore text value differences for specified nodes
            if (normalizedIgnoreTextNodes != null &&
                    comparison.getType() == ComparisonType.TEXT_VALUE &&
                    normalizedIgnoreTextNodes.stream().anyMatch(xpath::startsWith)) {
                return ComparisonResult.EQUAL;
            }

            return outcome;
        };
    }

    private static List<String> normalizeXPaths(List<String> xpaths) {
        if (xpaths == null) {
            return null;
        }
        return xpaths.stream()
                .map(XmlOperations::normalizeXPath)
                .collect(Collectors.toList());
    }

    private static String normalizeXPath(String xpath) {
        if (xpath == null) {
            return null;
        }
        return xpath.replaceAll("\\[\\d+\\]", "").replaceAll("/text\\(\\)", "");
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.XML, desc = "Compare XML files", input = InputType.YES, condition = InputType.OPTIONAL)
    public void compareXMLFiles() {

        try {

            String[] parts = Data.split(",");

            File file = new File(parts[0]);
            StringBuilder content = new StringBuilder();
            try (Scanner scanner = new Scanner(file)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    content.append(line).append("\n");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println(content.toString());

            File file2 = new File(parts[1]);
            StringBuilder content2 = new StringBuilder();
            try (Scanner scanner = new Scanner(file2)) {
                while (scanner.hasNextLine()) {
                    String line2 = scanner.nextLine();
                    content2.append(line2).append("\n");
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            System.out.println(content2.toString());

            String parentElementName = null;
            String sortByChildElement = null;
            boolean isNumeric = false;

            if (!Condition.isEmpty()) {
                String[] sortFields = Condition.split(",");
                parentElementName = sortFields[0];
                sortByChildElement = sortFields[1];
                isNumeric = Boolean.parseBoolean(sortFields[2]);
            }

            Diff diff = compareXML(content.toString(), content2.toString(), ignoreXMLNodes.get(iterationContext), ignoreXMLTextNodes.get(iterationContext),parentElementName,sortByChildElement,isNumeric);
            boolean areEqual = !diff.hasDifferences();
            System.out.println("XML files are equal: " + areEqual);

            if (!areEqual) {
                System.out.println("Differences found:");
                StringBuilder logDetails = new StringBuilder();
                for (Difference difference : diff.getDifferences()) {
                    String diffText = "- " + difference.toString();
                    System.out.println(diffText);
                    logDetails.append(diffText).append("\n");
                }
                Report.updateTestLog(Action, "Differences detected in XML files comparison:\n" + logDetails, Status.FAIL);
            } else {
                Report.updateTestLog(Action, "XML files compared successfully", Status.DONE);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during XML files comparison", ex);
            Report.updateTestLog(Action, "Error in XML files comparison: " + "\n" + ex.getMessage(), Status.FAIL);
        }
    }

    @Action(object = ObjectType.XML, desc = "Ignore XML Node", input = InputType.YES, condition = InputType.NO)
    public void ignoreXMLNode() throws Exception {
        try {
            ignoreXMLNodes.computeIfAbsent(iterationContext, k -> new ArrayList<>()).add(Data);
            Report.updateTestLog(Action, "XML Node to ignore for comparison has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting XML Node to ignore for comparison", ex);
            Report.updateTestLog(Action, "Error in setting XML Node to ignore for comparison: " + "\n" + ex.getMessage(), Status.FAIL);
        }
    }


    @Action(object = ObjectType.XML, desc = "Ignore XML Text Node", input = InputType.YES, condition = InputType.NO)
    public void ignoreXMLTextNode() throws Exception {
        try {
            ignoreXMLTextNodes.computeIfAbsent(iterationContext, k -> new ArrayList<>()).add(Data);
            Report.updateTestLog(Action, "XML Text Node(s) to ignore for comparison has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting XML Text Node(s) to ignore for comparison", ex);
            Report.updateTestLog(Action, "Error in setting XML Text Node(s) to ignore for comparison: " + "\n" + ex.getMessage(), Status.FAIL);
        }
    }

}
