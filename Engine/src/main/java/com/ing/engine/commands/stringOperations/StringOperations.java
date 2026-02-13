package com.ing.engine.commands.stringOperations;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Provides string manipulation operations for test automation.
 * This class extends General and offers various string operations such as concatenation,
 * trimming, substring extraction, replacement, case conversion, splitting, and more.
 * All operations store their results in variables that can be referenced in test cases.
 * 
 * @author Julie Ann Ayap
 */
public class StringOperations extends General {
    
    private final int limit = 5;
    private List<String> concatList = new ArrayList();
    private List<String> subStringList = new ArrayList();
    private List<String> getReplaceList = new ArrayList();
    private List<String> getSplitList = new ArrayList();
    private List<String> getOccurenceList = new ArrayList();

    /**
     * Constructs a new StringOperations instance with the specified CommandControl.
     * 
     * @param cc the CommandControl instance for managing test execution commands
     */
    public StringOperations(CommandControl cc) {
        super(cc);
    }
   
    /**
     * Retrieves the value of a string argument based on its format.
     * Supports three formats:
     * - Variables: %variableName%
     * - Datasheet references: {sheet:column}
     * - String literals: "text"
     * 
     * @param strArg the string argument to process
     * @return the resolved value of the argument, or empty string if format is invalid
     */
    private String getVarValue(String strArg){
        if (strArg.matches("%.*%")) 
            return getVar(strArg);
        else if (strArg.matches("^\\{.*:.*\\}")) 
            return getDatasheet(strArg);
        else if (strArg.matches("\".*\"")) 
            return strArg.substring( 1, strArg.length() - 1 );
        else 
            return "";
    }
    
    /**
     * Checks if a string represents a valid numeric value.
     * 
     * @param str the string to check
     * @return true if the string is a valid number, false otherwise
     */
    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        try {
            Double.parseDouble(str); // Or Integer.parseInt(str), etc.
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
    /**
     * Counts the number of occurrences of a specific character in a text string.
     * 
     * @param text the text to search in
     * @param targetChar the character to count
     * @return the number of times the target character appears in the text
     */
    public static int countCharOccurrences(String text, char targetChar) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == targetChar) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Concatenates multiple string inputs and stores the result in a variable.
     * Accepts up to 5 string inputs separated by commas.
     * Supports variables (%var%), datasheet references ({sheet:column}), and string literals ("text").
     * The concatenated result is stored in the variable specified in the Condition field.
     * 
     * <p>Example usage:
     * <ul>
     *   <li>Input: "Hello",%text%,{data:greeting}</li>
     *   <li>Condition: %result%</li>
     * </ul>
     * 
     * @throws ForcedException if input exceeds the limit of 5 strings or contains invalid format
     */
    @Action(object = ObjectType.STRINGOPERATIONS, desc = "Concats String inputs within testcase [<Data>]", input = InputType.YES, condition = InputType.YES)
    public void Concat() {
        if(!Condition.isBlank()){
            concatList = CommandControl.smartCommaSplitter(Input);
            String concatStr = "";
            int count = 0;
            String s;
            boolean isInValidInput = false;
            for (String part : concatList) {
                s = this.getVarValue(part);
                if (count >= limit) {
                    Report.updateTestLog("Concat", "Input ["+ Data +"] not added to variable " + Condition + ". String value exceeds expected limit: " + limit +".", Status.FAIL);
                    isInValidInput = !isInValidInput;
                    concatStr = "";
                    break;
                } else if(s.equals("")) {
                    Report.updateTestLog("Concat", "Input ["+ Data +"] not added to variable " + Condition + ". String contains invalid input format.", Status.FAIL);
                    isInValidInput = !isInValidInput;
                    break;
                } else {
                    concatStr = concatStr.concat(s);
                }
                count++;
            }

            if (!isInValidInput && !concatStr.equals("")) {
                addVar(Condition, concatStr);
                if (getVar(Condition) != null)
                    Report.updateTestLog("Concat", "Input ["+ Data +"] concatenated. Output [" + concatStr + "] stored into variable " + Condition +".", Status.DONE);
            }
        } else {
            Report.updateTestLog("Concat", "Input ["+ Data +"] not added to variable. No variable name assigned to operation.", Status.FAIL);
        }
    } 
    
