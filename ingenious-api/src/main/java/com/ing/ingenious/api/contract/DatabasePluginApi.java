package com.ing.ingenious.api.contract;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;
import com.ing.ingenious.api.dto.DMLResult;

/**
 * Interface for general database operations in INGenious.
 * Provides methods for database connection, query execution, result handling, and assertions.
 */
public interface DatabasePluginApi extends CommandPluginApi {

    /**
     * Gets the current database connection.
     * @return the JDBC Connection object
     */
    Connection getDbconnection();

    /**
     * Gets the current SQL statement.
     * @return the Statement object
     */
    Statement getStatement();

    /**
     * Gets the current SQL query result set.
     * @return the ResultSet object
     */
    ResultSet getResult();

    /**
     * Gets the metadata for the current result set.
     * @return the ResultSetMetaData object
     */
    ResultSetMetaData getResultData();

    /**
     * Gets the list of column names from the current result set.
     * @return a list of column names
     */
    List<String> getColNames();

    /**
     * Gets the data input parameter for the current command.
     * @return the data input string
     */
    String getData();

    /**
     * Gets the action name for the current command.
     * @return the action name
     */
    String getAction();

    /**
     * Gets the input parameter for the current command.
     * @return the input string
     */
    String getInput();

    /**
     * Retrieves database connection details for the specified database name.
     * @param dbName the database name
     * @return the database connection properties
     */
    Properties getDBDetails(String dbName);

    /**
     * Gets the index of the specified column in the result set.
     * @param columnName the column name
     * @return the column index (0-based)
     */
    int getColumnIndex(String columnName);

    /**
     * Verifies the database connection for the given database name.
     * @param dbName the database name
     * @return true if the connection is valid, false otherwise
     * @throws ClassNotFoundException if the JDBC driver class is not found
     * @throws java.sql.SQLException if a database access error occurs
     */
    boolean verifyDbConnection(String dbName) throws ClassNotFoundException, java.sql.SQLException;

    /**
     * Executes a SELECT SQL query using the current statement.
     * @throws java.sql.SQLException if a database access error occurs
     */
    void executeSelect() throws java.sql.SQLException;

    /**
     * Executes a DML (Data Manipulation Language) SQL statement (INSERT, UPDATE, DELETE).
     * 
     * @return a DMLResult containing the success status and executed query
     * @throws java.sql.SQLException if a database access error occurs
     */
    DMLResult executeDML() throws java.sql.SQLException;

    /**
     * Closes the current database connection.
     * @return true if the connection was closed successfully, false otherwise
     * @throws java.sql.SQLException if a database access error occurs
     */
    boolean closeConnection() throws java.sql.SQLException;

    /**
     * Asserts a condition on a database column value.
     * @param columnName the column name to check
     * @param condition the assertion condition (e.g., value, expression)
     * @return true if the assertion passes, false otherwise
     */
    boolean assertDB(String columnName, String condition);

    /**
     * Stores a value from the database into a variable or global context.
     * @param input the value to store
     * @param condition the condition or key for storage
     * @param isGlobal true to store globally, false for local storage
     */
    boolean storeValue(String input, String condition, boolean isGlobal);

}

