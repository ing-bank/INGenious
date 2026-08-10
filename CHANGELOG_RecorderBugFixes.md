# INGenious 3.0 — Recorder Bug Fixes Notes

Generated: 2026-06-27

## Table of Contents

1. [Playwright Recorder — OR Exact Attribute Encoding Fix](#1-playwright-recorder--or-exact-attribute-encoding-fix)
2. [Playwright Recorder — Duplicate Test Case Names Across Scenarios](#2-playwright-recorder--duplicate-test-case-names-across-scenarios)
3. [Playwright Recorder — Enter Key Starts Recording](#3-playwright-recorder--enter-key-starts-recording)
4. [Playwright Recorder — Trailing Empty Step on External Browser Close](#4-playwright-recorder--trailing-empty-step-on-external-browser-close)
5. [Playwright Recorder — Page/Tab Switch Steps Missing](#5-playwright-recorder--pagetab-switch-steps-missing)
6. [Playwright Recorder — Frame Locator Detection During Live Recording](#6-playwright-recorder--frame-locator-detection-during-live-recording)
7. [RecordFromHere — Recorded Steps Inserted at Wrong Index](#7-recordfromhere--recorded-steps-inserted-at-wrong-index)

---

---

## 1. Playwright Recorder — OR Exact Attribute Encoding Fix

### What changed

When a Playwright codegen recording used a locator with `.setExact(true)` (e.g. `getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Special Hot").setExact(true))`), the live recording showed the raw attribute value `LINK;Special Hot;exact` in the Object Repository and left the **Exact** checkbox unchecked. File import of the same recording showed the correct, clean value `LINK;Special Hot` with the Exact checkbox checked. The two code paths now produce identical in-memory objects.

---

### Root cause

The Playwright recorder's `attributeInitialization` method encodes Playwright's `setExact(true)` option as a `;exact` suffix appended to the attribute string (e.g. `LINK;Special Hot;exact`). The OR-building loop called `WebORObject.setAttributeByName(key, value)`, which only sets the raw string value — it never touched the structured `ORAttribute.exact` boolean flag. The live recorder showed the in-memory object directly (no reload), so the raw suffix and `exact = false` were visible.

File import appeared correct by accident: YAML round-tripping through `YamlElementDefinition.fromWebORObject` strips the `;exact` suffix into a structured `exact:` YAML field, and `toWebORObject` on the next project reload calls `orAttr.setExact(true)` to rebuild the clean value. The live recorder has no such reload cycle.

---

### Fix

In `PlaywrightRecordingParser.parseLinesToSteps`, the OR-building loop now normalizes every attribute value as it is written:

1. If `value.endsWith(";exact")`, strip the 6-character suffix and set a local `exactFlag = true`.
2. Write the clean value via `obj.setAttributeByName(key, cleanValue)`.
3. Look up the structured attribute object and call `orAttr.setExact(true)` when `exactFlag` is set.

This normalization applies to both the live-recording path and the file-import path (both share `parseLinesToSteps`), so the in-memory OR object is now identical in both cases.

---

### Files changed

| File | Status |
|---|---|
| [IDE/src/main/java/com/ing/ide/main/playwrightrecording/PlaywrightRecordingParser.java](IDE/src/main/java/com/ing/ide/main/playwrightrecording/PlaywrightRecordingParser.java) | Modified |

**Breaking changes**: none.

---

## 2. Playwright Recorder — Duplicate Test Case Names Across Scenarios

### What changed

The "Start Recording" dialog and all tree drag-and-drop operations previously enforced **globally unique** test case names across the entire project (Test Plan, Reusable, and Shared Reusable scopes). This blocked creating a test case named `Login` in `ScenarioA` when a test case with that name already existed anywhere else in the project. Names are now required to be unique only within the same scenario.

---

### Root cause

`Scenario.addTestCase(String)` contained a call to `project.testCaseExistsInAnyScope(testCaseName)` that rejected names present in any scenario, regardless of scope. The same guard was replicated in the three IDE tree classes (`ProjectTree.fetchNewTestCaseName`, `ReusableTree.fetchNewReusableTestCaseName`, `SharedReusableTree.fetchNewSharedReusableTestCaseName`) and in `ProjectDnD` for both the copy-with-suffix suffix loop and the `copyTestCases` method.

---

### Fix

- **`Scenario.addTestCase`** — removed the `project.testCaseExistsInAnyScope(testCaseName)` cross-scope check. Now returns `null` only if the name already exists within the same scenario (via `getTestCaseByName`).
- **`ProjectTree.fetchNewTestCaseName`** — removed the `&& !getProject().testCaseExistsInAnyScope(...)` conjunct.
- **`ReusableTree.fetchNewReusableTestCaseName`** — same relaxation.
- **`SharedReusableTree.fetchNewSharedReusableTestCaseName`** — same relaxation.
- **`ProjectDnD.addTestCase` and `ProjectDnD.copyTestCases`** — removed the `|| project.testCaseExistsInAnyScope(newName)` conditions from the suffix-generation `while` loops.

Scenario-name uniqueness and within-scenario test case name uniqueness are both preserved unchanged.

---

### Files changed

| File | Status |
|---|---|
| [Datalib/src/main/java/com/ing/datalib/component/Scenario.java](Datalib/src/main/java/com/ing/datalib/component/Scenario.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/mainui/appln/trees/ProjectTree.java](IDE/src/main/java/com/ing/ide/main/mainui/appln/trees/ProjectTree.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/mainui/appln/trees/ReusableTree.java](IDE/src/main/java/com/ing/ide/main/mainui/appln/trees/ReusableTree.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/mainui/appln/trees/SharedReusableTree.java](IDE/src/main/java/com/ing/ide/main/mainui/appln/trees/SharedReusableTree.java) | Modified |
| [IDE/src/main/java/com/ing/ide/main/mainui/appln/trees/dnd/ProjectDnD.java](IDE/src/main/java/com/ing/ide/main/mainui/appln/trees/dnd/ProjectDnD.java) | Modified |

**Breaking changes**: none.

---

## 3. Playwright Recorder — Enter Key Starts Recording

### What changed

The **Start Recording** dialog (`RecordingTargetDialog`) did not respond to the Enter key. Users had to click the button with the mouse to proceed. Enter now activates the **Start Recording** button, matching standard dialog behaviour.

---

### Fix

After constructing the `ok` button (`JButton ok = new JButton("Start Recording")`), a single call was added:

```java
getRootPane().setDefaultButton(ok);
```

This designates the button as the dialog's default button, so pressing Enter fires the action listener regardless of which dialog component currently has focus.

---

### Files changed

| File | Status |
|---|---|
| [IDE/src/main/java/com/ing/ide/main/playwrightrecording/RecordingTargetDialog.java](IDE/src/main/java/com/ing/ide/main/playwrightrecording/RecordingTargetDialog.java) | Modified |

**Breaking changes**: none.

---

## 4. Playwright Recorder — Trailing Empty Step on External Browser Close

### What changed

When the user closed the recording browser (Chrome) externally (via the window's close button or OS task manager) instead of using INGenious's **Stop Recording** control, an extra empty step was appended as the final step of the recorded test case. Stopping from within INGenious did not have this problem.

---

### Root cause

When the browser is closed externally, Playwright codegen flushes its teardown lines to the output file before the process exits. These lines take the form:

```java
page.close();
context.close();
browser.close();
```

The parser's line-exclusion guard did not filter `.close()` calls. Each such line starts with `page`, so it incremented the `playwrightSteps` counter and entered the step-emission block. Because there is no locator or action on a `.close()` line, the resulting step had empty fields.

In contrast, when INGenious triggers the stop (`stopPlaywrightRecording`), the process is force-killed before codegen can flush those teardown lines.

---

### Fix

`!line.contains(".close()")` was added to the line-exclusion guard in `parseLinesToSteps`, alongside the existing guards for `System.out.println(`, `.onceDialog(dialog`, and `.waitForPopup(() ->`. Teardown lines are now silently skipped regardless of how the browser session ends.

---

### Files changed

| File | Status |
|---|---|
| [IDE/src/main/java/com/ing/ide/main/playwrightrecording/PlaywrightRecordingParser.java](IDE/src/main/java/com/ing/ide/main/playwrightrecording/PlaywrightRecordingParser.java) | Modified |

**Breaking changes**: none.

---

## 5. Playwright Recorder — Page/Tab Switch Steps Missing

### What changed

When a recording navigated across multiple browser tabs/pages, neither live recording nor file import emitted `switchToPageByIndex` steps for actions that resumed on an **already-open** page. Only the initial `clickAndSwitchToNewPage` step (which opens the new tab) was captured. All subsequent interactions on other tabs appeared as if they ran on whichever tab happened to be active, producing test cases that could not be replayed correctly in a multi-tab scenario.

---

### Root cause

`PlaywrightRecordingParser.parseLinesToSteps` already tracked popup-opening events:

- `checkPageSwitch(line)` detected `Page pageN = page.waitForPopup(...)` lines and set a `pageSwitchOnClick` flag.
- `storePageIndex(line)` recorded the mapping `pageN → N` (the numeric browser-context index) in `pageMapping` and set `switchedPageName` to the new page variable.
- `getAction(line)` consumed `pageSwitchOnClick` and returned `"clickAndSwitchToNewPage"` for the triggering click.

However, there was no mechanism to track **which page was currently active** and therefore no way to detect when subsequent action lines targeted a different already-open page and emit a `switchToPageByIndex` step before that action. Playwright codegen names pages `page`, `page1`, `page2`, ... in creation order, with the numeric suffix equalling the browser-context index (0 for the implicit `page`).

---

### Fix

Three coordinated changes were made inside `parseLinesToSteps`:

**1. Active-page tracker**

Before the line-processing loop:

```java
// Tracks the page/tab that subsequent actions run against.
String activePageIndex = "0";
```

**2. Per-action page-switch detection**

At the top of the step-emission block, before `testCaseMap(...)`:

```java
String resolvedAction = getAction(line);   // called exactly once — consumes pageSwitchOnClick

String linePageVar   = line.trim().split("\\.")[0];
String linePageIndex = null;
if (linePageVar.equals("page")) {
    linePageIndex = "0";
} else if (linePageVar.startsWith("page") && pageMapping.containsKey(linePageVar)) {
    linePageIndex = pageMapping.get(linePageVar);
}
if (linePageIndex != null && !linePageIndex.equals(activePageIndex)) {
    steps.add(new ParsedStep("Browser", "switchToPageByIndex", "@" + linePageIndex, ""));
    activePageIndex = linePageIndex;
}
```

Lines that do not begin with a known page variable (e.g. `assertThat(...)`) resolve to `null`, so no spurious switch is emitted.

**3. Advance active page after opening a new tab**

After the step is appended to the list:

```java
if ("clickAndSwitchToNewPage".equals(resolvedAction)) {
    String newPageVar = pageMapping.get("switchedPageName");
    if (newPageVar != null && pageMapping.containsKey(newPageVar)) {
        activePageIndex = pageMapping.get(newPageVar);
    }
}
```

This keeps `activePageIndex` in sync when a click opens a new tab so that any subsequent return to an older page is correctly detected.

---

### Example

Given a session with five pages (`page` = 0, `page1` = 1, `page2` = 2, `page3` = 3, `page4` = 4), the parser now automatically inserts `switchToPageByIndex` steps whenever the active tab changes:

```
step: clickAndSwitchToNewPage          → activePageIndex becomes 1
step: [action on page1]
step: switchToPageByIndex @0           → resuming page 0
step: [action on page]
step: clickAndSwitchToNewPage          → activePageIndex becomes 2
step: [action on page2]
step: switchToPageByIndex @0           → resuming page 0
...
```

Both live recording and file import use the same `parseLinesToSteps` method and benefit from this fix.

---

### Files changed

| File | Status |
|---|---|
| [IDE/src/main/java/com/ing/ide/main/playwrightrecording/PlaywrightRecordingParser.java](IDE/src/main/java/com/ing/ide/main/playwrightrecording/PlaywrightRecordingParser.java) | Modified |

**Breaking changes**: none.

---

## 6. Playwright Recorder — Frame Locator Detection During Live Recording

### What changed

Actions performed inside an `<iframe>` were not captured correctly during live recording. Instead of producing an OR object with the correct **Frame** attribute set, they either fell back to a raw `Refactor_Object` chained locator or were parsed incorrectly. File import of the same recording worked because older exported scripts use the `frameLocator(...)` API. The two paths now produce identical results.

---

### Root cause

Playwright codegen has two syntaxes for frame interactions depending on the version used:

| Source | Emitted syntax |
|---|---|
| Imported script (older Playwright) | `page.frameLocator("css=iframe").getByRole(...)` |
| Live recorder (newer Playwright) | `page.locator("css=iframe").contentFrame().getByRole(...)` |

`attributeInitialization` (the parser's OR-building method) only understood the `frameLocator(...)` form. When it encountered a `.contentFrame()` line it could not match the frame selector pattern, so the element was misclassified and the `frame` attribute was never populated on the resulting `WebORObject`.

---

### Fix

A new static helper method `normalizeContentFrame(String line)` was added to `PlaywrightRecordingParser`. It is called at the very top of `attributeInitialization` (before any other processing), and rewrites every occurrence of the modern `.contentFrame()` syntax back to the classic `frameLocator` form using a single regex replacement:

```java
line.replaceAll("\\.locator\\((.*?)\\)\\.contentFrame\\(\\)", ".frameLocator($1)")
```

This handles both single frames and nested frames (chains of `.locator(...).contentFrame()` calls), since the replacement is applied to every match in the line. After normalisation, the existing `frameLocator`-based parsing path in `attributeInitialization` resolves the frame selector and locator attributes exactly as it does for imported scripts, producing a `WebORObject` with the `frame` field populated correctly.

---

### Files changed

| File | Status |
|---|---|
| [IDE/src/main/java/com/ing/ide/main/playwrightrecording/PlaywrightRecordingParser.java](IDE/src/main/java/com/ing/ide/main/playwrightrecording/PlaywrightRecordingParser.java) | Modified |

**Breaking changes**: none.

---

## 7. RecordFromHere — Recorded Steps Inserted at Wrong Index

### What changed

When a test case reached a `RecordFromHere` step and the user recorded new interactions, the captured steps were inserted at the **top** of the test case (row 0) instead of immediately after the `RecordFromHere` step. Any steps that already existed below it were displaced to the top of the list.

---

### Root cause

`TestCaseRunner` holds a `currentStepIndex` field that the IDE's `RecordFromHereHook` queries via `getCurrentStepIndex()` to determine where to insert recorded steps. The field was declared with an initial value of `-1` and **was never updated** during the execution loop:

```java
private volatile int currentStepIndex = -1;
```

Because the field stayed at `-1`, the hook calculated:

```java
final int firstInsertIndex = Math.max(insertAfterStepIndex + 1, 0);
// -1 + 1 = 0  →  always inserted at the top
```

This caused every recorded step to be prepended at row 0 regardless of where `RecordFromHere` actually appeared in the test case.

---

### Fix

In `TestCaseRunner.runTestCase`, `currentStepIndex` is now assigned at the start of each loop iteration, before the step is executed:

```java
for (int currStep = 0; canRunStep(currStep); currStep++) {
    TestStep testStep = testCase.getTestSteps().get(currStep);
    currentStepIndex = currStep;   // ← added
    ...
}
```

This ensures that when `RecordFromHere` fires and the hook calls `getCurrentStepIndex()`, it receives the correct zero-based index of the executing step. The hook then computes `firstInsertIndex = currentStepIndex + 1`, so the recorded steps are inserted immediately below the `RecordFromHere` row and all subsequent existing steps are pushed down.

---

### Files changed

| File | Status |
|---|---|
| [Engine/src/main/java/com/ing/engine/execution/run/TestCaseRunner.java](Engine/src/main/java/com/ing/engine/execution/run/TestCaseRunner.java) | Modified |

**Breaking changes**: none.
