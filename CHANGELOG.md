# INGenious Changelog

All notable changes to this project will be documented in this file.

## Version 3.1.0

Release Date: <insert date of release>

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

#### Changed

- Enabled reordering of data tabs
- Corrected misspelled word 'Reusabe' to 'Reusable'
- Reorganised Configurations menu with renames

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
- Fixed column add/delete behavior in datasheet related to frozen columns

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

#### Deprecated

#### Removed

#### Fixed

- Updated SSL context setting for webservice actions [Contribution]
- Improved bearer token masking for security [Contribution]
- Resolved restricted header handling when pasting curl commands
    - Updated all launcher scripts with `jdk.httpclient.allowRestrictedHeaders`
    - Added defensive fallback in `APIHttpClient`
    - Dropped client-managed headers (`Content-Length`, `Accept-Encoding`) silently

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