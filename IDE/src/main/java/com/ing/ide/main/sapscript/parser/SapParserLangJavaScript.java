package com.ing.ide.main.sapscript.parser;

/**
 * SAP GUI Script parser for JavaScript (.js) files.
 * JavaScript is another native format for SAP Script Tracker/Recorder.
 *
 * Language features:
 * - Comments: // or /* *\/
 * - No session variable prefix
 * - Parentheses required for method calls
 * - Case sensitive
 *
 * Example:
 * session.findById("wnd[0]/usr/txtRSYST-BNAME").text = "TESTUSER";
 * session.findById("wnd[0]").sendVKey(0);
 */
public class SapParserLangJavaScript extends SapLanguageParser {
    private boolean inBlockComment = false;

    @Override
    public String getLanguageName() {
        return "JavaScript";
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "js" };
    }

    @Override
    protected boolean isComment(String line) {
        // Handle block comments
        if (inBlockComment) {
            if (line.contains("*/")) {
                inBlockComment = false;
            }
            return true;
        }

        if (line.startsWith("/*")) {
            if (!line.contains("*/")) {
                inBlockComment = true;
            }
            return true;
        }

        return line.startsWith("//") || line.startsWith("*");
    }

    @Override
    protected String getSessionPrefix() {
        return ""; // JavaScript doesn't use a prefix
    }
}
