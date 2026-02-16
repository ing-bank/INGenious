package com.ing.ide.main.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Utility class for formatting/beautifying SQL, JSON, and XML code.
 */
public class CodeFormatter {

    private static final String INDENT = "    "; // 4 spaces

    /**
     * Beautifies JSON string with proper indentation.
     * 
     * @param json The JSON string to format
     * @return Formatted JSON string
     */
    public static String beautifyJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return json;
        }
        
        try {
            StringBuilder result = new StringBuilder();
            int indentLevel = 0;
            boolean inString = false;
            boolean escaped = false;
            char prevChar = 0;
            
            String trimmed = json.trim();
            
            for (int i = 0; i < trimmed.length(); i++) {
                char c = trimmed.charAt(i);
                
                if (escaped) {
                    result.append(c);
                    escaped = false;
                    continue;
                }
                
                if (c == '\\') {
                    result.append(c);
                    escaped = true;
                    continue;
                }
                
                if (c == '"' && !escaped) {
                    inString = !inString;
                    result.append(c);
                    continue;
                }
                
                if (inString) {
                    result.append(c);
                    continue;
                }
                
                switch (c) {
                    case '{':
                    case '[':
                        result.append(c);
                        // Check if next non-whitespace is closing bracket
                        int nextNonWs = findNextNonWhitespace(trimmed, i + 1);
                        if (nextNonWs < trimmed.length() && 
                            ((c == '{' && trimmed.charAt(nextNonWs) == '}') ||
                             (c == '[' && trimmed.charAt(nextNonWs) == ']'))) {
                            // Empty object/array, don't add newline
                        } else {
                            indentLevel++;
                            result.append('\n').append(getIndent(indentLevel));
                        }
                        break;
                        
                    case '}':
                    case ']':
                        // Check if previous non-whitespace was opening bracket
                        if (prevChar != '{' && prevChar != '[') {
                            indentLevel = Math.max(0, indentLevel - 1);
                            result.append('\n').append(getIndent(indentLevel));
                        }
                        result.append(c);
                        break;
                        
                    case ',':
                        result.append(c);
                        result.append('\n').append(getIndent(indentLevel));
                        break;
                        
                    case ':':
                        result.append(c).append(' ');
                        break;
                        
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                        // Skip whitespace outside strings
                        break;
                        
                    default:
                        result.append(c);
                }
                
                if (!Character.isWhitespace(c)) {
                    prevChar = c;
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            // If formatting fails, return original
            return json;
        }
    }
    
    private static int findNextNonWhitespace(String s, int start) {
        for (int i = start; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return s.length();
    }
    
    private static String getIndent(int level) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; i++) {
            sb.append(INDENT);
        }
        return sb.toString();
    }

    /**
     * Beautifies XML string with proper indentation.
     * 
     * @param xml The XML string to format
     * @return Formatted XML string
     */
    public static String beautifyXml(String xml) {
        if (xml == null || xml.trim().isEmpty()) {
            return xml;
        }
        
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml.trim())));
            
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 4);
            
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, 
                xml.trim().startsWith("<?xml") ? "no" : "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));
            
            return writer.toString().trim();
        } catch (Exception e) {
            // If XML parsing fails, try simple regex-based formatting
            return beautifyXmlSimple(xml);
        }
    }
    
    /**
     * Simple XML beautifier using regex (fallback when DOM parsing fails).
     */
    private static String beautifyXmlSimple(String xml) {
        try {
            StringBuilder result = new StringBuilder();
            int indentLevel = 0;
            
            // Split by tags
            String[] tokens = xml.replaceAll(">\\s*<", ">\n<").split("\n");
            
            for (String token : tokens) {
                String trimmed = token.trim();
                if (trimmed.isEmpty()) continue;
                
                // Decrease indent for closing tags
                if (trimmed.startsWith("</")) {
                    indentLevel = Math.max(0, indentLevel - 1);
                }
                
                result.append(getIndent(indentLevel)).append(trimmed).append('\n');
                
                // Increase indent for opening tags (not self-closing)
                if (trimmed.startsWith("<") && !trimmed.startsWith("</") && 
                    !trimmed.startsWith("<?") && !trimmed.startsWith("<!") &&
                    !trimmed.endsWith("/>") && !trimmed.contains("</")) {
                    indentLevel++;
                }
            }
            
            return result.toString().trim();
        } catch (Exception e) {
            return xml;
        }
    }

    /**
     * Beautifies SQL query with proper indentation and line breaks.
     * 
     * @param sql The SQL string to format
     * @return Formatted SQL string
     */
    public static String beautifySql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }
        
        try {
            String result = sql.trim();
            
            // Normalize whitespace
            result = result.replaceAll("\\s+", " ");
            
            // SQL keywords to put on new lines (uppercase)
            String[] majorKeywords = {
                "SELECT", "FROM", "WHERE", "AND", "OR", "ORDER BY", "GROUP BY", 
                "HAVING", "LIMIT", "OFFSET", "JOIN", "INNER JOIN", "LEFT JOIN", 
                "RIGHT JOIN", "OUTER JOIN", "LEFT OUTER JOIN", "RIGHT OUTER JOIN",
                "FULL OUTER JOIN", "CROSS JOIN", "ON", "UNION", "UNION ALL",
                "INSERT INTO", "VALUES", "UPDATE", "SET", "DELETE FROM",
                "CREATE TABLE", "ALTER TABLE", "DROP TABLE", "CREATE INDEX",
                "WITH", "AS", "CASE", "WHEN", "THEN", "ELSE", "END"
            };
            
            // Add line breaks before major keywords
            for (String keyword : majorKeywords) {
                // Case-insensitive replacement with newline
                Pattern pattern = Pattern.compile("(?i)\\s+" + Pattern.quote(keyword) + "\\s+");
                Matcher matcher = pattern.matcher(result);
                result = matcher.replaceAll("\n" + keyword.toUpperCase() + " ");
            }
            
            // Handle commas in SELECT - put each column on new line
            result = formatSelectColumns(result);
            
            // Indent subqueries
            result = indentSubqueries(result);
            
            // Clean up multiple newlines
            result = result.replaceAll("\n{3,}", "\n\n");
            result = result.replaceAll("(?m)^[ \t]+$", "");
            
            return result.trim();
        } catch (Exception e) {
            return sql;
        }
    }
    
    private static String formatSelectColumns(String sql) {
        try {
            // Find SELECT ... FROM blocks and format columns
            Pattern selectPattern = Pattern.compile(
                "(?i)(SELECT\\s+)(.*?)(\\s+FROM\\s+)", 
                Pattern.DOTALL
            );
            
            Matcher matcher = selectPattern.matcher(sql);
            StringBuffer sb = new StringBuffer();
            
            while (matcher.find()) {
                String select = matcher.group(1);
                String columns = matcher.group(2);
                String from = matcher.group(3);
                
                // Split columns by comma and format
                if (columns.contains(",")) {
                    String[] cols = columns.split(",");
                    StringBuilder formattedCols = new StringBuilder();
                    for (int i = 0; i < cols.length; i++) {
                        if (i > 0) {
                            formattedCols.append(",\n       ");
                        }
                        formattedCols.append(cols[i].trim());
                    }
                    columns = formattedCols.toString();
                }
                
                matcher.appendReplacement(sb, 
                    Matcher.quoteReplacement(select + columns + from));
            }
            matcher.appendTail(sb);
            
            return sb.toString();
        } catch (Exception e) {
            return sql;
        }
    }
    
    private static String indentSubqueries(String sql) {
        try {
            StringBuilder result = new StringBuilder();
            int indentLevel = 0;
            boolean inString = false;
            char stringChar = 0;
            
            for (int i = 0; i < sql.length(); i++) {
                char c = sql.charAt(i);
                
                // Handle string literals
                if ((c == '\'' || c == '"') && (i == 0 || sql.charAt(i - 1) != '\\')) {
                    if (!inString) {
                        inString = true;
                        stringChar = c;
                    } else if (c == stringChar) {
                        inString = false;
                    }
                }
                
                if (!inString) {
                    if (c == '(') {
                        result.append(c);
                        // Check if this is a subquery (SELECT after parenthesis)
                        String remaining = sql.substring(i + 1).trim().toUpperCase();
                        if (remaining.startsWith("SELECT")) {
                            indentLevel++;
                            result.append("\n").append(getIndent(indentLevel));
                        }
                    } else if (c == ')') {
                        // Check if we should decrease indent
                        String before = result.toString().trim();
                        if (before.endsWith("\n" + getIndent(indentLevel).trim()) || 
                            indentLevel > 0) {
                            indentLevel = Math.max(0, indentLevel - 1);
                            if (result.toString().endsWith("\n" + getIndent(indentLevel + 1))) {
                                // Remove trailing indent before )
                                int lastNewline = result.lastIndexOf("\n");
                                if (lastNewline >= 0) {
                                    result.setLength(lastNewline);
                                    result.append("\n").append(getIndent(indentLevel));
                                }
                            }
                        }
                        result.append(c);
                    } else if (c == '\n') {
                        result.append(c);
                        // Add proper indentation after newline
                        int j = i + 1;
                        while (j < sql.length() && (sql.charAt(j) == ' ' || sql.charAt(j) == '\t')) {
                            j++;
                        }
                        if (j < sql.length() && sql.charAt(j) != '\n') {
                            result.append(getIndent(indentLevel));
                        }
                        i = j - 1;
                    } else {
                        result.append(c);
                    }
                } else {
                    result.append(c);
                }
            }
            
            return result.toString();
        } catch (Exception e) {
            return sql;
        }
    }
}
