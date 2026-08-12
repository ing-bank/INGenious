#!/bin/bash

# INGenious launcher (macOS) - dispatches between IDE and CLI.
#
#   No arguments      -> launches the IDE (com.ing.ide.main.Main)
#   Any arguments     -> invokes the CLI (com.ing.engine.core.Control)
#
# The .command extension makes this file double-clickable in Finder.
# For terminal use, prefer the no-extension sibling 'ingenious'.
#
# Examples:
#   double-click in Finder                              # opens the GUI
#   ./ingenious.command run CLIDemo/APIBasics/GetUsers  # CLI: test case
#   ./ingenious.command run CLIDemo/R1/Smoke            # CLI: test set

INSTALL_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)" || exit 1
RUNTIME_DIR="$INSTALL_DIR/Runtime"
WORKSPACE_DIR="$INSTALL_DIR/Workspace"
APP_CLASSPATH="$RUNTIME_DIR/lib/*:$RUNTIME_DIR/lib/clib/*"

JVM_OPTIONS=(
  "-Xms128m"
  "-Xmx1024m"
  "-Dfile.encoding=UTF-8"
  "-Djdk.internal.httpclient.disableHostnameVerification=true"
  "-Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding"
  "-Dingenious.app.home=$RUNTIME_DIR"
  "-Dingenious.workspace=$WORKSPACE_DIR"
)

if [[ $# -eq 0 ]]; then
  # No args -> IDE
  exec java "${JVM_OPTIONS[@]}" \
    -cp "$RUNTIME_DIR/ingenious-ide-${project.version}.jar:$APP_CLASSPATH" \
    com.ing.ide.main.Main
else
  # Args -> CLI
  exec java "${JVM_OPTIONS[@]}" \
    -cp "$APP_CLASSPATH" \
    com.ing.engine.core.Control "$@"
fi