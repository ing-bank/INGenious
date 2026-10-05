# User Story: [SAP] Validate Visual Status Indicators (Colored Boxes)

Source: `SAP Ingenious Issues 2026.xlsx` — sheet `SAP Requests` (Item #14), sheet `US`.

## User Story

As an INGenious engineer, I want to validate visual SAP status indicators such as colored status
boxes, so that health check scenarios can automatically verify the operational state of SAP
configurations.

## Acceptance Criteria (ATDD)

### Scenario 1: Validate green status indicator

**Given** a SAP screen contains status indicators

**When** validation is executed

**Then** the configured indicators are verified as green

### Scenario 2: Detect unexpected indicator state

**Given** a SAP status indicator is not green

**When** validation is executed

**Then** the validation fails

### Scenario 3: Report indicator validation results

**Given** status validation has completed

**When** the execution report is generated

**Then** the validation result is included in the execution report

### Scenario 4: Documentation is updated

**Given** visual status validation support is implemented

**When** documentation is updated

**Then** setup instructions and usage examples are available in ingenious-doc

## Related SAP Request Items

### Item #14

**Description:** How to verify that colored boxes are in green for Filter 1-8 for dynamic and
Static configuration. If other then raise that case to be failed.

## Context

Certain SAP monitoring and configuration screens use color indicators to represent system health
and configuration status. Users need an automated way to validate that expected indicators remain
in an acceptable state.

## Goal

Provide validation actions capable of identifying and verifying SAP visual status indicators and
failing execution when unexpected states are detected.

## Value

- Improves automated health-check coverage.
- Detects configuration issues earlier.
- Eliminates manual visual verification.

## Scope

Affected Components:

- SAP Automation Framework
- SAP UI Validation Actions
- Validation Framework
- Workflow Actions
- Reporting Engine

## Current State (gap analysis)

**"Green" cannot mean rendered pixel color here — this is the same JACOB limitation already
established for screenshots.** As documented in
`SAP-Enhancement/07-SAP-Execution-Evidence-Screenshot-Capture.md`, JACOB (the COM bridge all SAP
automation in this codebase is built on) has no bitmap/imaging API at all - only property
get/set on the SAP GUI Scripting object model. A "colored box" therefore can't be validated by
reading its actual on-screen RGB value; it has to be validated through whatever **semantic**
property the SAP GUI Scripting object model exposes for that control instead. This is exactly
the same substitution already made for the status bar: `sapAssertStatusBarType`
(`Engine/src/main/java/com/ing/engine/commands/SAP/SAPActions.java`) doesn't read a color either -
it reads the status bar's `MessageType` property (`S`/`W`/`E`/`I`/`A`) as a stand-in for
green/red/yellow. A colored status box needs the equivalent stand-in property for *that* control
type.

**No action in this codebase reads a control's icon or tooltip today.** Every existing
`sapAssertElement*`/`sapStoreElement*` action (`sapAssertElementTextEquals`,
`sapAssertElementTextContains`, `sapStoreElementTextInVariable`, etc., all added earlier this
session) reads the `Text` property only. `GuiVComponent`-derived controls that render as a status
icon/box (`GuiLabel`, `GuiButton`, ALV cell icons, etc.) commonly expose the icon identity through
an `IconName` property and/or a `Tooltip` property instead of (or in addition to) `Text` - neither
is read anywhere in `SAPActions.java`. Which of `Text`/`IconName`/`Tooltip` actually carries the
"green/red/yellow" semantic depends entirely on how the specific custom screen behind Item #14
implements its "Filter 1-8" boxes (a custom Z-transaction/monitoring dashboard, not a standard SAP
screen this analysis has visibility into) - this needs to be confirmed against the real screen via
the SAP GUI Scripting Recorder before implementation, not assumed.

**No batch/loop assertion mechanism exists, nor is one needed.** Every prior multi-item action
added this session (table/grid row lookups, tree node selection) operates on **one** row/node per
step, addressed by an explicit match — there is no "assert all N rows satisfy X" action anywhere
in `SAPActions.java`, deliberately. "Filter 1-8" (8 named boxes) fits the same shape: 8 ordinary
object references in the Object Repository, each checked by one ordinary assertion step - no new
looping construct is needed, consistent with how every other multi-element scenario in this
codebase is handled (and reusable-component wrapping, per the `ingenious-browser-test-from-specification`
skill's "reusable component flows", already lets a tester wrap "assert one filter is green" once
and invoke it 8 times with 8 different object references).

## Proposed Actions

1. **`sapAssertElementIconEquals`** (new, `Object` + `Data`) — asserts a control's `IconName`
   property equals an expected icon identifier (e.g. `ICON_LED_GREEN`, or whatever resource name
   the target screen actually uses - to be confirmed via the recorder). Mirrors
   `sapAssertElementTextEquals`'s shape exactly, just reading `IconName` instead of `Text`.

2. **`sapAssertElementTooltipContains`** (new, `Object` + `Data`) — asserts a control's `Tooltip`
   **contains** an expected substring (e.g. `"Green"`, `"OK"`), for screens where the
   status/color semantic is only exposed through the hover text rather than a distinct icon
   resource name. Mirrors `sapAssertElementTextContains`'s shape, reading `Tooltip` instead.

3. **`sapStoreElementIconInVariable`** (new) — captures a control's `IconName` into a variable,
   generalizing `sapStoreElementTextInVariable` the same way `sapAssertElementIconEquals`
   generalizes `sapAssertElementTextEquals` - lets a captured icon identifier feed into
   `GeneralOperations.assertVariable`/`assertVariableFromDataSheet` for equality checks against a
   data-sheet-configured expected value, instead of hard-coding the expected icon name in every
   step (useful since "Filter 1-8" implies checking the same condition across many named objects).

4. **No new "raise as failed" mechanism needed for Scenario 2/3** — every proposed action above
   reports through the existing `Status.PASS`/`FAIL` (screenshot-eligible, per this session's
   retrofit) or `PASSNS`/`FAILNS` convention and the existing `updateTestLog`/report pipeline;
   Scenario 3's "included in the execution report" is already satisfied by that same existing
   mechanism, same as every other assertion action.

## Open Questions

- **Which property actually carries the color semantic on the real "Filter 1-8" screen -**
  `IconName`, `Tooltip`, or plain `Text`? Not resolved - this is a custom/Z-transaction screen
  this analysis has no visibility into; needs to be confirmed by recording the actual screen with
  the SAP GUI Scripting Recorder before `sapAssertElementIconEquals`/`Tooltip` variants are
  implemented, rather than assumed from generic SAP GUI Scripting knowledge. It's possible more
  than one of these three ends up needed (e.g. icon for the color, tooltip for the human-readable
  reason).
- **Dynamic vs Static configuration wording in Item #14** — the request distinguishes "dynamic
  and Static configuration" for Filter 1-8; it's unclear whether this means two separate sets of
  8 boxes (16 objects total), or one set of 8 boxes with a mode toggle. Needs clarification from
  the request owner - affects the object repository count more than the action design itself.
- **Expected icon/tooltip literal values** — once the real property is confirmed, the actual
  "green" identifier string(s) it can hold (e.g. does the same screen ever show yellow/red for a
  legitimately different-but-still-acceptable state, or is green strictly pass/fail) need to be
  captured from the live screen, not guessed.
