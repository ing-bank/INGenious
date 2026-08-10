package com.ing.ide.main.sapscript.parser;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FilenameUtils;

/**
 * Factory class to create the appropriate SAP language parser based on file extension.
 * Maps file extensions to their corresponding parser implementations.
 */
public class SapParserFactory {
    private static final Map<String, Class<? extends SapLanguageParser>> PARSER_MAP = new HashMap<>();

    static {
        // Register VBScript parser
        // PARSER_MAP.put("vbs", SapParserLangVBScript.class);
        // PARSER_MAP.put("vba", SapParserLangVBScript.class);

        // Register JavaScript parser
        // PARSER_MAP.put("js", SapParserLangJavaScript.class);

        // Register PowerShell parser
        PARSER_MAP.put("ps1", SapParserLangPowerShell.class);

        // Register Python parser
        // PARSER_MAP.put("py", SapParserLangPython.class);

        // Register AutoIt parser
        // PARSER_MAP.put("au3", SapParserLangAutoIt.class);

        // Register C# parser
        // PARSER_MAP.put("cs", SapParserLangCSharp.class);

        // Register VB.NET parser
        // PARSER_MAP.put("vb", SapParserLangVBNet.class);

        // Register Java parser
        PARSER_MAP.put("java", SapParserLangJava.class);
        PARSER_MAP.put("jsh", SapParserLangJava.class);
    }

    /**
     * Create a parser for the given file based on its extension.
     *
     * @param file SAP script file
     * @return Language-specific SAP parser
     * @throws IllegalArgumentException if file extension is not supported
     */
    public static SapLanguageParser createParser(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("SAP Script file does not exist: " + file);
        }

        String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();

        Class<? extends SapLanguageParser> parserClass = PARSER_MAP.get(extension);
        if (parserClass == null) {
            throw new IllegalArgumentException(
                String.format(
                    "Unsupported SAP script file extension: '.%s'. Supported: %s",
                    extension,
                    getSupportedExtensions()
                )
            );
        }

        try {
            return parserClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create parser for " + extension + " file", e);
        }
    }

    /**
     * Check if a file extension is supported.
     */
    public static boolean isSupported(String extension) {
        return PARSER_MAP.containsKey(extension.toLowerCase());
    }

    /**
     * Get a comma-separated list of supported extensions.
     */
    public static String getSupportedExtensions() {
        return String.join(", ", PARSER_MAP.keySet());
    }

    /**
     * Get the parser class name for a given extension.
     */
    public static String getParserName(String extension) {
        Class<? extends SapLanguageParser> parserClass = PARSER_MAP.get(extension.toLowerCase());
        return parserClass != null ? parserClass.getSimpleName() : "Unknown";
    }
}
