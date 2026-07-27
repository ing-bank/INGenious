# INGenious Changelog

All notable changes to this project will be documented in this file.

## Version 3.0.0

Release Date: <insert date of release>

### General/UI

#### Added

- Added a user-writable plugin search path with optional environment configuration and manifest-based plugin identity
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
  
#### Changed

- Enabled reordering of data tabs
- Corrected misspelled word 'Reusabe' to 'Reusable'
- Reorganised Configurations menu with renames
- Updated the Dashboard tree model to expand at Test Release level on load
- Updated reports to reference resources in Results/media for storage optimization
- Enhanced all bulk Delete confirmation dialog to support scrolling, improving usability when deleting a large number of test cases, objects, releases and test sets

#### Deprecated

#### Removed

#### Fixed

- Made plugin and application-root discovery tolerate missing or unreadable paths
- Kept one class loader per plugin, so repeated plugin discovery returns the same plugin classes instead of a new copy per lookup
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
- Enhanced Object Repository object name validation to prevent the creation of duplicate object names, regardless of letter casing (e.g., LoginButton, loginbutton, and LOGINBUTTON are now treated as duplicates).
- Fix context menu options for Reusable Component Test Cases for `New Group` option
- Added missing Test Manager option under TM Settings dropdown

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

- Refactored `APIHttpClient.getHttpClient()` for proxy and certificate support
- Converted API Workbench Authorization to a proper `addHeader` step
- Enhanced API Workbench with multiple fixes:
    - Fixed "(Copy) (Copy)" naming during request duplication
    - Added nested folder creation and deletion capability under collections
    - Implemented Delete and Add Request functionality in folders
    - Improved request moving between collections and folders
    - Mirrored click flow of object tree like in Bruno and VS Code behavior
    - Fixed shortcuts and removed inactive ones
- Renamed "⇢ Test" button to "⇢ Automation" in API Workbench

#### Deprecated

#### Removed

#### Fixed

- Updated SSL context setting for webservice actions [Contribution]
- Improved bearer token masking for security [Contribution]
- Resolved restricted header handling when pasting curl commands
    - Updated all launcher scripts with `jdk.httpclient.allowRestrictedHeaders`
    - Added defensive fallback in `APIHttpClient`
    - Dropped client-managed headers (`Content-Length`, `Accept-Encoding`) silently
- Conversion from API Request to Test Case now transfers username and password fields when using Basic Auth
- Fixed Bruno importer parsing of URLs containing `//`

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
- Restored missing Synthetic Data actions

### Framework Enhancements

#### Added

- Introduced Per-Step Hard/Soft Assertion Controls
    - Added `~` marker for hard assertion in test step tags
    - Implemented context menu options: Soft Assertion / Hard Assertion
    - Configured to stop current iteration immediately on hard assertion failures
- Implemented CLI override coverage audit with full CLI usage docs
- Added CLI overrides implementation of all missing prefixes with typed flags and `config prefixes` help
- Introduced `project validate` quality dashboard with per-test-case & per-reusable Kind classification
- Implemented `project upgrade` 4-step interactive modernisation wizard
- Added CLI dispatcher routing for `object` / `testset` / `data` to picocli
- Created single-source CLI reference document
- Ensured Build and Test Stability on JDK 26
    - Added Maven Surefire system property `net.bytebuddy.experimental=true`
    - Documented JaCoCo incompatibility on JDK 26
- Improved Reusable Scenario Creation Process
- Automated copy of API jar to `Dist/release/Engine/lib` when built

#### Changed

- Refined nested loop iterations with improved logic in `TestCaseRunner` and `TestStepRunner`
- Enhanced data processing for nested loops in `DataProcessor`
- Updated `TestDataComponent` with better error handling
- Applied Prettier formatting updates
- Enhanced HTML summary clickable rows (Tabulator v6 fix)
- Implemented HTML report in-page Console Viewer with working filter under `file://`
- Updated `project upgrade` and `project validate` CLI, included migration of Test data new `Scope` field and disabled auto-migration of project for `validate` command

#### Deprecated

#### Removed

#### Fixed

- Resolved test datasheet creation and rename operations
- Fixed Test Datasheet renaming / Global Datasheet addition crash
- Corrected Detailed Report cross-iteration reusable expand behavior
- Corrected `validate` Test-set coverage scoring at test-case granularity

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

- Fixed unit test for EnvTest Data Cross Environment Rename

### Contribution

#### Added
#### Changed
#### Deprecated
#### Removed
#### Fixed
- by Palmieri, G. (Gianluca) [@palmierigianlu](https://github.com/palmierigianlu): Fixed SQL query editor error that occurs after formatting queries with Beautify or inserting manual line breaks.
