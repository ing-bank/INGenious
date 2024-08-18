
package com.ing.engine.execution.data;

import com.ing.datalib.testdata.view.TestDataView;
import com.ing.engine.execution.run.TestCaseRunner;

/**
 *
 *
 */
public abstract class UserDataAccess {

    abstract public TestCaseRunner context();

    public String getCurrentScenario() {
        return context().scenario();
    }

    public String getCurrentTestCase() {
        return context().testcase();
    }

    public String getScenario() {
        return context().getRoot().scenario();
    }

    public String getTestCase() {
        return context().getRoot().testcase();
    }

    public String getIteration() {
        return context().iteration();
    }

    public String getTestCaseSubIteration() {
        return context().subIteration();
    }

    public String getSubIteration() {
        return context().getCurrentSubIteration();
    }

    public int getSubIterationAsNumber() {
        return Integer.valueOf(getSubIteration());
    }

    public String getGlobalData(String globalDataID, String columnName) {
        return DataAccess.getGlobalData(this.context(), globalDataID, columnName);
    }

    public void putGlobalData(String globalDataID, String columnName, String value) {
        DataAccess.putGlobalData(this.context(), globalDataID, columnName, value);
    }

    public String getData(String Sheet, String Column) {
        return DataAccess.getData(context(), Sheet, Column, getIteration(), getSubIteration());
    }

    public String getData(String Sheet, String Column, String Iteration, String SubIteration) {
        return DataAccess.getData(context(), Sheet, Column, Iteration, SubIteration);
    }

    public String getData(String sheet, String column, String scenario, String testcase, String iteration,
            String subiteration) {
        return DataAccess.getData(context(), sheet, column, scenario, testcase, iteration, subiteration);
    }

    public void putData(String sheet, String column, String value) {
        putData(sheet, column, value, getIteration(), getSubIteration());
    }

    public void putData(String sheet, String column, String value, String iteration, String subIteration) {
        DataAccess.putData(context(), sheet, column, value, iteration, subIteration);
    }

    public void putData(String sheet, String column, String value, String scenario, String testcase, String iteration,
            String subIteration) {
        DataAccess.putData(context(), sheet, column, value, scenario, testcase, iteration, subIteration);
    }

    public TestDataView getTestData(String sheetName) {
        return DataAccess.getTestData(context(), sheetName).withSubIter(context().scenario(), context().testcase(),
                context().iteration(), context().subIteration());
    }

}
