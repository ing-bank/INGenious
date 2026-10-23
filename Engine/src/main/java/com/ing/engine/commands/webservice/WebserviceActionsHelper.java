package com.ing.engine.commands.webservice;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.io.StringReader;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Shared helper methods for XML/XPath and JSON/JsonPath handling, and for the
 * datasheet/variable reference conventions used by both {@link Webservice} and
 * {@code com.ing.engine.commands.structuredData.StructuredData} actions.
 */
public final class WebserviceActionsHelper {

    private WebserviceActionsHelper() {}

    /**
     * Parses the given XML text into a normalized {@link Document}.
     *
     * @param xml the XML text to parse
     * @return the parsed and normalized document
     */
    public static Document parseXmlDocument(String xml)
        throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        InputSource inputSource = new InputSource();
        inputSource.setCharacterStream(new StringReader(xml));
        Document doc = dBuilder.parse(inputSource);
        doc.getDocumentElement().normalize();
        return doc;
    }

    /**
     * Evaluates an XPath expression against the given XML text and returns the matching node list.
     *
     * @param xml the XML text to evaluate the expression against
     * @param expression the XPath expression
     * @return the matching node list
     */
    public static NodeList evaluateXPathNodeList(String xml, String expression)
        throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        Document doc = parseXmlDocument(xml);
        XPath xPath = XPathFactory.newInstance().newXPath();
        return (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
    }

    /**
     * Evaluates an XPath expression against the given XML text and returns the text of the
     * first matching node.
     *
     * @param xml the XML text to evaluate the expression against
     * @param expression the XPath expression
     * @return the text of the first matching node, or an empty string when there is no match
     */
    public static String evaluateXPathValue(String xml, String expression)
        throws ParserConfigurationException, IOException, SAXException, XPathExpressionException {
        NodeList nodeList = evaluateXPathNodeList(xml, expression);
        if (nodeList == null || nodeList.getLength() == 0) {
            return "";
        }
        return extractXmlNodeText(nodeList.item(0));
    }

    /**
     * Extracts the text content of a DOM node selected by an XPath expression.
     * <p>
     * {@link Node#getNodeValue()} returns {@code null} for element nodes (it is only
     * meaningful for attribute, text, CDATA, PI and comment nodes), so it cannot be
     * used unconditionally on the result of an XPath that targets an element. This
     * helper falls back to {@link Node#getTextContent()} when {@code getNodeValue()}
     * is null and returns an empty string when the node itself is null (e.g. the
     * XPath matched no nodes).
     *
     * @param node the DOM node to read text from; may be {@code null}
     * @return the node's text content, never {@code null}
     */
    public static String extractXmlNodeText(Node node) {
        if (node == null) {
            return "";
        }
        String value = node.getNodeValue();
        if (value == null) {
            value = node.getTextContent();
        }
        return value == null ? "" : value;
    }

    /**
     * Calculates the count of JSON elements matched by a JsonPath expression.
     * <p>
     * Handles different JSON types (objects, arrays, primitives) and returns the
     * appropriate count.
     *
     * @param responseBody the JSON response body to evaluate
     * @param jsonpath the JsonPath expression to count elements for
     * @return the count of elements matched by the JsonPath expression
     * @throws ParseException if JSON parsing fails
     */
    public static int getJsonElementCount(String responseBody, String jsonpath)
        throws ParseException {
        int actualObjectCount = 0;
        JSONParser parser = new JSONParser();
        JSONObject json = (JSONObject) parser.parse(responseBody);

        try {
            Map<String, String> objectMap = JsonPath.read(json, jsonpath);
            actualObjectCount = objectMap.keySet().size();
        } catch (Exception ex) {
            try {
                JSONArray objectMap = JsonPath.read(json, jsonpath);
                actualObjectCount = objectMap.size();
            } catch (Exception ex1) {
                try {
                    net.minidev.json.JSONArray objectMap = JsonPath.read(json, jsonpath);
                    actualObjectCount = objectMap.size();
                } catch (Exception ex2) {
                    String objectMap = JsonPath.read(json, jsonpath);
                    actualObjectCount = 1;
                }
            }
        }
        return actualObjectCount;
    }

    /**
     * Checks whether the given reference follows the {@code sheetName:columnName} datasheet
     * reference format.
     *
     * @param reference the reference to validate
     * @return {@code true} if the reference matches the {@code sheetName:columnName} format
     */
    public static boolean isValidDataSheetReference(String reference) {
        return reference != null && reference.matches(".*:.*");
    }

    /**
     * Splits a {@code sheetName:columnName} datasheet reference into its parts.
     *
     * @param reference the reference to split, must match {@code sheetName:columnName}
     * @return a two element array containing the sheet name and the column name
     */
    public static String[] splitSheetAndColumn(String reference) {
        return reference.split(":", 2);
    }

    /**
     * Checks whether the given name follows the {@code %variableName%} variable format.
     *
     * @param variableName the variable name to validate
     * @return {@code true} if the name matches the {@code %variableName%} format
     */
    public static boolean isValidVariableFormat(String variableName) {
        return variableName != null && variableName.matches("%.*%");
    }
}
