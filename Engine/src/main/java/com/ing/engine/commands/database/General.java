package com.ing.engine.commands.database;

import com.google.common.base.Objects;
import com.ing.datalib.settings.UserDefinedSettings;
import com.ing.datalib.util.data.LinkedProperties;
import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.util.encryption.Encryption;
import com.ing.engine.core.Control;
import java.util.Collection;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides database command utilities for executing SQL queries, managing connections,
 * handling variable resolution, and storing results. This class is intended to be extended
 * for specific database operations and supports both DML and SELECT queries.
 */
public class General extends Command {

    public static Connection dbconnection;
    static Statement statement;
    static ResultSet result;
    static ResultSetMetaData resultData;

    static final String DB_NAME = "db.alias";
    static final String DB_USER = "user";
    static final String DB_PWD = "password";
    static final String DB_DRIVER = "driver";
    static final String DB_CONN_STR = "connectionString";
    static final String DB_TIME_OUT = "timeout";
    static final String DB_COMMIT = "commit";
    static final Pattern INPUTS = Pattern.compile("([^{]+?)(?=\\})");
    static List<String> colNames = new ArrayList<>();

    /**
     * Constructs a General database command handler with the given command control.
     *
     * @param cc the command control context
     */
    public General(CommandControl cc) {
        super(cc);
    }

    /**
     * Verifies and establishes a database connection using the specified database name.
     *
     * @param dbName the name or alias of the database
     * @return true if the connection is established successfully, false otherwise
     * @throws ClassNotFoundException if the database driver class is not found
     * @throws SQLException if a database access error occurs
     */
    public boolean verifyDbConnection(String dbName) throws ClassNotFoundException, SQLException {
        if (getDBFile(dbName).exists()) {
            Properties dbDetails = getDBDetails(dbName);
            
            String dbDriver             = resolveAllVariables(dbDetails.getProperty(DB_DRIVER));
            String dbConnectionString   = resolveAllVariables(dbDetails.getProperty(DB_CONN_STR));
            String dbUser               = resolveAllVariables(dbDetails.getProperty(DB_USER));
            String dbPass               = resolveAllVariables(dbDetails.getProperty(DB_PWD));
            String dbCommitStr          = resolveAllVariables(dbDetails.getProperty(DB_COMMIT));
            String dbTimeoutStr         = resolveAllVariables(dbDetails.getProperty(DB_TIME_OUT));
            
            if (dbPass.endsWith(" Enc")) {
                dbPass = dbPass.substring(0, dbPass.lastIndexOf(" Enc"));
                byte[] valueDecoded = Encryption.getInstance().decrypt(dbPass).getBytes();
                dbPass = new String(valueDecoded);
            }
            
            Boolean dbCommit = Boolean.valueOf(dbCommitStr);
            int dbTimeout = Integer.parseInt(dbTimeoutStr);

            if (dbDriver != null) {
                Class.forName(dbDriver);
                if (dbConnectionString != null && dbUser != null && dbPass != null) {
                    dbconnection = DriverManager.getConnection(dbConnectionString, dbUser,dbPass);
                } else if (dbConnectionString != null) {
                    dbconnection = DriverManager.getConnection(dbConnectionString);
                }
                initialize(dbCommit,dbTimeout);

                return (dbconnection != null);
            }
            return false;
        }
        return false;
    } 

    /**
     * Detects and resolves all variables in the input string, including datasheet variables,
     * user-defined variables, and runtime variables.
     *
     * <p>If no variables are present, the original string is returned unchanged.</p>
     *
     * @param str the input string to evaluate; may or may not contain variables
     * @return a string with all detected variables replaced by their corresponding values,
     *         or the original string if none are found
     */
    private String resolveAllVariables(String str) {
        str=handleDataSheetVariables(str);
        str=resolveAllRuntimeVars(str);
        return str;
    }

    /**
     * Executes a SELECT SQL query after resolving variables and stores the result set.
     *
     * @throws SQLException if a database access error occurs
     */
    public void executeSelect() throws SQLException {
        String query = Data;
    	query = handleDataSheetVariables(query);
    	query = handleUserDefinedVariables(query);
        System.out.println("Query :" + query);
        result = statement.executeQuery(query);
        resultData = result.getMetaData();
        populateColumnNames();
    }

    /**
     * Represents the result of a DML operation, including success status and the executed query.
     */
    public static class DMLResult {
        public final boolean success;
        public final String query;
        public DMLResult(boolean success, String query) {
            this.success = success;
            this.query = query;
        }
    }

    /**
     * Executes a DML SQL query (INSERT, UPDATE, DELETE) after resolving variables.
     *
     * @return a DMLResult containing the success status and the executed query
     * @throws SQLException if a database access error occurs
     */
    public DMLResult executeDML() throws SQLException {
        String query = Data;
        query = handleDataSheetVariables(query);
        query = handleUserDefinedVariables(query);
        System.out.println("Executing DML query: :" + query);
        boolean result = (statement.executeUpdate(query) >= 0);
        return new DMLResult(result, query);
    }

