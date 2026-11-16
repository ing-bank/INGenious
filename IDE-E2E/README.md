# IDE-E2E

End-to-end UI tests that launch the INGenious IDE, act like a user, assert on what is shown,
collect evidence (screenshots and/or a screen recording), write a report and quit. Swing is driven
with AssertJ Swing, embedded JavaFX with TestFX.

The module is only built with the `e2e` profile, so a normal `mvn install` is unaffected.

## Running

Run from the repository root. `-am` builds the upstream modules (IDE, Engine, ...) from source so the
test runs against your current code; `-Dtest=none` skips their unit tests.

Direct mode (default, silent: your mouse and keyboard stay free):

```bash
mvn -Pe2e -pl IDE-E2E -am verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false \
    -De2e.jvm=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java
```

Robot mode (real OS mouse and keyboard; do not touch them while it runs):

```bash
mvn -Pe2e -pl IDE-E2E -am verify -Dtest=none -Dsurefire.failIfNoSpecifiedTests=false \
    -De2e.input=robot \
    -De2e.jvm=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home/bin/java
```

On Linux, wrap either command in `xvfb-run -a -s "-screen 0 1920x1080x24"` to run on a virtual display.
Add `-De2e.evidence=recording` (or `both`) for a screen recording. `-De2e.jvm` is only needed when
Maven itself runs on JDK 26 or newer; point it at any JDK 17–25.

| Property | Default | Purpose |
|---|---|---|
| `e2e.input` | `direct` | `direct`: calls the UI controls directly (`doClick`, `fire`, `setText`) and paints the IDE windows for evidence. Never touches your mouse or keyboard and needs no OS permissions. `robot`: real OS mouse and keyboard plus full-desktop captures, exactly like a user; use on CI or a virtual display. |
| `e2e.evidence` | `screenshot` | `screenshot`: a PNG per step and per check. `recording`: one `recording.mp4` per test case instead. `both`: screenshots and a recording. A failure screenshot is always saved. |
| `e2e.report` | `html` | Report formats, comma-separated. Available: `html`. Add a format (e.g. `rtm`) by implementing `ReportWriter` and registering it in `ReportFormats`; unknown formats fail the run. |
| `e2e.jvm` | Maven's JVM | JVM that runs the IDE. Must be JDK 17–25 (AssertJ Swing needs `java.applet`, removed in JDK 26). |

### Prerequisites

- A display. Both modes open real IDE windows; true headless mode is not possible (see Limitations).
- Robot mode on macOS: grant **Accessibility** and **Screen Recording** to the app that starts Maven
  (VS Code, Terminal, iTerm) in System Settings > Privacy & Security, then restart that app.
- Robot mode on Windows: an unlocked, logged-in desktop session (no service account, no lock screen)
  at 100% display scaling.

## Results

Each `mvn verify` creates one run folder under `ui-test-output/` in the repository root (git-ignored):

```text
ui-test-output/<yyyyMMdd-HHmmss, UTC>/
├── report.html                      # one per selected format
├── results/<scenario>__<case>.json  # raw results, used to build the reports
└── <scenario code>/<test case code>/
    ├── NN-<step>.png                # e2e.evidence=screenshot|both
    └── recording.mp4                # e2e.evidence=recording|both
```

The HTML report shows each scenario (code, description, result) with its test cases (code,
description, result, duration, failure details) and their evidence. A `NN-FAILED-*` screenshot is
captured automatically when a test case fails.

## Evidence

**Checks.** `evidence.verify(...)` boxes the asserted item and captions it: green `PASSED` or red
`FAILED`. In a recording the video freezes for 2 s on that frame.

**Mouse pointer.** A mouse pointer is drawn on both screenshots and recordings. It is drawn by the
tests, not captured, because:

- OS screen captures (`java.awt.Robot`, used in robot mode) never include the mouse cursor on macOS
  or Windows.
- Direct mode has no mouse at all: it calls the controls in code.

