# SAP Actions Catalog

A complete inventory of every `@Action` method in
[`SAPActions.java`](../Engine/src/main/java/com/ing/engine/commands/SAP/SAPActions.java) (114
actions total as of this catalog), split into:

1. **Legacy actions** — existed in the codebase before the SAP-Enhancements initiative
   (docs `01`–`10` in this folder) began.
2. **New actions** — introduced by the SAP-Enhancements initiative, grouped by the doc/user
   story that drove each addition.

Classification method: `SAPActions.java` tags most of its own methods with an `EXISTING:` or
`NEW:` marker in the preceding Javadoc (the class-level comment itself states
`Total Actions: 100 (17 Existing + 83 New)` — now stale, see note at the end). Every
`EXISTING:`-tagged method is listed under Legacy below. `git log -S` tracing of every `NEW:`-tagged
method shows 36 of them (general window/table/grid/menu mechanics) actually predate this
initiative, so they're also listed under Legacy — bringing it to 53 total. Everything else —
including the 7 connection/session actions added by
[`01-SAP-Connections-Redesign.md`](01-SAP-Connections-Redesign.md), which predate the
`EXISTING:`/`NEW:` tagging convention and carry no tag at all — is listed under New (61 total),
grouped by source doc where traceable.

---

## Legacy Actions (53) — pre-existing before SAP-Enhancements

| Action | Purpose | What changed / status |
|---|---|---|
| `sapCloseLogonScreen` | Close the SAP logon landscape screen, killing the launched process | **Updated** — `object` reclassified `BROWSER` → `SAP` (01, Phase 1); `SapSessionManager.currentProcess()` added so `CommandControl.SAPProcess` is populated again under the driverless model, which this action depends on (01, Phase 1 fallout fix) |
| `sapSetglobalObjectProperty` | Set a global dynamic object property at runtime | **Updated** — `object` reclassified `BROWSER` → `SAP` (01, Phase 1); behavior unchanged |
| `sapFill` | Enter text into a field (`element.Text = value`) | Unchanged — `object` reclassified `SAP` → `SAP_OBJECT` (01, Phase 1, applies to all element-level legacy actions below); gained a stricter, XML-validating sibling, `sapSetElementXMLEditor` (10) — not a replacement, `sapFill` is still used for non-XML fields |
| `sapEnter` | Press Enter on the focused element | Unchanged (`SAP` → `SAP_OBJECT` reclassification only) |
| `sapSimulateKeyPress` | Simulate an arbitrary virtual key press | Unchanged (reclassification only) |
| `sapClick` | Click/press a button | Unchanged (reclassification only) |
| `sapSelectCheckBox` | Set a checkbox's `Selected` state | Unchanged (reclassification only); gained assertion complements `sapAssertCheckBoxSelected`/`sapAssertCheckBoxNotSelected` (04) — those are new, separate actions, not a replacement |
| `sapSelectRadioButtonInRow` | Select a radio button in a table row by index | Unchanged (reclassification only) |
| `sapSelectDropDownByText` | Select a dropdown value by visible text | Unchanged (reclassification only) |
| `sapSelectDropDownByKey` | Select a dropdown value by internal key | Unchanged (reclassification only) |
| `sapSelectDropDownByIndex` | Select a dropdown value by index | Unchanged (reclassification only) |
| `sapSelect` | Select a tab | Unchanged (reclassification only) |
| `sapDoubleClickCell` | Double-click the current table cell | Unchanged (reclassification only) |
| `sapSetFocus` | Set focus on an element | Unchanged (reclassification only) |
| `sapDoubleClick` | Double-click an element | Unchanged (reclassification only) |
| `sapAssertElementTextContains` | Assert an element's text contains a substring | Unchanged (reclassification only); gained siblings `sapAssertElementTextNotContains` and `sapAssertElementTextEquals` (04) — new, separate actions, not replacements |
| `sapSetObjectProperty` | Set a dynamic property on a specific SAP object at runtime | Unchanged (reclassification only) |

**Note on 3 borderline cases**: `sapExecuteTransaction`, `sapEndTransaction`, `sapRefreshSession`
are tagged `NEW:` in the current source, so they're listed under *New* below per that tag (and
per the `17 Existing` count matching exactly with them excluded). However,
[`01-SAP-Connections-Redesign.md`](01-SAP-Connections-Redesign.md) itself describes these three
as **pre-existing legacy actions** that were reclassified `object = BROWSER` → `SAP` and rerouted
to resolve the session through `SapSessionManager.current()` instead of a bare `Task`-assigned
field — i.e. the doc's own account treats them the same as `sapCloseLogonScreen`/
`sapSetglobalObjectProperty` above. This is a minor inconsistency in the source file's own
tagging, noted here rather than silently resolved one way or the other.

