# INGenious Changelog

All notable changes to this project will be documented in this file.

## Version 3.1.0

Release Date: <insert date of release>

### General/UI

#### Added
#### Changed
#### Deprecated
#### Removed
#### Fixed

### Browser/Playwright Testing

#### Added
#### Changed
#### Deprecate
#### Removed
#### Fixed

### Mobile App Testing

#### Added
#### Changed
#### Deprecated
#### Removed
#### Fixed

### API Testing

#### Added
#### Changed
#### Deprecated
#### Removed
#### Fixed

### Message Testing

#### Added
#### Changed
#### Deprecated
#### Removed
#### Fixed

### Database Testing

#### Added
#### Changed
#### Deprecated
#### Removed
#### Fixed

### SAP Testing

#### Added
#### Changed
#### Deprecated
#### Removed
#### Fixed

### Synthetic Data

#### Added
#### Changed
#### Deprecated
#### Removed
#### Fixed

### Framework Enhancements

#### Added
#### Changed
#### Deprecated
#### Removed
#### Fixed

### Security Fixes

#### Added
#### Changed
#### Deprecated
#### Removed
#### Fixed

### Unit Testing

#### Added
#### Changed
#### Deprecated
#### Removed
#### Fixed

### Contribution

#### Added
#### Changed
#### Deprecated
#### Removed
#### Fixed

---------------------------------------------------------------------

## Version 3.0.0

Release Date: August 10, 2026

### General/UI

#### Added

- Implemented `Shared Reusable Components` for cross-project reusables
    - Added dedicated UI section for managing shared reusable components
    - Introduced visual distinction between project-local and shared reusables
    - Added Shared Object Repository references across components
    - Enabled moving objects between project and shared repositories with confirmation dialogs
    - Introduced automatic object dependency tracking and validation
    - Added shared test data references and environment-specific data for shared reusables
    - Included test data migration support when moving components
- Implemented auto-save functionality for object properties across all ORs
- Added Object Repository auto-select for the right tab based on project type (Mobile/SAP/Structured-Data projects)
- Introduced API Workbench "+ New Request" button for creating requests without overwriting the current one
- Improved TM Settings Test Connection button readability when bulb turns green
- Reorganised Configurations menu with renames
- Implemented Validation Error Red-Marking Across Trees (errors propagate through reusable dependencies)
- Enhanced Object Repository with UX improvements
- Added inline “+” buttons to enable quick addition of rows and columns across test steps, test data sheets, settings, and configurations
- Implemented alphabetical sorting for test scenarios and test cases within the Test Lab
- Implemented closing of popups on Esc keypress
- Added Update (Rename) and Deleting for Test Case Tags
- Implemented Test Plan Scenario Groups with persistent organization
    - Added named group folders to organize Test Plan scenarios
    - Introduced visual distinction with stacked-folder group icon
    - Enabled drag-and-drop scenarios between groups
    - Created automatic (Ungrouped) bucket for unassigned scenarios
    - Added group creation, rename, and delete operations via context menu
    - Implemented persistent group membership and ordering across sessions
- Enhanced Workbench UI with refreshed branding
    - Redesigned Workbench button in toolbar, menu bar, and dock with prominent styling
    - Added larger, colored tiles in dock for Test Design, Execution, Dashboard, and API Workbench
    - Applied hover effects and shadows to dock buttons
    - Renamed "API Workbench" to "Workbench" with neutral icon treatment
- Implemented persistent sort order for scenarios and test cases
    - Scenarios in Test Plan, Reusable Components, and Shared Reusables now restore last sort order
    - Test cases within scenarios maintain custom ordering across restarts
    - Sort order persists after creation, deletion, rename, and drag-and-drop operations
- Added Auto-migration of Test Cases from `CSV` to `YAML` on project load
- Enhanced Auto-migration of Test Datasheet new `Scope` field on project load
- Added scope selector to Create Reusable dialog enabling direct creation to Project or Shared Reusable Components
- Implemented auto-population of Reference column with `[Project]` or `[Shared]` prefix when creating reusables
- Extended impact analysis to include Shared Reusable test cases alongside Test Plan and Project Reusable impacts
- Standardized Reusable Components UI with FXPanelHeader matching Test Plan and Object Repository style
- Updated Shared Reusable tree root node label to "Shared Reusable Components" for clarity
- Enhanced Web Object Repository role selection with dynamic filtering based on user-entered text
- Added cross-environment datasheet renaming functionality and its accompanying UI confirmation dialog
- Action 'assertVariable' can now assert runtime and global variables
- Added the JVM parameter `-Djdk.internal.httpclient.disableHostnameVerification=true` in ingenious.command and ingenious.bat startup scripts to allow connections to align behavior with Postman and Bruno when SSL verification is disabled.