    /**
     * Removes leading and trailing whitespace from a string input.
     * Supports variables (%var%), datasheet references ({sheet:column}), and string literals ("text").
     * The trimmed result is stored in the variable specified in the Condition field.
     * 
     * <p>Example usage:
     * <ul>
     *   <li>Input: "  Hello World  " or %text% or {data:greeting}</li>
     *   <li>Condition: %trimmedText%</li>
     *   <li>Result: "Hello World"</li>
     * </ul>
     * 
     * @throws ForcedException if input format is invalid or no variable name is assigned
     */
    @Action(object = ObjectType.STRINGOPERATIONS, desc = "Trim white spaces of String input within testcase", input = InputType.YES, condition = InputType.YES)
    public void Trim() {
        if(!Condition.isBlank()){
            String s = getVarValue(Input);
            String strTrimmed = "";
            boolean isInValidInput = false;
            if(s.equals("")) {
                Report.updateTestLog("Trim", "Input ["+ Data +"] not added to variable " + Condition +". String contains invalid input format.", Status.FAIL);
                isInValidInput = !isInValidInput;
            } else {
                strTrimmed = s.trim();
            }

            if (!isInValidInput && !strTrimmed.equals("")) {
                addVar(Condition, strTrimmed);
                if (getVar(Condition) != null)
                    Report.updateTestLog("Trim", "Input ["+ s +"] trimmed. Output [" + strTrimmed + "] stored into variable " + Condition + ".", Status.DONE);
            }
        } else {
            Report.updateTestLog("Trim", "Input ["+ Data +"] not added to variable. No variable name assigned to operation.", Status.FAIL);
        }
    } 
        
    /**
     * Extracts a substring from a string based on start and end indices.
     * Accepts 2 or 3 parameters: string, startIndex, and optionally endIndex.
     * If endIndex is not provided, extracts until the end of the string.
     * Supports variables (%var%), datasheet references ({sheet:column}), and string literals ("text").
     * The substring result is stored in the variable specified in the Condition field.
     * 
     * <p>Example usage:
     * <ul>
     *   <li>Input: "Hello World", 0, 5 (extracts "Hello")</li>
     *   <li>Input: %text%, 6 (extracts from index 6 to end from ther )</li>
     *   <li>Input: {data:greeting}, 6 (extracts from index 6 to end)</li>
     *   <li>Condition: %result%</li>
     * </ul>
     * 
     * @throws ForcedException if indices are not numeric, out of bounds, or input format is invalid
     */
    @Action(object = ObjectType.STRINGOPERATIONS, desc = "Substring String input within testcase", input = InputType.YES, condition = InputType.YES)
    public void Substring() {
        if(!Condition.isBlank()){
            String strSubstring = "";
            String s = "";
            boolean isInValidInput = false;
            subStringList = CommandControl.smartCommaSplitter(Input);
            if(subStringList.size() == 2 || subStringList.size() == 3){
                s = getVarValue(subStringList.get(0));
                String s2 = getVarValue(subStringList.get(1).trim()) ;
                String s3 = subStringList.size() == 3 ? getVarValue(subStringList.get(2).trim()) : String.valueOf(s.length());
                if (isNumeric(s2) && isNumeric(s3)){
                    int firstIndex = Integer.parseInt(s2);
                    int secondIndex = Integer.parseInt(s3);
                    if(s.equals("")) {
                        Report.updateTestLog("Substring", "Input ["+ Data +"] not added to variable " + Condition +". String contains invalid input format.", Status.FAIL);
                        isInValidInput = !isInValidInput;
                    } else {
                        strSubstring = s.substring(firstIndex, secondIndex);
                    }
                } else {
                        Report.updateTestLog("Substring", "Input ["+ Data +"] not added to variable " + Condition +". String parameter should be instance of a number.", Status.FAIL);
                        isInValidInput = !isInValidInput;
                    }
            } else {
                Report.updateTestLog("Substring", "Input ["+ Data +"] not added to variable " + Condition +". String index out of bound.", Status.FAIL);
                isInValidInput = !isInValidInput;
            }

            if (!isInValidInput && !strSubstring.equals("")) {
                addVar(Condition, strSubstring);
                if (getVar(Condition) != null)
                    Report.updateTestLog("Substring", "Substring ["+ strSubstring +"] of string " + s + " stored into variable " + Condition + ".", Status.DONE);
            }
        } else {
            Report.updateTestLog("Substring", "Input ["+ Data +"] not added to variable. No variable name assigned to operation.", Status.FAIL);
        }
    } 
        
