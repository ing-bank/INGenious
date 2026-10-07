# SAP Actions — User Guide

A practical, usage-focused reference for every SAP action available in INGenious: what it does,
and how to fill in `Object` / `Data` (Input) / `Condition` for it. Grouped the same way as
[`SAP-Actions-Catalog.md`](SAP-Actions-Catalog.md) — by the enhancement item that introduced each
group, with the original core action set listed first.

> **Field legend**: `Object` = the SAP element/control from the Object Repository (where
> applicable). `Input` = the action's main input (sometimes resolved into the internal `Data`
> field). `Condition` = the action's second input, when it has one (a destination variable, a
> secondary value, etc.). Blank means that field isn't used by the action.

> **`...InVariable` vs `...InDataSheet` pairs**: every action that stores a captured value has
> this naming pattern. Both siblings now share the same shape: the destination (a `%variable%`
> for `...InVariable`, or a `Sheet:Column` reference for `...InDataSheet`) always goes via
> `Condition`, while `Input` only ever holds the lookup fields (row/column, or match column +
> match text + target column). This applies to every such pair in this guide:
> `sapStoreCellValueByIndexInVariable`/`...InDataSheet`,
> `sapStoreGridValueByIndexInVariable`/`...InDataSheet`,
> `sapStoreCellValueByMatchInVariable`/`...InDataSheet` (and the grid/`Contains` equivalents),
> `sapStoreElementTextInVariable`/`...InDataSheet`, `sapStoreStatusBarTextInVariable`/`...InDataSheet`.

---

## Core Actions (available since before this enhancement work)

| Action | What it does | Object | Input | Condition |
|---|---|---|---|---|
| `sapCloseLogonScreen` | Closes the SAP logon landscape screen | — | — | — |
| `sapSetglobalObjectProperty` | Sets a property on all matching objects at runtime | — | `property=value` — `property`: the property name; `value`: the value to assign. Repeat as `prop1=val1,prop2=val2` for multiple, e.g. `Visible=true,Enabled=false` | — |
| `sapFill` | Enters a value into a field | the field | the value to enter | — |
| `sapEnter` | Presses Enter on the focused element | the element | — | — |
| `sapSimulateKeyPress` | Simulates a virtual key press (e.g. F4, F8, Save) | the element | VKey code (`0`=Enter, `3`=Back, `4`=F4, `8`=F8/Execute, `11`=Save, etc.) | — |
| `sapClick` | Clicks/presses a button | the button | — | — |
| `sapSelectCheckBox` | Sets a checkbox to checked/unchecked | the checkbox | `true` or `false` | — |
| `sapSelectRadioButtonInRow` | Selects a radio button in a table row by index | the radio group | row index | — |
| `sapSelectDropDownByText` | Selects a dropdown value by visible text | the dropdown | visible text | — |
| `sapSelectDropDownByKey` | Selects a dropdown value by internal key | the dropdown | internal key | — |
| `sapSelectDropDownByIndex` | Selects a dropdown value by position | the dropdown | index | — |
| `sapSelect` | Selects a tab | the tab | — | — |
| `sapDoubleClickCell` | Double-clicks the current table cell | the table | — | — |
| `sapSetFocus` | Sets keyboard focus on an element | the element | — | — |
| `sapDoubleClick` | Double-clicks an element | the element | — | — |
| `sapAssertElementTextContains` | Asserts an element's text contains a substring | the element | expected substring | — |
| `sapSetObjectProperty` | Sets a dynamic property on a specific object | the object | `property=value` | — |

---

## From "Support SAP Driverless" (connections & sessions) — design proposal, no Excel item #

| Action | What it does | Object | Input | Condition |
|---|---|---|---|---|
| `sapInitConnection` | Opens/adopts a SAP connection; blank = project default | — | *(Input)* blank, or `#SAP_QA` | — |
| `sapSwitchConnection` | Makes an already-open connection current | — | *(Input)* `#SAP_QA` | — |
| `sapCloseConnection` | Releases this runner's claim on a connection | — | *(Input)* blank, or `#SAP_QA` | — |
| `sapCloseAllConnection` | Force-closes every connection this run owns | — | — | — |
| `sapOpenSession` | Opens a new labeled session (up to 6 per connection) | — | a label, e.g. `@stock` | — |
| `sapSwitchSession` | Switches the current session; blank = primary session | — | blank, or a label | — |
| `sapCloseSession` | Closes a session; blank = current session | — | blank, or a label | — |
| `sapExecuteTransaction` | Starts a SAP transaction code | — | transaction code, e.g. `VA01` | — |
| `sapEndTransaction` | Ends the current transaction | — | — | — |
| `sapRefreshSession` | Refreshes the SAP session | — | — | — |

**Example**: `SAP` / `sapInitConnection` / Input: `#SAP_QA` → connects using `Settings/SAP/SAP_QA.properties`.

