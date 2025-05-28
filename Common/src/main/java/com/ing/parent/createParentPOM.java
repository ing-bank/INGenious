package com.ing.parent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class createParentPOM {

    public static void main(String[] args) {
        try {
            // Get target pom and source pom for properties
            String targetPath = args[0];
            String propertiesSourcePath = args[1];

            // Copy properties from main pom to target pom
            String sourcePomContent = Files.readString(Paths.get(propertiesSourcePath));
            String propertiesContent = getPropertiesContent(sourcePomContent);
            String destinationPomContent = Files.readString(Paths.get(targetPath));
            String updatedDestinationPomContent = updatePropertiesContent(destinationPomContent, propertiesContent);
            Files.write(Paths.get(targetPath), updatedDestinationPomContent.getBytes());
            System.out.println("Properties copied successfully!");

            // Create a new document for the target pom.xml file
            Document targetDocument = createTargetDocument(targetPath);

            System.out.println("TargetPath = " + targetPath);
            // Remove content under dependencies,repositories and pluginRepositories tag from target document
            removeExisting(targetDocument,"dependencies");
            removeExisting(targetDocument,"repositories");
            removeExisting(targetDocument,"pluginRepositories");

            // Copy dependencies from modules pom to target pom
            for (int i = 2; i < args.length; i++) {
                String sourcePath = args[i];
                System.out.println("------------" + sourcePath);
                Document sourceDocument = createSourceDocument(sourcePath);

                // Copy repositories and pluginRepositories
                if (args[i].contains("Engine")) {
                    copyChilds(sourceDocument, targetDocument,"repositories");
                    //removeDuplicates(targetDocument,"repositories","repository");
                    copyChilds(sourceDocument, targetDocument,"pluginRepositories");
                    //removeDuplicates(targetDocument,"pluginRepositories","pluginRepository");
                }
                // Copy dependencies
                copyChilds(sourceDocument, targetDocument,"dependencies");
                removeDuplicates(targetDocument,"dependencies","dependency");

                // Remove engine artifact
                removeEngineArtifact(targetDocument);

                saveXML(targetDocument, new File(targetPath));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getPropertiesContent(String pomContent) {
        String[] pomSections = pomContent.split("<properties>|</properties>");
        return pomSections[1];
    }

    private static String updatePropertiesContent(String destinationPomContent, String propertiesContent) {
        String[] pomSections = destinationPomContent.split("<properties>|</properties>");
        return destinationPomContent.replace(pomSections[1], propertiesContent);
    }

    private static Document createTargetDocument(String targetPath) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder document = dbFactory.newDocumentBuilder();
        File targetPomPath = new File(targetPath);
        return document.parse(targetPomPath);
    }

    private static Document createSourceDocument(String sourcePath) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder document = dbFactory.newDocumentBuilder();
        File sourcePomPath = new File(sourcePath);
        return document.parse(sourcePomPath);
    }

    private static void removeExisting(Document targetDocument,String tagName) {
        NodeList dependencyList = targetDocument.getElementsByTagName(tagName);
        if (dependencyList.getLength() > 0) {
            Node dependenciesNode = dependencyList.item(0);
            while (dependenciesNode.hasChildNodes()) {
                dependenciesNode.removeChild(dependenciesNode.getFirstChild());
            }
        }
    }

    private static void copyChilds(Document sourceDocument, Document targetDocument,String tagName) {
        Node sourceNode = getNodes(sourceDocument,tagName);
        Node targetNode = getNodes(targetDocument,tagName);
        if (sourceNode != null && targetNode != null) {
            copyNodes(sourceNode, targetNode, targetDocument);
        }
    }

    private static void copyNodes(Node sourceNode, Node targetNode, Document targetDocument) {
        NodeList dependenciesList = sourceNode.getChildNodes();
        for (int j = 0; j < dependenciesList.getLength(); j++) {
            Node dependency = dependenciesList.item(j);
            Node importedNode = targetDocument.importNode(dependency, true);
            targetNode.appendChild(importedNode);
        }
    }

    private static void removeDuplicates(Document targetDocument, String tagName, String childTagName) {
        Node dependenciesNode = getNodes(targetDocument,tagName);
        if (dependenciesNode != null) {
            removeDuplicateNodes(dependenciesNode, childTagName);
            System.out.println("Duplicate removed successfully");
        } else {
            System.out.println("No section found.");
        }
    }

    private static void removeDuplicateNodes(Node parentNode, String nodeName) {
        Set<String> uniqueKeys = new HashSet<>();
        NodeList nodeList = parentNode.getChildNodes();
        for (int i = nodeList.getLength() - 1; i >= 0; i--) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName().equals(nodeName)) {
                String uniqueKey = getUniqueKey(node);
                if (uniqueKeys.contains(uniqueKey)) {
                    parentNode.removeChild(node);
                } else {
                    uniqueKeys.add(uniqueKey);
                }
            }
        }
    }

    private static String getUniqueKey(Node node) {
        StringBuilder sb = new StringBuilder();
        NodeList childNodes = node.getChildNodes();
        for (int j = 0; j < childNodes.getLength(); j++) {
            Node child = childNodes.item(j);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                sb.append(child.getNodeName()).append(":").append(child.getTextContent().trim()).append(":");
            }
        }
        return sb.toString();
    }

    private static void removeEngineArtifact(Document targetDocument) throws XPathExpressionException {
        XPathFactory xpathFactory = XPathFactory.newInstance();
        XPath xpath = xpathFactory.newXPath();
        String artifactId = "ingenious-engine";
        XPathExpression expression = xpath.compile("//dependency[artifactId='" + artifactId + "']");
        NodeList dependencyNodes = (NodeList) expression.evaluate(targetDocument, XPathConstants.NODESET);
        for (int k = 0; k < dependencyNodes.getLength(); k++) {
            Node dependencyNode = dependencyNodes.item(k);
            Node parent = dependencyNode.getParentNode();
            parent.removeChild(dependencyNode);
        }
    }

    private static Node getNodes(Document doc, String tagName) {
        NodeList nodeList = doc.getElementsByTagName(tagName);
        return nodeList.getLength() > 0 ? nodeList.item(0) : null;
    }

    private static void saveXML(Document doc, File file) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(doc);
        StreamResult streamResult = new StreamResult(file);
        transformer.transform(domSource, streamResult);
    }

}