**Note on the 36 general-library actions (window/table/grid/menu mechanics)**: these are also
tagged `NEW:` in the current source, but `git log -S` on each method shows every one of them was
actually introduced by commit `c137d55d` ("Feature/or 3.0 (#201)", Object Repository 3.0 work),
which is an ancestor of — i.e. predates — the first SAP-Enhancements commit (`49735e91`). So
despite the `NEW:` tag, this entire group is pre-existing and has been moved into Legacy below
(bringing the true legacy count to 17 + 36 = 53, and New down to 97 − 36 = 61). The `NEW:` tag on
these methods is stale/incorrect, same class of issue as the 3 borderline cases above.

No legacy action has been **deprecated or removed** — every one of the 17 is still active and
callable exactly as before; the only changes are `ObjectType` reclassification (so actions show
up under the correct `Object` dropdown in the IDE) and, in two cases, a related wiring fix or new
complementary sibling action.

### Also pre-existing (36) — window/table/grid/menu mechanics, mistagged `NEW:` in source

All introduced by commit `c137d55d` ("Feature/or 3.0 (#201)"), which predates the SAP-Enhancements
initiative — see note above. Untouched by this initiative; listed here only to correct their
classification.

| Action | Purpose |
|---|---|
| `sapMaximizeWindow` / `sapMinimizeWindow` / `sapRestoreWindow` / `sapCloseWindow` / `sapIconifyWindow` | Window state management |
| `sapResizeWorkingPane` | Resize the SAP working pane |
| `sapSelectAllText` | Select all text in a field |
| `sapSetCaretPosition` | Set caret position in a field |
| `sapSetModified` | Set the modified flag on an element |
| `sapPressButton` | Press a button by ID/function code |
| `sapOpenComboBox` / `sapCloseComboBox` | Open/close a dropdown list |
| `sapSelectTableRow` | Select a table row by fixed index |
| `sapSetCurrentCell` / `sapSetCurrentCellRow` | Set current table cell/row |
| `sapModifyCell` | Modify a table cell's value |
| `sapClickCurrentCell` | Click the current table cell |
| `sapSetVerticalScrollPosition` | Set table vertical scroll position |
| `sapSelectGridRow` | Select an ALV grid row by fixed index |
| `sapSelectGridColumn` | Select an ALV grid column |
| `sapSetGridCurrentCell` | Set current grid cell |
| `sapClickGridCurrentCell` | Click the current grid cell |
| `sapPressToolbarButton` / `sapPressToolbarContextButton` | Press grid toolbar buttons |
| `sapSelectAllGrid` / `sapDeselectRow` / `sapClearSelection` | Grid selection management |
| `sapSetGridFilter` / `sapClearGridFilter` | Grid filtering |
| `sapSetSelectedRows` | Set selected rows directly |
| `sapSetCurrentCellColumn` / `sapSetFirstVisibleColumn` / `sapSetFirstVisibleRow` | Grid cell/scroll positioning |
| `sapSelectMenuItem` | Select a menu item |
| `sapPressContextButton` / `sapSelectContextMenuItem` | Context menu operations |

---

## New Actions (61) — introduced by the SAP-Enhancements initiative

### From `01-SAP-Connections-Redesign.md` (10)

Connection/session lifecycle actions (untagged in source — predate the `EXISTING:`/`NEW:`
convention) plus the 3 borderline-legacy actions noted above:

| Action | Purpose |
|---|---|
| `sapInitConnection` | Open/adopt/re-claim a SAP connection, blank = project default |
| `sapSwitchConnection` | Make an already-open connection current |
| `sapCloseConnection` | Release this runner's claim; closes when unclaimed |
| `sapCloseAllConnection` | Force-close every connection this run owns |
| `sapOpenSession` | Open a new labeled session on the current connection (up to 6) |
| `sapSwitchSession` | Switch current session (blank = primary) |
| `sapCloseSession` | Close a session (refuses to close the last one on a connection) |
| `sapExecuteTransaction` | Start a SAP transaction code — *see legacy note above* |
| `sapEndTransaction` | End the current transaction — *see legacy note above* |
| `sapRefreshSession` | Refresh the SAP session — *see legacy note above* |

### From `04-SAP-Capture-Store-Validate-Values.md` (20)

