package com.ing.engine.commands.general;

import com.ing.engine.commands.browser.CommonMethods;
import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ForcedException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneralOperations extends General {

    public GeneralOperations(CommandControl cc) {
        super(cc);
    }

    @Action(object = ObjectType.GENERAL, desc = "This a dummy function helpful with testing.")
    public void filler() {

    }

    @Action(object = ObjectType.GENERAL, desc = "print the data [<Data>]", input = InputType.YES)
    public void print() {
        System.out.println(Data);
        Report.updateTestLog("print", String.format("printed %s", Data), Status.DONE);
    }

    @Action(object = ObjectType.GENERAL, desc = "Wait for [<Data>] milli seconds", input = InputType.YES)
    public void pause() {
        try {
            Thread.sleep(Long.parseLong(Data));
            Report.updateTestLog(Action, "Thread sleep for '" + Long.parseLong(Data), Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(Action, e.getMessage(), Status.FAIL);
            Logger.getLogger(CommonMethods.class.getName()).log(Level.SEVERE, null, e);
        }

    }

    @Action(object = ObjectType.GENERAL,
            desc = "Assert if Key:Value -> [<Data>] is valid",
            input = InputType.YES)
    public void assertVariable() throws RuntimeException {
        try {
            String strObj = Data;
            String[] strTemp = strObj.split("=", 2);
            String strAns = strTemp[0].matches("%.+%") ? getVar(strTemp[0]) : strTemp[0];
            if (strAns.equals(strTemp[1])) {
                System.out.println("Condition '" + Input + "' is true ");
                Report.updateTestLog("assertVariable",
                        "Variable value matches with provided data " + strTemp[1], Status.PASSNS);

            } else {
                System.out.println("Condition '" + Input + "' is false ");
                Report.updateTestLog("assertVariable",
                        "Variable value is " + strAns + " but expected value is " + strTemp[1], Status.FAILNS);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            throw new ForcedException("assertVariable", ex.getMessage());
        }
    }

    @Action(object = ObjectType.GENERAL,
            desc = "Assert if  the  variable value matches with given value from datasheet(variable:datasheet->  [<Data>] )",
            input = InputType.YES,
            condition = InputType.YES)
    public void assertVariableFromDataSheet() throws RuntimeException {
        try {
            String strAns = getVar(Condition);
            if (strAns.equals(Data)) {
                System.out.println("Variable " + Condition + " equals "
                        + Input);
                Report.updateTestLog(Action,
                        "Variable is matched with the expected result", Status.DONE);

            } else {
                System.out.println("Variable " + Condition + " is not equal "
                        + Input);
                throw new ForcedException(Action,
                        "Variable did not match with provided data");
            }
        } catch (Exception e) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, e);
            throw new ForcedException("assertVariableFromDataSheet", e.getMessage());
        }
    }

    @Action(object = ObjectType.GENERAL, desc = "Add a variable to access within testcase", input = InputType.YES, condition = InputType.YES)
    public void AddVar() {
        if (Input.startsWith("=Replace(")) {
            replaceFunction();
        } else if (Input.startsWith("=Split(")) {
            splitFunction();
        } else if (Input.startsWith("=Substring(")) {
            subStringFunction();
        } else {
            addVar(Condition, Data);
        }

        if (getVar(Condition) != null) {
            Report.updateTestLog("addVar", "Variable " + Condition + " added with value [" + Data +"]", Status.DONE);
        } else {
            Report.updateTestLog("addVar", "Variable " + Condition + " not added ", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.GENERAL, desc = "Add a Global variable to access across test set", input = InputType.YES, condition = InputType.YES)
    public void AddGlobalVar() {
        addGlobalVar(Condition, Data);
        if (getVar(Condition) != null) {
            Report.updateTestLog(Action, "Variable " + Condition
                    + " added with value " + Data, Status.DONE);
        } else {
            Report.updateTestLog(Action, "Variable " + Condition
                    + " not added ", Status.DEBUG);
        }
    }

    @Action(object = ObjectType.GENERAL, desc = "store variable value [<Condition>] in data sheet[<Data>]", input = InputType.YES, condition = InputType.YES)
    public void storeVariableInDataSheet() {
        if (Input != null && Condition != null) {
            if (!getVar(Condition).isEmpty()) {
                System.out.println(Condition);
                String[] sheetDetail = Input.split(":");
                String sheetName = sheetDetail[0];
                String columnName = sheetDetail[1];
                userData.putData(sheetName, columnName, getVar(Condition));
                Report.updateTestLog(Action,
                        "Value of variable " + Condition + " has been stored into " + "the data sheet", Status.DONE);
            } else {
                Report.updateTestLog(Action, "The variable " + Condition + " does not contain any value", Status.FAIL);
            }
        } else {
            Report.updateTestLog(Action, "Incorrect input format", Status.DEBUG);
            System.out.println("Incorrect input format " + Condition);
        }
    }

    public void replaceFunction() {
        String op = "";
        String original = "";
        String targetString = "";
        String replaceString = "";
        String occurance = "";
        String[] args2 = null;
        String args1 = Input.split("Replace\\(")[1];
        if (args1.substring(args1.length() - 1).equals(":")) {
            args2 = args1.substring(0, args1.length() - 2).split(",'");
        } else {
            args2 = args1.substring(0, args1.length() - 1).split(",'");
        }
        targetString = args2[1].substring(0, args2[1].length() - 1);
        replaceString = args2[2].split("',")[0];
        occurance = args2[2].split("',")[1];
        Pattern pattern = Pattern.compile("%.*%");
        Matcher matcher = pattern.matcher(args2[0]);
        if (matcher.find()) {
            original = getVar(args2[0]);
        } else {
            original = args2[0].substring(1, args2[0].length() - 1);
        }
        try {
            if (args2.length > 0) {
                if (occurance.toLowerCase().equals("first")) {
                    System.out.println("original " + original);
                    System.out.println("targetString " + targetString);
                    System.out.println("replaceString " + replaceString);
                    System.out.println("occurance " + occurance);

                    op = original.replaceFirst(targetString, replaceString);
                } else {
                    System.out.println("original " + original);
                    System.out.println("targetString " + targetString);
                    System.out.println("replaceString " + replaceString);
                    System.out.println("occurance " + occurance);
                    op = original.replace(targetString, replaceString);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        addVar(Condition, op);
    }

    public void splitFunction() {
        try {
            String op = "";
            String original = "";
            String regex = "";
            String stringIndex = "";
            String splitLength = "";
            String[] args2 = null;
            String[] stringSplit = null;
            String args1 = Input.split("Split\\(")[1];

            if (args1.substring(args1.length() - 1).equals(":")) {
                args2 = args1.substring(0, args1.length() - 2).split(",'");
            } else {
                args2 = args1.substring(0, args1.length() - 1).split(",'");
            }
            regex = args2[1].split("',")[0];
            int arrayLength = args2.length;
            Pattern pattern = Pattern.compile("%.*%");
            Matcher matcher = pattern.matcher(args2[0]);
            if (matcher.find()) {
                original = getVar(args2[0]);
            } else {
                original = args2[0].substring(1, args2[0].length() - 1);
            }
            if (!(args2[1].split("',")[1]).contains(",")) {
                stringIndex = args2[1].split("',")[1];
                stringSplit = original.split(regex);
            } else {
                stringIndex = args2[1].split("',")[1].split(",")[1];
                splitLength = args2[1].split("',")[1].split(",")[0];
                stringSplit = original.split(regex, Integer.parseInt(splitLength));
            }
            op = stringSplit[Integer.parseInt(stringIndex)];
            addVar(Condition, op);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void subStringFunction() {
        try {
            String op = "";
            String original = "";
            String startIndex = "";
            String endIndex = "";
            String[] args2 = null;
            String args1 = Input.split("Substring\\(")[1];

            if (args1.substring(args1.length() - 1).equals(":")) {
                args2 = args1.substring(0, args1.length() - 2).split(",");
            } else {
                args2 = args1.substring(0, args1.length() - 1).split(",");
            }
            Pattern pattern = Pattern.compile("%.*%");
            Matcher matcher = pattern.matcher(args2[0]);
            if (matcher.find()) {
                original = getVar(args2[0]);
                startIndex = args2[1];
                if (args2.length == 3) {
                    endIndex = args2[2];
                }
            } else {
                String[] args3 = args1.substring(0, args1.length() - 2).split("',");
                original = args3[0].substring(1, args2[0].length() - 1);
                if (args3[1].contains(",")) {
                    startIndex = args3[1].split(",")[0];
                    endIndex = args3[1].split(",")[1];
                } else {
                    startIndex = args3[1];
                }
            }

            if (endIndex.equals("")) {
                op = original.substring(Integer.parseInt(startIndex));
            } else {
                op = original.substring(Integer.parseInt(startIndex), Integer.parseInt(endIndex));
            }
            addVar(Condition, op);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * ******************************************
     * Function to verify variable
     *
     * ******************************************
     */
    @Action(object = ObjectType.MOBILE, desc = "Verify if the specific [<Data>] is present", input = InputType.YES)
    public void verifyVariable() {
        String strObj = Data;
        String[] strTemp = strObj.split("=", 2);
        String strAns = getVar(strTemp[0]);
        if (strAns.equals(strTemp[1])) {
            System.out.println("Variable " + strTemp[0] + " equals "
                    + strTemp[1]);
            Report.updateTestLog(Action,
                    "Variable is matched with the expected result", Status.PASS);
        } else {
            System.out.println("Variable " + strTemp[0] + " not equals "
                    + strTemp[1]);
            Report.updateTestLog(Action,
                    "Variable doesn't match with the expected result",
                    Status.FAIL);
        }
    }

    @Action(object = ObjectType.MOBILE, desc = "Verify of variable [<Data>] from given datasheet", input = InputType.YES, condition = InputType.YES)
    public void verifyVariableFromDataSheet() {
        String strAns = getVar(Condition);
        if (strAns.equals(Data)) {
            System.out.println("Variable " + Condition + " equals "
                    + Input);
            Report.updateTestLog(Action,
                    "Variable is matched with the expected result", Status.DONE);

        } else {
            System.out.println("Variable " + Condition + " is not equal "
                    + Input);
            Report.updateTestLog(Action,
                    "Variable doesn't matched with the expected result",
                    Status.DEBUG);
        }
    }
    
    /**
    * Stores data from a previous test case into either a runtime variable or a target datasheet.
    * <p>
    * This method retrieves the value from a specified source datasheet column using the context of a previous
    * test case ({@code %PreviousScenario%}, {@code %PreviousTestCase%}, {@code %PreviousIteration%} and {@code %PreviousSubIteration%}). 
    * The retrieved value is then stored based on the format of the {@code Input} parameter:
    * <ul>
    *     <li>If {@code Input} is a runtime variable (e.g., "%VarName%"), the value is stored in that variable.</li>
    *     <li>If {@code Input} is a datasheet reference (e.g., "SheetName:ColumnName"), the value is stored in the specified column.</li>
    * </ul>
    * <p>
    * 
    * After execution, it resets the runtime variables related to the previous test case context.
    */
    @Action(object = ObjectType.GENERAL, desc = "Store Data from Previous Test Case Data", input = InputType.YES, condition = InputType.YES)
    public void storeDataFromPreviousTestCaseData() {
        if (Input.isBlank()) { 
            Report.updateTestLog(Action, "Input is required to get the source Datasheet.", Status.FAIL);
        } else if (Condition.isBlank()) {
            Report.updateTestLog(Action, "Condition is required for the target Datasheet.", Status.FAIL);
        } else if (!Input.isBlank() && !Condition.isBlank()){
            String prevScenarioVar = getRuntimeVar("%PreviousScenario%");
            String prevTestCaseVar = getRuntimeVar("%PreviousTestCase%");
            String prevIterationVar = getRuntimeVar("%PreviousIteration%");
            String prevSubIterationVar = getRuntimeVar("%PreviousSubIteration%");
            String sourceScenario = prevScenarioVar != null ? prevScenarioVar : userData.getScenario(); 
            String sourceTestCase = prevTestCaseVar != null ? prevTestCaseVar : userData.getTestCase(); 
            String sourceIteration = prevIterationVar != null ? prevIterationVar : userData.getIteration(); 
            String sourceSubIteration = prevSubIterationVar != null ? prevSubIterationVar : userData.getSubIteration();
            String sourceDataSheet = Condition;
            String sourceSheetName = sourceDataSheet.split(":",2)[0];
            String sourceColumnName = sourceDataSheet.split(":",2)[1];
            String reportDescription = "";
            String value = userData.getData(sourceSheetName, sourceColumnName, sourceScenario, sourceTestCase,
              sourceIteration, sourceSubIteration);

            if (Input.matches("%.*%")) { 
             addVar(Input, value);
             reportDescription = Input.replaceAll("%", "");
            } else { 
                String targetDataSheet = Input;
                String targetSheetName = targetDataSheet.split(":",2)[0];
                String targetColumnName = targetDataSheet.split(":",2)[1];
                reportDescription = targetColumnName;
                userData.putData(targetSheetName, targetColumnName, value, userData.getScenario(), userData.getTestCase(),
                userData.getIteration(), userData.getSubIteration());
            } 
            Report.updateTestLog(reportDescription, "Value [" + value + "] is successfully stored to [" + Input + "]", Status.DONE);  
        }
    }
    
    /**
     * Reset required variables for storeDataFromPreviousTestCaseData action to null 
     *  <ul>
     *     <li>{@code %PreviousScenario%}</li>
     *     <li>{@code %PreviousTestCase%}</li>
     *     <li>{@code %PreviousIteration%}</li>
     *     <li>{@code %PreviousSubIteration%}</li>
     * </ul>
     */
    @Action(object = ObjectType.GENERAL, desc = "Reset Required Variables for storeDataFromPreviousTestCaseData action", input = InputType.OPTIONAL)
    public void resetPreviousTestCaseDataVariables() {
        // Reset Variables
        addVar("%PreviousScenario%", null);
        addVar("%PreviousTestCase%", null);
        addVar("%PreviousIteration%", null);
        addVar("%PreviousSubIteration%", null);
        
        Report.updateTestLog("resetPreviousTestCaseDataVariables", " Variables %PreviousScenario%, %PreviousTestCase%, %PreviousIteration% and %PreviousSubIteration% has been reset." + Input, Status.DONE);  
    }
    
    @Action(object = ObjectType.GENERAL, desc = "store in Global Datasheet", input = InputType.YES, condition = InputType.YES)
    public void storeInGlobalDataSheet() {
        if (Condition != null) {

            String globalDataID = Condition.split(":")[0];
            String globalcolumnName = Condition.split(":")[1];
            userData.putGlobalData("#"+globalDataID, globalcolumnName, Data);
            Report.updateTestLog(Action,
                    "Global Value: " + Data + " has been stored into " + "the Global data sheet", Status.DONE);
        } else {
            Report.updateTestLog(Action, "Incorrect input format", Status.DEBUG);
            System.out.println("Incorrect input format " + Condition);
        }
    }
}