---

## From "Capture, Store and Validate SAP Values and Results" (Items #5, #7, #8, #9)

| Action | What it does | Object | Input | Condition |
|---|---|---|---|---|
| `sapStoreElementTextInVariable` | Captures an element's text into a variable | the element | — | destination variable, e.g. `%resultText%` |
| `sapStoreElementTextInDataSheet` | Captures an element's text into a datasheet cell | the element | — | `Sheet:Column`, e.g. `SapResults:ErrorText` |
| `sapAssertElementTextEquals` | Asserts an element's text equals an exact value | the element | expected text | — |
| `sapAssertElementTextNotContains` | Asserts an element's text does NOT contain a substring | the element | substring that must be absent | — |
| `sapAssertElementNumericValue` | Asserts numeric text against an expression | the element | `<>0`, `>=100`, etc. | — |
| `sapStoreStatusBarTextInVariable` | Captures the status bar message into a variable | status bar | — | destination variable |
| `sapStoreStatusBarTextInDataSheet` | Captures the status bar message into a datasheet cell | status bar | — | `Sheet:Column` |
| `sapAssertStatusBarType` | Asserts status bar severity | status bar | `S`/`W`/`E`/`I`/`A` (`S`=Success, `W`=Warning, `E`=Error, `I`=Info, `A`=Abort) | — |
| `sapAssertElementPresent` | Soft-asserts an element exists | the element | — | — |
| `sapAssertElementNotPresent` | Soft-asserts an element does NOT exist | the element | — | — |
| `sapAssertCheckBoxSelected` | Asserts a checkbox is checked | the checkbox | — | — |
| `sapAssertCheckBoxNotSelected` | Asserts a checkbox is unchecked | the checkbox | — | — |
| `sapAssertElementEnabled` | Asserts a field is enabled/changeable | the field | — | — |
| `sapAssertElementNotEnabled` | Asserts a field is disabled/read-only | the field | — | — |
| `sapAssertTableRowCount` | Asserts a table's row count against an expression | the table | `==0`, `>=1`, etc. | — |
| `sapAssertGridRowCount` | Asserts an ALV grid's row count against an expression | the grid | `==0`, `>=1`, etc. | — |
| `sapStoreCellValueByIndexInVariable` | Reads a table cell by row,column into a variable | the table | `row,column` — `row`: zero-based row index; `column`: column header name, e.g. `2,VendorName` | destination variable, e.g. `%cellValue%` |
| `sapStoreCellValueByIndexInDataSheet` | Same, into a datasheet cell | the table | `row,column` (same breakdown as above), e.g. `2,VendorName` | `Sheet:Column` |
| `sapStoreGridValueByIndexInVariable` | Reads a grid cell by row,column into a variable | the grid | `row,column` (same breakdown), e.g. `2,VendorName` | destination variable |
| `sapStoreGridValueByIndexInDataSheet` | Same, into a datasheet cell | the grid | `row,column` (same breakdown) | `Sheet:Column` |

**Example**: `ErrorField` / `sapAssertElementNumericValue` / Data: `<>0` → fails if the field's numeric value is 0.

---

## From "Support Dynamic Object Selection, Tables and Tree Navigation" (Items #6, #11, #13)

| Action | What it does | Object | Input | Condition |
|---|---|---|---|---|
| `sapSelectTableRowByText` | Selects a table row by exact column text | the table | `column,text` — `column`: column header to search in; `text`: expected value, exact match, e.g. `Description,Vendor Invoice` | — |
| `sapSelectTableRowByTextContains` | Same, substring match | the table | `column,text` (same breakdown, substring match) | — |
| `sapSelectGridRowByText` | Selects a grid row by exact column text | the grid | `column,text` (same breakdown) | — |
| `sapSelectGridRowByTextContains` | Same, substring match | the grid | `column,text` (same breakdown, substring match) | — |
| `sapSetCheckBoxInMatchingRow` | Ticks a checkbox in a row matched by another column (exact) | the table/grid | `matchColumn,matchText,checkboxColumn` — `matchColumn`: column to search in; `matchText`: text to find (exact); `checkboxColumn`: column holding the checkbox to tick, e.g. `ConditionType,ZPR0,Checkbox` | `true`/`false` (default `true`) |
| `sapSetCheckBoxInMatchingRowContains` | Same, substring match | the table/grid | `matchColumn,matchText,checkboxColumn` (same breakdown, substring match on `matchText`) | `true`/`false` |
| `sapExpandTreeNode` | Expands a tree node | the tree | node key | — |
| `sapCollapseTreeNode` | Collapses a tree node | the tree | node key | — |
| `sapSelectTreeNode` | Selects a tree node | the tree | node key | — |
| `sapDoubleClickTreeNode` | Double-clicks a tree node | the tree | node key | — |
| `sapSetTopNode` | Sets the tree's top visible node | the tree | node key | — |
| `sapNavigateTreePath` | Expands a parent node and selects its child in one step | the tree | `parentNodeKey\childNodeKey` — `parentNodeKey`: the node to expand; `childNodeKey`: the exact child node key to select, e.g. `Root\Child` | — |
| `sapSelectTreeNodeByText` | Same, but finds the child by its visible text (exact) | the tree | `parentNodeKey\childText` — `parentNodeKey`: the node to expand; `childText`: the child's visible text, exact match, e.g. `Root\Vendor Invoice` | — |
| `sapSelectTreeNodeByTextContains` | Same, substring match | the tree | `parentNodeKey\childText` (same breakdown, substring match on `childText`) | — |

