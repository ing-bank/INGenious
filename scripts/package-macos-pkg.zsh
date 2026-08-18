#!/bin/zsh

set -euo pipefail

fail() {
  print -u2 -- ""
  print -u2 -- "ERROR: $1"
  exit 1
}

(( $# == 1 )) ||
  fail "Usage: package-macos-pkg.zsh <application-version>"

readonly PACKAGE_VERSION="$1"

print -r -- "$PACKAGE_VERSION" |
  grep -Eq '^[0-9]+([.][0-9]+){0,2}$' ||
  fail "Invalid application version: $PACKAGE_VERSION"

readonly SCRIPT_DIR="${0:A:h}"
readonly REPO_ROOT="${SCRIPT_DIR:h}"

readonly RELEASE_APP="$REPO_ROOT/Dist/release/INGenious.app"
readonly WORKSPACE_SOURCE="$REPO_ROOT/Resources/Workspace"
readonly POSTINSTALL_SOURCE="$SCRIPT_DIR/macos-pkg/postinstall"
readonly DISTRIBUTION_TEMPLATE="$SCRIPT_DIR/macos-pkg/Distribution.xml"

readonly STAGING_ROOT="$REPO_ROOT/Dist/target/macos-pkg"
readonly PAYLOAD_ROOT="$STAGING_ROOT/root"
readonly PACKAGE_SCRIPTS="$STAGING_ROOT/package-scripts"
readonly COMPONENT_PACKAGE="$STAGING_ROOT/INGenious-component.pkg"
readonly RENDERED_DISTRIBUTION="$STAGING_ROOT/Distribution.xml"
readonly OUTPUT_PACKAGE="$REPO_ROOT/Dist/target/INGenious-${PACKAGE_VERSION}.pkg"

readonly PACKAGE_IDENTIFIER="com.ing.ingenious.pkg"

print -- ""
print -- "========================================"
print -- " INGenious macOS PKG packaging"
print -- "========================================"
print -- ""

[[ "$(uname -s)" == "Darwin" ]] ||
  fail "The macOS package can only be built on macOS."

[[ -x /usr/bin/pkgbuild ]] ||
  fail "pkgbuild is not available."

[[ -x /usr/bin/productbuild ]] ||
  fail "productbuild is not available."

[[ -d "$RELEASE_APP/Contents" ]] ||
  fail "Release application is missing or invalid: $RELEASE_APP"

[[ -d "$WORKSPACE_SOURCE/Configuration" ]] ||
  fail "Workspace template is missing Configuration."

[[ -d "$WORKSPACE_SOURCE/Projects" ]] ||
  fail "Workspace template is missing Projects."

[[ -d "$WORKSPACE_SOURCE/Shared" ]] ||
  fail "Workspace template is missing Shared."

[[ -f "$POSTINSTALL_SOURCE" ]] ||
  fail "Post-install script is missing: $POSTINSTALL_SOURCE"
[[ -f "$DISTRIBUTION_TEMPLATE" ]] ||
  fail "Distribution template is missing: $DISTRIBUTION_TEMPLATE"

/usr/bin/xmllint --noout "$DISTRIBUTION_TEMPLATE" ||
  fail "Distribution template is invalid."

[[ "$(grep -o '@APP_VERSION@' "$DISTRIBUTION_TEMPLATE" | wc -l | tr -d ' ')" == "1" ]] ||
  fail "Distribution template must contain exactly one @APP_VERSION@ token."

zsh -n "$POSTINSTALL_SOURCE" ||
  fail "Post-install script failed syntax validation."

print -- "[1/5] Validating the release application"

/usr/bin/codesign \
  --verify \
  --deep \
  --strict \
  --verbose=2 \
  "$RELEASE_APP"

print -- "OK: application signature is valid"

print -- ""
print -- "[2/5] Recreating package staging"

rm -rf -- "$STAGING_ROOT"
rm -f -- "$OUTPUT_PACKAGE"

mkdir -p -- "$PAYLOAD_ROOT/Applications"
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
  "$RELEASE_APP" \
  "$PAYLOAD_ROOT/Applications/INGenious.app"

/usr/bin/ditto \
  "$POSTINSTALL_SOURCE" \
  "$PACKAGE_SCRIPTS/postinstall"

/usr/bin/ditto \
  "$WORKSPACE_SOURCE" \
  "$PACKAGE_SCRIPTS/Workspace"

chmod 755 "$PACKAGE_SCRIPTS/postinstall"

[[ -d "$PAYLOAD_ROOT/Applications/INGenious.app/Contents" ]] ||
  fail "Staged application bundle is invalid."

[[ -d "$PACKAGE_SCRIPTS/Workspace/Configuration" ]] ||
  fail "Staged Workspace is missing Configuration."

[[ -d "$PACKAGE_SCRIPTS/Workspace/Projects" ]] ||
  fail "Staged Workspace is missing Projects."

[[ -d "$PACKAGE_SCRIPTS/Workspace/Shared" ]] ||
  fail "Staged Workspace is missing Shared."

print -- ""
print -- "[4/5] Building component package"

/usr/bin/pkgbuild \
  --root "$PAYLOAD_ROOT" \
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
