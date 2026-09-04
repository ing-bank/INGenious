# INGenious Installer Integration Changelog

**Base branch:** `release/4.0.0`

**Feature branch:** `poc/installer-4`

**Integration commit:** `f7ca09b7`

**Target version:** `4.0.0`

## Overview

This change introduces native macOS and Windows packaging while separating application-owned resources from user-writable data.

The new release structure is:

    INGenious distribution
    ├── Runtime/               Application-owned resources
    ├── Workspace/             User-writable and persistent data
    │   ├── Configuration/
    │   ├── Projects/
    │   ├── Shared/
    │   └── plugins/
    ├── INGenious.app          Native macOS application
    ├── ingenious              Unix/Linux launcher
    ├── ingenious.command      macOS portable launcher
    ├── ingenious.bat          Windows portable launcher
    └── Readme.md

The portable launchers remain supported alongside the native installers.

---

## Added

### Runtime path management

#### `Datalib/src/main/java/com/ing/datalib/util/RuntimePath.java`

Added centralized resolution for application-owned resources.

- Supports the `ingenious.app.home` JVM property.
- Resolves the application root, Runtime configuration, libraries, and browser-driver directories.
- Retains legacy and source-development fallbacks.

### Workspace path management

#### `Datalib/src/main/java/com/ing/datalib/util/WorkspacePath.java`

Added centralized resolution for writable user data.

Workspace resolution follows this precedence:

1. `ingenious.workspace` JVM property
2. `INGENIOUS_WORKSPACE` environment variable
3. Valid sibling Workspace for a portable macOS distribution
4. `~/Library/Application Support/INGenious` for an installed macOS application
5. `%LOCALAPPDATA%\INGenious` for an installed Windows application
6. Current working directory for legacy compatibility

The resolver provides paths for `Configuration`, `Projects`, `Shared`, `UserDefined`, and the persistent user plugin directory at `Workspace/plugins`.

Plugin discovery uses the resolved Workspace, so the `ingenious.workspace` JVM property and `INGENIOUS_WORKSPACE` environment variable remain authoritative. Application upgrades may replace Runtime while preserving user-installed plugins in the Workspace.

Existing plugin files are never overwritten automatically. This change does not automatically migrate or delete plugin data from legacy Runtime locations; any future migration requires an explicit, reviewed conflict and compatibility policy.

### Workspace initialization

#### `Datalib/src/main/java/com/ing/datalib/util/WorkspaceInitializer.java`

Added first-run Workspace initialization from a packaged `WorkspaceTemplate`.

- Copies missing directories and files.
- Preserves existing user files, including user-installed plugins.
- Creates the empty `Workspace/plugins` directory from the packaged Workspace template.
- Does nothing when no template is available.
- Supports upgrades without overwriting user content.

### Path and initialization tests

Added:

- `Datalib/src/test/java/com/ing/datalib/util/RuntimePathTest.java`
- `Datalib/src/test/java/com/ing/datalib/util/WorkspacePathTest.java`
- `Datalib/src/test/java/com/ing/datalib/util/WorkspaceInitializerTest.java`
- `Engine/src/test/java/com/ing/engine/plugin/loader/PluginLoaderTest.java`

Coverage includes path precedence, packaged platform defaults, portable behavior, Workspace initialization, plugin-directory resolution, missing plugin-directory behavior, and preservation of existing user plugin files.

---

## Platform-specific JavaFX packaging

Native installer inputs now exclude JavaFX libraries for other operating systems.

- macOS packages retain generic and macOS ARM64 JavaFX libraries.
- Windows packages retain generic and Windows JavaFX libraries.
- Portable distributions retain JavaFX libraries for all supported platforms.
- No Maven dependencies, project resources, or Workspace files are removed.
- The macOS application size was reduced by approximately 91 MB.
- The macOS PKG size was reduced by approximately 89 MB.

---

## Native macOS Packaging

### `scripts/package-macos-app.zsh`

