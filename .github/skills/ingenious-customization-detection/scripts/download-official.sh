#!/bin/bash
# Download official INGenious release from GitHub
# Usage: ./download-official.sh <version> <output-directory>

set -e

VERSION="$1"
OUTPUT_DIR="$2"

# Validate input
if [[ -z "$VERSION" ]] || [[ -z "$OUTPUT_DIR" ]]; then
    echo "ERROR: Missing required arguments" >&2
    echo "Usage: $0 <version> <output-directory>" >&2
    echo "Example: $0 2.3 /tmp/ingenious-official" >&2
    exit 1
fi

# Check if git is available
if ! command -v git &> /dev/null; then
    echo "ERROR: git is not installed" >&2
    exit 1
fi

# Create output directory
mkdir -p "$OUTPUT_DIR"

echo "Cloning official INGenious repository..."
git clone https://github.com/ing-bank/INGenious.git "$OUTPUT_DIR/official-ingenious"

cd "$OUTPUT_DIR/official-ingenious"

echo "Listing available tags..."
git tag -l | sort -V

echo ""
echo "Attempting to checkout version: $VERSION"

# Try different tag patterns
CHECKOUT_SUCCESS=false

for tag_pattern in "v${VERSION}" "${VERSION}" "release-${VERSION}" "Release-${VERSION}" "V${VERSION}"; do
    if git checkout "$tag_pattern" 2>/dev/null; then
        echo "✓ Successfully checked out tag: $tag_pattern"
        CHECKOUT_SUCCESS=true
        break
    fi
done

if [[ "$CHECKOUT_SUCCESS" == "false" ]]; then
    echo "⚠️  WARNING: Could not find exact tag for version $VERSION" >&2
    echo "Available tags:" >&2
    git tag -l | grep -i "$VERSION" || echo "  (no matches found)" >&2
    echo "" >&2
    echo "Using main branch instead (may have differences)" >&2
    git checkout main 2>/dev/null || git checkout master 2>/dev/null
    exit 2
fi

# Verify checkout
CHECKED_OUT_TAG=$(git describe --tags --exact-match 2>/dev/null || echo "")
if [[ -n "$CHECKED_OUT_TAG" ]]; then
    echo "Verified: Currently on tag $CHECKED_OUT_TAG"
fi

echo "✓ Official INGenious downloaded to: $OUTPUT_DIR/official-ingenious"
exit 0