**Example**: `OrgTree` / `sapSelectTreeNodeByText` / Data: `Root\Vendor Invoice` → expands `Root` and selects the child node whose text is exactly "Vendor Invoice".

---

## From "Support SAP Job Status Retrieval and Time-Based Validation" (Item #12)

| Action | What it does | Object | Input | Condition |
|---|---|---|---|---|
| `sapStoreCellValueByMatchInVariable` | Finds a table row by one column, reads another column into a variable (exact) | the table | `matchColumn,matchText,targetColumn` — `matchColumn`: column to search in; `matchText`: text to find (exact); `targetColumn`: column to read from the matched row, e.g. `JobName,BATCH_JOB_1,Status` | destination variable, e.g. `%jobStatus%` |
| `sapStoreCellValueByMatchInVariableContains` | Same, substring match | the table | `matchColumn,matchText,targetColumn` (same breakdown, substring match on `matchText`) | destination variable |
| `sapStoreCellValueByMatchInDataSheet` | Same, into a datasheet cell (exact) | the table | `matchColumn,matchText,targetColumn` (same breakdown) | `Sheet:Column` |
| `sapStoreCellValueByMatchInDataSheetContains` | Same, substring match | the table | `matchColumn,matchText,targetColumn` (same breakdown, substring match) | `Sheet:Column` |
| `sapStoreGridValueByMatchInVariable` | Same lookup, for an ALV grid (exact) | the grid | `matchColumn,matchText,targetColumn` (same breakdown) | destination variable |
| `sapStoreGridValueByMatchInVariableContains` | Same, substring match | the grid | `matchColumn,matchText,targetColumn` (same breakdown, substring match) | destination variable |
| `sapStoreGridValueByMatchInDataSheet` | Same, into a datasheet cell (exact) | the grid | `matchColumn,matchText,targetColumn` (same breakdown) | `Sheet:Column` |
| `sapStoreGridValueByMatchInDataSheetContains` | Same, substring match | the grid | `matchColumn,matchText,targetColumn` (same breakdown, substring match) | `Sheet:Column` |
| `sapAssertTableRowsHealthy` | Checks the first N rows' status + flags time gaps between rows exceeding an interval | the table | `statusColumn,expectedStatus,timeColumn,rowCount` — `statusColumn`: column holding job status; `expectedStatus`: required value, e.g. `Finished`; `timeColumn`: column holding each run's time; `rowCount`: how many rows (from the top) to check, e.g. `Status,Finished,Start Time,10` | max interval in minutes, e.g. `3` |
| `sapAssertGridRowsHealthy` | Same, for an ALV grid | the grid | `statusColumn,expectedStatus,timeColumn,rowCount` (same breakdown) | max interval in minutes |

**Example**: `JobStatusGrid` / `sapAssertGridRowsHealthy` / Data: `Status,Finished,Start Time,10` / Condition: `3` → checks the first 10 rows all show `Finished` and no two consecutive runs are more than 3 minutes apart.

---

## From "Provide SAP Execution Evidence Collection" (Item #3)

| Action | What it does | Object | Input | Condition |
|---|---|---|---|---|
| `sapCaptureScreenshot` | Captures a screenshot of the active SAP window on demand | — | — | — |

---

## From "Support Saving and Downloading PDF Documents Generated from SAP" (Item #4)

| Action | What it does | Object | Input | Condition |
|---|---|---|---|---|
| `sapSaveDialogConfirm` | Confirms the native Windows Save dialog SAP's PDF export opens | — | destination file path, e.g. `C:\Downloads\report.pdf` | timeout in ms (optional, default `10000`) |

---

## From "Support Validation of SAP Visual Status Indicators" (Item #14)

| Action | What it does | Object | Input | Condition |
|---|---|---|---|---|
| `sapAssertElementIconEquals` | Asserts a control's icon equals an expected icon name | the element | icon name, e.g. `ICON_LED_GREEN` | — |
| `sapAssertElementTooltipContains` | Asserts a control's tooltip contains a substring | the element | expected substring, e.g. `Green` | — |
| `sapStoreElementIconInVariable` | Captures a control's icon name into a variable | the element | — | destination variable, e.g. `%filterIcon%` |

