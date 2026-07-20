@echo off
REM INGenious CLI wrapper script for Windows
REM Usage: ingenious.bat <command> [options]

set SCRIPT_DIR=%~dp0
java -Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding -cp "%SCRIPT_DIR%lib\*" com.ing.engine.core.Control %*
