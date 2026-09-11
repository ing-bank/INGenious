package com.ing.ide.main.sapscript.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Abstract base class for language-specific SAP GUI Script parsers.
 * Each supported language (VBScript, JavaScript, PowerShell, Python, AutoIt)
 * extends this class to implement language-specific parsing rules.
 */
public abstract class SapLanguageParser {
    protected static final Logger LOGGER = Logger.getLogger(SapLanguageParser.class.getName());

    protected Map<String, SapObject> sapObjects = new LinkedHashMap<>();
    protected List<SapAction> sapActions = new ArrayList<>();

    // Statistics tracking
    protected int linesProcessed = 0;
    protected int linesParsed = 0;
    protected List<String> warnings = new ArrayList<>();

    // Phase 4: multi-session tracking. The recording's first session\d* variable is the
    // implicit primary (the one SAP.initConnection already opens) - every other variable seen
    // gets an invented label (s1, s2, ...) and a SAP.openSession / SAP.switchSession synthetic
    // action the moment the script switches to or away from it.
    private static final Pattern CON_SES_PREFIX = Pattern.compile(
        "^/?app/con\\[\\d+\\]/ses\\[(\\d+)\\]/"
    );
    protected String primarySessionVar;
    protected final Map<String, String> sessionVarLabels = new LinkedHashMap<>();
    protected final Set<String> seenSessionLabels = new LinkedHashSet<>();
    protected String currentSessionLabel;

    /**
     * Get the language name for this parser.
     */
    public abstract String getLanguageName();

    /**
     * Get the file extensions supported by this parser.
     */
    public abstract String[] getSupportedExtensions();

    /**
     * Check if a line is a comment in this language.
     */
    protected abstract boolean isComment(String line);

    /**
     * Get the session variable prefix for this language (e.g., "$" for PowerShell).
     */
    protected abstract String getSessionPrefix();

