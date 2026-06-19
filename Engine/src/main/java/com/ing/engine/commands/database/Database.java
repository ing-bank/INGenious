package com.ing.engine.commands.database;

import com.ing.datalib.testdata.view.TestDataView;
import com.ing.engine.core.CommandControl;

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

import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.dto.DMLResult;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides database-specific command implementations for executing queries, asserting results,
 * storing values, and managing database connections. Extends General for common database utilities.
 */
public class Database extends General {

    /**
     * Constructs a Database command handler with the given command control.
     *
     * @param cc the command control context
     */
    public Database(CommandControl cc) {
        super(cc);
    }

    /**
     * Initiates the database connection using the input database name.
     * Updates the test log with connection status and metadata.
     */
    @Action(
        object = ObjectType.DATABASE,
        desc = "Initiate the DB transaction",
        input = InputType.YES
    )
    public void initDBConnection() {
        try {
            String dbName = Input;
            if (dbName.startsWith("#")) {
                dbName = dbName.replace("#", "");
                if (verifyDbConnection(dbName)) {
                    DatabaseMetaData metaData = dbconnection.getMetaData();
                    Report.updateTestLog(
                        Action,
                        " Connected with " +
                        metaData.getDriverName() +
                        "\n" +
                        "Driver version " +
                        metaData.getDriverVersion() +
                        " \n" +
                        "Database product name " +
                        metaData.getDatabaseProductName() +
                        "\n" +
                        "Database product version " +
                        metaData.getDatabaseProductVersion(),
                        Status.PASSNS
                    );
                } else {
                    Report.updateTestLog(
                        Action,
                        "Could not able to make DB connection ",
                        Status.FAILNS
                    );
                }
            }
        } catch (ClassNotFoundException | SQLException ex) {
            Report.updateTestLog(
                Action,
                "Error connecting Database: " + ex.getMessage(),
                Status.FAILNS
            );
        }
    }

    /**
     * Executes a SELECT query and updates the test log with the result.
     */
    @Action(
        object = ObjectType.DATABASE,
        desc = "Execute the Query in [<Input>]",
        input = InputType.YES
    )
    public void executeSelectQuery() {
        try {
            executeSelect();
            Report.updateTestLog(Action, "Executed Select Query", Status.DONE);
        } catch (SQLException ex) {
            Report.updateTestLog(
                Action,
                "Error executing the SQL Query: " + ex.getMessage(),
                Status.FAILNS
            );
        }
    }

    /**
     * Executes a DML query (INSERT, UPDATE, DELETE) and updates the test log with the result and query used.
     */
    @Action(
        object = ObjectType.DATABASE,
        desc = "Execute the Query in [<Input>]",
        input = InputType.YES
    )
    public void executeDMLQuery() {
        try {
            DMLResult result = executeDML();
            if (result.success) {
                Report.updateTestLog(
                    Action,
                    "Table updated by using query: " + result.query,
                    Status.PASSNS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Table not updated by using query: " + result.query,
                    Status.FAILNS
                );
            }
        } catch (SQLException ex) {
            Report.updateTestLog(
                Action,
                "Error executing the SQL Query: " + ex.getMessage(),
                Status.FAILNS
            );
        }
    }

    /**
     * Asserts that the value in Data exists in the specified column (Condition) of the database.
     * Updates the test log with the assertion result.
     */
    @Action(
        object = ObjectType.DATABASE,
        desc = "Assert the value [<Input>] exist in the column [<Condition>] ",
        input = InputType.YES,
        condition = InputType.YES
    )
    public void assertDBResult() {
        if (assertDB(Condition, Data)) {
            Report.updateTestLog(Action, "Value " + Data + " exist in the Database", Status.PASSNS);
        } else {
            Report.updateTestLog(
                Action,
                "Value " + Data + " doesn't exist in the Database",
                Status.FAILNS
            );
        }
    }

    /**
     * Stores the value from the specified DB column (Condition) in a global variable (Input).
     * Updates the test log with the storage result.
     */
    @Action(
        object = ObjectType.DATABASE,
        desc = "Store it in Global variable from the DB column [<Condition>] ",
        input = InputType.YES,
        condition = InputType.YES
    )
    public void storeValueInGlobalVariable() {
        if (storeValue(Input, Condition, true)) {
            Report.updateTestLog(Action, "Stored in Global variable", Status.PASSNS);
        }
    }

    /**
     * Stores the value from the specified DB column (Condition) in a local variable (Input).
     * Updates the test log with the storage result.
     */
    @Action(
        object = ObjectType.DATABASE,
        desc = "Store it in the variable from the DB column [<Condition>] ",
        input = InputType.YES,
        condition = InputType.YES
    )
    public void storeValueInVariable() {
        if (storeValue(Input, Condition, false)) {
            Report.updateTestLog(Action, "Stored in the variable", Status.PASSNS);
        }
    }

