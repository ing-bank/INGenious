package com.ing.engine.commands.database;

import com.ing.datalib.testdata.view.TestDataView;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

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

    @Action(object = ObjectType.DATABASE, desc = "Assert the value [<Input>] exist in the column [<Condition>] ", input = InputType.YES, condition = InputType.YES)
    public void assertDBResult() {
        if (assertDB(Condition, Data)) {
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
                if (assertDB(column, dataView.getField(column))) {
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
}