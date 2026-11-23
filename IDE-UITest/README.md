# IDE-UITest

Automated UI tests for the INGenious IDE. Each test launches the IDE, acts like a user (menus,
buttons, dialogs), checks what is shown, collects evidence (boxed screenshots and/or a screen
recording), writes a report, optionally publishes the result to the RTM, and quits.

Test cases come from the RTM (`p33148-ingenious-requirement-traceability-matrix`) and use its codes,
e.g. scenario `GEN-SC001` and test case `GEN-SC001-TC002`.

New to the tools used here (TestNG, AssertJ, AssertJ Swing, TestFX, ...)? Start with the
[learning plan](LEARNING-PLAN.md).

**Contents**

1. [Quick start](#1-quick-start)
2. [Command-line parameters](#2-command-line-parameters)
3. [How the module works](#3-how-the-module-works)
4. [Writing a test, step by step](#4-writing-a-test-step-by-step)
5. [Screenshots and recordings](#5-screenshots-and-recordings)
6. [Best practices](#6-best-practices)
7. [Results and reports](#7-results-and-reports)
8. [Publishing to the RTM](#8-publishing-to-the-rtm)
9. [Tests in this module](#9-tests-in-this-module)
10. [Troubleshooting](#10-troubleshooting)
11. [Limitations](#11-limitations)

---

## 1. Quick start

**You need:** JDK 17–25 for the test JVM, Maven, a display (both modes open real IDE windows), and
for robot mode on macOS the **Accessibility** and **Screen Recording** permissions for the app that
starts Maven (VS Code, Terminal, iTerm; System Settings > Privacy & Security, then restart the app).

Run from the repository root. The module is only built with the `uitest` profile, so a normal
`mvn install` is unaffected.

```bash
# Everything, direct mode (silent: your mouse and keyboard stay free)
mvn -Puitest -pl IDE-UITest -am verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false \
    -Duitest.jvm=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java

# Smoke suite, robot mode (real mouse/keyboard - don't touch them), screenshots + recording,
# published to an RTM testing cycle
mvn -Puitest -pl IDE-UITest -am verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false \
    -Duitest.jvm=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java \
    -Duitest.suite=smoke -Duitest.input=robot -Duitest.evidence=both \
    -Duitest.report=html,rtm -Duitest.rtm.cycle="releasev4.1"

# Dry run: show which test cases a selection would run, start nothing
mvn -Puitest -pl IDE-UITest -am verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false \
    -Duitest.jvm=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java \
    -Duitest.list=true -Duitest.scenarios=GEN-SC003
```

Results: `ui-test-output/<run id>/report.html` in the repository root (see [7](#7-results-and-reports)).

On Linux, wrap the command in `xvfb-run -a -s "-screen 0 1920x1080x24"` to run on a virtual display.

---

## 2. Command-line parameters

### Maven flags

| Flag | Why |
|---|---|
| `-Puitest` | Activates the profile that adds this module to the build. |
| `-pl IDE-UITest` | Builds and runs only this module (plus `-am`). |
| `-am` | Also builds the modules it depends on (IDE, Engine, Datalib, ...) from source, so the tests run against your current code. Leave it out to reuse the jars in `~/.m2` (faster, but possibly stale). |
| `-Dtest=none -Dsurefire.failIfNoSpecifiedTests=false` | Skips the unit tests of the upstream modules. The UI tests run in the `verify` phase (failsafe), not as unit tests. |
| `-o` | Offline: don't check remote repositories. Speeds things up when nothing needs downloading. |
| `-Dit.test=WindowMenuIT` | Failsafe's own filter by Java class/method. Prefer the RTM-code selection below. |

### How the tests run

| Property | Default | Values and effect |
|---|---|---|
| `uitest.input` | `direct` | `direct`: calls the UI controls in code (`doClick`, `fire`, `setText`). Never touches your mouse or keyboard, needs no OS permissions, fast. `robot`: real OS mouse and keyboard, exactly like a user. More realistic; use on CI or a virtual display. |
| `uitest.jvm` | Maven's JVM | Java executable that runs the IDE. Must be JDK 17–25 (AssertJ Swing needs `java.applet`, removed in JDK 26). Only needed when Maven itself runs on JDK 26+. |

### Which tests run

RTM codes, comma-separated, `*` and `?` wildcards, case-insensitive. Without any of these,
everything runs. A test case runs if it matches any of `tests`, `scenarios` or `suite`, and no
`exclude`.

| Property | Default | Example |
|---|---|---|
| `uitest.tests` | (none) | `-Duitest.tests=GEN-SC003-TC003,GEN-SC001-TC00*` |
| `uitest.scenarios` | (none) | `-Duitest.scenarios=GEN-SC001,UI-SC006` (all test cases, across test classes) |
| `uitest.suite` | (none) | `-Duitest.suite=smoke` (files in `src/test/resources/suites/<name>.txt`) |
| `uitest.exclude` | (none) | `-Duitest.exclude=GEN-SC001-TC005` (applied last) |
| `uitest.list` | `false` | `true`: print what would run and start nothing |

- A code that matches nothing, or an unknown suite, **fails the run before any IDE starts** and
  lists what exists.
- Test cases that are not selected are **not reported at all** (not as skipped), so the HTML report
  and the RTM cycle only contain what ran.
- A selected test case's `dependsOnMethods` run too.

Suite files have one entry per line:

```text
# comment
UI-SC006-TC001        # a test case
GEN-SC003             # a whole scenario (patterns work too: GEN-SC003-*)
!GEN-SC003-TC005      # exclude
@smoke                # include another suite
```

Shipped suites: `smoke` (launch, create/open project, Window menu navigation, quit) and
`regression` (everything).

### Evidence and reports

| Property | Default | Values and effect |
|---|---|---|
| `uitest.evidence` | `screenshot` | `screenshot`: a PNG per step and per check. `recording`: one `recording.mp4` per test case instead. `both`. A failure screenshot is always saved. See [5](#5-screenshots-and-recordings). |
| `uitest.report` | `html` | `html`, `rtm` or `html,rtm`. Unknown formats fail the run. |
| `uitest.outputDir` | `<repo>/ui-test-output` | Where run folders are written. |
| `uitest.runId` | build timestamp (UTC) | Name of the run folder. All test JVMs of one `mvn` call share it. |

### RTM publishing (`uitest.report` contains `rtm`)

| Property | Default | Values and effect |
|---|---|---|
| `uitest.rtm.cycle` | (required) | Testing cycle: the folder under the RTM's `cycles/`, created if new. |
| `uitest.rtm.dir` | `../p33148-ingenious-requirement-traceability-matrix` (next to this repo) | RTM repository root. |
| `uitest.rtm.role` | `dev-test` | `dev-test` fills the Dev result columns and evidence folder; `qa` the QA ones. |
| `uitest.rtm.tester` | `INGenious UI Test (automated)` | Tester name recorded with each result. |
| `uitest.rtm.python` | `python3` | Python 3 with PyYAML (e.g. `python` on Windows). |
| `uitest.rtm.generateReport` | `true` | Regenerate the RTM's `reports/evidence-report.html`/`.md` for the cycle afterwards. |

---

## 3. How the module works

```text
mvn verify -Puitest
 └─ failsafe: one JVM per test class (*IT), working directory target/uitest-home
     │        (a test copy of an INGenious installation: Resources/ + the engine jar)
     ├─ TestSelectionInterceptor   keeps only the selected test methods (select/)
     ├─ IngeniousUiTest @BeforeClass
     │     resets uitest-home, prepares projects, launches the IDE in this JVM (Main.main)
     ├─ test methods               act through page objects -> UiDriver (direct | robot)
     │                             collect evidence through EvidenceCollector
     └─ UiTestReportListener       stores each result, rewrites report.html / publishes to the RTM
```

| Package | Contents |
|---|---|
| `uitest` | Base classes `IngeniousUiTest` (startup dialog) and `ProjectOpenUiTest` (project already open), `CreateProjectIT` |
| `uitest.gen` | Tests for the GEN scenarios |
| `uitest.pages` | Page objects: `MainWindowPage`, `MenuPage`, `StartupDialogPage`, `NewProjectDialogPage`, `OpenProjectDialogPage`, `QuitDialogPage`, `View` |
| `uitest.support` | Launching (`IngeniousApp`, `UiTestHome`), input (`UiDriver` and its robot/direct/observed implementations), evidence (`EvidenceCollector`, `ScreenCapture`, `ScreenRecorder`, `Highlight`), waits (`Waits`, `FxNodes`, `Timeouts`) |
| `uitest.report` | `@Scenario`, `@TestCase`, `StepRef`, results, HTML and RTM report writers |
| `uitest.select` | Test selection (`-Duitest.tests`, suites, ...) |

The IDE is Swing with embedded JavaFX (`JFXPanel`): the menu bar, toolbar and startup dialog are
JavaFX; the main window, dialogs and trees are Swing. Swing is found and driven with **AssertJ
Swing**, JavaFX with **TestFX**, both behind `UiDriver` so tests never care which is which.

---

## 4. Writing a test, step by step

GitHub Copilot users can ask for this directly ("create tests for GEN-SC004"): the
[`rtm-to-ui-test`](../.github/skills/rtm-to-ui-test/SKILL.md) skill follows the same procedure.
Below is the manual way, using a hypothetical "Help > About" test case as the example.

### Step 1: Get the RTM test case

Every test case must exist in the RTM, because its code is what reports and the RTM link to. Open
`scenarios/<SCENARIO>.yaml` in the RTM repository and read the test case's `steps` and `asserts`.
Note the **printed step numbers** of the `[ASSERT]` steps: you will link evidence to them.

```yaml
- testCaseCode: GEN-SC004-TC001
  testCaseDescription: About dialog shows the INGenious version
  steps: |-
    1. Click on the "Help" menu.
    2. [ASSERT] that the Help menu dropdown is displayed.
    3. Click on "About".
    4. [ASSERT] that the About dialog shows the INGenious version.
```

If it does not exist yet, create it with the RTM's tool (never edit the YAML by hand), from the RTM
repository root:

```bash
python3 scripts/validation/manage_test_case.py add-test-case --scenario-code GEN-SC004 \
    --application-component "Application Toolbar" --feature "Help menu" \
    --scenario-description "Use the Help menu options" \
    --test-case-description "About dialog shows the INGenious version" --execution-mode Automated \
    --automated-test-case-name "IDE-UITest: com.ing.ide.uitest.gen.HelpMenuIT#aboutShowsVersion" \
    --steps $'1. Click on the "Help" menu.\n2. [ASSERT] ...\n3. Click on "About".\n4. [ASSERT] ...' \
    --asserts "About dialog shows the version" --dry-run      # then again without --dry-run
```

Keep `steps` clean: numbered steps only (`1.`, `2.`, ...). Put preconditions in
`setupPreconditions`, not in `steps`.

### Step 2: Find the real UI in the IDE code

Before writing the test, find out what the user actually clicks and what happens:

| Need | Where to look |
|---|---|
| Menu items, labels, shortcuts | `IDE/.../main/fx/FXMenuBar.java` |
| Toolbar buttons (tooltips) | `IDE/.../main/fx/FXToolBar.java` |
| What a menu/toolbar command does | `AppActionListener.actionPerformed` -> `AppMainFrame` |
| Views, side view selector | `AppMainFrame.show*()`, `SlideShow`, `SimpleDock` |
| Dialogs, notifications | e.g. `NewProject`, `QuitConfirmationDialog`, `AboutUI`, `Notification` |

Labels in the RTM can be outdated (e.g. "API Tester" is now "API Workbench"): test the real label
and note the difference.

### Step 3: Create the test class

One class per scenario, one method per test case, named `*IT`. Choose the base class by the test
case's starting point:

| Starting point | Base class |
|---|---|
| Startup dialog (no project open) | `IngeniousUiTest` |
| Main window with a project open ("user is on the main application screen") | `ProjectOpenUiTest` (a copy of the Tutorial project is opened; `otherProject` is a second copy) |

```java
package com.ing.ide.uitest.gen;

import static org.assertj.core.api.Assertions.assertThat;

import com.ing.ide.uitest.ProjectOpenUiTest;
import com.ing.ide.uitest.pages.MainWindowPage;
import com.ing.ide.uitest.pages.MenuPage;
import com.ing.ide.uitest.report.Scenario;
import com.ing.ide.uitest.report.StepRef;
import com.ing.ide.uitest.report.TestCase;
import org.testng.annotations.Test;

/**
 * GEN-SC004 "Use the Help menu options". Starts with a project open.
 * StepRef numbers are the printed step numbers of the RTM test cases.
 */
@Scenario(code = "GEN-SC004", description = "Use the Help menu options")
public class HelpMenuIT extends ProjectOpenUiTest {

    @Test(priority = 1)
    @TestCase(code = "GEN-SC004-TC001", description = "About dialog shows the INGenious version")
    public void aboutShowsVersion() {
        final MainWindowPage main = mainWindow();

        final MenuPage help = main.menu("Help").open();                       // step 1
        evidence.verify(                                                       // step 2 [ASSERT]
            "help-menu-open",
            StepRef.step(2),
            help.dropdown("Help menu dropdown is displayed"),
            () -> assertThat(help.isOpen()).as("Help menu open").isTrue()
        );

        help.choose("About");                                                  // step 3
        final AboutDialogPage about = AboutDialogPage.waitFor(app.swing(), app.driver());
        evidence.verify(                                                       // step 4 [ASSERT]
            "about-shows-version",
            StepRef.step(4),
            about.versionLabel("About shows version " + System.getProperty("ingenious.expectedVersion")),
            () -> assertThat(about.version()).isEqualTo(System.getProperty("ingenious.expectedVersion"))
        );
    }
}
```

Rules for the class:

- Copy `code` and `description` exactly from the RTM.
- Test cases in a class share one IDE and run in `priority` order, but must **not depend on each
  other**: any one can be selected alone. If one really must, declare `dependsOnMethods`.
- A test case that ends the IDE (Quit) goes in its own class with the same `@Scenario`
  (`FileMenuQuitIT` next to `FileMenuIT`), because it ends that class's IDE.
- A test case that cannot be automated throws `new SkipException("Not automated: <reason>")`; it is
  reported as **Blocked** in the RTM.

### Step 4: Add page objects and driver actions when needed

Tests only call page objects; page objects find things and send input through `UiDriver`
(`app.driver()`), so every action works in both input modes and appears in the recording.

A new dialog gets a page object in `pages/`, modelled on `QuitDialogPage`:

```java
public final class AboutDialogPage {
    private final DialogFixture dialog;
    private final UiDriver driver;

    public static AboutDialogPage waitFor(final Robot robot, final UiDriver driver) {
        final DialogFixture dialog = WindowFinder
            .findDialog(new GenericTypeMatcher<>(AboutUI.class) {
                @Override
                protected boolean isMatching(final AboutUI d) {
                    return d.isShowing();
                }
            })
            .withTimeout(Timeouts.STEP.toMillis(), TimeUnit.MILLISECONDS)
            .using(robot);
        return new AboutDialogPage(dialog, driver);
    }
    // version(), versionLabel(label) -> Highlight.component(<label component>, label), close() ...
}
```

A new kind of user action (e.g. drag and drop) is added to the `UiDriver` interface and implemented
in **all three** implementations: `RobotUiDriver` (real input), `DirectUiDriver` (calls the control
on its UI thread; must not block on modal dialogs) and `ObservedUiDriver` (moves the pointer and
captions the recording).

Waiting and threads:

- Wait with `Waits.until("what", () -> condition)`; never `Thread.sleep`.
- Read Swing state with `Waits.swing(() -> ...)` and JavaFX state with `FxNodes.onFx(() -> ...)`.
- Never query JavaFX from the Swing event thread: embedded JavaFX can deadlock.

### Step 5: Run it alone, in both modes

```bash
BASE="mvn -o -Puitest -pl IDE-UITest -am verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false \
  -Duitest.jvm=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java"
$BASE -Duitest.list=true -Duitest.scenarios=GEN-SC004                 # selected correctly?
$BASE -Duitest.scenarios=GEN-SC004                                    # direct mode
$BASE -Duitest.scenarios=GEN-SC004 -Duitest.input=robot              # robot mode
$BASE -Duitest.scenarios=GEN-SC004 -Duitest.report=html,rtm \
      -Duitest.rtm.cycle=trial-gen-sc004 -Duitest.rtm.generateReport=false
```

### Step 6: Review the evidence, not only the result

1. Open `ui-test-output/<run>/report.html` and look at **every screenshot**, in both modes. A green
   result can hide a dropdown drawn behind a window or a box around the wrong element.
2. After the RTM trial run, check `ui-test-output/<run>/rtm-export.log` for `WARNING` lines (a
   `StepRef` that matches no RTM step), then delete `cycles/trial-gen-sc004` from the RTM repo.

### Step 7: Finish

- Add the test case to a suite in `src/test/resources/suites/` if it belongs in `smoke`.
- Add the class to the table in [9](#9-tests-in-this-module).

---

## 5. Screenshots and recordings

### Choose the kind of evidence

`-Duitest.evidence=screenshot|recording|both` (see [2](#evidence-and-reports)). Screenshots are
best for reviewing checks; a recording shows the whole flow, including every click.

### Choose when a screenshot is taken

Screenshots are only taken where the test asks for them, plus one on failure:

| Call | Screenshot | In a recording |
|---|---|---|
| `evidence.take("step-name")` | The screen as it is now | Caption "step-name" for 1.5 s |
| `evidence.take("step-name", StepRef.step(3))` | Same, linked to RTM step 3 | Same |
| `evidence.verify("check-name", StepRef.step(4), highlight, () -> assertThat(...))` | Runs the assertion, then the screen with the highlighted area boxed: green `PASSED: <label>` or red `FAILED: <label>` (file `NN-check-name-FAILED.png`); a failed assertion is rethrown | Freezes 2 s on the boxed frame |
| (automatic) test case fails | `NN-FAILED-<method>.png` | Freezes on a red caption |
| (automatic) each click/typing/close | — | Pointer glides to the target, 0.5 s freeze with click ripple and caption, e.g. `Click "About"` |

Files are numbered in capture order: `<run>/<scenario>/<test case>/NN-<name>.png`. Use short,
kebab-case names that say what is shown (`help-menu-open`, not `screenshot1`).

### Choose what is boxed

The `Highlight` passed to `verify` decides the box and its caption. Make the caption state the
expectation ("Help menu dropdown is displayed"), because it is what reviewers read.

| Highlight | Boxes |
|---|---|
| `main.titleBar(label)` | The window's title bar |
| `main.viewContent(View.X, label)` | A whole view (Test Design, Dashboard, ...) |
| `menu.dropdown(label)` / `menu.itemHighlight(item, label)` | An open JavaFX menu / one item of it |
| `dialogPage.highlight(label)` | A whole dialog |
| `Highlight.component(swingComponent, label)` | Any Swing component |
| `Highlight.fxNode(node, label)` | Any JavaFX node (e.g. a toolbar button) |
| `Highlight.onScreen(rectangle, label)` | Any screen area |
| `Highlight.captionOnly(label)` | Nothing; caption only, for checks with nothing on screen (files on disk, exit requested) |

### Link screenshots to RTM steps

| StepRef | Links to |
|---|---|
| `StepRef.step(n)` | The RTM step printed with number `n` (`4.` / `4)`), including `[ASSERT]` steps |
| `StepRef.assertion("text")` | The RTM `asserts` line containing the text (case-insensitive) |
| (none) | The test case only |

The RTM report shows these as "→ Step n" / "→ Assert n". Steps are matched by their printed number:
lines that don't start with a number continue the step above, and extra or invisible whitespace is
ignored. A reference that matches no RTM line is published without a link and logged as a warning.

### What is captured

| | Direct mode | Robot mode |
|---|---|---|
| Area | Only the IDE's windows (and JavaFX popups such as menus), painted into an image | The whole primary display |
| Title bar | Drawn from the window title (Swing does not paint native title bars) | The real one |
| Mouse pointer | Drawn at the last action's position | Drawn at the real cursor position |
| Permissions | None | macOS Screen Recording |

The pointer is always drawn by the tests: OS screen captures never include the cursor, and direct
mode has no mouse at all.

### Tune evidence (code constants)

| Setting | Where | Default |
|---|---|---|
| Caption time after `take` | `EvidenceCollector.STEP_CAPTION_MS` | 1500 ms |
| Freeze on a `verify` frame | `EvidenceCollector.CHECK_HOLD_MS` | 2000 ms |
| Freeze on each click | `EvidenceCollector.ACTION_HOLD_MS` | 500 ms |
| Video frame rate | `ScreenRecorder.FPS` | 12 |
| Background captures between actions | `ScreenRecorder.LIVE_CAPTURES_PER_SECOND` | 4 per second |
| Maximum video width | `ScreenRecorder.MAX_WIDTH` | 1024 px |
| Pointer glide steps | `ObservedUiDriver.GLIDE_STEPS` | 8 |
| Box padding / line width | `ScreenCapture.BOX_PADDING` / `BOX_STROKE` | 3 px / 3 px |
| Wait for IDE start / each step | `Timeouts.LAUNCH` / `Timeouts.STEP` | 120 s / 60 s |

Recordings are encoded with jcodec (pure-Java H.264, no ffmpeg needed). Higher frame rates or
widths slow the test down, because capturing and encoding happen in the test JVM.

---

## 6. Best practices

**Structure**

- Use the RTM codes and descriptions exactly. One class per scenario, one method per test case.
- Keep test cases independent so any one can run alone (`-Duitest.tests=<code>`).
- Set preconditions up quickly through the IDE's API (`main.arrangeView(View.TEST_EXECUTION)`), but
  perform **the step under test through the UI**.
- Give test projects names from `newProjectName(...)` (prefix `UITest`), so the next run deletes them.

**Interaction**

- Tests call page objects; page objects send input through `UiDriver`. Never use `app.swing()` /
  `app.fx()` to click or type: those bypass direct mode and the recording. Use them only to *find*
  things.
- Find components by stable properties: menu text, tooltip, component class, and ideally
  `setName(...)` / `setId(...)` added to the IDE control. Avoid screen positions and indexes.
- Wait for conditions (`Waits.until`, `WindowFinder...withTimeout`), never `Thread.sleep`.

**Checks and evidence**

- Every RTM `[ASSERT]` step becomes one `evidence.verify(...)` with `StepRef.step(n)` and a
  highlight around the thing being checked.
- Assertions use AssertJ with `.as("what")`, so failures read well: `assertThat(main.title()).as("title bar").startsWith(name)`.
- Check what the RTM asks for; if the harness can't check it literally (e.g. "no process remains"),
  check the closest equivalent and say so in the Javadoc.
- Review screenshots in both input modes before calling a test done.

**Code**

- Java 17, 4-space indent, no wildcard imports, Javadoc on public classes and methods.
- No records or text blocks: the build's prettier-java 0.7 cannot parse them. Prettier reformats
  files on every build.
- Keep `README.md` (tests table) and suites up to date.

**Running**

- Use direct mode while developing; run robot mode before merging and on CI.
- In robot mode don't touch the mouse or keyboard, switch desktops or (dis)connect displays.

---

## 7. Results and reports

Each `mvn verify` creates one run folder (git-ignored):

```text
ui-test-output/<yyyyMMdd-HHmmss, UTC>/
├── report.html                      # with -Duitest.report=html (default)
├── rtm-export.log                   # with -Duitest.report=rtm
├── results/<scenario>__<case>.json  # raw results, used to build the reports
└── <scenario code>/<test case code>/
    ├── NN-<step>.png                # uitest.evidence=screenshot|both
    └── recording.mp4                # uitest.evidence=recording|both
```

The HTML report shows each scenario (code, description, result) with its test cases (code,
description, result, duration, failure details) and their evidence, with "→ Step n" labels. The MP4
plays in Chrome, Edge, Safari and Firefox (not in VS Code's built-in browser).

Add a report format by implementing `ReportWriter` and registering it in `ReportFormats`.

---

## 8. Publishing to the RTM

With `-Duitest.report=rtm` (or `html,rtm`) and `-Duitest.rtm.cycle=<cycle>`, results and evidence are
published to the RTM testing cycle, next to manual results. Only the RTM's own scripts are used
(`results.yaml` is never written directly):

1. `save_evidence.py` copies the recording and screenshots to `cycles/<cycle>/<role>/<code>-<n>.<ext>`.
2. `record_test_result.py --batch-file` records the result (PASSED → `Pass`, FAILED → `Fail`,
   SKIPPED → `Blocked`), tester, date, notes (run id, version, input mode, OS, first line of a
   failure) and one evidence item per file with its caption and step/assert link.
3. `generate_evidence_report.py --cycle` regenerates the RTM report (unless
   `-Duitest.rtm.generateReport=false`).

- Script output goes to `ui-test-output/<run>/rtm-export.log`.
- Re-running the same cycle adds new evidence files next to the earlier ones and updates the result.
- A missing cycle or a wrong `uitest.rtm.dir` fails the run before the IDE starts; a failing RTM
  script is logged but does not fail the test.
- If a code appears twice in the RTM, the first one is used.
- Skipped test cases publish no evidence.

---

## 9. Tests in this module

| Class | Scenario | Test cases | Starts with |
|---|---|---|---|
| `CreateProjectIT` | UI-SC006 | TC001 | Startup dialog |
| `gen.FileMenuIT` | GEN-SC001 File menu | TC001 New, TC002 Open, TC003 Save, TC004 Restart (skipped → `Blocked`) | Project open |
| `gen.FileMenuQuitIT` | GEN-SC001 File menu | TC005 Quit (own JVM: it ends the IDE) | Project open |
| `gen.ToolbarIT` | GEN-SC002 toolbar | TC001 New, TC002 Open | Project open |
| `gen.WindowMenuIT` | GEN-SC003 Window menu | TC001–TC004 navigation, TC005 AdjustUI, TC006 menu closes, TC007 side view selector | Project open |

Notes:

- **GEN-SC001-TC004 Restart** is skipped: Restart launches INGenious as a new OS process
  (`ingenious.command` / `ingenious.bat`), outside the test's control.
- **GEN-SC002-TC002** ("Open Project Button from Menu Bar") is listed in the RTM as a duplicate
  `GEN-SC001-TC002`; the test publishes it as GEN-SC002-TC002, whose step links stay unresolved
  until the RTM is corrected.
- **GEN-SC003-TC004** says "API Tester"; the menu item is now "API Workbench".
- **Quit**: the IDE's `System.exit` is turned into a dispose, so "no process remains" is checked as
  "INGenious requested exit and no window is left".
- **Save**: checks that saving rewrote the project's `.project` file and showed the "Saved"
  notification (no unsaved changes are made first).
- `ProjectOpenUiTest` copies the Tutorial project twice (`UITest-<Class>-<time>-Main` / `-Other`),
  auto-opens the first, and in robot mode clicks into the window before the first step. Test
  projects (`Projects/UITest*`) are deleted at the start of the next run.

---

## 10. Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `UI tests need JDK 17-25 ...` / `NoClassDefFoundError: java/applet/Applet` | Maven runs on JDK 26+. Pass `-Duitest.jvm=<JDK 17–25>/bin/java`. |
| `... is not a prepared INGenious installation` | The test was started outside Maven (e.g. from the IDE's "Run test"). Run it with Maven so `target/uitest-home` is prepared. |
| `No UI test case matches ...` / `Unknown suite ...` | A typo in the selection; the message lists what exists. Use `-Duitest.list=true`. |
| `Report format 'rtm' needs -Duitest.rtm.cycle` | Add `-Duitest.rtm.cycle=<name>`. |
| Robot mode: clicks ignored, screenshots show only the wallpaper | macOS Accessibility / Screen Recording permission missing for the app that starts Maven; grant it and restart that app. |
| Robot mode: clicks land in the wrong place | The screen changed during the run (mouse used, desktop switched, display (dis)connected). Re-run without touching anything. |
| Robot mode: a dropdown is drawn behind the main window | The app wasn't frontmost. Call `MainWindowPage.activate()` before the first step (done by `ProjectOpenUiTest`). |
| Test hangs, then times out after 60 s | A wait whose condition never became true, often a dialog that didn't open or a JavaFX query from the Swing thread. Check the `FAILED-*` screenshot. |
| `WARNING ... published without a link` in `rtm-export.log` | The `StepRef` number or text doesn't exist in the RTM test case, or the code isn't in the RTM. |
| Build fails with "parsing errors ... found: 'record'" | Java records/text blocks are not supported by the build's prettier; use a plain class. |

---

## 11. Limitations

**Platform and tooling**

- JDK 17–25 only: AssertJ Swing references `java.applet`, removed in JDK 26, and has not been
  released since 2020. Replacing it with a small in-house Swing helper would lift this limit.
- Not truly headless: Swing cannot create windows with `java.awt.headless=true`, and TestFX's
  headless Monocle platform does not work for JavaFX embedded in Swing. Use a real or virtual display.
- Linux and Windows are not wired up yet: the JavaFX jars come from the IDE module's macOS (aarch64)
  dependencies only, and there is no CI workflow yet.
- Plain `xvfb-run` has no window manager, so windows have no title bar: the title-bar check is
  captioned but not boxed. Start a lightweight window manager (e.g. `fluxbox`).
- macOS needs a logged-in session; robot mode needs the permissions in [1](#1-quick-start).

**Input modes**

- Robot mode takes over the mouse and keyboard, and tests cannot run in parallel on one display.
  Spread tests across CI jobs or Xvfb displays instead. It records the primary display only.
- Direct mode still opens the IDE windows, which may take focus. It skips real mouse, keyboard and
  focus events, so it does not catch problems such as a control hidden behind another one, and it
  cannot really click outside a menu (`clickOutside` hides the menu instead).

**Evidence and report**

- Between actions the screen is captured only 4 times a second, so quick UI transitions can be
  missed; pointer movement is captured at the full 12 fps.
- `MenuPage.isOpen()` uses JavaFX state, which can't tell whether a dropdown is hidden behind
  another window; robot-mode screenshots show what was really on screen.
- Web content inside JavaFX `WebView`s (AI chat, embedded browser) can't be inspected by TestFX.
- The `rtm` format needs Python 3 with PyYAML and the RTM repository checked out, with the
  `record_test_result.py` that supports evidence items in batch files.
- Run folder names use UTC.

**Test setup**

- Many IDE controls have no `setName`/`setId`; the startup dialog page uses style classes and prompt
  text, menus use their text. Adding stable names to key controls makes tests less brittle.