Added Apple Silicon macOS application-image generation using Java 17 `jpackage`.

- Produces `Dist/release/INGenious.app`.
- Bundles a Java runtime.
- Receives the application version from Maven `${project.version}` and uses package identifier `com.ing.ingenious`.
- Uses the INGenious `.icns` icon.
- Packages application-owned Runtime resources.
- Excludes writable Workspace content from the application bundle.
- Retains native Java commands required by subprocess features.
- Validates required resources, launcher configuration, Engine JAR placement, ARM64 architecture, and code-signature integrity.
- Removes temporary `jpackage` files after packaging.

The packaged launcher includes:

    -Dingenious.app.home=$APPDIR
    -Djdk.internal.httpclient.disableHostnameVerification=true
    -Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding

The native macOS application discovers the writable Workspace at runtime instead of embedding a fixed Workspace path.

### `scripts/package-macos-pkg.zsh`

Added macOS PKG generation.

- Packages `INGenious.app` for installation under `/Applications`.
- Includes a Workspace template for user initialization.
- Produces `Dist/target/INGenious-<version>.pkg`, currently `INGenious-4.0.0.pkg`.
- Receives the package version from Maven `${project.version}`.
- Renders the versioned Distribution definition under `Dist/target`.
- Uses a fixed root payload containing `/Applications/INGenious.app`.
- Validates the Distribution template, rendered Distribution XML, post-install script, and application signature.
- Uses `pkgbuild` and `productbuild`.

The current POC package is unsigned.

### `scripts/macos-pkg/Distribution.xml`

Added the fixed system-level macOS product-archive template for package `com.ing.ingenious.pkg`.

The package version is represented by `@APP_VERSION@` and is rendered from Maven `${project.version}` during packaging. The PKG uses a non-relocatable root payload installed at `/`, with the application located under `/Applications/INGenious.app`.

### `scripts/macos-pkg/postinstall`

Added post-install Workspace initialization.

- Determines the active console user.
- Initializes `~/Library/Application Support/INGenious`.
- Copies only missing Workspace entries.
- Preserves existing user files.
- Assigns copied files to the correct user and group.

### `Resources/INGenious.icns`

Added the native macOS application icon.

### `scripts/GenerateMacOSIcon.java`

Added a utility for generating INGenious macOS icon artwork.

---

## Native Windows Packaging

### `scripts/package-windows-app.ps1`

Added Windows application-image and MSI generation using Java 17 `jpackage`.

- Produces a Windows application image.
- Produces `Dist/target/INGenious-<version>.msi`, currently `INGenious-4.0.0.msi`.
- Receives the application version from Maven `${project.version}`.
- Bundles a Java runtime.
- Uses the INGenious Windows icon.
- Packages `WorkspaceTemplate` for per-user initialization.
- Installs the application under `C:\Program Files\INGenious`.
- Uses `%LOCALAPPDATA%\INGenious` for writable per-user data.
- Adds Start menu integration.
- Validates required Runtime resources, Engine JAR placement, and launcher configuration.
- Prevents the active writable Workspace from being embedded in the application image.
- Preserves the team HTTP JVM settings.

---

## Runtime and Workspace Resource Layout

### Application-owned resources

Application-owned resources were moved under `Resources/Runtime`.

Major directory moves:

    Resources/Configuration/PageDump/
        → Resources/Runtime/Configuration/PageDump/

    Resources/Configuration/ReportTemplate/
        → Resources/Runtime/Configuration/ReportTemplate/

    Resources/Engine/
        → Resources/Runtime/Engine/

    Resources/lib/
        → Resources/Runtime/lib/

    Resources/web/
        → Resources/Runtime/web/

    Resources/package.json
        → Resources/Runtime/package.json

    Resources/package-lock.json
        → Resources/Runtime/package-lock.json

Application-owned configuration resources moved under `Resources/Runtime/Configuration` include:

