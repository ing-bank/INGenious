package com.ing.ingenious.api.contract.data;

import java.util.List;

/**
 * The test data of the project a user has open, for plugins that read or write it while tests
 * are being designed.
 *
 * <p>The engine already reads test data while a test runs, through {@link TestDataViewApi}.
 * This is the other half of the same model, at design time. A plugin that helps prepare a test
 * case — a data picker, an importer, a bridge to an external test management system — can
 * record on the test case itself what it decided, so the decision becomes part of the project,
 * is visible where test data is edited, and is still there the next time the project is opened.
 *
 * <p>Every method answers for whichever project is open at the moment it is called, so a plugin
 * can keep one instance for its lifetime instead of following project changes. With no project
 * open there is nothing to read or write: {@link #sheets()} is empty, {@link #testCase} returns
 * {@code null}, and the rest return {@code false}.
 *
 * <pre>{@code
 * testData.addSheet("Customers");
 * testData.addColumn("Customers", "Segment");
 * testData.testCase("Customers", "Checkout", "Pay by card").update("Segment", "business");
 * testData.save("Customers");
 * }</pre>
 */
public interface ProjectTestDataApi {
    /**
     * Names of the test data sheets in the open project.
     *
     * @return the sheet names, empty when no project is open
     */
    List<String> sheets();

    /**
     * Adds a sheet unless the project already has one under that name.
     *
     * @param sheet the sheet name
     * @return {@code true} when the project has the sheet afterwards
     */
    boolean addSheet(String sheet);

    /**
     * Adds a column to a sheet unless the sheet already has one under that name.
     *
     * @param sheet the sheet name
     * @param column the column name
     * @return {@code true} when the sheet has the column afterwards
     */
    boolean addColumn(String sheet, String column);

    /**
     * The records of one test case, to read from and to write to.
     *
     * <p>A test case that has no records in the sheet yet is given one, so a caller that means
     * to write does not have to know whether this test case has been given data before.
     *
     * @param sheet the sheet name
     * @param scenario the scenario the test case belongs to
     * @param testCase the test case name
     * @return the view, or {@code null} when no project is open or the sheet does not exist
     */
    TestDataViewApi testCase(String sheet, String scenario, String testCase);

    /**
     * Writes a sheet back to the project. Changes made through this interface are held in
     * memory until this is called.
     *
     * @param sheet the sheet name
     * @return {@code true} when the sheet was written
     */
    boolean save(String sheet);
}
