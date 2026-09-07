#!/bin/bash
################################################################################
# validate_ingenious_root.sh
#
# Purpose: Validate that a given path is an INGenious installation root
# Usage: ./validate_ingenious_root.sh <path_to_validate>
# Returns: 0 if valid, 1 if invalid
#
# This script checks for INGenious markers to confirm the path contains
# a valid INGenious installation. At least 2 indicators must be present.
################################################################################

USER_PATH="${1:-.}"

# INGenious markers to check
INDICATORS=(
    "Configuration/conf.js"
    "Configuration/XPLOR_SETTINGS.json"
    "Engine/pom.xml"
    "Projects/"
    "Run.bat"
    "Run.command"
)

# Count how many indicators exist
found=0
for indicator in "${INDICATORS[@]}"; do
    if [ -e "$USER_PATH/$indicator" ]; then
        ((found++))
        echo "✓ Found: $indicator"
    fi
done

echo ""
echo "Total indicators found: $found out of ${#INDICATORS[@]}"

# Require at least 2 indicators
if [ $found -lt 2 ]; then
    echo "⚠️  Warning: Only $found INGenious markers found in $USER_PATH"
    echo "Please confirm this is the correct path, or provide an alternative."
    exit 1
else
    echo "✓ Valid INGenious installation detected"
    exit 0
fi