| Action | Purpose |
|---|---|
| `sapStoreElementTextInVariable` / `sapStoreElementTextInDataSheet` | Capture any element's text |
| `sapAssertElementTextEquals` | Assert exact text match |
| `sapAssertElementTextNotContains` | Assert substring absence |
| `sapAssertElementNumericValue` | Numeric comparison (`=`,`!=`,`>`,`<`,`>=`,`<=`) |
| `sapStoreStatusBarTextInVariable` / `sapStoreStatusBarTextInDataSheet` | Capture status bar text |
| `sapAssertStatusBarType` | Assert status bar severity (S/W/E/I/A) |
| `sapAssertElementPresent` / `sapAssertElementNotPresent` | Soft existence assertions |
| `sapAssertCheckBoxSelected` / `sapAssertCheckBoxNotSelected` | Validate checkbox state |
| `sapAssertElementEnabled` / `sapAssertElementNotEnabled` | Validate changeable/read-only state |
| `sapAssertTableRowCount` / `sapAssertGridRowCount` | Assert row count against an expression |
| `sapStoreCellValueByIndexInVariable` / `...InDataSheet` | Get a table cell by row,column index |
| `sapStoreGridValueByIndexInVariable` / `...InDataSheet` | Get an ALV grid cell by row,column index |

### From `05-SAP-Dynamic-Object-Selection-Tables-Tree.md` (14)

| Action | Purpose |
|---|---|
| `sapSelectTableRowByText` / `...Contains` | Select a table row by column text |
| `sapSelectGridRowByText` / `...Contains` | Select an ALV grid row by column text |
| `sapSetCheckBoxInMatchingRow` / `...Contains` | Tick a checkbox in a row matched by another column |
| `sapExpandTreeNode` / `sapCollapseTreeNode` / `sapSelectTreeNode` / `sapDoubleClickTreeNode` / `sapSetTopNode` | Base tree node operations |
| `sapNavigateTreePath` | Expand parent + select child node by known keys |
| `sapSelectTreeNodeByText` / `...Contains` | Expand parent + select child by visible text |

### From `06-SAP-Job-Status-Time-Validation.md` (10)

| Action | Purpose |
|---|---|
| `sapStoreCellValueByMatchInVariable` / `...Contains` | Find row by column, read another column into a variable |
| `sapStoreCellValueByMatchInDataSheet` / `...Contains` | Same, into a datasheet cell |
| `sapStoreGridValueByMatchInVariable` / `...Contains` | Same, for ALV grid |
| `sapStoreGridValueByMatchInDataSheet` / `...Contains` | Same, into a datasheet cell |
| `sapAssertTableRowsHealthy` / `sapAssertGridRowsHealthy` | Combined status + time-interval health check over the first N rows |

### From `07-SAP-Execution-Evidence-Screenshot-Capture.md` (1)

| Action | Purpose |
|---|---|
| `sapCaptureScreenshot` | On-demand SAP window screenshot |

### From `08-SAP-PDF-Download-Save-Automation.md` (1)

| Action | Purpose |
|---|---|
| `sapSaveDialogConfirm` | Confirm native Windows Save dialog for PDF export |

### From `09-SAP-Visual-Status-Indicator-Validation.md` (3)

| Action | Purpose |
|---|---|
| `sapAssertElementIconEquals` | Assert a control's `IconName` equals expected value |
| `sapAssertElementTooltipContains` | Assert a control's `Tooltip` contains expected substring |
| `sapStoreElementIconInVariable` | Capture a control's `IconName` into a variable |

### From `10-SAP-WebService-Test-Tool-XML-Editing.md` (2)

| Action | Purpose |
|---|---|
| `sapSetElementXMLEditor` | Validate and write XML into an element's `Text` property |
| `sapCaptureElementTextViaClipboard` | Robot-based select-all+copy fallback capture, strips tree-view collapse-glyph artifacts |

---

## Housekeeping note

`SAPActions.java`'s class-level Javadoc still reads `Total Actions: 100 (17 Existing + 83 New)`.
The true current count is **53 Existing + 61 New = 114**, i.e. 17 originally-tagged `EXISTING:` +
36 mistagged-`NEW:`-but-actually-pre-existing (the general action library group above), and 61
genuinely new actions reflecting all additions documented above including the most recent ones
(`sapSetElementXMLEditor`, `sapCaptureElementTextViaClipboard`, `sapAssertTableRowsHealthy`,
`sapAssertGridRowsHealthy`, and the icon/tooltip actions from doc `09`). Worth a one-line fix in
the source file next time it's touched — both the stale total and the 36 incorrect `NEW:` tags.
