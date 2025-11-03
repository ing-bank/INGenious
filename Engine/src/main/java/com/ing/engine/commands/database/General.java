package com.ing.engine.commands.database;

import com.google.common.base.Objects;
import com.ing.datalib.settings.UserDefinedSettings;
import com.ing.datalib.util.data.LinkedProperties;
import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
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
 *
 *
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

    public General(CommandControl cc) {
        super(cc);
    }

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

    public void executeSelect() throws SQLException {
        String query = Data;
    	query = handleDataSheetVariables(query);
    	query = handleuserDefinedVariables(query);
        System.out.println("Query :" + query);
        result = statement.executeQuery(query);
        resultData = result.getMetaData();
        populateColumnNames();
    }

    public boolean executeDML() throws SQLException {
        String query = Data;
    	query = handleDataSheetVariables(query);
    	query = handleuserDefinedVariables(query);
        System.out.println("Query :" + query);
        return (statement.executeUpdate(query) >= 0);
    }

    private void initialize(Boolean commit,int timeout) throws SQLException {
        colNames.clear();
        dbconnection.setAutoCommit(commit);
        statement = dbconnection.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE,ResultSet.CONCUR_UPDATABLE);
        statement.setQueryTimeout(timeout);
        resolveVars();
    }

    public boolean closeConnection() throws SQLException {
        if (dbconnection != null && statement != null && result != null) {
            dbconnection.close();
            statement.close();
            result.close();
            return dbconnection.isClosed() && statement.isClosed() && result.isClosed();
        }
        return true;
    }

    public boolean assertDBEquals(String columnName, String condition) {
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


    public Properties getDBDetails(String dbName) {
        return getDataBaseData(dbName);
    }

    private void populateColumnNames() throws SQLException {
        int count = resultData.getColumnCount();
        for (int index = 1; index <= count; index++) {
            colNames.add(resultData.getColumnName(index));
        }
    }

    public int getColumnIndex(String columnName) {
        return colNames.indexOf(columnName);
    }

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

    private String handleuserDefinedVariables(String query) {
        Collection<Object> valuelist = Control.getCurrentProject().getProjectSettings().getUserDefinedSettings()
                .values();
        for (Object prop : valuelist) {
            if (query.contains("{" + prop + "}")) {
            	query = query.replace("{" + prop + "}", prop.toString());
            }
        }
        return query;
    }



    public boolean assertDBContains(String columnName, String condition) {
        boolean isExist = false;
        try {
            result.beforeFirst();
            if (getColumnIndex(columnName) != -1) {
                while (result.next()) {
                    String value = result.getString(columnName);
                    if (value != null && value.trim().contains(condition.trim())) {
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


    //TODO: REVIEW and CHOOSE if assertDBStartsWith is needed or assertDBDataStartsWith is sufficient
    public boolean assertDBStartsWith(String columnName, String prefix) {
        try {
            result.beforeFirst();
            if (getColumnIndex(columnName) != -1) {
                while (result.next()) {
                    String value = result.getString(columnName);
                    if (value != null && value.startsWith(prefix)) {
                        return true;
                    }
                }
            } else {
                Report.updateTestLog(Action, "Column " + columnName + " doesn't exist", Status.FAIL);
                return false;
            }
        } catch (SQLException ex) {
            Report.updateTestLog(Action, "Error asserting startsWith in DB: " + ex.getMessage(), Status.FAIL);
            return false;
        }
        return false;
    }

    //TODO: REVIEW and CHOOSE if assertDBPattern is needed or assertDBDataPattern is sufficient
    public boolean assertDBPattern(String columnName, String pattern) {
        try {
            result.beforeFirst();
            if (getColumnIndex(columnName) != -1) {
                while (result.next()) {
                    String value = result.getString(columnName);
                    if (value != null && value.matches(pattern)) {
                        return true;
                    }
                }
            } else {
                Report.updateTestLog(Action, "Column [" + columnName + "] doesn't exist", Status.FAIL);
                return false;
            }
        } catch (SQLException ex) {
            Report.updateTestLog(Action, "Error asserting pattern in DB: " + ex.getMessage(), Status.FAIL);
            return false;
        }
        return false;
    }



    //TODO: REVIEW and CHOOSE if assertDBResultSTARTSWITH is needed or assertDBDataStartsWith is sufficient
    @Action(object = ObjectType.DATABASE, desc = "Assert the value [<Input>] starts with column [<Condition>]", input = InputType.YES, condition = InputType.YES)
    public void assertDBResultStartsWith() {
        if (assertDBStartsWith(Condition, Data)) {
            Report.updateTestLog(Action, "DB column [" + Condition + "] starts with [" + Data + "]", Status.PASSNS);
        } else {
            Report.updateTestLog(Action, " DB column [" + Condition + "] does'nt starts with [" + Data + "]", Status.FAILNS);
        }
    }


    //TODO: REVIEW and CHOOSE if assertDBResultPATTERN is needed or assertDBDataPattern is sufficient
    @Action(object = ObjectType.DATABASE, desc = "Assert the value [<Input>] pattern matches column [<Condition>]", input = InputType.YES, condition = InputType.YES)
    public void assertDBResultPattern() {
        if (assertDBPattern(Condition, Data)) {
            Report.updateTestLog(Action, "DB column [" + Condition + "] matches pattern [" + Data + "]", Status.PASSNS);
        } else {
            Report.updateTestLog(Action, "DB column [" + Condition + "] does'nt match pattern [" + Data + "]", Status.FAILNS);
        }
    }

    public boolean executeStoredProcedure() throws SQLException {
        String query = Data;
        query = handleDataSheetVariables(query);
        query = handleuserDefinedVariables(query);
        System.out.println("Query :" + query);
        try (CallableStatement callableStatement = dbconnection.prepareCall(query)) {
            callableStatement.execute();
            return true;
        } catch (SQLException ex) {
            System.err.println("StoredProcedure execution failed: " + ex.getMessage());
            System.err.println("SQLState: " + ex.getSQLState() + ", ErrorCode: " + ex.getErrorCode());
            ex.printStackTrace();
            return false;
        }

    }
}