So the tests know where every action happens (the driver computes the target control's position)
and draw the pointer there:

- Screenshots: the real cursor position (robot) or the position of the last action (direct).
- Recordings: for every click, typing action and window close the pointer glides to the target in
  8 eased steps, each recorded as its own frame of the real screen, then the video freezes for 0.5 s
  on the target with an orange click ripple and a caption such as `Click "Create Project"`. Between
  actions, live frames show the pointer like screenshots do.

**Pointer movement.** In robot mode the real mouse glides to each target like a user's hand
instead of jumping there (AssertJ Swing alone jumps), so hover effects happen and are recorded. In
direct mode a virtual pointer glides from the previous action's position.

**Title bar.** Swing does not paint the native title bar, so in direct mode the window title is
drawn where the title bar would be. Robot mode captures the real title bar.

**Recordings** are captured by the tests themselves and encoded with jcodec, a pure-Java H.264
encoder, so no ffmpeg is needed. The video runs at 12 fps (scaled to at most 1024 px wide). During
an action every pointer step is a real capture; between actions the screen is captured 4 times a
second and each capture is repeated to keep the video in real time. In direct mode only the IDE
windows are recorded, on a dark background. The MP4 plays in Chrome, Edge, Safari and Firefox.

Robot-mode captures use `java.awt.Robot.createScreenCapture` directly (about 25 ms on a 3440x1440
display). AssertJ Swing's `ScreenshotTaker` is deliberately not used: it captures on the Swing event
thread and waits for an idle UI twice per shot, which made robot recordings take about 2 s per frame
(a 17 s test took 5–7 minutes).

## Writing tests

- One class per scenario, one method per test case. Extend `IngeniousE2ETest`, name the class `*IT`,
  and annotate it with `@Scenario(code, description)` and each test method with
  `@TestCase(code, description)`.
- Call `evidence.take("step-name")` at meaningful steps; it becomes a screenshot and/or a caption in
  the recording.
- Wrap assertions in `evidence.verify(step, highlight, () -> assertThat(...))`. It runs the
  assertion, captures the asserted item boxed and captioned, and rethrows a failed assertion. Get the
  `Highlight` from the page object, e.g. `mainWindow.titleBar("...")`, or use
  `Highlight.component(swingComponent, "...")`.
- Interact only through page objects in `pages/`, and send all input through `UiDriver`
  (`app.driver()`), never directly through the robots, so the test works in both modes and every
  action shows up in the evidence.
- Use the robots (`app.swing()`, `app.fx()`) only to find windows, components and nodes.
- Java style: the build runs prettier-java 0.7, which cannot parse records or text blocks; use
  plain immutable classes and string concatenation instead.

## Limitations

**Platform and tooling**

- JDK 17–25 only: AssertJ Swing references `java.applet`, which JDK 26 removed. AssertJ Swing has not
  been released since 2020; replacing it with a small in-house Swing helper would lift this limit.
- Not truly headless: Swing cannot create windows with `java.awt.headless=true`, and TestFX's headless
  Monocle platform does not work for JavaFX embedded in Swing (`JFXPanel`). Use a real or virtual
  display.
- Linux and Windows are not wired up yet: the JavaFX jars come from the IDE module's macOS (aarch64)
  dependencies only, so OS-specific JavaFX profiles are needed before running elsewhere, and there is
  no CI workflow yet.
- Plain `xvfb-run` has no window manager, so windows have no title bar: the title-bar check is
  captioned but not boxed. Start a lightweight window manager (e.g. `fluxbox`) to get title bars.
- macOS: the tests cannot be made to run without a logged-in session; robot mode needs the
  permissions listed under Prerequisites.

**Input modes**

- Robot mode takes over the mouse and keyboard, and tests cannot run in parallel on one display
  (they share one pointer and focus). Spread tests across CI jobs or Xvfb displays instead.
- Robot mode breaks if the screen changes during the run: using the mouse or keyboard, switching
  desktop/Space or full-screen app, or connecting/disconnecting a display sends the clicks to the
  wrong place. Robot mode records the default (primary) display only.
- Direct mode still opens the IDE windows on screen, and they may take focus.
- Direct mode is less realistic: it skips real mouse, keyboard and focus events, so it does not
  catch problems such as a control hidden behind another one. Use robot mode on CI.

**Evidence and report**

- The mouse pointer is drawn, not captured (see Evidence).
- Between actions the screen is captured only 4 times a second (pure-Java capture and encoding), so
  quick UI transitions can be missed and captions may appear a frame late; pointer movement itself is
  captured at the full 12 fps.
- VS Code's built-in browser has no H.264 support and cannot play the recording; use the
  "open recording" link or another browser.
- Robot-mode recordings on macOS need the Screen Recording permission.
- Only the title bar has a highlight helper; JavaFX nodes need one once a test checks them.
- Web content inside JavaFX `WebView`s (AI chat, embedded browser) cannot be inspected by TestFX;
  check it through `executeScript` or evidence only.
- Only the `html` report format exists; `rtm` is still to be implemented.
- Run folder names use UTC (Maven's build timestamp).

**Test setup**

- The startup dialog page finds controls by style class and prompt text; add stable `setId(...)` /
  `setName(...)` to key controls to make tests less brittle.
- Projects created by earlier runs stay in `target/e2e-home/Projects` (and show up in the startup
  dialog) until `mvn clean`.
