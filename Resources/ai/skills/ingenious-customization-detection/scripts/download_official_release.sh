#!/bin/bash
################################################################################
# download_official_release.sh
#
# Purpose: Clone official INGenious repository and checkout specific version
# Usage: ./download_official_release.sh <version> [output_dir]
# Example: ./download_official_release.sh 2.3 /tmp/ingenious-official
#
# Prerequisites:
# - Git must be installed
# - Internet connection available
# - Sufficient disk space (~100-500MB)
################################################################################

VERSION="$1"
OUTPUT_DIR="${2:-$(mktemp -d -t ingenious-official-XXXXXX)}"

if [ -z "$VERSION" ]; then
    echo "Usage: $0 <version> [output_dir]"
    echo "Example: $0 2.3 /tmp/ingenious-official"
    exit 1
fi

echo "Downloading official INGenious release v$VERSION..."
echo "Output directory: $OUTPUT_DIR"

# Clone official repository
git clone https://github.com/ing-bank/INGenious.git "$OUTPUT_DIR" || {
    echo "❌ Failed to clone repository"
    exit 1
}

cd "$OUTPUT_DIR" || exit 1

# List available tags
echo "Available tags:"
git tag -l | head -10

# Try different tag patterns to checkout
if git checkout "v${VERSION}" 2>/dev/null; then
    echo "✓ Checked out v${VERSION}"
elif git checkout "${VERSION}" 2>/dev/null; then
    echo "✓ Checked out ${VERSION}"
elif git checkout "release-${VERSION}" 2>/dev/null; then
    echo "✓ Checked out release-${VERSION}"
elif git checkout "Release-${VERSION}" 2>/dev/null; then
    echo "✓ Checked out Release-${VERSION}"
else
    echo "⚠️  Warning: Could not find exact tag for version $VERSION"
    echo "Available tags:"
    git tag -l
    echo ""
    echo "Using main branch instead"
    git checkout main 2>/dev/null || git checkout master 2>/dev/null
fi

# Verify checkout
current_version=$(git describe --tags --exact-match 2>/dev/null || git rev-parse --short HEAD)
echo "Current version: $current_version"
echo "Repository downloaded to: $OUTPUT_DIR"

exit 0
