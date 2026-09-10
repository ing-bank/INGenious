# INGenious Installer Integration Changelog

**Base branch:** `release/4.0.0`

**Feature branch:** `poc/installer-4`

## Overview

This work introduces native macOS and Windows packaging while separating application-owned resources from user-writable data.

The restructuring establishes two distinct distribution models:

1. **Portable distributions**, where Runtime and Workspace remain together under the extracted distribution.
2. **Installed applications**, where application resources are installed under the platform application directory and user data is stored in a configurable per-user Workspace.

Portable distributions use the following layout:

    INGenious distribution
    ├── Runtime/               Application-owned resources
    │   ├── Configuration/
    │   ├── Engine/
    │   ├── lib/
    │   ├── Tools/
    │   └── web/
    ├── Workspace/             Portable user-writable data
    │   ├── Configuration/
    │   ├── Projects/
    │   ├── Shared/
    │   ├── UserDefined/
    │   └── plugins/
    ├── INGenious.app          Native macOS application image, when generated
    ├── ingenious              Unix/Linux portable launcher
    ├── ingenious.command      macOS portable launcher
    ├── ingenious.bat          Windows portable launcher
    └── Readme.md

Installed applications use a platform-specific application directory for packaged Runtime resources and a separate per-user Workspace:

    macOS:
    ~/Documents/INGenious Workspace

    Windows:
    %USERPROFILE%\Documents\INGenious Workspace

The older nested installed layout is not part of the new convention:

    Documents/INGenious/Workspace

No migration, detection, compatibility fallback, or automatic import is provided for that older layout.

The Workspace is user data. Installers and uninstallers must not delete it.

---

## Runtime and Workspace Architecture

### Runtime

Runtime contains application-owned resources that are installed or distributed with the application.

Examples include:

- Application libraries
- Engine files
- Tools
- Web assets
- Browser drivers
- Report templates
- PageDump resources
- Static configuration templates
- Packaged executables
- npm resources

Runtime content is treated as application-owned and replaceable during an application upgrade.

### Workspace

Workspace contains writable and persistent user data.

Examples include:

- Projects
- Shared repositories
- User settings
- Logs and results
- AI token keys
- User-defined compiled classes
- User-installed plugins

Workspace content is treated as user-owned data and must be preserved across application upgrades, reinstalls, relocation, and uninstall.

### Installed Workspace convention

The default installed Workspace is:

    macOS:
    ~/Documents/INGenious Workspace

    Windows:
    %USERPROFILE%\Documents\INGenious Workspace

When the user selects a custom Workspace location, the application stores the selected base directory in:

    ~/.ingenious/config.properties

using:

    workspace.base=<selected base>

The final Workspace path is derived as:

    <workspace.base>/INGenious Workspace

Only the selected base is persisted. The `INGenious Workspace` directory name is appended by the application.

### Portable Workspace convention

Portable distributions remain fixed to:

    <portable root>/Workspace

Portable launchers explicitly pass:

    -Dingenious.workspace=<portable root>/Workspace

Portable Workspace behavior is intentionally separate from installed Workspace customization.

---

## Runtime Path Management

### `Datalib/src/main/java/com/ing/datalib/util/RuntimePath.java`

Added centralized resolution for application-owned resources.

The Runtime path abstraction:

- Supports the `ingenious.app.home` JVM property.
- Resolves the application root.
- Resolves Runtime configuration.
- Resolves Runtime libraries.
- Resolves browser-driver directories.
- Supports packaged application layouts.
- Retains source-development behavior where required by the existing development workflow.

Installed launchers identify the packaged application home through:

    -Dingenious.app.home=<packaged application home>

For the generated macOS application, this is represented in `INGenious.cfg` as:

    java-options=-Dingenious.app.home=$APPDIR

Installed launchers must not set `ingenious.workspace`. Its absence allows the application to resolve and customize the installed Workspace.

---

## Workspace Path Management

### `Datalib/src/main/java/com/ing/datalib/util/WorkspacePath.java`

Added centralized resolution for writable user data.

The Workspace resolver distinguishes explicitly configured portable Workspaces from installed Workspaces.

