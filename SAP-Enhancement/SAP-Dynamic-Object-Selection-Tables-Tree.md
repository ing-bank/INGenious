# User Story 9917356: [SAP] Support Dynamic Object Selection, Tables and Tree Navigation

Source: `SAP Ingenious Issues 2026.xlsx` — sheet `SAP Requests` (Items #6, #11, #13), sheet `US`.

## User Story

As an INGenious engineer, I want to locate and interact with SAP elements using business data,
dynamic text, tables and tree structures, so that automation remains reliable even when SAP
layouts change.

## Acceptance Criteria (ATDD)

### Scenario 1: Select row by text

**Given** a table contains multiple rows

**When** a text value is provided

**Then** the correct row is identified

### Scenario 2: Tick checkbox based on business condition

**Given** a matching row exists

**When** a checkbox action is executed

**Then** the checkbox in the matching row is selected

### Scenario 3: Navigate tree structures

**Given** a parent node and child node exist

**When** navigation is performed

**Then** the framework expands the parent node and selects the child node

### Scenario 4: Locate object using text property

**Given** an object contains identifiable text

**When** lookup is executed

**Then** the correct object is found regardless of row position

## Related SAP Request Items

### Item #6

**Description:** How to know which line I am selecting using text?

**Resolution note (from tracker):** Object using text property to select element in SAP GUI.

### Item #11

**Description:** How to locate row based on description or condition type and tick the checkbox.

**Resolution note (from tracker):** Create action to click on checkbox.

### Item #13

**Description:** How to open a Top Treenode and then click on child node.

## Context

Users need to identify SAP elements using text values rather than fixed row numbers and
navigate hierarchical tree controls commonly used in SAP transactions.

## Goal

Provide dynamic element location and hierarchical navigation capabilities.

## Value

- Improves automation stability.
- Reduces script maintenance.
- Supports dynamic SAP interfaces.

## Scope

Affected Components:

- SAP Object Repository
- Locator Engine
- Grid/Table Controls
- Tree Controls
- Workflow Actions

## Current State (gap analysis)

`Engine/src/main/java/com/ing/engine/commands/SAP/SAPActions.java` already covers table/grid/tree
mechanics, but every one of them is index- or key-based, never text-based:

| Action | What it does | Gap |
|---|---|---|
| `sapSelectTableRow` / `sapSelectGridRow` | Selects a row by numeric index (`Dispatch.call(SAPElement, "SelectRow", Integer.parseInt(Data))`) | The row index must already be known. If the row order changes (sort, filter, new entries), the hard-coded index selects the wrong line — exactly Item #6/#11's complaint. |
| `sapSetCurrentCell` / `sapSetGridCurrentCell` / `sapGetCellValue` / `sapGetGridCellValue` | Read/target a cell by `row,column` (numeric row + column name) | No way to find *which* row to target from a business value (description, condition type); callers must already know the index. |
| `sapSelectCheckBox` | Toggles a checkbox element's `Selected` property directly | Only works when the checkbox is already resolved as its own `[<Object>]`; there's no "find the row whose column X = Y, then tick its checkbox" action, which is what Item #11 actually asks for. |
| `sapExpandTreeNode` / `sapSelectTreeNode` / `sapDoubleClickTreeNode` / `sapSetTopNode` | Each takes a raw SAP GUI **node key** in `Data` (`Dispatch.call(SAPElement, "ExpandNode"/"SelectNode"/"DoubleClickNode", Data)`) | These are single, independent calls — there's no combined "expand parent then select/click child" action, and the node key (not the node's visible text) must already be known. Item #13's confusion is exactly about sequencing parent-then-child correctly. |

At the object-repository level, `SAPObject.SAPFindType` (`Engine/src/main/java/com/ing/engine/drivers/SAPObject.java`)
only distinguishes `GLOBAL_OBJECT` vs `DEFAULT` — there is no find-type for "resolve by visible
text/description among a set of candidates" (Item #6's "text property to select element").

## Proposed Actions

1. **Row-by-text lookup** — `sapSelectTableRowByText`/`sapSelectTableRowByTextContains` and
   `sapSelectGridRowByText`/`sapSelectGridRowByTextContains`: iterate rows via the existing
   cell-read plumbing (`GetCellValue(row, column)`), compare against `Data`, and select the first
   matching row. Exact and substring matching are separate actions (rather than one action with a
   `Condition`-selected mode), so the intent is explicit at the step level. Directly resolves Item #6.

2. **Conditional checkbox action** — `sapSetCheckBoxInMatchingRow`/`sapSetCheckBoxInMatchingRowContains`:
   combines the row-by-text lookup above with a checkbox toggle in the same matched row
   (`GetCell`/checkbox column + `Selected`), so a single action expresses "tick the checkbox where
   description/condition type = X" — again as separate exact/contains actions. Resolves Item #11
   without requiring two hand-sequenced steps.

3. **Tree path navigation** — `sapNavigateTreePath`: accepts a parent/child **node key** path in
   `Data` (e.g. `Root\Child`) and internally expands the parent node before selecting the child,
   removing the sequencing guesswork from Item #13.

3a. **Tree child-by-text lookup** — `sapSelectTreeNodeByText`/`sapSelectTreeNodeByTextContains`:
   resolves the child by its **visible text** instead of the raw node key — expands the parent,
   scans its children via `GetSubNodesCol`/`GetNodeTextByKey`, and selects the first one whose text
   matches (exact or contains, as separate actions, mirroring the row-by-text actions above).
   Completes Item #13 for callers who only know the child's business text, not its SAP GUI key.

4. **`SAPFindType.TEXT`** (or equivalent) at the object-repository level, so a static object
   definition can say "resolve by visible text" generically, addressing Item #6's "object using
   text property" resolution note at the locator layer rather than only inside table/grid actions.

## Open Questions

- ~~Should row/column text matching be exact-equals or contains/substring by default, and should
  that be selectable via `Condition` per call?~~ Resolved: implemented as separate exact and
  `...Contains` actions (`sapSelectTableRowByText`/`sapSelectTableRowByTextContains`,
  `sapSelectGridRowByText`/`sapSelectGridRowByTextContains`,
  `sapSetCheckBoxInMatchingRow`/`sapSetCheckBoxInMatchingRowContains`) instead of a
  `Condition`-selected mode on one action.
- For tree navigation, should the child be resolved by SAP GUI node key (fast, but requires users to
  know the key) or by visible text (friendlier, but requires an extra lookup pass over child nodes)?
  Resolved: both are supported — `sapNavigateTreePath` for a known node key, and
  `sapSelectTreeNodeByText`/`sapSelectTreeNodeByTextContains` for visible-text resolution.
- Should `sapSetCheckBoxInMatchingRow` fail (`FAILNS`) or no-op when no row matches, given it's used
  in assertion-adjacent business validation flows? Resolved: `FAILNS` — already implemented that way
  (no silent no-op), since a missing match in a business-validation flow should surface as a failure.
