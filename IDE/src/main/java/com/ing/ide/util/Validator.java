package com.ing.ide.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validator {
    private static final String EXCLUDE_LIST = "\\S*[,|#|$|{|}|^|\\[|\\]|%]\\S*";

    // Stricter list for reusable component names: blocks comma, dot, colon, brackets, percent, hash
    private static final String REUSABLE_EXCLUDE_LIST = "\\S*[,|\\.|:|\\[|\\]|%|#]\\S*";

    public static boolean isValidName(String text) {
        Pattern pattern = Pattern.compile(
            "# Match a valid Windows filename (unspecified file system).          \n" +
            "^                                    # Anchor to start of string.        \n" +
            "(?!                                  # Assert filename is not: CON, PRN, \n" +
            "  (?:                                # AUX, NUL, COM1, COM2, COM3, COM4, \n" +
            "    CON|PRN|AUX|NUL|                 # COM5, COM6, COM7, COM8, COM9,     \n" +
            "    COM[1-9]|LPT[1-9]                # LPT1, LPT2, LPT3, LPT4, LPT5,     \n" +
            "  )                                  # LPT6, LPT7, LPT8, and LPT9...     \n" +
            "  (?:\\.[^.]*)?                      # followed by optional extension    \n" +
            "  $                                  # and end of string                 \n" +
            ")                                    # End negative lookahead assertion. \n" +
            "[^<>:\"/\\\\|?*\\x00-\\x1F]*         # Zero or more valid filename chars.\n" +
            "[^<>:\"/\\\\|?*\\x00-\\x1F\\ .]      # Last char is not a space or dot.  \n" +
            "$                                    # Anchor to end of string.            ",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.COMMENTS
        );
        Matcher matcher = pattern.matcher(text);
        return matcher.matches() && !text.matches(EXCLUDE_LIST);
    }

    /**
     * Validates a reusable scenario or test case name.
     * Enforces stricter character restrictions than regular names:
     * Blocks comma, dot, colon, brackets, percent, and hash to avoid conflicts with
     * scoped reference syntax ([Project]/[Shared] Scenario:TestCase).
     *
     * @param text the name to validate
     * @return true if the name is valid for reusable components, false otherwise
     */
    public static boolean isValidReusableName(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        // Must pass basic name validation AND additional reusable-specific restrictions
        return isValidName(text) && !text.matches(REUSABLE_EXCLUDE_LIST);
    }
}
