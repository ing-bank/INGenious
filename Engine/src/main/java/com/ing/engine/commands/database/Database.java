package com.ing.engine.commands.database;

import com.ing.datalib.testdata.view.TestDataView;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 *
 */
public class Database extends General {

    public Database(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.DATABASE, desc = "Initiate the DB transaction", input = InputType.YES)
    public void initDBConnection() {
        try {
            String dbName = Input;
            if (dbName.startsWith("#"))
            {
                dbName = dbName.replace("#","");
                if (verifyDbConnection(dbName)) {
                    DatabaseMetaData metaData = dbconnection.getMetaData();
                    Report.updateTestLog(Action, " Connected with " + metaData.getDriverName() + "\n"
                                    + "Driver version " + metaData.getDriverVersion() + " \n"
                                    + "Database product name " + metaData.getDatabaseProductName() + "\n"
                                    + "Database product version " + metaData.getDatabaseProductVersion(),
                            Status.PASSNS);
                } else {
                    Report.updateTestLog(Action, "Could not able to make DB connection ", Status.FAILNS);
                }
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Report.updateTestLog(Action, "Error connecting Database: " + ex.getMessage(),
                    Status.FAILNS);
        }
    }

    @Action(object = ObjectType.DATABASE, desc = "Execute the Query in [<Input>]", input = InputType.YES)
    public void executeSelectQuery() {
        try {
            executeSelect();
            Report.updateTestLog(Action, "Executed Select Query", Status.DONE);
        } catch (SQLException ex) {
            Report.updateTestLog(Action, "Error executing the SQL Query: " + ex.getMessage(),
                    Status.FAILNS);
        }
    }

    @Action(object = ObjectType.DATABASE, desc = "Execute the Query in [<Input>]", input = InputType.YES)
    public void executeDMLQuery() {
        try {
            if (executeDML()) {
                Report.updateTestLog(Action, " Table updated by using " + Data, Status.PASSNS);
            } else {
                Report.updateTestLog(Action, " Table not updated by using " + Data, Status.FAILNS);
            }
        } catch (SQLException ex) {
            Report.updateTestLog(Action, "Error executing the SQL Query: " + ex.getMessage(),
                    Status.FAILNS);
        }
    }
    //TODO: Review - Renamed to assertDBResultEquals as to distinguish from other assertDB methods added next
    @Action(object = ObjectType.DATABASE, desc = "Assert the value [<Input>] exist in the column [<Condition>] ", input = InputType.YES, condition = InputType.YES)
    public void assertDBResultEquals() {
        if (assertDBEquals(Condition, Data)) {
            Report.updateTestLog(Action, "Value " + Data + " exist in the Database", Status.PASSNS);
        } else {
            Report.updateTestLog(Action, "Value " + Data + " doesn't exist in the Database", Status.FAILNS);
        }
    }

    @Action(object = ObjectType.DATABASE, desc = "Store it in Global variable from the DB column [<Condition>] ", input = InputType.YES, condition = InputType.YES)
    public void storeValueInGlobalVariable() {
        storeValue(Input, Condition, true);
        if (getVar(Input) != null && !getVar(Input).equals("")) {
            Report.updateTestLog(Action, "Stored in Global variable", Status.PASSNS);
        } else {
            Report.updateTestLog(Action, "Value doesn't stored in Global variable", Status.FAILNS);
        }
    }

    @Action(object = ObjectType.DATABASE, desc = "Store it in the variable from the DB column [<Condition>] ", input = InputType.YES, condition = InputType.YES)
    public void storeValueInVariable() {
        storeValue(Input, Condition, false);
        if (getVar(Input) != null && !getVar(Input).equals("")) {
            Report.updateTestLog(Action, "Stored in the variable", Status.PASSNS);
        } else {
            Report.updateTestLog(Action, "Value doesn't stored in Global variable", Status.FAILNS);
        }
    }

    @Action(object = ObjectType.DATABASE, desc = "Save DB value in Test Data Sheet", input = InputType.YES, condition = InputType.YES)
    public void storeDBValueinDataSheet() {
        try {
            if (Condition != null && Input != null) {
                int rowIndex = 1;
                result.first();
                String[] sheetDetail = Input.split(":");
                String sheetName = sheetDetail[0];
                String columnName = sheetDetail[1];
                String value;
                String[] split = Condition.split(",");
                if (split.length > 1) {
                    rowIndex = Integer.parseInt(split[1]);
                }
                if (!result.absolute(rowIndex)) {
                    Report.updateTestLog(Action, "Row : " + rowIndex + " doesn't exist ",
                            Status.FAILNS);
                } else if (getColumnIndex(split[0]) != -1) {
                    value = result.getString(split[0]);
                    userData.putData(sheetName, columnName, value);
                    Report.updateTestLog(Action, "Value from DB " + value + "  stored into " + "the data sheet", Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Column : " + split[0] + " doesn't exist",
                            Status.FAILNS);
                }
            } else {
                Report.updateTestLog(Action, "Incorrect Input or Condition format", Status.FAILNS);
            }
        } catch (SQLException ex) {
            Report.updateTestLog(Action, "Error: " + ex.getMessage(),
                    Status.FAILNS);
            System.out.println("Invalid Data " + Condition);
        }
    }

    @Action(object = ObjectType.DATABASE, desc = "Close the DB Connection")
    public void closeDBConnection() {
        try {
            if (closeConnection()) {
                Report.updateTestLog(Action, "DB Connection is closed", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Error in closing the DB Connection ", Status.FAILNS);
            }
        } catch (SQLException ex) {
            Report.updateTestLog(Action, "Error: " + ex.getMessage(),
                    Status.FAILNS);
        }
    }

    @Action(object = ObjectType.DATABASE, desc = "Verify Table values with the Test Data sheet ", input = InputType.YES)
    public void verifyWithDataSheet() {
        String sheetName = Data;
        TestDataView dataView;
        if (!sheetName.isEmpty() && (dataView = userData.getTestData(sheetName)) != null) {
            List<String> columns = dataView.columns();
            boolean isFailed = false;
            StringBuilder desc = new StringBuilder();
            for (String column : columns.subList(4, columns.size())) {
                if (assertDBEquals(column, dataView.getField(column))) {
                    desc.append("Value ").append(userData.getData(sheetName, column)).append(" exist in the Database").append("\n");
                } else {
                    isFailed = true;
                    desc.append("Value ").append(userData.getData(sheetName, column)).append(" doesn't exist in the Database").append("\n");
                }
            }
            Report.updateTestLog(Action, desc.toString(), isFailed ? Status.FAILNS : Status.PASSNS);
        } else {
            Report.updateTestLog(Action, "Incorrect Sheet Name", Status.FAILNS);
        }
    }

    /**
     * Under the assumption that 1. User executed only SELECT Query 2. Returns a
     * column with one or more rows
     */
    @Action(object = ObjectType.DATABASE, desc = "Query and save the result in variable(s) ", input = InputType.YES, condition = InputType.YES)
    public void storeResultInVariable() {
        String variableName = Condition;
        try {
            executeSelect();
            result.last();
            int totalRows = result.getRow();
            result.beforeFirst();
            for (int index = 1; index <= totalRows; index++) {
                if (result.absolute(index)) {
                    if (index == 1) {
                        addVar(variableName, result.getString(1));
                    } else {
                        String temp = variableName.replaceAll("[%]$", index + "%");
                        addVar(temp, result.getString(1));
                    }
                } else {
                    Report.updateTestLog(Action, "Row " + index + " doesn't exist",
                            Status.FAILNS);
                    return;
                }
            }
            Report.updateTestLog(Action, " SQL Query Result has been saved in the run time variable(s) ",
                    Status.PASSNS);
        } catch (SQLException ex) {
            Report.updateTestLog(Action, "Error executing the SQL Query: " + ex.getMessage(),
                    Status.FAILNS);
        }
    }

    /**
     * Under the assumption that 1. User executed only SELECT Query
     */
    @Action(object = ObjectType.DATABASE, desc = "Query and save the result in Datasheet ", input = InputType.YES, condition = InputType.YES)
    public void storeResultInDataSheet() {
        try {
            executeSelect();
            result.last();
            int totalRows = result.getRow();
            result.beforeFirst();
            int totalCols = resultData.getColumnCount();
            for (int colIndex = 0; colIndex < totalCols; colIndex++) {
                result.beforeFirst();
                for (int rowIndex = 1; rowIndex <= totalRows; rowIndex++) {
                    if (result.absolute(rowIndex)) {
                        userData.putData(Condition, colNames.get(colIndex), result.getString(colIndex + 1), userData.getIteration(), Integer.toString(rowIndex));
                    } else {
                        Report.updateTestLog(Action, "Row " + rowIndex + " doesn't exist",
                                Status.FAILNS);
                        return;
                    }
                }
            }
            Report.updateTestLog(Action, " SQL Query Result has been saved in DataSheet: ",
                    Status.PASSNS);
        } catch (SQLException ex) {
            Report.updateTestLog(Action, "Error executing the SQL Query: " + ex.getMessage(),
                    Status.FAILNS);
        }
    }

    /**
     * 1.similar to assertDBResult but performs a contains check
     * 2. Under the assumption that 1. DB Query is executed in Previous Step and column values are stored in Condition
     * 3. Data holds the expected value to be checked for contains
     */
    @Action(object = ObjectType.DATABASE, desc = "Assert the value [<Input>] contains in the column [<Condition>] ", input = InputType.YES, condition = InputType.YES)
    public void assertDBResultContains() {
        if (Data == null || Data.trim().isEmpty()) {
            Report.updateTestLog(Action, "Expected value is null or empty, cannot perform contains check", Status.FAILNS);
            return;
        }
        if (assertDBContains(Condition, Data)) {
            Report.updateTestLog(Action, "Value " + Data + " exists in the Database (contains match)", Status.PASSNS);
        } else {
            Report.updateTestLog(Action, "Value " + Data + " does not exist in the Database (contains match)", Status.FAILNS);
        }
    }


    /**
     * 1. Under the assumption that Select Query is executed in Previous Step
     * 2. Data holds the DB column value to be validated as not null
     */
    @Action(object = ObjectType.DATABASE, desc = "Assert DB Data Data Not Null ", input = InputType.YES)
    public void assertDBDataNotNull() {
        try {

            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "DB column [" + Data + "]is null or empty ", Status.FAILNS);

            } else {
                Report.updateTestLog(Action, "DB column [" + Data + "] is not null or empty ", Status.PASSNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating DB Data is not null :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }


    /**
     * 1. Under the assumption that 1. DB Query is executed in Previous Step and Value is stored in Condition
     * 2. Condition is Actual variable which holds the DB column value
     * 3. Data holds the expected prefix to be matched
     */
    //TODO :Review and Choose over assertDBResultStartsWith
    @Action(object = ObjectType.DATABASE, desc = "Assert DB Data Starts With ", input = InputType.YES, condition = InputType.YES)
    public void assertDBDataStartsWith() {
        try {

            String prefix = Data;
            String value;
            if (Condition != null && (Condition.startsWith("%") || Condition.endsWith("%"))) {
                value = getVar(Condition);
            } else {
                value = Condition;
            }
            if (value != null && value.startsWith(prefix)) {
                Report.updateTestLog(Action, "DB column [" + value + "] starts with [" + Data + "]", Status.PASSNS);

            } else {
                Report.updateTestLog(Action, "DB column [" + value + "]doesn't start with [" + Data + "]", Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating DB Data prefix :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    //TODO :Review and Choose over assertDBDataStartsWith
    @Action(object = ObjectType.DATABASE, desc = "Assert the value [<Input>] starts with column [<Condition>]", input = InputType.YES, condition = InputType.YES)
    public void assertDBResultStartsWith() {
        if (assertDBStartsWith(Condition, Data)) {
            Report.updateTestLog(Action, "DB column [" + Condition + "] starts with [" + Data + "]", Status.PASSNS);
        } else {
            Report.updateTestLog(Action, " DB column [" + Condition + "] does'nt starts with [" + Data + "]", Status.FAILNS);
        }
    }



    /**
     * 1. Under the assumption that  DB Query is executed in Previous Step and Value is stored in Condition
     * 2. Condition can be a variable which holds the Actual DB column value
     * 3. Data holds the pattern/Reg Exp to be matched
     */
    //TODO :Review and Choose over assertDBResultPattern
    @Action(object = ObjectType.DATABASE, desc = "Assert DB Data Pattern ", input = InputType.YES, condition = InputType.YES)
    public void assertDBDataPattern() {
        try {

            String pattern = Data;
            String value;
            if (Condition != null && (Condition.startsWith("%") || Condition.endsWith("%"))) {
                value = getVar(Condition);
            } else {
                value = Condition;
            }
            if (value != null && value.matches(pattern)) {
                Report.updateTestLog(Action, "DB column [" + value + "] matches the pattern [" + Data + "]", Status.PASSNS);

            } else {
                Report.updateTestLog(Action, "DB column [" + value + "]doesn't match the  pattern [" + Data + "]", Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating DB Data Pattern :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    //TODO :Review and Choose over assertDBDataPattern
    @Action(object = ObjectType.DATABASE, desc = "Assert the value [<Input>] pattern matches column [<Condition>]", input = InputType.YES, condition = InputType.YES)
    public void assertDBResultPattern() {
        if (assertDBPattern(Condition, Data)) {
            Report.updateTestLog(Action, "DB column [" + Condition + "] matches pattern [" + Data + "]", Status.PASSNS);
        } else {
            Report.updateTestLog(Action, "DB column [" + Condition + "] does'nt match pattern [" + Data + "]", Status.FAILNS);
        }
    }


    /**
     * Under the assumption that 1. DB Query is executed in Previous Step and Value is stored in Condition
     * Condition can be a variable which holds the DB column value
     * 2. Data holds the substring to be matched
     */
    /*TODO : REVIEW CHANGE  :  Tried  Pattern ( Reg Expression Checking ) , StartsWith case , contains addressing in 1 function
       ---> NOT FEASIBLE as can'nott have a action with input argument and INGenious design is actions with no arguments and instead
       having separate asserts for each of the case - Pattern , startsWith  and Contains Also contains and startsWith  did not merge as some scenario case contains needs partial text match in between a textline.*/
    @Action(object = ObjectType.DATABASE, desc = "Assert DB Data Pattern/Contains/Startswith ", input = InputType.YES, condition = InputType.YES)
    public void assertDBDataMatch(String mode) {
        try {
            String value;

            if (Condition != null && (Condition.startsWith("%") || Condition.endsWith("%"))) {
                value = getVar(Condition);
            } else {
                value = Condition;
            }
            boolean match = false;
            String passMsg = "";
            String failMsg = "";

            switch (mode.toLowerCase()) {
                case "startswith":
                    match = value != null && value.startsWith(Data);
                    passMsg = "starts with";
                    failMsg = "start with";
                    break;
                case "pattern":
                    match = value != null && value.matches(Data);
                    passMsg = "matches the pattern";
                    failMsg = "match the pattern";
                    break;
                case "contains":
                    match = value != null && value.contains(Data);
                    passMsg = "contains";
                    failMsg = "contain";
                    break;
                default:
                    Report.updateTestLog(Action, "Unknown match mode: " + mode, Status.DEBUG);
                    return;
            }

            if (match) {
                Report.updateTestLog(Action, "DB column [" + value + "] " + passMsg + " [" + Data + "]", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "DB column [" + value + "] doesn't " + failMsg + " [" + Data + "]", Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in validating DB Data " + mode + " :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Under the assumption that 1. Executes single Stored Procedure Call
     * 2. Data contains only a single stored procedure call
     * 3. Stored Procedure execution starts with 'begin' and ends with 'end'
     */
    @Action(object = ObjectType.DATABASE, desc = "Execute the StoredProcedure Query in [<Input>]", input = InputType.YES)
    public void executeStoredProcedureQuery() {
        try {
            // Validate that Data contains only a single stored procedure call
            String trimmedData = (Data != null) ? Data.trim() : "";
            // Simple check: should not contain multiple 'begin' or 'end' or multiple semicolons
            int beginCount = trimmedData.toLowerCase().split("begin", -1).length - 1;
            int endCount = trimmedData.toLowerCase().split("end", -1).length - 1;
            int semicolonCount = trimmedData.length() - trimmedData.replace(";", "").length();

            if (beginCount > 1 || endCount > 1 || semicolonCount > 1) {
                Report.updateTestLog(Action, "Invalid stored procedure input: Only a single stored procedure call is allowed. Input: " + Data, Status.FAILNS);
                return;
            }

            if (executeStoredProcedure()) {
                Report.updateTestLog(Action, "StoredProcedure operation successful using: " + Data, Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "StoredProcedure operation failed using: " + Data, Status.FAILNS);
            }
        } catch (SQLException ex) {
            Report.updateTestLog(Action, "Error executing the StoredProcedure Query: " + ex.getMessage(),
                    Status.FAILNS);
        }
    }


    /**
     * Under the assumption that 1. List of  XML Element .List is seperated by ; in Data sheet
     * Ensures exact match , node to node comparison; expected node list size should match actual node list size
     * expected node value should match actual node value
     * Expected - Data
     * XPath Expressions - Condition
     * If Expected Node and Actual node are reversed then results will be incorrect/Failed
     */
    @Action(object = ObjectType.DATABASE, desc = "Assert DB XML Element List Equal", input = InputType.YES, condition = InputType.YES)
    public void assertDBXMLelementlistEqual() {
        try {
            //Case Insensitive comparison
            boolean ignoreCase = true;
            // Parse the XML response
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(replybodies.get(key)));
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();

            XPath xPath = XPathFactory.newInstance().newXPath();
            String resolvedCondition = (Condition.startsWith("%") && Condition.endsWith("%"))
                    ? getVar(Condition)
                    : Condition;

            List<String> xpathExpressions = Arrays.asList(resolvedCondition.split("\\s*;\\s*"));
            List<String> expectedValues = Arrays.asList(Data.split("\\s*;\\s*"));

            boolean allMatch = true;
            //Compares to shorter list to avoid IndexOutOfBoundsException if size is smaller
            //if sizes are equal, full list is compared
            int minLength = Math.min(xpathExpressions.size(), expectedValues.size());

            for (int i = 0; i < minLength; i++) {
                String expression = xpathExpressions.get(i);
                String expectedValue = expectedValues.get(i);

                try {
                    Node node = (Node) xPath.compile(expression).evaluate(doc, XPathConstants.NODE);
                    String actualValue = (node != null) ? node.getTextContent().trim() : null;

                    boolean match = ignoreCase
                            ? expectedValue.equalsIgnoreCase(actualValue)
                            : expectedValue.equals(
                            actualValue);

                    if (!match) {
                        Report.updateTestLog(Action, "XPath [" + expression + "] → actual [" + actualValue + "] ≠ expected [" + expectedValue + "]", Status.FAILNS);
                        allMatch = false;
                    } else {
                        Report.updateTestLog(Action, "XPath [" + expression + "] → actual [" + actualValue + "] matches expected [" + expectedValue + "]", Status.PASSNS);
                    }
                } catch (XPathExpressionException e) {
                    Report.updateTestLog(Action, "Invalid XPath expression: " + expression + "\n" + e.getMessage(), Status.FAILNS);
                    allMatch = false;
                }
            }

            // Check for mismatched list sizes
            if (xpathExpressions.size() != expectedValues.size()) {
                Report.updateTestLog(Action, "Mismatch in number of XPath expressions and expected values. XPath count: " + xpathExpressions.size() + ", Expected count: " + expectedValues.size(), Status.FAILNS);
            }

        } catch (IOException | ParserConfigurationException | SAXException | DOMException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating DB XML element List:\n" + ex.getMessage(), Status.DEBUG);
        }
    }


    /**
     * Under the assumption that 1. List of  XML Element List is seperated by ; in Data sheet
     *   Ensures Partial text match , node to node comparison; expected node list size should match actual node list size
     *   expected node value partial text matches actual node value
     *   Expected - Data
     *   XPath Expressions - Condition
     *   If Expected Node and Actual node are reversed then results will be incorrect/Failed
     *
     */
    @Action(object = ObjectType.DATABASE, desc = "Assert DB XML Element List Contains", input = InputType.YES, condition = InputType.YES)
    public void assertDBXMLelementlistContains() {
        try {
            boolean ignoreCase = true;
            // Parse the XML response
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(replybodies.get(key)));
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();

            XPath xPath = XPathFactory.newInstance().newXPath();
            String resolvedCondition = (Condition.startsWith("%") && Condition.endsWith("%"))
                    ? getVar(Condition)
                    : Condition;

            List<String> xpathExpressions = Arrays.asList(resolvedCondition.split("\\s*;\\s*"));
            List<String> expectedValues = Arrays.asList(Data.split("\\s*;\\s*"));

            boolean allMatch = true;
            int minLength = Math.min(xpathExpressions.size(), expectedValues.size());

            for (int i = 0; i < minLength; i++) {
                String expression = xpathExpressions.get(i);
                String expectedValue = expectedValues.get(i);

                try {
                    Node node = (Node) xPath.compile(expression).evaluate(doc, XPathConstants.NODE);
                    String actualValue = (node != null) ? node.getTextContent().trim() : null;

                    boolean match = ignoreCase
                            ? actualValue != null && actualValue.toLowerCase().contains(expectedValue.toLowerCase())
                            : actualValue != null && actualValue.contains(expectedValue);

                    if (!match) {
                        Report.updateTestLog(Action, "XPath [" + expression + "] → actual [" + actualValue + "] does not contain expected [" + expectedValue + "]", Status.FAILNS);
                        allMatch = false;
                    } else {
                        Report.updateTestLog(Action, "XPath [" + expression + "] → actual [" + actualValue + "] contains expected [" + expectedValue + "]", Status.PASSNS);
                    }
                } catch (XPathExpressionException e) {
                    Report.updateTestLog(Action, "Invalid XPath expression: " + expression + "\n" + e.getMessage(), Status.FAILNS);
                    allMatch = false;
                }
            }

            // Check for mismatched list sizes
            if (xpathExpressions.size() != expectedValues.size()) {
                Report.updateTestLog(Action, "Mismatch in number of XPath expressions and expected values. XPath count: " + xpathExpressions.size() + ", Expected count: " + expectedValues.size(), Status.FAILNS);
            }

        } catch (IOException | ParserConfigurationException | SAXException | DOMException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating DB XML element List:\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    /**
     * Under the assumption that
     * 1. Store the XMLTag Element Value into VariableName - Condition
     * 2. Variable Can be used later to save in Datasheet or validate
     * 3. Condition holds the XPath Expression to fetch the particular XML Element Value
     *
     */
    @Action(object = ObjectType.DATABASE, desc = "Store DB XML Element", input = InputType.YES, condition = InputType.YES)
    public void storeDBXMLelementInVariable() {
        try {
            String variableName = Condition;
            //If the expreession is datasheet reference, fetch the actual expression gets processed in the resolveVars
            String expression = Data;
            if (variableName.matches("%.*%")) {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder;
                InputSource inputSource = new InputSource();
                inputSource.setCharacterStream(new StringReader(replybodies.get(key)));
                dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(inputSource);
                doc.getDocumentElement().normalize();
                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);
                Node nNode = nodeList.item(0);
                String value = (nNode != null) ? nNode.getTextContent() : null;
                addVar(variableName, value);
                Report.updateTestLog(Action, "DB XML element value stored", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException
                 | SAXException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing DB XML element :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }


    /**
     * Under the assumption that 1. User executed only SELECT Query
     * 2. Result has  only XML Reply cell value
     * 3. Store the XML REply into VariableName - Condition
     * 4. Variable Can be used later to save in Datasheet or validate
     */
    @Action(object = ObjectType.DATABASE, desc = "Query and save the XMLReply in Datasheet ", input = InputType.YES, condition = InputType.YES)
    public void storeResultDBXMLReplyInVariable() {
        String variableName = Condition;
        try {
            executeSelect();
            result.last();
            int totalRows = result.getRow();
            result.beforeFirst();
            int totalCols = resultData.getColumnCount();
            for (int colIndex = 0; colIndex < totalCols; colIndex++) {
                result.beforeFirst();
                replybodies.clear();
                for (int rowIndex = 1; rowIndex <= totalRows; rowIndex++) {
                    if (result.absolute(rowIndex)) {
                        String xmlReply = result.getString(colIndex + 1);
                        //userData.putData(Condition, colNames.get(colIndex), result.getString(colIndex + 1), userData.getIteration(), Integer.toString(rowIndex));
                        String varName = (rowIndex == 1)
                                ? variableName
                                : variableName.replaceAll("[%]$", rowIndex + "%");
                        addVar(varName, result.getString(1));

                        if (xmlReply != null) {
                            replybodies.put(key, xmlReply);
                        }
                    } else {
                        Report.updateTestLog(Action, "Row " + rowIndex + " doesn't exist",
                                Status.FAILNS);
                        return;
                    }
                }
            }
            Report.updateTestLog(Action, " SQL Query Reply has been saved in run time variable(s) ",
                    Status.PASSNS);
        } catch (SQLException ex) {
            Report.updateTestLog(Action, "Error executing the SQL Query: " + ex.getMessage(),
                    Status.FAILNS);
        }

    }


    /*  Under the assumption that 1. XML Reply/Text Message is stored in Variable in previous step - storeDBXMLelementInVariable and
     *                           2. XML Reply/text- replybodies is input to Datasheet
     *                          3. Stores particular XMLTagElement Values List separated by ; in Datasheet
     *                          4. Condition - List of XPath Expressions separated by ; to extract multiple element values
     *                         5. INput - sheetName:ColumnName format to store value in Datasheet
     *                          6. Works for Single and multiple element values
     * */
    @Action(object = ObjectType.DATABASE, desc = "Store DB XML ElementList In DataSheet", input = InputType.YES, condition = InputType.YES)
    public void storeDBXMLelementListInDataSheet() {
        try {
            String strObj = Input;
            if (strObj.matches(".*:.*")) {
                try {
                    System.out.println("Updating value in SubIteration " + userData.getSubIteration());
                    String sheetName = strObj.split(":", 2)[0];
                    String columnName = strObj.split(":", 2)[1];
                    String xmlText = replybodies.get(key);
                    System.out.println("XML Text: " + xmlText);
                    if (xmlText == null) {
                        Report.updateTestLog(Action, "No XML found for key: " + key, Status.DEBUG);
                        return;
                    }

                    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                    InputSource inputSource = new InputSource(new StringReader(xmlText));
                    Document doc = dBuilder.parse(inputSource);
                    doc.getDocumentElement().normalize();
                    XPath xPath = XPathFactory.newInstance().newXPath();

                    String[] expressions;
                    if (Condition != null && (Condition.startsWith("%") || Condition.endsWith("%"))) {
                        expressions = getVar(Condition).split(";");
                    } else {
                        expressions = Condition.split(";");
                    }

                    List<String> values = new ArrayList<>();
                    for (String expr : expressions) {
                        expr = expr.trim();
                        if (!expr.isEmpty()) {
                            System.out.println("Evaluating XPath Expression: " + expr);
                            NodeList nodeList = (NodeList) xPath.compile(expr).evaluate(doc, XPathConstants.NODESET);
                            Node nNode = (nodeList != null && nodeList.getLength() > 0) ? nodeList.item(0) : null;
                            String value = (nNode != null && nNode.getTextContent() != null) ? nNode.getTextContent().trim() : "";
                            values.add(value);
                        } else {
                            values.add(""); // Add empty string for empty expressions
                        }
                    }
                    String combinedValue = String.join(";", values);
                    System.out.println("Combined value: " + combinedValue);
                    //Updates both Iteration and SubIteration
                    userData.putData(sheetName, columnName, combinedValue);
                    Report.updateTestLog(Action, "Element texts [" + combinedValue + "] stored in " + strObj, Status.DONE);

                } catch (IOException | ParserConfigurationException | XPathExpressionException | DOMException |
                         SAXException ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.OFF, ex.getMessage(), ex);
                    Report.updateTestLog(Action, "Error storing DB XML element List in datasheet:\n" + ex.getMessage(), Status.DEBUG);
                }
            } else {
                Report.updateTestLog(Action,
                        "Given input [" + Input + "] format is invalid. It should be [sheetName:ColumnName]",
                        Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error storing DB XML element List in datasheet:\n" + ex.getMessage(), Status.DEBUG);
        }
    }



    /**
     * Asserts the presence or absence of an XML tag or element in the stored XML reply.
     * Supports both XMLTag (by tag name) and XMLElement (by full element or attribute via XPath).
     * <p>
     * - XMLTag: Provide the tag name or XPath to the tag (e.g., //xmltag or /xmltag/subtag)
     * - XMLElement: Provide a full XPath to the element, attribute, or value (e.g., //xmltag[text()=''])
     * <p>
     * Data: Set to 'true' if the tag/element is expected to be present, 'false' if it should be absent.
     * Condition: The XPath expression or variable key for the tag/element to check.
     */
    @Action(object = ObjectType.DATABASE, desc = "Assert DB XML Tag or Element Presence", input = InputType.YES, condition = InputType.YES)
    public void assertDBXMLTagorElementPresence() {
        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(replybodies.get(key)));
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputSource);
            doc.getDocumentElement().normalize();
            XPath xPath = XPathFactory.newInstance().newXPath();
            // XMLTag or XMLElement to be verified
            String expression;
            if (Condition != null && (Condition.startsWith("%") || Condition.endsWith("%"))) {
                // Treat as a variable key and fetch the actual expression
                expression = getVar(Condition);
            } else {
                // Use Condition directly as XPath
                expression = Condition;
            }
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(doc, XPathConstants.NODESET);

            boolean expectTagOrElementPresent = Boolean.parseBoolean(Data.trim());
            String checkType = (expression.matches("^(/\\w+)+$") && !expression.contains("[")) ? "XML Tag" : "XML Element";
            if (expectTagOrElementPresent) {
                if (nodeList.getLength() > 0) {
                    Report.updateTestLog(Action, checkType + " is present as expected (XPath: " + expression + ")", Status.PASSNS);
                } else {
                    Report.updateTestLog(Action, checkType + " is missing but was expected (XPath: " + expression + ")", Status.FAILNS);
                }
            } else {
                if (nodeList.getLength() == 0) {
                    Report.updateTestLog(Action, checkType + " is not present as expected (XPath: " + expression + ")", Status.PASSNS);
                } else {
                    Report.updateTestLog(Action, checkType + " is present but should not be (XPath: " + expression + ")", Status.FAILNS);
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error validating DB XML tag/element presence: " + ex.getMessage(), Status.DEBUG);
        }
    }
}