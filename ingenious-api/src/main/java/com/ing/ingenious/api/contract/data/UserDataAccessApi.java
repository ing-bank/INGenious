package com.ing.ingenious.api.contract.data;

/**
 * Interface for user data access operations in INGenious.
 * Provides methods to retrieve and update test data, global data, and iteration context.
 */
public interface UserDataAccessApi {

    /**
     * Gets the current scenario name in execution context.
     * @return the current scenario name
     */
    String getCurrentScenario();

    /**
     * Gets the current test case name in execution context.
     * @return the current test case name
     */
    String getCurrentTestCase();

    /**
     * Gets the scenario name for the current data context.
     * @return the scenario name
     */
    String getScenario();

    /**
     * Gets the test case name for the current data context.
     * @return the test case name
     */
    String getTestCase();

    /**
     * Gets the current iteration value.
     * @return the iteration value as a string
     */
    String getIteration();

    /**
     * Gets the sub-iteration value for the current test case.
     * @return the sub-iteration value as a string
     */
    String getTestCaseSubIteration();

    /**
     * Gets the current sub-iteration value.
     * @return the sub-iteration value as a string
     */
    String getSubIteration();

    /**
     * Gets the current sub-iteration as an integer.
     * @return the sub-iteration value as an integer
     */
    int getSubIterationAsNumber();

    /**
     * Retrieves a value from the global data store.
     * @param globalDataID the global data identifier
     * @param columnName the column name
     * @return the value from the global data
     */
    String getGlobalData(String globalDataID, String columnName);

    /**
     * Updates a value in the global data store.
     * @param globalDataID the global data identifier
     * @param columnName the column name
     * @param value the value to set
     */
    void putGlobalData(String globalDataID, String columnName, String value);

    /**
     * Retrieves a value from the test data sheet for the current scenario/testcase/iteration context.
     * @param sheet the sheet name
     * @param column the column name
     * @return the value from the test data
     */
    String getData(String sheet, String column);

    /**
     * Retrieves a value from the test data sheet for a specific iteration and sub-iteration.
     * @param sheet the sheet name
     * @param column the column name
     * @param iteration the iteration value
     * @param subIteration the sub-iteration value
     * @return the value from the test data
     */
    String getData(String sheet, String column, String iteration, String subIteration);

    /**
     * Retrieves a value from the test data sheet for a specific scenario, testcase, iteration, and sub-iteration.
     * @param sheet the sheet name
     * @param column the column name
     * @param scenario the scenario name
     * @param testcase the test case name
     * @param iteration the iteration value
     * @param subiteration the sub-iteration value
     * @return the value from the test data
     */
    String getData(String sheet, String column, String scenario, String testcase, String iteration, String subiteration);

    /**
     * Updates a value in the test data sheet for the current context.
     * @param sheet the sheet name
     * @param column the column name
     * @param value the value to set
     */
    void putData(String sheet, String column, String value);

    /**
     * Updates a value in the test data sheet for a specific iteration and sub-iteration.
     * @param sheet the sheet name
     * @param column the column name
     * @param value the value to set
     * @param iteration the iteration value
     * @param subIteration the sub-iteration value
     */
    void putData(String sheet, String column, String value, String iteration, String subIteration);

    /**
     * Updates a value in the test data sheet for a specific scenario, testcase, iteration, and sub-iteration.
     * @param sheet the sheet name
     * @param column the column name
     * @param value the value to set
     * @param scenario the scenario name
     * @param testcase the test case name
     * @param iteration the iteration value
     * @param subIteration the sub-iteration value
     */
    void putData(String sheet, String column, String value, String scenario, String testcase, String iteration, String subIteration);

    /**
     * Gets a view of the test data for the specified sheet.
     * @param sheetName the sheet name
     * @return a TestDataViewApi for the sheet
     */
    TestDataViewApi getTestData(String sheetName);
}