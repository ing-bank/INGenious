package com.ing.ide.main.sapscript.parser;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SAP GUI Script parser for AutoIt (.au3) files.
 * AutoIt can access SAP GUI Scripting COM API via ObjCreate.
 *
 * Language features:
 * - Comments: ;
 * - Session variable prefix: $
 * - Parentheses optional for methods with no arguments
 * - Case insensitive
 * - Boolean values: True, False
 *
 * Example:
 * $session.findById("wnd[0]/usr/txtRSYST-BNAME").text = "TESTUSER"
 * $session.findById("wnd[0]/usr/txtRSYST-LANGU").setFocus
 * $session.findById("wnd[0]").sendVKey(0)
 */
public class SapParserLangAutoIt extends SapLanguageParser {

    @Override
    public String getLanguageName() {
        return "AutoIt";
    }

    @Override
    public String[] getSupportedExtensions() {
        return new String[] { "au3" };
    }

    @Override
    protected boolean isComment(String line) {
        return line.startsWith(";");
    }

    @Override
    protected String getSessionPrefix() {
        return "$"; // AutoIt uses $ prefix for variables
    }

    /**
     * Override to handle AutoIt's optional parentheses for setFocus.
     * AutoIt allows calling methods without parentheses when they have no arguments.
     */
    @Override
    protected boolean parseSetFocusAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        // Try with optional parentheses: .setFocus or .setFocus()
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.setFocus(?:\\(\\))?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Element", transaction);
            sapActions.add(new SapAction("SetFocus", id, "", lineNumber));
            return true;
        }
        return false;
    }

    /**
     * Override to handle AutoIt's optional parentheses for press.
     * AutoIt allows calling methods without parentheses when they have no arguments.
     */
    @Override
    protected boolean parsePressAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        // Try with optional parentheses: .press or .press()
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\.findById\\(\"([^\"]+)\"\\)\\.press(?:\\(\\))?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Element", transaction);
            sapActions.add(new SapAction("Press", id, "", lineNumber));
            return true;
        }
        return false;
    }
}
