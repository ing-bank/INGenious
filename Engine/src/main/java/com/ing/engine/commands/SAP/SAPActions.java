package com.ing.engine.commands.SAP;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.drivers.AutomationObject;
import com.ing.engine.drivers.sap.SapConnectionException;
import com.ing.engine.drivers.sap.SapSessionManager;
import com.ing.engine.execution.run.TestCaseRunner;
import com.ing.engine.util.NumberConditionEvaluator;
import com.ing.engine.util.RowMatcher;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.annotation.Args;
import com.ing.ingenious.api.exception.mobile.ElementException;
import com.ing.ingenious.api.exception.mobile.ElementException.ExceptionType;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.types.ArgType;
import com.ing.ingenious.api.types.ConditionKind;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import java.util.HashMap;
import java.util.Map;

/**
 * SAP GUI Scripting Actions for INGenious Test Automation Framework
 *
 * This class provides comprehensive SAP GUI automation actions organized by element type.
 * Actions are grouped into logical sections for better maintainability and discoverability.
 *
 * Total Actions: 99 (17 Existing + 82 New)
 *
 * Sections:
 * 1. Session & Transaction Management
 * 2. Window Management
 * 3. Text Field Actions
 * 4. Button Actions
 * 5. Checkbox & Radio Button Actions
 * 6. ComboBox/Dropdown Actions
 * 7. Tab Actions
 * 8. Table Control Actions (GuiTableControl)
 * 9. Grid/ALV Actions (GuiGridView)
 * 10. Menu & Toolbar Actions
 * 11. Context Menu Actions
 * 12. Tree Control Actions
 * 13. Status Bar Actions
 * 14. General Element Actions
 * 15. Execution Evidence
 */
public class SAPActions extends General {

    public SAPActions(CommandControl cc) {
        super(cc);
    }

    // ============================================================================
    // HELPER METHODS - VALIDATION & ERROR HANDLING
    // ============================================================================

    /**
     * Validates that SAPElement exists before attempting operations
     * @throws ElementException if SAPElement is null
     */
    private void validateSAPElement() {
        if (SAPElement == null) {
            throw new ElementException(ExceptionType.Element_Not_Found, ObjectName);
        }
    }

    /**
     * Validates that a SAP connection is open before attempting operations
     * @throws ElementException if no SAP connection is open
     */
    private void validateSAPSession() {
        if (!SapSessionManager.INSTANCE.hasConnection()) {
            Report.updateTestLog(
                Action,
                "No SAP connection - add a SAP.initConnection step.",
                Status.FAIL
            );
            throw new ElementException(ExceptionType.Element_Not_Found, "SAP Session");
        }
    }

    private TestCaseRunner currentRunner() {
        return userData.context();
    }

    /**
     * Finds the first row (of a GuiTableControl/GuiGridView, both expose the same RowCount /
     * GetCellValue API) whose {@code matchColumn} text matches {@code matchText}.
     * @return matching row index, or -1 if no row matched
     */
    private int findMatchingRow(String matchColumn, String matchText, RowMatcher.MatchMode mode) {
        int rowCount = Integer.parseInt(Dispatch.get(SAPElement, "RowCount").toString());
        return RowMatcher.findRow(
            rowCount,
            row -> Dispatch.call(SAPElement, "GetCellValue", row, matchColumn).toString(),
            matchText,
            mode
        );
    }

    /**
     * Shared body for the exact/contains variants of {@code sapSelectTableRowByText} /
     * {@code sapSelectGridRowByText} - table and grid controls expose the same
     * RowCount/GetCellValue/SelectRow API.
     * Data format: matchColumn,matchText
     */
    private void selectRowByText(RowMatcher.MatchMode mode) {
        try {
            String[] parts = Data.split(",", 2);
            if (parts.length < 2) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: column,text",
                    Status.FAIL
                );
                return;
            }
            String column = parts[0].trim();
            String text = parts[1].trim();