#### Changed

- Enabled reordering of data tabs
- Corrected misspelled word 'Reusabe' to 'Reusable'
- Reorganised Configurations menu with renames
- Updated the Dashboard tree model to expand at Test Release level on load
- Updated reports to reference resources in Results/media for storage optimization
- Disabled edit mode on `Scope` field in Test datasheet to prevent user edits while maintaining visibility of the configured scope.
- Updated the list and display order of test cases in Test Data and Test Plan to improve usability and maintain a consistent user experience.
- Ensured Shared Reusables continue to correctly reference and inherit their associated Test Datasheet scope.
- Enhanced all bulk Delete confirmation dialog to support scrolling, improving usability when deleting a large number of test cases, objects, releases and test sets
- Disabled edit mode on `Scope` field in Test datasheet to prevent user edits while maintaining visibility of the configured scope.
- Updated the list and display order of test cases in Test Data and Test Plan to improve usability and maintain a consistent user experience.
- Ensured Shared Reusables continue to correctly reference and inherit their associated Test Datasheet scope.

#### Deprecated

#### Removed

#### Fixed

- Resolved global shortcut keys functionality including:
    - Playwright recorder enablement
    - Run Test command
    - Debug command
- Fixed File → Restart to properly relaunch INGenious on macOS/Windows/Linux
- Implemented auto-masking for Remote URL credentials on entry/paste
- Resolved case-only rename reliability (e.g., `ABC` → `abc` now works correctly)
- Fixed focus issue on newly created items (new scenarios/test cases now remain selected)
- Corrected Object Repository same-name rename blocked incorrectly (case-only renames now allowed)
- Fixed Test Manager publish report to flow through Console report (`console.txt`)
- Resolved Test Case Tags to persist to YAML and survive reloads
- Added missing Web Objects for sample project Tutorial
- Fixed column add/delete behavior in datasheet related to frozen columns
- Fixed OR Tables' add row (+) button 
- Updated suffix handling for copied scenarios, test cases, objects, and pages to apply only when an item with the same name already exists
- Resolved issues with multi-object and multi-page copy-paste and cut-paste operations in the Object Repository
- Fixed issue where enabling or disabling the LambdaTest option in the Manange Devices caused the Properties table to display empty entries.
- Corrected Save button behavior that becomes disabled after switching applications using Alt+Tab, despite having unsaved changes.
- Fixed behavior in the LambdaTest Remote URL field where the cursor unexpectedly jumped to the beginning of the text after typing characters beyond the @ symbol.
- Add Row (+) button is restricted to the first column hover
- Enhanced Object Repository and Test Data sheet name validation to prevent the creation of duplicate names, regardless of letter casing (e.g., LoginButton, loginbutton, and LOGINBUTTON are now treated as duplicates)
- Fix context menu options for Reusable Component Test Cases for `New Group` option
- Added missing Test Manager option under TM Settings dropdown
- Resolved issues with Test datasheet references in Reusable components.
- Fixed the delete confirmation message for Impacted Assets to ensure correct notification and confirmation behavior during deletion actions.
- Fixed "ghost submenu" whenever switching from INGenious to other applications
- Allow reuse of deleted scenarios in Project Reusable Components
- Fix incorrect reference when executing test cases when resolving test datasheets with reusables components
- Fix Param Loop error when using reusable compenents with test data
- Fixed Test Data `Scope` being reset/mismatched on project reload for duplicate scenario/test case names across Test Plan, Project Reusables, and Shared Reusables, and ensured conversions between these locations update `Scope` explicitly.
- Fixed HTML report step details disappearing from Detailed/Summary reports when Azure DevOps reporting is enabled, caused by `AzureTestCaseHandler` incorrectly becoming the primary report data source instead of `HtmlTestCaseHandler`.
- Fixed shared HTML report assets (JS/CSS/fonts) never refreshing after an engine upgrade, which left migrated projects serving stale, incompatible report scripts.
- Fixed copy-paste between Project and Shared SAP Objects
- Fixed copy-paste behavior in Object Repository to mirror test cases

### Browser/Playwright Testing

#### Added