- `SampleScript.java`
- `StepMap.csv`
- `conf.js`
- `err.html`
- `har_to_pagespeed.exe`
- `ignore.conf`
- `package.properties`

Report templates, PageDump assets, CSS, JavaScript, fonts, images, themes, previews, web-dashboard assets, Engine files, and supporting binaries were moved without intentional content changes.

### User-writable resources

User-writable seed resources were moved under `Resources/Workspace`.

    Resources/Configuration/.enc
        → Resources/Workspace/Configuration/.enc

    Resources/Configuration/ExplorerConfig.properties
        → Resources/Workspace/Configuration/ExplorerConfig.properties

    Resources/Configuration/XPLOR_SETTINGS.json
        → Resources/Workspace/Configuration/XPLOR_SETTINGS.json

    Resources/Projects/
        → Resources/Workspace/Projects/

The current Tutorial project moved with the Projects directory.

Shared seed directories now live under:

    Resources/Workspace/Shared/
    Resources/Workspace/Shared/SharedObjectRepository/

---

## Centralized Application Paths

### `Engine/src/main/java/com/ing/engine/constants/AppResourcePath.java`

Expanded the Engine-facing path abstraction.

Added or updated resolution for:

- Application root
- Workspace root
- Projects
- Shared
- UserDefined
- Writable Workspace configuration
- Read-only Runtime configuration
- Runtime libraries
- Engine
- Tools
- Web resources
- StepMap
- SampleScript
- Package properties
- PageSpeed executable
- Report templates
- PageDump resources

This separates writable configuration from application-owned templates and resources.

---

## Workspace-Aware Application Features

The following components now use Runtime or Workspace paths instead of relying on `user.dir` or relative directories:

- `Datalib/src/main/java/com/ing/datalib/component/Project.java`
- `Datalib/src/main/java/com/ing/datalib/or/ObjectRepository.java`
- `Datalib/src/main/java/com/ing/datalib/settings/DriverProperties.java`
- `Datalib/src/main/java/com/ing/datalib/settings/DriverSettings.java`
- `Datalib/src/main/java/com/ing/datalib/testdata/TestDataFactory.java`
- `Engine/src/main/java/com/ing/engine/cli/commands/ProjectCommand.java`
- `Engine/src/main/java/com/ing/engine/cli/commands/RunCommand.java`
- `Engine/src/main/java/com/ing/engine/cli/commands/ServerCommand.java`
- `Engine/src/main/java/com/ing/engine/cli/commands/UpgradeCommand.java`
- `Engine/src/main/java/com/ing/engine/reporting/performance/metrics/MetricsProvider.java`
- `Engine/src/main/java/com/ing/engine/support/AnnontationUtil.java`
- `Engine/src/main/java/com/ing/engine/support/reflect/Discovery.java`
- `IDE/src/main/java/com/ing/ide/main/dashboard/server/DashBoardData.java`
- `IDE/src/main/java/com/ing/ide/main/dashboard/server/DashBoardServer.java`
- `IDE/src/main/java/com/ing/ide/main/explorer/settings/ReportingModuleSettings.java`
- `IDE/src/main/java/com/ing/ide/main/explorer/settings/Settings.java`
- `IDE/src/main/java/com/ing/ide/main/ui/FXStartUp.java`
- `IDE/src/main/java/com/ing/ide/main/ui/NewProject.java`
- `IDE/src/main/java/com/ing/ide/main/ui/StartUp.java`
- `IDE/src/main/java/com/ing/ide/main/ui/InjectScript.java`
- `IDE/src/main/java/com/ing/ide/main/mainui/components/aichat/auth/SecureTokenStore.java`
- `IDE/src/main/java/com/ing/ide/main/mainui/components/aichat/mcp/ActionCatalog.java`
- `IDE/src/main/java/com/ing/ide/main/utils/CMProjectCreator.java`
- `IDE/src/main/java/com/ing/ide/main/utils/INGeniousFileChooser.java`
- `IDE/src/main/java/com/ing/ide/main/utils/StepMap.java`
- `IDE/src/main/java/com/ing/ide/settings/AppSettings.java`
- `IDE/src/main/java/com/ing/ide/util/logging/UILogger.java`