            int row = findMatchingRow(column, text, mode);
            if (row < 0) {
                Report.updateTestLog(
                    Action,
                    "No row found where column [" + column + "] matches [" + text + "]",
                    Status.FAIL
                );
                return;
            }
            Dispatch.call(SAPElement, "SelectRow", row);
            Report.updateTestLog(
                Action,
                "Selected row " + row + " where column [" + column + "] matches [" + text + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to select row by text. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * Finds a row by {@code matchColumn}/{@code matchText}, then reads {@code targetColumn}'s
     * value from that same row - lets a value from one column be retrieved using another
     * column as the lookup key, since {@link #findMatchingRow} only returns an index that
     * {@code GetCellValue} needs but never exposes to the caller.
     * @return the target column's value, or null if no row matched
     */
    private String findValueInMatchingRow(
        String matchColumn,
        String matchText,
        String targetColumn,
        RowMatcher.MatchMode mode
    ) {
        int row = findMatchingRow(matchColumn, matchText, mode);
        if (row < 0) {
            return null;
        }
        return Dispatch.call(SAPElement, "GetCellValue", row, targetColumn).toString();
    }

    /**
     * Shared body for the exact/contains, table/grid variants of storing a matched row's cell
     * into a variable. Data format: matchColumn,matchText,targetColumn
     */
    private void storeCellValueByMatchInVariable(RowMatcher.MatchMode mode) {
        try {
            String[] parts = Data.split(",", 3);
            if (parts.length < 3) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: matchColumn,matchText,targetColumn",
                    Status.FAIL
                );
                return;
            }
            String matchColumn = parts[0].trim();
            String matchText = parts[1].trim();
            String targetColumn = parts[2].trim();

            String value = findValueInMatchingRow(matchColumn, matchText, targetColumn, mode);
            if (value == null) {
                Report.updateTestLog(
                    Action,
                    "No row found where column [" + matchColumn + "] matches [" + matchText + "]",
                    Status.FAIL
                );
                return;
            }
            addVar(Condition, value);
            Report.updateTestLog(
                Action,
                "Got value [" +
                value +
                "] from column [" +
                targetColumn +
                "] in row where [" +
                matchColumn +
                "] matches [" +
                matchText +
                "] into variable [" +
                Condition +
                "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to store cell in matching row. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * Shared body for the exact/contains, table/grid variants of storing a matched row's cell
     * into a datasheet cell. Data format: matchColumn,matchText,targetColumn,Sheet:Column
     */
    private void storeCellValueByMatchInDataSheet(RowMatcher.MatchMode mode) {
        try {
            String[] parts = Data.split(",", 4);
            if (parts.length < 4) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: matchColumn,matchText,targetColumn,Sheet:Column",
                    Status.FAIL
                );
                return;
            }
            String matchColumn = parts[0].trim();
            String matchText = parts[1].trim();
            String targetColumn = parts[2].trim();
            String[] sheetRef = parts[3].trim().split(":", 2);
            if (sheetRef.length < 2) {
                Report.updateTestLog(
                    Action,
                    "Invalid datasheet reference. Expected: Sheet:Column",
                    Status.FAIL
                );
                return;
            }
            String sheetName = sheetRef[0].trim();
            String columnName = sheetRef[1].trim();

            String value = findValueInMatchingRow(matchColumn, matchText, targetColumn, mode);
            if (value == null) {
                Report.updateTestLog(
                    Action,
                    "No row found where column [" + matchColumn + "] matches [" + matchText + "]",
                    Status.FAIL
                );
                return;
            }
            userData.putData(sheetName, columnName, value);
            Report.updateTestLog(
                Action,
                "Got value [" +
                value +
                "] from column [" +
                targetColumn +
                "] in row where [" +
                matchColumn +
                "] matches [" +
                matchText +
                "] into datasheet [" +
                sheetName +
                ":" +
                columnName +
                "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to store cell in matching row. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * Shared body for {@code sapAssertTableRowCount} / {@code sapAssertGridRowCount} - table
     * and grid controls expose the same RowCount API.
     */
    private void assertRowCount() {
        String actualCount = null;
        try {
            actualCount = Dispatch.get(SAPElement, "RowCount").toString();
            NumberConditionEvaluator.Condition parsed = NumberConditionEvaluator.parse(Data);
            boolean result = NumberConditionEvaluator.evaluate(
                actualCount,
                parsed.operator(),
                parsed.expected()
            );
            if (result) {
                Report.updateTestLog(
                    Action,
                    "[" +
                    ObjectName +
                    "] row count [" +
                    actualCount +
                    "] satisfies condition [" +
                    Data +
                    "]",
                    Status.PASS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "[" +
                    ObjectName +
                    "] row count [" +
                    actualCount +
                    "] does not satisfy condition [" +
                    Data +
                    "]",
                    Status.FAIL
                );
            }
        } catch (NumberFormatException e) {
            Report.updateTestLog(
                Action,
                "[" +
                ObjectName +
                "] row count [" +
                actualCount +
                "] is not numeric. Error: " +
                e.getMessage(),
                Status.FAIL
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to assert row count on [" + ObjectName + "]. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    // ============================================================================
    // SECTION 0: CONNECTION MANAGEMENT (driverless SAP)
    // ============================================================================

    /**
     * Open (or adopt), or re-claim if already open, a SAP connection and make it
     * current. Mirrors {@code Database.initDBConnection}. Blank input = the project
     * default connection; {@code #alias} = {@code Settings/SAP/<alias>.properties}.
     */
    @Action(
        object = ObjectType.SAP,
        desc = "Initialise SAP connection [<Input>] (blank = project default)",
        input = InputType.OPTIONAL
    )
    @Args(
        input = ArgType.ALIAS_SAP,
        inputExample = "#SAP_QA",
        help = "Blank = project default; #alias = Settings/SAP/<alias>.properties"
    )
    public void sapInitConnection() {
        try {
            SapSessionManager.INSTANCE.initConnection(Input, currentRunner());
            Report.setSapSession(SapSessionManager.INSTANCE.current());
            Report.updateTestLog(
                Action,
                "SAP connection ready [" + SapSessionManager.INSTANCE.currentAliasName() + "]",
                Status.PASSNS
            );
        } catch (SapConnectionException ex) {
            Report.updateTestLog(Action, ex.getMessage(), Status.FAILNS);
        }
    }

    /** Make an already-open SAP connection current (no claim change). */
    @Action(
        object = ObjectType.SAP,
        desc = "Switch current SAP connection to [<Input>]",
        input = InputType.OPTIONAL
    )
    @Args(
        input = ArgType.ALIAS_SAP,
        inputExample = "#SAP_QA",
        help = "An already-open connection alias."
    )
    public void sapSwitchConnection() {
        try {
            SapSessionManager.INSTANCE.switchConnection(Input);
            Report.updateTestLog(
                Action,
                "Switched to SAP connection [" +
                SapSessionManager.INSTANCE.currentAliasName() +
                "]",
                Status.PASSNS
            );
        } catch (SapConnectionException ex) {
            Report.updateTestLog(Action, ex.getMessage(), Status.FAILNS);
        }
    }

    /**
     * Release this runner's claim on a SAP connection (blank = project default).
     * Physically closes only when the claim stack empties and the run owns it.
     */
    @Action(
        object = ObjectType.SAP,
        desc = "Close SAP connection [<Input>] (blank = project default)",
        input = InputType.OPTIONAL
    )
    @Args(
        input = ArgType.ALIAS_SAP,
        inputExample = "#SAP_QA",
        help = "Blank = project default; #alias = that connection only."
    )
    public void sapCloseConnection() {
        try {
            SapSessionManager.INSTANCE.closeConnection(Input, currentRunner());
            Report.updateTestLog(Action, "SAP connection released", Status.PASSNS);
        } catch (Exception ex) {
            Report.updateTestLog(
                Action,
                "Error releasing SAP connection: " + ex.getMessage(),
                Status.FAILNS
            );
        }
    }

    /** Force-close every SAP connection this run owns (last-resort teardown). */
    @Action(object = ObjectType.SAP, desc = "Force-close every SAP connection this run owns")
    public void sapCloseAllConnection() {
        try {
            SapSessionManager.INSTANCE.closeAll(currentRunner().getRoot());
            Report.updateTestLog(Action, "All SAP connections closed", Status.PASSNS);
        } catch (Exception ex) {
            Report.updateTestLog(
                Action,
                "Error closing SAP connections: " + ex.getMessage(),
                Status.FAILNS
            );
        }
    }

    /**
     * Create a new session (one connection, up to 6 sessions) on the current SAP connection,
     * labeled by the resolved input, and make it current. Unlike {@code #alias} connection
     * references, a session label is runtime-only and resolved through the ordinary value
     * pipeline ({@code @literal}, datasheet, {@code %var%}, {@code ${env}}, formula) - so this
     * reads {@code Data} (already resolved), not the raw {@code Input}.
     */
    @Action(
        object = ObjectType.SAP,
        desc = "Open a new SAP session labeled [<Data>]",
        input = InputType.YES
    )
    @Args(
        inputExample = "@stock",
        help = "A session label, e.g. @stock - a plain value, not a #alias."
    )
    public void sapOpenSession() {
        try {
            SapSessionManager.INSTANCE.openSession(Data);
            Report.setSapSession(SapSessionManager.INSTANCE.current());
            Report.updateTestLog(
                Action,
                "SAP session opened [" + SapSessionManager.INSTANCE.currentSessionLabel() + "]",
                Status.PASSNS
            );
        } catch (SapConnectionException ex) {
            Report.updateTestLog(Action, ex.getMessage(), Status.FAILNS);
        }
    }

    /**
     * Make an already-open session on the current SAP connection current (blank = the
     * connection's primary session - the one {@code initConnection} created). No session is
     * created.
     */
    @Action(
        object = ObjectType.SAP,
        desc = "Switch current SAP session to [<Data>] (blank = primary session)",
        input = InputType.OPTIONAL
    )
    @Args(
        inputExample = "@stock",
        help = "Blank = the connection's primary session; a label = that session only."
    )
    public void sapSwitchSession() {
        try {
            SapSessionManager.INSTANCE.switchSession(Data);
            Report.setSapSession(SapSessionManager.INSTANCE.current());
            Report.updateTestLog(
                Action,
                "Switched to SAP session [" +
                SapSessionManager.INSTANCE.currentSessionLabel() +
                "]",
                Status.PASSNS
            );
        } catch (SapConnectionException ex) {
            Report.updateTestLog(Action, ex.getMessage(), Status.FAILNS);
        }
    }

    /**
     * Close a session on the current SAP connection (blank = the current session). Refuses to
     * close a connection's only remaining session - use {@code SAP.closeConnection} for that.
     */
    @Action(
        object = ObjectType.SAP,
        desc = "Close SAP session [<Data>] (blank = current session)",
        input = InputType.OPTIONAL
    )
    @Args(inputExample = "@stock", help = "Blank = current session; a label = that session only.")
    public void sapCloseSession() {
        try {
            SapSessionManager.INSTANCE.closeSession(Data);
            Report.updateTestLog(Action, "SAP session closed", Status.PASSNS);
        } catch (Exception ex) {
            Report.updateTestLog(
                Action,
                "Error closing SAP session: " + ex.getMessage(),
                Status.FAILNS
            );
        }
    }

    /**
     * Safely parses integer input with validation
     * @param value the string value to parse
     * @param fieldName the field name for error reporting
     * @return the parsed integer
     * @throws NumberFormatException if value is not a valid integer
     */
    private int parseIntSafely(String value, String fieldName) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            Report.updateTestLog(
                Action,
                "Invalid " + fieldName + " format [" + value + "]. Expected integer.",
                Status.DEBUG
            );
            throw e;
        }
    }

    // ============================================================================
    // SECTION 1: SESSION & TRANSACTION MANAGEMENT (BROWSER Level)
    // ============================================================================

    /**
     * NEW: Execute SAP transaction code
     * SAP API: session.startTransaction(tcode)
     */
    @Action(
        object = ObjectType.SAP,
        desc = "Execute SAP transaction [<Data>]",
        input = InputType.YES
    )
    public void sapExecuteTransaction() {
        try {
            Dispatch.call(SAPsession, "startTransaction", Data);
            Report.updateTestLog(Action, "Executed transaction [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to execute transaction. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: End current SAP transaction
     * SAP API: session.endTransaction()
     */
    @Action(object = ObjectType.SAP, desc = "End current SAP transaction", input = InputType.NO)
    public void sapEndTransaction() {
        try {
            Dispatch.call(SAPsession, "endTransaction");
            Report.updateTestLog(Action, "Ended current transaction successfully", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to end transaction. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Refresh SAP session
     * SAP API: session.Refresh()
     */
    @Action(object = ObjectType.SAP, desc = "Refresh SAP session", input = InputType.NO)
    public void sapRefreshSession() {
        try {
            Dispatch.call(SAPsession, "Refresh");
            Report.updateTestLog(Action, "SAP session refreshed successfully", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to refresh session. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * EXISTING: Close SAP logon landscape screen
     * Process-level operation
     */
    @Action(
        object = ObjectType.SAP,
        desc = "Close SAP logon landscape screen",
        input = InputType.NO
    )
    public void sapCloseLogonScreen() {
        try {
            SAPProcess.destroy();
            Report.updateTestLog(Action, "Logon landscape screen closed successfully", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to close Logon landscape screen. Error: " + e.getMessage(),
                Status.FAILNS
            );
        }
    }

    /**
     * EXISTING: Set global object property at runtime
     * Format: property=value or multiple properties separated by comma
     */
    @Action(
        object = ObjectType.SAP,
        desc = "Set all objects property to [<Data>] at runtime",
        input = InputType.YES,
        condition = InputType.YES
    )
    public void sapSetglobalObjectProperty() {
        if (!Data.isEmpty()) {
            if (Condition.isEmpty()) {
                String[] groups = Data.split(",");
                for (String group : groups) {
                    String[] vals = group.split("=", 2);
                    AutomationObject.globalDynamicValue.put(vals[0], vals[1]);
                }
            } else {
                AutomationObject.globalDynamicValue.put(Condition, Data);
            }
            String text = String.format(
                "Setting Global Object Property for %s with %s",
                Condition,
                Data
            );
            Report.updateTestLog(Action, text, Status.DONE);
        } else {
            Report.updateTestLog(Action, "Input should not be empty", Status.FAILNS);
        }
    }

    // ============================================================================
    // SECTION 2: WINDOW MANAGEMENT (GuiFrameWindow)
    // ============================================================================

    /**
     * NEW: Maximize SAP window
     * SAP API: window.Maximize()
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Maximize SAP window [<Object>]",
        input = InputType.NO
    )
    public void sapMaximizeWindow() {
        try {
            Dispatch.call(SAPElement, "Maximize");
            Report.updateTestLog(
                Action,
                "Window [" + ObjectName + "] maximized successfully",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to maximize window. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Minimize SAP window
     * SAP API: window.Minimize()
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Minimize SAP window [<Object>]",
        input = InputType.NO
    )
    public void sapMinimizeWindow() {
        try {
            Dispatch.call(SAPElement, "Minimize");
            Report.updateTestLog(
                Action,
                "Window [" + ObjectName + "] minimized successfully",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to minimize window. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Restore SAP window to normal size
     * SAP API: window.Restore()
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Restore SAP window [<Object>] to normal size",
        input = InputType.NO
    )
    public void sapRestoreWindow() {
        try {
            Dispatch.call(SAPElement, "Restore");
            Report.updateTestLog(
                Action,
                "Window [" + ObjectName + "] restored successfully",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to restore window. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Close SAP window or popup
     * SAP API: window.Close()
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Close SAP window or popup [<Object>]",
        input = InputType.NO
    )
    public void sapCloseWindow() {
        try {
            Dispatch.call(SAPElement, "Close");
            Report.updateTestLog(
                Action,
                "Window [" + ObjectName + "] closed successfully",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to close window. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Iconify SAP window
     * SAP API: window.Iconify()
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Iconify SAP window [<Object>]",
        input = InputType.NO
    )
    public void sapIconifyWindow() {
        try {
            Dispatch.call(SAPElement, "Iconify");
            Report.updateTestLog(
                Action,
                "Window [" + ObjectName + "] iconified successfully",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to iconify window. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Resize working pane
     * SAP API: window.resizeWorkingPane(width, height, fullScreen)
     * Data format: width,height[,fullScreen]
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Resize working pane with parameters [<Data>]",
        input = InputType.YES
    )
    public void sapResizeWorkingPane() {
        try {
            String[] params = Data.split(",");
            if (params.length >= 2) {
                int width = Integer.parseInt(params[0].trim());
                int height = Integer.parseInt(params[1].trim());
                boolean fullScreen = params.length > 2
                    ? Boolean.parseBoolean(params[2].trim())
                    : false;

                Variant[] args = new Variant[3];
                args[0] = new Variant(width);
                args[1] = new Variant(height);
                args[2] = new Variant(fullScreen);

                Dispatch.call(SAPElement, "resizeWorkingPane", (Object[]) args);
                Report.updateTestLog(
                    Action,
                    "Resized working pane to " + width + "x" + height,
                    Status.DONE
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: width,height[,fullScreen]",
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to resize working pane. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    // ============================================================================
    // SECTION 3: TEXT FIELD ACTIONS (GuiTextField, GuiCTextField)
    // ============================================================================

    /**
     * EXISTING: Enter text value in field
     * SAP API: element.Text = value
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Enter the value [<Data>] in the field [<Object>]",
        input = InputType.YES
    )
    public void sapFill() {
        try {
            validateSAPElement();
            Dispatch.put(SAPElement, "Text", Data);
            Report.updateTestLog(
                Action,
                "Entered Text '" + Data + "' on '" + ObjectName + "'",
                Status.DONE
            );
        } catch (ElementException ex) {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAIL);
            throw ex;
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to Enter Text. Error : " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * EXISTING: Press Enter key
     * SAP API: element.sendVKey(0)
     */
    @Action(object = ObjectType.SAP_OBJECT, desc = "Press [<Enter>] key", input = InputType.NO)
    public void sapEnter() {
        try {
            validateSAPElement();
            Dispatch.call(SAPElement, "sendVKey", 0);
            Report.updateTestLog(Action, "Enter key pressed successfully.", Status.DONE);
        } catch (ElementException ex) {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAIL);
            throw ex;
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Error in Enter key press. Error : " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * EXISTING: Simulate virtual key press
     * SAP API: element.sendVKey(vkey)
     * VKey codes: 0=Enter, 3=Back, 4=F4, 8=F8/Execute, 11=Save, etc.
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Simulate key press with VCode [<Data>]",
        input = InputType.YES
    )
    public void sapSimulateKeyPress() {
        try {
            validateSAPElement();
            int vkeyCode = parseIntSafely(Data, "VKey code");
            Dispatch.call(SAPElement, "sendVKey", vkeyCode);
            Report.updateTestLog(
                Action,
                "Simulate key press with VKey code [" + Data + "]",
                Status.DONE
            );
        } catch (ElementException ex) {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAIL);
            throw ex;
        } catch (NumberFormatException ex) {
            Report.updateTestLog(
                Action,
                "Invalid VKey code format [" + Data + "]. Expected integer.",
                Status.FAIL
            );
            throw new IllegalArgumentException(ex);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Fails to simulate key press with VKey code [" +
                Data +
                "]. Error : " +
                e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Select all text in field
     * SAP API: element.SelectAll()
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select all text in field [<Object>]",
        input = InputType.NO
    )
    public void sapSelectAllText() {
        try {
            validateSAPElement();
            Dispatch.call(SAPElement, "SelectAll");
            Report.updateTestLog(Action, "Selected all text in [" + ObjectName + "]", Status.DONE);
        } catch (ElementException ex) {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAIL);
            throw ex;
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to select all text. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Set caret position in text field
     * SAP API: element.caretPosition = position
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set caret position to [<Data>]",
        input = InputType.YES
    )
    public void sapSetCaretPosition() {
        try {
            validateSAPElement();
            int position = parseIntSafely(Data, "caret position");
            Dispatch.put(SAPElement, "caretPosition", position);
            Report.updateTestLog(Action, "Set caret position to " + Data, Status.DONE);
        } catch (ElementException ex) {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAIL);
            throw ex;
        } catch (NumberFormatException ex) {
            Report.updateTestLog(
                Action,
                "Invalid caret position format [" + Data + "]. Expected integer.",
                Status.FAIL
            );
            throw new IllegalArgumentException(ex);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set caret position. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Set modified flag on element
     * SAP API: element.modified = boolean
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set modified property to [<Data>]",
        input = InputType.YES
    )
    public void sapSetModified() {
        try {
            validateSAPElement();
            if (!Data.toLowerCase().matches("^(true|false)$")) {
                Report.updateTestLog(
                    Action,
                    "Invalid boolean format [" + Data + "]. Expected 'true' or 'false'.",
                    Status.DEBUG
                );
                throw new IllegalArgumentException("Invalid boolean value: " + Data);
            }
            Dispatch.put(SAPElement, "modified", Boolean.parseBoolean(Data));
            Report.updateTestLog(Action, "Set modified property to " + Data, Status.DONE);
        } catch (ElementException ex) {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAIL);
            throw ex;
        } catch (IllegalArgumentException ex) {
            Report.updateTestLog(
                Action,
                "Invalid boolean format [" + Data + "]. Expected 'true' or 'false'.",
                Status.FAIL
            );
            throw ex;
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set modified property. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    // ============================================================================
    // SECTION 4: BUTTON ACTIONS (GuiButton)
    // ============================================================================

    /**
     * EXISTING: Click/Press button
     * SAP API: button.press()
     */
    @Action(object = ObjectType.SAP_OBJECT, desc = "Click the [<Object>]")
    public void sapClick() {
        try {
            validateSAPElement();
            Dispatch.call(SAPElement, "press");
            Report.updateTestLog(Action, "Clicking on [" + ObjectName + "]", Status.DONE);
        } catch (ElementException ex) {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAIL);
            throw ex;
        } catch (Exception e) {
            Report.updateTestLog(Action, "Failed to click. Error : " + e.getMessage(), Status.FAIL);
        }
    }

    /**
     * NEW: Press button by ID or function code
     * SAP API: toolbar.pressButton(buttonId)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Press button with ID [<Data>]",
        input = InputType.YES
    )
    public void sapPressButton() {
        try {
            validateSAPElement();
            Dispatch.call(SAPElement, "pressButton", Data);
            Report.updateTestLog(Action, "Pressed button [" + Data + "]", Status.DONE);
        } catch (ElementException ex) {
            Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAIL);
            throw ex;
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to press button. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    // ============================================================================
    // SECTION 5: CHECKBOX & RADIO BUTTON ACTIONS
    // ============================================================================

    /**
     * EXISTING: Select/Deselect checkbox
     * SAP API: checkbox.Selected = boolean
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set checkbox [<Object>] to [<Data>] (true/false)",
        input = InputType.YES
    )
    public void sapSelectCheckBox() {
        try {
            boolean value = Boolean.parseBoolean(Data);
            Dispatch.put(SAPElement, "Selected", value);
            Report.updateTestLog(
                Action,
                "Checkbox [" + ObjectName + "] set to " + value,
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Fails to set Checkbox. Error : " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Assert that a checkbox is selected, complementing {@link #sapSelectCheckBox()}
     * which only sets the state, never verifies it.
     * SAP API: checkbox.Selected
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Assert that checkbox [<Object>] is selected",
        input = InputType.NO
    )
    public void sapAssertCheckBoxSelected() {
        try {
            boolean selected = Boolean.parseBoolean(
                Dispatch.get(SAPElement, "Selected").toString()
            );
            if (selected) {
                Report.updateTestLog(
                    Action,
                    "Checkbox [" + ObjectName + "] is selected.",
                    Status.PASS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Checkbox [" + ObjectName + "] is not selected.",
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to get checkbox state. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /** NEW: Assert that a checkbox is not selected, the complement of {@link #sapAssertCheckBoxSelected()}. */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Assert that checkbox [<Object>] is not selected",
        input = InputType.NO
    )
    public void sapAssertCheckBoxNotSelected() {
        try {
            boolean selected = Boolean.parseBoolean(
                Dispatch.get(SAPElement, "Selected").toString()
            );
            if (!selected) {
                Report.updateTestLog(
                    Action,
                    "Checkbox [" + ObjectName + "] is not selected, as expected.",
                    Status.PASS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "Checkbox [" + ObjectName + "] is selected, but expected it not to be.",
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to get checkbox state. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * Shared body for the exact/contains variants of {@code sapSetCheckBoxInMatchingRow} -
     * locates a table/grid row by one column's text (e.g. description, condition type) and
     * ticks the checkbox in another column of that same row, without needing the row index.
     * Data format: matchColumn,matchText,checkboxColumn | Condition: true/false (default true)
     */
    private void setCheckBoxInMatchingRow(RowMatcher.MatchMode mode) {
        try {
            String[] parts = Data.split(",", 3);
            if (parts.length < 3) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: matchColumn,matchText,checkboxColumn",
                    Status.FAIL
                );
                return;
            }
            String matchColumn = parts[0].trim();
            String matchText = parts[1].trim();
            String checkboxColumn = parts[2].trim();
            boolean checked = Condition == null || Condition.trim().isEmpty()
                ? true
                : Boolean.parseBoolean(Condition.trim());

            int row = findMatchingRow(matchColumn, matchText, mode);
            if (row < 0) {
                Report.updateTestLog(
                    Action,
                    "No row found where column [" + matchColumn + "] matches [" + matchText + "]",
                    Status.FAIL
                );
                return;
            }
            Dispatch checkbox = Dispatch
                .call(SAPElement, "GetCell", row, checkboxColumn)
                .toDispatch();
            Dispatch.put(checkbox, "Selected", checked);
            Report.updateTestLog(
                Action,
                "Set checkbox [" +
                checkboxColumn +
                "] to " +
                checked +
                " in row " +
                row +
                " where column [" +
                matchColumn +
                "] matches [" +
                matchText +
                "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set checkbox in matching row. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /** NEW: {@link #setCheckBoxInMatchingRow(RowMatcher.MatchMode)} with an exact column match. */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set checkbox in row where column exactly matches [<Data>] (format: matchColumn,matchText,checkboxColumn)",
        input = InputType.YES,
        condition = InputType.OPTIONAL
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "ConditionType,ZPR0,Checkbox",
        inputHelp = "match column, match text, checkbox column (format: matchColumn,matchText,checkboxColumn)",
        condition = ConditionKind.TEXT,
        conditionExample = "true",
        conditionHelp = "checkbox state to set (true/false); blank defaults to true"
    )
    public void sapSetCheckBoxInMatchingRow() {
        setCheckBoxInMatchingRow(RowMatcher.MatchMode.EQUALS);
    }

    /** NEW: {@link #setCheckBoxInMatchingRow(RowMatcher.MatchMode)} with a substring column match. */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set checkbox in row where column contains [<Data>] (format: matchColumn,matchText,checkboxColumn)",
        input = InputType.YES,
        condition = InputType.OPTIONAL
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "ConditionType,ZPR0,Checkbox",
        inputHelp = "match column, match text, checkbox column (format: matchColumn,matchText,checkboxColumn)",
        condition = ConditionKind.TEXT,
        conditionExample = "true",
        conditionHelp = "checkbox state to set (true/false); blank defaults to true"
    )
    public void sapSetCheckBoxInMatchingRowContains() {
        setCheckBoxInMatchingRow(RowMatcher.MatchMode.CONTAINS);
    }

    /**
     * EXISTING: Select radio button in table row
     * SAP API: table.GetAbsoluteRow(row).Selected = true
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select Radio Button in row [<Data>]",
        input = InputType.YES
    )
    public void sapSelectRadioButtonInRow() {
        try {
            Dispatch row = Dispatch.call(SAPElement, "GetAbsoluteRow", Data).toDispatch();
            Dispatch.put(row, "Selected", true);
            Report.updateTestLog(Action, "Radio button selected successfully.", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Fails to select radio button. Error : " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    // ============================================================================
    // SECTION 6: COMBOBOX/DROPDOWN ACTIONS (GuiComboBox)
    // ============================================================================

    /**
     * EXISTING: Select dropdown by visible text
     * SAP API: comboBox.Text = value
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select dropdown value by visible text [<Data>]",
        input = InputType.YES
    )
    public void sapSelectDropDownByText() {
        try {
            Dispatch.put(SAPElement, "Text", Data);
            Report.updateTestLog(Action, "Dropdown set with text [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set dropdown with text. Error : " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * EXISTING: Select dropdown by key (internal value)
     * SAP API: comboBox.Key = value
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select Dropdown value by Key [<Data>]",
        input = InputType.YES
    )
    public void sapSelectDropDownByKey() {
        try {
            Dispatch.put(SAPElement, "Key", Data);
            Report.updateTestLog(Action, "Dropdown set with Key [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set dropdown with key. Error : " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * EXISTING: Select dropdown by index
     * SAP API: comboBox.Select(index)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select dropdown value by index [<Data>]",
        input = InputType.YES
    )
    public void sapSelectDropDownByIndex() {
        try {
            Dispatch.call(SAPElement, "Select", new Variant(Integer.parseInt(Data)));
            Report.updateTestLog(Action, "Dropdown set with index [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set dropdown with index. Error : " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Open dropdown list
     * SAP API: comboBox.Open()
     */
    @Action(object = ObjectType.SAP_OBJECT, desc = "Open dropdown [<Object>]", input = InputType.NO)
    public void sapOpenComboBox() {
        try {
            Dispatch.call(SAPElement, "Open");
            Report.updateTestLog(Action, "Opened dropdown [" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to open dropdown. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Close dropdown list
     * SAP API: comboBox.Close()
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Close dropdown [<Object>]",
        input = InputType.NO
    )
    public void sapCloseComboBox() {
        try {
            Dispatch.call(SAPElement, "Close");
            Report.updateTestLog(Action, "Closed dropdown [" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to close dropdown. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    // ============================================================================
    // SECTION 7: TAB ACTIONS (GuiTabStrip, GuiTab)
    // ============================================================================

    /**
     * EXISTING: Select tab
     * SAP API: tab.select()
     */
    @Action(object = ObjectType.SAP_OBJECT, desc = "Select the [<Object>]")
    public void sapSelect() {
        try {
            Dispatch.call(SAPElement, "select");
            Report.updateTestLog(Action, "Selected tab [" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to select tab. Error : " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    // ============================================================================
    // SECTION 8: TABLE CONTROL ACTIONS (GuiTableControl)
    // ============================================================================

    /**
     * NEW: Select table row
     * SAP API: table.SelectRow(row)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select table row [<Data>]",
        input = InputType.YES
    )
    public void sapSelectTableRow() {
        try {
            Dispatch.call(SAPElement, "SelectRow", Integer.parseInt(Data));
            Report.updateTestLog(Action, "Selected table row " + Data, Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to select table row. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Select a table row by an exact match of a column's text value instead of a
     * hard-coded row index - the row order can change (sort/filter/new entries) without
     * breaking the step.
     * Data format: matchColumn,matchText
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select table row where column exactly matches [<Data>] (format: column,text)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "Description,Vendor Invoice",
        inputHelp = "column name + expected text (format: column,text)"
    )
    public void sapSelectTableRowByText() {
        selectRowByText(RowMatcher.MatchMode.EQUALS);
    }

    /**
     * NEW: Select a table row by a substring match of a column's text value instead of a
     * hard-coded row index.
     * Data format: matchColumn,matchText
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select table row where column contains [<Data>] (format: column,text)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "Description,Vendor Invoice",
        inputHelp = "column name + expected text (format: column,text)"
    )
    public void sapSelectTableRowByTextContains() {
        selectRowByText(RowMatcher.MatchMode.CONTAINS);
    }

    /**
     * NEW: Set current table cell
     * SAP API: table.SetCurrentCell(row, column)
     * Data format: row,column
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set current table cell [<Data>] (format: row,column)",
        input = InputType.YES
    )
    public void sapSetCurrentCell() {
        try {
            String[] parts = Data.split(",", 2);
            if (parts.length < 2) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: row,column",
                    Status.FAIL
                );
                return;
            }
            int row = Integer.parseInt(parts[0].trim());
            String column = parts[1].trim();

            Dispatch.call(SAPElement, "SetCurrentCell", row, column);
            Report.updateTestLog(
                Action,
                "Set current cell to row " + row + ", column " + column,
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set current cell. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Set current table cell row
     * SAP API: table.currentCellRow = row
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set current table cell row to [<Data>]",
        input = InputType.YES
    )
    public void sapSetCurrentCellRow() {
        try {
            Dispatch.put(SAPElement, "currentCellRow", Integer.parseInt(Data));
            Report.updateTestLog(Action, "Set current cell row to " + Data, Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set current cell row. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Store a table cell's value into a variable.
     * SAP API: table.GetCellValue(row, column)
     * Data format: row,column | Condition: destination variable in %var% format
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Get cell value [<Data>] (format: row,column) and store into variable [<Condition>]",
        input = InputType.YES,
        condition = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "2,VendorName",
        inputHelp = "row + column (format: row,column)",
        condition = ConditionKind.TEXT,
        conditionExample = "%cellValue%",
        conditionHelp = "destination variable in %var% format to store the captured value"
    )
    public void sapStoreCellValueByIndexInVariable() {
        try {
            String[] parts = Data.split(",", 2);
            if (parts.length < 2) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: row,column",
                    Status.FAIL
                );
                return;
            }
            int row = Integer.parseInt(parts[0].trim());
            String column = parts[1].trim();

            String cellValue = Dispatch.call(SAPElement, "GetCellValue", row, column).toString();
            addVar(Condition, cellValue);
            Report.updateTestLog(
                Action,
                "Got cell value [" +
                cellValue +
                "] from row " +
                row +
                ", column " +
                column +
                " into variable [" +
                Condition +
                "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to get cell value. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Store a table cell's value into a datasheet cell, complementing
     * {@link #sapStoreCellValueByIndexInVariable()} which stores into a variable instead.
     * SAP API: table.GetCellValue(row, column)
     * Data format: row,column,Sheet:Column
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Get cell value and store into datasheet [<Data>] (format: row,column,Sheet:Column)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "2,VendorName,SapResults:Vendor",
        inputHelp = "row + column + destination datasheet reference (format: row,column,Sheet:Column)"
    )
    public void sapStoreCellValueByIndexInDataSheet() {
        try {
            String[] parts = Data.split(",", 3);
            if (parts.length < 3) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: row,column,Sheet:Column",
                    Status.FAIL
                );
                return;
            }
            int row = Integer.parseInt(parts[0].trim());
            String column = parts[1].trim();
            String[] sheetRef = parts[2].trim().split(":", 2);
            if (sheetRef.length < 2) {
                Report.updateTestLog(
                    Action,
                    "Invalid datasheet reference. Expected: Sheet:Column",
                    Status.FAIL
                );
                return;
            }
            String sheetName = sheetRef[0].trim();
            String columnName = sheetRef[1].trim();

            String cellValue = Dispatch.call(SAPElement, "GetCellValue", row, column).toString();
            userData.putData(sheetName, columnName, cellValue);
            Report.updateTestLog(
                Action,
                "Got cell value [" +
                cellValue +
                "] from row " +
                row +
                ", column " +
                column +
                " into datasheet [" +
                sheetName +
                ":" +
                columnName +
                "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to get cell value. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Locate a table row by one column's text (e.g. job name) and store a *different*
     * column's value (e.g. Status, End time) from that same row into a variable - exact match.
     * Data format: matchColumn,matchText,targetColumn | Condition: destination variable
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Store column [<Data>] from row matched by another column, into variable [<Condition>] (format: matchColumn,matchText,targetColumn)",
        input = InputType.YES,
        condition = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "JobName,BATCH_JOB_1,Status",
        inputHelp = "match column, match text, target column to read (format: matchColumn,matchText,targetColumn)",
        condition = ConditionKind.TEXT,
        conditionExample = "%jobStatus%",
        conditionHelp = "destination variable in %var% format to store the captured value"
    )
    public void sapStoreCellValueByMatchInVariable() {
        storeCellValueByMatchInVariable(RowMatcher.MatchMode.EQUALS);
    }

    /** NEW: {@link #sapStoreCellValueByMatchInVariable()} with a substring match column. */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Store column [<Data>] from row matched (contains) by another column, into variable [<Condition>] (format: matchColumn,matchText,targetColumn)",
        input = InputType.YES,
        condition = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "JobName,BATCH_JOB,Status",
        inputHelp = "match column, match text, target column to read (format: matchColumn,matchText,targetColumn)",
        condition = ConditionKind.TEXT,
        conditionExample = "%jobStatus%",
        conditionHelp = "destination variable in %var% format to store the captured value"
    )
    public void sapStoreCellValueByMatchInVariableContains() {
        storeCellValueByMatchInVariable(RowMatcher.MatchMode.CONTAINS);
    }

    /**
     * NEW: Locate a table row by one column's text and store a *different* column's value from
     * that same row into a datasheet cell instead of a variable - exact match.
     * Data format: matchColumn,matchText,targetColumn,Sheet:Column
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Store column and row matched by another column, into datasheet [<Data>] (format: matchColumn,matchText,targetColumn,Sheet:Column)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "JobName,BATCH_JOB_1,Status,SapResults:JobStatus",
        inputHelp = "match column, match text, target column, destination datasheet reference (format: matchColumn,matchText,targetColumn,Sheet:Column)"
    )
    public void sapStoreCellValueByMatchInDataSheet() {
        storeCellValueByMatchInDataSheet(RowMatcher.MatchMode.EQUALS);
    }

    /** NEW: {@link #sapStoreCellValueByMatchInDataSheet()} with a substring match column. */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Store column and row matched (contains) by another column, into datasheet [<Data>] (format: matchColumn,matchText,targetColumn,Sheet:Column)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "JobName,BATCH_JOB,Status,SapResults:JobStatus",
        inputHelp = "match column, match text, target column, destination datasheet reference (format: matchColumn,matchText,targetColumn,Sheet:Column)"
    )
    public void sapStoreCellValueByMatchInDataSheetContains() {
        storeCellValueByMatchInDataSheet(RowMatcher.MatchMode.CONTAINS);
    }

    /**
     * NEW: Modify table cell value
     * SAP API: table.modifyCell(row, column, value)
     * Data format: row,column,value
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Modify table cell with data [<Data>] in format row,column,value",
        input = InputType.YES
    )
    public void sapModifyCell() {
        try {
            String[] parts = Data.split(",", 3);
            if (parts.length < 3) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: row,column,value",
                    Status.FAIL
                );
                return;
            }
            int row = Integer.parseInt(parts[0].trim());
            String column = parts[1].trim();
            String value = parts[2].trim();

            Dispatch.call(SAPElement, "modifyCell", row, column, value);
            Report.updateTestLog(
                Action,
                "Modified cell at row " + row + ", column " + column + " with value: " + value,
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to modify cell. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Click current table cell
     * SAP API: table.ClickCurrentCell()
     */
    @Action(object = ObjectType.SAP_OBJECT, desc = "Click current table cell", input = InputType.NO)
    public void sapClickCurrentCell() {
        try {
            Dispatch.call(SAPElement, "ClickCurrentCell");
            Report.updateTestLog(
                Action,
                "Clicked current cell in table [" + ObjectName + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to click current cell. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * EXISTING: Double-click current table cell
     * SAP API: table.doubleClickCurrentCell()
     */
    @Action(object = ObjectType.SAP_OBJECT, desc = "Double click the current Cell")
    public void sapDoubleClickCell() {
        try {
            Dispatch.call(SAPElement, "doubleClickCurrentCell");
            Report.updateTestLog(
                Action,
                "Double Clicking on Current cell [" + ObjectName + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to double click. Error : " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Set vertical scroll position
     * SAP API: table.VerticalScrollbar.Position = position
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set vertical scroll position to [<Data>]",
        input = InputType.YES
    )
    public void sapSetVerticalScrollPosition() {
        try {
            Dispatch.put(SAPElement, "VerticalScrollbar.Position", Integer.parseInt(Data));
            Report.updateTestLog(Action, "Set vertical scroll position to " + Data, Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set scroll position. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Assert a table's row count against an operator + expected value expression,
     * e.g. {@code ==0} for "no results", {@code >=1} for "at least one match".
     * SAP API: table.RowCount
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Assert that table [<Object>] row count satisfies expression [<Data>] (e.g. ==0, >=1)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = ">=1",
        inputHelp = "operator + expected row count (e.g. ==0, >=1, >10)"
    )
    public void sapAssertTableRowCount() {
        assertRowCount();
    }

    // ============================================================================
    // SECTION 9: GRID/ALV ACTIONS (GuiGridView)
    // ============================================================================

    /**
     * NEW: Select grid row
     * SAP API: grid.SelectRow(row)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select ALV grid row [<Data>]",
        input = InputType.YES
    )
    public void sapSelectGridRow() {
        try {
            Dispatch.call(SAPElement, "SelectRow", Integer.parseInt(Data));
            Report.updateTestLog(Action, "Selected grid row " + Data, Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to select grid row. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Select an ALV grid row by an exact match of a column's text value instead of a
     * hard-coded row index - the row order can change (sort/filter/new entries) without
     * breaking the step.
     * Data format: matchColumn,matchText
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select ALV grid row where column exactly matches [<Data>] (format: column,text)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "Description,Vendor Invoice",
        inputHelp = "column name + expected text (format: column,text)"
    )
    public void sapSelectGridRowByText() {
        selectRowByText(RowMatcher.MatchMode.EQUALS);
    }

    /**
     * NEW: Select an ALV grid row by a substring match of a column's text value instead of a
     * hard-coded row index.
     * Data format: matchColumn,matchText
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select ALV grid row where column contains [<Data>] (format: column,text)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "Description,Vendor Invoice",
        inputHelp = "column name + expected text (format: column,text)"
    )
    public void sapSelectGridRowByTextContains() {
        selectRowByText(RowMatcher.MatchMode.CONTAINS);
    }

    /**
     * NEW: Select grid column
     * SAP API: grid.SelectColumn(columnName)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select ALV grid column [<Data>]",
        input = InputType.YES
    )
    public void sapSelectGridColumn() {
        try {
            Dispatch.call(SAPElement, "SelectColumn", Data);
            Report.updateTestLog(Action, "Selected grid column [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to select grid column. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Set current grid cell
     * SAP API: grid.SetCurrentCell(row, column)
     * Data format: row,column
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set current grid cell [<Data>] (format: row,column)",
        input = InputType.YES
    )
    public void sapSetGridCurrentCell() {
        try {
            String[] parts = Data.split(",", 2);
            if (parts.length < 2) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: row,column",
                    Status.FAIL
                );
                return;
            }
            int row = Integer.parseInt(parts[0].trim());
            String column = parts[1].trim();

            Dispatch.call(SAPElement, "SetCurrentCell", row, column);
            Report.updateTestLog(
                Action,
                "Set current grid cell to row " + row + ", column " + column,
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set current grid cell. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Store an ALV grid cell's value into a variable.
     * SAP API: grid.GetCellValue(row, column)
     * Data format: row,column | Condition: destination variable in %var% format
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Get grid cell value [<Data>] (format: row,column) and store into variable [<Condition>]",
        input = InputType.YES,
        condition = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "2,VendorName",
        inputHelp = "row + column (format: row,column)",
        condition = ConditionKind.TEXT,
        conditionExample = "%gridCellValue%",
        conditionHelp = "destination variable in %var% format to store the captured value"
    )
    public void sapStoreGridValueByIndexInVariable() {
        try {
            String[] parts = Data.split(",", 2);
            if (parts.length < 2) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: row,column",
                    Status.FAIL
                );
                return;
            }
            int row = Integer.parseInt(parts[0].trim());
            String column = parts[1].trim();

            String cellValue = Dispatch.call(SAPElement, "GetCellValue", row, column).toString();
            addVar(Condition, cellValue);
            Report.updateTestLog(
                Action,
                "Got grid cell value [" +
                cellValue +
                "] from row " +
                row +
                ", column " +
                column +
                " into variable [" +
                Condition +
                "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to get grid cell value. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Store an ALV grid cell's value into a datasheet cell, complementing
     * {@link #sapStoreGridValueByIndexInVariable()} which stores into a variable instead.
     * SAP API: grid.GetCellValue(row, column)
     * Data format: row,column,Sheet:Column
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Get grid cell value and store into datasheet [<Data>] (format: row,column,Sheet:Column)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "2,VendorName,SapResults:Vendor",
        inputHelp = "row + column + destination datasheet reference (format: row,column,Sheet:Column)"
    )
    public void sapStoreGridValueByIndexInDataSheet() {
        try {
            String[] parts = Data.split(",", 3);
            if (parts.length < 3) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: row,column,Sheet:Column",
                    Status.FAIL
                );
                return;
            }
            int row = Integer.parseInt(parts[0].trim());
            String column = parts[1].trim();
            String[] sheetRef = parts[2].trim().split(":", 2);
            if (sheetRef.length < 2) {
                Report.updateTestLog(
                    Action,
                    "Invalid datasheet reference. Expected: Sheet:Column",
                    Status.FAIL
                );
                return;
            }
            String sheetName = sheetRef[0].trim();
            String columnName = sheetRef[1].trim();

            String cellValue = Dispatch.call(SAPElement, "GetCellValue", row, column).toString();
            userData.putData(sheetName, columnName, cellValue);
            Report.updateTestLog(
                Action,
                "Got grid cell value [" +
                cellValue +
                "] from row " +
                row +
                ", column " +
                column +
                " into datasheet [" +
                sheetName +
                ":" +
                columnName +
                "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to get grid cell value. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Locate an ALV grid row by one column's text (e.g. job name) and store a *different*
     * column's value (e.g. Status, End time) from that same row into a variable - exact match.
     * Data format: matchColumn,matchText,targetColumn | Condition: destination variable
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Store column [<Data>] from grid row matched by another column, into variable [<Condition>] (format: matchColumn,matchText,targetColumn)",
        input = InputType.YES,
        condition = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "JobName,BATCH_JOB_1,Status",
        inputHelp = "match column, match text, target column to read (format: matchColumn,matchText,targetColumn)",
        condition = ConditionKind.TEXT,
        conditionExample = "%jobStatus%",
        conditionHelp = "destination variable in %var% format to store the captured value"
    )
    public void sapStoreGridValueByMatchInVariable() {
        storeCellValueByMatchInVariable(RowMatcher.MatchMode.EQUALS);
    }

    /** NEW: {@link #sapStoreGridValueByMatchInVariable()} with a substring match column. */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Store column [<Data>] from grid row matched (contains) by another column, into variable [<Condition>] (format: matchColumn,matchText,targetColumn)",
        input = InputType.YES,
        condition = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "JobName,BATCH_JOB,Status",
        inputHelp = "match column, match text, target column to read (format: matchColumn,matchText,targetColumn)",
        condition = ConditionKind.TEXT,
        conditionExample = "%jobStatus%",
        conditionHelp = "destination variable in %var% format to store the captured value"
    )
    public void sapStoreGridValueByMatchInVariableContains() {
        storeCellValueByMatchInVariable(RowMatcher.MatchMode.CONTAINS);
    }

    /**
     * NEW: Locate an ALV grid row by one column's text and store a *different* column's value
     * from that same row into a datasheet cell instead of a variable - exact match.
     * Data format: matchColumn,matchText,targetColumn,Sheet:Column
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Store column and grid row matched by another column, into datasheet [<Data>] (format: matchColumn,matchText,targetColumn,Sheet:Column)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "JobName,BATCH_JOB_1,Status,SapResults:JobStatus",
        inputHelp = "match column, match text, target column, destination datasheet reference (format: matchColumn,matchText,targetColumn,Sheet:Column)"
    )
    public void sapStoreGridValueByMatchInDataSheet() {
        storeCellValueByMatchInDataSheet(RowMatcher.MatchMode.EQUALS);
    }

    /** NEW: {@link #sapStoreGridValueByMatchInDataSheet()} with a substring match column. */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Store column and grid row matched (contains) by another column, into datasheet [<Data>] (format: matchColumn,matchText,targetColumn,Sheet:Column)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "JobName,BATCH_JOB,Status,SapResults:JobStatus",
        inputHelp = "match column, match text, target column, destination datasheet reference (format: matchColumn,matchText,targetColumn,Sheet:Column)"
    )
    public void sapStoreGridValueByMatchInDataSheetContains() {
        storeCellValueByMatchInDataSheet(RowMatcher.MatchMode.CONTAINS);
    }

    /**
     * NEW: Click current grid cell
     * SAP API: grid.ClickCurrentCell()
     */
    @Action(object = ObjectType.SAP_OBJECT, desc = "Click current grid cell", input = InputType.NO)
    public void sapClickGridCurrentCell() {
        try {
            Dispatch.call(SAPElement, "ClickCurrentCell");
            Report.updateTestLog(
                Action,
                "Clicked current grid cell in [" + ObjectName + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to click current grid cell. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Press toolbar button on grid
     * SAP API: grid.PressToolbarButton(buttonId)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Press grid toolbar button [<Data>]",
        input = InputType.YES
    )
    public void sapPressToolbarButton() {
        try {
            Dispatch.call(SAPElement, "PressToolbarButton", Data);
            Report.updateTestLog(Action, "Pressed toolbar button [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to press toolbar button. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Press toolbar context button
     * SAP API: grid.PressToolbarContextButton(buttonId)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Press grid toolbar context button [<Data>]",
        input = InputType.YES
    )
    public void sapPressToolbarContextButton() {
        try {
            Dispatch.call(SAPElement, "PressToolbarContextButton", Data);
            Report.updateTestLog(
                Action,
                "Pressed toolbar context button [" + Data + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to press toolbar context button. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Select all rows in grid
     * SAP API: grid.SelectAll()
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select all rows in grid [<Object>]",
        input = InputType.NO
    )
    public void sapSelectAllGrid() {
        try {
            Dispatch.call(SAPElement, "SelectAll");
            Report.updateTestLog(
                Action,
                "Selected all rows in grid [" + ObjectName + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to select all. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Deselect grid row
     * SAP API: grid.DeselectRow(row)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Deselect grid row [<Data>]",
        input = InputType.YES
    )
    public void sapDeselectRow() {
        try {
            Dispatch.call(SAPElement, "DeselectRow", Integer.parseInt(Data));
            Report.updateTestLog(Action, "Deselected grid row " + Data, Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to deselect row. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Clear grid selection
     * SAP API: grid.clearSelection()
     */
    @Action(object = ObjectType.SAP_OBJECT, desc = "Clear selection on [<Object>]")
    public void sapClearSelection() {
        try {
            Dispatch.call(SAPElement, "clearSelection");
            Report.updateTestLog(Action, "Cleared selection on [" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to clear selection. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Set grid filter
     * SAP API: grid.SetFilter(column, filterValue)
     * Data format: column,filterValue
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set grid filter [<Data>] (format: column,filterValue)",
        input = InputType.YES
    )
    public void sapSetGridFilter() {
        try {
            String[] parts = Data.split(",", 2);
            if (parts.length < 2) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: column,filterValue",
                    Status.FAIL
                );
                return;
            }
            String column = parts[0].trim();
            String filterValue = parts[1].trim();

            Dispatch.call(SAPElement, "SetFilter", column, filterValue);
            Report.updateTestLog(
                Action,
                "Set filter on column [" + column + "] with value [" + filterValue + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set filter. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Clear grid filter
     * SAP API: grid.ClearFilter()
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Clear grid filter on [<Object>]",
        input = InputType.NO
    )
    public void sapClearGridFilter() {
        try {
            Dispatch.call(SAPElement, "ClearFilter");
            Report.updateTestLog(
                Action,
                "Cleared filter on grid [" + ObjectName + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to clear filter. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Set selected rows
     * SAP API: grid.selectedRows = value
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set selected rows to [<Data>]",
        input = InputType.YES
    )
    public void sapSetSelectedRows() {
        try {
            Dispatch.put(SAPElement, "selectedRows", Data);
            Report.updateTestLog(Action, "Set selected rows to [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set selected rows. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Set current cell column
     * SAP API: grid.CurrentCellColumn = column
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set current cell column to [<Data>]",
        input = InputType.YES
    )
    public void sapSetCurrentCellColumn() {
        try {
            Dispatch.put(SAPElement, "CurrentCellColumn", Data);
            Report.updateTestLog(Action, "Set current cell column to [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set current cell column. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Set first visible column
     * SAP API: grid.firstVisibleColumn = column
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set first visible column to [<Data>]",
        input = InputType.YES
    )
    public void sapSetFirstVisibleColumn() {
        try {
            Dispatch.put(SAPElement, "firstVisibleColumn", Data);
            Report.updateTestLog(Action, "Set first visible column to [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set first visible column. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Set first visible row
     * SAP API: grid.FirstVisibleRow = row
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set first visible row to [<Data>]",
        input = InputType.YES
    )
    public void sapSetFirstVisibleRow() {
        try {
            Dispatch.put(SAPElement, "FirstVisibleRow", Integer.parseInt(Data));
            Report.updateTestLog(Action, "Set first visible row to " + Data, Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set first visible row. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Assert an ALV grid's row count against an operator + expected value expression,
     * e.g. {@code ==0} for "no results", {@code >=1} for "at least one match".
     * SAP API: grid.RowCount
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Assert that grid [<Object>] row count satisfies expression [<Data>] (e.g. ==0, >=1)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = ">=1",
        inputHelp = "operator + expected row count (e.g. ==0, >=1, >10)"
    )
    public void sapAssertGridRowCount() {
        assertRowCount();
    }

    // ============================================================================
    // SECTION 10: MENU & TOOLBAR ACTIONS
    // ============================================================================

    /**
     * NEW: Select menu item
     * SAP API: menu.Select()
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select menu item [<Object>]",
        input = InputType.NO
    )
    public void sapSelectMenuItem() {
        try {
            Dispatch.call(SAPElement, "Select");
            Report.updateTestLog(Action, "Selected menu item [" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to select menu item. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    // ============================================================================
    // SECTION 11: CONTEXT MENU ACTIONS
    // ============================================================================

    /**
     * NEW: Press context button
     * SAP API: element.pressContextButton(buttonId)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Press context button with parameter [<Data>]",
        input = InputType.YES
    )
    public void sapPressContextButton() {
        try {
            if (Data != null && !Data.isEmpty()) {
                Dispatch.call(SAPElement, "pressContextButton", Data);
                Report.updateTestLog(
                    Action,
                    "Pressed context button with parameter [" + Data + "]",
                    Status.DONE
                );
            } else {
                Dispatch.call(SAPElement, "pressContextButton");
                Report.updateTestLog(
                    Action,
                    "Pressed context button on [" + ObjectName + "]",
                    Status.DONE
                );
            }
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to press context button. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Select context menu item
     * SAP API: element.selectContextMenuItem(menuId)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select context menu item [<Data>]",
        input = InputType.YES
    )
    public void sapSelectContextMenuItem() {
        try {
            Dispatch.call(SAPElement, "selectContextMenuItem", Data);
            Report.updateTestLog(Action, "Selected context menu item [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to select context menu item. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    // ============================================================================
    // SECTION 12: TREE CONTROL ACTIONS (GuiTree)
    // ============================================================================

    /**
     * NEW: Expand tree node
     * SAP API: tree.ExpandNode(nodeKey)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Expand tree node [<Data>]",
        input = InputType.YES
    )
    public void sapExpandTreeNode() {
        try {
            Dispatch.call(SAPElement, "ExpandNode", Data);
            Report.updateTestLog(Action, "Expanded tree node [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to expand tree node. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Collapse tree node
     * SAP API: tree.CollapseNode(nodeKey)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Collapse tree node [<Data>]",
        input = InputType.YES
    )
    public void sapCollapseTreeNode() {
        try {
            Dispatch.call(SAPElement, "CollapseNode", Data);
            Report.updateTestLog(Action, "Collapsed tree node [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to collapse tree node. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Select tree node
     * SAP API: tree.SelectNode(nodeKey)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Select tree node [<Data>]",
        input = InputType.YES
    )
    public void sapSelectTreeNode() {
        try {
            Dispatch.call(SAPElement, "SelectNode", Data);
            Report.updateTestLog(Action, "Selected tree node [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to select tree node. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Double-click tree node
     * SAP API: tree.DoubleClickNode(nodeKey)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Double-click tree node [<Data>]",
        input = InputType.YES
    )
    public void sapDoubleClickTreeNode() {
        try {
            Dispatch.call(SAPElement, "DoubleClickNode", Data);
            Report.updateTestLog(Action, "Double-clicked tree node [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to double-click tree node. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Set top node in tree
     * SAP API: tree.topNode = nodeKey
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set top node to [<Data>]",
        input = InputType.YES
    )
    public void sapSetTopNode() {
        try {
            Dispatch.put(SAPElement, "topNode", Data);
            Report.updateTestLog(Action, "Set top node to [" + Data + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set top node. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Expand a parent tree node and select its child node in one step - removes the
     * open-then-click sequencing guesswork of driving {@link #sapExpandTreeNode()} and
     * {@link #sapSelectTreeNode()} as two separate, order-sensitive steps.
     * Data format: parentNodeKey\childNodeKey
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Navigate tree path [<Data>] (format: parentNodeKey\\childNodeKey)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "Root\\Child",
        inputHelp = "parent node key + child node key, separated by a backslash"
    )
    public void sapNavigateTreePath() {
        try {
            String[] parts = Data.split("\\\\", 2);
            if (parts.length < 2) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: parentNodeKey\\childNodeKey",
                    Status.FAIL
                );
                return;
            }
            String parentKey = parts[0].trim();
            String childKey = parts[1].trim();

            Dispatch.call(SAPElement, "ExpandNode", parentKey);
            Dispatch.call(SAPElement, "SelectNode", childKey);
            Report.updateTestLog(
                Action,
                "Expanded node [" + parentKey + "] and selected child node [" + childKey + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to navigate tree path. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * Finds the child node (of {@code parentKey}, via {@code GetSubNodesCol}) whose visible
     * text ({@code GetNodeTextByKey}) matches - so the child can be resolved without knowing
     * its raw SAP GUI node key up front.
     * @return the matching child's node key, or null if none matched
     */
    private String findMatchingChildNodeKey(
        String parentKey,
        String childText,
        RowMatcher.MatchMode mode
    ) {
        Dispatch subNodes = Dispatch.call(SAPElement, "GetSubNodesCol", parentKey).toDispatch();
        int count = Integer.parseInt(Dispatch.get(subNodes, "Count").toString());
        int index = RowMatcher.findRow(
            count,
            i ->
                Dispatch
                    .call(
                        SAPElement,
                        "GetNodeTextByKey",
                        Dispatch.call(subNodes, "ElementAt", i).toString()
                    )
                    .toString(),
            childText,
            mode
        );
        return index < 0 ? null : Dispatch.call(subNodes, "ElementAt", index).toString();
    }

    /**
     * Shared body for the exact/contains variants of {@code sapSelectTreeNodeByText} - expands
     * the parent node and selects the first child whose visible text matches, instead of
     * requiring the raw SAP GUI child node key up front (complements {@link #sapNavigateTreePath()}).
     * Data format: parentNodeKey\childText
     */
    private void selectTreeNodeByText(RowMatcher.MatchMode mode) {
        try {
            String[] parts = Data.split("\\\\", 2);
            if (parts.length < 2) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: parentNodeKey\\childText",
                    Status.FAIL
                );
                return;
            }
            String parentKey = parts[0].trim();
            String childText = parts[1].trim();

            Dispatch.call(SAPElement, "ExpandNode", parentKey);
            String childKey = findMatchingChildNodeKey(parentKey, childText, mode);
            if (childKey == null) {
                Report.updateTestLog(
                    Action,
                    "No child node found under [" + parentKey + "] with text [" + childText + "]",
                    Status.FAIL
                );
                return;
            }
            Dispatch.call(SAPElement, "SelectNode", childKey);
            Report.updateTestLog(
                Action,
                "Expanded node [" +
                parentKey +
                "] and selected child node [" +
                childKey +
                "] matching text [" +
                childText +
                "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to select tree node by text. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /** NEW: {@link #selectTreeNodeByText(RowMatcher.MatchMode)} with an exact child-text match. */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Expand parent and select child tree node where text exactly matches [<Data>] (format: parentNodeKey\\childText)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "Root\\Vendor Invoice",
        inputHelp = "parent node key + expected child text, separated by a backslash"
    )
    public void sapSelectTreeNodeByText() {
        selectTreeNodeByText(RowMatcher.MatchMode.EQUALS);
    }

    /** NEW: {@link #selectTreeNodeByText(RowMatcher.MatchMode)} with a substring child-text match. */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Expand parent and select child tree node where text contains [<Data>] (format: parentNodeKey\\childText)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "Root\\Vendor Invoice",
        inputHelp = "parent node key + expected child text, separated by a backslash"
    )
    public void sapSelectTreeNodeByTextContains() {
        selectTreeNodeByText(RowMatcher.MatchMode.CONTAINS);
    }

    // ============================================================================
    // SECTION 13: STATUS BAR ACTIONS (GuiStatusBar)
    // ============================================================================

    /**
     * NEW: Store the status bar text into a variable.
     * SAP API: statusBar.Text
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Get status bar text from [<Object>] and store into variable [<Condition>]",
        input = InputType.NO,
        condition = InputType.YES
    )
    @Args(
        condition = ConditionKind.TEXT,
        conditionExample = "%statusText%",
        conditionHelp = "destination variable in %var% format to store the captured status bar text"
    )
    public void sapStoreStatusBarTextInVariable() {
        try {
            String statusText = Dispatch.get(SAPElement, "Text").toString();
            addVar(Condition, statusText);
            Report.updateTestLog(
                Action,
                "Status bar text [" + statusText + "] stored into variable [" + Condition + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to get status bar text. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Store the status bar text into a datasheet cell, complementing
     * {@link #sapStoreStatusBarTextInVariable()} which stores into a variable instead.
     * SAP API: statusBar.Text
     * Data format: Sheet:Column
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Get status bar text from [<Object>] and store into datasheet [<Data>] (format: Sheet:Column)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.DATA_REF,
        inputExample = "SapResults:StatusText",
        inputHelp = "destination datasheet reference in Sheet:Column format"
    )
    public void sapStoreStatusBarTextInDataSheet() {
        try {
            String[] parts = Data.split(":", 2);
            if (parts.length < 2) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: Sheet:Column",
                    Status.FAIL
                );
                return;
            }
            String sheetName = parts[0].trim();
            String columnName = parts[1].trim();

            String statusText = Dispatch.get(SAPElement, "Text").toString();
            userData.putData(sheetName, columnName, statusText);
            Report.updateTestLog(
                Action,
                "Status bar text [" + statusText + "] stored into datasheet [" + Data + "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to get status bar text. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Assert the status bar message type/severity (S=success, W=warning, E=error,
     * I=info, A=abort) against an expected value - the common "assert no error after this
     * transaction" check, distinct from the message text itself.
     * SAP API: statusBar.MessageType
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Assert that status bar [<Object>] message type equals [<Data>] (S/W/E/I/A)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "S",
        inputHelp = "expected message type: S=success, W=warning, E=error, I=info, A=abort"
    )
    public void sapAssertStatusBarType() {
        try {
            String actualType = Dispatch.get(SAPElement, "MessageType").toString();
            if (actualType.equalsIgnoreCase(Data)) {
                Report.updateTestLog(
                    Action,
                    "[" +
                    ObjectName +
                    "] message type [" +
                    actualType +
                    "] equals expected type [" +
                    Data +
                    "].",
                    Status.PASS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "[" +
                    ObjectName +
                    "] message type [" +
                    actualType +
                    "] does not equal expected type [" +
                    Data +
                    "].",
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to get status bar message type. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    // ============================================================================
    // SECTION 14: GENERAL ELEMENT ACTIONS
    // ============================================================================

    /**
     * EXISTING: Set focus on element
     * SAP API: element.setFocus()
     */
    @Action(object = ObjectType.SAP_OBJECT, desc = "Set focus on [<Object>]", input = InputType.NO)
    public void sapSetFocus() {
        try {
            Dispatch.call(SAPElement, "setFocus");
            Report.updateTestLog(Action, "Focus has been set on [" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to set focus on [" + ObjectName + "]. Error : " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * EXISTING: Double-click on element
     * SAP API: element.doubleClick()
     */
    @Action(object = ObjectType.SAP_OBJECT, desc = "Double click on [<Object>]")
    public void sapDoubleClick() {
        try {
            Dispatch.call(SAPElement, "doubleClick");
            Report.updateTestLog(Action, "Double clicked on [" + ObjectName + "]", Status.DONE);
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to double click. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Soft assertion (PASSNS/FAILNS, never throws) that [<Object>] resolved to a real SAP
     * element - complements the hard {@link ElementException} thrown by
     * {@link #validateSAPElement()}-guarded actions like Fill/Click.
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Assert that [<Object>] is present",
        input = InputType.NO
    )
    public void sapAssertElementPresent() {
        if (SAPElement != null) {
            Report.updateTestLog(Action, "[" + ObjectName + "] is present.", Status.PASS);
        } else {
            Report.updateTestLog(Action, "[" + ObjectName + "] is not present.", Status.FAIL);
        }
    }

    /** NEW: Assert that [<Object>] is not present, the complement of {@link #sapAssertElementPresent()}. */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Assert that [<Object>] is not present",
        input = InputType.NO
    )
    public void sapAssertElementNotPresent() {
        if (SAPElement == null) {
            Report.updateTestLog(
                Action,
                "[" + ObjectName + "] is not present, as expected.",
                Status.PASS
            );
        } else {
            Report.updateTestLog(
                Action,
                "[" + ObjectName + "] is present, but expected it not to be.",
                Status.FAIL
            );
        }
    }

    /**
     * EXISTING: Assert element contains text
     * SAP API: element.Text (with assertion logic)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Assert that [<Object>] contains Text [<Data>]",
        input = InputType.YES
    )
    public void sapAssertElementTextContains() {
        try {
            String actualText = Dispatch.get(SAPElement, "Text").toString();
            if (actualText.contains(Data)) {
                Report.updateTestLog(
                    Action,
                    "[" +
                    ObjectName +
                    "] actual text [" +
                    actualText +
                    "] contains expected text [" +
                    Data +
                    "].",
                    Status.PASS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "[" +
                    ObjectName +
                    "] actual text [" +
                    actualText +
                    "] not contains expected text [" +
                    Data +
                    "].",
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Fails to get Element text. Error : " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Assert element does not contain text, complementing {@link #sapAssertElementTextContains()}.
     * SAP API: element.Text (with assertion logic)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Assert that [<Object>] does not contain Text [<Data>]",
        input = InputType.YES
    )
    public void sapAssertElementTextNotContains() {
        try {
            String actualText = Dispatch.get(SAPElement, "Text").toString();
            if (!actualText.contains(Data)) {
                Report.updateTestLog(
                    Action,
                    "[" +
                    ObjectName +
                    "] actual text [" +
                    actualText +
                    "] does not contain [" +
                    Data +
                    "], as expected.",
                    Status.PASS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "[" +
                    ObjectName +
                    "] actual text [" +
                    actualText +
                    "] contains [" +
                    Data +
                    "], but expected it not to.",
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Fails to get Element text. Error : " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Capture the text of any SAP element (window title, result field, ST22 dump
     * field, etc.) into a variable, generalizing {@link #sapStoreStatusBarTextInVariable()}.
     * SAP API: element.Text
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Get text from [<Object>] and store into variable [<Condition>]",
        input = InputType.NO,
        condition = InputType.YES
    )
    @Args(
        condition = ConditionKind.TEXT,
        conditionExample = "%resultText%",
        conditionHelp = "destination variable in %var% format to store the captured text"
    )
    public void sapStoreElementTextInVariable() {
        try {
            String text = Dispatch.get(SAPElement, "Text").toString();
            addVar(Condition, text);
            Report.updateTestLog(
                Action,
                "Captured text [" +
                text +
                "] from [" +
                ObjectName +
                "] into variable [" +
                Condition +
                "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to get text from [" + ObjectName + "]. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Capture the text of any SAP element into a datasheet cell, complementing
     * {@link #sapStoreElementTextInVariable()} which stores into a variable instead.
     * Data format: Sheet:Column
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Get text from [<Object>] and store into datasheet [<Data>] (format: Sheet:Column)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.DATA_REF,
        inputExample = "SapResults:ErrorText",
        inputHelp = "destination datasheet reference in Sheet:Column format"
    )
    public void sapStoreElementTextInDataSheet() {
        try {
            String[] parts = Data.split(":", 2);
            if (parts.length < 2) {
                Report.updateTestLog(
                    Action,
                    "Invalid data format. Expected: Sheet:Column",
                    Status.FAIL
                );
                return;
            }
            String sheetName = parts[0].trim();
            String columnName = parts[1].trim();

            String text = Dispatch.get(SAPElement, "Text").toString();
            userData.putData(sheetName, columnName, text);
            Report.updateTestLog(
                Action,
                "Captured text [" +
                text +
                "] from [" +
                ObjectName +
                "] into datasheet [" +
                Data +
                "]",
                Status.DONE
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to get text from [" + ObjectName + "]. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Assert element text (or window title, exposed via the same Text property)
     * equals an expected value, complementing the "contains" only
     * {@link #sapAssertElementTextContains()}.
     * SAP API: element.Text (with assertion logic)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Assert that [<Object>] text equals [<Data>]",
        input = InputType.YES
    )
    public void sapAssertElementTextEquals() {
        try {
            String actualText = Dispatch.get(SAPElement, "Text").toString();
            if (actualText.equals(Data)) {
                Report.updateTestLog(
                    Action,
                    "[" +
                    ObjectName +
                    "] actual text [" +
                    actualText +
                    "] equals expected text [" +
                    Data +
                    "].",
                    Status.PASS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "[" +
                    ObjectName +
                    "] actual text [" +
                    actualText +
                    "] does not equal expected text [" +
                    Data +
                    "].",
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Fails to get Element text. Error : " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Assert element text numerically against an operator + expected value
     * expression, e.g. {@code <>0}, {@code >=100}.
     * SAP API: element.Text (with numeric assertion logic)
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Assert that [<Object>] numeric text satisfies expression [<Data>] (e.g. <>0, >=100)",
        input = InputType.YES
    )
    @Args(
        input = ArgType.TEXT,
        inputExample = "<>0",
        inputHelp = "operator + expected numeric value (e.g. <>0, >=100, ==5, !=10)"
    )
    public void sapAssertElementNumericValue() {
        String actualText = null;
        try {
            actualText = Dispatch.get(SAPElement, "Text").toString();
            NumberConditionEvaluator.Condition parsed = NumberConditionEvaluator.parse(Data);
            boolean result = NumberConditionEvaluator.evaluate(
                actualText,
                parsed.operator(),
                parsed.expected()
            );
            if (result) {
                Report.updateTestLog(
                    Action,
                    "[" +
                    ObjectName +
                    "] value [" +
                    actualText +
                    "] satisfies condition [" +
                    Data +
                    "]",
                    Status.PASS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "[" +
                    ObjectName +
                    "] value [" +
                    actualText +
                    "] does not satisfy condition [" +
                    Data +
                    "]",
                    Status.FAIL
                );
            }
        } catch (NumberFormatException e) {
            Report.updateTestLog(
                Action,
                "[" +
                ObjectName +
                "] value [" +
                actualText +
                "] is not numeric. Error: " +
                e.getMessage(),
                Status.FAIL
            );
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to assert numeric condition on [" +
                ObjectName +
                "]. Error: " +
                e.getMessage(),
                Status.FAIL
            );
        }
    }

    /**
     * NEW: Assert that a field is enabled/changeable (as opposed to greyed-out/read-only) -
     * useful for verifying a field became locked after a business action (e.g. after posting).
     * SAP API: element.Changeable
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Assert that [<Object>] is enabled/changeable",
        input = InputType.NO
    )
    public void sapAssertElementEnabled() {
        try {
            boolean changeable = Boolean.parseBoolean(
                Dispatch.get(SAPElement, "Changeable").toString()
            );
            if (changeable) {
                Report.updateTestLog(
                    Action,
                    "[" + ObjectName + "] is enabled/changeable.",
                    Status.PASS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "[" + ObjectName + "] is not enabled/changeable.",
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to get Changeable property. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    /** NEW: Assert that a field is disabled/read-only, the complement of {@link #sapAssertElementEnabled()}. */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Assert that [<Object>] is disabled/not changeable",
        input = InputType.NO
    )
    public void sapAssertElementNotEnabled() {
        try {
            boolean changeable = Boolean.parseBoolean(
                Dispatch.get(SAPElement, "Changeable").toString()
            );
            if (!changeable) {
                Report.updateTestLog(
                    Action,
                    "[" + ObjectName + "] is disabled/not changeable, as expected.",
                    Status.PASS
                );
            } else {
                Report.updateTestLog(
                    Action,
                    "[" + ObjectName + "] is enabled/changeable, but expected it not to be.",
                    Status.FAIL
                );
            }
        } catch (Exception e) {
            Report.updateTestLog(
                Action,
                "Failed to get Changeable property. Error: " + e.getMessage(),
                Status.FAIL
            );
        }
    }

    // ============================================================================
    // SECTION 15: EXECUTION EVIDENCE
    // ============================================================================

    /**
     * NEW: On-demand SAP screenshot capture, mirroring the browser's
     * {@code CommonMethods.TakePageScreenshot()}. Uses {@code Status.SCREENSHOT} so the capture
     * is forced regardless of the {@code screenShotFor} (Fail/Pass/Both) setting - see
     * {@code SapDriverCreation} for how the image itself is obtained (SAP GUI Scripting has no
     * bitmap-capture API, so this crops an OS-level capture to the active window's bounds).
     */
    @Action(object = ObjectType.SAP, desc = "Capture a screenshot of the active SAP window")
    public void sapCaptureScreenshot() {
        Report.updateTestLog(Action, "SAP screenshot captured", Status.SCREENSHOT);
    }

    /**
     * EXISTING: Set object property at runtime
     * Dynamic property setting for specific SAP object
     */
    @Action(
        object = ObjectType.SAP_OBJECT,
        desc = "Set object [<Object>] property as [<Data>] at runtime",
        input = InputType.YES,
        condition = InputType.YES
    )
    public void sapSetObjectProperty() {
        if (!Data.isEmpty()) {
            if (Condition.isEmpty()) {
                String[] groups = Data.split(",");
                for (String group : groups) {
                    String[] vals = group.split("=", 2);
                    setProperty(vals[0], vals[1]);
                }
            } else {
                setProperty(Condition, Data);
            }
            String text = String.format(
                "Setting Object Property for [%s] with [%s] for Object [%s - %s]",
                Condition,
                Data,
                Reference,
                ObjectName
            );
            Report.updateTestLog(Action, text, Status.DONE);
        } else {
            Report.updateTestLog(Action, "Input should not be empty", Status.FAILNS);
        }
    }

    /**
     * Helper method for setting dynamic properties
     */
    private void setProperty(String key, String value) {
        com.ing.engine.core.InlineObjectProperty.putObjectProperty(
            SAPObject.dynamicValue,
            Reference,
            ObjectName,
            key,
            value
        );
    }
}
