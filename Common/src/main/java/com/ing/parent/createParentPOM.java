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

            // Remove content under dependencies and Repositories tag from target document
            removeExistingDependencies(targetDocument);
            removeExistingRepositories(targetDocument);
            removeExistingPluginRepositories(targetDocument);
 

            // Copy dependencies from modules pom to target pom
            for (int i = 2; i < args.length; i++) {
                String sourcePath = args[i];
                System.out.println("------------"+sourcePath);
                Document sourceDocument = createSourceDocument(sourcePath);

                // Copy repositories and pluginRepositories
                if (args[i].contains("Engine")) {
                    //copyRepositories(sourceDocument, targetDocument);
                    //removeDuplicateRepositories(targetDocument);
                    copyPluginRepositories(sourceDocument,targetDocument);
                    removeDuplicatePluginRepositories(targetDocument);
                }

                // Copy dependencies
                copyDependencies(sourceDocument, targetDocument);
                removeDuplicateDependencies(targetDocument);

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

    private static void removeExistingDependencies(Document targetDocument) {
        NodeList dependencyList = targetDocument.getElementsByTagName("dependencies");
        if (dependencyList.getLength() > 0) {
            Node dependenciesNode = dependencyList.item(0);
            while (dependenciesNode.hasChildNodes()) {
                dependenciesNode.removeChild(dependenciesNode.getFirstChild());
            }
        }
    }
    
    private static void copyPluginRepositories(Document sourceDocument, Document targetDocument) {
        Node sourcePluginRepositoriesNode = getPluginRepositoriesNode(sourceDocument);
        Node targetPluginRepositoriesNode = getPluginRepositoriesNode(targetDocument);
        if (sourcePluginRepositoriesNode != null && targetPluginRepositoriesNode != null) {
            copyNodes(sourcePluginRepositoriesNode, targetPluginRepositoriesNode, targetDocument);
        }
    }

    private static void copyRepositories(Document sourceDocument, Document targetDocument) {
        Node sourceRepositoriesNode = getRepositoriesNode(sourceDocument);
        Node targetRepositoriesNode = getRepositoriesNode(targetDocument);
        if (sourceRepositoriesNode != null && targetRepositoriesNode != null) {
            copyNodes(sourceRepositoriesNode, targetRepositoriesNode, targetDocument);
        }
    }

    private static void copyDependencies(Document sourceDocument, Document targetDocument) {
        Node sourceDependenciesNode = getDependenciesNode(sourceDocument);
        Node targetDependenciesNode = getDependenciesNode(targetDocument);
        if (sourceDependenciesNode != null && targetDependenciesNode != null) {
            copyNodes(sourceDependenciesNode, targetDependenciesNode, targetDocument);
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

    private static void removeDuplicateDependencies(Document targetDocument) {
        Node dependenciesNode = getDependenciesNode(targetDocument);
        if (dependenciesNode != null) {
            removeDuplicateNodes(dependenciesNode, "dependency");
            System.out.println("Duplicate dependency removed successfully");
        } else {
            System.out.println("No <dependencies> section found.");
        }
    }

    private static void removeDuplicateRepositories(Document targetDocument) {
        Node repositoriesNode = getRepositoriesNode(targetDocument);
        if (repositoriesNode != null) {
            removeDuplicateNodes(repositoriesNode, "repository");
            System.out.println("Duplicate repository removed successfully.");
        } else {
            System.out.println("No <repositories> section found.");
        }
    }
    
    private static void removeDuplicatePluginRepositories(Document targetDocument) {
        Node pluginRepositoriesNode = getPluginRepositoriesNode(targetDocument);
        if (pluginRepositoriesNode != null) {
            removeDuplicateNodes(pluginRepositoriesNode, "pluginRepository");
            System.out.println("Duplicate plugin repository removed successfully.");
        } else {
            System.out.println("No <pluginRepositories> section found.");
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

    private static Node getDependenciesNode(Document doc) {
        NodeList nodeList = doc.getElementsByTagName("dependencies");
        return nodeList.getLength() > 0 ? nodeList.item(0) : null;
    }

    private static Node getRepositoriesNode(Document doc) {
        NodeList nodeList = doc.getElementsByTagName("repositories");
        return nodeList.getLength() > 0 ? nodeList.item(0) : null;
    }
    
    private static Node getPluginRepositoriesNode(Document doc) {
        NodeList nodeList = doc.getElementsByTagName("pluginRepositories");
        return nodeList.getLength() > 0 ? nodeList.item(0) : null;
    }

    private static void saveXML(Document doc, File file) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        DOMSource domSource = new DOMSource(doc);
        StreamResult streamResult = new StreamResult(file);
        transformer.transform(domSource, streamResult);
    }
    
    private static void removeExistingRepositories(Document targetDocument) {
        NodeList dependencyList = targetDocument.getElementsByTagName("repositories");
        if (dependencyList.getLength() > 0) {
            Node dependenciesNode = dependencyList.item(0);
            while (dependenciesNode.hasChildNodes()) {
                dependenciesNode.removeChild(dependenciesNode.getFirstChild());
            }
        }
    }
    private static void removeExistingPluginRepositories(Document targetDocument) {
        NodeList dependencyList = targetDocument.getElementsByTagName("pluginRepositories");
        if (dependencyList.getLength() > 0) {
            Node dependenciesNode = dependencyList.item(0);
            while (dependenciesNode.hasChildNodes()) {
                dependenciesNode.removeChild(dependenciesNode.getFirstChild());
            }
        }
    }
}