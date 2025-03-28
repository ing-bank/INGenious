package com.ing.engine.commands.database;

import com.google.common.base.Objects;
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

            String dbDriver = dbDetails.getProperty(DB_DRIVER);
            String dbConnectionString = dbDetails.getProperty(DB_CONN_STR);
            String dbUser = dbDetails.getProperty(DB_USER);
            String dbPass = dbDetails.getProperty(DB_PWD);
            if (dbPass.endsWith(" Enc")) {
                dbPass = dbPass.substring(0, dbPass.lastIndexOf(" Enc"));
                byte[] valueDecoded = Encryption.getInstance().decrypt(dbPass).getBytes();
                dbPass = new String(valueDecoded);
            }
            Boolean dbCommit = Boolean.valueOf(dbDetails.getProperty(DB_COMMIT));
            int dbTimeout=Integer.parseInt(dbDetails.getProperty(DB_TIME_OUT));


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
}