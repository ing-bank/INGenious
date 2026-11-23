---
name: rtm-to-ui-test
description: 'Convert RTM test cases (p33148-ingenious-requirement-traceability-matrix scenarios/*.yaml) into automated IDE UI tests in the IDE-UITest module, with step-linked evidence published back to the RTM. Use when asked to automate, create tests for, or implement RTM scenarios/test cases (e.g. "create tests for GEN-SC001..SC003").'
argument-hint: 'RTM scenario or test case codes, e.g. GEN-SC004 or GEN-SC004-TC002'
---

# RTM test cases -> IDE UI tests

Turns manual RTM test cases into TestNG UI tests in `IDE-UITest/` that drive the INGenious IDE like
a user, capture boxed evidence per `[ASSERT]` step, and publish results to the RTM. Read
`IDE-UITest/README.md` first: it documents the harness, options and limitations.

## Ground rules

- **Never edit the RTM without asking.** Report problems you find (duplicate codes, outdated
  labels, header lines in `steps`) and let the user decide. If they agree, change scenario data only
  through `scripts/validation/manage_test_case.py` (run with `--dry-run` first, then
  `validate_scenarios.py`), never by hand. `results.yaml` is only written by the RTM's
  `record_test_result.py` (the `rtm` report writer does this).
- **The step under test always goes through the UI** (`UiDriver` via page objects). Preconditions
  may be set up through the IDE's API (e.g. `MainWindowPage.arrangeView(View.TEST_EXECUTION)`).
- **Every test must pass in both input modes** (`direct` and `robot`). Robot mode takes over the
  user's mouse: ask before running it.
- **No Java records or text blocks**: the build's prettier-java 0.7 cannot parse them. Prettier also
  reformats files on every build, so re-read a file before editing it again.

## Procedure

### 1. Read the RTM test cases

The RTM repo is a sibling of this one. Read `scenarios/<SCENARIO>.yaml` and note for each test case:
`steps` (printed numbers and `[ASSERT]` lines), `asserts`, `setupPreconditions`, `executionMode`.

Check for, and report to the user:
- **Duplicate `testCaseCode`s** (the RTM tooling only sees the first). Example: GEN-SC001 had two
  `GEN-SC001-TC002`; the user chose to publish the second as GEN-SC002-TC002 without editing the RTM.
- **Header lines inside `steps`** ("Test Case ID:", "Title:", "Preconditions:", bullets, "Steps:").
  They used to break step links; the user had GEN-SC001..003 cleaned (preconditions moved to
  `setupPreconditions`, steps renumbered from 1, step text unchanged).
- **Labels that no longer match the IDE** (GEN-SC003-TC004 says "API Tester"; the menu item is
  "API Workbench"). Test the real label and mention it in the test's comment and the README.

### 2. Map each step to IDE code

Find the real UI and its behaviour before writing anything:

| Need | Where to look |
|---|---|
| Menu items, labels, shortcuts | `IDE/.../main/fx/FXMenuBar.java` (JavaFX menu bar) |
| Toolbar buttons (found by tooltip) | `IDE/.../main/fx/FXToolBar.java` |
| What a command does | `AppActionListener.actionPerformed` switch -> `AppMainFrame` methods |
| Views and side view selector | `AppMainFrame.show*()`, `SlideShow`, `SimpleDock` (glass pane) |
| Dialogs | e.g. `NewProject`, `QuitConfirmationDialog`, `INGeniousFileChooser.OPEN_PROJECT` |
| Notifications | `Notification` -> `ToasterDialog` windows |

Decide per test case whether it can be automated in-process. Example: **Restart** launches a new OS
process (`ingenious.command` / `.bat`), so GEN-SC001-TC004 throws
`SkipException("Not automated: ...")`, which the RTM records as **Blocked**.

### 3. Structure the test classes

