#!/bin/bash
# Detect INGenious version from multiple sources
# Usage: ./detect-version.sh <ingenious-root-path>

set -e

ROOT_PATH="$1"

# Validate input
if [[ -z "$ROOT_PATH" ]]; then
    echo "ERROR: Provide INGenious root path" >&2
    echo "Usage: $0 <ingenious-root-path>" >&2
    exit 1
fi

if [[ ! -d "$ROOT_PATH" ]]; then
    echo "ERROR: Path does not exist: $ROOT_PATH" >&2
    exit 1
fi

VERSION=""

# Strategy 1: Check pom.xml files for version tags
if [[ -f "$ROOT_PATH/Engine/pom.xml" ]]; then
    VERSION=$(grep -m 1 "<version>" "$ROOT_PATH/Engine/pom.xml" 2>/dev/null | \
        sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d '[:space:]')
fi

# Strategy 2: Check manifest files in JAR files
if [[ -z "$VERSION" ]] && [[ -d "$ROOT_PATH/Engine/target" ]]; then
    for jar in "$ROOT_PATH/Engine/target"/*.jar; do
        if [[ -f "$jar" ]]; then
            VERSION=$(unzip -p "$jar" META-INF/MANIFEST.MF 2>/dev/null | \
                grep "Implementation-Version" | cut -d: -f2 | tr -d '[:space:]')
            [[ -n "$VERSION" ]] && break
        fi
    done
fi

# Strategy 3: Check property files
if [[ -z "$VERSION" ]]; then
    for prop_file in "$ROOT_PATH/Configuration"/*.properties "$ROOT_PATH/Configuration"/*.Properties; do
        if [[ -f "$prop_file" ]]; then
            VERSION=$(grep -i "VERSION" "$prop_file" 2>/dev/null | head -1 | \
                cut -d= -f2 | tr -d '[:space:]')
            [[ -n "$VERSION" ]] && break
        fi
    done
fi

# Strategy 4: Check Git tags (if repository is git-enabled)
if [[ -z "$VERSION" ]] && [[ -d "$ROOT_PATH/.git" ]]; then
    VERSION=$(cd "$ROOT_PATH" && git describe --tags --always 2>/dev/null)
fi

# Strategy 5: Search for version constants in source code
if [[ -z "$VERSION" ]] && [[ -d "$ROOT_PATH/Engine/src" ]]; then
    VERSION=$(grep -r "VERSION\s*=\s*" "$ROOT_PATH/Engine/src" --include="*.java" 2>/dev/null | \
        head -1 | sed 's/.*"\(.*\)".*/\1/')
fi

# Output result
if [[ -n "$VERSION" ]]; then
    echo "$VERSION"
    exit 0
else
    echo "UNKNOWN"
    exit 1
fi
