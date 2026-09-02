#!/bin/zsh

set -euo pipefail

fail() {
  print -u2 -- ""
  print -u2 -- "ERROR: $1"
  exit 1
}

(( $# == 3 )) ||
  fail "Usage: package-macos-pkg.zsh <application-version> <arm64|x86_64> <application-path>"

readonly PACKAGE_VERSION="$1"
readonly TARGET_ARCH="$2"
readonly SOURCE_APP="$3"

case "$TARGET_ARCH" in
  arm64)
    readonly EXPECTED_ARCH="arm64"
    ;;
  x86_64)
    readonly EXPECTED_ARCH="x86_64"
    ;;
  *)
    fail "Unsupported macOS architecture: $TARGET_ARCH"
    ;;
esac

print -r -- "$PACKAGE_VERSION" |
  grep -Eq '^[0-9]+([.][0-9]+){0,2}$' ||
  fail "Invalid application version: $PACKAGE_VERSION"

readonly SCRIPT_DIR="${0:A:h}"
readonly REPO_ROOT="${SCRIPT_DIR:h}"

readonly WORKSPACE_SOURCE="$REPO_ROOT/Resources/Workspace"
readonly POSTINSTALL_SOURCE="$SCRIPT_DIR/macos-pkg/postinstall"
readonly CLI_WRAPPER_SOURCE="$SCRIPT_DIR/macos-pkg/ingenious"
readonly DISTRIBUTION_TEMPLATE="$SCRIPT_DIR/macos-pkg/Distribution.xml"

readonly STAGING_ROOT="$REPO_ROOT/Dist/target/macos-pkg/$TARGET_ARCH"
readonly PAYLOAD_ROOT="$STAGING_ROOT/root"
readonly PACKAGE_SCRIPTS="$STAGING_ROOT/package-scripts"
readonly COMPONENT_PACKAGE="$STAGING_ROOT/INGenious-component.pkg"
readonly COMPONENT_PLIST="$STAGING_ROOT/components.plist"
readonly RENDERED_DISTRIBUTION="$STAGING_ROOT/Distribution.xml"
readonly OUTPUT_PACKAGE="$REPO_ROOT/Dist/target/INGenious-${PACKAGE_VERSION}-macos-${TARGET_ARCH}.pkg"

readonly PACKAGE_IDENTIFIER="com.ing.ingenious.pkg"

print -- ""
print -- "========================================"
print -- " INGenious macOS $TARGET_ARCH PKG packaging"
print -- "========================================"
print -- ""

[[ "$(uname -s)" == "Darwin" ]] ||
  fail "The macOS package can only be built on macOS."

[[ -x /usr/bin/pkgbuild ]] ||
  fail "pkgbuild is not available."

[[ -x /usr/bin/productbuild ]] ||
  fail "productbuild is not available."

[[ -d "$SOURCE_APP/Contents" ]] ||
  fail "Source application is missing or invalid: $SOURCE_APP"

source_launcher="$SOURCE_APP/Contents/MacOS/INGenious"
source_jvm="$SOURCE_APP/Contents/runtime/Contents/Home/lib/server/libjvm.dylib"

[[ -x "$source_launcher" ]] ||
  fail "Source application launcher is missing or not executable: $source_launcher"

[[ -f "$source_jvm" ]] ||
  fail "Source application JVM is missing: $source_jvm"

launcher_info="$(file "$source_launcher")"
jvm_info="$(file "$source_jvm")"

print -- "$launcher_info"
print -- "$jvm_info"

[[ "$launcher_info" == *"$EXPECTED_ARCH"* ]] ||
  fail "Source application launcher is not $EXPECTED_ARCH"

[[ "$jvm_info" == *"$EXPECTED_ARCH"* ]] ||
  fail "Source application JVM is not $EXPECTED_ARCH"

[[ -d "$WORKSPACE_SOURCE/Configuration" ]] ||
  fail "Workspace template is missing Configuration."

[[ -d "$WORKSPACE_SOURCE/Projects" ]] ||
  fail "Workspace template is missing Projects."

[[ -d "$WORKSPACE_SOURCE/Shared" ]] ||
  fail "Workspace template is missing Shared."

[[ -d "$WORKSPACE_SOURCE/plugins" ]] ||
  fail "Workspace template is missing plugins."

[[ -f "$POSTINSTALL_SOURCE" ]] ||
  fail "Post-install script is missing: $POSTINSTALL_SOURCE"
[[ -f "$CLI_WRAPPER_SOURCE" ]] ||
  fail "CLI wrapper is missing: $CLI_WRAPPER_SOURCE"
[[ -f "$DISTRIBUTION_TEMPLATE" ]] ||
  fail "Distribution template is missing: $DISTRIBUTION_TEMPLATE"

/usr/bin/xmllint --noout "$DISTRIBUTION_TEMPLATE" ||
  fail "Distribution template is invalid."

[[ "$(grep -o '@APP_VERSION@' "$DISTRIBUTION_TEMPLATE" | wc -l | tr -d ' ')" == "1" ]] ||
  fail "Distribution template must contain exactly one @APP_VERSION@ token."

zsh -n "$POSTINSTALL_SOURCE" ||
  fail "Post-install script failed syntax validation."

zsh -n "$CLI_WRAPPER_SOURCE" ||
  fail "CLI wrapper failed syntax validation."

print -- "[1/5] Validating the $TARGET_ARCH application"