- One class per scenario: `@Scenario(code, description)`, extends `ProjectOpenUiTest` ("user is on
  the main application screen": a Tutorial copy is auto-opened; `otherProject` is a second copy) or
  `IngeniousUiTest` (startup dialog). Name classes `*IT`; GEN tests live in `uitest/gen/`.
- One method per test case: `@Test(priority = n)` + `@TestCase(code, description)`, copied from the RTM.
- Test cases in a class share one IDE and run in priority order, but **must not depend on each
  other** (any one can be selected alone with `-Duitest.tests`). If one must, use `dependsOnMethods`.
- A test case that ends the IDE (Quit) gets its **own class** (own JVM), with the same `@Scenario`:
  `FileMenuIT` + `FileMenuQuitIT` are both GEN-SC001.
- Test projects created during a test must be named with `newProjectName(...)` (prefix `UITest`) so
  the next run cleans them up.

### 4. Write the steps

Every RTM `[ASSERT]` step becomes an `evidence.verify(...)` linked with `StepRef.step(<printed
number>)`; plain action steps go through page objects. `StepRef.assertion("text")` links an
`asserts` line by its text when there is no matching step.

```java
// RTM GEN-SC001-TC002 steps: 1. Click File  2. [ASSERT] dropdown opens
// 3. [ASSERT] "Open Project" visible and enabled  4. Click it  5. [ASSERT] dialog shown ...
@Test(priority = 2)
@TestCase(code = "GEN-SC001-TC002", description = "Functioning Open Project Button from File menu")
public void openProjectFromFileMenu() {
    final MainWindowPage main = mainWindow();
    final MenuPage file = main.menu("File").open();
    evidence.verify("file-menu-open", StepRef.step(2), file.dropdown("File menu dropdown opens"),
        () -> assertThat(file.isOpen()).as("File menu open").isTrue());
    evidence.verify("open-project-option", StepRef.step(3),
        file.itemHighlight("Open Project", "\"Open Project\" is visible and enabled"),
        () -> assertThat(file.isVisibleAndEnabled("Open Project")).isTrue());

    file.choose("Open Project");
    final OpenProjectDialogPage chooser = OpenProjectDialogPage.waitFor(app.swing(), app.driver());
    evidence.verify("open-project-dialog", StepRef.step(5), chooser.highlight("Open Project dialog is displayed"), () -> {});
    ...
}
```

- **Highlights**: `titleBar`, `viewContent`, `dropdown`, `itemHighlight`, `Highlight.component(...)`,
  `Highlight.fxNode(...)`; `Highlight.captionOnly(...)` for checks with nothing on screen (files on
  disk, "no process remains").
- **"Appears" checks** (a dialog or popup is shown): wait for it via its page object, then
  `verify(..., () -> {})` so the screenshot is the evidence.
- **Preserve the RTM's intent when the harness limits it**, and say so in the class Javadoc. Example:
  the IDE runs in the test JVM, so Quit checks "IDE requested exit (`app.exitRequested()`) and no
  window remains" instead of "no process remains".

### 5. Extend page objects and the driver when needed

- New user actions go in `UiDriver` and must be implemented in **all three**: `RobotUiDriver` (real
  input), `DirectUiDriver` (calls controls on their UI thread, never blocking on modal dialogs), and
  `ObservedUiDriver` (pointer glide + caption for recordings).
- Waits: `Waits.until(...)` evaluates on the test thread. Read Swing state with `Waits.swing(...)` and
  JavaFX state with `FxNodes.onFx(...)`. **Never query JavaFX from the Swing event thread** (embedded
  JavaFX can deadlock).
- Existing helpers: `MainWindowPage` (`menu`, `toolbarButton`, `arrangeView`, `waitForView`,
  `waitForNotification`, `testDesignSplitPane`, `leftEdge`/`hover`/`viewSelectorButton`,
  `statusBar`, `projectLocation`, `activate`), `MenuPage`, `NewProjectDialogPage`,
  `OpenProjectDialogPage`, `QuitDialogPage`, `View`.

### 6. Verify

Run from the INGenious root (JDK 17-25 for the test JVM):

```bash
BASE="mvn -Puitest -pl IDE-UITest -am verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false \
  -Duitest.jvm=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java"
$BASE -Duitest.list=true -Duitest.scenarios=GEN-SC004          # selection is right, nothing starts
$BASE -Duitest.scenarios=GEN-SC004 -Duitest.evidence=both      # direct mode
$BASE -Duitest.scenarios=GEN-SC004 -Duitest.input=robot        # robot mode (ask the user first)
$BASE -Duitest.scenarios=GEN-SC004 -Duitest.report=html,rtm \
      -Duitest.rtm.cycle=trial-<name> -Duitest.rtm.generateReport=false   # check step links
```

Then:
1. **Look at the screenshots**, in both modes, not only the pass/fail result. A green check can hide
   a covered dropdown or a box around the wrong element.
2. Check `ui-test-output/<run>/rtm-export.log` for `WARNING` lines (step not found in the RTM).
3. Confirm every link lands on the intended line: for each published evidence item, compare its
   caption with the RTM step it points to.
4. **Delete the trial cycle** from the RTM repo (`cycles/trial-<name>`); don't touch the user's cycles.
5. Update `IDE-UITest/README.md` (Tests table and notes) and add the test cases to a suite in
   `src/test/resources/suites/` if the user wants them in `smoke`.

## Learnings

- **Step links use the printed step number.** `StepRef.step(4)` means the line printed `4.`/`4)`.
  The RTM report anchors steps by printed number (unnumbered lines continue the step above,
  whitespace and zero-width characters are ignored); the `rtm` writer checks the number exists.
  Before this, links used line positions, and evidence for printed step 9 showed as "Step 14".
- **macOS robot mode: activate the window first.** The first click into an app that isn't frontmost
  only brings it forward, and the first JavaFX dropdown then opens *behind* the main window while
  JavaFX still reports it as showing. `ProjectOpenUiTest` clicks the status bar first
  (`MainWindowPage.activate()`); do the same in any other setup. It was found from screenshots, not
  from assertions.
- **Direct mode cannot click outside a menu**: `clickOutside` hides it the way JavaFX auto-hide
  would. Robot mode does the real click, so test both.
- **JavaFX menu items have no reliable IDs**; `MenuPage` finds them by text and the menu-bar title
  via `.menu-bar .menu` lookups. Toolbar buttons are found by tooltip.
- **AdjustUI** resets `TestDesignUI`'s outer split pane to 0.70; resize it first (precondition), then
  check the proportion.
- **Save** rewrites the project's `.project` file: backdate its modification time before saving and
  compare after.