Relevant inputs include:

- The `ingenious.workspace` JVM property for explicit and portable Workspace selection.
- The `INGENIOUS_WORKSPACE` environment variable where supported by the existing resolver.
- The stored installed Workspace base in `~/.ingenious/config.properties`.
- Installed platform defaults.
- Portable distribution layout detection where applicable.

For installed applications, the default Workspace resolves to:

    ~/Documents/INGenious Workspace

on macOS, and:

    %USERPROFILE%\Documents\INGenious Workspace

on Windows.

For a custom installed location, the final path resolves to:

    <workspace.base>/INGenious Workspace

The resolver provides paths for:

- `Configuration`
- `Projects`
- `Shared`
- `UserDefined`
- `plugins`

Plugin discovery uses the resolved Workspace. Application upgrades may replace Runtime while preserving user-installed plugins and other Workspace content.

No automatic migration or deletion of data from older layouts is performed.

---

## Workspace Initialization

### `Datalib/src/main/java/com/ing/datalib/util/WorkspaceInitializer.java`

Added first-run Workspace initialization from the packaged `WorkspaceTemplate`.

For a missing or incomplete first-launch Workspace, initialization creates the application-required Workspace structure from the packaged template.

The initialized structure includes applicable content under:

- `Configuration`
- `Projects`
- `Shared`
- `UserDefined`
- `plugins`

A valid existing Workspace is authoritative.

The initializer does not overlay `WorkspaceTemplate` onto a valid existing Workspace. This prevents renamed or deliberately deleted user content from reappearing after restart or Workspace relocation.

The initializer does not overwrite existing user files or user-installed plugins.

Installed application startup is the authoritative initialization point. Installer-time initialization should not duplicate this behavior unless a platform packaging design specifically requires it.

### `IDE/src/main/java/com/ing/ide/main/Main.java`

The application invokes Workspace initialization during startup.

This allows installed applications to initialize the default or configured Workspace on first launch without embedding writable Workspace content inside the installed application image.

---

## Installed Workspace Location Feature

The installed application includes a Workspace-location command under:

    Configurations → Workspace Location

The command is shown only when Workspace customization is available.

Portable distributions do not expose installed Workspace customization because their launchers explicitly set `ingenious.workspace`.

### Preference storage

The selected installed Workspace base is saved to:

    ~/.ingenious/config.properties

using:

    workspace.base=<selected base>

The application derives the actual Workspace as:

    <selected base>/INGenious Workspace

### Verified relocation operation

Moving the installed Workspace follows this sequence:

1. Validate the selected destination.
2. Save the currently loaded project.
3. Copy the active Workspace exactly.
4. Verify copied relative paths, file sizes, and SHA-256 hashes.
5. Save the new `workspace.base`.
6. Verify the copied Workspace again.
7. Delete the old Workspace only after successful verification.
8. Restart the application without saving into the deleted source Workspace.

### Destination safety checks

Relocation rejects unsafe destinations, including:

- The active Workspace itself.
- A path inside the active Workspace.
- A path that contains the active Workspace.
- An equivalent normalized path.
- A symbolic link that resolves into the active Workspace.
- A destination where `INGenious Workspace` already exists.
- A base path that is not a directory.
- A base path that is not writable.

### Existing Workspace behavior

A valid existing Workspace remains authoritative.

The packaged `WorkspaceTemplate` is not overlaid onto a valid Workspace after relocation or restart. Renamed or deleted user content must not be recreated merely because it exists in the packaged template.

### Scope boundary

The installed Workspace feature does not provide:

- Migration from `Documents/INGenious/Workspace`.
- Detection of older Workspace layouts.
- Legacy configuration keys.
- Compatibility aliases.
- Automatic import of previous Workspaces.
- Automatic Workspace merging.
- Portable Workspace customization.
- Network or cloud Workspace support.
- Installer upgrade migration.

---

## Path and Initialization Tests

Added:

- `Datalib/src/test/java/com/ing/datalib/util/RuntimePathTest.java`
- `Datalib/src/test/java/com/ing/datalib/util/WorkspacePathTest.java`
- `Datalib/src/test/java/com/ing/datalib/util/WorkspaceInitializerTest.java`
- `Engine/src/test/java/com/ing/engine/plugin/loader/PluginLoaderTest.java`

Additional coverage was added for the installed Workspace-location implementation and `AppResourcePath` integration.

Coverage includes:

- Runtime path resolution.
- Workspace path precedence.
- Installed platform defaults.
- Portable Workspace behavior.
- Stored Workspace base resolution.
- Workspace initialization.
- Valid existing Workspace preservation.
- No-template-overlay behavior.
- Plugin-directory resolution.
- Missing plugin-directory behavior.
- Preservation of existing user plugin files.
- Unsafe relocation destination rejection.
- Verified Workspace copying.
- Renamed-content preservation.
- Restart behavior after relocation.

The latest focused results recorded for the completed application feature were:

    Datalib:
    40 tests
    0 failures
    0 errors

    Engine AppResourcePath:
    38 tests
    0 failures
    0 errors

The complete IDE package also succeeded under Java 17.

A broader reactor test run previously exposed an unrelated `TestData - Csv` failure involving an unresolved `FileScanner` reference. That failure was not caused by the Workspace relocation implementation.

---

## Runtime and Workspace Resource Layout

### Application-owned resources

Application-owned resources were moved under:

    Resources/Runtime

Major directory moves include:

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

### Workspace seed resources

Workspace seed resources were moved under the Workspace template source used for distribution packaging.

Relevant resources include:

    Configuration/.enc
    Configuration/ExplorerConfig.properties
    Configuration/XPLOR_SETTINGS.json
    Projects/
    Shared/
    Shared/SharedObjectRepository/
    plugins/

The Tutorial project moved with the Projects content.

Installed native packages include `WorkspaceTemplate` as application-owned seed content. The application uses it only when startup initialization is required.

Portable distributions continue to include their writable Workspace directly under:

    <portable root>/Workspace

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

This separates writable configuration and user data from application-owned templates and resources.

---

## Workspace-Aware Application Features

The following components now use Runtime or Workspace paths instead of relying on `user.dir` or unqualified relative directories:

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
- Applicable shared XML cleanup

---

## Browser Driver Paths

### `DriverProperties.java`

### `DriverSettings.java`

Browser-driver defaults now resolve through:

    Runtime/lib/Drivers

Updated defaults include:

- `geckodriver`
- `chromedriver`
- `IEDriverServer.exe`
- `MicrosoftWebDriver.exe`

Associated tests were updated to use centralized Runtime path resolution.

---

## CLI Improvements

### `ProjectCommand.java`

Named projects now resolve through the configured Workspace Projects directory.

### `RunCommand.java`

Project lookup follows this order:

1. Explicit absolute path.
2. Path relative to the current terminal directory.
3. Named project under Workspace Projects.

Test-case and test-set files are resolved in this order:

1. `.yaml`
2. `.yml`
3. `.csv`

### `ServerCommand.java`

Project listing and fallback project lookup use Workspace Projects.

### `UpgradeCommand.java`

Project lookup supports Workspace Projects while retaining explicit absolute and terminal-relative paths.

---

## StoryWriter Subprocess Fix

### `IDE/src/main/java/com/ing/ide/main/bdd/BddParser.java`

Updated StoryWriter launch behavior for packaged applications.

- Resolves Tools through `AppResourcePath.getToolsPath()`.
- Locates the StoryWriter JAR in the packaged Tools directory.
- Uses Java from `System.getProperty("java.home")`.
- Uses `java.exe` on Windows and `java` on Unix and macOS.
- Launches the JAR by absolute path.
- Sets the Tools directory as the subprocess working directory.
- Avoids dependence on the system `PATH`.
- Avoids duplicate StoryWriter processes.
- Logs missing Tools directories and missing bundled Java.

---

## Playwright Recorder Subprocess Fix

### `IDE/src/main/java/com/ing/ide/main/mainui/components/testdesign/testcase/TestCaseComponent.java`

Updated Playwright recording for packaged applications.

