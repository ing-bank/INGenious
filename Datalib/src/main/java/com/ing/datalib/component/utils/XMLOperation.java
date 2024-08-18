
package com.ing.datalib.component.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * 
 */
public class XMLOperation {

    private static String sanitizePathTraversal(String filepath) {
        Path p = Paths.get(filepath);
        return p.toAbsolutePath().toString();
    }
    public static Document initTreeOp() {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            return docBuilder.newDocument();
        } catch (ParserConfigurationException ex) {
            Logger.getLogger(XMLOperation.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;

    }

    public static Document initTreeOp(String xmlPath) {
        try {
            if (new File(sanitizePathTraversal(xmlPath)).exists()) {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
          
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                
                return docBuilder.parse(xmlPath);
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(XMLOperation.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static Document initTreeOpFromString(String xml) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            return docBuilder.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Logger.getLogger(XMLOperation.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public static Element getElement(String value, Element rootElement, String tag, String reference) {
        NodeList child = rootElement.getChildNodes();
        Element parentElement = null;
        int totalnodes = child.getLength();
        for (int i = 0; i < totalnodes; i++) {
            if (child.item(i).getNodeName().equalsIgnoreCase(tag) && child.item(i).getAttributes().getNamedItem(reference).getTextContent().equals(value)) {
                parentElement = (Element) child.item(i);
                return parentElement;
            }
        }
        return parentElement;
    }

    public static void finishTreeOp(Document doc, String xmlPath) {
        createFile(xmlPath);
        try (FileOutputStream fout = new FileOutputStream(xmlPath)) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(fout);
            transformer.transform(source, result);
        } catch (TransformerException | IOException ex) {
            Logger.getLogger(XMLOperation.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void createFile(String xmlPath) {
        File xml = new File(xmlPath);
        if (!xml.exists()) {
            try {
                xml.getParentFile().mkdirs();
                xml.createNewFile();
            } catch (IOException ex) {
                Logger.getLogger(XMLOperation.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public static void removeAll(Node node) {
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if (n.hasChildNodes()) {
                removeAll(n);
                node.removeChild(n);
            } else {
                node.removeChild(n);
            }
        }
    }

    public static void replaceParent(Element oldParent, Element newParent) {
        while (oldParent.hasChildNodes()) {
            newParent.appendChild(oldParent.getFirstChild());
        }
        oldParent.getParentNode().replaceChild(newParent, oldParent);
    }

    public static String getAttribute(Node node, String attribute) {
        Node attr = node.getAttributes().getNamedItem(attribute);
        return attr == null ? null : attr.getTextContent();
    }
}
