#!/bin/zsh

set -euo pipefail

fail() {
  print -u2 -- "ERROR: $1"
  exit 1
}

(( $# == 1 )) ||
  fail "Usage: package-macos-app.zsh <application-version>"

readonly APP_VERSION="$1"

print -r -- "$APP_VERSION" |
  grep -Eq '^[0-9]+([.][0-9]+){0,2}$' ||
  fail "Invalid application version: $APP_VERSION"

readonly IDE_JAR_NAME="ingenious-ide-${APP_VERSION}.jar"
readonly ENGINE_JAR_NAME="ingenious-engine-${APP_VERSION}.jar"

readonly SCRIPT_DIR="${0:A:h}"
readonly REPO_ROOT="${SCRIPT_DIR:h}"
readonly RELEASE="$REPO_ROOT/Dist/release"
readonly RELEASE_RUNTIME="$RELEASE/Runtime"
readonly RELEASE_WORKSPACE="$RELEASE/Workspace"
readonly RELEASE_APP="$RELEASE/INGenious.app"
readonly INPUT="$REPO_ROOT/Dist/target/jpackage/input"
readonly OUTPUT="$REPO_ROOT/Dist/target/jpackage/output"
readonly GUI_APP="$OUTPUT/INGenious.app"
readonly APP_DIR="$GUI_APP/Contents/app"
readonly CFG="$APP_DIR/INGenious.cfg"
readonly LAUNCHER="$GUI_APP/Contents/MacOS/INGenious"
readonly JVM_LIBRARY="$GUI_APP/Contents/runtime/Contents/Home/lib/server/libjvm.dylib"
readonly APP_ICON="$REPO_ROOT/Resources/INGenious.icns"

print -- ""
print -- "========================================"
print -- " INGenious macOS app-image packaging"
print -- "========================================"
print -- "Repository: $REPO_ROOT"
print -- ""

[[ "$(uname -s)" == "Darwin" ]] ||
  fail "The macOS app-image can only be built on macOS"

[[ -d "$RELEASE" ]] ||
  fail "Release directory does not exist: $RELEASE"

[[ -d "$RELEASE_RUNTIME" ]] ||
  fail "Release Runtime directory does not exist: $RELEASE_RUNTIME"

[[ -d "$RELEASE_WORKSPACE" ]] ||
  fail "Release Workspace directory does not exist: $RELEASE_WORKSPACE"

[[ -f "$RELEASE_RUNTIME/${IDE_JAR_NAME}" ]] ||
  fail "Release Runtime is missing ${IDE_JAR_NAME}"

[[ -f "$APP_ICON" ]] ||
  fail "Application icon is missing: $APP_ICON"

/usr/libexec/java_home -v 17 >/dev/null 2>&1 ||
  fail "A Java 17 JDK could not be located"

JPACKAGE_HOME="$(/usr/libexec/java_home -v 17)"
readonly JPACKAGE_HOME
readonly JPACKAGE="$JPACKAGE_HOME/bin/jpackage"

[[ -x "$JPACKAGE" ]] ||
  fail "Java 17 jpackage is missing or not executable: $JPACKAGE"

jpackage_version="$("$JPACKAGE" --version 2>&1)"
[[ "$jpackage_version" == 17* ]] ||
  fail "Expected jpackage 17, but detected: $jpackage_version"

print -- "Using jpackage $jpackage_version from:"
print -- "  $JPACKAGE"

print -- "[1/5] Recreating jpackage input"

rm -rf -- "$INPUT"
mkdir -p -- "$INPUT"
ditto "$RELEASE_RUNTIME" "$INPUT"

print -- ""
print -- "Removing JavaFX libraries for non-macOS platforms"

removed_javafx_count=0

while IFS= read -r foreign_javafx_jar; do
  rm -f -- "$foreign_javafx_jar"
  print -- "Removed: ${foreign_javafx_jar:t}"
  removed_javafx_count=$((removed_javafx_count + 1))
done < <(
  find "$INPUT/lib" -maxdepth 1 -type f -print |
    grep -E '/javafx-.+-(linux|win)[.]jar$' ||
    true
)

print -- "Removed JavaFX JARs: $removed_javafx_count"

print -- ""
print -- "[2/5] Validating staged resources"

for item in \
  "$INPUT/lib" \
  "$INPUT/Engine" \
  "$INPUT/plugins" \
  "$INPUT/Tools" \
  "$INPUT/web" \
  "$INPUT/Configuration" \
  "$INPUT/${IDE_JAR_NAME}"
do
  [[ -e "$item" ]] ||
    fail "Required staged resource is missing: $item"

  print -- "OK: $item"
done

engine_jars=(
  "${(@f)$(find "$INPUT" -type f -name "$ENGINE_JAR_NAME" -print)}"
)

if (( ${#engine_jars[@]} != 1 )); then
  print -u2 -- "Unexpected Engine JAR count: ${#engine_jars[@]}"

  for item in "${engine_jars[@]}"; do
    print -u2 -- "$item"
  done

  fail "Expected exactly one ${ENGINE_JAR_NAME}"
fi

[[ "${engine_jars[1]}" == "$INPUT/lib/${ENGINE_JAR_NAME}" ]] ||
  fail "Engine JAR is not in the required input/lib location"

print -- "OK: exactly one Engine JAR at ${engine_jars[1]}"

for item in \
  "$INPUT/Workspace" \
  "$INPUT/Projects" \
  "$INPUT/Shared" \
  "$INPUT/ingenious" \
  "$INPUT/ingenious.bat" \
  "$INPUT/ingenious.command" \
  "$INPUT/Readme.md"
do
  [[ ! -e "$item" ]] ||
    fail "Traditional release content must not be packaged inside the app: $item"
done

print -- "OK: Workspace and traditional launchers are excluded from the app input"

print -- ""
print -- "[3/5] Recreating the macOS app-image"

rm -rf -- "$OUTPUT"
mkdir -p -- "$OUTPUT"

"$JPACKAGE" \
  --type app-image \
  --name INGenious \
  --app-version "$APP_VERSION" \
  --vendor "ING" \
  --description "INGenious Playwright Studio" \
  --icon "$APP_ICON" \
  --input "$INPUT" \
  --dest "$OUTPUT" \
  --main-jar "$IDE_JAR_NAME" \
  --main-class com.ing.ide.main.Main \
  --jlink-options "--strip-debug --no-man-pages --no-header-files" \
  --java-options '-Dingenious.app.home=$APPDIR' \
  --java-options "-Xms128m" \
  --java-options "-Xmx1024m" \
  --java-options "-Dfile.encoding=UTF-8" \
  --java-options "-Djdk.internal.httpclient.disableHostnameVerification=true" \
  --java-options "-Djdk.httpclient.allowRestrictedHeaders=host,connection,content-length,upgrade,expect,via,date,accept-encoding" \
  --mac-package-identifier com.ing.ingenious \
  --mac-package-name INGenious \
  --verbose

[[ -d "$GUI_APP" ]] ||
  fail "jpackage did not create $GUI_APP"

print -- ""
print -- "[4/5] Validating the generated application"

[[ -x "$LAUNCHER" ]] ||
  fail "Native launcher is missing or not executable: $LAUNCHER"

[[ -f "$JVM_LIBRARY" ]] ||
  fail "Bundled JVM library is missing: $JVM_LIBRARY"

[[ -f "$CFG" ]] ||
  fail "Launcher configuration is missing: $CFG"

for item in \
  "$APP_DIR/lib" \
  "$APP_DIR/Engine" \
  "$APP_DIR/plugins" \
  "$APP_DIR/Tools" \
  "$APP_DIR/web" \
  "$APP_DIR/Configuration" \
  "$APP_DIR/${IDE_JAR_NAME}"
do
  [[ -e "$item" ]] ||
    fail "Required packaged resource is missing: $item"
done

for item in \
  "$APP_DIR/Configuration/.enc" \
  "$APP_DIR/Configuration/ExplorerConfig.properties" \
  "$APP_DIR/Configuration/XPLOR_SETTINGS.json" \
  "$APP_DIR/Configuration/app.settings"
do
  [[ ! -e "$item" ]] ||
    fail "Writable Configuration must not be packaged inside the app: $item"
done

packaged_engine_jars=(
  "${(@f)$(find "$APP_DIR" -type f -name "$ENGINE_JAR_NAME" -print)}"
)

if (( ${#packaged_engine_jars[@]} != 1 )); then
  print -u2 -- "Unexpected packaged Engine JAR count: ${#packaged_engine_jars[@]}"

  for item in "${packaged_engine_jars[@]}"; do
    print -u2 -- "$item"
  done

  fail "Expected exactly one packaged ${ENGINE_JAR_NAME}"
fi

[[ "${packaged_engine_jars[1]}" == "$APP_DIR/lib/${ENGINE_JAR_NAME}" ]] ||
  fail "Packaged Engine JAR is not in Contents/app/lib"

grep -Fq 'java-options=-Dingenious.app.home=$APPDIR' "$CFG" ||
  fail "Finder-safe ingenious.app.home option is missing"

if grep -Fq 'java-options=-Dingenious.workspace=' "$CFG"; then
  fail "The native macOS app must discover its Workspace at runtime"
fi

grep -Fq 'java-options=-Djdk.internal.httpclient.disableHostnameVerification=true' "$CFG" ||
  fail "Hostname verification option is missing"

grep -Fq 'app.mainclass=com.ing.ide.main.Main' "$CFG" ||
  fail "GUI main class is missing from launcher configuration"

grep -Fq "app.classpath=\$APPDIR/$IDE_JAR_NAME" "$CFG" ||
  fail "GUI main JAR is missing from launcher configuration"

launcher_info="$(file "$LAUNCHER")"
jvm_info="$(file "$JVM_LIBRARY")"

print -- "$launcher_info"
print -- "$jvm_info"

[[ "$launcher_info" == *"arm64"* ]] ||
  fail "Native launcher is not arm64"

[[ "$jvm_info" == *"arm64"* ]] ||
  fail "Bundled JVM is not arm64"

codesign --verify --deep --strict --verbose=2 "$GUI_APP"

rm -rf -- "$RELEASE_APP"
ditto "$GUI_APP" "$RELEASE_APP"

[[ -d "$RELEASE_APP" ]] ||
  fail "Generated app was not copied into the release: $RELEASE_APP"

[[ -d "$RELEASE_APP/Contents/app" ]] ||
  fail "Release app is missing Contents/app"

[[ ! -e "$RELEASE_APP/Contents/app/Workspace" ]] ||
  fail "Workspace must remain outside the application bundle"

print -- "OK: INGenious.app added to the existing release"

print -- ""
print -- "Cleaning temporary jpackage files"
rm -rf -- "$REPO_ROOT/Dist/target/jpackage"

[[ ! -e "$REPO_ROOT/Dist/target/jpackage" ]] ||
  fail "Temporary jpackage directory could not be removed"

print -- "OK: temporary jpackage files removed"

print -- ""
print -- "[5/5] macOS app-image completed successfully"
print -- ""
print -- "Application:"
print -- "  $RELEASE_APP"
print -- ""
print -- "Application Workspace:"
print -- "  $RELEASE_WORKSPACE"
print -- ""
