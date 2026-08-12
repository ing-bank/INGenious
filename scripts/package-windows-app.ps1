param(
    [Parameter(Mandatory = $true)]
    [string]$JdkHome
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Fail {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Message
    )

    throw "ERROR: $Message"
}

if ($env:OS -ne "Windows_NT") {
    Fail "The Windows app-image can only be built on Windows"
}

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir

$Release = Join-Path $RepoRoot "Dist\release"
$ReleaseRuntime = Join-Path $Release "Runtime"
$ReleaseWorkspace = Join-Path $Release "Workspace"
$WorkspaceSource = Join-Path $RepoRoot "Resources\Workspace"
$ReleaseApp = Join-Path $Release "INGenious-Windows"
$InstallerOutput = Join-Path $RepoRoot "Dist\target"
$Installer = Join-Path $InstallerOutput "INGenious-3.1.0.msi"

$InputDir = Join-Path $RepoRoot "Dist\target\jpackage\windows-input"
$OutputDir = Join-Path $RepoRoot "Dist\target\jpackage\windows-output"
$GeneratedApp = Join-Path $OutputDir "INGenious"

$AppDir = Join-Path $GeneratedApp "app"
$ConfigFile = Join-Path $AppDir "INGenious.cfg"
$Launcher = Join-Path $GeneratedApp "INGenious.exe"
$JvmLibrary = Join-Path $GeneratedApp "runtime\bin\server\jvm.dll"
$Jpackage = Join-Path $JdkHome "bin\jpackage.exe"
$AppIcon = Join-Path $RepoRoot "Resources\INGenious.ico"

Write-Host ""
Write-Host "========================================"
Write-Host " INGenious Windows app-image packaging"
Write-Host "========================================"
Write-Host "Repository: $RepoRoot"
Write-Host ""

if (-not (Test-Path -LiteralPath $ReleaseRuntime -PathType Container)) {
    Fail "Release Runtime does not exist: $ReleaseRuntime"
}

if (-not (Test-Path -LiteralPath $ReleaseWorkspace -PathType Container)) {
    Fail "Release Workspace does not exist: $ReleaseWorkspace"
}

if (-not (Test-Path -LiteralPath $WorkspaceSource -PathType Container)) {
    Fail "Workspace template does not exist: $WorkspaceSource"
}

$GuiJar = Join-Path $ReleaseRuntime "ingenious-ide-3.1.0.jar"

if (-not (Test-Path -LiteralPath $GuiJar -PathType Leaf)) {
    Fail "Release Runtime is missing ingenious-ide-3.1.0.jar"
}

if (-not (Test-Path -LiteralPath $Jpackage -PathType Leaf)) {
    Fail "jpackage.exe is missing: $Jpackage"
}

if (-not (Test-Path -LiteralPath $AppIcon -PathType Leaf)) {
    Fail "Windows application icon is missing: $AppIcon"
}

$JpackageVersion = (& $Jpackage --version 2>&1 | Out-String).Trim()

if (-not $JpackageVersion.StartsWith("17")) {
    Fail "Expected jpackage 17, but detected: $JpackageVersion"
}

Write-Host "Using jpackage $JpackageVersion from:"
Write-Host "  $Jpackage"
Write-Host ""

Write-Host "[1/6] Recreating jpackage input"

if (Test-Path -LiteralPath $InputDir) {
    Remove-Item -LiteralPath $InputDir -Recurse -Force
}

New-Item -ItemType Directory -Path $InputDir -Force | Out-Null

Get-ChildItem -LiteralPath $ReleaseRuntime -Force |
    Copy-Item -Destination $InputDir -Recurse -Force

Copy-Item `
    -LiteralPath $WorkspaceSource `
    -Destination (Join-Path $InputDir "WorkspaceTemplate") `
    -Recurse `
    -Force

Write-Host ""
Write-Host "[2/6] Validating staged Runtime"

$RequiredInputItems = @(
    (Join-Path $InputDir "lib"),
    (Join-Path $InputDir "Engine"),
    (Join-Path $InputDir "plugins"),
    (Join-Path $InputDir "Tools"),
    (Join-Path $InputDir "web"),
    (Join-Path $InputDir "Configuration"),
    (Join-Path $InputDir "WorkspaceTemplate"),
    (Join-Path $InputDir "ingenious-ide-3.1.0.jar")
)

