#!/bin/bash
# INGenious launcher (macOS) — dispatches between IDE and CLI.
#
#   No arguments       -> launches the IDE (com.ing.ide.main.Main)
#   Any arguments      -> invokes the CLI (com.ing.engine.core.Control)
#
# The .command extension makes this file double-clickable in Finder.
# For terminal use, prefer the no-extension sibling 'ingenious'.
#
# Examples:
#   double-click in Finder                               # opens the GUI
#   ./ingenious.command run CLIDemo/APIBasics/GetUsers   # CLI: test case
#   ./ingenious.command run CLIDemo/R1/Smoke             # CLI: test set

DIRNAME="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
cd "$DIRNAME"

APP_CLASSPATH="lib/*:lib/clib/*"

if [ "$#" -eq 0 ]; then
    # No args -> IDE
    exec java -Xms128m -Xmx1024m -Dfile.encoding=UTF-8 \
        -Djdk.internal.httpclient.disableHostnameVerification=true \
        -Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding \
        -cp "ingenious-ide-${project.version}.jar:$APP_CLASSPATH" \
        com.ing.ide.main.Main
else
    # Args -> CLI
    exec java -Xms128m -Xmx1024m -Dfile.encoding=UTF-8 \
        -Djdk.internal.httpclient.disableHostnameVerification=true \
        -Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding \
        -cp "$APP_CLASSPATH" \
        com.ing.engine.core.Control "$@"
fi
