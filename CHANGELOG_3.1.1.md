# INGenious — Consolidated Change Log


## Table of Contents

1. [Test Manager — publish report flows through Console report](#1-test-manager--publish-report-flows-through-console-report)
2. [Test Case Tags — persist to YAML and survive reloads](#2-test-case-tags--persist-to-yaml-and-survive-reloads)
3. [File → Restart actually restarts INGenious](#3-file--restart-actually-restarts-ingenious)
4. [Object Repository — auto-select the right tab for the project type](#4-object-repository--auto-select-the-right-tab-for-the-project-type)
5. [API Workbench — paste a `curl` command in the URL bar](#5-api-workbench--paste-a-curl-command-in-the-url-bar)
6. [API Workbench — Authorization converts to a proper `addHeader` step](#6-api-workbench--authorization-converts-to-a-proper-addheader-step)
7. [API Workbench — "+ New Request" button](#7-api-workbench--new-request-button)
8. [Mobile Object Repository — per-platform (Android / iOS) properties](#8-mobile-object-repository--per-platform-android--ios-properties)
9. [TM Settings — Test Connection button readable when bulb turns green](#9-tm-settings--test-connection-button-readable-when-bulb-turns-green)
10. [Postman & Bruno collection import → INGenious Reusables](#10-postman--bruno-collection-import--ingenious-reusables)
11. [Manage Devices — new tab, LambdaTest categorised capabilities](#11-manage-devices--new-tab-lambdatest-categorised-capabilities)
12. [Remote URL — auto-mask credentials on entry/paste](#12-remote-url--auto-mask-credentials-on-entrypaste)
13. [Manage Devices — cleaner, sectioned LambdaTest capabilities view](#13-manage-devices--cleaner-sectioned-lambdatest-capabilities-view)
14. [Mobile Scroll — unified `scroll` for Android + iOS](#14-mobile-scroll--unified-scroll-for-android--ios)
15. [Rename: "LambdaTest Capabilities" → "LambdaTest Grid Capabilities"](#15-rename-lambdatest-capabilities--lambdatest-grid-capabilities)
16. [Phase-out Manage Browser → Emulators path](#16-phase-out-manage-browser--emulators-path)
17. [Configurations menu reorganisation & renames](#17-configurations-menu-reorganisation--renames)
18. [CLI override coverage audit + full CLI usage docs](#18-cli-override-coverage-audit--full-cli-usage-docs)
19. [HTML report — color-formatted Response payload, separate Headers section, copy buttons](#19-html-report--color-formatted-response-payload-separate-headers-section-copy-buttons)
20. [HTML summary — clickable rows (Tabulator v6 fix)](#20-html-summary--clickable-rows-tabulator-v6-fix)
21. [HTML report — in-page Console Viewer with working filter under `file://`](#21-html-report--in-page-console-viewer-with-working-filter-under-file)
22. [API Workbench — right-click response to auto-build path assertions (JSON + XPath)](#22-api-workbench--right-click-response-to-auto-build-path-assertions-json--xpath)
23. [CLI overrides — implementation of all missing prefixes + typed flags + `config prefixes` help](#23-cli-overrides--implementation-of-all-missing-prefixes--typed-flags--config-prefixes-help)
24. [`project validate` — quality dashboard with per-test-case & per-reusable Kind classification](#24-project-validate--quality-dashboard-with-per-test-case--per-reusable-kind-classification)
25. [`project upgrade` — 4-step interactive modernisation wizard](#25-project-upgrade--4-step-interactive-modernisation-wizard)
26. [`validate` Test-set coverage — correct scoring at test-case granularity](#26-validate-test-set-coverage--correct-scoring-at-test-case-granularity)
27. [CLI dispatcher — `object` / `testset` / `data` now route to picocli](#27-cli-dispatcher--object--testset--data-now-route-to-picocli)
28. [Single-source CLI reference document](#28-single-source-cli-reference-document)

---

## 1. Test Manager — publish report flows through Console report

**What changed**
The publish banner that Test Manager prints (`📡 Test Manager — publishing results …`) was only appearing in the live terminal. It now also lands in the per-run `console.txt` (the Console report).

**Root cause**
[TestManagerSync.java](Engine/src/main/java/com/ing/engine/reporting/sync/testmanager/TestManagerSync.java) cached `System.out` in a `static final OUT` field at class-load time. `ConsoleReport.init()` later swaps `System.out` for a `MultiOutputStream` that tees to both the terminal and `console.txt`, but the cached reference still pointed at the original stream and bypassed the tee.

**Fix**
Removed the cached `OUT` field; `println()` now resolves `System.out` at call time.

**Files**
- [Engine/src/main/java/com/ing/engine/reporting/sync/testmanager/TestManagerSync.java](Engine/src/main/java/com/ing/engine/reporting/sync/testmanager/TestManagerSync.java)
- [Engine/src/main/java/com/ing/engine/reporting/impl/ConsoleReport.java](Engine/src/main/java/com/ing/engine/reporting/impl/ConsoleReport.java) (context only)

**Breaking changes:** none.
**How to verify:** run any suite that publishes to Test Manager and open the run's `console.txt`; the publish banner is present there.

---

## 2. Test Case Tags — persist to YAML and survive reloads

**What changed**
Right-click a test case → **Edit Tags** → add e.g. `@smoke`. Previously the tags were updated in memory but did not appear in the YAML file and were lost after a project reload. Now they round-trip correctly.

**Implementation (three coordinated changes)**
1. [YamlTestCaseStore.java](Datalib/src/main/java/com/ing/datalib/component/io/YamlTestCaseStore.java) — new `loadTags(File)` reads just the `tags:` section.
2. [TestCase.java](Datalib/src/main/java/com/ing/datalib/component/TestCase.java)
   - New public `saveMetadata()` — flushes a tags-only change to YAML by loading steps (if not loaded), re-saving, then restoring in-memory state.
   - New private `syncTagsFromStore()` called from `loadSteps()` mirrors on-disk tags back into the project's `DataItem`, so YAML edits survive a reload.
3. [ProjectTree.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/tree/ProjectTree.java) — `editTag(TreePath)` for a `TestCaseNode` now uses a new `editTag(DataItem, TestCase)` overload whose callback invokes `testCase.saveMetadata()` after `tc.setTags(...)`.

**Tests:** 490 tests pass; full BUILD SUCCESS.
**Breaking changes:** none.
**How to verify:** add `@smoke` to a test case; open the corresponding YAML and confirm:
```yaml
tags:
  - '@smoke'
```

---

## 3. File → Restart actually restarts INGenious

**Symptom**
File → **Restart** was only shutting INGenious down (no relaunch).

**Root cause**
`doRestart()` used `Desktop.getDesktop().open(new File("ingenious.command"))`, which is async on macOS — the JVM exited (via `EXIT_ON_CLOSE` on `dispose()`) before Terminal could relaunch. Worse, `doRestart()` ran *after* `dispose()`, racing with JVM shutdown.

**Fix** — in [AppMainFrame.java](IDE/src/main/java/com/ing/ide/main/mainui/AppMainFrame.java)
- Replaced `Desktop.open` with `ProcessBuilder` so the relauncher is a properly detached child:
  - Windows: `cmd /c start "INGenious" ingenious.bat`
  - macOS:   `/usr/bin/open ingenious.command`
  - Linux/other Unix: `/bin/sh ingenious.command` (executable bit ensured)
- Working dir pinned to `user.dir`, stdout/stderr discarded.
- `doRestart()` is now called *before* `dispose()`.

**Breaking changes:** none.
**How to verify:** File → Restart on macOS/Windows/Linux now closes the current window and brings up a fresh INGenious process.

---

## 4. Object Repository — auto-select the right tab for the project type

Mobile/SAP/Structured-Data projects previously always opened the Web OR by default, forcing an extra navigation.

**Change** — [ObjectRepo.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/ObjectRepo.java)
New `selectDefaultRepo()` runs after `load()`:
- Web OR has pages → **Web** (preserves existing web behaviour)
- else Mobile OR has pages → **Mobile**
- else Structured Data OR has pages → **Structured Data**
- else SAP OR has pages → **SAP**
- empty project → falls back to **Web**

The existing `ItemListener` handles `CardLayout` swap and `adjustUI()`, so simply selecting the toggle is enough. Errors during inspection fall back to Web.

**Breaking changes:** none.

---

## 5. API Workbench — paste a `curl` command in the URL bar

**Feature**
Postman-style "paste a curl command into the URL field" auto-populates method, URL, headers and body.

**New module** — [Datalib/src/main/java/com/ing/datalib/api/CurlParser.java](Datalib/src/main/java/com/ing/datalib/api/CurlParser.java)
Shell-aware parser, converts a curl invocation to an `APIRequest`. Supports:
- Methods: `-X`/`--request`; inference (POST if body present else GET)
- Headers: `-H`/`--header`; shortcuts `-A`/`--user-agent`, `-e`/`--referer`, `-b`/`--cookie`
- Body: `-d`, `--data`, `--data-*` family, `--form` (multipart key/value)
- Basic auth via `-u user:pass` → `Authorization: Basic …`
- Line continuations (`\`), single/double quotes, `$'…'` strings, query params

**Wired into**
- [RequestPanel.java](IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/RequestPanel.java) — URL field detects `curl …`, calls the parser, repopulates method/headers/body/params.
- New unit tests under `Datalib/src/test/java/com/ing/datalib/api/` — 15 parser tests, all passing.

**Files touched**
- [Datalib/src/main/java/com/ing/datalib/api/CurlParser.java](Datalib/src/main/java/com/ing/datalib/api/CurlParser.java) *(new)*
- [Datalib/src/main/java/com/ing/datalib/api/APIRequest.java](Datalib/src/main/java/com/ing/datalib/api/APIRequest.java)
- [Datalib/src/main/java/com/ing/datalib/api/AuthConfig.java](Datalib/src/main/java/com/ing/datalib/api/AuthConfig.java)
- [Datalib/src/main/java/com/ing/datalib/api/KeyValuePair.java](Datalib/src/main/java/com/ing/datalib/api/KeyValuePair.java)
- [Datalib/src/main/java/com/ing/datalib/api/RequestBody.java](Datalib/src/main/java/com/ing/datalib/api/RequestBody.java)

**Breaking changes:** none.
**How to verify:** copy any `curl https://api.github.com/users/octocat -H 'Accept: application/json'` and paste into the URL field of API Workbench.

---

## 6. API Workbench — Authorization converts to a proper `addHeader` step

**Symptom**
When converting an API-workbench request to a test case, Authorization was being emitted with `Input=Authorization` and `Condition=Bearer …`, which doesn't match how the engine reads header inputs.

**Fix** — [APITester.java](IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/APITester.java) (`addAuthSteps`)
All three auth flavors (Basic, Bearer, API Key) now emit a single `addHeader` step:
- Input: `@<HeaderName>=<value>` (e.g. `@Authorization=Bearer <token>`)
- Condition: empty
Matches the existing header-step pattern.

**Breaking changes:** none — newly generated steps follow the standard `@HeaderName=value` convention.

---

## 7. API Workbench — "+ New Request" button

**Feature**
A new request can now be created without overwriting or deleting the visible one.

**Change** — [APITesterUI.java](IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/APITesterUI.java)
- New **+ New Request** button on the right-panel header, opposite the "Editing…" label.
- Calls a new `newBlankRequest()`:
  - Auto-saves the currently-edited request (existing `loadRequest` flow persists collection-backed requests — no data loss).
  - Loads a fresh blank `APIRequest` (method=GET, no URL/headers/body), clears source tracking and the response pane.
  - Focuses the URL field for immediate typing or curl-paste.
- New request isn't attached to a collection yet — pressing **Save** triggers the existing "save new request" flow that prompts for target collection/name. Tree-context "Add Request" still works unchanged.

**Breaking changes:** none.

---

## 8. Mobile Object Repository — per-platform (Android / iOS) properties

**Feature**
A Mobile OR object can now carry **two independent property lists** — one for Android, one for iOS — toggled with an Android/iOS switch in the table toolbar. At runtime, the engine picks the right list based on the active Appium driver.

**Data model (Datalib)**
- New [MobilePlatform.java](Datalib/src/main/java/com/ing/datalib/or/mobile/MobilePlatform.java) enum (`ANDROID`, `IOS`).
- [MobileOR.java](Datalib/src/main/java/com/ing/datalib/or/mobile/MobileOR.java) — defaults split into `ANDROID_PROPS` (UiAutomator, id, Accessibility, xpath, css, name, tagName, link_text, class) and `IOS_PROPS` (UiAutomation, …). `OBJECT_PROPS` retained as the union for back-compat; `defaultPropsFor(platform)` added.
- [MobileORObject.java](Datalib/src/main/java/com/ing/datalib/or/mobile/MobileORObject.java) — two parallel lists (`AndroidProperty`, `IOSProperty`) + transient `activePlatform`. `TableModel` and convenience setters (`setId`, `setXpath`, …) route through the active platform. Platform-explicit variants (`getAttributes(platform)`, `setAttributeByName(platform,…)`, `addNewAttribute(platform,…)`, `removeAttribute(platform,…)`, `setAttributeOnBothPlatforms(...)`) added for bulk ops and engine. **Legacy `<Property>` lists are auto-migrated into both platform lists** via `@JsonSetter("Property")`. `clone` and `isEqualOf` cover both lists.
- [ResolvedMobileObject.java](Datalib/src/main/java/com/ing/datalib/or/mobile/ResolvedMobileObject.java), [ORAttribute.java](Datalib/src/main/java/com/ing/datalib/or/common/ORAttribute.java) — updated to carry per-platform context.
- YAML loader: [YamlMobileElementDefinition…](Datalib/src/main/java/com/ing/datalib/or/yaml/) reads both `android:` and `ios:` blocks into the object.

**IDE UI**
- [MobileORTable.java](IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/or/mobile/MobileORTable.java) — toolbar now has **Android** (default) and **iOS** toggle buttons in a `ButtonGroup`. `setActivePlatform` reroutes the table model and refreshes columns. `loadObject` syncs the object's `activePlatform` to the toolbar's selection. All bulk ops (`clearFromSelected/Page`, `removeFromSelected/Page`, `addToSelected/Page`, `setPriorityToSelected/Page`, single-row `removeRow`) call platform-explicit methods on the active platform.

**Execution engine**
- [MobileObject.java](Engine/src/main/java/com/ing/engine/drivers/MobileObject.java)
  - `resolvePlatform()` — inspects the live driver; returns `IOS` if it's an `io.appium.java_client.ios.IOSDriver`, else `ANDROID`. **Single source of truth at runtime.**
  - `getMElements(...)` — for each `MobileORObject`, calls `getAttributes(platform)`; if blank, falls back to the other platform's list via `hasUsableValue(...)`.
  - `getElements(...)` walks the chosen list and resolves each `ORAttribute` via `ByObjectProp`.

**Breaking changes**
- Old projects load fine — the legacy single-list `<Property>` block is auto-migrated into both platform lists, so first save will write `AndroidProperty` + `IOSProperty`. Reading remains backwards compatible.
- New default property sets differ between platforms (`UiAutomator` vs `UiAutomation` etc.) — when adding a *new* mobile object the visible columns now depend on the toggle.

**How to verify**
1. Open a Mobile project's OR → see Android/iOS toggle in the toolbar.
2. Add an iOS-only locator under **iOS**, an Android-only under **Android**.
3. Run the test on each platform — Engine resolves the right list. Missing platform falls back to the populated one.

---

## 9. TM Settings — Test Connection button readable when bulb turns green

**Symptom**
On a successful TM connection, the green bulb on the **Test Connection** button was invisible against the button's blue background.

**Change** — [INGeniousSettings.java](IDE/src/main/java/com/ing/ide/main/settings/INGeniousSettings.java)
- In `testConnection(...)`, on success the button switches to a **white background** (dark text, blue outline) via the new `applyTestConnSuccessStyle()` helper — green bulb is visible.
- On failure or yellow bulb reset (e.g. switching TM modules), button restored to original primary blue via `applyTestConnDefaultStyle()`.
- Hover/pressed client properties (used by [ConnectButton.java](IDE/src/main/java/com/ing/ide/main/utils/ConnectButton.java)) updated to match the new state.

**Breaking changes:** none (cosmetic).

---

## 10. Postman & Bruno collection import → INGenious Reusables

This is the largest feature in the period. Imports a Postman or Bruno collection and converts each request into an INGenious **Reusable** (a `Scenario` containing `Webservice` step test cases under `ReusableComponents/<Scenario>/<TestCase>.csv`).

### Implementation plan
Created at [INGenious_Postman_Bruno_Import_Implementation_Plan.md](INGenious_Postman_Bruno_Import_Implementation_Plan.md). Highlights:
- **Reuses existing machinery.** Phase 0 refactor extracts the step-building body of [APITester.convertRequestToTestCase](IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/APITester.java) into a shared `buildStepsForRequest`, then adds a sibling `convertRequestToReusable(...)` that writes to a `Scenario` created via `Project.addReusableScenario(...)`.
- **Pluggable importer SPI** (`CollectionImporter`) → leaves room for OpenAPI / HAR / Insomnia later.
- **Detailed mappings** for folders→scenarios (two strategies), all auth types, all body modes, `{{var}}` → `%var%`, and a closed set of script→assertion translations (`pm.response.to.have.status`, `pm.expect(...).to.eql/.to.include`, `pm.environment.set`, Bruno `assert` / `vars:post-response`); everything else preserved verbatim with `// TODO` warnings.

### New files

**Datalib** (`com.ing.datalib.api.importer`)
- `ImportSource`, `ImportWarning`, `ImportException`, `ImportOptions`, `ImportResult`, `ImportUtils`
- `NormalizedCollection`, `NormalizedRequest`, `NormalizedEnvironment`, `NormalizedVariable`
- `spi/CollectionImporter.java` — SPI for format-specific parsers
- `postman/PostmanImporter.java` — full Postman v2/v2.1 parser (url string & object form, query params, headers, body, all auth types, common test scripts)
- `bruno/BrunoParser.java` + `bruno/BrunoImporter.java` — Bruno `.bru` block parser that walks the collection folder

**IDE** (`com.ing.ide.main.mainui.components.apitester.importing`)
- `ReusableImportEngine.java` — maps a `NormalizedCollection` → reusable scenarios/test cases with hierarchy + conflict policy
- `ImportCollectionWizard.java` — Swing wizard (Source → Format → File chooser → Options → Result)
- `ImportCollectionAction.java` — orchestrates parse → import → report on a `SwingWorker`
- `ImportReportWriter.java` — writes Markdown report to `<project>/api/import-reports/`

### Modified files
- [APITester.java](IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/APITester.java) — Phase 0 refactor; new `convertRequestToReusable`; `assertResponseCode` now writes `@200` / `@201` so the engine treats it as a literal (matching the `@` convention) rather than a data-sheet reference. Pre-existing `@`-prefixed values are left untouched.
- [AppMenuBar.java](IDE/src/main/java/com/ing/ide/main/mainui/AppMenuBar.java) — `Tools → Import Collection → Postman | Bruno`.
- [FXMenuBar.java](IDE/src/main/java/com/ing/ide/main/fx/FXMenuBar.java) — same on the JavaFX bar.
- [AppActionListener.java](IDE/src/main/java/com/ing/ide/main/mainui/AppActionListener.java) — dispatch via existing `"Import Collection:Postman"` / `"Import Collection:Bruno"` convention (same as `Import SAP Recording:<lang>`).

### Sample assets
- [Resources/SamplePostmanCollection/INGenious_Sample_API.postman_collection.json](Resources/SamplePostmanCollection/INGenious_Sample_API.postman_collection.json) — 5 folders × 14 requests against `httpbin.org` & `jsonplaceholder.typicode.com`; exercises every parser branch.
- [Resources/SamplePostmanCollection/INGenious_Sample_API-Dev.postman_environment.json](Resources/SamplePostmanCollection/INGenious_Sample_API-Dev.postman_environment.json) — `baseUrl`, `httpbin`, `userId`, plus two `secret` values (imported as empty + warning).
- [Resources/SamplePostmanCollection/README.md](Resources/SamplePostmanCollection/README.md)

### Folder support & environments
- `PostmanImporter.parse` accepts either a JSON file **or a folder** — folder is scanned for `*.postman_collection.json` (with `*.json` schema-sniff fallback). Missing path → "path does not exist".
- `ImportCollectionWizard.chooseFile` uses `FILES_AND_DIRECTORIES` so directories aren't greyed out for Postman (matches Bruno).
- Environment auto-loading: any `*.postman_environment.json` in the same folder as the picked collection JSON is auto-loaded into `NormalizedCollection.environments`. Picking an environment JSON directly imports it as environment-only. `ReusableImportEngine` creates an `APIEnvironment` per entry when the wizard's **Import environments** checkbox is on. Postman `secret`-typed values import with empty values + a warning.

### Breaking changes
None. New code is additive; the Phase 0 refactor of `APITester.convertRequestToTestCase` keeps existing behaviour for the API-tab → test-case path. The `@200` change on `assertResponseCode` is forward-compatible.

### How to use
1. **Tools → Import Collection → Postman** (or **Bruno**).
2. Pick a collection file *or folder*.
3. Choose options (target reusable scenario name, conflict policy, import environments).
4. Click **Import** → a Markdown report is written under `<project>/api/import-reports/`.

---

## 11. Manage Devices — new tab, LambdaTest categorised capabilities

**Feature**
Devices/emulators added in **Configurations → Manage Browsers** used to live mixed in with browsers. They now have their own **Manage Devices** tab, and when **LambdaTest Device** is checked, capabilities are shown in **Mandatory / Optional** categories instead of a flat list.

**Key files**
- [DriverSettings.java](IDE/src/main/java/com/ing/ide/main/settings/DriverSettings.java) — adds the **Manage Devices** tab next to Manage Browsers; the in-row label says "Device" (not "Browser").
- [LambdaTestCapsPanel.java](IDE/src/main/java/com/ing/ide/main/settings/devices/LambdaTestCapsPanel.java) *(new)* — categorised capability editor (mandatory vs optional) when **LambdaTest Device** is checked. When unchecked, the default property set is shown.
- [Devices.java](Datalib/src/main/java/com/ing/datalib/settings/Devices.java), [Emulators.java](Datalib/src/main/java/com/ing/datalib/settings/Emulators.java), [emulators/Device.java](Datalib/src/main/java/com/ing/datalib/settings/emulators/Device.java), [emulators/Emulator.java](Datalib/src/main/java/com/ing/datalib/settings/emulators/Emulator.java) — model split between browser-style emulators (legacy) and the new dedicated devices.
- [MobileOR.java](Datalib/src/main/java/com/ing/datalib/or/mobile/MobileOR.java), [MobileObject.java](Engine/src/main/java/com/ing/engine/drivers/MobileObject.java) — wiring for runtime platform pickup (see §8).

**Breaking changes**
- New UI tab. Legacy emulator entries continue to load (see §16 for the migration plan).

---

## 12. Remote URL — auto-mask credentials on entry/paste

**Feature**
When a user types or pastes a LambdaTest mobile-hub URL of the form
`https://<username>:<accessKey>@mobile-hub.lambdatest.com/wd/hub`
the visible field is immediately replaced with `https://****:****@mobile-hub.lambdatest.com/wd/hub`. The real value is preserved internally and used by the engine.

**Change** — [DriverSettings.java](IDE/src/main/java/com/ing/ide/main/settings/DriverSettings.java)
- Added regex `REMOTE_URL_CRED_PATTERN` + helpers `maskRemoteUrl`, `hasCredentials`, `setRemoteUrlValue`, `syncAndMaskRemoteUrlField`.
- New `actualRemoteUrl` holds the real (unmasked) URL; the text field only ever shows the masked form once credentials are present.
- The existing `DocumentListener` detects credentials and masks them on the EDT via `SwingUtilities.invokeLater(...)`.
- [Encryption.java](Engine/src/main/java/com/ing/util/encryption/Encryption.java) used to cipher the value at rest.
- [WebDriverFactory.java](Engine/src/main/java/com/ing/engine/drivers/WebDriverFactory.java) reads the unmasked value at execution time.

**Breaking changes:** none — the on-disk format already supported the ciphered value.

**How to verify:** paste the URL above into Run Settings → the field shows the masked form immediately.

---

## 13. Manage Devices — cleaner, sectioned LambdaTest capabilities view

**Symptom**
The LambdaTest device caps view had too many fields in a single flat list, with sections separated only by `--- section name ---` rows.

**Change**
Beyond the section-aware UI above, this session also patched two correctness bugs surfaced by the new layout:

- [Datalib/.../settings/Devices.java](Datalib/src/main/java/com/ing/datalib/settings/Devices.java#L176) — default for `waitForIdleTimeout` is now `""` (blank). Empty values are skipped at send time, so no spurious `FALSE` boolean is sent to LambdaTest.
- [Engine/.../drivers/WebDriverFactory.java](Engine/src/main/java/com/ing/engine/drivers/WebDriverFactory.java) — added a `LONG_ONLY_CAPABILITIES` set and a key-aware `coerceCapabilityValue(capability, value)` overload. For `waitForIdleTimeout`, numeric input is forced to `Long` (not `Integer`/`Boolean`); non-numeric input falls through as a string instead of becoming `Boolean.FALSE`.

Other files involved: [LinkedProperties.java](Datalib/src/main/java/com/ing/datalib/util/data/LinkedProperties.java), [LambdaTestCapsPanel.java](IDE/src/main/java/com/ing/ide/main/settings/devices/LambdaTestCapsPanel.java), [DriverSettings.java](IDE/src/main/java/com/ing/ide/main/settings/DriverSettings.java).

**Breaking changes:** none.

---

## 14. Mobile Scroll — unified `scroll` for Android + iOS

Replaced the two separate `scrollinAndroid` / `scrolliniOS` actions with a single **normalized `scroll`** action that detects the active driver and dispatches.

**Change** — [Scroll.java](Engine/src/main/java/com/ing/engine/commands/mobile/Scroll.java#L127)
- **Input (`Data`)**: direction — `up | down | left | right`.
- **Condition (optional)**: target element
  - Android → visible text (uses `UiScrollable.scrollIntoView`)
  - iOS → `attribute=value` (e.g. `name=submitBtn`) passed to mobile gesture
- Driver type detected via `mDriver instanceof AndroidDriver/IOSDriver` (same pattern as [AppiumDeviceCommands.java](Engine/src/main/java/com/ing/engine/commands/mobile/AppiumDeviceCommands.java)).

**Breaking changes**
The old per-platform action names still exist (not deleted), but new test cases should use **`scroll`**. Recommended to migrate existing test cases over time.

---

## 15. Rename: "LambdaTest Capabilities" → "LambdaTest Grid Capabilities"

Plus a small auto-build-name behaviour change:

- In Run Settings UI ([INGeniousSettings.java](IDE/src/main/java/com/ing/ide/main/settings/INGeniousSettings.java)) the label is now **LambdaTest Grid Capabilities**.
- [WebDriverFactory.java](Engine/src/main/java/com/ing/engine/drivers/WebDriverFactory.java#L103-L116) — LambdaTest branch of `getEmulatorCapabilities` now auto-generates a `build` capability as `"<Scenario> - <executionStartTime>"` when none is supplied, mirroring the browser-testing logic in `PlaywrightDriverFactory.lambdaTestCapabilities`. User-supplied `build` continues to take precedence.

**Breaking changes:** none.

---

## 16. Phase-out Manage Browser → Emulators path

**Goal**
Devices added in **Manage Browser** (legacy) are deprecated in favour of **Manage Devices** (§11). Existing projects auto-migrate; the obsolete UI/path is removed in stages.

**Phase 5 cleanup (completed)**
- Deleted [ChromeEmulators.java](Engine/src/main/java/com/ing/engine/drivers/ChromeEmulators.java), its two test files, and `Resources/Configuration/chrome-emulators.json`.
- Removed `CHROME_EMULATOR_FILE` constant and `getChromeEmulatorsFile()` from [AppResourcePath.java](Engine/src/main/java/com/ing/engine/constants/AppResourcePath.java) plus its test.

**Intentionally kept** (back-compat)
- `Driver` / `Size` / `UserAgent` / `@JsonAnySetter` fields on [Emulator.java](Datalib/src/main/java/com/ing/datalib/settings/emulators/Emulator.java) — needed by Jackson to read existing `Emulators.json` without errors. Safe to drop in a future major version.
- [Emulators.java](Datalib/src/main/java/com/ing/datalib/settings/Emulators.java) — still required as the SAP entry holder.

Migration of existing projects happens transparently in [ProjectSettings.java](Datalib/src/main/java/com/ing/datalib/settings/ProjectSettings.java) (legacy emulator entries are read and surfaced under the new Manage Devices tab).

**Tests:** 930 tests, 0 failures across Datalib + Engine + IDE.

**Breaking changes**
- The **Add Emulator** button in Manage Browsers is gone — use **Manage Devices** instead.
- `ChromeEmulators` class and `chrome-emulators.json` are deleted. Custom integrations that referenced them must be updated.

---

## 17. Configurations menu reorganisation & renames

**Renames & moves**

| Before | After |
|---|---|
| Configurations → **Run Settings** | Configurations → **Settings** (inner sub-tab still called "Run Settings") |
| Configurations → **Browser Configuration** | Configurations → **Archetype Configurations** |
| **Extent Report Settings** tab | Removed; `dark` theme hardcoded |
| Settings → **Kafka SSL Configurations** | Archetype Configurations → **Kafka SSL Configurations** |

**Files touched**
- Swing menu/toolbar/listener: [AppMenuBar.java](IDE/src/main/java/com/ing/ide/main/mainui/AppMenuBar.java), [AppActionListener.java](IDE/src/main/java/com/ing/ide/main/mainui/AppActionListener.java), [AppToolBar.java](IDE/src/main/java/com/ing/ide/main/mainui/AppToolBar.java)
- JavaFX equivalents: [FXMenuBar.java](IDE/src/main/java/com/ing/ide/main/fx/FXMenuBar.java), [FXToolBar.java](IDE/src/main/java/com/ing/ide/main/fx/FXToolBar.java)
- [INGeniousSettings.java](IDE/src/main/java/com/ing/ide/main/settings/INGeniousSettings.java) + `.form` — dialog title; removed Extent Report tab; removed Kafka SSL tab (panel/field/load/save).
- [INGIcons.java](IDE/src/main/java/com/ing/ide/main/fx/INGIcons.java) — added `Settings` and `ArchetypeConfigurations` icon aliases.
- [ExtentSummaryHandler.java](Engine/src/main/java/com/ing/engine/reporting/impl/extent/ExtentSummaryHandler.java) — `initiateExtentReport("dark", ...)` hardcoded.
- [DriverSettings.java](IDE/src/main/java/com/ing/ide/main/settings/DriverSettings.java) — new `kafkaSSLPanel` field plus `buildKafkaSSLTab()`, `loadKafkaSSLConfigurations()`, `saveKafkaSSLConfigurations()`.
- [ProjectSettings.java](Datalib/src/main/java/com/ing/datalib/settings/ProjectSettings.java) — Kafka SSL config now persisted under the Archetype Configurations namespace (legacy location still read for back-compat).

**Breaking changes**
- The **Extent Report Settings** tab is gone — users who previously chose `light` will now always render `dark`.
- Kafka SSL config has moved between two persistence locations; legacy entries are read and re-written under the new location on first save.
- Action-command strings have changed for the two renamed menu items; any external automation that relied on the old commands needs updating.

**Build:** clean across all modules.

---

## 18. CLI override coverage audit + full CLI usage docs

**Goal**
Produce a single source of truth for which IDE settings can be overridden from the command line, what's missing, and how to invoke every flag/subcommand.

**Deliverable**
New repo-root document — [CLI_Override_Plan_And_Usage.md](CLI_Override_Plan_And_Usage.md). Three parts:

- **Part A — Where overrides are wired today.** All `-setEnv "<prefix>.<key>=<value>"` traffic flows through a single dispatcher, `ProjectRunner.overrideWithEnv()` in [Engine/src/main/java/com/ing/engine/execution/run/ProjectRunner.java](Engine/src/main/java/com/ing/engine/execution/run/ProjectRunner.java). Known prefixes today: `exe / run / user / tm / driver / capability.<browser> / db.<db> / context.<ctx> / api.<api> / kafkaSSl`.
- **Part B — Coverage matrix for the 13 IDE sub-items under **Settings** and **Archetype Configurations**.** 8 already overridable; 5 missing:
  1. Settings → **LambdaTest Grid Capabilities** (new `lambdatest.*` namespace)
  2. Archetype → **Manage Browsers** (browser registry / launch args / binary path → `browser.<b>.*` / `browserArg.<b>.*`)
  3. Archetype → **Manage Devices / Mobile** (new `device.<name>.*` namespace)
  4. AzureDevOps TestPlan → **Modules** (add/rename/delete via `tmModule.<mod>.*`)
  5. AzureDevOps TestPlan → **per-module options**
  Each item gets a concrete §B-1…§B-5 plan with the new namespace, plumbing point in `overrideWithEnv()`, optional typed flags on `ingenious run …`, and a unit-test sketch. Housekeeping items: `kafkaSSl` → `kafkaSsl` alias, a `config list-prefixes` help command, and a round-trip regression test.
- **Part C — Usage reference.** Global flags, modern subcommands (`project / run / config / report / server / shell / …`) sourced from [INGeniousCLI.java](Engine/src/main/java/com/ing/engine/cli/INGeniousCLI.java) and [Engine/src/main/java/com/ing/engine/cli/commands/](Engine/src/main/java/com/ing/engine/cli/commands), legacy flags still parsed by the older [CLI.java](Engine/src/main/java/com/ing/engine/cli/CLI.java) (`-run / -rerun / -setEnv / -setVar / -op_setHeadless / -setThreads / -latestExe* / …`), the override-prefix cheat sheet, recipes and exit codes.

**Files touched (read-only audit)** — [INGeniousCLI.java](Engine/src/main/java/com/ing/engine/cli/INGeniousCLI.java), [CLI.java](Engine/src/main/java/com/ing/engine/cli/CLI.java), [LookUp.java](Engine/src/main/java/com/ing/engine/cli/LookUp.java), [ConfigCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/ConfigCommand.java), [RunCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/RunCommand.java), [ProjectRunner.java](Engine/src/main/java/com/ing/engine/execution/run/ProjectRunner.java), [AppActionListener.java](IDE/src/main/java/com/ing/ide/main/mainui/AppActionListener.java), [AppMenuBar.java](IDE/src/main/java/com/ing/ide/main/mainui/AppMenuBar.java).

**Breaking changes:** none — this is a documentation + planning deliverable. The §B-1…§B-5 implementations will land in follow-up sessions.

**How to use:** open [CLI_Override_Plan_And_Usage.md](CLI_Override_Plan_And_Usage.md) and jump to the Part C recipes for the override syntax for any tab in Run Settings.

---

## 19. HTML report — color-formatted Response payload, separate Headers section, copy buttons

**Symptom**
In the detailed HTML report's **API Payload** overlay, the Request payload was syntax-highlighted but the Response payload rendered as plain unformatted text. Also, response headers were stuffed into the same `<pre>` as the body.

**Root cause**
The Engine writes `Response.txt` as:
```
<response body>

--- Response Headers ---
<HttpHeaders.toString()>
```
The full blob was passed to `formatPayload()`. Because the headers tail starts with `---` (not `{`, `[`, or `<`), the JSON/XML/text detection fell through to `escapeHtml()` and no coloring was applied.

**Fix** — split body from headers, format the body the same way as the Request, and render headers in their own grid section.

**Files**
- [Resources/Configuration/ReportTemplate/media/js/detailed.js](Resources/Configuration/ReportTemplate/media/js/detailed.js#L243-L313) — new `splitResponsePayload`, `parseHttpHeaders`, `renderHeadersSection`; `openPayloadModal` updated to split, format body via `formatPayload`, and append a dedicated **Response Headers** section.
- [Resources/Configuration/ReportTemplate/html/detailed-v2.html](Resources/Configuration/ReportTemplate/html/detailed-v2.html#L1232-L1290) — same logic mirrored into the Alpine.js component.

**Copy buttons** (same session, second feature)
Added a **Copy** button on each section header (Request Payload / Response Payload / Response Headers).
- [detailed.js](Resources/Configuration/ReportTemplate/media/js/detailed.js#L267-L334) — three new top-level helpers:
  - `encodePayloadForCopy(text)` — UTF-8-safe base64 encoding for embedding text in an HTML attribute.
  - `renderPayloadSectionHeader(title, copyText)` — renders title + a "Copy" button with the text embedded as `data-copy-text-b64`.
  - `copyPayloadFromButton(btn)` — global click handler; uses `navigator.clipboard.writeText` with a `document.execCommand('copy')` fallback (needed for `file://` reports where the async Clipboard API can be restricted). Flashes "Copied!" on the button for 1.5s.
- [openPayloadModal](Resources/Configuration/ReportTemplate/media/js/detailed.js#L375-L404) and the Alpine mirror in [detailed-v2.html](Resources/Configuration/ReportTemplate/html/detailed-v2.html#L1326-L1358) now build their Request/Response section headers via `renderPayloadSectionHeader`. `renderHeadersSection` produces a plain-text `Name: value\n…` block for copying.

**What gets copied**
| Section | Copied content |
|---|---|
| Request Payload | Pretty-printed JSON/XML/text (no HTML color spans) |
| Response Payload | Pretty-printed body only — headers tail excluded |
| Response Headers | Parsed `Name: value` per line (or raw block if parsing fails) |

**Breaking changes:** none. No rebuild required — both files live under `Resources/Configuration/ReportTemplate/` and are copied as-is into each report folder.

---

## 20. HTML summary — clickable rows (Tabulator v6 fix)

**Feature**
In the summary HTML report, clicking anywhere on a test case row now opens the detailed/step-level report. Previously only the Test Case **name** column was a link.

**Root cause of the first attempt**
The initial edit added `rowClick:` inline in `new Tabulator({...})`. Tabulator **v6.2.1** (bundled in the report templates) **dropped the `rowClick` config option**; v6 requires `table.on('rowClick', …)` after instantiation. The inline config was silently ignored.

**Fix** — [Resources/Configuration/ReportTemplate/html/summary-v2.html](Resources/Configuration/ReportTemplate/html/summary-v2.html)
- Removed the inline `rowClick:` config from both `new Tabulator(...)` calls.
- Added `this.executionsTable.on('rowClick', …)` after the **Single View** table is created — navigates via `getDetailedLink(data)`.
- Added `this.groupTable.on('rowClick', …)` after the **Group View** table is created — navigates to `detailed.html?SC=<scenario>&TC=<testCase>&BRO=ALL`.
- Both handlers skip navigation when the click target is inside an `<a>` so Cmd/Ctrl-click on the existing Test Case link still opens in a new tab.
- Added `cursor: pointer` to `.tabulator-row` so the row signals it is clickable.

The runtime template copy at `Dist/release/Configuration/ReportTemplate/html/summary-v2.html` was refreshed in the session so the next run picked up the fix without a Maven rebuild.

**Breaking changes:** none. Older report folders keep their pre-fix template (templates are copied per-run and frozen at run time) — only **new runs** get clickable rows.

---

## 21. HTML report — in-page Console Viewer with working filter under `file://`

**Feature**
Every HTML report now ships with a floating dark **Console** pill button (bottom-left, hidden on print) that opens a full-screen modal showing the run's `console.txt` — monospaced, scrollable, dark theme, with a **Filter lines…** input that hides non-matching lines and scrolls to the first match.

**The journey** (this was the most iterated-on item of the period)

### 21.1 Initial implementation
A console icon + modal was added to all 6 report templates:
- summary.html, summary-v2.html, detailed.html, detailed-v2.html, testCase.html, testCase-v2.html under [Resources/Configuration/ReportTemplate/html/](Resources/Configuration/ReportTemplate/html).
- The modal loads `console.txt` (already produced by [ConsoleReport.java](Engine/src/main/java/com/ing/engine/reporting/impl/ConsoleReport.java#L27-L48)) and renders each line as `<span class="ing-line">` inside `#ing-console-pre`.
- The filter is a live `input` handler — substring case-insensitive — that toggles each span's `hidden` flag and highlights matches.

### 21.2 Reduce launcher log noise
Follow-up: the 5 `Trying to Open …` / `Trying to use …` info lines from [DesktopApi.java](Engine/src/main/java/com/ing/engine/support/DesktopApi.java#L202-L207) were emitted every time the report auto-opened. The `logOut` helper was demoted from `LOG.info` to `LOG.debug`, so they're suppressed by default but available when log level is raised.

### 21.3 Unify all banner versions on `${project.version}`
Second follow-up: the IDE banner showed `🚀 Test Automation Framework v2.3.1` while the Engine banner showed `INGenious Playwright Studio engine : 3.0.0-preview`. Four hardcoded version strings were replaced with a single source of truth — Maven's `${project.version}` filtered into `build.properties` and exposed via `SystemDefaults.getBuildVersion()`.

- New file [Engine/src/main/java/com/ing/engine/cli/INGeniousVersionProvider.java](Engine/src/main/java/com/ing/engine/cli/INGeniousVersionProvider.java) — a small picocli `IVersionProvider` so `ingenious --version` also returns the Maven version.
- Touched: [INGeniousCLI.java](Engine/src/main/java/com/ing/engine/cli/INGeniousCLI.java), [Main.java](IDE/src/main/java/com/ing/ide/main/Main.java), [About.java](IDE/src/main/java/com/ing/ide/main/ui/About.java), [Control.java](Engine/src/main/java/com/ing/engine/core/Control.java), [build.properties](IDE/src/main/resources/ui/resources/build.properties), [build.properties](Engine/src/main/resources/engine/build.properties).
- Bonus fix: `About.java` previously NPE'd if `getBuildVersion()` ran before `About.init()` (`Main.printBanner()` runs before `initCommonDependencies()`). `About` now lazy-loads its `Properties` via a private synchronized `load()` helper, safe to call any time.

### 21.4 "Filter lines…" did nothing under `file://`
The filter never fired when opening reports from disk. Iterated through three loader strategies — `fetch`, `XMLHttpRequest`, hidden iframe + `contentDocument.body.innerText` — all of which Chrome blocks for cross-origin `file://` reads. The visible-iframe fallback kicked in, `#ing-console-pre` stayed hidden, and the filter had no `<span>`s to act on.

### 21.5 Definitive fix — inline-embed `console.txt` at end-of-run
Server rewrites the HTML at the source instead of fetching at view time.

**Server-side (Java):**
- New class [Engine/src/main/java/com/ing/engine/reporting/ConsoleEmbedder.java](Engine/src/main/java/com/ing/engine/reporting/ConsoleEmbedder.java) — at end-of-run, reads `console.txt` and rewrites every `*.html` in the results folder, inlining the log into a `<script type="text/plain" id="ing-console-data">…</script>` data-island. Uses a `</script>`-escape pass (`</script` → `<\/script`) so the data is safe to embed.
- [SummaryReport.finalizeReport()](Engine/src/main/java/com/ing/engine/reporting/SummaryReport.java) now invokes `ConsoleEmbedder.embedInto(...)` after all other report handlers have written their HTML.

**Client-side (all 6 templates):**
- Each template carries a placeholder `<script type="text/plain" id="ing-console-data">__INGENIOUS_CONSOLE_DATA__</script>`.
- A new `viaInline()` loader runs **first** in the chain `viaInline → viaFetch → viaXHR → viaHiddenIframe → fallbackIframe`. It reads the script's `textContent`, undoes the `</script` protection, and populates `#ing-console-pre` directly — no network call, no browser security restriction.

**Filter UX polish** (same session)
| State | Visual feedback |
|---|---|
| Matches found | Input border **green**, hover tooltip `"N matches"`, non-matching lines hidden, matches highlighted (light indigo), first match scrolled into view |
| No matches | Input border **red** |
| Cleared | Border resets, all lines restored |
| Loader fell to visible iframe (pre-fix only) | Input border **amber** — diagnostic signal |

**Sanity-check command**
```bash
grep -c "__INGENIOUS_CONSOLE_DATA__" <new-run>/summary-v2.html
# 0 → placeholder replaced → embed worked → filter will work.
# 1 → still has placeholder → embed didn't run (re-check the run finished cleanly).
```

**Breaking changes:** none. Existing reports keep their old (broken) viewer because templates are frozen per-run; only newly generated reports get the inline embed.

---

## 22. API Workbench — right-click response to auto-build path assertions (JSON + XPath)

**Feature**
After firing a request, right-click anywhere in the **Response → Body** view. INGenious computes the path under the cursor (JSONPath for JSON bodies, XPath for XML bodies) and shows a popup with:

- A header row showing the path with a **copy-to-clipboard** icon (Material Design `content_copy`)
- *Assert path exists*
- *Assert value equals "<preview>"* (when the click is on a leaf)
- *Assert value contains "<preview>"*
- *Assert value (custom)…* — opens a dialog with full operator choice (EQUALS / NOT_EQUALS / CONTAINS / NOT_CONTAINS / STARTS_WITH / ENDS_WITH / MATCHES_REGEX / GREATER_THAN / LESS_THAN / EXISTS / NOT_EXISTS)
- Selection-based fallback: *Assert body contains "<selection>"*

Picking an option appends an `APIAssertion` to the current request, persists via `forceSaveCurrentRequest()`, and shows a notification. Re-send the request and the assertion appears in the **Test Results** tab. When the request is converted to a test case, each assertion becomes a `STRUCTUREDDATA` step that targets the OR-resolved path.

### 22.1 New IDE locators
- **New** [IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/util/JsonPathLocator.java](IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/util/JsonPathLocator.java) — recursive-descent JSON parser. Given a character offset, returns the deepest JSONPath enclosing it (e.g. `$.user.address.city`, `$.items[2]`) plus the primitive value at that location (string/number/bool/null, unescaped). Tolerates malformed JSON and pretty-printed whitespace.
- **New** [IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/util/XPathLocator.java](IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/util/XPathLocator.java) — character-offset → XPath resolver, mirrors `JsonPathLocator`:
  - Walks the XML, tracks per-parent sibling counts, builds positional XPath like `/root/items/item[2]/id`.
  - Records spans for opening tag, closing tag, full element, text node, and each attribute. Picks the *smallest* enclosing span (deepest match) at the click offset.
  - Tolerates declarations (`<?xml ?>`), DOCTYPE, comments, CDATA, namespaced names, self-closing tags, single/double-quoted and unquoted attribute values.

### 22.2 IDE wiring — ResponsePanel
[IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/response/ResponsePanel.java](IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/response/ResponsePanel.java)
- New `installAssertionPopup` installs the right-click `JPopupMenu`. Menu is built dynamically from the click position — different items for JSON vs XML bodies (detected via `SYNTAX_STYLE_*`).
- New `buildPathHeader(label, path, owner)` returns a `JPanel` row with a disabled label (`Path: …`) and a transparent **copy** button (FontIcon Material Design content_copy). Click copies the raw path (no `Path:` prefix) to the system clipboard and shows an info notification. Reused for both JSON and XPath headers.
- New `promptCustomXPathAssertion(path, currentValue)` mirrors the JSON custom-assertion dialog.

### 22.3 Datalib — new assertion factories
[Datalib/src/main/java/com/ing/datalib/api/APIAssertion.java](Datalib/src/main/java/com/ing/datalib/api/APIAssertion.java)
- `jsonPath(path, op, expected)` / `jsonPathExists(path)` (already existed)
- **New** `xPath(path, op, expected)` → builds an `XPATH` assertion
- **New** `xPathExists(path)` → builds an `XPATH` + `EXISTS` assertion

### 22.4 Engine — full operator set for both JSON and XML paths
[Engine/src/main/java/com/ing/engine/commands/structuredData/StructuredData.java](Engine/src/main/java/com/ing/engine/commands/structuredData/StructuredData.java) — added 7 new JSON actions and 7 matching XPath actions to cover the operator menu:

| Action | Operator | Input |
|---|---|---|
| `assertJsonPathResultStartsWith` / `assertXmlPathResultStartsWith` | STARTS_WITH | expected prefix |
| `assertJsonPathResultEndsWith` / `assertXmlPathResultEndsWith` | ENDS_WITH | expected suffix |
| `assertJsonPathResultMatchesRegex` / `assertXmlPathResultMatchesRegex` | MATCHES_REGEX | Java regex |
| `assertJsonPathResultGreaterThan` / `assertXmlPathResultGreaterThan` | GREATER_THAN | numeric threshold |
| `assertJsonPathResultLessThan` / `assertXmlPathResultLessThan` | LESS_THAN | numeric threshold |
| `assertJsonPathExists` / `assertXmlPathExists` | EXISTS | — (ignored) |
| `assertJsonPathNotExists` / `assertXmlPathNotExists` | NOT_EXISTS | — (ignored) |

All follow the established conventions:
- `Data` = path expression (resolved from the OR via the `Object`/`Reference` columns)
- `getInputValue(Input)` = expected value (strips the `@` prefix added by the test-step generator)
- `PASSNS` / `FAILNS` / `DEBUG` reporting via `Report.updateTestLog(...)`
- Numeric comparisons report `FAILNS` (not a swallowed exception) when either side isn't parseable
- `assertJsonPathExists` distinguishes between `PathNotFoundException` (missing → FAIL), empty collection (treated as missing → FAIL), and any other value (PASS); `assertJsonPathNotExists` is the inverse
- `assertXmlPathExists` / `NotExists` use `NodeList.getLength()`

Two private helpers (`readXmlPathValue`, `readXmlPathNodes`) factor out the parser/XPath boilerplate for the new XPath actions.

### 22.5 IDE generator — `APITester.pickPathActionName`
[IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/APITester.java](IDE/src/main/java/com/ing/ide/main/mainui/components/apitester/APITester.java)
Replaced the hard-coded "Contains → Contains, else Equals" mapping with a complete switch:

```java
private String pickPathActionName(boolean isXPath, APIAssertion.Operator op) { ... }
```

maps every supported operator to the matching engine action; unsupported operators (e.g. `IS_NULL`, `IS_ARRAY`) are skipped with a warning so test-case generation never produces an invalid action name.

Also: when converting an API-workbench request to a test case, assertion **values** are now placed in the **Input** column prefixed with `@` (matching the existing convention for literals), and the path is stored as a **Structured Data** object in the API OR with a page named after the request (Reference column = `[Project] <RequestName>`).

### 22.6 Engine — SObject init for API-only / mixed runs
The new STRUCTUREDDATA assertions were failing for API-only tests with `SObject=null`. Root cause: `SObject` was only initialised in [CommandControl.java](Engine/src/main/java/com/ing/engine/core/CommandControl.java) when `Page.page != null` (Playwright path). For Webservice-only tests, [Task.java](Engine/src/main/java/com/ing/engine/core/Task.java#L162) falls into the `else` branch and creates a *non-null* `webDriver = new WebDriverCreation()` wrapper with no real driver, so the `Commander.webDriver != null` check in `Command(cc)` took the WebDriver branch and never copied `SObject`.

**Two-part fix:**
1. [CommandControl.java](Engine/src/main/java/com/ing/engine/core/CommandControl.java) — after the driver-specific blocks, **unconditionally** instantiate `SObject = new StructuredDataObject()` if still null. STRUCTUREDDATA is driver-agnostic (OR lookup uses the project's repository, not a `Page`).
2. [Engine/src/main/java/com/ing/engine/commands/browser/Command.java](Engine/src/main/java/com/ing/engine/commands/browser/Command.java) — after the three branches, copy `SObject` from `Commander.SObject` if not already set, so all driver modes (web / mobile / SAP) propagate it. This also makes mixed flows work — a browser/mobile test that fires API steps with SObject path assertions in the middle now resolves correctly.

**Breaking changes**
- None on disk. The new operator-mapped engine actions are additive; existing `assertJsonPathResultEquals` / `assertJsonPathResultContains` / `assertXmlPathResultEquals` / `assertXmlPathResultNotEquals` are unchanged.
- Generated test cases for new requests use the richer `pickPathActionName` mapping — previously-generated test cases keep their hand-edited actions.

**How to verify**
1. In API Workbench, fire any GET that returns JSON. Right-click a value → *Assert value equals*. Path appears in the header with a copy icon.
2. Repeat for an XML body — XPath shown instead of JSONPath.
3. Convert the request to a test case → step uses `assertJsonPathResultEquals` (or the relevant variant) and Input is `@<expected>`.
4. Run the test case — no `SObject=null` errors, regardless of whether a browser is launched.

---

## 23. CLI overrides — implementation of all missing prefixes + typed flags + `config prefixes` help

**Goal**
Make every IDE Configurations sub-item overridable from the command line in a standardised way, and produce comprehensive end-user documentation. Implementation of the §B-1…§B-5 + §C-3 plan from section 18's planning doc.

**What changed**

1. **New override prefixes** in [Engine/src/main/java/com/ing/engine/execution/run/ProjectRunner.java](Engine/src/main/java/com/ing/engine/execution/run/ProjectRunner.java) — `overrideWithEnv()` refactored to delegate every `-setEnv` entry through a private `applyOverride(String key, String value)` switch, and extended with:
   - `lambdatest.<key>` — LambdaTest Grid capabilities (Settings → LambdaTest Grid Capabilities).
   - `browser.<browser>.<key>` — arbitrary per-browser property; create-on-missing alias of `capability.*`.
   - `browserArg.<browser>.<n>=<arg>` — indexed per-browser launch flag (writes `arg.<n>` into the browser's capability bag).
   - `device.<name>.<key>` — Manage Devices entry. Reserved keys: `RemoteURL`, `LambdaTest`, `__enabled`. Anything else lands in the device's capability map (consistent with Mobile/Appium usage in `CapabilitiesTest`).
   - `emulator.<name>.<key>` — alias of `device.*` for back-compat with the phased-out Emulators tab.
   - `tmModule.<module>.<key>` — AzureDevOps TestPlan per-module option. Creates the module on first use. Reserved key: `__enabled`.
   - `kafkaSsl.<key>` — canonical-cased alias of the legacy `kafkaSSl.<key>`.
   - A new `public static final LinkedHashMap<String,String> PREFIX_CATALOGUE` documents every prefix; consumed by the new `config prefixes` help command so help stays in sync with the dispatcher automatically.
   - Existing `capability.<browser>.<key>` switched to a new `Capabilities.getOrCreateCapabiltiesFor(String)` helper (avoids `NullPointerException` when overriding a browser that has no capability bag yet) and uses `split("\\.", 3)` for safer key parsing when keys themselves contain `.` (e.g. `Chrome.goog:chromeOptions.args`).

2. **Datalib helpers** — three small additions that the dispatcher relies on:
   - [Datalib/src/main/java/com/ing/datalib/settings/Capabilities.java](Datalib/src/main/java/com/ing/datalib/settings/Capabilities.java) — `LinkedProperties getOrCreateCapabiltiesFor(String browserName)`.
   - [Datalib/src/main/java/com/ing/datalib/settings/Devices.java](Datalib/src/main/java/com/ing/datalib/settings/Devices.java) — `Device getOrCreateDevice(String name)`.
   - [Datalib/src/main/java/com/ing/datalib/settings/TestMgmtModule.java](Datalib/src/main/java/com/ing/datalib/settings/TestMgmtModule.java) — `void setOption(String moduleName, String optionName, String value)` that creates the module if missing and either replaces an existing `Option` or appends a new one.

3. **Typed CLI override flags** — new Picocli `@Mixin` [Engine/src/main/java/com/ing/engine/cli/commands/OverrideOptions.java](Engine/src/main/java/com/ing/engine/cli/commands/OverrideOptions.java) exposes 14 repeatable flags (`--set-env`, `--driver`, `--user`, `--tm`, `--capability`, `--browser-set`, `--browser-arg`, `--db`, `--context`, `--api`, `--kafka-ssl`, `--lambdatest-cap`, `--device`, `--tm-module`). Each splices the right prefix and writes `k=v` into `SystemDefaults.EnvVars` so behaviour is identical to `-setEnv`.

4. **Wired into every `run` subcommand** — [Engine/src/main/java/com/ing/engine/cli/commands/RunCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/RunCommand.java) — `TestCaseRunCommand`, `TestSetRunCommand`, `TagsRunCommand`, `RerunCommand` all gained `@Mixin OverrideOptions overrides;` and call `overrides.applyAll();` as the first line of `call()`. Single mixin = single source of truth = identical spelling everywhere.

5. **New `ingenious config prefixes` help command** — [Engine/src/main/java/com/ing/engine/cli/commands/ConfigCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/ConfigCommand.java) — `PrefixesCommand` inner class iterates `ProjectRunner.PREFIX_CATALOGUE`, prints it as an aligned 2-column table followed by examples. Registered in the `subcommands = {…}` array of the outer `@Command`.

6. **Comprehensive end-user CLI usage guide** — [CLI_Override_Plan_And_Usage.md](CLI_Override_Plan_And_Usage.md) completely rewritten as the single source of truth. New table of contents: Quick start → Global flags → Every command + subcommand + every flag with examples → Override prefix catalogue (mirrors `PREFIX_CATALOGUE`) → Typed override flag mapping table → Legacy CLI reference → Recipes → Exit codes → Coverage matrix (all 16 rows now ✅).

**Override precedence (unchanged)** — `project files on disk  <  app.* env vars  <  -setEnv / typed flags`. All `-setEnv` values are kept in memory only (`SystemDefaults.EnvVars`) and disappear at the end of the run; they never modify the project files.

**How to verify**

```bash
# 1. Discover everything from the CLI itself:
ingenious config prefixes

# 2. New typed flags surface in help for every run subcommand:
ingenious run testcase --help
ingenious run testset --help
ingenious run tags --help
ingenious run rerun --help
```

Each prints the full 14-flag override mixin.

**Build status**
`mvn -pl Datalib,Engine -am install -DskipTests` → BUILD SUCCESS across `ingenious / Datalib / TestData - Csv / Engine`. The previously-flagged JDT classpath warnings on `ProjectRunner.java` are VS Code language-server cache only — Maven resolves cleanly.

**Breaking changes:** none.
- All new prefixes are additive.
- Existing `kafkaSSl.<key>` continues to work; `kafkaSsl.<key>` is its canonical-cased alias.
- All existing `-setEnv` users keep working — no behaviour changes for `exe / run / user / tm / driver / capability / db / context / api`.
- The new typed flags are repeatable Picocli options; absent flags = no overrides applied (identical to today).

**Files touched**
- [Datalib/src/main/java/com/ing/datalib/settings/Capabilities.java](Datalib/src/main/java/com/ing/datalib/settings/Capabilities.java)
- [Datalib/src/main/java/com/ing/datalib/settings/Devices.java](Datalib/src/main/java/com/ing/datalib/settings/Devices.java)
- [Datalib/src/main/java/com/ing/datalib/settings/TestMgmtModule.java](Datalib/src/main/java/com/ing/datalib/settings/TestMgmtModule.java)
- [Engine/src/main/java/com/ing/engine/execution/run/ProjectRunner.java](Engine/src/main/java/com/ing/engine/execution/run/ProjectRunner.java)
- [Engine/src/main/java/com/ing/engine/cli/commands/OverrideOptions.java](Engine/src/main/java/com/ing/engine/cli/commands/OverrideOptions.java) (new)
- [Engine/src/main/java/com/ing/engine/cli/commands/RunCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/RunCommand.java)
- [Engine/src/main/java/com/ing/engine/cli/commands/ConfigCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/ConfigCommand.java)
- [CLI_Override_Plan_And_Usage.md](CLI_Override_Plan_And_Usage.md) (rewritten)

---

## 24. `project validate` — quality dashboard with per-test-case & per-reusable Kind classification

**Feature**
A new `ingenious project validate <project>` command renders a colour-coded health dashboard with six scored dimensions (OR modernisation, Test-case modernisation, Modularity, Data parameterisation, Test-set coverage, Tag adoption), an overall A–F grade, and two breakdown tables (Per-Test-Case Quality / Per-Reusable-Component Quality). Each row carries a **Kind** column that classifies the test case / reusable as `UI`, `API`, `Mobile`, `DB`, `Kafka`, a combination like `UI + API`, or `Unknown` when no archetype applies.

**Kind classification rules** (per user spec)
- `UI` — only browser steps or step objects that resolve to a Web OR
- `API` — only Webservice steps or step objects from the StructuredData OR
- `Mobile` — only Mobile steps or step objects from the Mobile OR
- `Kafka` — only Kafka steps
- `DB` — only Database steps
- Combination (e.g. `UI + API`, `Mobile + DB`) when a mix is detected — joined with ` + ` in archetype-discovery order
- `Unknown` — when none of these archetypes apply (e.g. generic `print` actions on the `General` object)

### Implementation
- **`TestCaseQuality`** inner class in [Engine/src/main/java/com/ing/engine/cli/commands/ProjectCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/ProjectCommand.java) carries per-archetype step counters (`webSteps`, `apiSteps`, `mobileSteps`, `dbSteps`, `kafkaSteps`) plus parameterisation/reuse counters. `kindLabel()` joins all non-zero archetypes; `isApi()` is derived from `apiSteps > 0` so JSON-payload scoring stays gated on real API steps.
- **`classifyObjectType(Project, String obj)`** — switches on the lowercase step object for built-in archetypes (`browser→Web`, `webservice→API`, `mobile→Mobile`, `database/db→DB`, `kafka/queue→Kafka`). When the cell is a project-defined name (page name, object-group name, or individual object name) it looks the name up in a lazily-built OR index.
- **`orNameIndex(Project)`** — cached per `Project` in a `Collections.synchronizedMap(new IdentityHashMap<>())`. Walks every OR (`WebOR`, `SapOR`, `MobileOR`, `StructuredDataOR`, plus their `Shared*OR` siblings via `getWebSharedOR/getMobileSharedOR/getSapSharedOR/getStructuredDataSharedOR`) and adds **page names, object-group names, and individual object names** to a `Map<String,String>` (lowercase → archetype). Uses reflection (`indexWeb/indexMobile/indexStructured` share the same shape since every OR exposes `getPages() → page.getObjectGroups() → group.getObjects()`).
- **`mergeReusableInto`** — when a reusable-component call is encountered, per-archetype counters propagate from the inner test case into the parent's counters so the parent's Kind reflects everything the run touches.
- **Rendering** — `renderTestCaseTable` and `renderReusableComponentTable` print the Kind column via `colourKind(s, kindRaw)` (`Unknown→dim`, combinations bold, `UI→cyan`, `API→magenta`, `Mobile→blue`, `DB→yellow`, `Kafka→green`).
- **Scoring details** — six sub-scores averaged into the overall grade. `OR modernisation` and `Test-case modernisation` track XML→YAML / CSV→YAML migration progress. `Modularity` saturates at 30% reusable steps. `Data parameterisation` is the fraction of step inputs that are **not** hard-coded literals (i.e. they reference a datasheet column, variable, or expression rather than starting with `@`). Tag adoption hits 100 at 50% tagged coverage.

### Bug fixes folded in
- The OR-name index initially keyed only on **page** names, so reusables whose step `object:` cells referenced individual object names (e.g. `Describe`, `Send`, `First name`) classified as `Unknown`. The index now includes object-group names and individual object names too, so a step like `object: Send` inside the `ContactUs` page correctly classifies as `UI`.
- Test cases are constructed lazily — `tc.loadTestCaseTableModel()` is called inside a `Silencer.aroundProjectLoad()` block before counting steps. Same lazy-load handling is applied to releases / test sets (see §26).

**How to verify**
```bash
ingenious project validate ING-Public-Web
```
Expect a banner like:
```
Per-Reusable-Component Quality
──────────────────────────────
  StepDefinitions/User fills up personal ... UI    7    100%   ·
  StepDefinitions/submits relevant question  UI    3    100%   ·
```

**Breaking changes:** none. New subcommand only.

---

## 25. `project upgrade` — 4-step interactive modernisation wizard

**Feature**
`ingenious project upgrade <project>` walks an old project through every modernisation step that the validate command might flag, **interactively**. Each step is opt-in (`y/N`), with a `-y / --yes` flag to accept all defaults for unattended CI use and a `--dry-run` flag to preview without writing anything.

The four steps are:
1. **Convert XML object repositories → per-page YAML** — invokes `ObjectRepository.saveAsYaml()` and lets the loader auto-archive the originals under `ProjectXMLOR/` and `SharedXMLOR/`.
2. **Convert CSV test cases → YAML** — delegates to `ProjectMigrator.migrate(projectDir, dryRun, keepBackup)` and reports `converted / skippedYamlExisted / conflicts / errors`. With `--keep-backup`, originals land under `.migration-backup/`.
3. **Delete deprecated legacy files** — `IOR.object`, `ReusableComponent.xml`, `ReusableComponent.xml.bak`, `ProjectXMLOR/`, `SharedXMLOR/`, plus the small (<500 B) stub XML files left behind by `ObjectRepository.init()` after a fresh OR conversion (caught by a `rescanDeprecated()` pass between steps 1 and 3).
4. **Relocate YAML reusables found under `TestPlan/`** — any YAML where `TestCaseYaml.isReusable()` returns true is moved into `ReusableComponents/` while preserving its relative path.

### Implementation
- **New file** [Engine/src/main/java/com/ing/engine/cli/commands/UpgradeCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/UpgradeCommand.java) — Picocli `@Command(name="upgrade")`. Registered as a subcommand of `project` in [ProjectCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/ProjectCommand.java).
- **`UpgradePlan`** inner class precomputes everything that needs doing: `boolean hasXmlOR`, `List<File> xmlORFiles`, `int csvTestCases`, `List<File> deprecated`, `List<File> mislocatedReusables`. The wizard always shows the user *what* will happen before asking.
- **`scan(File)`** — walks `IOR.object / MOR.object / StructuredDataOR.object / SapOR.object` plus `../Shared/SharedWebObjects/SharedOR.object` etc. (only > 0 B files count as live XML); recursively counts CSVs under TestPlan + ReusableComponents + TestLab; collects mislocated reusables by parsing TestPlan YAMLs through `TestCaseYaml.isReusable()`.
- **`resolveProjectDir(String)`** accepts an absolute path, a path relative to the current directory, or a bare project name resolved against `<cwd>/Projects/<name>`.
- **`askYes(cli, question, true)`** — `BufferedReader` over `stdin`; `--yes` short-circuits to accept all.
- **Idempotency** — running the wizard twice on an already-modernised project is a no-op (every step reports `nothing to do`).

**Live-tested** against `Projects/TutorialUpgradeTest`: 33 changes applied on first run (XML→YAML, CSV→YAML for the test cases, 4 deprecated files removed, 2 mislocated reusables relocated). `project validate` test-case modernisation jumped 0→100/100. Second run = no-op.

**How to verify**
```bash
ingenious project upgrade ING-Public-Web --dry-run            # preview
ingenious project upgrade ING-Public-Web --keep-backup         # safe live run
ingenious project upgrade ING-Public-Web -y                    # unattended
```

**Breaking changes:** none. New subcommand only; every destructive step is gated by an explicit y/N prompt.

---

## 26. `validate` Test-set coverage — correct scoring at test-case granularity

**Symptoms (two separate bugs)**

1. A project with a fully-populated test set was scoring **70/100** for Test-set coverage instead of 100.
2. After fixing (1), a project where the only test set covered **1 of 2** test cases was still scoring **100/100** instead of partial.

**Root causes & fixes** — both in [Engine/src/main/java/com/ing/engine/cli/commands/ProjectCommand.java](Engine/src/main/java/com/ing/engine/cli/commands/ProjectCommand.java)

**(1) Lazy load of test-set executions.** `Release.loadTestSets()` only creates empty `TestSet` shells; the actual execution rows are loaded lazily via `TestSet.loadTestSetTableModel()`. The validate command was reading `ts.getTestSteps()` without first triggering that load, so `scenariosInTestSets` was always empty → coverage component always 0 → score capped at the base 70. **Fix:** call `ts.loadTestSetTableModel()` (wrapped in `Silencer.aroundProjectLoad()`) before iterating execution steps.

**(2) Coverage measured at the wrong granularity.** The score was based on `scenariosInTestSets.size() / scenarioCount`. Any one execution row covering a scenario credited the whole scenario, regardless of how many sibling test cases it actually had. **Fix:** track coverage as `Set<String> testCasesInTestSets` using `"<scenario>/<testCase>"` keys, and switch the scoring formula to **base 50 + up to 50 × (covered test cases / total test cases)** so partial coverage now feels genuinely partial:

```java
private static int scoreTestSets(int totalTestSets, int totalTestCases,
                                 Set<String> testCasesInTestSets) {
    if (totalTestSets == 0) return 0;
    if (totalTestCases == 0) return 50;
    double coverage = Math.min(1.0,
            testCasesInTestSets.size() / (double) totalTestCases);
    return (int) Math.round(50 + coverage * 50);
}
```

A new warning section also surfaces each uncovered test case so users see exactly what to add:
```
⚠ Test case not in any test set: Contact Us/User should be able to send a question on Sustainability
```

**How to verify**

Single test set covering one of two test cases:
```
• Test-set coverage  ███████████████░░░░░  75/100
```
Single test set covering both:
```
• Test-set coverage  ████████████████████  100/100
```

**Breaking changes**
- Scoring formula tightened. Projects with a trivial single-execution test set that previously coasted to 70/100 will now reflect their real test-case coverage. The threshold for an A grade is unchanged (≥90 overall average), so projects that genuinely cover all their cases land on the same grade as before.

---

## 27. CLI dispatcher — `object` / `testset` / `data` now route to picocli

**Symptom**
`ingenious object list -p <path>`, `ingenious testset list -p <path>`, and `ingenious data list -p <path>` all printed
```
[SEVERE] com.ing.engine.cli.LookUp exe:Unrecognized option: -p
```
while `ingenious scenario list -p <path>` worked fine.

**Root cause**
[Engine/src/main/java/com/ing/engine/core/Control.java](Engine/src/main/java/com/ing/engine/core/Control.java) `isNewCLICommand(String[])` decides between the new Picocli CLI and the legacy `-run`-style CLI based on the first argument. Its `newCommands[]` whitelist was missing `object` / `objects` / `or` / `testset` / `data`, so those three command families were silently falling through to the legacy parser, which doesn't understand `-p`.

**Fix**
Added the missing tokens to the dispatcher whitelist:

```java
String[] newCommands = {
    "project", "scenario", "testcase", "testset",
    "object", "objects", "or",
    "data", "action", "actions",
    "run", "report", "config", "server",
    "shell", "interactive", "repl",
    "help",
    "--help", "-h",
    "--version", "-v", "-V"
};
```

(`upgrade` is intentionally excluded from this list — it's a subcommand of `project`, not a top-level verb.)

**Breaking changes:** none. The three command families now reach the picocli handlers that were already wired up; the legacy CLI is unaffected for first-arg tokens it does understand.

---

## 28. Single-source CLI reference document

**Deliverable** — [INGenious_CLI_Reference.md](INGenious_CLI_Reference.md) at the repo root.

A single document containing the complete CLI usage reference: every command, every subcommand, the global flags they share, and a representative real-output sample per category. Sections:

1. Top-level entry & global flags (`-h`, `-v`, `--json`, `--yaml`, `--no-color`, `-p`)
2. `project` — `list`, `info`, `validate` (with full scoring table + Kind legend), `create`, `upgrade`
3. `scenario` — `list`, `info`, `create`, `delete`
4. `testcase` — `list`, `show`, `create`, `validate`
5. `testset` — `list`, `show`, `create`, `add`
6. `object` (aliases `objects`, `or`) — `list`, `show`, `search`, `create`
7. `data` — `list`, `show`, `get`, `set`, `import`
8. `action` (alias `actions`) — `list`, `search`, `info`, `categories`
9. `run` — auto-detect + `testcase`, `testset`, `tags`, `rerun`, plus the common run flags (`--headless`, `--parallel`, `--set-env`, `--capability`)
10. `report` — `latest`, `history`, `show`, `export`, `compare`
11. `config` — `show`, `get`, `set`, `drivers`, `reset`, `prefixes` (full list of `--set-env` namespaces)
12. `server` — `mcp`, `rest`, `status`
13. `shell` — interactive REPL with the ASCII banner
14. Legacy `-run` shim with a side-by-side modern equivalent
15. Quick recipe cheat-sheet (validate, upgrade, run testcase/testset, rerun, open latest report, start MCP)

Most samples are real outputs captured live against `Projects/ING-Public-Web`. A handful (`data show`, `report latest`, `run testset` on a project without prior runs) use representative output rather than a "no results" stub so the doc stays useful as a reference. Companion to (and slimmer than) [CLI_Override_Plan_And_Usage.md](CLI_Override_Plan_And_Usage.md): this one is the day-to-day **command** reference, the other is the **override-prefix and integration** reference.

**Breaking changes:** none — pure documentation.

---

## Notes & build status

- Every session ended with a successful `mvn` build across at least the modules it touched.
- Cumulative test count at end of the period: **930 tests, 0 failures** (last full run, session 7525f906).
- All "errors" the assistant flagged during individual sessions were pre-existing JDT/classpath warnings, not regressions introduced by the changes.
- For sections 18–22 (added 2026-06-05/06): all Maven builds across Engine + IDE + Datalib + Common returned BUILD SUCCESS; HTML-template changes do not require a rebuild as the files are copied as-is into each report folder.
- For section 23 (added 2026-06-06): `mvn -pl Datalib,Engine -am install -DskipTests` returned BUILD SUCCESS across Datalib + TestData-Csv + Engine; the new `config prefixes` subcommand and the 14 typed `--*` override flags on `run testcase/testset/tags/rerun` were smoke-tested directly against the freshly built JAR.
- For sections 24–28 (added 2026-06-06): `mvn -pl Engine -am install -DskipTests -q` returned BUILD SUCCESS; the new `project validate` / `project upgrade` subcommands and the dispatcher fix were verified live against `Projects/ING-Public-Web` and `Projects/TutorialUpgradeTest`, including the partial-coverage scoring case (75/100 with 1 of 2 test cases referenced).