---

## Shared Resources

### `Project.java`

Shared reusable components now resolve from the Workspace `Shared` directory.

### `ObjectRepository.java`

Updated Workspace-based locations for:

- Shared web objects
- Shared mobile objects
- Shared SAP objects
- Shared object repository
- Shared XML archive
- Legacy shared XML cleanup

---

## Browser Driver Paths

### `DriverProperties.java`
### `DriverSettings.java`

Browser-driver defaults now resolve through `Runtime/lib/Drivers`.

Updated defaults include:

- `geckodriver`
- `chromedriver`
- `IEDriverServer.exe`
- `MicrosoftWebDriver.exe`

Associated tests were updated to use `RuntimePath`.

---

## CLI Improvements

### `ProjectCommand.java`

Named projects now resolve through the configured Workspace Projects directory.

### `RunCommand.java`

Updated project lookup order:

1. Explicit absolute path
2. Path relative to the current terminal directory
3. Named project under Workspace Projects

Added test-case and test-set file resolution in this order:

1. `.yaml`
2. `.yml`
3. Legacy `.csv`

### `ServerCommand.java`

Project listing and fallback project lookup now use Workspace Projects.

### `UpgradeCommand.java`

Project lookup now supports Workspace Projects while retaining absolute and terminal-relative paths.

---

## Workspace Initialization at Startup

### `IDE/src/main/java/com/ing/ide/main/Main.java`

The application now invokes `WorkspaceInitializer.initialize()` during startup.

Installed applications can initialize missing Workspace files while preserving existing user data.

---

## StoryWriter Subprocess Fix

### `IDE/src/main/java/com/ing/ide/main/bdd/BddParser.java`

Updated StoryWriter launch behavior for packaged applications.

- Resolves Tools through `AppResourcePath.getToolsPath()`.
- Locates the StoryWriter JAR in the packaged Tools directory.
- Uses Java from `System.getProperty("java.home")`.
- Uses `java.exe` on Windows and `java` on Unix/macOS.
- Launches the JAR by absolute path.
- Sets the Tools directory as the subprocess working directory.
- Avoids dependence on the system `PATH`.
- Avoids duplicate StoryWriter processes.
- Logs missing Tools directories and missing bundled Java.

---

## Playwright Recorder Subprocess Fix

### `IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseComponent.java`

Updated Playwright recording for packaged applications.

- Resolves Java from the bundled/current `java.home`.
- Resolves Playwright libraries through `RuntimePath.getLibPath()`.
- Uses the platform classpath separator.
- Sets Runtime as the child-process working directory.
- Removes dependence on Terminal login-shell behavior.
- Preserves Windows `PrintDeps.exe` initialization.
- Improves error logging.

This fixes Playwright CLI loading in the packaged macOS application.

---

## UserDefined Scripts

### `InjectScript.java`

- Loads `SampleScript.java` from Runtime configuration.
- Stores compiled classes under Workspace `UserDefined`.
- Creates `UserDefined` when needed.
- Updates user guidance to reference the Workspace root.

### `AnnontationUtil.java`
### `Discovery.java`

User-defined package discovery now uses the Workspace `UserDefined` directory.

---

## Settings, Logs, and Tokens

### `SecureTokenStore.java`

The AI chat encryption key now resides under writable Workspace configuration.

### `AppSettings.java`

Application settings now use the Workspace configuration directory and create the directory before writing.

### `UILogger.java`

- Resolves relative log paths against the Workspace.
- Preserves explicitly configured absolute paths.
- Creates missing log directories.
- Exposes the resolved log-file path publicly.

### `LoadingFail.java`

The Playwright failure window now opens the configured application log rather than assuming `user.dir/log.txt`.

---

## Font Loading

