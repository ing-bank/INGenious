# INGenious Changelog

All notable changes to this project will be documented in this file.

## Version 3.1.0

Release Date: <insert date of release>

### General/UI

- Allow reordering of data tabs
- Fixed global shortcut keys functionality including:
    - Playwright recorder enablement
    - Run Test command
    - Debug command
- Update misspelled word 'Reusabe' to 'Reusable'
- Implemented auto-save functionality for object properties across all ORs
- Implemented `Shared Reusable Components` for cross-project reusables
    - Dedicated UI section for managing shared reusable components
    - Visual distinction between project-local and shared reusables
    - Shared Object Repository references across components
    - Move objects between project and shared repositories with confirmation dialogs
    - Automatic object dependency tracking and validation
    - Shared test data references and environment-specific data for shared reusables
    - Test data migration support when moving components

### Browser/Playwright Testing

- Added `JSPath` as a new locator attribute in Web Object Repository
- Integration with Engine's `AutomationObject` for runtime execution
- Fix Refactor_Object suffix when importing Playwright
- Implemented `Live Playwright Recording` with improved hook mechanisms for capturing test steps during execution
- Preserved `;exact` modifier in XML to YAML OR conversion
- Added `setAssertionTimeout` action for runtime timeout configuration

### Mobile App Testing

- Added null-safe handling in `setLambdaStatus` method for LambdaTest integration

### API Testing

- Added new Proxy Tab in API Workbench with per-request proxy configuration
- New `ProxyConfig` data model class for proxy settings persistence
- Refactored `APIHttpClient.getHttpClient()` for proxy and certificate support
- Enhancements and fixes in API Workbench including:
    - Fixed "(Copy) (Copy)" naming during request duplication
    - Added folder creation and deletion capability under collections
    - Delete and Add Request functionality in folders
    - Improved request moving between collections and folders
    - Click flow now mirrors Bruno and VS Code behavior
    - Fixed shortcuts and removed inactive ones
- Updated SSL context setting for webservice actions [Contribution]
- Improved bearer token masking for security
- Automatic copy of API jar to `Dist/release/Engine/lib` when built
- Fixed restricted header handling when pasting curl commands

### Synthetic Data

- Restored missing Synthetic Data actions

### Framework Enhancements

- Refined nested loop iterations with improved logic in `TestCaseRunner` and `TestStepRunner`
- Enhanced data processing for nested loops in `DataProcessor`
- Fixed test datasheet creation and rename operations
- Updated `TestDataComponent` with better error handling
- Prettier formatting updates

### Security Fixes

- Improved bearer token masking in webservice actions
- Enhanced credential handling in API proxy configuration
- Better SSL/TLS certificate validation