/usr/bin/codesign \
  --verify \
  --deep \
  --strict \
  --verbose=2 \
  "$SOURCE_APP"

print -- "OK: application signature is valid"

print -- ""
print -- "[2/5] Recreating package staging"

rm -rf -- "$STAGING_ROOT"
rm -f -- "$OUTPUT_PACKAGE"

mkdir -p -- "$PAYLOAD_ROOT/Applications"
mkdir -p -- "$PAYLOAD_ROOT/usr/local/bin"
mkdir -p -- "$PACKAGE_SCRIPTS"

/usr/bin/sed   "s/@APP_VERSION@/$PACKAGE_VERSION/g"   "$DISTRIBUTION_TEMPLATE"   > "$RENDERED_DISTRIBUTION"

/usr/bin/xmllint --noout "$RENDERED_DISTRIBUTION" ||
  fail "Rendered Distribution definition is invalid."

grep -Fq "version=\"$PACKAGE_VERSION\"" "$RENDERED_DISTRIBUTION" ||
  fail "Rendered Distribution definition has the wrong package version."

if grep -Fq '@APP_VERSION@' "$RENDERED_DISTRIBUTION"; then
  fail "Rendered Distribution definition still contains the version token."
fi

print -- ""
print -- "[3/5] Staging application and Workspace template"

/usr/bin/ditto \
  "$SOURCE_APP" \
  "$PAYLOAD_ROOT/Applications/INGenious.app"

/usr/bin/ditto \
  "$CLI_WRAPPER_SOURCE" \
  "$PAYLOAD_ROOT/usr/local/bin/ingenious"

/usr/bin/ditto \
  "$POSTINSTALL_SOURCE" \
  "$PACKAGE_SCRIPTS/postinstall"

/usr/bin/ditto \
  "$WORKSPACE_SOURCE" \
  "$PACKAGE_SCRIPTS/Workspace"

chmod 755 "$PACKAGE_SCRIPTS/postinstall"
chmod 755 "$PAYLOAD_ROOT/usr/local/bin/ingenious"

[[ -d "$PAYLOAD_ROOT/Applications/INGenious.app/Contents" ]] ||
  fail "Staged application bundle is invalid."

[[ -x "$PAYLOAD_ROOT/usr/local/bin/ingenious" ]] ||
  fail "Staged CLI wrapper is missing or not executable."

[[ -d "$PACKAGE_SCRIPTS/Workspace/Configuration" ]] ||
  fail "Staged Workspace is missing Configuration."

[[ -d "$PACKAGE_SCRIPTS/Workspace/Projects" ]] ||
  fail "Staged Workspace is missing Projects."

[[ -d "$PACKAGE_SCRIPTS/Workspace/Shared" ]] ||
  fail "Staged Workspace is missing Shared."

[[ -d "$PACKAGE_SCRIPTS/Workspace/plugins" ]] ||
  fail "Staged Workspace is missing plugins."

print -- ""
print -- "[4/5] Building non-relocatable component package"

/usr/bin/pkgbuild \
  --analyze \
  --root "$PAYLOAD_ROOT" \
  "$COMPONENT_PLIST"

/usr/bin/plutil -lint "$COMPONENT_PLIST" >/dev/null ||
  fail "Generated component property list is invalid."

component_count="$(
  /usr/libexec/PlistBuddy -c "Print" "$COMPONENT_PLIST" |
    /usr/bin/grep -c '^    Dict {'
)"

[[ "$component_count" == "1" ]] ||
  fail "Expected exactly one application component, found: $component_count"

/usr/libexec/PlistBuddy \
  -c "Set :0:BundleIsRelocatable false" \
  "$COMPONENT_PLIST"

component_path="$(
  /usr/libexec/PlistBuddy \
    -c "Print :0:RootRelativeBundlePath" \
    "$COMPONENT_PLIST"
)"

[[ "$component_path" == "Applications/INGenious.app" ]] ||
  fail "Unexpected application component path: $component_path"

relocatable="$(
  /usr/libexec/PlistBuddy \
    -c "Print :0:BundleIsRelocatable" \
    "$COMPONENT_PLIST"
)"

[[ "$relocatable" == "false" ]] ||
  fail "Application component is still relocatable."

print -- "OK: application bundle is non-relocatable"

/usr/bin/pkgbuild \
  --root "$PAYLOAD_ROOT" \
  --component-plist "$COMPONENT_PLIST" \
  --scripts "$PACKAGE_SCRIPTS" \
  --identifier "$PACKAGE_IDENTIFIER" \
  --version "$PACKAGE_VERSION" \
  --install-location / \
  --ownership recommended \
  "$COMPONENT_PACKAGE"

[[ -f "$COMPONENT_PACKAGE" ]] ||
  fail "pkgbuild did not create the component package."

print -- ""
print -- "[5/5] Building fixed-destination product archive"

/usr/bin/productbuild \
  --distribution "$RENDERED_DISTRIBUTION" \
  --package-path "$STAGING_ROOT" \
  "$OUTPUT_PACKAGE"

[[ -f "$OUTPUT_PACKAGE" ]] ||
  fail "productbuild did not create the expected product archive."

/usr/sbin/pkgutil --check-signature "$OUTPUT_PACKAGE" || true

print -- ""
print -- "Package built successfully:"
print -- "  $OUTPUT_PACKAGE"
print -- ""
print -- "Package identifier:"
print -- "  $PACKAGE_IDENTIFIER"
print -- ""
print -- "This POC package is unsigned."
print -- "It has not been installed automatically."
