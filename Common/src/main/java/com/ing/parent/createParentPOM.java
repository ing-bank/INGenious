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

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException, TransformerException {
        try {
            //Get target pom and source pom for properties
            String targetPath = args[0];
            System.out.println("Properties Copy to" + targetPath);
            String propertiesSourcePath = args[1];
            System.out.println("Properties Copy from" + propertiesSourcePath);
            //copy properties from main pom to target pom
            String sourcePomContent = Files.readString(Paths.get(propertiesSourcePath));
            String[] pomSections = sourcePomContent.split("<properties>|</properties>");
            String propertiesContent = pomSections[1];
            String destinationPomContent = Files.readString(Paths.get(targetPath));
            pomSections = destinationPomContent.split("<properties>|</properties>");
            String updatedDestinationPomContent = destinationPomContent.replace(pomSections[1], propertiesContent);
            Files.write(Paths.get(targetPath), updatedDestinationPomContent.getBytes());
            System.out.println("Properties copied successfully!");
            File targetPomPath = new File(targetPath);

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder document = dbFactory.newDocumentBuilder();

            // Create a new document for the target pom.xml file
            Document targetDocument = document.parse(targetPomPath);
            targetDocument.getDocumentElement().normalize();

            //Remove content under dependencies tag from target document
            NodeList dependencyList = targetDocument.getElementsByTagName("dependencies");
            if (dependencyList.getLength() > 0) {
                Node dependenciesNode = dependencyList.item(0);
                while (dependenciesNode.hasChildNodes()) {
                    dependenciesNode.removeChild(dependenciesNode.getFirstChild());
                }
            }

            //copy dependencies from modules pom to target pom
            for (int i = 2; i < args.length; i++) {

                String sourcePath = args[i];
                System.out.println("Source Path (Copy from)" + sourcePath);
                File sourcePomPath = new File(sourcePath);
                Document doc = document.parse(sourcePomPath);
                doc.getDocumentElement().normalize();
                Node sourceDependenciesNode = getDependenciesNode(doc);
                Node targetDependenciesNode = getDependenciesNode(targetDocument);
                if (sourceDependenciesNode != null && targetDependenciesNode != null) {
                    NodeList dependenciesList = sourceDependenciesNode.getChildNodes();
                    for (int j = 0; j < dependenciesList.getLength(); j++) {
                        Node dependency = dependenciesList.item(j);
                        // Import the node from the source doc to the target doc and append
                        Node importedNode = targetDocument.importNode(dependency, true);
                        targetDependenciesNode.appendChild(importedNode);
                    }
                }
                //remove duplicate entries
                Node dependenciesNode = getDependenciesNode(targetDocument);
                if (dependenciesNode != null) {
                    removeDuplicateDependencies(dependenciesNode);
                    System.out.println("Duplicates removed successfully.");
                } else {
                    System.out.println("No <dependencies> section found.");
                }

                //Remove engine artifact
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
                saveXML(targetDocument, targetPomPath);
            }

        } catch (ParserConfigurationException | SAXException | IOException  e) {
            e.printStackTrace();
        }
    }

    private static Node getDependenciesNode(Document doc) {
        NodeList nodeList = doc.getElementsByTagName("dependencies");
        if (nodeList.getLength() > 0) {
            return nodeList.item(0);
        }
        return null;
    }

    private static void saveXML(Document doc, File file) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(doc);
        StreamResult streamResult = new StreamResult(file);
        transformer.transform(domSource, streamResult);
    }

    private static void removeDuplicateDependencies(Node dependenciesNode) {
        Set<String> uniqueDependencies = new HashSet<>();
        NodeList dependenciesList = dependenciesNode.getChildNodes();
        for (int i = dependenciesList.getLength() - 1; i >= 0; i--) {
            Node dependency = dependenciesList.item(i);
            if (dependency.getNodeType() == Node.ELEMENT_NODE && dependency.getNodeName().equals("dependency")) {
                // Extract the unique key for the dependency (groupId:artifactId:version)
                String uniqueKey = getDependencyKey(dependency);
                // If the key is already present, it's a duplicate, so remove the node
                if (uniqueDependencies.contains(uniqueKey)) {
                    dependenciesNode.removeChild(dependency);
                } else {
                    uniqueDependencies.add(uniqueKey);
                }
            }
        }
    }

    // Helper method to generate a unique key for a <dependency> (groupId:artifactId:version)
    private static String getDependencyKey(Node dependencyNode) {
        String groupId = "";
        String artifactId = "";
        String classifier = "";

        NodeList childNodes = dependencyNode.getChildNodes();
        for (int j = 0; j < childNodes.getLength(); j++) {
            Node child = childNodes.item(j);
            if (child.getNodeType() == Node.ELEMENT_NODE) {

                switch (child.getNodeName()) {
                    case "groupId":
                        groupId = child.getTextContent().trim();
                        break;
                    case "artifactId":
                        artifactId = child.getTextContent().trim();
                        break;
                    case "classifier":
                        classifier = child.getTextContent().trim();
                        break;
                }
            }
        }
        return groupId + ":" + artifactId + ":" + classifier;
    }
}