    /**
     * Replaces occurrences of a substring within a string.
     * Accepts 3 or 4 parameters: string, searchText, replacementText, and optionally replaceType.
     * ReplaceType can be "first" (replace first occurrence) or "all" (replace all occurrences).
     * If replaceType is not provided, it must be explicitly specified.
     * Supports variables (%var%), datasheet references ({sheet:column}), and string literals ("text").
     * The result is stored in the variable specified in the Condition field.
     * 
     * <p>Example usage:
     * <ul>
     *   <li>Input: "Hello World", "World", "Universe", "first"</li>
     *   <li>Input: %text%, "old", "new", "all"</li>
     *   <li>Input: {data:greeting}, "old", "new", "all"</li>
     *   <li>Condition: %result%</li>
     * </ul>
     * 
     * @throws ForcedException if replaceType is not "first" or "all", or input format is invalid
     */
    @Action(object = ObjectType.STRINGOPERATIONS, desc = "Replace String input within testcase", input = InputType.YES, condition = InputType.YES)
    public void Replace() {
        if(!Condition.isBlank()){
            String strNewReplace = "";
            String s = "";
            String s2 = "";
            String s3 = "";
            String s4 = "";
            boolean isInValidInput = false;
            getReplaceList = CommandControl.smartCommaSplitter(Input);
            if(getReplaceList.size() == 3 || getReplaceList.size() == 4 ){
                s = getVarValue(getReplaceList.get(0));
                s2 = getVarValue(getReplaceList.get(1));
                s3 = getVarValue(getReplaceList.get(2));
                s4 = getReplaceList.size() == 4 ? getVarValue(getReplaceList.get(3)) : "";
                if(s.equals("")) {
                    Report.updateTestLog("Replace", "Input ["+ Data +"] not added to variable "+ Condition +". String contains invalid input format.", Status.FAIL);
                    isInValidInput = !isInValidInput;
                } else {
                    if(s4.equals("first") || s4.equals("all")) {
                        strNewReplace = s4.equals("first") ? s.replaceFirst(s2, s3) : s.replaceAll(s2, s3);
                    } else {
                        Report.updateTestLog("Replace", "Input ["+ Data +"] not added to variable "+ Condition +". Type of replace should be 'first' or 'all'.", Status.FAIL);
                        isInValidInput = !isInValidInput;
                    }
                }
            } else {
                Report.updateTestLog("Replace", "Input ["+ Data +"] not added to variable "+ Condition +". String index out of bound.", Status.FAIL);
                isInValidInput = !isInValidInput;
            }

            if (!isInValidInput && !strNewReplace.equals("")) {
                addVar(Condition, strNewReplace);
                if (getVar(Condition) != null && !isInValidInput)   {
                    String report;
                    if(s4.equals("all")) 
                        report = "All instances of [" + s2 + "] in string [" + s + "] replaced with [" + s3 + "]. New value [" + strNewReplace + "] stored into variable " + Condition + ".";
                    else 
                        report = "First instance of [" + s2 + "] in string [" + s + "] replaced with [" + s3 + "]. New value [" + strNewReplace + "] stored into variable " + Condition + ".";
                    Report.updateTestLog("Replace", report, Status.DONE);
                }
            }
        } else {
            Report.updateTestLog("Replace", "Input ["+ Data +"] not added to variable. No variable name assigned to operation.", Status.FAIL);
        }
    } 
        
    /**
     * Converts a string input to lowercase.
     * Supports variables (%var%), datasheet references ({sheet:column}), and string literals ("text").
     * The lowercase result is stored in the variable specified in the Condition field.
     * 
     * <p>Example usage:
     * <ul>
     *   <li>Input: "HELLO WORLD" or %text% or {data:greeting}</li>
     *   <li>Condition: %lowerText%</li>
     *   <li>Result: "hello world"</li>
     * </ul>
     * 
     * @throws ForcedException if input format is invalid or no variable name is assigned
     */
    @Action(object = ObjectType.STRINGOPERATIONS, desc = "Converts String input to lower case within testcase", input = InputType.YES, condition = InputType.YES)
    public void ToLower() {
        if(!Condition.isBlank()){
            String s = getVarValue(Input);
            String strToLower = "";
            boolean isInValidInput = false;
            if(s.equals("")) {
                Report.updateTestLog("ToLower", "Input ["+ Data +"] not added to variable " + Condition +". String contains invalid input format.", Status.FAIL);
                isInValidInput = !isInValidInput;
            } else {
                strToLower = s.toLowerCase();
            }

            if (!isInValidInput && !strToLower.equals("")) {
                addVar(Condition, strToLower);
                if (getVar(Condition) != null)
                    Report.updateTestLog("ToLower", "Input [" + Data + "] converted to lower case. New value [" + strToLower + "] stored into variable " + Condition + ".", Status.DONE);
            }
        } else {
            Report.updateTestLog("ToLower", "Input ["+ Data +"] not added to variable. No variable name assigned to operation.", Status.FAIL);
        }
    } 
        
