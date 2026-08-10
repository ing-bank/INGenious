@echo off
REM INGenious launcher (Windows) — dispatches between IDE and CLI.
REM
REM   No arguments       -> launches the IDE (com.ing.ide.main.Main)
REM   Any arguments      -> invokes the CLI (com.ing.engine.core.Control)
REM
REM Examples:
REM   ingenious                                  # opens the GUI (double-click works too)
REM   ingenious run CLIDemo/APIBasics/GetUsers   # CLI: test case
REM   ingenious run CLIDemo/R1/Smoke             # CLI: test set
REM   ingenious --help                           # CLI: top-level help

pushd %~dp0

SET APP_CLASSPATH=lib\*;lib\clib\*

IF "%~1" == "" (
    REM No args -> IDE (windowed, detached)
    start javaw -Xms128m -Xmx1024m -Dfile.encoding=UTF-8 ^
        -Djdk.internal.httpclient.disableHostnameVerification=true ^
        -Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding ^
        -cp "ingenious-ide-${project.version}.jar;%APP_CLASSPATH%" ^
        com.ing.ide.main.Main
) ELSE (
    REM Args -> CLI
    java -Xms128m -Xmx1024m -Dfile.encoding=UTF-8 ^
        -Djdk.internal.httpclient.disableHostnameVerification=true ^
        -Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding ^
        -cp "%APP_CLASSPATH%" ^
        com.ing.engine.core.Control %*
)

popd
