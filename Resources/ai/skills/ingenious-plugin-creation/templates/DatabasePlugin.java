package com.ing.plugin.database;

import com.ing.ingenious.api.contract.DatabasePluginApi;
import com.ing.ingenious.api.contract.data.UserDataAccessApi;
import com.ing.ingenious.api.contract.reports.TestCaseReportApi;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import com.ing.ingenious.api.annotation.Action;

import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Database testing plugin for SQL operations
 * 
 * @author Your Name
 */
public class DatabasePlugin {
    
    // API contract instance
    DatabasePluginApi gen;
    
    // Test data fields
    public String Data;
    public String Action;
    public String Input;
    public String Condition;
    public TestCaseReportApi Report;
    public UserDataAccessApi userData;
    public String ObjectName;

    /**
     * Constructor - receives DatabasePluginApi from framework
     */
    public DatabasePlugin(DatabasePluginApi gen) {
        this.gen = gen;
        this.Data = gen.getData();
        this.Action = gen.getAction();
        this.Input = gen.getInput();
        this.Condition = gen.getCondition();
        this.Report = gen.getReport();
        this.userData = gen.getUserData();
        this.ObjectName = gen.getObjectName();
    }

    /**
     * Execute DML query with variable substitution
     * Demonstrates processQuery() helper method
     */
    @Action(object = ObjectType.DATABASE, 
            desc = "Execute DML Query Example [<Data>]", 
            input = InputType.YES)
    public void executeDMLQueryExample() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Query is empty or null", Status.FAIL);
                return;
            }
            
            // Process query for variable substitution using helper method
            String processedQuery = processQuery(Data);
            String originalData = this.Data;
            this.Data = processedQuery;
            
            // Execute DML through framework API
            gen.executeDML();
            
            // Restore original data
            this.Data = originalData;
            
            Report.updateTestLog(Action, 
                "DML Query executed successfully: " + processedQuery, 
                Status.DONE);
                
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, 
                "SQL Error: " + ex.getMessage(), 
                Status.FAIL);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, 
                "Unexpected error: " + ex.getMessage(), 
                Status.FAIL);
        }
    }

    /**
     * Execute SELECT and store column value from result
     * Demonstrates complex query handling with result extraction
     */
    @Action(object = ObjectType.DATABASE, 
            desc = "Execute Query Example [<Data>] and Store Column [<Input>] in [<Condition>]", 
            input = InputType.YES, 
            condition = InputType.YES)
    public void executeQueryAndStoreValueExample() {
        try {
            if (Data == null || Data.trim().isEmpty()) {
                Report.updateTestLog(Action, "Query is empty or null", Status.FAIL);
                return;
            }
            
            String columnName = Input;
            String variableName = Condition;
            
            if (!variableName.matches("%.*%")) {
                Report.updateTestLog(Action, 
                    "Variable format incorrect. Expected: %variableName%", 
                    Status.FAIL);
                return;
            }
            
            // Process and execute query using helper method
            String processedQuery = processQuery(Data);
            String originalData = this.Data;
            this.Data = processedQuery;
            
            gen.executeSelect();
            ResultSet rs = gen.getResult();
            
            if (rs != null && rs.next()) {
                String value = rs.getString(columnName);
                gen.addVar(variableName, value);
                
                Report.updateTestLog(Action, 
                    "Value [" + value + "] from column [" + columnName + 
                    "] stored in " + variableName, 
                    Status.PASS);
            } else {
                Report.updateTestLog(Action, "Query returned no results", Status.DONE);
            }
            
            this.Data = originalData;
            
        } catch (SQLException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, 
                "SQL Error: " + ex.getMessage(), 
                Status.FAIL);
        }
    }

    // ========== Helper Methods ==========

    /**
     * Process query - handles variable substitution
     * Replaces %variableName% with actual values
     */
    private String processQuery(String query) {
        if (query == null) return null;
        
        // Find all variables in format %variableName%
        Pattern pattern = Pattern.compile("%([^%]+)%");
        Matcher matcher = pattern.matcher(query);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String variableName = "%" + matcher.group(1) + "%";
            String value = gen.getVar(variableName);
            
            if (value == null || value.equals(variableName)) {
                // Variable not found, keep original
                matcher.appendReplacement(result, 
                    Matcher.quoteReplacement(variableName));
            } else {
                // Replace with actual value
                matcher.appendReplacement(result, 
                    Matcher.quoteReplacement(value));
            }
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
}