- Introduced `JSPath` as a new locator attribute in Web Object Repository
    - Integrated with Engine's `AutomationObject` for runtime execution
    - Marked as `[Discouraged]` in UI with explanatory tooltip
- Implemented `Live Playwright Recording` with improved hook mechanisms for capturing test steps during execution
- Added `setAssertionTimeout` action for runtime timeout configuration

#### Changed

#### Deprecated

#### Removed

#### Fixed

- Corrected Refactor_Object suffix when importing Playwright recorded scripts
- Preserved `;exact` modifier in XML to YAML OR conversion
- Resolved assertURLmatches pattern compile error
- Fixed issue where the aXe accessibility report failed to load or display when `testAccessibility` is executed inside a reusable component
- Fixed Playwright recorder exact-match attributes to correctly populate the Exact flag and display consistent Object Repository values during live recording and file import.
- Fixed validation that incorrectly prevented duplicate test case names across different scenarios; test case names now only need to be unique within the same scenario.
- Added Enter key support to the Start Recording dialog for quicker recording startup.
- Fixed an issue where closing the recording browser externally added an unwanted empty step at the end of recorded test cases.
- Fixed Playwright recordings to correctly capture and insert page/tab switch actions in multi-tab scenarios.
- Fixed iframe element detection during live Playwright recording to correctly populate frame-related Object Repository attributes.
- Fixed `Record From Here` recordings being inserted at the beginning of a test case instead of immediately after the selected step.
- Fixed Live Recorder to prevent test case overwrite by appending suffix to duplicate names
- Fixed Hard Assertions waiting for timeout instead of failing immediately
- Fixed Debug Mode resume behavior for reusable test case breakpoints
- Restored Har Compare static asset routing after Jetty 12 dropped its `"/*"` → root context-path normalization
- Resolved Har Compare static resources 404ing due to Jetty 12's stricter resource-alias check on the unnormalized `./web` base path
- Fixed Har Compare WebSocket handshake failing with `IllegalStateException: WebSocketComponents has not been created` by explicitly registering `JettyWebSocketServletContainerInitializer`
- Fixed Dashboard server incorrectly attempting to rebind its port on repeated "Har Compare" opens by checking actual Jetty server state instead of the wrapper thread's liveness
- Hardened Dashboard server's local port availability check against false positives from lingering `TIME_WAIT` sockets
- Fixed Har Compare throwing `NullPointerException` when `config.json` was empty or corrupted

### Mobile App Testing

#### Added

- Implemented Mobile Object Repository per-platform (Android / iOS) properties
    - Added two independent property lists per object (Android/iOS)
    - Introduced toggle switch in table toolbar
    - Implemented runtime platform detection
    - Added YAML support for both `android:` and `ios:` blocks
    - Included legacy XML auto-migration to both platforms
- Introduced Manage Devices new tab with LambdaTest categorised capabilities
- Enhanced Manage Devices with cleaner, sectioned LambdaTest capabilities view

#### Changed

- Renamed "LambdaTest Capabilities" to "LambdaTest Grid Capabilities"
- Unified Mobile Scroll for Android + iOS
- Allowed shake to execute for IOS devices

#### Deprecated

- Phased out Manage Browser → Emulators path

#### Removed

#### Fixed

- Added null-safe handling in `setLambdaStatus` method for LambdaTest integration
- Resolved Manage Devices accordion scroll behavior
- Fixed the iOS and Android `platformVersion` for non-LambdaTest configurations to ensure only valid numeric values are accepted

### API Testing

#### Added

- Introduced Proxy Tab in API Workbench with per-request proxy configuration
    - Created `ProxyConfig` data model class for proxy settings persistence
    - Enabled per-request HTTP proxy configuration without touching config files
    - Implemented proxy details carried forward when converting request to test case
    - Added save proxy to default or new API config during conversion
- Enabled pasting `curl` command in API Workbench URL bar (Postman-style)
    - Implemented shell-aware parser supporting methods, headers, body, auth, query params
    - Added line continuations, quotes, multipart forms
    - Integrated with request panel
- Introduced right-click response in API Workbench to auto-build path assertions (JSON + XPath)
- Added Postman & Bruno collection import to INGenious Reusables
- Implemented color-formatted Response payload in HTML report with separate Headers section and copy buttons
- Strengthened SSL/TLS certificate validation
- Enhanced credential handling in API proxy configuration
- Introduced API Workbench Environment Management to fully manage and use environments and environment variables

#### Changed

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