    /**
     * Parse a SAP script file and extract objects and actions.
     */
    public void parse(File file) throws IOException {
        LOGGER.info(
            "Parsing SAP Script file with " +
            getLanguageName() +
            " parser: " +
            file.getAbsolutePath()
        );

        // Reset statistics
        linesProcessed = 0;
        linesParsed = 0;
        warnings.clear();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int lineNumber = 0;
            String currentTransaction = null;
            int actionCountBefore;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                linesProcessed++;
                String trimmedLine = line.trim();

                // Skip empty lines and comments
                if (trimmedLine.isEmpty() || isComment(trimmedLine)) {
                    continue;
                }

                // Phase 4: note which session variable this line touches before anything else,
                // so every action recorded from it (including a transaction, below) is tagged
                // with the right session and a switch is emitted the moment it changes.
                trackSessionVariable(trimmedLine, lineNumber, getSessionPrefix());

                actionCountBefore = sapActions.size();

                // Extract transaction
                String transaction = extractTransaction(trimmedLine);
                if (transaction != null) {
                    currentTransaction = transaction;
                    LOGGER.fine("Found transaction: " + currentTransaction);
                    addAction(
                        new SapAction("Transaction", "SAP_SYSTEM", currentTransaction, lineNumber)
                    );
                    linesParsed++;
                    continue;
                }

                // Parse SAP GUI actions
                parseSapAction(trimmedLine, lineNumber, currentTransaction);

                // If an action was added, count this line as parsed
                if (sapActions.size() > actionCountBefore) {
                    linesParsed++;
                }
            }
        }

        LOGGER.info(
            String.format(
                "Parsed %d SAP objects and %d actions from %s script",
                sapObjects.size(),
                sapActions.size(),
                getLanguageName()
            )
        );
    }

    /**
     * Extract transaction code from a line (e.g., session.startTransaction("VA03")).
     */
    protected String extractTransaction(String line) {
        String prefix = getSessionPrefix();
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) +
            "session\\w*\\.startTransaction\\s*\\(?\\s*\"([^\"]+)\"\\s*\\)?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Parse a single line for SAP actions.
     * This method should be overridden by language-specific parsers if needed.
     */
    protected void parseSapAction(String line, int lineNumber, String transaction) {
        String prefix = getSessionPrefix();

        // Try to match setText action
        if (parseSetTextAction(line, lineNumber, transaction, prefix)) return;

        // Try to match press action
        if (parsePressAction(line, lineNumber, transaction, prefix)) return;

        // Try to match dropdown key selection
        if (parseDropdownKeyAction(line, lineNumber, transaction, prefix)) return;

        // Try to match dropdown select by index
        if (parseDropdownSelectAction(line, lineNumber, transaction, prefix)) return;

        // Try to match combo value action
        if (parseComboValueAction(line, lineNumber, transaction, prefix)) return;

        // Try to match select action
        if (parseSelectAction(line, lineNumber, transaction, prefix)) return;

        // Try to match tab select action
        if (parseTabSelectAction(line, lineNumber, transaction, prefix)) return;

        // Try to match setFocus action
        if (parseSetFocusAction(line, lineNumber, transaction, prefix)) return;

        // Try to match sendVKey action
        if (parseSendVKeyAction(line, lineNumber, transaction, prefix)) return;

        // Try to match double click on current cell
        if (parseDoubleClickCellAction(line, lineNumber, transaction, prefix)) return;

        // Try to match general doubleClick action
        if (parseDoubleClickAction(line, lineNumber, transaction, prefix)) return;

        // Try to match modifyCell action
        if (parseModifyCellAction(line, lineNumber, transaction, prefix)) return;

        // Try to match setCurrentCell action
        if (parseSetCurrentCellAction(line, lineNumber, transaction, prefix)) return;

        // Generic findById for objects not yet handled
        if (parseFindByIdAction(line, transaction, prefix)) return;

        // Try to capture general property assignments
        parsePropertyAssignment(line, prefix);
    }

    protected boolean parseSetTextAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) +
            "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.(?:text|Text)\\s*=\\s*\"([^\"]*)\"",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String value = matcher.group(2);
            String objType = determineObjectType(id);
            addSapObject(id, objType, transaction);
            // Text value should NOT be stored in SAP Object - it goes in test case Input column only
            // addPropertyToSapObject(id, "text", value); // REMOVED
            addAction(new SapAction("Set", id, value, lineNumber));
            return true;
        }
        return false;
    }

    protected boolean parsePressAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.press\\(\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Button", transaction);
            addAction(new SapAction("Click", id, "", lineNumber));
            return true;
        }
        return false;
    }

    protected boolean parseDropdownKeyAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) +
            "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.Key\\s*=\\s*\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String key = matcher.group(2);
            addSapObject(id, "ComboBox", transaction);
            addAction(new SapAction("SelectDropDownByKey", id, key, lineNumber));
            return true;
        }
        return false;
    }

    protected boolean parseDropdownSelectAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) +
            "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.Select\\s*\\(?\\s*(\\d+)\\s*\\)?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String index = matcher.group(2);
            addSapObject(id, "ComboBox", transaction);
            addAction(new SapAction("SelectDropDownByIndex", id, index, lineNumber));
            return true;
        }
        return false;
    }

    protected boolean parseComboValueAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) +
            "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.value\\s*=\\s*\"([^\"]*)\"",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String value = matcher.group(2);
            String objType = determineObjectType(id);
            if (objType.equals("ComboBox")) {
                addSapObject(id, "ComboBox", transaction);
                addAction(new SapAction("SelectDropDownByText", id, value, lineNumber));
                return true;
            }
        }
        return false;
    }

    protected boolean parseSelectAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) +
            "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.selected\\s*=\\s*(true|false|-?\\d+|\\$true|\\$false)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String selected = matcher.group(2);
            String objType = determineObjectType(id);

            if (objType.equals("Checkbox")) {
                addSapObject(id, "Checkbox", transaction);
                addAction(new SapAction("SelectCheckBox", id, selected, lineNumber));
            } else if (objType.equals("RadioButton")) {
                addSapObject(id, "RadioButton", transaction);
                addAction(new SapAction("SelectRadioButton", id, selected, lineNumber));
            } else if (objType.equals("Tab")) {
                addSapObject(id, "Tab", transaction);
                addAction(new SapAction("SelectTab", id, selected, lineNumber));
            } else {
                addSapObject(id, objType, transaction);
                addAction(new SapAction("Select", id, selected, lineNumber));
            }
            return true;
        }
        return false;
    }

    protected boolean parseTabSelectAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.select\\(\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Tab", transaction);
            addAction(new SapAction("SelectTab", id, "", lineNumber));
            return true;
        }
        return false;
    }

    protected boolean parseSetFocusAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.setFocus\\(\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Element", transaction);
            addAction(new SapAction("SetFocus", id, "", lineNumber));
            return true;
        }
        return false;
    }

    protected boolean parseSendVKeyAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) +
            "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.sendVKey\\s*\\(?\\s*(\\d+)\\s*\\)?",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String vkey = matcher.group(2);
            addSapObject(id, "Window", transaction);
            addAction(new SapAction("SendVKey", id, vkey, lineNumber));
            return true;
        }
        return false;
    }

    protected boolean parseDoubleClickCellAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) +
            "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.doubleClickCurrentCell",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Table", transaction);
            addAction(new SapAction("DoubleClickCell", id, "", lineNumber));
            return true;
        }
        return false;
    }

    protected boolean parseDoubleClickAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.doubleClick\\(\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Element", transaction);
            addAction(new SapAction("DoubleClick", id, "", lineNumber));
            return true;
        }
        return false;
    }

    protected boolean parseModifyCellAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) +
            "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.modifyCell\\s*\\(\\s*(\\d+)\\s*,\\s*\"([^\"]+)\"\\s*,\\s*\"([^\"]*)\"\\s*\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String row = matcher.group(2);
            String column = matcher.group(3);
            String value = matcher.group(4);
            addSapObject(id, "Table", transaction);
            addAction(
                new SapAction("ModifyCell", id, row + "," + column + "," + value, lineNumber)
            );
            return true;
        }
        return false;
    }

    protected boolean parseSetCurrentCellAction(
        String line,
        int lineNumber,
        String transaction,
        String prefix
    ) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) +
            "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.currentCellRow\\s*=\\s*(\\d+)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String row = matcher.group(2);
            addSapObject(id, "Table", transaction);
            addAction(new SapAction("SetCurrentCell", id, row, lineNumber));
            return true;
        }
        return false;
    }

    protected boolean parseFindByIdAction(String line, String transaction, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "session\\w*\\.findById\\(\"([^\"]+)\"\\)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            addSapObject(id, "Element", transaction);
            return true;
        }
        return false;
    }

    protected void parsePropertyAssignment(String line, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) +
            "session\\w*\\.findById\\(\"([^\"]+)\"\\)\\.([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*\"([^\"]*)\"",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (matcher.find()) {
            String id = matcher.group(1);
            String propertyName = matcher.group(2);
            String propertyValue = matcher.group(3);

            // Skip if this is an action property
            String propLower = propertyName.toLowerCase();
            if (
                !propLower.equals("text") &&
                !propLower.equals("selected") &&
                !propLower.equals("key") &&
                !propLower.equals("value") &&
                !propLower.equals("caretposition") &&
                !propLower.equals("currentcellrow")
            ) {
                addPropertyToSapObject(id, propertyName, propertyValue);
                LOGGER.fine(
                    String.format("Captured property: %s.%s = %s", id, propertyName, propertyValue)
                );
            }
        }
    }

    /** Tags {@code action} with the session active when it was recorded, then queues it. */
    protected void addAction(SapAction action) {
        action.sessionLabel = currentSessionLabel;
        sapActions.add(action);
    }

    protected void addSapObject(String id, String type, String transaction) {
        id = stripAbsoluteSessionPrefix(id);
        String key = sessionScopedKey(id);
        if (!sapObjects.containsKey(key)) {
            SapObject obj = new SapObject(id, type, transaction);
            obj.sessionLabel = currentSessionLabel;
            // Text should only be set when explicitly captured from the script
            sapObjects.put(key, obj);
            LOGGER.fine(String.format("Added SAP object: id=%s, type=%s", id, type));
        }
    }

    protected void addPropertyToSapObject(String id, String propertyName, String propertyValue) {
        id = stripAbsoluteSessionPrefix(id);
        String key = sessionScopedKey(id);
        if (sapObjects.containsKey(key)) {
            SapObject obj = sapObjects.get(key);
            obj.setProperty(propertyName, propertyValue);
        } else {
            SapObject obj = new SapObject(id, determineObjectType(id), null);
            obj.sessionLabel = currentSessionLabel;
            obj.setProperty(propertyName, propertyValue);
            sapObjects.put(key, obj);
        }
    }

    /**
     * Looks up a previously-added object by id, honoring the current session scoping - use this
     * instead of {@code sapObjects.get(id)} directly, which would miss an object stored under a
     * session-qualified key once more than one session is in play.
     */
    protected SapObject getSapObject(String id) {
        return sapObjects.get(sessionScopedKey(id));
    }

    /** Same relative id can legitimately appear on more than one session's screen - key by (session, id) so they don't collide. */
    private String sessionScopedKey(String id) {
        return (currentSessionLabel == null || currentSessionLabel.isEmpty())
            ? id
            : currentSessionLabel + "::" + id;
    }

    /**
     * Absolute ids ({@code /app/con[x]/ses[y]/wnd[...]}) are stripped to the session-relative
     * form stored everywhere else ({@code wnd[...]}), and the embedded {@code ses[y]} index
     * takes over as the current session (0 = primary) - a second, path-based way scripts convey
     * which session an action targets, alongside the session-variable-name tracking above.
     */
    private String stripAbsoluteSessionPrefix(String id) {
        if (id == null) {
            return null;
        }
        Matcher m = CON_SES_PREFIX.matcher(id);
        if (!m.find()) {
            return id;
        }
        int sesIndex = Integer.parseInt(m.group(1));
        noteSessionLabel(sesIndex == 0 ? null : "s" + sesIndex, -1);
        return id.substring(m.end());
    }

    /**
     * Records that the recording just switched to {@code label} ({@code null} = the primary
     * session) - a no-op if it's already current. Emits {@code SAP.openSession} the first time a
     * non-primary label is seen, {@code SAP.switchSession} on every return to a label already
     * seen (blank objectId for a switch back to the primary).
     */
    protected void noteSessionLabel(String label, int lineNumber) {
        if (Objects.equals(label, currentSessionLabel)) {
            return;
        }
        if (label != null && seenSessionLabels.add(label)) {
            addAction(new SapAction("OpenSession", label, "", lineNumber));
        } else {
            addAction(new SapAction("SwitchSession", label == null ? "" : label, "", lineNumber));
        }
        currentSessionLabel = label;
    }

    /**
     * Looks for a reference to a {@code session}/{@code session2}/... variable on this line and,
     * if it differs from the connection's implicit primary (the first such variable ever seen),
     * routes the switch through {@link #noteSessionLabel}. A no-op for lines that don't touch
     * any session variable at all.
     */
    protected void trackSessionVariable(String line, int lineNumber, String prefix) {
        Pattern pattern = Pattern.compile(
            Pattern.quote(prefix) + "(session\\w*)\\.",
            Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(line);
        if (!matcher.find()) {
            return;
        }
        noteSessionLabel(labelForSessionVar(matcher.group(1)), lineNumber);
    }

    /**
     * Resolves (inventing one if this is a session variable never seen before) the label for a
     * script variable name - {@code null} for the recording's first such variable (the implicit
     * primary, already open via {@code initConnection}). Does not itself emit any switch action;
     * pass the result to {@link #noteSessionLabel} for that. Exposed for language parsers (e.g.
     * Java, PowerShell) whose own variable-capture syntax needs this same first-seen-is-primary
     * resolution but can't reuse {@link #trackSessionVariable}'s line-oriented regex directly.
     */
    protected String labelForSessionVar(String var) {
        if (var == null) {
            return currentSessionLabel;
        }
        if (primarySessionVar == null) {
            primarySessionVar = var;
            return null;
        }
        if (var.equalsIgnoreCase(primarySessionVar)) {
            return null;
        }
        String label = sessionVarLabels.get(var);
        if (label == null) {
            label = "s" + (sessionVarLabels.size() + 1);
            sessionVarLabels.put(var, label);
        }
        return label;
    }

    protected String determineObjectType(String sapId) {
        String lastSegment = sapId;
        int lastSlash = sapId.lastIndexOf('/');
        if (lastSlash >= 0) {
            lastSegment = sapId.substring(lastSlash + 1);
        }

        if (lastSegment.startsWith("txt") || lastSegment.startsWith("ctxt")) {
            return "TextField";
        }
        if (lastSegment.startsWith("pwd")) {
            return "PasswordField";
        }
        if (lastSegment.startsWith("btn")) {
            return "Button";
        }
        if (lastSegment.startsWith("chk")) {
            return "Checkbox";
        }
        if (lastSegment.startsWith("rad")) {
            return "RadioButton";
        }
        if (lastSegment.startsWith("cmb") || lastSegment.startsWith("cbo")) {
            return "ComboBox";
        }
        if (lastSegment.startsWith("tbl")) {
            return "Table";
        }
        if (lastSegment.startsWith("tab")) {
            return "Tab";
        }
        if (lastSegment.startsWith("wnd")) {
            return "Window";
        }
        if (lastSegment.startsWith("usr") || lastSegment.startsWith("sub")) {
            return "Container";
        }

        return "Element";
    }

    protected String extractTextFromId(String id) {
        String[] parts = id.split("/");
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            return lastPart.replaceAll("^(txt|btn|cbo|chk|tbl|tab|ctxt|cmbBox|rad)", "");
        }
        return "";
    }

    public Map<String, SapObject> getSapObjects() {
        return sapObjects;
    }

    public List<SapAction> getSapActions() {
        return sapActions;
    }

    // -------- Inner Classes --------

    public static class SapObject {
        public String id;
        public String type;
        public String text;
        public String name;
        public String transaction;
        public Map<String, String> additionalProperties;
        /** Phase 4: the session this element was captured under - null for the primary (single-session) case. */
        public String sessionLabel;

        public SapObject(String id, String type, String transaction) {
            this.id = id;
            this.type = type;
            this.transaction = transaction;
            this.text = "";
            this.name = "";
            this.additionalProperties = new LinkedHashMap<>();
        }

        public void setProperty(String propertyName, String propertyValue) {
            switch (propertyName.toLowerCase()) {
                case "text":
                    this.text = propertyValue;
                    break;
                case "name":
                    this.name = propertyValue;
                    break;
                default:
                    additionalProperties.put(propertyName, propertyValue);
                    break;
            }
        }
    }

    /**
     * Parse a SAP script file with statistics tracking.
     * Returns a SapParseResult with detailed metrics about the parse operation.
     */
    public SapParseResult parseWithStats(File file) throws IOException {
        long startTime = System.currentTimeMillis();
        parse(file);
        long parseTime = System.currentTimeMillis() - startTime;

        return new SapParseResult(
            sapObjects.size(),
            sapActions.size(),
            linesProcessed,
            linesParsed,
            parseTime,
            warnings,
            calculateActionTypeCounts()
        );
    }

    /**
     * Calculate count of each action type from parsed actions.
     */
    protected Map<String, Integer> calculateActionTypeCounts() {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (SapAction action : sapActions) {
            counts.merge(action.actionType, 1, Integer::sum);
        }
        return counts;
    }

    public static class SapAction {
        public String actionType;
        public String objectId;
        public String value;
        public int lineNumber;
        /** Phase 4: the session this action ran against - null for the primary (single-session) case. Unused for the synthetic OpenSession/SwitchSession actions themselves. */
        public String sessionLabel;

        public SapAction(String actionType, String objectId, String value, int lineNumber) {
            this.actionType = actionType;
            this.objectId = objectId;
            this.value = value;
            this.lineNumber = lineNumber;
        }
    }
}
