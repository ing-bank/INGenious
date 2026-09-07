#!/bin/bash
################################################################################
# detect_version.sh
#
# Purpose: Detect INGenious version from various sources
# Usage: ./detect_version.sh <path_to_ingenious>
# Output: Prints detected version to stdout
#
# Multi-strategy version detection:
# 1. Check Engine/pom.xml for <version> tag
# 2. Check Configuration/package.properties
# 3. Check Global Settings.Properties
# 4. Check JAR manifest files
# 5. Check Git tags (if repository)
################################################################################

INGENIOUS_PATH="${1:-.}"

# Strategy 1: Check Engine/pom.xml
if [ -f "$INGENIOUS_PATH/Engine/pom.xml" ]; then
    version=$(grep -m 1 "<version>" "$INGENIOUS_PATH/Engine/pom.xml" | sed 's/.*<version>\(.*\)<\/version>.*/\1/')
    if [ -n "$version" ]; then
        echo "$version"
        exit 0
    fi
fi

# Strategy 2: Check Configuration/*.properties
if [ -d "$INGENIOUS_PATH/Configuration" ]; then
    version=$(grep -r "VERSION\|version" "$INGENIOUS_PATH/Configuration/"*.properties 2>/dev/null | grep -o "[0-9]\+\.[0-9]\+\(\.[0-9]\+\)\?" | head -1)
    if [ -n "$version" ]; then
        echo "$version"
        exit 0
    fi
fi

# Strategy 3: Check JAR manifest
if [ -d "$INGENIOUS_PATH/Engine/target" ]; then
    jar_file=$(find "$INGENIOUS_PATH/Engine/target" -name "*.jar" | head -1)
    if [ -n "$jar_file" ]; then
        version=$(unzip -p "$jar_file" META-INF/MANIFEST.MF 2>/dev/null | grep "Implementation-Version" | cut -d: -f2 | tr -d ' ')
        if [ -n "$version" ]; then
            echo "$version"
            exit 0
        fi
    fi
fi

# Strategy 4: Check Git tags (if git repository)
if [ -d "$INGENIOUS_PATH/.git" ]; then
    version=$(cd "$INGENIOUS_PATH" && git describe --tags --always 2>/dev/null)
    if [ -n "$version" ]; then
        echo "$version"
        exit 0
    fi
fi

echo "UNKNOWN"
exit 1
