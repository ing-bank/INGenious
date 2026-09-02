#!/bin/zsh

set -euo pipefail

fail() {
  print -u2 -- ""
  print -u2 -- "ERROR: $1"
  exit 1
}

(( $# == 3 )) ||
  fail "Usage: package-macos-zip.zsh <application-version> <arm64|x86_64> <application-path>"

readonly APP_VERSION="$1"
readonly TARGET_ARCH="$2"
readonly SOURCE_APP="$3"

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

print -r -- "$APP_VERSION" |
  grep -Eq '^[0-9]+([.][0-9]+){0,2}$' ||
  fail "Invalid application version: $APP_VERSION"

readonly SCRIPT_DIR="${0:A:h}"
readonly REPO_ROOT="${SCRIPT_DIR:h}"
readonly RELEASE_SOURCE="$REPO_ROOT/Dist/release"
readonly STAGING_ROOT="$REPO_ROOT/Dist/target/macos-zip/$TARGET_ARCH"
readonly ARCHIVE_ROOT="$STAGING_ROOT/INGenious-$APP_VERSION-macos-$TARGET_ARCH"
readonly STAGED_APP="$ARCHIVE_ROOT/INGenious.app"
readonly OUTPUT_ZIP="$REPO_ROOT/Dist/target/INGenious-$APP_VERSION-macos-$TARGET_ARCH.zip"
readonly SOURCE_LAUNCHER="$SOURCE_APP/Contents/MacOS/INGenious"
readonly SOURCE_JVM="$SOURCE_APP/Contents/runtime/Contents/Home/lib/server/libjvm.dylib"

[[ "$(uname -s)" == "Darwin" ]] ||
  fail "The macOS ZIP can only be built on macOS"

[[ -d "$RELEASE_SOURCE/Runtime" ]] ||
  fail "Release Runtime is missing: $RELEASE_SOURCE/Runtime"

[[ -d "$RELEASE_SOURCE/Workspace" ]] ||
  fail "Release Workspace is missing: $RELEASE_SOURCE/Workspace"

[[ -d "$SOURCE_APP/Contents" ]] ||
  fail "Source application is missing or invalid: $SOURCE_APP"

[[ -x "$SOURCE_LAUNCHER" ]] ||
  fail "Source application launcher is missing: $SOURCE_LAUNCHER"

[[ -f "$SOURCE_JVM" ]] ||
  fail "Source application JVM is missing: $SOURCE_JVM"

launcher_info="$(file "$SOURCE_LAUNCHER")"
jvm_info="$(file "$SOURCE_JVM")"

print -- "$launcher_info"
print -- "$jvm_info"

[[ "$launcher_info" == *"$EXPECTED_ARCH"* ]] ||
  fail "Source launcher is not $EXPECTED_ARCH"

[[ "$jvm_info" == *"$EXPECTED_ARCH"* ]] ||
  fail "Source JVM is not $EXPECTED_ARCH"

codesign --verify --deep --strict --verbose=2 "$SOURCE_APP"

print -- ""
print -- "========================================"
print -- " INGenious macOS $TARGET_ARCH ZIP packaging"
print -- "========================================"
print -- ""

rm -rf -- "$STAGING_ROOT"
rm -f -- "$OUTPUT_ZIP"

mkdir -p -- "$ARCHIVE_ROOT"

for item in \
  Runtime \
  Workspace \
  Readme.md \
  ingenious \
  ingenious.command \
  ingenious.bat
do
  [[ -e "$RELEASE_SOURCE/$item" ]] ||
    fail "Required release item is missing: $RELEASE_SOURCE/$item"

  /usr/bin/ditto \
    "$RELEASE_SOURCE/$item" \
    "$ARCHIVE_ROOT/$item"
done

runtime_lib="$ARCHIVE_ROOT/Runtime/lib"

removed_javafx_count=0

while IFS= read -r foreign_javafx_jar; do
  rm -f -- "$foreign_javafx_jar"
  removed_javafx_count=$((removed_javafx_count + 1))
done < <(
  find "$runtime_lib" -maxdepth 1 -type f -print |
    grep -E '/javafx-.+-(linux|win)[.]jar$|/javafx-.+-mac(-aarch64)?[.]jar$' |
    grep -v -- "-${JAVAFX_CLASSIFIER}.jar$" ||
    true
)

matching_javafx_count="$(
  find "$runtime_lib"     -maxdepth 1     -type f     -name "javafx-*-${JAVAFX_CLASSIFIER}.jar" |
    wc -l |
    tr -d ' '
)"

[[ "$matching_javafx_count" == "6" ]] ||
  fail "Expected 6 ${JAVAFX_CLASSIFIER} Runtime JavaFX JARs, found: $matching_javafx_count"

foreign_javafx_count="$(
  {
    find "$runtime_lib" -maxdepth 1 -type f -print |
      grep -E '/javafx-.+-(linux|win)[.]jar$|/javafx-.+-mac(-aarch64)?[.]jar$' |
      grep -v -- "-${JAVAFX_CLASSIFIER}.jar$"
  } 2>/dev/null || true
)"