    /**
     * Converts a string input to uppercase.
     * Supports variables (%var%), datasheet references ({sheet:column}), and string literals ("text").
     * The uppercase result is stored in the variable specified in the Condition field.
     * 
     * <p>Example usage:
     * <ul>
     *   <li>Input: "hello world" or %text% or {data:greeting}</li>
     *   <li>Condition: %upperText%</li>
     *   <li>Result: "HELLO WORLD"</li>
     * </ul>
     * 
     * @throws ForcedException if input format is invalid or no variable name is assigned
     */
    @Action(object = ObjectType.STRINGOPERATIONS, desc = "Converts String input to upper case within testcase", input = InputType.YES, condition = InputType.YES)
    public void ToUpper() {
        if(!Condition.isBlank()){
            String s = getVarValue(Input);
            String strToUpper = "";
            boolean isInValidInput = false;
            if(s.equals("")) {
                Report.updateTestLog("ToUpper", "Input ["+ Data +"] not added to variable " + Condition + ". String contains invalid input format.", Status.FAIL);
                isInValidInput = !isInValidInput;
            } else {
                strToUpper = s.toUpperCase();
            }

            if (!isInValidInput && !strToUpper.equals("")) {
                addVar(Condition, strToUpper);
                if (getVar(Condition) != null)
                    Report.updateTestLog("ToUpper", "Input [" + Data + "] converted to upper case. New value [" + strToUpper + "] stored into variable " + Condition + ".", Status.DONE);
            }
        } else {
            Report.updateTestLog("ToUpper", "Input ["+ Data +"] not added to variable. No variable name assigned to operation.", Status.FAIL);
        }
    } 
        
    /**
     * Splits a string by a delimiter and retrieves a specific element from the result.
     * Supports variables (%var%), datasheet references ({sheet:column}), and string literals ("text").
     * Accepts 3 or 4 parameters: string, delimiter, index, and optionally limit.
     * The limit parameter controls the maximum number of splits (-1 for unlimited).
     * The element at the specified index is stored in the variable specified in the Condition field.
     * 
     * <p>Example usage:
     * <ul>
     *   <li>Input: "one,two,three", ",", 1 (returns "two")</li>
     *   <li>Input: %text%, ",", 1 (returns "two")</li>
     *   <li>Input: {data:string_value}, ",", 1 (returns "two")</li>
     *   <li>Condition: %result%</li>
     * </ul>
     * 
     * @throws ForcedException if index/limit are not numeric, index is out of bounds, or input format is invalid
     */
    @Action(object = ObjectType.STRINGOPERATIONS, desc = "Split String input within testcase", input = InputType.YES, condition = InputType.YES)
    public void Split() {
        if(!Condition.isBlank()){
            String strSplitted = "";
            String[] strSplittedArr = new String[0];
            boolean isInValidInput = false;
            int index = 0;
            getSplitList = CommandControl.smartCommaSplitter(Input);
            if(getSplitList.size() == 3 || getSplitList.size() == 4 ){
                String s = getVarValue(getSplitList.get(0));
                String s2 = getVarValue(getSplitList.get(1));
                String s3 = getVarValue(getSplitList.get(2).trim());
                String s4 = getSplitList.size() == 4 ? getVarValue(getSplitList.get(3).trim()) : "-1";
                if(isNumeric(s3) && isNumeric(s4)) {
                    index = Integer.parseInt(s3);
                    int limit = Integer.parseInt(s4);
                    if(s.equals("")) {
                        Report.updateTestLog("Split", "Input ["+ Data +"] not added to variable "+ Condition +". String contains invalid input format.", Status.FAIL);
                        isInValidInput = !isInValidInput;
                    } else {
                        strSplittedArr = limit == -1 ? s.split(s2) : s.split(s2, limit);
                        strSplitted = strSplittedArr.length-1 >= index ? strSplittedArr[index] : "";
                        if(strSplitted.equals("")) {
                            Report.updateTestLog("Split", "Input ["+ Data +"] not added to variable "+ Condition +". String index out of bound.", Status.FAIL);
                            isInValidInput = !isInValidInput;
                        }
                    }
                } else {
                        Report.updateTestLog("Split", "Input ["+ Data +"] not added to variable " + Condition + ". String parameter should be instance of a number.", Status.FAIL);
                        isInValidInput = !isInValidInput;
                    }
            } else {
                Report.updateTestLog("Split", "Input ["+ Data +"] not added to variable "+ Condition +". String index out of bound.", Status.FAIL);
                isInValidInput = !isInValidInput;
            }

            if (!isInValidInput && !strSplitted.equals("")) {
                addVar(Condition, strSplitted);
                if (getVar(Condition) != null)
                    Report.updateTestLog("Split", "Input ["+ Data +"] has been split into [" + strSplittedArr.length + "]. Index value [" + strSplitted + "] stored into variable " + Condition + ".", Status.DONE);
            }
        } else {
            Report.updateTestLog("Split", "Input ["+ Data +"] not added to variable. No variable name assigned to operation.", Status.FAIL);
        }
    } 
        
