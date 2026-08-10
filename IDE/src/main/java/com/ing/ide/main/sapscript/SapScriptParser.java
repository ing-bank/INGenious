package com.ing.ide.main.sapscript;

import com.ing.datalib.or.common.ObjectGroup;
import com.ing.datalib.or.sap.SapOR;
import com.ing.datalib.or.sap.SapORObject;
import com.ing.datalib.or.sap.SapORPage;
import com.ing.ide.main.mainui.AppMainFrame;
import com.ing.ide.main.sapscript.parser.SapLanguageParser;
import com.ing.ide.main.sapscript.parser.SapParserFactory;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Main orchestrator for SAP GUI Script parsing with language-specific parser delegation.
 *
 * This class uses language-specific parsers to extract SAP GUI commands from various
 * scripting languages and converts them into INGenious test cases and SAP Object Repository entries.
 *
 * Supported Languages:
 * - VBScript (.vbs, .vba) - SAP Script Tracker native format
 * - JavaScript (.js) - SAP Script Tracker native format
 * - PowerShell (.ps1) - Windows automation via COM
 * - Python (.py) - Automation via win32com/pywin32
 * - AutoIt (.au3) - Automation scripting
 *
 * Architecture:
 * 1. SapParserFactory selects the appropriate language parser based on file extension
 * 2. Language-specific parser extracts SAP objects and actions
 * 3. SapScriptParser generates Object Repository (YAML by default) and Test Cases (CSV)
 */
public class SapScriptParser {
    private static final Logger LOGGER = Logger.getLogger(SapScriptParser.class.getName());

    private final AppMainFrame sMainFrame;
    private final Map<String, String> filePath = new HashMap<>();
    private final Map<String, String> testCase = new HashMap<>();
    private Map<String, SapLanguageParser.SapObject> sapObjects = new LinkedHashMap<>();
    private List<SapLanguageParser.SapAction> sapActions = new ArrayList<>();

    // Track used object names to avoid duplicates
    private final Set<String> usedObjectNames = new HashSet<>();

    // Map from original object ID to generated unique name (to ensure consistency)
    private final Map<String, String> objectIdToNameCache = new HashMap<>();

    public SapScriptParser(AppMainFrame sMainFrame) {
        this.sMainFrame = sMainFrame;
    }

    /**
     * Validate input file before parsing.
     * Checks file size, readability, and content.
     */
    private void validateInputFile(File file) throws IOException {
        // Check file size
        long fileSize = file.length();
        if (fileSize > SapParserConstants.MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                String.format(
                    "File too large: %.2f MB. Maximum size: %s",
                    fileSize / (1024.0 * 1024.0),
                    SapParserConstants.MAX_FILE_SIZE_DISPLAY
                )
            );
        }

        // Check file is readable
        if (!file.canRead()) {
            throw new IOException(
                "Cannot read file: " + file.getAbsolutePath() + ". Check file permissions."
            );
        }

        // Check not empty
        if (fileSize == 0) {
            throw new IllegalArgumentException("File is empty: " + file.getName());
        }

