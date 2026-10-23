# INGenious Comprehensive Changelog

> Scope: recent codebase changes discussed in the Playwright BrowserContext, WebView context switching, column selection, ADB/mobile actions, IDE input focus, IntelliSense, and LambdaTest reusable component markers chats.

## 8. LambdaTest reusable component markers

When a browser-based test runs on LambdaTest (Playwright via grid/CDP), each Reusable Component / User Intent execution now fires a `lambda-testCase-start` marker before it runs and a `lambda-testCase-end` marker after it completes. This creates a named, collapsible sub-test entry in LambdaTest's session report for every reusable, enabling nested reporting.

The markers use the existing `page.evaluate("_ => {}", ...)` pattern already used by `setLambdaStatus`. Failures in the marker call are swallowed with a WARNING log so a LambdaTest API issue can never break test execution.

### What changed

- `PlaywrightDriverCreation` gained `isLambdaTestExecutionPlatform()`, which detects LambdaTest grid execution by checking that `RemoteGridURL` contains `lambdatest.com` and `isGridExecution()` is true. This mirrors the equivalent method already on `WebDriverCreation`.
- `PlaywrightDriverCreationApi` was extended with `isLambdaTestExecutionPlatform()` so plugins can detect the platform through the API contract.
- `TestStepRunner.executeTestCase()` now calls `fireLambdaTestCaseMarker()` with `lambda-testCase-start` immediately after `startComponent` and `lambda-testCase-end` in the `finally` block immediately before `endComponent`. The marker name is the step's action string (e.g. `Login:SuccessfulLogin`).
- `fireLambdaTestCaseMarker()` is a private helper on `TestStepRunner` that guards on `pw != null && pw.page != null && pw.isLambdaTestExecutionPlatform()` and silently logs on exception.

### Files

- [Engine/src/main/java/com/ing/engine/drivers/PlaywrightDriverCreation.java](Engine/src/main/java/com/ing/engine/drivers/PlaywrightDriverCreation.java)
- [ingenious-api/src/main/java/com/ing/ingenious/api/contract/drivers/PlaywrightDriverCreationApi.java](ingenious-api/src/main/java/com/ing/ingenious/api/contract/drivers/PlaywrightDriverCreationApi.java)
- [Engine/src/main/java/com/ing/engine/execution/run/TestStepRunner.java](Engine/src/main/java/com/ing/engine/execution/run/TestStepRunner.java)

## 1. Console and report experience

The live console was moved to a JavaFX WebView-backed renderer and the report output was aligned with that new presentation layer.

### What changed

- `ConsolePanel` now embeds `ConsoleWebView` and redirects `System.out` / `System.err` into the HTML console during a run.
- `ConsoleWebView` renders tagged log lines as colored pills, strips ANSI escapes and emoji, supports clickable report paths, and follows the app theme.
- `ConsoleReport` continues to capture execution output into the run console file, while `SummaryReport` emits structured summary payloads that the WebView can render cleanly.
- `ReportCommand` now points the latest summary-report flow at the updated report layout.

### Files

- [IDE/src/main/java/com/ing/ide/main/utils/ConsolePanel.java](IDE/src/main/java/com/ing/ide/main/utils/ConsolePanel.java)
- [IDE/src/main/java/com/ing/ide/main/utils/ConsoleWebView.java](IDE/src/main/java/com/ing/ide/main/utils/ConsoleWebView.java)
- [Engine/src/main/java/com/ing/engine/reporting/impl/ConsoleReport.java](Engine/src/main/java/com/ing/engine/reporting/impl/ConsoleReport.java)
- [Engine/src/main/java/com/ing/engine/reporting/SummaryReport.java](Engine/src/main/java/com/ing/engine/reporting/SummaryReport.java)
- [Engine/src/main/java/com/ing/engine/cli/commands/ReportCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/ReportCommand.java)

## 2. Playwright BrowserContext settings and context switching

Playwright runtime setup was expanded so browser contexts can be configured from stored settings, and context transitions are handled explicitly.

### What changed

- `ContextOptions` now persists BrowserContext settings under the per-project BrowserContexts folder.
- Default context settings include auth state, storage state, viewport, device scale, touch/mobile flags, screen size, user agent, locale, timezone, offline mode, record-video settings, and page timeout.
- `PlaywrightDriverFactory.createContext` applies those settings for both local runs and grid-backed runs.
- `Switch` now manages browser context transitions by creating a new context with updated options and keeping the active page and driver in sync.

### Files

- [Datalib/src/main/java/com/ing/datalib/settings/ContextOptions.java](Datalib/src/main/java/com/ing/datalib/settings/ContextOptions.java)
- [Engine/src/main/java/com/ing/engine/drivers/PlaywrightDriverFactory.java](Engine/src/main/java/com/ing/engine/drivers/PlaywrightDriverFactory.java)
- [Engine/src/main/java/com/ing/engine/commands/browser/Switch.java](Engine/src/main/java/com/ing/engine/commands/browser/Switch.java)
- [Engine/src/main/java/com/ing/engine/drivers/PlaywrightDriverCreation.java](Engine/src/main/java/com/ing/engine/drivers/PlaywrightDriverCreation.java)

## 3. Mobile execution, ADB, and device switching

Mobile support was tightened up across device launch, session reuse, and ADB-driven flows.