    /**
     * Stores the value from the specified DB column (Condition) in the test data sheet (Input).
     * Updates the test log with the storage result.
     */
    @Action(
        object = ObjectType.DATABASE,
        desc = "Save DB value in Test Data Sheet",
        input = InputType.YES,
        condition = InputType.YES
    )
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
                    Report.updateTestLog(
                        Action,
                        "Row : " + rowIndex + " doesn't exist ",
                        Status.FAILNS
                    );
                } else if (getColumnIndex(split[0]) != -1) {
                    value = result.getString(split[0]);
                    userData.putData(sheetName, columnName, value);
                    Report.updateTestLog(
                        Action,
                        "Value from DB " + value + "  stored into " + "the data sheet",
                        Status.DONE
                    );
                } else {
                    Report.updateTestLog(
                        Action,
                        "Column : " + split[0] + " doesn't exist",
                        Status.FAILNS
                    );
                }
            } else {
                Report.updateTestLog(Action, "Incorrect Input or Condition format", Status.FAILNS);
            }
        } catch (SQLException ex) {
            Report.updateTestLog(Action, "Error: " + ex.getMessage(), Status.FAILNS);
            System.out.println("Invalid Data " + Condition);
        }
    }

    /**
     * Closes the database connection and updates the test log with the result.
     */
    @Action(object = ObjectType.DATABASE, desc = "Close the DB Connection")
    public void closeDBConnection() {
        try {
            if (closeConnection()) {
                Report.updateTestLog(Action, "DB Connection is closed", Status.PASSNS);
            } else {
                Report.updateTestLog(Action, "Error in closing the DB Connection ", Status.FAILNS);
            }
        } catch (SQLException ex) {
            Report.updateTestLog(Action, "Error: " + ex.getMessage(), Status.FAILNS);
        }
    }

    /**
     * Verifies table values against the test data sheet and updates the test log with the result.
     */
    @Action(
        object = ObjectType.DATABASE,
        desc = "Verify Table values with the Test Data sheet ",
        input = InputType.YES
    )
    public void verifyWithDataSheet() {
        String sheetName = Data;
        TestDataView dataView;
        if (!sheetName.isEmpty() && (dataView = userData.getTestData(sheetName)) != null) {
            List<String> columns = dataView.columns();
            boolean isFailed = false;
            StringBuilder desc = new StringBuilder();
            for (String column : columns.subList(4, columns.size())) {
                if (assertDB(column, dataView.getField(column))) {
                    desc
                        .append("Value ")
                        .append(userData.getData(sheetName, column))
                        .append(" exist in the Database")
                        .append("\n");
                } else {
                    isFailed = true;
                    desc
                        .append("Value ")
                        .append(userData.getData(sheetName, column))
                        .append(" doesn't exist in the Database")
                        .append("\n");
                }
            }
            Report.updateTestLog(Action, desc.toString(), isFailed ? Status.FAILNS : Status.PASSNS);
        } else {
            Report.updateTestLog(Action, "Incorrect Sheet Name", Status.FAILNS);
        }
    }

    /**
     * Stores the result of a SELECT query in runtime variable(s) based on the specified condition.
     * Assumes the query returns one or more rows in a column.
     */
    @Action(
        object = ObjectType.DATABASE,
        desc = "Query and save the result in variable(s) ",
        input = InputType.YES,
        condition = InputType.YES
    )
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
                    Report.updateTestLog(Action, "Row " + index + " doesn't exist", Status.FAILNS);
                    return;
                }
            }
            Report.updateTestLog(
                Action,
                " SQL Query Result has been saved in the run time variable(s) ",
                Status.PASSNS
            );
        } catch (SQLException ex) {
            Report.updateTestLog(
                Action,
                "Error executing the SQL Query: " + ex.getMessage(),
                Status.FAILNS
            );
        }
    }

    /**
     * Stores the result of a SELECT query in the datasheet based on the specified condition.
     * Assumes the query returns one or more rows.
     */
    @Action(
        object = ObjectType.DATABASE,
        desc = "Query and save the result in Datasheet ",
        input = InputType.YES,
        condition = InputType.YES
    )
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
                        userData.putData(
                            Condition,
                            colNames.get(colIndex),
                            result.getString(colIndex + 1),
                            userData.getIteration(),
                            Integer.toString(rowIndex)
                        );
                    } else {
                        Report.updateTestLog(
                            Action,
                            "Row " + rowIndex + " doesn't exist",
                            Status.FAILNS
                        );
                        return;
                    }
                }
            }
            Report.updateTestLog(
                Action,
                " SQL Query Result has been saved in DataSheet: ",
                Status.PASSNS
            );
        } catch (SQLException ex) {
            Report.updateTestLog(
                Action,
                "Error executing the SQL Query: " + ex.getMessage(),
                Status.FAILNS
            );
        }
    }


    /** Validates that the DB column value contains the expected value.
     * <p> Fetches Value from DB column name passed in Condition</p>
     *
     * @param - Condition - Actual - A variable which holds the DB column name.
     *          Input/Data - Expected - Data stored in datasheet and checked against the DB column value.
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

    /** Validates that the DB column value stored in Input/Data is not null or empty
     *
     * @param - Condition - Not used in this action.
     *          Input/Data - Actual - Datasheet Value stored in SheetName:ColumnName format.
     * @throws  if an error occurs during Validation
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


    /** Validates that the DB column value stored in Condition matches the expected prefix.
     *
     * @param - Condition - Actual - A variable which holds the DB column value.
     *          Input/Data - Expected - A prefix to be checked against the DB column value.
     * @throws  if an error occurs during Validation
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

    /** Validates that the DB column value matches the expected prefix.
     * <p> Fetches Value from DB column name passed in Condition</p>
     *
     * @param - Condition - Actual - A variable which holds the DB column name.
     *          Input/Data - Expected - A prefix to be checked against the DB column value.
     * @throws  if an error occurs during Validation
     */
    //TODO :Review and Choose over assertDBDataStartsWith
    @Action(object = ObjectType.DATABASE, desc = "Assert the value [<Input>] starts with column [<Condition>]", input = InputType.YES, condition = InputType.YES)
    public void assertDBResultStartsWith() {
        if (assertDBStartsWith(Condition, Data)) {
            Report.updateTestLog(Action, "DB column [" + Condition + "] starts with [" + Data + "]", Status.PASSNS);
        } else {
            Report.updateTestLog(Action, " DB column [" + Condition + "] does'nt starts with [" + Data + "]", Status.FAILNS);
        }
    }



    /** Validates that the DB column value stored in Condition matches the expected pattern/Reg Exp.
     *
     * @param - Condition - Actual - A variable which holds the DB column value.
     *          Input/Data - Expected - A pattern/Reg Exp to be checked against the DB column value.
     * @throws  if an error occurs during Validation
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

    /** Validates that the DB column value matches the expected pattern/Reg Exp.
     * <p> Fetches Value from DB column name passed in Condition</p>

     * @param - Condition - Actual - A variable which holds the DB column name.
     *          Input/Data - Expected - A pattern/Reg Exp to be checked against the DB column value.
     * @throws Exception if an error occurs during Validation
     */
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
     * Validates that the XML elements extracted via XPath expressions match the expected values based on the specified mode (pattern, startsWith, contains).
     * @param mode The mode of validation: "pattern", "startsWith", or "contains".
     *             - Condition - Actual - A variable which holds the DB column value.
     *
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
     * Executes a stored procedure call provided in the Data field.
     * @param - Condition - Not used in this action.
     *          Input/Data - The stored procedure call to be executed.
     * @throws Exception if an error occurs during stored procedure execution or multiple stored procedure calls are detected.
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
     * Validates that the XML elements extracted via XPath expressions are exactly equal to the expected values.
     * @param - Condition - Actual - A semicolon-separated list of XPath expressions to extract multiple XML element values.
     *          Input/Data - Expected - A semicolon-separated list of expected values to be checked for equality.
     * @throws Exception if an error occurs during XML parsing or XPath evaluation or Validation
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
     * Validates that the XML elements extracted via XPath expressions contain( partial text match) the expected values.
     * @param - Condition - Actual - A semicolon-separated list of XPath expressions to extract multiple XML element values.
     *          Input/Data - Expected - A semicolon-separated list of expected values to be checked for containment.
     * @throws Exception if an error occurs during XML parsing or XPath evaluation or Validation
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
     * Stores XML reply from the database query result into runtime variables.
     * @param - Condition - The name of the variable to store the XML reply.
     *          Input/Data - Query to be executed to fetch the XML reply.
     * @throws Exception if an error occurs during storing the XML reply in variable or executing the SQL query
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
     * Stores XML reply from the database query result into runtime variables.
     * @param - Condition - The name of the variable to store the XML reply.
     *          Input/Data - Query to be executed to fetch the XML reply.
     * @throws Exception if an error occurs during storing the XML reply in variable or executing the SQL query
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

    /**
     * Stores XML element single or list of values from the stored XML reply into the specified Datasheet.
     * @param - Input - The Datasheet and column to store the extracted values.Data should be in the format sheetName:ColumnName to store the extracted values.
     *         - Condition - A semicolon-separated list of XPath expressions to extract multiple XML element values.
     * @throws Exception if an error occurs during storing XML element values in Datasheet
     */
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
     * Validates the presence or absence of an XML tag or element in the stored XML reply.
     * @param - Condition -The XML tag name or XPath expression to check for presence.Can be a variable key (e.g., %varName%) or direct XPath.
     *          Data should be 'true' or 'false' (case-insensitive) indicating expected presence or absence.
     * @throws Exception if an error occurs during XML parsing or XPath evaluation
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
            String trimmedData = Data.trim();
            if (!trimmedData.equalsIgnoreCase("true") && !trimmedData.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException("Data must be 'true' or 'false' (case-insensitive), but was: " + Data);
            }
            boolean expectTagOrElementPresent = Boolean.parseBoolean(trimmedData);
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
