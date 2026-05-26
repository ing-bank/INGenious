package com.ing.engine.commands.SAP;

import java.util.HashMap;
import java.util.Map;

import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.drivers.AutomationObject;
import com.ing.ingenious.api.exception.mobile.ElementException;
import com.ing.ingenious.api.exception.mobile.ElementException.ExceptionType;
import com.ing.ingenious.api.status.Status;
import com.ing.ingenious.api.annotation.Action;
import com.ing.ingenious.api.types.InputType;
import com.ing.ingenious.api.types.ObjectType;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

/**
 * SAP GUI Scripting Actions for INGenious Test Automation Framework
 * 
 * This class provides comprehensive SAP GUI automation actions organized by element type.
 * Actions are grouped into logical sections for better maintainability and discoverability.
 * 
 * Total Actions: 64 (17 Existing + 47 New)
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
	 * Validates that SAPsession exists before attempting operations
	 * @throws ElementException if SAPsession is null
	 */
	private void validateSAPSession() {
		if (SAPsession == null) {
			throw new ElementException(ExceptionType.Element_Not_Found, "SAP Session");
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
			Report.updateTestLog(Action, "Invalid " + fieldName + " format [" + value + "]. Expected integer.", Status.DEBUG);
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
	@Action(object = ObjectType.BROWSER, desc = "Execute SAP transaction [<Data>]", input = InputType.YES)
	public void sapExecuteTransaction() {
		try {
			Dispatch.call(SAPsession, "startTransaction", Data);
			Report.updateTestLog(Action, "Executed transaction [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to execute transaction. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: End current SAP transaction
	 * SAP API: session.endTransaction()
	 */
	@Action(object = ObjectType.BROWSER, desc = "End current SAP transaction", input = InputType.NO)
	public void sapEndTransaction() {
		try {
			Dispatch.call(SAPsession, "endTransaction");
			Report.updateTestLog(Action, "Ended current transaction successfully", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to end transaction. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Refresh SAP session
	 * SAP API: session.Refresh()
	 */
	@Action(object = ObjectType.BROWSER, desc = "Refresh SAP session", input = InputType.NO)
	public void sapRefreshSession() {
		try {
			Dispatch.call(SAPsession, "Refresh");
			Report.updateTestLog(Action, "SAP session refreshed successfully", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to refresh session. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * EXISTING: Close SAP logon landscape screen
	 * Process-level operation
	 */
	@Action(object = ObjectType.BROWSER, desc = "Close SAP logon landscape screen", input = InputType.NO)
	public void sapCloseLogonScreen() {
		try {
			SAPProcess.destroy();
			Report.updateTestLog(Action, "Logon landscape screen closed successfully", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to close Logon landscape screen. Error: " + e.getMessage(),
					Status.FAILNS);
		}
	}

	/**
	 * EXISTING: Set global object property at runtime
	 * Format: property=value or multiple properties separated by comma
	 */
	@Action(object = ObjectType.BROWSER, desc = "Set all objects property to [<Data>] at runtime", input = InputType.YES, condition = InputType.YES)
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
			String text = String.format("Setting Global Object Property for %s with %s", Condition, Data);
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
	@Action(object = ObjectType.SAP, desc = "Maximize SAP window [<Object>]", input = InputType.NO)
	public void sapMaximizeWindow() {
		try {
			Dispatch.call(SAPElement, "Maximize");
			Report.updateTestLog(Action, "Window [" + ObjectName + "] maximized successfully", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to maximize window. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Minimize SAP window
	 * SAP API: window.Minimize()
	 */
	@Action(object = ObjectType.SAP, desc = "Minimize SAP window [<Object>]", input = InputType.NO)
	public void sapMinimizeWindow() {
		try {
			Dispatch.call(SAPElement, "Minimize");
			Report.updateTestLog(Action, "Window [" + ObjectName + "] minimized successfully", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to minimize window. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Restore SAP window to normal size
	 * SAP API: window.Restore()
	 */
	@Action(object = ObjectType.SAP, desc = "Restore SAP window [<Object>] to normal size", input = InputType.NO)
	public void sapRestoreWindow() {
		try {
			Dispatch.call(SAPElement, "Restore");
			Report.updateTestLog(Action, "Window [" + ObjectName + "] restored successfully", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to restore window. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Close SAP window or popup
	 * SAP API: window.Close()
	 */
	@Action(object = ObjectType.SAP, desc = "Close SAP window or popup [<Object>]", input = InputType.NO)
	public void sapCloseWindow() {
		try {
			Dispatch.call(SAPElement, "Close");
			Report.updateTestLog(Action, "Window [" + ObjectName + "] closed successfully", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to close window. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Iconify SAP window
	 * SAP API: window.Iconify()
	 */
	@Action(object = ObjectType.SAP, desc = "Iconify SAP window [<Object>]", input = InputType.NO)
	public void sapIconifyWindow() {
		try {
			Dispatch.call(SAPElement, "Iconify");
			Report.updateTestLog(Action, "Window [" + ObjectName + "] iconified successfully", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to iconify window. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Resize working pane
	 * SAP API: window.resizeWorkingPane(width, height, fullScreen)
	 * Data format: width,height[,fullScreen]
	 */
	@Action(object = ObjectType.SAP, desc = "Resize working pane with parameters [<Data>]", input = InputType.YES)
	public void sapResizeWorkingPane() {
		try {
			String[] params = Data.split(",");
			if (params.length >= 2) {
				int width = Integer.parseInt(params[0].trim());
				int height = Integer.parseInt(params[1].trim());
				boolean fullScreen = params.length > 2 ? Boolean.parseBoolean(params[2].trim()) : false;
				
				Variant[] args = new Variant[3];
				args[0] = new Variant(width);
				args[1] = new Variant(height);
				args[2] = new Variant(fullScreen);
				
				Dispatch.call(SAPElement, "resizeWorkingPane", (Object[]) args);
				Report.updateTestLog(Action, "Resized working pane to " + width + "x" + height, Status.DONE);
			} else {
				Report.updateTestLog(Action, "Invalid data format. Expected: width,height[,fullScreen]", Status.FAILNS);
			}
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to resize working pane. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	// ============================================================================
	// SECTION 3: TEXT FIELD ACTIONS (GuiTextField, GuiCTextField)
	// ============================================================================

	/**
	 * EXISTING: Enter text value in field
	 * SAP API: element.Text = value
	 */
	@Action(object = ObjectType.SAP, desc = "Enter the value [<Data>] in the field [<Object>]", input = InputType.YES)
	public void sapFill() {
		try {
			validateSAPElement();
			Dispatch.put(SAPElement, "Text", Data);
			Report.updateTestLog(Action, "Entered Text '" + Data + "' on '" + ObjectName + "'", Status.DONE);
		} catch (ElementException ex) {
			Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAILNS);
			throw ex;
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to Enter Text. Error : " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * EXISTING: Press Enter key
	 * SAP API: element.sendVKey(0)
	 */
	@Action(object = ObjectType.SAP, desc = "Press [<Enter>] key", input = InputType.NO)
	public void sapEnter() {
		try {
			validateSAPElement();
			Dispatch.call(SAPElement, "sendVKey", 0);
			Report.updateTestLog(Action, "Enter key pressed successfully.", Status.DONE);
		} catch (ElementException ex) {
			Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAILNS);
			throw ex;
		} catch (Exception e) {
			Report.updateTestLog(Action, "Error in Enter key press. Error : " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * EXISTING: Simulate virtual key press
	 * SAP API: element.sendVKey(vkey)
	 * VKey codes: 0=Enter, 3=Back, 4=F4, 8=F8/Execute, 11=Save, etc.
	 */
	@Action(object = ObjectType.SAP, desc = "Simulate key press with VCode [<Data>]", input = InputType.YES)
	public void sapSimulateKeyPress() {
		try {
			validateSAPElement();
			int vkeyCode = parseIntSafely(Data, "VKey code");
			Dispatch.call(SAPElement, "sendVKey", vkeyCode);
			Report.updateTestLog(Action, "Simulate key press with VKey code [" + Data + "]", Status.DONE);
		} catch (ElementException ex) {
			Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAILNS);
			throw ex;
		} catch (NumberFormatException ex) {
			Report.updateTestLog(Action, "Invalid VKey code format [" + Data + "]. Expected integer.", Status.FAILNS);
			throw new IllegalArgumentException(ex);
		} catch (Exception e) {
			Report.updateTestLog(Action,
					"Fails to simulate key press with VKey code [" + Data + "]. Error : " + e.getMessage(),
					Status.FAILNS);
		}
	}

	/**
	 * NEW: Select all text in field
	 * SAP API: element.SelectAll()
	 */
	@Action(object = ObjectType.SAP, desc = "Select all text in field [<Object>]", input = InputType.NO)
	public void sapSelectAllText() {
		try {
			validateSAPElement();
			Dispatch.call(SAPElement, "SelectAll");
			Report.updateTestLog(Action, "Selected all text in [" + ObjectName + "]", Status.DONE);
		} catch (ElementException ex) {
			Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAILNS);
			throw ex;
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to select all text. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Set caret position in text field
	 * SAP API: element.caretPosition = position
	 */
	@Action(object = ObjectType.SAP, desc = "Set caret position to [<Data>]", input = InputType.YES)
	public void sapSetCaretPosition() {
		try {
			validateSAPElement();
			int position = parseIntSafely(Data, "caret position");
			Dispatch.put(SAPElement, "caretPosition", position);
			Report.updateTestLog(Action, "Set caret position to " + Data, Status.DONE);
		} catch (ElementException ex) {
			Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAILNS);
			throw ex;
		} catch (NumberFormatException ex) {
			Report.updateTestLog(Action, "Invalid caret position format [" + Data + "]. Expected integer.", Status.FAILNS);
			throw new IllegalArgumentException(ex);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set caret position. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Set modified flag on element
	 * SAP API: element.modified = boolean
	 */
	@Action(object = ObjectType.SAP, desc = "Set modified property to [<Data>]", input = InputType.YES)
	public void sapSetModified() {
		try {
			validateSAPElement();
			if (!Data.toLowerCase().matches("^(true|false)$")) {
				Report.updateTestLog(Action, "Invalid boolean format [" + Data + "]. Expected 'true' or 'false'.", Status.DEBUG);
				throw new IllegalArgumentException("Invalid boolean value: " + Data);
			}
			Dispatch.put(SAPElement, "modified", Boolean.parseBoolean(Data));
			Report.updateTestLog(Action, "Set modified property to " + Data, Status.DONE);
		} catch (ElementException ex) {
			Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAILNS);
			throw ex;
		} catch (IllegalArgumentException ex) {
			Report.updateTestLog(Action, "Invalid boolean format [" + Data + "]. Expected 'true' or 'false'.", Status.FAILNS);
			throw ex;
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set modified property. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	// ============================================================================
	// SECTION 4: BUTTON ACTIONS (GuiButton)
	// ============================================================================

	/**
	 * EXISTING: Click/Press button
	 * SAP API: button.press()
	 */
	@Action(object = ObjectType.SAP, desc = "Click the [<Object>]")
	public void sapClick() {
		try {
			validateSAPElement();
			Dispatch.call(SAPElement, "press");
			Report.updateTestLog(Action, "Clicking on [" + ObjectName + "]", Status.DONE);
		} catch (ElementException ex) {
			Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAILNS);
			throw ex;
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to click. Error : " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Press button by ID or function code
	 * SAP API: toolbar.pressButton(buttonId)
	 */
	@Action(object = ObjectType.SAP, desc = "Press button with ID [<Data>]", input = InputType.YES)
	public void sapPressButton() {
		try {
			validateSAPElement();
			Dispatch.call(SAPElement, "pressButton", Data);
			Report.updateTestLog(Action, "Pressed button [" + Data + "]", Status.DONE);
		} catch (ElementException ex) {
			Report.updateTestLog(Action, "Element [" + ObjectName + "] not found", Status.FAILNS);
			throw ex;
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to press button. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	// ============================================================================
	// SECTION 5: CHECKBOX & RADIO BUTTON ACTIONS
	// ============================================================================

	/**
	 * EXISTING: Select/Deselect checkbox
	 * SAP API: checkbox.Selected = boolean
	 */
	@Action(object = ObjectType.SAP, desc = "Set checkbox [<Object>] to [<Data>] (true/false)", input = InputType.YES)
	public void sapSelectCheckBox() {
		try {
			boolean value = Boolean.parseBoolean(Data);
			Dispatch.put(SAPElement, "Selected", value);
			Report.updateTestLog(Action, "Checkbox [" + ObjectName + "] set to " + value, Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Fails to set Checkbox. Error : " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * EXISTING: Select radio button in table row
	 * SAP API: table.GetAbsoluteRow(row).Selected = true
	 */
	@Action(object = ObjectType.SAP, desc = "Select Radio Button in row [<Data>]", input = InputType.YES)
	public void sapSelectRadioButtonInRow() {
		try {
			Dispatch row = Dispatch.call(SAPElement, "GetAbsoluteRow", Data).toDispatch();
			Dispatch.put(row, "Selected", true);
			Report.updateTestLog(Action, "Radio button selected successfully.", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Fails to select radio button. Error : " + e.getMessage(), Status.FAILNS);
		}
	}

	// ============================================================================
	// SECTION 6: COMBOBOX/DROPDOWN ACTIONS (GuiComboBox)
	// ============================================================================

	/**
	 * EXISTING: Select dropdown by visible text
	 * SAP API: comboBox.Text = value
	 */
	@Action(object = ObjectType.SAP, desc = "Select dropdown value by visible text [<Data>]", input = InputType.YES)
	public void sapSelectDropDownByText() {
		try {
			Dispatch.put(SAPElement, "Text", Data);
			Report.updateTestLog(Action, "Dropdown set with text [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set dropdown with text. Error : " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * EXISTING: Select dropdown by key (internal value)
	 * SAP API: comboBox.Key = value
	 */
	@Action(object = ObjectType.SAP, desc = "Select Dropdown value by Key [<Data>]", input = InputType.YES)
	public void sapSelectDropDownByKey() {
		try {
			Dispatch.put(SAPElement, "Key", Data);
			Report.updateTestLog(Action, "Dropdown set with Key [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set dropdown with key. Error : " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * EXISTING: Select dropdown by index
	 * SAP API: comboBox.Select(index)
	 */
	@Action(object = ObjectType.SAP, desc = "Select dropdown value by index [<Data>]", input = InputType.YES)
	public void sapSelectDropDownByIndex() {
		try {
			Dispatch.call(SAPElement, "Select", new Variant(Integer.parseInt(Data)));
			Report.updateTestLog(Action, "Dropdown set with index [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set dropdown with index. Error : " + e.getMessage(),
					Status.FAILNS);
		}
	}

	/**
	 * NEW: Open dropdown list
	 * SAP API: comboBox.Open()
	 */
	@Action(object = ObjectType.SAP, desc = "Open dropdown [<Object>]", input = InputType.NO)
	public void sapOpenComboBox() {
		try {
			Dispatch.call(SAPElement, "Open");
			Report.updateTestLog(Action, "Opened dropdown [" + ObjectName + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to open dropdown. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Close dropdown list
	 * SAP API: comboBox.Close()
	 */
	@Action(object = ObjectType.SAP, desc = "Close dropdown [<Object>]", input = InputType.NO)
	public void sapCloseComboBox() {
		try {
			Dispatch.call(SAPElement, "Close");
			Report.updateTestLog(Action, "Closed dropdown [" + ObjectName + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to close dropdown. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	// ============================================================================
	// SECTION 7: TAB ACTIONS (GuiTabStrip, GuiTab)
	// ============================================================================

	/**
	 * EXISTING: Select tab
	 * SAP API: tab.select()
	 */
	@Action(object = ObjectType.SAP, desc = "Select the [<Object>]")
	public void sapSelect() {
		try {
			Dispatch.call(SAPElement, "select");
			Report.updateTestLog(Action, "Selected tab [" + ObjectName + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to select tab. Error : " + e.getMessage(), Status.FAILNS);
		}
	}

	// ============================================================================
	// SECTION 8: TABLE CONTROL ACTIONS (GuiTableControl)
	// ============================================================================

	/**
	 * NEW: Select table row
	 * SAP API: table.SelectRow(row)
	 */
	@Action(object = ObjectType.SAP, desc = "Select table row [<Data>]", input = InputType.YES)
	public void sapSelectTableRow() {
		try {
			Dispatch.call(SAPElement, "SelectRow", Integer.parseInt(Data));
			Report.updateTestLog(Action, "Selected table row " + Data, Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to select table row. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Set current table cell
	 * SAP API: table.SetCurrentCell(row, column)
	 * Data format: row,column
	 */
	@Action(object = ObjectType.SAP, desc = "Set current table cell [<Data>] (format: row,column)", input = InputType.YES)
	public void sapSetCurrentCell() {
		try {
			String[] parts = Data.split(",", 2);
			if (parts.length < 2) {
				Report.updateTestLog(Action, "Invalid data format. Expected: row,column", Status.FAILNS);
				return;
			}
			int row = Integer.parseInt(parts[0].trim());
			String column = parts[1].trim();
			
			Dispatch.call(SAPElement, "SetCurrentCell", row, column);
			Report.updateTestLog(Action, "Set current cell to row " + row + ", column " + column, Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set current cell. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Set current table cell row
	 * SAP API: table.currentCellRow = row
	 */
	@Action(object = ObjectType.SAP, desc = "Set current table cell row to [<Data>]", input = InputType.YES)
	public void sapSetCurrentCellRow() {
		try {
			Dispatch.put(SAPElement, "currentCellRow", Integer.parseInt(Data));
			Report.updateTestLog(Action, "Set current cell row to " + Data, Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set current cell row. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Get table cell value
	 * SAP API: table.GetCellValue(row, column)
	 * Data format: row,column
	 */
	@Action(object = ObjectType.SAP, desc = "Get cell value [<Data>] (format: row,column)", input = InputType.YES)
	public void sapGetCellValue() {
		try {
			String[] parts = Data.split(",", 2);
			if (parts.length < 2) {
				Report.updateTestLog(Action, "Invalid data format. Expected: row,column", Status.FAILNS);
				return;
			}
			int row = Integer.parseInt(parts[0].trim());
			String column = parts[1].trim();
			
			String cellValue = Dispatch.call(SAPElement, "GetCellValue", row, column).toString();
			addVar("CellValue", cellValue);
			Report.updateTestLog(Action, "Got cell value: " + cellValue + " from row " + row + ", column " + column, Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to get cell value. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Modify table cell value
	 * SAP API: table.modifyCell(row, column, value)
	 * Data format: row,column,value
	 */
	@Action(object = ObjectType.SAP, desc = "Modify table cell with data [<Data>] in format row,column,value", input = InputType.YES)
	public void sapModifyCell() {
		try {
			String[] parts = Data.split(",", 3);
			if (parts.length < 3) {
				Report.updateTestLog(Action, "Invalid data format. Expected: row,column,value", Status.FAILNS);
				return;
			}
			int row = Integer.parseInt(parts[0].trim());
			String column = parts[1].trim();
			String value = parts[2].trim();
			
			Dispatch.call(SAPElement, "modifyCell", row, column, value);
			Report.updateTestLog(Action, "Modified cell at row " + row + ", column " + column + " with value: " + value, Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to modify cell. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Click current table cell
	 * SAP API: table.ClickCurrentCell()
	 */
	@Action(object = ObjectType.SAP, desc = "Click current table cell", input = InputType.NO)
	public void sapClickCurrentCell() {
		try {
			Dispatch.call(SAPElement, "ClickCurrentCell");
			Report.updateTestLog(Action, "Clicked current cell in table [" + ObjectName + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to click current cell. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * EXISTING: Double-click current table cell
	 * SAP API: table.doubleClickCurrentCell()
	 */
	@Action(object = ObjectType.SAP, desc = "Double click the current Cell")
	public void sapDoubleClickCell() {
		try {
			Dispatch.call(SAPElement, "doubleClickCurrentCell");
			Report.updateTestLog(Action, "Double Clicking on Current cell [" + ObjectName + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to double click. Error : " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Set vertical scroll position
	 * SAP API: table.VerticalScrollbar.Position = position
	 */
	@Action(object = ObjectType.SAP, desc = "Set vertical scroll position to [<Data>]", input = InputType.YES)
	public void sapSetVerticalScrollPosition() {
		try {
			Dispatch.put(SAPElement, "VerticalScrollbar.Position", Integer.parseInt(Data));
			Report.updateTestLog(Action, "Set vertical scroll position to " + Data, Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set scroll position. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	// ============================================================================
	// SECTION 9: GRID/ALV ACTIONS (GuiGridView)
	// ============================================================================

	/**
	 * NEW: Select grid row
	 * SAP API: grid.SelectRow(row)
	 */
	@Action(object = ObjectType.SAP, desc = "Select ALV grid row [<Data>]", input = InputType.YES)
	public void sapSelectGridRow() {
		try {
			Dispatch.call(SAPElement, "SelectRow", Integer.parseInt(Data));
			Report.updateTestLog(Action, "Selected grid row " + Data, Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to select grid row. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Select grid column
	 * SAP API: grid.SelectColumn(columnName)
	 */
	@Action(object = ObjectType.SAP, desc = "Select ALV grid column [<Data>]", input = InputType.YES)
	public void sapSelectGridColumn() {
		try {
			Dispatch.call(SAPElement, "SelectColumn", Data);
			Report.updateTestLog(Action, "Selected grid column [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to select grid column. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Set current grid cell
	 * SAP API: grid.SetCurrentCell(row, column)
	 * Data format: row,column
	 */
	@Action(object = ObjectType.SAP, desc = "Set current grid cell [<Data>] (format: row,column)", input = InputType.YES)
	public void sapSetGridCurrentCell() {
		try {
			String[] parts = Data.split(",", 2);
			if (parts.length < 2) {
				Report.updateTestLog(Action, "Invalid data format. Expected: row,column", Status.FAILNS);
				return;
			}
			int row = Integer.parseInt(parts[0].trim());
			String column = parts[1].trim();
			
			Dispatch.call(SAPElement, "SetCurrentCell", row, column);
			Report.updateTestLog(Action, "Set current grid cell to row " + row + ", column " + column, Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set current grid cell. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Get grid cell value
	 * SAP API: grid.GetCellValue(row, column)
	 * Data format: row,column
	 */
	@Action(object = ObjectType.SAP, desc = "Get grid cell value [<Data>] (format: row,column)", input = InputType.YES)
	public void sapGetGridCellValue() {
		try {
			String[] parts = Data.split(",", 2);
			if (parts.length < 2) {
				Report.updateTestLog(Action, "Invalid data format. Expected: row,column", Status.FAILNS);
				return;
			}
			int row = Integer.parseInt(parts[0].trim());
			String column = parts[1].trim();
			
			String cellValue = Dispatch.call(SAPElement, "GetCellValue", row, column).toString();
			addVar("GridCellValue", cellValue);
			Report.updateTestLog(Action, "Got grid cell value: " + cellValue + " from row " + row + ", column " + column, Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to get grid cell value. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Click current grid cell
	 * SAP API: grid.ClickCurrentCell()
	 */
	@Action(object = ObjectType.SAP, desc = "Click current grid cell", input = InputType.NO)
	public void sapClickGridCurrentCell() {
		try {
			Dispatch.call(SAPElement, "ClickCurrentCell");
			Report.updateTestLog(Action, "Clicked current grid cell in [" + ObjectName + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to click current grid cell. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Press toolbar button on grid
	 * SAP API: grid.PressToolbarButton(buttonId)
	 */
	@Action(object = ObjectType.SAP, desc = "Press grid toolbar button [<Data>]", input = InputType.YES)
	public void sapPressToolbarButton() {
		try {
			Dispatch.call(SAPElement, "PressToolbarButton", Data);
			Report.updateTestLog(Action, "Pressed toolbar button [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to press toolbar button. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Press toolbar context button
	 * SAP API: grid.PressToolbarContextButton(buttonId)
	 */
	@Action(object = ObjectType.SAP, desc = "Press grid toolbar context button [<Data>]", input = InputType.YES)
	public void sapPressToolbarContextButton() {
		try {
			Dispatch.call(SAPElement, "PressToolbarContextButton", Data);
			Report.updateTestLog(Action, "Pressed toolbar context button [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to press toolbar context button. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Select all rows in grid
	 * SAP API: grid.SelectAll()
	 */
	@Action(object = ObjectType.SAP, desc = "Select all rows in grid [<Object>]", input = InputType.NO)
	public void sapSelectAllGrid() {
		try {
			Dispatch.call(SAPElement, "SelectAll");
			Report.updateTestLog(Action, "Selected all rows in grid [" + ObjectName + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to select all. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Deselect grid row
	 * SAP API: grid.DeselectRow(row)
	 */
	@Action(object = ObjectType.SAP, desc = "Deselect grid row [<Data>]", input = InputType.YES)
	public void sapDeselectRow() {
		try {
			Dispatch.call(SAPElement, "DeselectRow", Integer.parseInt(Data));
			Report.updateTestLog(Action, "Deselected grid row " + Data, Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to deselect row. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Clear grid selection
	 * SAP API: grid.clearSelection()
	 */
	@Action(object = ObjectType.SAP, desc = "Clear selection on [<Object>]")
	public void sapClearSelection() {
		try {
			Dispatch.call(SAPElement, "clearSelection");
			Report.updateTestLog(Action, "Cleared selection on [" + ObjectName + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to clear selection. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Set grid filter
	 * SAP API: grid.SetFilter(column, filterValue)
	 * Data format: column,filterValue
	 */
	@Action(object = ObjectType.SAP, desc = "Set grid filter [<Data>] (format: column,filterValue)", input = InputType.YES)
	public void sapSetGridFilter() {
		try {
			String[] parts = Data.split(",", 2);
			if (parts.length < 2) {
				Report.updateTestLog(Action, "Invalid data format. Expected: column,filterValue", Status.FAILNS);
				return;
			}
			String column = parts[0].trim();
			String filterValue = parts[1].trim();
			
			Dispatch.call(SAPElement, "SetFilter", column, filterValue);
			Report.updateTestLog(Action, "Set filter on column [" + column + "] with value [" + filterValue + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set filter. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Clear grid filter
	 * SAP API: grid.ClearFilter()
	 */
	@Action(object = ObjectType.SAP, desc = "Clear grid filter on [<Object>]", input = InputType.NO)
	public void sapClearGridFilter() {
		try {
			Dispatch.call(SAPElement, "ClearFilter");
			Report.updateTestLog(Action, "Cleared filter on grid [" + ObjectName + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to clear filter. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Set selected rows
	 * SAP API: grid.selectedRows = value
	 */
	@Action(object = ObjectType.SAP, desc = "Set selected rows to [<Data>]", input = InputType.YES)
	public void sapSetSelectedRows() {
		try {
			Dispatch.put(SAPElement, "selectedRows", Data);
			Report.updateTestLog(Action, "Set selected rows to [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set selected rows. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Set current cell column
	 * SAP API: grid.CurrentCellColumn = column
	 */
	@Action(object = ObjectType.SAP, desc = "Set current cell column to [<Data>]", input = InputType.YES)
	public void sapSetCurrentCellColumn() {
		try {
			Dispatch.put(SAPElement, "CurrentCellColumn", Data);
			Report.updateTestLog(Action, "Set current cell column to [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set current cell column. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Set first visible column
	 * SAP API: grid.firstVisibleColumn = column
	 */
	@Action(object = ObjectType.SAP, desc = "Set first visible column to [<Data>]", input = InputType.YES)
	public void sapSetFirstVisibleColumn() {
		try {
			Dispatch.put(SAPElement, "firstVisibleColumn", Data);
			Report.updateTestLog(Action, "Set first visible column to [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set first visible column. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Set first visible row
	 * SAP API: grid.FirstVisibleRow = row
	 */
	@Action(object = ObjectType.SAP, desc = "Set first visible row to [<Data>]", input = InputType.YES)
	public void sapSetFirstVisibleRow() {
		try {
			Dispatch.put(SAPElement, "FirstVisibleRow", Integer.parseInt(Data));
			Report.updateTestLog(Action, "Set first visible row to " + Data, Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set first visible row. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	// ============================================================================
	// SECTION 10: MENU & TOOLBAR ACTIONS
	// ============================================================================

	/**
	 * NEW: Select menu item
	 * SAP API: menu.Select()
	 */
	@Action(object = ObjectType.SAP, desc = "Select menu item [<Object>]", input = InputType.NO)
	public void sapSelectMenuItem() {
		try {
			Dispatch.call(SAPElement, "Select");
			Report.updateTestLog(Action, "Selected menu item [" + ObjectName + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to select menu item. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	// ============================================================================
	// SECTION 11: CONTEXT MENU ACTIONS
	// ============================================================================

	/**
	 * NEW: Press context button
	 * SAP API: element.pressContextButton(buttonId)
	 */
	@Action(object = ObjectType.SAP, desc = "Press context button with parameter [<Data>]", input = InputType.YES)
	public void sapPressContextButton() {
		try {
			if (Data != null && !Data.isEmpty()) {
				Dispatch.call(SAPElement, "pressContextButton", Data);
				Report.updateTestLog(Action, "Pressed context button with parameter [" + Data + "]", Status.DONE);
			} else {
				Dispatch.call(SAPElement, "pressContextButton");
				Report.updateTestLog(Action, "Pressed context button on [" + ObjectName + "]", Status.DONE);
			}
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to press context button. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Select context menu item
	 * SAP API: element.selectContextMenuItem(menuId)
	 */
	@Action(object = ObjectType.SAP, desc = "Select context menu item [<Data>]", input = InputType.YES)
	public void sapSelectContextMenuItem() {
		try {
			Dispatch.call(SAPElement, "selectContextMenuItem", Data);
			Report.updateTestLog(Action, "Selected context menu item [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to select context menu item. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	// ============================================================================
	// SECTION 12: TREE CONTROL ACTIONS (GuiTree)
	// ============================================================================

	/**
	 * NEW: Expand tree node
	 * SAP API: tree.ExpandNode(nodeKey)
	 */
	@Action(object = ObjectType.SAP, desc = "Expand tree node [<Data>]", input = InputType.YES)
	public void sapExpandTreeNode() {
		try {
			Dispatch.call(SAPElement, "ExpandNode", Data);
			Report.updateTestLog(Action, "Expanded tree node [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to expand tree node. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Collapse tree node
	 * SAP API: tree.CollapseNode(nodeKey)
	 */
	@Action(object = ObjectType.SAP, desc = "Collapse tree node [<Data>]", input = InputType.YES)
	public void sapCollapseTreeNode() {
		try {
			Dispatch.call(SAPElement, "CollapseNode", Data);
			Report.updateTestLog(Action, "Collapsed tree node [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to collapse tree node. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Select tree node
	 * SAP API: tree.SelectNode(nodeKey)
	 */
	@Action(object = ObjectType.SAP, desc = "Select tree node [<Data>]", input = InputType.YES)
	public void sapSelectTreeNode() {
		try {
			Dispatch.call(SAPElement, "SelectNode", Data);
			Report.updateTestLog(Action, "Selected tree node [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to select tree node. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Double-click tree node
	 * SAP API: tree.DoubleClickNode(nodeKey)
	 */
	@Action(object = ObjectType.SAP, desc = "Double-click tree node [<Data>]", input = InputType.YES)
	public void sapDoubleClickTreeNode() {
		try {
			Dispatch.call(SAPElement, "DoubleClickNode", Data);
			Report.updateTestLog(Action, "Double-clicked tree node [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to double-click tree node. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * NEW: Set top node in tree
	 * SAP API: tree.topNode = nodeKey
	 */
	@Action(object = ObjectType.SAP, desc = "Set top node to [<Data>]", input = InputType.YES)
	public void sapSetTopNode() {
		try {
			Dispatch.put(SAPElement, "topNode", Data);
			Report.updateTestLog(Action, "Set top node to [" + Data + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set top node. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	// ============================================================================
	// SECTION 13: STATUS BAR ACTIONS (GuiStatusBar)
	// ============================================================================

	/**
	 * NEW: Get status bar text
	 * SAP API: statusBar.Text
	 */
	@Action(object = ObjectType.SAP, desc = "Get status bar text from [<Object>]", input = InputType.NO)
	public void sapGetStatusBarText() {
		try {
			String statusText = Dispatch.get(SAPElement, "Text").toString();
			addVar("StatusBarText", statusText);
			Report.updateTestLog(Action, "Status bar text: " + statusText, Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to get status bar text. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	// ============================================================================
	// SECTION 14: GENERAL ELEMENT ACTIONS
	// ============================================================================

	/**
	 * EXISTING: Set focus on element
	 * SAP API: element.setFocus()
	 */
	@Action(object = ObjectType.SAP, desc = "Set focus on [<Object>]", input = InputType.NO)
	public void sapSetFocus() {
		try {
			Dispatch.call(SAPElement, "setFocus");
			Report.updateTestLog(Action, "Focus has been set on [" + ObjectName + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to set focus on [" + ObjectName + "]. Error : " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * EXISTING: Double-click on element
	 * SAP API: element.doubleClick()
	 */
	@Action(object = ObjectType.SAP, desc = "Double click on [<Object>]")
	public void sapDoubleClick() {
		try {
			Dispatch.call(SAPElement, "doubleClick");
			Report.updateTestLog(Action, "Double clicked on [" + ObjectName + "]", Status.DONE);
		} catch (Exception e) {
			Report.updateTestLog(Action, "Failed to double click. Error: " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * EXISTING: Assert element contains text
	 * SAP API: element.Text (with assertion logic)
	 */
	@Action(object = ObjectType.SAP, desc = "Assert that [<Object>] contains Text [<Data>]", input = InputType.YES)
	public void sapAssertElementTextContains() {
		try {
			String actualText = Dispatch.get(SAPElement, "Text").toString();
			if (actualText.contains(Data)) {
				Report.updateTestLog(Action, "[" + ObjectName +
						"] actual text [" + actualText + "] contains expected text [" + Data + "].",
						Status.PASSNS);
			} else {
				Report.updateTestLog(Action, "[" + ObjectName +
						"] actual text [" + actualText + "] not contains expected text [" + Data + "].", Status.FAILNS);
			}
		} catch (Exception e) {
			Report.updateTestLog(Action, "Fails to get Element text. Error : " + e.getMessage(), Status.FAILNS);
		}
	}

	/**
	 * EXISTING: Set object property at runtime
	 * Dynamic property setting for specific SAP object
	 */
	@Action(object = ObjectType.SAP, desc = "Set object [<Object>] property as [<Data>] at runtime", input = InputType.YES, condition = InputType.YES)
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
			String text = String.format("Setting Object Property for [%s] with [%s] for Object [%s - %s]", Condition, Data,
					Reference, ObjectName);
			Report.updateTestLog(Action, text, Status.DONE);
		} else {
			Report.updateTestLog(Action, "Input should not be empty", Status.FAILNS);
		}
	}

	/**
	 * Helper method for setting dynamic properties
	 */
	private void setProperty(String key, String value) {
		if (!SAPObject.dynamicValue.containsKey(Reference)) {
			Map<String, Map<String, String>> Object = new HashMap<>();
			Map<String, String> property = new HashMap<>();
			property.put(key, value);
			Object.put(ObjectName, property);
			SAPObject.dynamicValue.put(Reference, Object);
		} else if (!SAPObject.dynamicValue.get(Reference).containsKey(ObjectName)) {
			Map<String, String> property = new HashMap<>();
			property.put(key, value);
			SAPObject.dynamicValue.get(Reference).put(ObjectName, property);
		} else {
			SAPObject.dynamicValue.get(Reference).get(ObjectName).put(key, value);
		}
	}
}