    /**
     * Initializes the database connection, statement, and variable resolution.
     *
     * @param commit whether to use auto-commit mode
     * @param timeout the query timeout in seconds
     * @throws SQLException if a database access error occurs
     */
    private void initialize(Boolean commit,int timeout) throws SQLException {
        colNames.clear();
        dbconnection.setAutoCommit(commit);
        statement = dbconnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_READ_ONLY);
        statement.setQueryTimeout(timeout);
        resolveVars();
    }

    /**
     * Closes the database connection, statement, and result set.
     *
     * @return true if all resources are closed successfully, false otherwise
     * @throws SQLException if a database access error occurs
     */
    public boolean closeConnection() throws SQLException {
        if (dbconnection != null && statement != null && result != null) {
            dbconnection.close();
            statement.close();
            result.close();
            return dbconnection.isClosed() && statement.isClosed() && result.isClosed();
        }
        return true;
    }

    /**
     * Asserts that a value exists in the specified column of the result set.
     *
     * @param columnName the column to check
     * @param condition the value to assert
     * @return true if the value exists, false otherwise
     */
    public boolean assertDB(String columnName, String condition) {
        boolean isExist = false;
        try {
            result.beforeFirst();
            if (getColumnIndex(columnName) != -1) {
                while (result.next()) {
                    if (Objects.equal(result.getString(columnName), condition)) {
                        isExist = true;
                        break;
                    }
                }
            } else {
                Report.updateTestLog(Action, "Column " + columnName + " doesn't exist", Status.FAIL);
            }
        } catch (SQLException ex) {
            Report.updateTestLog(Action, "Error asserting the value in DB " + ex.getMessage(), Status.FAIL);
            return false;
        }
        return isExist;
    }

    /**
     * Stores a value from the result set in a variable or global variable.
     *
     * @param input the variable name
     * @param condition the column and row specification
     * @param isGlobal true to store as a global variable, false for local
     */
    public void storeValue(String input, String condition, boolean isGlobal) {
        String value;
        int rowIndex = 1;
        String[] split = condition.split(",");
        if (split.length > 1) {
            rowIndex = Integer.parseInt(split[1]);
        }
        try {
            if (getColumnIndex(split[0]) != -1) {
                result.first();
                if (result.absolute(rowIndex)) {
                    value = result.getString(split[0]);
                    if (isGlobal) {
                        addGlobalVar(input, value);
                    } else {
                        addVar(input, value);
                    }
                } else {
                    Report.updateTestLog(Action, "Row " + rowIndex + " doesn't exist",
                            Status.FAIL);
                }
            } else {
                Report.updateTestLog(Action, "Column " + split[0] + " doesn't exist ",
                        Status.FAIL);
            }
        } catch (SQLException se) {
            Report.updateTestLog(Action, "Error storing value in variable " + se.getMessage(), Status.FAIL);
        }
    }

    /**
     * Resolves variables in the Data string and replaces them with their values.
     */
    private void resolveVars() {
        Matcher matcher = INPUTS.matcher(Data);
        Set<String> listMatches = new HashSet<>();
        while (matcher.find()) {
            listMatches.add(matcher.group(1));
        }
        listMatches.stream().forEach((s) -> {
            String replace;
            if (s.contains("%")) {
                replace = getVar(s);
            } else {
                String[] sheet = s.split(":");
                replace = userData.getData(sheet[0], sheet[1]);
            }
            if (replace != null) {
                Data = Data.replace("{" + s + "}", "'" + replace + "'");
            }
        });
    }


    /**
     * Retrieves database properties for the specified database name.
     *
     * @param dbName the database name or alias
     * @return the database properties
     */
    public Properties getDBDetails(String dbName) {
        return getDataBaseData(dbName);
    }

    /**
     * Populates the column names from the result set metadata.
     *
     * @throws SQLException if a database access error occurs
     */
    private void populateColumnNames() throws SQLException {
        int count = resultData.getColumnCount();
        for (int index = 1; index <= count; index++) {
            colNames.add(resultData.getColumnName(index));
        }
    }

    /**
     * Gets the index of the specified column name in the column list.
     *
     * @param columnName the column name to search for
     * @return the index of the column, or -1 if not found
     */
    public int getColumnIndex(String columnName) {
        return colNames.indexOf(columnName);
    }

    /**
     * Resolves datasheet variables in the query string.
     *
     * @param query the SQL query string
     * @return the query with datasheet variables replaced
     */
    private String handleDataSheetVariables(String query) {
        List<String> sheetlist = Control.getCurrentProject().getTestData().getTestDataFor(Control.exe.runEnv())
                .getTestDataNames();
        for (int sheet = 0; sheet < sheetlist.size(); sheet++) {
            if (query.contains("{" + sheetlist.get(sheet) + ":")) {
                com.ing.datalib.testdata.model.TestDataModel tdModel = Control.getCurrentProject()
                        .getTestData().getTestDataByName(sheetlist.get(sheet));
                List<String> columns = tdModel.getColumns();
                for (int col = 0; col < columns.size(); col++) {
                    if (query.contains("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}")) {
                    	query = query.replace("{" + sheetlist.get(sheet) + ":" + columns.get(col) + "}",
                                userData.getData(sheetlist.get(sheet), columns.get(col)));
                    }
                }
            }
        }
        return query;
    }

    /**
     * Resolves user-defined variables in the query string.
     *
     * @param query the SQL query string
     * @return the query with user-defined variables replaced
     */
    private String handleUserDefinedVariables(String query) {
        Collection<Object> valuelist = Control.getCurrentProject().getProjectSettings().getUserDefinedSettings()
                .values();
        for (Object prop : valuelist) {
            if (query.contains("{" + prop + "}")) {
            	query = query.replace("{" + prop + "}", prop.toString());
            }
        }
        return query;
    }
}