foreign_javafx_count="$(
  print -r -- "$foreign_javafx_count" |
    grep -c . ||
    true
)"

[[ "$foreign_javafx_count" == "0" ]] ||
  fail "Portable Runtime still contains foreign JavaFX JARs"

print -- "Removed foreign Runtime JavaFX JARs: $removed_javafx_count"
print -- "Matching Runtime JavaFX JARs: $matching_javafx_count"

/usr/bin/ditto "$SOURCE_APP" "$STAGED_APP"

if [[ -f "$REPO_ROOT/LICENSE" ]]; then
  /usr/bin/ditto \
    "$REPO_ROOT/LICENSE" \
    "$ARCHIVE_ROOT/LICENSE"
fi

chmod 755 "$ARCHIVE_ROOT/ingenious"
chmod 755 "$ARCHIVE_ROOT/ingenious.command"

staged_launcher_info="$(file "$STAGED_APP/Contents/MacOS/INGenious")"
staged_jvm_info="$(file "$STAGED_APP/Contents/runtime/Contents/Home/lib/server/libjvm.dylib")"

[[ "$staged_launcher_info" == *"$EXPECTED_ARCH"* ]] ||
  fail "Staged launcher is not $EXPECTED_ARCH"

[[ "$staged_jvm_info" == *"$EXPECTED_ARCH"* ]] ||
  fail "Staged JVM is not $EXPECTED_ARCH"

codesign --verify --deep --strict --verbose=2 "$STAGED_APP"

(
  cd "$STAGING_ROOT"
  /usr/bin/ditto \
    -c \
    -k \
    --sequesterRsrc \
    --keepParent \
    "${ARCHIVE_ROOT:t}" \
    "$OUTPUT_ZIP"
)

[[ -f "$OUTPUT_ZIP" ]] ||
  fail "ZIP archive was not created: $OUTPUT_ZIP"

zip_launcher_entry="$(
  unzip -Z1 "$OUTPUT_ZIP" |
    grep '/INGenious[.]app/Contents/MacOS/INGenious$' |
    head -1
)"

[[ -n "$zip_launcher_entry" ]] ||
  fail "ZIP does not contain the native INGenious launcher"

unzip -Z1 "$OUTPUT_ZIP" |
  grep -E '/Workspace/plugins(/|$)' >/dev/null ||
  fail "ZIP does not contain Workspace/plugins"

if unzip -Z1 "$OUTPUT_ZIP" |
  grep -E '/Runtime/plugins(/|$)' >/dev/null; then
  fail "ZIP unexpectedly contains Runtime/plugins"
fi

print -- "ZIP built successfully:"
print -- "  $OUTPUT_ZIP"
print -- ""
print -- "Architecture:"
print -- "  $TARGET_ARCH"
