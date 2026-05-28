package com.ing.ide.main.sapscript.parser;

/**
 * SAP GUI Script parser for Python (.py) files.
 * Python can access SAP GUI Scripting COM API via win32com.client (pywin32).
 * 
 * Language features:
 * - Comments: #
 * - No session variable prefix
 * - Parentheses required for method calls
 * - Case sensitive
 * - Boolean values: True, False
 * 
 * Example:
 * session.findById("wnd[0]/usr/txtRSYST-BNAME").text = "TESTUSER"
 * session.findById("wnd[0]").sendVKey(0)
 */
public class SapParserLangPython extends SapLanguageParser {
    
    @Override
    public String getLanguageName() {
        return "Python";
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{"py"};
    }
    
    @Override
    protected boolean isComment(String line) {
        return line.startsWith("#");
    }
    
    @Override
    protected String getSessionPrefix() {
        return ""; // Python doesn't use a prefix
    }
}
