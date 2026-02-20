package com.ing.ingenious.api.contract.data;

import java.util.List;
import java.util.Set;

/**
 * Interface for viewing and manipulating test data records in INGenious.
 * Provides methods for accessing, updating, and filtering test data by scenario, testcase, iteration, and sub-iteration.
 */
public interface TestDataViewApi {

    /**
     * Gets the list of column names in the test data.
     * @return a list of column names
     */
    List columns();

    /**
     * Gets the list of all records in the test data.
     * @return a list of records
     */
    List records();

    /**
     * Gets the index of the specified field (column).
     * @param field the field (column) name
     * @return the index of the field
     */
    int index(String field);

    /**
     * Checks if the specified field (column) can be updated.
     * @param field the field (column) name
     * @return true if the field can be updated, false otherwise
     */
    boolean canUpdate(String field);

    /**
     * Adds a new record for the specified scenario, testcase, iteration, and sub-iteration.
     * @param scenario the scenario name
     * @param testcase the testcase name
     * @param iteration the iteration value
     * @param subIteration the sub-iteration value
     * @return the list of field values for the new record
     */
    List<String> addRecord(String scenario, String testcase, String iteration, String subIteration);

    /**
     * Clears all records from the test data view.
     */
    void clear();

    /**
     * Puts a list of records for the specified key.
     * @param key the key (e.g., scenario or testcase)
     * @param records the list of records to put
     */
    void put(String key, List records);

    /**
     * Adds a list of records for the specified key.
     * @param key the key (e.g., scenario or testcase)
     * @param records the list of records to add
     */
    void add(String key, List<String> records);

    /**
     * Gets the list of records for the specified key.
     * @param key the key (e.g., scenario or testcase)
     * @return the list of records
     */
    List get(String key);

    /**
     * Gets the list of all records.
     * @return the list of all records
     */
    List get();

    /**
     * Gets the value of a specific field for a given key.
     * @param key the key (e.g., scenario or testcase)
     * @param field the field (column) name
     * @return the value of the field
     */
    String getField(String key, String field);

    /**
     * Gets the value of a specific field for the current record.
     * @param field the field (column) name
     * @return the value of the field
     */
    String getField(String field);

    /**
     * Updates the value of a specific field for the current record.
     * @param field the field (column) name
     * @param value the new value to set
     * @return true if the update was successful, false otherwise
     */
    boolean update(String field, String value);

    /**
     * Gets the list of values for a specific field for a given key.
     * @param key the key (e.g., scenario or testcase)
     * @param field the field (column) name
     * @return the list of field values
     */
    List<String> getFields(String key, String field);

    /**
     * Gets the list of values for a specific field for all records.
     * @param field the field (column) name
     * @return the list of field values
     */
    List<String> getFields(String field);

    /**
     * Gets the set of all iteration values present in the test data.
     * @return a set of iteration values
     */
    Set<String> getIterations();

    /**
     * Gets the set of all sub-iteration values present in the test data.
     * @return a set of sub-iteration values
     */
    Set<String> getSubIterations();

    /**
     * Filters the test data view by scenario or global ID.
     * @param scnOrgid the scenario or global ID
     * @return a filtered TestDataViewApi instance
     */
    TestDataViewApi withScenarioOrGID(String scnOrgid);

    /**
     * Filters the test data view by scenario and testcase.
     * @param scn the scenario name
     * @param tc the testcase name
     * @return a filtered TestDataViewApi instance
     */
    TestDataViewApi withTestcase(String scn, String tc);

    /**
     * Filters the test data view by scenario, testcase, and iteration.
     * @param scn the scenario name
     * @param tc the testcase name
     * @param iter the iteration value
     * @return a filtered TestDataViewApi instance
     */
    TestDataViewApi withIter(String scn, String tc, String iter);

    /**
     * Filters the test data view by scenario, testcase, and iteration, with option to add if not present.
     * @param scn the scenario name
     * @param tc the testcase name
     * @param iter the iteration value
     * @param addIfNotPresent true to add the record if not present
     * @return a filtered TestDataViewApi instance
     */
    TestDataViewApi withIter(String scn, String tc, String iter, Boolean addIfNotPresent);

    /**
     * Filters the test data view by scenario, testcase, iteration, and sub-iteration.
     * @param scn the scenario name
     * @param tc the testcase name
     * @param iter the iteration value
     * @param subIter the sub-iteration value
     * @return a filtered TestDataViewApi instance
     */
    TestDataViewApi withSubIter(String scn, String tc, String iter, String subIter);

    /**
     * Filters the test data view by scenario, testcase, iteration, and sub-iteration, with option to add if not present.
     * @param scn the scenario name
     * @param tc the testcase name
     * @param iter the iteration value
     * @param subIter the sub-iteration value
     * @param addIfNotPresent true to add the record if not present
     * @return a filtered TestDataViewApi instance
     */
    TestDataViewApi withSubIter(String scn, String tc, String iter, String subIter, Boolean addIfNotPresent);
}
