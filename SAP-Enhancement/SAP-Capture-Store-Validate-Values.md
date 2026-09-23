# User Story 9917336: [SAP] Capture, Store and Validate SAP Values and Results

Source: `SAP Ingenious Issues 2026.xlsx` — sheet `SAP Requests` (Items #5, #7, #8, #9), sheet `US`.

## User Story

As an INGenious engineer, I want to retrieve values from SAP screens, store those values, and
validate them against expected results, so that dynamic SAP data can be verified automatically
during execution.

## Acceptance Criteria (ATDD)

### Scenario 1: Capture text from SAP

**Given** a SAP field contains a value

**When** the Get Text action is executed

**Then** the value is captured successfully

### Scenario 2: Store values for later use

**Given** a value is captured

**When** a future step references the value

**Then** the stored value is available for execution

### Scenario 3: Validate text values

**Given** an expected value is configured

**When** an Assert Text action is executed

**Then** the actual value matches the expected value

### Scenario 4: Compare numerical values

**Given** a numerical value is captured

**When** a comparison is executed

**Then** operators such as `=`, `!=`, `>`, `<`, `>=` and `<=` are supported

### Scenario 5: Documentation

**Given** the feature is implemented

**When** documentation is updated

**Then** usage examples for Get Text and Assert Text are available in ingenious-doc

## Related SAP Request Items

### Item #5

**Description:** Assertion of title not able to match with text.

**Resolution note (from tracker):** Create assert text action.

### Item #7

**Description:** How to capture value into a text to be used later in the next steps? Using
Assert, Assert Validate or GlobalVar.

**Resolution note (from tracker):** Create get text value action.

### Item #8

**Description:** ST22 — how to capture number for check `<> 0`.

**Additional detail:** How to get the values of runtime errors using assert or something else?

**Resolution note (from tracker):** Create get text value action.

### Item #9

**Description:** How to get the values from Result and compare if `<>` something.

**Additional detail:** Check where Logon is the action, the value of result should be in `ms`
and not `Connection Error`.

**Resolution note (from tracker):** Create assert text action.

## Context

Users need the ability to capture dynamic values from SAP screens, runtime errors (e.g. `ST22`
dumps), transaction results and window titles, store them for later use, and validate them using
assertions and comparison logic.

## Goal

Provide reusable actions for getting text values, storing values, and performing text and
numeric assertions.

## Value

- Supports dynamic validation.
- Improves automation quality.
- Reduces custom coding.
- Supports reusable business validations.

## Scope

Affected Components:

- SAP Actions
- Assertion Framework
- Variable Management
- Workflow Actions
- Shared Utilities

## Current State (gap analysis)

`Engine/src/main/java/com/ing/engine/commands/SAP/SAPActions.java` already provides some of the
building blocks, but coverage is narrow:

| Action | What it does | Gap |
|---|---|---|
| `sapGetStatusBarText` | Reads the status bar `Text` property and stores it via `addVar("StatusBarText", ...)` | Hard-coded to the status bar element and a fixed variable name — can't capture an arbitrary object's text (e.g. a window title, `ST22` dump field, result label) into a user-chosen variable. |
| `sapAssertElementTextContains` | Asserts that an element's `Text` **contains** a literal substring | Only supports "contains". No equals/not-equals, no numeric comparison (`<>`, `>`, `<`, `>=`, `<=`), and no window-**title** assertion. |

`Engine/src/main/java/com/ing/engine/commands/general/GeneralOperations.java` provides
variable-level assertions, but they are also equality-only:

| Action | What it does | Gap |
|---|---|---|
| `assertVariable` | Splits `Data` on `=` and asserts exact string equality (resolving `%var%` on the left side) | No `<>`/`!=`, no numeric operators, no way to assert "not equal to a value" as requested in Items #8/#9. |
| `assertVariableFromDataSheet` | Asserts a variable equals a value pulled from a data sheet | Same equality-only limitation. |

Variable/global-variable storage plumbing (`Command.addVar`, `CommandControl.addVar`,
`GeneralOperations.AddVar`, `Database.storeValueInGlobalVariable`) already exists and is reused
by `sapGetStatusBarText`, so new "get text" actions should follow the same pattern rather than
introducing a new storage mechanism.

## Proposed Actions

1. **`sapStoreElementTextInVariable`** (SAP object action, no input) — generalizes `sapGetStatusBarText`:
   reads `Text` off any `[<Object>]` (window title bar, result field, `ST22` dump field, etc.)
   and stores it into a variable name supplied via `Condition`/`Data`, instead of a fixed key.
   Covers Item #7 and the capture half of Item #8.

1a. **`sapStoreElementTextInDataSheet`** — same capture, but stores into a datasheet cell
   (`Data` = `Sheet:Column`, following the existing `store<Data>InDataSheet` convention used by
   `storeElementTextinDataSheet` / `storeDBValueinDataSheet`) instead of a runtime variable, for
   callers who need the captured value to persist/iterate via a data sheet rather than `%var%`.

2. **`sapAssertElementTextEquals`** — asserts an element's `Text` against expected data,
   addressing Item #5. This also covers window-**title** assertions: SAP GUI window elements
   (`GuiFrameWindow`/`GuiMainWindow`/`GuiModalWindow`) have no separate `Title` property - the
   title bar text is exposed through the same `Text` property every other `GuiVComponent` uses,
   so no distinct title action/property is needed.

3. **Numeric/inequality assertion support** — extend the assertion vocabulary beyond equality:
   - `sapAssertElementNumericValue` (SAP object variant) and a general
     `assertNumericValue` (`GeneralOperations` variant, so it also works on plain variables
     and datasheet-sourced values).
   - `Condition` carries the operator (`==`, `<>`, `>`, `<`, `>=`, `<=`); `Data` carries the
     expected value. Non-numeric actual values (e.g. `Connection Error` instead of a duration in
     `ms`, per Item #9) should fail the assertion with a clear message rather than throwing an
     unhandled `NumberFormatException`.
   - Directly resolves Item #8 (`ST22` numeric check `<> 0`) and Item #9 (`Result <> something`,
     including the "not a connection error" case).

4. Reuse existing storage plumbing (`addVar`, `putUserDefinedData`) for all new "get"/"store"
   actions — no new variable-storage mechanism is introduced.

## Open Questions

- Should numeric assertions coerce/parse values (e.g. strip `ms`, thousands separators) before
  comparison, or require the captured value to already be a clean number? Resolved:
  `NumberConditionEvaluator` extracts the first numeric token from either side (e.g. `0` from
  `"0 Runtime Errors"`, `1500` from `"Duration: 1500 ms"`) instead of requiring a clean number.
  Thousands separators (e.g. `1,234`) are not handled yet - a plain digit run per token only.
- Should window-title assertions be a distinct SAP object action, or should `sapStoreElementTextInVariable` /
  `sapAssertElementTextContains` be extended to accept a `Title` property alongside `Text`?
  Resolved: no extension needed - SAP GUI window elements don't have a separate `Title` property
  at all, so `sapAssertElementTextEquals`/`sapAssertElementTextContains` pointed at a window
  object already asserts its title, via the same `Text` property.