- Resolves Java from the active or bundled `java.home`.
- Resolves Playwright libraries through `RuntimePath.getLibPath()`.
- Uses the platform classpath separator.
- Sets Runtime as the child-process working directory.
- Removes dependence on terminal login-shell behavior.
- Preserves Windows `PrintDeps.exe` initialization.
- Improves error logging.

This fixes Playwright CLI loading in the packaged macOS application.

---

## UserDefined Scripts

### `InjectScript.java`

- Loads `SampleScript.java` from Runtime configuration.
- Stores compiled classes under Workspace `UserDefined`.
- Creates `UserDefined` when needed.
- References the Workspace root in user guidance.

### `AnnontationUtil.java`

### `Discovery.java`

User-defined package discovery uses the Workspace `UserDefined` directory.

---

## Settings, Logs, and Tokens

### `SecureTokenStore.java`

The AI chat encryption key resides under writable Workspace configuration.

### `AppSettings.java`

Application settings use the Workspace configuration directory and create the directory before writing.

### `UILogger.java`

- Resolves relative log paths against the Workspace.
- Preserves explicitly configured absolute paths.
- Creates missing log directories.
- Exposes the resolved log-file path publicly.

### `LoadingFail.java`

The Playwright failure window opens the configured application log instead of assuming `user.dir/log.txt`.

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

Updated IDE consumers include:

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

StoryWriter loads the ING Me font from its packaged classpath instead of a relative filesystem location.

---

## Application Icons

### `IDE/src/main/java/com/ing/ide/main/utils/AppIcon.java`

- The bundled `.icns` is authoritative on macOS.
- Runtime Java code no longer replaces the macOS Dock icon.
- Windows and other supported platforms retain generated taskbar and window icons.
- The macOS icon no longer changes after application launch.

### `Resources/INGenious.icns`

Added the native macOS application icon.

### `scripts/GenerateMacOSIcon.java`

Added a utility for generating the INGenious macOS icon artwork.

---

## Restart Behavior

### `IDE/src/main/java/com/ing/ide/main/mainui/AppMainFrame.java`

Restart logic supports:

- Native macOS `.app` bundles.
- Portable macOS launcher.
- Portable Windows launcher.
- Portable Unix/Linux launcher.

Native macOS restart uses:

    /usr/bin/open -n

Launcher and application paths are resolved from the Runtime and release layout instead of the process working directory.

The installed Workspace relocation flow restarts without saving into the deleted source Workspace.

---

## Dashboard and Static Resources

### `DashBoardData.java`

### `DashBoardServer.java`

Dashboard and web resources resolve from Runtime web assets.

### `MetricsProvider.java`

`har_to_pagespeed.exe` resolves from Runtime configuration.

### `ActionCatalog.java`

### `StepMap.java`

StepMap resources resolve through centralized application paths.

### `CMProjectCreator.java`

Engine and SampleScript locations resolve from Runtime paths.

---

## Project Creation and Selection

Updated:

- `FXStartUp.java`
- `StartUp.java`
- `NewProject.java`
- `INGeniousFileChooser.java`

Default project selection and creation use:

    <Workspace>/Projects

Explicit external project paths remain supported.

---

## Portable Launchers

Updated:

- `Resources/ingenious`
- `Resources/ingenious.command`
- `Resources/ingenious.bat`

The portable launchers calculate:

- `INSTALL_DIR`
- `RUNTIME_DIR`
- `WORKSPACE_DIR`
- `APP_CLASSPATH`

All portable launchers pass both:

    -Dingenious.app.home=<portable root>/Runtime
    -Dingenious.workspace=<portable root>/Workspace

They retain:

    -Djdk.internal.httpclient.disableHostnameVerification=true
    -Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding

The Windows launcher uses detached-launch syntax:

    start "" javaw

Portable launchers do not depend on the terminal's current working directory to locate Runtime resources.

The explicit `ingenious.workspace` property is the distribution marker that keeps portable Workspace behavior fixed and disables installed Workspace customization.

---

## Platform-Specific JavaFX Packaging

Native installer inputs exclude JavaFX libraries for other operating systems.

