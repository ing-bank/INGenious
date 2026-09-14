@echo off
setlocal
REM INGenious launcher (Windows) — dispatches between IDE and CLI.
REM
REM   No arguments       -> launches the IDE (com.ing.ide.main.Main)
REM   Any arguments      -> invokes the CLI (com.ing.engine.core.Control)
REM
REM Examples:
REM   ingenious                                  # opens the GUI (double-click works too)
REM   ingenious run CLIDemo/APIBasics/GetUsers   # CLI: run a test case
REM   ingenious run CLIDemo/R1/Smoke             # CLI: run a test set
REM   ingenious --help                           # CLI: top-level help

set "INSTALL_DIR=%~dp0"
set "RUNTIME_DIR=%INSTALL_DIR%Runtime"
set "WORKSPACE_DIR=%INSTALL_DIR%Workspace"

REM The release bundle ships JavaFX natives for every OS/arch. Loading more
REM than one platform's javafx-* jars on the same classpath causes
REM "No toolkit found" at startup, so keep only the win jars for this platform.
setlocal enabledelayedexpansion
set "APP_CLASSPATH="
for %%F in ("%RUNTIME_DIR%\lib\*.jar") do (
    set "SKIP=0"
    echo %%~nxF| findstr /i /c:"javafx-" >nul
    if !errorlevel! equ 0 (
        echo %%~nxF| findstr /i /c:"-win.jar" >nul
        if !errorlevel! neq 0 set "SKIP=1"
    )
    if "!SKIP!"=="0" (
        if defined APP_CLASSPATH (set "APP_CLASSPATH=!APP_CLASSPATH!;%%F") else (set "APP_CLASSPATH=%%F")
    )
)
for %%F in ("%RUNTIME_DIR%\lib\clib\*.jar") do (
    if defined APP_CLASSPATH (set "APP_CLASSPATH=!APP_CLASSPATH!;%%F") else (set "APP_CLASSPATH=%%F")
)
endlocal & set "APP_CLASSPATH=%APP_CLASSPATH%"

if "%~1" == "" (
    REM No args -> IDE (windowed, detached)
    start "" javaw -Xms128m -Xmx1024m -Dfile.encoding=UTF-8 ^
        -Djdk.internal.httpclient.disableHostnameVerification=true ^
        -Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding ^
        "-Dingenious.app.home=%RUNTIME_DIR%" ^
        "-Dingenious.workspace=%WORKSPACE_DIR%" ^
        -cp "%RUNTIME_DIR%\ingenious-ide-${project.version}.jar;%APP_CLASSPATH%" ^
        com.ing.ide.main.Main
) else (
    REM Args -> CLI
    java -Xms128m -Xmx1024m -Dfile.encoding=UTF-8 ^
        -Djdk.internal.httpclient.disableHostnameVerification=true ^
        -Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding ^
        "-Dingenious.app.home=%RUNTIME_DIR%" ^
        "-Dingenious.workspace=%WORKSPACE_DIR%" ^
        -cp "%APP_CLASSPATH%" ^
        com.ing.engine.core.Control %*
)

endlocal