    /**
     * Counts the number of occurrences of a specific character in a string.
     * Accepts 2 parameters: string and character (must be a single character).
     * Supports variables (%var%), datasheet references ({sheet:column}), and string literals ("text").
     * The count is stored in the variable specified in the Condition field.
     * 
     * <p>Example usage:
     * <ul>
     *   <li>Input: "Hello World", "l" (returns 3)</li>
     *   <li>Input: %text%, "a"</li>
     *   <li>Input: {data:greeting}, "a"</li>
     *   <li>Condition: %count%</li>
     * </ul>
     * 
     * @throws ForcedException if second parameter is not a single character or input format is invalid
     */
    @Action(object = ObjectType.STRINGOPERATIONS, desc = "Get occurence of String input within testcase", input = InputType.YES, condition = InputType.YES)
    public void GetOccurence() {
        if(!Condition.isBlank()){
            String strGetOcc = "";
            String s = "";
            String s2 = "";
            boolean isInValidInput = false;
            getOccurenceList = CommandControl.smartCommaSplitter(Input);
            if(getOccurenceList.size() == 2){
                s = getVarValue(getOccurenceList.get(0));
                s2 = getVarValue(getOccurenceList.get(1)).trim();
                if(s2.length() == 1) {
                    char charToFind = s2.charAt(0);
                    if(s.equals("")) {
                        Report.updateTestLog("getOccurence", "Input ["+ Data +"] not added to variable "+ Condition +". String contains invalid input format.", Status.FAIL);
                        isInValidInput = !isInValidInput;
                    } else {
                        strGetOcc = Integer.toString(countCharOccurrences(s, charToFind));
                    }
                } else {
                    Report.updateTestLog("getOccurence", "Input ["+ Data +"] not added to variable "+ Condition +". String parameter should be single character to get occurence.", Status.FAIL);
                    isInValidInput = !isInValidInput;
                }
            } else {
                Report.updateTestLog("getOccurence", "Input ["+ Data +"] not added to variable "+ Condition +". String index out of bound.", Status.FAIL);
                isInValidInput = !isInValidInput;
            }

            if (!isInValidInput && !strGetOcc.equals("")) {
                addVar(Condition, strGetOcc);
                if (getVar(Condition) != null)
                    Report.updateTestLog("getOccurence", "Occurrences of character ["+ s2 +"] from string [" + s + "] stored into variable " + Condition + " with value [" + strGetOcc + "].", Status.DONE);
            }
        } else {
            Report.updateTestLog("getOccurence", "Input ["+ Data +"] not added to variable. No variable name assigned to operation.", Status.FAIL);
        }
    } 
        
    /**
     * Calculates the length of a string input.
     * The length is stored in the variable specified in the Condition field.
     * 
     * <p>Example usage:
     * <ul>
     *   <li>Input: "Hello World" or %text% or {data:greeting}</li>
     *   <li>Condition: %length%</li>
     * </ul>
     * 
     * @throws ForcedException if input format is invalid or no variable name is assigned
     */
    @Action(object = ObjectType.STRINGOPERATIONS, desc = "Get length of String input within testcase", input = InputType.YES, condition = InputType.YES)
    public void GetLength() {
        if(!Condition.isBlank()){
            String s = getVarValue(Input);
            String strGetLen = "";
            boolean isInValidInput = false;
            if(s.equals("")) {
                Report.updateTestLog("getLength", "Input ["+ Data +"] not added to variable " + Condition +". String contains invalid input format.", Status.FAIL);
                isInValidInput = !isInValidInput;
            } else {
                strGetLen = Integer.toString(s.length());
            }

            if (!isInValidInput && !strGetLen.equals("")) {
                addVar(Condition, strGetLen);
                if (getVar(Condition) != null)
                    Report.updateTestLog("getLength", "Length of ["+ Data +"] stored into variable " + Condition + " with value [" + strGetLen + "].", Status.DONE);
            }
        } else {
            Report.updateTestLog("getLength", "Input ["+ Data +"] not added to variable. No variable name assigned to operation.", Status.FAIL);
        }
    } 
    
}