### IDE font utility

Added:

    IDE/src/main/java/com/ing/ide/main/utils/AppFonts.java

The utility:

- Loads `ingme_regular.ttf` from the classpath.
- Registers the font once.
- Caches registration state.
- Logs missing or invalid resources.
- Removes dependence on filesystem-relative font paths.

Updated IDE consumers:

- `Main.java`
- `AppMenuBar.java`
- `AppToolBar.java`
- `TestDesignUI.java`
- `ObjectTree.java`
- `ProjectTree.java`
- `TestSetTree.java`
- `StartUp.java`
- `XTable.java`

### StoryWriter font utility

Added:

    StoryWriter/src/main/java/com/ing/storywriter/util/AppFonts.java

Updated:

- `StoryWriter/src/main/java/com/ing/storywriter/bdd/editor/StyledEditor.java`
- `StoryWriter/src/main/java/com/ing/storywriter/bdd/ui/UI2.java`

StoryWriter now loads the ING Me font from its packaged classpath instead of a relative filesystem location.

---

## Application Icons

### `IDE/src/main/java/com/ing/ide/main/utils/AppIcon.java`

- The bundled `.icns` remains authoritative on macOS.
- Runtime Java code no longer replaces the macOS Dock icon.
- Windows and other supported platforms retain generated taskbar and window icons.
- Prevents the icon from changing after macOS application launch.

---

## Restart Behavior

### `IDE/src/main/java/com/ing/ide/main/mainui/AppMainFrame.java`

Restart logic now supports:

- Native macOS `.app` bundles
- Portable macOS launcher
- Portable Windows launcher
- Portable Unix/Linux launcher

Native macOS restart uses `/usr/bin/open -n`.

Launcher and application paths are resolved from the Runtime/release layout rather than the process working directory.

---

## Dashboard and Static Resources

### `DashBoardData.java`
### `DashBoardServer.java`

Dashboard and web resources now resolve from Runtime web assets.

### `MetricsProvider.java`

`har_to_pagespeed.exe` now resolves from Runtime configuration.

### `ActionCatalog.java`
### `StepMap.java`

StepMap resources now resolve through centralized application paths.

### `CMProjectCreator.java`

Engine and SampleScript locations now resolve from Runtime paths.

---

## Project Creation and Selection

Updated:

- `FXStartUp.java`
- `StartUp.java`
- `NewProject.java`
- `INGeniousFileChooser.java`

Default project selection and creation now use `Workspace/Projects`.

Explicit external project paths remain supported.

---

## Portable Launchers

Updated:

- `Resources/ingenious`
- `Resources/ingenious.command`
- `Resources/ingenious.bat`

The launchers now calculate `INSTALL_DIR`, `RUNTIME_DIR`, `WORKSPACE_DIR`, and `APP_CLASSPATH`.

All portable launchers pass:

    -Dingenious.app.home=<Runtime>
    -Dingenious.workspace=<Workspace>

They retain:

    -Djdk.internal.httpclient.disableHostnameVerification=true
    -Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding

The Windows launcher uses the correct detached-launch syntax:

    start "" javaw

Portable launchers no longer depend on the terminal's current working directory to find Runtime resources.

---

## Maven and Build Configuration

### Root `pom.xml`

Added `runtimeDir` and `workspaceDir` properties.

### `Common/pom.xml`

Updated the generated Runtime Engine POM target to `Resources/Runtime/Engine/pom.xml`.

### `Engine/pom.xml`

Engine source and resource output now targets Runtime.

### `IDE/pom.xml`

- IDE dependencies now copy to Runtime `lib`.
- Removed duplicate whole-resource-tree copying from the IDE module.

### `Dist/pom.xml`

Distribution generation now separates:

- Release-root launchers and documentation
- Runtime resources
- Workspace resources

Other changes:

- Packaging work moved to `prepare-package`.
- Added Maven `macos` profile.
- Added Maven `windows` profile.
- macOS profile runs `.app` and PKG packaging scripts.
- Windows profile runs the PowerShell app-image/MSI script.
- npm operations now run against Runtime.