**Example**: `Filter1Box` / `sapAssertElementIconEquals` / Data: `ICON_LED_GREEN` → fails if Filter 1's status box isn't showing the green LED icon.

---

## From "Assert and Edit XML in SAP's Embedded Document Viewer" — new request #10, no formal Excel item

| Action | What it does | Object | Input | Condition |
|---|---|---|---|---|
| `sapSetElementXMLEditor` | Validates and writes a full XML document into a field | the XML Editor field | the XML to write, e.g. `<root/>` | — |
| `sapCaptureElementTextViaClipboard` | Fallback capture of on-screen text via select-all + copy, cleaned of tree-view artifacts | the element | — | destination variable, e.g. `%capturedXml%` |

**Example**: `RequestXmlEditor` / `sapSetElementXMLEditor` / Data: `<n0:Query xmlns:n0="...">...</n0:Query>` → validates the XML is well-formed, then writes it into the Request tab's XML Editor field.

---

## General action library (window, table/grid mechanics, menus) — pre-existing, not part of this enhancement work

> Tagged `NEW:` in source, but `git log -S` on every method below traces back to commit `c137d55d`
> ("Feature/or 3.0 #201"), which predates the first SAP-Enhancements commit. These are legacy
> actions mistagged in the Javadoc — see the Catalog's Legacy Actions section for the correction.

| Action | What it does | Object | Input | Condition |
|---|---|---|---|---|
| `sapMaximizeWindow` / `sapMinimizeWindow` / `sapRestoreWindow` / `sapCloseWindow` / `sapIconifyWindow` | Window state management | the window | — | — |
| `sapResizeWorkingPane` | Resizes the SAP working pane | the window | `width,height[,fullScreen]` — `width`/`height`: pane size in pixels; `fullScreen` (optional): `true`/`false`, e.g. `800,600,false` | — |
| `sapSelectAllText` | Selects all text in a field | the field | — | — |
| `sapSetCaretPosition` | Sets caret position in a field | the field | position | — |
| `sapSetModified` | Sets the "modified" flag on an element | the element | `true`/`false` | — |
| `sapPressButton` | Presses a button by ID/function code | the toolbar | button ID | — |
| `sapOpenComboBox` / `sapCloseComboBox` | Opens/closes a dropdown list | the dropdown | — | — |
| `sapSelectTableRow` | Selects a table row by fixed index | the table | row index | — |
| `sapSetCurrentCell` | Sets current table cell | the table | `row,column` — `row`: zero-based row index; `column`: column header name, e.g. `2,VendorName` | — |
| `sapSetCurrentCellRow` | Sets current table cell row | the table | row index | — |
| `sapModifyCell` | Modifies a table cell's value | the table | `row,column,value` — `row`/`column`: cell location; `value`: new value to write, e.g. `2,VendorName,Acme Corp` | — |
| `sapClickCurrentCell` | Clicks the current table cell | the table | — | — |
| `sapSetVerticalScrollPosition` | Sets table vertical scroll position | the table | position | — |
| `sapSelectGridRow` | Selects a grid row by fixed index | the grid | row index | — |
| `sapSelectGridColumn` | Selects a grid column | the grid | column name | — |
| `sapSetGridCurrentCell` | Sets current grid cell | the grid | `row,column` (same breakdown as `sapSetCurrentCell`) | — |
| `sapClickGridCurrentCell` | Clicks the current grid cell | the grid | — | — |
| `sapPressToolbarButton` / `sapPressToolbarContextButton` | Presses a grid toolbar button | the grid | button ID | — |
| `sapSelectAllGrid` | Selects all rows in a grid | the grid | — | — |
| `sapDeselectRow` | Deselects a grid row | the grid | row index | — |
| `sapClearSelection` | Clears grid selection | the grid | — | — |
| `sapSetGridFilter` | Sets a grid filter | the grid | `column,filterValue` — `column`: column to filter; `filterValue`: value to filter by, e.g. `Status,Finished` | — |
| `sapClearGridFilter` | Clears a grid filter | the grid | — | — |
| `sapSetSelectedRows` | Sets selected rows directly | the grid | row list | — |
| `sapSetCurrentCellColumn` | Sets current cell column | the grid | column name | — |
| `sapSetFirstVisibleColumn` / `sapSetFirstVisibleRow` | Scrolls to a column/row | the grid | column name / row index | — |
| `sapSelectMenuItem` | Selects a menu item | the menu item | — | — |
| `sapPressContextButton` | Presses a context button | the element | parameter (optional) | — |
| `sapSelectContextMenuItem` | Selects a context menu item | the element | menu item ID | — |