### What changed

- `launchAndSwitchToDevice()` now launches a new device session, registers it by alias, and switches the active driver to it.
- `switchToDevice()` now only reattaches to an already-launched device session, making the separation between launch and switch explicit.
- `Task` tracks the session lifecycle so device sessions can be cleaned up and reused reliably.
- `AppiumDeviceCommands` adds the mobile action surface for swipe, rotation, and related device gestures.
- `AdbCommands` and the IDE-side `AndroidAdbCLI` provide ADB command execution paths for Android workflows.

### Files

- [Engine/src/main/java/com/ing/engine/commands/mobile/SwitchTo.java](Engine/src/main/java/com/ing/engine/commands/mobile/SwitchTo.java)
- [Engine/src/main/java/com/ing/engine/core/Task.java](Engine/src/main/java/com/ing/engine/core/Task.java)
- [Engine/src/main/java/com/ing/engine/commands/mobile/AppiumDeviceCommands.java](Engine/src/main/java/com/ing/engine/commands/mobile/AppiumDeviceCommands.java)
- [Engine/src/main/java/com/ing/engine/commands/mobile/AdbCommands.java](Engine/src/main/java/com/ing/engine/commands/mobile/AdbCommands.java)
- [IDE/src/main/java/com/ing/ide/main/shr/mobile/android/AndroidAdbCLI.java](IDE/src/main/java/com/ing/ide/main/shr/mobile/android/AndroidAdbCLI.java)

## 4. Test case canvas column selection

The Test Plan and Reusable canvases now support persistent column visibility control without breaking renderers or editors when columns are hidden.

### What changed

- `TableColumnManager` gained explicit visible-column handling and menu-building support.
- `TestCaseComponent` persists the selected column layout and re-applies editors and renderers after reloads or visibility changes.
- `TestCaseValidator` and `TestCaseAutoSuggest` now use view/model index conversion so hidden columns do not break validation, dropdowns, or row edits.
- Column visibility is stored in `AppSettings`, so the layout survives reloads.

### Files

- [IDE/src/main/java/com/ing/ide/main/utils/table/TableColumnManager.java](IDE/src/main/java/com/ing/ide/main/utils/table/TableColumnManager.java)
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseComponent.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseComponent.java)
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseValidator.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseValidator.java)
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseAutoSuggest.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseAutoSuggest.java)
- [IDE/src/main/java/com/ing/ide/settings/AppSettings.java](IDE/src/main/java/com/ing/ide/settings/AppSettings.java)

## 5. IDE input focus and step editing fixes

The test-step editor now keeps focus, caret position, and inline editing behavior stable when users work in the Input column or switch between cells.

### What changed

- `TestCaseAutoSuggest` now routes editing through the current model state when columns are hidden and keeps the input editors focused while editing.
- `InputMainAutoSuggest` preserves the caret position and avoids rewriting identical text, which removes the jumpy editing behavior.
- Function-style and alias-style inputs are no longer treated like data-sheet references when the editor auto-appends separators.
- The test-case panel keeps its keyboard shortcuts usable even when focus is inside child editors.

### Files

- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseAutoSuggest.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseAutoSuggest.java)
- [IDE/src/main/java/com/ing/ide/main/utils/table/autosuggest/InputMainAutoSuggest.java](IDE/src/main/java/com/ing/ide/main/utils/table/autosuggest/InputMainAutoSuggest.java)
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseComponent.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseComponent.java)

## 6. IntelliSense and action-spec metadata

The action metadata surface was made more explicit so IDE and MCP consumers can drive suggestions from structured action specs instead of hard-coded heuristics.

### What changed

- `ArgSpec` now represents per-action metadata such as input requirements, examples, and condition grammar.
- `ActionSpecCatalog` loads structured action specs and sidecar overrides, making action metadata available in one shared place.
- `MethodInfoManager` still provides the action-to-method mapping, while `TestCaseAutoSuggest` and `InputMainAutoSuggest` consume the richer metadata for editor suggestions.
- The result is better IntelliSense for action, input, and condition editing across the IDE.

### Files

- [Engine/src/main/java/com/ing/engine/mcp/ArgSpec.java](Engine/src/main/java/com/ing/engine/mcp/ArgSpec.java)
- [Engine/src/main/java/com/ing/engine/mcp/ActionSpecCatalog.java](Engine/src/main/java/com/ing/engine/mcp/ActionSpecCatalog.java)
- [Engine/src/main/java/com/ing/engine/support/methodInf/MethodInfoManager.java](Engine/src/main/java/com/ing/engine/support/methodInf/MethodInfoManager.java)
- [IDE/src/main/java/com/ing/ide/main/utils/table/autosuggest/InputMainAutoSuggest.java](IDE/src/main/java/com/ing/ide/main/utils/table/autosuggest/InputMainAutoSuggest.java)
- [IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseAutoSuggest.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseAutoSuggest.java)

## 7. Notes on the broader implementation set

This changelog intentionally groups the related work from the following chat topics into a single document:

- Customize Playwright BrowserContext settings
- WebView context switching capability
- Column selection feature plan
- ADB command execution plan
- Mobile actions analysis and plan
- Difference between launchAndSwitchToDevice and switchToDevice
- IDE input focus issue
- IntelliSense implementation plan
