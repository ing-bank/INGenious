# Learning plan: IDE UI tests

What a developer needs to learn to read, write and maintain the tests in `IDE-UITest`, in the order
that builds on itself. Each topic says **why** it matters here, **where** it is used in this module,
what to **learn**, and a **checkpoint** exercise on this repository.

Expected effort for a Java developer new to UI testing: about **2–3 weeks part-time**. Stages 1–3 are
needed to write tests; stage 4 to change the framework itself.

## Stack at a glance

| Tool | Version | Used for |
|---|---|---|
| Java | 17 (test JVM: JDK 17–25) | Everything |
| Swing + FlatLaf | JDK | The IDE's main window, dialogs, trees |
| JavaFX (embedded via `JFXPanel`) | 17.0.14 | The IDE's menu bar, toolbar, startup dialog |
| Maven + failsafe | failsafe 3.5.2 | Building the IDE, preparing `uitest-home`, running the tests |
| TestNG | 7.7.1 | Test structure, ordering, listeners, filtering |
| AssertJ | 3.27.7 | Assertions |
| AssertJ Swing | 3.17.1 | Finding and driving Swing windows and components |
| TestFX | 4.0.18 | Finding and driving JavaFX nodes |
| `java.awt.Robot` | JDK | Real mouse/keyboard input, screen capture |
| jcodec | 0.2.5 | Encoding screen recordings (H.264 MP4) |
| Jackson (JSON + YAML) | parent POM | Result files, reading RTM scenario YAML |
| Python 3 + PyYAML | — | The RTM repository's scripts |

---

## Stage 1: Foundations (≈ 3–4 days)

### 1.1 Swing and its event thread

- **Why:** the IDE is mostly Swing. Every UI read or change must happen on the Event Dispatch
  Thread (EDT); getting this wrong gives flaky tests or deadlocks.
- **Where:** `Waits.swing(...)`, `GuiActionRunner.execute(...)`, `DirectUiDriver`
  (`SwingUtilities.invokeLater`), `MainWindowPage`.
- **Learn:** component hierarchy (`Window` → `Container` → `Component`), `JFrame`/`JDialog`, modal
  dialogs, `SwingUtilities.invokeLater` vs `invokeAndWait`, `CardLayout`, `JSplitPane`, glass panes.
- **Read:** Oracle tutorial "Concurrency in Swing":
  https://docs.oracle.com/javase/tutorial/uiswing/concurrency/
- **Checkpoint:** in `IDE/.../AppMainFrame.java`, find how views are switched (`showTestDesign`,
  `SlideShow`) and why `DirectUiDriver.click` uses `invokeLater` instead of calling `doClick` directly
  (hint: modal dialogs).

### 1.2 JavaFX inside Swing

- **Why:** the menu bar, toolbar and startup dialog are JavaFX scenes inside Swing (`JFXPanel`).
  JavaFX has its own UI thread; mixing the two threads wrongly can deadlock.
- **Where:** `FxNodes.onFx(...)`, `MenuPage`, `StartupDialogPage`, `ScreenCapture.fxPopups()`.
- **Learn:** scene graph, CSS style classes and `lookup`/`lookupAll`, `Platform.runLater`,
  `Menu`/`MenuItem`/`ContextMenu` popups (separate windows), `JFXPanel` interop.
- **Read:** https://openjfx.io/ and "JavaFX and Swing interoperability":
  https://docs.oracle.com/javafx/2/swing/swing-fx-interoperability.htm
- **Checkpoint:** in `IDE/.../fx/FXMenuBar.java`, list the Window menu items and their keyboard
  shortcuts, then find where `MenuPage.shownShortcut` reads the shortcut text.

### 1.3 Maven for this module

- **Why:** the tests only run through Maven, which builds the IDE, prepares the test installation
  and passes the `-Duitest.*` settings.
- **Where:** root `pom.xml` (`uitest` profile), `IDE-UITest/pom.xml`.
- **Learn:** profiles, multi-module builds (`-pl`, `-am`), lifecycle phases (`pre-integration-test`,
  `integration-test`, `verify`), surefire vs **failsafe**, `forkCount`/`reuseForks`, `workingDirectory`,
  `systemPropertyVariables`.
- **Read:** https://maven.apache.org/guides/introduction/introduction-to-profiles.html,
  https://maven.apache.org/surefire/maven-failsafe-plugin/
- **Checkpoint:** run the dry run from README §1 and explain from `IDE-UITest/pom.xml` how
  `target/uitest-home` is built and why each test class gets its own JVM.

### 1.4 `java.awt.Robot`: what robot mode is built on

