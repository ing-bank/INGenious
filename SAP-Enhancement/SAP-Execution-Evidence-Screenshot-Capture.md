# User Story: [SAP] Capture Execution Evidence (Screenshots) for SAP Automation

Source: `SAP Ingenious Issues 2026.xlsx` — sheet `SAP Requests` (Item #3), sheet `US`.

## User Story

As an INGenious engineer, I want to capture execution evidence during SAP automation, so that
test failures can be investigated and execution results can be audited effectively.

## Acceptance Criteria (ATDD)

### Scenario 1: Capture screenshots during execution

**Given** SAP automation execution is running

**When** screenshot collection is enabled

**Then** screenshots are captured during execution

### Scenario 2: Capture evidence on failure

**Given** a SAP automation step fails

**When** execution stops

**Then** execution evidence is automatically collected

### Scenario 3: Include evidence in execution reports

**Given** screenshots were collected

**When** execution reporting is generated

**Then** the screenshots are attached and accessible from the report

### Scenario 4: Preserve evidence artifacts

**Given** execution completes

**When** artifacts are generated

**Then** evidence files are retained according to framework retention settings

### Scenario 5: Documentation is updated

**Given** evidence collection is implemented

**When** documentation is updated

**Then** setup instructions and usage examples are available in ingenious-doc

## Related SAP Request Items

### Item #3

**Description:** Video and trace.zip capture not available for SAP. What option do we have for
screenshots?

**Comment:** POC - Video/screenshot capture for SAP, similar to Browser screenshots/video. Need
to assess feasibility for SAP GUI compared to Playwright browser automation.

## Context

Browser automation provides screenshots, videos, and execution artifacts that help users
troubleshoot failures. SAP automation currently lacks similar evidence collection capabilities.
Users need execution evidence that can be attached to reports and reviewed after execution.

## Goal

Provide screenshot and evidence collection capabilities for SAP automation executions.

## Value

- Improves troubleshooting and root cause analysis.
- Provides execution visibility.
- Supports audit and compliance requirements.
- Aligns SAP reporting experience with browser automation reporting.

## Scope

Affected Components:

- SAP Automation Framework
- Reporting Engine
- Artifact Collection Module
- Execution Runtime
- Test Results Viewer

## Current State (gap analysis)

**The `Status` enum already distinguishes screenshot-eligible outcomes from not** —
`Status.PASS`/`FAIL` trigger a screenshot attempt, `Status.PASSNS`/`FAILNS` ("Pass/Fail **N**o
**S**creenshot") explicitly skip it, and `Status.SCREENSHOT` forces one
(`ingenious-api/src/main/java/com/ing/ingenious/api/status/Status.java`). Every single SAP action
added this session — and, checking the pre-existing ones, every SAP action already in
`SAPActions.java` — uses `PASSNS`/`FAILNS`/`DONE`, never `PASS`/`FAIL`/`SCREENSHOT`. That is not
a style choice; it is a deliberate workaround, because the screenshot pipeline cannot capture a
SAP screen today.

**The screenshot pipeline is hard-wired to two driver types only.** `ReportUtils.takeScreenshot`
(`Engine/src/main/java/com/ing/engine/reporting/util/ReportUtils.java`) has exactly two
parameters — `PlaywrightDriverCreation` and `WebDriverCreation` (mobile) — and returns `false`
(silently, no screenshot) if neither is available. The `Report` interface
(`Engine/src/main/java/com/ing/engine/reporting/intf/Report.java`) only exposes
`getPlaywrightDriver()`/`getWebDriver()`; there is no `getSapDriver()` or equivalent. Every
`TestCaseHandler` implementation (`HtmlTestCaseHandler`, `ExtentTestCaseHandler`,
`RPTestCaseHandler`, `AzureTestCaseHandler`, `ExcelTestCaseHandler`) calls this same
`ReportUtils.takeScreenshot(getPlaywrightDriver(), getWebDriver(), imgSrc)` — none of them have a
third, SAP branch.

**Each existing driver implements its own `createScreenShot()`**, and the shapes differ by what
the underlying automation library can provide:

| Driver | File | Mechanism |
|---|---|---|
| `PlaywrightDriverCreation` | `Engine/src/main/java/com/ing/engine/drivers/PlaywrightDriverCreation.java` | `page.screenshot(...)` — native Playwright API, headless-capable. |
| `WebDriverCreation` (Selenium/mobile) | `Engine/src/main/java/com/ing/engine/drivers/WebDriverCreation.java` | `((TakesScreenshot) driver).getScreenshotAs(...)`, falling back to `EmptyDriver` when the driver/alert state prevents a normal capture. |
| `EmptyDriver` (fallback only) | `Engine/src/main/java/com/ing/engine/drivers/customWebDriver/EmptyDriver.java` | `java.awt.Robot().createScreenCapture(rectangle)` over the **full desktop** — an OS-level capture, not an API-level one. |

**JACOB (`io.github.osobolev:jacob:1.21`, the Java-COM bridge SAP automation is built on) has no
imaging or video capability at all** — confirmed by inspecting the jar directly: its only classes
are `ActiveXComponent`, `Dispatch`, `Variant`, `SafeArray`, `ComThread`, `ROT`, etc. (pure COM
method/property marshalling, no bitmap/media classes). Screenshot capability is therefore not a
JACOB feature question — it depends on what the SAP GUI Scripting COM object model exposes, and
that model is behavior/data-oriented (`Text`, `Selected`, `RowCount`, ...), not
rendering-oriented; there is no widely-documented, version-stable "capture bitmap" method on
`GuiComponent`/`GuiSession` to fall back on.

**What SAP GUI Scripting *does* reliably expose**: every `GuiVComponent` (which `SAPElement`
already is, throughout `SAPActions.java`) has standard `ScreenLeft`/`ScreenTop`/`Width`/`Height`
properties — retrievable via the exact same `Dispatch.get(SAPElement, ...)` pattern already used
for `Text`/`Selected`/`RowCount` elsewhere in this class. Combined with the `Robot`-based full-
desktop capture technique **already shipping** in `EmptyDriver`, a SAP screenshot is achievable by
reading the element's on-screen rectangle via JACOB and then cropping a `Robot` capture to it —
no new native/JACOB plumbing required, only a new `SapDriverCreation`-style class following the
existing `createScreenShot()` shape.

**Video and `trace.zip` have no SAP-side equivalent to build on.** Both are 100% native Playwright
features — `PlaywrightDriverFactory` sets `BrowserContext.setRecordVideoDir(...)`, and
`HtmlTestCaseHandler` reads `page.video().path()` / parses the Playwright trace file directly.
Neither JACOB nor SAP GUI Scripting has any comparable recording/streaming API, so there is no
"missing wiring" to add here the way there is for screenshots — a SAP video would have to be
assembled independently (e.g. stitching periodic `Robot` captures), which is a materially larger
and separate effort from screenshot capture.

## Proposed Actions

1. **`SapDriverCreation.createScreenShot()`** (new class,
   `Engine/src/main/java/com/ing/engine/drivers/SapDriverCreation.java`) — mirrors the
   `File createScreenShot()` shape of `PlaywrightDriverCreation`/`WebDriverCreation`:
   - Resolve the active window via `SapSessionManager.INSTANCE.current()` (falling back to the
     whole screen if no specific element is focused).
   - Read `ScreenLeft`/`ScreenTop`/`Width`/`Height` off that `GuiVComponent` via `Dispatch.get`.
   - Crop a `Robot.createScreenCapture(rectangle)` to those bounds and `ImageIO.write` it to a
     temp PNG, exactly like `EmptyDriver` already does for the full-desktop fallback case.

2. **Wire it into the existing pipeline, not a parallel one**:
   - Add `getSapDriver()` to the `Report` interface alongside `getPlaywrightDriver()`/`getWebDriver()`.
   - Add a third parameter to `ReportUtils.takeScreenshot(..., SapDriverCreation sapDriver, ...)`
     with a SAP branch mirroring the existing Playwright/mobile branches.
   - Update the five `TestCaseHandler` implementations' `takeScreenShot`/`takeSSAndPutDetail`
     call sites to pass `getSapDriver()` through, the same one-line change repeated per handler.
   - No changes needed to `TestCaseReport.getNewScreenShotName()` or `FilePath.getCurrentResultsPath()`
     — the filename convention and results folder are already driver-agnostic.

3. **`sapCaptureScreenshot`** (new explicit `@Action`, `ObjectType.SAP`/`SAP_OBJECT`) — an
   on-demand capture step mirroring the browser's `General.TakePageScreenshot()`, for evidence a
   tester wants regardless of pass/fail (Scenario 1), using `Status.SCREENSHOT` to force capture
   through the now-SAP-aware pipeline.

4. **Automatic capture on failure (Scenario 2)** reuses the *existing* `Status.PASS`/`FAIL` +
   `RunSettings.getScreenShotFor()` (`Fail`/`Pass`/`Both`) machinery unchanged — once
   `ReportUtils.takeScreenshot` has a working SAP branch, any SAP action already reporting
   `Status.FAIL` gets a screenshot for free.

5. **Retrofit GUI-facing SAP actions from `PASSNS`/`FAILNS` to `PASS`/`FAIL`.** Implemented —
   not a blanket rename. Actions that assert or interact with visible SAP GUI state (window,
   text field, button, checkbox, combobox, tab, table, grid, menu, context menu, tree, status
   bar, general element) now report `Status.PASS`/`FAIL`, picking up automatic capture per
   `RunSettings.getScreenShotFor()`. Connection/session lifecycle and pure config actions kept
   `PASSNS`/`FAILNS` — see the resolved open question below for why.

6. **Retention (Scenario 4)** needs no new mechanism — SAP screenshots land in
   `FilePath.getCurrentResultsPath()` exactly like browser/mobile screenshots, so they're already
   subject to whatever retention/cleanup policy governs that folder today.

## Open Questions

- **Retrofit or additive?** Resolved — retrofit, but scoped to GUI-facing actions only. All new
  and pre-existing SAP actions used `PASSNS`/`FAILNS` specifically because screenshots weren't
  possible; once `SapDriverCreation` exists, that workaround no longer applies for actions whose
  outcome depends on visible SAP GUI state (`sapAssert*`, table/grid lookups, checkbox/tree/status
  bar/general element actions, window management, `sapExecuteTransaction`/`sapEndTransaction`/
  `sapRefreshSession`) — those now report `Status.PASS`/`FAIL` and pick up automatic capture via
  `RunSettings.getScreenShotFor()` (`Fail`/`Pass`/`Both`), the same as browser/mobile actions.
  Connection/session lifecycle actions (`sapInitConnection`, `sapSwitchConnection`,
  `sapCloseConnection`, `sapCloseAllConnection`, `sapOpenSession`, `sapSwitchSession`,
  `sapCloseSession`, `sapCloseLogonScreen`) and pure in-memory config actions
  (`sapSetglobalObjectProperty`, `sapSetObjectProperty`) kept `PASSNS`/`FAILNS` on purpose: they
  fail on JACOB/COM connectivity or Java-side map manipulation, not on-screen state, so a
  screenshot at that point is either unavailable (no window yet) or diagnostically useless
  (wouldn't explain a connection/config error) — capturing one anyway would just add I/O overhead
  for every connection/session event across a suite. This does increase report volume/size for
  the GUI-facing actions once `Fail`/`Both` is configured — acceptable, the same trade-off
  browser/mobile tests already make.
- **`Robot`-based capture requires an active, unlocked, visible desktop session** — unlike
  Playwright, which is headless-capable. Resolved — not a new constraint in practice: INGenious
  runs co-located with SAP GUI on the same server/environment (no separate RDP hop), and SAP GUI
  Scripting itself already requires an interactive desktop session to run at all, with or without
  screenshot capture. So this isn't an incremental risk introduced by `Robot` — it's the same
  prerequisite SAP GUI automation already has today. Still worth a one-line callout in setup docs
  (Scenario 5) so a locked session explains a black/stale image rather than looking like a bug.
- **Multi-session/multi-window resolution** — `SapSessionManager` can have multiple concurrent
  sessions/windows open (per `sapOpenSession`/`sapSwitchSession`). Resolved — use `ActiveWindow`
  bounds as the primary target, with a full-desktop `Robot` fallback if no active window can be
  resolved. Since the environment runs only the SAP GUI (no unrelated apps sharing the screen),
  a full-desktop fallback is safe either way — it'll only ever show SAP GUI content.
- **Video capture is out of scope for this POC.** Resolved — skip video for now. Given
  JACOB/SAP GUI Scripting have no native recording API, and the `Robot`-per-screenshot approach
  would need to assemble frames externally, sustained over the whole test duration, video is
  deferred to a separate, later effort rather than bundled into this screenshot POC. Item #3's
  "video" ask is deferred, not dropped.