foreach ($Item in $RequiredInputItems) {
    if (-not (Test-Path -LiteralPath $Item)) {
        Fail "Required staged resource is missing: $Item"
    }

    Write-Host "OK: $Item"
}

$EngineJars = @(
    Get-ChildItem -LiteralPath $InputDir -Recurse -File |
        Where-Object { $_.Name -eq "ingenious-engine-3.1.0.jar" }
)

if ($EngineJars.Count -ne 1) {
    Fail "Expected one staged Engine JAR; found $($EngineJars.Count)"
}

$ExpectedEngineJar = Join-Path $InputDir "lib\ingenious-engine-3.1.0.jar"

if ($EngineJars[0].FullName -ne $ExpectedEngineJar) {
    Fail "Engine JAR is not in the required input\lib directory"
}

$ExcludedItems = @(
    (Join-Path $InputDir "Workspace"),
    (Join-Path $InputDir "Projects"),
    (Join-Path $InputDir "Shared"),
    (Join-Path $InputDir "ingenious"),
    (Join-Path $InputDir "ingenious.bat"),
    (Join-Path $InputDir "ingenious.command"),
    (Join-Path $InputDir "Readme.md")
)

foreach ($Item in $ExcludedItems) {
    if (Test-Path -LiteralPath $Item) {
        Fail "Non-Runtime content is inside the app input: $Item"
    }
}

Write-Host "OK: Workspace and traditional launchers are excluded"

Write-Host ""
Write-Host "[3/6] Recreating the Windows app-image"

if (Test-Path -LiteralPath $OutputDir) {
    Remove-Item -LiteralPath $OutputDir -Recurse -Force
}

New-Item -ItemType Directory -Path $OutputDir -Force | Out-Null

$JpackageArguments = @(
    "--type", "app-image",
    "--name", "INGenious",
    "--app-version", "3.1.0",
    "--vendor", "ING",
    "--description", "INGenious Playwright Studio",
    "--icon", $AppIcon,
    "--input", $InputDir,
    "--dest", $OutputDir,
    "--main-jar", "ingenious-ide-3.1.0.jar",
    "--main-class", "com.ing.ide.main.Main",
    "--jlink-options", "--strip-debug --no-man-pages --no-header-files",
    "--java-options", '-Dingenious.app.home=$APPDIR',
    "--java-options", "-Xms128m",
    "--java-options", "-Xmx1024m",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--java-options", "-Djdk.internal.httpclient.disableHostnameVerification=true",
    "--java-options", "-Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding",
    "--verbose"
)

& $Jpackage @JpackageArguments

if ($LASTEXITCODE -ne 0) {
    Fail "jpackage exited with code $LASTEXITCODE"
}

if (-not (Test-Path -LiteralPath $GeneratedApp -PathType Container)) {
    Fail "jpackage did not create $GeneratedApp"
}

Write-Host ""
Write-Host "[4/6] Validating the generated application"

$RequiredPackagedItems = @(
    $Launcher,
    $JvmLibrary,
    $ConfigFile,
    (Join-Path $AppDir "lib"),
    (Join-Path $AppDir "Engine"),
    (Join-Path $AppDir "plugins"),
    (Join-Path $AppDir "Tools"),
    (Join-Path $AppDir "web"),
    (Join-Path $AppDir "Configuration"),
    (Join-Path $AppDir "WorkspaceTemplate"),
    (Join-Path $AppDir "ingenious-ide-3.1.0.jar")
)

foreach ($Item in $RequiredPackagedItems) {
    if (-not (Test-Path -LiteralPath $Item)) {
        Fail "Required packaged resource is missing: $Item"
    }
}

$WritableConfigurationItems = @(
    (Join-Path $AppDir "Configuration\.enc"),
    (Join-Path $AppDir "Configuration\ExplorerConfig.properties"),
    (Join-Path $AppDir "Configuration\XPLOR_SETTINGS.json"),
    (Join-Path $AppDir "Configuration\app.settings")
)

