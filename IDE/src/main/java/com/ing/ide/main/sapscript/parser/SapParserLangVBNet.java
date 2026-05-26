package com.ing.ide.main.sapscript.parser;

/**
 * SAP GUI Script parser for VB.NET (.vb) files.
 * VB.NET can access SAP GUI Scripting COM API via Interop.
 * 
 * Language features:
 * - Comments: ' or REM
 * - No session variable prefix
 * - Similar syntax to VBScript but with .NET features
 * - Case insensitive
 * - Parentheses optional for method calls without parameters
 * 
 * Example:
 * session.findById("wnd[0]/usr/txtRSYST-BNAME").text = "TESTUSER"
 * session.findById("wnd[0]").sendVKey(0)
 */
public class SapParserLangVBNet extends SapLanguageParser {
    
    @Override
    public String getLanguageName() {
        return "VB.NET";
    }
    
    @Override
    public String[] getSupportedExtensions() {
        return new String[]{"vb"};
    }
    
    @Override
    protected boolean isComment(String line) {
        return line.startsWith("'") || line.toUpperCase().startsWith("REM ");
    }
    
    @Override
    protected String getSessionPrefix() {
        return ""; // VB.NET doesn't use a prefix
    }
}