---

## Documentation and Ignore Rules

### `Resources/Readme.md`

Updated documentation for:

- Runtime/Workspace layout
- Native macOS application
- Windows, macOS, and Linux launch methods
- Shared Workspace behavior
- Project locations
- Upgrade behavior
- Workspace preservation across upgrades

### `Resources/.gitignore`

Updated ignored paths for the Runtime and Workspace directory structure.

---

## Tests Updated

Updated existing tests include:

- `Datalib/src/test/java/com/ing/datalib/component/TestCaseTest.java`
- `Datalib/src/test/java/com/ing/datalib/settings/DriverSettingsTest.java`
- `Engine/src/test/java/com/ing/engine/constants/AppResourcePathTest.java`

Coverage was updated for Runtime paths, Workspace paths, driver locations, project/test-case paths, Runtime versus writable configuration, packaged-platform defaults, and portable legacy behavior.

---

## Removed or Replaced Behavior

The following patterns were removed or replaced where applicable:

- Direct reliance on `System.getProperty("user.dir")` for packaged resources
- Relative `Configuration` paths
- Relative `Projects` paths
- Relative `Shared` paths
- Relative `Tools` paths
- Relative `lib` paths for Playwright
- PATH-dependent StoryWriter Java invocation
- Repeated filesystem-based ING Me font loading
- Runtime replacement of the bundled macOS Dock icon
- Writable configuration embedded inside native application images

No major user-facing feature was intentionally removed.

---

## Validation Status

### Completed

- Runtime and Workspace path tests
- Workspace initialization tests
- Full Maven reactor build
- Existing automated test suites
- macOS Apple Silicon application-image build
- macOS PKG build
- Generated application version validation
- Generated classpath validation
- JVM-option validation
- Bundled Java architecture validation
- macOS application signature validation
- StoryWriter packaged launch-path update
- Playwright packaged recording fix
- IDE packaged font fix
- StoryWriter packaged font fix

### Generated artifacts

    Dist/release/INGenious.app
    Dist/target/INGenious-4.0.0.pkg
    Dist/release/Runtime/ingenious-ide-4.0.0.jar
    Dist/release/Runtime/lib/ingenious-engine-4.0.0.jar

---

## Remaining Release Work

- Validate the Windows application image and MSI on a clean Windows machine.
- Validate install, reinstall, upgrade, and uninstall behavior.
- Confirm the Windows uninstall process preserves per-user Workspace data.
- Validate macOS PKG installation from `/Applications`.
- Validate Workspace preservation across macOS upgrades.
- Add production Developer ID Application signing.
- Add Developer ID Installer signing.
- Add Apple notarization and stapling.
- Perform Gatekeeper testing on a clean Mac.
- Add automated cross-platform packaging checks to CI where practical.

---

## Known Limitation

The current macOS PKG is unsigned.

Production distribution still requires:

- Developer ID Application signing
- Developer ID Installer signing
- Apple notarization
- Stapling

---

## Architectural Rule for Future Changes

Every new filesystem dependency should be classified before implementation.

### Application-owned and read-only

Use Runtime, `RuntimePath`, or Runtime-oriented `AppResourcePath` methods.

Examples:

- Libraries
- Engine files
- Tools
- Web assets
- Report templates
- Packaged executables
- Static configuration templates

### User-writable and persistent

Use Workspace, `WorkspacePath`, or Workspace-oriented `AppResourcePath` methods.

Examples:

- Projects
- Shared repositories
- User settings
- Logs
- Results
- AI token keys
- User-defined compiled classes

### Project-specific

Resolve relative to the selected project directory.

### External tools

Use an explicit or configured external dependency path.

Avoid introducing new assumptions based on:

    user.dir
    ./Configuration
    ./Projects
    ./Shared
    ./Tools
    ./lib
    ./web
