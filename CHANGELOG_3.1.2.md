# INGenious 3.1.2 — Release Notes

Generated: 2026-06-14

## Table of Contents

1. [Build and Test Stability on JDK 26](#1-build-and-test-stability-on-jdk-26)
2. [Per-Step Hard/Soft Assertion Controls](#2-per-step-hardsoft-assertion-controls)
3. [Validation Error Red-Marking Across Trees](#3-validation-error-red-marking-across-trees)
4. [Case-Only Rename Reliability Fix](#4-case-only-rename-reliability-fix)
5. [Live Recording Feature Research and Technical Findings](#5-live-recording-feature-research-and-technical-findings)
6. [Focus Issue on Newly Created Items](#6-focus-issue-on-newly-created-items)
7. [Object Repository — Same-Name Rename Blocked Incorrectly](#7-object-repository--same-name-rename-blocked-incorrectly)
8. [assertURLmatches — Pattern Compile Error](#8-asserturlmatches--pattern-compile-error)
9. [Test Datasheet Renaming / Global Datasheet Addition Crash](#9-test-datasheet-renaming--global-datasheet-addition-crash)
10. [Manage Devices — Accordion Scroll Fix](#10-manage-devices--accordion-scroll-fix)
11. [Reusable Scenario Creation Process](#11-reusable-scenario-creation-process)
12. [Object Repository — UX Improvements](#12-object-repository--ux-improvements)
13. [Detailed Report — Cross-Iteration Reusable Expand Fix](#13-detailed-report--cross-iteration-reusable-expand-fix)

---

## 1. Build and Test Stability on JDK 26

**What changed**  
Test execution and build behavior were stabilized for JDK 26 environments.

**Details**
- Added Maven Surefire system property `net.bytebuddy.experimental=true` at parent build level to prevent Mockito inline mocking failures caused by Byte Buddy version checks on JDK 26.
- Verified repository-wide build with `mvn clean install` (all modules successful).
- Documented JaCoCo incompatibility on JDK 26 (`Unsupported class file major version 70` with JaCoCo 0.8.12) and established operational fallback for tests using `-Djacoco.skip=true`.

**Impact**
- Developers can run tests consistently on JDK 26 with fewer environment-specific failures.
- Coverage collection may need to be skipped in JDK 26 pipelines until JaCoCo/toolchain alignment is upgraded.

**Breaking changes**: none.

---

## 2. Per-Step Hard/Soft Assertion Controls

**What changed**  
Assertion behavior is now configurable at individual test-step level instead of being uniformly soft.

**Data model behavior**
- Introduced per-step hard assertion marker `~` in `TestStep` tag metadata.
- Existing marker compatibility retained:
  - `*` for breakpoint
  - `//` for comment
- Assertion logic enhancements include:
  - `isAssertStep()` to identify assertion actions.
  - `isHardAssertion()` to detect hard-assert marker presence.
  - `setHardAssertion(...)` to toggle hard assertion while preserving existing numbering/tag handling.

**IDE behavior**
- Added assert-step context menu options:
  - `Soft Assertion` (default)
  - `Hard Assertion`
- Options are shown only for assertion steps and are mutually exclusive.

**Engine runtime behavior**
- After step execution, if a step is marked hard assertion and fails (`FAIL` or `FAILNS`), current iteration stops immediately.
- Execution flow then continues according to outer loop behavior (next iteration when applicable).

**Impact**
- Teams can mix soft and hard assertions in the same test case without custom flow workarounds.
- Fast-fail behavior becomes deterministic and explicit at step level.

**Breaking changes**: none. Default behavior remains soft unless a step is explicitly marked hard.

---

## 3. Validation Error Red-Marking Across Trees

**What changed**  
Scenario and test case nodes are now visually marked red when any underlying step has validation errors, including reusable dependency propagation.

**Implementation summary**
- Added renderer-backed validation aggregation through `TestCaseValidation`.
- Validation is computed at step level and rolled up to:
  - Test case nodes
  - Scenario nodes
  - Execute/reusable call sites
- Reusable propagation supported: if a reusable has errors, callers and relevant execute steps are marked red.

**UI refresh behavior**
- Save-triggered repaint added so tree color state refreshes immediately after edits.
- Both project and reusable trees are refreshed to keep visual state consistent.

**Lazy-load and performance handling**
- Addressed lazy loading of test steps by introducing async validation pass on project load.
- Added cache behavior for unloaded test cases so tree validation remains accurate before manual file opens.
- Introduced thread-safe validation strategy (thread-local renderer usage) to avoid UI/background race conditions.

**Impact**
- Validation defects are visible earlier and at navigation level, not only inside the editor table.
- Reusable dependency issues are surfaced where they are consumed.

**Breaking changes**: none.

---

## 4. Case-Only Rename Reliability Fix

**What changed**  
Case-only renames (example: `ABC` → `abc`) now work correctly across entities and filesystems.

**Root cause**
- Name lookups were case-insensitive (`equalsIgnoreCase`) and interpreted case-only rename targets as already-existing collisions.

**Fix strategy**
- Updated rename validation pattern to allow rename when the located match is the same entity instance (or no entity found).
- Applied across major model entities including scenarios, test cases, releases, test sets, object groups, and OR entities.
- File rename utility updated for case-insensitive filesystems using safe two-step rename flow:
  - `source -> temp`
  - `temp -> target`

**IDE behavior**
- Existing UI rename checks were already case-sensitive and required no additional change.

**Impact**
- Eliminates false “already present” failures for case-normalization renames.
- Improves cross-platform consistency (especially macOS/Windows case-insensitive defaults).

**Breaking changes**: none.

---

## 5. Live Recording Feature Research and Technical Findings

**What was delivered**  
A structured discovery pass was completed to de-risk implementation of live recording import/creation workflows.

**Findings documented**
- Core Datalib model and persistence flow for:
  - Project
  - Scenario
  - TestCase
  - TestStep
- IDE loading path for test-case display in `TestCaseComponent`.
- Tree reload mechanics in `ProjectTree` and reusable tree interaction.
- `PlaywrightRecordingParser` parse/write flow and CSV generation behavior.
- Clear distinction and storage model for TestPlan vs ReusableComponents scenarios.

**Impact**
- Provides implementation-ready map for future live recording improvements.
- Reduces integration risk by clarifying where object creation, persistence, and tree refresh need to occur.

**Breaking changes**: none (research/documentation milestone).

---

## 6. Focus Issue on Newly Created Items

**Symptom**
When adding a new scenario or test case from the project/reusable tree, the newly created node was not left selected and visible. Pressing the `New` keyboard shortcut while a test case node was selected did nothing instead of adding a sibling test case.

**Root cause**
- `onNewAction()` in both `ProjectTree` and `ReusableTree` only triggered the add-test-case path when a *scenario* node was selected; a *test-case* selection was silently ignored.
- `addTestCase()` / `addReusableTestCase()` only operated on `getSelectedScenarioNode()` and did not fall back to the parent of a selected test case node.
- `selectAndScrollTo()` called `tree.removeSelectionPath(path)` immediately after setting the selection, causing the new node to lose focus.

**Fix**
- `onNewAction()` updated to also trigger add-test-case when a test case node is already selected (`|| getSelectedTestCaseNode() != null`).
- `addTestCase()` / `addReusableTestCase()` now resolve the parent `ScenarioNode` from the selected test case when no scenario node is directly selected.
- Removed the spurious `removeSelectionPath` + `addSelectionPaths` calls from `selectAndScrollTo()` so the newly created item keeps focus.

**Files**
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ProjectTree.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ProjectTree.java)
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ReusableTree.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ReusableTree.java)

**Breaking changes**: none.

---

## 7. Object Repository — Same-Name Rename Blocked Incorrectly

**Symptom**
Renaming an OR object to a name that differed only in case (e.g. `btnLogin` → `BtnLogin`) was rejected with a "name already present" error because the duplicate-name check found the object itself.

**Root cause**
`WebORObject.rename()` checked `if (getParent().getObjectByName(newName) != null)` — the lookup returned the current object, which was then treated as a collision.

**Fix**
The guard was tightened to `if (existing != null && existing != this)` so a case-only rename on the same object is allowed.

**Additional change**
`WebORTable.save()` now calls `table.getCellEditor().stopCellEditing()` before persisting so that in-flight edits are committed rather than discarded.

**Files**
- [Datalib/src/main/java/com/ing/datalib/or/web/WebORObject.java](Datalib/src/main/java/com/ing/datalib/or/web/WebORObject.java)
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/web/WebORTable.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/web/WebORTable.java)

**Breaking changes**: none.

---

## 8. assertURLmatches — Pattern Compile Error

**Symptom**
`assertURLmatches` failed at runtime even when the URL matched the supplied pattern, because the data was being wrapped in `Pattern.compile()` and passed as a regex instead of a plain string URL.

**Root cause**
In `Assertions.assertURLmatches()`, the call was:
```java
assertThat(Page).hasURL(Pattern.compile(Data), options);
```
Playwright's `hasURL(Pattern, options)` treats the argument as a regex. When a literal URL string (e.g. `https://example.com/page`) was passed through `Pattern.compile()`, special characters in the URL were misinterpreted, causing false assertion failures.

**Fix**
Changed to pass `Data` directly as a string:
```java
assertThat(Page).hasURL(Data, options);
```

**Files**
- [Engine/src/main/java/com/ing/engine/commands/browser/Assertions.java](Engine/src/main/java/com/ing/engine/commands/browser/Assertions.java)

**Breaking changes**: none. Users who previously relied on regex matching via this action should use a dedicated regex-match assertion instead.

---

## 9. Test Datasheet Renaming / Global Datasheet Addition Crash

**Symptom**
Two distinct user actions threw a `NullPointerException`:
1. Renaming a test datasheet tab.
2. Adding or editing a cell in the Global Data sheet.

Both crashed with an NPE on `frozenScrollPane.getFixedTable().getCellEditor()`.

**Root cause**
`stopCellEditing()` in `TestDataComponent` always dereferenced `frozenScrollPane` without a null guard. The Global Data tab does not use a frozen-column scroll pane, so `frozenScrollPane` was `null` when either of the above operations triggered `stopCellEditing()`.

**Fix**
Added a null guard:
```java
if (frozenScrollPane != null && frozenScrollPane.getFixedTable().getCellEditor() != null) {
    frozenScrollPane.getFixedTable().getCellEditor().stopCellEditing();
}
```

**Files**
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testdata/TestDataComponent.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testdata/TestDataComponent.java)

**Breaking changes**: none.

---

## 10. Manage Devices — Accordion Scroll Fix

**Symptom**
In the **Manage Devices** tab, scrolling with the mouse wheel while the pointer was over one of the capability tables did not scroll the outer accordion panel — the scroll event was consumed by the inner table's scroll pane and had no visible effect.

**Root cause**
Each capability sub-section in `LambdaTestCapsPanel` placed its table inside its own `JScrollPane`. Swing routes wheel events to the component under the pointer, so the inner (never-scrolling) scroll pane swallowed all wheel events.

**Fix**
Added `forwardWheelToEnclosingScrollPane(JScrollPane inner)`: wheel scrolling is disabled on inner scroll panes and the events are re-dispatched to the first ancestor `JScrollPane`, which is the outer accordion scroller.

**Files**
- [IDE/src/main/java/com/ing/ide/main/settings/devices/LambdaTestCapsPanel.java](IDE/src/main/java/com/ing/ide/main/settings/devices/LambdaTestCapsPanel.java)

**Breaking changes**: none.

---

## 11. Reusable Scenario Creation Process

**Symptom**
When converting selected test steps into a reusable via right-click → **Make as Reusable**, the dialog only asked for a reusable name. There was no way to choose or create a target Reusable Components scenario, so all reusables were created in the default scenario regardless of the project structure.

**Fix**
- Introduced `ReusableComponentDialog` — a new modal dialog that asks for both a **Reusable Scenario Name** (drop-down of existing reusable scenarios, editable to create a new one) and a **Reusable Name**.
- `ScenarioComponent` and `TestCaseComponent` were updated to use the new dialog instead of the bare `JOptionPane.showInputDialog` call.
- If the chosen scenario does not yet exist, it is created automatically via `Project.addReusableScenario()`.
- `TestCase.save()` is called after the reusable is created to flush the parent test case changes to disk immediately.
- Focus is placed on the name field when existing scenarios are present, or on the scenario field when the list is empty.

**Files**
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/ReusableComponentDialog.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/ReusableComponentDialog.java) *(new)*
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/scenario/ScenarioComponent.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/scenario/ScenarioComponent.java)
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseComponent.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseComponent.java)

**Breaking changes**: none.

---

## 12. Object Repository — UX Improvements

### 12a. Page Object Count Pill

**What changed**  
Each page node in the Object Repository tree now displays a styled pill badge showing the total number of objects it contains.

**Details**
- The pill is painted via a `paintComponent` override on the `TreeSelectionRenderer` subclass inside `ObjectTree`.
- The count reflects all `ORObjectInf` items across all `ObjectGroup`s in the page.
- The pill is right-aligned to the full tree width so all page badges form a clean vertical column regardless of page name length.
- Visual: fully rounded capsule shape (`fillRoundRect` with arc equal to height), `#F1E9FF` lavender background, bold black text at 9.5 pt.
- The pill updates automatically on every tree reload (paste, sort, delete, etc.).

**Files**
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/ObjectTree.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/ObjectTree.java)

---

### 12b. Page Stays Expanded After Copy/Cut-Paste

**Symptom**  
After pasting an object into a page, the entire Object Repository tree collapsed, forcing the user to re-expand the target page manually.

**Root cause**  
`DefaultTreeModel.reload()` resets the expansion state of all nodes. The `reload()` helper in `ObjectTree` called it unconditionally without preserving state.

**Fix**
- `reload()` now saves the `TreePath` of every currently expanded `ORPageInf` node before calling `DefaultTreeModel.reload()`, then re-expands them immediately afterwards.
- For same-OR paste operations (Web, Mobile, SAP, StructuredData), `pastedObject` tracking was extended to all OR types and `selectAndSrollTo()` is invoked after reload so the newly pasted object is selected and scrolled into view.

**Files**
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/ObjectTree.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/ObjectTree.java)

---

### 12c. Auto-Commit Cell Edit on Focus Loss

**Symptom**  
While editing an attribute value in the OR properties table, clicking anywhere else in the IDE discarded the partially typed value instead of saving it.

**Root cause**  
Switching the tree selection triggered `loadObject()` on each OR table, which called `table.setModel(...)` and destroyed the active cell editor without committing its value. The `terminateEditOnFocusLost` client property handled focus changes *within* the table but not a full model replacement.

**Fix**  
Each OR table's `loadObject()` method now calls `table.getCellEditor().stopCellEditing()` before replacing the model, committing any in-flight edit to the underlying data model.

**Files**
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/web/WebORTable.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/web/WebORTable.java)
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/mobile/MobileORTable.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/mobile/MobileORTable.java)
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/sap/SapORTable.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/sap/SapORTable.java)
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/structureddata/StructuredDataORTable.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/structureddata/StructuredDataORTable.java)

**Breaking changes**: none.

---

## 13. Detailed Report — Cross-Iteration Reusable Expand Fix

**Symptom**
In the detailed test-case report (`detailed-v2.html`), clicking to expand a reusable's steps under Iteration 2 expanded the matching reusable under Iteration 1 instead.

**Root cause**
`renderStepsV2()` in `detailed.js` is invoked once per iteration and built each step/reusable `keyPath` starting from index `0`. As a result, a reusable at position 0 in Iteration 2 received the same `data-key` / `data-reusable-body="0"` as the one in Iteration 1. `toggleReusableV2()` resolves the target via `document.querySelector('[data-reusable-body="0"]')`, which returns the first DOM match — always the Iteration 1 reusable.

**Fix**
- Added a `keyPrefix` parameter to `renderStepsV2()` and pass the iteration index (`'iter' + idx`) from `injectStepsV2()`, so each iteration's steps and reusables receive unique keys (e.g. `iter0-...`, `iter1-...`).

**Files**
- [Resources/Configuration/ReportTemplate/media/js/detailed.js](Resources/Configuration/ReportTemplate/media/js/detailed.js)

**Breaking changes**: none.