- macOS packages retain generic and macOS ARM64 JavaFX libraries.
- Windows packages retain generic and Windows JavaFX libraries.
- Portable distributions retain JavaFX libraries for all supported platforms.
- Maven dependencies, project resources, and Workspace files are not removed from the portable distribution.
- The macOS application size was reduced by approximately 91 MB.
- The macOS PKG size was reduced by approximately 89 MB.

---

## Native macOS Packaging

### `scripts/package-macos-app.zsh`

Added Apple Silicon macOS application-image generation using Java 17 `jpackage`.

The script:

- Produces `Dist/release/INGenious.app`.
- Bundles a Java runtime.
- Receives the application version from Maven `${project.version}`.
- Uses package identifier `com.ing.ingenious`.
- Uses the INGenious `.icns` icon.
- Packages application-owned Runtime resources.
- Packages `WorkspaceTemplate` for application-managed first-launch initialization.
- Excludes the active writable Workspace from the application bundle.
- Retains native Java commands required by subprocess features.
- Validates required resources.
- Validates launcher configuration.
- Validates Engine JAR placement.
- Validates ARM64 architecture.
- Validates code-signature integrity.
- Removes temporary `jpackage` files after packaging.

The generated installed launcher contains:

    -Dingenious.app.home=$APPDIR
    -Djdk.internal.httpclient.disableHostnameVerification=true
    -Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding

The generated installed launcher must not contain:

    -Dingenious.workspace=...

The absence of `ingenious.workspace` allows the installed application to use the default Workspace or the stored `workspace.base`.

The final generated configuration is located at:

    Dist/release/INGenious.app/Contents/app/INGenious.cfg

The corresponding configuration for an installed application is located at:

    /Applications/INGenious.app/Contents/app/INGenious.cfg

### `scripts/package-macos-pkg.zsh`

Added macOS PKG generation.

The script:

- Packages `INGenious.app` for installation under `/Applications`.
- Produces `Dist/target/INGenious-<version>.pkg`.
- Produces `Dist/target/INGenious-4.0.0.pkg` for version `4.0.0`.
- Receives the package version from Maven `${project.version}`.
- Renders the versioned Distribution definition under `Dist/target`.
- Uses a fixed root payload containing `/Applications/INGenious.app`.
- Validates the Distribution template.
- Validates the rendered Distribution XML.
- Validates applicable package scripts.
- Validates the application signature.
- Uses `pkgbuild` and `productbuild`.

The current proof-of-concept package is unsigned.

### `scripts/macos-pkg/Distribution.xml`

Added the system-level macOS product-archive template for package:

    com.ing.ingenious.pkg

The package version is represented by:

    @APP_VERSION@

It is rendered from Maven `${project.version}` during packaging.

The PKG uses a non-relocatable root payload installed at `/`, with the application installed at:

    /Applications/INGenious.app

### Workspace handling in the macOS installer

The installed application owns first-launch Workspace initialization.

The macOS package must not delete, replace, merge, or migrate:

    ~/Documents/INGenious Workspace

The installer must not probe for or migrate:

    ~/Documents/INGenious/Workspace

The packaged application must contain the `WorkspaceTemplate` required by startup initialization.

If package scripts are present, they must not make the installed application depend on installer-time Workspace creation.

---

## Native Windows Packaging

### `scripts/package-windows-app.ps1`

Added Windows application-image and MSI generation using Java 17 `jpackage`.

The script:

- Produces a Windows application image.
- Produces `Dist/target/INGenious-<version>.msi`.
- Produces `Dist/target/INGenious-4.0.0.msi` for version `4.0.0`.
- Receives the application version from Maven `${project.version}`.
- Bundles a Java runtime.
- Uses the INGenious Windows icon.
- Packages `WorkspaceTemplate` for application-managed first-launch initialization.
- Installs the application under `C:\Program Files\INGenious`.
- Uses `%USERPROFILE%\Documents\INGenious Workspace` as the default installed Workspace.
- Adds Start menu integration.
- Validates required Runtime resources.
- Validates Engine JAR placement.
- Validates installed launcher configuration.
- Prevents the active writable Workspace from being embedded in the application image.
- Preserves the team HTTP JVM settings.