- **Why:** robot mode (`-Duitest.input=robot`) is real OS input. Underneath AssertJ Swing's
  `BasicRobot` and TestFX's `FxRobot` (with `-Dtestfx.robot=awt`) sits the JDK's `java.awt.Robot`,
  which moves the real pointer, presses real keys and reads screen pixels. Knowing it explains why
  robot mode needs OS permissions, a visible unlocked screen, and why another window on top makes
  a click land in the wrong place.
- **Where:**
  - Indirectly, for clicks and typing: `RobotUiDriver`, through AssertJ Swing and TestFX.
  - Directly, in only two places: `ScreenCapture` (`createScreenCapture`, `MouseInfo` for the
    pointer) and `ObservedUiDriver` (`mouseMove` for the smooth pointer glide).
  - Test classes and page objects never use it; they go through `UiDriver`.
- **Learn:**
  - Input: `mouseMove(x, y)` (screen coordinates), `mousePress`/`mouseRelease` with
    `InputEvent.BUTTON1_DOWN_MASK`, `keyPress`/`keyRelease` with `KeyEvent.VK_*`, `delay(ms)`,
    `setAutoDelay`, `waitForIdle()` (never call it on the EDT).
  - Screen: `createScreenCapture(Rectangle)`, `MouseInfo.getPointerInfo().getLocation()`,
    `Component.getLocationOnScreen()` to turn a control into screen coordinates.
  - Limits: `new Robot()` throws `AWTException` when there is no display (`GraphicsEnvironment.isHeadless()`);
    on macOS it needs Accessibility (input) and Screen Recording (capture) permission; on HiDPI
    screens captures are in logical pixels; the OS, not Java, decides which window gets the event.
  - Minimal example:

    ```java
    Robot robot = new Robot();
    Point p = button.getLocationOnScreen();   // read on the EDT
    robot.mouseMove(p.x + button.getWidth() / 2, p.y + button.getHeight() / 2);
    robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
    robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    BufferedImage shot = robot.createScreenCapture(new Rectangle(p, button.getSize()));
    ```

- **Read:** https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/java/awt/Robot.html
- **Checkpoint:** in `ScreenCapture`, find why screenshots use a plain `Robot.createScreenCapture`
  instead of AssertJ Swing's `ScreenshotTaker`, and in `ObservedUiDriver`, how the pointer glide
  splits one move into `GLIDE_STEPS` steps.

---

## Stage 2: Test tools (≈ 3–4 days)

### 2.1 TestNG

- **Why:** structures the tests and lets the module hook into the run.
- **Where:** `@Test(priority = n)` in every `*IT`, `@BeforeClass`/`@BeforeMethod` in
  `IngeniousUiTest`, `SkipException` (Restart), `@Listeners` with `UiTestReportListener`
  (`ITestListener`) and `TestSelectionInterceptor` (`IMethodInterceptor`).
- **Learn:** annotations and their order, `priority` vs `dependsOnMethods`, `alwaysRun`, skipping,
  listeners, method interceptors.
- **Read:** https://testng.org/
- **Checkpoint:** explain why `FileMenuQuitIT` is a separate class instead of one more method in
  `FileMenuIT`.

### 2.2 AssertJ

- **Why:** every check is an AssertJ assertion inside `evidence.verify(...)`.
- **Learn:** `assertThat(...)` chains, `.as("description")` for readable failures, assertions on
  strings, numbers (`isCloseTo(..., within(...))`), paths (`isDirectory()`).
- **Read:** https://assertj.github.io/doc/
- **Checkpoint:** make a check fail on purpose (e.g. change the expected title in `CreateProjectIT`),
  run it, and read the failure message, the red boxed screenshot and the report. Revert afterwards.

### 2.3 AssertJ Swing

- **Why:** finds Swing windows and components and performs real input in robot mode.
- **Where:** `IngeniousApp` (`BasicRobot`), page objects (`WindowFinder`, `GenericTypeMatcher`,
  `DialogFixture`, `JButtonMatcher`), `RobotUiDriver`, `Waits` (`Pause`, `Condition`).
- **Learn:** `Robot` and its settings, finders and matchers, fixtures, `GuiActionRunner`, timeouts.
  Note: it is unmaintained and needs JDK ≤ 25 and `--add-opens java.base/java.util=ALL-UNNAMED`.
- **Read:** https://github.com/assertj/assertj-swing
- **Checkpoint:** read `QuitDialogPage.waitFor` and write the same for another Swing dialog (e.g.
  `AboutUI`).

### 2.4 TestFX

