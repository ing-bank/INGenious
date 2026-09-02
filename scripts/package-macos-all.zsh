#!/bin/zsh

set -euo pipefail

fail() {
  print -u2 -- ""
  print -u2 -- "ERROR: $1"
  exit 1
}

(( $# == 1 )) ||
  fail "Usage: package-macos-all.zsh <application-version>"

readonly APP_VERSION="$1"
readonly SCRIPT_DIR="${0:A:h}"
readonly REPO_ROOT="${SCRIPT_DIR:h}"

print -r -- "$APP_VERSION" |
  grep -Eq '^[0-9]+([.][0-9]+){0,2}$' ||
  fail "Invalid application version: $APP_VERSION"

find_jdk() {
  local expected_arch="$1"
  local candidate
  local java_info

  for candidate in \
    "${INGENIOUS_MACOS_ARM64_JDK:-}" \
    "${INGENIOUS_MACOS_X86_64_JDK:-}" \
    "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home" \
    "$HOME/Library/Java/JavaVirtualMachines/temurin-17-x64.jdk/Contents/Home" \
    /Library/Java/JavaVirtualMachines/*.jdk/Contents/Home \
    "$HOME"/Library/Java/JavaVirtualMachines/*.jdk/Contents/Home
  do
    [[ -n "$candidate" ]] || continue
    [[ -x "$candidate/bin/java" ]] || continue
    [[ -x "$candidate/bin/jpackage" ]] || continue

    java_info="$(file "$candidate/bin/java")"

    if [[ "$java_info" == *"$expected_arch"* ]]; then
      print -r -- "$candidate"
      return 0
    fi
  done

  return 1
}

readonly HOST_ARCH="$(uname -m)"

[[ "$HOST_ARCH" == "arm64" ]] ||
  fail "Dual macOS packaging currently requires an Apple Silicon build host"

if ! /usr/bin/arch -x86_64 /usr/bin/true 2>/dev/null; then
  fail "Rosetta x86_64 execution is unavailable"
fi

ARM64_JDK="$(find_jdk arm64)" ||
  fail "Could not locate an ARM64 Java 17 JDK with jpackage"

X86_64_JDK="$(find_jdk x86_64)" ||
  fail "Could not locate an x86_64 Java 17 JDK with jpackage"

readonly ARM64_JDK
readonly X86_64_JDK

readonly ARM64_APP="$REPO_ROOT/Dist/target/macos-arm64/INGenious.app"
readonly X86_64_APP="$REPO_ROOT/Dist/target/macos-x86_64/INGenious.app"
readonly RELEASE_APP="$REPO_ROOT/Dist/release/INGenious.app"

print -- ""
print -- "========================================"
print -- " INGenious dual macOS packaging"
print -- "========================================"
print -- ""
print -- "ARM64 JDK:"
print -- "  $ARM64_JDK"
print -- "Intel JDK:"
print -- "  $X86_64_JDK"
print -- ""

rm -rf -- \
  "$REPO_ROOT/Dist/target/macos-arm64" \
  "$REPO_ROOT/Dist/target/macos-x86_64" \
  "$REPO_ROOT/Dist/target/macos-pkg/arm64" \
  "$REPO_ROOT/Dist/target/macos-pkg/x86_64" \
  "$REPO_ROOT/Dist/target/macos-zip/arm64" \
  "$REPO_ROOT/Dist/target/macos-zip/x86_64"

rm -f -- \
  "$REPO_ROOT/Dist/target/INGenious-${APP_VERSION}-macos-arm64.pkg" \
  "$REPO_ROOT/Dist/target/INGenious-${APP_VERSION}-macos-x86_64.pkg" \
  "$REPO_ROOT/Dist/target/INGenious-${APP_VERSION}-macos-arm64.zip" \
  "$REPO_ROOT/Dist/target/INGenious-${APP_VERSION}-macos-x86_64.zip"

workspace_plugins_source="$REPO_ROOT/Resources/Workspace/plugins"
workspace_plugins_release="$REPO_ROOT/Dist/release/Workspace/plugins"

[[ -d "$workspace_plugins_source" ]] ||
  fail "Workspace plugin template is missing: $workspace_plugins_source"

mkdir -p -- "$workspace_plugins_release"
/usr/bin/ditto   "$workspace_plugins_source"   "$workspace_plugins_release"

[[ -d "$workspace_plugins_release" ]] ||
  fail "Release Workspace plugins directory was not created"

print -- "OK: Workspace/plugins is available for packaging"
print -- ""
print -- "[1/6] Building Apple Silicon application"

"$SCRIPT_DIR/package-macos-app.zsh" \
  "$APP_VERSION" \
  arm64 \
  "$ARM64_JDK"

print -- ""
print -- "[2/6] Building Intel application"

"$SCRIPT_DIR/package-macos-app.zsh" \
  "$APP_VERSION" \
  x86_64 \
  "$X86_64_JDK"

print -- ""
print -- "[3/6] Building Apple Silicon installer and ZIP"

"$SCRIPT_DIR/package-macos-pkg.zsh" \
  "$APP_VERSION" \
  arm64 \
  "$ARM64_APP"

"$SCRIPT_DIR/package-macos-zip.zsh" \
  "$APP_VERSION" \
  arm64 \
  "$ARM64_APP"

print -- ""
print -- "[4/6] Building Intel installer and ZIP"

"$SCRIPT_DIR/package-macos-pkg.zsh" \
  "$APP_VERSION" \
  x86_64 \
  "$X86_64_APP"

"$SCRIPT_DIR/package-macos-zip.zsh" \
  "$APP_VERSION" \
  x86_64 \
  "$X86_64_APP"

print -- ""
print -- "[5/6] Validating architecture-specific artifacts"

for artifact in \
  "$REPO_ROOT/Dist/target/INGenious-${APP_VERSION}-macos-arm64.pkg" \
  "$REPO_ROOT/Dist/target/INGenious-${APP_VERSION}-macos-x86_64.pkg" \
  "$REPO_ROOT/Dist/target/INGenious-${APP_VERSION}-macos-arm64.zip" \
  "$REPO_ROOT/Dist/target/INGenious-${APP_VERSION}-macos-x86_64.zip"
do
  [[ -f "$artifact" ]] ||
    fail "Expected artifact is missing: $artifact"

  print -- "OK: $artifact"
done

arm_launcher_info="$(file "$ARM64_APP/Contents/MacOS/INGenious")"
intel_launcher_info="$(file "$X86_64_APP/Contents/MacOS/INGenious")"
release_launcher_info="$(file "$RELEASE_APP/Contents/MacOS/INGenious")"

[[ "$arm_launcher_info" == *"arm64"* ]] ||
  fail "Apple Silicon application is not arm64"

[[ "$intel_launcher_info" == *"x86_64"* ]] ||
  fail "Intel application is not x86_64"

[[ "$release_launcher_info" == *"$HOST_ARCH"* ]] ||
  fail "Dist/release/INGenious.app is not compatible with the host"

print -- ""
print -- "Cleaning temporary macOS packaging directories"

rm -rf -- \
  "$REPO_ROOT/Dist/target/jpackage" \
  "$REPO_ROOT/Dist/target/macos-arm64" \
  "$REPO_ROOT/Dist/target/macos-x86_64" \
  "$REPO_ROOT/Dist/target/macos-pkg" \
  "$REPO_ROOT/Dist/target/macos-zip"

for temporary_path in \
  "$REPO_ROOT/Dist/target/jpackage" \
  "$REPO_ROOT/Dist/target/macos-arm64" \
  "$REPO_ROOT/Dist/target/macos-x86_64" \
  "$REPO_ROOT/Dist/target/macos-pkg" \
  "$REPO_ROOT/Dist/target/macos-zip"
do
  [[ ! -e "$temporary_path" ]] ||
    fail "Temporary packaging path could not be removed: $temporary_path"
done

print -- "OK: temporary macOS packaging directories removed"
print -- ""
print -- "[6/6] Dual macOS packaging completed successfully"
print -- ""
print -- "Host release application:"
print -- "  $RELEASE_APP"
print -- ""
print -- "Generated artifacts:"
print -- "  Dist/target/INGenious-${APP_VERSION}-macos-arm64.pkg"
print -- "  Dist/target/INGenious-${APP_VERSION}-macos-x86_64.pkg"
print -- "  Dist/target/INGenious-${APP_VERSION}-macos-arm64.zip"
print -- "  Dist/target/INGenious-${APP_VERSION}-macos-x86_64.zip"
