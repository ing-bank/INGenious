package com.ing.parent;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import javax.xml.XMLConstants;
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
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class createParentPOM {

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        try {
            //Get target pom and source pom for properties
            String targetPath = args[0];
            System.out.println("Target Path (Copy to)" + targetPath);
            String propertiesSourcePath = args[1];
            System.out.println("Properties Source Path (Copy to)" + propertiesSourcePath);

            //copy properties from main pom to target pom
            String sourcePomContent = Files.readString(Paths.get(propertiesSourcePath));
            String[] pomSections = sourcePomContent.split("<properties>|</properties>");
            String propertiesContent = pomSections[1];
            String destinationPomContent = Files.readString(Paths.get(targetPath));
            pomSections = destinationPomContent.split("<properties>|</properties>");
            String updatedDestinationPomContent = destinationPomContent.replace(pomSections[1], propertiesContent);
            Files.write(Paths.get(targetPath), updatedDestinationPomContent.getBytes());
            System.out.println("Properties copied successfully!");
            String targetPomPath = targetPath;

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder document = dbFactory.newDocumentBuilder();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            //copy dependencies from modules pom to target pom
            for (int i = 2; i < args.length; i++) {

                String sourcePath = args[i];
                System.out.println("Source Path (Copy from)" + sourcePath);
                String sourcePomPath = sourcePath;

                // Load source pom.xml file
                // DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                //DocumentBuilder document = dbFactory.newDocumentBuilder();
                //TransformerFactory transformerFactory = TransformerFactory.newInstance();
                //Transformer transformer = transformerFactory.newTransformer();
                //Formatting of XML
                Document doc = document.parse(new File(sourcePomPath));
                NodeList dependencyList = doc.getElementsByTagName("dependency");

                // Create a new document for the target pom.xml file
                Document targetDocument = document.parse(new File(targetPomPath));
                Element dependenciesTag = (Element) targetDocument.getElementsByTagName("dependencies").item(0);

                // Copy specific tags from source to target document
                for (int j = 0; j < dependencyList.getLength(); j++) {
                    Node dependency = dependencyList.item(j);
                    Node copiedNode = targetDocument.importNode(dependency, true);
                    dependenciesTag.appendChild(copiedNode);
                }

                // Write the target pom.xml document to file
                transformer.transform(new DOMSource(targetDocument), new StreamResult(new File(targetPomPath)));

                System.out.println("Tags copied from " + sourcePath + " to " + targetPath);
            }
            //Remove Duplicate dependency 
            Document finalPom = document.parse(new File(targetPomPath));
            Element finalDependenciesTag = (Element) finalPom.getElementsByTagName("dependencies").item(0);
            NodeList finalDependencyList = finalDependenciesTag.getElementsByTagName("dependency");
            HashSet<String> uniqueEntries = new HashSet<>();
            for (int k = finalDependencyList.getLength() - 1; k >= 0; k--) {
                Element dependency = (Element) finalDependencyList.item(k);
                String entry = "";
                String groupId = dependency.getElementsByTagName("groupId").item(0).getTextContent();
                String artifactId = dependency.getElementsByTagName("artifactId").item(0).getTextContent();
                try{
                    String classifier = dependency.getElementsByTagName("classifier").item(0).getTextContent(); 
                    entry = groupId + ":" + artifactId + ":" + classifier;
                }
                catch(NullPointerException e)
                {
                     entry = groupId + ":" + artifactId;
                }               
                // If the entry already exists, remove the duplicate dependency
                if (uniqueEntries.contains(entry)) {
                    finalDependenciesTag.removeChild(dependency);
                } else {
                    uniqueEntries.add(entry);
                }
            }
            System.out.println("Duplicate entries removed successfully.");
            //Removing engine artifact
            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();
            String artifactId = "ingenious-engine";

            XPathExpression expression = xpath.compile("//dependency[artifactId='" + artifactId + "']");
            NodeList dependencyNodes = (NodeList) expression.evaluate(finalPom, XPathConstants.NODESET);
            for (int i = 0; i < dependencyNodes.getLength(); i++) {
                Node dependencyNode = dependencyNodes.item(i);
                Node parent = dependencyNode.getParentNode();
                parent.removeChild(dependencyNode);
            }
            // Save the modified POM XML file
            transformer.transform(new DOMSource(finalPom), new StreamResult(new File(targetPomPath)));
            System.out.println("Removed Engine artifact");

        } catch (ParserConfigurationException | SAXException | IOException | TransformerException e) {
            e.printStackTrace();
        }
    }
}