- **Why:** finds JavaFX nodes and clicks/types on them in robot mode.
- **Where:** `IngeniousApp.fx()` (`FxRobot`), `StartupDialogPage`, `RobotUiDriver`, `FxNodes`
  (`WaitForAsyncUtils`). Runs with `-Dtestfx.robot=awt` so it shares the OS input with AssertJ Swing.
- **Learn:** `FxRobot.lookup(...)`, `clickOn`, `write`, `WaitForAsyncUtils.asyncFx/waitForFxEvents`.
  The module does **not** use TestFX's `ApplicationTest`/`FxToolkit` setup, because the IDE starts
  JavaFX itself.
- **Read:** https://github.com/TestFX/TestFX
- **Checkpoint:** in `StartupDialogPage`, explain how the project-name field is found without an id.

---

## Stage 3: This module (≈ 3–4 days)

### 3.1 Architecture and patterns

- **Learn:** README §3; the **Page Object** pattern (https://martinfowler.com/bliki/PageObject.html);
  the `UiDriver` abstraction with its three implementations (direct, robot, observed) and why tests
  never call the robots directly.
- **Checkpoint:** trace one click, `file.choose("Open Project")`, from `MenuPage` through
  `ObservedUiDriver` to `RobotUiDriver` and `DirectUiDriver`.

### 3.2 Evidence and reports

- **Learn:** README §5 and §7: `evidence.take` / `evidence.verify`, `Highlight`, `StepRef`, what each
  input mode captures, recordings, the HTML report.
- **Checkpoint:** run `-Duitest.scenarios=GEN-SC003 -Duitest.evidence=both` and match every
  screenshot to a line in `WindowMenuIT`.

### 3.3 The RTM

- **Why:** test cases come from the RTM, and results are published back to it.
- **Learn:** the RTM repository's `scenarios/*.yaml` (scenario → test cases, numbered `steps` with
  `[ASSERT]`), testing cycles (`cycles/<cycle>/results.yaml`, `dev-test/`, `qa/`), its scripts
  (`manage_test_case.py`, `validate_scenarios.py`, `save_evidence.py`, `record_test_result.py`,
  `generate_evidence_report.py`) and design doc `docs/design-docs/DD-EVID-2026-001-*`. README §8.
- **Checkpoint:** publish one scenario to a trial cycle (`-Duitest.report=html,rtm
  -Duitest.rtm.cycle=trial-<you> -Duitest.rtm.generateReport=false`), generate that cycle's RTM
  report into a temporary folder, click a "→ Step n" link, then delete the trial cycle.

### 3.4 Writing tests

- **Learn:** README §4 and §6, and the existing GEN tests as examples.
- **Checkpoint (capstone):** write the "Help > About shows the version" test from README §4 end to
  end, including the `AboutDialogPage`, in both input modes, with reviewed screenshots.

---

## Stage 4: Changing the framework (as needed)

| Topic | Why / where | Read |
|---|---|---|
| `java.awt.Robot` in depth (see 1.4) | Changing captures or the pointer glide (`ScreenCapture`, `ObservedUiDriver`) | https://docs.oracle.com/en/java/javase/17/docs/api/java.desktop/java/awt/Robot.html |
| Java 2D (`BufferedImage`, `Graphics2D`, `ImageIO`) | Painting windows, boxes, captions, pointer (`ScreenCapture`) | Java 2D tutorial: https://docs.oracle.com/javase/tutorial/2d/ |
| jcodec | Recordings (`ScreenRecorder`) | https://github.com/jcodec/jcodec |
| Jackson | Result JSON (`ResultStore`), RTM YAML (`RtmStepResolver`) | https://github.com/FasterXML/jackson |
| Python 3 + PyYAML | Reading or extending the RTM scripts | https://pyyaml.org/wiki/PyYAMLDocumentation |
| Xvfb, CI runners | Running robot mode on Linux/Windows CI | `man xvfb-run` |

When changing the framework, keep both input modes working, run the whole suite in both, and update
the README.

---

## Pitfalls to know early

- **Threads:** Swing on the EDT, JavaFX on the FX thread; never query JavaFX from the EDT.
- **Waiting:** wait for conditions, never `Thread.sleep`.
- **Robot mode on macOS:** needs permissions; the first click into a background app only activates it.
- **Direct vs robot:** a test passing in direct mode can still fail in robot mode (covered controls,
  focus); always check both and look at the screenshots.
- **Build:** no Java records or text blocks (prettier-java 0.7); prettier reformats files on each build.
- **RTM:** never edit its YAML or `results.yaml` by hand; use its scripts.