foreach ($Item in $WritableConfigurationItems) {
    if (Test-Path -LiteralPath $Item) {
        Fail "Writable Configuration must not be packaged inside the app: $Item"
    }
}

$PackagedEngineJars = @(
    Get-ChildItem -LiteralPath $AppDir -Recurse -File |
        Where-Object { $_.Name -eq "ingenious-engine-3.1.0.jar" }
)

if ($PackagedEngineJars.Count -ne 1) {
    Fail "Expected one packaged Engine JAR; found $($PackagedEngineJars.Count)"
}

$ExpectedPackagedEngineJar = Join-Path $AppDir "lib\ingenious-engine-3.1.0.jar"

if ($PackagedEngineJars[0].FullName -ne $ExpectedPackagedEngineJar) {
    Fail "Packaged Engine JAR is not in app\lib"
}

$ConfigText = Get-Content -LiteralPath $ConfigFile -Raw

if (-not $ConfigText.Contains(
    "java-options=-Djdk.internal.httpclient.disableHostnameVerification=true"
)) {
    Fail "Hostname verification option is missing from INGenious.cfg"
}

if (-not $ConfigText.Contains('java-options=-Dingenious.app.home=$APPDIR')) {
    Fail "ingenious.app.home is missing from INGenious.cfg"
}

if ($ConfigText.Contains("java-options=-Dingenious.workspace=")) {
    Fail "The installed Windows launcher must discover Local AppData at runtime"
}

if (-not $ConfigText.Contains("app.mainclass=com.ing.ide.main.Main")) {
    Fail "GUI main class is missing from INGenious.cfg"
}

if (-not $ConfigText.Contains('app.classpath=$APPDIR\ingenious-ide-3.1.0.jar')) {
    Fail "GUI main JAR is missing from INGenious.cfg"
}

if (Test-Path -LiteralPath (Join-Path $AppDir "Workspace")) {
    Fail "Workspace must remain outside the Windows app-image"
}

if (Test-Path -LiteralPath $ReleaseApp) {
    Remove-Item -LiteralPath $ReleaseApp -Recurse -Force
}

Copy-Item -LiteralPath $GeneratedApp -Destination $ReleaseApp -Recurse -Force

if (-not (Test-Path -LiteralPath $ReleaseApp -PathType Container)) {
    Fail "Windows app-image was not copied into the release"
}

if (-not (Test-Path -LiteralPath (Join-Path $ReleaseApp "INGenious.exe") -PathType Leaf)) {
    Fail "Release Windows application is missing INGenious.exe"
}

Write-Host "OK: INGenious-Windows added to the existing release"

Write-Host ""
Write-Host "[5/6] Building machine-wide Windows MSI"

if (Test-Path -LiteralPath $Installer) {
    Remove-Item -LiteralPath $Installer -Force
}

$MsiArguments = @(
    "--type", "msi",
    "--name", "INGenious",
    "--app-version", "3.1.0",
    "--vendor", "ING",
    "--description", "INGenious Playwright Studio",
    "--app-image", $GeneratedApp,
    "--dest", $InstallerOutput,
    "--install-dir", "INGenious",
    "--win-menu",
    "--win-menu-group", "INGenious",
    "--verbose"
)

& $Jpackage @MsiArguments

if ($LASTEXITCODE -ne 0) {
    Fail "MSI jpackage exited with code $LASTEXITCODE"
}

if (-not (Test-Path -LiteralPath $Installer -PathType Leaf)) {
    Fail "jpackage did not create the expected MSI: $Installer"
}

Write-Host ""
Write-Host "[6/6] Windows packaging completed successfully"
Write-Host ""
Write-Host "Application image:"
Write-Host "  $ReleaseApp"
Write-Host ""
Write-Host "Machine-wide installer:"
Write-Host "  $Installer"
Write-Host ""
Write-Host "Installed application:"
Write-Host "  C:\Program Files\INGenious"
Write-Host ""
Write-Host "Per-user Workspace:"
Write-Host "  %LOCALAPPDATA%\INGenious"
Write-Host ""
Write-Host "Start menu:"
Write-Host "  INGenious\INGenious"
Write-Host ""
