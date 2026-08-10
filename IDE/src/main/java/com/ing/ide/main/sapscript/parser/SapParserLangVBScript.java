package com.ing.ide.main.sapscript.parser;

/**
 * SAP GUI Script parser for VBScript (.vbs) files.
 * VBScript is the native format for SAP Script Tracker/Recorder.
 *
 * Language features:
 * - Comments: ' or REM
 * - No session variable prefix
 * - Parentheses optional for method calls without parameters
 * - Case insensitive
 *
 * Example:
 * session.findById("wnd[0]/usr/txtRSYST-BNAME").text = "TESTUSER"
 * session.findById("wnd[0]").sendVKey 0
 */
public class SapParserLangVBScript extends SapLanguageParser {

    @Override
    public String getLanguageName() {
        return "VBScript";
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "vbs", "vba" };
    }

    @Override
    protected boolean isComment(String line) {
        return line.startsWith("'") || line.toUpperCase().startsWith("REM ");
    }

    @Override
    protected String getSessionPrefix() {
        return ""; // VBScript doesn't use a prefix
    }
}