        LOGGER.fine(
            String.format("File validation passed: %s (%.2f KB)", file.getName(), fileSize / 1024.0)
        );
    }

    /**
     * Parse a SAP GUI Script file (VBScript, JavaScript, PowerShell, Python, AutoIt, or custom).
     * The parser is language-agnostic and extracts SAP COM API calls regardless of source language.
     */
    public void parseSapScript(File file) throws Exception {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("SAP Script file does not exist: " + file);
        }

        // Validate file before processing
        validateInputFile(file);

        String extension = FilenameUtils.getExtension(file.getName()).toLowerCase();
        if (!SapParserFactory.isSupported(extension)) {
            LOGGER.warning(
                String.format(
                    "File extension '%s' is uncommon for SAP scripts. Supported: %s",
                    extension,
                    SapParserFactory.getSupportedExtensions()
                )
            );
        }

        try {
            System.out.println("Starting SAP Script import: " + file.getName());
            initializeFilePaths(file);

            // Clear object name tracking for new parse
            usedObjectNames.clear();
            objectIdToNameCache.clear();

            // Use factory to get language-specific parser
            SapLanguageParser languageParser = SapParserFactory.createParser(file);
            System.out.println("Using " + languageParser.getLanguageName() + " parser");

            // Parse the script file
            languageParser.parse(file);

            // Get parsed objects and actions
            sapObjects = languageParser.getSapObjects();
            sapActions = languageParser.getSapActions();

            generateObjectRepository();
            generateTestCase();

            System.out.println("Successfully imported SAP Script: " + file.getName());
            LOGGER.info(
                String.format(
                    "SAP Script parsing completed successfully. Created %d test steps.",
                    sapActions.size()
                )
            );

            cleanup();
        } catch (IOException ex) {
            String errorMsg =
                "Cannot read SAP script file: " +
                file.getName() +
                ". Ensure file exists and is readable.";
            LOGGER.log(Level.SEVERE, errorMsg, ex);
            System.err.println("File Error: " + errorMsg);
            throw new IOException(errorMsg, ex);
        } catch (IllegalArgumentException ex) {
            String errorMsg =
                "Invalid SAP script format in file: " + file.getName() + ". " + ex.getMessage();
            LOGGER.log(Level.SEVERE, errorMsg, ex);
            System.err.println("Validation Error: " + errorMsg);
            throw new IllegalArgumentException(errorMsg, ex);
        } catch (Exception ex) {
            String errorMsg =
                "Unexpected error importing SAP script: " + file.getName() + ". " + ex.getMessage();
            LOGGER.log(Level.SEVERE, errorMsg, ex);
            System.err.println("Import Error: " + errorMsg);
            throw new Exception(errorMsg, ex);
        }
    }

    private void initializeFilePaths(File file) {
        filePath.put("projectPath", sMainFrame.getProject().getLocation());
        filePath.put("importSapScriptFilePath", file.getAbsolutePath());
        String baseName = FilenameUtils.getBaseName(file.getName());
        testCase.put("fileName", StringUtils.capitalize(baseName));
        testCase.put("pageName", testCase.get("fileName"));

        // Test scenario path
        testCase.put(
            "testScenarioName",
            (filePath.get("projectPath") + "/TestPlan/" + testCase.get("fileName")).replace(
                    "\\",
                    "/"
                )
        );
        File testScenario = new File(testCase.get("testScenarioName"));
        if (!testScenario.exists()) {
            testScenario.mkdirs();
        }
        testCase.put("pageName", getUniqueName(testScenario, testCase.get("pageName")));
    }

    private void generateObjectRepository() throws Exception {
        // Use ObjectRepository pattern - get or create SapOR, add page, mark as unsaved
        generateSapORPage();
    }

    /**
     * Generate SAP Object Repository page following the same pattern as WebOR.
     * Gets the SapOR from the project's ObjectRepository, adds a new page with parsed objects,
     * and marks it as unsaved. The ObjectRepository will handle persisting to YAML/XML format.
     */
    private void generateSapORPage() throws Exception {
        LOGGER.info("Generating SAP Object Repository page");

        // Get SapOR from project's ObjectRepository
        SapOR sapOR = sMainFrame.getProject().getObjectRepository().getSapOR();
        if (sapOR == null) {
            // Create new SapOR if none exists
            sapOR = new SapOR(sMainFrame.getProject().getName());
            sapOR.setObjectRepository(sMainFrame.getProject().getObjectRepository());
            LOGGER.info("Created new SapOR for project");
        }

        // Check if page already exists and get unique name if needed
        String pageName = testCase.get("pageName");
        SapORPage existingPage = sapOR.getPageByName(pageName);
        if (existingPage != null) {
            // Generate unique page name
            int counter = 1;
            String uniqueName;
            do {
                uniqueName = pageName + "_" + counter++;
            } while (sapOR.getPageByName(uniqueName) != null);
            pageName = uniqueName;
            testCase.put("pageName", pageName);
            LOGGER.info("Page name already exists, using unique name: " + pageName);
        }

        // Create new page using SapOR's addPage method (same pattern as WebOR)
        SapORPage page = sapOR.addPage(pageName);
        if (page == null) {
            throw new Exception("Failed to create SAP OR page: " + pageName);
        }

        // Convert parsed objects to SapORObject instances and add to page
        for (SapLanguageParser.SapObject sapObj : sapObjects.values()) {
            String objectName = generateUniqueObjectName(sapObj.id);

            // Create ObjectGroup with single object (same pattern as WebOR)
            ObjectGroup<SapORObject> group = new ObjectGroup<>(objectName, page);
            SapORObject orObject = new SapORObject(objectName, group);

            // Set property values (id, name) - Text property should NOT be stored in OR
            // Text values are only used in test case Input column
            setObjectProperty(orObject, "id", sapObj.id);
            setObjectProperty(orObject, "name", sapObj.name != null ? sapObj.name : "");

            group.getObjects().add(orObject);
            page.getObjectGroups().add(group);
        }

        // Mark SapOR as unsaved - ObjectRepository will save it on next save() call
        sapOR.setSaved(false);

        // Save the ObjectRepository (which will write SapOR in YAML or XML based on project settings)
        sMainFrame.getProject().getObjectRepository().save();

        // Reload SAP OR trees in UI to show newly created page
        // Use load() to create fresh tree models from the updated in-memory SapOR
        if (sMainFrame.isTestDesign()) {
            sMainFrame.getTestDesign().getObjectRepo().getSapOR().load();
        }

        LOGGER.info("SAP OR page created with " + sapObjects.size() + " objects: " + pageName);
        System.out.println(
            "Created SAP OR page in project format with " + sapObjects.size() + " objects"
        );
    }

    /**
     * Helper method to set property value on SapORObject.
     */
    private void setObjectProperty(SapORObject object, String propertyName, String value) {
        object
            .getAttributes()
            .stream()
            .filter(attr -> attr.getName().equals(propertyName))
            .findFirst()
            .ifPresent(attr -> attr.setValue(value));
    }

    private void generateTestCase() throws IOException {
        LOGGER.info("Generating test case from SAP actions");

        String testCasePath =
            testCase.get("testScenarioName") + "/" + testCase.get("pageName") + ".csv";
        File testCaseFile = new File(testCasePath);

        try (PrintWriter writer = new PrintWriter(testCaseFile)) {
            // Write CSV header
            writer.println("Step,ObjectName,Description,Action,Input,Condition,Reference");

            int stepNo = 1;

            for (SapLanguageParser.SapAction action : sapActions) {
                if (action.actionType.equals("Transaction")) {
                    // Transaction action - no object reference needed
                    String stepName = "Execute Transaction " + action.value;
                    writer.println(
                        String.format(
                            "%d,%s,%s,%s,%s,%s,%s",
                            stepNo++,
                            "SAP_SYSTEM",
                            stepName,
                            "executeTransaction",
                            action.value,
                            "",
                            ""
                        )
                    );
                } else {
                    String objectName = generateUniqueObjectName(action.objectId);
                    String stepName = action.actionType + " [<Object>]";
                    String sapAction = mapToINGeniousAction(action.actionType);
                    String reference = "[Project] " + testCase.get("pageName");

                    // Add @ prefix whenever there's a value to reference static value from object
                    String data = action.value;
                    if (data != null && !data.isEmpty()) {
                        data = "@" + data;
                    }
                    data = escapeCSV(data);

                    writer.println(
                        String.format(
                            "%d,%s,%s,%s,%s,%s,%s",
                            stepNo++,
                            objectName,
                            stepName,
                            sapAction,
                            data,
                            "",
                            reference
                        )
                    );
                }
            }
        }

        LOGGER.info("Test case generated: " + testCaseFile.getAbsolutePath());
        System.out.println("Created test case with " + sapActions.size() + " steps");
    }

    /**
     * Map parsed SAP action types to INGenious SAP action method names.
     * Enhanced to support 64 total actions (17 existing + 47 new).
     *
     * @param sapActionType The action type extracted from SAP script
     * @return The corresponding INGenious SAPActions method name
     */
    private String mapToINGeniousAction(String sapActionType) {
        switch (sapActionType.toLowerCase()) {
            // ========== SESSION & TRANSACTION MANAGEMENT ==========
            case "transaction":
            case "starttransaction":
                return "sapExecuteTransaction";
            case "endtransaction":
                return "sapEndTransaction";
            case "refresh":
                return "sapRefreshSession";
            // ========== WINDOW MANAGEMENT ==========
            case "maximize":
                return "sapMaximizeWindow";
            case "minimize":
                return "sapMinimizeWindow";
            case "restore":
                return "sapRestoreWindow";
            case "close":
                return "sapCloseWindow";
            case "iconify":
                return "sapIconifyWindow";
            case "resizeworkingpane":
                return "sapResizeWorkingPane";
            // ========== TEXT FIELD ACTIONS ==========
            case "set":
                return "sapFill";
            case "selectall":
                return "sapSelectAllText";
            case "caretposition":
            case "cursorposition":
                return "sapSetCaretPosition";
            case "modified":
                return "sapSetModified";
            // ========== BUTTON ACTIONS ==========
            case "click":
            case "press":
                return "sapClick";
            case "pressbutton":
                return "sapPressButton";
            // ========== CHECKBOX & RADIO BUTTON ==========
            case "selectcheckbox":
            case "check":
                return "sapSelectCheckBox";
            case "selectradiobutton":
                return "sapSelectRadioButtonInRow";
            // ========== COMBOBOX/DROPDOWN ==========
            case "selectdropdownbytext":
                return "sapSelectDropDownByText";
            case "selectdropdownbykey":
            case "dropdownkey":
                return "sapSelectDropDownByKey";
            case "selectdropdownbyindex":
                return "sapSelectDropDownByIndex";
            case "opencombobox":
                return "sapOpenComboBox";
            case "closecombobox":
                return "sapCloseComboBox";
            // ========== TAB ACTIONS ==========
            case "select":
            case "selecttab":
                return "sapSelect";
            // ========== TABLE CONTROL ACTIONS ==========
            case "selectrow":
                return "sapSelectTableRow";
            case "setcurrentcell":
                return "sapSetCurrentCell";
            case "currentcellrow":
                return "sapSetCurrentCellRow";
            case "getcellvalue":
                return "sapGetCellValue";
            case "modifycell":
                return "sapModifyCell";
            case "clickcurrentcell":
                return "sapClickCurrentCell";
            case "doubleclickcell":
            case "doubleclickcurrentcell":
                return "sapDoubleClickCell";
            case "verticalscrollbarposition":
                return "sapSetVerticalScrollPosition";
            // ========== GRID/ALV ACTIONS ==========
            case "gridselectrow":
                return "sapSelectGridRow";
            case "gridselectcolumn":
                return "sapSelectGridColumn";
            case "gridsetcurrentcell":
                return "sapSetGridCurrentCell";
            case "gridgetcellvalue":
                return "sapGetGridCellValue";
            case "gridclickcurrentcell":
                return "sapClickGridCurrentCell";
            case "presstoolbarbutton":
                return "sapPressToolbarButton";
            case "presstoolbarcontextbutton":
                return "sapPressToolbarContextButton";
            case "gridselectall":
                return "sapSelectAllGrid";
            case "deselectrow":
                return "sapDeselectRow";
            case "clearselection":
                return "sapClearSelection";
            case "setfilter":
                return "sapSetGridFilter";
            case "clearfilter":
                return "sapClearGridFilter";
            case "selectedrows":
                return "sapSetSelectedRows";
            case "currentcellcolumn":
                return "sapSetCurrentCellColumn";
            case "firstvisiblecolumn":
                return "sapSetFirstVisibleColumn";
            case "firstvisiblerow":
                return "sapSetFirstVisibleRow";
            // ========== MENU & TOOLBAR ==========
            case "menuselect":
                return "sapSelectMenuItem";
            // ========== CONTEXT MENU ==========
            case "presscontextbutton":
                return "sapPressContextButton";
            case "selectcontextmenuitem":
                return "sapSelectContextMenuItem";
            // ========== TREE CONTROL ==========
            case "expandnode":
                return "sapExpandTreeNode";
            case "collapsenode":
                return "sapCollapseTreeNode";
            case "selectnode":
                return "sapSelectTreeNode";
            case "doubleclicknode":
                return "sapDoubleClickTreeNode";
            case "topnode":
                return "sapSetTopNode";
            // ========== STATUS BAR ==========
            case "getstatusbartext":
                return "sapGetStatusBarText";
            // ========== GENERAL ELEMENT ACTIONS ==========
            case "setfocus":
                return "sapSetFocus";
            case "doubleclick":
                return "sapDoubleClick";
            case "sendvkey":
                return "sapSimulateKeyPress";
            // ========== DYNAMIC PROPERTY SETTERS ==========
            default:
                if (sapActionType.toLowerCase().startsWith("setproperty_")) {
                    return "manual_review";
                }
                return "manual_review";
        }
    }

    private String generateObjectName(String id) {
        // Extract readable name from SAP ID
        // Example: wnd[0]/usr/txtRSYST-BNAME -> RSYST_BNAME
        String name = id;

        // Get last segment after final /
        int lastSlash = name.lastIndexOf('/');
        if (lastSlash >= 0) {
            name = name.substring(lastSlash + 1);
        }

        // Keep prefixes (txt, btn, cbo, etc.) for clarity and SAP convention alignment

        // Replace - with _
        name = name.replace("-", "_");

        // Remove brackets and special characters
        name = name.replaceAll("[\\[\\]()]", "");

        // Remove other special characters
        name = name.replaceAll("[^a-zA-Z0-9_]", "");

        // Ensure it starts with a letter
        if (!name.isEmpty() && !Character.isLetter(name.charAt(0))) {
            name = "obj_" + name;
        }

        // If empty, generate generic name
        if (name.isEmpty()) {
            name = "SAPObject_" + (sapObjects.size() + 1);
        }

        return name;
    }

    /**
     * Generate a unique object name, handling collisions by appending a counter.
     * Prevents duplicate object names in the Object Repository.
     * Uses caching to ensure the same ID always returns the same unique name.
     */
    private String generateUniqueObjectName(String id) {
        // Check cache first - return cached name if already generated
        if (objectIdToNameCache.containsKey(id)) {
            return objectIdToNameCache.get(id);
        }

        String baseName = generateObjectName(id);
        String uniqueName = baseName;
        int counter = 1;

        // Ensure uniqueness
        while (usedObjectNames.contains(uniqueName)) {
            uniqueName = baseName + "_" + counter++;
        }

        usedObjectNames.add(uniqueName);
        objectIdToNameCache.put(id, uniqueName);
        LOGGER.fine("Generated unique object name: " + uniqueName + " from " + id);
        return uniqueName;
    }

    private String getUniqueName(File directory, String baseName) {
        String uniqueName = baseName;
        int counter = 1;
        File testFile = new File(directory, uniqueName + ".csv");

        while (testFile.exists()) {
            uniqueName = baseName + "_" + counter;
            testFile = new File(directory, uniqueName + ".csv");
            counter++;
        }

        return uniqueName;
    }

    private String escapeCSV(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private void cleanup() {
        sapObjects.clear();
        sapActions.clear();
        testCase.clear();
        filePath.clear();
    }
}