The generated installed Windows launcher configuration must include:

    -Dingenious.app.home=<packaged application home>

It must not include:

    -Dingenious.workspace=...

The absence of `ingenious.workspace` distinguishes the installed application from the portable Windows distribution.

The Windows installer and uninstaller must not delete:

    %USERPROFILE%\Documents\INGenious Workspace

The installer must not probe for or migrate:

    %USERPROFILE%\Documents\INGenious\Workspace

---

## Installed and Portable Distribution Distinction

Launcher configuration is the authoritative distinction between installed and portable distributions.

### Installed package

Installed launchers set:

    -Dingenious.app.home=<packaged application home>

Installed launchers do not set:

    -Dingenious.workspace=...

This enables:

- The platform default installed Workspace.
- Custom installed Workspace-base persistence.
- The Workspace Location command.
- Verified installed Workspace relocation.

### Portable package

Portable launchers set:

    -Dingenious.app.home=<portable root>/Runtime
    -Dingenious.workspace=<portable root>/Workspace

This keeps the portable Workspace fixed to the extracted distribution and prevents installed Workspace customization from changing portable behavior.

No compatibility aliases or legacy launcher conventions are introduced.

---

## Installer Data-Preservation Rules

Workspace content is user data.

Installers and uninstallers must not delete:

    ~/Documents/INGenious Workspace

or:

    %USERPROFILE%\Documents\INGenious Workspace

Installers must not:

- Delete an existing Workspace.
- Replace an existing Workspace.
- Merge template content into a valid existing Workspace.
- Reset renamed or deleted user content.
- Delete user-installed plugins.
- Import an older Workspace automatically.
- Migrate `Documents/INGenious/Workspace`.
- Remove a custom Workspace selected through `workspace.base`.
- remove `~/.ingenious/config.properties` as a side effect of deleting Workspace content.

Application removal and Workspace removal are separate operations. Uninstalling INGenious must preserve user data.

---

## Maven and Build Configuration

### Root `pom.xml`

Added Runtime and Workspace-related build properties.

### `Common/pom.xml`

Updated the generated Runtime Engine POM target to:

    Resources/Runtime/Engine/pom.xml

### `Engine/pom.xml`

Engine source and resource output targets Runtime.

### `IDE/pom.xml`

- IDE dependencies copy to Runtime `lib`.
- Duplicate whole-resource-tree copying was removed from the IDE module.

### `Dist/pom.xml`

Distribution generation separates:

- Release-root launchers and documentation.
- Runtime resources.
- Portable Workspace resources.
- Native package WorkspaceTemplate resources.

Other changes include:

- Packaging work moved to `prepare-package`.
- Added Maven `macos` profile.
- Added Maven `windows` profile.
- The macOS profile runs application-image and PKG packaging scripts.
- The Windows profile runs the PowerShell application-image and MSI script.
- npm operations run against Runtime.

Packaging operations must use Java 17.

The expected local Java configuration is:

    export JAVA_HOME="/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"
    export PATH="$JAVA_HOME/bin:/opt/homebrew/bin:/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin"
    rehash

Java should be verified before Maven packaging:

    mvn -version

JaCoCo `0.8.12` emits unsupported-class-version instrumentation errors if tests are accidentally run against Java 25 classes.

---

## Documentation and Ignore Rules

### `Resources/Readme.md`

Updated documentation covers:

- Runtime and Workspace separation.
- Native macOS application packaging.
- Windows, macOS, and Linux launch methods.
- Installed and portable Workspace behavior.
- Shared Workspace resources.
- Project locations.
- Upgrade behavior.
- Workspace preservation across upgrades.

### `Resources/.gitignore`

Updated ignored paths for the Runtime and Workspace directory structure.

---

## Removed or Replaced Behavior

The following patterns were removed or replaced where applicable:

- Direct reliance on `System.getProperty("user.dir")` for packaged resources.
- Relative `Configuration` paths.
- Relative `Projects` paths.
- Relative `Shared` paths.
- Relative `Tools` paths.
- Relative `lib` paths for Playwright.
- PATH-dependent StoryWriter Java invocation.
- Repeated filesystem-based ING Me font loading.
- Runtime replacement of the bundled macOS Dock icon.
- Writable configuration embedded inside native application images.
- Installed launchers with a fixed `ingenious.workspace`.
- Installed Workspace resolution using `Documents/INGenious/Workspace`.
- Template overlay onto a valid existing Workspace.
- Restart-time saving into a relocated and deleted source Workspace.

No migration or compatibility layer was added for earlier installed Workspace conventions.

---

## Validation Status

### Application validation completed

- Runtime path tests.
- Workspace path tests.
- Workspace initialization tests.
- Plugin path tests.
- Focused Datalib Workspace tests.
- Focused Engine `AppResourcePath` integration tests.
- Complete IDE reactor compilation under Java 17.
- Installed application manual testing.
- Portable menu-visibility testing.
- Exact Workspace transfer testing.
- Relative-path verification.
- File-size verification.
- SHA-256 verification.
- Renamed-content preservation testing.
- No-template-overlay testing.
- Nested destination rejection testing.
- Symbolic-link destination safety testing.
- Automatic restart testing.

### Packaging validation performed

- macOS Apple Silicon application-image build.
- macOS PKG build.
- Generated application version validation.
- Generated classpath validation.
- Installed JVM-option validation.
- Bundled Java architecture validation.
- macOS application signature validation.
- Packaged Runtime resource validation.
- Packaged Engine JAR validation.
- Packaged `WorkspaceTemplate` validation.
- StoryWriter packaged launch-path validation.
- Playwright packaged recording validation.
- IDE packaged font validation.
- StoryWriter packaged font validation.

### Required installed artifact properties

The generated macOS configuration must contain:

    java-options=-Dingenious.app.home=$APPDIR

It must not contain:

    java-options=-Dingenious.workspace=...

The Windows installed launcher configuration follows the same distinction.

Portable launchers must continue to contain:

    -Dingenious.workspace=<portable root>/Workspace

Installer and uninstaller logic must not delete user Workspace content.

### Generated artifacts

Observed generated artifacts include:

    Dist/release/INGenious.app
    Dist/target/INGenious-4.0.0.pkg
    Dist/release/Runtime/ingenious-ide-4.0.0.jar
    Dist/release/Runtime/lib/ingenious-engine-4.0.0.jar

The Windows packaging workflow produces:

    Dist/target/INGenious-4.0.0.msi

when executed successfully on the required Windows packaging environment.

---

## Known Limitation

The current macOS PKG is unsigned.

Production distribution requires the appropriate Apple signing and distribution process, including applicable application signing, installer signing, notarization, and stapling.

This limitation does not change Runtime resolution, Workspace behavior, installer data-preservation rules, or the installed-versus-portable launcher distinction.

---

## Filesystem Classification Reference

Every filesystem dependency belongs to one of the following categories.

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
- `WorkspaceTemplate`

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
- User-installed plugins

### Project-specific

Resolve relative resources against the selected project directory.

### External tools

Use an explicit or configured external dependency path.

Filesystem access should not introduce new assumptions based on:

    user.dir
    ./Configuration
    ./Projects
    ./Shared
    ./Tools
    ./lib
    ./web

---

## Handoff Summary

The INGenious packaging architecture now separates replaceable application resources from persistent user data.

The key invariants are:

1. Runtime is application-owned.
2. Workspace is user-owned.
3. Installed applications default to `Documents/INGenious Workspace`.
4. A custom installed location stores only `workspace.base`.
5. The final custom Workspace is `<workspace.base>/INGenious Workspace`.
6. Installed launchers set `ingenious.app.home` but not `ingenious.workspace`.
7. Portable launchers set both `ingenious.app.home` and `ingenious.workspace`.
8. Valid existing Workspaces are authoritative.
9. `WorkspaceTemplate` is not overlaid onto a valid Workspace.
10. Installers and uninstallers must not delete Workspace data.

## Next Steps


