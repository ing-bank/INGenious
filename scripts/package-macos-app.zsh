#!/bin/zsh

set -euo pipefail

fail() {
  print -u2 -- "ERROR: $1"
  exit 1
}

(( $# == 3 )) ||
  fail "Usage: package-macos-app.zsh <application-version> <arm64|x86_64> <jdk-home>"

readonly APP_VERSION="$1"
readonly TARGET_ARCH="$2"
readonly JPACKAGE_HOME="$3"

case "$TARGET_ARCH" in
  arm64)
    readonly EXPECTED_ARCH="arm64"
    readonly JAVAFX_CLASSIFIER="mac-aarch64"
    ;;
  x86_64)
    readonly EXPECTED_ARCH="x86_64"
    readonly JAVAFX_CLASSIFIER="mac"
    ;;
  *)
    fail "Unsupported macOS architecture: $TARGET_ARCH"
    ;;
esac

case "$(uname -m)" in
  arm64)
    readonly HOST_ARCH="arm64"
    ;;
  x86_64)
    readonly HOST_ARCH="x86_64"
    ;;
  *)
    fail "Unsupported macOS host architecture: $(uname -m)"
    ;;
esac

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
readonly ARCH_OUTPUT_ROOT="$REPO_ROOT/Dist/target/macos-$TARGET_ARCH"
readonly ARCH_APP="$ARCH_OUTPUT_ROOT/INGenious.app"
readonly JPACKAGE_ROOT="$REPO_ROOT/Dist/target/jpackage/$TARGET_ARCH"
readonly INPUT="$JPACKAGE_ROOT/input"
readonly OUTPUT="$JPACKAGE_ROOT/output"
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

readonly JAVA="$JPACKAGE_HOME/bin/java"
readonly JPACKAGE="$JPACKAGE_HOME/bin/jpackage"
readonly JPACKAGE_JVM="$JPACKAGE_HOME/lib/server/libjvm.dylib"

[[ -x "$JAVA" ]] ||
  fail "Java is missing or not executable: $JAVA"

[[ -f "$JPACKAGE_JVM" ]] ||
  fail "JVM library is missing: $JPACKAGE_JVM"

[[ -x "$JPACKAGE" ]] ||
  fail "Java 17 jpackage is missing or not executable: $JPACKAGE"

jpackage_version="$("$JPACKAGE" --version 2>&1)"
[[ "$jpackage_version" == 17* ]] ||
  fail "Expected jpackage 17, but detected: $jpackage_version"

java_info="$(file "$JAVA")"
jpackage_info="$(file "$JPACKAGE")"
jpackage_jvm_info="$(file "$JPACKAGE_JVM")"

print -- "$java_info"
print -- "$jpackage_info"
print -- "$jpackage_jvm_info"

[[ "$java_info" == *"$EXPECTED_ARCH"* ]] ||
  fail "Selected Java is not $EXPECTED_ARCH"

[[ "$jpackage_info" == *"$EXPECTED_ARCH"* ]] ||
  fail "Selected jpackage is not $EXPECTED_ARCH"

[[ "$jpackage_jvm_info" == *"$EXPECTED_ARCH"* ]] ||
  fail "Selected JDK JVM is not $EXPECTED_ARCH"

print -- "Using $TARGET_ARCH jpackage $jpackage_version from:"
print -- "  $JPACKAGE"

print -- "[1/5] Recreating jpackage input"

rm -rf -- "$INPUT"
mkdir -p -- "$INPUT"
ditto "$RELEASE_RUNTIME" "$INPUT"

print -- ""
print -- "Keeping JavaFX classifier: $JAVAFX_CLASSIFIER"

removed_javafx_count=0

while IFS= read -r foreign_javafx_jar; do
  rm -f -- "$foreign_javafx_jar"
  print -- "Removed: ${foreign_javafx_jar:t}"
  removed_javafx_count=$((removed_javafx_count + 1))
done < <(
  find "$INPUT/lib" -maxdepth 1 -type f -print |
    grep -E '/javafx-.+-(linux|win)[.]jar$|/javafx-.+-mac(-aarch64)?[.]jar$' |
    grep -v -- "-${JAVAFX_CLASSIFIER}.jar$" ||
    true
)

matching_javafx_count="$(
  find "$INPUT/lib"     -maxdepth 1     -type f     -name "javafx-*-${JAVAFX_CLASSIFIER}.jar" |
    wc -l |
    tr -d ' '
)"

[[ "$matching_javafx_count" == "6" ]] ||
  fail "Expected 6 ${JAVAFX_CLASSIFIER} JavaFX JARs, found: $matching_javafx_count"

print -- "Removed JavaFX JARs: $removed_javafx_count"
print -- "Matching JavaFX JARs: $matching_javafx_count"

print -- ""
print -- "[2/5] Validating staged resources"

for item in \
  "$INPUT/lib" \
  "$INPUT/Engine" \
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

[[ "$launcher_info" == *"$EXPECTED_ARCH"* ]] ||
  fail "Native launcher is not $EXPECTED_ARCH"

[[ "$jvm_info" == *"$EXPECTED_ARCH"* ]] ||
  fail "Bundled JVM is not $EXPECTED_ARCH"

codesign --verify --deep --strict --verbose=2 "$GUI_APP"

rm -rf -- "$ARCH_OUTPUT_ROOT"
mkdir -p -- "$ARCH_OUTPUT_ROOT"
ditto "$GUI_APP" "$ARCH_APP"

[[ -d "$ARCH_APP/Contents/app" ]] ||
  fail "Architecture-specific app is invalid: $ARCH_APP"

[[ ! -e "$ARCH_APP/Contents/app/Workspace" ]] ||
  fail "Workspace must remain outside the architecture-specific app bundle"

arch_launcher_info="$(file "$ARCH_APP/Contents/MacOS/INGenious")"
arch_jvm_info="$(file "$ARCH_APP/Contents/runtime/Contents/Home/lib/server/libjvm.dylib")"

[[ "$arch_launcher_info" == *"$EXPECTED_ARCH"* ]] ||
  fail "Preserved launcher is not $EXPECTED_ARCH"

[[ "$arch_jvm_info" == *"$EXPECTED_ARCH"* ]] ||
  fail "Preserved JVM is not $EXPECTED_ARCH"

codesign --verify --deep --strict --verbose=2 "$ARCH_APP"

print -- "OK: preserved $TARGET_ARCH app at $ARCH_APP"

if [[ "$TARGET_ARCH" == "$HOST_ARCH" ]]; then
  rm -rf -- "$RELEASE_APP"
  ditto "$ARCH_APP" "$RELEASE_APP"

  [[ -d "$RELEASE_APP/Contents/app" ]] ||
    fail "Host-compatible release app is invalid: $RELEASE_APP"

  [[ ! -e "$RELEASE_APP/Contents/app/Workspace" ]] ||
    fail "Workspace must remain outside the release app bundle"

  codesign --verify --deep --strict --verbose=2 "$RELEASE_APP"

  print -- "OK: host-compatible INGenious.app added to the release"
else
  print -- "Host architecture is $HOST_ARCH; release app was not replaced by $TARGET_ARCH"
fi

print -- ""
print -- "Cleaning temporary jpackage files"
rm -rf -- "$JPACKAGE_ROOT"

[[ ! -e "$JPACKAGE_ROOT" ]] ||
  fail "Temporary $TARGET_ARCH jpackage directory could not be removed"

print -- "OK: temporary jpackage files removed"

print -- ""
print -- "[5/5] macOS app-image completed successfully"
print -- ""
print -- "Architecture-specific application:"
print -- "  $ARCH_APP"
print -- ""
print -- "Host release application:"
print -- "  $RELEASE_APP"
print -- ""
print -- "Application Workspace:"
print -- "  $RELEASE_WORKSPACE"
print -- ""
